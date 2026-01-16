package com.ruolijianzhen.app.di

import android.content.Context
import com.ruolijianzhen.app.data.ai.UserAiServiceImpl
import com.ruolijianzhen.app.data.preferences.UserAiConfigStore
import com.ruolijianzhen.app.domain.ai.UserAiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AI模块 - 提供AI相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindUserAiService(impl: UserAiServiceImpl): UserAiService
    
    companion object {
        @Provides
        @Singleton
        fun provideUserAiConfigStore(@ApplicationContext context: Context): UserAiConfigStore {
            return UserAiConfigStore(context)
        }
    }
}
