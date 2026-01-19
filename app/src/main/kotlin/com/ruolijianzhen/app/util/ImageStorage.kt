package com.ruolijianzhen.app.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图片存储工具类
 * 用于保存识别时的缩略图到本地存储
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ImageStorage"
        private const val THUMBNAIL_DIR = "thumbnails"
        private const val THUMBNAIL_QUALITY = 80
        private const val THUMBNAIL_MAX_SIZE = 300 // 缩略图最大尺寸
    }
    
    private val thumbnailDir: File by lazy {
        File(context.filesDir, THUMBNAIL_DIR).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * 保存缩略图
     * @param bitmap 原始图片
     * @param id 唯一标识（用于文件名）
     * @return 保存的文件路径，失败返回 null
     */
    suspend fun saveThumbnail(bitmap: Bitmap, id: String): String? = withContext(Dispatchers.IO) {
        try {
            // 创建缩略图
            val thumbnail = createThumbnail(bitmap)
            
            // 生成文件名
            val fileName = "thumb_${id}_${dateFormat.format(Date())}.jpg"
            val file = File(thumbnailDir, fileName)
            
            // 保存到文件
            FileOutputStream(file).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }
            
            // 如果缩略图是新创建的，回收它
            if (thumbnail != bitmap) {
                thumbnail.recycle()
            }
            
            Log.d(TAG, "Thumbnail saved: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save thumbnail", e)
            null
        }
    }
    
    /**
     * 创建缩略图
     */
    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 如果图片已经足够小，直接返回
        if (width <= THUMBNAIL_MAX_SIZE && height <= THUMBNAIL_MAX_SIZE) {
            return bitmap
        }
        
        // 计算缩放比例
        val scale = minOf(
            THUMBNAIL_MAX_SIZE.toFloat() / width,
            THUMBNAIL_MAX_SIZE.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * 删除缩略图
     * @param path 文件路径
     */
    suspend fun deleteThumbnail(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete thumbnail", e)
            false
        }
    }
    
    /**
     * 清理所有缩略图
     */
    suspend fun clearAllThumbnails(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            thumbnailDir.listFiles()?.forEach { file ->
                if (file.delete()) {
                    count++
                }
            }
            Log.d(TAG, "Cleared $count thumbnails")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear thumbnails", e)
        }
        count
    }
    
    /**
     * 清理过期的缩略图（超过指定天数）
     */
    suspend fun cleanOldThumbnails(daysToKeep: Int): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            
            thumbnailDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        count++
                    }
                }
            }
            Log.d(TAG, "Cleaned $count old thumbnails")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old thumbnails", e)
        }
        count
    }
    
    /**
     * 获取缩略图目录大小（字节）
     */
    fun getThumbnailDirSize(): Long {
        return try {
            thumbnailDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取缩略图数量
     */
    fun getThumbnailCount(): Int {
        return try {
            thumbnailDir.listFiles()?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 检查缩略图是否存在
     */
    fun thumbnailExists(path: String?): Boolean {
        if (path == null) return false
        return File(path).exists()
    }
}