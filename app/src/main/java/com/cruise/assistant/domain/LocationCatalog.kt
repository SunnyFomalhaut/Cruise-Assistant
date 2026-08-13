package com.cruise.assistant.domain

import java.time.LocalTime

/** 餐饮地点的四个分类 */
enum class DiningGroup(val label: String) {
    MAIN("正餐餐厅"),
    QUICK("快餐厅/小吃吧"),
    PAID("收费餐厅"),
    BAR("酒吧"),
}

/** 营业时段：end < start 表示跨午夜（次日）；meal 为餐次标签，设施为 null */
data class OperatingSlot(
    val start: LocalTime,
    val end: LocalTime,
    val meal: MealPeriod? = null,
    /** 夜宵打标（导入时按规则自动判定） */
    val lateNight: Boolean = false,
)

data class OperatingHours(val slots: List<OperatingSlot> = emptyList()) {
    /** 是否配置过营业时间（空列表 = 未配置，不参与筛选/校验） */
    val isConfigured: Boolean get() = slots.isNotEmpty()
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
    val deck: Int?,          // null = 无甲板（如 Room Service "Dial 0"）
    val zone: ShipZone?,     // null = 无区域
    /** 中文名（含英文），如 "华特迪士尼剧院 Walt Disney Theatre" */
    val placeName: String,
    /** 餐饮条目分组；舞台条目为 null */
    val diningGroup: DiningGroup? = null,
    /** 数据表类别（meal/pool/shop/youth/chore） */
    val category: String = "meal",
    /** 营业时间；null 或未配置 = 不参与筛选/校验（宽松处理） */
    val hours: OperatingHours? = null,
)

/** 运行时地点目录：CruiseApp 启动时从 assets 加载；未加载时返回空目录 */
object LocationCatalog {
    @Volatile
    private var catalog: Map<String, List<LocationCatalogEntry>> = emptyMap()

    fun venuesByCategory(): Map<String, List<LocationCatalogEntry>> = catalog

    fun setCatalog(venues: List<LocationCatalogEntry>) {
        catalog = venues.groupBy { it.category }
    }
}

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
    val entries = LocationCatalog.venuesByCategory()[iconKey] ?: return emptyList()
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

/** 某时刻是否在任一营业时段内；跨午夜时段（end < start）：time >= start || time < end；未配置恒营业 */
fun LocationCatalogEntry.isOpenAt(time: LocalTime): Boolean {
    val h = hours ?: return true
    if (!h.isConfigured) return true
    return h.slots.any { slot ->
        if (slot.end > slot.start) {
            !time.isBefore(slot.start) && time.isBefore(slot.end)
        } else {
            !time.isBefore(slot.start) || time.isBefore(slot.end)
        }
    }
}

/** 餐次是否营业：按时段标签匹配；LATE_NIGHT 看任一 lateNight；未配置恒营业 */
fun LocationCatalogEntry.isOpenFor(period: MealPeriod): Boolean {
    val h = hours ?: return true
    if (!h.isConfigured) return true
    return h.slots.any { slot ->
        when (period) {
            MealPeriod.BREAKFAST -> slot.meal == MealPeriod.BREAKFAST
            MealPeriod.LUNCH -> slot.meal == MealPeriod.LUNCH
            MealPeriod.DINNER -> slot.meal == MealPeriod.DINNER
            MealPeriod.LATE_NIGHT -> slot.lateNight
            MealPeriod.SNACK -> slot.meal == MealPeriod.SNACK
            MealPeriod.DRINK -> slot.meal == MealPeriod.DRINK
        }
    }
}
