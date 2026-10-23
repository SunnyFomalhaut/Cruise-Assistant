package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixieDustLinkageTest {

    private fun given(id: Long, deck: Int, room: String, levelId: Long, ts: Long) = PixieDustRecord(
        id = id, direction = PixieDustDirection.GIVEN, deck = deck, room = room,
        levelId = levelId, levelNameSnapshot = "", timestamp = ts,
    )

    private fun received(id: Long, deck: Int, room: String, levelId: Long, ts: Long, replied: Boolean = false) =
        PixieDustRecord(
            id = id, direction = PixieDustDirection.RECEIVED, deck = deck, room = room,
            levelId = levelId, levelNameSnapshot = "", replied = replied, timestamp = ts,
        )

    // ---- autoReplyTarget ----

    @Test
    fun `auto reply target picks earliest unreplied same-room received`() {
        val list = listOf(
            received(1, 11, "550", 1, 300),
            received(2, 11, "550", 2, 100),  // 最早未回礼 → 目标
            received(3, 11, "550", 3, 200),
        )
        assertEquals(2L, autoReplyTarget(list, 11, "550")?.id)
    }

    @Test
    fun `auto reply target skips replied records`() {
        val list = listOf(
            received(1, 11, "550", 1, 100, replied = true),
            received(2, 11, "550", 2, 200),
        )
        assertEquals(2L, autoReplyTarget(list, 11, "550")?.id)
    }

    @Test
    fun `auto reply target requires same deck and room`() {
        val list = listOf(
            received(1, 11, "551", 1, 100),
            received(2, 12, "550", 1, 100),
        )
        assertNull(autoReplyTarget(list, 11, "550"))
    }

    @Test
    fun `auto reply target ignores given records`() {
        assertNull(autoReplyTarget(listOf(given(1, 11, "550", 1, 100)), 11, "550"))
    }

    @Test
    fun `auto reply target empty list returns null`() {
        assertNull(autoReplyTarget(emptyList(), 11, "550"))
    }

    // ---- pairByRoom ----

    @Test
    fun `pair by room merges both directions and computes unreplied`() {
        val pairs = pairByRoom(
            given = listOf(given(1, 11, "550", 1, 100), given(2, 11, "550", 2, 200)),
            received = listOf(received(3, 11, "550", 3, 300), received(4, 11, "550", 1, 50, replied = true)),
        )
        assertEquals(1, pairs.size)
        val p = pairs[0]
        assertEquals(11, p.deck)
        assertEquals("550", p.room)
        assertEquals(listOf(1L, 2L), p.given.map { it.id })
        assertEquals(listOf(3L, 4L), p.received.map { it.id })
        assertEquals(listOf(3L), p.unrepliedReceived.map { it.id })
        assertEquals(300L, p.lastInteractTs)
    }

    @Test
    fun `pair by room includes single-direction rooms`() {
        val pairs = pairByRoom(
            given = listOf(given(1, 11, "550", 1, 100)),
            received = listOf(received(2, 12, "100", 1, 200)),
        )
        // 房 "100" 有未回礼收到（replied=false）→ 最前；房 "550" 纯发出（不平衡）→ 第二组
        assertEquals(listOf("100", "550"), pairs.map { it.room })
    }

    @Test
    fun `pair by room sorts unreplied-with-highest-rank first`() {
        // 房 A(11,100)：未回礼 rank=1；房 B(11,200)：未回礼 rank=3 → B 在前
        val pairs = pairByRoom(
            given = emptyList(),
            received = listOf(
                received(1, 11, "100", 1, 100),
                received(2, 11, "200", 3, 100),
            ),
            levelRankOf = { if (it.levelId == 1L) 1 else if (it.levelId == 2L) 2 else 3 },
        )
        assertEquals(listOf("200", "100"), pairs.map { it.room })
    }

    @Test
    fun `pair by room unreplied before unbalanced before balanced`() {
        // 房 U(11,100)：未回礼收到 → 第一组
        // 房 B1(11,200)：发出1 收到0 → 不平衡（无未回礼）
        // 房 S(11,300)：发出1 收到1 已回礼 → 平衡
        val pairs = pairByRoom(
            given = listOf(given(1, 11, "200", 1, 100), given(2, 11, "300", 1, 200)),
            received = listOf(
                received(3, 11, "100", 1, 50),
                received(4, 11, "300", 1, 150, replied = true),
            ),
        )
        assertEquals(listOf("100", "200", "300"), pairs.map { it.room })
    }

    @Test
    fun `pair by room same group sorts by last interact desc`() {
        // 两个平衡房：ts 大的在前
        val pairs = pairByRoom(
            given = listOf(
                given(1, 11, "100", 1, 100),
                given(2, 11, "200", 1, 900),
            ),
            received = listOf(
                received(3, 11, "100", 1, 50, replied = true),
                received(4, 11, "200", 1, 800, replied = true),
            ),
        )
        assertEquals(listOf("200", "100"), pairs.map { it.room })
    }

    @Test
    fun `pair by room empty inputs returns empty`() {
        assertTrue(pairByRoom(emptyList(), emptyList()).isEmpty())
    }

    // ---- hasGivenForRoom ----

    @Test
    fun `has given for room true when same deck and room given exists`() {
        val list = listOf(given(1, 11, "550", 1, 100))
        assertTrue(hasGivenForRoom(list, 11, "550"))
    }

    @Test
    fun `has given for room false when no given for room`() {
        val list = listOf(given(1, 11, "551", 1, 100), given(2, 12, "550", 1, 100))
        assertFalse(hasGivenForRoom(list, 11, "550"))
    }

    @Test
    fun `has given for room ignores received records`() {
        val list = listOf(received(1, 11, "550", 1, 100))
        assertFalse(hasGivenForRoom(list, 11, "550"))
    }

    @Test
    fun `has given for room respects existing replied count to prevent 1-to-many auto reply`() {
        val singleGiven = listOf(given(1, 11, "550", 1, 100))
        assertTrue(hasGivenForRoom(singleGiven, 11, "550", existingRepliedCount = 0))
        assertFalse(hasGivenForRoom(singleGiven, 11, "550", existingRepliedCount = 1))

        val twoGiven = listOf(given(1, 11, "550", 1, 100), given(2, 11, "550", 1, 200))
        assertTrue(hasGivenForRoom(twoGiven, 11, "550", existingRepliedCount = 1))
        assertFalse(hasGivenForRoom(twoGiven, 11, "550", existingRepliedCount = 2))
    }
}
