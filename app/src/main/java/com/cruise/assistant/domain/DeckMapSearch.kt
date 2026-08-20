package com.cruise.assistant.domain

/**
 * 搜索结果类别
 */
enum class DeckSearchResultType {
    FACILITY,
    STATEROOM,
    RESTROOM,
}

/**
 * 甲板地图搜索条目模型
 */
data class DeckSearchResult(
    val title: String,
    val subtitle: String,
    val deckNumber: Int,
    val highlightQuery: String,
    val type: DeckSearchResultType,
    val categoryKey: String? = null,
)

/**
 * 根据客房号智能解析所在甲板层级
 */
fun parseStateroomDeck(roomStr: String): Int? {
    val trimmed = roomStr.trim()
    if (!trimmed.all { it.isDigit() }) return null
    val parsedDeck = when (trimmed.length) {
        5 -> trimmed.substring(0, 2).toIntOrNull()
        4 -> trimmed.substring(0, 1).toIntOrNull()
        else -> null
    }
    return parsedDeck?.takeIf { it in DeckMapRepository.AVAILABLE_DECKS }
}

/**
 * 全船甲板智能检索：支持设施名称、客房号解析、洗手间快捷检索
 */
fun searchDeckMap(
    query: String,
    currentDeck: Int,
    facilities: List<FacilityItemUiModel> = getAllFacilityItems(),
): List<DeckSearchResult> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        val quickList = mutableListOf<DeckSearchResult>()
        // 当前甲板洗手间快捷项
        quickList.add(
            DeckSearchResult(
                title = "洗手间 Restroom",
                subtitle = "Deck $currentDeck · 当前甲板",
                deckNumber = currentDeck,
                highlightQuery = "Restroom",
                type = DeckSearchResultType.RESTROOM,
            )
        )
        // 常用核心设施推荐
        val popularNames = listOf("华特迪士尼剧院", "宾客服务台", "帕洛意大利餐厅", "动画师味觉餐厅", "魔法盛夏餐厅", "皮克斯市场餐厅", "童话洗衣房")
        popularNames.forEach { name ->
            facilities.firstOrNull { it.placeName.contains(name) }?.let { fac ->
                fac.primaryDeck?.let { deck ->
                    quickList.add(
                        DeckSearchResult(
                            title = fac.placeName,
                            subtitle = fac.formattedLocation,
                            deckNumber = deck,
                            highlightQuery = fac.placeName,
                            type = DeckSearchResultType.FACILITY,
                            categoryKey = fac.categoryKey,
                        )
                    )
                }
            }
        }
        return quickList
    }

    val results = mutableListOf<DeckSearchResult>()

    // 1. 房间号解析匹配（如 11770, 6012）
    parseStateroomDeck(trimmed)?.let { deck ->
        results.add(
            DeckSearchResult(
                title = "客房 $trimmed",
                subtitle = "Deck $deck · 客房区域",
                deckNumber = deck,
                highlightQuery = trimmed,
                type = DeckSearchResultType.STATEROOM,
            )
        )
    }

    // 2. 洗手间/厕所关键词检索
    val restroomKeywords = listOf("厕所", "洗手间", "卫生间", "restroom", "wc", "toilet", "washroom")
    if (restroomKeywords.any { trimmed.lowercase().contains(it) }) {
        results.add(
            DeckSearchResult(
                title = "洗手间 Restroom",
                subtitle = "Deck $currentDeck · 当前甲板",
                deckNumber = currentDeck,
                highlightQuery = "Restroom",
                type = DeckSearchResultType.RESTROOM,
            )
        )
        listOf(5, 6, 7, 8, 10, 11, 17, 18, 19).filter { it != currentDeck }.forEach { d ->
            results.add(
                DeckSearchResult(
                    title = "洗手间 Restroom",
                    subtitle = "Deck $d · 公共区域",
                    deckNumber = d,
                    highlightQuery = "Restroom",
                    type = DeckSearchResultType.RESTROOM,
                )
            )
        }
    }

    // 3. 船上设施匹配（名称、甲板、类别）
    val lowerQuery = trimmed.lowercase()
    facilities.filter {
        it.placeName.lowercase().contains(lowerQuery) ||
            it.formattedLocation.lowercase().contains(lowerQuery) ||
            it.categoryLabel.lowercase().contains(lowerQuery)
    }.forEach { fac ->
        fac.primaryDeck?.let { deck ->
            results.add(
                DeckSearchResult(
                    title = fac.placeName,
                    subtitle = fac.formattedLocation,
                    deckNumber = deck,
                    highlightQuery = fac.placeName,
                    type = DeckSearchResultType.FACILITY,
                    categoryKey = fac.categoryKey,
                )
            )
        }
    }

    return results
}
