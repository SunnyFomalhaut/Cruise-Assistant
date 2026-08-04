package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPresetDao {
    @Query("SELECT * FROM location_preset ORDER BY useCount DESC, lastUsedEpochMs DESC")
    fun observeAll(): Flow<List<LocationPresetEntity>>

    @Query("SELECT * FROM location_preset WHERE deck = :deck AND zone = :zone AND placeName = :placeName")
    suspend fun find(deck: Int, zone: String, placeName: String): LocationPresetEntity?

    @Upsert
    suspend fun upsert(preset: LocationPresetEntity)

    @Delete
    suspend fun delete(preset: LocationPresetEntity)

    @Query("DELETE FROM location_preset")
    suspend fun clearAll()
}
