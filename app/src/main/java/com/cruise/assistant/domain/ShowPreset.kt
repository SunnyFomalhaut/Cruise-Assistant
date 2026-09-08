package com.cruise.assistant.domain

import java.time.LocalDate
import java.time.LocalTime

/**
 * 预置演出：绑定在船名和航次上的演出数据（如迪士尼探险号）。
 * 由 VoyageRepository.createVoyage 生成事件，全部放进第二栏（lane=1）。
 */
data class ShowPreset(
    /** 写入事件的 presetKey，如 "show_set_sail" */
    val key: String,
    /** 完整标题（中文 + 英文） */
    val title: String,
    val deck: Int,
    val zone: ShipZone,
    /** 演出地点名，如 "花园舞台 Garden Stage" */
    val placeName: String,
    val durationMinutes: Int,
    /** 相对登船日的天数（第1天 = 1） */
    val dayOffsets: List<Int>,
    /** 非空 = 从 dayOffsets 首日起每天都有，直到 min(该值, 航次天数-1)（不含下船日） */
    val dailyRangeTo: Int? = null,
    /** 一天内的所有场次 */
    val startTimes: List<LocalTime>,
    /** true = 与晚餐时间绑定，只取与晚餐不冲突的那一场 */
    val dinnerLinked: Boolean = false,
    /** 默认候场时间（分钟） */
    val defaultWaitMinutes: Int = 30,
    val note: String? = null,
)

/**
 * 演出在航次中的全部日期。固定天只取列出的偏移、过滤掉 dayCount 及之后的（下船日无演出）；
 * 范围天截断到 dayCount-1。
 */
fun ShowPreset.resolveDates(boardDate: LocalDate, dayCount: Int): List<LocalDate> {
    val lastAllowed = dayCount - 1
    return if (dailyRangeTo != null) {
        val firstDay = dayOffsets.first()
        val lastDay = minOf(dailyRangeTo, lastAllowed)
        if (firstDay > lastDay) {
            emptyList()
        } else {
            (firstDay..lastDay).map { boardDate.plusDays((it - 1).toLong()) }
        }
    } else {
        // 固定天：只取列出的偏移，过滤掉越界（≥ dayCount 即下船日及之后）的
        dayOffsets.filter { it <= lastAllowed }
            .map { boardDate.plusDays((it - 1).toLong()) }
    }
}

/**
 * 晚餐联动演出选场：17:45 晚餐（早于 19:00）→ 20:30 场；20:15 晚餐 → 18:00 场。
 * 选中的恰好是与晚餐不冲突的那场（17:45–19:00 vs 20:30 场；20:15–21:30 vs 18:00 场）。
 */
fun pickDinnerLinkedTime(dinnerTime: LocalTime): LocalTime =
    if (dinnerTime.isBefore(LocalTime.of(19, 0))) {
        LocalTime.of(20, 30)
    } else {
        LocalTime.of(18, 0)
    }

