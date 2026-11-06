package com.yin.statusbarshow

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Volatile var screenstate: Boolean = true
@Volatile var netsamplingtime: Long = 1000L
@Volatile var cmsamplingtime: Long = 1500L
@Volatile var cpucorecount: Int = 0

val memstate = mutableStateListOf(0, 0)
val memusage = mutableIntStateOf(0)
val cpuusage = mutableStateListOf<SnapshotStateList<Int>>()
val totalcpuusage = mutableStateListOf(0, 0)
val netspeedRx = mutableFloatStateOf(0f)
val netspeedTx = mutableFloatStateOf(0f)

@Volatile var CPUNotiType: Int = 0
@Volatile var MEMNotiType: Int = 1

@Volatile var CMNotiState: Boolean = false
@Volatile var NETNotiState: Boolean = false
@Volatile var NetFloatingState: Boolean = false
@Volatile var CMFloatingState: Boolean = false
val CPUDrawState = mutableStateListOf<Boolean>()

val appThemeMode = mutableIntStateOf(0)       // 0 = Dark, 1 = Light
val appCurveColor1 = mutableIntStateOf(0)
val appCurveColor2 = mutableIntStateOf(1)
val floatingSizeIndex = mutableIntStateOf(0)

object AppState {
    @Volatile private var initialized = false

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        LogUtils.enableLog = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.let { screenstate = it.isInteractive }

        val prefs = appContext.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean("PrefState", false)) {
            MyFunction.getSysInfo(appContext)
        }

        cpucorecount = prefs.getInt("CPUCoreNumber", 0)
        netsamplingtime = prefs.getLong("NETSamplingTime", 1000L)
        cmsamplingtime = prefs.getLong("CMSamplingTime", 1500L)
        CPUNotiType = prefs.getInt("CPUNotiType", 0)
        MEMNotiType = prefs.getInt("MEMNotiType", 1)
        CMNotiState = prefs.getBoolean("CMNotiState", false)
        NETNotiState = prefs.getBoolean("NETNotiState", false)
        NetFloatingState = prefs.getBoolean("NetFloatingState", false)
        CMFloatingState = prefs.getBoolean("CMFloatingState", false)

        appThemeMode.intValue = prefs.getInt("AppThemeMode", 0)
        appCurveColor1.intValue = prefs.getInt("AppCurveColor", 0)
        appCurveColor2.intValue = prefs.getInt("AppCurveColor2", 1)
        floatingSizeIndex.intValue = prefs.getInt("FloatingSizeIndex", 0)

        val requiredSize = cpucorecount + 1
        if (cpuusage.size != requiredSize) {
            cpuusage.clear()
            repeat(requiredSize) {
                cpuusage.add(mutableStateListOf(0, 0))
            }
        }
        if (CPUDrawState.size != requiredSize) {
            CPUDrawState.clear()
            for (index in 0 until requiredSize) {
                CPUDrawState.add(prefs.getBoolean("CPU${index}DrawState", true))
            }
        }

        initialized = true
    }
}

class ScreenStateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppState.ensureInitialized(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    screenstate = when (intent?.action) {
                        Intent.ACTION_SCREEN_ON -> true
                        Intent.ACTION_SCREEN_OFF -> false
                        else -> screenstate
                    }
                    LogUtils.d("ScreenStateReceiver", "Screen state changed: $screenstate")
                }
            },
            filter,
        )
    }
}

object MyFunction {

