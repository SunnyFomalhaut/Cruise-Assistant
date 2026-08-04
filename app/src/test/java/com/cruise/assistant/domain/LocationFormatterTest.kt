package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationFormatterTest {

    @Test
    fun fullLocation_isDeckZonePlace() {
        assertEquals("7F船中花园", formatLocation(7, ShipZone.MID, "花园"))
        assertEquals("9F船尾剧场", formatLocation(9, ShipZone.STERN, "剧场"))
        assertEquals("3F船头健身房", formatLocation(3, ShipZone.BOW, "健身房"))
    }

    @Test
    fun blankPlaceName_isOmitted() {
        assertEquals("7F船中", formatLocation(7, ShipZone.MID, null))
        assertEquals("7F船中", formatLocation(7, ShipZone.MID, ""))
        assertEquals("7F船中", formatLocation(7, ShipZone.MID, "   "))
    }

    @Test
    fun placeNameIsTrimmed() {
        assertEquals("7F船中花园", formatLocation(7, ShipZone.MID, "  花园  "))
    }

    @Test
    fun nullDeck_returnsNull_soTheRowIsHidden() {
        // 上船/下船事件不设地址，卡片第二行整行不显示
        assertNull(formatLocation(null, null, null))
        assertNull(formatLocation(null, ShipZone.MID, "花园"))
    }

    @Test
    fun deckWithoutZone_showsDeckAndPlace() {
        assertEquals("7F花园", formatLocation(7, null, "花园"))
        assertEquals("7F", formatLocation(7, null, null))
    }

    @Test
    fun twoDigitDeck_works() {
        assertEquals("11F船中泳池", formatLocation(11, ShipZone.MID, "泳池"))
    }
}
