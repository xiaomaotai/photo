package com.ruolijianzhen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * API额度存储实体
 */
@Entity(tableName = "api_quotas")
data class ApiQuotaEntity(
    @PrimaryKey val apiName: String,    // API名称
    val dailyUsed: Int = 0,             // 今日已用次数
    val dailyLimit: Int,                // 每日限额
    val monthlyUsed: Int = 0,           // 本月已用次数
    val monthlyLimit: Int,              // 每月限额
    val lastDailyReset: Long = 0,       // 上次日重置时间
    val lastMonthlyReset: Long = 0      // 上次月重置时间
)
