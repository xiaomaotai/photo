package com.ruolijianzhen.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * 主题偏好设置
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
    
    /**
     * 获取主题模式
     */
    fun getThemeMode(): Flow<ThemeMode> {
        return context.themeDataStore.data.map { preferences ->
            val value = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(value)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
    }
    
    /**
     * 设置主题模式
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }
    
    /**
     * 获取是否启用动态颜色
     */
    fun getDynamicColor(): Flow<Boolean> {
        return context.themeDataStore.data.map { preferences ->
            preferences[KEY_DYNAMIC_COLOR] ?: true
        }
    }
    
    /**
     * 设置是否启用动态颜色
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR] = enabled
        }
    }
}

/**
 * 主题模式
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("浅色模式"),
    DARK("深色模式"),
    SYSTEM("跟随系统")
}
