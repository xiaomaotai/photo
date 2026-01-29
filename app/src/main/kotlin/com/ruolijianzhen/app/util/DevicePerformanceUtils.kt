package com.ruolijianzhen.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * 设备性能检测工具类
 * 用于判断设备是否为低端机，以决定是否简化动画效果
 */
object DevicePerformanceUtils {

    /**
     * 判断是否为低端设备（低内存设备）
     */
    fun isLowEndDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.isLowRamDevice
    }

    /**
     * 检查系统是否开启了减少动画效果
     */
    fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 判断是否应该简化动画
     * 在以下情况返回true：
     * 1. 低端设备
     * 2. 用户开启了减少动画
     */
    fun shouldReduceAnimations(context: Context): Boolean {
        return isLowEndDevice(context) || isReducedMotionEnabled(context)
    }

    /**
     * 获取推荐的粒子数量
     * 低端机减少到12个，正常设备使用40个
     */
    fun getRecommendedParticleCount(context: Context): Int {
        return if (shouldReduceAnimations(context)) 12 else 40
    }

    /**
     * 获取推荐的动画帧率（毫秒每帧）
     * 低端机降低到30fps（33ms），正常设备60fps（16ms）
     */
    fun getRecommendedFrameDelay(context: Context): Long {
        return if (shouldReduceAnimations(context)) 33L else 16L
    }
}
