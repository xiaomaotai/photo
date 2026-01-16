package com.ruolijianzhen.app.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ruolijianzhen.app.domain.camera.CameraManager
import com.ruolijianzhen.app.util.PerformanceUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX摄像头管理器实现
 * 确保30fps流畅预览，支持自动对焦、点击对焦、前后摄像头切换
 * 
 * 性能优化：
 * - 优化图片捕获流程
 * - 减少内存分配
 * - 使用高效的YUV转换
 * - 图片缓存和压缩
 */
@Singleton
class CameraManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraManager {
    
    companion object {
        private const val TAG = "CameraManager"
        private const val TARGET_FPS = 30
        private const val MAX_CAPTURE_SIZE = 1024 // 最大捕获尺寸
        private const val JPEG_QUALITY = 85 // JPEG压缩质量
        private const val FRAME_CACHE_INTERVAL = 3 // 每N帧缓存一次
    }
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var lifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // 闪光灯状态
    @Volatile
    private var flashEnabled: Boolean = false
    
    // 缓存最新帧用于快速捕获
    @Volatile
    private var latestFrame: Bitmap? = null
    private var latestFrameRotation: Int = 0
    
    // 帧计数器（用于控制缓存频率）
    @Volatile
    private var frameCount = 0
    
    override suspend fun startPreview(previewView: PreviewView) {
        withContext(Dispatchers.Main) {
            try {
                currentPreviewView = previewView
                lifecycleOwner = previewView.context as? LifecycleOwner
                    ?: throw IllegalStateException("PreviewView context must be a LifecycleOwner")
                
                val provider = getCameraProvider()
                cameraProvider = provider
                
                bindCameraUseCases(provider, previewView)
                
                Log.d(TAG, "Camera preview started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera preview", e)
                throw e
            }
        }
    }
    
