package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.data.learning.LearningRepositoryImpl
import com.ruolijianzhen.app.domain.learning.LearningRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LearningModule {
    
    @Binds
    @Singleton
    abstract fun bindLearningRepository(
        impl: LearningRepositoryImpl
    ): LearningRepository
}
