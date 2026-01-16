package com.ruolijianzhen.app.ui.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.WindowInsets as ComposeWindowInsets

/**
 * 屏幕适配工具
 * 支持刘海屏、挖孔屏、折叠屏等异形屏适配
 */
object ScreenUtils {
    
    /**
     * 设置全屏模式（隐藏状态栏和导航栏）
     */
    fun setFullScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type.statusBars() or 
                    android.view.WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
    
    /**
     * 设置沉浸式状态栏
     */
    fun setImmersiveStatusBar(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
    
    /**
     * 适配刘海屏/挖孔屏
     */
    fun enableDisplayCutout(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}

/**
 * 获取状态栏高度
 */
@Composable
fun getStatusBarHeight(): Dp {
    return ComposeWindowInsets.statusBars.asPaddingValues().calculateTopPadding()
}

/**
 * 获取导航栏高度
 */
@Composable
fun getNavigationBarHeight(): Dp {
    return ComposeWindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * 获取刘海屏/挖孔屏安全区域
 */
@Composable
fun getDisplayCutoutPadding(): PaddingValues {
    return ComposeWindowInsets.displayCutout.asPaddingValues()
}

/**
 * 获取系统栏（状态栏+导航栏）安全区域
 */
@Composable
fun getSystemBarsPadding(): PaddingValues {
    return ComposeWindowInsets.systemBars.asPaddingValues()
}
