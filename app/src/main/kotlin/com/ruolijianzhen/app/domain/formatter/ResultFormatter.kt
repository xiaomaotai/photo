package com.ruolijianzhen.app.domain.formatter

import com.ruolijianzhen.app.domain.model.AiResult
import com.ruolijianzhen.app.domain.model.ApiResult
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.OfflineResult
import com.ruolijianzhen.app.domain.model.RecognitionSource

/**
 * 结果格式化器接口
 * 将不同来源的识别结果统一为标准ObjectInfo格式
 */
interface ResultFormatter {
    /**
     * 格式化离线识别结果
     */
    fun format(result: OfflineResult): ObjectInfo
    
    /**
     * 格式化API识别结果
     */
    fun format(result: ApiResult): ObjectInfo
    
    /**
     * 格式化用户AI识别结果
     */
    fun format(result: AiResult): ObjectInfo
}
