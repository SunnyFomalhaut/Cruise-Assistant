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

enum class DollDistributionMode {
    COLOR_AFFINITY,
    UNIFORM_RANDOM
}
