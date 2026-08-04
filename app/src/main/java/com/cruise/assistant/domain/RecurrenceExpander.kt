package com.cruise.assistant.domain

import java.time.LocalDate

/**
 * 需求6：把一个事件按"每日同一时间"展开。
 *
 * 范围是 template.date 起、到下船日的前一天为止 —— 下船日只有一件"下船"，
 * 不生成循环项（用户明确要求）。
 *
 * 若 template.date 已经是下船日或更晚，只返回它自己一份，避免用户手建的事件被吞掉。
 * 所有副本 id = 0，交给 Room 自增；共享同一个 recurrenceGroupId。
 */
fun expandRecurrence(
    template: CruiseEvent,
    disembarkDate: LocalDate,
    groupId: String,
): List<CruiseEvent> {
    val base = template.copy(id = 0, recurrenceGroupId = groupId)
    val lastDate = disembarkDate.minusDays(1)
    if (template.date.isAfter(lastDate)) return listOf(base)

    val dayCount = (lastDate.toEpochDay() - template.date.toEpochDay()).toInt() + 1
    return (0 until dayCount).map { offset ->
        base.copy(date = template.date.plusDays(offset.toLong()))
    }
}
