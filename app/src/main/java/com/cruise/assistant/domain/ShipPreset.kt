package com.cruise.assistant.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class SchedulePreset(
    val label: String,
    val nights: Int,
    val boardDayOfWeek: DayOfWeek,
    val disembarkDayOfWeek: DayOfWeek,
    val boardTime: LocalTime,
    val disembarkTime: LocalTime,
) {
    /** Compute board date as the next occurrence of boardDayOfWeek from `today` (inclusive). */
    fun boardDateFrom(today: LocalDate): LocalDate =
        nextOccurrenceOfDayOfWeek(boardDayOfWeek, today)

    /** Compute disembark date = board date + nights. */
    fun disembarkDateFrom(today: LocalDate): LocalDate =
        boardDateFrom(today).plusDays(nights.toLong())
}

data class ShipPreset(
    val name: String,
    val schedules: List<SchedulePreset>,
    /** 内置船可用的最高甲板层数；null 表示不锁定（自定义船期/船名时使用） */
    val maxDeck: Int? = null,
    /** 内置船客用最低甲板层数（如迪士尼探险号 Deck 5-19 的 5）；null 表示无下限（Deck 1） */
    val minDeck: Int? = null,
)

/**
 * Returns the date of the next occurrence of `target` on or after `today`.
 * If `today` itself is the target day-of-week, returns `today`.
 */
fun nextOccurrenceOfDayOfWeek(target: DayOfWeek, today: LocalDate): LocalDate {
    val diff = (target.value - today.dayOfWeek.value + 7) % 7
    return today.plusDays(diff.toLong())
}

/** 登船时间段选项：11:45 起每 15 分钟一段至 3:00（次日凌晨），共 62 段（11:45-23:45 共 49 段 + 0:00-3:00 共 13 段；迪士尼探险号 PAT） */
fun boardingSlots(): List<LocalTime> {
    val startMin = 11 * 60 + 45          // 11:45
    val endMin = 24 * 60 + 3 * 60        // 次日 3:00
    return buildList {
        var m = startMin
        while (m <= endMin) {
            add(LocalTime.of((m / 60) % 24, m % 60))
            m += 15
        }
    }
}

val BUILT_IN_SHIPS: List<ShipPreset> = listOf(
    ShipPreset(
        name = "Disney Adventure",
        schedules = listOf(
            SchedulePreset(
                label = "四天三夜",
                nights = 3,
                boardDayOfWeek = DayOfWeek.MONDAY,
                disembarkDayOfWeek = DayOfWeek.THURSDAY,
                boardTime = LocalTime.of(12, 0),
                disembarkTime = LocalTime.of(7, 0),
            ),
            SchedulePreset(
                label = "五天四夜",
                nights = 4,
                boardDayOfWeek = DayOfWeek.THURSDAY,
                disembarkDayOfWeek = DayOfWeek.MONDAY,
                boardTime = LocalTime.of(12, 0),
                disembarkTime = LocalTime.of(7, 0),
            ),
        ),
        maxDeck = 19,
        minDeck = 5,
    ),
)
