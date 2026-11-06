package com.yin.statusbarshow.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.TrafficStats
import android.os.IBinder
import androidx.compose.runtime.snapshots.Snapshot
import com.yin.statusbarshow.AppState
import com.yin.statusbarshow.FloatingWindowHelper
import com.yin.statusbarshow.LogUtils
import com.yin.statusbarshow.MyFunction
import com.yin.statusbarshow.NETNotiState
import com.yin.statusbarshow.NetFloatingState
import com.yin.statusbarshow.netsamplingtime
import com.yin.statusbarshow.netspeedRx
import com.yin.statusbarshow.netspeedTx
import com.yin.statusbarshow.screenstate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class NetService : Service() {

    private val channelId = "net_channel"
    private var notificationManager: NotificationManager? = null
    private val floatingHelper by lazy {
        FloatingWindowHelper(this, prefKeyPrefix = "NetFloating", defaultY = 450)
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
        MyFunction.ensureSilentChannel(notificationManager, channelId, "NET Channel")

        val initialValues = arrayOf("0", "KB/s")
        MyFunction.startMonitorForeground(
            this,
            2,
            updateNotification(initialValues, "0 KB/s", "0 KB/s"),
        )
        if (NetFloatingState) {
            floatingHelper.show(initialValues)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppState.ensureInitialized(applicationContext)
        if (monitorJob?.isActive == true) return START_STICKY

        monitorJob = serviceScope.launch {
            var lastRxBytes = TrafficStats.getTotalRxBytes()
            var lastTxBytes = TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()
            var lastNotiData: Array<String>? = null
            var lastFloatingData: Array<String>? = null

            while (NETNotiState || NetFloatingState) {
                if (!screenstate) {
                    if (floatingHelper.isShowing) {
                        withContext(Dispatchers.Main) { floatingHelper.hide() }
                        lastFloatingData = null
                    }
                    while (!screenstate && (NETNotiState || NetFloatingState)) {
                        delay(2000L.milliseconds)
                    }
                    if (!(NETNotiState || NetFloatingState)) break

                    lastRxBytes = TrafficStats.getTotalRxBytes()
                    lastTxBytes = TrafficStats.getTotalTxBytes()
                    lastTime = System.currentTimeMillis()
                    continue
                }

                try {
                    delay(netsamplingtime.milliseconds)
                    if (!screenstate || !(NETNotiState || NetFloatingState)) continue

                    val currentRxBytes = TrafficStats.getTotalRxBytes()
                    val currentTxBytes = TrafficStats.getTotalTxBytes()
                    val currentTime = System.currentTimeMillis()

                    val timeDelta = (currentTime - lastTime) / 1000f
                    if ((timeDelta > 0f) && (lastRxBytes >= 0L) && (currentRxBytes >= 0L)) {
                        val rxKB = ((currentRxBytes - lastRxBytes).coerceAtLeast(0L) / timeDelta) / 1024f
                        val txKB = ((currentTxBytes - lastTxBytes).coerceAtLeast(0L) / timeDelta) / 1024f

                        Snapshot.withMutableSnapshot {
                            netspeedRx.floatValue = rxKB
                            netspeedTx.floatValue = txKB
                        }

                        val totalSpeedKB = rxKB + txKB
                        val displayData = when {
                            totalSpeedKB >= (1024f * 100f) -> {
                                val mb = totalSpeedKB / 1024f
                                if (mb >= 1024f) {
                                    arrayOf(String.format(Locale.US, "%.1f", mb / 1024f), "GB/s")
                                } else {
                                    arrayOf(String.format(Locale.US, "%.0f", mb), "MB/s")
                                }
                            }
                            totalSpeedKB >= 1000f -> {
                                arrayOf(String.format(Locale.US, "%.1f", totalSpeedKB / 1024f), "MB/s")
                            }
                            else -> {
                                arrayOf(String.format(Locale.US, "%.0f", totalSpeedKB), "KB/s")
                            }
                        }

                        if (NETNotiState) {
                            if (!displayData.contentEquals(lastNotiData)) {
                                val rxStr = formatSpeed(rxKB)
                                val txStr = formatSpeed(txKB)
                                notificationManager?.notify(2, updateNotification(displayData, rxStr, txStr))
                                lastNotiData = displayData
                            }
                        } else if (lastNotiData != null) {
                            notificationManager?.cancel(2)
                            lastNotiData = null
                        }

                        if (NetFloatingState) {
                            if (!floatingHelper.isShowing || !displayData.contentEquals(lastFloatingData)) {
                                withContext(Dispatchers.Main) { floatingHelper.show(displayData) }
                                lastFloatingData = displayData
                            }
                        } else if (floatingHelper.isShowing) {
                            withContext(Dispatchers.Main) { floatingHelper.hide() }
                            lastFloatingData = null
                        }
                    }

                    lastRxBytes = currentRxBytes
                    lastTxBytes = currentTxBytes
                    lastTime = currentTime
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LogUtils.e("NetService", "Network speed calculation error", e)
                    delay(2000L.milliseconds)
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
        LogUtils.d("NetService", "Net Service Stopped")
        super.onDestroy()
    }

    private fun formatSpeed(speedKB: Float): String =
        if (speedKB >= 1024f) {
            String.format(Locale.US, "%.1f MB/s", speedKB / 1024f)
        } else {
            String.format(Locale.US, "%.0f KB/s", speedKB)
        }

    private fun updateNotification(currentValue: Array<String>, rxStr: String, txStr: String): Notification =
        MyFunction.buildStatusNotification(
            context = this,
            channelId = channelId,
            iconValues = currentValue,
            refText = "88.8",
            title = "Net Speed",
            contentText = "↓: $rxStr   ↑: $txStr",
            pendingIntent = contentPendingIntent,
            baseWhen = baseWhen,
        )
}
