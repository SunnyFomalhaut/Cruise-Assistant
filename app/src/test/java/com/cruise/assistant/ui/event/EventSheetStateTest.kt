package com.cruise.assistant.ui.event

import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.MealPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventSheetStateTest {

    @Test
    fun `test EventSheetState note default is empty`() {
        val state = EventSheetState(
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
        )
        assertEquals("", state.note)
    }

    @Test
    fun `test EventSheetState note copy updates correctly`() {
        val state = EventSheetState(
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
        )
        val updated = state.copy(note = "请穿正装参加晚宴")
        assertEquals("请穿正装参加晚宴", updated.note)
    }

    @Test
    fun `test EventSheetState lane default is zero and copy updates correctly`() {
        val state = EventSheetState(
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
        )
        assertEquals(0, state.lane)
        val updated = state.copy(lane = 2)
        assertEquals(2, updated.lane)
    }

    @Test
    fun `test note trimming and null conversion for CruiseEvent`() {
        val blankNote = "   "
        val trimmedBlank = blankNote.trim().takeIf { it.isNotEmpty() }
        assertNull(trimmedBlank)

        val validNote = "  预约号 #123456  "
        val trimmedValid = validNote.trim().takeIf { it.isNotEmpty() }
        assertEquals("预约号 #123456", trimmedValid)

        val event = CruiseEvent(
            id = 1L,
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(18, 0),
            durationMinutes = 60,
            categoryId = 1L,
            title = "晚餐",
            note = trimmedValid,
        )
        assertEquals("预约号 #123456", event.note)
    }

    @Test
    fun `test EventSheetState presetKey default and copy updates correctly`() {
        val state = EventSheetState(
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
        )
        assertNull(state.presetKey)
        val updated = state.copy(presetKey = "dinner")
        assertEquals("dinner", updated.presetKey)
    }

    @Test
    fun `test resolveMealPeriod with title and fallback times`() {
        // Title match has priority
        assertEquals(MealPeriod.LUNCH, resolveMealPeriod("午餐", LocalTime.of(18, 0)))
        assertEquals(MealPeriod.BREAKFAST, resolveMealPeriod("早餐", LocalTime.of(12, 0)))
        assertEquals(MealPeriod.DINNER, resolveMealPeriod("晚餐", LocalTime.of(8, 0)))
        assertEquals(MealPeriod.LATE_NIGHT, resolveMealPeriod("夜宵", LocalTime.of(12, 0)))

        // Fallback by start time when title does not match
        assertEquals(MealPeriod.BREAKFAST, resolveMealPeriod("", LocalTime.of(8, 0)))
        assertEquals(MealPeriod.BREAKFAST, resolveMealPeriod("随便吃点", LocalTime.of(10, 29)))
        assertEquals(MealPeriod.LUNCH, resolveMealPeriod("", LocalTime.of(10, 30)))
        assertEquals(MealPeriod.LUNCH, resolveMealPeriod("家庭聚餐", LocalTime.of(14, 59)))
        assertEquals(MealPeriod.DINNER, resolveMealPeriod("", LocalTime.of(15, 0)))
        assertEquals(MealPeriod.DINNER, resolveMealPeriod("餐厅打卡", LocalTime.of(21, 29)))
        assertEquals(MealPeriod.LATE_NIGHT, resolveMealPeriod("", LocalTime.of(21, 30)))
        assertEquals(MealPeriod.LATE_NIGHT, resolveMealPeriod("深夜食堂", LocalTime.of(23, 0)))
    }

    @Test
    fun `test computeMealPeriodTitle when user selects meal period`() {
        // Default / empty title gets updated to meal period label
        assertEquals("早餐", computeMealPeriodTitle("", MealPeriod.BREAKFAST))
        assertEquals("早餐", computeMealPeriodTitle("晚餐", MealPeriod.BREAKFAST))
        assertEquals("午餐", computeMealPeriodTitle("早餐", MealPeriod.LUNCH))
        assertEquals("晚餐", computeMealPeriodTitle("午餐", MealPeriod.DINNER))
        assertEquals("夜宵", computeMealPeriodTitle("晚餐", MealPeriod.LATE_NIGHT))

        // Custom title is preserved
        assertEquals("船长晚宴", computeMealPeriodTitle("船长晚宴", MealPeriod.BREAKFAST))
        assertEquals("皇家下午茶", computeMealPeriodTitle("皇家下午茶", MealPeriod.LUNCH))
    }

    @Test
    fun `test computePresetKey for meal and non-meal categories`() {
        // For meal category, only DINNER gets presetKey = "dinner", other periods get null
        assertEquals("dinner", computePresetKey("meal", MealPeriod.DINNER, "dinner"))
        assertNull(computePresetKey("meal", MealPeriod.BREAKFAST, "dinner"))
        assertNull(computePresetKey("meal", MealPeriod.LUNCH, "dinner"))
        assertNull(computePresetKey("meal", MealPeriod.LATE_NIGHT, "dinner"))
        assertNull(computePresetKey("meal", null, "dinner"))

        // For non-meal categories, presetKey is preserved
        assertEquals("show_seas_adventure", computePresetKey("show", null, "show_seas_adventure"))
        assertEquals("safety_drill", computePresetKey("important", null, "safety_drill"))
        assertNull(computePresetKey("custom", null, null))
    }
}
