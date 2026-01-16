package com.ruolijianzhen.app.util

import android.util.Log
import com.ruolijianzhen.app.data.ml.TFLiteClassifier
import com.ruolijianzhen.app.domain.api.ApiManager
import com.ruolijianzhen.app.domain.recognition.OfflineRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预加载管理器
 * 在应用启动时预加载关键资源，提升首次识别速度
 */
@Singleton
class PreloadManager @Inject constructor(
    private val offlineRecognizer: OfflineRecognizer,
    private val apiManager: ApiManager
) {
    companion object {
        private const val TAG = "PreloadManager"
    }
    
    @Volatile
    private var isPreloaded = false
    
    @Volatile
    private var isPreloading = false
    
    private var preloadStartTime = 0L
    
    /**
     * 开始预加载
     * 在后台线程异步加载资源
     */
    fun startPreload(scope: CoroutineScope) {
        if (isPreloaded || isPreloading) {
            Log.d(TAG, "Already preloaded or preloading")
            return
        }
        
        isPreloading = true
        preloadStartTime = System.currentTimeMillis()
        
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting preload...")
                
                // 并行预加载多个资源
                val offlineJob = async { preloadOfflineRecognizer() }
                val apiJob = async { preloadApiManager() }
                
                // 等待所有预加载完成
                offlineJob.await()
                apiJob.await()
                
                isPreloaded = true
                val duration = System.currentTimeMillis() - preloadStartTime
                Log.d(TAG, "Preload completed in ${duration}ms")
                
            } catch (e: Exception) {
                Log.e(TAG, "Preload failed", e)
            } finally {
                isPreloading = false
            }
        }
    }
    
    /**
     * 预加载离线识别器
     */
    private suspend fun preloadOfflineRecognizer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val success = offlineRecognizer.initialize()
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Offline recognizer preload: ${if (success) "success" else "failed"} (${duration}ms)")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload offline recognizer", e)
            false
        }
    }
    
    /**
     * 预加载API管理器
     */
    private suspend fun preloadApiManager(): Boolean = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            // 预热API配额检查
            val hasApi = apiManager.hasAvailableApi()
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "API manager preload: hasApi=$hasApi (${duration}ms)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload API manager", e)
            false
        }
    }
    
    /**
     * 检查是否已预加载完成
     */
    fun isReady(): Boolean = isPreloaded
    
    /**
     * 等待预加载完成
     */
    suspend fun waitForPreload(timeoutMs: Long = 5000): Boolean = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        while (!isPreloaded && (System.currentTimeMillis() - startTime) < timeoutMs) {
            kotlinx.coroutines.delay(50)
        }
        isPreloaded
    }
    
    /**
     * 获取预加载状态
     */
    fun getStatus(): PreloadStatus {
        return PreloadStatus(
            isPreloaded = isPreloaded,
            isPreloading = isPreloading,
            offlineReady = offlineRecognizer.isInitialized(),
            apiReady = true // API总是可用的
        )
    }
}

/**
 * 预加载状态
 */
data class PreloadStatus(
    val isPreloaded: Boolean,
    val isPreloading: Boolean,
    val offlineReady: Boolean,
    val apiReady: Boolean
) {
    val isFullyReady: Boolean
        get() = offlineReady && apiReady
}
