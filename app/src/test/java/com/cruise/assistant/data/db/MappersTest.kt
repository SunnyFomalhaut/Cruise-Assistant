package com.cruise.assistant.data.db

import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.Voyage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // ---- 联程航次 legNights JSON ↔ List 双向映射 ----

    @Test
    fun `voyage legs round-trip via legNights json`() {
        val voyage = Voyage(
            id = 1,
            shipName = "Disney Adventure",
            boardDate = LocalDate.of(2026, 9, 7),
            boardTime = LocalTime.of(12, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
            legs = listOf(3, 4),
        )

        val entity = voyage.toEntity()
        assertEquals("[3,4]", entity.legNights)

        val back = entity.toDomain()
        assertEquals(listOf(3, 4), back.legs)
        assertEquals(voyage, back)
    }

    @Test
    fun `single leg voyage maps legNights to null`() {
        val voyage = Voyage(
            id = 1,
            shipName = null,
            boardDate = LocalDate.of(2026, 9, 7),
            boardTime = LocalTime.of(12, 0),
            disembarkDate = LocalDate.of(2026, 9, 10),
        )

        val entity = voyage.toEntity()
        assertNull(entity.legNights)

        val back = entity.toDomain()
        assertTrue(back.legs.isEmpty())
        assertEquals(voyage, back)
    }

    @Test
    fun `corrupt legNights json falls back to empty legs`() {
        // 旧库脏数据/手改坏值：解析失败兜底为空（单程），不能让加载崩溃
        val entity = VoyageEntity(
            id = 1,
            shipName = null,
            boardDate = LocalDate.of(2026, 9, 7),
            boardTime = LocalTime.of(12, 0),
            disembarkDate = LocalDate.of(2026, 9, 10),
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = 19,
            legNights = "[3,oops",
        )
        assertTrue(entity.toDomain().legs.isEmpty())
    }

    // ---- 登岸兰卡威 ashoreLegs JSON ↔ Set 双向映射 ----

    @Test
    fun `voyage ashoreLegs round-trip via json`() {
        val voyage = Voyage(
            id = 1,
            shipName = "Disney Adventure",
            boardDate = LocalDate.of(2026, 9, 10),
            boardTime = LocalTime.of(12, 0),
            disembarkDate = LocalDate.of(2026, 9, 21),
            legs = listOf(4, 3, 4),
            ashoreLegs = setOf(0, 2),
        )

        val entity = voyage.toEntity()
        assertEquals("[0,2]", entity.ashoreLegs)

        val back = entity.toDomain()
        assertEquals(setOf(0, 2), back.ashoreLegs)
        assertEquals(voyage, back)
    }

    @Test
    fun `empty ashoreLegs maps to null json`() {
        val voyage = Voyage(
            id = 1,
            shipName = null,
            boardDate = LocalDate.of(2026, 9, 10),
            boardTime = LocalTime.of(12, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
        )

        val entity = voyage.toEntity()
        assertNull(entity.ashoreLegs)

        val back = entity.toDomain()
        assertTrue(back.ashoreLegs.isEmpty())
        assertEquals(voyage, back)
    }

    @Test
    fun `corrupt ashoreLegs json falls back to empty set`() {
        // 旧库脏数据/手改坏值：解析失败兜底为空集（无登岸程），不能让加载崩溃
        val entity = VoyageEntity(
            id = 1,
            shipName = null,
            boardDate = LocalDate.of(2026, 9, 10),
            boardTime = LocalTime.of(12, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = 19,
            ashoreLegs = "[0,oops",
        )
        assertTrue(entity.toDomain().ashoreLegs.isEmpty())
    }
}
