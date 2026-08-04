package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LaneAssignerTest {

    private val day = LocalDate.of(2026, 9, 3)

    private fun ev(id: Long, hour: Int, minute: Int, durMin: Int, lane: Int) = CruiseEvent(
        id = id, date = day, startTime = LocalTime.of(hour, minute),
        durationMinutes = durMin, categoryId = 1, title = "e" + id, lane = lane,
    )

    @Test
    fun overlaps_isTrue_whenRangesIntersect() {
        assertTrue(overlaps(100, 200, 150, 250))
        assertTrue(overlaps(150, 250, 100, 200))
        assertTrue(overlaps(100, 300, 150, 200)) // 完全包含
    }

    @Test
    fun adjacentRanges_doNotOverlap() {
        // 13:00-13:30 与 13:30-14:00 相邻，不算冲突
        assertFalse(overlaps(780, 810, 810, 840))
        assertFalse(overlaps(810, 840, 780, 810))
    }

    @Test
    fun identicalRanges_overlap() {
        assertTrue(overlaps(780, 810, 780, 810))
    }

    @Test
    fun firstEvent_goesToLane0() {
        assertEquals(0, assignLane(780, 810, emptyList()))
    }

    @Test
    fun nonOverlappingEvent_reusesLane0() {
        val existing = listOf(ev(1, 13, 0, 30, lane = 0))
        // 14:00-14:30 与栏0 的 13:00-13:30 不重叠
        assertEquals(0, assignLane(840, 870, existing))
    }

    @Test
    fun overlappingEvent_fallsToLane1() {
        val existing = listOf(ev(1, 13, 0, 60, lane = 0))
        // 13:15-13:45 与栏0 重叠
        assertEquals(1, assignLane(795, 825, existing))
    }

    @Test
    fun twoOverlappingEvents_pushThirdToNarrowLane2() {
        val existing = listOf(
            ev(1, 13, 0, 60, lane = 0),
            ev(2, 13, 10, 60, lane = 1),
        )
        assertEquals(2, assignLane(800, 830, existing))
    }

    @Test
    fun allThreeLanesBusy_returnsLane2AsOverflow() {
        val existing = listOf(
            ev(1, 13, 0, 60, lane = 0),
            ev(2, 13, 0, 60, lane = 1),
            ev(3, 13, 0, 60, lane = 2),
        )
        assertEquals(LANE_COUNT - 1, assignLane(790, 820, existing))
    }

    @Test
    fun excludeId_letsEventKeepItsOwnLane_whenEditing() {
        val existing = listOf(ev(7, 13, 0, 60, lane = 0))
        // 编辑事件 7 本身，应忽略自己 → 仍可回到栏0
        assertEquals(0, assignLane(780, 840, existing, excludeId = 7L))
    }

    @Test
    fun higherLaneEvents_areIgnored_whenLowerLaneIsFree() {
        val existing = listOf(ev(1, 13, 0, 60, lane = 2))
        assertEquals(0, assignLane(780, 840, existing))
    }
}
