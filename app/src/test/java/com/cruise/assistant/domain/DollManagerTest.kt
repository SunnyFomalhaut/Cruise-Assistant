package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DollManagerTest {

    @Test
    fun compactCard_under44dp_returnsNullForBoth() {
        val result = DollManager.selectDollsForCard(
            eventId = 1001L,
            cardColorArgb = 0xFF9C27B0.toInt(),
            cardHeightDp = 40f,
            blankHeightDp = 10f,
            mode = DollDistributionMode.COLOR_AFFINITY
        )
        assertNull("Primary doll must be null for compact card (< 44dp)", result.first)
        assertNull("Secondary doll must be null for compact card (< 44dp)", result.second)
    }

    @Test
    fun normalCard_returnsPrimaryDollAndNullSecondary() {
        val result = DollManager.selectDollsForCard(
            eventId = 1002L,
            cardColorArgb = 0xFF4CAF50.toInt(),
            cardHeightDp = 70f,
            blankHeightDp = 30f,
            mode = DollDistributionMode.COLOR_AFFINITY
        )
        assertNotNull("Primary doll should be present for normal card", result.first)
        assertTrue("Primary doll path should not be empty", result.first!!.isNotEmpty())
        assertNull("Secondary doll should be null for normal card (< 90dp or blank < 50dp)", result.second)
    }

    @Test
    fun largeCard_withEnoughBlankHeight_returnsBothPrimaryAndSecondaryDolls() {
        val result = DollManager.selectDollsForCard(
            eventId = 1003L,
            cardColorArgb = 0xFFFFEB3B.toInt(),
            cardHeightDp = 120f,
            blankHeightDp = 70f,
            mode = DollDistributionMode.COLOR_AFFINITY
        )
        val primary = result.first
        val secondary = result.second

        assertNotNull("Primary doll should be present for large card", primary)
        assertNotNull("Secondary doll should be present for large card with >= 50dp blank height", secondary)
        assertNotEquals("Primary and secondary dolls must not be the exact same asset", primary, secondary)

        // Verify primary and secondary belong to different characters
        val primaryChar = DollManager.getCharacterForAsset(primary!!)
        val secondaryChar = DollManager.getCharacterForAsset(secondary!!)
        assertNotNull(primaryChar)
        assertNotNull(secondaryChar)
        assertNotEquals("Primary and secondary dolls must belong to different characters", primaryChar, secondaryChar)
    }

    @Test
    fun largeCard_withInsufficientBlankHeight_returnsOnlyPrimaryDoll() {
        val result = DollManager.selectDollsForCard(
            eventId = 1004L,
            cardColorArgb = 0xFFFFEB3B.toInt(),
            cardHeightDp = 120f,
            blankHeightDp = 40f, // < 50dp
            mode = DollDistributionMode.COLOR_AFFINITY
        )
        assertNotNull(result.first)
        assertNull("Secondary doll must be null if blankHeightDp < 50dp", result.second)
    }

    @Test
    fun deterministicSelection_sameEventIdProducesIdenticalResult() {
        val eventId = 8888L
        val color = 0xFF00BCD4.toInt()

        val firstRun = DollManager.selectDollsForCard(eventId, color, 100f, 60f)
        val secondRun = DollManager.selectDollsForCard(eventId, color, 100f, 60f)
        val thirdRun = DollManager.selectDollsForCard(eventId, color, 100f, 60f)

        assertEquals("First and second run must produce identical result", firstRun, secondRun)
        assertEquals("First and third run must produce identical result", firstRun, thirdRun)
    }

    @Test
    fun colorAffinity_increasesMatchingCharacterProbability() {
        // Purple color (Hue ~ 270-290°): Preferred STELLALOU
        val purpleArgb = 0xFF9C27B0.toInt() // Purple
        val preferredChar = DuffyCharacter.STELLALOU

        var affinityMatches = 0
        var uniformMatches = 0
        val sampleSize = 2000

        for (i in 0 until sampleSize) {
            val eventId = i.toLong()

            val affinityDoll = DollManager.selectDollsForCard(
                eventId = eventId,
                cardColorArgb = purpleArgb,
                cardHeightDp = 70f,
                blankHeightDp = 30f,
                mode = DollDistributionMode.COLOR_AFFINITY
            ).first!!

            if (DollManager.getCharacterForAsset(affinityDoll) == preferredChar) {
                affinityMatches++
            }

            val uniformDoll = DollManager.selectDollsForCard(
                eventId = eventId,
                cardColorArgb = purpleArgb,
                cardHeightDp = 70f,
                blankHeightDp = 30f,
                mode = DollDistributionMode.UNIFORM_RANDOM
            ).first!!

            if (DollManager.getCharacterForAsset(uniformDoll) == preferredChar) {
                uniformMatches++
            }
        }

        val affinityRatio = affinityMatches.toDouble() / sampleSize
        val uniformRatio = uniformMatches.toDouble() / sampleSize

        // In Uniform mode, 1/7 ~= 14.3%
        // In Affinity mode, weight is 5 / (5 + 6*1) = 5/11 ~= 45.45%
        assertTrue("Affinity ratio ($affinityRatio) must be significantly higher than uniform ratio ($uniformRatio)",
            affinityRatio > uniformRatio * 2.0)
        assertTrue("Affinity ratio ($affinityRatio) should be around 45% (> 35%)", affinityRatio > 0.35)
        assertTrue("Uniform ratio ($uniformRatio) should be around 14% (< 25%)", uniformRatio < 0.25)
    }

    @Test
    fun colorAffinity_colorRangeMappings() {
        // Purple (Hue in 240°..300°) -> STELLALOU
        val purple = DollManager.getPreferredCharacters(0xFF9C27B0.toInt())
        assertEquals(listOf(DuffyCharacter.STELLALOU), purple)

        // Yellow (Hue in 35°..65°) -> COOKIEANN
        val yellow = DollManager.getPreferredCharacters(0xFFFFEB3B.toInt())
        assertEquals(listOf(DuffyCharacter.COOKIEANN), yellow)

        // Green (Hue in 80°..165°) -> OLUMEL
        val green = DollManager.getPreferredCharacters(0xFF4CAF50.toInt())
        assertEquals(listOf(DuffyCharacter.OLUMEL), green)

        // Mint/Cyan (Hue in 165°..205°) -> GELATONI
        val cyan = DollManager.getPreferredCharacters(0xFF00BCD4.toInt())
        assertEquals(listOf(DuffyCharacter.GELATONI), cyan)

        // Pink (Hue in 300°..360° or 0°..15° with saturation > 0.12f) -> LINABELL & SHELLIEMAY
        val pink = DollManager.getPreferredCharacters(0xFFE91E63.toInt())
        assertEquals(listOf(DuffyCharacter.LINABELL, DuffyCharacter.SHELLIEMAY), pink)

        // Brown / Warm Earth (e.g. Hue in 15°..35°) -> DUFFY
        val brown = DollManager.getPreferredCharacters(0xFF795548.toInt())
        assertEquals(listOf(DuffyCharacter.DUFFY), brown)
    }

    @Test
    fun all60AssetsRegistered_andNonEmpty() {
        val totalAssets = DollManager.allDolls.values.sumOf { it.size }
        assertEquals("Total registered doll assets must be exactly 60", 60, totalAssets)
        assertEquals(7, DollManager.allDolls.keys.size)
        DuffyCharacter.entries.forEach { character ->
            val assets = DollManager.getAssetsForCharacter(character)
            assertTrue("Character ${character.name} must have assets", assets.isNotEmpty())
            assets.forEach { path ->
                assertTrue("Asset path $path must start with ${character.assetDir}", path.startsWith(character.assetDir))
            }
        }
    }
}
