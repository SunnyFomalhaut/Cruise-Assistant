package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DollManagerTest {

    @Test
    fun dollTier_forCardHeight_thresholds() {
        // 30分钟及以下 (<= 60dp, 钳制到61dp) -> HEAD
        assertEquals(DollTier.HEAD, DollTier.forCardHeight(0f))
        assertEquals(DollTier.HEAD, DollTier.forCardHeight(35f))
        assertEquals(DollTier.HEAD, DollTier.forCardHeight(60f))
        assertEquals(DollTier.HEAD, DollTier.forCardHeight(61f))
        // 55分钟及以下 (31~55分钟，62dp ~ 110dp) -> HALF_BODY
        assertEquals(DollTier.HALF_BODY, DollTier.forCardHeight(62f))
        assertEquals(DollTier.HALF_BODY, DollTier.forCardHeight(90f))
        assertEquals(DollTier.HALF_BODY, DollTier.forCardHeight(110f))
        assertEquals(DollTier.HALF_BODY, DollTier.forCardHeight(111f))
        // 55分钟以上 (> 111dp) -> FULL_BODY
        assertEquals(DollTier.FULL_BODY, DollTier.forCardHeight(112f))
        assertEquals(DollTier.FULL_BODY, DollTier.forCardHeight(150f))
        assertEquals(DollTier.FULL_BODY, DollTier.forCardHeight(200f))
    }

    @Test
    fun all60AssetsRegistered_andProperlyTiered() {
        val allRegistered = DollManager.getAllAssets()
        assertEquals("Total registered doll assets must be exactly 60", 60, allRegistered.size)
        assertEquals(60, allRegistered.distinct().size)

        var headCount = 0
        var halfBodyCount = 0
        var fullBodyCount = 0

        DuffyCharacter.entries.forEach { character ->
            val heads = DollManager.getAssets(character, DollTier.HEAD)
            val halfBodies = DollManager.getAssets(character, DollTier.HALF_BODY)
            val fullBodies = DollManager.getAssets(character, DollTier.FULL_BODY)

            headCount += heads.size
            halfBodyCount += halfBodies.size
            fullBodyCount += fullBodies.size

            heads.forEach { path ->
                assertTrue("Head path $path must contain /head/", path.contains("/head/"))
                assertTrue("Head path $path must start with ${character.assetDir}", path.startsWith(character.assetDir))
                assertEquals(character, DollManager.getCharacterForAsset(path))
                assertEquals(DollTier.HEAD, DollManager.getTierForAsset(path))
            }
            halfBodies.forEach { path ->
                assertTrue("HalfBody path $path must contain /half_body/", path.contains("/half_body/"))
                assertTrue("HalfBody path $path must start with ${character.assetDir}", path.startsWith(character.assetDir))
                assertEquals(character, DollManager.getCharacterForAsset(path))
                assertEquals(DollTier.HALF_BODY, DollManager.getTierForAsset(path))
            }
            fullBodies.forEach { path ->
                assertTrue("FullBody path $path must contain /full_body/", path.contains("/full_body/"))
                assertTrue("FullBody path $path must start with ${character.assetDir}", path.startsWith(character.assetDir))
                assertEquals(character, DollManager.getCharacterForAsset(path))
                assertEquals(DollTier.FULL_BODY, DollManager.getTierForAsset(path))
            }
        }

        assertEquals("Total head tier assets must be 10", 10, headCount)
        assertEquals("Total half_body tier assets must be 7", 7, halfBodyCount)
        assertEquals("Total full_body tier assets must be 43", 43, fullBodyCount)
    }

    @Test
    fun shortCard_30minAndUnder_returnsHeadAsset() {
        // Cards <= 61dp (<= 30 min) get HEAD tier
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(eventId = eventId, cardHeightDp = 50f)
            assertNotNull("Asset must not be null for event $eventId", asset)
            val tier = DollManager.getTierForAsset(asset!!)
            // Every character has at least 1 head asset, so it should always resolve to HEAD
            assertEquals("Short card (50dp) should select HEAD tier for event $eventId", DollTier.HEAD, tier)
        }
    }

    @Test
    fun mediumCard_31to55min_returnsHalfBodyOrFallback() {
        // Cards between 62dp and 111dp (31~55 min) prefer HALF_BODY, fallback to HEAD then FULL_BODY
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(eventId = eventId, cardHeightDp = 90f)
            assertNotNull("Asset must not be null for event $eventId", asset)
            val char = DollManager.getCharacterForAsset(asset!!)
            assertNotNull(char)
            val tier = DollManager.getTierForAsset(asset)

            val hasHalfBody = DollManager.getAssets(char!!, DollTier.HALF_BODY).isNotEmpty()
            if (hasHalfBody) {
                assertEquals("Character ${char.name} has half_body, so it must return HALF_BODY", DollTier.HALF_BODY, tier)
            } else {
                // Duffy, ShellieMay, StellaLou don't have half_body, fallback order is HEAD then FULL_BODY
                val hasHead = DollManager.getAssets(char, DollTier.HEAD).isNotEmpty()
                if (hasHead) {
                    assertEquals("Character ${char.name} has no half_body, should fallback to HEAD", DollTier.HEAD, tier)
                } else {
                    assertEquals(DollTier.FULL_BODY, tier)
                }
            }
        }
    }

    @Test
    fun largeCard_over55min_returnsFullBodyAsset() {
        // Cards > 111dp (> 55 min) get FULL_BODY tier
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(eventId = eventId, cardHeightDp = 150f)
            assertNotNull("Asset must not be null for event $eventId", asset)
            val tier = DollManager.getTierForAsset(asset!!)
            // Every character has full_body assets, so it should always resolve to FULL_BODY
            assertEquals("Large card (150dp) should select FULL_BODY tier for event $eventId", DollTier.FULL_BODY, tier)
        }
    }

    @Test
    fun deterministicSelection_sameEventIdProducesIdenticalResult() {
        val eventId = 123456L
        val first = DollManager.selectDollForCard(eventId, 60f)
        val second = DollManager.selectDollForCard(eventId, 60f)
        val third = DollManager.selectDollForCard(eventId, 60f)

        assertNotNull(first)
        assertEquals("First and second run must produce identical result", first, second)
        assertEquals("First and third run must produce identical result", first, third)
    }

    @Test
    fun uniformDistribution_all7CharactersAppear() {
        val characterCounts = mutableMapOf<DuffyCharacter, Int>()
        val sampleSize = 700

        for (i in 1..sampleSize) {
            val asset = DollManager.selectDollForCard(eventId = i.toLong(), cardHeightDp = 80f)
            assertNotNull(asset)
            val char = DollManager.getCharacterForAsset(asset!!)
            assertNotNull(char)
            characterCounts[char!!] = characterCounts.getOrDefault(char, 0) + 1
        }

        // Verify all 7 characters appear
        assertEquals("All 7 characters must appear in random sampling", 7, characterCounts.keys.size)

        // Each character should appear roughly sampleSize / 7 = 100 times (+/- variance)
        characterCounts.forEach { (char, count) ->
            assertTrue(
                "Character ${char.name} count ($count) should be well distributed (between 50 and 150)",
                count in 50..150
            )
        }
    }

    @Test
    fun characterSelection_matchesEventIdModulo() {
        val characters = DuffyCharacter.entries
        characters.forEachIndexed { index, expectedChar ->
            val eventId = index.toLong()
            val asset = DollManager.selectDollForCard(eventId, 60f)
            assertNotNull(asset)
            val actualChar = DollManager.getCharacterForAsset(asset!!)
            assertEquals("EventId $eventId should map to character $expectedChar", expectedChar, actualChar)
        }

        // Verify with offset
        characters.forEachIndexed { index, expectedChar ->
            val eventId = (index + characters.size * 5).toLong()
            val asset = DollManager.selectDollForCard(eventId, 60f)
            assertNotNull(asset)
            val actualChar = DollManager.getCharacterForAsset(asset!!)
            assertEquals("EventId $eventId should map to character $expectedChar", expectedChar, actualChar)
        }
    }

    @Test
    fun negativeEventId_handlesGracefully() {
        for (eventId in -20L..-1L) {
            val asset = DollManager.selectDollForCard(eventId, 60f)
            assertNotNull("Asset must not be null for negative eventId $eventId", asset)
            val char = DollManager.getCharacterForAsset(asset!!)
            assertNotNull("Character must be resolved for negative eventId $eventId", char)
        }
    }
}
