package com.ruolijianzhen.app.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruolijianzhen.app.data.engine.RecognitionEngineImpl
import com.ruolijianzhen.app.domain.camera.CameraManager
import com.ruolijianzhen.app.domain.engine.RecognitionEngine
import com.ruolijianzhen.app.domain.history.HistoryRepository
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.RecognitionProgress
import com.ruolijianzhen.app.domain.model.RecognitionState
import com.ruolijianzhen.app.ui.util.ErrorContext
import com.ruolijianzhen.app.ui.util.ErrorHandler
import com.ruolijianzhen.app.util.ImageStorage
import com.ruolijianzhen.app.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 主界面ViewModel
 * 集成错误处理、历史记录保存（含缩略图）、进度反馈和网络状态
 * 支持相机拍摄和本地图片识别
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val recognitionEngine: RecognitionEngine,
    val cameraManager: CameraManager,
    private val historyRepository: HistoryRepository,
    private val errorHandler: ErrorHandler,
    private val networkUtils: NetworkUtils,
    private val imageStorage: ImageStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
        private const val MAX_IMAGE_SIZE = 2048 // 最大图片尺寸
    }
    
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    
    // 识别进度
    private val _recognitionProgress = MutableStateFlow<RecognitionProgress?>(null)
    val recognitionProgress: StateFlow<RecognitionProgress?> = _recognitionProgress.asStateFlow()
    
    // 网络状态
    val networkInfo = networkUtils.networkInfo
    
    // 冻结帧 - 用于在识别过程中显示捕获的画面
    private val _frozenBitmap = MutableStateFlow<Bitmap?>(null)
    val frozenBitmap: StateFlow<Bitmap?> = _frozenBitmap.asStateFlow()
    
    // 保存最后捕获的图片用于重试和保存缩略图
    private var lastCapturedBitmap: Bitmap? = null
    
    // 保存最后选择的URI用于重试
    private var lastSelectedUri: Uri? = null
    
    init {
        // 监听识别状态
        viewModelScope.launch {
            recognitionEngine.getRecognitionState().collect { state ->
                _recognitionState.value = state
                
                when (state) {
                    is RecognitionState.Success -> {
                        _uiState.value = MainUiState.ShowResult(state.info)
                        // 保存到历史记录（含缩略图）
                        saveToHistoryWithThumbnail(state.info)
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
        
        // 监听识别进度（如果引擎支持）
        viewModelScope.launch {
            if (recognitionEngine is RecognitionEngineImpl) {
                recognitionEngine.recognitionProgress.collect { progress ->
                    _recognitionProgress.value = progress
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
        
        // 清除上次选择的URI
        lastSelectedUri = null
        
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
                
                // 保存用于重试和缩略图
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
                    saveToHistoryWithThumbnail(objectInfo)
                }.onFailure { error ->
                    Log.e(TAG, "Recognition failed", error)
                    val errorMsg = errorHandler.handleException(error, ErrorContext.RECOGNITION)
                    _uiState.value = MainUiState.Error(errorMsg)
                    // 错误时保持冻结帧，让用户看到识别失败的是哪张图片
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Capture and recognize failed", e)
                val errorMsg = errorHandler.handleException(e, ErrorContext.RECOGNITION)
                _uiState.value = MainUiState.Error(errorMsg)
            }
        }
    }
    
    /**
     * 从URI识别图片（本地相册选择）
     */
    fun recognizeFromUri(uri: Uri) {
        // 防止重复点击
        if (_uiState.value is MainUiState.Processing) {
            Log.d(TAG, "Already processing, ignoring click")
            return
        }
        
        // 保存URI用于重试
        lastSelectedUri = uri
        
        viewModelScope.launch(errorHandler.createCoroutineExceptionHandler(ErrorContext.RECOGNITION)) {
            try {
                Log.d(TAG, "Starting recognize from URI: $uri")
                
                // 设置处理状态
                _uiState.value = MainUiState.Processing
                
                // 在IO线程加载图片
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(uri)
                }
                
                if (bitmap == null) {
                    Log.e(TAG, "Failed to load image from URI")
                    val errorMsg = errorHandler.handleException(
                        IllegalStateException("无法加载图片"),
                        ErrorContext.RECOGNITION
                    )
                    _uiState.value = MainUiState.Error(errorMsg)
                    return@launch
                }
                
                // 保存用于重试和缩略图
                lastCapturedBitmap = bitmap
                
                // 设置冻结帧 - 显示选择的图片
                _frozenBitmap.value = bitmap
                
                Log.d(TAG, "Image loaded from URI, size: ${bitmap.width}x${bitmap.height}")
                
                // 识别物品
                val result = recognitionEngine.recognize(bitmap)
                result.onSuccess { objectInfo ->
                    Log.d(TAG, "Recognition success: ${objectInfo.name}")
                    _uiState.value = MainUiState.ShowResult(objectInfo)
                    saveToHistoryWithThumbnail(objectInfo)
                }.onFailure { error ->
                    Log.e(TAG, "Recognition failed", error)
                    val errorMsg = errorHandler.handleException(error, ErrorContext.RECOGNITION)
                    _uiState.value = MainUiState.Error(errorMsg)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Recognize from URI failed", e)
                val errorMsg = errorHandler.handleException(e, ErrorContext.RECOGNITION)
                _uiState.value = MainUiState.Error(errorMsg)
            }
        }
    }
    
    /**
     * 从URI加载Bitmap
     * 自动缩放大图片以避免OOM
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 先获取图片尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                // 计算采样率
                val sampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
                
                // 重新打开流并解码
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI", e)
            null
        }
    }
    
    /**
     * 计算图片采样率
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * 保存识别结果到历史记录（含缩略图）
     */
    private fun saveToHistoryWithThumbnail(objectInfo: ObjectInfo) {
        viewModelScope.launch {
            try {
                // 保存缩略图
                val thumbnailPath = lastCapturedBitmap?.let { bitmap ->
                    imageStorage.saveThumbnail(bitmap, objectInfo.id)
                }
                
                // 保存历史记录
                historyRepository.save(objectInfo, thumbnailPath)
                Log.d(TAG, "Saved to history with thumbnail: ${objectInfo.name}, path: $thumbnailPath")
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
            _recognitionProgress.value = null
            recognitionEngine.resetState()
        }
    }
    
    /**
     * 重试识别
     * 使用上次捕获的图片或URI重新识别
     */
    fun retry() {
        viewModelScope.launch {
            // 如果有上次选择的URI，重新从URI加载
            lastSelectedUri?.let { uri ->
                recognizeFromUri(uri)
                return@launch
            }
            
            // 如果有上次捕获的图片，直接重新识别
            lastCapturedBitmap?.let { bitmap ->
                // 确保冻结帧显示的是要重试的图片
                _frozenBitmap.value = bitmap
                _uiState.value = MainUiState.Processing
                
                val result = recognitionEngine.recognize(bitmap)
                result.onSuccess { objectInfo ->
                    _uiState.value = MainUiState.ShowResult(objectInfo)
                    saveToHistoryWithThumbnail(objectInfo)
                }.onFailure { error ->
                    val errorMsg = errorHandler.handleException(error, ErrorContext.RECOGNITION)
                    _uiState.value = MainUiState.Error(errorMsg)
                }
            } ?: run {
                // 没有缓存的图片，清除状态让用户重新拍摄
                _uiState.value = MainUiState.Idle
                _frozenBitmap.value = null
                _recognitionProgress.value = null
                recognitionEngine.resetState()
            }
        }
    }
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        return networkUtils.isNetworkAvailable()
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
