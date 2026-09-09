package com.cruise.assistant.domain

enum class StateroomValidationStatus {
    INCOMPLETE,
    VALID,
    INVALID,
}

data class StateroomValidationResult(
    val status: StateroomValidationStatus,
    val deck: Int? = null,
    val room: String? = null,
    val fullRoom: String? = null,
    val message: String? = null,
)

/**
 * 将 4 位 (6012 -> 6, "012") 或 5 位 (11216 -> 11, "216") 房号解析为 Deck 与 Room
 */
fun parseStateroomInput(input: String): Pair<Int, String>? {
    val trimmed = input.trim()
    if (!trimmed.all { it.isDigit() }) return null
    val (deck, room) = when (trimmed.length) {
        4 -> {
            val d = trimmed.substring(0, 1).toIntOrNull() ?: return null
            val r = trimmed.substring(1)
            d to r
        }
        5 -> {
            val d = trimmed.substring(0, 2).toIntOrNull() ?: return null
            val r = trimmed.substring(2)
            d to r
        }
        else -> return null
    }
    if (deck !in DeckMapRepository.AVAILABLE_DECKS) return null
    return deck to room
}

/**
 * 实时校验房号是否存在于客房全量目录中
 */
fun validateStateroomInput(
    input: String,
    catalog: List<StateroomEntry> = StateroomCatalog.getAllStaterooms(),
): StateroomValidationResult {
    val trimmed = input.trim()
    if (trimmed.length < 4) {
        return StateroomValidationResult(
            status = StateroomValidationStatus.INCOMPLETE,
            message = "请输入 4 或 5 位客房号",
        )
    }
    val parsed = parseStateroomInput(trimmed)
    if (parsed == null) {
        return StateroomValidationResult(
            status = StateroomValidationStatus.INVALID,
            message = "地图上无此房间，请核对",
        )
    }
    val (deck, room) = parsed
    val full = "$deck$room"
    val exists = catalog.any { it.roomNumber == full && it.deck == deck }
    return if (exists) {
        StateroomValidationResult(
            status = StateroomValidationStatus.VALID,
            deck = deck,
            room = room,
            fullRoom = full,
            message = "Deck $deck 客房有效",
        )
    } else {
        StateroomValidationResult(
            status = StateroomValidationStatus.INVALID,
            deck = deck,
            room = room,
            fullRoom = full,
            message = "地图上无此房间，请核对",
        )
    }
}
