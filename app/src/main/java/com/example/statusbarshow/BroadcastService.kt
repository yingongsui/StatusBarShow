package com.example.statusbarshow

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit

class BroadcastService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
    }

    private lateinit var screenReceiver: BroadcastReceiver

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //注册广播
        LogUtils.d("BroadcastService", "Start Service")
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
                        val memintent = Intent(context, MEMService::class.java)
                        val cpuintent = Intent(context, CPUService::class.java)
                        val memnointent = Intent(context, MEMNotiService::class.java)
                        val cpunointent = Intent(context, CPUNotiService::class.java)
                        val netintent = Intent(context, NetService::class.java)
                        val cmnointent = Intent(context, CPUMEMNotiService::class.java)

                        if (prefs.getBoolean("CPUState", false)) startService(cpuintent) else stopService(cpuintent)
                        if (prefs.getBoolean("CPUNoState", false)) startService(cpunointent) else stopService(cpunointent)
                        if (prefs.getBoolean("MEMState", false)) startService(memintent) else stopService(memintent)
                        if (prefs.getBoolean("MEMNoState", false)) startService(memnointent) else stopService(memnointent)
                        //startForegroundService()只有app在前台时才能使用，后台使用会被拒
                        if (prefs.getBoolean("NETSpState", false)) startService(netintent) else stopService(netintent)
                        if (prefs.getBoolean("CMNoState", false)) startService(cmnointent) else stopService(cmnointent)

                        LogUtils.d("BroadcastService", "ON")
                    }
                }
            }
        }

        //监听广播
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON) // 可选：监听屏幕亮起
        }
        registerReceiver(screenReceiver, filter)

        return START_STICKY

    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        super.onDestroy()
        LogUtils.d("BroadcastService", "Stop Service")
    }


}