package com.cruise.assistant.domain

/**
 * 卡片第二行的地点文案，形如 "Deck 10 船中 花园舞台"；compact=true 时形如 "D6 船中 花园舞台"
 * （主界面省空间，编辑/引导/通知等非主界面保持 Deck）。
 *
 * deck 为 null 表示不设地址（如登船/下船事件），返回 null 让 UI 整行隐藏。
 * zone 与 placeName 都是可选的，缺失时按序省略。
 */
fun formatLocation(
    deck: Int?,
    zone: ShipZone?,
    placeName: String?,
    compact: Boolean = false,
): String? {
    if (deck == null) return null
    return buildString {
        append(if (compact) "D" else "Deck ").append(deck)
        zone?.let { append(" ").append(it.label) }
        placeName?.trim()?.takeIf { it.isNotEmpty() }?.let { append(" ").append(it) }
    }
}
