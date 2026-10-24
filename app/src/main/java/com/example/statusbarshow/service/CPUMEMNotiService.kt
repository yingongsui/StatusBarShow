package com.example.statusbarshow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.statusbarshow.CMNotiState
import com.example.statusbarshow.CPUNotiType
import com.example.statusbarshow.LogUtils
import com.example.statusbarshow.MEMNotiType
import com.example.statusbarshow.MyFunction
import com.example.statusbarshow.cmsamplingtime
import com.example.statusbarshow.cpuusage
import com.example.statusbarshow.totalcpuusage
import com.example.statusbarshow.memstate
import com.example.statusbarshow.screenstate
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class CPUMEMNotiService : Service() {

    private val channelId = "cpumem_channel"
    private var notificationManager: NotificationManager? = null

    // 使用专门的协程作用域管理后台任务
    private val monitorserviceJob = SupervisorJob()
    private val monitorserviceScope = CoroutineScope(Dispatchers.IO + monitorserviceJob)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0+ 频道创建
        val channel = NotificationChannel(
            channelId,
            "CPUMEM Channel",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false) // 状态标监测通常不显示角标
        }
        notificationManager?.createNotificationChannel(channel)

        // 立即启动前台服务，防止 ANR
        startForeground(1, buildNotification(arrayOf("--%", "--G")))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val coreNumber = prefs.getInt("CPUCoreNumber", 0)

        // 如果任务已经在运行，不要重复启动
        if (monitorJob?.isActive == true) return START_STICKY

        monitorJob = monitorserviceScope.launch {
            // 预分配内存，避免在 while 循环中重复创建数组
            val allCpuTime1 = Array(coreNumber + 1) { Array(3) { 0L } }
            val allCpuTime2 = Array(coreNumber + 1) { Array(3) { 0L } }

            while (CMNotiState) {
                if (screenstate) {
                    try {
                        // 1. 获取时刻1数据
                        MyFunction.readMemStatus()
                        for (i in 0..coreNumber) {
                            allCpuTime1[i] = MyFunction.readCpuStatus(i)
                        }

                        delay(cmsamplingtime.milliseconds)

                        // 2. 获取时刻2数据
                        for (i in 0..coreNumber) {
                            allCpuTime2[i] = MyFunction.readCpuStatus(i)
                        }

                        // 3. 计算各核心使用率
                        for (i in 0..coreNumber) {
                            val timeDelta = allCpuTime2[i][2] - allCpuTime1[i][2]

                            // 优化：防止除以零崩溃 (系统负载过低或采样间隔太短时可能发生)
                            if (timeDelta > 0) {
                                val rawUsage1 = (allCpuTime2[i][1] - allCpuTime1[i][1]) * 100 / timeDelta
                                val rawUsage2 = 100 - (allCpuTime2[i][0] - allCpuTime1[i][0]) * 100 / timeDelta

                                val usage1 = rawUsage1.coerceIn(0L, 100L)
                                val usage2 = rawUsage2.coerceIn(0L, 100L)

                                // 更新状态列表 (UI 会响应)
                                cpuusage[i][0] = usage1.toInt()
                                cpuusage[i][1] = usage2.toInt()
                            }
                        }

                        // 4. 计算平均负载
                        val avgWork = cpuusage.map { it[0] }.average().toInt()
                        val avgIdle = cpuusage.map { it[1] }.average().toInt()

                        totalcpuusage[0] = avgWork
                        totalcpuusage[1] = avgIdle

                        // 5. 更新通知栏
                        val memG = arrayOf(memstate[1], memstate[0] - memstate[1])[MEMNotiType].toFloat() / 1024 / 1024
                        val displayValues = arrayOf(
                            "%02d%%".format(totalcpuusage[CPUNotiType]),
                            "%.1fG".format(memG)
                        )

                        notificationManager?.notify(1, buildNotification(displayValues))

                    } catch (e: Exception) {
                        LogUtils.e("CPUMEMService", "Calculation Error", e)
                        delay(2000.milliseconds) // 发生错误时稍作等待，防止无限快速循环
                    }
                } else {
                    // 屏幕关闭时进入低频轮询，节省电量
                    delay(3000.milliseconds)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        monitorserviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        LogUtils.d("CPUMEMNotiService", "Stop Service")
        super.onDestroy()
    }

    /**
     * 优化：更名为 buildNotification 明确意图
     * 增加 setOnlyAlertOnce 防止更新时反复响铃或振动
     */
    private fun buildNotification(currentValue: Array<String>): Notification {
        val bitmap = MyFunction.createBitmapFromString(currentValue, 0.6f)

        val builder = NotificationCompat.Builder(this, channelId)
            // 1. 依然保留 SmallIcon，但建议传入一个纯透明或极简的资源 ID
            .setSmallIcon(IconCompat.createWithBitmap(bitmap))

            //.setLargeIcon(bitmap)

            .setContentTitle("System Status")
            .setContentText("CPU: ${currentValue[0]} | MEM: ${currentValue[1]}")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)

        // 3. 针对某些 ROM，通过风格设置强制显示位图
        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())

        return builder.build()
    }
}