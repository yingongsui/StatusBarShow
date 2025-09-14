package com.example.statusbarshow

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.TrafficStats
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

class NetService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
    override fun onCreate() {
    }
    private var monitorThread: Thread? = null
//    private var isForeground = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        if (monitorThread?.isAlive != true) {       //防止跳转到其他界面时反复创建线程
            monitorThread = Thread {
                try {
                    while (prefs.getBoolean("NETSpState", false) && prefs.getBoolean("ScreenState", true)) {
                        val lastRxBytes1 = TrafficStats.getTotalRxBytes()
                        val lastTxBytes1 = TrafficStats.getTotalTxBytes()
                        Thread.sleep(samplingtime)
                        val lastRxBytes2 = TrafficStats.getTotalRxBytes()
                        val lastTxBytes2 = TrafficStats.getTotalTxBytes()
                        netspeedRx = (lastRxBytes2-lastRxBytes1)/samplingtime*1000f/1024
                        netspeedTx = (lastTxBytes2-lastTxBytes1)/samplingtime*1000f/1024
                        if((netspeedRx+netspeedTx).toInt() > 1000)
                            updateNotification(arrayOf("%.1f".format((netspeedRx+netspeedTx)/1024),"MB/s"))
                        else
                            updateNotification(arrayOf( "${(netspeedRx+netspeedTx).toInt()}","kB/s"))
                        LogUtils.d("NetService", "Running")
                } }catch (_: InterruptedException) {
                        LogUtils.d("NetService", "Catch Exception >> End Thread")
                    }//捕获异常，防止导致服务崩溃。
                    stopSelf() //必须加这个，防止服务重启

            }//捕获异常，防止导致服务崩溃。

            monitorThread?.start()
        }
        return START_STICKY     //保证服务被销毁后不会重启

    }

    override fun onDestroy() {
//        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
        //当调用 monitorThread?.interrupt() 时，如果线程正处于 sleep() 状态，会抛出 InterruptedException， 需要用try catch捕获这个异常，否则会导致服务崩溃。
        monitorThread?.interrupt()
        monitorThread = null
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(2)
        LogUtils.d("NetService", "Stop Service")
//        isForeground = false
        super.onDestroy()
    }

    @SuppressLint("ForegroundServiceType")      //保证前台服务权限已启动，以实现startForeground
    fun updateNotification(currentvalue: Array<String>){

        //创建显示CPU的比特图
        val neticon = IconCompat.createWithBitmap(MyFunction.createBitmapFromString(currentvalue,0.6f))

        //通知频道设置
        val channelId = "net_channel"
        val channelName = "NET Channel"
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
                .setSmallIcon(neticon)
                .setContentTitle("NETWORK")
                .setContentText("NETWORK SPEED : ${(netspeedRx + netspeedTx).toInt()}kB/s")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setGroup("SYS_STATE")
                .setSortKey("NET")

            //.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))

//                .setDefaults(NotificationCompat.DEFAULT_ALL)  // 声音，振动之类的设定
        }

        //设置小图标
        notification.setSmallIcon(neticon)
        notificationManager.notify(2, notification.build())
//        打开通知为前台通知
//        if(!isForeground) {
//            startForeground(2, notification.build())
//            isForeground = true
//        } else {
//            notificationManager.notify(2, notification.build())
//        }

    }
}