package com.example.statusbarshow.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.statusbarshow.MyFunction

@Preview(showBackground = true)
@Composable
fun InformationScreen() {
    val prefs =  LocalContext.current.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    //*号意味着逐个添加，没有*则是整体添加，会变为数组的数组
    val cpudrawstate = remember {
        mutableStateListOf<Boolean>().apply { repeat(prefs.getInt("CPUCoreNumber",0)+1) {
            add(prefs.getBoolean("CPU${it}DrawState", true))
        } }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                text = "CPU Information",
                fontSize = 25.sp ,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)

            )

            (0 .. prefs.getInt("CPUCoreNumber",0)).forEach {
                term->
                SettingItem(
                    icon = Icons.Default.Memory,
                    title = "CPU$term",
                    subtitle = "%.2f".format(prefs.getLong("CPU${term}MaxFreq",0).toFloat()/1000f/1000f)
                            + "GHz " + MyFunction.getCPUArch(prefs.getString("CPU$term","").toString()),
                    hasSwitch = true,
                    switchState = cpudrawstate[term],
                    onSwitchChange = {
                        cpudrawstate[term] = it     //改变值触发重绘，it是Switch改变后的状态
                        prefs.edit { putBoolean("CPU${term}DrawState", it) }
                                     },
                    )
            }


            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Text(
                text = "Storage Information",
                fontSize = 25.sp ,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)

            )
            SettingItem(
                icon = Icons.Default.Memory,
                title = "Total Memory",
                subtitle = "${MyFunction.getCPUArch(prefs.getString("TotalMemory","").toString()).toLong()/1000} MB",
                onClick = { /* 跳转语言设置 */ }
            )
        }

    }
}




