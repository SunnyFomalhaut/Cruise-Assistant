package com.cruise.assistant.domain

/**
 * 需求8：找出同一天内所有时间重叠的事件对。
 * 每对只报一次，按开始时间排序后 first 早于（或同时但 id 更小）second。
 * 相邻（前一个结束 == 后一个开始）不算冲突。
 */
fun findConflicts(events: List<CruiseEvent>): List<Pair<CruiseEvent, CruiseEvent>> {
    val sorted = events.sortedWith(compareBy({ it.startMinute }, { it.id }))
    val result = mutableListOf<Pair<CruiseEvent, CruiseEvent>>()
    for (i in sorted.indices) {
        val a = sorted[i]
        for (j in i + 1 until sorted.size) {
            val b = sorted[j]
            // 排序后 b.startMinute >= a.startMinute，一旦不早于 a 的结束就不可能再重叠
            if (b.startMinute >= a.endMinute) break
            if (overlaps(a.startMinute, a.endMinute, b.startMinute, b.endMinute)) {
                result += a to b
            }
        }
    }
    return result
}
