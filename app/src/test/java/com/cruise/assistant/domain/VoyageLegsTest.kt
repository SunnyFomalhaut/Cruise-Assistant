package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * 联程航次 Voyage 派生函数单测：legBoardDates / junctionDates。
 * 2026-09-07 是周一（3夜船期登船日），09-10 周四（4夜船期登船日）。
 */
class VoyageLegsTest {
    private val board = LocalDate.of(2026, 9, 7)

    private fun voyage(legs: List<Int>, nights: Int = legs.sum()) = Voyage(
        id = 1,
        shipName = "Disney Adventure",
        boardDate = board,
        boardTime = LocalTime.of(12, 0),
        disembarkDate = board.plusDays(nights.toLong()),
        legs = legs,
    )

    @Test
    fun `legBoardDates and junctionDates for 3 plus 4`() {
        val v = voyage(listOf(3, 4))
        // 各程登船日：首程 = 周一，第二程 = 周四（周一 + 3 晚）
        assertEquals(listOf(board, board.plusDays(3)), v.legBoardDates())
        // 交接日 = 除最后一程外各程的下船日：恰为第二程登船日（周四）
        assertEquals(listOf(board.plusDays(3)), v.junctionDates())
        assertEquals(8, v.dayCount)
    }

    @Test
    fun `legBoardDates and junctionDates for 3 plus 4 plus 3`() {
        val v = voyage(listOf(3, 4, 3))
        assertEquals(
            listOf(board, board.plusDays(3), board.plusDays(7)),
            v.legBoardDates(),
        )
        assertEquals(
            listOf(board.plusDays(3), board.plusDays(7)),
            v.junctionDates(),
        )
    }

    @Test
    fun `empty legs means single leg with no junctions`() {
        val v = voyage(emptyList(), nights = 3)
        // 单程：各程登船日就是 boardDate，无交接日
        assertEquals(listOf(board), v.legBoardDates())
        assertTrue(v.junctionDates().isEmpty())
    }

    @Test
    fun `seven leg chain has 7 board dates and 6 junctions`() {
        // 7 程长链（3+4 交替连乘到 24 晚）：各程登船日/交接日逐程累积
        val v = voyage(listOf(3, 4, 3, 4, 3, 4, 3))
        assertEquals(
            listOf(0L, 3L, 7L, 10L, 14L, 17L, 21L).map { board.plusDays(it) },
            v.legBoardDates(),
        )
        // 交接日 = 除最后一程外各程的结束日（即下一程登船日），共 6 个
        assertEquals(
            listOf(3L, 7L, 10L, 14L, 17L, 21L).map { board.plusDays(it) },
            v.junctionDates(),
        )
    }

    @Test
    fun `non alternating 3 3 4 junctions on day 3 and 6`() {
        // 非交替链：两个 3夜连乘，交接日落在 +3 和 +6（自由组合同样成立）
        val v = voyage(listOf(3, 3, 4))
        assertEquals(listOf(board.plusDays(3), board.plusDays(6)), v.junctionDates())
    }
}
