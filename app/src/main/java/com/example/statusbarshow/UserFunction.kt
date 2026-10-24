package com.example.statusbarshow

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import java.io.File

// 全局变量 - 建议：未来迁移到 ViewModel 或 Repository 中
var screenstate = true
var netsamplingtime: Long = 1000
var cmsamplingtime: Long = 1500
var cpucorecount: Int = 0

var memstate = mutableStateListOf(0, 0)
var memusage = mutableIntStateOf(0)
var cpuusage = mutableStateListOf<SnapshotStateList<Int>>()
var totalcpuusage = mutableStateListOf(0, 0)
var netspeedRx = mutableFloatStateOf(0f)
var netspeedTx = mutableFloatStateOf(0f)

var CPUNotiType = 0
var MEMNotiType = 1

var CMNotiState = false
var NETNotiState = false
var CPUDrawState = mutableStateListOf<Boolean>()

// 监听屏幕状态
class ScreenStateApp : Application() {
    private lateinit var screenStateReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        screenStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                screenstate = when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> true
                    Intent.ACTION_SCREEN_OFF -> false
                    else -> screenstate
                }
                LogUtils.d("ScreenStateReceiver", "Screen state changed: $screenstate")
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    // 注意：onTerminate 在真机几乎不触发，建议将注销逻辑放入 Service 的 onDestroy
}

object MyFunction {

