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
    /** 礼宾（Concierge）客户：不受登船批次限制，11:00 即可优先登船 */
    val isConcierge: Boolean = false,
    /**
     * 联程航次各程晚数（如 [3, 4] = 3夜+4夜 back-to-back），空 = 单程（默认，兼容旧行为）。
     * 联程链交替拼接：3夜（周一起）止于周四，4夜恰从周四起，游客不下船、在船上办理出入境交接。
     */
    val legs: List<Int> = emptyList(),
    /**
     * 登岸兰卡威的程下标集合（对 legs 的 0-based 下标，仅对 4夜程有意义）：
     * 明年1月起部分 4夜航线变为 新加坡-兰卡威-新加坡，第三天停靠兰卡威岛，
     * 客人可选上岸游玩或留在船上。空集 = 全程不留岸（默认）。
     */
    val ashoreLegs: Set<Int> = emptySet(),
) {
    /** 航次总天数，含登船日和下船日 */
    val dayCount: Int
        get() = (disembarkDate.toEpochDay() - boardDate.toEpochDay()).toInt() + 1

    fun datesInVoyage(): List<LocalDate> =
        (0 until dayCount).map { boardDate.plusDays(it.toLong()) }

    /**
     * 各程登船日：首程 = boardDate，之后逐程累积（前一程登船日 + 前一程晚数）。
     * 单程（legs 为空）返回 [boardDate]。
     * 例：boardDate=周一、legs=[3,4] → [周一, 周四]。
     */
    fun legBoardDates(): List<LocalDate> {
        if (legs.isEmpty()) return listOf(boardDate)
        val result = mutableListOf(boardDate)
        for (i in 0 until legs.size - 1) {
            result += result.last().plusDays(legs[i].toLong())
        }
        return result
    }

    /**
     * 各程交接日（办理出入境交接手续的日子）= 除最后一程外各程的下船日。
     * 单程（legs 为空）无交接，返回空列表。
     * 例：boardDate=周一、legs=[3,4] → [周四]（周四早上不下船，在船上办手续继续第二程）。
     */
    fun junctionDates(): List<LocalDate> {
        if (legs.isEmpty()) return emptyList()
        val boardDates = legBoardDates()
        // 除最后一程外，每程的登船日 + 该程晚数即交接日（也是下一程的登船日）
        return boardDates.dropLast(1)
            .mapIndexed { i, board -> board.plusDays(legs[i].toLong()) }
    }

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
