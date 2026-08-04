package com.cruise.assistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import com.cruise.assistant.data.db.entity.VoyageEntity

@Database(
    entities = [
        VoyageEntity::class,
        CategoryEntity::class,
        EventEntity::class,
        DayPlanEntity::class,
        LocationPresetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CruiseDatabase : RoomDatabase() {
    abstract fun voyageDao(): VoyageDao
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun locationPresetDao(): LocationPresetDao

    companion object {
        @Volatile private var instance: CruiseDatabase? = null

        fun get(context: Context): CruiseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CruiseDatabase::class.java,
                    "cruise.db",
                ).build().also { instance = it }
            }
    }
}
