package com.cruise.assistant.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * OnboardingState 联程派生状态单测（纯 JVM）：班次只有 3夜/4夜两种，链强制交替
 *（3+4+3+4…或 4+3+4+3…，禁止 3+3），legs / totalNights / 下船日 / 摘要 / 下一程晚数全部随之派生。
 * 迪士尼探险号船期0 = 四天三夜（3晚），船期1 = 五天四夜（4晚）。
 */
class OnboardingStateTest {
    /** 2026-09-07 是周一（3夜船期登船日） */
    private val board: LocalDate = LocalDate.of(2026, 9, 7)

    private fun state(
        extraLegs: List<Int> = emptyList(),
        scheduleIndex: Int = 0,
    ) = OnboardingState(
        selectedShipIndex = 0,
        selectedScheduleIndex = scheduleIndex,
        boardDate = board,
        disembarkDate = board.plusDays(3),
        extraLegs = extraLegs,
    )

    @Test
    fun `one extra leg of 4 makes legs 3 plus 4`() {
        val s = state(extraLegs = listOf(4))
        assertTrue(s.isMultiLeg)
        assertEquals(listOf(3, 4), s.legs)
        assertEquals(7, s.totalNights)
        assertEquals(board.plusDays(7), s.multiLegDisembarkDate(board))
    }

    @Test
    fun `long chain 4 3 4 makes legs 3 4 3 4 with total 14 nights`() {
        val s = state(extraLegs = listOf(4, 3, 4))
        assertEquals(listOf(3, 4, 3, 4), s.legs)
        assertEquals(14, s.totalNights)
        assertEquals(board.plusDays(14), s.multiLegDisembarkDate(board))
        // 摘要：14 晚恰好跨两周，下船日星期叠两个"下"前缀
        val summary = s.multiLegSummary!!
        assertEquals("3+4+3+4", summary.legsLabel)
        assertEquals(14, summary.totalNights)
        assertEquals("周一", summary.boardDow)
        assertEquals("下下周一", summary.disembarkDow)
    }

    @Test
    fun `empty extraLegs or no schedule means single leg`() {
        // 未追加后续程 = 单程：legs 空、无联程下船日、无摘要
        val single = state()
        assertFalse(single.isMultiLeg)
        assertTrue(single.legs.isEmpty())
        assertNull(single.multiLegDisembarkDate(board))
        assertNull(single.multiLegSummary)
        // 未选船期（如切到自定义船期前的兜底）：即便 extraLegs 非空，legs 也保持空（单程分支）
        val noSchedule = OnboardingState(
            selectedShipIndex = 0,
            selectedScheduleIndex = -1,
            boardDate = board,
            disembarkDate = board.plusDays(3),
            extraLegs = listOf(3),
        )
        assertTrue(noSchedule.legs.isEmpty())
        assertNull(noSchedule.multiLegDisembarkDate(board))
    }

    @Test
    fun `next leg alternates from chain tail`() {
        // 首程 3夜（船期0）+ 已追加 4夜：链 3+4，下一程回到 3
        assertEquals(3, state(extraLegs = listOf(4)).nextLegNights)
        // 首程 4夜（船期1）+ 已追加 3夜：链 4+3，下一程回到 4
        assertEquals(4, state(extraLegs = listOf(3), scheduleIndex = 1).nextLegNights)
        // 未追加后续程：下一程由首程交替得出（3 → 4）
        assertEquals(4, state().nextLegNights)
        // 交替链 3+4+3（首程3）：下一程是 4
        assertEquals(4, state(extraLegs = listOf(4, 3)).nextLegNights)
        // 未选船期（自定义船期）：无法确定交替起点，下一程为 null
        assertNull(state(scheduleIndex = -1).nextLegNights)
    }

    @Test
    fun `nextLegNightsFor boundaries`() {
        // 未选船期：无法确定首程晚数，返回 null
        assertNull(nextLegNightsFor(null, emptyList()))
        assertNull(nextLegNightsFor(null, listOf(4)))
        // 空 extras：下一程由 scheduleNights 交替得出
        assertEquals(4, nextLegNightsFor(3, emptyList()))
        assertEquals(3, nextLegNightsFor(4, emptyList()))
        // 链末交替：3 → 4，4 → 3
        assertEquals(4, nextLegNightsFor(3, listOf(4, 3)))
        assertEquals(3, nextLegNightsFor(3, listOf(4)))
        assertEquals(3, nextLegNightsFor(4, listOf(3, 4)))
        // 链末异常值（3/4 之外）：无班次可接，兜底 null
        assertNull(nextLegNightsFor(5, emptyList()))
        assertNull(nextLegNightsFor(3, listOf(4, 5)))
    }
}
