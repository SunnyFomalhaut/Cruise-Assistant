package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PixieDustSweepSortingTest {

    private val levelStandard = PixieLevel(id = 1, name = "普通", colorArgb = 0, iconKey = "gift", rank = 1)
    private val levelDeluxe = PixieLevel(id = 2, name = "豪华", colorArgb = 0, iconKey = "gift", rank = 2)

    @Test
    fun testSortForSweepDelivery_SortedByDeckAndRoomAscending() {
        val r1 = PixieDustRecord(id = 1, direction = PixieDustDirection.RECEIVED, deck = 11, room = "216", levelId = 1, levelNameSnapshot = "普通", replied = false, timestamp = 100)
        val r2 = PixieDustRecord(id = 2, direction = PixieDustDirection.RECEIVED, deck = 6, room = "012", levelId = 1, levelNameSnapshot = "普通", replied = false, timestamp = 200)
        val r3 = PixieDustRecord(id = 3, direction = PixieDustDirection.RECEIVED, deck = 11, room = "200", levelId = 2, levelNameSnapshot = "豪华", replied = false, timestamp = 300)
        val r4 = PixieDustRecord(id = 4, direction = PixieDustDirection.RECEIVED, deck = 11, room = "200", levelId = 1, levelNameSnapshot = "普通", replied = true, timestamp = 400)

        val sorted = sortForSweepDelivery(listOf(r1, r2, r3, r4), listOf(levelStandard, levelDeluxe))

        // Order should be:
        // 1. Deck 6, Room 012
        // 2. Deck 11, Room 200 (unreplied)
        // 3. Deck 11, Room 200 (replied)
        // 4. Deck 11, Room 216
        assertEquals(listOf(2L, 3L, 4L, 1L), sorted.map { it.id })
    }

    @Test
    fun testPairByRoomForSweep_SortedByDeckAndRoomAscending() {
        val g1 = PixieDustRecord(id = 1, direction = PixieDustDirection.GIVEN, deck = 11, room = "216", levelId = 1, levelNameSnapshot = "普通", replied = false, timestamp = 100)
        val r1 = PixieDustRecord(id = 2, direction = PixieDustDirection.RECEIVED, deck = 6, room = "012", levelId = 1, levelNameSnapshot = "普通", replied = false, timestamp = 200)

        val pairs = pairByRoomForSweep(listOf(g1), listOf(r1))
        assertEquals(2, pairs.size)
        assertEquals(6, pairs[0].deck)
        assertEquals("012", pairs[0].room)
        assertEquals(11, pairs[1].deck)
        assertEquals("216", pairs[1].room)
    }
}
