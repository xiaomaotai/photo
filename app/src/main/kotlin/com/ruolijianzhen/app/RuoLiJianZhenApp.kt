package com.ruolijianzhen.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import com.ruolijianzhen.app.util.MemoryManager
import com.ruolijianzhen.app.util.PerformanceUtils
import com.ruolijianzhen.app.util.PreloadManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * 若里见真 - 物品识别应用
 * Application类，配置Hilt依赖注入、全局异常处理和性能优化
 */
@HiltAndroidApp
class RuoLiJianZhenApp : Application() {
    
    companion object {
        private const val TAG = "RuoLiJianZhenApp"
    }
    
    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 内存管理器
    private lateinit var memoryManager: MemoryManager
    
    @Inject
    lateinit var preloadManager: PreloadManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 设置全局未捕获异常处理器
        setupUncaughtExceptionHandler()
        
        // 初始化内存管理器
        initMemoryManager()
        
        Log.d(TAG, "若里见真 Application initialized")
    }
    
    /**
     * 初始化内存管理器
     */
    private fun initMemoryManager() {
        memoryManager = MemoryManager(this)
        memoryManager.startMonitoring(applicationScope)
    }
    
    /**
     * 开始预加载（在首个Activity创建后调用）
     */
    fun startPreload() {
        if (::preloadManager.isInitialized) {
            preloadManager.startPreload(applicationScope)
        }
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "Memory running low, clearing caches")
                PerformanceUtils.clearCache()
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "UI hidden, reducing memory usage")
                // 可以在这里释放一些UI相关的缓存
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "App in background, clearing all caches")
                PerformanceUtils.clearCache()
                memoryManager.forceCleanup()
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "System low memory, clearing all caches")
        PerformanceUtils.clearCache()
        memoryManager.forceCleanup()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        memoryManager.stopMonitoring()
    }
    
    /**
     * 设置全局未捕获异常处理器
     * 防止应用因未处理的异常而崩溃
     */
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            
            // 记录崩溃信息（可以后续添加崩溃上报）
            try {
                logCrashInfo(throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash info", e)
            }
            
            // 调用默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * 记录崩溃信息
     */
    private fun logCrashInfo(throwable: Throwable) {
        val stackTrace = throwable.stackTraceToString()
        Log.e(TAG, "Crash stack trace:\n$stackTrace")
        
        // 可以在这里添加崩溃信息持久化或上报逻辑
    }
}
