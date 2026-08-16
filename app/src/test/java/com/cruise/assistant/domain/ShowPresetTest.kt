package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ShowPresetTest {

    private val boardMonday = LocalDate.of(2026, 8, 10) // Monday

    @Test
    fun fixedDay_showOccursOnThatDayOffset() {
        val setSail = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_set_sail" }
        assertEquals(listOf(boardMonday), setSail.resolveDates(boardMonday, dayCount = 4))
    }

    @Test
    fun moana_3NightCruise_occursOnDay2And3() {
        // 四天三夜：第 4 天（下船日）无场次
        val moana = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_moana" }
        assertEquals(
            listOf(boardMonday.plusDays(1), boardMonday.plusDays(2)),
            moana.resolveDates(boardMonday, dayCount = 4),
        )
    }

    @Test
    fun moana_4NightCruise_occursOnDay2To4() {
        // 五天四夜：第 2、3、4 天
        val moana = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_moana" }
        assertEquals(
            listOf(boardMonday.plusDays(1), boardMonday.plusDays(2), boardMonday.plusDays(3)),
            moana.resolveDates(boardMonday, dayCount = 5),
        )
    }

    @Test
    fun dinnerLinked_1745_returns2030Show() {
        assertEquals(LocalTime.of(20, 30), pickDinnerLinkedTime(LocalTime.of(17, 45)))
    }

    @Test
    fun dinnerLinked_2015_returns1800Show() {
        assertEquals(LocalTime.of(18, 0), pickDinnerLinkedTime(LocalTime.of(20, 15)))
    }

    @Test
    fun fixedDayWithMultipleOffsets_returnsOnlyListedDays() {
        // 固定天契约：只取 dayOffsets 列出的天，不填充中间天（1、3 不产生第 2 天）
        val show = ShowPreset(
            key = "test_multi_offset",
            title = "测试 Test",
            deck = 10,
            zone = ShipZone.MID,
            placeName = "花园舞台",
            durationMinutes = 30,
            dayOffsets = listOf(1, 3),
            dailyRangeTo = null,
            startTimes = listOf(LocalTime.of(14, 0)),
            dinnerLinked = false,
        )
        assertEquals(
            listOf(boardMonday, boardMonday.plusDays(2)),
            show.resolveDates(boardMonday, dayCount = 5),
        )
    }

    @Test
    fun catalogRows_pinnedToSpec() {
        val expected = listOf(
            ShowPreset(
                key = "show_set_sail",
                title = "扬帆起航 Let's Set Sail",
                deck = 10,
                zone = ShipZone.STERN,
                placeName = "花园舞台",
                durationMinutes = 30,
                dayOffsets = listOf(1),
                dailyRangeTo = null,
                startTimes = listOf(LocalTime.of(13, 15), LocalTime.of(14, 0), LocalTime.of(14, 45)),
                dinnerLinked = false,
            ),
            ShowPreset(
                key = "show_lion_king",
                title = "狮子王烟花 The Lion King: Celebration in the Sky",
                deck = 17,
                zone = ShipZone.MID,
                placeName = "露天泳池",
                durationMinutes = 10,
                dayOffsets = listOf(2),
                dailyRangeTo = null,
                startTimes = listOf(LocalTime.of(22, 30)),
                dinnerLinked = false,
            ),
            ShowPreset(
                key = "show_seas_adventure",
                title = "海洋奇遇 Disney Seas The Adventure",
                deck = 6,
                zone = ShipZone.BOW,
                placeName = "华特迪士尼剧院 Walt Disney Theatre",
                durationMinutes = 60,
                dayOffsets = listOf(1),
                dailyRangeTo = null,
                startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
                dinnerLinked = true,
            ),
            ShowPreset(
                key = "show_remember",
                title = "回忆：瓦力与伊芙 Remember",
                deck = 6,
                zone = ShipZone.BOW,
                placeName = "华特迪士尼剧院 Walt Disney Theatre",
                durationMinutes = 60,
                dayOffsets = listOf(2),
                dailyRangeTo = null,
                startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
                dinnerLinked = true,
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
                startTimes = listOf(LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30)),
                dinnerLinked = false,
            ),
        )
        assertEquals(expected, DISNEY_ADVENTURE_SHOWS)
    }

    @Test
    fun catalog_containsFiveShows_withUniqueKeysAndValidDecks() {
        assertEquals(5, DISNEY_ADVENTURE_SHOWS.size)
        val keys = DISNEY_ADVENTURE_SHOWS.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
        // 迪士尼探险号客用甲板 Deck 5-19
        DISNEY_ADVENTURE_SHOWS.forEach { show ->
            assertTrue("${show.key} deck ${show.deck} out of 5..19", show.deck in 5..19)
        }
    }

    @Test
    fun onlySeasAdventureAndRemember_areDinnerLinked() {
        assertEquals(true, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_seas_adventure" }.dinnerLinked)
        assertEquals(true, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_remember" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_set_sail" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_lion_king" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_moana" }.dinnerLinked)
    }

    @Test
    fun dinnerLinked_at1900_returns1800Show() {
        assertEquals(LocalTime.of(18, 0), pickDinnerLinkedTime(LocalTime.of(19, 0)))
    }

    @Test
    fun dinnerLinked_1859_returns2030Show() {
        assertEquals(LocalTime.of(20, 30), pickDinnerLinkedTime(LocalTime.of(18, 59)))
    }

    @Test
    fun presetShowsSummary_listsAllShowsWithDayAndCount() {
        val summary = presetShowsSummary()
        val lines = summary.split("\n")
        assertEquals(5, lines.size)
        assertTrue("line0=$lines", lines[0].contains("扬帆起航 Let's Set Sail"))
        assertTrue(lines[0].contains("第1天"))
        assertTrue(lines[0].contains("3场"))
        assertTrue(lines[1].contains("狮子王烟花"))
        assertTrue(lines[1].contains("第2天"))
        assertTrue(lines[1].contains("1场"))
        assertTrue(lines[2].contains("海洋奇遇") && lines[2].contains("按晚餐时段"))
        assertTrue(lines[3].contains("回忆") && lines[3].contains("按晚餐时段"))
        assertTrue(lines[4].contains("海洋奇缘"))
        assertTrue(lines[4].contains("第2天起每天"))
        assertTrue(lines[4].contains("3场"))
    }

    @Test
    fun catalog_hasNoTimeOverlapOnSharedDays() {
        // 规格 §1/§3f：每个出现演出的天，场次区间两两不重叠（不同天的演出允许同时段——
        // 海洋奇遇与回忆场次相同但分别在第 1/第 2 天，不能做全局两两比较）。
        // 固定天取 dayOffsets；范围天取 dayOffsets.first()..dailyRangeTo。
        // dinner-linked 的两个候选场次都参与检查（候选间本身不重叠，不会误报）。
        val daysPresent = DISNEY_ADVENTURE_SHOWS.flatMap { show ->
            if (show.dailyRangeTo != null) {
                show.dayOffsets.first()..show.dailyRangeTo
            } else {
                show.dayOffsets
            }
        }.distinct()

        daysPresent.forEach { day ->
            val intervals = DISNEY_ADVENTURE_SHOWS.filter { show ->
                val days = if (show.dailyRangeTo != null) {
                    show.dayOffsets.first()..show.dailyRangeTo
                } else {
                    show.dayOffsets
                }
                day in days
            }.flatMap { show ->
                show.startTimes.map { time ->
                    val start = time.hour * 60 + time.minute
                    start to (start + show.durationMinutes)
                }
            }
            for (i in intervals.indices) {
                for (j in i + 1 until intervals.size) {
                    val (aStart, aEnd) = intervals[i]
                    val (bStart, bEnd) = intervals[j]
                    assertTrue(
                        "day $day: [$aStart,$aEnd) overlaps [$bStart,$bEnd)",
                        aStart >= bEnd || bStart >= aEnd,
                    )
                }
            }
        }
    }

    @Test
    fun showEffectiveRange_showEvent_extendsStartBy30() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(20, 30),
            durationMinutes = 60,
            categoryId = 1,
            title = "演出",
        )
        assertEquals((20 * 60 + 30 - 30) to (20 * 60 + 30 + 60), showEffectiveRange(event, isShow = true))
    }

    @Test
    fun showEffectiveRange_nonShowEvent_keepsOriginalRange() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(9, 0),
            durationMinutes = 30,
            categoryId = 1,
            title = "早餐",
        )
        assertEquals((9 * 60) to (9 * 60 + 30), showEffectiveRange(event, isShow = false))
    }

    @Test
    fun showRangeOf_matchesByCategoryId_notEventId() {
        // C1 回归测试：判定必须按 categoryId，不能按 eventId（事件 id 与类别 id 各自自增会撞值）
        val showCategoryIds = setOf(1L)          // show 类别的 id = 1
        val showEvent = CruiseEvent(
            id = 5, date = LocalDate.of(2026, 8, 10),   // 事件 id = 5（故意不同）
            startTime = LocalTime.of(20, 0), durationMinutes = 60,
            categoryId = 1, title = "演出",
        )
        val ordinaryEvent = CruiseEvent(
            id = 1, date = LocalDate.of(2026, 8, 10),   // 事件 id = 1（撞类别 id！）
            startTime = LocalTime.of(19, 45), durationMinutes = 10,
            categoryId = 2, title = "普通事件",
        )
        val rangeOf = showRangeOf(showCategoryIds)
        // showEvent 按扩展区间（19:30 起），ordinaryEvent 原始区间——两者重叠 → 冲突。
        // 19:45–19:55 落在 showEvent 的扩展区间（19:30 起）内、但落在其原始区间（20:00 起）外，
        // 因此 buggy 逻辑（按事件 id 判定：showEvent id=5 ∉ {1} 不扩展、ordinaryEvent id=1 ∈ {1}
        // 被误扩展为 19:15–19:55）下两者不重叠 → 冲突数为 0，测试失败。能区分正确/错误实现。
        val conflicts = findConflicts(listOf(showEvent, ordinaryEvent), rangeOf)
        assertEquals(1, conflicts.size)
    }

    @Test
    fun fixedDayShows_clampToLastVoyageDay() {
        // 1 夜航程（dayCount=2）：第 2 天 = 下船日，固定天演出不得生成在下船日
        val lionKing = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_lion_king" }
        assertEquals(emptyList<LocalDate>(), lionKing.resolveDates(boardMonday, dayCount = 2))
        // 常规 4 天航程不受影响
        assertEquals(listOf(boardMonday.plusDays(1)), lionKing.resolveDates(boardMonday, dayCount = 4))
    }

    @Test
    fun isSameShowAs_matchesWhenOnlyTimeAndDateDiffer() {
        val showSession1 = CruiseEvent(
            id = 10,
            date = LocalDate.of(2026, 8, 16),
            startTime = LocalTime.of(9, 30),
            durationMinutes = 15,
            categoryId = 2,
            title = "海洋奇缘 Moana: Call of the Sea",
            deck = 10,
            zone = ShipZone.STERN,
            placeName = "寻路者湾 Wayfinder Bay",
        )
        val showSession2 = CruiseEvent(
            id = 15,
            date = LocalDate.of(2026, 8, 17),
            startTime = LocalTime.of(10, 30),
            durationMinutes = 15,
            categoryId = 2,
            title = "海洋奇缘 Moana: Call of the Sea",
            deck = 10,
            zone = ShipZone.STERN,
            placeName = "寻路者湾 Wayfinder Bay",
        )
        assertTrue(showSession1.isSameShowAs(showSession2))
    }

    @Test
    fun isSameShowAs_differentTitleOrLocation_returnsFalse() {
        val showSession1 = CruiseEvent(
            id = 10,
            date = LocalDate.of(2026, 8, 16),
            startTime = LocalTime.of(9, 30),
            durationMinutes = 15,
            categoryId = 2,
            title = "海洋奇缘 Moana: Call of the Sea",
            deck = 10,
            zone = ShipZone.STERN,
            placeName = "寻路者湾 Wayfinder Bay",
        )
        val differentTitle = showSession1.copy(title = "扬帆起航 Let's Set Sail")
        val differentDeck = showSession1.copy(deck = 6)
        val differentCategory = showSession1.copy(categoryId = 3)
        assertEquals(false, showSession1.isSameShowAs(differentTitle))
        assertEquals(false, showSession1.isSameShowAs(differentDeck))
        assertEquals(false, showSession1.isSameShowAs(differentCategory))
    }
}
