package com.ruolijianzhen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruolijianzhen.app.data.local.entity.ApiQuotaEntity

/**
 * API额度数据访问对象
 */
@Dao
interface QuotaDao {
    
    @Query("SELECT * FROM api_quotas WHERE apiName = :apiName")
    suspend fun getQuotaByName(apiName: String): ApiQuotaEntity?
    
    @Query("SELECT * FROM api_quotas")
    suspend fun getAllQuotas(): List<ApiQuotaEntity>
    
    @Query("SELECT * FROM api_quotas WHERE dailyUsed < dailyLimit OR monthlyUsed < monthlyLimit ORDER BY dailyUsed ASC")
    suspend fun getAvailableApis(): List<ApiQuotaEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuota(quota: ApiQuotaEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotas(quotas: List<ApiQuotaEntity>)
    
    @Update
    suspend fun updateQuota(quota: ApiQuotaEntity)
    
    @Query("UPDATE api_quotas SET dailyUsed = dailyUsed + 1 WHERE apiName = :apiName")
    suspend fun incrementDailyUsage(apiName: String)
    
    @Query("UPDATE api_quotas SET monthlyUsed = monthlyUsed + 1 WHERE apiName = :apiName")
    suspend fun incrementMonthlyUsage(apiName: String)
    
    @Query("UPDATE api_quotas SET dailyUsed = 0, lastDailyReset = :resetTime WHERE apiName = :apiName")
    suspend fun resetDailyQuota(apiName: String, resetTime: Long)
    
    @Query("UPDATE api_quotas SET monthlyUsed = 0, lastMonthlyReset = :resetTime WHERE apiName = :apiName")
    suspend fun resetMonthlyQuota(apiName: String, resetTime: Long)
}
