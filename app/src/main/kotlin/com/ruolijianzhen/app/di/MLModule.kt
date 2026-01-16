package com.ruolijianzhen.app.di

import android.content.Context
import com.ruolijianzhen.app.data.ml.TFLiteClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ML模块 - 提供机器学习相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideTFLiteClassifier(@ApplicationContext context: Context): TFLiteClassifier {
        return TFLiteClassifier(context)
    }
}
