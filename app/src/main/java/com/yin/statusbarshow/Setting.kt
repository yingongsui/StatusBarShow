package com.yin.statusbarshow

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.yin.statusbarshow.service.CPUMEMNotiService
import com.yin.statusbarshow.service.NetService
import com.yin.statusbarshow.ui.theme.AppCurveColors
import com.yin.statusbarshow.ui.theme.DodgerBlue
import kotlin.math.roundToLong

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) }
    val isDark = appThemeMode.intValue == 0
    val cardBg = if (isDark) Color.Black else Color.White

    val appVersion = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "3.2.5"
    }

    var showDialog by remember { mutableStateOf(value = false) }
    var cpuselected by remember { mutableIntStateOf(CPUNotiType) }
    var memselected by remember { mutableIntStateOf(MEMNotiType) }

    val fwSizeOptions = remember { arrayOf("5%", "7.5%", "10%", "12.5%", "15%") }
    val cpuNotiOptions = remember { arrayOf("Norm-Usage", "Usage") }
    val memNotiOptions = remember { arrayOf("Available", "Used") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Setting",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth(),
            )

            SettingCard(title = "Appearance", cardBg = cardBg) {
                ThemeSwatchRow(
                    title = "Theme",
                    selectedIndex = appThemeMode.intValue,
                ) { mode ->
                    appThemeMode.intValue = mode
                    prefs.edit { putInt("AppThemeMode", mode) }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                ColorPickerRow(
                    title = "Main Color",
                    colors = AppCurveColors,
                    selectedIndex = appCurveColor1.intValue,
                ) { colorIdx ->
                    appCurveColor1.intValue = colorIdx
                    prefs.edit { putInt("AppCurveColor", colorIdx) }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                ColorPickerRow(
                    title = "Sub Color",
                    colors = AppCurveColors,
                    selectedIndex = appCurveColor2.intValue,
                ) { colorIdx ->
                    appCurveColor2.intValue = colorIdx
                    prefs.edit { putInt("AppCurveColor2", colorIdx) }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                OptionSwatchRow(
                    icon = Icons.Default.OpenWith,
                    title = "FW Size",
                    contents = fwSizeOptions,
                    selectedIndex = floatingSizeIndex.intValue,
                ) { sizeIdx ->
                    floatingSizeIndex.intValue = sizeIdx
                    prefs.edit { putInt("FloatingSizeIndex", sizeIdx) }
                }
            }

            SettingCard(title = "Monitor Setting", cardBg = cardBg) {
                MonitorControlGroup(
                    context = context,
                    prefs = prefs,
                    icon = Icons.Default.ImportExport,
                    monitorTitle = "Net Speed Monitor",
                    monitorSubtitle = "Net Speed Monitor and Notification",
                    floatingTitle = "Net Speed Floating Window",
                    serviceClass = NetService::class.java,
                    notiPrefKey = "NETNotiState",
                    floatingPrefKey = "NetFloatingState",
                    samplingPrefKey = "NETSamplingTime",
                    initialNotiState = NETNotiState,
                    initialFloatingState = NetFloatingState,
                    initialSamplingTime = netsamplingtime,
                    onStatesUpdated = { noti, floating ->
                        NETNotiState = noti
                        NetFloatingState = floating
                    },
                    onSamplingUpdated = { netsamplingtime = it },
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                MonitorControlGroup(
                    context = context,
                    prefs = prefs,
                    icon = Icons.Default.DeveloperBoard,
                    monitorTitle = "CPU Memory Monitor",
                    monitorSubtitle = "CPU/Memory Monitor and Notification",
                    floatingTitle = "CPU/MEM Floating Window",
                    serviceClass = CPUMEMNotiService::class.java,
                    notiPrefKey = "CMNotiState",
                    floatingPrefKey = "CMFloatingState",
                    samplingPrefKey = "CMSamplingTime",
                    initialNotiState = CMNotiState,
                    initialFloatingState = CMFloatingState,
                    initialSamplingTime = cmsamplingtime,
                    onStatesUpdated = { noti, floating ->
                        CMNotiState = noti
                        CMFloatingState = floating
                    },
                    onSamplingUpdated = { cmsamplingtime = it },
                )
            }

            SettingCard(title = "Notification Setting", cardBg = cardBg) {
                OptionSwatchRow(
                    icon = Icons.Default.Extension,
                    title = "CPU",
                    contents = cpuNotiOptions,
                    selectedIndex = cpuselected,
                ) { idx ->
                    cpuselected = idx
                    CPUNotiType = idx
                    prefs.edit { putInt("CPUNotiType", idx) }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                OptionSwatchRow(
                    icon = Icons.Default.Extension,
                    title = "MEM",
                    contents = memNotiOptions,
                    selectedIndex = memselected,
                ) { idx ->
                    memselected = idx
                    MEMNotiType = idx
                    prefs.edit { putInt("MEMNotiType", idx) }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clickable { showDialog = true },
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Text(
                    text = "About",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            if (showDialog) {
                AboutDialog(
                    versionName = appVersion,
                    onDismiss = { showDialog = false },
                    onOpenGithub = {
                        val githubUri = "https://github.com/yingongsui/StatusBarShow".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, githubUri))
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    cardBg: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color.Gray),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(16.dp),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        content()
    }
}

@Composable
private fun MonitorControlGroup(
    context: Context,
    prefs: SharedPreferences,
    icon: ImageVector,
    monitorTitle: String,
    monitorSubtitle: String,
    floatingTitle: String,
    serviceClass: Class<out Service>,
    notiPrefKey: String,
    floatingPrefKey: String,
    samplingPrefKey: String,
    initialNotiState: Boolean,
    initialFloatingState: Boolean,
    initialSamplingTime: Long,
    onStatesUpdated: (noti: Boolean, floating: Boolean) -> Unit,
    onSamplingUpdated: (Long) -> Unit,
) {
    var monitorChecked by remember { mutableStateOf(initialNotiState || initialFloatingState) }
    var floatingChecked by remember { mutableStateOf(initialFloatingState) }
    var sliderValue by remember { mutableLongStateOf(initialSamplingTime) }

    SettingItem(
        icon = icon,
        title = monitorTitle,
        subtitle = monitorSubtitle,
        switchScale = 1.2f,
        switchState = monitorChecked,
        onSwitchChange = { enabled ->
            monitorChecked = enabled
            if (enabled) {
                val notiActive = !floatingChecked
                onStatesUpdated(notiActive, floatingChecked)
                prefs.edit { putBoolean(notiPrefKey, notiActive) }
                context.startForegroundService(Intent(context, serviceClass))
                Toast.makeText(context, "Start Service", Toast.LENGTH_SHORT).show()
            } else {
                floatingChecked = false
                onStatesUpdated(false, false)
                prefs.edit {
                    putBoolean(floatingPrefKey, false)
                    putBoolean(notiPrefKey, false)
                }
                context.stopService(Intent(context, serviceClass))
                Toast.makeText(context, "Stop Service", Toast.LENGTH_SHORT).show()
            }
        },
    )

    SettingItem(
        icon = Icons.Default.OpenWith,
        title = floatingTitle,
        subtitle = "Show floating window on screen",
        switchScale = 1.2f,
        switchState = floatingChecked,
        enabled = monitorChecked,
        onSwitchChange = { enabled ->
            if (enabled && !Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "Please grant overlay permission", Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
                context.startActivity(intent)
            } else {
                floatingChecked = enabled
                onStatesUpdated(!enabled, enabled)
                prefs.edit {
                    putBoolean(floatingPrefKey, enabled)
                    putBoolean(notiPrefKey, !enabled)
                }
                context.startForegroundService(Intent(context, serviceClass))
            }
        },
    )

    ValueSliderBar(
        title = "Sampling Rate",
        value = sliderValue.toFloat(),
        onValueChange = { raw ->
            val snapped = raw.roundToLong()
            sliderValue = snapped
            onSamplingUpdated(snapped)
        },
        onValueChangeFinished = {
            prefs.edit { putLong(samplingPrefKey, sliderValue) }
        },
        valueRange = 500f..3000f,
        unit = "ms",
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
    )
}

@Composable
private fun AboutDialog(
    versionName: String,
    onDismiss: () -> Unit,
    onOpenGithub: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "StatusBarShow",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Version v$versionName",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "A lightweight system monitor providing real-time CPU, Memory, and Network speed statistics right in your status bar.",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Text(
                    text = "Quick Tips:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = "• Long press graphs: Toggle history\n• Double tap graphs: Switch metric lines",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Source code: ",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = DodgerBlue,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 14.sp,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ) {
                                append("GitHub")
                            }
                        },
                        modifier = Modifier.clickable(onClick = onOpenGithub),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontFamily = FontFamily.Monospace)
            }
        },
    )
}
