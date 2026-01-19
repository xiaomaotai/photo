package com.ruolijianzhen.app.di

import android.content.Context
import com.ruolijianzhen.app.util.BitmapPool
import com.ruolijianzhen.app.util.ImageStorage
import com.ruolijianzhen.app.util.NetworkUtils
import com.ruolijianzhen.app.util.PerceptualHash
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 性能优化相关依赖模块
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    
    @Provides
    @Singleton
    fun provideNetworkUtils(
        @ApplicationContext context: Context
    ): NetworkUtils {
        return NetworkUtils(context).also {
            it.startMonitoring()
        }
    }
    
    @Provides
    @Singleton
    fun provideBitmapPool(): BitmapPool {
        return BitmapPool()
    }
    
    @Provides
    @Singleton
    fun providePerceptualHash(): PerceptualHash {
        return PerceptualHash()
    }
    
    @Provides
    @Singleton
    fun provideImageStorage(
        @ApplicationContext context: Context
    ): ImageStorage {
        return ImageStorage(context)
    }
}
