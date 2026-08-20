package com.cruise.assistant.domain

import com.cruise.assistant.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperPresetTest {

    @Test
    fun fromKey_matchesAllPresets() {
        assertEquals(WallpaperPreset.NONE, WallpaperPreset.fromKey("none"))
        assertEquals(WallpaperPreset.DUFFY, WallpaperPreset.fromKey("duffy"))
        assertEquals(WallpaperPreset.LINABELL, WallpaperPreset.fromKey("linabell"))
        assertEquals(WallpaperPreset.SHELLIEMAY, WallpaperPreset.fromKey("shelliemay"))
        assertEquals(WallpaperPreset.STELLALOU, WallpaperPreset.fromKey("stellalou"))
        assertEquals(WallpaperPreset.GELATONI, WallpaperPreset.fromKey("gelatoni"))
        assertEquals(WallpaperPreset.COOKIEANN, WallpaperPreset.fromKey("cookieann"))
        assertEquals(WallpaperPreset.OLU, WallpaperPreset.fromKey("olu"))
        assertEquals(WallpaperPreset.CUSTOM, WallpaperPreset.fromKey("custom"))
    }

    @Test
    fun fromKey_fallbackToNone() {
        assertEquals(WallpaperPreset.NONE, WallpaperPreset.fromKey(null))
        assertEquals(WallpaperPreset.NONE, WallpaperPreset.fromKey(""))
        assertEquals(WallpaperPreset.NONE, WallpaperPreset.fromKey("unknown"))
    }

    @Test
    fun presetsList_containsAllSevenCharacters() {
        assertEquals(7, WallpaperPreset.PRESETS.size)
    }
}
