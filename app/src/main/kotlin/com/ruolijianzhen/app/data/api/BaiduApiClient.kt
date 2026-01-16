package com.ruolijianzhen.app.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.ruolijianzhen.app.domain.model.ApiResult
import com.ruolijianzhen.app.domain.model.RecognitionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 百度AI图像识别客户端
 */
@Singleton
class BaiduApiClient @Inject constructor() : ApiClient {
    
    companion object {
        private const val TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
        private const val RECOGNIZE_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    // API密钥（需要用户配置）
    private var apiKey: String = ""
    private var secretKey: String = ""
    private var accessToken: String? = null
    private var tokenExpireTime: Long = 0
    
    fun configure(apiKey: String, secretKey: String) {
        this.apiKey = apiKey
        this.secretKey = secretKey
        this.accessToken = null
    }
    
    override suspend fun recognize(bitmap: Bitmap): ApiResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || secretKey.isBlank()) {
            return@withContext null
        }
        
        // 获取或刷新Token
        val token = getAccessToken() ?: return@withContext null
        
        // 图片转Base64
        val imageBase64 = bitmapToBase64(bitmap)
        
        // 调用识别API
        val requestBody = FormBody.Builder()
            .add("image", imageBase64)
            .build()
        
        val request = Request.Builder()
            .url("$RECOGNIZE_URL?access_token=$token")
            .post(requestBody)
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            
            parseResponse(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override fun getSource(): RecognitionSource = RecognitionSource.BAIDU_API
    
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        // 检查Token是否有效
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return@withContext accessToken
        }
        
        val requestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", apiKey)
            .add("client_secret", secretKey)
            .build()
        
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(requestBody)
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            val json = JSONObject(responseBody)
            
            accessToken = json.optString("access_token")
            val expiresIn = json.optLong("expires_in", 2592000)
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000
            
            accessToken
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseResponse(responseBody: String): ApiResult? {
        return try {
            val json = JSONObject(responseBody)
            val results = json.optJSONArray("result") ?: return null
            
            if (results.length() == 0) return null
            
            val topResult = results.getJSONObject(0)
            val keyword = topResult.optString("keyword", "")
            val score = topResult.optDouble("score", 0.0).toFloat()
            val root = topResult.optString("root", "")
            
            ApiResult(
                name = keyword,
                description = "分类: $root",
                confidence = score,
                source = RecognitionSource.BAIDU_API,
                rawResponse = mapOf("root" to root, "keyword" to keyword)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
