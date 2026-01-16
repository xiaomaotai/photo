package com.ruolijianzhen.app.domain.stats

import com.ruolijianzhen.app.domain.history.HistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * 统计数据仓库接口
 */
interface StatsRepository {
    /**
     * 获取今日识别次数
     */
    fun getTodayCount(): Flow<Int>
    
    /**
     * 获取总识别次数
     */
    fun getTotalCount(): Flow<Int>
    
    /**
     * 获取最近识别的物品
     * @param limit 数量限制
     */
    fun getRecentItems(limit: Int = 3): Flow<List<HistoryItem>>
    
    /**
     * 获取收藏的物品
     * @param limit 数量限制
     */
    fun getFavoriteItems(limit: Int = 10): Flow<List<HistoryItem>>
    
    /**
     * 获取收藏数量
     */
    fun getFavoriteCount(): Flow<Int>
}
