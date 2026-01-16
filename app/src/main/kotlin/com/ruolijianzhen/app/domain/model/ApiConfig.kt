package com.ruolijianzhen.app.domain.model

/**
 * API配置
 */
data class ApiConfig(
    val name: String,                    // API名称
    val baseUrl: String,                 // 基础URL
    val apiKey: String,                  // API密钥
    val secretKey: String? = null,       // 密钥（部分API需要）
    val dailyLimit: Int,                 // 每日限额
    val monthlyLimit: Int                // 每月限额
)

/**
 * API额度状态
 */
data class QuotaStatus(
    val apiName: String,                 // API名称
    val dailyUsed: Int,                  // 今日已用
    val dailyLimit: Int,                 // 每日限额
    val monthlyUsed: Int,                // 本月已用
    val monthlyLimit: Int,               // 每月限额
    val lastResetDate: Long              // 上次重置时间
) {
    /**
     * 是否有可用额度
     */
    val isAvailable: Boolean
        get() = dailyUsed < dailyLimit && monthlyUsed < monthlyLimit
    
    /**
     * 剩余每日额度
     */
    val remainingDaily: Int
        get() = (dailyLimit - dailyUsed).coerceAtLeast(0)
    
    /**
     * 剩余每月额度
     */
    val remainingMonthly: Int
        get() = (monthlyLimit - monthlyUsed).coerceAtLeast(0)
}

/**
 * API状态
 */
data class ApiStatus(
    val name: String,                    // API名称
    val remainingQuota: Int,             // 剩余额度
    val resetTime: Long,                 // 重置时间
    val isAvailable: Boolean             // 是否可用
)
