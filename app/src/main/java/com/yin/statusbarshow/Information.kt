package com.yin.statusbarshow

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import java.util.Locale

@Composable
fun InformationScreen() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) }
    val coreCount = remember(prefs) { prefs.getInt("CPUCoreNumber", 0) }
    val cpuSubtitles = remember(prefs, coreCount) {
        Array(coreCount + 1) { term ->
            val ghz = prefs.getLong("CPU${term}MaxFreq", 0L).toFloat() / 1_000_000f
            val arch = MyFunction.getCPUArch(prefs.getString("CPU$term", "").orEmpty())
            String.format(Locale.US, "%.2fGHz %s", ghz, arch)
        }
    }
    val totalMemorySubtitle = remember(prefs) {
        val rawKb = MyFunction.getCPUArch(prefs.getString("TotalMemory", "").orEmpty()).toLongOrNull() ?: 0L
        "${rawKb / 1000L} MB"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "CPU Information",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )

            for (term in 0..coreCount) {
                SettingItem(
                    icon = Icons.Default.Memory,
                    title = "CPU$term",
                    subtitle = cpuSubtitles[term],
                    switchState = CPUDrawState.getOrElse(term) { true },
                    onSwitchChange = { checked ->
                        if (term < CPUDrawState.size) {
                            CPUDrawState[term] = checked
                        }
                        prefs.edit { putBoolean("CPU${term}DrawState", checked) }
                    },
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Text(
                text = "Memory Information",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )

            SettingItem(
                icon = Icons.Default.Memory,
                title = "Total Memory",
                subtitle = totalMemorySubtitle,
            )
        }
    }
}
