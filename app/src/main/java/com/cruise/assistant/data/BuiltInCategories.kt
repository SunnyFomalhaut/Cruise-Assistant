package com.cruise.assistant.data

import com.cruise.assistant.domain.Category

/** spec §8：预制 9 个类别，颜色有区分度 */
val BUILT_IN_CATEGORIES: List<Category> = listOf(
    Category(name = "餐食", colorArgb = 0xFF34A853.toInt(), iconKey = "meal", isBuiltIn = true, sortOrder = 0),
    Category(name = "演出", colorArgb = 0xFFF5B301.toInt(), iconKey = "show", isBuiltIn = true, sortOrder = 1),
    Category(name = "重要事项", colorArgb = 0xFFE8563F.toInt(), iconKey = "important", isBuiltIn = true, sortOrder = 2),
    Category(name = "琐事", colorArgb = 0xFF4A7DFF.toInt(), iconKey = "chore", isBuiltIn = true, sortOrder = 3),
    Category(name = "娱乐项目", colorArgb = 0xFF9B5DE5.toInt(), iconKey = "fun", isBuiltIn = true, sortOrder = 4),
    Category(name = "见面会", colorArgb = 0xFF00A5A5.toInt(), iconKey = "meetup", isBuiltIn = true, sortOrder = 5),
    Category(name = "泳池娱乐", colorArgb = 0xFF45C4B8.toInt(), iconKey = "pool", isBuiltIn = true, sortOrder = 6),
    Category(name = "商店", colorArgb = 0xFFF2994A.toInt(), iconKey = "shop", isBuiltIn = true, sortOrder = 7),
    Category(name = "青少年活动", colorArgb = 0xFFA97BF0.toInt(), iconKey = "youth", isBuiltIn = true, sortOrder = 8),
)

/** 截图3 的 8 色调色板 */
val CATEGORY_PALETTE: List<Int> = listOf(
    0xFFEE6C7A.toInt(), // 珊瑚红
    0xFFF2994A.toInt(), // 橙
    0xFFF2C94C.toInt(), // 黄
    0xFF4ECD8F.toInt(), // 绿
    0xFF45C4B8.toInt(), // 青
    0xFF5B8DEF.toInt(), // 蓝
    0xFFA97BF0.toInt(), // 紫
    0xFF4A5568.toInt(), // 深灰蓝
)

/** 找不到类别时的兜底：琐事 */
const val FALLBACK_CATEGORY_NAME = "琐事"
