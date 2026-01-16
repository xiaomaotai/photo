package com.ruolijianzhen.app.domain.recognition

import android.graphics.Bitmap
import com.ruolijianzhen.app.domain.model.ObjectDetails
import com.ruolijianzhen.app.domain.model.OfflineResult

/**
 * 离线识别器接口
 */
interface OfflineRecognizer {
    /**
     * 本地识别物品
     * @param bitmap 待识别图片
     * @return 识别结果，包含置信度；识别失败返回null
     */
    suspend fun recognize(bitmap: Bitmap): OfflineResult?
    
    /**
     * 获取内置物品详细信息
     * @param label 识别标签
     * @return 物品详细信息；未找到返回null
     */
    suspend fun getObjectDetails(label: String): ObjectDetails?
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean
    
    /**
     * 初始化识别器
     */
    suspend fun initialize(): Boolean
}
