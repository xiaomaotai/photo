package com.ruolijianzhen.app.data.recognition

import android.graphics.Bitmap
import com.ruolijianzhen.app.data.local.dao.ObjectDao
import com.ruolijianzhen.app.data.ml.TFLiteClassifier
import com.ruolijianzhen.app.domain.model.ObjectDetails
import com.ruolijianzhen.app.domain.model.OfflineResult
import com.ruolijianzhen.app.domain.recognition.OfflineRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线识别器实现
 * 基于TensorFlow Lite和MobileNet模型
 */
@Singleton
class OfflineRecognizerImpl @Inject constructor(
    private val classifier: TFLiteClassifier,
    private val objectDao: ObjectDao
) : OfflineRecognizer {
    
    companion object {
        // 置信度阈值，低于此值认为识别不可靠
        // 降低阈值以便返回更多结果，让用户至少能看到识别尝试
        const val CONFIDENCE_THRESHOLD = 0.1f
    }
    
    private var initialized = false
    
    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext true
        initialized = classifier.initialize()
        initialized
    }
    
    override fun isInitialized(): Boolean = initialized
    
    override suspend fun recognize(bitmap: Bitmap): OfflineResult? = withContext(Dispatchers.Default) {
        if (!initialized && !initialize()) {
            return@withContext null
        }
        
        // 获取分类结果
        val results = classifier.classifyTopK(bitmap, 5)
        if (results.isEmpty()) {
            return@withContext null
        }
        
        // 取置信度最高的结果
        val topResult = results.first()
        
        // 如果置信度太低，返回null
        if (topResult.confidence < CONFIDENCE_THRESHOLD) {
            return@withContext null
        }
        
        // 查询物品详情
        val details = getObjectDetails(topResult.label)
        
        OfflineResult(
            label = topResult.label,
            confidence = topResult.confidence,
            details = details
        )
    }
    
    override suspend fun getObjectDetails(label: String): ObjectDetails? = withContext(Dispatchers.IO) {
        // 先查询学习数据库
        val learningObject = objectDao.getLearningObjectByLabel(label)
        if (learningObject != null) {
            return@withContext ObjectDetails(
                name = learningObject.nameCn,
                nameEn = learningObject.nameEn,
                aliases = parseAliases(learningObject.aliases),
                origin = learningObject.origin,
                usage = learningObject.usage,
                category = learningObject.category
            )
        }
        
        // 再查询内置数据库
        val builtInObject = objectDao.getObjectByLabel(label)
        if (builtInObject != null) {
            return@withContext ObjectDetails(
                name = builtInObject.nameCn,
                nameEn = builtInObject.nameEn,
                aliases = parseAliases(builtInObject.aliases),
                origin = builtInObject.origin,
                usage = builtInObject.usage,
                category = builtInObject.category
            )
        }
        
        // 未找到详情，返回基于标签的默认信息
        ObjectDetails(
            name = formatLabelToName(label),
            nameEn = label,
            aliases = emptyList(),
            origin = "暂无来历信息",
            usage = "暂无用途信息",
            category = "未分类"
        )
    }
    
    /**
     * 解析别名JSON数组
     */
    private fun parseAliases(aliasesJson: String): List<String> {
        return try {
            if (aliasesJson.isBlank() || aliasesJson == "[]") {
                emptyList()
            } else {
                // 简单解析JSON数组格式 ["alias1", "alias2"]
                aliasesJson
                    .trim()
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 将标签格式化为可读名称
     * 例如: "golden_retriever" -> "金毛寻回犬"
     */
    private fun formatLabelToName(label: String): String {
        return label
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
}
