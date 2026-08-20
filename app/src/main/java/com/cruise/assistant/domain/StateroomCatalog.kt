package com.cruise.assistant.domain

import org.json.JSONObject

/**
 * 客房条目模型
 */
data class StateroomEntry(
    val roomNumber: String,
    val deck: Int,
)

/**
 * 全船客房全量目录索引仓储
 */
object StateroomCatalog {
    @Volatile
    private var staterooms: List<StateroomEntry> = emptyList()

    fun getAllStaterooms(): List<StateroomEntry> = staterooms

    fun setStaterooms(rooms: List<StateroomEntry>) {
        staterooms = rooms.sortedWith(compareBy<StateroomEntry> { it.roomNumber.toIntOrNull() ?: 0 })
    }

    /**
     * 根据房号前缀或模糊子串进行搜索，按数值升序返回
     */
    fun search(query: String, limit: Int = 100): List<StateroomEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || !trimmed.all { it.isDigit() }) return emptyList()

        // 优先前缀全匹配（如 112 -> 11200, 11201...）
        val prefixMatches = staterooms.filter { it.roomNumber.startsWith(trimmed) }
        if (prefixMatches.isNotEmpty()) {
            return prefixMatches.take(limit)
        }

        // 兜底包含匹配
        return staterooms.filter { it.roomNumber.contains(trimmed) }.take(limit)
    }

    /**
     * 解析 staterooms.json
     */
    fun parseStateroomsJson(json: String): List<StateroomEntry> = runCatching {
        val arr = JSONObject(json).getJSONArray("staterooms")
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    StateroomEntry(
                        roomNumber = obj.getString("room"),
                        deck = obj.getInt("deck"),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
