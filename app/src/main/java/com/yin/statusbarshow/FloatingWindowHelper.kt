package com.yin.statusbarshow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
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

class FloatingWindowHelper(
    private val context: Context,
    private val prefKeyPrefix: String = "Floating",
    private val defaultY: Int = 300,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: ComposeView? = null
    private var lifecycleOwner: MyLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null
    private val valuesState = mutableStateOf(arrayOf("0", "KB/s"))
    private val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    @Volatile
    var isShowing: Boolean = false
        private set

    @SuppressLint("ClickableViewAccessibility")
    fun show(values: Array<String>) {
        if (floatingView != null) {
            update(values)
            return
        }
        valuesState.value = values

        val savedX = prefs.getInt("${prefKeyPrefix}X", prefs.getInt("FloatingX", 100))
        val savedY = prefs.getInt("${prefKeyPrefix}Y", prefs.getInt("FloatingY", defaultY))

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
        params = layoutParams

        val owner = MyLifecycleOwner().apply {
            performRestore(null)
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner = owner

        val bounds = windowManager.currentWindowMetrics.bounds
        val shortEdgePx = minOf(bounds.width(), bounds.height())

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                StatusBarShowTheme {
                    val sizePercents = remember { floatArrayOf(0.05f, 0.075f, 0.10f, 0.125f, 0.15f) }
                    val sizePercent = sizePercents.getOrElse(floatingSizeIndex.intValue) { 0.05f }
                    val sizePx = (shortEdgePx * sizePercent).toInt().coerceAtLeast(80)
                    val sizeDp = with(LocalDensity.current) { sizePx.toDp() }
                    val currentValues = valuesState.value
                    val shape = remember { RoundedCornerShape(8.dp) }

                    Box(
                        modifier = Modifier
                            .size(sizeDp)
                            .clip(shape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.Gray, shape)
                            .padding(1.dp),
                    ) {
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
                    params?.let { lp ->
                        lp.x = initialX + (event.rawX - initialTouchX).toInt()
                        lp.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager.updateViewLayout(composeView, lp)
                        } catch (e: Exception) {
                            LogUtils.e("FloatingWindowHelper", "Failed to update floating layout", e)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    params?.let { lp ->
                        prefs.edit {
                            putInt("${prefKeyPrefix}X", lp.x)
                            putInt("${prefKeyPrefix}Y", lp.y)
                            putInt("FloatingX", lp.x)
                            putInt("FloatingY", lp.y)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(composeView, layoutParams)
            floatingView = composeView
            isShowing = true
        } catch (e: Exception) {
            owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            lifecycleOwner = null
            LogUtils.e("FloatingWindowHelper", "Failed to add floating view", e)
        }
    }

    fun update(values: Array<String>) {
        if (!values.contentEquals(valuesState.value)) {
            valuesState.value = values
        }
    }

    fun hide() {
        floatingView?.let { view ->
            lifecycleOwner?.apply {
                handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            lifecycleOwner = null
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                LogUtils.e("FloatingWindowHelper", "Failed to remove floating view", e)
            }
            floatingView = null
            isShowing = false
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
