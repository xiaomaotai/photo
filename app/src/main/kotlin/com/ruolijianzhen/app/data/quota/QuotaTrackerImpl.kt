package com.ruolijianzhen.app.data.quota

import com.ruolijianzhen.app.data.local.dao.QuotaDao
import com.ruolijianzhen.app.data.local.entity.ApiQuotaEntity
import com.ruolijianzhen.app.domain.model.ApiConfig
import com.ruolijianzhen.app.domain.model.QuotaStatus
import com.ruolijianzhen.app.domain.quota.QuotaTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 额度追踪器实现
 * 使用Room持久化存储API额度状态
 */
@Singleton
class QuotaTrackerImpl @Inject constructor(
    private val quotaDao: QuotaDao
) : QuotaTracker {
    
    // API配置（只使用百度API）
    private val apiConfigs = mapOf(
        "BAIDU_API" to ApiConfig(
            name = "BAIDU_API",
            baseUrl = "https://aip.baidubce.com",
            apiKey = com.ruolijianzhen.app.data.api.ApiConfig.Baidu.API_KEY,
            dailyLimit = com.ruolijianzhen.app.data.api.ApiConfig.Baidu.DAILY_LIMIT,
            monthlyLimit = com.ruolijianzhen.app.data.api.ApiConfig.Baidu.MONTHLY_LIMIT
        )
    )
    
    override suspend fun getNextAvailableApi(): ApiConfig? = withContext(Dispatchers.IO) {
        checkAndResetQuotas()
        
        val availableApis = quotaDao.getAvailableApis()
        if (availableApis.isEmpty()) return@withContext null
        
        // 返回剩余额度最多的API
        val bestApi = availableApis.minByOrNull { it.dailyUsed }
        bestApi?.let { apiConfigs[it.apiName] }
    }
    
    override suspend fun decrementQuota(apiName: String) = withContext(Dispatchers.IO) {
        quotaDao.incrementDailyUsage(apiName)
        quotaDao.incrementMonthlyUsage(apiName)
    }
    
    override suspend fun checkAndResetQuotas() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val quotas = quotaDao.getAllQuotas()
        
        for (quota in quotas) {
            var needsUpdate = false
            var updatedQuota = quota
            
            // 检查日重置
            if (shouldResetDaily(quota.lastDailyReset, now)) {
                quotaDao.resetDailyQuota(quota.apiName, now)
                needsUpdate = true
            }
            
            // 检查月重置
            if (shouldResetMonthly(quota.lastMonthlyReset, now)) {
                quotaDao.resetMonthlyQuota(quota.apiName, now)
            }
        }
    }

    override suspend fun getAllQuotaStatus(): List<QuotaStatus> = withContext(Dispatchers.IO) {
        checkAndResetQuotas()
        quotaDao.getAllQuotas().map { it.toQuotaStatus() }
    }
    
    override suspend fun getQuotaStatus(apiName: String): QuotaStatus? = withContext(Dispatchers.IO) {
        checkAndResetQuotas()
        quotaDao.getQuotaByName(apiName)?.toQuotaStatus()
    }
    
    override suspend fun hasAvailableQuota(apiName: String): Boolean = withContext(Dispatchers.IO) {
        checkAndResetQuotas()
        val quota = quotaDao.getQuotaByName(apiName) ?: return@withContext false
        quota.dailyUsed < quota.dailyLimit && quota.monthlyUsed < quota.monthlyLimit
    }
    
    /**
     * 检查是否需要日重置
     */
    private fun shouldResetDaily(lastReset: Long, now: Long): Boolean {
        if (lastReset == 0L) return false
        
        val lastResetCal = Calendar.getInstance().apply { timeInMillis = lastReset }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        
        return lastResetCal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR) ||
               lastResetCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)
    }
    
    /**
     * 检查是否需要月重置
     */
    private fun shouldResetMonthly(lastReset: Long, now: Long): Boolean {
        if (lastReset == 0L) return false
        
        val lastResetCal = Calendar.getInstance().apply { timeInMillis = lastReset }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        
        return lastResetCal.get(Calendar.MONTH) != nowCal.get(Calendar.MONTH) ||
               lastResetCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)
    }
    
    /**
     * 转换为QuotaStatus
     */
    private fun ApiQuotaEntity.toQuotaStatus(): QuotaStatus = QuotaStatus(
        apiName = apiName,
        dailyUsed = dailyUsed,
        dailyLimit = dailyLimit,
        monthlyUsed = monthlyUsed,
        monthlyLimit = monthlyLimit,
        lastResetDate = maxOf(lastDailyReset, lastMonthlyReset)
    )
}
