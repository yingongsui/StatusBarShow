package com.example.statusbarshow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.IconCompat
import com.example.statusbarshow.LogUtils
import com.example.statusbarshow.MyFunction
import com.example.statusbarshow.cpuusage
import com.example.statusbarshow.samplingtime
import com.example.statusbarshow.totalcpuusage
import com.example.statusbarshow.memstate


class CPUMEMNotiService : Service() {

    private lateinit var screenReceiver: BroadcastReceiver  //用于监听屏幕是否开启

    //通知频道设置
    val channelId = "cpumem_channel"

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {

        val channel = NotificationChannel(channelId, "CPUMEM Channel", NotificationManager.IMPORTANCE_LOW)

        //创建通知频道
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        //静音无震动
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)

        //启用通知
        startForeground(1, updateNotification(arrayOf("--%","--G" )))
        //注册广播接收器
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        prefs.edit { putBoolean("ScreenState", false) }
                        LogUtils.d("BroadcastService", "OFF")
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        prefs.edit { putBoolean("ScreenState", true) }
                        LogUtils.d("BroadcastService", "ON")
                    }
                }
            }
        }

        //监听广播接收器
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON) // 可选：监听屏幕亮起
        }
        registerReceiver(screenReceiver, filter)

    }

    private var monitorThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val corenumber : Int = prefs.getInt("CPUCoreNumber",0)

        if (monitorThread?.isAlive != true) {       //防止跳转到其他界面时反复创建线程
            val allcputime1state : Array<Array<Long>> = Array(corenumber+1) { Array(3) { 0L } }
            val allcputime2state : Array<Array<Long>> = Array(corenumber+1) { Array(3) { 0L } }

            monitorThread = Thread {
                try {
                    while (prefs.getBoolean("CMNoState", false)) {
                        if(prefs.getBoolean("ScreenState", true)) {

                            LogUtils.d("CPUMEMService", "Running")
                            //时刻1数据
                            MyFunction.readMemStatus() //计算内存使用率

                            allcputime1state.indices.forEach { i ->
                                allcputime1state[i] = MyFunction.readCpuStatus(i)
                            }
                            Thread.sleep(samplingtime)
                            //时刻2数据
                            allcputime2state.indices.forEach { i ->
                                allcputime2state[i] = MyFunction.readCpuStatus(i)
                            }

                            for (i in 0..corenumber) {
                                //正规化工作时间计算使用率
                                val cpuusage1 = (allcputime2state[i][1] - allcputime1state[i][1]) * 100 / (allcputime2state[i][2] - allcputime1state[i][2])
                                //空闲时间计算使用率
                                val cpuusage2 = 100 - (allcputime2state[i][0] - allcputime1state[i][0]) * 100 / (allcputime2state[i][2] - allcputime1state[i][2])
                                cpuusage[i] = mutableStateListOf(cpuusage1.toInt(), cpuusage2.toInt())
                            }

//                            totalcpuusage = mutableStateListOf(
//                                cpuusage.map { it[0] }.average().toInt(),
//                                cpuusage.map { it[1] }.average().toInt()
//                            )
                            totalcpuusage[0] = cpuusage.map { it[0] }.average().toInt()
                            totalcpuusage[1] = cpuusage.map { it[1] }.average().toInt()

                            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(
                                1,
                                updateNotification(
                                    arrayOf(
                                        "${totalcpuusage[prefs.getInt("CPUNotiType",0)]}%",
                                        "${"%.1f".format(memstate[prefs.getInt("MEMNotiType",1)].toFloat() / 1024 / 1024)}G"
                                    )
                                )
                            )


                        }
                        else{
                            Thread.sleep(1000)
                        }
                    }
                } catch (_: InterruptedException) {
                    LogUtils.d("CPUMEMNotiService", "Catch Exception >> End Thread")
                }//捕获异常，防止导致服务崩溃。
                stopSelf() //必须加这个，防止服务重启
            }

            monitorThread?.start()
        }
        return START_STICKY

    }

    override fun onDestroy() {
        monitorThread?.interrupt()
        monitorThread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        unregisterReceiver(screenReceiver)
        LogUtils.d("CPUMEMNotiService", "Stop Service")
        super.onDestroy()

    }

    fun updateNotification(currentvalue: Array<String>) : Notification { //定义通知内容
        return NotificationCompat.Builder(this, channelId)
                .setSmallIcon(IconCompat.createWithBitmap(MyFunction.createBitmapFromString(currentvalue,0.6f)))        //创建显示CPU的比特图
                .setContentTitle("USAGE")
                .setContentText("CPU : ${currentvalue[0]} | MEM : ${currentvalue[1]}")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setGroup("SYS_STATE")
                .setSortKey("CPUMEM")
                .setGroupSummary(false)
                .build()

    }
}