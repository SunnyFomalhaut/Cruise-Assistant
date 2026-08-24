package com.cruise.assistant.domain

import java.time.LocalDate
import java.time.LocalTime

/** 3 栏布局的栏数：栏0/栏1 等宽偏左，栏2 更窄贴最右 */
const val LANE_COUNT = 3

enum class ShipZone(val label: String) {
    BOW("船头"), MID("船中"), STERN("船尾")
}

data class Voyage(
    val id: Int = 1,
    val shipName: String? = null,
    val boardDate: LocalDate,
    val boardTime: LocalTime,
    val disembarkDate: LocalDate,
    val disembarkTime: LocalTime = LocalTime.of(7, 0),
    val maxDeck: Int = 20,
    /** 客用最低楼层（如迪士尼探险号 Deck 5-19，minDeck=5） */
    val minDeck: Int = 1,
) {
    /** 航次总天数，含登船日和下船日 */
    val dayCount: Int
        get() = (disembarkDate.toEpochDay() - boardDate.toEpochDay()).toInt() + 1

    fun datesInVoyage(): List<LocalDate> =
        (0 until dayCount).map { boardDate.plusDays(it.toLong()) }

    fun containsDate(date: LocalDate): Boolean =
        !date.isBefore(boardDate) && !date.isAfter(disembarkDate)

    /** date 是第几天（1-based）；不在航次内返回 null */
    fun dayIndexOf(date: LocalDate): Int? =
        if (containsDate(date)) (date.toEpochDay() - boardDate.toEpochDay()).toInt() + 1 else null

    /** 把任意日期夹到航次范围内，用于启动时定位 */
    fun clampDate(date: LocalDate): LocalDate = when {
        date.isBefore(boardDate) -> boardDate
        date.isAfter(disembarkDate) -> disembarkDate
        else -> date
    }
}

data class Category(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    val parentId: Long? = null,
    val isBuiltIn: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)

data class CruiseEvent(
    val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val categoryId: Long,
    val title: String,
    val deck: Int? = null,
    val zone: ShipZone? = null,
    val placeName: String? = null,
    val lane: Int = 0,
    val recurrenceGroupId: String? = null,
    val reminderMinutesBefore: Int? = null,
    val isSystemGenerated: Boolean = false,
    val isActive: Boolean = true,
    val presetKey: String? = null,
    val waitMinutes: Int? = null,
    val note: String? = null,
)

val CruiseEvent.startMinute: Int get() = startTime.hour * 60 + startTime.minute
val CruiseEvent.endMinute: Int get() = startMinute + durationMinutes

data class DayPlan(
    val date: LocalDate,
    val finalized: Boolean = false,
    val finalizedAt: Long? = null,
)

data class LocationPreset(
    val id: Long = 0,
    val deck: Int,
    val zone: ShipZone,
    val placeName: String,
    val lastUsedEpochMs: Long = 0,
    val useCount: Int = 0,
)
