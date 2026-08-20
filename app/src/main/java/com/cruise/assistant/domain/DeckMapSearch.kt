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
 * 全船甲板智能检索：支持设施名称、客房号前缀联想、洗手间快捷检索
 */
fun searchDeckMap(
    query: String,
    currentDeck: Int,
    facilities: List<FacilityItemUiModel> = getAllFacilityItems(),
    stateroomSearcher: (String) -> List<StateroomEntry> = StateroomCatalog::search,
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
    val lowerQuery = trimmed.lowercase()
    val isNumeric = trimmed.all { it.isDigit() }
    val numericDeck = if (isNumeric) trimmed.toIntOrNull() else null

    // 1. 设施匹配（优先展示在最上方）
    facilities.filter {
        it.placeName.lowercase().contains(lowerQuery) ||
            it.formattedLocation.lowercase().contains(lowerQuery) ||
            it.categoryLabel.lowercase().contains(lowerQuery) ||
            (numericDeck != null && (it.allDecks.contains(numericDeck) || it.deck == numericDeck))
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

    // 2. 客房前缀与数值模糊检索（排在设施之后，严格按数值升序呈现）
    if (isNumeric) {
        val matchedRooms = stateroomSearcher(trimmed)
        if (matchedRooms.isNotEmpty()) {
            matchedRooms.forEach { room ->
                results.add(
                    DeckSearchResult(
                        title = "客房 ${room.roomNumber}",
                        subtitle = "Deck ${room.deck} · 客房区域",
                        deckNumber = room.deck,
                        highlightQuery = room.roomNumber,
                        type = DeckSearchResultType.STATEROOM,
                    )
                )
            }
        } else {
            // 如果静态目录暂未包含该房间，但符合甲板房号规则，允许单独解析
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
        }
    }

    // 3. 洗手间/厕所关键词检索
    val restroomKeywords = listOf("厕所", "洗手间", "卫生间", "restroom", "wc", "toilet", "washroom")
    if (restroomKeywords.any { lowerQuery.contains(it) }) {
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

    return results
}
