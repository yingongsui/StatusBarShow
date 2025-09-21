package com.example.statusbarshow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.statusbarshow.service.CPUMEMNotiService
import com.example.statusbarshow.service.NetService
import com.example.statusbarshow.ui.MainScreen
import com.example.statusbarshow.ui.theme.DodgerBlue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = DodgerBlue,
                    background = Color.Black,
                    surface = Color.Black,
                    onPrimary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                MainScreen()
            }

        }

        //获取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001 // 请求码，可自定义
                )
            }
        }

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        if( !prefs.getBoolean("PrefState",false)) MyFunction.getSysInfo(this) //传入 Activity 的 Context，实行该函数，获取核心信息

        repeat(prefs.getInt("CPUCoreNumber",0)+1) {cpuusage.add(mutableStateListOf(0,0))  }  //确定cpu使用率应该使用的数组个数

        //        启用服务
        val netintent = Intent(this, NetService::class.java)
        val cmnointent = Intent(this, CPUMEMNotiService::class.java)


        //如果前台服务，必须6秒内调用startForeground来启用通知；前台服务无法在后台启动，会导致广播失效，系统资源浪费
        if (prefs.getBoolean("NETSpState", false)) this.startForegroundService(netintent) else this.stopService(netintent)
        if (prefs.getBoolean("CMNoState", false)) this.startForegroundService(cmnointent) else this.stopService(cmnointent)
        }
    }



