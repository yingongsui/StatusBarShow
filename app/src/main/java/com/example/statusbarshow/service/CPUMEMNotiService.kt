package com.example.statusbarshow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.statusbarshow.CMFloatingState
import com.example.statusbarshow.CMNotiState
import com.example.statusbarshow.CPUNotiType
import com.example.statusbarshow.FloatingWindowHelper
import com.example.statusbarshow.LogUtils
import com.example.statusbarshow.MEMNotiType
import com.example.statusbarshow.MyFunction
import com.example.statusbarshow.cmsamplingtime
import com.example.statusbarshow.cpuusage
import com.example.statusbarshow.memstate
import com.example.statusbarshow.screenstate
import com.example.statusbarshow.totalcpuusage
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class CPUMEMNotiService : Service() {

    private val channelId = "cpumem_channel"
    private var notificationManager: NotificationManager? = null
    private val floatingHelper by lazy { FloatingWindowHelper(this) }
    private val baseWhen = System.currentTimeMillis()

    private val monitorserviceJob = SupervisorJob()
    private val monitorserviceScope = CoroutineScope(Dispatchers.IO + monitorserviceJob)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "CPUMEM Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager?.createNotificationChannel(channel)

        startForeground(1, buildNotification(arrayOf("--%", "--G")))
        if (CMFloatingState) {
            floatingHelper.show(arrayOf("--%", "--G"))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val coreNumber = prefs.getInt("CPUCoreNumber", 0)

        if (monitorJob?.isActive == true) return START_STICKY

        monitorJob = monitorserviceScope.launch {
            val allCpuTime1 = Array(coreNumber + 1) { Array(3) { 0L } }
            val allCpuTime2 = Array(coreNumber + 1) { Array(3) { 0L } }
            val maxFreqs = DoubleArray(coreNumber + 1) { i ->
                prefs.getLong("CPU${i}MaxFreq", 1L).toDouble()
            }
            val totalMaxFreq = maxFreqs.sum().coerceAtLeast(1.0)
            var lastDisplayValues: Array<String>? = null

            while (CMNotiState || CMFloatingState) {
                if (!screenstate) {
                    withContext(Dispatchers.Main) {
                        floatingHelper.hide()
                    }
                    while (!screenstate && (CMNotiState || CMFloatingState)) {
                        delay(2000.milliseconds)
                    }
                    continue
                }

                try {
                    MyFunction.readMemStatus()
                    for (i in 0..coreNumber) {
                        allCpuTime1[i] = MyFunction.readCpuStatus(i)
                    }

                    delay(cmsamplingtime.milliseconds)

                    for (i in 0..coreNumber) {
                        allCpuTime2[i] = MyFunction.readCpuStatus(i)
                    }

                    for (i in 0..coreNumber) {
                        val timeDelta = allCpuTime2[i][2] - allCpuTime1[i][2]
                        if (timeDelta > 0) {
                            val rawUsage1 = (allCpuTime2[i][1] - allCpuTime1[i][1]) * 100 / timeDelta
                            val rawUsage2 = 100 - (allCpuTime2[i][0] - allCpuTime1[i][0]) * 100 / timeDelta

                            cpuusage[i][0] = rawUsage1.coerceIn(0L, 100L).toInt()
                            cpuusage[i][1] = rawUsage2.coerceIn(0L, 100L).toInt()
                        }
                    }

                    var weightedWorkSum = 0.0
                    var weightedIdleSum = 0.0
                    for (i in 0..coreNumber) {
                        val weight = maxFreqs[i] / totalMaxFreq
                        weightedWorkSum += cpuusage[i][0].toDouble() * weight
                        weightedIdleSum += cpuusage[i][1].toDouble() * weight
                    }

                    totalcpuusage[0] = weightedWorkSum.roundToInt().coerceIn(0, 100)
                    totalcpuusage[1] = weightedIdleSum.roundToInt().coerceIn(0, 100)

                    val memG = arrayOf(memstate[1], memstate[0] - memstate[1])[MEMNotiType].toFloat() / 1024 / 1024
                    val displayValues = arrayOf(
                        "%02d%%".format(totalcpuusage[CPUNotiType]),
                        "%.1fG".format(memG)
                    )

                    if (CMNotiState) {
                        if (!displayValues.contentEquals(lastDisplayValues)) {
                            notificationManager?.notify(1, buildNotification(displayValues))
                            lastDisplayValues = displayValues
                        }
                    } else {
                        notificationManager?.cancel(1)
                        lastDisplayValues = null
                    }

                    if (CMFloatingState) {
                        withContext(Dispatchers.Main) {
                            floatingHelper.show(displayValues)
                            floatingHelper.update(displayValues)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            floatingHelper.hide()
                        }
                    }

                } catch (e: Exception) {
                    LogUtils.e("CPUMEMService", "Calculation Error", e)
                    delay(2000.milliseconds)
                }
            }
            withContext(Dispatchers.Main) {
                floatingHelper.hide()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        floatingHelper.hide()
        monitorJob?.cancel()
        monitorserviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        LogUtils.d("CPUMEMNotiService", "Stop Service")
        super.onDestroy()
    }

    private fun buildNotification(currentValue: Array<String>): Notification {
        val bitmap = MyFunction.createBitmapFromString(currentValue, 0.6f, refText = "100%")
        val cpuStr = String.format(java.util.Locale.US, "%3s", currentValue[0])
        val memStr = String.format(java.util.Locale.US, "%5s", currentValue[1])

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(IconCompat.createWithBitmap(bitmap))
            .setContentTitle("System Status")
            .setContentText("CPU: $cpuStr | MEM: $memStr")
            .setGroup("GROUP_USAGE")
            .setSortKey("1")
            .setWhen(baseWhen + 1000L)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
