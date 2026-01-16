package com.ruolijianzhen.app.domain.quota

import com.ruolijianzhen.app.domain.model.ApiConfig
import com.ruolijianzhen.app.domain.model.QuotaStatus

/**
 * 额度追踪器接口
 * 持久化管理API额度
 */
interface QuotaTracker {
    /**
     * 获取下一个可用的API配置
     */
    suspend fun getNextAvailableApi(): ApiConfig?
    
    /**
     * 减少指定API的额度
     */
    suspend fun decrementQuota(apiName: String)
    
    /**
     * 检查并重置过期的额度
     */
    suspend fun checkAndResetQuotas()
    
    /**
     * 获取所有API的额度状态
     */
    suspend fun getAllQuotaStatus(): List<QuotaStatus>
    
    /**
     * 获取指定API的额度状态
     */
    suspend fun getQuotaStatus(apiName: String): QuotaStatus?
    
    /**
     * 检查指定API是否有可用额度
     */
    suspend fun hasAvailableQuota(apiName: String): Boolean
}
