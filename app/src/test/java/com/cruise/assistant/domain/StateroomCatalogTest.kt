package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StateroomCatalogTest {

    @Test
    fun `test parseStateroomsJson and prefix search`() {
        val json = """{
            "staterooms": [
                {"room": "11220", "deck": 11},
                {"room": "11216", "deck": 11},
                {"room": "11200", "deck": 11},
                {"room": "6012", "deck": 6},
                {"room": "16802", "deck": 16}
            ]
        }"""
        val rooms = StateroomCatalog.parseStateroomsJson(json)
        assertEquals(5, rooms.size)

        StateroomCatalog.setStaterooms(rooms)

        // Prefix search for "112"
        val prefix112 = StateroomCatalog.search("112")
        assertEquals(3, prefix112.size)
        // Check sorted numerically
        assertEquals("11200", prefix112[0].roomNumber)
        assertEquals("11216", prefix112[1].roomNumber)
        assertEquals("11220", prefix112[2].roomNumber)

        // Search for "6"
        val prefix6 = StateroomCatalog.search("6")
        assertEquals(1, prefix6.size)
        assertEquals("6012", prefix6[0].roomNumber)

        // Non digit search
        val nonDigit = StateroomCatalog.search("abc")
        assertTrue(nonDigit.isEmpty())
    }

    @Test
    fun `test real staterooms json loads all 2111 rooms`() {
        val file = File("src/main/assets/staterooms.json")
        if (!file.exists()) return
        val json = file.readText()
        val rooms = StateroomCatalog.parseStateroomsJson(json)
        assertEquals(2111, rooms.size)

        StateroomCatalog.setStaterooms(rooms)
        val prefix112 = StateroomCatalog.search("112")
        assertTrue("Should have multiple rooms starting with 112", prefix112.size > 10)
        assertEquals("11200", prefix112.first().roomNumber)
    }
}
