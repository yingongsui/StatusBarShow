package com.yin.statusbarshow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
//import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.yin.statusbarshow.ui.theme.StatusBarShowTheme

class FloatingWindowHelper(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var valuesState = androidx.compose.runtime.mutableStateOf(arrayOf("0", "KB/s"))
    private val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    @SuppressLint("ClickableViewAccessibility")
    fun show(values: Array<String>) {
        if (floatingView != null) {
            update(values)
            return
        }
        valuesState.value = values

        val layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // 读取保存的悬浮窗位置
        val savedX = prefs.getInt("FloatingX", 100)
        val savedY = prefs.getInt("FloatingY", 300)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        val composeView = ComposeView(context).apply {
            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            @Suppress("DEPRECATION")
            val displayMetrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            
            // 读取用户设置的小窗大小索引（0: 5%, 1: 7.5%, 2: 10%, 3: 12.5%, 4: 15%）
            val sizePercents = arrayOf(0.05f, 0.075f, 0.10f, 0.125f, 0.15f)
            val sizePercent = sizePercents.getOrElse(prefs.getInt("FloatingSizeIndex", 0)) { 0.05f }
            val sizePx = (displayMetrics.widthPixels * sizePercent).toInt().coerceAtLeast(80)
            val sizeDp = with(context.resources.displayMetrics) { (sizePx / density).dp }

            setContent {
                StatusBarShowTheme {
                    Box(
                        modifier = Modifier
                            .size(sizeDp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(1.dp),
                    ) {
                        val currentValues = valuesState.value

                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {

                            // 主数字
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(6f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = currentValues.getOrElse(0) { "" },
                                    fontSize = (sizeDp.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    color = Color.White,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false,
                                        ),
                                    ),
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = 0.95f
                                        scaleY = 1.3f
                                    },
                                )
                            }

                            // 副数字
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(4f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = currentValues.getOrElse(1) { "" },
                                    fontSize = (sizeDp.value * 0.3f).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    color = Color.LightGray,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false,
                                        ),
                                    ),
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = 0.9f
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(composeView, params)
                        // 实时保存位置
                        prefs.edit {
                            putInt("FloatingX", params?.x ?: 100)
                            putInt("FloatingY", params?.y ?: 300)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(composeView, params)
            floatingView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun update(values: Array<String>) {
        valuesState.value = values
        floatingView?.invalidate()
        floatingView?.requestLayout()
    }

    fun hide() {
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
        }
    }
}

class MyLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycle.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}
