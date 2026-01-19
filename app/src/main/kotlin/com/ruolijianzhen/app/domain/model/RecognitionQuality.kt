package com.ruolijianzhen.app.domain.model

/**
 * 识别质量等级
 * 用于向用户展示识别结果的可信度
 */
enum class RecognitionQuality(
    val displayName: String,
    val description: String,
    val minConfidence: Float,
    val maxConfidence: Float,
    val color: Long // ARGB color
) {
    /**
     * 高置信度 - 非常确定
     */
    HIGH(
        displayName = "高可信度",
        description = "识别结果非常可靠",
        minConfidence = 0.75f,
        maxConfidence = 1.0f,
        color = 0xFF4CAF50 // Green
    ),
    
    /**
     * 中置信度 - 较为确定
     */
    MEDIUM(
        displayName = "中等可信度",
        description = "识别结果较为可靠，建议核实",
        minConfidence = 0.45f,
        maxConfidence = 0.75f,
        color = 0xFFFFC107 // Amber
    ),
    
    /**
     * 低置信度 - 不太确定
     */
    LOW(
        displayName = "低可信度",
        description = "识别结果仅供参考，建议重新拍摄",
        minConfidence = 0.2f,
        maxConfidence = 0.45f,
        color = 0xFFFF9800 // Orange
    ),
    
    /**
     * 极低置信度 - 猜测
     */
    UNCERTAIN(
        displayName = "不确定",
        description = "无法确定识别结果，请调整拍摄角度或光线",
        minConfidence = 0.0f,
        maxConfidence = 0.2f,
        color = 0xFFFF5722 // Deep Orange
    );
    
    companion object {
        /**
         * 根据置信度获取质量等级
         */
        fun fromConfidence(confidence: Float): RecognitionQuality {
            return when {
                confidence >= HIGH.minConfidence -> HIGH
                confidence >= MEDIUM.minConfidence -> MEDIUM
                confidence >= LOW.minConfidence -> LOW
                else -> UNCERTAIN
            }
        }
        
        /**
         * 获取置信度对应的颜色
         */
        fun getColorForConfidence(confidence: Float): Long {
            return fromConfidence(confidence).color
        }
    }
}

/**
 * 识别结果评估
 * 包含质量等级和改进建议
 */
data class RecognitionAssessment(
    val quality: RecognitionQuality,
    val confidence: Float,
    val suggestions: List<String> = emptyList(),
    val isReliable: Boolean = quality == RecognitionQuality.HIGH || quality == RecognitionQuality.MEDIUM
) {
    companion object {
        /**
         * 创建评估结果
         */
        fun assess(confidence: Float, source: RecognitionSource): RecognitionAssessment {
            val quality = RecognitionQuality.fromConfidence(confidence)
            val suggestions = mutableListOf<String>()
            
            // 根据质量等级生成建议
            when (quality) {
                RecognitionQuality.UNCERTAIN -> {
                    suggestions.add("请确保物品在画面中央且清晰可见")
                    suggestions.add("尝试调整光线，避免过暗或过亮")
                    suggestions.add("尝试从不同角度拍摄")
                }
                RecognitionQuality.LOW -> {
                    suggestions.add("可以尝试靠近物品拍摄")
                    suggestions.add("确保物品没有被遮挡")
                }
                RecognitionQuality.MEDIUM -> {
                    if (source == RecognitionSource.OFFLINE) {
                        suggestions.add("联网后可获得更准确的识别结果")
                    }
                }
                RecognitionQuality.HIGH -> {
                    // 高置信度不需要建议
                }
            }
            
            return RecognitionAssessment(
                quality = quality,
                confidence = confidence,
                suggestions = suggestions,
                isReliable = quality == RecognitionQuality.HIGH || quality == RecognitionQuality.MEDIUM
            )
        }
    }
}