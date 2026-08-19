package com.cruise.assistant.domain

import java.time.LocalTime

/**
 * 设施指南 UI 模型
 */
data class FacilityItemUiModel(
    val placeName: String,
    val categoryKey: String,
    val categoryLabel: String,
    val deck: Int?,
    val zone: ShipZone?,
    val formattedLocation: String,
    val slots: List<OperatingSlot>,
    val is24Hours: Boolean,
    val isOpenNow: Boolean,
    val hasOperatingHours: Boolean,
    val diningGroupLabel: String? = null,
)

/** 类别 iconKey 到用户可读标签的映射 */
fun categoryLabelFor(categoryKey: String): String = when (categoryKey) {
    "meal" -> "餐食"
    "show" -> "演出场地"
    "chore" -> "船上服务"
    "pool" -> "泳池娱乐"
    "shop" -> "商店"
    "youth" -> "青少年活动"
    "fun" -> "娱乐项目"
    "meetup" -> "见面会"
    "important" -> "重要事项"
    else -> "其他设施"
}

/**
 * 将 LocationCatalogEntry 转换为 FacilityItemUiModel
 */
fun LocationCatalogEntry.toFacilityItemUiModel(currentTime: LocalTime = LocalTime.now()): FacilityItemUiModel {
    val configuredSlots = hours?.slots.orEmpty()
    val is24H = configuredSlots.any {
        it.start == LocalTime.of(0, 0) && (it.end == LocalTime.of(23, 59) || it.end == LocalTime.of(0, 0))
    }
    val hasHours = hours?.isConfigured == true
    val open = if (is24H) true else isOpenAt(currentTime)

    val locationStr = formatLocation(deck, zone, null, compact = false) ?: if (deck == null) "全船/Dial 0" else "Deck $deck"

    return FacilityItemUiModel(
        placeName = placeName,
        categoryKey = category,
        categoryLabel = categoryLabelFor(category),
        deck = deck,
        zone = zone,
        formattedLocation = locationStr,
        slots = configuredSlots,
        is24Hours = is24H,
        isOpenNow = open,
        hasOperatingHours = hasHours,
        diningGroupLabel = diningGroup?.label,
    )
}

/**
 * 检索所有内置设施列表
 */
fun getAllFacilityItems(currentTime: LocalTime = LocalTime.now()): List<FacilityItemUiModel> {
    return LocationCatalog.venuesByCategory().values.flatten().map {
        it.toFacilityItemUiModel(currentTime)
    }
}

/**
 * 设施多维筛选与搜索匹配
 */
fun filterFacilities(
    facilities: List<FacilityItemUiModel>,
    query: String,
    categoryKey: String,
    onlyOpenNow: Boolean,
): List<FacilityItemUiModel> {
    val trimmedQuery = query.trim().lowercase()

    return facilities.filter { item ->
        // 1. 分类筛选
        val matchesCategory = if (categoryKey == "all" || categoryKey.isEmpty()) {
            true
        } else {
            item.categoryKey == categoryKey
        }

        // 2. 营业中筛选
        val matchesOpen = if (!onlyOpenNow) {
            true
        } else {
            item.isOpenNow
        }

        // 3. 搜索匹配（名称、甲板、区域）
        val matchesSearch = if (trimmedQuery.isEmpty()) {
            true
        } else {
            val nameMatch = item.placeName.lowercase().contains(trimmedQuery)
            val locationMatch = item.formattedLocation.lowercase().contains(trimmedQuery)
            val deckMatch = item.deck?.let { "deck $it".contains(trimmedQuery) || "d$it".contains(trimmedQuery) || it.toString() == trimmedQuery } ?: false
            val zoneMatch = item.zone?.label?.contains(trimmedQuery) ?: false
            val diningMatch = item.diningGroupLabel?.lowercase()?.contains(trimmedQuery) ?: false

            nameMatch || locationMatch || deckMatch || zoneMatch || diningMatch
        }

        matchesCategory && matchesOpen && matchesSearch
    }
}
