package com.example.statusbarshow

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {

    val prefs =  LocalContext.current.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val corenum :Int = prefs.getInt("CPUCoreNumber", 0)
    var columnWidth by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().padding(start=10.dp,end=10.dp,top=20.dp), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).onGloballyPositioned { layoutCoordinates ->
            columnWidth = layoutCoordinates.size.width // 单位是像素 px
        }
        ) {
            Text(
                text = "CPU Usage",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)

            )
            UsageGraph("CPU", 20, 1f, columnWidth/16*9, true, 2, "C")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            ) {
                (0..(corenum/2)).forEach {
                        val name = "  CPU${it}\n" + "%.1f".format(prefs.getLong("CPU${it}MaxFreq", 0).toFloat() / 1000000f) + "GHz"
                        Box(modifier = Modifier.weight(1f)){
                            UsageGraph( name, 10, 1f, columnWidth/(corenum/2+1), false, 1, "C$it")

                        }
                }

            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            ) {
                ((corenum/2)+1..corenum).forEach {
                    val name = "  CPU${it}\n" + "%.1f".format(prefs.getLong("CPU${it}MaxFreq", 0).toFloat() / 1000000f) + "GHz"
                    Box(modifier = Modifier.weight(1f)){
                        UsageGraph(name, 10, 1f, columnWidth/(corenum/2+1), false, 1, "C$it")
                    }
                }

            }
            HorizontalDivider(Modifier, thickness = 3.dp, DividerDefaults.color)
            Text(
                text = "Memory Usage",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)

            )

            UsageGraph("Memory", 20, 1f, columnWidth/16*9, true, 1, "M")
        }

    }
}
