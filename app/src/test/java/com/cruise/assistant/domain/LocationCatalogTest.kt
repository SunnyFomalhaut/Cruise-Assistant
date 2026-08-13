package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class LocationCatalogTest {

    /** 动画师餐厅：早餐/午餐/两档晚餐/夜宵零食档（带标签与打标） */
    private fun animatorHours() = OperatingHours(
        slots = listOf(
            OperatingSlot(LocalTime.of(8, 30), LocalTime.of(10, 0), MealPeriod.BREAKFAST),
            OperatingSlot(LocalTime.of(12, 0), LocalTime.of(13, 30), MealPeriod.LUNCH),
            OperatingSlot(LocalTime.of(17, 45), LocalTime.of(19, 45), MealPeriod.DINNER),
            OperatingSlot(LocalTime.of(20, 15), LocalTime.of(22, 15), MealPeriod.DINNER),
            OperatingSlot(LocalTime.of(22, 0), LocalTime.of(23, 59), MealPeriod.SNACK, lateNight = true),
        ),
    )

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
            assertTrue("${entry.placeName} deck ${entry.deck} out of 5..19", entry.deck != null && entry.deck in 5..19)
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
            diningGroup = DiningGroup.MAIN, hours = animatorHours(),
        )
        assertTrue(animator.isOpenFor(MealPeriod.BREAKFAST))
        assertTrue(animator.isOpenFor(MealPeriod.LUNCH))
        assertTrue(animator.isOpenFor(MealPeriod.DINNER))
        assertTrue(animator.isOpenFor(MealPeriod.LATE_NIGHT))   // 22:00-23:59 打标
        assertTrue(animator.isOpenFor(MealPeriod.SNACK))        // 零食档带 snack 标签
        assertFalse(animator.isOpenFor(MealPeriod.DRINK))       // 无 drink 标签
    }

    @Test
    fun isOpenFor_usesSlotTags() {
        val animator = LocationCatalogEntry(
            deck = 9, zone = ShipZone.STERN, placeName = "动画师餐厅 Animator's Table",
            diningGroup = DiningGroup.MAIN, hours = animatorHours(),
        )
        assertTrue(animator.isOpenFor(MealPeriod.BREAKFAST))
        assertTrue(animator.isOpenFor(MealPeriod.LUNCH))
        assertTrue(animator.isOpenFor(MealPeriod.DINNER))
        assertTrue(animator.isOpenFor(MealPeriod.LATE_NIGHT))  // 22:00-23:59 打标
        assertTrue(animator.isOpenFor(MealPeriod.SNACK))
        assertFalse(animator.isOpenFor(MealPeriod.DRINK))      // 无 drink 标签
    }

    @Test
    fun isOpenFor_emptySlots_alwaysOpen() {
        val stage = LocationCatalogEntry(6, ShipZone.BOW, "华特迪士尼剧院 Walt Disney Theatre")
        MealPeriod.entries.forEach { assertTrue(stage.isOpenFor(it)) }
        assertTrue(stage.isOpenAt(LocalTime.of(3, 0)))
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
                slots = listOf(
                    OperatingSlot(LocalTime.of(12, 0), LocalTime.of(18, 0), MealPeriod.LUNCH),
                    // 晚餐到 23:59 且 18:00 开始 → 按导入规则打夜宵标
                    OperatingSlot(LocalTime.of(18, 0), LocalTime.of(23, 59), MealPeriod.DINNER, lateNight = true),
                ),
            ),
        )
        assertFalse(mogli.isOpenFor(MealPeriod.BREAKFAST))
        assertTrue(mogli.isOpenFor(MealPeriod.LUNCH))
        assertTrue(mogli.isOpenFor(MealPeriod.LATE_NIGHT))
        assertFalse(mogli.isOpenFor(MealPeriod.DRINK))  // 无 drink 标签
    }

    @Test
    fun isOpenAt_halfOpenRanges() {
        val animator = LocationCatalogEntry(
            deck = 9, zone = ShipZone.STERN, placeName = "动画师餐厅 Animator's Table Restaurant",
            diningGroup = DiningGroup.MAIN, hours = animatorHours(),
        )
        assertTrue(animator.isOpenAt(LocalTime.of(9, 0)))     // 早餐中
        assertFalse(animator.isOpenAt(LocalTime.of(10, 0)))    // 结束点不营业（半开）
        assertTrue(animator.isOpenAt(LocalTime.of(12, 30)))    // 午餐中
        assertFalse(animator.isOpenAt(LocalTime.of(15, 0)))    // 空档
        assertTrue(animator.isOpenAt(LocalTime.of(21, 59)))    // 第二档晚餐中
        assertFalse(animator.isOpenAt(LocalTime.of(20, 0)))    // 两档晚餐之间的空档
        assertTrue(animator.isOpenAt(LocalTime.of(22, 15)))    // 零食档在晚餐结束后继续
        assertTrue(animator.isOpenAt(LocalTime.of(23, 30)))    // 零食档中
        assertFalse(animator.isOpenAt(LocalTime.of(23, 59)))   // 结束点不营业（半开）
    }

    @Test
    fun isOpenAt_crossMidnightSlot() {
        // Edge 9:00–次日 1:00：23:00 营业、2:00 不营业、8:00 不营业
        val edge = LocationCatalogEntry(
            deck = 7, zone = ShipZone.STERN, placeName = "Edge", category = "youth",
            hours = OperatingHours(slots = listOf(OperatingSlot(LocalTime.of(9, 0), LocalTime.of(1, 0)))),
        )
        assertTrue(edge.isOpenAt(LocalTime.of(23, 0)))
        assertTrue(edge.isOpenAt(LocalTime.of(0, 30)))
        assertFalse(edge.isOpenAt(LocalTime.of(2, 0)))
        assertFalse(edge.isOpenAt(LocalTime.of(8, 0)))
    }

    @Test
    fun isOpenAt_unconfigured_alwaysOpen() {
        val stage = LocationCatalogEntry(6, ShipZone.BOW, "华特迪士尼剧院 Walt Disney Theatre")
        assertTrue(stage.isOpenAt(LocalTime.of(3, 0)))
    }
}
