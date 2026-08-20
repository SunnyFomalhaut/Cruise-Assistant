package com.cruise.assistant.domain

import com.cruise.assistant.R

enum class AppColorTheme(
    val key: String,
    val displayNameRes: Int,
    val descriptionRes: Int,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val containerArgb: Long,
) {
    JUDY_BLUE(
        key = "judy_blue",
        displayNameRes = R.string.theme_judy_blue,
        descriptionRes = R.string.theme_judy_blue_desc,
        primaryArgb = 0xFF0284C7,
        secondaryArgb = 0xFF0369A1,
        containerArgb = 0xFFE0F2FE,
    ),
    DUFFY_BLUE(
        key = "duffy_blue",
        displayNameRes = R.string.theme_duffy_blue,
        descriptionRes = R.string.theme_duffy_blue_desc,
        primaryArgb = 0xFF2563EB,
        secondaryArgb = 0xFF1E3A8A,
        containerArgb = 0xFFEFF6FF,
    ),
    LINABELL_PINK(
        key = "linabell_pink",
        displayNameRes = R.string.theme_linabell_pink,
        descriptionRes = R.string.theme_linabell_pink_desc,
        primaryArgb = 0xFFD47A70,
        secondaryArgb = 0xFF8C3E3E,
        containerArgb = 0xFFFAF0EE,
    ),
    SHELLIEMAY_PINK(
        key = "shelliemay_pink",
        displayNameRes = R.string.theme_shelliemay_pink,
        descriptionRes = R.string.theme_shelliemay_pink_desc,
        primaryArgb = 0xFFC06C84,
        secondaryArgb = 0xFF7A2B48,
        containerArgb = 0xFFFDF1F5,
    ),
    LULU_PURPLE(
        key = "lulu_purple",
        displayNameRes = R.string.theme_stellalou_purple,
        descriptionRes = R.string.theme_stellalou_purple_desc,
        primaryArgb = 0xFF8B5CF6,
        secondaryArgb = 0xFF5B21B6,
        containerArgb = 0xFFF5F3FF,
    ),
    GELATONI_GREEN(
        key = "gelatoni_green",
        displayNameRes = R.string.theme_gelatoni_green,
        descriptionRes = R.string.theme_gelatoni_green_desc,
        primaryArgb = 0xFF4E8A73,
        secondaryArgb = 0xFF1E4636,
        containerArgb = 0xFFEEF7F2,
    ),
    OLU_GREEN(
        key = "olu_green",
        displayNameRes = R.string.theme_olu_green,
        descriptionRes = R.string.theme_olu_green_desc,
        primaryArgb = 0xFF65A30D,
        secondaryArgb = 0xFF365314,
        containerArgb = 0xFFF4FBE8,
    ),
    COOKIEANN_YELLOW(
        key = "cookieann_yellow",
        displayNameRes = R.string.theme_cookieann_yellow,
        descriptionRes = R.string.theme_cookieann_yellow_desc,
        primaryArgb = 0xFFD97706,
        secondaryArgb = 0xFF78350F,
        containerArgb = 0xFFFFFBEB,
    );

    companion object {
        val STELLALOU_PURPLE = LULU_PURPLE

        fun fromKey(key: String?): AppColorTheme = when (key) {
            "stellalou_purple" -> LULU_PURPLE
            else -> entries.firstOrNull { it.key == key } ?: JUDY_BLUE
        }
    }
}
