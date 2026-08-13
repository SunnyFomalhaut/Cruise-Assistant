package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ShipPresetTest {

    @Test
    fun nextOccurrence_sameDay_returnsToday() {
        // 2026-08-10 is a Monday
        val today = LocalDate.of(2026, 8, 10)
        assertEquals(today, nextOccurrenceOfDayOfWeek(DayOfWeek.MONDAY, today))
    }

    @Test
    fun nextOccurrence_futureDay_returnsNextOccurrence() {
        // 2026-08-11 is Tuesday; next Monday is 2026-08-17
        val tuesday = LocalDate.of(2026, 8, 11)
        assertEquals(
            LocalDate.of(2026, 8, 17),
            nextOccurrenceOfDayOfWeek(DayOfWeek.MONDAY, tuesday),
        )
    }

    @Test
    fun nextOccurrence_thursdayFromWednesday_returnsTomorrow() {
        // 2026-08-12 is Wednesday; next Thursday is 2026-08-13
        val wednesday = LocalDate.of(2026, 8, 12)
        assertEquals(
            LocalDate.of(2026, 8, 13),
            nextOccurrenceOfDayOfWeek(DayOfWeek.THURSDAY, wednesday),
        )
    }

    @Test
    fun builtInShips_containsDisneyAdventure() {
        assertEquals(1, BUILT_IN_SHIPS.size)
        assertEquals("Disney Adventure", BUILT_IN_SHIPS[0].name)
        assertEquals(2, BUILT_IN_SHIPS[0].schedules.size)
    }

    @Test
    fun disneyAdventure_schedule1_is3NightsMonToThu() {
        val s = BUILT_IN_SHIPS[0].schedules[0]
        assertEquals("四天三夜", s.label)
        assertEquals(3, s.nights)
        assertEquals(DayOfWeek.MONDAY, s.boardDayOfWeek)
        assertEquals(DayOfWeek.THURSDAY, s.disembarkDayOfWeek)
    }

    @Test
    fun disneyAdventure_schedule2_is4NightsThuToMon() {
        val s = BUILT_IN_SHIPS[0].schedules[1]
        assertEquals("五天四夜", s.label)
        assertEquals(4, s.nights)
        assertEquals(DayOfWeek.THURSDAY, s.boardDayOfWeek)
        assertEquals(DayOfWeek.MONDAY, s.disembarkDayOfWeek)
    }

    @Test
    fun boardingSlots_62Slots_from1145to0300() {
        val slots = boardingSlots()
        // 11:45-23:45 共 49 段 + 次日 0:00-3:00 共 13 段 = 62 段（设计文档 "60 段" 为笔误，
        // 枚举 11:45…3:00 的实际段数是 62；首末窗口 11:45-12:00 / 3:00-3:15 以设计为准）
        assertEquals(62, slots.size)
        assertEquals(LocalTime.of(11, 45), slots.first())
        assertEquals(LocalTime.of(3, 0), slots.last())
        // 跨午夜连续：23:45 后是 0:00
        assertTrue(slots.contains(LocalTime.of(23, 45)))
        assertTrue(slots.contains(LocalTime.of(0, 0)))
        assertEquals(LocalTime.of(0, 0), slots[slots.indexOf(LocalTime.of(23, 45)) + 1])
    }
}
