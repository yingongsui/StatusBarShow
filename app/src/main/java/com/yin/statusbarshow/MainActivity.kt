package com.yin.statusbarshow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.yin.statusbarshow.service.CPUMEMNotiService
import com.yin.statusbarshow.service.NetService
import com.yin.statusbarshow.ui.theme.StatusBarShowTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppState.ensureInitialized(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark = appThemeMode.intValue == 0
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }

            StatusBarShowTheme {
                MainScreen()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        syncMonitorServices()
    }

    private fun syncMonitorServices() {
        val netIntent = Intent(this, NetService::class.java)
        if (NETNotiState || NetFloatingState) {
            startForegroundService(netIntent)
        } else {
            stopService(netIntent)
        }

        val cmIntent = Intent(this, CPUMEMNotiService::class.java)
        if (CMNotiState || CMFloatingState) {
            startForegroundService(cmIntent)
        } else {
            stopService(cmIntent)
        }
    }
}
