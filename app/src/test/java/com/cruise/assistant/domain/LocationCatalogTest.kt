package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class LocationCatalogTest {

    @Test
    fun catalog_hasFiveStages_andTenDiningSpots() {
        assertEquals(4, BUILT_IN_LOCATION_CATALOG.getValue("show").size)
        assertEquals(10, BUILT_IN_LOCATION_CATALOG.getValue("meal").size)
    }

    @Test
    fun diningGroups_haveExpectedCounts() {
        val meal = BUILT_IN_LOCATION_CATALOG.getValue("meal")
        assertEquals(3, meal.count { it.diningGroup == DiningGroup.MAIN })
        assertEquals(3, meal.count { it.diningGroup == DiningGroup.BAR })
        assertEquals(2, meal.count { it.diningGroup == DiningGroup.PAID })
        assertEquals(2, meal.count { it.diningGroup == DiningGroup.QUICK })
    }

    @Test
    fun stageEntries_haveNoDiningGroup() {
        BUILT_IN_LOCATION_CATALOG.getValue("show").forEach { entry ->
            assertTrue("stage ${entry.placeName} has diningGroup", entry.diningGroup == null)
        }
    }

    @Test
    fun allEntries_deckWithinAdventureRange_andPlaceNamesUnique() {
        val all = BUILT_IN_LOCATION_CATALOG.values.flatten()
        all.forEach { entry ->
            assertTrue("${entry.placeName} deck ${entry.deck} out of 5..19", entry.deck in 5..19)
        }
        val names = all.map { it.placeName }
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun stageCatalog_matchesSpecData() {
        val stages = BUILT_IN_LOCATION_CATALOG.getValue("show")
        assertTrue(stages.any { it.placeName == "华特迪士尼剧院 Walt Disney Theatre" && it.deck == 6 && it.zone == ShipZone.BOW })
        assertTrue(stages.any { it.placeName == "寻路者湾 Wayfinder Bay" && it.deck == 10 && it.zone == ShipZone.STERN })
        assertTrue(stages.any { it.placeName == "花园舞台 Garden Stage" && it.deck == 10 && it.zone == ShipZone.STERN })
        assertTrue(stages.any { it.placeName == "露天泳池" && it.deck == 17 && it.zone == ShipZone.MID })
    }

    @Test
    fun isOpenFor_mealPeriodRules() {
        val animator = LocationCatalogEntry(
            deck = 9, zone = ShipZone.STERN, placeName = "动画师餐厅 Animator's Table Restaurant",
            diningGroup = DiningGroup.MAIN,
            hours = OperatingHours(
                breakfast = OperatingSlot(LocalTime.of(8, 30), LocalTime.of(10, 0)),
                lunch = OperatingSlot(LocalTime.of(12, 0), LocalTime.of(13, 30)),
                dinner = OperatingSlot(LocalTime.of(18, 0), LocalTime.of(22, 0)),
            ),
        )
        assertTrue(animator.isOpenFor(MealPeriod.BREAKFAST))
        assertTrue(animator.isOpenFor(MealPeriod.LUNCH))
        assertTrue(animator.isOpenFor(MealPeriod.DINNER))
        assertTrue(animator.isOpenFor(MealPeriod.LATE_NIGHT))   // 晚餐结束 22:00 → ≥22 成立
        assertTrue(animator.isOpenFor(MealPeriod.SNACK))
        assertTrue(animator.isOpenFor(MealPeriod.DRINK))        // 晚餐非空
    }

    @Test
    fun isOpenFor_unconfigured_alwaysOpen() {
        val moana = LocationCatalogEntry(6, ShipZone.BOW, "华特迪士尼剧院 Walt Disney Theatre")
        MealPeriod.entries.forEach { assertTrue(moana.isOpenFor(it)) }
    }

    @Test
    fun isOpenFor_quickService_noBreakfast() {
        // 快餐店：只做午餐+晚餐，早餐不营业
        val mogli = LocationCatalogEntry(
            deck = 10, zone = ShipZone.BOW, placeName = "莫格利餐厅 Mowgli's Eatery",
            diningGroup = DiningGroup.QUICK,
            hours = OperatingHours(
                lunch = OperatingSlot(LocalTime.of(12, 0), LocalTime.of(18, 0)),
                dinner = OperatingSlot(LocalTime.of(18, 0), LocalTime.of(23, 59)),  // brief 的 24:00 无法用 LocalTime 表示
            ),
        )
        assertFalse(mogli.isOpenFor(MealPeriod.BREAKFAST))
        assertTrue(mogli.isOpenFor(MealPeriod.LUNCH))
        assertTrue(mogli.isOpenFor(MealPeriod.LATE_NIGHT))  // 晚餐到 23:59（end.hour=23 ≥ 22）
        assertTrue(mogli.isOpenFor(MealPeriod.DRINK))
    }

    @Test
    fun isOpenAt_halfOpenRanges() {
        val animator = LocationCatalogEntry(
            deck = 9, zone = ShipZone.STERN, placeName = "动画师餐厅 Animator's Table Restaurant",
            diningGroup = DiningGroup.MAIN,
            hours = OperatingHours(
                breakfast = OperatingSlot(LocalTime.of(8, 30), LocalTime.of(10, 0)),
                lunch = OperatingSlot(LocalTime.of(12, 0), LocalTime.of(13, 30)),
                dinner = OperatingSlot(LocalTime.of(18, 0), LocalTime.of(22, 0)),
            ),
        )
        assertTrue(animator.isOpenAt(LocalTime.of(9, 0)))     // 早餐中
        assertFalse(animator.isOpenAt(LocalTime.of(10, 0)))    // 结束点不营业（半开）
        assertTrue(animator.isOpenAt(LocalTime.of(12, 30)))    // 午餐中
        assertFalse(animator.isOpenAt(LocalTime.of(15, 0)))    // 空档
        assertTrue(animator.isOpenAt(LocalTime.of(21, 59)))    // 晚餐中
        assertFalse(animator.isOpenAt(LocalTime.of(22, 0)))    // 结束点不营业
    }

    @Test
    fun isOpenAt_unconfigured_alwaysOpen() {
        val stage = LocationCatalogEntry(6, ShipZone.BOW, "华特迪士尼剧院 Walt Disney Theatre")
        assertTrue(stage.isOpenAt(LocalTime.of(3, 0)))
    }
}
