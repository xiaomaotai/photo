package com.ruolijianzhen.app.data.ai

import android.graphics.Bitmap
import com.ruolijianzhen.app.data.preferences.UserAiConfigStore
import com.ruolijianzhen.app.domain.ai.UserAiService
import com.ruolijianzhen.app.domain.model.AiApiType
import com.ruolijianzhen.app.domain.model.AiResult
import com.ruolijianzhen.app.domain.model.UserAiConfig
import com.ruolijianzhen.app.domain.model.UserAiConfigList
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户AI服务实现
 * 根据配置类型选择对应的AI客户端
 * 支持多AI配置管理
 */
@Singleton
class UserAiServiceImpl @Inject constructor(
    private val configStore: UserAiConfigStore,
    private val geminiClient: GeminiClient,
    private val openAiClient: OpenAiCompatibleClient
) : UserAiService {
    
    override suspend fun recognize(bitmap: Bitmap): AiResult? {
        val config = configStore.getActiveConfig() ?: return null
        
        return when (config.apiType) {
            AiApiType.GOOGLE_GEMINI -> {
                geminiClient.recognize(
                    bitmap = bitmap,
                    apiKey = config.apiKey,
                    modelName = config.modelName
                )
            }
            AiApiType.OPENAI_COMPATIBLE -> {
                openAiClient.recognize(
                    bitmap = bitmap,
                    apiUrl = config.apiUrl,
                    apiKey = config.apiKey,
                    modelName = config.modelName
                )
            }
        }
    }
    
    override suspend fun isConfigured(): Boolean {
        return configStore.isConfigured()
    }
    
    override suspend fun saveConfig(config: UserAiConfig): Boolean {
        return try {
            configStore.saveConfig(config)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    override suspend fun getConfig(): UserAiConfig? {
        return configStore.getActiveConfig()
    }
    
    override suspend fun validateConfig(config: UserAiConfig): Boolean {
        return when (config.apiType) {
            AiApiType.GOOGLE_GEMINI -> {
                geminiClient.validateApiKey(config.apiKey)
            }
            AiApiType.OPENAI_COMPATIBLE -> {
                openAiClient.validateConfig(config.apiUrl, config.apiKey)
            }
        }
    }
    
    override suspend fun clearConfig() {
        configStore.clearConfig()
    }
    
    // ========== 多配置管理方法 ==========
    
    /**
     * 获取配置列表Flow
     */
    fun getConfigListFlow(): Flow<UserAiConfigList> {
        return configStore.configListFlow
    }
    
    /**
     * 获取所有配置
     */
    suspend fun getAllConfigs(): List<UserAiConfig> {
        return configStore.getAllConfigs()
    }
    
    /**
     * 添加配置
     */
    suspend fun addConfig(config: UserAiConfig): Boolean {
        return try {
            configStore.addConfig(config)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 更新配置
     */
    suspend fun updateConfig(config: UserAiConfig): Boolean {
        return try {
            configStore.updateConfig(config)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 删除配置
     */
    suspend fun deleteConfig(configId: String): Boolean {
        return try {
            configStore.deleteConfig(configId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 设置激活配置
     */
    suspend fun setActiveConfig(configId: String): Boolean {
        return try {
            configStore.setActiveConfig(configId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取当前激活配置ID
     */
    suspend fun getActiveConfigId(): String? {
        return configStore.getConfigList().activeConfigId
    }
}