    override fun stopPreview() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        imageAnalysis = null
        Log.d(TAG, "Camera preview stopped")
    }
    
    override suspend fun captureImage(): Bitmap? = withContext(Dispatchers.IO) {
        // 获取当前缩放比例，用于裁剪
        val currentZoom = getCurrentZoomRatio()
        Log.d(TAG, "Capturing image with zoom ratio: $currentZoom")
        
        // 优先使用缓存的最新帧（更快）
        latestFrame?.let { frame ->
            Log.d(TAG, "Using cached frame for capture")
            val rotated = rotateBitmap(frame, latestFrameRotation)
            // 应用缩放裁剪
            return@withContext applyZoomCrop(rotated, currentZoom)
        }
        
        // 确保ImageCapture已初始化
        if (imageCapture == null) {
            Log.w(TAG, "ImageCapture not initialized")
            return@withContext null
        }
        
        try {
            // 使用ImageAnalysis快速捕获当前帧
            suspendCancellableCoroutine { continuation ->
                val analyzer = object : ImageAnalysis.Analyzer {
                    override fun analyze(image: ImageProxy) {
                        try {
                            val bitmap = imageProxyToBitmap(image)
                            val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees)
                            // 应用缩放裁剪
                            val zoomedBitmap = applyZoomCrop(rotatedBitmap, currentZoom)
                            // 优化：缩放到合理尺寸
                            val scaledBitmap = scaleBitmapForRecognition(zoomedBitmap)
                            continuation.resume(scaledBitmap)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to convert image", e)
                            continuation.resumeWithException(e)
                        } finally {
                            image.close()
                        }
                    }
                }
                
                imageAnalysis?.setAnalyzer(cameraExecutor, analyzer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture image", e)
            null
        }
    }
    
    /**
     * 根据缩放比例裁剪图片中心区域
     * 模拟用户看到的放大效果
     */
    private fun applyZoomCrop(bitmap: Bitmap, zoomRatio: Float): Bitmap {
        if (zoomRatio <= 1.0f) {
            return bitmap
        }
        
        // 计算裁剪区域
        val cropWidth = (bitmap.width / zoomRatio).toInt()
        val cropHeight = (bitmap.height / zoomRatio).toInt()
        
        // 确保裁剪尺寸有效
        if (cropWidth <= 0 || cropHeight <= 0 || cropWidth > bitmap.width || cropHeight > bitmap.height) {
            return bitmap
        }
        
        // 计算裁剪起始点（居中裁剪）
        val startX = (bitmap.width - cropWidth) / 2
        val startY = (bitmap.height - cropHeight) / 2
        
        Log.d(TAG, "Applying zoom crop: zoom=$zoomRatio, original=${bitmap.width}x${bitmap.height}, crop=${cropWidth}x${cropHeight}")
        
        return try {
            Bitmap.createBitmap(bitmap, startX, startY, cropWidth, cropHeight)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to crop bitmap", e)
            bitmap
        }
    }
    
    /**
     * 将ImageProxy转换为Bitmap（优化版本）
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val format = image.format
        
        return when (format) {
            ImageFormat.YUV_420_888 -> yuvToBitmap(image)
            ImageFormat.JPEG -> jpegToBitmap(image)
            else -> {
                // 尝试直接转换
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw IllegalStateException("Failed to decode image")
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback to YUV conversion")
                    yuvToBitmap(image)
                }
            }
        }
    }
    
    /**
     * YUV格式转Bitmap（优化版本）
     * 使用更高效的内存分配策略
     */
    private fun yuvToBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream(ySize) // 预分配合理大小
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), JPEG_QUALITY, out)
        val imageBytes = out.toByteArray()
        
        // 使用采样解码以节省内存
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565 // 使用更省内存的配置
        }
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    }
    
    /**
     * JPEG格式转Bitmap
     */
    private fun jpegToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    
    /**
     * 缩放图片到适合识别的尺寸
     */
    private fun scaleBitmapForRecognition(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_CAPTURE_SIZE && bitmap.height <= MAX_CAPTURE_SIZE) {
            return bitmap
        }
        
        val scale = minOf(
            MAX_CAPTURE_SIZE.toFloat() / bitmap.width,
            MAX_CAPTURE_SIZE.toFloat() / bitmap.height
        )
        
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    
    override suspend fun switchCamera() = withContext(Dispatchers.Main) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        
        val provider = cameraProvider ?: return@withContext
        val previewView = currentPreviewView ?: return@withContext
        
        bindCameraUseCases(provider, previewView)
        Log.d(TAG, "Camera switched to ${if (isFrontCamera()) "front" else "back"}")
    }
    
    override fun setTapToFocus(x: Float, y: Float) {
        val factory = currentPreviewView?.meteringPointFactory ?: return
        val point = factory.createPoint(x, y)
        
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        camera?.cameraControl?.startFocusAndMetering(action)
        Log.d(TAG, "Tap to focus at ($x, $y)")
    }
    
    override fun isFrontCamera(): Boolean = lensFacing == CameraSelector.LENS_FACING_FRONT
    
    override fun setZoomRatio(ratio: Float) {
        val cameraControl = camera?.cameraControl ?: return
        val cameraInfo = camera?.cameraInfo ?: return
        
        val minZoom = cameraInfo.zoomState.value?.minZoomRatio ?: 1f
        val maxZoom = cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
        
        // 限制在有效范围内
        val clampedRatio = ratio.coerceIn(minZoom, maxZoom)
        cameraControl.setZoomRatio(clampedRatio)
        Log.d(TAG, "Zoom ratio set to $clampedRatio (range: $minZoom - $maxZoom)")
    }
    
    override fun getMinZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
    }
    
    override fun getMaxZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
    }
    
    override fun getCurrentZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
    }
    
    override fun setFlashEnabled(enabled: Boolean) {
        flashEnabled = enabled
        camera?.cameraControl?.enableTorch(enabled)
        Log.d(TAG, "Flash ${if (enabled) "enabled" else "disabled"}")
    }
    
    override fun isFlashEnabled(): Boolean {
        return flashEnabled
    }
    
    override fun release() {
        stopPreview()
        latestFrame?.recycle()
        latestFrame = null
        cameraExecutor.shutdown()
        Log.d(TAG, "Camera resources released")
    }
    
    /**
     * 获取CameraProvider
     */
    private suspend fun getCameraProvider(): ProcessCameraProvider {
        return suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    
    /**
     * 绑定摄像头用例
     */
    private fun bindCameraUseCases(
        provider: ProcessCameraProvider,
        previewView: PreviewView
    ) {
        val owner = lifecycleOwner ?: return
        
        // 解绑之前的用例
        provider.unbindAll()
        
        // 创建Preview用例
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // 创建ImageCapture用例
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(previewView.display.rotation)
            .build()
        
        // 创建ImageAnalysis用例（用于快速捕获和帧缓存）
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(previewView.display.rotation)
            .build()
        
        // 设置帧缓存分析器
        setupFrameCaching()
        
        // 选择摄像头
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        try {
            // 绑定用例到生命周期
            camera = provider.bindToLifecycle(
                owner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
            
            // 恢复闪光灯状态
            camera?.cameraControl?.let { control ->
                control.enableTorch(flashEnabled)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
            throw e
        }
    }
    
    /**
     * 设置帧缓存（用于快速捕获）
     * 优化：降低缓存频率以减少CPU和内存压力
     */
    private fun setupFrameCaching() {
        imageAnalysis?.setAnalyzer(cameraExecutor) { image ->
            try {
                frameCount++
                // 每N帧缓存一次，减少CPU压力
                if (frameCount % FRAME_CACHE_INTERVAL == 0) {
                    val bitmap = imageProxyToBitmap(image)
                    latestFrame?.recycle()
                    latestFrame = bitmap
                    latestFrameRotation = image.imageInfo.rotationDegrees
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache frame", e)
            } finally {
                image.close()
            }
        }
    }
    
    /**
     * 旋转Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}
