package com.cruise.assistant.ui.event

import com.cruise.assistant.domain.CruiseEvent
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
}
