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
    /** 演出地点名，如 "花园舞台" */
    val placeName: String,
    val durationMinutes: Int,
    /** 相对上船日的天数（第1天 = 1） */
    val dayOffsets: List<Int>,
    /** 非空 = 从 dayOffsets 首日起每天都有，直到 min(该值, 航次天数-1)（不含下船日） */
    val dailyRangeTo: Int? = null,
    /** 一天内的所有场次 */
    val startTimes: List<LocalTime>,
    /** true = 与晚餐时间绑定，只取与晚餐不冲突的那一场 */
    val dinnerLinked: Boolean = false,
)

/** 演出在航次中的全部日期。固定天只取 dayOffsets；范围天截断到 dayCount-1（下船日无演出）。 */
fun ShowPreset.resolveDates(boardDate: LocalDate, dayCount: Int): List<LocalDate> {
    if (dailyRangeTo == null) {
        return dayOffsets.map { boardDate.plusDays((it - 1).toLong()) }
    }
    val firstDay = dayOffsets.first()
    val lastDay = minOf(dailyRangeTo, dayCount - 1)
    return (firstDay..lastDay).map { boardDate.plusDays((it - 1).toLong()) }
}

/**
 * 晚餐联动演出选场：17:45 晚餐（早于 19:00）→ 20:30 场；20:00 晚餐 → 18:00 场。
 * 选中的恰好是与晚餐不冲突的那场（17:45–19:00 vs 20:30 场；20:00–21:15 vs 18:00 场）。
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
        placeName = "花园舞台",
        durationMinutes = 30,
        dayOffsets = listOf(1),
        startTimes = listOf(
            LocalTime.of(13, 15),
            LocalTime.of(14, 0),
            LocalTime.of(14, 45),
        ),
    ),
    ShowPreset(
        key = "show_lion_king",
        title = "狮子王烟花 The Lion King: Celebration in the Sky",
        deck = 17,
        zone = ShipZone.MID,
        placeName = "露天泳池",
        durationMinutes = 10,
        dayOffsets = listOf(2),
        startTimes = listOf(LocalTime.of(22, 30)),
    ),
    ShowPreset(
        key = "show_seas_adventure",
        title = "海洋奇遇 Disney Seas The Adventure",
        deck = 6,
        zone = ShipZone.BOW,
        placeName = "迪士尼剧院",
        durationMinutes = 60,
        dayOffsets = listOf(1),
        startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
        dinnerLinked = true,
    ),
    ShowPreset(
        key = "show_remember",
        title = "回忆：瓦力与伊芙 Remember",
        deck = 6,
        zone = ShipZone.BOW,
        placeName = "迪士尼剧院",
        durationMinutes = 60,
        dayOffsets = listOf(2),
        startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
        dinnerLinked = true,
    ),
    ShowPreset(
        key = "show_moana",
        title = "海洋奇缘 Moana: Call of the Sea",
        deck = 10,
        zone = ShipZone.STERN,
        placeName = "泳池舞台",
        durationMinutes = 15,
        dayOffsets = listOf(2),
        dailyRangeTo = 4,
        startTimes = listOf(
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            LocalTime.of(10, 30),
        ),
    ),
)
