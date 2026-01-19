package com.ruolijianzhen.app.data.engine

import android.graphics.Bitmap
import android.util.Log
import com.ruolijianzhen.app.domain.ai.UserAiService
import com.ruolijianzhen.app.domain.api.ApiManager
import com.ruolijianzhen.app.domain.engine.RecognitionEngine
import com.ruolijianzhen.app.domain.formatter.ResultFormatter
import com.ruolijianzhen.app.domain.knowledge.KnowledgeEnhancer
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.RecognitionAssessment
import com.ruolijianzhen.app.domain.model.RecognitionMethod
import com.ruolijianzhen.app.domain.model.RecognitionProgress
import com.ruolijianzhen.app.domain.model.RecognitionQuality
import com.ruolijianzhen.app.domain.model.RecognitionSource
import com.ruolijianzhen.app.domain.model.RecognitionStage
import com.ruolijianzhen.app.domain.model.RecognitionState
import com.ruolijianzhen.app.domain.priority.PriorityManager
import com.ruolijianzhen.app.domain.recognition.OfflineRecognizer
import com.ruolijianzhen.app.util.NetworkUtils
import com.ruolijianzhen.app.util.PerceptualHash
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
 * - 感知哈希缓存（相似图片复用结果）
 * - 网络状态检测（无网络时跳过在线识别）
 * - 进度反馈（实时显示识别阶段）
 * - 超时控制
 */
