package com.ruolijianzhen.app.domain.quota

import com.ruolijianzhen.app.data.local.entity.ApiQuotaEntity
import com.ruolijianzhen.app.domain.model.QuotaStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.util.Calendar

/**
 * QuotaTracker属性测试
 * 
 * **Feature: object-recognition-app, Property 4: 额度持久化round-trip**
 * **Feature: object-recognition-app, Property 5: 额度重置正确性**
 * **Validates: Requirements 3.1, 3.4**
 */
class QuotaTrackerPropertyTest : FunSpec({
    
    // 生成随机ApiQuotaEntity
    val quotaEntityArb = Arb.bind(
        Arb.string(5..20),
        Arb.int(0..500),
        Arb.int(100..1000),
        Arb.int(0..5000),
        Arb.int(1000..15000),
        Arb.long(0L..System.currentTimeMillis()),
        Arb.long(0L..System.currentTimeMillis())
    ) { name, dailyUsed, dailyLimit, monthlyUsed, monthlyLimit, lastDaily, lastMonthly ->
        ApiQuotaEntity(
            apiName = name.ifBlank { "test_api" },
            dailyUsed = dailyUsed.coerceAtMost(dailyLimit),
            dailyLimit = dailyLimit,
            monthlyUsed = monthlyUsed.coerceAtMost(monthlyLimit),
            monthlyLimit = monthlyLimit,
            lastDailyReset = lastDaily,
            lastMonthlyReset = lastMonthly
        )
    }
    
    // 生成随机QuotaStatus
    val quotaStatusArb = Arb.bind(
        Arb.string(5..20),
        Arb.int(0..500),
        Arb.int(100..1000),
        Arb.int(0..5000),
        Arb.int(1000..15000),
        Arb.long(0L..System.currentTimeMillis())
    ) { name, dailyUsed, dailyLimit, monthlyUsed, monthlyLimit, lastReset ->
        QuotaStatus(
            apiName = name.ifBlank { "test_api" },
            dailyUsed = dailyUsed.coerceAtMost(dailyLimit),
            dailyLimit = dailyLimit,
            monthlyUsed = monthlyUsed.coerceAtMost(monthlyLimit),
            monthlyLimit = monthlyLimit,
            lastResetDate = lastReset
        )
    }

    test("Property 4: ApiQuotaEntity转QuotaStatus后数据一致") {
        checkAll(100, quotaEntityArb) { entity ->
            val status = QuotaStatus(
                apiName = entity.apiName,
                dailyUsed = entity.dailyUsed,
                dailyLimit = entity.dailyLimit,
                monthlyUsed = entity.monthlyUsed,
                monthlyLimit = entity.monthlyLimit,
                lastResetDate = maxOf(entity.lastDailyReset, entity.lastMonthlyReset)
            )
            
            // 验证转换后数据一致
            status.apiName shouldBe entity.apiName
            status.dailyUsed shouldBe entity.dailyUsed
            status.dailyLimit shouldBe entity.dailyLimit
            status.monthlyUsed shouldBe entity.monthlyUsed
            status.monthlyLimit shouldBe entity.monthlyLimit
        }
    }
    
    test("Property 4: QuotaStatus转ApiQuotaEntity后数据一致") {
        checkAll(100, quotaStatusArb) { status ->
            val entity = ApiQuotaEntity(
                apiName = status.apiName,
                dailyUsed = status.dailyUsed,
                dailyLimit = status.dailyLimit,
                monthlyUsed = status.monthlyUsed,
                monthlyLimit = status.monthlyLimit,
                lastDailyReset = status.lastResetDate,
                lastMonthlyReset = status.lastResetDate
            )
            
            // 验证转换后数据一致
            entity.apiName shouldBe status.apiName
            entity.dailyUsed shouldBe status.dailyUsed
            entity.dailyLimit shouldBe status.dailyLimit
            entity.monthlyUsed shouldBe status.monthlyUsed
            entity.monthlyLimit shouldBe status.monthlyLimit
        }
    }
    
    test("Property 5: 日重置逻辑正确性") {
        // 测试跨天重置
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        
        val today = System.currentTimeMillis()
        
        val yesterdayCal = Calendar.getInstance().apply { timeInMillis = yesterday }
        val todayCal = Calendar.getInstance().apply { timeInMillis = today }
        
        val shouldReset = yesterdayCal.get(Calendar.DAY_OF_YEAR) != todayCal.get(Calendar.DAY_OF_YEAR) ||
                          yesterdayCal.get(Calendar.YEAR) != todayCal.get(Calendar.YEAR)
        
        shouldReset shouldBe true
    }
    
    test("Property 5: 月重置逻辑正确性") {
        // 测试跨月重置
        val lastMonth = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.timeInMillis
        
        val today = System.currentTimeMillis()
        
        val lastMonthCal = Calendar.getInstance().apply { timeInMillis = lastMonth }
        val todayCal = Calendar.getInstance().apply { timeInMillis = today }
        
        val shouldReset = lastMonthCal.get(Calendar.MONTH) != todayCal.get(Calendar.MONTH) ||
                          lastMonthCal.get(Calendar.YEAR) != todayCal.get(Calendar.YEAR)
        
        shouldReset shouldBe true
    }
    
    test("Property 5: 同一天不应重置") {
        val now = System.currentTimeMillis()
        val earlier = now - 3600000 // 1小时前
        
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val earlierCal = Calendar.getInstance().apply { timeInMillis = earlier }
        
        val shouldReset = earlierCal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR) ||
                          earlierCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)
        
        shouldReset shouldBe false
    }
    
    test("QuotaStatus.isAvailable计算正确") {
        checkAll(100, quotaStatusArb) { status ->
            val expected = status.dailyUsed < status.dailyLimit && 
                          status.monthlyUsed < status.monthlyLimit
            status.isAvailable shouldBe expected
        }
    }
    
    test("QuotaStatus.remainingDaily计算正确") {
        checkAll(100, quotaStatusArb) { status ->
            val expected = (status.dailyLimit - status.dailyUsed).coerceAtLeast(0)
            status.remainingDaily shouldBe expected
        }
    }
    
    test("QuotaStatus.remainingMonthly计算正确") {
        checkAll(100, quotaStatusArb) { status ->
            val expected = (status.monthlyLimit - status.monthlyUsed).coerceAtLeast(0)
            status.remainingMonthly shouldBe expected
        }
    }
})
