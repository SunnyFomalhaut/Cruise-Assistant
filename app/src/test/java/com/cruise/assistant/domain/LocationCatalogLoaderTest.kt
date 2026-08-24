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

    @Test
    fun parse_barAndQuickVenues_areMealCategoryWithGroups() {
        val json = """{"venues":[
            {"name":"Spellbound","deck":6,"zone":"mid","category":"meal","group":"bar",
             "slots":[{"start":"07:30","end":"01:00","meal":"drink"}]},
            {"name":"Mowgli's Eatery","deck":10,"zone":"mid","category":"meal","group":"quick",
             "slots":[{"start":"22:00","end":"00:00","meal":"snack","lateNight":true}]}
        ]}"""
        val venues = parseLocationsJson(json)
        assertEquals(2, venues.size)
        assertEquals(DiningGroup.BAR, venues[0].diningGroup)
        assertEquals(DiningGroup.QUICK, venues[1].diningGroup)
        assertTrue(venues[0].isOpenFor(MealPeriod.DRINK))
        assertTrue(venues[1].isOpenFor(MealPeriod.LATE_NIGHT))
        assertTrue(venues[1].isOpenAt(LocalTime.of(23, 0)))  // 跨午夜
    }

    @Test
    fun parse_dinnerSecondSeating_lateNightTagged() {
        val json = """{"venues":[{"name":"Animator's Table","deck":9,"zone":"stern",
            "category":"meal","group":"main",
            "slots":[{"start":"20:15","end":"22:15","meal":"dinner","lateNight":true}]}]}"""
        val venue = parseLocationsJson(json).first()
        assertTrue(venue.isOpenFor(MealPeriod.LATE_NIGHT))
    }

    @Test
    fun parse_roomService_hasBreakfastAndSnackTags() {
        val json = """{"venues":[{"name":"客房送餐 Room Service","category":"meal","group":"quick",
            "slots":[{"start":"00:00","end":"23:59","meal":"breakfast"},{"start":"00:00","end":"23:59","meal":"snack"}]}]}"""
        val venue = parseLocationsJson(json).first()
        assertTrue(venue.isOpenFor(MealPeriod.BREAKFAST))
        assertTrue(venue.isOpenFor(MealPeriod.SNACK))
    }

    @Test
    fun parse_dollarVenues_arePaidGroup() {
        val json = """{"venues":[{"name":"巴哈咖啡 Bacha Coffee","deck":7,"zone":"mid","category":"meal","group":"paid",
            "slots":[{"start":"10:00","end":"22:00","meal":"drink"}]}]}"""
        val venue = parseLocationsJson(json).first()
        assertEquals(DiningGroup.PAID, venue.diningGroup)
    }

    @Test
    fun parse_realLocationsJson_mainDiningDinnerHoursUnified() {
        val jsonFile = java.io.File("src/main/assets/locations.json")
        if (!jsonFile.exists()) return
        val json = jsonFile.readText()
        val venues = parseLocationsJson(json)
        val mainVenues = venues.filter { it.diningGroup == DiningGroup.MAIN }

        val seating2200 = listOf(
            "动画师餐厅 Animator's Table Restaurant",
            "魔法盛夏餐厅 Enchanted Summer Restaurant",
            "动画师味觉餐厅 Animator's Palate",
            "好莱坞聚光灯俱乐部 Hollywood Spotlight Club",
        )
        val seating2215 = listOf(
            "皮克斯市场餐厅 Pixar Market Restaurant",
            "航海家俱乐部 Navigator's Club Restaurant",
        )

        for (name in seating2200) {
            val v = mainVenues.firstOrNull { it.placeName == name }
            org.junit.Assert.assertNotNull("Venue $name should exist", v)
            val dinnerSlots = v!!.hours!!.slots.filter { it.meal == MealPeriod.DINNER }
            assertEquals("Venue $name should have exactly 1 dinner slot", 1, dinnerSlots.size)
            assertEquals(LocalTime.of(17, 45), dinnerSlots[0].start)
            assertEquals(LocalTime.of(22, 0), dinnerSlots[0].end)
        }

        for (name in seating2215) {
            val v = mainVenues.firstOrNull { it.placeName == name }
            org.junit.Assert.assertNotNull("Venue $name should exist", v)
            val dinnerSlots = v!!.hours!!.slots.filter { it.meal == MealPeriod.DINNER }
            assertEquals("Venue $name should have exactly 1 dinner slot", 1, dinnerSlots.size)
            assertEquals(LocalTime.of(17, 45), dinnerSlots[0].start)
            assertEquals(LocalTime.of(22, 15), dinnerSlots[0].end)
        }
    }
}

class TmpNullGuardTest {
    // 防御回归：Android 框架 org.json 对 JSON null 值 optString 返回字符串 "null"，
    // JVM 测试库返回 null——用字面量 "null" 模拟 Android 行为，钉住防御。
    @org.junit.Test
    fun parse_nullZoneAsLiteralNullString_isSafe() {
        val json = """{"venues":[{"name":"Room Service","deck":null,"zone":null,"category":"meal"}]}"""
        val venues = parseLocationsJson(json)
        org.junit.Assert.assertEquals(1, venues.size)
        org.junit.Assert.assertNull(venues[0].zone)
        org.junit.Assert.assertNull(venues[0].deck)
    }
}
