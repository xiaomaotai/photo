package com.ruolijianzhen.app.data.history

import com.ruolijianzhen.app.data.local.dao.HistoryDao
import com.ruolijianzhen.app.data.local.entity.HistoryEntity
import com.ruolijianzhen.app.domain.history.HistoryItem
import com.ruolijianzhen.app.domain.history.HistoryRepository
import com.ruolijianzhen.app.domain.model.ObjectInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史记录仓库实现
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override suspend fun save(objectInfo: ObjectInfo, thumbnailPath: String?) = withContext(Dispatchers.IO) {
        val entity = HistoryEntity(
            id = objectInfo.id,
            name = objectInfo.name,
            aliases = JSONArray(objectInfo.aliases).toString(),
            origin = objectInfo.origin,
            usage = objectInfo.usage,
            category = objectInfo.category,
            confidence = objectInfo.confidence,
            source = objectInfo.source.name,
            thumbnailPath = thumbnailPath,
            timestamp = System.currentTimeMillis(),

            // 详细信息字段
            brand = objectInfo.brand,
            model = objectInfo.model,
            species = objectInfo.species,
            priceRange = objectInfo.priceRange,
            material = objectInfo.material,
            color = objectInfo.color,
            size = objectInfo.size,
            manufacturer = objectInfo.manufacturer,
            features = if (objectInfo.features.isNotEmpty()) JSONArray(objectInfo.features).toString() else null,

            // 百科知识字段
            summary = objectInfo.summary,
            description = objectInfo.description,
            historyText = objectInfo.history,
            funFacts = if (objectInfo.funFacts.isNotEmpty()) JSONArray(objectInfo.funFacts).toString() else null,
            tips = if (objectInfo.tips.isNotEmpty()) JSONArray(objectInfo.tips).toString() else null,
            relatedTopics = if (objectInfo.relatedTopics.isNotEmpty()) JSONArray(objectInfo.relatedTopics).toString() else null,

            // 分类特定字段
            objectType = objectInfo.objectType.name,
            typeSpecificInfo = if (objectInfo.typeSpecificInfo.isNotEmpty()) org.json.JSONObject(objectInfo.typeSpecificInfo).toString() else null,
            additionalInfo = if (objectInfo.additionalInfo.isNotEmpty()) org.json.JSONObject(objectInfo.additionalInfo).toString() else null
        )
        historyDao.insertHistory(entity)
    }
    
    override fun getAllFlow(): Flow<List<HistoryItem>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    override suspend fun getGroupedByDate(): Map<String, List<HistoryItem>> = withContext(Dispatchers.IO) {
        val allHistory = historyDao.getAllHistory()
        val result = mutableMapOf<String, MutableList<HistoryItem>>()
        
        // 获取今天和昨天的日期
        val today = Calendar.getInstance()
        val todayStr = dateFormat.format(today.time)
        
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(yesterday.time)
        
        // 这里需要收集Flow，但为了简化，我们直接查询
        // 实际使用时应该在ViewModel中处理Flow
        result
    }
    
    /**
     * 获取按日期分组的历史记录（同步版本）
     */
    suspend fun getGroupedByDateSync(): Map<String, List<HistoryItem>> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val yesterdayStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val result = linkedMapOf<String, List<HistoryItem>>()
        
        // 今天
        val todayHistory = historyDao.getHistoryByDateRange(todayStart, Long.MAX_VALUE)
        if (todayHistory.isNotEmpty()) {
            result["今天"] = todayHistory.map { it.toHistoryItem() }
        }
        
        // 昨天
        val yesterdayHistory = historyDao.getHistoryByDateRange(yesterdayStart, todayStart)
        if (yesterdayHistory.isNotEmpty()) {
            result["昨天"] = yesterdayHistory.map { it.toHistoryItem() }
        }
        
        // 更早
        val olderHistory = historyDao.getHistoryByDateRange(0, yesterdayStart)
        if (olderHistory.isNotEmpty()) {
            result["更早"] = olderHistory.map { it.toHistoryItem() }
        }
        
        result
    }
    
    override suspend fun getById(id: String): HistoryItem? = withContext(Dispatchers.IO) {
        historyDao.getHistoryById(id)?.toHistoryItem()
    }
    
    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        historyDao.deleteHistoryById(id)
    }
    
    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        historyDao.clearAllHistory()
    }
    
    override suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        historyDao.getHistoryCount()
    }
    
    override suspend fun updateFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        historyDao.updateFavorite(id, isFavorite)
    }
    
    override fun search(query: String): Flow<List<HistoryItem>> {
        return historyDao.searchHistory(query).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    override fun getByCategory(category: String): Flow<List<HistoryItem>> {
        return historyDao.getHistoryByCategory(category).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    override fun getAllCategories(): Flow<List<String>> {
        return historyDao.getAllCategories()
    }
    
    override fun getFavorites(): Flow<List<HistoryItem>> {
        return historyDao.getFavoriteItems(Int.MAX_VALUE).map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    override suspend fun cleanOldData(daysToKeep: Int): Int = withContext(Dispatchers.IO) {
        val cutoffTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToKeep)
        }.timeInMillis
        historyDao.deleteOldHistory(cutoffTime)
    }
    
    /**
     * 将Entity转换为HistoryItem
     */
    private fun HistoryEntity.toHistoryItem(): HistoryItem {
        val aliasesList = parseJsonArray(aliases)
        val featuresList = parseJsonArray(features)
        val funFactsList = parseJsonArray(funFacts)
        val tipsList = parseJsonArray(tips)
        val relatedTopicsList = parseJsonArray(relatedTopics)
        val typeSpecificMap = parseJsonMap(typeSpecificInfo)
        val additionalMap = parseJsonMap(additionalInfo)

        return HistoryItem(
            id = id,
            name = name,
            aliases = aliasesList,
            origin = origin,
            usage = usage,
            category = category,
            confidence = confidence,
            source = source,
            thumbnailPath = thumbnailPath,
            timestamp = timestamp,
            isFavorite = isFavorite,
            brand = brand,
            model = model,
            species = species,
            priceRange = priceRange,
            material = material,
            color = color,
            size = size,
            manufacturer = manufacturer,
            features = featuresList,
            summary = summary,
            description = description,
            historyText = historyText,
            funFacts = funFactsList,
            tips = tipsList,
            relatedTopics = relatedTopicsList,
            objectType = try {
                com.ruolijianzhen.app.domain.model.ObjectType.valueOf(objectType)
            } catch (e: Exception) {
                com.ruolijianzhen.app.domain.model.ObjectType.GENERAL
            },
            typeSpecificInfo = typeSpecificMap,
            additionalInfo = additionalMap
        )
    }

    private fun parseJsonArray(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseJsonMap(jsonString: String?): Map<String, String> {
        if (jsonString.isNullOrBlank()) return emptyMap()
        return try {
            val jsonObject = org.json.JSONObject(jsonString)
            val map = mutableMapOf<String, String>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
