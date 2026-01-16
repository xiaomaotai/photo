package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.domain.api.ApiManager
import com.ruolijianzhen.app.domain.recognition.OfflineRecognizer
import com.ruolijianzhen.app.util.PreloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 性能优化相关的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    
    @Provides
    @Singleton
    fun providePreloadManager(
        offlineRecognizer: OfflineRecognizer,
        apiManager: ApiManager
    ): PreloadManager {
        return PreloadManager(offlineRecognizer, apiManager)
    }
}
