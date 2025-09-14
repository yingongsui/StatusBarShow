package com.example.statusbarshow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

class MEMNotiService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
    }

    private var noitThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        if (noitThread?.isAlive != true) {       //防止跳转到其他界面时反复创建线程
            noitThread = Thread {
                try {
                    while (prefs.getBoolean("MEMNoState", false)&& prefs.getBoolean("ScreenState", true)) {
                        LogUtils.d("MEMNotiService", "Thread Running")
                        updateNotification(arrayOf("$memusage%", "${"%.1f".format(memstate[1].toFloat() / 1024 / 1024)}G"))
                        Thread.sleep(samplingtime)
                    }
                } catch (_: InterruptedException) {
                    LogUtils.d("MEMNotiService", "Catch Exception >> End Thread")
                }
                LogUtils.d("MEMNotiService", "Exit Thread")
                stopSelf() //必须加这个，防止服务重启
            }//捕获异常，防止导致服务崩溃。

            noitThread?.start()
        }
        return START_NOT_STICKY     //保证服务被销毁后不会重启

    }

    override fun onDestroy() {
//        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
        //当调用 monitorThread?.interrupt() 时，如果线程正处于 sleep() 状态，会抛出 InterruptedException， 需要用trycatch捕获这个异常，否则会导致服务崩溃。
        noitThread?.interrupt()
        noitThread = null
        LogUtils.d("MEMNotiService", "Stop Service")
        //停止通知
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
        super.onDestroy()

    }
    //创建通知频道
    fun updateNotification(currentvalue: Array<String>){

        //创建显示内存的比特图
        val memicon = IconCompat.createWithBitmap(MyFunction.createBitmapFromString(currentvalue,0.6f))

        //通知频道设置
        val channelId = "mem_channel"
        val channelName = "Memory Channel"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelId, channelName, importance)
        //静音无震动
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        //用lazy定义无需初始化的通知变量
        val notification by lazy {
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(memicon)
                .setContentTitle("MEM")
                .setContentText("MEM : ${memstate[1]/1000}MB/${memstate[0]/1000}MB USED")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setGroup("SYS_STATE")
                .setSortKey("MEM")
//                .setDefaults(NotificationCompat.DEFAULT_ALL)  // 声音，振动之类的设定
        }

        //设置小图标
        notification.setSmallIcon(memicon)
        //打开通知
        notificationManager.notify(1, notification.build())
    }
}