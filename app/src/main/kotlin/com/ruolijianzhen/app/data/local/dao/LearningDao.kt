package com.ruolijianzhen.app.data.local.dao

import androidx.room.*
import com.ruolijianzhen.app.data.local.entity.LearningObjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * 学习数据DAO
 */
@Dao
interface LearningDao {
    
    /**
     * 插入或更新学习数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: LearningObjectEntity)
    
    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LearningObjectEntity>)
    
    /**
     * 根据标签查询
     */
    @Query("SELECT * FROM learning_objects WHERE label = :label")
    suspend fun getByLabel(label: String): LearningObjectEntity?
    
    /**
     * 根据名称模糊查询
     */
    @Query("SELECT * FROM learning_objects WHERE nameCn LIKE '%' || :name || '%' OR nameEn LIKE '%' || :name || '%'")
    suspend fun searchByName(name: String): List<LearningObjectEntity>
    
    /**
     * 获取所有学习数据
     */
    @Query("SELECT * FROM learning_objects ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<LearningObjectEntity>>
    
    /**
     * 获取所有学习数据（非Flow）
     */
    @Query("SELECT * FROM learning_objects ORDER BY createdAt DESC")
    suspend fun getAll(): List<LearningObjectEntity>
    
    /**
     * 获取学习数据数量
     */
    @Query("SELECT COUNT(*) FROM learning_objects")
    suspend fun getCount(): Int
    
    /**
     * 删除指定标签的数据
     */
    @Query("DELETE FROM learning_objects WHERE label = :label")
    suspend fun deleteByLabel(label: String)
    
    /**
     * 清空所有学习数据
     */
    @Query("DELETE FROM learning_objects")
    suspend fun deleteAll()
    
    /**
     * 删除指定时间之前的数据
     */
    @Query("DELETE FROM learning_objects WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
