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
import org.json.JSONArray

/** 联程各程晚数 List ↔ JSON 数组文本（如 "[3,4]"）的双向转换 */
fun List<Int>.toLegNightsJson(): String? =
    if (isEmpty()) null else JSONArray(this).toString()

fun String?.toLegNights(): List<Int> {
    if (isNullOrBlank()) return emptyList()
    // 数据库出现脏数据（手改/旧版本残留）时不让整表加载崩溃，兜底为单程
    return runCatching {
        val array = JSONArray(this)
        (0 until array.length()).map { array.getInt(it) }
    }.getOrDefault(emptyList())
}

/** 登岸兰卡威的程下标集合 Set ↔ JSON int 数组文本（如 "[0,2]"）的双向转换 */
fun Set<Int>.toAshoreLegsJson(): String? =
    if (isEmpty()) null else JSONArray(this).toString()

fun String?.toAshoreLegs(): Set<Int> {
    if (isNullOrBlank()) return emptySet()
    // 数据库出现脏数据（手改/旧版本残留）时不让整表加载崩溃，兜底为无登岸程
    return runCatching {
        val array = JSONArray(this)
        (0 until array.length()).map { array.getInt(it) }.toSet()
    }.getOrDefault(emptySet())
}

fun VoyageEntity.toDomain() = Voyage(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck, minDeck, isConcierge,
    legs = legNights.toLegNights(),
    ashoreLegs = ashoreLegs.toAshoreLegs(),
)

fun Voyage.toEntity() = VoyageEntity(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck, minDeck, isConcierge,
    legNights = legs.toLegNightsJson(),
    ashoreLegs = ashoreLegs.toAshoreLegsJson(),
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

// 数据库出现未知 zone 值（旧库脏数据/枚举改名）时兜底为船中，避免整表加载崩溃
fun LocationPresetEntity.toDomain() = LocationPreset(
    id, deck, runCatching { ShipZone.valueOf(zone) }.getOrNull() ?: ShipZone.MID,
    placeName, lastUsedEpochMs, useCount,
)

fun LocationPreset.toEntity() = LocationPresetEntity(
    id, deck, zone.name, placeName, lastUsedEpochMs, useCount,
)

// 数据库出现未知 direction 值（旧库脏数据/枚举改名）时兜底为 GIVEN，避免整表加载崩溃
fun PixieDustEntity.toDomain() = PixieDustRecord(
    id = id,
    direction = runCatching { PixieDustDirection.valueOf(direction) }
        .getOrNull() ?: PixieDustDirection.GIVEN,
    deck = deck, room = room,
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
    zone = zone?.let { runCatching { com.cruise.assistant.domain.ShipZone.valueOf(it) }.getOrNull() },
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
