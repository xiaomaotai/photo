package com.ruolijianzhen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruolijianzhen.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 历史记录数据访问对象
 */
@Dao
interface HistoryDao {
    
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getHistoryById(id: String): HistoryEntity?
    
    @Query("SELECT * FROM history WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    suspend fun getHistoryByDateRange(startTime: Long, endTime: Long): List<HistoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)
    
    @Delete
    suspend fun deleteHistory(history: HistoryEntity)
    
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)
    
    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
    
    @Query("SELECT COUNT(*) FROM history")
    suspend fun getHistoryCount(): Int
    
    @Query("UPDATE history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)
    
    // ========== 统计查询 ==========
    
    @Query("SELECT COUNT(*) FROM history WHERE timestamp >= :since")
    fun getCountSince(since: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM history")
    fun getTotalCount(): Flow<Int>
    
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentItems(limit: Int): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE isFavorite = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getFavoriteItems(limit: Int): Flow<List<HistoryEntity>>
    
    @Query("SELECT COUNT(*) FROM history WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>
    
    // ========== 搜索和筛选 ==========
    
    @Query("SELECT * FROM history WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE category = :category ORDER BY timestamp DESC")
    fun getHistoryByCategory(category: String): Flow<List<HistoryEntity>>
    
    @Query("SELECT DISTINCT category FROM history ORDER BY category")
    fun getAllCategories(): Flow<List<String>>
    
    // ========== 数据清理 ==========
    
    @Query("DELETE FROM history WHERE timestamp < :before AND isFavorite = 0")
    suspend fun deleteOldHistory(before: Long): Int
}
