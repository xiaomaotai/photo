package com.ruolijianzhen.app.domain.priority

import com.ruolijianzhen.app.domain.model.PriorityConfig
import com.ruolijianzhen.app.domain.model.RecognitionMethod
import kotlinx.coroutines.flow.Flow

/**
 * 优先级管理器接口
 */
interface PriorityManager {
    /**
     * 获取当前优先级配置
     */
    suspend fun getConfig(): PriorityConfig
    
    /**
     * 保存优先级配置
     */
    suspend fun saveConfig(config: PriorityConfig)
    
    /**
     * 获取配置Flow（用于UI响应式更新）
     */
    fun getConfigFlow(): Flow<PriorityConfig>
    
    /**
     * 获取按优先级排序的已启用识别方式列表
     */
    suspend fun getEnabledMethodsInOrder(): List<RecognitionMethod>
    
    /**
     * 重置为默认配置
     */
    suspend fun resetToDefault()
}
