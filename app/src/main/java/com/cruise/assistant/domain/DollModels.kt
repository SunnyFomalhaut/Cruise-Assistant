package com.cruise.assistant.domain

enum class DuffyCharacter(
    val key: String,
    val displayName: String,
    val assetDir: String
) {
    DUFFY("duffy", "达菲", "dolls/duffy"),
    SHELLIEMAY("shelliemay", "雪莉玫", "dolls/shelliemay"),
    STELLALOU("stellalou", "星黛露", "dolls/stellalou"),
    LINABELL("linabell", "玲娜贝儿", "dolls/linabell"),
    GELATONI("gelatoni", "杰拉多尼", "dolls/gelatoni"),
    COOKIEANN("cookieann", "可琦安", "dolls/cookieann"),
    OLUMEL("olumel", "奥乐米拉", "dolls/olumel");

    companion object {
        fun fromKey(key: String?): DuffyCharacter? =
            entries.firstOrNull { it.key == key }
    }
}

enum class DollTier(val folderName: String) {
    HEAD("head"),
    HALF_BODY("half_body"),
    FULL_BODY("full_body");

    companion object {
        fun forCardHeight(heightDp: Float): DollTier = when {
            heightDp < 44f -> HEAD
            heightDp < 72f -> HALF_BODY
            else -> FULL_BODY
        }
    }
}

enum class DollDistributionMode {
    COLOR_AFFINITY,
    UNIFORM_RANDOM
}
