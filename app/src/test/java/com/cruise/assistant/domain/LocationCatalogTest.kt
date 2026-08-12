package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCatalogTest {

    @Test
    fun catalog_hasFiveStages_andTenDiningSpots() {
        assertEquals(5, BUILT_IN_LOCATION_CATALOG.getValue("show").size)
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
        assertTrue(stages.any { it.placeName == "泳池舞台" && it.deck == 10 && it.zone == ShipZone.STERN })
    }
}
