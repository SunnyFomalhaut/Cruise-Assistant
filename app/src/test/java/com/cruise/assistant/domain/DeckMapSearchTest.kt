package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckMapSearchTest {

    @Test
    fun `test parseStateroomDeck with 5-digit room number`() {
        assertEquals(11, parseStateroomDeck("11770"))
        assertEquals(10, parseStateroomDeck("10216"))
        assertEquals(15, parseStateroomDeck("15012"))
        assertNull(parseStateroomDeck("14001")) // Deck 14 doesn't exist
        assertNull(parseStateroomDeck("04001")) // Deck 4 not available
    }

    @Test
    fun `test parseStateroomDeck with 4-digit room number`() {
        assertEquals(6, parseStateroomDeck("6012"))
        assertEquals(5, parseStateroomDeck("5512"))
        assertEquals(9, parseStateroomDeck("9100"))
        assertNull(parseStateroomDeck("4012")) // Deck 4 not available
    }

    @Test
    fun `test searchDeckMap with stateroom number returns stateroom result`() {
        val results = searchDeckMap("11770", currentDeck = 6, facilities = emptyList())
        val roomResult = results.firstOrNull { it.type == DeckSearchResultType.STATEROOM }
        assertNotNull(roomResult)
        assertEquals(11, roomResult!!.deckNumber)
        assertEquals("11770", roomResult.highlightQuery)
    }

    @Test
    fun `test searchDeckMap with restroom keyword returns restroom options`() {
        val results = searchDeckMap("洗手间", currentDeck = 6, facilities = emptyList())
        val restrooms = results.filter { it.type == DeckSearchResultType.RESTROOM }
        assertTrue(restrooms.isNotEmpty())
        assertEquals(6, restrooms.first().deckNumber)
        assertEquals("Restroom", restrooms.first().highlightQuery)
    }

    @Test
    fun `test searchDeckMap with facility query matches catalog items`() {
        val sampleFacilities = listOf(
            FacilityItemUiModel(
                placeName = "帕洛意大利餐厅 Palo Trattoria",
                categoryKey = "meal",
                categoryLabel = "餐食",
                deck = 10,
                allDecks = listOf(10, 11),
                primaryDeck = 10,
                zone = ShipZone.STERN,
                formattedLocation = "Deck 10, 11 船尾",
                slots = emptyList(),
                is24Hours = false,
                isOpenNow = true,
                hasOperatingHours = true,
            )
        )
        val results = searchDeckMap("Palo", currentDeck = 5, facilities = sampleFacilities)
        val facResult = results.firstOrNull { it.type == DeckSearchResultType.FACILITY }
        assertNotNull(facResult)
        assertEquals(10, facResult!!.deckNumber)
        assertEquals("帕洛意大利餐厅 Palo Trattoria", facResult.highlightQuery)
    }

    @Test
    fun `test searchDeckMap with empty query returns shortcuts`() {
        val results = searchDeckMap("", currentDeck = 7, facilities = emptyList())
        assertTrue(results.isNotEmpty())
        assertEquals(DeckSearchResultType.RESTROOM, results.first().type)
        assertEquals(7, results.first().deckNumber)
    }

    @Test
    fun `test searchDeckMap with prefix 11 prioritizes facilities and lists rooms in order`() {
        val sampleFacilities = listOf(
            FacilityItemUiModel(
                placeName = "城堡典藏 Castle Collection",
                categoryKey = "shop",
                categoryLabel = "商店",
                deck = 11,
                allDecks = listOf(11),
                primaryDeck = 11,
                zone = ShipZone.MID,
                formattedLocation = "Deck 11 船中",
                slots = emptyList(),
                is24Hours = false,
                isOpenNow = true,
                hasOperatingHours = true,
            )
        )
        val sampleRooms = listOf(
            StateroomEntry("11220", 11),
            StateroomEntry("11200", 11),
            StateroomEntry("11216", 11),
        )

        val results = searchDeckMap(
            query = "11",
            currentDeck = 11,
            facilities = sampleFacilities,
            stateroomSearcher = { sampleRooms.sortedBy { it.roomNumber.toInt() } },
        )

        assertTrue(results.size >= 4)
        // Facilities should be at the top
        assertEquals(DeckSearchResultType.FACILITY, results[0].type)
        assertEquals("城堡典藏 Castle Collection", results[0].title)

        // Staterooms should follow, sorted numerically
        assertEquals(DeckSearchResultType.STATEROOM, results[1].type)
        assertEquals("客房 11200", results[1].title)
        assertEquals("客房 11216", results[2].title)
        assertEquals("客房 11220", results[3].title)
    }

    @Test
    fun `test searchDeckMap with prefix 112 returns sorted staterooms`() {
        val sampleRooms = listOf(
            StateroomEntry("11230", 11),
            StateroomEntry("11200", 11),
            StateroomEntry("11216", 11),
            StateroomEntry("11220", 11),
        )

        val results = searchDeckMap(
            query = "112",
            currentDeck = 11,
            facilities = emptyList(),
            stateroomSearcher = { sampleRooms.filter { it.roomNumber.startsWith("112") }.sortedBy { it.roomNumber.toInt() } },
        )

        assertEquals(4, results.size)
        assertEquals("客房 11200", results[0].title)
        assertEquals("客房 11216", results[1].title)
        assertEquals("客房 11220", results[2].title)
        assertEquals("客房 11230", results[3].title)
    }
}
