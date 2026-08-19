package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimelineLayoutTest {

    private val day = LocalDate.of(2026, 9, 3)
    private val EPS = 0.01f

    private fun ev(hour: Int, minute: Int, durMin: Int) = CruiseEvent(
        id = 0, date = day, startTime = LocalTime.of(hour, minute),
        durationMinutes = durMin, categoryId = 1, title = "e",
    )

    // ---------- 无折叠：纯线性映射 ----------

    @Test
    fun rangeStart_mapsToZero() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(0f, l.minuteToDp(420), EPS)
    }

    @Test
    fun oneHourAfterStart_mapsToOneHourHeight() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(60f, l.minuteToDp(480), EPS)
    }

    @Test
    fun halfHour_mapsToHalfHeight() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(30f, l.minuteToDp(450), EPS)
    }

    @Test
    fun totalHeight_matchesRangeTimesHourHeight() {
        // 07:00-24:00 = 17 小时
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(17 * 60f, l.totalHeightDp, EPS)
    }

    @Test
    fun customHourHeight_scalesProportionally() {
        val l = buildTimelineLayout(420, 1440, 100, emptyList(), false)
        assertEquals(100f, l.minuteToDp(480), EPS)
        assertEquals(17 * 100f, l.totalHeightDp, EPS)
    }

    @Test
    fun minutesBeforeRange_clampToZero() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(0f, l.minuteToDp(0), EPS)
        assertEquals(0f, l.minuteToDp(300), EPS)
    }

    @Test
    fun minutesAfterRange_clampToTotalHeight() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(l.totalHeightDp, l.minuteToDp(1500), EPS)
    }

    // ---------- 反向映射 ----------

    @Test
    fun dpToMinute_isInverseOfMinuteToDp() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        for (minute in listOf(420, 500, 780, 1000, 1439)) {
            assertEquals(minute, l.dpToMinute(l.minuteToDp(minute)))
        }
    }

    @Test
    fun dpToMinute_clampsToRange() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(420, l.dpToMinute(-50f))
        assertEquals(1440, l.dpToMinute(99999f))
    }

    // ---------- 小时刻度 ----------

    @Test
    fun hourTicks_coverEveryWholeHourInRange() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        val ticks = l.hourTicks()
        // 07:00 到 24:00 含两端 = 18 个刻度
        assertEquals(18, ticks.size)
        assertEquals(420, ticks.first().first)
        assertEquals(1440, ticks.last().first)
        assertEquals(0f, ticks.first().second, EPS)
        // 60dp 高度 < 90，不插入半点
        assertTrue(ticks.all { !it.third })
    }

    @Test
    fun hourTicks_startAtFirstWholeHourAtOrAfterRangeStart() {
        // 07:30 起 → 第一个刻度是 08:00
        val l = buildTimelineLayout(450, 1440, 60, emptyList(), false)
        val first = l.hourTicks().first()
        assertEquals(480, first.first)
        assertEquals(false, first.third)
    }

    // ---------- 折叠空时段 ----------

    @Test
    fun collapseOff_keepsSingleSegment() {
        val l = buildTimelineLayout(420, 1440, 60, listOf(ev(13, 0, 60)), false)
        assertEquals(1, l.segments.size)
        assertTrue(l.segments.none { it.collapsed })
    }

    @Test
    fun collapseOn_withNoEvents_collapsesWholeRange() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), true)
        assertTrue(l.segments.all { it.collapsed })
        assertEquals(COLLAPSED_SEGMENT_DP, l.totalHeightDp, EPS)
    }

    @Test
    fun collapseOn_shrinksLongEmptyGapBetweenEvents() {
        // 08:00-09:00 一件、20:00-21:00 一件，中间 11 小时空
        val events = listOf(ev(8, 0, 60), ev(20, 0, 60))
        val collapsed = buildTimelineLayout(420, 1440, 60, events, true)
        val expanded = buildTimelineLayout(420, 1440, 60, events, false)
        assertTrue(collapsed.totalHeightDp < expanded.totalHeightDp)
        assertTrue(collapsed.segments.any { it.collapsed })
    }

    @Test
    fun collapseOn_keepsEventSegmentsAtFullScale() {
        val events = listOf(ev(8, 0, 60))
        val l = buildTimelineLayout(420, 1440, 60, events, true)
        // 08:00→09:00 之间仍是完整 60dp
        assertEquals(60f, l.minuteToDp(540) - l.minuteToDp(480), EPS)
    }

    @Test
    fun collapseOn_keepsShortGapsExpanded() {
        // 两件事只隔 20 分钟，短于阈值，不该折叠
        val events = listOf(ev(8, 0, 60), ev(9, 20, 60))
        val l = buildTimelineLayout(480, 660, 60, events, true)
        assertTrue(l.segments.none { it.collapsed })
    }

    @Test
    fun minuteToDp_isMonotonic_whenCollapsing() {
        val events = listOf(ev(8, 0, 60), ev(20, 0, 60))
        val l = buildTimelineLayout(420, 1440, 60, events, true)
        var prev = -1f
        for (minute in 420..1440 step 5) {
            val dp = l.minuteToDp(minute)
            assertTrue("dp 必须单调不减 @minute=" + minute, dp >= prev - EPS)
            prev = dp
        }
    }

    @Test
    fun dpToMinute_isInverse_whenCollapsing() {
        val events = listOf(ev(8, 0, 60), ev(20, 0, 60))
        val l = buildTimelineLayout(420, 1440, 60, events, true)
        // 展开段内的分钟必须能精确往返
        for (minute in listOf(480, 510, 540, 1200, 1260)) {
            assertEquals(minute, l.dpToMinute(l.minuteToDp(minute)))
        }
    }

    // ---------- 范围外事件计数 ----------

    @Test
    fun countsEventsBeforeAndAfterRange() {
        val events = listOf(ev(3, 0, 60), ev(5, 0, 60), ev(13, 0, 60), ev(23, 30, 60))
        // 范围 07:00-23:00 → 前面 2 件、后面 1 件
        val l = buildTimelineLayout(420, 1380, 60, events, false)
        assertEquals(2, l.hiddenBefore)
        assertEquals(1, l.hiddenAfter)
    }

    @Test
    fun eventPartiallyInsideRange_isNotCountedAsHidden() {
        // 06:30-07:30 跨过范围起点，算在范围内
        val l = buildTimelineLayout(420, 1380, 60, listOf(ev(6, 30, 60)), false)
        assertEquals(0, l.hiddenBefore)
    }

    // ---------- 参数校验 ----------

    @Test
    fun invalidRange_isNormalizedToMinimumTwoHours() {
        val l = buildTimelineLayout(600, 600, 60, emptyList(), false)
        assertTrue(l.rangeEndMinute - l.rangeStartMinute >= 120)
    }

    @Test
    fun showEvent_extendsOccupiedRangeBy30Minutes() {
        // 构造一个 20:00 开始的 60 分钟 show 事件与一个 19:40 的普通事件：
        // 普通事件结束(20:10) 在 show 扩展区间(19:30 起)内 → 应算冲突
        val show = CruiseEvent(
            id = 1, date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(20, 0), durationMinutes = 60,
            categoryId = 1, title = "演出",
        )
        val ordinary = CruiseEvent(
            id = 2, date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(19, 40), durationMinutes = 30,
            categoryId = 1, title = "普通事件",
        )
        val conflicts = findConflicts(listOf(show, ordinary)) { e ->
            showEffectiveRange(e, isShow = e.id == 1L)
        }
        assertEquals(1, conflicts.size)
    }

    // ---------- 半点刻度 ----------

    @Test
    fun hourTicks_includeHalfHours_whenHourHeight90Plus() {
        val layout = buildTimelineLayout(
            rangeStartMinute = 300, rangeEndMinute = 420, hourHeightDp = 90,
            events = emptyList(), collapseEmptyGaps = false,
        )
        val ticks = layout.hourTicks()
        // 5:00, 5:30, 6:00, 6:30, 7:00
        assertEquals(listOf(300, 330, 360, 390, 420), ticks.map { it.first })
        // 半点标记正确
        assertEquals(listOf(false, true, false, true, false), ticks.map { it.third })
    }

    @Test
    fun hourTicks_omitHalfHours_below90() {
        val layout = buildTimelineLayout(
            rangeStartMinute = 300, rangeEndMinute = 420, hourHeightDp = 89,
            events = emptyList(), collapseEmptyGaps = false,
        )
        assertEquals(listOf(300, 360, 420), layout.hourTicks().map { it.first })
    }

    @Test
    fun nearestHourAtDp_roundsToNearestWholeHour() {
        // 420..1440 (07:00..24:00), 60dp/hr -> 1dp = 1min, topDp of 07:00 is 0dp
        val layout = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        // 07:00 is minute 420 -> dp 0
        assertEquals(7 * 60, layout.nearestHourAtDp(0f))
        // 15:29 is minute 929 -> minuteToDp(929) -> nearest 15:00 (900)
        assertEquals(15 * 60, layout.nearestHourAtDp(layout.minuteToDp(15 * 60 + 29)))
        // 15:30 is minute 930 -> minuteToDp(930) -> nearest 16:00 (960)
        assertEquals(16 * 60, layout.nearestHourAtDp(layout.minuteToDp(15 * 60 + 30)))
        // 16:00 is minute 960 -> minuteToDp(960) -> nearest 16:00 (960)
        assertEquals(16 * 60, layout.nearestHourAtDp(layout.minuteToDp(16 * 60)))
        // 16:29 is minute 989 -> minuteToDp(989) -> nearest 16:00 (960)
        assertEquals(16 * 60, layout.nearestHourAtDp(layout.minuteToDp(16 * 60 + 29)))
        // 23:45 is minute 1425 -> clamped to 23:00 (1380)
        assertEquals(23 * 60, layout.nearestHourAtDp(layout.minuteToDp(23 * 60 + 45)))
    }
}
