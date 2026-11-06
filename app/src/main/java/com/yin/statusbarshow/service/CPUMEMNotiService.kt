package com.yin.statusbarshow.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.snapshots.Snapshot
import com.yin.statusbarshow.AppState
import com.yin.statusbarshow.CMFloatingState
import com.yin.statusbarshow.CMNotiState
import com.yin.statusbarshow.CPUNotiType
import com.yin.statusbarshow.FloatingWindowHelper
import com.yin.statusbarshow.LogUtils
import com.yin.statusbarshow.MEMNotiType
import com.yin.statusbarshow.MyFunction
import com.yin.statusbarshow.cmsamplingtime
import com.yin.statusbarshow.cpuusage
import com.yin.statusbarshow.memstate
import com.yin.statusbarshow.screenstate
import com.yin.statusbarshow.totalcpuusage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class CPUMEMNotiService : Service() {

    private val channelId = "cpumem_channel"
    private var notificationManager: NotificationManager? = null
    private val floatingHelper by lazy {
        FloatingWindowHelper(this, prefKeyPrefix = "CMFloating", defaultY = 300)
    }
    private val contentPendingIntent: PendingIntent by lazy {
        MyFunction.createContentPendingIntent(this)
    }
    private val baseWhen = System.currentTimeMillis()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppState.ensureInitialized(applicationContext)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        MyFunction.ensureSilentChannel(notificationManager, channelId, "CPUMEM Channel")

        val initialValues = arrayOf("--%", "--G")
        MyFunction.startMonitorForeground(this, 1, buildNotification(initialValues))
        if (CMFloatingState) {
            floatingHelper.show(initialValues)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppState.ensureInitialized(applicationContext)
        if (monitorJob?.isActive == true) return START_STICKY

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val coreNumber = prefs.getInt("CPUCoreNumber", 0)

        monitorJob = serviceScope.launch {
            var prevCpuTime = Array(coreNumber + 1) { LongArray(3) }
            var currCpuTime = Array(coreNumber + 1) { LongArray(3) }
            val maxFreqLongs = LongArray(coreNumber + 1) { i ->
                prefs.getLong("CPU${i}MaxFreq", 1L).coerceAtLeast(1L)
            }
            val totalMaxFreq = maxFreqLongs.sum().toDouble().coerceAtLeast(1.0)
            val coreWeights = DoubleArray(coreNumber + 1) { i ->
                maxFreqLongs[i].toDouble() / totalMaxFreq
            }
            val tempUsages0 = IntArray(coreNumber + 1)
            val tempUsages1 = IntArray(coreNumber + 1)

            var lastNotiValues: Array<String>? = null
            var lastFloatingValues: Array<String>? = null

            for (i in 0..coreNumber) {
                MyFunction.readCpuStatusInto(i, maxFreqLongs[i], prevCpuTime[i])
            }

            while (CMNotiState || CMFloatingState) {
                if (!screenstate) {
                    if (floatingHelper.isShowing) {
                        withContext(Dispatchers.Main) { floatingHelper.hide() }
                        lastFloatingValues = null
                    }
                    while (!screenstate && (CMNotiState || CMFloatingState)) {
                        delay(2000.milliseconds)
                    }
                    if (!(CMNotiState || CMFloatingState)) break

                    for (i in 0..coreNumber) {
                        MyFunction.readCpuStatusInto(i, maxFreqLongs[i], prevCpuTime[i])
                    }
                    continue
                }

                try {
                    delay(cmsamplingtime.milliseconds)
                    if (!screenstate || !(CMNotiState || CMFloatingState)) continue

                    MyFunction.readMemStatus()
                    for (i in 0..coreNumber) {
                        MyFunction.readCpuStatusInto(i, maxFreqLongs[i], currCpuTime[i])
                    }

                    var weightedWorkSum = 0.0
                    var weightedIdleSum = 0.0

                    for (i in 0..coreNumber) {
                        val timeDelta = currCpuTime[i][2] - prevCpuTime[i][2]
                        if (timeDelta > 0L) {
                            tempUsages0[i] = (((currCpuTime[i][1] - prevCpuTime[i][1]) * 100L) / timeDelta)
                                .coerceIn(0L, 100L).toInt()
                            tempUsages1[i] = (100L - (((currCpuTime[i][0] - prevCpuTime[i][0]) * 100L) / timeDelta))
                                .coerceIn(0L, 100L).toInt()
                        } else {
                            tempUsages0[i] = cpuusage.getOrNull(i)?.getOrNull(0) ?: 0
                            tempUsages1[i] = cpuusage.getOrNull(i)?.getOrNull(1) ?: 0
                        }
                        weightedWorkSum += tempUsages0[i] * coreWeights[i]
                        weightedIdleSum += tempUsages1[i] * coreWeights[i]
                    }

                    val swap = prevCpuTime
                    prevCpuTime = currCpuTime
                    currCpuTime = swap

                    val total0 = weightedWorkSum.roundToInt().coerceIn(0, 100)
                    val total1 = weightedIdleSum.roundToInt().coerceIn(0, 100)

                    Snapshot.withMutableSnapshot {
                        for (i in 0..coreNumber) {
                            if (i < cpuusage.size) {
                                cpuusage[i][0] = tempUsages0[i]
                                cpuusage[i][1] = tempUsages1[i]
                            }
                        }
                        totalcpuusage[0] = total0
                        totalcpuusage[1] = total1
                    }

                    val selectedCpu = if (CPUNotiType == 0) total0 else total1
                    val memKb = if (MEMNotiType == 0) memstate[1] else (memstate[0] - memstate[1])
                    val memG = memKb.toFloat() / (1024f * 1024f)

                    val displayValues = arrayOf(
                        String.format(Locale.US, "%02d%%", selectedCpu),
                        String.format(Locale.US, "%.1fG", memG),
                    )

                    if (CMNotiState) {
                        if (!displayValues.contentEquals(lastNotiValues)) {
                            notificationManager?.notify(1, buildNotification(displayValues))
                            lastNotiValues = displayValues
                        }
                    } else if (lastNotiValues != null) {
                        notificationManager?.cancel(1)
                        lastNotiValues = null
                    }

                    if (CMFloatingState) {
                        if (!floatingHelper.isShowing || !displayValues.contentEquals(lastFloatingValues)) {
                            withContext(Dispatchers.Main) { floatingHelper.show(displayValues) }
                            lastFloatingValues = displayValues
                        }
                    } else if (floatingHelper.isShowing) {
                        withContext(Dispatchers.Main) { floatingHelper.hide() }
                        lastFloatingValues = null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LogUtils.e("CPUMEMService", "Calculation Error", e)
                    delay(2000.milliseconds)
                }
            }

            if (floatingHelper.isShowing) {
                withContext(Dispatchers.Main) { floatingHelper.hide() }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        floatingHelper.hide()
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        LogUtils.d("CPUMEMNotiService", "Stop Service")
        super.onDestroy()
    }

    private fun buildNotification(currentValue: Array<String>): Notification {
        val cpuStr = String.format(Locale.US, "%3s", currentValue[0])
        val memStr = String.format(Locale.US, "%5s", currentValue[1])
        return MyFunction.buildStatusNotification(
            context = this,
            channelId = channelId,
            iconValues = currentValue,
            refText = "100%",
            title = "System Status",
            contentText = "CPU: $cpuStr | MEM: $memStr",
            pendingIntent = contentPendingIntent,
            baseWhen = baseWhen,
        )
    }
}
