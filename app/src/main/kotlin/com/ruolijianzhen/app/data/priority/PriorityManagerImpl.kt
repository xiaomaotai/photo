package com.ruolijianzhen.app.data.priority

import android.util.Log
import com.ruolijianzhen.app.domain.model.PriorityConfig
import com.ruolijianzhen.app.domain.model.RecognitionMethod
import com.ruolijianzhen.app.domain.priority.PriorityConfigStore
import com.ruolijianzhen.app.domain.priority.PriorityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 优先级管理器实现
 */
@Singleton
class PriorityManagerImpl @Inject constructor(
    private val configStore: PriorityConfigStore
) : PriorityManager {
    
    companion object {
        private const val TAG = "PriorityManager"
    }
    
    override suspend fun getConfig(): PriorityConfig {
        return try {
            configStore.load() ?: PriorityConfig.DEFAULT
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config, using default", e)
            PriorityConfig.DEFAULT
        }
    }
    
    override suspend fun saveConfig(config: PriorityConfig) {
        try {
            configStore.save(config)
            Log.d(TAG, "Config saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config", e)
            throw e
        }
    }
    
    override fun getConfigFlow(): Flow<PriorityConfig> {
        return configStore.observe().map { config ->
            config ?: PriorityConfig.DEFAULT
        }
    }
    
    override suspend fun getEnabledMethodsInOrder(): List<RecognitionMethod> {
        return getConfig().getEnabledMethodsInOrder()
    }
    
    override suspend fun resetToDefault() {
        try {
            configStore.save(PriorityConfig.DEFAULT)
            Log.d(TAG, "Config reset to default")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset config", e)
            throw e
        }
    }
}
