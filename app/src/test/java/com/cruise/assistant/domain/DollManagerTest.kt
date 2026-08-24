package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun all91AssetsRegistered_andProperlyTiered_andAllWebP() {
        val allRegistered = DollManager.getAllAssets()
        assertEquals("Total registered doll assets must be exactly 91", 91, allRegistered.size)
        assertEquals("All asset paths must be distinct", 91, allRegistered.distinct().size)

        // Verify all 91 assets end with .webp and have non-empty valid names
        allRegistered.forEach { path ->
            assertTrue("Asset path $path must end with .webp", path.endsWith(".webp"))
            assertTrue("Asset path $path must not contain double slashes", !path.contains("//"))
            assertTrue("Asset path $path must start with dolls/", path.startsWith("dolls/"))
        }

        // Verify Duo assets (8 total)
        assertEquals("Duo assets pool must contain exactly 8 items", 8, DollManager.DUO_ASSETS.size)
        assertEquals(8, DollManager.DUO_ASSETS.distinct().size)
        DollManager.DUO_ASSETS.forEach { duoPath ->
            assertTrue("Duo asset $duoPath must start with dolls/duo/", duoPath.startsWith("dolls/duo/"))
            assertTrue("Duo asset $duoPath must end with .webp", duoPath.endsWith(".webp"))
            assertEquals(DollTier.DUO, DollManager.getTierForAsset(duoPath))
            assertNull("Duo asset should not map to a single character", DollManager.getCharacterForAsset(duoPath))
        }

        // Verify single character assets (83 total: 17 head, 11 half_body, 55 full_body)
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
                assertTrue("Head path $path must start with ${character.assetDir}/head/", path.startsWith("${character.assetDir}/head/"))
                assertTrue("Head path $path must end with .webp", path.endsWith(".webp"))
                assertEquals(character, DollManager.getCharacterForAsset(path))
                assertEquals(DollTier.HEAD, DollManager.getTierForAsset(path))
            }
            halfBodies.forEach { path ->
                assertTrue("HalfBody path $path must contain /half_body/", path.contains("/half_body/"))
                assertTrue("HalfBody path $path must start with ${character.assetDir}/half_body/", path.startsWith("${character.assetDir}/half_body/"))
                assertTrue("HalfBody path $path must end with .webp", path.endsWith(".webp"))
                assertEquals(character, DollManager.getCharacterForAsset(path))
                assertEquals(DollTier.HALF_BODY, DollManager.getTierForAsset(path))
            }
            fullBodies.forEach { path ->
                assertTrue("FullBody path $path must contain /full_body/", path.contains("/full_body/"))
                assertTrue("FullBody path $path must start with ${character.assetDir}/full_body/", path.startsWith("${character.assetDir}/full_body/"))
                assertTrue("FullBody path $path must end with .webp", path.endsWith(".webp"))
                assertEquals(character, DollManager.getCharacterForAsset(path))
                assertEquals(DollTier.FULL_BODY, DollManager.getTierForAsset(path))
            }
        }

        assertEquals("Total head tier assets must be 17", 17, headCount)
        assertEquals("Total half_body tier assets must be 11", 11, halfBodyCount)
        assertEquals("Total full_body tier assets must be 55", 55, fullBodyCount)
        assertEquals("Total single character assets must be 83", 83, headCount + halfBodyCount + fullBodyCount)
    }

    @Test
    fun isEligibleForDuo_evaluates5ConditionsCorrectly() {
        // Condition 1: Finalized + 75min + clean (no wait, no show, no conflict) -> true
        assertTrue(
            DollTier.isEligibleForDuo(
                isFinalized = true,
                durationMinutes = 75,
                hasWaitSegment = false,
                isShowCategory = false,
                hasConflict = false
            )
        )
        assertTrue(
            DollTier.isEligibleForDuo(
                isFinalized = true,
                durationMinutes = 70, // Boundary: exactly 70min
                hasWaitSegment = false,
                isShowCategory = false,
                hasConflict = false
            )
        )

        // Draft mode -> false
        assertFalse(
            DollTier.isEligibleForDuo(
                isFinalized = false,
                durationMinutes = 75,
                hasWaitSegment = false,
                isShowCategory = false,
                hasConflict = false
            )
        )

        // Duration < 70min -> false
        assertFalse(
            DollTier.isEligibleForDuo(
                isFinalized = true,
                durationMinutes = 69,
                hasWaitSegment = false,
                isShowCategory = false,
                hasConflict = false
            )
        )
        assertFalse(
            DollTier.isEligibleForDuo(
                isFinalized = true,
                durationMinutes = 60,
                hasWaitSegment = false,
                isShowCategory = false,
                hasConflict = false
            )
        )

        // Has wait segment -> false
        assertFalse(
            DollTier.isEligibleForDuo(
                isFinalized = true,
                durationMinutes = 75,
                hasWaitSegment = true,
                isShowCategory = false,
                hasConflict = false
            )
        )

        // Is show category -> false
        assertFalse(
            DollTier.isEligibleForDuo(
                isFinalized = true,
                durationMinutes = 75,
                hasWaitSegment = false,
                isShowCategory = true,
                hasConflict = false
            )
        )

        // Has conflict -> false
        assertFalse(
            DollTier.isEligibleForDuo(
                isFinalized = true,
                durationMinutes = 75,
                hasWaitSegment = false,
                isShowCategory = false,
                hasConflict = true
            )
        )
    }

    @Test
    fun selectDollForCard_finalizedAndLongAndClean_triggersDuoAsset() {
        // Test 1: Finalized + 75min + clean layout (isComplex = false) -> returns path starting with dolls/duo/
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(
                eventId = eventId,
                cardHeightDp = 150f,
                durationMinutes = 75,
                isFinalized = true,
                isComplex = false
            )
            assertNotNull("Asset must not be null for event $eventId", asset)
            assertTrue("Asset must start with dolls/duo/ for event $eventId, got $asset", asset!!.startsWith("dolls/duo/"))
            assertEquals(DollTier.DUO, DollManager.getTierForAsset(asset))
        }
    }

    @Test
    fun selectDollForCard_draftMode_fallsBackToSingleDollFullBody() {
        // Test 2: Draft mode (isFinalized = false) + 75min + clean layout -> returns single doll FULL_BODY
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(
                eventId = eventId,
                cardHeightDp = 150f,
                durationMinutes = 75,
                isFinalized = false,
                isComplex = false
            )
            assertNotNull("Asset must not be null for event $eventId", asset)
            assertFalse("Draft mode should NOT return duo asset", asset!!.startsWith("dolls/duo/"))
            val char = DollManager.getCharacterForAsset(asset)
            assertNotNull("Draft mode must return single character doll", char)
            assertEquals("Draft mode >111dp should return FULL_BODY tier", DollTier.FULL_BODY, DollManager.getTierForAsset(asset))
        }
    }

    @Test
    fun selectDollForCard_complexLayout_fallsBackToSingleDollFullBody() {
        // Test 3: Finalized + 75min + complex layout (isComplex = true) -> returns single doll FULL_BODY
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(
                eventId = eventId,
                cardHeightDp = 150f,
                durationMinutes = 75,
                isFinalized = true,
                isComplex = true
            )
            assertNotNull("Asset must not be null for event $eventId", asset)
            assertFalse("Complex layout should NOT return duo asset", asset!!.startsWith("dolls/duo/"))
            val char = DollManager.getCharacterForAsset(asset)
            assertNotNull("Complex layout must return single character doll", char)
            assertEquals("Complex layout >111dp should return FULL_BODY tier", DollTier.FULL_BODY, DollManager.getTierForAsset(asset))
        }
    }

    @Test
    fun selectDollForCard_durationUnder70Min_fallsBackToSingleDollFullBody() {
        // Test 4: Finalized + 60min (< 70 min) + clean layout -> returns single doll FULL_BODY
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(
                eventId = eventId,
                cardHeightDp = 120f,
                durationMinutes = 60,
                isFinalized = true,
                isComplex = false
            )
            assertNotNull("Asset must not be null for event $eventId", asset)
            assertFalse("Duration < 70min should NOT return duo asset", asset!!.startsWith("dolls/duo/"))
            val char = DollManager.getCharacterForAsset(asset)
            assertNotNull("Duration < 70min must return single character doll", char)
            assertEquals("Card >111dp should return FULL_BODY tier", DollTier.FULL_BODY, DollManager.getTierForAsset(asset))
        }
    }

    @Test
    fun shortCard_30minAndUnder_returnsHeadAsset() {
        // Cards <= 61dp (<= 30 min) get HEAD tier
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(eventId = eventId, cardHeightDp = 50f, durationMinutes = 30)
            assertNotNull("Asset must not be null for event $eventId", asset)
            val tier = DollManager.getTierForAsset(asset!!)
            // Every character has at least 1 head asset, so it should always resolve to HEAD
            assertEquals("Short card (50dp) should select HEAD tier for event $eventId", DollTier.HEAD, tier)
            assertFalse("Short card should not be duo", asset.startsWith("dolls/duo/"))
        }
    }

    @Test
    fun mediumCard_31to55min_returnsHalfBodyOrFallback() {
        // Cards between 62dp and 111dp (31~55 min) prefer HALF_BODY, fallback to HEAD then FULL_BODY
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(eventId = eventId, cardHeightDp = 90f, durationMinutes = 45)
            assertNotNull("Asset must not be null for event $eventId", asset)
            val char = DollManager.getCharacterForAsset(asset!!)
            assertNotNull(char)
            val tier = DollManager.getTierForAsset(asset)

            val hasHalfBody = DollManager.getAssets(char!!, DollTier.HALF_BODY).isNotEmpty()
            if (hasHalfBody) {
                assertEquals("Character ${char.name} has half_body, so it must return HALF_BODY", DollTier.HALF_BODY, tier)
            } else {
                // ShellieMay and StellaLou don't have half_body, fallback order is HEAD then FULL_BODY
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
        // Cards > 111dp (> 55 min) in draft mode get FULL_BODY tier
        for (eventId in 1L..100L) {
            val asset = DollManager.selectDollForCard(eventId = eventId, cardHeightDp = 150f, durationMinutes = 55, isFinalized = false)
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

        // Deterministic duo selection
        val duo1 = DollManager.selectDollForCard(eventId, 150f, durationMinutes = 75, isFinalized = true, isComplex = false)
        val duo2 = DollManager.selectDollForCard(eventId, 150f, durationMinutes = 75, isFinalized = true, isComplex = false)
        assertNotNull(duo1)
        assertEquals("Duo selection must be deterministic", duo1, duo2)
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

            val duoAsset = DollManager.selectDollForCard(eventId, 150f, durationMinutes = 75, isFinalized = true, isComplex = false)
            assertNotNull("Duo asset must not be null for negative eventId $eventId", duoAsset)
            assertTrue("Duo asset must start with dolls/duo/", duoAsset!!.startsWith("dolls/duo/"))
        }
    }
}

