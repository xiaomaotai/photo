package com.ruolijianzhen.app.domain.model

import java.util.UUID

/**
 * 用户AI配置
 * 仅支持两种API格式：Google Gemini 和 OpenAI兼容格式
 */
data class UserAiConfig(
    val id: String = UUID.randomUUID().toString(),  // 唯一标识
    val name: String = "",                           // 配置名称（用户自定义）
    val apiType: AiApiType,                          // API类型
    val apiUrl: String,                              // API地址
    val apiKey: String,                              // API密钥
    val modelName: String                            // 模型名称
) {
    companion object {
        /**
         * 创建Google Gemini配置
         */
        fun gemini(
            apiKey: String, 
            modelName: String = "gemini-pro-vision",
            name: String = "Gemini"
        ): UserAiConfig {
            return UserAiConfig(
                name = name,
                apiType = AiApiType.GOOGLE_GEMINI,
                apiUrl = "https://generativelanguage.googleapis.com/v1beta",
                apiKey = apiKey,
                modelName = modelName
            )
        }
        
        /**
         * 创建OpenAI兼容配置
         */
        fun openAiCompatible(
            apiUrl: String = "https://api.openai.com/v1",
            apiKey: String,
            modelName: String = "gpt-4-vision-preview",
            name: String = "OpenAI"
        ): UserAiConfig {
            return UserAiConfig(
                name = name,
                apiType = AiApiType.OPENAI_COMPATIBLE,
                apiUrl = apiUrl,
                apiKey = apiKey,
                modelName = modelName
            )
        }
    }
}

/**
 * 用户AI配置列表
 * 存储多个AI配置和当前激活的配置ID
 */
data class UserAiConfigList(
    val configs: List<UserAiConfig> = emptyList(),
    val activeConfigId: String? = null
) {
    /**
     * 获取当前激活的配置
     */
    fun getActiveConfig(): UserAiConfig? {
        return configs.find { it.id == activeConfigId }
    }
    
    /**
     * 添加配置
     */
    fun addConfig(config: UserAiConfig): UserAiConfigList {
        val newConfigs = configs + config
        // 如果是第一个配置，自动设为激活
        val newActiveId = if (configs.isEmpty()) config.id else activeConfigId
        return copy(configs = newConfigs, activeConfigId = newActiveId)
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(config: UserAiConfig): UserAiConfigList {
        val newConfigs = configs.map { if (it.id == config.id) config else it }
        return copy(configs = newConfigs)
    }
    
    /**
     * 删除配置
     */
    fun deleteConfig(configId: String): UserAiConfigList {
        val newConfigs = configs.filter { it.id != configId }
        // 如果删除的是激活配置，选择第一个作为新激活配置
        val newActiveId = if (activeConfigId == configId) {
            newConfigs.firstOrNull()?.id
        } else {
            activeConfigId
        }
        return copy(configs = newConfigs, activeConfigId = newActiveId)
    }
    
    /**
     * 设置激活配置
     */
    fun setActiveConfig(configId: String): UserAiConfigList {
        return if (configs.any { it.id == configId }) {
            copy(activeConfigId = configId)
        } else {
            this
        }
    }
}

/**
 * AI API类型枚举
 */
enum class AiApiType(val displayName: String) {
    GOOGLE_GEMINI("Google Gemini"),
    OPENAI_COMPATIBLE("OpenAI兼容");
    
    companion object {
        fun fromString(value: String): AiApiType {
            return entries.find { it.name == value } ?: OPENAI_COMPATIBLE
        }
    }
}
