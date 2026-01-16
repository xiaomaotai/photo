package com.ruolijianzhen.app.domain.priority

import com.ruolijianzhen.app.domain.model.PriorityConfig
import kotlinx.coroutines.flow.Flow

/**
 * 优先级配置存储接口
 */
interface PriorityConfigStore {
    /**
     * 保存优先级配置
     */
    suspend fun save(config: PriorityConfig)
    
    /**
     * 加载优先级配置
     */
    suspend fun load(): PriorityConfig?
    
    /**
     * 观察配置变化
     */
    fun observe(): Flow<PriorityConfig?>
    
    /**
     * 清除配置
     */
    suspend fun clear()
}
