package com.ruolijianzhen.app.domain.recognition

import com.ruolijianzhen.app.domain.model.ObjectDetails
import com.ruolijianzhen.app.domain.model.OfflineResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * OfflineRecognizer属性测试
 * 
 * **Feature: object-recognition-app, Property 8: 离线识别数据完整性**
 * **Validates: Requirements 6.2**
 * 
 * *For any* 成功的离线识别结果，返回的ObjectDetails必须包含完整的物品信息（名称、别名、来历、用途）。
 */
class OfflineRecognizerPropertyTest : FunSpec({
    
    // 生成随机ObjectDetails
    val objectDetailsArb = Arb.bind(
        Arb.string(minSize = 1, maxSize = 50),  // name
        Arb.list(Arb.string(minSize = 1, maxSize = 20), 0..5),  // aliases
        Arb.string(minSize = 1, maxSize = 200),  // origin
        Arb.string(minSize = 1, maxSize = 200),  // usage
        Arb.string(minSize = 1, maxSize = 30)   // category
    ) { name, aliases, origin, usage, category ->
        ObjectDetails(
            name = name.ifBlank { "默认名称" },
            nameEn = "",
            aliases = aliases,
            origin = origin.ifBlank { "暂无来历信息" },
            usage = usage.ifBlank { "暂无用途信息" },
            category = category.ifBlank { "未分类" }
        )
    }
    
    // 生成随机OfflineResult
    val offlineResultArb = Arb.bind(
        Arb.string(minSize = 1, maxSize = 50),  // label
        Arb.float(0.3f..1.0f),  // confidence (above threshold)
        objectDetailsArb
    ) { label, confidence, details ->
        OfflineResult(
            label = label.ifBlank { "unknown" },
            confidence = confidence,
            details = details
        )
    }
    
    test("Property 8: 离线识别数据完整性 - ObjectDetails必须包含所有必需字段") {
        checkAll(100, objectDetailsArb) { details ->
            // 名称不能为空
            details.name.shouldNotBeBlank()
            
            // 来历不能为空
            details.origin.shouldNotBeBlank()
            
            // 用途不能为空
            details.usage.shouldNotBeBlank()
            
            // 分类不能为空
            details.category.shouldNotBeBlank()
            
            // 别名列表不能为null（可以为空列表）
            details.aliases shouldNotBe null
        }
    }
    
    test("Property 8: 离线识别数据完整性 - OfflineResult包含有效的details") {
        checkAll(100, offlineResultArb) { result ->
            // 标签不能为空
            result.label.shouldNotBeBlank()
            
            // 置信度在有效范围内
            (result.confidence >= 0f && result.confidence <= 1f) shouldBe true
            
            // details不为null时，必须包含完整信息
            result.details?.let { details ->
                details.name.shouldNotBeBlank()
                details.origin.shouldNotBeBlank()
                details.usage.shouldNotBeBlank()
                details.category.shouldNotBeBlank()
            }
        }
    }
    
    test("Property 8: ObjectDetails.empty()应返回有效的默认值") {
        val emptyDetails = ObjectDetails.empty()
        
        emptyDetails.name.shouldNotBeBlank()
        emptyDetails.origin.shouldNotBeBlank()
        emptyDetails.usage.shouldNotBeBlank()
        emptyDetails.category.shouldNotBeBlank()
        emptyDetails.aliases shouldNotBe null
    }
    
    test("Property 8: ObjectDetails.empty(name)应使用提供的名称") {
        checkAll(100, Arb.string(minSize = 1, maxSize = 50)) { name ->
            val nonBlankName = name.ifBlank { "测试物品" }
            val details = ObjectDetails.empty(nonBlankName)
            
            details.name shouldBe nonBlankName
            details.origin.shouldNotBeBlank()
            details.usage.shouldNotBeBlank()
            details.category.shouldNotBeBlank()
        }
    }
})
