package com.ruolijianzhen.app.di

import com.ruolijianzhen.app.data.formatter.ResultFormatterImpl
import com.ruolijianzhen.app.data.recognition.OfflineRecognizerImpl
import com.ruolijianzhen.app.domain.formatter.ResultFormatter
import com.ruolijianzhen.app.domain.recognition.OfflineRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 识别模块 - 提供识别相关依赖绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RecognitionModule {

    @Binds
    @Singleton
    abstract fun bindOfflineRecognizer(impl: OfflineRecognizerImpl): OfflineRecognizer
    
    @Binds
    @Singleton
    abstract fun bindResultFormatter(impl: ResultFormatterImpl): ResultFormatter
}
