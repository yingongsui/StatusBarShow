package com.yin.statusbarshow

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) }
    val coreNum = remember(prefs) { prefs.getInt("CPUCoreNumber", 0) }
    val coreNames = remember(prefs, coreNum) {
        Array(coreNum + 1) { index ->
            val ghz = prefs.getLong("CPU${index}MaxFreq", 0L).toFloat() / 1_000_000f
            " CPU$index\n" + String.format(Locale.US, "%.1fGHz", ghz)
        }
    }
    var columnWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    if (columnWidth != layoutCoordinates.size.width) {
                        columnWidth = layoutCoordinates.size.width
                    }
                },
        ) {
            SectionTitle("CPU Usage")

            val mainGraphHeight = (columnWidth / 16) * 9
            val miniGraphHeight = columnWidth / ((coreNum / 2) + 1)

            UsageGraph("CPU", 20, mainGraphHeight, axisON = true, 2, "C")

            CoreRow(range = 0..(coreNum / 2), coreNames = coreNames, height = miniGraphHeight)
            if (((coreNum / 2) + 1) <= coreNum) {
                CoreRow(range = ((coreNum / 2) + 1)..coreNum, coreNames = coreNames, height = miniGraphHeight)
            }

            HorizontalDivider(Modifier, thickness = 3.dp, DividerDefaults.color)
            SectionTitle("Memory Usage")
            UsageGraph("Memory", 20, mainGraphHeight, axisON = true, 1, "M")

            HorizontalDivider(Modifier, thickness = 3.dp, DividerDefaults.color)
            SectionTitle("Net Speed")
            UsageGraph("Net Speed", 20, mainGraphHeight, axisON = true, 2, "N")
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 25.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    )
}

@Composable
private fun CoreRow(range: IntRange, coreNames: Array<String>, height: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        for (i in range) {
            Box(modifier = Modifier.weight(1f)) {
                UsageGraph(coreNames[i], 10, height, axisON = false, 1, "C$i")
            }
        }
    }
}
