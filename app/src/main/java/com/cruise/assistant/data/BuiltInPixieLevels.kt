package com.cruise.assistant.data

import com.cruise.assistant.domain.PixieLevel

/** 默认三级：普通（灰）/精致（蓝）/豪华（金）。用户可自由增删改。 */
val BUILT_IN_PIXIE_LEVELS: List<PixieLevel> = listOf(
    PixieLevel(name = "普通", colorArgb = 0xFF9E9E9E.toInt(), iconKey = "gift", rank = 1, sortOrder = 0),
    PixieLevel(name = "精致", colorArgb = 0xFF5B8DEF.toInt(), iconKey = "sparkle", rank = 2, sortOrder = 1),
    PixieLevel(name = "豪华", colorArgb = 0xFFF2C94C.toInt(), iconKey = "diamond", rank = 3, sortOrder = 2),
)
