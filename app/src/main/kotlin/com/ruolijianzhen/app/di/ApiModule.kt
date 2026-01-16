package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.data.api.ApiManagerImpl
import com.ruolijianzhen.app.data.quota.QuotaTrackerImpl
import com.ruolijianzhen.app.domain.api.ApiManager
import com.ruolijianzhen.app.domain.quota.QuotaTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * API模块 - 提供API相关依赖绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {

    @Binds
    @Singleton
    abstract fun bindQuotaTracker(impl: QuotaTrackerImpl): QuotaTracker
    
    @Binds
    @Singleton
    abstract fun bindApiManager(impl: ApiManagerImpl): ApiManager
}
