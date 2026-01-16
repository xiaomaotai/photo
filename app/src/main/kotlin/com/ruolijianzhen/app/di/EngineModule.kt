package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.data.engine.RecognitionEngineImpl
import com.ruolijianzhen.app.data.knowledge.KnowledgeEnhancerImpl
import com.ruolijianzhen.app.domain.engine.RecognitionEngine
import com.ruolijianzhen.app.domain.knowledge.KnowledgeEnhancer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {
    
    @Binds
    @Singleton
    abstract fun bindRecognitionEngine(
        impl: RecognitionEngineImpl
    ): RecognitionEngine
    
    @Binds
    @Singleton
    abstract fun bindKnowledgeEnhancer(
        impl: KnowledgeEnhancerImpl
    ): KnowledgeEnhancer
}
