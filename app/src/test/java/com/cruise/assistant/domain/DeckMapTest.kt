package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckMapTest {

    @Test
    fun `test getDecks returns passenger decks in descending order excluding 1-4 and 14`() {
        val decks = DeckMapRepository.getDecks(maxDeck = 19)
        assertEquals(14, decks.size)
        assertEquals(19, decks.first().deckNumber)
        assertEquals(5, decks.last().deckNumber)
        assertEquals("D19", decks.first().shortLabel)
        assertEquals("Deck 19", decks.first().name)

        val deckNumbers = decks.map { it.deckNumber }
        assertFalse(deckNumbers.contains(1))
        assertFalse(deckNumbers.contains(2))
        assertFalse(deckNumbers.contains(3))
        assertFalse(deckNumbers.contains(4))
        assertFalse(deckNumbers.contains(14))
    }

    @Test
    fun `test available deck map verification`() {
        // All passenger decks 5-13, 15-19 should have maps
        listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19).forEach {
            assertTrue("Deck $it should have map", DeckMapRepository.hasDeckMap(it))
        }

        assertFalse(DeckMapRepository.hasDeckMap(1))
        assertFalse(DeckMapRepository.hasDeckMap(4))
        assertFalse(DeckMapRepository.hasDeckMap(14))
        assertFalse(DeckMapRepository.hasDeckMap(20))
    }

    @Test
    fun `test assetUrlFor deck`() {
        assertEquals("file:///android_asset/decks/deck_5.svg", DeckMapRepository.assetUrlFor(5))
        assertEquals("file:///android_asset/decks/deck_19.svg", DeckMapRepository.assetUrlFor(19))
    }
}
