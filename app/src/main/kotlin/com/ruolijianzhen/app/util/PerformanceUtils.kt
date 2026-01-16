package com.ruolijianzhen.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * 性能优化工具类
 * 提供图片压缩、缓存管理、内存优化等功能
 */
object PerformanceUtils {
    
    private const val TAG = "PerformanceUtils"
    
    // 图片缓存 - 使用可用内存的1/8
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
        
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) {
                // 不主动回收，让GC处理
                Log.d(TAG, "Cache entry removed: $key")
            }
        }
    }
    
    /**
     * 压缩图片到指定最大尺寸
     * @param bitmap 原始图片
     * @param maxSize 最大边长（像素）
     * @param quality JPEG压缩质量（0-100）- 保留参数用于未来扩展
     * @return 压缩后的图片
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun compressBitmap(
        bitmap: Bitmap,
        maxSize: Int = 1024,
        quality: Int = 85
    ): Bitmap = withContext(Dispatchers.Default) {
        // 如果图片已经足够小，直接返回
        if (bitmap.width <= maxSize && bitmap.height <= maxSize) {
            return@withContext bitmap
        }
        
        // 计算缩放比例
        val scale = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        // 缩放图片
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        
        Log.d(TAG, "Compressed bitmap: ${bitmap.width}x${bitmap.height} -> ${newWidth}x${newHeight}")
        
        scaledBitmap
    }
    
    /**
     * 压缩图片为JPEG字节数组
     * @param bitmap 原始图片
     * @param maxSize 最大边长
     * @param quality JPEG质量
     * @return 压缩后的字节数组
     */
    suspend fun compressToBytes(
        bitmap: Bitmap,
        maxSize: Int = 1024,
        quality: Int = 85
    ): ByteArray = withContext(Dispatchers.Default) {
        val compressed = compressBitmap(bitmap, maxSize, quality)
        
        ByteArrayOutputStream().use { stream ->
            compressed.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }
    
    /**
     * 从字节数组解码图片（带采样优化）
     * @param bytes 图片字节数组
     * @param reqWidth 目标宽度
     * @param reqHeight 目标高度
     * @return 解码后的图片
     */
    suspend fun decodeSampledBitmap(
        bytes: ByteArray,
        reqWidth: Int = 1024,
        reqHeight: Int = 1024
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // 先获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            // 计算采样率
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // 使用更省内存的配置
            
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap", e)
            null
        }
    }
    
    /**
     * 计算采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * 缓存图片
     * @param key 缓存键
     * @param bitmap 图片
     */
    fun cacheBitmap(key: String, bitmap: Bitmap) {
        if (bitmapCache.get(key) == null) {
            bitmapCache.put(key, bitmap)
            Log.d(TAG, "Cached bitmap: $key")
        }
    }
    
    /**
     * 从缓存获取图片
     * @param key 缓存键
     * @return 缓存的图片，如果不存在返回null
     */
    fun getCachedBitmap(key: String): Bitmap? {
        return bitmapCache.get(key)
    }
    
    /**
     * 生成图片的缓存键
     * @param bitmap 图片
     * @return 缓存键（基于图片内容的哈希）
     */
    fun generateCacheKey(bitmap: Bitmap): String {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            val bytes = stream.toByteArray()
            
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "${bitmap.width}_${bitmap.height}_${System.currentTimeMillis()}"
        }
    }
    
    /**
     * 清除图片缓存
     */
    fun clearCache() {
        bitmapCache.evictAll()
        Log.d(TAG, "Bitmap cache cleared")
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = bitmapCache.size(),
            maxSize = bitmapCache.maxSize(),
            hitCount = bitmapCache.hitCount(),
            missCount = bitmapCache.missCount()
        )
    }
    
    /**
     * 旋转图片
     * @param bitmap 原始图片
     * @param degrees 旋转角度
     * @return 旋转后的图片
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }
        
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }
    
    /**
     * 裁剪图片中心区域
     * @param bitmap 原始图片
     * @param targetRatio 目标宽高比
     * @return 裁剪后的图片
     */
    fun cropCenter(bitmap: Bitmap, targetRatio: Float = 1f): Bitmap {
        val srcRatio = bitmap.width.toFloat() / bitmap.height
        
        return if (srcRatio > targetRatio) {
            // 图片太宽，裁剪左右
            val newWidth = (bitmap.height * targetRatio).toInt()
            val x = (bitmap.width - newWidth) / 2
            Bitmap.createBitmap(bitmap, x, 0, newWidth, bitmap.height)
        } else {
            // 图片太高，裁剪上下
            val newHeight = (bitmap.width / targetRatio).toInt()
            val y = (bitmap.height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, y, bitmap.width, newHeight)
        }
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitCount: Int,
    val missCount: Int
) {
    val hitRate: Float
        get() = if (hitCount + missCount > 0) {
            hitCount.toFloat() / (hitCount + missCount)
        } else 0f
}
