package com.cruise.assistant.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class DeckMapPixieOverlayTest {

    @Test
    fun testInlineHtmlWrapperFor_InjectsPixieMarkers() {
        val svg = """<svg><rect id="room-11216" /><text id="number-11216">11216</text></svg>"""
        val markers = listOf(
            PixieRoomMarker(roomNumber = "11216", status = PixieRoomStatus.UNREPLIED, unrepliedCount = 1),
            PixieRoomMarker(roomNumber = "11200", status = PixieRoomStatus.NEED_SUPPLEMENT, givenCount = 1, receivedCount = 2),
            PixieRoomMarker(roomNumber = "11220", status = PixieRoomStatus.COMPLETED, givenCount = 1, receivedCount = 1),
        )

        val html = DeckMapRepository.inlineHtmlWrapperFor(svg, null, markers)

        assertTrue("Should contain pixie-room-marker styles", html.contains("pixie-marker-unreplied"))
        assertTrue("Should contain pixie-marker-supplement styles", html.contains("pixie-marker-supplement"))
        assertTrue("Should contain pixie-marker-completed styles", html.contains("pixie-marker-completed"))
        assertTrue("Should inject room 11216 marker JS", html.contains("11216"))
        assertTrue("Should inject room 11200 marker JS", html.contains("11200"))
        assertTrue("Should inject room 11220 marker JS", html.contains("11220"))
    }
}
