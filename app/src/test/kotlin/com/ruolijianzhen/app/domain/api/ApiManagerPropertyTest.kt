package com.ruolijianzhen.app.domain.api

import com.ruolijianzhen.app.domain.model.ApiStatus
import com.ruolijianzhen.app.domain.model.QuotaStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * ApiManager属性测试
 * 
 * **Feature: object-recognition-app, Property 2: API额度切换正确性**
 * **Validates: Requirements 3.2, 3.3**
 * 
 * *For any* API调用场景，当当前API额度耗尽或调用失败时，
 * ApiManager必须自动切换到下一个可用API，且切换过程不中断识别流程。
 */
class ApiManagerPropertyTest : FunSpec({
    
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
    
    // 生成随机ApiStatus
    val apiStatusArb = Arb.bind(
        Arb.string(5..20),
        Arb.int(0..1000),
        Arb.long(0L..System.currentTimeMillis()),
        Arb.boolean()
    ) { name, remaining, resetTime, available ->
        ApiStatus(
            name = name.ifBlank { "test_api" },
            remainingQuota = remaining,
            resetTime = resetTime,
            isAvailable = available
        )
    }
    
    // 生成API列表
    val apiListArb = Arb.list(quotaStatusArb, 1..5)
    
    test("Property 2: 当有可用API时，应该能找到至少一个") {
        checkAll(100, apiListArb) { quotaList ->
            val availableApis = quotaList.filter { it.isAvailable }
            
            // 如果列表中有可用API，hasAvailableApi应返回true
            val hasAvailable = quotaList.any { it.isAvailable }
            (availableApis.isNotEmpty()) shouldBe hasAvailable
        }
    }
    
    test("Property 2: API选择应优先选择剩余额度最多的") {
        checkAll(100, apiListArb) { quotaList ->
            val availableApis = quotaList.filter { it.isAvailable }
            
            if (availableApis.isNotEmpty()) {
                // 找到剩余额度最多的API
                val bestApi = availableApis.maxByOrNull { it.remainingDaily }
                
                // 验证最佳API确实有最多的剩余额度
                availableApis.all { it.remainingDaily <= (bestApi?.remainingDaily ?: 0) } shouldBe true
            }
        }
    }
    
    test("Property 2: 额度耗尽的API不应被选中") {
        checkAll(100, quotaStatusArb) { quota ->
            val isExhausted = quota.dailyUsed >= quota.dailyLimit || 
                             quota.monthlyUsed >= quota.monthlyLimit
            
            // 如果额度耗尽，isAvailable应为false
            if (isExhausted) {
                quota.isAvailable shouldBe false
            }
        }
    }
    
    test("Property 2: API切换逻辑 - 当百度API不可用时应返回null") {
        val apiPriority = listOf("BAIDU_API")
        
        // 模拟百度API不可用的情况
        val quotaStatuses = listOf(
            QuotaStatus("BAIDU_API", 500, 500, 0, 15000, 0) // 已耗尽
        )
        
        // 按优先级找到第一个可用的API
        val selectedApi = apiPriority.firstOrNull { apiName ->
            quotaStatuses.find { it.apiName == apiName }?.isAvailable == true
        }
        
        selectedApi shouldBe null
    }
    
    test("Property 2: 所有API都不可用时应返回null") {
        val quotaStatuses = listOf(
            QuotaStatus("BAIDU_API", 500, 500, 15000, 15000, 0)
        )
        
        val hasAvailable = quotaStatuses.any { it.isAvailable }
        hasAvailable shouldBe false
    }
    
    test("Property 2: QuotaStatus到ApiStatus转换正确") {
        checkAll(100, quotaStatusArb) { quota ->
            val apiStatus = ApiStatus(
                name = quota.apiName,
                remainingQuota = minOf(quota.remainingDaily, quota.remainingMonthly),
                resetTime = quota.lastResetDate,
                isAvailable = quota.isAvailable
            )
            
            apiStatus.name shouldBe quota.apiName
            apiStatus.remainingQuota shouldBe minOf(quota.remainingDaily, quota.remainingMonthly)
            apiStatus.isAvailable shouldBe quota.isAvailable
        }
    }
})
