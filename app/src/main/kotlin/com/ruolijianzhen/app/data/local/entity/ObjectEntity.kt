package com.ruolijianzhen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 内置物品数据实体
 */
@Entity(tableName = "objects")
data class ObjectEntity(
    @PrimaryKey val label: String,      // MobileNet标签
    val nameCn: String,                  // 中文名称
    val nameEn: String,                  // 英文名称
    val aliases: String,                 // 别名(JSON数组)
    val origin: String,                  // 来历描述
    val usage: String,                   // 用途说明
    val category: String,                // 分类
    val imageAsset: String? = null       // 内置图片资源名
)

/**
 * 学习物品数据实体 - 通过API/AI识别后缓存的新物品
 */
@Entity(tableName = "learning_objects")
data class LearningObjectEntity(
    @PrimaryKey val label: String,      // 识别标签
    val nameCn: String,                  // 中文名称
    val nameEn: String = "",             // 英文名称
    val aliases: String = "[]",          // 别名(JSON数组)
    val origin: String = "",             // 来历描述
    val usage: String = "",              // 用途说明
    val category: String = "",           // 分类
    val source: String = "",             // 数据来源(API名称)
    val createdAt: Long = System.currentTimeMillis()
)
