package com.example.statusbarshow

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CPUService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        LogUtils.d("CPUService", "Created")
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
                    while (prefs.getBoolean("CPUState", false) && prefs.getBoolean("ScreenState", true)) {
                        LogUtils.d("CPUService", "Thread Running")
                        //时刻1数据
                        allcputime1state.indices.forEach { i -> allcputime1state[i] = MyFunction.readCpuStatus(i) }
                        Thread.sleep(samplingtime)
                        //时刻2数据
                        allcputime2state.indices.forEach { i -> allcputime2state[i] = MyFunction.readCpuStatus(i) }

                        for(i in 0..corenumber){
                            //空闲时间计算使用率
                            val cpuusage1= 100-(allcputime2state[i][0]-allcputime1state[i][0])*100/(allcputime2state[i][2]-allcputime1state[i][2])
                            //正规化工作时间计算使用率
                            val cpuusage2= (allcputime2state[i][1]-allcputime1state[i][1])*100/(allcputime2state[i][2]-allcputime1state[i][2])
                            cpuusage[i] = mutableListOf(cpuusage1.toInt(),cpuusage2.toInt())
                        }
//                        Thread.sleep(1000)
                        totalcpuusage = arrayOf( cpuusage.map { it[0] }.average().toInt(), cpuusage.map{ it[1] }.average().toInt())
                    }
                } catch (_: InterruptedException) {
                    LogUtils.d("CPUService", "Catch Exception >> End Thread")
                }//捕获异常，防止导致服务崩溃。
                stopSelf() //必须加这个，防止服务重启
            }
            monitorThread?.start()
        }
        return START_NOT_STICKY

    }

    override fun onDestroy() {
//        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
        //当调用 monitorThread?.interrupt() 时，如果线程正处于 sleep() 状态，会抛出 InterruptedException， 需要用trycatch捕获这个异常，否则会导致服务崩溃。
        monitorThread?.interrupt()
        monitorThread = null
        LogUtils.d("CPUService", "Stop Service")
        super.onDestroy()
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.cancel(1)
    }

}