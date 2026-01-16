package com.ruolijianzhen.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ruolijianzhen.app.domain.model.AiApiType
import com.ruolijianzhen.app.domain.model.UserAiConfig
import com.ruolijianzhen.app.domain.model.UserAiConfigList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userAiDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_ai_config")

/**
 * 用户AI配置存储
 * 支持多个AI配置的存储和管理
 */
@Singleton
class UserAiConfigStore @Inject constructor(
    private val context: Context
) {
    companion object {
        // 旧版单配置键（用于迁移）
        private val KEY_API_TYPE = stringPreferencesKey("api_type")
        private val KEY_API_URL = stringPreferencesKey("api_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        
        // 新版多配置键
        private val KEY_CONFIG_LIST = stringPreferencesKey("config_list_json")
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    /**
     * 获取配置列表Flow
     */
    val configListFlow: Flow<UserAiConfigList> = context.userAiDataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_CONFIG_LIST]
        if (jsonStr != null) {
            try {
                val stored = json.decodeFromString<StoredConfigList>(jsonStr)
                stored.toUserAiConfigList()
            } catch (e: Exception) {
                UserAiConfigList()
            }
        } else {
            // 尝试迁移旧版单配置
            migrateOldConfig(preferences)
        }
    }
    
    /**
     * 获取当前激活配置Flow（兼容旧接口）
     */
    val configFlow: Flow<UserAiConfig?> = configListFlow.map { it.getActiveConfig() }
    
    /**
     * 迁移旧版单配置到新版多配置
     */
    private fun migrateOldConfig(preferences: Preferences): UserAiConfigList {
        val apiType = preferences[KEY_API_TYPE] ?: return UserAiConfigList()
        val apiUrl = preferences[KEY_API_URL] ?: return UserAiConfigList()
        val apiKey = preferences[KEY_API_KEY] ?: return UserAiConfigList()
        val modelName = preferences[KEY_MODEL_NAME] ?: return UserAiConfigList()
        
        val oldConfig = UserAiConfig(
            name = "默认配置",
            apiType = AiApiType.fromString(apiType),
            apiUrl = apiUrl,
            apiKey = apiKey,
            modelName = modelName
        )
        
        return UserAiConfigList(
            configs = listOf(oldConfig),
            activeConfigId = oldConfig.id
        )
    }
    
    /**
     * 获取配置列表
     */
    suspend fun getConfigList(): UserAiConfigList {
        return configListFlow.first()
    }
    
    /**
     * 保存配置列表
     */
    private suspend fun saveConfigList(configList: UserAiConfigList) {
        context.userAiDataStore.edit { preferences ->
            val stored = StoredConfigList.fromUserAiConfigList(configList)
            preferences[KEY_CONFIG_LIST] = json.encodeToString(stored)
            // 清除旧版键
            preferences.remove(KEY_API_TYPE)
            preferences.remove(KEY_API_URL)
            preferences.remove(KEY_API_KEY)
            preferences.remove(KEY_MODEL_NAME)
        }
    }
    
    /**
     * 添加配置
     */
    suspend fun addConfig(config: UserAiConfig) {
        val currentList = getConfigList()
        val newList = currentList.addConfig(config)
        saveConfigList(newList)
    }
    
    /**
     * 更新配置
     */
    suspend fun updateConfig(config: UserAiConfig) {
        val currentList = getConfigList()
        val newList = currentList.updateConfig(config)
        saveConfigList(newList)
    }
    
    /**
     * 删除配置
     */
    suspend fun deleteConfig(configId: String) {
        val currentList = getConfigList()
        val newList = currentList.deleteConfig(configId)
        saveConfigList(newList)
    }
    
    /**
     * 设置激活配置
     */
    suspend fun setActiveConfig(configId: String) {
        val currentList = getConfigList()
        val newList = currentList.setActiveConfig(configId)
        saveConfigList(newList)
    }
    
    /**
     * 获取所有配置
     */
    suspend fun getAllConfigs(): List<UserAiConfig> {
        return getConfigList().configs
    }
    
    /**
     * 获取当前激活配置
     */
    suspend fun getActiveConfig(): UserAiConfig? {
        return getConfigList().getActiveConfig()
    }
    
    /**
     * 保存配置（兼容旧接口，添加或更新）
     */
    suspend fun saveConfig(config: UserAiConfig) {
        val currentList = getConfigList()
        val existingConfig = currentList.configs.find { it.id == config.id }
        if (existingConfig != null) {
            updateConfig(config)
        } else {
            addConfig(config)
        }
    }
    
    /**
     * 获取当前配置（兼容旧接口）
     */
    suspend fun getConfig(): UserAiConfig? {
        return getActiveConfig()
    }
    
    /**
     * 清除配置
     */
    suspend fun clearConfig() {
        context.userAiDataStore.edit { preferences ->
            preferences.remove(KEY_CONFIG_LIST)
            preferences.remove(KEY_API_TYPE)
            preferences.remove(KEY_API_URL)
            preferences.remove(KEY_API_KEY)
            preferences.remove(KEY_MODEL_NAME)
        }
    }
    
    /**
     * 检查是否已配置
     */
    suspend fun isConfigured(): Boolean {
        return getActiveConfig() != null
    }
}

/**
 * 用于JSON序列化的配置存储类
 */
@Serializable
private data class StoredConfig(
    val id: String,
    val name: String,
    val apiType: String,
    val apiUrl: String,
    val apiKey: String,
    val modelName: String
) {
    fun toUserAiConfig(): UserAiConfig {
        return UserAiConfig(
            id = id,
            name = name,
            apiType = AiApiType.fromString(apiType),
            apiUrl = apiUrl,
            apiKey = apiKey,
            modelName = modelName
        )
    }
    
    companion object {
        fun fromUserAiConfig(config: UserAiConfig): StoredConfig {
            return StoredConfig(
                id = config.id,
                name = config.name,
                apiType = config.apiType.name,
                apiUrl = config.apiUrl,
                apiKey = config.apiKey,
                modelName = config.modelName
            )
        }
    }
}

@Serializable
private data class StoredConfigList(
    val configs: List<StoredConfig>,
    val activeConfigId: String?
) {
    fun toUserAiConfigList(): UserAiConfigList {
        return UserAiConfigList(
            configs = configs.map { it.toUserAiConfig() },
            activeConfigId = activeConfigId
        )
    }
    
    companion object {
        fun fromUserAiConfigList(configList: UserAiConfigList): StoredConfigList {
            return StoredConfigList(
                configs = configList.configs.map { StoredConfig.fromUserAiConfig(it) },
                activeConfigId = configList.activeConfigId
            )
        }
    }
}
