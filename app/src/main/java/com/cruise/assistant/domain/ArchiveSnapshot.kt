package com.cruise.assistant.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/** 快照格式版本：未来字段演进时据此做向后兼容判断 */
const val ARCHIVE_SNAPSHOT_VERSION = 1

/**
 * 航次存档快照：航次结束时对全部业务表的完整拷贝。
 *
 * 保真度是生命线——存档后用户会"重建航次"开始新旅程，未来船上的餐厅供餐、演出安排都会变，
 * 旧存档必须原样还原当时状态。因此快照冗余存下渲染所需的全部数据（类别颜色/图标、仙尘
 * 等级名快照等），而不是只存 id 让未来配置反查。
 *
 * 有意不入档的全局数据：地点预设（LocationPreset，用户手输地点的使用频次缓存）、
 * 甲板图/船只预设资产（ShipPreset、壁纸等）——它们是"会随未来版本变化的全局配置"，
 * 不是这段旅程的记录；事件/愿望单里固化的 deck/zone/placeName 文本已足够还原当时状态。
 */
data class ArchiveSnapshot(
    val version: Int = ARCHIVE_SNAPSHOT_VERSION,
    val voyage: Voyage,
    /** 渲染事件卡片/愿望单需要（id/name/colorArgb/iconKey），存全量行 */
    val categories: List<Category>,
    /** 事件全字段（title/deck/zone/placeName/lane/recurrenceGroupId/presetKey/waitMinutes/note/...） */
    val events: List<CruiseEvent>,
    /** 每日定稿状态 */
    val dayPlans: List<DayPlan>,
    val pixieLevels: List<PixieLevel>,
    /** levelNameSnapshot/replied/note/timestamp 全量保留 */
    val pixieDust: List<PixieDustRecord>,
    /** isCompleted/createdAt 全量保留 */
    val wishlist: List<WishlistItem>,
)

/**
 * 序列化快照为 JSON 文本（紧凑单行）。
 * 可空字段约定：null 一律省略键（不写 JSON null），解析端按"键不存在"还原为 null——
 * 与 Android/JVM 两种 org.json 实现对 null 值 optString 的行为差异解耦。
 */
fun serializeArchiveSnapshot(snapshot: ArchiveSnapshot): String = JSONObject().apply {
    put("version", snapshot.version)
    put("voyage", snapshot.voyage.toJson())
    put("categories", JSONArray().apply { snapshot.categories.forEach { put(it.toJson()) } })
    put("events", JSONArray().apply { snapshot.events.forEach { put(it.toJson()) } })
    put("dayPlans", JSONArray().apply { snapshot.dayPlans.forEach { put(it.toJson()) } })
    put("pixieLevels", JSONArray().apply { snapshot.pixieLevels.forEach { put(it.toJson()) } })
    put("pixieDust", JSONArray().apply { snapshot.pixieDust.forEach { put(it.toJson()) } })
    put("wishlist", JSONArray().apply { snapshot.wishlist.forEach { put(it.toJson()) } })
}.toString()

/**
 * 解析存档 JSON 为快照；任何损坏数据（非法 JSON、必填字段缺失/类型错）一律返回 null，绝不抛异常。
 * 段落级字段（categories/events 等）缺失按向后兼容处理为空列表；可空字段缺失（optXXX）取默认值。
 */
fun parseArchiveSnapshot(json: String): ArchiveSnapshot? = runCatching {
    val root = JSONObject(json)
    ArchiveSnapshot(
        version = root.optInt("version", ARCHIVE_SNAPSHOT_VERSION),
        voyage = root.getJSONObject("voyage").toVoyage(),
        categories = root.mapList("categories") { it.toCategory() },
        events = root.mapList("events") { it.toCruiseEvent() },
        dayPlans = root.mapList("dayPlans") { it.toDayPlan() },
        pixieLevels = root.mapList("pixieLevels") { it.toPixieLevel() },
        pixieDust = root.mapList("pixieDust") { it.toPixieDustRecord() },
        wishlist = root.mapList("wishlist") { it.toWishlistItem() },
    )
}.getOrNull()

