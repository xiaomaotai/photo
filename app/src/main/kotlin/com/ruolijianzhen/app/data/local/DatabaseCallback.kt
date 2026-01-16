package com.ruolijianzhen.app.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ruolijianzhen.app.data.api.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 数据库初始化回调
 * 在数据库创建时预填充必要数据
 */
class DatabaseCallback(
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // 在数据库创建时初始化API额度记录
        scope.launch(Dispatchers.IO) {
            initializeApiQuotas(db)
        }
    }
    
    private fun initializeApiQuotas(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        
        // 只初始化百度API的额度记录
        db.execSQL(
            """
            INSERT INTO api_quotas (
                apiName, 
                dailyUsed, 
                dailyLimit, 
                monthlyUsed, 
                monthlyLimit, 
                lastDailyReset, 
                lastMonthlyReset
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                "BAIDU_API",
                0,
                ApiConfig.Baidu.DAILY_LIMIT,
                0,
                ApiConfig.Baidu.MONTHLY_LIMIT,
                now,
                now
            )
        )
    }
}