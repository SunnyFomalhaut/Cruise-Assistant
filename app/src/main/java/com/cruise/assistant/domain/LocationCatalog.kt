package com.cruise.assistant.domain

import java.time.LocalTime

/** 餐饮地点的四个分类 */
enum class DiningGroup(val label: String) {
    MAIN("正餐餐厅"),
    QUICK("快餐厅/小吃吧"),
    PAID("收费餐厅"),
    BAR("酒吧"),
}

/** 一个营业时段（半开区间 [start, end)） */
data class OperatingSlot(val start: LocalTime, val end: LocalTime)

/** 餐厅三时段营业时间；某时段为 null 表示该时段不营业 */
data class OperatingHours(
    val breakfast: OperatingSlot? = null,
    val lunch: OperatingSlot? = null,
    val dinner: OperatingSlot? = null,
) {
    /** 是否配置过营业时间（三时段全 null = 未配置，不参与筛选/校验） */
    val isConfigured: Boolean get() = breakfast != null || lunch != null || dinner != null
}

/** 餐次（二级选项） */
enum class MealPeriod(val label: String) {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    LATE_NIGHT("夜宵"),
    SNACK("零食"),
    DRINK("酒水"),
}

/** 预置地点条目（绑定迪士尼探险号） */
data class LocationCatalogEntry(
    val deck: Int,
    val zone: ShipZone,
    /** 中文名（含英文），如 "华特迪士尼剧院 Walt Disney Theatre" */
    val placeName: String,
    /** 餐饮条目分组；舞台条目为 null */
    val diningGroup: DiningGroup? = null,
    /** 营业时间；null 或未配置 = 不参与筛选/校验（宽松处理） */
    val hours: OperatingHours? = null,
)

/** 按类别 iconKey 索引的预置地点（show = 舞台 5 个；meal = 餐饮 10 个、四组） */
val BUILT_IN_LOCATION_CATALOG: Map<String, List<LocationCatalogEntry>> = mapOf(
    "show" to listOf(
        LocationCatalogEntry(6, ShipZone.BOW, "华特迪士尼剧院 Walt Disney Theatre"),
        LocationCatalogEntry(10, ShipZone.STERN, "寻路者湾 Wayfinder Bay"),
        LocationCatalogEntry(10, ShipZone.STERN, "花园舞台 Garden Stage"),
        LocationCatalogEntry(17, ShipZone.MID, "露天泳池"),

    ),
    "meal" to listOf(
        LocationCatalogEntry(6, ShipZone.MID, "魔法盛夏餐厅 Enchanted Summer Restaurant", DiningGroup.MAIN),
        LocationCatalogEntry(6, ShipZone.STERN, "航海家俱乐部 Navigator's Club Restaurant", DiningGroup.MAIN),
        LocationCatalogEntry(9, ShipZone.STERN, "动画师餐厅 Animator's Table Restaurant", DiningGroup.MAIN,
            hours = OperatingHours(
                breakfast = OperatingSlot(LocalTime.of(8, 30), LocalTime.of(10, 0)),
                lunch = OperatingSlot(LocalTime.of(12, 0), LocalTime.of(13, 30)),
                dinner = OperatingSlot(LocalTime.of(18, 0), LocalTime.of(22, 0)),
            )),
        LocationCatalogEntry(6, ShipZone.MID, "咒语酒廊 Spellbound", DiningGroup.BAR),
        LocationCatalogEntry(6, ShipZone.MID, "皇家宫廷酒廊 Royal Court", DiningGroup.BAR),
        LocationCatalogEntry(6, ShipZone.STERN, "海盗酒吧 Buccaneer Bar", DiningGroup.BAR),
        LocationCatalogEntry(10, ShipZone.STERN, "Mike & Sulley's Flavors of Asia 怪兽亚洲风味", DiningGroup.PAID),
        LocationCatalogEntry(10, ShipZone.STERN, "Palo Trattoria 帕洛意大利餐厅", DiningGroup.PAID),
        LocationCatalogEntry(10, ShipZone.BOW, "莫格利餐厅 Mowgli's Eatery", DiningGroup.QUICK),
        LocationCatalogEntry(10, ShipZone.BOW, "塔拉奶奶的厨房 Gramma Talas Kitchen", DiningGroup.QUICK),
    ),
)

/** 地点下拉的选项：分组标题（餐饮四组）/ 预置条目 / 自定义入口 */
sealed interface LocationPresetOption {
    data class Group(val label: String) : LocationPresetOption
    data class Entry(val entry: LocationCatalogEntry) : LocationPresetOption
    data object Custom : LocationPresetOption
}

/** 按类别 iconKey 展开成下拉选项（meal 按四组分组，组标题不可选） */
fun locationPresetOptions(
    iconKey: String,
    mealPeriod: MealPeriod? = null,
): List<LocationPresetOption> {
    val entries = BUILT_IN_LOCATION_CATALOG[iconKey] ?: return emptyList()
    if (iconKey != "meal") {
        return entries.map { LocationPresetOption.Entry(it) } + LocationPresetOption.Custom
    }
    // 餐次筛选：选了餐次时只保留该餐次营业的餐厅（未配置营业时间的餐厅恒显示）
    val filtered = if (mealPeriod == null) entries else entries.filter { it.isOpenFor(mealPeriod) }
    return buildList {
        DiningGroup.entries.forEach { group ->
            val groupEntries = filtered.filter { it.diningGroup == group }
            if (groupEntries.isNotEmpty()) {
                add(LocationPresetOption.Group(group.label))
                groupEntries.forEach { add(LocationPresetOption.Entry(it)) }
            }
        }
        add(LocationPresetOption.Custom)
    }
}

/** 餐厅在某个餐次是否营业；未配置营业时间的餐厅恒视为营业 */
fun LocationCatalogEntry.isOpenFor(period: MealPeriod): Boolean {
    val h = hours ?: return true
    if (!h.isConfigured) return true
    return when (period) {
        MealPeriod.BREAKFAST -> h.breakfast != null
        MealPeriod.LUNCH -> h.lunch != null
        MealPeriod.DINNER -> h.dinner != null
        MealPeriod.LATE_NIGHT -> (h.dinner?.end?.hour ?: 0) >= 22
        MealPeriod.SNACK -> true          // 任一餐次营业即算（isConfigured 已保证至少一个）
        MealPeriod.DRINK -> h.dinner != null
    }
}

/** 某时刻是否在餐厅任一营业时段内（半开区间）；未配置营业时间的餐厅恒视为营业 */
fun LocationCatalogEntry.isOpenAt(time: LocalTime): Boolean {
    val h = hours ?: return true
    if (!h.isConfigured) return true
    return listOfNotNull(h.breakfast, h.lunch, h.dinner)
        .any { !time.isBefore(it.start) && time.isBefore(it.end) }
}
