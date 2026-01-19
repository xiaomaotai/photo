package com.ruolijianzhen.app.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.util.LruCache
import com.ruolijianzhen.app.domain.model.ObjectInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 感知哈希 (pHash) 工具类
 * 用于计算图片的感知哈希值，支持相似图片检测
 * 
 * 算法原理：
 * 1. 缩小图片到固定尺寸
 * 2. 转换为灰度
 * 3. 计算 DCT（离散余弦变换）
 * 4. 取左上角低频部分
 * 5. 计算平均值并生成哈希
 */
@Singleton
class PerceptualHash @Inject constructor() {
    
    companion object {
        private const val TAG = "PerceptualHash"
        
        // 哈希计算用的图片尺寸
        private const val HASH_SIZE = 32
        
        // DCT 取样尺寸
        private const val DCT_SIZE = 8
        
        // 相似度阈值（汉明距离）
        private const val SIMILARITY_THRESHOLD = 10
        
        // 缓存大小
        private const val CACHE_SIZE = 50
    }
    
    // 结果缓存：哈希值 -> 识别结果
    private val resultCache = LruCache<String, CachedResult>(CACHE_SIZE)
    
    // DCT 系数缓存
    private val dctCoefficients: Array<DoubleArray> by lazy {
        Array(HASH_SIZE) { i ->
            DoubleArray(HASH_SIZE) { j ->
                cos((2 * i + 1) * j * Math.PI / (2 * HASH_SIZE))
            }
        }
    }
    
    /**
     * 计算图片的感知哈希值
     * @param bitmap 输入图片
     * @return 64位哈希值的十六进制字符串
     */
    fun computeHash(bitmap: Bitmap): String {
        return try {
            // 1. 缩小图片
            val scaled = Bitmap.createScaledBitmap(bitmap, HASH_SIZE, HASH_SIZE, true)
            
            // 2. 转换为灰度矩阵
            val grayMatrix = toGrayMatrix(scaled)
            
            // 3. 计算 DCT
            val dctMatrix = computeDCT(grayMatrix)
            
            // 4. 取左上角 8x8 区域（低频部分）
            val lowFreq = Array(DCT_SIZE) { i ->
                DoubleArray(DCT_SIZE) { j ->
                    dctMatrix[i][j]
                }
            }
            
            // 5. 计算平均值（排除第一个 DC 分量）
            var sum = 0.0
            var count = 0
            for (i in 0 until DCT_SIZE) {
                for (j in 0 until DCT_SIZE) {
                    if (i != 0 || j != 0) {
                        sum += lowFreq[i][j]
                        count++
                    }
                }
            }
            val average = sum / count
            
            // 6. 生成哈希
            val hash = StringBuilder()
            for (i in 0 until DCT_SIZE) {
                for (j in 0 until DCT_SIZE) {
                    hash.append(if (lowFreq[i][j] > average) "1" else "0")
                }
            }
            
            // 转换为十六进制
            binaryToHex(hash.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute hash", e)
            // 返回基于时间的唯一值作为后备
            System.currentTimeMillis().toString(16)
        }
    }
    
    /**
     * 计算两个哈希值的汉明距离
     * @return 汉明距离（不同位的数量）
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) {
            return Int.MAX_VALUE
        }
        
        val binary1 = hexToBinary(hash1)
        val binary2 = hexToBinary(hash2)
        
        var distance = 0
        for (i in binary1.indices) {
            if (binary1[i] != binary2[i]) {
                distance++
            }
        }
        return distance
    }
    
    /**
     * 判断两张图片是否相似
     */
    fun isSimilar(hash1: String, hash2: String, threshold: Int = SIMILARITY_THRESHOLD): Boolean {
        return hammingDistance(hash1, hash2) <= threshold
    }
    
    /**
     * 计算相似度（0-1，1表示完全相同）
     */
    fun similarity(hash1: String, hash2: String): Float {
        val distance = hammingDistance(hash1, hash2)
        val maxDistance = 64 // 64位哈希
        return 1f - (distance.toFloat() / maxDistance)
    }
    
    /**
     * 从缓存中查找相似图片的结果
     * @param bitmap 输入图片
     * @return 如果找到相似图片的缓存结果则返回，否则返回 null
     */
    fun findSimilarResult(bitmap: Bitmap): CachedResult? {
        val hash = computeHash(bitmap)
        
        // 精确匹配
        resultCache.get(hash)?.let {
            Log.d(TAG, "Exact cache hit for hash: $hash")
            return it
        }
        
        // 相似匹配
        val snapshot = resultCache.snapshot()
        for ((cachedHash, result) in snapshot) {
            if (isSimilar(hash, cachedHash)) {
                Log.d(TAG, "Similar cache hit: distance=${hammingDistance(hash, cachedHash)}")
                return result.copy(isExactMatch = false)
            }
        }
        
        return null
    }
    
    /**
     * 缓存识别结果
     */
    fun cacheResult(bitmap: Bitmap, result: ObjectInfo) {
        val hash = computeHash(bitmap)
        resultCache.put(hash, CachedResult(
            hash = hash,
            result = result,
            timestamp = System.currentTimeMillis(),
            isExactMatch = true
        ))
        Log.d(TAG, "Cached result for hash: $hash, name: ${result.name}")
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        resultCache.evictAll()
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * 获取缓存统计
     */
    fun getCacheStats(): PerceptualHashCacheStats {
        return PerceptualHashCacheStats(
            size = resultCache.size(),
            maxSize = CACHE_SIZE,
            hitCount = resultCache.hitCount(),
            missCount = resultCache.missCount()
        )
    }
    
    /**
     * 将图片转换为灰度矩阵
     */
    private fun toGrayMatrix(bitmap: Bitmap): Array<DoubleArray> {
        val width = bitmap.width
        val height = bitmap.height
        val matrix = Array(height) { DoubleArray(width) }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // 使用标准灰度转换公式
                matrix[y][x] = 0.299 * r + 0.587 * g + 0.114 * b
            }
        }
        
        return matrix
    }
    
