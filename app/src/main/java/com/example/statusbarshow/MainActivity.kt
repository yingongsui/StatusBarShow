package com.example.statusbarshow


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.statusbarshow.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
//    private lateinit var cpuintent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //获取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001 // 请求码，可自定义
                )
            }
        }

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        if( !prefs.getBoolean("PrefState",false)) MyFunction.getSysInfo(this) //传入 Activity 的 Context，实行该函数，获取核心信息

        repeat(prefs.getInt("CPUCoreNumber",0)+1) {cpuusage.add(mutableListOf(0,0))  }  //确定cpu使用率应该使用的数组个数

//        启用服务
        val memintent = Intent(this, MEMService::class.java)
        val cpuintent = Intent(this, CPUService::class.java)
        val memnointent = Intent(this, MEMNotiService::class.java)
        val cpunointent = Intent(this, CPUNotiService::class.java)
        val netintent = Intent(this, NetService::class.java)
        val cmnointent = Intent(this, CPUMEMNotiService::class.java)

        if (prefs.getBoolean("CPUState", false)) this.startService(cpuintent) else this.stopService(cpuintent)
        if (prefs.getBoolean("CPUNoState", false)) this.startService(cpunointent) else this.stopService(cpunointent)
        if (prefs.getBoolean("MEMState", false)) this.startService(memintent) else this.stopService(memintent)
        if (prefs.getBoolean("MEMNoState", false)) this.startService(memnointent) else this.stopService(memnointent)

        //如果前台服务，必须6秒内调用startForeground来启用通知；前台服务无法在后台启动，会导致广播失效，系统资源浪费
        if (prefs.getBoolean("NETSpState", false)) this.startService(netintent) else this.stopService(netintent)
        if (prefs.getBoolean("CMNoState", false)) this.startService(cmnointent) else this.stopService(cmnointent)

        this.startService(Intent(this, BroadcastService::class.java))

    }

    //监听是否进入后台
    override fun onResume() {
        super.onResume()
        isInForeground = true
        LogUtils.d("MainActivity", "Go Foreground")

    }

    override fun onStop() {
        super.onStop()
        isInForeground = false
        LogUtils.d("MainActivity", "Go Background")
    }


}