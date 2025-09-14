package com.example.statusbarshow
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import androidx.core.graphics.createBitmap
import androidx.core.content.withStyledAttributes


//主页CPU GPU显示图
class RealTimeCurveView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var axisON: Boolean = false
    private var cpuNu: String = ""
    private var historyON: Boolean = false
    private val pointNum : Int = 20


    private var viewid : String =""

    //View创建时的初始化操作
    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.RealTimeCurveView) {
                axisON = getBoolean(R.styleable.RealTimeCurveView_axisON, false)
                cpuNu = getString(R.styleable.RealTimeCurveView_cpuNu).toString()
                historyON = getBoolean(R.styleable.RealTimeCurveView_historyON, false)
            }
        }

        viewid = try {
            resources.getResourceEntryName(id)
        } catch (_: Resources.NotFoundException) {
            "no-id"
        }

//        LogUtils.d("ViewName",viewid)

    }

    private val path = Path()
    private val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)

    private val points : Array<Float> = prefs.getString(viewid+"HIS","0 ".repeat(pointNum+1).trimEnd())!!.split(" ").map { it.toFloat() }.toTypedArray()

//    private val points : Array<Float> = Array(pointNum+1){ 0f }

    fun toggleHistory() {
        historyON = !historyON
        Toast.makeText(context, if (historyON) "History ON" else "History OFF",Toast.LENGTH_SHORT).show()
//        LogUtils.d("HistoryState",historyON.toString())
    }

    fun addPoint(y: Float) {
        (0..pointNum-1).forEach { i -> points[i] = points[i+1] }
        points[pointNum] = y
        if(historyON){
            prefs.edit{putString(viewid+"HIS",points.joinToString(" "))}
            LogUtils.d(viewid+"HIS",prefs.getString(viewid+"HIS"," ").toString())
        }
        invalidate()
//        LogUtils.d("Addpoint",y.toString())
    }

    //创建画笔
    val axisPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    val textPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 20f
        isAntiAlias = true
    }
    val infotextPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 2f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        infotextPaint.textSize = height/5f
        //原点设置
        val originX = 40f
        val originY = height - 40f  //y轴反向
        val endX = width - 40f
        val endY = 30f
        // X轴
        canvas.drawLine(originX, originY, endX, originY, axisPaint)
        canvas.drawLine(originX, endY, endX, endY, axisPaint)
        // Y轴
        canvas.drawLine(originX, originY, originX, endY, axisPaint)
        canvas.drawLine(endX, originY, endX, endY, axisPaint)

        val xscale = (endX-originX)/pointNum
        val yscale = (originY-endY)/10

        //标明CPU名字或者标明%号
        if (cpuNu!=null.toString()) canvas.drawText(cpuNu,endX/10F*3F, originY/10F*6.5F,  infotextPaint) else canvas.drawText("(%)", originX + 10f, endY-10f, textPaint)

        // X轴刻度
        for (i in 0..pointNum) {
            val x = originX + i * xscale
            canvas.drawLine(x, originY - 5f, x, originY + 5f, axisPaint)
            canvas.drawLine(x, endY - 5f, x, endY + 5f, axisPaint)
            //if(axisON){ canvas.drawText("${i * 10}", x - 10f, originY + 25f, textPaint)}
        }

        // Y轴刻度
        for (i in 0..10) {
            val y = originY - i * yscale
            canvas.drawLine(originX - 5f, y, originX + 5f, y, axisPaint)
            canvas.drawLine(endX - 5f, y, endX + 5f, y, axisPaint)
            if(i%2==0 && axisON) canvas.drawText("${i * 10}", originX - 40f, y + 8f, textPaint)
        }

        if (points.size < 2) return
        path.reset()
        path.moveTo(originX, originY - points[0]/10 * yscale)
        for (i in 0 until pointNum+1) {
            path.lineTo(originX + i % (pointNum+1) * xscale, originY - points[i]/10 * yscale)
        }
        canvas.drawPath(path, paint)
    }
}

//全局变量
const val samplingtime :Long = 1500 //ms
var isInForeground = true
var memstate : Array<Int> = arrayOf(0,0)
var memusage : Int = 0
var cpuusage : MutableList<MutableList<Int>> = mutableListOf()
var totalcpuusage : Array<Int>  = arrayOf(0,0)
var netspeedRx : Float = 0f
var netspeedTx : Float = 0f
object MyFunction {


