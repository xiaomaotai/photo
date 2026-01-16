package com.ruolijianzhen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruolijianzhen.app.data.local.entity.ObjectEntity
import com.ruolijianzhen.app.data.local.entity.LearningObjectEntity

/**
 * 物品数据访问对象
 */
@Dao
interface ObjectDao {
    
    // 内置物品数据库操作
    @Query("SELECT * FROM objects WHERE label = :label")
    suspend fun getObjectByLabel(label: String): ObjectEntity?
    
    @Query("SELECT * FROM objects WHERE nameCn LIKE '%' || :keyword || '%' OR nameEn LIKE '%' || :keyword || '%'")
    suspend fun searchObjects(keyword: String): List<ObjectEntity>
    
    @Query("SELECT * FROM objects WHERE category = :category")
    suspend fun getObjectsByCategory(category: String): List<ObjectEntity>
    
    @Query("SELECT COUNT(*) FROM objects")
    suspend fun getObjectCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjects(objects: List<ObjectEntity>)
    
    // 学习数据库操作
    @Query("SELECT * FROM learning_objects WHERE label = :label")
    suspend fun getLearningObjectByLabel(label: String): LearningObjectEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningObject(obj: LearningObjectEntity)
    
    @Query("DELETE FROM learning_objects")
    suspend fun clearLearningObjects()
    
    @Query("SELECT COUNT(*) FROM learning_objects")
    suspend fun getLearningObjectCount(): Int
}
