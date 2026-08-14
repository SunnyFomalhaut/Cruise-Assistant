package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixieDustTest {

    private val levels = listOf(
        PixieLevel(id = 1, name = "普通", colorArgb = 0, iconKey = "gift", rank = 1, sortOrder = 0),
        PixieLevel(id = 2, name = "精致", colorArgb = 0, iconKey = "sparkle", rank = 2, sortOrder = 1),
        PixieLevel(id = 3, name = "豪华", colorArgb = 0, iconKey = "diamond", rank = 3, sortOrder = 2),
    )

    private fun given(id: Long, deck: Int, room: String, levelId: Long, ts: Long) = PixieDustRecord(
        id = id, direction = PixieDustDirection.GIVEN, deck = deck, room = room,
        levelId = levelId, levelNameSnapshot = "", timestamp = ts,
    )

    private fun received(id: Long, deck: Int, room: String, levelId: Long, ts: Long, replied: Boolean = false) =
        PixieDustRecord(
            id = id, direction = PixieDustDirection.RECEIVED, deck = deck, room = room,
            levelId = levelId, levelNameSnapshot = "", replied = replied, timestamp = ts,
        )

    @Test
    fun `room input requires exactly three digits`() {
        assertTrue(isValidRoomInput("550"))
        assertFalse(isValidRoomInput("55"))
        assertFalse(isValidRoomInput("5500"))
        assertFalse(isValidRoomInput("5a0"))
        assertFalse(isValidRoomInput(""))
    }

    @Test
    fun `full room label concatenates deck and room`() {
        assertEquals("11550", given(1, 11, "550", 1, 0).fullRoom)
    }

    @Test
    fun `level counts sorted by rank desc`() {
        val counts = levelCountsOf(
            listOf(given(1, 11, "550", 1, 0), given(2, 11, "551", 1, 1), given(3, 11, "552", 3, 2)),
            levels,
        )
        assertEquals(arrayListOf(3L to 1, 1L to 2), counts.map { it.level.id to it.count })
    }

    @Test
    fun `level counts fall back to snapshot name for deleted level`() {
        val counts = levelCountsOf(listOf(given(1, 11, "550", 99, 0).copy(levelNameSnapshot = "绝版")), levels)
        assertEquals(1, counts.size)
        assertEquals("绝版", counts[0].level.name)
        assertEquals(1, counts[0].count)
    }

    @Test
    fun `given grouped by room sorted by last timestamp desc`() {
        val groups = groupGivenByRoom(
            listOf(
                given(1, 11, "550", 1, 100),
                given(2, 11, "550", 3, 300),
                given(3, 12, "100", 2, 200),
            ),
            levels,
        )
        assertEquals(2, groups.size)
        assertEquals("550", groups[0].room)   // 最近一次 ts=300
        assertEquals(2, groups[0].total)
        assertEquals(3, groups[0].levelCounts[0].level.id) // 组内也按 rank 降序
        assertEquals("100", groups[1].room)
    }

    @Test
    fun `received sorted unreplied first by rank desc then replied by time desc`() {
        val sorted = sortReceived(
            listOf(
                received(1, 11, "550", 1, 100, replied = true),
                received(2, 11, "551", 3, 50),          // 豪华未回礼 → 第一
                received(3, 11, "552", 1, 60),          // 普通未回礼 → 第三（rank 3>2>1）
                received(4, 11, "553", 2, 70),          // 精致未回礼 → 第二
                received(5, 11, "554", 3, 80, replied = true),  // 已回礼 ts=80 → 第六
                received(6, 11, "555", 1, 90, replied = true),  // 已回礼 ts=90 → 第五
            ),
            levels,
        )
        assertEquals(listOf(2L, 4L, 3L, 1L, 6L, 5L), sorted.map { it.id })
    }

    @Test
    fun `received sorting ignores given records`() {
        val sorted = sortReceived(listOf(given(1, 11, "550", 1, 100)), levels)
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `last level for room returns latest given only`() {
        val records = listOf(
            given(1, 11, "550", 1, 100),
            given(2, 11, "550", 3, 300),
            given(3, 12, "550", 2, 999),   // 不同楼层不算
            given(4, 11, "551", 2, 400),
        )
        assertEquals(2L, lastLevelForRoom(records, 11, "550")?.id)
        assertNull(lastLevelForRoom(records, 11, "999"))
    }
}
