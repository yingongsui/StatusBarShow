package com.example.statusbarshow.ui


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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.statusbarshow.LogUtils
import com.example.statusbarshow.cpuusage
import com.example.statusbarshow.memusage
import com.example.statusbarshow.totalcpuusage
import com.example.statusbarshow.ui.theme.BlueViolet
import com.example.statusbarshow.ui.theme.DeepSkyBlue
import com.example.statusbarshow.ui.theme.ForestGreen
import com.example.statusbarshow.ui.theme.SteelBlue


sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Profile : Screen("profile", "Information", Icons.Default.Info)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}



@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Profile, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
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
                        }
                    )
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


@Composable
fun UsageGraph(Name : String,pointNum:Int, width : Float ,height : Int, axisON : Boolean, linenum :Int, type: String){

    val textMeasurer = rememberTextMeasurer()
    var historyON = false
    val context = LocalContext.current
    val paintColor : Array<Color> = arrayOf(SteelBlue ,ForestGreen,BlueViolet)

    val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
    val path: Array< androidx.compose.ui.graphics.Path> = Array(linenum){  androidx.compose.ui.graphics.Path() }
    var points : MutableList<Array<Float>> = MutableList(linenum){
            row -> prefs.getString(Name+"HIS$row","0 ".repeat(pointNum+1).trimEnd())!!.split(" ").map { it.toFloat() }.toTypedArray()
    }

    var linetype = 0

    fun addPoint(y: Array<Float>) {
        (0..linenum-1).forEach {
                row ->
                (0..pointNum-1).forEach { col -> points[row][col] = points[row][col+1] }
                points[row][pointNum] = y[row]
//                LogUtils.d("Addpoint",row.toString())
        }

        if(historyON){
            (0..linenum-1).forEach { prefs.edit{putString(Name+"HIS$it",points[it].joinToString(" "))}}
            LogUtils.d(Name+"HIS0",prefs.getString(Name+"HIS"," ").toString())
        }
//        LogUtils.d("Addpoint",y.toString())
    }


    fun toggleHistory() {
        if (historyON) {
            Toast.makeText(context, "History OFF", Toast.LENGTH_SHORT).show()
            (0..linenum - 1).forEach { prefs.edit { putString(Name + "HIS$it", "0 ".repeat(pointNum + 1).trimEnd()) }}
            points = MutableList(linenum) { row -> Array(pointNum + 1) { 0f }}
        } else {
            Toast.makeText(context,"History ON", Toast.LENGTH_SHORT).show()
//        LogUtils.d("HistoryState",historyON.toString())
        }
        historyON = !historyON

    }

    Canvas(modifier = Modifier
        .fillMaxWidth(width)
        .height(height.dp)
        .padding(10.dp)
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    toggleHistory()
                },
                onDoubleTap = { linetype = if(linetype==0){1} else{0} },

                )
        }

    ) {

        //原点设置
        val originX = if(axisON) size.width/20 else 5f
        val endY = if(axisON) size.height/20 else 5f
        val originY = size.height - endY   //y轴反向
        val endX = size.width - originX

        val xscale = (endX-originX)/pointNum
        val yscale = (originY-endY)/10

        // X轴
        drawLine(
            color = Color.Gray,
            start = Offset(originX, originY),
            end = Offset(endX, originY),
            strokeWidth = 2f,
            cap = StrokeCap.Round

        )
        drawLine(
            color = Color.Gray,
            start = Offset(originX, endY),
            end = Offset(endX, endY),
            strokeWidth = 2f,
            cap = StrokeCap.Round

        )

        // Y轴
        drawLine(
            color = Color.Gray,
            start = Offset(originX, originY),
            end = Offset(originX, endY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.Gray,
            start = Offset(endX, originY),
            end = Offset(endX, endY),
            strokeWidth = 2f,
            cap = StrokeCap.Round

        )


        //标明CPU名字或者标明%号
        if (!axisON)  drawText(textMeasurer, Name, Offset(endX/10F*4F, originY/10F*4F), TextStyle(fontSize = (height/8).sp, color = Color.Gray))
        else drawText(textMeasurer,"(%)",Offset(originX, endY-size.height/20),TextStyle(fontSize = 10.sp, color = Color.Gray))


        //  X轴刻度
        for (i in 0..pointNum) {
            val x = originX + i * xscale
            drawLine(
                color = Color.Gray,
                start = Offset(x, originY - 5f),
                end = Offset(x, originY + 5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round

            )

            drawLine(
                color = Color.Gray,
                start = Offset(x, endY - 5f),
                end = Offset(x, endY + 5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round

            )
        }

        // Y轴刻度
        for (i in 0..10) {
            val y = originY - i * yscale
            drawLine(
                color = Color.Gray,
                start = Offset(originX - 5f, y),
                end = Offset(originX + 5f, y),
                strokeWidth = 2f,
                cap = StrokeCap.Round

            )

            drawLine(
                color = Color.Gray,
                start = Offset(endX - 5f, y),
                end = Offset(endX + 5f, y),
                strokeWidth = 2f,
                cap = StrokeCap.Round

            )

            if(i%2==0 && axisON)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${i * 10}",
                    topLeft = Offset(0f, y -15f),
                    style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                )
        }

        drawIntoCanvas { canvas ->
            val number = type.filter { it.isDigit() }
            if(number.isNotEmpty() && prefs.getBoolean("CPU${number}DrawState", true)) {
                addPoint(arrayOf(cpuusage[number.toInt()][linetype].toFloat()))
            }else {
                when (type) {
                    "C" -> addPoint(arrayOf(totalcpuusage[0].toFloat(),totalcpuusage[1].toFloat()))
                    "M" -> addPoint(arrayOf(memusage.value.toFloat()))
                }
            }

            (0..linenum-1).forEach {
                path[it].reset()
                path[it].moveTo(originX, originY - points[it][0] / 10 * yscale)
                for (i in 0 until pointNum + 1) {
                    path[it].lineTo(
                        originX + i % (pointNum + 1) * xscale,
                        originY - points[it][i] / 10 * yscale
                    )
                }
                canvas.drawPath(path[it], Paint().apply {
                    color = paintColor[if(number.isNotEmpty()){linetype}else{it}]
                    strokeWidth = 5f
                    style = PaintingStyle.Stroke
                    isAntiAlias = true
                })
            }

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
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(text = it, fontSize = 14.sp, color = Color.Gray)
            }
        }

        if (hasSwitch && onSwitchChange != null) {
            Switch(
                checked = switchState,
                onCheckedChange = onSwitchChange,
                modifier = Modifier.scale(switchScale)
            )
        }
    }
}

