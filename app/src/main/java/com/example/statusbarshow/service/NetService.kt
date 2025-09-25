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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NetService : Service() {

    val channelId = "net_channel"

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



    }
    private val monitorserviceJob = Job()
    private val monitorserviceScope = CoroutineScope(Dispatchers.IO + monitorserviceJob)
    private var monitorJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (monitorJob?.isActive != true) {       //防止跳转到其他界面时反复创建线程
            monitorJob = monitorserviceScope.launch {
                try {
                    while (NETNotiState) {
                        if(screenstate){

                            LogUtils.d("NetService", "Running")

                            val lastRxBytes1 = TrafficStats.getTotalRxBytes()
                            val lastTxBytes1 = TrafficStats.getTotalTxBytes()
                            delay(netsamplingtime)
                            val lastRxBytes2 = TrafficStats.getTotalRxBytes()
                            val lastTxBytes2 = TrafficStats.getTotalTxBytes()
                            netspeedRx.floatValue = (lastRxBytes2-lastRxBytes1)/netsamplingtime*1000f/1024
                            netspeedTx.floatValue = (lastTxBytes2-lastTxBytes1)/netsamplingtime*1000f/1024

                            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            if((netspeedRx.floatValue+netspeedTx.floatValue).toInt() > 1000)
                                notificationManager.notify(2,updateNotification(arrayOf("%.1f".format((netspeedRx.floatValue+netspeedTx.floatValue)/1024),"MB/s")))
                            else
                                notificationManager.notify(2,updateNotification(arrayOf( "${(netspeedRx.floatValue+netspeedTx.floatValue).toInt()}","kB/s")))
                        }
                        else{
                            delay(netsamplingtime)
                        }
                } }catch (_: InterruptedException) {
                        LogUtils.d("NetService", "Catch Exception >> End Thread")
                    }//捕获异常，防止导致服务崩溃。

            }//捕获异常，防止导致服务崩溃。

        }
        return START_STICKY     //保证服务被销毁后重启

    }

    override fun onDestroy() {
        monitorserviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
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