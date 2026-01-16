package com.ruolijianzhen.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ruolijianzhen.app.ui.history.HistoryScreen
import com.ruolijianzhen.app.ui.home.HomeScreen
import com.ruolijianzhen.app.ui.main.MainScreen
import com.ruolijianzhen.app.ui.settings.SettingsScreen

/**
 * 导航路由
 */
object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    
    // 保留旧路由名称的兼容性
    @Deprecated("Use CAMERA instead", ReplaceWith("CAMERA"))
    const val MAIN = CAMERA
}

// 动画时长
private const val ANIM_DURATION = 200

/**
 * 应用导航 - 优化页面切换动画
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
        exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
        popEnterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
        popExitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
    ) {
        // 首页
        composable(Routes.HOME) {
            HomeScreen(
                onStartRecognition = {
                    navController.navigate(Routes.CAMERA)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        
        // 相机/识别页面（原MainScreen）
        composable(
            Routes.CAMERA,
            enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
            popEnterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
            popExitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
        ) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 设置页面
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 历史记录页面
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