/** 迪士尼探险号预置演出目录（规格 §1 数据表） */
val DISNEY_ADVENTURE_SHOWS: List<ShowPreset> = listOf(
    ShowPreset(
        key = "show_set_sail",
        title = "扬帆起航 Let's Set Sail",
        deck = 10,
        zone = ShipZone.MID,
        placeName = "花园舞台 Garden Stage",
        durationMinutes = 30,
        dayOffsets = listOf(1),
        startTimes = listOf(
            LocalTime.of(13, 15),
            LocalTime.of(14, 0),
            LocalTime.of(14, 45),
        ),
        defaultWaitMinutes = 15,
    ),
    ShowPreset(
        key = "show_lion_king",
        title = "狮子王烟花 The Lion King: Celebration in the Sky",
        deck = 17,
        zone = ShipZone.MID,
        placeName = "阳光泳池 Sunnyside Pool",
        durationMinutes = 10,
        dayOffsets = listOf(2),
        startTimes = listOf(LocalTime.of(22, 30)),
        defaultWaitMinutes = 45,
        note = "烟花在右舷，可以在这个泳池旁边的空地或者18甲左舷走廊（人比较多）",
    ),
    ShowPreset(
        key = "show_seas_adventure",
        title = "海洋奇遇 Disney Seas The Adventure",
        deck = 6,
        zone = ShipZone.BOW,
        placeName = "华特迪士尼剧院 Walt Disney Theatre",
        durationMinutes = 60,
        dayOffsets = listOf(1),
        startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
        dinnerLinked = true,
        defaultWaitMinutes = 45,
    ),
    ShowPreset(
        key = "show_remember",
        title = "回忆：瓦力与伊芙 Remember",
        deck = 6,
        zone = ShipZone.BOW,
        placeName = "华特迪士尼剧院 Walt Disney Theatre",
        durationMinutes = 60,
        dayOffsets = listOf(2),
        startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
        dinnerLinked = true,
        defaultWaitMinutes = 45,
        note = "主秀之一，各航次日期场次请连船上 Wi-Fi 查阅 Navigator App。剧院演出建议提前 45 分钟候场。",
    ),
    ShowPreset(
        key = "show_moana",
        title = "海洋奇缘 Moana: Call of the Sea",
        deck = 10,
        zone = ShipZone.STERN,
        placeName = "寻路者湾 Wayfinder Bay",
        durationMinutes = 15,
        dayOffsets = listOf(2),
        dailyRangeTo = 4,
        startTimes = listOf(
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            LocalTime.of(10, 30),
        ),
        defaultWaitMinutes = 30,
        note = "露天互动音乐剧，各航次时间以 Navigator App 为准。建议提前 30 分钟候场。",
    ),
    ShowPreset(
        key = "show_avengers",
        title = "复仇者集结 Avengers Assemble!",
        deck = 10,
        zone = ShipZone.MID,
        placeName = "花园舞台 Garden Stage",
        durationMinutes = 40,
        dayOffsets = listOf(2),
        dailyRangeTo = 4,
        startTimes = listOf(
            LocalTime.of(18, 15),
            LocalTime.of(20, 45),
        ),
        defaultWaitMinutes = 15,
        note = "漫威英雄集结舞台秀，各航次时间以 Navigator App 为准。建议提前 15 分钟候场。",
    ),
    ShowPreset(
        key = "show_baymax_expo",
        title = "大白超级健身世博会 Baymax Super Exercise Expo",
        deck = 10,
        zone = ShipZone.MID,
        placeName = "花园舞台 Garden Stage",
        durationMinutes = 30,
        dayOffsets = listOf(2),
        dailyRangeTo = 4,
        startTimes = listOf(LocalTime.of(8, 45)),
        defaultWaitMinutes = 15,
        note = "大白晨间互动健身，非每日固定，请查阅 Navigator App。建议提前 15 分钟候场。",
    ),
    ShowPreset(
        key = "show_duffy_friend_ship",
        title = "达菲与友谊之船 Duffy and The Friend Ship",
        deck = 6,
        zone = ShipZone.BOW,
        placeName = "华特迪士尼剧院 Walt Disney Theatre",
        durationMinutes = 30,
        dayOffsets = listOf(2),
        dailyRangeTo = 4,
        startTimes = listOf(LocalTime.of(9, 45)),
        defaultWaitMinutes = 45,
        note = "达菲与朋友们的主题秀，非每日固定，请查阅 Navigator App。建议提前 45 分钟候场。",
    ),
)

val TIMELINE_AUTO_GENERATED_SHOWS: List<ShowPreset> = DISNEY_ADVENTURE_SHOWS.filter {
    it.key in setOf("show_set_sail", "show_lion_king", "show_seas_adventure")
}

val WISHLIST_SEED_SHOWS: List<ShowPreset> = DISNEY_ADVENTURE_SHOWS.filter {
    it.key in setOf(
        "show_remember",
        "show_moana",
        "show_avengers",
        "show_baymax_expo",
        "show_duffy_friend_ship",
    )
}

