package com.ruolijianzhen.app.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.ruolijianzhen.app.domain.model.AiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Gemini客户端
 * 支持Gemini Pro Vision模型进行图像识别
 */
@Singleton
class GeminiClient @Inject constructor() {
    
    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val DEFAULT_MODEL = "gemini-pro-vision"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 识别图片中的物品
     */
    suspend fun recognize(
        bitmap: Bitmap,
        apiKey: String,
        modelName: String = DEFAULT_MODEL
    ): AiResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        
        val imageBase64 = bitmapToBase64(bitmap)
        val requestBody = buildRequestBody(imageBase64)
        
        val url = "$BASE_URL/models/$modelName:generateContent?key=$apiKey"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            parseResponse(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 验证API Key有效性
     */
    suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        
        val url = "$BASE_URL/models?key=$apiKey"
        val request = Request.Builder().url(url).get().build()
        
        try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    private fun buildRequestBody(imageBase64: String): String {
        val prompt = """
            你是一个专业的物品识别专家。请仔细分析这张图片，识别其中的主要物品，并提供尽可能详细和具体的信息。

            识别要求：
            1. 如果是电子产品、家电、工具等，请识别具体的品牌和型号
            2. 如果是动物，请识别具体的物种（如：麻雀、喜鹊、金毛犬）
            3. 如果是植物，请识别具体的品种（如：月季、玫瑰、绿萝）
            4. 如果是食品，请识别具体的名称和类型
            5. 请根据物品外观估算市场价格区间
            6. 请描述物品的材质、颜色、尺寸等特征

            请以JSON格式返回以下信息：
            {
                "name": "物品的具体名称（中文，尽量具体，如'罗技K380键盘'而非'键盘'）",
                "brand": "品牌名称（如：罗技、雷蛇、苹果、华为、小米，无品牌则为null）",
                "model": "具体型号（如：K380、iPhone 15 Pro、无型号则为null）",
                "species": "物种/品种名称（仅动植物填写，如：麻雀、金毛犬、月季，非动植物为null）",
                "aliases": ["别名1", "别名2"],
                "origin": "物品的来历或起源（100字以内）",
                "usage": "物品的主要用途（100字以内）",
                "category": "物品分类（如：电子产品、家具、动物、植物、食品等）",
                "priceRange": "市场价格区间（如：'100-200元'、'500-1000元'，无法估算则为null）",
                "material": "主要材质（如：塑料、金属、木质、布料，无法判断则为null）",
                "color": "主要颜色（如：黑色、白色、蓝色）",
                "size": "大致尺寸（如：'约15cm x 10cm'，无法判断则为null）",
                "manufacturer": "制造商或产地（如：中国深圳、日本东京，无法判断则为null）",
                "features": ["特征1", "特征2", "特征3"]
            }
            
            注意：
            - 请尽量识别具体的品牌和型号，不要只返回通用名称
            - 如果无法确定某个字段，请返回null而不是猜测
            - 只返回JSON，不要其他文字
        """.trimIndent()
        
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", imageBase64)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 2048)
            })
        }.toString()
    }
    
    private fun parseResponse(responseBody: String): AiResult? {
        return try {
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            
            val content = candidates.getJSONObject(0)
                .optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            
            val text = parts.getJSONObject(0).optString("text", "")
            parseAiText(text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseAiText(text: String): AiResult? {
        return try {
            // 提取JSON部分
            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}") + 1
            if (jsonStart < 0 || jsonEnd <= jsonStart) return null
            
            val jsonStr = text.substring(jsonStart, jsonEnd)
            val json = JSONObject(jsonStr)
            
            val aliases = mutableListOf<String>()
            json.optJSONArray("aliases")?.let { arr ->
                for (i in 0 until arr.length()) {
                    aliases.add(arr.getString(i))
                }
            }
            
            val features = mutableListOf<String>()
            json.optJSONArray("features")?.let { arr ->
                for (i in 0 until arr.length()) {
                    features.add(arr.getString(i))
                }
            }
            
            AiResult(
                name = json.optString("name", "未知物品"),
                description = "由Google Gemini识别",
                aliases = aliases,
                origin = json.optString("origin", "暂无来历信息"),
                usage = json.optString("usage", "暂无用途信息"),
                category = json.optString("category", "未分类"),
                confidence = 0.85f,
                brand = json.optString("brand").takeIf { it.isNotBlank() && it != "null" },
                model = json.optString("model").takeIf { it.isNotBlank() && it != "null" },
                species = json.optString("species").takeIf { it.isNotBlank() && it != "null" },
                priceRange = json.optString("priceRange").takeIf { it.isNotBlank() && it != "null" },
                material = json.optString("material").takeIf { it.isNotBlank() && it != "null" },
                color = json.optString("color").takeIf { it.isNotBlank() && it != "null" },
                size = json.optString("size").takeIf { it.isNotBlank() && it != "null" },
                manufacturer = json.optString("manufacturer").takeIf { it.isNotBlank() && it != "null" },
                features = features
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
