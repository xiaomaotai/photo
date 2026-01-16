package com.ruolijianzhen.app.data.knowledge

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 百科知识获取客户端
 * 通过百度百科获取物品的详细知识介绍
 */
@Singleton
class BaikeClient @Inject constructor() {
    
    companion object {
        private const val TAG = "BaikeClient"
        // 百度百科OpenAPI（免费）
        private const val BAIKE_API_URL = "https://baike.baidu.com/api/openapi/BaikeLemmaCardApi"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    
    /**
     * 获取百科知识
     * @param keyword 搜索关键词
     * @return 百科信息，失败返回null
     */
    suspend fun getKnowledge(keyword: String): BaikeInfo? = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext null
        
        try {
            val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
            val url = "$BAIKE_API_URL?scope=103&format=json&appid=379020&bk_key=$encodedKeyword&bk_length=600"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            
            parseResponse(responseBody)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get baike info for: $keyword", e)
            null
        }
    }
    
    private fun parseResponse(responseBody: String): BaikeInfo? {
        return try {
            val json = JSONObject(responseBody)
            
            // 检查是否有结果
            if (!json.has("title") || json.optString("title").isBlank()) {
                return null
            }
            
            val title = json.optString("title", "")
            val summary = json.optString("abstract", "")
            val description = json.optString("desc", "")
            val imageUrl = json.optString("image", "")
            val baikeUrl = json.optString("url", "")
            
            // 解析基本信息
            val basicInfo = mutableMapOf<String, String>()
            val card = json.optJSONArray("card")
            if (card != null) {
                for (i in 0 until card.length()) {
                    val item = card.optJSONObject(i)
                    if (item != null) {
                        val key = item.optString("key", "")
                        val value = item.optString("value", "")
                        if (key.isNotBlank() && value.isNotBlank()) {
                            basicInfo[key] = value
                        }
                    }
                }
            }
            
            BaikeInfo(
                title = title,
                summary = summary.ifBlank { description },
                description = description,
                imageUrl = imageUrl.ifBlank { null },
                baikeUrl = baikeUrl.ifBlank { null },
                basicInfo = basicInfo
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse baike response", e)
            null
        }
    }
}

/**
 * 百科信息数据类
 */
data class BaikeInfo(
    val title: String,
    val summary: String,
    val description: String,
    val imageUrl: String?,
    val baikeUrl: String?,
    val basicInfo: Map<String, String>
)
