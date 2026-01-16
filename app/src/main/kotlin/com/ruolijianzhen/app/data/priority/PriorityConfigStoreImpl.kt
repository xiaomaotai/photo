package com.ruolijianzhen.app.data.priority

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ruolijianzhen.app.domain.model.PriorityConfig
import com.ruolijianzhen.app.domain.model.RecognitionMethod
import com.ruolijianzhen.app.domain.model.RecognitionMethodConfig
import com.ruolijianzhen.app.domain.priority.PriorityConfigStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.priorityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "priority_config"
)

/**
 * 优先级配置存储实现
 * 使用DataStore进行JSON序列化存储
 */
@Singleton
class PriorityConfigStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PriorityConfigStore {
    
    companion object {
        private val KEY_PRIORITY_CONFIG = stringPreferencesKey("priority_config_json")
    }
    
    override suspend fun save(config: PriorityConfig) {
        context.priorityDataStore.edit { preferences ->
            preferences[KEY_PRIORITY_CONFIG] = configToJson(config)
        }
    }
    
    override suspend fun load(): PriorityConfig? {
        return observe().first()
    }
    
    override fun observe(): Flow<PriorityConfig?> {
        return context.priorityDataStore.data.map { preferences ->
            preferences[KEY_PRIORITY_CONFIG]?.let { json ->
                try {
                    jsonToConfig(json)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }
    
    override suspend fun clear() {
        context.priorityDataStore.edit { preferences ->
            preferences.remove(KEY_PRIORITY_CONFIG)
        }
    }
    
    /**
     * 将配置转换为JSON字符串
     */
    private fun configToJson(config: PriorityConfig): String {
        val jsonArray = JSONArray()
        config.methods.forEach { methodConfig ->
            val jsonObject = JSONObject().apply {
                put("method", methodConfig.method.name)
                put("enabled", methodConfig.enabled)
                put("priority", methodConfig.priority)
            }
            jsonArray.put(jsonObject)
        }
        return JSONObject().apply {
            put("methods", jsonArray)
        }.toString()
    }
    
    /**
     * 从JSON字符串解析配置
     */
    private fun jsonToConfig(json: String): PriorityConfig {
        val jsonObject = JSONObject(json)
        val methodsArray = jsonObject.getJSONArray("methods")
        val methods = mutableListOf<RecognitionMethodConfig>()
        
        for (i in 0 until methodsArray.length()) {
            val methodJson = methodsArray.getJSONObject(i)
            val method = RecognitionMethod.valueOf(methodJson.getString("method"))
            val enabled = methodJson.getBoolean("enabled")
            val priority = methodJson.getInt("priority")
            methods.add(RecognitionMethodConfig(method, enabled, priority))
        }
        
        return PriorityConfig(methods)
    }
}
