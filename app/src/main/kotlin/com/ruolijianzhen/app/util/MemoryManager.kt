package com.ruolijianzhen.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 内存管理器
 * 监控内存使用，在内存紧张时自动清理缓存
 */
class MemoryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MemoryManager"
        
        // 内存警告阈值（可用内存低于此比例时触发清理）
        private const val LOW_MEMORY_THRESHOLD = 0.15f
        
        // 监控间隔（毫秒）
        private const val MONITOR_INTERVAL = 30_000L
    }
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var monitorJob: Job? = null
    private val listeners = mutableListOf<MemoryWarningListener>()
    
    /**
     * 开始内存监控
     */
    fun startMonitoring(scope: CoroutineScope) {
        if (monitorJob?.isActive == true) return
        
        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                checkMemory()
                delay(MONITOR_INTERVAL)
            }
        }
        
        Log.d(TAG, "Memory monitoring started")
    }
    
    /**
     * 停止内存监控
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        Log.d(TAG, "Memory monitoring stopped")
    }
    
    /**
     * 检查内存状态
     */
    private fun checkMemory() {
        val memInfo = getMemoryInfo()
        
        if (memInfo.isLowMemory) {
            Log.w(TAG, "Low memory detected! Available: ${memInfo.availableMemoryMB}MB")
            triggerMemoryCleanup()
        } else if (memInfo.availableRatio < LOW_MEMORY_THRESHOLD) {
            Log.w(TAG, "Memory running low: ${(memInfo.availableRatio * 100).toInt()}% available")
            triggerMemoryCleanup()
        }
    }
    
    /**
     * 获取内存信息
     */
    fun getMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        return MemoryInfo(
            totalMemory = memInfo.totalMem,
            availableMemory = memInfo.availMem,
            threshold = memInfo.threshold,
            isLowMemory = memInfo.lowMemory,
            appUsedMemory = usedMemory,
            appMaxMemory = maxMemory,
            nativeHeapSize = Debug.getNativeHeapSize(),
            nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()
        )
    }
    
    /**
     * 触发内存清理
     */
    private fun triggerMemoryCleanup() {
        Log.d(TAG, "Triggering memory cleanup")
        
        // 清理图片缓存
        PerformanceUtils.clearCache()
        
        // 通知监听器
        listeners.forEach { it.onLowMemory() }
        
        // 建议GC
        System.gc()
    }
    
    /**
     * 手动触发内存清理
     */
    fun forceCleanup() {
        triggerMemoryCleanup()
    }
    
    /**
     * 添加内存警告监听器
     */
    fun addListener(listener: MemoryWarningListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除内存警告监听器
     */
    fun removeListener(listener: MemoryWarningListener) {
        listeners.remove(listener)
    }
    
    /**
     * 获取内存使用报告
     */
    fun getMemoryReport(): String {
        val info = getMemoryInfo()
        return buildString {
            appendLine("=== Memory Report ===")
            appendLine("System Memory:")
            appendLine("  Total: ${info.totalMemoryMB}MB")
            appendLine("  Available: ${info.availableMemoryMB}MB (${(info.availableRatio * 100).toInt()}%)")
            appendLine("  Low Memory: ${info.isLowMemory}")
            appendLine()
            appendLine("App Memory:")
            appendLine("  Used: ${info.appUsedMemoryMB}MB")
            appendLine("  Max: ${info.appMaxMemoryMB}MB")
            appendLine("  Usage: ${(info.appMemoryUsage * 100).toInt()}%")
            appendLine()
            appendLine("Native Heap:")
            appendLine("  Size: ${info.nativeHeapSizeMB}MB")
            appendLine("  Allocated: ${info.nativeHeapAllocatedMB}MB")
            appendLine()
            appendLine("Cache Stats:")
            val cacheStats = PerformanceUtils.getCacheStats()
            appendLine("  Size: ${cacheStats.size}/${cacheStats.maxSize}")
            appendLine("  Hit Rate: ${(cacheStats.hitRate * 100).toInt()}%")
        }
    }
}

/**
 * 内存信息
 */
data class MemoryInfo(
    val totalMemory: Long,
    val availableMemory: Long,
    val threshold: Long,
    val isLowMemory: Boolean,
    val appUsedMemory: Long,
    val appMaxMemory: Long,
    val nativeHeapSize: Long,
    val nativeHeapAllocated: Long
) {
    val totalMemoryMB: Long get() = totalMemory / (1024 * 1024)
    val availableMemoryMB: Long get() = availableMemory / (1024 * 1024)
    val appUsedMemoryMB: Long get() = appUsedMemory / (1024 * 1024)
    val appMaxMemoryMB: Long get() = appMaxMemory / (1024 * 1024)
    val nativeHeapSizeMB: Long get() = nativeHeapSize / (1024 * 1024)
    val nativeHeapAllocatedMB: Long get() = nativeHeapAllocated / (1024 * 1024)
    
    val availableRatio: Float get() = availableMemory.toFloat() / totalMemory
    val appMemoryUsage: Float get() = appUsedMemory.toFloat() / appMaxMemory
}

/**
 * 内存警告监听器
 */
interface MemoryWarningListener {
    fun onLowMemory()
}
