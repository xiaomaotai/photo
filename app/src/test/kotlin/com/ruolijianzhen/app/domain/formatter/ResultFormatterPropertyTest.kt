package com.ruolijianzhen.app.domain.formatter

import com.ruolijianzhen.app.data.formatter.ResultFormatterImpl
import com.ruolijianzhen.app.domain.model.AiResult
import com.ruolijianzhen.app.domain.model.ApiResult
import com.ruolijianzhen.app.domain.model.ObjectDetails
import com.ruolijianzhen.app.domain.model.OfflineResult
import com.ruolijianzhen.app.domain.model.RecognitionSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * ResultFormatter属性测试
 * 
 * **Feature: object-recognition-app, Property 6: 结果格式化完整性**
 * **Validates: Requirements 4.1, 4.2, 4.3**
 * 
 * *For any* 识别结果（无论来源），经过ResultFormatter格式化后的ObjectInfo
 * 必须包含所有必需字段（名称、别名列表、来历描述、用途说明、分类标签），
 * 且缺失字段使用默认占位符而非空值。
 */
class ResultFormatterPropertyTest : FunSpec({
    
    val formatter = ResultFormatterImpl()
    
    // 生成随机ObjectDetails（可能为null或有空字段）
    val objectDetailsArb = Arb.bind(
        Arb.string(0..50),
        Arb.list(Arb.string(0..20), 0..5),
        Arb.string(0..200),
        Arb.string(0..200),
        Arb.string(0..30)
    ) { name, aliases, origin, usage, category ->
        ObjectDetails(
            name = name,
            nameEn = "",
            aliases = aliases,
            origin = origin,
            usage = usage,
            category = category
        )
    }.orNull(0.3)
    
    // 生成随机OfflineResult
    val offlineResultArb = Arb.bind(
        Arb.string(1..50),
        Arb.float(0f..1f),
        objectDetailsArb
    ) { label, confidence, details ->
        OfflineResult(
            label = label.ifBlank { "unknown" },
            confidence = confidence,
            details = details
        )
    }
    
    // 生成随机ApiResult
    val apiResultArb = Arb.bind(
        Arb.string(0..50),
        Arb.string(0..500),
        Arb.float(0f..1f),
        Arb.enum<RecognitionSource>()
    ) { name, description, confidence, source ->
        ApiResult(
            name = name,
            description = description,
            confidence = confidence,
            source = source
        )
    }
    
    // 生成随机AiResult
    val aiResultArb = Arb.bind(
        Arb.string(0..50),
        Arb.string(0..500),
        Arb.list(Arb.string(0..20), 0..5),
        Arb.string(0..200),
        Arb.string(0..200),
        Arb.string(0..30),
        Arb.float(0f..1f)
    ) { name, desc, aliases, origin, usage, category, confidence ->
        AiResult(name, desc, aliases, origin, usage, category, confidence)
    }

    test("Property 6: OfflineResult格式化后必须包含所有必需字段且不为空") {
        checkAll(100, offlineResultArb) { result ->
            val formatted = formatter.format(result)
            
            // ID不能为空
            formatted.id.shouldNotBeBlank()
            
            // 名称不能为空
            formatted.name.shouldNotBeBlank()
            
            // 来历不能为空
            formatted.origin.shouldNotBeBlank()
            
            // 用途不能为空
            formatted.usage.shouldNotBeBlank()
            
            // 分类不能为空
            formatted.category.shouldNotBeBlank()
            
            // 别名列表不能为null
            formatted.aliases shouldNotBe null
            
            // 来源应该是OFFLINE
            formatted.source shouldBe RecognitionSource.OFFLINE
        }
    }
    
    test("Property 6: ApiResult格式化后必须包含所有必需字段且不为空") {
        checkAll(100, apiResultArb) { result ->
            val formatted = formatter.format(result)
            
            formatted.id.shouldNotBeBlank()
            formatted.name.shouldNotBeBlank()
            formatted.origin.shouldNotBeBlank()
            formatted.usage.shouldNotBeBlank()
            formatted.category.shouldNotBeBlank()
            formatted.aliases shouldNotBe null
        }
    }
    
    test("Property 6: AiResult格式化后必须包含所有必需字段且不为空") {
        checkAll(100, aiResultArb) { result ->
            val formatted = formatter.format(result)
            
            formatted.id.shouldNotBeBlank()
            formatted.name.shouldNotBeBlank()
            formatted.origin.shouldNotBeBlank()
            formatted.usage.shouldNotBeBlank()
            formatted.category.shouldNotBeBlank()
            formatted.aliases shouldNotBe null
            formatted.source shouldBe RecognitionSource.USER_AI
        }
    }
    
    test("Property 6: 空字段应使用默认占位符而非空字符串") {
        // 测试所有字段为空的情况
        val emptyOfflineResult = OfflineResult(
            label = "test",
            confidence = 0.5f,
            details = ObjectDetails(
                name = "",
                nameEn = "",
                aliases = emptyList(),
                origin = "",
                usage = "",
                category = ""
            )
        )
        
        val formatted = formatter.format(emptyOfflineResult)
        
        formatted.name.shouldNotBeBlank()
        formatted.origin.shouldNotBeBlank()
        formatted.usage.shouldNotBeBlank()
        formatted.category.shouldNotBeBlank()
    }
    
    test("Property 6: null details应使用默认值") {
        val nullDetailsResult = OfflineResult(
            label = "test_label",
            confidence = 0.8f,
            details = null
        )
        
        val formatted = formatter.format(nullDetailsResult)
        
        formatted.name.shouldNotBeBlank()
        formatted.origin.shouldNotBeBlank()
        formatted.usage.shouldNotBeBlank()
        formatted.category.shouldNotBeBlank()
    }
})
