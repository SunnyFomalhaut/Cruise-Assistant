package com.cruise.assistant.data.db

import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.Voyage

fun VoyageEntity.toDomain() = Voyage(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck,
)

fun Voyage.toEntity() = VoyageEntity(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck,
)

fun CategoryEntity.toDomain() = Category(
    id, name, colorArgb, iconKey, parentId, isBuiltIn, isArchived, sortOrder,
)

fun Category.toEntity() = CategoryEntity(
    id, name, colorArgb, iconKey, parentId, isBuiltIn, isArchived, sortOrder,
)

fun EventEntity.toDomain() = CruiseEvent(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck,
    zone = zone?.let { runCatching { ShipZone.valueOf(it) }.getOrNull() },
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
    isActive = isActive,
)

fun CruiseEvent.toEntity() = EventEntity(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck, zone = zone?.name,
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
    isActive = isActive,
)

fun DayPlanEntity.toDomain() = DayPlan(date, finalized, finalizedAt)
fun DayPlan.toEntity() = DayPlanEntity(date, finalized, finalizedAt)

fun LocationPresetEntity.toDomain() = LocationPreset(
    id, deck, ShipZone.valueOf(zone), placeName, lastUsedEpochMs, useCount,
)

fun LocationPreset.toEntity() = LocationPresetEntity(
    id, deck, zone.name, placeName, lastUsedEpochMs, useCount,
)
