package com.cruise.assistant.domain

/**
 * 无固定时间项目 / 随心打卡条目模型
 */
data class WishlistItem(
    val id: Long = 0,
    val title: String,
    val categoryId: Long,
    val category: Category? = null,
    val deck: Int? = null,
    val zone: ShipZone? = null,
    val placeName: String? = null,
    val note: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val formattedLocation: String?
        get() = formatLocation(deck, zone, placeName, compact = false)
}