    /**
     * 计算 DCT（离散余弦变换）
     */
    private fun computeDCT(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val n = matrix.size
        val result = Array(n) { DoubleArray(n) }
        
        for (u in 0 until n) {
            for (v in 0 until n) {
                var sum = 0.0
                for (i in 0 until n) {
                    for (j in 0 until n) {
                        sum += matrix[i][j] * dctCoefficients[i][u] * dctCoefficients[j][v]
                    }
                }
                
                val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                
                result[u][v] = 0.25 * cu * cv * sum
            }
        }
        
        return result
    }
    
    /**
     * 二进制字符串转十六进制
     */
    private fun binaryToHex(binary: String): String {
        val hex = StringBuilder()
        for (i in binary.indices step 4) {
            val end = minOf(i + 4, binary.length)
            val nibble = binary.substring(i, end).padEnd(4, '0')
            hex.append(Integer.parseInt(nibble, 2).toString(16))
        }
        return hex.toString()
    }
    
    /**
     * 十六进制转二进制字符串
     */
    private fun hexToBinary(hex: String): String {
        val binary = StringBuilder()
        for (c in hex) {
            val value = c.toString().toInt(16)
            binary.append(value.toString(2).padStart(4, '0'))
        }
        return binary.toString()
    }
}

/**
 * 缓存的识别结果
 */
data class CachedResult(
    val hash: String,
    val result: ObjectInfo,
    val timestamp: Long,
    val isExactMatch: Boolean = true
)

/**
 * 感知哈希缓存统计
 */
data class PerceptualHashCacheStats(
    val size: Int,
    val maxSize: Int,
    val hitCount: Int,
    val missCount: Int
) {
    val hitRate: Float
        get() = if (hitCount + missCount > 0) {
            hitCount.toFloat() / (hitCount + missCount)
        } else 0f
    
    val hitRatePercent: Int
        get() = (hitRate * 100).toInt()
}