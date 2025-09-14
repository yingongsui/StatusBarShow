package com.example.statusbarshow

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.NotificationManager
import android.util.Log

class MEMService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
    }

    private var monitorThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        if (monitorThread?.isAlive != true) {       //防止跳转到其他界面时反复创建线程
            monitorThread = Thread {
                try {
                    while (prefs.getBoolean("MEMState", false)&& prefs.getBoolean("ScreenState", true)) {
                        LogUtils.d("MEMService", "Thread Running")
                        Thread.sleep(samplingtime)
                        MyFunction.readMemStatus()
                    }
                } catch (_: InterruptedException) {
                    LogUtils.d("MEMService", "Catch Exception >> End Thread")
                }
                LogUtils.d("MEMService", "Exit Thread")
                stopSelf() //必须加这个，防止服务重启
            }//捕获异常，防止导致服务崩溃。

            monitorThread?.start()
        }
        return START_NOT_STICKY     //保证服务被销毁后不会重启

    }

    override fun onDestroy() {
//        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
        //当调用 monitorThread?.interrupt() 时，如果线程正处于 sleep() 状态，会抛出 InterruptedException， 需要用try catch捕获这个异常，否则会导致服务崩溃。
        monitorThread?.interrupt()
        monitorThread = null
        LogUtils.d("MEMService", "Stop Service")
        super.onDestroy()
        //停止通知
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(0)
    }
}