    private class RenderScratch {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textSize = 100f
        }
        val path = Path()
        val bounds = RectF()
        val refBounds = RectF()
        val dstRect = RectF()
        val matrix = Matrix()
    }

    private val renderScratch = ThreadLocal.withInitial { RenderScratch() }
    private val refBoundsCache = ConcurrentHashMap<String, RectF>()

    private class CoreSysFiles(cpuIndex: Int) {
        val rootPath = "/sys/devices/system/cpu/cpu$cpuIndex"
        val timeInStateFile = File("$rootPath/cpufreq/stats/time_in_state")
        @Volatile var idleTimeFiles: Array<File>? = null

        fun resolveIdleFiles(): Array<File> {
            idleTimeFiles?.let { if (it.isNotEmpty()) return it }
            val discovered = ArrayList<File>(4)
            var j = 0
            while (true) {
                val file = File("$rootPath/cpuidle/state$j/time")
                if (file.exists()) {
                    discovered.add(file)
                    j++
                } else {
                    break
                }
            }
            val arr = discovered.toTypedArray()
            if (arr.isNotEmpty()) {
                idleTimeFiles = arr
            }
            return arr
        }
    }

    private val coreFilesCache = ConcurrentHashMap<Int, CoreSysFiles>()
    private val memInfoFile = File("/proc/meminfo")

    fun createContentPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun ensureSilentChannel(
        notificationManager: NotificationManager?,
        channelId: String,
        channelName: String,
    ) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager?.createNotificationChannel(channel)
    }

    fun startMonitorForeground(service: Service, id: Int, notification: Notification) {
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(service, id, notification, fgsType)
    }

    fun buildStatusNotification(
        context: Context,
        channelId: String,
        iconValues: Array<String>,
        refText: String,
        title: String,
        contentText: String,
        pendingIntent: PendingIntent,
        baseWhen: Long,
    ): Notification {
        val bitmap = createBitmapFromString(iconValues, 0.6f, refText = refText)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(IconCompat.createWithBitmap(bitmap))
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setWhen(baseWhen)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun createBitmapFromString(
        stateValue: Array<String>,
        size: Float,
        px: Int = 96,
        refText: String? = null,
    ): Bitmap {
        val bitmap = createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scratch = renderScratch.get()!!

        val gap = px * 0.06f
        val h1 = (px - gap) * size

        scratch.dstRect.apply {
            left = 0f
            top = 0f
            right = px.toFloat()
            bottom = h1
        }
        drawFitted(canvas, scratch, stateValue[0], scratch.dstRect, refText = refText)

        scratch.dstRect.apply {
            left = 0f
            top = h1 + gap
            right = px.toFloat()
            bottom = px.toFloat()
        }
        drawFitted(canvas, scratch, stateValue[1], scratch.dstRect)
        return bitmap
    }

    /** 把文字按笔画真实边界画进 dst：高度铺满；太宽就横向压缩，太窄就居中（最多横向拉宽 maxStretch 倍） */
    private fun drawFitted(
        canvas: Canvas,
        scratch: RenderScratch,
        text: String,
        dst: RectF,
        refText: String? = null,
        maxStretch: Float = 1.2f,
    ) {
        if (text.isEmpty()) return
        val paint = scratch.paint
        val path = scratch.path
        val b = scratch.bounds

        path.reset()
        paint.getTextPath(text, 0, text.length, 0f, 0f, path)
        path.computeBounds(b, true)
        if (b.height() <= 0f) return

        val refB: RectF = if (refText != null) {
            refBoundsCache.getOrPut(refText) {
                val tmpPath = Path()
                paint.getTextPath(refText, 0, refText.length, 0f, 0f, tmpPath)
                RectF().also { tmpPath.computeBounds(it, true) }
            }
        } else {
            scratch.refBounds.apply { set(b) }
        }

        if ((refB.width() <= 0f) || (refB.height() <= 0f)) return

        val sy = dst.height() / refB.height()
        val sx = minOf(dst.width() / refB.width(), sy * maxStretch)
        val m = scratch.matrix.apply {
            reset()
            postTranslate(-b.left, -b.top)
            postScale(sx, sy)
            postTranslate(dst.left + ((dst.width() - (b.width() * sx)) / 2f), dst.top)
        }
        path.transform(m)
        canvas.drawPath(path, paint)
    }

    fun readMemStatus() {
        var totalKb = 0
        var availKb = 0
        var foundMask = 0
        try {
            memInfoFile.bufferedReader().use { reader ->
                var line = reader.readLine()
                while ((line != null) && (foundMask != 3)) {
                    if (line.startsWith("MemTotal:")) {
                        totalKb = parseFirstLong(line).toInt()
                        foundMask = foundMask or 1
                    } else if (line.startsWith("MemAvailable:")) {
                        availKb = parseFirstLong(line).toInt()
                        foundMask = foundMask or 2
                    }
                    line = reader.readLine()
                }
            }
            if (totalKb > 0) {
                val usagePercent = (100 - ((availKb * 100L) / totalKb)).toInt().coerceIn(0, 100)
                Snapshot.withMutableSnapshot {
                    memstate[0] = totalKb
                    memstate[1] = availKb
                    memusage.intValue = usagePercent
                }
            }
        } catch (e: Exception) {
            LogUtils.e("MyFunction", "Failed to read /proc/meminfo", e)
        }
    }

    fun readCpuStatusInto(cpuIndex: Int, maxFreq: Long, out: LongArray) {
        out[0] = 0L
        out[1] = 0L
        out[2] = 0L
        val safeMaxFreq = maxFreq.coerceAtLeast(1L)
        val coreFiles = coreFilesCache.getOrPut(cpuIndex) { CoreSysFiles(cpuIndex) }

        try {
            var totalIdleTime = 0L
            for (idleFile in coreFiles.resolveIdleFiles()) {
                readFirstLineOrNull(idleFile)?.let { line ->
                    totalIdleTime += parseFirstLong(line)
                }
            }
            out[0] = totalIdleTime / 1000L

            if (coreFiles.timeInStateFile.exists()) {
                coreFiles.timeInStateFile.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        parseTimeInStateLine(line, safeMaxFreq, out)
                        line = reader.readLine()
                    }
                }
            }
        } catch (_: Exception) {
            out[0] = 0L
            out[1] = 0L
            out[2] = 0L
        }
    }

    private fun parseFirstLong(line: String): Long {
        val len = line.length
        var i = 0
        while ((i < len) && (line[i] !in '0'..'9')) i++
        if (i >= len) return 0L
        var value = 0L
        while (i < len) {
            val c = line[i]
            if (c in '0'..'9') {
                value = (value * 10L) + (c - '0')
                i++
            } else {
                break
            }
        }
        return value
    }

    private fun parseTimeInStateLine(line: String, maxFreq: Long, out: LongArray) {
        val len = line.length
        var i = 0
        while ((i < len) && (line[i] <= ' ')) i++
        if ((i >= len) || (line[i] !in '0'..'9')) return
        var freq = 0L
        while ((i < len) && (line[i] in '0'..'9')) {
            freq = (freq * 10L) + (line[i] - '0')
            i++
        }
        while ((i < len) && (line[i] <= ' ')) i++
        if ((i >= len) || (line[i] !in '0'..'9')) return
        var rawTime = 0L
        while ((i < len) && (line[i] in '0'..'9')) {
            rawTime = (rawTime * 10L) + (line[i] - '0')
            i++
        }
        val time = rawTime * 10L
        out[1] += (freq * time) / maxFreq
        out[2] += time
    }

    private fun readFirstLineOrNull(file: File): String? =
        try {
            file.bufferedReader().use { it.readLine() }
        } catch (_: Exception) {
            null
        }

    fun getSysInfo(context: Context) {
        val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val dataMap = mutableMapOf<String, String>()
        val longMap = mutableMapOf<String, Long>()

        try {
            var currentProcessor = -1
            File("/proc/cpuinfo").useLines { lines ->
                lines.forEach { line ->
                    val colonIdx = line.indexOf(':')
                    if (colonIdx <= 0) return@forEach
                    val key = line.substring(0, colonIdx).trim()
                    val value = line.substring(colonIdx + 1).trim()

                    if (key.contains("processor")) {
                        currentProcessor = value.toIntOrNull() ?: 0
                        cpucorecount = maxOf(cpucorecount, currentProcessor)

                        val maxFreqFile = File("/sys/devices/system/cpu/cpu$currentProcessor/cpufreq/cpuinfo_max_freq")
                        longMap["CPU${currentProcessor}MaxFreq"] = parseFirstLong(readFirstLineOrNull(maxFreqFile).orEmpty())
                    }

                    if ((currentProcessor != -1) &&
                        (key.contains("implementer") || key.contains("architecture") ||
                            key.contains("variant") || key.contains("part"))
                    ) {
                        val currentVal = dataMap["CPU$currentProcessor"].orEmpty()
                        dataMap["CPU$currentProcessor"] = "$currentVal$value;"
                    }
                }
            }

            memInfoFile.useLines { lines ->
                lines.firstOrNull { it.startsWith("MemTotal") }
            }?.let { memTotalLine ->
                val colonIdx = memTotalLine.indexOf(':')
                if (colonIdx >= 0) {
                    dataMap["TotalMemory"] = memTotalLine.substring(colonIdx + 1).trim()
                }
            }

            prefs.edit {
                putBoolean("PrefState", true)
                putInt("CPUCoreNumber", cpucorecount)
                dataMap.forEach { (k, v) -> putString(k, v) }
                longMap.forEach { (k, v) -> putLong(k, v) }
            }
        } catch (e: Exception) {
            LogUtils.e("MyFunction", "Failed to read system info", e)
        }
    }

    fun getCPUArch(info: String): String {
        return when {
            info.contains("0xd85", ignoreCase = true) -> "Cortex-X925 ARMv9.4-A"
            info.contains("0xd84", ignoreCase = true) -> "Cortex-A725 ARMv9.2-A"
            info.contains("0xd82", ignoreCase = true) -> "Cortex-X4 ARMv9.2-A"
            info.contains("0xc05", ignoreCase = true) -> "Cortex-A5 ARMv7-A"
            info.contains("0xc07", ignoreCase = true) -> "Cortex-A7 ARMv7-A"
            info.contains("0xc08", ignoreCase = true) -> "Cortex-A8 ARMv7-A"
            info.contains("0xc09", ignoreCase = true) -> "Cortex-A9 ARMv7-A"
            info.contains("0xc0d", ignoreCase = true) -> "Cortex-A12 ARMv7-A"
            info.contains("0xc0e", ignoreCase = true) -> "Cortex-A17 ARMv7-A"
            info.contains("0xc0f", ignoreCase = true) -> "Cortex-A15 ARMv7-A"
            info.contains("0xd01", ignoreCase = true) -> "Cortex-A32 ARMv8-A (32-bit)"
            info.contains("0xd02", ignoreCase = true) -> "Cortex-A34 ARMv8-A"
            info.contains("0xd03", ignoreCase = true) -> "Cortex-A53 ARMv8-A"
            info.contains("0xd04", ignoreCase = true) -> "Cortex-A35 ARMv8-A"
            info.contains("0xd05", ignoreCase = true) -> "Cortex-A55 ARMv8.2-A"
            info.contains("0xd06", ignoreCase = true) -> "Cortex-A65 ARMv8.2-A"
            info.contains("0xd07", ignoreCase = true) -> "Cortex-A57 ARMv8-A"
            info.contains("0xd08", ignoreCase = true) -> "Cortex-A72 ARMv8-A"
            info.contains("0xd09", ignoreCase = true) -> "Cortex-A73 ARMv8-A"
            info.contains("0xd0a", ignoreCase = true) -> "Cortex-A75 ARMv8.2-A"
            info.contains("0xd0b", ignoreCase = true) -> "Cortex-A76 ARMv8.2-A"
            info.contains("0xd0d", ignoreCase = true) -> "Cortex-A77 ARMv8.2-A"
            info.contains("0xd0e", ignoreCase = true) -> "Cortex-A76AE ARMv8.2-A"
            info.contains("0xd41", ignoreCase = true) -> "Cortex-A78 ARMv8.2-A"
            info.contains("0xd43", ignoreCase = true) -> "Cortex-A65AE ARMv8.2-A"
            info.contains("0xd44", ignoreCase = true) -> "Cortex-X1 ARMv8.2-A"
            info.contains("0xd46", ignoreCase = true) -> "Cortex-A510 ARMv9-A"
            info.contains("0xd47", ignoreCase = true) -> "Cortex-A710 ARMv9-A"
            info.contains("0xd48", ignoreCase = true) -> "Cortex-X2 ARMv9-A"
            info.contains("0xd4d", ignoreCase = true) -> "Cortex-A715 ARMv9.2-A"
            info.contains("0xd4e", ignoreCase = true) -> "Cortex-X3 ARMv9-A"

            info.contains("0x002", ignoreCase = true) -> {
                when {
                    info.contains("0x3", ignoreCase = true) -> "Qualcomm Oryon V2 (Performance)"
                    info.contains("0x2", ignoreCase = true) -> "Qualcomm Oryon V2 (Efficiency)"
                    else -> "Qualcomm Oryon V2"
                }
            }

            info.contains("0x001", ignoreCase = true) -> {
                val parts = info.split(";").map { it.trim() }
                when {
                    parts.contains("0x2") || parts.contains("0x3") -> "Qualcomm Oryon V1 (Performance)"
                    parts.contains("0x4") || parts.contains("0x5") -> "Qualcomm Oryon V1 (Efficiency)"
                    else -> "Qualcomm Oryon V1"
                }
            }

            info.contains("kB", ignoreCase = true) -> parseFirstLong(info).toString()
            else -> info.trim().ifEmpty { "Unknown" }
        }
    }
}

object LogUtils {
    @Volatile var enableLog: Boolean = false
    fun d(tag: String, msg: String) {
        if (enableLog) Log.d(tag, msg)
    }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (enableLog) Log.e(tag, msg, t)
    }
}
