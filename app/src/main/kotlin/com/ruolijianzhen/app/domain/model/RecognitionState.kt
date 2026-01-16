package com.ruolijianzhen.app.domain.model

/**
 * 识别状态 - 用于UI状态管理
 */
sealed class RecognitionState {
    /**
     * 空闲状态 - 等待用户操作
     */
    data object Idle : RecognitionState()
    
    /**
     * 处理中状态 - 正在识别
     */
    data object Processing : RecognitionState()
    
    /**
     * 成功状态 - 识别完成
     */
    data class Success(val info: ObjectInfo) : RecognitionState()
    
    /**
     * 错误状态 - 识别失败
     */
    data class Error(val message: String) : RecognitionState()
}
