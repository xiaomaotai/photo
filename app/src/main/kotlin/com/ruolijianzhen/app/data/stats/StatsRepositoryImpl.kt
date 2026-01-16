package com.ruolijianzhen.app.data.stats

import com.ruolijianzhen.app.data.local.dao.HistoryDao
import com.ruolijianzhen.app.domain.history.HistoryItem
import com.ruolijianzhen.app.domain.stats.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统计数据仓库实现
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : StatsRepository {
    
    override fun getTodayCount(): Flow<Int> {
        val todayStart = getTodayStartTimestamp()
        return historyDao.getCountSince(todayStart)
    }
    
    override fun getTotalCount(): Flow<Int> {
        return historyDao.getTotalCount()
    }
    
    override fun getRecentItems(limit: Int): Flow<List<HistoryItem>> {
        return historyDao.getRecentItems(limit).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    override fun getFavoriteItems(limit: Int): Flow<List<HistoryItem>> {
        return historyDao.getFavoriteItems(limit).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    override fun getFavoriteCount(): Flow<Int> {
        return historyDao.getFavoriteCount()
    }
    
    /**
     * 获取今天0点的时间戳
     */
    private fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    /**
     * 转换为HistoryItem
     */
    private fun com.ruolijianzhen.app.data.local.entity.HistoryEntity.toHistoryItem(): HistoryItem {
        return HistoryItem(
            id = id,
            name = name,
            aliases = aliases.split(",").filter { it.isNotBlank() },
            origin = origin,
            usage = usage,
            category = category,
            confidence = confidence,
            source = source,
            timestamp = timestamp,
            isFavorite = isFavorite
        )
    }
}
