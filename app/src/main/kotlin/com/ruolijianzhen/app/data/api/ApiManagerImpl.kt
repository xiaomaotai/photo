package com.ruolijianzhen.app.data.api

import android.graphics.Bitmap
import com.ruolijianzhen.app.domain.api.ApiManager
import com.ruolijianzhen.app.domain.model.ApiResult
import com.ruolijianzhen.app.domain.model.ApiStatus
import com.ruolijianzhen.app.domain.model.RecognitionSource
import com.ruolijianzhen.app.domain.quota.QuotaTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API管理器实现
 * 管理多个免费API的调用和自动切换
 */
@Singleton
class ApiManagerImpl @Inject constructor(
    private val quotaTracker: QuotaTracker,
    private val baiduApiClient: BaiduApiClient
) : ApiManager {
    
    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val TIMEOUT_MS = 5000L
    }
    
    init {
        // 初始化百度API客户端，使用内置凭证
        baiduApiClient.configure(
            apiKey = ApiConfig.Baidu.API_KEY,
            secretKey = ApiConfig.Baidu.SECRET_KEY
        )
    }
    
    // API客户端映射（目前只使用百度API）
    private val apiClients = mapOf(
        "BAIDU_API" to baiduApiClient
    )
    
    // API优先级顺序（目前只有百度API）
    private val apiPriority = listOf("BAIDU_API")
    
    override suspend fun recognize(bitmap: Bitmap): ApiResult? = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        var retryCount = 0
        
        // 按优先级尝试各个API
        for (apiName in apiPriority) {
            if (retryCount >= MAX_RETRY_COUNT) break
            
            // 检查额度
            if (!quotaTracker.hasAvailableQuota(apiName)) {
                continue
            }
            
            val client = apiClients[apiName] ?: continue
            
            try {
                val result = client.recognize(bitmap)
                if (result != null) {
                    // 扣减额度
                    quotaTracker.decrementQuota(apiName)
                    return@withContext result
                }
            } catch (e: Exception) {
                lastError = e
                retryCount++
                // 继续尝试下一个API
            }
        }
        
        // 所有API都失败
        null
    }
    
    override suspend fun getApiStatus(): List<ApiStatus> = withContext(Dispatchers.IO) {
        quotaTracker.getAllQuotaStatus().map { quota ->
            ApiStatus(
                name = quota.apiName,
                remainingQuota = minOf(quota.remainingDaily, quota.remainingMonthly),
                resetTime = quota.lastResetDate,
                isAvailable = quota.isAvailable
            )
        }
    }
    
    override suspend fun hasAvailableApi(): Boolean = withContext(Dispatchers.IO) {
        quotaTracker.getAllQuotaStatus().any { it.isAvailable }
    }
}

/**
 * API客户端接口
 */
interface ApiClient {
    suspend fun recognize(bitmap: Bitmap): ApiResult?
    fun getSource(): RecognitionSource
}