    fun createBitmapFromString(stateValue: Array<String>, size: Float, px: Int = 96, refText: String? = null): Bitmap {
        val bitmap = createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD) // 窄体粗体，代替 fakeBold
            textSize = 100f  // 只用来生成 Path，数值无所谓
        }
        val gap = px * 0.06f                 // 两行之间的间隙
        val h1 = (px - gap) * size           // 第一行高度
        //val top = if (stateValue[0].length < 4) stateValue[0] else "1.0"

        drawFitted(canvas, paint, stateValue[0], RectF(0f, 0f, px.toFloat(), h1), refText = refText)
        drawFitted(canvas, paint, stateValue[1], RectF(0f, h1 + gap, px.toFloat(), px.toFloat()))
        return bitmap
    }

    /** 把文字按笔画真实边界画进 dst：高度铺满；太宽就横向压缩，太窄就居中（最多横向拉宽 maxStretch 倍） */
    private fun drawFitted(canvas: Canvas, paint: Paint, text: String, dst: RectF, refText: String? = null, maxStretch: Float = 1.2f) {
        if (text.isEmpty()) return
        val measureText = refText ?: text
        val refPath = Path()
        paint.getTextPath(measureText, 0, measureText.length, 0f, 0f, refPath)
        val refB = RectF().also { refPath.computeBounds(it, true) }

        val path = Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, path)
        val b = RectF().also { path.computeBounds(it, true) }
        if (refB.width() <= 0f || refB.height() <= 0f || b.height() <= 0f) return

        val sy = dst.height() / refB.height()
        val sx = minOf(dst.width() / refB.width(), sy * maxStretch)
        val m = Matrix().apply {
            postTranslate(-b.left, -b.top)
            postScale(sx, sy)
            postTranslate(dst.left + (dst.width() - b.width() * sx) / 2f, dst.top)
        }
        path.transform(m)
        canvas.drawPath(path, paint)
    }

    fun readMemStatus(): Array<Int> {
        try {
            File("/proc/meminfo").useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("MemTotal") -> memstate[0] = parseMemLine(line)
                        line.startsWith("MemAvailable") -> memstate[1] = parseMemLine(line)
                    }
                }
            }
            // 优化：防止除以零异常
            val total = memstate[0]
            if (total > 0) {
                memusage.intValue = 100 - (memstate[1] * 100 / total)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return memstate.toTypedArray()
    }

    private fun parseMemLine(line: String): Int =
        line.split(Regex("\\s+"))[1].toIntOrNull() ?: 0

    fun readCpuStatus(cpuIndex: Int): Array<Long> {
        val root = "/sys/devices/system/cpu/cpu$cpuIndex"
        val alltimePath = "$root/cpufreq/stats/time_in_state"
        val maxFreqPath = "$root/cpufreq/cpuinfo_max_freq"

        return try {
            val maxFreq = readFirstLine(maxFreqPath).toLongOrNull() ?: 1L // 防止除以0

            // 循环累加所有 cpuidle 状态的时间 (state0, state1, state2...)
            var totalIdleTime = 0L
            var j = 0
            while (true) {
                val stateIdlePath = "$root/cpuidle/state$j/time"
                val file = File(stateIdlePath)
                if (file.exists()) {
                    val stateTime = file.readText().trim().toLongOrNull() ?: 0L
                    totalIdleTime += stateTime
                    j++
                } else {
                    break // 没有更多 state 时退出
                }
            }

            val cpustate = arrayOf(totalIdleTime / 1000, 0L, 0L) // (总idle, normalized_work, total_work)

            File(alltimePath).useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        val freq = parts[0].toLong()
                        val time = parts[1].toLong() * 10
                        cpustate[1] += (freq * time) / maxFreq
                        cpustate[2] += time
                    }
                }
            }
            cpustate
        } catch (_: Exception) {
            arrayOf(0L, 0L, 0L)
        }
    }

    private fun readFirstLine(path: String): String =
        File(path).bufferedReader().use { it.readLine() ?: "" }

    /**
     * 优化点：极大地减少 SharedPreferences 的打开次数。
     * 原本在循环中 edit，现在改为批量提交。
     */
    fun getSysInfo(context: Context) {
        val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val dataMap = mutableMapOf<String, String>()
        val longMap = mutableMapOf<String, Long>()

        try {
            var currentProcessor = -1
            File("/proc/cpuinfo").useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(":").map { it.trim() }
                    if (parts.size < 2) return@forEach
                    val key = parts[0]
                    val value = parts[1]

                    if (key.contains("processor")) {
                        currentProcessor = value.toIntOrNull() ?: 0
                        cpucorecount = maxOf(cpucorecount, currentProcessor)

                        // 获取该核最大频率
                        val maxFreq = readFirstLine("/sys/devices/system/cpu/cpu$currentProcessor/cpufreq/cpuinfo_max_freq").toLongOrNull() ?: 0L
                        longMap["CPU${currentProcessor}MaxFreq"] = maxFreq
                    }

                    if (currentProcessor != -1) {
                        val currentVal = dataMap["CPU$currentProcessor"] ?: ""
                        when {
                            key.contains("implementer") || key.contains("architecture") ||
                                    key.contains("variant") || key.contains("part") -> {
                                dataMap["CPU$currentProcessor"] = "$currentVal$value;"
                            }
                        }
                    }
                }
            }

            // 内存总量
            File("/proc/meminfo").useLines { it.firstOrNull { l -> l.startsWith("MemTotal") } }?.let {
                dataMap["TotalMemory"] = it.split(":")[1].trim()
            }

            // 一次性写入磁盘
            prefs.edit {
                putBoolean("PrefState", true)
                putInt("CPUCoreNumber", cpucorecount)
                dataMap.forEach { (k, v) -> putString(k, v) }
                longMap.forEach { (k, v) -> putLong(k, v) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCPUArch(info: String): String {
        return when {
            // --- 常见 ARM 核心 ---
            info.contains("0xd44", ignoreCase = true) -> "Cortex-X1 ARMv8.2-A"
            info.contains("0xd82", ignoreCase = true) -> "Cortex-X4 ARMv9.2-A"
            info.contains("0xd84", ignoreCase = true) -> "Cortex-A725 ARMv9.2-A"
            info.contains("0xd85", ignoreCase = true) -> "Cortex-X925 ARMv9.4-A"
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

            // --- 高通 Oryon V2 (如 Snapdragon 8 Gen 5 等) ---
            info.contains("0x002", ignoreCase = true) -> {
                when {
                    // 如果实际测试中发现 0x3 是性能核、0x2 是能效核，把这里的条件对调即可
                    info.contains("0x3", ignoreCase = true) -> "Qualcomm Oryon V2 (Performance)"
                    info.contains("0x2", ignoreCase = true) -> "Qualcomm Oryon V2 (Efficiency)"
                    else -> "Qualcomm Oryon V2"
                }
            }

            // --- 高通 Oryon V1 (如 Snapdragon 8 Elite / 8 Gen 4) ---
            info.contains("0x001", ignoreCase = true) -> {
                val parts = info.split(";").map { it.trim() }
                when {
                    parts.contains("0x2") || parts.contains("0x3") -> "Qualcomm Oryon V1 (Performance)"
                    parts.contains("0x4") || parts.contains("0x5") -> "Qualcomm Oryon V1 (Efficiency)"
                    else -> "Qualcomm Oryon V1"
                }
            }

            // --- 兜底处理 ---
            info.contains("kB", ignoreCase = true) -> Regex("\\d+").find(info)?.value ?: info
            else -> info.trim().ifEmpty { "Unknown" }
        }
    }
}

object LogUtils {
    private const val ENABLE_LOG = true
    fun d(tag: String, msg: String) { if (ENABLE_LOG) Log.d(tag, msg) }
    fun e(tag: String, msg: String, t: Throwable? = null) { if (ENABLE_LOG) Log.e(tag, msg, t) }
}