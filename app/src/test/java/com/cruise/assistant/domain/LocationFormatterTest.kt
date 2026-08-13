package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationFormatterTest {

    @Test
    fun fullLocation_isDeckZonePlace() {
        assertEquals("Deck 7 船中 花园", formatLocation(7, ShipZone.MID, "花园"))
        assertEquals("Deck 9 船尾 剧场", formatLocation(9, ShipZone.STERN, "剧场"))
        assertEquals("Deck 3 船头 健身房", formatLocation(3, ShipZone.BOW, "健身房"))
    }

    @Test
    fun blankPlaceName_isOmitted() {
        assertEquals("Deck 7 船中", formatLocation(7, ShipZone.MID, null))
        assertEquals("Deck 7 船中", formatLocation(7, ShipZone.MID, ""))
        assertEquals("Deck 7 船中", formatLocation(7, ShipZone.MID, "   "))
    }

    @Test
    fun placeNameIsTrimmed() {
        assertEquals("Deck 7 船中 花园", formatLocation(7, ShipZone.MID, "  花园  "))
    }

    @Test
    fun nullDeck_returnsNull_soTheRowIsHidden() {
        // 登船/下船事件不设地址，卡片第二行整行不显示
        assertNull(formatLocation(null, null, null))
        assertNull(formatLocation(null, ShipZone.MID, "花园"))
    }

    @Test
    fun deckWithoutZone_showsDeckAndPlace() {
        assertEquals("Deck 7 花园", formatLocation(7, null, "花园"))
        assertEquals("Deck 7", formatLocation(7, null, null))
    }

    @Test
    fun twoDigitDeck_works() {
        assertEquals("Deck 11 船中 泳池", formatLocation(11, ShipZone.MID, "泳池"))
    }

    @Test
    fun compactFormat_usesDPrefix() {
        assertEquals("D6 船中 花园舞台", formatLocation(6, ShipZone.MID, "花园舞台", compact = true))
        assertEquals("D7 花园", formatLocation(7, null, "花园", compact = true))
        assertNull(formatLocation(null, null, null, compact = true))
    }

    @Test
    fun defaultFormat_keepsDeck() {
        // 通知正文/编辑界面等不传 compact → 保持 "Deck N"
        assertEquals("Deck 6 船中 花园舞台", formatLocation(6, ShipZone.MID, "花园舞台"))
    }
}
