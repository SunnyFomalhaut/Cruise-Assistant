package com.cruise.assistant.ui.theme

import androidx.compose.ui.graphics.Color
import com.cruise.assistant.domain.AppColorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeEngineTest {

    @Test
    fun testJudyBluePaletteTokens() {
        val palette = paletteFor(AppColorTheme.JUDY_BLUE)
        assertEquals(Color(0xFF0284C7), palette.primary)
        assertEquals(Color(0xFF0369A1), palette.secondary)
        assertEquals(Color(0xFFE0F2FE), palette.primaryContainer)
        assertEquals(Color(0xFF0369A1), palette.onPrimaryContainer)
        assertEquals(Color(0xFF38BDF8), palette.primaryLight)
        assertEquals(Color(0xFFF8FAFC), palette.surfaceContainer)
    }

    @Test
    fun testDuffyBluePaletteTokens() {
        val palette = paletteFor(AppColorTheme.DUFFY_BLUE)
        assertEquals(Color(0xFF2563EB), palette.primary)
        assertEquals(Color(0xFF1E3A8A), palette.secondary)
        assertEquals(Color(0xFFEFF6FF), palette.primaryContainer)
        assertEquals(Color(0xFF1E3A8A), palette.onPrimaryContainer)
        assertEquals(Color(0xFFB45309), palette.accent) // Duffy Bear warm brown
    }

    @Test
    fun testLinaBellPinkPaletteTokens() {
        val palette = paletteFor(AppColorTheme.LINABELL_PINK)
        assertEquals(Color(0xFFE5879B), palette.primary)
        assertEquals(Color(0xFF8C3349), palette.secondary)
        assertEquals(Color(0xFFFFF0F3), palette.primaryContainer)
        assertEquals(Color(0xFF8C3349), palette.onPrimaryContainer)
        assertEquals(Color(0xFFA855F7), palette.accent) // Orchid Purple
        assertEquals(Color(0xFFFDFBFB), palette.surfaceContainer)
    }

    @Test
    fun testShellieMayPinkPaletteTokens() {
        val palette = paletteFor(AppColorTheme.SHELLIEMAY_PINK)
        assertEquals(Color(0xFFC06C84), palette.primary)
        assertEquals(Color(0xFF7A2B48), palette.secondary)
        assertEquals(Color(0xFFFDF1F5), palette.primaryContainer)
        assertEquals(Color(0xFF7A2B48), palette.onPrimaryContainer)
        assertEquals(Color(0xFF9CA3AF), palette.accent) // ShellieMay bow gray
    }

    @Test
    fun testLuluPurplePaletteTokens() {
        val palette = paletteFor(AppColorTheme.LULU_PURPLE)
        assertEquals(Color(0xFF8B5CF6), palette.primary)
        assertEquals(Color(0xFF5B21B6), palette.secondary)
        assertEquals(Color(0xFFF5F3FF), palette.primaryContainer)
        assertEquals(Color(0xFF5B21B6), palette.onPrimaryContainer)
        assertEquals(Color(0xFFC4B5FD), palette.primaryLight)
        assertEquals(Color(0xFF14B8A6), palette.accent) // Mint Green
        assertEquals(Color(0xFFFAFAFE), palette.surfaceContainer)
    }

    @Test
    fun testGelatoniGreenPaletteTokens() {
        val palette = paletteFor(AppColorTheme.GELATONI_GREEN)
        assertEquals(Color(0xFF4E8A73), palette.primary)
        assertEquals(Color(0xFF1E4636), palette.secondary)
        assertEquals(Color(0xFFEEF7F2), palette.primaryContainer)
        assertEquals(Color(0xFF0284C7), palette.accent) // Gelatoni beret blue
    }

    @Test
    fun testOluGreenPaletteTokens() {
        val palette = paletteFor(AppColorTheme.OLU_GREEN)
        assertEquals(Color(0xFF65A30D), palette.primary)
        assertEquals(Color(0xFF365314), palette.secondary)
        assertEquals(Color(0xFFF4FBE8), palette.primaryContainer)
        assertEquals(Color(0xFFD97706), palette.accent) // Ukulele amber
    }

    @Test
    fun testCookieAnnYellowPaletteTokens() {
        val palette = paletteFor(AppColorTheme.COOKIEANN_YELLOW)
        assertEquals(Color(0xFFE2A428), palette.primary)
        assertEquals(Color(0xFF7A4308), palette.secondary)
        assertEquals(Color(0xFFFFFDEB), palette.primaryContainer)
        assertEquals(Color(0xFF7A4308), palette.onPrimaryContainer)
        assertEquals(Color(0xFFF472B6), palette.accent) // CookieAnn bow pink
    }

    @Test
    fun testPalettesAreDistinct() {
        val allPalettes = AppColorTheme.entries.map { paletteFor(it) }
        val primaryColors = allPalettes.map { it.primary }.toSet()
        assertEquals("All 8 theme primary colors should be unique", 8, primaryColors.size)
    }
}
