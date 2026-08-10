package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

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
}
