package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StateroomInputValidationTest {

    @Before
    fun setup() {
        StateroomCatalog.setStaterooms(
            listOf(
                StateroomEntry(roomNumber = "6012", deck = 6),
                StateroomEntry(roomNumber = "11216", deck = 11),
                StateroomEntry(roomNumber = "11200", deck = 11),
                StateroomEntry(roomNumber = "16802", deck = 16),
            )
        )
    }

    @Test
    fun testParseStateroomInput() {
        assertEquals(Pair(6, "012"), parseStateroomInput("6012"))
        assertEquals(Pair(11, "216"), parseStateroomInput("11216"))
        assertEquals(Pair(16, "802"), parseStateroomInput("16802"))
        assertNull(parseStateroomInput("123"))
        assertNull(parseStateroomInput("123456"))
        assertNull(parseStateroomInput("abcd"))
        assertNull(parseStateroomInput("14100")) // Deck 14 doesn't exist
    }

    @Test
    fun testValidateStateroomInput_Incomplete() {
        val result1 = validateStateroomInput("")
        assertEquals(StateroomValidationStatus.INCOMPLETE, result1.status)

        val result2 = validateStateroomInput("112")
        assertEquals(StateroomValidationStatus.INCOMPLETE, result2.status)
    }

    @Test
    fun testValidateStateroomInput_Valid() {
        val result = validateStateroomInput("11216")
        assertEquals(StateroomValidationStatus.VALID, result.status)
        assertEquals(11, result.deck)
        assertEquals("216", result.room)
        assertEquals("11216", result.fullRoom)
    }

    @Test
    fun testValidateStateroomInput_Invalid() {
        val result = validateStateroomInput("11999")
        assertEquals(StateroomValidationStatus.INVALID, result.status)
        assertEquals("地图上无此房间，请核对", result.message)
    }
}
