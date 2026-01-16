package com.ruolijianzhen.app.data.engine

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.ruolijianzhen.app.domain.ai.UserAiService
import com.ruolijianzhen.app.domain.api.ApiManager
import com.ruolijianzhen.app.domain.engine.RecognitionEngine
import com.ruolijianzhen.app.domain.formatter.ResultFormatter
import com.ruolijianzhen.app.domain.knowledge.KnowledgeEnhancer
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.RecognitionMethod
import com.ruolijianzhen.app.domain.model.RecognitionState
import com.ruolijianzhen.app.domain.priority.PriorityManager
import com.ruolijianzhen.app.domain.recognition.OfflineRecognizer
import com.ruolijianzhen.app.util.PerformanceUtils
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 识别引擎实现
 * 支持用户配置的优先级顺序执行识别
 * 识别后自动增强知识内容
 * 
 * 性能优化：
 * - 结果缓存（相似图片复用结果）
 * - 超时控制
 * - 并行处理
 */
@Singleton
class RecognitionEngineImpl @Inject constructor(
    private val offlineRecognizer: OfflineRecognizer,
    private val apiManager: ApiManager,
    private val userAiService: UserAiService,
    private val resultFormatter: ResultFormatter,
    private val priorityManager: PriorityManager,
    private val knowledgeEnhancer: KnowledgeEnhancer
) : RecognitionEngine {
    
    companion object {
        private const val TAG = "RecognitionEngine"
        
        // 超时设置（毫秒）
        private const val TOTAL_TIMEOUT_MS = 12000L     // 总体最大超时12秒（包含知识增强）
        private const val OFFLINE_TIMEOUT_MS = 1500L    // 本地识别1.5秒
        private const val API_TIMEOUT_MS = 4000L        // API识别4秒
        private const val AI_TIMEOUT_MS = 5000L         // AI识别5秒
        private const val KNOWLEDGE_TIMEOUT_MS = 3000L  // 知识增强3秒
        
        // 结果缓存大小
        private const val RESULT_CACHE_SIZE = 20
    }
    
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    
    private var confidenceThreshold = RecognitionEngine.DEFAULT_CONFIDENCE_THRESHOLD
    
    // 结果缓存（基于图片哈希）
    private val resultCache = LruCache<String, ObjectInfo>(RESULT_CACHE_SIZE)
    
    override suspend fun recognize(bitmap: Bitmap): Result<ObjectInfo> {
        _recognitionState.value = RecognitionState.Processing
        
        // 生成图片缓存键
        val cacheKey = generateImageHash(bitmap)
        
        // 检查缓存
        resultCache.get(cacheKey)?.let { cachedResult ->
            Log.d(TAG, "Cache hit for image: ${cachedResult.name}")
            _recognitionState.value = RecognitionState.Success(cachedResult)
            return Result.success(cachedResult)
        }
        
        return try {
            // 总体超时控制
            val result = withTimeout(TOTAL_TIMEOUT_MS) {
                val recognitionResult = executeRecognitionStrategy(bitmap)
                
                // 如果识别成功，尝试增强知识
                if (recognitionResult != null) {
                    try {
                        withTimeout(KNOWLEDGE_TIMEOUT_MS) {
                            knowledgeEnhancer.enhance(recognitionResult)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Knowledge enhancement failed, using original result", e)
                        recognitionResult
                    }
                } else {
                    null
                }
            }
            
            if (result != null) {
                // 缓存结果
                resultCache.put(cacheKey, result)
                
                _recognitionState.value = RecognitionState.Success(result)
                Result.success(result)
            } else {
                val error = "无法识别该物品，请尝试调整角度或光线后重试"
                _recognitionState.value = RecognitionState.Error(error)
                Result.failure(RecognitionException(error))
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Recognition timeout after ${TOTAL_TIMEOUT_MS}ms")
            val errorMessage = "识别超时，请重试"
            _recognitionState.value = RecognitionState.Error(errorMessage)
            Result.failure(RecognitionException(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Recognition failed", e)
            val errorMessage = "识别过程出错：${e.message ?: "未知错误"}"
            _recognitionState.value = RecognitionState.Error(errorMessage)
            Result.failure(RecognitionException(errorMessage, e))
        }
    }
    
    /**
     * 生成图片哈希（用于缓存键）
     * 使用简化的哈希算法，平衡速度和准确性
     */
    private fun generateImageHash(bitmap: Bitmap): String {
        return try {
            PerformanceUtils.generateCacheKey(bitmap)
        } catch (e: Exception) {
            "${bitmap.width}_${bitmap.height}_${System.currentTimeMillis()}"
        }
    }


    /**
     * 按用户配置的优先级执行识别策略
     * 优化：快速返回高置信度结果，低置信度时继续尝试
     * 改进：即使置信度低也返回结果，不要返回null
     */
    private suspend fun executeRecognitionStrategy(bitmap: Bitmap): ObjectInfo? {
        val enabledMethods = priorityManager.getEnabledMethodsInOrder()
        Log.d(TAG, "Recognition order: ${enabledMethods.map { it.name }}")
        
        var fallbackResult: ObjectInfo? = null
        val startTime = System.currentTimeMillis()
        
        for (method in enabledMethods) {
            // 检查剩余时间
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > TOTAL_TIMEOUT_MS - 500) {
                Log.d(TAG, "Time running out, returning current result")
                break
            }
            
            val result = when (method) {
                RecognitionMethod.OFFLINE -> tryOfflineRecognition(bitmap)
                RecognitionMethod.BAIDU_API -> tryApiRecognition(bitmap)
                RecognitionMethod.USER_AI -> tryUserAiRecognition(bitmap)
            }
            
            if (result != null) {
                val methodTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "${method.name} completed in ${methodTime}ms, confidence: ${result.confidence}")
                
                // 如果置信度足够高，直接返回
                if (result.confidence >= confidenceThreshold) {
                    return result
                }
                // 保存低置信度结果作为备选（选择置信度最高的）
                if (fallbackResult == null || result.confidence > fallbackResult.confidence) {
                    fallbackResult = result
                }
            } else {
                Log.d(TAG, "${method.name} failed, trying next")
            }
        }
        
        // 返回最佳的备选结果（即使置信度低也返回，让用户看到结果）
        if (fallbackResult != null) {
            Log.d(TAG, "Returning fallback result: ${fallbackResult.name} with confidence: ${fallbackResult.confidence}")
        } else {
            Log.w(TAG, "All recognition methods failed, no result available")
        }
        return fallbackResult
    }
    
    /**
     * 尝试离线识别（带超时）
     */
    private suspend fun tryOfflineRecognition(bitmap: Bitmap): ObjectInfo? {
        return try {
            withTimeout(OFFLINE_TIMEOUT_MS) {
                if (!offlineRecognizer.isInitialized()) {
                    offlineRecognizer.initialize()
                }
                val result = offlineRecognizer.recognize(bitmap) ?: return@withTimeout null
                resultFormatter.format(result)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Offline recognition timeout")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Offline recognition failed", e)
            null
        }
    }
    
    /**
     * 尝试API识别（带超时）
     */
    private suspend fun tryApiRecognition(bitmap: Bitmap): ObjectInfo? {
        return try {
            withTimeout(API_TIMEOUT_MS) {
                if (!apiManager.hasAvailableApi()) {
                    Log.d(TAG, "No available API quota")
                    return@withTimeout null
                }
                val result = apiManager.recognize(bitmap) ?: return@withTimeout null
                resultFormatter.format(result)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "API recognition timeout")
            null
        } catch (e: Exception) {
            Log.w(TAG, "API recognition failed", e)
            null
        }
    }
    
    /**
     * 尝试用户AI识别（带超时）
     */
    private suspend fun tryUserAiRecognition(bitmap: Bitmap): ObjectInfo? {
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                if (!userAiService.isConfigured()) {
                    Log.d(TAG, "User AI not configured")
                    return@withTimeout null
                }
                val result = userAiService.recognize(bitmap) ?: return@withTimeout null
                resultFormatter.format(result)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "User AI recognition timeout")
            null
        } catch (e: Exception) {
            Log.w(TAG, "User AI recognition failed", e)
            null
        }
    }
    
    override fun getRecognitionState(): StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    
    override fun resetState() {
        _recognitionState.value = RecognitionState.Idle
    }
    
    override fun getConfidenceThreshold(): Float = confidenceThreshold
    
    /**
     * 设置置信度阈值（用于测试）
     */
    fun setConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold.coerceIn(0f, 1f)
    }
    
    /**
     * 清除结果缓存
     */
    fun clearCache() {
        resultCache.evictAll()
        Log.d(TAG, "Recognition result cache cleared")
    }
}

/**
 * 识别异常
 */
class RecognitionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
