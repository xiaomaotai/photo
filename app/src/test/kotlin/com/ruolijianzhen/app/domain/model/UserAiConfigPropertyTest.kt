package com.ruolijianzhen.app.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * UserAiConfig属性测试
 * 
 * **Feature: object-recognition-app, Property 7: 用户配置持久化round-trip**
 * **Validates: Requirements 5.3**
 * 
 * *For any* UserAiConfig对象，保存后再读取应该得到等价的配置。
 */
class UserAiConfigPropertyTest : FunSpec({
    
    // 生成随机UserAiConfig
    val userAiConfigArb = Arb.bind(
        Arb.enum<AiApiType>(),
        Arb.string(10..100),
        Arb.string(20..100),
        Arb.string(5..50)
    ) { apiType, apiUrl, apiKey, modelName ->
        UserAiConfig(
            apiType = apiType,
            apiUrl = apiUrl.ifBlank { "https://api.example.com" },
            apiKey = apiKey.ifBlank { "sk-test-key" },
            modelName = modelName.ifBlank { "gpt-4" }
        )
    }
    
    test("Property 7: UserAiConfig序列化后反序列化应得到等价对象") {
        checkAll(100, userAiConfigArb) { config ->
            // 模拟序列化（转为Map）
            val serialized = mapOf(
                "apiType" to config.apiType.name,
                "apiUrl" to config.apiUrl,
                "apiKey" to config.apiKey,
                "modelName" to config.modelName
            )
            
            // 模拟反序列化
            val deserialized = UserAiConfig(
                apiType = AiApiType.fromString(serialized["apiType"]!!),
                apiUrl = serialized["apiUrl"]!!,
                apiKey = serialized["apiKey"]!!,
                modelName = serialized["modelName"]!!
            )
            
            // 验证等价性
            deserialized.apiType shouldBe config.apiType
            deserialized.apiUrl shouldBe config.apiUrl
            deserialized.apiKey shouldBe config.apiKey
            deserialized.modelName shouldBe config.modelName
        }
    }
    
    test("Property 7: AiApiType.fromString应正确解析所有枚举值") {
        AiApiType.entries.forEach { type ->
            val parsed = AiApiType.fromString(type.name)
            parsed shouldBe type
        }
    }
    
    test("Property 7: AiApiType.fromString对未知值应返回默认值") {
        val unknown = AiApiType.fromString("UNKNOWN_TYPE")
        unknown shouldBe AiApiType.OPENAI_COMPATIBLE
    }
    
    test("Property 7: UserAiConfig.gemini工厂方法应创建正确配置") {
        checkAll(100, Arb.string(20..100)) { apiKey ->
            val nonBlankKey = apiKey.ifBlank { "test-api-key" }
            val config = UserAiConfig.gemini(nonBlankKey)
            
            config.apiType shouldBe AiApiType.GOOGLE_GEMINI
            config.apiKey shouldBe nonBlankKey
            config.apiUrl shouldNotBe null
            config.modelName shouldNotBe null
        }
    }
    
    test("Property 7: UserAiConfig.openAiCompatible工厂方法应创建正确配置") {
        checkAll(100, Arb.string(20..100), Arb.string(10..100)) { apiKey, apiUrl ->
            val nonBlankKey = apiKey.ifBlank { "test-api-key" }
            val nonBlankUrl = apiUrl.ifBlank { "https://api.openai.com/v1" }
            val config = UserAiConfig.openAiCompatible(
                apiUrl = nonBlankUrl,
                apiKey = nonBlankKey
            )
            
            config.apiType shouldBe AiApiType.OPENAI_COMPATIBLE
            config.apiKey shouldBe nonBlankKey
            config.apiUrl shouldBe nonBlankUrl
            config.modelName shouldNotBe null
        }
    }
    
    test("Property 7: 配置字段不应为空") {
        checkAll(100, userAiConfigArb) { config ->
            config.apiUrl.isNotBlank() shouldBe true
            config.apiKey.isNotBlank() shouldBe true
            config.modelName.isNotBlank() shouldBe true
        }
    }
})
