package com.ruolijianzhen.app.domain.model

/**
 * 物品详细信息 - 内置数据库中的物品描述
 */
data class ObjectDetails(
    val name: String,                    // 物品名称
    val nameEn: String = "",             // 英文名称
    val aliases: List<String>,           // 别名列表
    val origin: String,                  // 来历描述
    val usage: String,                   // 用途说明
    val category: String                 // 分类
) {
    companion object {
        /**
         * 创建空的物品详情
         */
        fun empty(name: String = "未知物品"): ObjectDetails = ObjectDetails(
            name = name,
            nameEn = "",
            aliases = emptyList(),
            origin = "暂无来历信息",
            usage = "暂无用途信息",
            category = "未分类"
        )
    }
}
