package com.ruolijianzhen.app.domain.learning

import com.ruolijianzhen.app.domain.model.ObjectDetails
import com.ruolijianzhen.app.domain.model.RecognitionSource
import kotlinx.coroutines.flow.Flow

/**
 * 学习数据仓库接口
 */
interface LearningRepository {
    
    /**
     * 保存学习数据
     * @param label 识别标签
     * @param details 物品详情
     * @param source 数据来源
     */
    suspend fun save(label: String, details: ObjectDetails, source: RecognitionSource)
    
    /**
     * 根据标签查询学习数据
     * @param label 识别标签
     * @return 物品详情，未找到返回null
     */
    suspend fun getByLabel(label: String): ObjectDetails?
    
    /**
     * 搜索学习数据
     * @param query 搜索关键词
     * @return 匹配的物品列表
     */
    suspend fun search(query: String): List<ObjectDetails>
    
    /**
     * 获取所有学习数据
     */
    fun getAllFlow(): Flow<List<ObjectDetails>>
    
    /**
     * 获取学习数据数量
     */
    suspend fun getCount(): Int
    
    /**
     * 删除指定标签的数据
     */
    suspend fun delete(label: String)
    
    /**
     * 清空所有学习数据
     */
    suspend fun clearAll()
    
    /**
     * 清理过期数据（超过指定天数）
     */
    suspend fun cleanOldData(daysToKeep: Int = 30)
}
