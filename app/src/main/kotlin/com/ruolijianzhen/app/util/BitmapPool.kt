package com.ruolijianzhen.app.util

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bitmap 池
 * 复用 Bitmap 对象，减少内存分配和 GC 压力
 */
@Singleton
class BitmapPool @Inject constructor() {
    
    companion object {
        private const val TAG = "BitmapPool"
        
        // 池大小（字节）- 默认 20MB
        private const val DEFAULT_POOL_SIZE = 20 * 1024 * 1024L
        
        // 单个 Bitmap 最大尺寸
        private const val MAX_BITMAP_SIZE = 4096
        
        // 每种尺寸最多缓存的数量
        private const val MAX_BITMAPS_PER_SIZE = 4
    }
    
    // 按尺寸分组的 Bitmap 池
    private val pool = mutableMapOf<String, ConcurrentLinkedQueue<Bitmap>>()
    
    // 当前池大小
    @Volatile
    private var currentSize = 0L
    
    // 最大池大小
    private var maxSize = DEFAULT_POOL_SIZE
    
    // 统计信息
    @Volatile
    private var hitCount = 0
    @Volatile
    private var missCount = 0
    @Volatile
    private var putCount = 0
    @Volatile
    private var evictionCount = 0
    
    /**
     * 设置池大小
     */
    fun setMaxSize(maxSizeBytes: Long) {
        maxSize = maxSizeBytes
        trimToSize(maxSize)
    }
    
    /**
     * 获取可复用的 Bitmap
     * @param width 宽度
     * @param height 高度
     * @param config Bitmap 配置
     * @return 可复用的 Bitmap，如果没有则返回 null
     */
    fun get(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        if (width <= 0 || height <= 0 || width > MAX_BITMAP_SIZE || height > MAX_BITMAP_SIZE) {
            return null
        }
        
        val key = generateKey(width, height, config)
        val queue = pool[key]
        
        val bitmap = queue?.poll()
        if (bitmap != null) {
            hitCount++
            currentSize -= getBitmapSize(bitmap)
            Log.d(TAG, "Pool hit: ${width}x${height}, current size: ${currentSize / 1024}KB")
            return bitmap
        }
        
        missCount++
        return null
    }
    
    /**
     * 获取或创建 Bitmap
     * @param width 宽度
     * @param height 高度
     * @param config Bitmap 配置
     * @return Bitmap（可能是复用的或新创建的）
     */
    fun getOrCreate(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        return get(width, height, config) ?: Bitmap.createBitmap(width, height, config)
    }
    
    /**
     * 将 Bitmap 放入池中以供复用
     * @param bitmap 要放入的 Bitmap
     * @return 是否成功放入
     */
    fun put(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled) {
            return false
        }
        
        val width = bitmap.width
        val height = bitmap.height
        
        // 检查尺寸限制
        if (width <= 0 || height <= 0 || width > MAX_BITMAP_SIZE || height > MAX_BITMAP_SIZE) {
            bitmap.recycle()
            return false
        }
        
        val bitmapSize = getBitmapSize(bitmap)
        
        // 如果单个 Bitmap 太大，直接回收
        if (bitmapSize > maxSize / 4) {
            bitmap.recycle()
            return false
        }
        
        val key = generateKey(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        
        synchronized(pool) {
            val queue = pool.getOrPut(key) { ConcurrentLinkedQueue() }
            
            // 检查该尺寸的缓存数量
            if (queue.size >= MAX_BITMAPS_PER_SIZE) {
                bitmap.recycle()
                return false
            }
            
            // 确保有足够空间
            trimToSize(maxSize - bitmapSize)
            
            // 清除 Bitmap 内容
            try {
                bitmap.eraseColor(0)
            } catch (e: Exception) {
                bitmap.recycle()
                return false
            }
            
            queue.offer(bitmap)
            currentSize += bitmapSize
            putCount++
            
            Log.d(TAG, "Pool put: ${width}x${height}, current size: ${currentSize / 1024}KB")
        }
        
        return true
    }
    
    /**
     * 清空池
     */
    fun clear() {
        synchronized(pool) {
            pool.values.forEach { queue ->
                while (true) {
                    val bitmap = queue.poll() ?: break
                    bitmap.recycle()
                }
            }
            pool.clear()
            currentSize = 0
            Log.d(TAG, "Pool cleared")
        }
    }
    
    /**
     * 裁剪池到指定大小
     */
    private fun trimToSize(targetSize: Long) {
        synchronized(pool) {
            while (currentSize > targetSize && pool.isNotEmpty()) {
                // 找到最大的队列并移除一个
                var largestQueue: ConcurrentLinkedQueue<Bitmap>? = null
                var largestKey: String? = null
                var largestSize = 0
                
                for ((key, queue) in pool) {
                    if (queue.isNotEmpty() && queue.size > largestSize) {
                        largestQueue = queue
                        largestKey = key
                        largestSize = queue.size
                    }
                }
                
                if (largestQueue == null) break
                
                val bitmap = largestQueue.poll()
                if (bitmap != null) {
                    currentSize -= getBitmapSize(bitmap)
                    bitmap.recycle()
                    evictionCount++
                }
                
                // 如果队列空了，移除它
                if (largestQueue.isEmpty() && largestKey != null) {
                    pool.remove(largestKey)
                }
            }
        }
    }
    
    /**
     * 生成缓存键
     */
    private fun generateKey(width: Int, height: Int, config: Bitmap.Config): String {
        return "${width}_${height}_${config.name}"
    }
    
    /**
     * 获取 Bitmap 大小（字节）
     */
    private fun getBitmapSize(bitmap: Bitmap): Long {
        return bitmap.allocationByteCount.toLong()
    }
    
    /**
     * 获取池统计信息
     */
    fun getStats(): BitmapPoolStats {
        val totalRequests = hitCount + missCount
        val hitRate = if (totalRequests > 0) hitCount.toFloat() / totalRequests else 0f
        
        return BitmapPoolStats(
            currentSize = currentSize,
            maxSize = maxSize,
            hitCount = hitCount,
            missCount = missCount,
            putCount = putCount,
            evictionCount = evictionCount,
            hitRate = hitRate,
            pooledBitmapCount = pool.values.sumOf { it.size }
        )
    }
}

/**
 * Bitmap 池统计信息
 */
data class BitmapPoolStats(
    val currentSize: Long,
    val maxSize: Long,
    val hitCount: Int,
    val missCount: Int,
    val putCount: Int,
    val evictionCount: Int,
    val hitRate: Float,
    val pooledBitmapCount: Int
) {
    val currentSizeKB: Long get() = currentSize / 1024
    val maxSizeKB: Long get() = maxSize / 1024
    val hitRatePercent: Int get() = (hitRate * 100).toInt()
    
    override fun toString(): String {
        return "BitmapPoolStats(size=${currentSizeKB}KB/${maxSizeKB}KB, " +
                "hit=$hitCount, miss=$missCount, hitRate=$hitRatePercent%, " +
                "pooled=$pooledBitmapCount)"
    }
}