@Composable
fun TypeRatioButton(
    icon: ImageVector,
    title: String,
    selectedstate: Int,
    contents: Array<String> ,
    onOptionSelected: (Int) -> Unit, //没有返回值的参数为字符串的函数
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 0.dp,end = 5.dp).width(50.dp)
        )
        contents.forEach {
            Box(modifier = Modifier.weight(1f),contentAlignment = Alignment.CenterStart) {
                RadioButton(
                    selected = (contents[selectedstate] == it),
                    onClick = { onOptionSelected(contents.indexOf(it)) }
                )
                Text(
                    text = it ,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 50.dp).fillMaxWidth()
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
    valueRange: ClosedFloatingPointRange<Float> = 500f..3000f,
    unit: String = ""
) {
    var sliderWidthPx by remember { mutableStateOf(0) }// 获取slider宽度

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 20.sp,
                style = MaterialTheme.typography.bodyLarge,
    //                    modifier = Modifier.padding(end = 230.dp)
            )
            // 滑条
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                    sliderWidthPx = coordinates.size.width
                },
                steps = 4
            )

            Box(
                modifier = Modifier
                    .offset {
                        val pxOffset= (-21.dp.toPx() + sliderWidthPx * (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).toInt()
                        IntOffset(pxOffset, -150)
                    } ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${value.toInt()}$unit",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
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