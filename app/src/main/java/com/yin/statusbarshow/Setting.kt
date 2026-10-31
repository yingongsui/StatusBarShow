package com.yin.statusbarshow

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Extension
//import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.OpenWith
//import com.yin.statusbarshow.NetFloatingState
//import com.yin.statusbarshow.CMFloatingState
//import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.yin.statusbarshow.service.CPUMEMNotiService
import com.yin.statusbarshow.service.NetService
import com.yin.statusbarshow.ui.theme.DodgerBlue
//import com.yin.statusbarshow.ui.theme.ForestGreen
//import com.yin.statusbarshow.ui.theme.BlueViolet
//import com.yin.statusbarshow.ui.theme.SteelBlue
import com.yin.statusbarshow.ui.theme.AppCurveColors
import androidx.core.net.toUri
import androidx.compose.ui.res.stringResource

//import androidx.compose.ui.tooling.preview.Preview

//@Preview(showBackground = true)
@Composable
fun SettingsScreen() {

    val context = LocalContext.current
    val prefs =  context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val isDark = prefs.getInt("AppThemeMode", 0) == 0
    val cardBg = if (isDark) Color.Black else Color.White

    var showDialog by remember { mutableStateOf(value = false) }
    var netspeedchecked by remember { mutableStateOf(NETNotiState || NetFloatingState) }
    var cpumemchecked by remember { mutableStateOf(CMNotiState || CMFloatingState) }
    var netfloatingchecked by remember { mutableStateOf(NetFloatingState) }
    var cpumemfloatingchecked by remember { mutableStateOf(CMFloatingState) }

    var cpuselected by remember { mutableIntStateOf(CPUNotiType) }
    var memselected by remember { mutableIntStateOf(MEMNotiType) }

    var netslidervalue by remember { mutableLongStateOf(netsamplingtime) }
    var cmslidervalue by remember { mutableLongStateOf(cmsamplingtime) }
    var fwSizeIndex by remember { mutableIntStateOf(prefs.getInt("FloatingSizeIndex", 0)) }

    Box(modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 20.dp), contentAlignment = Alignment.TopCenter) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Setting",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth(),
            )
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
                    text = "Appearance",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                ThemeSwatchRow(
                    title = "Theme",
                    selectedIndex = prefs.getInt("AppThemeMode", 0),
                ) {
                    prefs.edit { putInt("AppThemeMode", it) }
                    (context as? android.app.Activity)?.recreate()
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                ColorPickerRow(
                    title = "Main Color",
                    colors = AppCurveColors,
                    selectedIndex = prefs.getInt("AppCurveColor", 0),
                ) {
                    prefs.edit { putInt("AppCurveColor", it) }
                    (context as? android.app.Activity)?.recreate()
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                ColorPickerRow(
                    title = "Sub Color",
                    colors = AppCurveColors,
                    selectedIndex = prefs.getInt("AppCurveColor2", 1),
                ) {
                    prefs.edit { putInt("AppCurveColor2", it) }
                    (context as? android.app.Activity)?.recreate()
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                OptionSwatchRow(
                    icon = Icons.Default.OpenWith,
                    title = "FW Size",
                    contents = arrayOf("5%", "7.5%", "10%", "12.5%", "15%"),
                    selectedIndex = fwSizeIndex,
                ) {
                    fwSizeIndex = it
                    prefs.edit { putInt("FloatingSizeIndex", it) }
                }
            }

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
                    text = "Monitor Setting",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                SettingItem(
                    icon = Icons.Default.ImportExport,
                    title = "Net Speed Monitor",
                    subtitle = "Net Speed Monitor and Notification",
                    hasSwitch = true,
                    switchScale = 1.2f,
                    switchState = netspeedchecked,
                    onSwitchChange = {
                        netspeedchecked = it
                        NETNotiState = it
                        prefs.edit { putBoolean("NETNotiState", it) }
                        if (it) {
                            if (!netfloatingchecked) {
                                NETNotiState = true
                                prefs.edit { putBoolean("NETNotiState", true) }
                            }
                            val intent = Intent(context, NetService::class.java)
                            context.startForegroundService(intent)
                            Toast.makeText(context, "Start Service", Toast.LENGTH_SHORT).show()
                        } else {
                            netfloatingchecked = false
                            NetFloatingState = false
                            prefs.edit { putBoolean("NetFloatingState", false) }
                            NETNotiState = false
                            prefs.edit { putBoolean("NETNotiState", false) }
                            val intent = Intent(context, NetService::class.java)
                            context.stopService(intent)
                            Toast.makeText(context, "Stop Service", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
                SettingItem(
                    icon = Icons.Default.OpenWith,
                    title = "Net Speed Floating Window",
                    subtitle = "Show floating window on screen",
                    hasSwitch = true,
                    switchScale = 1.2f,
                    switchState = netfloatingchecked,
                    enabled = netspeedchecked,
                    onSwitchChange = {
                        if (it && !android.provider.Settings.canDrawOverlays(context)) {
                            Toast.makeText(context, "Please grant overlay permission", Toast.LENGTH_SHORT).show()
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri(),
                            )
                            context.startActivity(intent)
                        } else {
                            netfloatingchecked = it
                            NetFloatingState = it
                            prefs.edit { putBoolean("NetFloatingState", it) }
                            if (it) {
                                NETNotiState = false
                                prefs.edit { putBoolean("NETNotiState", false) }
                            } else {
                                NETNotiState = true
                                prefs.edit { putBoolean("NETNotiState", true) }
                            }
                            val intent = Intent(context, NetService::class.java)
                            context.startForegroundService(intent)
                        }
                    },
                )
                ValueSliderBar(
                    title = "Sampling Rate",
                    value = netslidervalue.toFloat(),
                    onValueChange = {
                        netslidervalue = it.toLong()
                        netsamplingtime = netslidervalue
                        prefs.edit { putLong("NETSamplingTime", netslidervalue) }
                    },
                    valueRange = 500f..3000f,
                    unit = "ms",
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 10.dp,
                        bottom = 10.dp,
                    ),
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                SettingItem(
                    icon = Icons.Default.DeveloperBoard,
                    title = "CPU Memory Monitor",
                    subtitle = "CPU/Memory Monitor and Notification",
                    hasSwitch = true,
                    switchScale = 1.2f,
                    switchState = cpumemchecked,
                    onSwitchChange = {
                        cpumemchecked = it
                        CMNotiState = it
                        prefs.edit { putBoolean("CMNotiState", it) }
                        if (it) {
                            if (!cpumemfloatingchecked) {
                                CMNotiState = true
                                prefs.edit { putBoolean("CMNotiState", true) }
                            }
                            val intent = Intent(context, CPUMEMNotiService::class.java)
                            context.startForegroundService(intent)
                            Toast.makeText(context, "Start Service", Toast.LENGTH_SHORT).show()
                        } else {
                            cpumemfloatingchecked = false
                            CMFloatingState = false
                            prefs.edit { putBoolean("CMFloatingState", false) }
                            CMNotiState = false
                            prefs.edit { putBoolean("CMNotiState", false) }
                            val intent = Intent(context, CPUMEMNotiService::class.java)
                            context.stopService(intent)
                            Toast.makeText(context, "Stop Service", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
                SettingItem(
                    icon = Icons.Default.OpenWith,
                    title = "CPU/MEM Floating Window",
                    subtitle = "Show floating window on screen",
                    hasSwitch = true,
                    switchScale = 1.2f,
                    switchState = cpumemfloatingchecked,
                    enabled = cpumemchecked,
                    onSwitchChange = {
                        if (it && !android.provider.Settings.canDrawOverlays(context)) {
                            Toast.makeText(context, "Please grant overlay permission", Toast.LENGTH_SHORT).show()
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri(),
                            )
                            context.startActivity(intent)
                        } else {
                            cpumemfloatingchecked = it
                            CMFloatingState = it
                            prefs.edit { putBoolean("CMFloatingState", it) }
                            if (it) {
                                CMNotiState = false
                                prefs.edit { putBoolean("CMNotiState", false) }
                            } else {
                                CMNotiState = true
                                prefs.edit { putBoolean("CMNotiState", true) }
                            }
                            val intent = Intent(context, CPUMEMNotiService::class.java)
                            context.startForegroundService(intent)
                        }
                    },
                )
                ValueSliderBar(
                    title = "Sampling Rate",
                    value = cmslidervalue.toFloat(),
                    onValueChange = {
                        cmslidervalue = it.toLong()
                        cmsamplingtime = cmslidervalue
                        prefs.edit { putLong("CMSamplingTime", cmslidervalue) }
                    },
                    valueRange = 500f..3000f,
                    unit = "ms",
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 10.dp,
                        bottom = 10.dp,
                    ),
                )
            }
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
                    text = "Notification Setting",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                OptionSwatchRow(
                    icon = Icons.Default.Extension,
                    title = "CPU",
                    contents = arrayOf("Norm-Usage", "Usage"),
                    selectedIndex = cpuselected,
                ) {
                    cpuselected = it
                    prefs.edit { putInt("CPUNotiType", it) }
                    CPUNotiType = it
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                OptionSwatchRow(
                    icon = Icons.Default.Extension,
                    title = "MEM",
                    contents = arrayOf("Available", "Used"),
                    selectedIndex = memselected,
                ) {
                    memselected = it
                    prefs.edit { putInt("MEMNotiType", it) }
                    MEMNotiType = it
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)


            }


            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clickable{showDialog = true},
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
                AlertDialog(
                    onDismissRequest = { showDialog = false },
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
                                text = "Version " + stringResource(R.string.appversion),
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
                                    modifier = Modifier.clickable {
                                        "https://github.com/yingongsui/StatusBarShow".let {
                                            val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                                            context.startActivity(intent)
                                        }
                                    },
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Close", fontFamily = FontFamily.Monospace)
                        }
                    },
                )
            }

        }

    }
}
