package com.ruolijianzhen.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ruolijianzhen.app.RuoLiJianZhenApp
import com.ruolijianzhen.app.data.preferences.ThemeMode
import com.ruolijianzhen.app.data.preferences.ThemePreferences
import com.ruolijianzhen.app.ui.navigation.AppNavigation
import com.ruolijianzhen.app.ui.theme.RuoLiJianZhenTheme
import com.ruolijianzhen.app.ui.util.ScreenUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主Activity - 应用入口
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var themePreferences: ThemePreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        // 适配刘海屏/挖孔屏
        ScreenUtils.enableDisplayCutout(this)
        
        // 触发预加载（在后台异步进行）
        (application as? RuoLiJianZhenApp)?.startPreload()
        
        setContent {
            val themeMode by themePreferences.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by themePreferences.getDynamicColor().collectAsState(initial = true)
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            RuoLiJianZhenTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
