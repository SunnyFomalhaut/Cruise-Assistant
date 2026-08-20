package com.cruise.assistant.domain

import com.cruise.assistant.R
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.ui.theme.CookieAnnYellowPalette
import com.cruise.assistant.ui.theme.DuffyBluePalette
import com.cruise.assistant.ui.theme.GelatoniGreenPalette
import com.cruise.assistant.ui.theme.JudyBluePalette
import com.cruise.assistant.ui.theme.LinaBellPinkPalette
import com.cruise.assistant.ui.theme.LuluPurplePalette
import com.cruise.assistant.ui.theme.OluGreenPalette
import com.cruise.assistant.ui.theme.ShellieMayPinkPalette
import com.cruise.assistant.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Test

class AppColorThemeTest {

    @Test
    fun fromKey_returnsCorrectTheme_forValidKeys() {
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey("judy_blue"))
        assertEquals(AppColorTheme.DUFFY_BLUE, AppColorTheme.fromKey("duffy_blue"))
        assertEquals(AppColorTheme.LINABELL_PINK, AppColorTheme.fromKey("linabell_pink"))
        assertEquals(AppColorTheme.SHELLIEMAY_PINK, AppColorTheme.fromKey("shelliemay_pink"))
        assertEquals(AppColorTheme.LULU_PURPLE, AppColorTheme.fromKey("lulu_purple"))
        assertEquals(AppColorTheme.LULU_PURPLE, AppColorTheme.fromKey("stellalou_purple")) // Backward compatibility
        assertEquals(AppColorTheme.GELATONI_GREEN, AppColorTheme.fromKey("gelatoni_green"))
        assertEquals(AppColorTheme.OLU_GREEN, AppColorTheme.fromKey("olu_green"))
        assertEquals(AppColorTheme.COOKIEANN_YELLOW, AppColorTheme.fromKey("cookieann_yellow"))
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
        assertEquals(0xFF0284C7, judy.primaryArgb)

        val duffy = AppColorTheme.DUFFY_BLUE
        assertEquals("duffy_blue", duffy.key)
        assertEquals(R.string.theme_duffy_blue, duffy.displayNameRes)
        assertEquals(0xFF2563EB, duffy.primaryArgb)

        val linabell = AppColorTheme.LINABELL_PINK
        assertEquals("linabell_pink", linabell.key)
        assertEquals(R.string.theme_linabell_pink, linabell.displayNameRes)
        assertEquals(0xFFD47A70, linabell.primaryArgb)

        val shelliemay = AppColorTheme.SHELLIEMAY_PINK
        assertEquals("shelliemay_pink", shelliemay.key)
        assertEquals(R.string.theme_shelliemay_pink, shelliemay.displayNameRes)
        assertEquals(0xFFC06C84, shelliemay.primaryArgb)

        val lulu = AppColorTheme.LULU_PURPLE
        assertEquals("lulu_purple", lulu.key)
        assertEquals(R.string.theme_stellalou_purple, lulu.displayNameRes)
        assertEquals(0xFF8B5CF6, lulu.primaryArgb)

        val gelatoni = AppColorTheme.GELATONI_GREEN
        assertEquals("gelatoni_green", gelatoni.key)
        assertEquals(R.string.theme_gelatoni_green, gelatoni.displayNameRes)
        assertEquals(0xFF4E8A73, gelatoni.primaryArgb)

        val olu = AppColorTheme.OLU_GREEN
        assertEquals("olu_green", olu.key)
        assertEquals(R.string.theme_olu_green, olu.displayNameRes)
        assertEquals(0xFF65A30D, olu.primaryArgb)

        val cookieann = AppColorTheme.COOKIEANN_YELLOW
        assertEquals("cookieann_yellow", cookieann.key)
        assertEquals(R.string.theme_cookieann_yellow, cookieann.displayNameRes)
        assertEquals(0xFFD97706, cookieann.primaryArgb)
    }

    @Test
    fun appSettings_defaultColorThemeIsJudyBlue() {
        val settings = AppSettings()
        assertEquals(AppColorTheme.JUDY_BLUE, settings.colorTheme)
    }

    @Test
    fun paletteFor_returnsCorrectPalette() {
        assertEquals(JudyBluePalette, paletteFor(AppColorTheme.JUDY_BLUE))
        assertEquals(DuffyBluePalette, paletteFor(AppColorTheme.DUFFY_BLUE))
        assertEquals(LinaBellPinkPalette, paletteFor(AppColorTheme.LINABELL_PINK))
        assertEquals(ShellieMayPinkPalette, paletteFor(AppColorTheme.SHELLIEMAY_PINK))
        assertEquals(LuluPurplePalette, paletteFor(AppColorTheme.LULU_PURPLE))
        assertEquals(GelatoniGreenPalette, paletteFor(AppColorTheme.GELATONI_GREEN))
        assertEquals(OluGreenPalette, paletteFor(AppColorTheme.OLU_GREEN))
        assertEquals(CookieAnnYellowPalette, paletteFor(AppColorTheme.COOKIEANN_YELLOW))
    }
}