/** 弹窗消息中的演出列表摘要：每行 "· {title}（{天数} {场次数}）"，行间 \n 连接 */
fun presetShowsSummary(): String = TIMELINE_AUTO_GENERATED_SHOWS.joinToString("\n") { show ->
    val dayLabel = show.dailyRangeTo?.let { "第${show.dayOffsets.first()}天起每天" }
        ?: "第${show.dayOffsets.joinToString("、")}天"
    val countLabel = if (show.dinnerLinked) "1场（按晚餐时段）" else "${show.startTimes.size}场"
    "· ${show.title}（$dayLabel $countLabel）"
}

/** 演出等待入场的默认延伸分钟数（提前占位兜底） */
const val SHOW_WAIT_MINUTES = 30

/**
 * 根据演出地点、楼层、标题或预置 key 解析默认候场时间（分钟）：
 * - D6 华特迪士尼剧院（或任何迪士尼剧院演出）：45 分钟
 * - 狮子王烟花：45 分钟
 * - 大白剧院：15 分钟
 * - 花园舞台：15 分钟
 * - 寻路者湾：30 分钟
 * - 兜底：30 分钟
 */
fun resolveDefaultWaitMinutes(
    placeName: String?,
    deck: Int? = null,
    title: String? = null,
    presetKey: String? = null,
): Int {
    val text = "${placeName.orEmpty()} ${title.orEmpty()} ${presetKey.orEmpty()}"
    if (text.contains("迪士尼剧院") || text.contains("Disney Theatre") || text.contains("Disney Theater") || (deck == 6 && text.contains("剧院"))) {
        return 45
    }
    if (text.contains("狮子王") || text.contains("Lion King") || text.contains("show_lion_king")) {
        return 45
    }
    if (text.contains("大白剧院") || text.contains("Baymax Cinema") || text.contains("Baymax Cinemas")) {
        return 15
    }
    if (text.contains("花园舞台") || text.contains("Garden Stage")) {
        return 15
    }
    if (text.contains("寻路者湾") || text.contains("Wayfinder Bay")) {
        return 30
    }
    return 30
}

/** 事件的实际候场分钟数：优先使用自定义 waitMinutes，未设置时从地点/演出规则派生 */
val CruiseEvent.effectiveWaitMinutes: Int
    get() = waitMinutes ?: resolveDefaultWaitMinutes(placeName, deck, title, presetKey)

/**
 * 事件在时间轴上的有效区间（分钟对 [start, end)）。
 * show 类别事件向前延伸 effectiveWaitMinutes（等待入场占位），其他事件返回原始区间。
 * 返回值可能为负起点（深夜演出跨午夜），区间比较纯算术、调用方自行处理。
 */
fun showEffectiveRange(event: CruiseEvent, isShow: Boolean): Pair<Int, Int> =
    if (isShow) (event.startMinute - event.effectiveWaitMinutes) to event.endMinute
    else event.startMinute to event.endMinute

/**
 * 构造 rangeOf 闭包：show 类别事件（categoryId ∈ showCategoryIds）按扩展区间参与，
 * 其他事件用原始区间。EventRepository/TimelineLayout/TimelineViewModel 共用。
 */
fun showRangeOf(showCategoryIds: Set<Long>): (CruiseEvent) -> Pair<Int, Int> = { e ->
    showEffectiveRange(e, isShow = e.categoryId in showCategoryIds)
}

/**
 * 判断两个事件是否属于同一个演出的不同场次（同类别、同标题、同甲板、同区域、同地点名）。
 */
fun CruiseEvent.isSameShowAs(other: CruiseEvent): Boolean =
    categoryId == other.categoryId &&
        title == other.title &&
        deck == other.deck &&
        zone == other.zone &&
        placeName == other.placeName

/**
 * 格式化演出开始前的候场时间（默认使用 event.effectiveWaitMinutes）。
 * 例如 20:30 演出（提前 45 分钟）返回 "19:45"，09:30 演出（提前 30 分钟）返回 "09:00"。
 */
fun formatWaitStart(event: CruiseEvent, waitMinutes: Int = event.effectiveWaitMinutes): String {
    val waitMin = ((event.startMinute - waitMinutes) % 1440 + 1440) % 1440
    val hour = waitMin / 60
    val minute = waitMin % 60
    return "%02d:%02d".format(hour, minute)
}

