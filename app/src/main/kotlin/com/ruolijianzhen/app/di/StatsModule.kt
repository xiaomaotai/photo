package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.data.stats.StatsRepositoryImpl
import com.ruolijianzhen.app.domain.stats.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StatsModule {
    
    @Binds
    @Singleton
    abstract fun bindStatsRepository(
        impl: StatsRepositoryImpl
    ): StatsRepository
}
