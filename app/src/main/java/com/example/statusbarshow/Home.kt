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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {

    val prefs =  LocalContext.current.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val corenum :Int = prefs.getInt("CPUCoreNumber", 0)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(
                text = "CPU USAGE",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)

            )
            UsageGraph("CPU", 20, 1f, 300, true, 2, "C")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            ) {
                (0..(corenum/2)).forEach {
                        Box(modifier = Modifier.weight(1f)){
                            UsageGraph("CPU${it}", 10, 1f, 100, false, 1, "C$it")
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
                    Box(modifier = Modifier.weight(1f)){
                        UsageGraph("CPU${it}", 10, 1f, 100, false, 1, "C$it")
                    }
                }

            }
            HorizontalDivider(Modifier, thickness = 3.dp, DividerDefaults.color)
            Text(
                text = "Memory USAGE",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)

            )

            UsageGraph("Memory", 20, 1f, 300, true, 1, "M")
        }

    }
}
