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
    fun testLinaBellPinkPaletteTokens() {
        val palette = paletteFor(AppColorTheme.LINABELL_PINK)
        assertEquals(Color(0xFFE05375), palette.primary)
        assertEquals(Color(0xFF9E1C3F), palette.secondary)
        assertEquals(Color(0xFFFDF2F8), palette.primaryContainer)
        assertEquals(Color(0xFF9E1C3F), palette.onPrimaryContainer)
        assertEquals(Color(0xFFF472B6), palette.primaryLight)
        assertEquals(Color(0xFFA855F7), palette.accent) // Orchid Purple
        assertEquals(Color(0xFFFDFBFB), palette.surfaceContainer)
    }

    @Test
    fun testStellaLouPurplePaletteTokens() {
        val palette = paletteFor(AppColorTheme.STELLALOU_PURPLE)
        assertEquals(Color(0xFF8B5CF6), palette.primary)
        assertEquals(Color(0xFF5B21B6), palette.secondary)
        assertEquals(Color(0xFFF5F3FF), palette.primaryContainer)
        assertEquals(Color(0xFF5B21B6), palette.onPrimaryContainer)
        assertEquals(Color(0xFFC4B5FD), palette.primaryLight)
        assertEquals(Color(0xFF14B8A6), palette.accent) // Mint Green
        assertEquals(Color(0xFFFAFAFE), palette.surfaceContainer)
    }

    @Test
    fun testPalettesAreDistinct() {
        val blue = paletteFor(AppColorTheme.JUDY_BLUE)
        val pink = paletteFor(AppColorTheme.LINABELL_PINK)
        val purple = paletteFor(AppColorTheme.STELLALOU_PURPLE)

        assertNotEquals(blue.primary, pink.primary)
        assertNotEquals(pink.primary, purple.primary)
        assertNotEquals(blue.primaryContainer, pink.primaryContainer)
        assertNotEquals(pink.primaryContainer, purple.primaryContainer)
    }
}
