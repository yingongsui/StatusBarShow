package com.example.statusbarshow


import android.annotation.SuppressLint
//import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import com.example.statusbarshow.ui.theme.AppCurveColors
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.statusbarshow.ui.theme.BlueViolet
import com.example.statusbarshow.ui.theme.DeepSkyBlue
import com.example.statusbarshow.ui.theme.DodgerBlue
import com.example.statusbarshow.ui.theme.ForestGreen
import kotlin.time.Duration.Companion.milliseconds

//import kotlin.time.Duration.Companion.milliseconds


sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Profile : Screen("profile", "Information", Icons.Default.Info)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Profile, Screen.Settings)
    val isSmallWindow = LocalConfiguration.current.screenHeightDp < 400 //用于小窗时隐藏导航栏



    Scaffold(
        bottomBar = {
            if(!isSmallWindow) {
                NavigationBar {
                    val currentRoute =
                        navController.currentBackStackEntryAsState().value?.destination?.route
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = Color.White,
                                indicatorColor = DodgerBlue.copy(alpha = 0.3f)
                            )

                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Profile.route) { InformationScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}


@SuppressLint("RestrictedApi", "MutableCollectionMutableState", "ConfigurationScreenWidthHeight")
@Composable
fun UsageGraph(name: String, pointNum: Int, width: Float, height: Int, axisON: Boolean, linenum: Int, type: String) {
    val textMeasurer = rememberTextMeasurer()
    var historyON by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
    val colorIndex1 = prefs.getInt("AppCurveColor", 0)
    val colorIndex2 = prefs.getInt("AppCurveColor2", 1)
    val color1 = AppCurveColors.getOrElse(colorIndex1) { DodgerBlue }
    val color2 = AppCurveColors.getOrElse(colorIndex2) { ForestGreen }
    val paintColor: Array<Color> = arrayOf(color1, color2, BlueViolet)
    
    // 使用 remember 缓存 Path 对象，避免每次重绘重复创建
    val path = remember(linenum) { Array(linenum) { Path() } }

    // 使用 remember 缓存点数据列表，避免每次重绘重新读取 SP
    var points by remember(name, linenum, pointNum) {
        mutableStateOf(
            MutableList(linenum) { row ->
                prefs.getString(name + "HIS$row", "0 ".repeat(pointNum + 1).trimEnd())!!
                    .split(" ")
                    .map { it.toFloat() }
                    .toFloatArray()
            }
        )
    }

    var linetype by remember { mutableIntStateOf(0) }

    fun addPoint() {
        val number = type.filter { it.isDigit() }
        val newValues = FloatArray(linenum) { idx ->
            if (number.isNotEmpty() && CPUDrawState.getOrElse(number.toInt()) { true }) {
                cpuusage.getOrNull(number.toInt())?.getOrNull(linetype)?.toFloat() ?: 0f
            } else {
                when (type) {
                    "C" -> if (idx == 0) totalcpuusage[0].toFloat() else totalcpuusage[1].toFloat()
                    "M" -> memusage.intValue.toFloat()
                    "N" -> if (idx == 0) netspeedRx.floatValue else netspeedTx.floatValue
                    else -> 0f
                }
            }
        }

        points = points.map { rowArray ->
            val newRow = rowArray.copyOf()
            for (i in 0 until pointNum) {
                newRow[i] = newRow[i + 1]
            }
            newRow[pointNum] = newValues.getOrElse(points.indexOf(rowArray)) { 0f }
            newRow
        }.toMutableList()

        if (historyON) {
            points.forEachIndexed { idx, rowArray ->
                prefs.edit { putString(name + "HIS$idx", rowArray.joinToString(" ")) }
            }
        }
    }

    // 使用 LaunchedEffect 在后台定时采集点，如果监控关闭或屏幕熄灭则暂停
    LaunchedEffect(name, historyON, type) {
        while (true) {
            delay(1000L.milliseconds)
            val number = type.filter { it.isDigit() }
            val isActive = if (number.isNotEmpty()) {
                CMNotiState || CMFloatingState
            } else {
                when (type) {
                    "C" -> CMNotiState || CMFloatingState
                    "M" -> CMNotiState || CMFloatingState
                    else -> NETNotiState || NetFloatingState
                }
            }
            if (screenstate && isActive) {
                addPoint()
            }
        }
    }

    fun toggleHistory() {
        if (historyON) {
            Toast.makeText(context, "History OFF", Toast.LENGTH_SHORT).show()
            (0 until linenum).forEach { prefs.edit { putString(name + "HIS$it", "0 ".repeat(pointNum + 1).trimEnd()) } }
            points = MutableList(linenum) { FloatArray(pointNum + 1) }
        } else {
            Toast.makeText(context, "History ON", Toast.LENGTH_SHORT).show()
        }
        historyON = !historyON
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(width)
            .height(with(LocalDensity.current) { height.toDp() })
            .padding(10.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { toggleHistory() },
                    onDoubleTap = { linetype = if (linetype == 0) 1 else 0 }
                )
            }
    ) {
        val originX = if (axisON) size.width / 20 else 5f
        val endY = if (axisON) size.height / 20 else 5f
        val originY = size.height - endY
        val endX = size.width - originX

        val xscale = (endX - originX) / pointNum
        val yscale = (originY - endY) / 10

        // X轴
        drawLine(color = Color.Gray, start = Offset(originX, originY), end = Offset(endX, originY), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = Color.Gray, start = Offset(originX, endY), end = Offset(endX, endY), strokeWidth = 2f, cap = StrokeCap.Round)

        // Y轴
        drawLine(color = Color.Gray, start = Offset(originX, originY), end = Offset(originX, endY), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = Color.Gray, start = Offset(endX, originY), end = Offset(endX, endY), strokeWidth = 2f, cap = StrokeCap.Round)

        val isNet = (type == "N")
        val maxVal = if (isNet) {
            maxOf(100f, points.maxOfOrNull { row -> row.maxOrNull() ?: 0f } ?: 100f)
        } else {
            100f
        }
        val maxY = if (isNet) {
            when {
                maxVal <= 500f -> 500f
                maxVal <= 1024f -> 1024f
                maxVal <= 1024f * 10f -> 1024f * 10f
                maxVal <= 1024f * 100f -> 1024f * 100f
                else -> maxVal * 1.2f
            }
        } else {
            100f
        }

        // 标签绘制
        if (!axisON) {
            val dynamicDivisor = if (screenWidthDp >= 600) 20f else 30f
            drawText(
                textMeasurer,
                name,
                Offset(endX / 10F * 2.5F, originY / 10F * 3F),
                TextStyle(
                    fontSize = (height / dynamicDivisor).sp,
                    color = Color.Gray.copy(alpha = 0.3f),
                    fontFamily = FontFamily.Monospace
                )
            )
        } else {
            val unitText = if (isNet) {
                if (maxY >= 1024f) "(MB/s)" else "(KB/s)"
            } else {
                "(%)"
            }
            drawText(textMeasurer, unitText, Offset(originX, endY - size.height / 15), TextStyle(fontSize = 10.sp, color = Color.Gray))
        }

        // X轴刻度
        for (i in 0..pointNum) {
            val x = originX + i * xscale
            drawLine(color = Color.Gray, start = Offset(x, originY - 5f), end = Offset(x, originY + 5f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(color = Color.Gray, start = Offset(x, endY - 5f), end = Offset(x, endY + 5f), strokeWidth = 2f, cap = StrokeCap.Round)
        }

        // Y轴刻度
        for (i in 0..10) {
            val y = originY - i * yscale
            drawLine(color = Color.Gray, start = Offset(originX - 5f, y), end = Offset(originX + 5f, y), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(color = Color.Gray, start = Offset(endX - 5f, y), end = Offset(endX + 5f, y), strokeWidth = 2f, cap = StrokeCap.Round)

            if (i % 2 == 0 && axisON) {
                val labelText = if (isNet) {
                    val valKb = (maxY / 10f) * i
                    if (maxY == 1024f) {
                        // 1 MB/s 档位：保留一位小数
                        "%.1f".format(valKb / 1024f)
                    } else if (maxY > 1024f) {
                        // 其他 MB/s 档位：不保留小数
                        "%.0f".format(valKb / 1024f)
                    } else {
                        // KB/s 档位：不保留小数
                        "%.0f".format(valKb)
                    }
                } else {
                    "${i * 10}"
                }
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(0f, y - 15f),
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
            }
        }

        // 纯净的绘制阶段：只读取 points 进行绘制，无任何副作用
        val number = type.filter { it.isDigit() }
        (0 until linenum).forEach { itIdx ->
            path[itIdx].reset()
            val raw0 = points[itIdx][0]
            val norm0 = if (isNet) (raw0 / maxY).coerceIn(0f, 1f) else (raw0 / 100f).coerceIn(0f, 1f)
            path[itIdx].moveTo(originX, originY - norm0 * 10 * yscale)
            for (i in 0..pointNum) {
                val rawVal = points[itIdx][i]
                val normVal = if (isNet) (rawVal / maxY).coerceIn(0f, 1f) else (rawVal / 100f).coerceIn(0f, 1f)
                path[itIdx].lineTo(
                    originX + i * xscale,
                    originY - normVal * 10 * yscale
                )
            }
            drawPath(
                path = path[itIdx],
                color = paintColor[if (number.isNotEmpty()) linetype else itIdx],
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    hasSwitch: Boolean = false,
    switchScale:Float = 1f,
    switchState: Boolean = false,
    enabled: Boolean = true,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            subtitle?.let {
                Text(text = it, fontSize = 15.sp, color = Color.Gray,overflow = TextOverflow.Clip, fontFamily = FontFamily.Monospace)
            }
        }

        if (hasSwitch && onSwitchChange != null) {
            Switch(
                checked = switchState,
                onCheckedChange = onSwitchChange,
                enabled = enabled,
                modifier = Modifier.scale(switchScale)
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ColorPickerRow(
    title: String,
    colors: List<Color>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp * 0.15f)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEachIndexed { index, color ->
                val isSelected = (index == selectedIndex)
                Box(
                    modifier = Modifier
                        .size(40.dp, 28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 2.dp,
                            color = if (isSelected) Color.White else Color.Gray,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onColorSelected(index) }
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ThemeSwatchRow(
    title: String,
    selectedIndex: Int,
    onThemeSelected: (Int) -> Unit
) {
    val themes = listOf("Dark", "Light")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Brightness4, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp * 0.15f)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            themes.forEachIndexed { index, name ->
                val isSelected = (index == selectedIndex)
                val innerBg = if (index == 0) Color.Black else Color.White
                val textColor = if (index == 0) Color.White else Color.Black
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(innerBg)
                        .border(
                            width = if (isSelected) 4.dp else 2.dp,
                            color = if (isSelected) Color.White else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onThemeSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun OptionSwatchRow(
    icon: ImageVector,
    title: String,
    contents: Array<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    val prefs = LocalContext.current.getSharedPreferences("MyPrefs", MODE_PRIVATE)
    val colorIndex1 = prefs.getInt("AppCurveColor", 0)
    val colorIndex2 = prefs.getInt("AppCurveColor2", 1)
    val mainColor = AppCurveColors.getOrElse(colorIndex1) { DodgerBlue }
    val subColor = AppCurveColors.getOrElse(colorIndex2) { ForestGreen }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp * 0.15f)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) activeColor else Color.Transparent)
                        .border(
                            width = if (isSelected) 3.dp else 2.dp,
                            color = if (isSelected) activeColor else Color.Gray,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Gray,
                        maxLines = 1,
                        softWrap = false
                    )
                }
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
    valueRange: ClosedFloatingPointRange<Float> = 500f..3000f,
    unit: String = ""
) {
    var sliderWidthPx by remember { mutableIntStateOf(0) }// 获取slider宽度

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyLarge,
    //                    modifier = Modifier.padding(end = 230.dp)
            )
            // 滑条
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        sliderWidthPx = coordinates.size.width
                    },
                steps = 4
            )

            Box(
                modifier = Modifier
                    .offset {
                        val pxOffset= (-21.dp.toPx() + sliderWidthPx * (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).toInt()
                        IntOffset(pxOffset, 0)
                    } ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${value.toInt()}$unit",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = DeepSkyBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Black, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, Color.Gray, shape = RoundedCornerShape(10.dp))
                        .width(42.dp)
                )
            }


        }
    }

}