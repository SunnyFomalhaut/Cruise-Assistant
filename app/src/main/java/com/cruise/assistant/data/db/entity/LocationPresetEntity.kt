package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 自然键是 (deck, zone, placeName)（由唯一索引强制），而 [id] 只是自增代理键。
 * 二者不一致，正是 [com.cruise.assistant.data.db.dao.LocationPresetDao.upsert] 要求
 * 调用方必须先 [com.cruise.assistant.data.db.dao.LocationPresetDao.find] 再 copy 的原因。
 */
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
