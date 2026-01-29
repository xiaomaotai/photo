package com.ruolijianzhen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 识别历史记录实体
 * 保存ObjectInfo的所有关键字段，确保历史记录显示完整
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,                    // 物品名称
    val aliases: String,                 // 别名(JSON数组)
    val origin: String,                  // 来历描述
    val usage: String,                   // 用途说明
    val category: String,                // 分类
    val confidence: Float,               // 识别置信度
    val source: String,                  // 识别来源
    val thumbnailPath: String? = null,   // 缩略图路径
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,     // 是否收藏

    // 详细信息字段
    val brand: String? = null,           // 品牌
    val model: String? = null,           // 型号
    val species: String? = null,         // 物种
    val priceRange: String? = null,      // 价格区间
    val material: String? = null,        // 材质
    val color: String? = null,           // 颜色
    val size: String? = null,            // 尺寸规格
    val manufacturer: String? = null,    // 制造商/产地
    val features: String? = null,        // 特征列表(JSON数组)

    // 百科知识字段
    val summary: String? = null,         // 简介/摘要
    val description: String? = null,     // 详细描述
    val historyText: String? = null,     // 历史背景
    val funFacts: String? = null,        // 趣味知识(JSON数组)
    val tips: String? = null,            // 使用技巧(JSON数组)
    val relatedTopics: String? = null,   // 延伸阅读(JSON数组)

    // 分类特定字段
    val objectType: String = "GENERAL",  // 物品类型
    val typeSpecificInfo: String? = null,// 类型特定信息(JSON Map)
    val additionalInfo: String? = null   // 额外信息(JSON Map)
)
