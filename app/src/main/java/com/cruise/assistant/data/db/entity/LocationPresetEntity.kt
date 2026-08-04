package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_preset",
    indices = [Index(value = ["deck", "zone", "placeName"], unique = true)],
)
data class LocationPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deck: Int,
    /** ShipZone.name */
    val zone: String,
    val placeName: String,
    val lastUsedEpochMs: Long,
    val useCount: Int,
)
