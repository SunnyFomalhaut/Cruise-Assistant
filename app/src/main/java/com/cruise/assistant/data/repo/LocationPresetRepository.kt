package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocationPresetRepository(private val dao: LocationPresetDao) {

    fun observeAll(): Flow<List<LocationPreset>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 保存事件时调用：已存在则 useCount+1 并更新时间，否则新建 */
    suspend fun remember(deck: Int, zone: ShipZone, placeName: String, nowMs: Long) {
        val trimmed = placeName.trim()
        if (trimmed.isEmpty()) return
        val existing = dao.find(deck, zone.name, trimmed)
        if (existing == null) {
            dao.upsert(
                LocationPreset(
                    deck = deck, zone = zone, placeName = trimmed,
                    lastUsedEpochMs = nowMs, useCount = 1,
                ).toEntity(),
            )
        } else {
            dao.upsert(existing.copy(lastUsedEpochMs = nowMs, useCount = existing.useCount + 1))
        }
    }

    suspend fun delete(preset: LocationPreset) = dao.delete(preset.toEntity())
}
