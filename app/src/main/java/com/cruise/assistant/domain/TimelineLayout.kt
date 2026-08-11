package com.cruise.assistant.domain

/** 折叠后的空时段固定高度 */
const val COLLAPSED_SEGMENT_DP = 28f

/** 空档超过这么多分钟才折叠 */
const val MIN_COLLAPSIBLE_GAP_MINUTES = 90

/** 折叠时在事件前后各保留的缓冲分钟数 */
private const val EVENT_PADDING_MINUTES = 30

/** 显示范围的最小跨度 */
private const val MIN_RANGE_MINUTES = 120

/**
 * 时间轴的一段。collapsed 段用固定高度，其余按 hourHeightDp 比例展开。
 * [startMinute, endMinute) 为半开区间；topDp 是该段顶部的累计偏移。
 */
data class Segment(
    val startMinute: Int,
    val endMinute: Int,
    val topDp: Float,
    val heightDp: Float,
    val collapsed: Boolean,
) {
    val spanMinutes: Int get() = endMinute - startMinute
}

data class TimelineLayout(
    val hourHeightDp: Int,
    val rangeStartMinute: Int,
    val rangeEndMinute: Int,
    val segments: List<Segment>,
    val totalHeightDp: Float,
    val hiddenBefore: Int,
    val hiddenAfter: Int,
)

/**
 * 需求2：构造时间轴布局。
 *
 * @param rangeStartMinute 显示起点（当日 0 点起的分钟数）
 * @param rangeEndMinute   显示终点，1440 表示 24:00
 * @param hourHeightDp     每小时高度，缩放滑块控制
 * @param events           当日事件，用于决定哪些空档可以折叠
 * @param collapseEmptyGaps 是否折叠无事件的长空档
 */
fun buildTimelineLayout(
    rangeStartMinute: Int,
    rangeEndMinute: Int,
    hourHeightDp: Int,
    events: List<CruiseEvent>,
    collapseEmptyGaps: Boolean,
    showEventIds: Set<Long> = emptySet(),
): TimelineLayout {
    val start = rangeStartMinute.coerceIn(0, 1440 - MIN_RANGE_MINUTES)
    val end = rangeEndMinute.coerceIn(start + MIN_RANGE_MINUTES, 1440)

    val rangeOf: (CruiseEvent) -> Pair<Int, Int> = { e ->
        showEffectiveRange(e, isShow = e.id in showEventIds)
    }
    val hiddenBefore = events.count { rangeOf(it).second <= start }
    val hiddenAfter = events.count { rangeOf(it).first >= end }

    val rawSegments = if (collapseEmptyGaps) {
        buildCollapsedSpans(start, end, events, rangeOf)
    } else {
        listOf(start to end to false)
    }

    var cursorDp = 0f
    val segments = rawSegments.map { (span, collapsed) ->
        val (segStart, segEnd) = span
        val height = if (collapsed) {
            COLLAPSED_SEGMENT_DP
        } else {
            (segEnd - segStart) / 60f * hourHeightDp
        }
        Segment(segStart, segEnd, cursorDp, height, collapsed).also { cursorDp += height }
    }

    return TimelineLayout(
        hourHeightDp = hourHeightDp,
        rangeStartMinute = start,
        rangeEndMinute = end,
        segments = segments,
        totalHeightDp = cursorDp,
        hiddenBefore = hiddenBefore,
        hiddenAfter = hiddenAfter,
    )
}

/**
 * 把 [start, end) 切成"保留段 / 折叠段"交替的序列。
 * 保留段 = 事件占用区间（前后各留 EVENT_PADDING_MINUTES 缓冲）的并集；
 * show 事件按 [rangeOf] 的扩展区间（含 30 分钟等待段）参与。
 * 两个保留段之间超过 MIN_COLLAPSIBLE_GAP_MINUTES 的空档才折叠。
 */
private fun buildCollapsedSpans(
    start: Int,
    end: Int,
    events: List<CruiseEvent>,
    rangeOf: (CruiseEvent) -> Pair<Int, Int>,
): List<Pair<Pair<Int, Int>, Boolean>> {
    val keepRanges = events
        .filter { rangeOf(it).second > start && rangeOf(it).first < end && it.durationMinutes > 0 }
        .map {
            val (eStart, eEnd) = rangeOf(it)
            (eStart - EVENT_PADDING_MINUTES).coerceAtLeast(start) to
                (eEnd + EVENT_PADDING_MINUTES).coerceAtMost(end)
        }
        .sortedBy { it.first }
        .fold(mutableListOf<Pair<Int, Int>>()) { acc, range ->
            val last = acc.lastOrNull()
            if (last != null && range.first <= last.second) {
                acc[acc.lastIndex] = last.first to maxOf(last.second, range.second)
            } else {
                acc += range
            }
            acc
        }

    if (keepRanges.isEmpty()) return listOf((start to end) to true)

    val result = mutableListOf<Pair<Pair<Int, Int>, Boolean>>()
    var cursor = start
    for ((keepStart, keepEnd) in keepRanges) {
        if (keepStart > cursor) {
            val gap = keepStart - cursor
            result += (cursor to keepStart) to (gap >= MIN_COLLAPSIBLE_GAP_MINUTES)
        }
        result += (keepStart to keepEnd) to false
        cursor = keepEnd
    }
    if (cursor < end) {
        val gap = end - cursor
        result += (cursor to end) to (gap >= MIN_COLLAPSIBLE_GAP_MINUTES)
    }

    // 合并相邻的非折叠段，避免碎片
    return result.fold(mutableListOf()) { acc, item ->
        val last = acc.lastOrNull()
        if (last != null && !last.second && !item.second && last.first.second == item.first.first) {
            acc[acc.lastIndex] = (last.first.first to item.first.second) to false
        } else {
            acc += item
        }
        acc
    }
}

/** 分钟 → 垂直偏移 dp。范围外夹到 0 或 totalHeightDp。段内线性插值。 */
fun TimelineLayout.minuteToDp(minute: Int): Float {
    if (minute <= rangeStartMinute) return 0f
    if (minute >= rangeEndMinute) return totalHeightDp
    val seg = segments.firstOrNull { minute < it.endMinute } ?: return totalHeightDp
    if (minute <= seg.startMinute) return seg.topDp
    val ratio = (minute - seg.startMinute).toFloat() / seg.spanMinutes
    return seg.topDp + ratio * seg.heightDp
}

/** 垂直偏移 dp → 分钟，minuteToDp 的逆运算。 */
fun TimelineLayout.dpToMinute(dp: Float): Int {
    if (dp <= 0f) return rangeStartMinute
    if (dp >= totalHeightDp) return rangeEndMinute
    val seg = segments.firstOrNull { dp < it.topDp + it.heightDp } ?: return rangeEndMinute
    if (seg.heightDp <= 0f) return seg.startMinute
    val ratio = (dp - seg.topDp) / seg.heightDp
    return (seg.startMinute + ratio * seg.spanMinutes).toInt().coerceIn(seg.startMinute, seg.endMinute)
}

/**
 * 需要绘制的整点刻度：(分钟, dp偏移)。
 * 第一个刻度是 >= rangeStartMinute 的第一个整点；折叠段内的整点跳过不画。
 */
fun TimelineLayout.hourTicks(): List<Pair<Int, Float>> {
    val firstHour = ((rangeStartMinute + 59) / 60) * 60
    return (firstHour..rangeEndMinute step 60).mapNotNull { minute ->
        val inCollapsed = segments.any { it.collapsed && minute > it.startMinute && minute < it.endMinute }
        if (inCollapsed) null else minute to minuteToDp(minute)
    }
}
