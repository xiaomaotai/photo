package com.ruolijianzhen.app.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruolijianzhen.app.domain.camera.CameraManager
import com.ruolijianzhen.app.domain.engine.RecognitionEngine
import com.ruolijianzhen.app.domain.history.HistoryRepository
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.RecognitionState
import com.ruolijianzhen.app.ui.util.ErrorContext
import com.ruolijianzhen.app.ui.util.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主界面ViewModel
 * 集成错误处理和历史记录保存
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val recognitionEngine: RecognitionEngine,
    val cameraManager: CameraManager,
    private val historyRepository: HistoryRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    
    // 冻结帧 - 用于在识别过程中显示捕获的画面
    private val _frozenBitmap = MutableStateFlow<Bitmap?>(null)
    val frozenBitmap: StateFlow<Bitmap?> = _frozenBitmap.asStateFlow()
    
    // 保存最后捕获的图片用于重试
    private var lastCapturedBitmap: Bitmap? = null
    
    init {
        // 监听识别状态
        viewModelScope.launch {
            recognitionEngine.getRecognitionState().collect { state ->
                _recognitionState.value = state
                
                when (state) {
                    is RecognitionState.Success -> {
                        _uiState.value = MainUiState.ShowResult(state.info)
                        // 保存到历史记录
                        saveToHistory(state.info)
                    }
                    is RecognitionState.Error -> {
                        _uiState.value = MainUiState.Error(state.message)
                    }
                    is RecognitionState.Processing -> {
                        _uiState.value = MainUiState.Processing
                    }
                    is RecognitionState.Idle -> {
                        if (_uiState.value !is MainUiState.ShowResult) {
                            _uiState.value = MainUiState.Idle
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 拍摄并识别
     */
    fun captureAndRecognize() {
        // 防止重复点击
        if (_uiState.value is MainUiState.Processing) {
            Log.d(TAG, "Already processing, ignoring click")
            return
        }
        
        viewModelScope.launch(errorHandler.createCoroutineExceptionHandler(ErrorContext.RECOGNITION)) {
            try {
                Log.d(TAG, "Starting capture and recognize")
                
                // 先捕获图片
                val bitmap = cameraManager.captureImage()
                if (bitmap == null) {
                    Log.e(TAG, "Failed to capture image - bitmap is null")
                    val errorMsg = errorHandler.handleException(
                        IllegalStateException("Failed to capture image"),
                        ErrorContext.CAMERA
                    )
                    _uiState.value = MainUiState.Error(errorMsg)
                    return@launch
                }
                
                // 保存用于重试
                lastCapturedBitmap = bitmap
                
                // 设置冻结帧 - 在识别过程中显示捕获的画面
                _frozenBitmap.value = bitmap
                
                // 然后设置处理状态（这样冻结帧会先显示）
                _uiState.value = MainUiState.Processing
                
                Log.d(TAG, "Frozen bitmap set, size: ${bitmap.width}x${bitmap.height}")
                
                // 识别物品
                val result = recognitionEngine.recognize(bitmap)
                result.onSuccess { objectInfo ->
                    Log.d(TAG, "Recognition success: ${objectInfo.name}")
                    _uiState.value = MainUiState.ShowResult(objectInfo)
                    saveToHistory(objectInfo)
                }.onFailure { error ->
                    Log.e(TAG, "Recognition failed", error)
                    val errorMsg = errorHandler.handleException(error, ErrorContext.RECOGNITION)
                    _uiState.value = MainUiState.Error(errorMsg)
                    // 错误时保持冻结帧，让用户看到识别失败的是哪张图片
                    // 冻结帧会在用户关闭错误提示或重新拍摄时清除
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Capture and recognize failed", e)
                val errorMsg = errorHandler.handleException(e, ErrorContext.RECOGNITION)
                _uiState.value = MainUiState.Error(errorMsg)
                // 异常时也保持冻结帧
            }
        }
    }
    
    /**
     * 保存识别结果到历史记录
     */
    private fun saveToHistory(objectInfo: ObjectInfo) {
        viewModelScope.launch {
            try {
                historyRepository.save(objectInfo)
                Log.d(TAG, "Saved to history: ${objectInfo.name}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save history", e)
                // 历史记录保存失败不影响主流程
            }
        }
    }
    
    /**
     * 切换摄像头
     */
    fun switchCamera() {
        viewModelScope.launch(errorHandler.createCoroutineExceptionHandler(ErrorContext.CAMERA)) {
            try {
                cameraManager.switchCamera()
            } catch (e: Exception) {
                Log.e(TAG, "Switch camera failed", e)
                val errorMsg = errorHandler.handleException(e, ErrorContext.CAMERA)
                _uiState.value = MainUiState.Error(errorMsg)
            }
        }
    }
    
    /**
     * 设置闪光灯开关
     */
    fun setFlashEnabled(enabled: Boolean) {
        try {
            cameraManager.setFlashEnabled(enabled)
            Log.d(TAG, "Flash ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set flash", e)
        }
    }
    
    /**
     * 关闭结果弹窗
     * 异步清理资源避免UI卡顿
     */
    fun dismissResult() {
        _uiState.value = MainUiState.Idle
        // 异步清除冻结帧，避免主线程阻塞
        viewModelScope.launch {
            _frozenBitmap.value = null
            recognitionEngine.resetState()
        }
    }
    
    /**
     * 重试识别
     * 使用上次捕获的图片重新识别，保持冻结帧显示
     */
    fun retry() {
        viewModelScope.launch {
            // 如果有上次捕获的图片，直接重新识别
            lastCapturedBitmap?.let { bitmap ->
                // 确保冻结帧显示的是要重试的图片
                _frozenBitmap.value = bitmap
                _uiState.value = MainUiState.Processing
                
                val result = recognitionEngine.recognize(bitmap)
                result.onSuccess { objectInfo ->
                    _uiState.value = MainUiState.ShowResult(objectInfo)
                    saveToHistory(objectInfo)
                }.onFailure { error ->
                    val errorMsg = errorHandler.handleException(error, ErrorContext.RECOGNITION)
                    _uiState.value = MainUiState.Error(errorMsg)
                    // 重试失败也保持冻结帧
                }
            } ?: run {
                // 没有缓存的图片，清除状态让用户重新拍摄
                _uiState.value = MainUiState.Idle
                _frozenBitmap.value = null
                recognitionEngine.resetState()
            }
        }
    }
    
    /**
     * 切换收藏状态（保留但不再使用）
     */
    @Suppress("UNUSED_PARAMETER")
    fun toggleFavorite(objectInfo: ObjectInfo) {
        // 功能已移除
    }
    
    override fun onCleared() {
        super.onCleared()
        // 异步释放资源
        viewModelScope.launch {
            lastCapturedBitmap?.recycle()
            lastCapturedBitmap = null
        }
        cameraManager.release()
    }
}

/**
 * 主界面UI状态
 */
sealed class MainUiState {
    data object Idle : MainUiState()
    data object Processing : MainUiState()
    data class ShowResult(val objectInfo: ObjectInfo) : MainUiState()
    data class Error(val message: String) : MainUiState()
}