@Singleton
class RecognitionEngineImpl @Inject constructor(
    private val offlineRecognizer: OfflineRecognizer,
    private val apiManager: ApiManager,
    private val userAiService: UserAiService,
    private val resultFormatter: ResultFormatter,
    private val priorityManager: PriorityManager,
    private val knowledgeEnhancer: KnowledgeEnhancer,
    private val networkUtils: NetworkUtils,
    private val perceptualHash: PerceptualHash
) : RecognitionEngine {
    
    companion object {
        private const val TAG = "RecognitionEngine"
        
        // 超时设置（毫秒）
        private const val TOTAL_TIMEOUT_MS = 15000L     // 总体最大超时15秒
        private const val OFFLINE_TIMEOUT_MS = 2000L    // 本地识别2秒
        private const val API_TIMEOUT_MS = 5000L        // API识别5秒
        private const val AI_TIMEOUT_MS = 6000L         // AI识别6秒
        private const val KNOWLEDGE_TIMEOUT_MS = 3000L  // 知识增强3秒
    }
    
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    
    // 识别进度
    private val _recognitionProgress = MutableStateFlow<RecognitionProgress?>(null)
    val recognitionProgress: StateFlow<RecognitionProgress?> = _recognitionProgress.asStateFlow()
    
    private var confidenceThreshold = RecognitionEngine.DEFAULT_CONFIDENCE_THRESHOLD
    
    override suspend fun recognize(bitmap: Bitmap): Result<ObjectInfo> {
        _recognitionState.value = RecognitionState.Processing
        _recognitionProgress.value = RecognitionProgress.preparing()
        
        // 检查感知哈希缓存
        val cachedResult = perceptualHash.findSimilarResult(bitmap)
        if (cachedResult != null) {
            Log.d(TAG, "Cache hit for similar image: ${cachedResult.result.name}, exact=${cachedResult.isExactMatch}")
            val result = if (cachedResult.isExactMatch) {
                cachedResult.result
            } else {
                // 相似匹配，稍微降低置信度
                cachedResult.result.copy(
                    confidence = cachedResult.result.confidence * 0.95f
                )
            }
            _recognitionProgress.value = RecognitionProgress.completed()
            _recognitionState.value = RecognitionState.Success(result)
            return Result.success(result)
        }
        
        return try {
            // 总体超时控制
            val result = withTimeout(TOTAL_TIMEOUT_MS) {
                val recognitionResult = executeRecognitionStrategy(bitmap)
                
                // 如果识别成功，尝试增强知识
                if (recognitionResult != null) {
                    _recognitionProgress.value = RecognitionProgress.knowledgeEnhancement()
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
                perceptualHash.cacheResult(bitmap, result)
                
                _recognitionProgress.value = RecognitionProgress.completed()
                _recognitionState.value = RecognitionState.Success(result)
                Result.success(result)
            } else {
                val error = "无法识别该物品，请尝试调整角度或光线后重试"
                _recognitionProgress.value = RecognitionProgress.failed(error)
                _recognitionState.value = RecognitionState.Error(error)
                Result.failure(RecognitionException(error))
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Recognition timeout after ${TOTAL_TIMEOUT_MS}ms")
            val errorMessage = "识别超时，请重试"
            _recognitionProgress.value = RecognitionProgress.failed(errorMessage)
            _recognitionState.value = RecognitionState.Error(errorMessage)
            Result.failure(RecognitionException(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Recognition failed", e)
            val errorMessage = "识别过程出错：${e.message ?: "未知错误"}"
            _recognitionProgress.value = RecognitionProgress.failed(errorMessage)
            _recognitionState.value = RecognitionState.Error(errorMessage)
            Result.failure(RecognitionException(errorMessage, e))
        }
    }

    /**
     * 按用户配置的优先级执行识别策略
     * 优化：
     * - 快速返回高置信度结果
     * - 网络不可用时跳过在线识别
     * - 实时进度反馈
     */
    private suspend fun executeRecognitionStrategy(bitmap: Bitmap): ObjectInfo? {
        val enabledMethods = priorityManager.getEnabledMethodsInOrder()
        Log.d(TAG, "Recognition order: ${enabledMethods.map { it.name }}")
        
        // 检查网络状态
        val isNetworkAvailable = networkUtils.isNetworkAvailable()
        Log.d(TAG, "Network available: $isNetworkAvailable")
        
        var fallbackResult: ObjectInfo? = null
        val startTime = System.currentTimeMillis()
        val attemptedMethods = mutableListOf<RecognitionMethod>()
        
        for (method in enabledMethods) {
            // 检查剩余时间
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > TOTAL_TIMEOUT_MS - 1000) {
                Log.d(TAG, "Time running out, returning current result")
                break
            }
            
            // 如果是在线方法但网络不可用，跳过
            if (!isNetworkAvailable && (method == RecognitionMethod.BAIDU_API || method == RecognitionMethod.USER_AI)) {
                Log.d(TAG, "Skipping ${method.name} due to no network")
                continue
            }
            
            attemptedMethods.add(method)
            
            // 更新进度
            _recognitionProgress.value = when (method) {
                RecognitionMethod.OFFLINE -> RecognitionProgress(
                    stage = RecognitionStage.OFFLINE_RECOGNITION,
                    currentMethod = method,
                    attemptedMethods = attemptedMethods.toList(),
                    message = "正在进行本地识别..."
                )
                RecognitionMethod.BAIDU_API -> RecognitionProgress(
                    stage = RecognitionStage.API_RECOGNITION,
                    currentMethod = method,
                    attemptedMethods = attemptedMethods.toList(),
                    message = "正在调用云端API..."
                )
                RecognitionMethod.USER_AI -> RecognitionProgress(
                    stage = RecognitionStage.AI_RECOGNITION,
                    currentMethod = method,
                    attemptedMethods = attemptedMethods.toList(),
                    message = "正在使用AI分析..."
                )
            }
            
            val result = when (method) {
                RecognitionMethod.OFFLINE -> tryOfflineRecognition(bitmap)
                RecognitionMethod.BAIDU_API -> tryApiRecognition(bitmap)
                RecognitionMethod.USER_AI -> tryUserAiRecognition(bitmap)
            }
            
            if (result != null) {
                val methodTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "${method.name} completed in ${methodTime}ms, confidence: ${result.confidence}")
                
                // 评估结果质量
                val assessment = RecognitionAssessment.assess(result.confidence, result.source)
                
                // 如果是高质量结果，直接返回
                if (assessment.quality == RecognitionQuality.HIGH) {
                    return result
                }
                
                // 如果是中等质量且置信度足够，也可以返回
                if (assessment.quality == RecognitionQuality.MEDIUM && result.confidence >= confidenceThreshold) {
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
        
        // 返回最佳的备选结果
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
        _recognitionProgress.value = null
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
        perceptualHash.clearCache()
        Log.d(TAG, "Recognition result cache cleared")
    }
    
    /**
     * 获取缓存统计
     */
    fun getCacheStats() = perceptualHash.getCacheStats()
}

/**
 * 识别异常
 */
class RecognitionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
