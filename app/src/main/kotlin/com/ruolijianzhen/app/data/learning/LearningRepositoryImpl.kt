package com.ruolijianzhen.app.data.learning

import com.ruolijianzhen.app.data.local.dao.LearningDao
import com.ruolijianzhen.app.data.local.entity.LearningObjectEntity
import com.ruolijianzhen.app.domain.learning.LearningRepository
import com.ruolijianzhen.app.domain.model.ObjectDetails
import com.ruolijianzhen.app.domain.model.RecognitionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习数据仓库实现
 */
@Singleton
class LearningRepositoryImpl @Inject constructor(
    private val learningDao: LearningDao
) : LearningRepository {
    
    override suspend fun save(
        label: String,
        details: ObjectDetails,
        source: RecognitionSource
    ) = withContext(Dispatchers.IO) {
        val entity = LearningObjectEntity(
            label = label,
            nameCn = details.name,
            nameEn = details.nameEn,
            aliases = JSONArray(details.aliases).toString(),
            origin = details.origin,
            usage = details.usage,
            category = details.category,
            source = source.name
        )
        learningDao.insertOrUpdate(entity)
    }
    
    override suspend fun getByLabel(label: String): ObjectDetails? = withContext(Dispatchers.IO) {
        learningDao.getByLabel(label)?.toObjectDetails()
    }
    
    override suspend fun search(query: String): List<ObjectDetails> = withContext(Dispatchers.IO) {
        learningDao.searchByName(query).map { it.toObjectDetails() }
    }
    
    override fun getAllFlow(): Flow<List<ObjectDetails>> {
        return learningDao.getAllFlow().map { entities ->
            entities.map { it.toObjectDetails() }
        }
    }
    
    override suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        learningDao.getCount()
    }
    
    override suspend fun delete(label: String) = withContext(Dispatchers.IO) {
        learningDao.deleteByLabel(label)
    }
    
    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        learningDao.deleteAll()
    }
    
    override suspend fun cleanOldData(daysToKeep: Int) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())
        learningDao.deleteOlderThan(cutoffTime)
    }
    
    /**
     * 将Entity转换为ObjectDetails
     */
    private fun LearningObjectEntity.toObjectDetails(): ObjectDetails {
        val aliasesList = try {
            val jsonArray = JSONArray(aliases)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        
        return ObjectDetails(
            name = nameCn,
            nameEn = nameEn,
            aliases = aliasesList,
            origin = origin,
            usage = usage,
            category = category
        )
    }
}
