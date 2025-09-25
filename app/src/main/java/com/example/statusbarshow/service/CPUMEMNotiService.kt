package com.example.statusbarshow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CPUMEMNotiService : Service() {

    //通知频道设置
    val channelId = "cpumem_channel"

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {

        val channel = NotificationChannel(channelId, "CPUMEM Channel", NotificationManager.IMPORTANCE_MIN)

        //创建通知频道
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        //静音无震动
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)

        //启用通知
        startForeground(1, updateNotification(arrayOf("--%","--G" )))

    }

    private val monitorserviceJob = Job()
    private val monitorserviceScope = CoroutineScope(Dispatchers.IO + monitorserviceJob)
    private var monitorJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val corenumber : Int = prefs.getInt("CPUCoreNumber",0)

        if (monitorJob?.isActive != true) {       //防止跳转到其他界面时反复创建线程
            val allcputime1state : Array<Array<Long>> = Array(corenumber+1) { Array(3) { 0L } }
            val allcputime2state : Array<Array<Long>> = Array(corenumber+1) { Array(3) { 0L } }

            monitorJob = monitorserviceScope.launch {
                try {
                    while (CMNotiState) {
                        if(screenstate) {

                            LogUtils.d("CPUMEMService", "Running")
                            //时刻1数据
                            MyFunction.readMemStatus() //计算内存使用率

                            allcputime1state.indices.forEach { i ->
                                allcputime1state[i] = MyFunction.readCpuStatus(i)
                            }
                            delay(cmsamplingtime)
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

//                            println(cpuusage.map { it[0] }.joinToString(separator = " | "))
//                            println(totalcpuusage[0])
//
//                            println(cpuusage.map { it[1] }.joinToString(separator = " | "))
//                            println(totalcpuusage[1])



                            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(
                                1,
                                updateNotification(
                                    arrayOf(
                                        "${totalcpuusage[CPUNotiType]}%",
                                        "${"%.1f".format(memstate[MEMNotiType].toFloat() / 1024 / 1024)}G"
                                    )
                                )
                            )


                        }
                        else{
                            delay(cmsamplingtime)
                        }
                    }
                } catch (_: InterruptedException) {
                    LogUtils.d("CPUMEMNotiService", "Catch Exception >> End Thread")
                }//捕获异常，防止导致服务崩溃。
            }

        }
        return START_STICKY

    }

    override fun onDestroy() {
        monitorserviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        LogUtils.d("CPUMEMNotiService", "Stop Service")
        super.onDestroy()

    }

    fun updateNotification(currentvalue: Array<String>) : Notification { //定义通知内容
        return NotificationCompat.Builder(this, channelId)
                .setSmallIcon(IconCompat.createWithBitmap(MyFunction.createBitmapFromString(currentvalue,0.6f)))        //创建显示CPU的比特图
                .setContentTitle("USAGE")
                .setContentText("CPU : ${currentvalue[0]} | MEM : ${currentvalue[1]}")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setAutoCancel(true)
                .setGroup("SYS_STATE")
                .setSortKey("CPUMEM")
                .setGroupSummary(false)
                .build()

    }
}