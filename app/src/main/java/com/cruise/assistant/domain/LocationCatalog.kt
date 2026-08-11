package com.cruise.assistant.domain

/** 餐饮地点的四个分类 */
enum class DiningGroup(val label: String) {
    MAIN("正餐餐厅"),
    QUICK("快餐厅/小吃吧"),
    PAID("收费餐厅"),
    BAR("酒吧"),
}

/** 预置地点条目（绑定迪士尼探险号） */
data class LocationCatalogEntry(
    val deck: Int,
    val zone: ShipZone,
    /** 中文名（含英文），如 "华特迪士尼剧院 Walt Disney Theatre" */
    val placeName: String,
    /** 餐饮条目分组；舞台条目为 null */
    val diningGroup: DiningGroup? = null,
)

/** 按类别 iconKey 索引的预置地点（show = 舞台 5 个；meal = 餐饮 10 个、四组） */
val BUILT_IN_LOCATION_CATALOG: Map<String, List<LocationCatalogEntry>> = mapOf(
    "show" to listOf(
        LocationCatalogEntry(6, ShipZone.BOW, "华特迪士尼剧院 Walt Disney Theatre"),
        LocationCatalogEntry(10, ShipZone.STERN, "寻路者湾 Wayfinder Bay"),
        LocationCatalogEntry(10, ShipZone.STERN, "花园舞台 Garden Stage"),
        LocationCatalogEntry(17, ShipZone.MID, "露天泳池"),
        LocationCatalogEntry(10, ShipZone.STERN, "泳池舞台"),
    ),
    "meal" to listOf(
        LocationCatalogEntry(6, ShipZone.MID, "魔法盛夏餐厅 Enchanted Summer Restaurant", DiningGroup.MAIN),
        LocationCatalogEntry(6, ShipZone.STERN, "航海家俱乐部 Navigator's Club Restaurant", DiningGroup.MAIN),
        LocationCatalogEntry(9, ShipZone.STERN, "动画师餐厅 Animator's Table Restaurant", DiningGroup.MAIN),
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
fun locationPresetOptions(iconKey: String): List<LocationPresetOption> {
    val entries = BUILT_IN_LOCATION_CATALOG[iconKey] ?: return emptyList()
    if (iconKey != "meal") {
        return entries.map { LocationPresetOption.Entry(it) } + LocationPresetOption.Custom
    }
    return buildList {
        DiningGroup.entries.forEach { group ->
            val groupEntries = entries.filter { it.diningGroup == group }
            if (groupEntries.isNotEmpty()) {
                add(LocationPresetOption.Group(group.label))
                groupEntries.forEach { add(LocationPresetOption.Entry(it)) }
            }
        }
        add(LocationPresetOption.Custom)
    }
}
