package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class LocationCatalogLoaderTest {
    @Test
    fun parse_sampleJson_returnsVenues() {
        val json = """{"venues":[
            {"name":"动画师餐厅 Animator's Table","deck":9,"zone":"stern","category":"meal","group":"main",
             "slots":[
               {"start":"08:30","end":"10:00","meal":"breakfast"},
               {"start":"17:45","end":"19:45","meal":"dinner"},
               {"start":"22:00","end":"23:59","meal":"snack","lateNight":true}]},
            {"name":"Edge","deck":7,"zone":"stern","category":"youth",
             "slots":[{"start":"09:00","end":"01:00"}]},
            {"name":"Room Service","category":"meal"}
        ]}"""
        val venues = parseLocationsJson(json)
        assertEquals(3, venues.size)
        val animator = venues[0]
        assertEquals("动画师餐厅 Animator's Table", animator.placeName)
        assertEquals(ShipZone.STERN, animator.zone)
        assertEquals(DiningGroup.MAIN, animator.diningGroup)
        assertEquals(3, animator.hours?.slots?.size)
        assertTrue(animator.hours!!.slots[2].lateNight)
        assertTrue(animator.isOpenAt(LocalTime.of(18, 0)))
        val edge = venues[1]
        assertEquals("youth", edge.category)
        assertTrue(edge.isOpenAt(LocalTime.of(0, 30)))  // 跨午夜
        assertNull(venues[2].deck)                       // Room Service 无甲板
        assertNull(venues[2].hours)                      // 无时段
    }

    @Test
    fun parse_invalidJson_returnsEmpty() {
        assertEquals(emptyList<LocationCatalogEntry>(), parseLocationsJson("not json"))
    }
}
