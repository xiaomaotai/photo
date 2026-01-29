package com.ruolijianzhen.app.domain.history

import com.ruolijianzhen.app.domain.model.ObjectInfo
import kotlinx.coroutines.flow.Flow

/**
 * 历史记录仓库接口
 */
interface HistoryRepository {
    
    /**
     * 保存识别历史
     * @param objectInfo 识别结果
     * @param thumbnailPath 缩略图路径（可选）
     */
    suspend fun save(objectInfo: ObjectInfo, thumbnailPath: String? = null)
    
    /**
     * 获取所有历史记录
     */
    fun getAllFlow(): Flow<List<HistoryItem>>
    
    /**
     * 获取按日期分组的历史记录
     */
    suspend fun getGroupedByDate(): Map<String, List<HistoryItem>>
    
    /**
     * 根据ID获取历史记录
     */
    suspend fun getById(id: String): HistoryItem?
    
    /**
     * 删除指定历史记录
     */
    suspend fun delete(id: String)
    
    /**
     * 清空所有历史记录
     */
    suspend fun clearAll()
    
    /**
     * 获取历史记录数量
     */
    suspend fun getCount(): Int
    
    /**
     * 更新收藏状态
     */
    suspend fun updateFavorite(id: String, isFavorite: Boolean)
    
    /**
     * 搜索历史记录
     */
    fun search(query: String): Flow<List<HistoryItem>>
    
    /**
     * 按分类筛选
     */
    fun getByCategory(category: String): Flow<List<HistoryItem>>
    
    /**
     * 获取所有分类
     */
    fun getAllCategories(): Flow<List<String>>
    
    /**
     * 获取收藏的历史记录
     */
    fun getFavorites(): Flow<List<HistoryItem>>
    
    /**
     * 清理旧数据（保留收藏）
     * @param daysToKeep 保留天数
     * @return 删除的记录数
     */
    suspend fun cleanOldData(daysToKeep: Int = 30): Int
}

/**
 * 历史记录项
 * 保存完整的ObjectInfo信息
 */
data class HistoryItem(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val origin: String,
    val usage: String,
    val category: String,
    val confidence: Float,
    val source: String,
    val thumbnailPath: String? = null,
    val timestamp: Long,
    val isFavorite: Boolean = false,

    // 详细信息字段
    val brand: String? = null,
    val model: String? = null,
    val species: String? = null,
    val priceRange: String? = null,
    val material: String? = null,
    val color: String? = null,
    val size: String? = null,
    val manufacturer: String? = null,
    val features: List<String> = emptyList(),

    // 百科知识字段
    val summary: String? = null,
    val description: String? = null,
    val historyText: String? = null,
    val funFacts: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val relatedTopics: List<String> = emptyList(),

    // 分类特定字段
    val objectType: com.ruolijianzhen.app.domain.model.ObjectType = com.ruolijianzhen.app.domain.model.ObjectType.GENERAL,
    val typeSpecificInfo: Map<String, String> = emptyMap(),
    val additionalInfo: Map<String, String> = emptyMap()
) {
    /**
     * 转换为ObjectInfo
     */
    fun toObjectInfo(): com.ruolijianzhen.app.domain.model.ObjectInfo {
        return com.ruolijianzhen.app.domain.model.ObjectInfo(
            id = id,
            name = name,
            aliases = aliases,
            origin = origin,
            usage = usage,
            category = category,
            confidence = confidence,
            source = com.ruolijianzhen.app.domain.model.RecognitionSource.fromString(source),
            imageUrl = thumbnailPath,
            isFavorite = isFavorite,
            brand = brand,
            model = model,
            species = species,
            priceRange = priceRange,
            material = material,
            color = color,
            size = size,
            manufacturer = manufacturer,
            features = features,
            summary = summary,
            description = description,
            history = historyText,
            funFacts = funFacts,
            tips = tips,
            relatedTopics = relatedTopics,
            objectType = objectType,
            typeSpecificInfo = typeSpecificInfo,
            additionalInfo = additionalInfo
        )
    }
}
