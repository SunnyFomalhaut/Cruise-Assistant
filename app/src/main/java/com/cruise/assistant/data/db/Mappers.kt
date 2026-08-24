package com.cruise.assistant.data.db

import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.WishlistItemEntity
import com.cruise.assistant.domain.WishlistItem
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import com.cruise.assistant.data.db.entity.PixieDustEntity
import com.cruise.assistant.data.db.entity.PixieLevelEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.Voyage

fun VoyageEntity.toDomain() = Voyage(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck, minDeck,
)

fun Voyage.toEntity() = VoyageEntity(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck, minDeck,
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
    isActive = isActive, presetKey = presetKey, waitMinutes = waitMinutes,
    note = note,
)

fun CruiseEvent.toEntity() = EventEntity(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck, zone = zone?.name,
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
    isActive = isActive, presetKey = presetKey, waitMinutes = waitMinutes,
    note = note,
)

fun DayPlanEntity.toDomain() = DayPlan(date, finalized, finalizedAt)
fun DayPlan.toEntity() = DayPlanEntity(date, finalized, finalizedAt)

fun LocationPresetEntity.toDomain() = LocationPreset(
    id, deck, ShipZone.valueOf(zone), placeName, lastUsedEpochMs, useCount,
)

fun LocationPreset.toEntity() = LocationPresetEntity(
    id, deck, zone.name, placeName, lastUsedEpochMs, useCount,
)

fun PixieDustEntity.toDomain() = PixieDustRecord(
    id = id, direction = PixieDustDirection.valueOf(direction), deck = deck, room = room,
    levelId = levelId, levelNameSnapshot = levelNameSnapshot, replied = replied, note = note,
    timestamp = timestamp,
)

fun PixieDustRecord.toEntity() = PixieDustEntity(
    id = id, direction = direction.name, deck = deck, room = room,
    levelId = levelId, levelNameSnapshot = levelNameSnapshot, replied = replied, note = note,
    timestamp = timestamp,
)

fun PixieLevelEntity.toDomain() = PixieLevel(id, name, colorArgb, iconKey, rank, sortOrder)

fun PixieLevel.toEntity() = PixieLevelEntity(id, name, colorArgb, iconKey, rank, sortOrder)

fun WishlistItemEntity.toDomain(category: com.cruise.assistant.domain.Category? = null) = WishlistItem(
    id = id,
    title = title,
    categoryId = categoryId,
    category = category,
    deck = deck,
    zone = zone?.let { com.cruise.assistant.domain.ShipZone.valueOf(it) },
    placeName = placeName,
    note = note,
    isCompleted = isCompleted,
    createdAt = createdAt,
)

fun WishlistItem.toEntity() = WishlistItemEntity(
    id = id,
    title = title,
    categoryId = categoryId,
    deck = deck,
    zone = zone?.name,
    placeName = placeName,
    note = note,
    isCompleted = isCompleted,
    createdAt = createdAt,
)
