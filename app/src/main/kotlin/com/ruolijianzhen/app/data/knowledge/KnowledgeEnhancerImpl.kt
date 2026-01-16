package com.ruolijianzhen.app.data.knowledge

import android.util.Log
import com.ruolijianzhen.app.domain.knowledge.KnowledgeEnhancer
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.ObjectType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 知识增强器实现
 * 通过百度百科等来源丰富识别结果
 */
@Singleton
class KnowledgeEnhancerImpl @Inject constructor(
    private val baikeClient: BaikeClient
) : KnowledgeEnhancer {
    
    companion object {
        private const val TAG = "KnowledgeEnhancer"
    }
    
    override suspend fun enhance(objectInfo: ObjectInfo): ObjectInfo {
        Log.d(TAG, "Enhancing knowledge for: ${objectInfo.name}")
        
        // 获取百科信息
        val baikeInfo = baikeClient.getKnowledge(objectInfo.name)
        
        if (baikeInfo == null) {
            Log.d(TAG, "No baike info found, returning original")
            // 即使没有百科信息，也要设置物品类型
            return objectInfo.copy(
                objectType = ObjectType.fromCategory(objectInfo.category)
            )
        }
        
        Log.d(TAG, "Got baike info: ${baikeInfo.title}")
        
        // 推断物品类型
        val objectType = ObjectType.fromCategory(objectInfo.category)
        
        // 从百科基本信息中提取类型特定信息
        val typeSpecificInfo = extractTypeSpecificInfo(baikeInfo.basicInfo, objectType)
        
        // 生成趣味知识和使用技巧
        val funFacts = generateFunFacts(baikeInfo, objectType)
        val tips = generateTips(objectType, objectInfo.name)
        
        // 生成延伸阅读主题
        val relatedTopics = generateRelatedTopics(objectInfo.name, objectType)
        
        return objectInfo.copy(
            summary = baikeInfo.summary.ifBlank { objectInfo.summary },
            description = baikeInfo.description.ifBlank { objectInfo.description },
            imageUrl = baikeInfo.imageUrl ?: objectInfo.imageUrl,
            objectType = objectType,
            typeSpecificInfo = typeSpecificInfo,
            funFacts = funFacts,
            tips = tips,
            relatedTopics = relatedTopics,
            // 从百科基本信息中提取更多字段
            origin = extractFromBasicInfo(baikeInfo.basicInfo, listOf("产地", "原产地", "起源", "发源地")) 
                ?: objectInfo.origin,
            manufacturer = extractFromBasicInfo(baikeInfo.basicInfo, listOf("生产商", "制造商", "厂商", "品牌"))
                ?: objectInfo.manufacturer,
            material = extractFromBasicInfo(baikeInfo.basicInfo, listOf("材质", "材料", "成分"))
                ?: objectInfo.material
        )
    }
    
    /**
     * 从基本信息中提取值
     */
    private fun extractFromBasicInfo(basicInfo: Map<String, String>, keys: List<String>): String? {
        for (key in keys) {
            val value = basicInfo.entries.find { it.key.contains(key) }?.value
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return null
    }
    
    /**
     * 提取类型特定信息
     */
    private fun extractTypeSpecificInfo(
        basicInfo: Map<String, String>,
        objectType: ObjectType
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        when (objectType) {
            ObjectType.ELECTRONICS -> {
                extractIfExists(basicInfo, result, "屏幕尺寸", "屏幕")
                extractIfExists(basicInfo, result, "处理器", "CPU")
                extractIfExists(basicInfo, result, "内存", "RAM")
                extractIfExists(basicInfo, result, "存储", "容量")
                extractIfExists(basicInfo, result, "电池", "续航")
            }
            ObjectType.ANIMAL -> {
                extractIfExists(basicInfo, result, "界", "门", "纲", "目", "科", "属", "种")
                extractIfExists(basicInfo, result, "分布区域", "栖息地")
                extractIfExists(basicInfo, result, "保护级别", "保护等级")
                extractIfExists(basicInfo, result, "寿命", "生命周期")
            }
            ObjectType.PLANT -> {
                extractIfExists(basicInfo, result, "界", "门", "纲", "目", "科", "属", "种")
                extractIfExists(basicInfo, result, "花期", "开花时间")
                extractIfExists(basicInfo, result, "分布区域", "产地")
                extractIfExists(basicInfo, result, "习性", "生长习性")
            }
            ObjectType.FOOD -> {
                extractIfExists(basicInfo, result, "热量", "卡路里")
                extractIfExists(basicInfo, result, "营养成分", "营养")
                extractIfExists(basicInfo, result, "产地", "原产地")
                extractIfExists(basicInfo, result, "口味", "风味")
            }
            else -> {
                // 通用：提取所有基本信息
                basicInfo.forEach { (key, value) ->
                    if (value.isNotBlank() && value.length < 100) {
                        result[key] = value
                    }
                }
            }
        }
        
        return result
    }
    
    private fun extractIfExists(
        source: Map<String, String>,
        target: MutableMap<String, String>,
        vararg keys: String
    ) {
        for (key in keys) {
            source.entries.find { it.key.contains(key) }?.let {
                if (it.value.isNotBlank()) {
                    target[it.key] = it.value
                }
            }
        }
    }
    
    /**
     * 生成趣味知识
     */
    private fun generateFunFacts(baikeInfo: BaikeInfo, objectType: ObjectType): List<String> {
        val facts = mutableListOf<String>()
        
        // 从摘要中提取有趣的句子
        val summary = baikeInfo.summary
        if (summary.length > 50) {
            // 提取第一句作为核心知识
            val firstSentence = summary.split("。", "！", "？").firstOrNull()
            if (!firstSentence.isNullOrBlank() && firstSentence.length > 10) {
                facts.add(firstSentence)
            }
        }
        
        return facts.take(3)
    }
    
    /**
     * 生成使用技巧/注意事项
     */
    private fun generateTips(objectType: ObjectType, name: String): List<String> {
        return when (objectType) {
            ObjectType.ELECTRONICS -> listOf(
                "使用前请阅读说明书",
                "避免在潮湿环境中使用",
                "定期清洁保养延长使用寿命"
            )
            ObjectType.PLANT -> listOf(
                "注意光照和浇水频率",
                "定期施肥促进生长",
                "注意病虫害防治"
            )
            ObjectType.ANIMAL -> listOf(
                "了解其习性有助于更好地观察",
                "保持安全距离，不要惊扰",
                "保护野生动物，人人有责"
            )
            ObjectType.FOOD -> listOf(
                "注意保质期和储存条件",
                "适量食用，均衡营养",
                "了解过敏原信息"
            )
            else -> emptyList()
        }
    }
    
    /**
     * 生成延伸阅读主题
     */
    private fun generateRelatedTopics(name: String, objectType: ObjectType): List<String> {
        return when (objectType) {
            ObjectType.ELECTRONICS -> listOf(
                "${name}使用教程",
                "${name}评测对比",
                "如何选购${name}"
            )
            ObjectType.ANIMAL -> listOf(
                "${name}的生活习性",
                "${name}的保护现状",
                "与${name}相似的物种"
            )
            ObjectType.PLANT -> listOf(
                "${name}的养护方法",
                "${name}的药用价值",
                "${name}的文化寓意"
            )
            ObjectType.FOOD -> listOf(
                "${name}的做法大全",
                "${name}的营养价值",
                "${name}的挑选技巧"
            )
            else -> listOf(
                "了解更多关于${name}",
                "${name}的历史文化",
                "${name}的相关知识"
            )
        }
    }
}
