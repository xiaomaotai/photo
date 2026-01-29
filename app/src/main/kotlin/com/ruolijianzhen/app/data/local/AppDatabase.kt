package com.ruolijianzhen.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ruolijianzhen.app.data.local.dao.ObjectDao
import com.ruolijianzhen.app.data.local.dao.QuotaDao
import com.ruolijianzhen.app.data.local.dao.HistoryDao
import com.ruolijianzhen.app.data.local.dao.LearningDao
import com.ruolijianzhen.app.data.local.entity.ObjectEntity
import com.ruolijianzhen.app.data.local.entity.LearningObjectEntity
import com.ruolijianzhen.app.data.local.entity.ApiQuotaEntity
import com.ruolijianzhen.app.data.local.entity.HistoryEntity

/**
 * Room数据库 - 若里见真应用数据库
 */
@Database(
    entities = [
        ObjectEntity::class,
        LearningObjectEntity::class,
        ApiQuotaEntity::class,
        HistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun objectDao(): ObjectDao
    abstract fun quotaDao(): QuotaDao
    abstract fun historyDao(): HistoryDao
    abstract fun learningDao(): LearningDao

    companion object {
        /**
         * 数据库迁移：版本1 -> 版本2
         * 添加 isFavorite 字段到 history 表
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE history ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 数据库迁移：版本2 -> 版本3
         * 添加完整的ObjectInfo字段到history表
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 详细信息字段
                database.execSQL("ALTER TABLE history ADD COLUMN brand TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN model TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN species TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN priceRange TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN material TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN color TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN size TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN manufacturer TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN features TEXT")

                // 百科知识字段
                database.execSQL("ALTER TABLE history ADD COLUMN summary TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN description TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN historyText TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN funFacts TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN tips TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN relatedTopics TEXT")

                // 分类特定字段
                database.execSQL("ALTER TABLE history ADD COLUMN objectType TEXT NOT NULL DEFAULT 'GENERAL'")
                database.execSQL("ALTER TABLE history ADD COLUMN typeSpecificInfo TEXT")
                database.execSQL("ALTER TABLE history ADD COLUMN additionalInfo TEXT")
            }
        }
    }
}
