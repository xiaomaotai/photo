package com.ruolijianzhen.app.domain.model

/**
 * 离线识别结果
 */
data class OfflineResult(
    val label: String,                   // 识别标签
    val confidence: Float,               // 置信度 (0.0 - 1.0)
    val details: ObjectDetails?          // 物品详细信息
)

/**
 * API识别结果
 */
data class ApiResult(
    val name: String,                    // 物品名称
    val description: String,             // 描述
    val confidence: Float,               // 置信度
    val source: RecognitionSource,       // API来源
    val rawResponse: Map<String, Any>? = null // 原始响应
)

/**
 * 用户AI识别结果
 */
data class AiResult(
    val name: String,                    // 物品名称
    val description: String,             // AI生成的描述
    val aliases: List<String>,           // 别名
    val origin: String,                  // 来历
    val usage: String,                   // 用途
    val category: String,                // 分类
    val confidence: Float = 0.8f,        // 默认置信度
    // 详细信息字段
    val brand: String? = null,           // 品牌（如：罗技、雷蛇）
    val model: String? = null,           // 型号（如：K380、BlackWidow）
    val species: String? = null,         // 物种（动植物专用，如：麻雀、玫瑰）
    val priceRange: String? = null,      // 价格区间（如：100-200元）
    val material: String? = null,        // 材质（如：塑料、金属、木质）
    val color: String? = null,           // 颜色
    val size: String? = null,            // 尺寸规格
    val manufacturer: String? = null,    // 制造商/产地
    val features: List<String> = emptyList() // 特征列表
)
