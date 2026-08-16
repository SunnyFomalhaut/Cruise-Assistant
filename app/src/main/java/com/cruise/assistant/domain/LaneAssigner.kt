package com.cruise.assistant.domain

/**
 * 半开区间重叠判定：[aStart, aEnd) 与 [bStart, bEnd)。
 * 相邻（一个的结束等于另一个的开始）不算重叠。
 * 空区间（start == end，如零时长事件）不占据任何时间，永远不算重叠。
 */
fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean =
    aStart < aEnd && bStart < bEnd && aStart < bEnd && bStart < aEnd

/**
 * 需求4：为新事件挑选栏位。依次尝试栏 0 → 1 → 2，
 * 返回第一个与该栏已有事件都不重叠的栏号。
 * 三栏都冲突时返回最后一栏（窄栏）作为溢出位。
 *
 * @param excludeId 编辑已有事件时传入其 id，避免与自己比较。
 */
fun assignLane(
    startMinute: Int,
    endMinute: Int,
    existing: List<CruiseEvent>,
    excludeId: Long? = null,
    rangeOf: (CruiseEvent) -> Pair<Int, Int> = { it.startMinute to it.endMinute },
): Int {
    val candidates = existing.filter { it.id != excludeId }
    for (lane in 0 until LANE_COUNT) {
        val busy = candidates.any { other ->
            val (oStart, oEnd) = rangeOf(other)
            other.lane == lane && overlaps(startMinute, endMinute, oStart, oEnd)
        }
        if (!busy) return lane
    }
    return LANE_COUNT - 1
}

/**
 * 重新激活事件时优先保留原栏位（preferredLane）——原栏位与同日其他活动事件
 * 不重叠就直接沿用；只有原栏位已被占才回退到普通 assignLane（0→1→2 找空栏）。
 * "只看本场"复活的演出期望留在原来的栏位，而不是因为栏 0 空着就跳过去。
 */
fun assignLanePreserving(
    startMinute: Int,
    endMinute: Int,
    existing: List<CruiseEvent>,
    preferredLane: Int,
    excludeId: Long? = null,
    rangeOf: (CruiseEvent) -> Pair<Int, Int> = { it.startMinute to it.endMinute },
): Int {
    val candidates = existing.filter { it.id != excludeId }
    val preferredBusy = candidates.any { other ->
        val (oStart, oEnd) = rangeOf(other)
        other.lane == preferredLane && overlaps(startMinute, endMinute, oStart, oEnd)
    }
    return if (!preferredBusy) {
        preferredLane
    } else {
        assignLane(startMinute, endMinute, existing, excludeId, rangeOf)
    }
}

/**
 * 预置演出生成时的栏位分配：优先第二栏（1），冲突则第三栏（2），再冲突回退第一栏（0）。
 * occupied 为已分配演出（按扩展区间）的 (lane, effStart, effEnd) 列表。
 */
fun assignShowLane(
    startMinute: Int,
    endMinute: Int,
    occupied: List<Triple<Int, Int, Int>>,
): Int {
    for (lane in listOf(1, 2, 0)) {
        val busy = occupied.any { (l, s, e) ->
            l == lane && overlaps(startMinute, endMinute, s, e)
        }
        if (!busy) return lane
    }
    return 0
}
