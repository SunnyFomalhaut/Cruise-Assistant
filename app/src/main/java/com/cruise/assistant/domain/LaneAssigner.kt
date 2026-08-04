package com.cruise.assistant.domain

/**
 * 半开区间重叠判定：[aStart, aEnd) 与 [bStart, bEnd)。
 * 相邻（一个的结束等于另一个的开始）不算重叠。
 */
fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean =
    aStart < bEnd && bStart < aEnd

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
): Int {
    val candidates = existing.filter { it.id != excludeId }
    for (lane in 0 until LANE_COUNT) {
        val busy = candidates.any { other ->
            other.lane == lane &&
                overlaps(startMinute, endMinute, other.startMinute, other.endMinute)
        }
        if (!busy) return lane
    }
    return LANE_COUNT - 1
}
