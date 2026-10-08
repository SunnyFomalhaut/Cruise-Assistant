package com.cruise.assistant.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ReminderScheduler 的槽位计算是纯函数（[reminderTriggersFor]），这里直接覆盖；
 * rescheduleAll 依赖 AlarmManager/Context，不在 JVM 单测范围内。
 */
class ReminderSchedulerTest {

    // 开场时刻取一个足够大的 epoch ms（2001-09），保证"开场前 45 分钟"仍为正数，
    // 不会被 triggerMs > nowMs（nowMs=0）的保底过滤掉
    private val showMs = 1_000_000_000_000L

    private val minuteMs = 60_000L

    @Test
    fun show_withWait_slotsRelativeToWaitStart_plusOpen5() {
        val triggers = reminderTriggersFor(
            showMs = showMs,
            isShow = true,
            waitMinutes = 30,
            enabledSlots = listOf(0, 1, 2),
            nowMs = 0L,
        )
        // slot 0/1/2 相对候场开始（开场-30min）：-45/-35/-30 分钟；slot 3 固定开场前 5 分钟
        assertEquals(
            listOf(
                0 to (showMs - 45 * minuteMs),
                1 to (showMs - 35 * minuteMs),
                2 to (showMs - 30 * minuteMs),
                3 to (showMs - 5 * minuteMs),
            ),
            triggers,
        )
    }

    @Test
    fun show_zeroWait_skipsWaitSlots_keepsOnlyOpen5() {
        // 不候场：三个开关都相对候场，候场不存在 → 只剩"开场前 5 分钟"，文案不再出现"候场"
        val triggers = reminderTriggersFor(
            showMs = showMs,
            isShow = true,
            waitMinutes = 0,
            enabledSlots = listOf(0, 1, 2),
            nowMs = 0L,
        )
        assertEquals(listOf(3 to (showMs - 5 * minuteMs)), triggers)
    }

    @Test
    fun nonShow_slotsRelativeToStartTime_noSlot3() {
        val triggers = reminderTriggersFor(
            showMs = showMs,
            isShow = false,
            waitMinutes = 0,
            enabledSlots = listOf(0, 1, 2),
            nowMs = 0L,
        )
        // 非演出保持原语义：相对开场时刻的三个槽位，没有 slot 3
        assertEquals(
            listOf(
                0 to (showMs - 15 * minuteMs),
                1 to (showMs - 5 * minuteMs),
                2 to showMs,
            ),
            triggers,
        )
    }

    @Test
    fun disabledSlots_notGenerated() {
        // 用户只开了"开始时"提醒（slot 2）
        val triggers = reminderTriggersFor(
            showMs = showMs,
            isShow = true,
            waitMinutes = 15,
            enabledSlots = listOf(2),
            nowMs = 0L,
        )
        assertEquals(listOf(2 to (showMs - 15 * minuteMs), 3 to (showMs - 5 * minuteMs)), triggers)
    }

    @Test
    fun pastTriggers_dropped() {
        // nowMs = 开场时刻：所有槽位触发点都已过期 → 空列表
        val triggers = reminderTriggersFor(
            showMs = showMs,
            isShow = true,
            waitMinutes = 30,
            enabledSlots = listOf(0, 1, 2),
            nowMs = showMs,
        )
        assertEquals(emptyList<Pair<Int, Long>>(), triggers)
    }
}
