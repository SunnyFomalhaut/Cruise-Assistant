package com.cruise.assistant.data.prefs

import com.cruise.assistant.domain.DollDistributionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun defaultSettings_hasCardDollsEnabledAndColorAffinityMode() {
        val settings = AppSettings()
        assertTrue("Card dolls should be enabled by default", settings.cardDollsEnabled)
        assertEquals(
            "Default doll distribution should be COLOR_AFFINITY",
            DollDistributionMode.COLOR_AFFINITY,
            settings.cardDollDistribution,
        )
    }
}
