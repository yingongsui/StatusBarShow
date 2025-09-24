package com.example.statusbarshow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.IconCompat
import com.example.statusbarshow.LogUtils
import com.example.statusbarshow.MyFunction
import com.example.statusbarshow.netspeedRx
import com.example.statusbarshow.netspeedTx
import com.example.statusbarshow.netsamplingtime

class NetService : Service() {

    val channelId = "net_channel"
    private lateinit var screenReceiver: BroadcastReceiver  //用于监听屏幕是否开启

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
    override fun onCreate() {
        //通知频道设置
        val channel = NotificationChannel(channelId, "NET Channel", NotificationManager.IMPORTANCE_LOW)
        //静音无震动
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)

        //创建通知频道
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        //启用通知
        startForeground(2, updateNotification(arrayOf("0","kB/s" )))
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
//    private var isForeground = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        if (monitorThread?.isAlive != true) {       //防止跳转到其他界面时反复创建线程
            monitorThread = Thread {
                try {
                    while (prefs.getBoolean("NETSpState", false) ) {
                        if(prefs.getBoolean("ScreenState", true)){

                            val lastRxBytes1 = TrafficStats.getTotalRxBytes()
                            val lastTxBytes1 = TrafficStats.getTotalTxBytes()
                            Thread.sleep(netsamplingtime)
                            val lastRxBytes2 = TrafficStats.getTotalRxBytes()
                            val lastTxBytes2 = TrafficStats.getTotalTxBytes()
                            netspeedRx.value = (lastRxBytes2-lastRxBytes1)/netsamplingtime*1000f/1024
                            netspeedTx.value = (lastTxBytes2-lastTxBytes1)/netsamplingtime*1000f/1024
                            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                            if((netspeedRx.value+netspeedTx.value).toInt() > 1000)
                                notificationManager.notify(2,updateNotification(arrayOf("%.1f".format((netspeedRx.value+netspeedTx.value)/1024),"MB/s")))
                            else
                                notificationManager.notify(2,updateNotification(arrayOf( "${(netspeedRx.value+netspeedTx.value).toInt()}","kB/s")))
                            LogUtils.d("NetService", "Running")
                        }
                        else{
                            Thread.sleep(1000)
                        }
                } }catch (_: InterruptedException) {
                        LogUtils.d("NetService", "Catch Exception >> End Thread")
                    }//捕获异常，防止导致服务崩溃。
                    stopSelf() //必须加这个，防止服务重启

            }//捕获异常，防止导致服务崩溃。

            monitorThread?.start()
        }
        return START_STICKY     //保证服务被销毁后重启

    }

    override fun onDestroy() {
//        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
        //当调用 monitorThread?.interrupt() 时，如果线程正处于 sleep() 状态，会抛出 InterruptedException， 需要用try catch捕获这个异常，否则会导致服务崩溃。
        monitorThread?.interrupt()
        monitorThread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        unregisterReceiver(screenReceiver)
        LogUtils.d("NetService", "Stop Service")
        super.onDestroy()
    }

    fun updateNotification(currentvalue: Array<String>): Notification {

        return  NotificationCompat.Builder(this, channelId)
                .setSmallIcon(IconCompat.createWithBitmap(MyFunction.createBitmapFromString(currentvalue,0.6f)))
                .setContentTitle("NETWORK")
                .setContentText("NETWORK SPEED : ${currentvalue[0]} ${currentvalue[1]}")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setGroup("NET_STATE")
                .setSortKey("NET")
                .build()



    }
}