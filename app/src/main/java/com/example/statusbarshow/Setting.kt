package com.example.statusbarshow

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.ImportExport
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.statusbarshow.service.CPUMEMNotiService
import com.example.statusbarshow.service.NetService
import com.example.statusbarshow.ui.theme.DodgerBlue
import androidx.core.net.toUri

@Preview(showBackground = true)
@Composable
fun SettingsScreen() {

    val context = LocalContext.current
    val prefs =  context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    Box(modifier = Modifier.fillMaxSize().padding(start=10.dp,end=10.dp,top=20.dp), contentAlignment = Alignment.TopCenter) {
        var showDialog by remember { mutableStateOf(false) }
        var netspeedchecked by remember { mutableStateOf(prefs.getBoolean("NETSpState", false)) }
        var cpumemchecked by remember { mutableStateOf(prefs.getBoolean("CMNoState", false)) }
        var cpuselected by remember { mutableIntStateOf(prefs.getInt("CPUNotiType",0)) }
        var memselected by remember { mutableIntStateOf(prefs.getInt("MEMNotiType",1)) }

        var slidervalue by remember { mutableLongStateOf(samplingtime) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(top=10.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Setting",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()

            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = "Monitor Setting",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 30.sp ,
                    fontWeight = FontWeight.Bold
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
                        prefs.edit { putBoolean("NETSpState", it) }   //记录控件状态到实体文件
                        val intent = Intent(context, NetService::class.java)
                        if (it) context.startService(intent) else context.stopService(intent)
                    },
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
                        prefs.edit { putBoolean("CMNoState", it) }   //记录控件状态到实体文件
                        val intent = Intent(context, CPUMEMNotiService::class.java)
                        if (it) context.startService(intent) else context.stopService(intent)
                    },
                )


            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = "Notification Setting",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 30.sp ,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                //核心在onOptionSelected = {cpuselected =it}，it就是组件中传入的参数：就是onClick = { onOptionSelected(it) }中的it
                TypeRatioButton(
                    icon = Icons.Default.Extension,
                    title = "CPU",
                    contents = arrayOf("Normalized Usage", "Usage"),
                    selectedstate = cpuselected,
                    onOptionSelected = {
                        cpuselected = it
                        prefs.edit { putInt("CPUNotiType", it) }
                    })
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                TypeRatioButton(
                    icon = Icons.Default.Extension,
                    title = "MEM",
                    contents = arrayOf("Used", "Available"),
                    selectedstate = memselected,
                    onOptionSelected = {
                        memselected = it
                        prefs.edit { putInt("MEMNotiType", it) }
                    })
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                ValueSliderBar(
                    title = "Sampling Rate(ms)",
                    value = slidervalue.toFloat(),
                    onValueChange = {
                        slidervalue = it.toLong()
                        samplingtime = it.toLong()
                    },
                    valueRange = 500f..3000f,
                    unit = "ms",
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp,
                        bottom = 10.dp
                    )
                )



            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clickable{showDialog = true},
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = "About",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 30.sp ,
                    fontWeight = FontWeight.Bold
                )

            }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("StatusBarShow") },
                    text = {
                        Column {
                            Text(//设置文本格式
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.White,fontWeight = FontWeight.Bold,fontSize = 17.sp)) {
                                        append("Version:" + context.getString(R.string.appversion) )
                                    }
                                }
                            )
                            Row(modifier = Modifier.padding(top=8.dp)){Text("View source code on ")
                                Text(//设置文本格式
                                    text = buildAnnotatedString { withStyle(style = SpanStyle(color = DodgerBlue, fontStyle = FontStyle.Italic, fontSize = 15.sp, textDecoration = TextDecoration.Underline))
                                    { append("github") } },
                                    modifier = Modifier.clickable {
                                        "https://github.com/yingongsui/StatusBarShow".let {
                                            val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                                            context.startActivity(intent)
                                        }

                                    }
                                )
                                Text(".")
                            }


                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

        }

    }
}
