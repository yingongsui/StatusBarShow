package com.example.statusbarshow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.TrafficStats
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.statusbarshow.FloatingWindowHelper
import com.example.statusbarshow.LogUtils
import com.example.statusbarshow.MyFunction
import com.example.statusbarshow.NETNotiState
import com.example.statusbarshow.NetFloatingState
import com.example.statusbarshow.netsamplingtime
import com.example.statusbarshow.netspeedRx
import com.example.statusbarshow.netspeedTx
import com.example.statusbarshow.screenstate
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class NetService : Service() {

    private val channelId = "net_channel"
    private var notificationManager: NotificationManager? = null
    private val floatingHelper by lazy { FloatingWindowHelper(this) }
    private val baseWhen = System.currentTimeMillis()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "NET Channel", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager?.createNotificationChannel(channel)

        startForeground(2, updateNotification(arrayOf("0", "KB/s"), "0 KB/s", "0 KB/s"))
        if (NetFloatingState) {
            floatingHelper.show(arrayOf("0", "KB/s"))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob?.isActive != true) {
            monitorJob = serviceScope.launch {
                var lastRxBytes = TrafficStats.getTotalRxBytes()
                var lastTxBytes = TrafficStats.getTotalTxBytes()
                var lastTime = System.currentTimeMillis()
                var lastDisplayData: Array<String>? = null
                var lastRxStr = ""
                var lastTxStr = ""

                while (NETNotiState || NetFloatingState) {
                    if (!screenstate) {
                        withContext(Dispatchers.Main) {
                            floatingHelper.hide()
                        }
                        while (!screenstate && (NETNotiState || NetFloatingState)) {
                            delay(2000L.milliseconds)
                        }
                        lastRxBytes = TrafficStats.getTotalRxBytes()
                        lastTxBytes = TrafficStats.getTotalTxBytes()
                        lastTime = System.currentTimeMillis()
                        continue
                    }

                    val currentRxBytes = TrafficStats.getTotalRxBytes()
                    val currentTxBytes = TrafficStats.getTotalTxBytes()
                    val currentTime = System.currentTimeMillis()

                    val timeDelta = (currentTime - lastTime) / 1000f
                    if ((timeDelta > 0) && (lastRxBytes > 0)) {
                        val rxSpeed = (currentRxBytes - lastRxBytes) / timeDelta
                        val txSpeed = (currentTxBytes - lastTxBytes) / timeDelta

                        netspeedRx.floatValue = rxSpeed / 1024f // KB/s
                        netspeedTx.floatValue = txSpeed / 1024f // KB/s

                        val totalSpeedKB = netspeedRx.floatValue + netspeedTx.floatValue

                        val displayData = when {
                            totalSpeedKB >= (1024 * 100) -> {
                                val mb = totalSpeedKB / 1024f
                                if (mb >= 1024) arrayOf("%.1f".format(mb / 1024f), "GB/s")
                                else arrayOf("%.0f".format(mb), "MB/s")
                            }
                            totalSpeedKB >= 1000f -> {
                                val mb = totalSpeedKB / 1024f
                                arrayOf("%.1f".format(mb), "MB/s")
                            }
                            else -> {
                                arrayOf("%.0f".format(totalSpeedKB), "KB/s")
                            }
                        }

                        val rxKB = netspeedRx.floatValue
                        val txKB = netspeedTx.floatValue
                        val rxStr = if (rxKB >= 1024) "%.1f MB/s".format(rxKB / 1024f) else "%.0f KB/s".format(rxKB)
                        val txStr = if (txKB >= 1024) "%.1f MB/s".format(txKB / 1024f) else "%.0f KB/s".format(txKB)

                        if (NETNotiState) {
                            if ((!displayData.contentEquals(lastDisplayData)) || (rxStr != lastRxStr) || (txStr != lastTxStr)) {
                                notificationManager?.notify(2, updateNotification(displayData, rxStr, txStr))
                                lastDisplayData = displayData
                                lastRxStr = rxStr
                                lastTxStr = txStr
                            }
                        } else {
                            notificationManager?.cancel(2)
                            lastDisplayData = null
                        }

                        if (NetFloatingState) {
                            withContext(Dispatchers.Main) {
                                floatingHelper.show(displayData)
                                floatingHelper.update(displayData)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                floatingHelper.hide()
                            }
                        }
                    }

                    lastRxBytes = currentRxBytes
                    lastTxBytes = currentTxBytes
                    lastTime = currentTime

                    delay(netsamplingtime.milliseconds)
                }
                withContext(Dispatchers.Main) {
                    floatingHelper.hide()
                }
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

    private fun updateNotification(currentvalue: Array<String>, rxStr: String, txStr: String): Notification {
        val bitmap = MyFunction.createBitmapFromString(currentvalue, 0.6f, refText = "88.8")

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(IconCompat.createWithBitmap(bitmap))
            .setContentTitle("Net Speed")
            .setContentText("↓: $rxStr   ↑: $txStr")
            .setGroup("GROUP_SPEED")
            .setSortKey("2")
            .setWhen(baseWhen)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
