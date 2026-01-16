package com.ruolijianzhen.app.domain.ai

import android.graphics.Bitmap
import com.ruolijianzhen.app.domain.model.AiResult
import com.ruolijianzhen.app.domain.model.UserAiConfig

/**
 * 用户AI服务接口
 */
interface UserAiService {
    /**
     * 使用用户配置的AI识别
     */
    suspend fun recognize(bitmap: Bitmap): AiResult?
    
    /**
     * 检查用户是否已配置AI
     */
    suspend fun isConfigured(): Boolean
    
    /**
     * 保存用户AI配置
     */
    suspend fun saveConfig(config: UserAiConfig): Boolean
    
    /**
     * 获取当前配置
     */
    suspend fun getConfig(): UserAiConfig?
    
    /**
     * 验证配置有效性
     */
    suspend fun validateConfig(config: UserAiConfig): Boolean
    
    /**
     * 清除配置
     */
    suspend fun clearConfig()
}
