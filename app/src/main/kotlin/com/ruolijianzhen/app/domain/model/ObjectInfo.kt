package com.ruolijianzhen.app.domain.model

/**
 * 统一物品信息 - 所有识别结果的标准展示格式
 * 支持丰富的知识展示
 */
data class ObjectInfo(
    val id: String,                              // 唯一标识
    val name: String,                            // 物品名称
    val aliases: List<String>,                   // 别名列表
    val origin: String,                          // 来历/起源
    val usage: String,                           // 用途说明
    val category: String,                        // 分类标签
    val confidence: Float,                       // 识别置信度
    val source: RecognitionSource,               // 识别来源
    val imageUrl: String? = null,                // 参考图片URL
    val capturedImagePath: String? = null,       // 识别时拍摄的图片路径
    val additionalInfo: Map<String, String> = emptyMap(), // 额外信息
    
    // 详细信息字段
    val brand: String? = null,                   // 品牌
    val model: String? = null,                   // 型号
    val species: String? = null,                 // 物种（动植物专用）
    val priceRange: String? = null,              // 价格区间
    val material: String? = null,                // 材质
    val color: String? = null,                   // 颜色
    val size: String? = null,                    // 尺寸规格
    val manufacturer: String? = null,            // 制造商/产地
    val features: List<String> = emptyList(),    // 特征列表
    
    // 百科知识字段
    val summary: String? = null,                 // 简介/摘要
    val description: String? = null,             // 详细描述
    val history: String? = null,                 // 历史背景
    val funFacts: List<String> = emptyList(),    // 趣味知识
    val tips: List<String> = emptyList(),        // 使用技巧/注意事项
    val relatedTopics: List<String> = emptyList(), // 延伸阅读主题
    
    // 分类特定字段
    val objectType: ObjectType = ObjectType.GENERAL, // 物品类型
    val typeSpecificInfo: Map<String, String> = emptyMap(), // 类型特定信息
    
    // 元数据
    val recognizedAt: Long = System.currentTimeMillis(), // 识别时间
    val isFavorite: Boolean = false              // 是否收藏
) {
    companion object {
        const val DEFAULT_PLACEHOLDER = "暂无信息"
    }
}

/**
 * 物品类型枚举 - 用于决定展示哪些字段
 * icon字段存储Material Icon名称，使用getTypeIcon()扩展函数获取实际图标
 */
enum class ObjectType(val displayName: String, val iconName: String) {
    GENERAL("通用", "Inventory2"),
    ELECTRONICS("电子产品", "Smartphone"),
    ANIMAL("动物", "Pets"),
    PLANT("植物", "Eco"),
    FOOD("食物", "Restaurant"),
    DAILY_USE("日用品", "Home"),
    ARTWORK("艺术品", "Palette"),
    LANDMARK("地标建筑", "AccountBalance"),
    VEHICLE("交通工具", "DirectionsCar"),
    CLOTHING("服饰", "Checkroom"),
    BOOK("书籍", "AutoStories");
    
    companion object {
        /**
         * 根据分类名称推断物品类型
         */
        fun fromCategory(category: String): ObjectType {
            val lowerCategory = category.lowercase()
            return when {
                lowerCategory.contains("电") || lowerCategory.contains("手机") || 
                lowerCategory.contains("电脑") || lowerCategory.contains("数码") -> ELECTRONICS
                
                lowerCategory.contains("动物") || lowerCategory.contains("宠物") ||
                lowerCategory.contains("鸟") || lowerCategory.contains("鱼") -> ANIMAL
                
                lowerCategory.contains("植物") || lowerCategory.contains("花") ||
                lowerCategory.contains("树") || lowerCategory.contains("草") -> PLANT
                
                lowerCategory.contains("食") || lowerCategory.contains("饮") ||
                lowerCategory.contains("水果") || lowerCategory.contains("蔬菜") -> FOOD
                
                lowerCategory.contains("日用") || lowerCategory.contains("家居") ||
                lowerCategory.contains("厨房") || lowerCategory.contains("卫浴") -> DAILY_USE
                
                lowerCategory.contains("艺术") || lowerCategory.contains("画") ||
                lowerCategory.contains("雕") || lowerCategory.contains("工艺") -> ARTWORK
                
                lowerCategory.contains("建筑") || lowerCategory.contains("景点") ||
                lowerCategory.contains("地标") -> LANDMARK
                
                lowerCategory.contains("车") || lowerCategory.contains("交通") ||
                lowerCategory.contains("飞机") || lowerCategory.contains("船") -> VEHICLE
                
                lowerCategory.contains("服") || lowerCategory.contains("鞋") ||
                lowerCategory.contains("帽") || lowerCategory.contains("包") -> CLOTHING
                
                lowerCategory.contains("书") || lowerCategory.contains("杂志") -> BOOK
                
                else -> GENERAL
            }
        }
    }
}

/**
 * 识别来源枚举
 */
enum class RecognitionSource(val displayName: String) {
    OFFLINE("本地识别"),
    BAIDU_API("百度AI"),
    USER_AI("自定义AI");
    
    companion object {
        fun fromString(value: String): RecognitionSource {
            return entries.find { it.name == value } ?: OFFLINE
        }
    }
}
