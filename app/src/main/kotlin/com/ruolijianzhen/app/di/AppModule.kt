package com.ruolijianzhen.app.di

import android.content.Context
import androidx.room.Room
import com.ruolijianzhen.app.data.local.AppDatabase
import com.ruolijianzhen.app.data.local.DatabaseCallback
import com.ruolijianzhen.app.data.local.dao.ObjectDao
import com.ruolijianzhen.app.data.local.dao.QuotaDao
import com.ruolijianzhen.app.data.local.dao.HistoryDao
import com.ruolijianzhen.app.data.local.dao.LearningDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Hilt依赖注入模块 - 提供全局单例依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ruolijianzhen.db"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .addCallback(DatabaseCallback(CoroutineScope(Dispatchers.IO)))
        .build()
    }

    @Provides
    @Singleton
    fun provideObjectDao(database: AppDatabase): ObjectDao {
        return database.objectDao()
    }

    @Provides
    @Singleton
    fun provideQuotaDao(database: AppDatabase): QuotaDao {
        return database.quotaDao()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideLearningDao(database: AppDatabase): LearningDao {
        return database.learningDao()
    }
}
