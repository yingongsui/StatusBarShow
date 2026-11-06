package com.yin.statusbarshow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.yin.statusbarshow.appCurveColor1
import com.yin.statusbarshow.appThemeMode

private val MonospaceTextStyle = TextStyle(fontFamily = FontFamily.Monospace)

@Composable
fun StatusBarShowTheme(
    content: @Composable () -> Unit,
) {
    val isDark = appThemeMode.intValue == 0
    val selectedPrimary = AppCurveColors.getOrElse(appCurveColor1.intValue) { DodgerBlue }

    val colorScheme = remember(isDark, selectedPrimary) {
        if (isDark) {
            darkColorScheme(
                primary = selectedPrimary,
                background = Color.Black,
                surface = Color.Black,
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White,
            )
        } else {
            lightColorScheme(
                primary = selectedPrimary,
                background = Color.White,
                surface = Color.White,
                onPrimary = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        ProvideTextStyle(
            value = MonospaceTextStyle,
            content = content,
        )
    }
}
