package com.ruolijianzhen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 识别历史记录实体
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
    val isFavorite: Boolean = false      // 是否收藏
)
