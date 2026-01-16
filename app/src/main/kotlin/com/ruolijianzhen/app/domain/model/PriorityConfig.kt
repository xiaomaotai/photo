package com.ruolijianzhen.app.domain.model

/**
 * 识别方式枚举
 */
enum class RecognitionMethod(val displayName: String) {
    OFFLINE("本地识别"),
    BAIDU_API("百度API"),
    USER_AI("自定义AI")
}

/**
 * 识别方式配置项
 */
data class RecognitionMethodConfig(
    val method: RecognitionMethod,
    val enabled: Boolean = true,
    val priority: Int  // 0 = highest priority
)

/**
 * 优先级配置
 */
data class PriorityConfig(
    val methods: List<RecognitionMethodConfig>
) {
    /**
     * 获取按优先级排序的已启用方法列表
     */
    fun getEnabledMethodsInOrder(): List<RecognitionMethod> {
        return methods
            .filter { it.enabled }
            .sortedBy { it.priority }
            .map { it.method }
    }
    
    /**
     * 检查是否至少有一个方法启用
     */
    fun hasEnabledMethod(): Boolean {
        return methods.any { it.enabled }
    }
    
    /**
     * 更新方法的启用状态
     */
    fun updateMethodEnabled(method: RecognitionMethod, enabled: Boolean): PriorityConfig {
        return copy(
            methods = methods.map {
                if (it.method == method) it.copy(enabled = enabled) else it
            }
        )
    }
    
    /**
     * 重新排序方法
     */
    fun reorder(newOrder: List<RecognitionMethod>): PriorityConfig {
        val methodMap = methods.associateBy { it.method }
        return copy(
            methods = newOrder.mapIndexed { index, method ->
                methodMap[method]?.copy(priority = index) 
                    ?: RecognitionMethodConfig(method, true, index)
            }
        )
    }
    
    companion object {
        /**
         * 默认配置：本地识别 → 百度API → 自定义AI
         */
        val DEFAULT = PriorityConfig(
            methods = listOf(
                RecognitionMethodConfig(RecognitionMethod.OFFLINE, true, 0),
                RecognitionMethodConfig(RecognitionMethod.BAIDU_API, true, 1),
                RecognitionMethodConfig(RecognitionMethod.USER_AI, true, 2)
            )
        )
    }
}
