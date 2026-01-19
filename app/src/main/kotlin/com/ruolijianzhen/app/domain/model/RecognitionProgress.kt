package com.ruolijianzhen.app.domain.model

/**
 * 识别进度阶段
 * 用于向用户展示当前识别进行到哪一步
 */
enum class RecognitionStage(
    val displayName: String,
    val description: String,
    val progress: Float // 0.0 - 1.0
) {
    /**
     * 准备阶段 - 图片预处理
     */
    PREPARING(
        displayName = "准备中",
        description = "正在处理图片...",
        progress = 0.1f
    ),
    
    /**
     * 本地识别阶段
     */
    OFFLINE_RECOGNITION(
        displayName = "本地识别",
        description = "正在进行本地识别...",
        progress = 0.3f
    ),
    
    /**
     * API识别阶段
     */
    API_RECOGNITION(
        displayName = "云端识别",
        description = "正在调用云端API...",
        progress = 0.5f
    ),
    
    /**
     * AI识别阶段
     */
    AI_RECOGNITION(
        displayName = "AI识别",
        description = "正在使用AI分析...",
        progress = 0.6f
    ),
    
    /**
     * 知识增强阶段
     */
    KNOWLEDGE_ENHANCEMENT(
        displayName = "知识增强",
        description = "正在获取详细信息...",
        progress = 0.8f
    ),
    
    /**
     * 完成阶段
     */
    COMPLETED(
        displayName = "完成",
        description = "识别完成",
        progress = 1.0f
    ),
    
    /**
     * 失败阶段
     */
    FAILED(
        displayName = "失败",
        description = "识别失败",
        progress = 0f
    )
}

/**
 * 识别进度信息
 */
data class RecognitionProgress(
    val stage: RecognitionStage,
    val currentMethod: RecognitionMethod? = null,
    val attemptedMethods: List<RecognitionMethod> = emptyList(),
    val message: String = stage.description,
    val estimatedTimeRemaining: Long? = null // 毫秒
) {
    val progressPercent: Int
        get() = (stage.progress * 100).toInt()
    
    val isCompleted: Boolean
        get() = stage == RecognitionStage.COMPLETED
    
    val isFailed: Boolean
        get() = stage == RecognitionStage.FAILED
    
    val isInProgress: Boolean
        get() = !isCompleted && !isFailed
    
    companion object {
        fun preparing() = RecognitionProgress(RecognitionStage.PREPARING)
        
        fun offlineRecognition() = RecognitionProgress(
            stage = RecognitionStage.OFFLINE_RECOGNITION,
            currentMethod = RecognitionMethod.OFFLINE
        )
        
        fun apiRecognition() = RecognitionProgress(
            stage = RecognitionStage.API_RECOGNITION,
            currentMethod = RecognitionMethod.BAIDU_API
        )
        
        fun aiRecognition() = RecognitionProgress(
            stage = RecognitionStage.AI_RECOGNITION,
            currentMethod = RecognitionMethod.USER_AI
        )
        
        fun knowledgeEnhancement() = RecognitionProgress(
            stage = RecognitionStage.KNOWLEDGE_ENHANCEMENT
        )
        
        fun completed() = RecognitionProgress(RecognitionStage.COMPLETED)
        
        fun failed(message: String) = RecognitionProgress(
            stage = RecognitionStage.FAILED,
            message = message
        )
    }
}