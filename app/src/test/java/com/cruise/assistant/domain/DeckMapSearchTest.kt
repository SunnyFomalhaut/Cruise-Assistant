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
}
