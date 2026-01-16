package com.ruolijianzhen.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TensorFlow Lite分类器 - 封装MobileNet模型推理
 * 性能优化：支持GPU加速、NNAPI加速、图片处理缓存
 */
@Singleton
class TFLiteClassifier @Inject constructor(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isInitialized = false
    private var gpuDelegate: GpuDelegate? = null
    private var nnApiDelegate: NnApiDelegate? = null
    
    // 缓存ImageProcessor避免重复创建
    private var imageProcessor: ImageProcessor? = null
    
    companion object {
        private const val TAG = "TFLiteClassifier"
        private const val MODEL_FILE = "mobilenet_v2.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val IMAGE_SIZE = 224
        private const val PIXEL_SIZE = 3
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
        private const val NUM_THREADS = 4
    }
    
    /**
     * 初始化分类器
     * 优先使用GPU加速，其次NNAPI，最后CPU
     */
    @Synchronized
    fun initialize(): Boolean {
        if (isInitialized) return true
        
        return try {
            // 加载模型
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            val options = createInterpreterOptions()
            interpreter = Interpreter(modelBuffer, options)
            
            // 加载标签
            labels = loadLabels()
            
            // 预创建ImageProcessor
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build()
            
            isInitialized = true
            Log.d(TAG, "TFLite classifier initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLite classifier", e)
            false
        }
    }
    
    /**
     * 创建Interpreter选项，优先使用硬件加速
     */
    private fun createInterpreterOptions(): Interpreter.Options {
        val options = Interpreter.Options()
        
        // 尝试GPU加速
        try {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                options.addDelegate(gpuDelegate)
                Log.d(TAG, "Using GPU acceleration")
                return options
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU delegate not available", e)
        }
        
        // 尝试NNAPI加速（Android 8.1+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                nnApiDelegate = NnApiDelegate()
                options.addDelegate(nnApiDelegate)
                Log.d(TAG, "Using NNAPI acceleration")
                return options
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI delegate not available", e)
            }
        }
        
        // 使用CPU多线程
        options.setNumThreads(NUM_THREADS)
        Log.d(TAG, "Using CPU with $NUM_THREADS threads")
        return options
    }
    
    /**
     * 加载标签文件
     */
    private fun loadLabels(): List<String> {
        return try {
            context.assets.open(LABELS_FILE).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load labels", e)
            emptyList()
        }
    }
    
    /**
     * 对图片进行分类
     * @param bitmap 输入图片
     * @return 分类结果列表，按置信度降序排列
     * 
     * 性能优化：
     * - 使用缓存的ImageProcessor
     * - 优化内存分配
     * - 支持硬件加速
     */
    fun classify(bitmap: Bitmap): List<ClassificationResult> {
        if (!isInitialized) {
            if (!initialize()) {
                return emptyList()
            }
        }
        
        val interpreter = this.interpreter ?: return emptyList()
        val processor = this.imageProcessor ?: return emptyList()
        
        return try {
            // 优化：缩放图片以减少内存占用
            val scaledBitmap = scaleBitmapIfNeeded(bitmap)
            
            // 图片预处理（使用缓存的processor）
            val tensorImage = TensorImage.fromBitmap(scaledBitmap)
            val processedImage = processor.process(tensorImage)
            
            // 准备输出缓冲区
            val outputShape = interpreter.getOutputTensor(0).shape()
            val outputBuffer = TensorBuffer.createFixedSize(outputShape, org.tensorflow.lite.DataType.FLOAT32)
            
            // 运行推理
            interpreter.run(processedImage.buffer, outputBuffer.buffer.rewind())
            
            // 解析结果
            val probabilities = outputBuffer.floatArray
            probabilities.mapIndexed { index, confidence ->
                ClassificationResult(
                    label = labels.getOrElse(index) { "unknown_$index" },
                    confidence = confidence
                )
            }.sortedByDescending { it.confidence }
            
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed", e)
            emptyList()
        }
    }
    
    /**
     * 缩放图片以优化内存占用
     * 如果图片过大，先缩放到合理尺寸
     */
    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxSize = IMAGE_SIZE * 2 // 最大允许448x448
        
        if (bitmap.width <= maxSize && bitmap.height <= maxSize) {
            return bitmap
        }
        
        val scale = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * 获取Top-K分类结果
     */
    fun classifyTopK(bitmap: Bitmap, k: Int = 5): List<ClassificationResult> {
        return classify(bitmap).take(k)
    }
    
    /**
     * 释放资源
     */
    fun close() {
        gpuDelegate?.close()
        gpuDelegate = null
        
        nnApiDelegate?.close()
        nnApiDelegate = null
        
        interpreter?.close()
        interpreter = null
        
        imageProcessor = null
        isInitialized = false
        
        Log.d(TAG, "TFLite classifier resources released")
    }
}

/**
 * 分类结果
 */
data class ClassificationResult(
    val label: String,
    val confidence: Float
)
