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
import com.example.statusbarshow.LogUtils
import com.example.statusbarshow.MyFunction
import com.example.statusbarshow.NETNotiState
import com.example.statusbarshow.netsamplingtime
import com.example.statusbarshow.netspeedRx
import com.example.statusbarshow.netspeedTx
import com.example.statusbarshow.screenstate
import kotlinx.coroutines.*

class NetService : Service() {

    private val channelId = "net_channel"
    private var notificationManager: NotificationManager? = null

    // 使用 SupervisorJob 防止单个任务失败导致整个作用域崩溃
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // ZUI/国产ROM 适配：重要性设为 IMPORTANCE_LOW 防止状态栏弹出横幅干扰
        val channel = NotificationChannel(channelId, "NET Channel", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager?.createNotificationChannel(channel)

        // 启动前台，初始显示占位
        startForeground(2, updateNotification(arrayOf("0", "KB/s")))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob?.isActive != true) {
            monitorJob = serviceScope.launch {
                // 记录上次总流量，避免在循环内多次 delay 导致计算不准
                var lastRxBytes = TrafficStats.getTotalRxBytes()
                var lastTxBytes = TrafficStats.getTotalTxBytes()
                var lastTime = System.currentTimeMillis()

                while (NETNotiState) {
                    if (screenstate) {
                        // 1. 获取当前流量和时间
                        val currentRxBytes = TrafficStats.getTotalRxBytes()
                        val currentTxBytes = TrafficStats.getTotalTxBytes()
                        val currentTime = System.currentTimeMillis()

                        // 2. 计算间隔（防止除以0）
                        val timeDelta = (currentTime - lastTime) / 1000f // 转换为秒
                        if (timeDelta > 0 && lastRxBytes > 0) {

                            // 计算下行和上行速率 (B/s)
                            val rxSpeed = (currentRxBytes - lastRxBytes) / timeDelta
                            val txSpeed = (currentTxBytes - lastTxBytes) / timeDelta

                            netspeedRx.floatValue = rxSpeed / 1024f // KB/s
                            netspeedTx.floatValue = txSpeed / 1024f // KB/s

                            val totalSpeedKB = netspeedRx.floatValue + netspeedTx.floatValue

                            // 3. 动态选择单位并更新通知
                            val displayData = if (totalSpeedKB >= 1024) {
                                arrayOf("%.1f".format(totalSpeedKB / 1024f), "MB/s")
                            } else {
                                arrayOf("${totalSpeedKB.toInt()}", "KB/s")
                            }

                            notificationManager?.notify(2, updateNotification(displayData))
                        }

                        // 4. 更新历史记录点
                        lastRxBytes = currentRxBytes
                        lastTxBytes = currentTxBytes
                        lastTime = currentTime

                        delay(netsamplingtime)
                    } else {
                        // 屏幕关闭时大幅降低频率，仅维持后台不被杀，节省电量
                        delay(5000)
                        // 息屏重置基准点，防止亮屏瞬间数值飙升
                        lastRxBytes = TrafficStats.getTotalRxBytes()
                        lastTxBytes = TrafficStats.getTotalTxBytes()
                        lastTime = System.currentTimeMillis()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        LogUtils.d("NetService", "Net Service Stopped")
        super.onDestroy()
    }

    private fun updateNotification(currentvalue: Array<String>): Notification {
        val bitmap = MyFunction.createBitmapFromString(currentvalue, 0.6f, refText = "888")

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(IconCompat.createWithBitmap(bitmap))
            //.setLargeIcon(bitmap)
            .setContentTitle("NetSpeed")
            .setContentText("${currentvalue[0]} ${currentvalue[1]}")
            // 设为 PRIORITY_LOW 以便在状态栏安静显示数字，不产生横幅
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(true)
            .setSilent(true) // 强制静音，避免频繁通知导致的系统卡顿
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }
}