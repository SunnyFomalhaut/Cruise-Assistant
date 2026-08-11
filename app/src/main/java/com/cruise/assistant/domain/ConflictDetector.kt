package com.cruise.assistant.domain

/**
 * 需求8：找出同一天内所有时间重叠的事件对。
 * 每对只报一次，按开始时间排序后 first 早于（或同时但 id 更小）second。
 * 相邻（前一个结束 == 后一个开始）不算冲突。
 */
fun findConflicts(
    events: List<CruiseEvent>,
    rangeOf: (CruiseEvent) -> Pair<Int, Int> = { it.startMinute to it.endMinute },
): List<Pair<CruiseEvent, CruiseEvent>> {
    val activeEvents = events.filter { it.isActive }
    val sorted = activeEvents.sortedWith(
        compareBy({ rangeOf(it).first }, { it.id }),
    )
    val result = mutableListOf<Pair<CruiseEvent, CruiseEvent>>()
    for (i in sorted.indices) {
        val a = sorted[i]
        val (aStart, aEnd) = rangeOf(a)
        for (j in i + 1 until sorted.size) {
            val b = sorted[j]
            val (bStart, bEnd) = rangeOf(b)
            // 排序后 bStart >= aStart，一旦不早于 a 的结束就不可能再重叠
            if (bStart >= aEnd) break
            if (overlaps(aStart, aEnd, bStart, bEnd)) {
                result += a to b
            }
        }
    }
    return result
}
