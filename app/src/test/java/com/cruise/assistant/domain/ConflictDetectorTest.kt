package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConflictDetectorTest {

    private val day = LocalDate.of(2026, 9, 3)

    private fun ev(id: Long, hour: Int, minute: Int, durMin: Int) = CruiseEvent(
        id = id, date = day, startTime = LocalTime.of(hour, minute),
        durationMinutes = durMin, categoryId = 1, title = "e" + id,
    )

    @Test
    fun emptyList_hasNoConflicts() {
        assertTrue(findConflicts(emptyList()).isEmpty())
    }

    @Test
    fun singleEvent_hasNoConflicts() {
        assertTrue(findConflicts(listOf(ev(1, 13, 0, 30))).isEmpty())
    }

    @Test
    fun sequentialEvents_haveNoConflicts() {
        val events = listOf(ev(1, 12, 0, 30), ev(2, 13, 0, 30), ev(3, 14, 0, 30))
        assertTrue(findConflicts(events).isEmpty())
    }

    @Test
    fun backToBackEvents_haveNoConflicts() {
        // 13:00-13:30 紧接 13:30-14:00
        val events = listOf(ev(1, 13, 0, 30), ev(2, 13, 30, 30))
        assertTrue(findConflicts(events).isEmpty())
    }

    @Test
    fun overlappingPair_isReportedOnce() {
        val events = listOf(ev(1, 13, 0, 45), ev(2, 13, 20, 60))
        val conflicts = findConflicts(events)
        assertEquals(1, conflicts.size)
        assertEquals(1L, conflicts[0].first.id)
        assertEquals(2L, conflicts[0].second.id)
    }

    @Test
    fun threeMutuallyOverlappingEvents_yieldThreePairs() {
        val events = listOf(ev(1, 13, 0, 60), ev(2, 13, 10, 60), ev(3, 13, 20, 60))
        assertEquals(3, findConflicts(events).size)
    }

    @Test
    fun conflicts_areOrderedByStartTime() {
        val events = listOf(ev(2, 15, 0, 60), ev(1, 13, 0, 60), ev(3, 15, 30, 60))
        val conflicts = findConflicts(events)
        assertEquals(1, conflicts.size)
        // 先开始的排在 first
        assertEquals(2L, conflicts[0].first.id)
        assertEquals(3L, conflicts[0].second.id)
    }

    @Test
    fun identicalEvents_conflict() {
        val events = listOf(ev(1, 13, 0, 30), ev(2, 13, 0, 30))
        assertEquals(1, findConflicts(events).size)
    }

    @Test
    fun lane_doesNotAffectConflictDetection() {
        // 分在不同栏只是显示上不重叠，时间上仍然算冲突
        val events = listOf(
            ev(1, 13, 0, 60).copy(lane = 0),
            ev(2, 13, 10, 60).copy(lane = 1),
        )
        assertEquals(1, findConflicts(events).size)
    }

    @Test
    fun zeroDurationEvent_neverConflicts() {
        val events = listOf(ev(1, 13, 0, 60), ev(2, 13, 30, 0))
        assertTrue(findConflicts(events).isEmpty())
    }
}
