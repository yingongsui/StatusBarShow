package com.example.statusbarshow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

class CPUNotiService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
    }

    private var notiThread: Thread? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        if (notiThread?.isAlive != true) {       //防止跳转到其他界面时反复创建线程
            notiThread = Thread {
                try {
                    while (prefs.getBoolean("CPUNoState", false)&& prefs.getBoolean("ScreenState", true)) {
                        updateNotification(arrayOf( "${totalcpuusage[0]}%","${totalcpuusage[1]}%"))
                        Thread.sleep(samplingtime)
                        LogUtils.d("CPUNotiService", "Thread Running")
                    }
                } catch (_: InterruptedException) {
                    LogUtils.d("CPUNotiService", "Catch Exception >> End Thread")
                }//捕获异常，防止导致服务崩溃。
                stopSelf() //必须加这个，防止服务重启
            }
            notiThread?.start()
        }
        return START_NOT_STICKY

    }

    override fun onDestroy() {
        notiThread?.interrupt()
        notiThread = null
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(0)
        LogUtils.d("CPUNotiService", "Stop Service")
        super.onDestroy()

    }

    fun updateNotification(currentvalue: Array<String>){

        //创建显示CPU的比特图
        val cpuicon = IconCompat.createWithBitmap(MyFunction.createBitmapFromString(currentvalue,0.5f))

        //通知频道设置
        val channelId = "cpu_channel"
        val channelName = "CPU Channel"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelId, channelName, importance)
        //静音无震动
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)

        //创建通知频道
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        //用lazy定义无需初始化的通知变量
        val notification by lazy {
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(cpuicon)
                .setContentTitle("CPU")
                .setContentText("CPU : ${totalcpuusage[0]}%")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setGroup("SYS_STATE")
                .setSortKey("CPU")
//                .setDefaults(NotificationCompat.DEFAULT_ALL)  // 声音，振动之类的设定
        }

        //设置小图标
        notification.setSmallIcon(cpuicon)
        //打开通知
        notificationManager.notify(0, notification.build())

    }
}