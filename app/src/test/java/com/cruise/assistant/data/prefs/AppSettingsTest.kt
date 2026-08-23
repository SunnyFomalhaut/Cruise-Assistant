package com.cruise.assistant.data.prefs

import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun defaultSettings_hasCardDollsEnabled() {
        val settings = AppSettings()
        assertTrue("Card dolls should be enabled by default", settings.cardDollsEnabled)
    }
}
