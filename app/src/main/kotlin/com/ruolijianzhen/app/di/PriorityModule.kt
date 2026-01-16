package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.data.priority.PriorityConfigStoreImpl
import com.ruolijianzhen.app.data.priority.PriorityManagerImpl
import com.ruolijianzhen.app.domain.priority.PriorityConfigStore
import com.ruolijianzhen.app.domain.priority.PriorityManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 优先级模块 - 提供优先级相关依赖绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PriorityModule {
    
    @Binds
    @Singleton
    abstract fun bindPriorityConfigStore(
        impl: PriorityConfigStoreImpl
    ): PriorityConfigStore
    
    @Binds
    @Singleton
    abstract fun bindPriorityManager(
        impl: PriorityManagerImpl
    ): PriorityManager
}
