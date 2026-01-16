package com.ruolijianzhen.app.ui.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局错误处理器
 * 统一处理应用中的各类异常，提供友好的错误提示
 */
@Singleton
class ErrorHandler @Inject constructor() {
    
    companion object {
        private const val TAG = "ErrorHandler"
    }
    
    private val _errorEvents = MutableSharedFlow<ErrorEvent>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()
    
    /**
     * 处理异常并返回友好的错误消息
     */
    fun handleException(throwable: Throwable, context: ErrorContext = ErrorContext.GENERAL): String {
        Log.e(TAG, "Error in ${context.name}", throwable)
        
        val message = when (context) {
            ErrorContext.CAMERA -> handleCameraError(throwable)
            ErrorContext.RECOGNITION -> handleRecognitionError(throwable)
            ErrorContext.NETWORK -> handleNetworkError(throwable)
            ErrorContext.DATABASE -> handleDatabaseError(throwable)
            ErrorContext.AI_SERVICE -> handleAiServiceError(throwable)
            ErrorContext.GENERAL -> handleGeneralError(throwable)
        }
        
        _errorEvents.tryEmit(ErrorEvent(message, context, throwable))
        return message
    }
    
    /**
     * 处理摄像头相关错误
     */
    private fun handleCameraError(throwable: Throwable): String {
        return when {
            throwable.message?.contains("permission", ignoreCase = true) == true ->
                "摄像头权限被拒绝，请在设置中允许"
            throwable.message?.contains("camera", ignoreCase = true) == true ->
                "摄像头启动失败，请检查是否被其他应用占用"
            throwable.message?.contains("lifecycle", ignoreCase = true) == true ->
                "摄像头初始化失败，请重试"
            else -> "摄像头出现问题，请重试"
        }
    }
    
    /**
     * 处理识别相关错误
     */
    private fun handleRecognitionError(throwable: Throwable): String {
        return when {
            throwable.message?.contains("model", ignoreCase = true) == true ->
                "识别模型加载失败，请重启应用"
            throwable.message?.contains("bitmap", ignoreCase = true) == true ->
                "图片处理失败，请重新拍摄"
            throwable.message?.contains("confidence", ignoreCase = true) == true ->
                "无法识别该物品，请调整角度或光线后重试"
            else -> "识别失败，请重试"
        }
    }
    
    /**
     * 处理网络相关错误
     */
    private fun handleNetworkError(throwable: Throwable): String {
        return when {
            throwable.message?.contains("timeout", ignoreCase = true) == true ->
                "网络请求超时，请检查网络连接"
            throwable.message?.contains("connect", ignoreCase = true) == true ->
                "无法连接到服务器，请检查网络"
            throwable.message?.contains("ssl", ignoreCase = true) == true ->
                "网络安全连接失败，请检查网络环境"
            throwable.message?.contains("host", ignoreCase = true) == true ->
                "无法解析服务器地址，请检查网络"
            else -> "网络请求失败，请检查网络连接"
        }
    }
    
    /**
     * 处理数据库相关错误
     */
    private fun handleDatabaseError(throwable: Throwable): String {
        return when {
            throwable.message?.contains("disk", ignoreCase = true) == true ->
                "存储空间不足，请清理空间后重试"
            throwable.message?.contains("corrupt", ignoreCase = true) == true ->
                "数据损坏，请清除应用数据后重试"
            else -> "数据存储失败，请重试"
        }
    }
    
    /**
     * 处理AI服务相关错误
     */
    private fun handleAiServiceError(throwable: Throwable): String {
        return when {
            throwable.message?.contains("api key", ignoreCase = true) == true ||
            throwable.message?.contains("apikey", ignoreCase = true) == true ||
            throwable.message?.contains("unauthorized", ignoreCase = true) == true ->
                "API密钥无效，请检查配置"
            throwable.message?.contains("quota", ignoreCase = true) == true ||
            throwable.message?.contains("limit", ignoreCase = true) == true ->
                "API调用次数已用完"
            throwable.message?.contains("rate", ignoreCase = true) == true ->
                "请求过于频繁，请稍后重试"
            else -> "AI服务调用失败，请检查配置"
        }
    }
    
    /**
     * 处理通用错误
     */
    private fun handleGeneralError(throwable: Throwable): String {
        return when {
            throwable is OutOfMemoryError -> "内存不足，请关闭其他应用后重试"
            throwable.message?.contains("memory", ignoreCase = true) == true ->
                "内存不足，请关闭其他应用后重试"
            else -> "发生错误，请重试"
        }
    }
    
    /**
     * 创建协程异常处理器
     */
    fun createCoroutineExceptionHandler(context: ErrorContext = ErrorContext.GENERAL): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            handleException(throwable, context)
        }
    }
}

/**
 * 错误上下文
 */
enum class ErrorContext {
    CAMERA,
    RECOGNITION,
    NETWORK,
    DATABASE,
    AI_SERVICE,
    GENERAL
}

/**
 * 错误事件
 */
data class ErrorEvent(
    val message: String,
    val context: ErrorContext,
    val throwable: Throwable
)
