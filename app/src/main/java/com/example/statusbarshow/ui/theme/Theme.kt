package com.example.statusbarshow.ui.theme

//import android.os.Build
//import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
//import androidx.compose.material3.dynamicDarkColorScheme
//import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

//private val DarkColorScheme = darkColorScheme(
//    primary = Purple80,
//    secondary = PurpleGrey80,
//    tertiary = Pink80
//)
//
//private val LightColorScheme = lightColorScheme(
//    primary = DodgerBlue,
//    secondary = SteelBlue,
//    tertiary = DeepSkyBlue
//
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */
//)

@Composable
fun StatusBarShowTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyPrefs", android.content.Context.MODE_PRIVATE)

    val themeMode = prefs.getInt("AppThemeMode", 0) // 0 = Dark, 1 = Light
    val isDark = themeMode == 0

    val curveColorIndex = prefs.getInt("AppCurveColor", 0)
    val selectedPrimary = AppCurveColors.getOrElse(curveColorIndex) { DodgerBlue }

    val colorScheme = if (isDark) {
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        ProvideTextStyle(
            value = TextStyle(fontFamily = FontFamily.Monospace),
            content = content,
        )
    }
}