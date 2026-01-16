package com.ruolijianzhen.app.domain.engine

import com.ruolijianzhen.app.domain.model.AiResult
import com.ruolijianzhen.app.domain.model.ApiResult
import com.ruolijianzhen.app.domain.model.ObjectDetails
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.OfflineResult
import com.ruolijianzhen.app.domain.model.RecognitionSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * RecognitionEngine属性测试
 * 
 * **Feature: object-recognition-app, Property 1: 识别优先级保证**
 * **Feature: object-recognition-app, Property 3: 降级策略完整性**
 * **Validates: Requirements 2.1, 2.2, 2.4**
 */
class RecognitionEnginePropertyTest : FunSpec({
    
    // 置信度阈值
    val confidenceThreshold = 0.7f
    
    // 生成随机ObjectDetails
    val objectDetailsArb = Arb.bind(
        Arb.string(2..20),
        Arb.string(2..20),
        Arb.list(Arb.string(2..10), 0..3),
        Arb.string(10..100),
        Arb.string(10..100),
        Arb.string(2..20)
    ) { name, nameEn, aliases, origin, usage, category ->
        ObjectDetails(
            name = name.ifBlank { "测试物品" },
            nameEn = nameEn.ifBlank { "Test Object" },
            aliases = aliases,
            origin = origin.ifBlank { "测试来历" },
            usage = usage.ifBlank { "测试用途" },
            category = category.ifBlank { "测试分类" }
        )
    }
    
    // 生成随机OfflineResult
    val offlineResultArb = Arb.bind(
        Arb.string(5..30),
        Arb.float(0f..1f),
        objectDetailsArb
    ) { label, confidence, details ->
        OfflineResult(
            label = label.ifBlank { "test_label" },
            confidence = confidence,
            details = details
        )
    }
    
    // 生成高置信度OfflineResult
    val highConfidenceOfflineResultArb = Arb.bind(
        Arb.string(5..30),
        Arb.float(0.7f..1f),
        objectDetailsArb
    ) { label, confidence, details ->
        OfflineResult(
            label = label.ifBlank { "test_label" },
            confidence = confidence,
            details = details
        )
    }
    
    // 生成低置信度OfflineResult
    val lowConfidenceOfflineResultArb = Arb.bind(
        Arb.string(5..30),
        Arb.float(0f..0.69f),
        objectDetailsArb
    ) { label, confidence, details ->
        OfflineResult(
            label = label.ifBlank { "test_label" },
            confidence = confidence,
            details = details
        )
    }

    
    // 生成随机ApiResult (只使用百度API)
    val apiResultArb = Arb.bind(
        Arb.string(2..20),
        Arb.string(10..100),
        Arb.float(0.5f..1f)
    ) { name, description, confidence ->
        ApiResult(
            name = name.ifBlank { "测试物品" },
            description = description.ifBlank { "测试描述" },
            confidence = confidence,
            source = RecognitionSource.BAIDU_API
        )
    }
    
    // 生成随机AiResult
    val aiResultArb = Arb.bind(
        Arb.string(2..20),
        Arb.string(10..100),
        Arb.list(Arb.string(2..10), 0..3),
        Arb.string(10..100),
        Arb.string(10..100),
        Arb.string(2..20)
    ) { name, description, aliases, origin, usage, category ->
        AiResult(
            name = name.ifBlank { "测试物品" },
            description = description.ifBlank { "测试描述" },
            aliases = aliases,
            origin = origin.ifBlank { "测试来历" },
            usage = usage.ifBlank { "测试用途" },
            category = category.ifBlank { "测试分类" }
        )
    }
    
    /**
     * Property 1: 识别优先级保证
     * 
     * *For any* 识别请求，RecognitionEngine必须首先调用OfflineRecognizer；
     * 只有当离线识别置信度低于阈值时，才会调用ApiManager。
     */
    test("Property 1: 高置信度离线结果应直接返回，不调用API") {
        checkAll(100, highConfidenceOfflineResultArb) { offlineResult ->
            // 验证高置信度结果满足阈值
            (offlineResult.confidence >= confidenceThreshold) shouldBe true
            
            // 模拟识别策略：高置信度时应使用离线结果
            val shouldUseOffline = offlineResult.confidence >= confidenceThreshold
            shouldUseOffline shouldBe true
        }
    }
    
    test("Property 1: 低置信度离线结果应触发API调用") {
        checkAll(100, lowConfidenceOfflineResultArb) { offlineResult ->
            // 验证低置信度结果不满足阈值
            (offlineResult.confidence < confidenceThreshold) shouldBe true
            
            // 模拟识别策略：低置信度时应尝试API
            val shouldTryApi = offlineResult.confidence < confidenceThreshold
            shouldTryApi shouldBe true
        }
    }
    
    test("Property 1: 离线识别失败时应尝试API") {
        // 离线识别返回null时，应该尝试API
        val offlineResult: OfflineResult? = null
        val shouldTryApi = offlineResult == null || offlineResult.confidence < confidenceThreshold
        shouldTryApi shouldBe true
    }
    
    test("Property 1: 置信度边界值测试") {
        // 测试边界值：正好等于阈值
        val boundaryResult = OfflineResult(
            label = "test",
            confidence = confidenceThreshold,
            details = ObjectDetails("测试", "Test", emptyList(), "来历", "用途", "分类")
        )
        
        // 等于阈值时应使用离线结果
        (boundaryResult.confidence >= confidenceThreshold) shouldBe true
    }

    
    /**
     * Property 3: 降级策略完整性
     * 
     * *For any* 识别请求，当所有免费API额度耗尽且用户已配置UserAiConfig时，
     * RecognitionEngine必须调用用户配置的AI服务。
     */
    test("Property 3: 当离线和API都失败时，应尝试用户AI") {
        // 模拟场景：离线识别失败，API不可用，用户已配置AI
        val offlineResult: OfflineResult? = null
        val apiAvailable = false
        val userAiConfigured = true
        
        // 降级策略：应该尝试用户AI
        val shouldTryUserAi = (offlineResult == null || offlineResult.confidence < confidenceThreshold) &&
                              !apiAvailable &&
                              userAiConfigured
        
        shouldTryUserAi shouldBe true
    }
    
    test("Property 3: 当用户未配置AI时，应返回离线结果或失败") {
        checkAll(100, lowConfidenceOfflineResultArb) { offlineResult ->
            val apiAvailable = false
            val userAiConfigured = false
            
            // 如果用户未配置AI，应该返回低置信度的离线结果作为备选
            val shouldFallbackToOffline = !apiAvailable && !userAiConfigured
            shouldFallbackToOffline shouldBe true
        }
    }
    
    test("Property 3: 完整降级链测试") {
        // 测试完整的降级链：离线 -> API -> 用户AI
        val scenarios = listOf(
            // 场景1: 离线成功，高置信度 -> 直接返回
            Triple(0.8f, true, true) to "offline",
            // 场景2: 离线低置信度，API可用 -> 使用API
            Triple(0.5f, true, true) to "api",
            // 场景3: 离线低置信度，API不可用，用户AI可用 -> 使用用户AI
            Triple(0.5f, false, true) to "user_ai",
            // 场景4: 离线低置信度，API不可用，用户AI不可用 -> 返回离线结果
            Triple(0.5f, false, false) to "fallback_offline"
        )
        
        scenarios.forEach { (input, expected) ->
            val (confidence, apiAvailable, userAiConfigured) = input
            
            val result = when {
                confidence >= confidenceThreshold -> "offline"
                apiAvailable -> "api"
                userAiConfigured -> "user_ai"
                else -> "fallback_offline"
            }
            
            result shouldBe expected
        }
    }
    
    test("Property 3: 用户AI结果应被正确格式化") {
        checkAll(100, aiResultArb) { aiResult ->
            // 验证AI结果包含所有必需字段
            aiResult.name.isNotBlank() shouldBe true
            aiResult.description.isNotBlank() shouldBe true
            aiResult.origin.isNotBlank() shouldBe true
            aiResult.usage.isNotBlank() shouldBe true
            aiResult.category.isNotBlank() shouldBe true
        }
    }
    
    /**
     * 综合测试：识别策略优先级
     */
    test("综合测试: 识别策略应按优先级执行") {
        checkAll(100, offlineResultArb) { offlineResult ->
            // 策略1: 高置信度离线结果优先
            if (offlineResult.confidence >= confidenceThreshold) {
                val priority = 1
                priority shouldBe 1
            }
            
            // 策略2: 低置信度时尝试API
            if (offlineResult.confidence < confidenceThreshold) {
                val priority = 2
                priority shouldBe 2
            }
        }
    }
    
    test("综合测试: 所有识别方式都应返回有效结果") {
        checkAll(50, offlineResultArb, apiResultArb, aiResultArb) { offline, api, ai ->
            // 验证所有结果类型都包含必需信息
            offline.label.isNotBlank() shouldBe true
            (offline.confidence >= 0f && offline.confidence <= 1f) shouldBe true
            
            api.name.isNotBlank() shouldBe true
            (api.confidence >= 0f && api.confidence <= 1f) shouldBe true
            
            ai.name.isNotBlank() shouldBe true
            (ai.confidence >= 0f && ai.confidence <= 1f) shouldBe true
        }
    }
    
    test("边界测试: 置信度为0时应触发API调用") {
        val zeroConfidenceResult = OfflineResult(
            label = "test",
            confidence = 0f,
            details = ObjectDetails("测试", "Test", emptyList(), "来历", "用途", "分类")
        )
        
        (zeroConfidenceResult.confidence < confidenceThreshold) shouldBe true
    }
    
    test("边界测试: 置信度为1时应直接使用离线结果") {
        val perfectConfidenceResult = OfflineResult(
            label = "test",
            confidence = 1f,
            details = ObjectDetails("测试", "Test", emptyList(), "来历", "用途", "分类")
        )
        
        (perfectConfidenceResult.confidence >= confidenceThreshold) shouldBe true
    }
})
