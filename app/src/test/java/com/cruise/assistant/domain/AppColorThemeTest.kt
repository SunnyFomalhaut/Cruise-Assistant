package com.cruise.assistant.domain

import com.cruise.assistant.R
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.ui.theme.JudyBluePalette
import com.cruise.assistant.ui.theme.LinaBellPinkPalette
import com.cruise.assistant.ui.theme.StellaLouPurplePalette
import com.cruise.assistant.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Test

class AppColorThemeTest {

    @Test
    fun fromKey_returnsCorrectTheme_forValidKeys() {
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey("judy_blue"))
        assertEquals(AppColorTheme.LINABELL_PINK, AppColorTheme.fromKey("linabell_pink"))
        assertEquals(AppColorTheme.STELLALOU_PURPLE, AppColorTheme.fromKey("stellalou_purple"))
    }

    @Test
    fun fromKey_returnsJudyBlue_forNullOrUnknownKey() {
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey(null))
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey(""))
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey("invalid_key"))
    }

    @Test
    fun themeProperties_areCorrectlyConfigured() {
        val judy = AppColorTheme.JUDY_BLUE
        assertEquals("judy_blue", judy.key)
        assertEquals(R.string.theme_judy_blue, judy.displayNameRes)
        assertEquals(R.string.theme_judy_blue_desc, judy.descriptionRes)
        assertEquals(0xFF0284C7, judy.primaryArgb)
        assertEquals(0xFF0369A1, judy.secondaryArgb)
        assertEquals(0xFFE0F2FE, judy.containerArgb)

        val linabell = AppColorTheme.LINABELL_PINK
        assertEquals("linabell_pink", linabell.key)
        assertEquals(R.string.theme_linabell_pink, linabell.displayNameRes)
        assertEquals(R.string.theme_linabell_pink_desc, linabell.descriptionRes)
        assertEquals(0xFFE05375, linabell.primaryArgb)
        assertEquals(0xFF9E1C3F, linabell.secondaryArgb)
        assertEquals(0xFFFDF2F8, linabell.containerArgb)

        val stellalou = AppColorTheme.STELLALOU_PURPLE
        assertEquals("stellalou_purple", stellalou.key)
        assertEquals(R.string.theme_stellalou_purple, stellalou.displayNameRes)
        assertEquals(R.string.theme_stellalou_purple_desc, stellalou.descriptionRes)
        assertEquals(0xFF8B5CF6, stellalou.primaryArgb)
        assertEquals(0xFF5B21B6, stellalou.secondaryArgb)
        assertEquals(0xFFF5F3FF, stellalou.containerArgb)
    }

    @Test
    fun appSettings_defaultColorThemeIsJudyBlue() {
        val settings = AppSettings()
        assertEquals(AppColorTheme.JUDY_BLUE, settings.colorTheme)
    }

    @Test
    fun paletteFor_returnsCorrectPalette() {
        assertEquals(JudyBluePalette, paletteFor(AppColorTheme.JUDY_BLUE))
        assertEquals(LinaBellPinkPalette, paletteFor(AppColorTheme.LINABELL_PINK))
        assertEquals(StellaLouPurplePalette, paletteFor(AppColorTheme.STELLALOU_PURPLE))
    }
}
