package com.cruise.assistant.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.dao.PixieLevelDao
import com.cruise.assistant.data.db.dao.VoyageArchiveDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.dao.WishlistDao
import com.cruise.assistant.data.db.entity.VoyageArchiveEntity
import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import com.cruise.assistant.data.db.entity.PixieDustEntity
import com.cruise.assistant.data.db.entity.PixieLevelEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.data.db.entity.WishlistItemEntity

@Database(
    entities = [
        VoyageEntity::class,
        CategoryEntity::class,
        EventEntity::class,
        DayPlanEntity::class,
        LocationPresetEntity::class,
        PixieDustEntity::class,
        PixieLevelEntity::class,
        VoyageArchiveEntity::class,
        WishlistItemEntity::class,
    ],
    version = 12,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
    ],
)
@TypeConverters(Converters::class)
abstract class CruiseDatabase : RoomDatabase() {
    abstract fun voyageDao(): VoyageDao
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun locationPresetDao(): LocationPresetDao
    abstract fun pixieDustDao(): PixieDustDao
    abstract fun pixieLevelDao(): PixieLevelDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun voyageArchiveDao(): VoyageArchiveDao

    companion object {
        @Volatile private var instance: CruiseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE event ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE event ADD COLUMN presetKey TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voyage ADD COLUMN minDeck INTEGER NOT NULL DEFAULT 1")
            }
        }

        // v9：礼宾优先登船标记（礼宾客户不受登船批次限制，11:00 即可登船）
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voyage ADD COLUMN isConcierge INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v10：联程航次各程晚数（JSON 数组文本如 "[3,4]"，null = 单程）
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voyage ADD COLUMN legNights TEXT")
            }
        }

        // v11：航次存档表（航次结束时全部业务表的精确快照，payloadJson 存 ArchiveSnapshot 序列化；
        // SQL 列定义必须与 VoyageArchiveEntity 生成的 schema 完全一致）
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `voyage_archive` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`shipName` TEXT, " +
                        "`boardDate` TEXT NOT NULL, " +
                        "`disembarkDate` TEXT NOT NULL, " +
                        "`nightsSummary` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`payloadJson` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_voyage_archive_createdAt` " +
                        "ON `voyage_archive` (`createdAt`)",
                )
            }
        }

        // v12：登岸兰卡威的程下标集合（JSON int 数组文本如 "[0,2]"，null = 无登岸程）
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voyage ADD COLUMN ashoreLegs TEXT")
            }
        }

        fun get(context: Context): CruiseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CruiseDatabase::class.java,
                    "cruise.db",
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11, MIGRATION_11_12,
                ).build().also { instance = it }
            }
    }
}
