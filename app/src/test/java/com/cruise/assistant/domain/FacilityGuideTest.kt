package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class FacilityGuideTest {

    @Test
    fun `test 24 hours facility identification and status`() {
        val entry = LocationCatalogEntry(
            deck = 6,
            zone = ShipZone.MID,
            placeName = "宾客服务台 Guest Services",
            category = "chore",
            hours = OperatingHours(
                listOf(
                    OperatingSlot(
                        start = LocalTime.of(0, 0),
                        end = LocalTime.of(23, 59),
                    )
                )
            ),
        )

        val model = entry.toFacilityItemUiModel(currentTime = LocalTime.of(14, 0))
        assertTrue(model.is24Hours)
        assertTrue(model.isOpenNow)
        assertTrue(model.hasOperatingHours)
        assertEquals("chore", model.categoryKey)
        assertEquals("Deck 6 船中", model.formattedLocation)
    }

    @Test
    fun `test 24 hours facility evaluated at 2359 returns isOpenNow true`() {
        val entry = LocationCatalogEntry(
            deck = 6,
            zone = ShipZone.MID,
            placeName = "宾客服务台 Guest Services",
            category = "chore",
            hours = OperatingHours(
                listOf(
                    OperatingSlot(
                        start = LocalTime.of(0, 0),
                        end = LocalTime.of(23, 59),
                    )
                )
            ),
        )

        val modelAt2359 = entry.toFacilityItemUiModel(currentTime = LocalTime.of(23, 59))
        assertTrue(modelAt2359.is24Hours)
        assertTrue(modelAt2359.isOpenNow)
    }

    @Test
    fun `test operating hours and status for specific slot`() {
        val entry = LocationCatalogEntry(
            deck = 10,
            zone = ShipZone.MID,
            placeName = "健身中心 Fitness Center",
            category = "chore",
            hours = OperatingHours(
                listOf(
                    OperatingSlot(
                        start = LocalTime.of(6, 0),
                        end = LocalTime.of(22, 0),
                    )
                )
            ),
        )

        val openModel = entry.toFacilityItemUiModel(currentTime = LocalTime.of(10, 0))
        assertTrue(openModel.isOpenNow)
        assertFalse(openModel.is24Hours)

        val closedModel = entry.toFacilityItemUiModel(currentTime = LocalTime.of(23, 0))
        assertFalse(closedModel.isOpenNow)
    }

    @Test
    fun `test facility without static hours like show stage`() {
        val entry = LocationCatalogEntry(
            deck = 6,
            zone = ShipZone.BOW,
            placeName = "华特迪士尼剧院 Walt Disney Theatre",
            category = "show",
            hours = null,
        )

        val model = entry.toFacilityItemUiModel(currentTime = LocalTime.of(15, 0))
        assertFalse(model.hasOperatingHours)
        assertFalse(model.is24Hours)
        assertTrue(model.isOpenNow) // non-configured hours default to open
    }

    @Test
    fun `test multi deck facility formatting and primary deck`() {
        val entry = LocationCatalogEntry(
            deck = 6,
            additionalDecks = listOf(7),
            zone = ShipZone.BOW,
            placeName = "华特迪士尼剧院 Walt Disney Theatre",
            category = "show",
            hours = null,
        )

        val model = entry.toFacilityItemUiModel()
        assertEquals(listOf(6, 7), model.allDecks)
        assertEquals(6, model.primaryDeck)
        assertEquals("Deck 6, 7 船头", model.formattedLocation)
    }

    @Test
    fun `test filter facilities by category, search query, and open now`() {
        val f1 = FacilityItemUiModel(
            placeName = "米奇泳池 Mickey Pool",
            categoryKey = "pool",
            categoryLabel = "泳池娱乐",
            deck = 11,
            zone = ShipZone.MID,
            formattedLocation = "Deck 11 船中",
            slots = listOf(OperatingSlot(LocalTime.of(8, 0), LocalTime.of(20, 0))),
            is24Hours = false,
            isOpenNow = true,
            hasOperatingHours = true,
            diningGroupLabel = null,
        )
        val f2 = FacilityItemUiModel(
            placeName = "宾客服务台 Guest Services",
            categoryKey = "chore",
            categoryLabel = "船上服务",
            deck = 6,
            zone = ShipZone.MID,
            formattedLocation = "Deck 6 船中",
            slots = listOf(OperatingSlot(LocalTime.of(0, 0), LocalTime.of(23, 59))),
            is24Hours = true,
            isOpenNow = true,
            hasOperatingHours = true,
            diningGroupLabel = null,
        )
        val f3 = FacilityItemUiModel(
            placeName = "漫威餐厅 Marvel Restaurant",
            categoryKey = "meal",
            categoryLabel = "餐食",
            deck = 4,
            zone = ShipZone.STERN,
            formattedLocation = "Deck 4 船尾",
            slots = listOf(OperatingSlot(LocalTime.of(17, 30), LocalTime.of(21, 0))),
            is24Hours = false,
            isOpenNow = false,
            hasOperatingHours = true,
            diningGroupLabel = "正餐餐厅",
        )

        val all = listOf(f1, f2, f3)

        // 1. All category without query
        assertEquals(3, filterFacilities(all, query = "", categoryKey = "all", onlyOpenNow = false).size)

        // 2. Filter by category
        val poolOnly = filterFacilities(all, query = "", categoryKey = "pool", onlyOpenNow = false)
        assertEquals(1, poolOnly.size)
        assertEquals("米奇泳池 Mickey Pool", poolOnly[0].placeName)

        // 3. Search by name (Chinese / English)
        val searchMickey = filterFacilities(all, query = "mickey", categoryKey = "all", onlyOpenNow = false)
        assertEquals(1, searchMickey.size)

        // 4. Search by Deck
        val searchDeck6 = filterFacilities(all, query = "Deck 6", categoryKey = "all", onlyOpenNow = false)
        assertEquals(1, searchDeck6.size)
        assertEquals("宾客服务台 Guest Services", searchDeck6[0].placeName)

        // 5. Search by Zone
        val searchStern = filterFacilities(all, query = "船尾", categoryKey = "all", onlyOpenNow = false)
        assertEquals(1, searchStern.size)
        assertEquals("漫威餐厅 Marvel Restaurant", searchStern[0].placeName)

        // 6. Filter by onlyOpenNow
        val openOnly = filterFacilities(all, query = "", categoryKey = "all", onlyOpenNow = true)
        assertEquals(2, openOnly.size)
    }
}
