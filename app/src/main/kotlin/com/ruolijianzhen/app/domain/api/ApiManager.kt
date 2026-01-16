package com.ruolijianzhen.app.domain.api

import android.graphics.Bitmap
import com.ruolijianzhen.app.domain.model.ApiResult
import com.ruolijianzhen.app.domain.model.ApiStatus

/**
 * API管理器接口
 * 管理多个免费API的调用和切换
 */
interface ApiManager {
    /**
     * 使用在线API识别
     * 自动选择可用API，额度耗尽自动切换
     */
    suspend fun recognize(bitmap: Bitmap): ApiResult?
    
    /**
     * 获取所有API状态
     */
    suspend fun getApiStatus(): List<ApiStatus>
    
    /**
     * 检查是否有可用的API
     */
    suspend fun hasAvailableApi(): Boolean
}
