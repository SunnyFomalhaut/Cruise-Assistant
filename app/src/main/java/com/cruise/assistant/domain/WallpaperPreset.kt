package com.cruise.assistant.domain

import com.cruise.assistant.R

enum class WallpaperPreset(
    val key: String,
    val displayNameRes: Int,
    val assetPath: String,
) {
    NONE("none", R.string.wallpaper_none, ""),
    DUFFY("duffy", R.string.theme_duffy_blue, "wallpapers/duffy.png"),
    LINABELL("linabell", R.string.theme_linabell_pink, "wallpapers/linabell.jpg"),
    SHELLIEMAY("shelliemay", R.string.theme_shelliemay_pink, "wallpapers/shelliemay.jpg"),
    STELLALOU("stellalou", R.string.theme_stellalou_purple, "wallpapers/stellalou.jpg"),
    GELATONI("gelatoni", R.string.theme_gelatoni_green, "wallpapers/gelatoni.jpg"),
    COOKIEANN("cookieann", R.string.theme_cookieann_yellow, "wallpapers/cookieann.jpg"),
    OLU("olu", R.string.theme_olu_green, "wallpapers/olu.jpg"),
    CUSTOM("custom", R.string.wallpaper_custom, "custom_wallpaper.jpg");

    companion object {
        /** 默认推荐的内置壁纸（开启底图但尚未指定时的默认项） */
        val DEFAULT_PRESET = DUFFY

        /** 供用户选择的 7 个内置角色预设列表 */
        val PRESETS = listOf(DUFFY, LINABELL, SHELLIEMAY, STELLALOU, GELATONI, COOKIEANN, OLU)

        fun fromKey(key: String?): WallpaperPreset =
            entries.firstOrNull { it.key == key } ?: NONE
    }
}
