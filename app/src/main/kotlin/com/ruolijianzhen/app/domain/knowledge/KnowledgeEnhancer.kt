package com.ruolijianzhen.app.domain.knowledge

import com.ruolijianzhen.app.domain.model.ObjectInfo

/**
 * 知识增强器接口
 * 用于丰富识别结果的知识内容
 */
interface KnowledgeEnhancer {
    /**
     * 增强物品信息
     * @param objectInfo 原始识别结果
     * @return 增强后的物品信息
     */
    suspend fun enhance(objectInfo: ObjectInfo): ObjectInfo
}
