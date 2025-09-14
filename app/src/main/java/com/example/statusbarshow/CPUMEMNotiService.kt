package com.example.statusbarshow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

class CPUMEMNotiService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
    override fun onCreate() {
    }

    private var notiThread: Thread? = null
//    private var isForeground = false


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        if (notiThread?.isAlive != true) {       //防止跳转到其他界面时反复创建线程
            notiThread = Thread {
                try {
                    while (prefs.getBoolean("CMNoState", false) && prefs.getBoolean("ScreenState", true)) {
                        updateNotification(arrayOf( "${totalcpuusage[1]}%","${"%.1f".format(memstate[1].toFloat() / 1024 / 1024)}G"))
                        Thread.sleep(samplingtime)
                        LogUtils.d("CPUMEMNotiService", "Thread Running")
                    }
                } catch (_: InterruptedException) {
                    LogUtils.d("CPUMEMNotiService", "Catch Exception >> End Thread")
                }//捕获异常，防止导致服务崩溃。
                stopSelf() //必须加这个，防止服务重启
            }
            notiThread?.start()
        }
        return START_STICKY

    }

    override fun onDestroy() {
        notiThread?.interrupt()
        notiThread = null
//        isForeground = false
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(3)
        LogUtils.d("CPUMEMNotiService", "Stop Service")
        super.onDestroy()

    }

    fun updateNotification(currentvalue: Array<String>){

        //创建显示CPU的比特图
        val cpumemicon = IconCompat.createWithBitmap(MyFunction.createBitmapFromString(currentvalue,0.6f))

        //通知频道设置
        val channelId = "cpumem_channel"
        val channelName = "CPUMEM Channel"
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
                .setSmallIcon(cpumemicon)
                .setContentTitle("USAGE")
                .setContentText("CPU : ${totalcpuusage[1]}% | MEM : ${"%.1f".format((memstate[0]-memstate[1]).toFloat() / 1024 / 1024)}/${"%.1f".format(memstate[0].toFloat() / 1024 / 1024)}GB")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setGroup("SYS_STATE")
                .setSortKey("CPUMEM")
                .setGroupSummary(false)

//                .setDefaults(NotificationCompat.DEFAULT_ALL)  // 声音，振动之类的设定
        }

        //设置小图标
        notification.setSmallIcon(cpumemicon)
        notificationManager.notify(3, notification.build())

        //打开通知
//        if(!isForeground) {
//            startForeground(3, notification.build())
//            isForeground = true
//        } else {
//            notificationManager.notify(3, notification.build())
//        }
    }
}