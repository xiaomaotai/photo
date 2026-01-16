package com.ruolijianzhen.app.domain.engine

import android.graphics.Bitmap
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.RecognitionState
import kotlinx.coroutines.flow.StateFlow

/**
 * 识别引擎接口
 * 核心调度组件，负责协调三层识别策略：离线优先→免费API→用户AI
 */
interface RecognitionEngine {
    /**
     * 识别图片中的物品
     * @param bitmap 待识别的图片
     * @return 识别结果，包含物品信息
     */
    suspend fun recognize(bitmap: Bitmap): Result<ObjectInfo>
    
    /**
     * 获取当前识别状态
     */
    fun getRecognitionState(): StateFlow<RecognitionState>
    
    /**
     * 重置状态为空闲
     */
    fun resetState()
    
    /**
     * 获取离线识别置信度阈值
     */
    fun getConfidenceThreshold(): Float
    
    companion object {
        /**
         * 默认置信度阈值 - 低于此值时切换到API识别
         * 降低阈值以便更容易返回结果
         */
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f
    }
}