    //创建通知栏数字图像
    fun createBitmapFromString(stateValue: Array<String>, size : Float) : Bitmap {

        val height = 400

        val paint1 = Paint()
        paint1.isAntiAlias = true
        paint1.textSize = size * height   //字体高度，F表示Float类型
        paint1.textAlign = Paint.Align.CENTER
        paint1.isFakeBoldText = true
//        paint1.typeface = Typeface.MONOSPACE


        val paint2 = Paint()
        paint2.isAntiAlias = true
        paint2.textSize = height*(1.0f-size)   //字体高度，F表示Float类型
        paint2.textAlign = Paint.Align.CENTER
        paint2.isFakeBoldText = true

//        val width = when(stateValue[0].length){
//            4 -> paint1.measureText("100%").toInt()
//            else -> paint1.measureText("99%").toInt()
//        }

//        val width = if(stateValue[0].length!=4) paint1.measureText("99%").toInt() else paint1.measureText("100%").toInt()

        val width = paint1.measureText("99%").toInt()
        val bitmap = createBitmap(width+2, height,Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        //创建画布
        canvas.drawText(if(stateValue[0].length<4) stateValue[0] else "1.00" , width / 2F, size * height, paint1)
        canvas.drawText(stateValue[1], width / 2F, height*1.0f, paint2) //在横轴居中，纵轴偏移的位置创建，纵轴是向下的

        return bitmap


    }

    //获取内存信息
    fun readMemStatus():Array<Int>{

        try {
            val memInfo = FileReader("/proc/meminfo")
            val memBuR = BufferedReader(memInfo)

            memBuR.forEachLine { line ->
                if(line.contains("MemTotal")){
                    memstate[0] = Regex("\\d+").find(line)?.value?.toInt() ?: 0
                }else if(line.contains("MemAvailable")){
                    memstate[1] = Regex("\\d+").find(line)?.value?.toInt() ?: 0
                }else{
                    //pass
                    //是否需要在读取到足够信息后直接跳出？
                }
            }
            memusage =  100-memstate[1] * 100 / memstate[0]

            memBuR.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return memstate
    }

    fun readCpuStatus(cpuIndex: Int): Array<Long> {

        //val curFreqPath = "/sys/devices/system/cpu/cpu$cpuIndex/cpufreq/scaling_cur_freq"
        val alltimePath = "/sys/devices/system/cpu/cpu$cpuIndex/cpufreq/stats/time_in_state" //10ms
        val idletimePath = "/sys/devices/system/cpu/cpu$cpuIndex/cpuidle/state0/time"         //0.001s
        val maxFreqPath = "/sys/devices/system/cpu/cpu$cpuIndex/cpufreq/cpuinfo_max_freq"

        try {
            val reader1 = BufferedReader(FileReader(alltimePath))
            val reader2 = BufferedReader(FileReader(idletimePath))
            val reader3 = BufferedReader(FileReader(maxFreqPath))
            val cpustate : Array<Long> = arrayOf(0L,0L,0L)     //(idletime,norworktime,worktime)

            cpustate[0] = reader2.readLine().toLong()/1000
            val maxfreq : Long = reader3.readLine().toLong()


            reader1.forEachLine { line ->
//                LogUtils.d("time in state",line)
                //正规化工作时间
                cpustate[1] += line.split(" ")[0].toLong()*line.split(" ")[1].toLong()*10/maxfreq
                //工作时间
                cpustate[2] += line.split(" ")[1].toLong()*10
            }

//            LogUtils.d("usage","$cpustate")

            reader1.close()
            reader2.close()
            reader3.close()

            return cpustate

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return arrayOf(0,0,0)

    }

    fun getSysInfo(context: Context){

        val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        prefs.edit{putBoolean("PrefState",true)}

        var corecount  =0

        //获取CPU信息
        try {
            val cpuInfo = FileReader("/proc/cpuinfo")
            val cpuBuR = BufferedReader(cpuInfo)
            cpuBuR.forEachLine { line ->
                if(line.contains("processor")){
                    val cucore= Regex("\\d+").find(line)?.value?.toInt() ?: 0
                    corecount = maxOf(corecount, cucore)
                    prefs.edit { putString("CPU$corecount", "") }
                    //获取最大频率
                    val maxFreqPath = "/sys/devices/system/cpu/cpu$cucore/cpufreq/cpuinfo_max_freq"
                    try {
                        val reader = BufferedReader(FileReader(maxFreqPath))
                        prefs.edit { putLong("CPU${corecount}MaxFreq", reader.readLine().toLong()) }
                        reader.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }else if(line.contains("implementer")){
                    prefs.edit { putString("CPU$corecount", prefs.getString("CPU$corecount", "")+line.split(":")[1]+";") }
                //
                }else if(line.contains("architecture")){
                    prefs.edit { putString("CPU$corecount", prefs.getString("CPU$corecount", "")+line.split(":")[1]+";") }
                    //
                }else if(line.contains("variant")){
                    prefs.edit { putString("CPU$corecount", prefs.getString("CPU$corecount", "")+line.split(":")[1]+";") }
                    //
                }else if(line.contains("part")){
                    prefs.edit { putString("CPU$corecount", prefs.getString("CPU$corecount", "")+line.split(":")[1]+";") }
                    //
                }else{
                    //pass
                    //是否需要在读取到足够信息后直接跳出？
                }
            }
            prefs.edit { putInt("CPUCoreNumber", corecount ) }
            cpuBuR.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //获取内存信息
        try {
            val memInfo = FileReader("/proc/meminfo")
            val memBuR = BufferedReader(memInfo)
            memBuR.forEachLine { line ->
                if(line.contains("MemTotal")) {
                    prefs.edit{putString("TotalMemory", line.split(":")[1].trim() )}
//                    println(line)
                } else{
                    //pass
                    //是否需要在读取到足够信息后直接跳出？
                }
            }
            memBuR.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getCPUArch(info: String): String{
        return when {
            info.contains("0xc05", ignoreCase = true) -> "Cortex-A5 ARMv7-A"
            info.contains("0xc07", ignoreCase = true) -> "Cortex-A7 ARMv7-A"
            info.contains("0xc08", ignoreCase = true) -> "Cortex-A8 ARMv7-A"
            info.contains("0xc09", ignoreCase = true) -> "Cortex-A9 ARMv7-A"
            info.contains("0xc0d", ignoreCase = true) -> "Cortex-A12 ARMv7-A"
            info.contains("0xc0e", ignoreCase = true) -> "Cortex-A17 ARMv7-A"
            info.contains("0xc0f", ignoreCase = true) -> "Cortex-A15 ARMv7-A"
            info.contains("0xd01", ignoreCase = true) -> "Cortex-A32 ARMv8-A (32-bit)"
            info.contains("0xd02", ignoreCase = true) -> "Cortex-A34 ARMv8-A"
            info.contains("0xd03", ignoreCase = true) -> "Cortex-A53 ARMv8-A"
            info.contains("0xd04", ignoreCase = true) -> "Cortex-A35 ARMv8-A"
            info.contains("0xd05", ignoreCase = true) -> "Cortex-A55 ARMv8.2-A"
            info.contains("0xd06", ignoreCase = true) -> "Cortex-A65 ARMv8.2-A"
            info.contains("0xd07", ignoreCase = true) -> "Cortex-A57 ARMv8-A"
            info.contains("0xd08", ignoreCase = true) -> "Cortex-A72 ARMv8-A"
            info.contains("0xd09", ignoreCase = true) -> "Cortex-A73 ARMv8-A"
            info.contains("0xd0a", ignoreCase = true) -> "Cortex-A75 ARMv8.2-A"
            info.contains("0xd0b", ignoreCase = true) -> "Cortex-A76 ARMv8.2-A"
            info.contains("0xd0d", ignoreCase = true) -> "Cortex-A77 ARMv8.2-A"
            info.contains("0xd0e", ignoreCase = true) -> "Cortex-A76AE ARMv8.2-A"
            info.contains("0xd41", ignoreCase = true) -> "Cortex-A78 ARMv8.2-A"
            info.contains("0xd43", ignoreCase = true) -> "Cortex-A65AE ARMv8.2-A"
            info.contains("0xd44", ignoreCase = true) -> "Cortex-X1 ARMv8.2-A"
            info.contains("0xd46", ignoreCase = true) -> "Cortex-A510 ARMv9-A"
            info.contains("0xd47", ignoreCase = true) -> "Cortex-A710 ARMv9-A"
            info.contains("0xd48", ignoreCase = true) -> "Cortex-X2 ARMv9-A"
            info.contains("0xd4d", ignoreCase = true) -> "Cortex-A715 ARMv9.2-A"
            info.contains("0xd4e", ignoreCase = true) -> "Cortex-X3 ARMv9-A"
            info.contains("kB", ignoreCase = true) -> Regex("\\d+").find(info)?.value.toString()
            else -> info
        }
    }

}

//日志类文件
object LogUtils {
    private const val ENABLE_LOG = false

    fun d(tag: String, msg: String) {
        if (ENABLE_LOG) Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        if (ENABLE_LOG) Log.i(tag, msg)
    }

    fun w(tag: String, msg: String) {
        if (ENABLE_LOG) Log.w(tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (ENABLE_LOG) {
            if (throwable != null) Log.e(tag, msg, throwable)
            else Log.e(tag, msg)
        }
    }
}

