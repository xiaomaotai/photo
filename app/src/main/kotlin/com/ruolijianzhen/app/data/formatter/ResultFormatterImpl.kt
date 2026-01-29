package com.ruolijianzhen.app.data.formatter

import com.ruolijianzhen.app.domain.formatter.ResultFormatter
import com.ruolijianzhen.app.domain.model.AiResult
import com.ruolijianzhen.app.domain.model.ApiResult
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.OfflineResult
import com.ruolijianzhen.app.domain.model.RecognitionSource
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 结果格式化器实现
 * 将不同来源的识别结果统一为标准ObjectInfo格式
 * 确保所有必需字段都有值，缺失字段使用默认占位符
 */
@Singleton
class ResultFormatterImpl @Inject constructor() : ResultFormatter {
    
    companion object {
        const val DEFAULT_PLACEHOLDER = "暂无信息"
        const val DEFAULT_CATEGORY = "未分类"
    }
    
    /**
     * 格式化离线识别结果
     */
    override fun format(result: OfflineResult): ObjectInfo {
        val details = result.details
        
        return ObjectInfo(
            id = generateId(),
            name = ensureNotBlank(details?.name, formatLabelToName(result.label)),
            aliases = details?.aliases ?: emptyList(),
            origin = ensureNotBlank(details?.origin),
            usage = ensureNotBlank(details?.usage),
            category = ensureNotBlank(details?.category, DEFAULT_CATEGORY),
            confidence = result.confidence,
            source = RecognitionSource.OFFLINE,
            imageUrl = null,
            // 不显示技术字段
            additionalInfo = emptyMap()
        )
    }
    
    /**
     * 格式化API识别结果
     */
    override fun format(result: ApiResult): ObjectInfo {
        // 从描述中尝试提取信息
        val parsedInfo = parseDescription(result.description)
        
        // 从rawResponse中提取分类信息（百度API的root字段）
        val categoryFromApi = result.rawResponse?.get("root")?.toString()
        val finalCategory = if (!categoryFromApi.isNullOrBlank() && categoryFromApi != "null") {
            categoryFromApi
        } else {
            parsedInfo.category
        }
        
        return ObjectInfo(
            id = generateId(),
            name = ensureNotBlank(result.name),
            aliases = parsedInfo.aliases,
            origin = ensureNotBlank(parsedInfo.origin),
            usage = ensureNotBlank(parsedInfo.usage),
            category = ensureNotBlank(finalCategory, DEFAULT_CATEGORY),
            confidence = result.confidence,
            source = result.source,
            imageUrl = null,
            // 不再将原始API响应字段放入additionalInfo，避免显示root/keyword等技术字段
            additionalInfo = emptyMap()
        )
    }
    
    /**
     * 格式化用户AI识别结果
     */
    override fun format(result: AiResult): ObjectInfo {
        return ObjectInfo(
            id = generateId(),
            name = ensureNotBlank(result.name),
            aliases = result.aliases,
            origin = ensureNotBlank(result.origin),
            usage = ensureNotBlank(result.usage),
            category = ensureNotBlank(result.category, DEFAULT_CATEGORY),
            confidence = result.confidence,
            source = RecognitionSource.USER_AI,
            imageUrl = null,
            additionalInfo = emptyMap(),
            brand = result.brand,
            model = result.model,
            species = result.species,
            priceRange = result.priceRange,
            material = result.material,
            color = result.color,
            size = result.size,
            manufacturer = result.manufacturer,
            features = result.features
        )
    }
    
    /**
     * 生成唯一ID
     */
    private fun generateId(): String = UUID.randomUUID().toString()
    
    /**
     * 确保字符串不为空，为空则使用默认值
     */
    private fun ensureNotBlank(value: String?, default: String = DEFAULT_PLACEHOLDER): String {
        return if (value.isNullOrBlank()) default else value
    }
    
    /**
     * 将标签格式化为可读名称
     * 过滤掉技术性参数名（如 m25_nameC）
     */
    private fun formatLabelToName(label: String): String {
        // 检测是否为技术性参数名（包含数字+字母组合如 m25_nameC, n01_xxx 等）
        val technicalPattern = Regex("^[a-z]\\d+[_-]|^\\d+[_-]|[A-Z]$")
        if (technicalPattern.containsMatchIn(label)) {
            return "未知物品"
        }

        return label
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * 从API描述中解析信息
     */
    private fun parseDescription(description: String): ParsedInfo {
        // 简单的关键词匹配解析
        val aliases = mutableListOf<String>()
        var origin = DEFAULT_PLACEHOLDER
        var usage = DEFAULT_PLACEHOLDER
        var category = DEFAULT_CATEGORY
        
        // 尝试从描述中提取信息
        val lines = description.split("\n", "。", "；")
        
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.contains("别名") || trimmed.contains("又称") || trimmed.contains("也叫") -> {
                    val aliasText = trimmed
                        .replace("别名", "")
                        .replace("又称", "")
                        .replace("也叫", "")
                        .replace("：", "")
                        .replace(":", "")
                        .trim()
                    aliases.addAll(aliasText.split("、", ",", "，").map { it.trim() }.filter { it.isNotBlank() })
                }
                trimmed.contains("起源") || trimmed.contains("来历") || trimmed.contains("历史") -> {
                    origin = trimmed
                }
                trimmed.contains("用途") || trimmed.contains("用于") || trimmed.contains("作用") -> {
                    usage = trimmed
                }
                trimmed.contains("类别") || trimmed.contains("分类") || trimmed.contains("属于") -> {
                    category = trimmed
                        .replace("类别", "")
                        .replace("分类", "")
                        .replace("属于", "")
                        .replace("：", "")
                        .replace(":", "")
                        .trim()
                        .ifBlank { DEFAULT_CATEGORY }
                }
            }
        }
        
        // 如果没有解析到具体信息，使用整个描述作为用途说明
        if (usage == DEFAULT_PLACEHOLDER && description.isNotBlank()) {
            usage = description.take(200)
        }
        
        return ParsedInfo(aliases, origin, usage, category)
    }
    
    /**
     * 解析后的信息
     */
    private data class ParsedInfo(
        val aliases: List<String>,
        val origin: String,
        val usage: String,
        val category: String
    )
}
