package com.yin.statusbarshow

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yin.statusbarshow.ui.theme.AppCurveColors
import com.yin.statusbarshow.ui.theme.BlueViolet
import com.yin.statusbarshow.ui.theme.DeepSkyBlue
import com.yin.statusbarshow.ui.theme.DodgerBlue
import com.yin.statusbarshow.ui.theme.ForestGreen
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Profile : Screen("profile", "Information", Icons.Default.Info)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = remember { listOf(Screen.Home, Screen.Profile, Screen.Settings) }
    val isSmallWindow = LocalConfiguration.current.screenHeightDp < 400

    Scaffold(
        bottomBar = {
            if (!isSmallWindow) {
                NavigationBar {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val selectedContentColor = MaterialTheme.colorScheme.onSurface
                    val indicatorTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = selectedContentColor,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = selectedContentColor,
                                indicatorColor = indicatorTint,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Profile.route) { InformationScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

@SuppressLint("MutableCollectionMutableState", "ConfigurationScreenWidthHeight")
@Composable
fun UsageGraph(
    name: String,
    pointNum: Int,
    height: Int,
    axisON: Boolean,
    linenum: Int,
    type: String,
) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val prefs = remember(context) { context.getSharedPreferences("MyPrefs", MODE_PRIVATE) }

    val color1 = AppCurveColors.getOrElse(appCurveColor1.intValue) { DodgerBlue }
    val color2 = AppCurveColors.getOrElse(appCurveColor2.intValue) { ForestGreen }
    val paintColor = remember(color1, color2) { arrayOf(color1, color2, BlueViolet) }

    val coreIndex = remember(type) { type.filter { it.isDigit() }.toIntOrNull() }
    var historyON by remember(name) { mutableStateOf(prefs.getBoolean("${name}_HIS_ON", false)) }

    val path = remember(linenum) { Array(linenum) { Path() } }
    val curveStroke = remember { Stroke(width = 5f, cap = StrokeCap.Round) }

    var points by remember(name, linenum, pointNum) {
        mutableStateOf(
            List(linenum) { row ->
                val saved = prefs.getString("${name}HIS$row", null)
                val arr = FloatArray(pointNum + 1)
                if (!saved.isNullOrBlank()) {
                    val parts = saved.split(' ')
                    val limit = minOf(parts.size, arr.size)
                    for (i in 0 until limit) {
                        arr[i] = parts[i].toFloatOrNull() ?: 0f
                    }
                }
                arr
            },
        )
    }

    var linetype by remember { mutableIntStateOf(0) }

    fun addPoint() {
        val updated = List(linenum) { idx ->
            val oldRow = points[idx]
            val newRow = FloatArray(pointNum + 1)
            oldRow.copyInto(newRow, destinationOffset = 0, startIndex = 1, endIndex = pointNum + 1)
            newRow[pointNum] = if (coreIndex != null) {
                if (CPUDrawState.getOrElse(coreIndex) { true }) {
                    cpuusage.getOrNull(coreIndex)?.getOrNull(linetype)?.toFloat() ?: 0f
                } else {
                    0f
                }
            } else {
                when (type) {
                    "C" -> if (idx == 0) totalcpuusage[0].toFloat() else totalcpuusage[1].toFloat()
                    "M" -> memusage.intValue.toFloat()
                    "N" -> if (idx == 0) netspeedRx.floatValue else netspeedTx.floatValue
                    else -> 0f
                }
            }
            newRow
        }
        points = updated

        if (historyON) {
            prefs.edit {
                updated.forEachIndexed { idx, rowArray ->
                    putString("${name}HIS$idx", rowArray.joinToString(" "))
                }
            }
        }
    }

    LaunchedEffect(name, historyON, type) {
        while (true) {
            delay(1000L.milliseconds)
            val isActive = if ((coreIndex != null) || (type == "C") || (type == "M")) {
                CMNotiState || CMFloatingState
            } else {
                NETNotiState || NetFloatingState
            }
            if (screenstate && isActive) {
                addPoint()
            }
        }
    }

    fun toggleHistory() {
        val nextState = !historyON
        historyON = nextState
        if (!nextState) {
            Toast.makeText(context, "History OFF", Toast.LENGTH_SHORT).show()
            val zeroStr = "0 ".repeat(pointNum + 1).trimEnd()
            prefs.edit {
                putBoolean("${name}_HIS_ON", false)
                for (row in 0 until linenum) {
                    putString("${name}HIS$row", zeroStr)
                }
            }
            points = List(linenum) { FloatArray(pointNum + 1) }
        } else {
            Toast.makeText(context, "History ON", Toast.LENGTH_SHORT).show()
            prefs.edit {
                putBoolean("${name}_HIS_ON", true)
                points.forEachIndexed { idx, rowArray ->
                    putString("${name}HIS$idx", rowArray.joinToString(" "))
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { height.toDp() })
            .padding(10.dp)
            .pointerInput(name, linenum, pointNum) {
                detectTapGestures(
                    onLongPress = { toggleHistory() },
                    onDoubleTap = { linetype = if (linetype == 0) 1 else 0 },
                )
            },
    ) {
        if ((size.width <= 0f) || (size.height <= 0f)) return@Canvas

        val originX = if (axisON) size.width / 20f else 5f
        val endY = if (axisON) size.height / 20f else 5f
        val originY = size.height - endY
        val endX = size.width - originX

        val xscale = (endX - originX) / pointNum
        val yscale = (originY - endY) / 10f

        drawLine(color = Color.Gray, start = Offset(originX, originY), end = Offset(endX, originY), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = Color.Gray, start = Offset(originX, endY), end = Offset(endX, endY), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = Color.Gray, start = Offset(originX, originY), end = Offset(originX, endY), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = Color.Gray, start = Offset(endX, originY), end = Offset(endX, endY), strokeWidth = 2f, cap = StrokeCap.Round)

        val isNet = (type == "N")
        val maxVal = if (isNet) {
            var peak = 100f
            for (row in points) {
                for (v in row) {
                    if (v > peak) peak = v
                }
            }
            peak
        } else {
            100f
        }
        val maxY = if (isNet) {
            when {
                maxVal <= 500f -> 500f
                maxVal <= 1024f -> 1024f
                maxVal <= (1024f * 10f) -> 1024f * 10f
                maxVal <= (1024f * 100f) -> 1024f * 100f
                else -> maxVal * 1.2f
            }
        } else {
            100f
        }

        if (!axisON) {
            val dynamicDivisor = if (screenWidthDp >= 600) 20f else 30f
            val labelFontSize = (height / dynamicDivisor).coerceAtLeast(1f)
            drawText(
                textMeasurer,
                name,
                Offset((endX / 10f) * 2.5f, (originY / 10f) * 3f),
                TextStyle(
                    fontSize = labelFontSize.sp,
                    color = Color.Gray.copy(alpha = 0.3f),
                    fontFamily = FontFamily.Monospace,
                ),
            )
        } else {
            val unitText = if (isNet) {
                if (maxY >= 1024f) "(MB/s)" else "(KB/s)"
            } else {
                "(%)"
            }
            drawText(
                textMeasurer,
                unitText,
                Offset(originX, endY - (size.height / 15f)),
                TextStyle(fontSize = 10.sp, color = Color.Gray),
            )
        }

        for (i in 0..pointNum) {
            val x = originX + (i * xscale)
            drawLine(color = Color.Gray, start = Offset(x, originY - 5f), end = Offset(x, originY + 5f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(color = Color.Gray, start = Offset(x, endY - 5f), end = Offset(x, endY + 5f), strokeWidth = 2f, cap = StrokeCap.Round)
        }

        for (i in 0..10) {
            val y = originY - (i * yscale)
            drawLine(color = Color.Gray, start = Offset(originX - 5f, y), end = Offset(originX + 5f, y), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(color = Color.Gray, start = Offset(endX - 5f, y), end = Offset(endX + 5f, y), strokeWidth = 2f, cap = StrokeCap.Round)

            if (((i % 2) == 0) && axisON) {
                val labelText = if (isNet) {
                    val valKb = (maxY / 10f) * i
                    when {
                        maxY == 1024f -> String.format(Locale.US, "%.1f", valKb / 1024f)
                        maxY > 1024f -> String.format(Locale.US, "%.0f", valKb / 1024f)
                        else -> String.format(Locale.US, "%.0f", valKb)
                    }
                } else {
                    (i * 10).toString()
                }
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(0f, y - 15f),
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray),
                )
            }
        }

        val spanY = 10f * yscale
        for (itIdx in 0 until linenum) {
            val currentPath = path[itIdx]
            currentPath.reset()
            val row = points[itIdx]
            val norm0 = (row[0] / maxY).coerceIn(0f, 1f)
            currentPath.moveTo(originX, originY - (norm0 * spanY))
            for (i in 1..pointNum) {
                val normVal = (row[i] / maxY).coerceIn(0f, 1f)
                currentPath.lineTo(
                    originX + (i * xscale),
                    originY - (normVal * spanY),
                )
            }
            drawPath(
                path = currentPath,
                color = paintColor[if (coreIndex != null) linetype else itIdx],
                style = curveStroke,
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    switchScale: Float = 1f,
    switchState: Boolean = false,
    enabled: Boolean = true,
    onSwitchChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 15.sp,
                    color = Color.Gray,
                    overflow = TextOverflow.Clip,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        if (onSwitchChange != null) {
            Switch(
                checked = switchState,
                onCheckedChange = onSwitchChange,
                enabled = enabled,
                modifier = Modifier.scale(switchScale),
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun LabeledSettingRow(
    icon: ImageVector,
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp * 0.15f),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun ColorPickerRow(
    title: String,
    colors: List<Color>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit,
) {
    val activeBorderColor = MaterialTheme.colorScheme.onSurface
    val swatchShape = remember { RoundedCornerShape(6.dp) }

    LabeledSettingRow(icon = Icons.Default.Palette, title = title) {
        colors.forEachIndexed { index, color ->
            val isSelected = (index == selectedIndex)
            Box(
                modifier = Modifier
                    .size(40.dp, 28.dp)
                    .clip(swatchShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 2.dp,
                        color = if (isSelected) activeBorderColor else Color.Gray,
                        shape = swatchShape,
                    )
                    .clickable { onColorSelected(index) },
            )
        }
    }
}

@Composable
fun ThemeSwatchRow(
    title: String,
    selectedIndex: Int,
    onThemeSelected: (Int) -> Unit,
) {
    val themes = remember { listOf("Dark", "Light") }
    val activeBorderColor = MaterialTheme.colorScheme.onSurface
    val itemShape = remember { RoundedCornerShape(8.dp) }

    LabeledSettingRow(icon = Icons.Default.Brightness4, title = title) {
        themes.forEachIndexed { index, name ->
            val isSelected = (index == selectedIndex)
            val innerBg = if (index == 0) Color.Black else Color.White
            val textColor = if (index == 0) Color.White else Color.Black
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .height(36.dp)
                    .clip(itemShape)
                    .background(innerBg)
                    .border(
                        width = if (isSelected) 4.dp else 2.dp,
                        color = if (isSelected) activeBorderColor else Color.Gray,
                        shape = itemShape,
                    )
                    .clickable { onThemeSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
fun OptionSwatchRow(
    icon: ImageVector,
    title: String,
    contents: Array<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
) {
    val mainColor = AppCurveColors.getOrElse(appCurveColor1.intValue) { DodgerBlue }
    val subColor = AppCurveColors.getOrElse(appCurveColor2.intValue) { ForestGreen }
    val optionShape = remember { RoundedCornerShape(6.dp) }

    LabeledSettingRow(icon = icon, title = title) {
        contents.forEachIndexed { index, text ->
            val isSelected = (index == selectedIndex)
            val activeColor = if (title == "CPU") {
                if (index == 0) mainColor else subColor
            } else {
                mainColor
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .height(28.dp)
                    .clip(optionShape)
                    .background(if (isSelected) activeColor else Color.Transparent)
                    .border(
                        width = if (isSelected) 3.dp else 2.dp,
                        color = if (isSelected) activeColor else Color.Gray,
                        shape = optionShape,
                    )
                    .clickable { onOptionSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.Gray,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
fun ValueSliderBar(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 500f..3000f,
    unit: String = "",
) {
    var sliderWidthPx by remember { mutableIntStateOf(0) }
    val pillShape = remember { RoundedCornerShape(10.dp) }

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        sliderWidthPx = coordinates.size.width
                    },
                steps = 4,
            )

            Box(
                modifier = Modifier
                    .offset {
                        val badgeWidthPx = 42.dp.roundToPx()
                        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                            .coerceIn(0f, 1f)
                        val rawOffset = ((sliderWidthPx * fraction) - (badgeWidthPx / 2f)).roundToInt()
                        val maxOffset = (sliderWidthPx - badgeWidthPx).coerceAtLeast(0)
                        IntOffset(rawOffset.coerceIn(0, maxOffset), 0)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${value.roundToInt()}$unit",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = DeepSkyBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Black, shape = pillShape)
                        .border(1.dp, Color.Gray, shape = pillShape)
                        .width(42.dp),
                )
            }
        }
    }
}
