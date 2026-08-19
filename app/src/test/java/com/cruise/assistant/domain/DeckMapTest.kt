package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckMapTest {

    @Test
    fun `test getDecks returns descending order from maxDeck to 1`() {
        val decks = DeckMapRepository.getDecks(maxDeck = 19)
        assertEquals(19, decks.size)
        assertEquals(19, decks.first().deckNumber)
        assertEquals(1, decks.last().deckNumber)
        assertEquals("D19", decks.first().shortLabel)
        assertEquals("Deck 19", decks.first().name)
    }

    @Test
    fun `test available deck map verification`() {
        assertTrue(DeckMapRepository.hasDeckMap(5))
        assertTrue(DeckMapRepository.hasDeckMap(6))
        assertTrue(DeckMapRepository.hasDeckMap(7))
        assertTrue(DeckMapRepository.hasDeckMap(8))
        assertTrue(DeckMapRepository.hasDeckMap(9))

        assertFalse(DeckMapRepository.hasDeckMap(1))
        assertFalse(DeckMapRepository.hasDeckMap(4))
        assertFalse(DeckMapRepository.hasDeckMap(10))
        assertFalse(DeckMapRepository.hasDeckMap(19))
    }

    @Test
    fun `test assetUrlFor deck`() {
        assertEquals("file:///android_asset/decks/deck_5.svg", DeckMapRepository.assetUrlFor(5))
        assertEquals("file:///android_asset/decks/deck_6.svg", DeckMapRepository.assetUrlFor(6))
    }
}
