package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RecurrenceExpanderTest {

    private val disembark = LocalDate.of(2026, 9, 8)

    private fun template(date: LocalDate) = CruiseEvent(
        id = 0, date = date, startTime = LocalTime.of(7, 30),
        durationMinutes = 45, categoryId = 1, title = "早餐",
        deck = 5, zone = ShipZone.MID, placeName = "自助餐厅",
    )

    @Test
    fun expandsFromTemplateDate_toDayBeforeDisembark() {
        // 9/3 创建，下船日 9/8 → 生成 9/3,9/4,9/5,9/6,9/7（9/8 不含）
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "g1")
        assertEquals(5, result.size)
        assertEquals(LocalDate.of(2026, 9, 3), result.first().date)
        assertEquals(LocalDate.of(2026, 9, 7), result.last().date)
    }

    @Test
    fun disembarkDate_isNeverIncluded() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "g1")
        assertTrue(result.none { it.date == disembark })
    }

    @Test
    fun templateOnDayBeforeDisembark_yieldsSingleEvent() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 7)), disembark, "g1")
        assertEquals(1, result.size)
        assertEquals(LocalDate.of(2026, 9, 7), result[0].date)
    }

    @Test
    fun templateOnDisembarkDate_yieldsSingleEventOnThatDate() {
        // 用户在下船日当天手动创建的事件不应被循环吞掉，至少保留自己
        val result = expandRecurrence(template(disembark), disembark, "g1")
        assertEquals(1, result.size)
        assertEquals(disembark, result[0].date)
    }

    @Test
    fun allCopiesShareGroupId() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "grp-42")
        assertTrue(result.all { it.recurrenceGroupId == "grp-42" })
    }

    @Test
    fun allCopiesPreserveTimeDurationAndLocation() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "g1")
        assertTrue(result.all { it.startTime == LocalTime.of(7, 30) })
        assertTrue(result.all { it.durationMinutes == 45 })
        assertTrue(result.all { it.deck == 5 && it.zone == ShipZone.MID })
        assertTrue(result.all { it.placeName == "自助餐厅" })
        assertTrue(result.all { it.title == "早餐" })
    }

    @Test
    fun allCopiesHaveZeroId_soRoomAutoGenerates() {
        val result = expandRecurrence(
            template(LocalDate.of(2026, 9, 3)).copy(id = 99), disembark, "g1",
        )
        assertTrue(result.all { it.id == 0L })
    }

    @Test
    fun templateAfterDisembark_yieldsSingleEvent() {
        val late = disembark.plusDays(3)
        val result = expandRecurrence(template(late), disembark, "g1")
        assertEquals(1, result.size)
        assertEquals(late, result[0].date)
    }
}