// ---- 写入端：领域对象 → JSON ----

private fun Voyage.toJson() = JSONObject().apply {
    put("id", id)
    // JSONObject.put(String, null) 会直接移除键，正好落入"null = 键不存在"约定
    put("shipName", shipName)
    put("boardDate", boardDate.toString())
    put("boardTime", boardTime.toString())
    put("disembarkDate", disembarkDate.toString())
    put("disembarkTime", disembarkTime.toString())
    put("maxDeck", maxDeck)
    put("minDeck", minDeck)
    put("isConcierge", isConcierge)
    put("legs", JSONArray(legs))
    // 登岸兰卡威的程下标：空集省略键（可空约定——put(String, null) 会移除键，正好落入该约定）
    put("ashoreLegs", if (ashoreLegs.isEmpty()) null else JSONArray(ashoreLegs))
}

private fun Category.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("colorArgb", colorArgb)
    put("iconKey", iconKey)
    put("parentId", parentId)
    put("isBuiltIn", isBuiltIn)
    put("isArchived", isArchived)
    put("sortOrder", sortOrder)
}

private fun CruiseEvent.toJson() = JSONObject().apply {
    put("id", id)
    put("date", date.toString())
    put("startTime", startTime.toString())
    put("durationMinutes", durationMinutes)
    put("categoryId", categoryId)
    put("title", title)
    put("deck", deck)
    put("zone", zone?.name)
    put("placeName", placeName)
    put("lane", lane)
    put("recurrenceGroupId", recurrenceGroupId)
    put("reminderMinutesBefore", reminderMinutesBefore)
    put("isSystemGenerated", isSystemGenerated)
    put("isActive", isActive)
    put("presetKey", presetKey)
    put("waitMinutes", waitMinutes)
    put("note", note)
}

private fun DayPlan.toJson() = JSONObject().apply {
    put("date", date.toString())
    put("finalized", finalized)
    put("finalizedAt", finalizedAt)
}

private fun PixieLevel.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("colorArgb", colorArgb)
    put("iconKey", iconKey)
    put("rank", rank)
    put("sortOrder", sortOrder)
}

private fun PixieDustRecord.toJson() = JSONObject().apply {
    put("id", id)
    put("direction", direction.name)
    put("deck", deck)
    put("room", room)
    put("levelId", levelId)
    put("levelNameSnapshot", levelNameSnapshot)
    put("replied", replied)
    put("note", note)
    put("timestamp", timestamp)
}

private fun WishlistItem.toJson() = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("categoryId", categoryId)
    put("deck", deck)
    put("zone", zone?.name)
    put("placeName", placeName)
    put("note", note)
    put("isCompleted", isCompleted)
    put("createdAt", createdAt)
    // category 字段有意不存：它是仓库查询时按 categoryId 联表得出的展示对象，
    // 快照里已有完整 categories 列表，按 id 关联即可还原
}

// ---- 解析端：JSON → 领域对象 ----

/** 读取段落列表；段落缺失（旧版本/损坏）按向后兼容处理为空列表 */
private inline fun <T> JSONObject.mapList(key: String, transform: (JSONObject) -> T): List<T> =
    optJSONArray(key)?.let { arr -> List(arr.length()) { transform(arr.getJSONObject(it)) } }
        ?: emptyList()

/** 可空字符串：键不存在返回 null；Android 框架 org.json 对 JSON null 的 optString 返回
 *  "null" 字符串（JVM 库返回 ""），与 LocationCatalogLoader 同款防御须显式排除 */
private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key)) optString(key).takeIf { it != "null" } else null

/** 可空整数：键不存在或为 JSON null 返回 null */
private fun JSONObject.optNullableInt(key: String): Int? =
    if (!isNull(key)) optInt(key) else null

/** 可空长整数：键不存在或为 JSON null 返回 null */
private fun JSONObject.optNullableLong(key: String): Long? =
    if (!isNull(key)) optLong(key) else null

