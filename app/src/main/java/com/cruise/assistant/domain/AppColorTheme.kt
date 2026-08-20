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
    LINABELL_PINK(
        key = "linabell_pink",
        displayNameRes = R.string.theme_linabell_pink,
        descriptionRes = R.string.theme_linabell_pink_desc,
        primaryArgb = 0xFFE05375,
        secondaryArgb = 0xFF9E1C3F,
        containerArgb = 0xFFFDF2F8,
    ),
    STELLALOU_PURPLE(
        key = "stellalou_purple",
        displayNameRes = R.string.theme_stellalou_purple,
        descriptionRes = R.string.theme_stellalou_purple_desc,
        primaryArgb = 0xFF8B5CF6,
        secondaryArgb = 0xFF5B21B6,
        containerArgb = 0xFFF5F3FF,
    );

    companion object {
        fun fromKey(key: String?): AppColorTheme =
            entries.firstOrNull { it.key == key } ?: JUDY_BLUE
    }
}
