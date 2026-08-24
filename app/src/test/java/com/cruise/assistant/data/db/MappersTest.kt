package com.cruise.assistant.data.db

import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.ShipZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MappersTest {

    @Test
    fun `test EventEntity to CruiseEvent mapping with note`() {
        val entity = EventEntity(
            id = 10L,
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(14, 30),
            durationMinutes = 60,
            categoryId = 1L,
            title = "迪士尼盛宴",
            deck = 5,
            zone = "MID",
            placeName = "华特迪士尼剧院",
            lane = 0,
            recurrenceGroupId = null,
            reminderMinutesBefore = 15,
            isSystemGenerated = false,
            isActive = true,
            presetKey = "show_1",
            waitMinutes = 30,
            note = "提前入场抢前排座位",
        )

        val domain = entity.toDomain()

        assertEquals(10L, domain.id)
        assertEquals("迪士尼盛宴", domain.title)
        assertEquals(ShipZone.MID, domain.zone)
        assertEquals("提前入场抢前排座位", domain.note)
    }

    @Test
    fun `test CruiseEvent to EventEntity mapping with note`() {
        val domain = CruiseEvent(
            id = 10L,
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(14, 30),
            durationMinutes = 60,
            categoryId = 1L,
            title = "迪士尼盛宴",
            deck = 5,
            zone = ShipZone.MID,
            placeName = "华特迪士尼剧院",
            lane = 0,
            recurrenceGroupId = null,
            reminderMinutesBefore = 15,
            isSystemGenerated = false,
            isActive = true,
            presetKey = "show_1",
            waitMinutes = 30,
            note = "带好房卡和相机",
        )

        val entity = domain.toEntity()

        assertEquals(10L, entity.id)
        assertEquals("迪士尼盛宴", entity.title)
        assertEquals("MID", entity.zone)
        assertEquals("带好房卡和相机", entity.note)
    }

    @Test
    fun `test EventEntity to CruiseEvent mapping default null note`() {
        val entity = EventEntity(
            id = 11L,
            date = LocalDate.of(2026, 8, 24),
            startTime = LocalTime.of(16, 0),
            durationMinutes = 30,
            categoryId = 2L,
            title = "游泳池派对",
            deck = 11,
            zone = "BOW",
            placeName = "主泳池",
            lane = 1,
            recurrenceGroupId = null,
            reminderMinutesBefore = null,
            isSystemGenerated = false,
        )

        val domain = entity.toDomain()

        assertNull(domain.note)
        val mappedEntity = domain.toEntity()
        assertNull(mappedEntity.note)
    }
}