/** 可空枚举：未知枚举值（旧档/改名）兜底 null，不让整份存档解析失败 */
private fun JSONObject.optZone(key: String): ShipZone? =
    optStringOrNull(key)?.let { runCatching { ShipZone.valueOf(it) }.getOrNull() }

private fun JSONObject.toVoyage() = Voyage(
    id = optInt("id", 1),
    shipName = optStringOrNull("shipName"),
    boardDate = LocalDate.parse(getString("boardDate")),
    boardTime = LocalTime.parse(getString("boardTime")),
    disembarkDate = LocalDate.parse(getString("disembarkDate")),
    disembarkTime = LocalTime.parse(getString("disembarkTime")),
    maxDeck = optInt("maxDeck", 20),
    minDeck = optInt("minDeck", 1),
    isConcierge = optBoolean("isConcierge", false),
    legs = optJSONArray("legs")?.let { arr ->
        List(arr.length()) { arr.getInt(it) }
    } ?: emptyList(),
    // 登岸兰卡威的程下标：键缺失（旧档）默认空集，向后兼容
    ashoreLegs = optJSONArray("ashoreLegs")?.let { arr ->
        List(arr.length()) { arr.getInt(it) }.toSet()
    } ?: emptySet(),
)

private fun JSONObject.toCategory() = Category(
    id = getLong("id"),
    name = getString("name"),
    colorArgb = getInt("colorArgb"),
    iconKey = getString("iconKey"),
    parentId = optNullableLong("parentId"),
    isBuiltIn = optBoolean("isBuiltIn", false),
    isArchived = optBoolean("isArchived", false),
    sortOrder = optInt("sortOrder", 0),
)

private fun JSONObject.toCruiseEvent() = CruiseEvent(
    id = getLong("id"),
    date = LocalDate.parse(getString("date")),
    startTime = LocalTime.parse(getString("startTime")),
    durationMinutes = getInt("durationMinutes"),
    categoryId = getLong("categoryId"),
    title = getString("title"),
    deck = optNullableInt("deck"),
    zone = optZone("zone"),
    placeName = optStringOrNull("placeName"),
    lane = optInt("lane", 0),
    recurrenceGroupId = optStringOrNull("recurrenceGroupId"),
    reminderMinutesBefore = optNullableInt("reminderMinutesBefore"),
    isSystemGenerated = optBoolean("isSystemGenerated", false),
    isActive = optBoolean("isActive", true),
    presetKey = optStringOrNull("presetKey"),
    waitMinutes = optNullableInt("waitMinutes"),
    note = optStringOrNull("note"),
)

private fun JSONObject.toDayPlan() = DayPlan(
    date = LocalDate.parse(getString("date")),
    finalized = optBoolean("finalized", false),
    finalizedAt = optNullableLong("finalizedAt"),
)

private fun JSONObject.toPixieLevel() = PixieLevel(
    id = getLong("id"),
    name = getString("name"),
    colorArgb = getInt("colorArgb"),
    iconKey = getString("iconKey"),
    rank = optInt("rank", 0),
    sortOrder = optInt("sortOrder", 0),
)

private fun JSONObject.toPixieDustRecord() = PixieDustRecord(
    id = getLong("id"),
    // 未知 direction 值（旧档/枚举改名）兜底 GIVEN，与 Mappers 同款防御
    direction = runCatching { PixieDustDirection.valueOf(getString("direction")) }
        .getOrNull() ?: PixieDustDirection.GIVEN,
    deck = getInt("deck"),
    room = getString("room"),
    levelId = getLong("levelId"),
    levelNameSnapshot = getString("levelNameSnapshot"),
    replied = optBoolean("replied", false),
    note = optString("note", ""),
    timestamp = getLong("timestamp"),
)

private fun JSONObject.toWishlistItem() = WishlistItem(
    id = getLong("id"),
    title = getString("title"),
    categoryId = getLong("categoryId"),
    deck = optNullableInt("deck"),
    zone = optZone("zone"),
    placeName = optStringOrNull("placeName"),
    note = optStringOrNull("note"),
    isCompleted = optBoolean("isCompleted", false),
    createdAt = getLong("createdAt"),
)
