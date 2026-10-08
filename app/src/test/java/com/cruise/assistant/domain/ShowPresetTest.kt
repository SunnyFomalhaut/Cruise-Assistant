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
                zone = ShipZone.MID,
                placeName = "花园舞台 Garden Stage",
                durationMinutes = 30,
                dayOffsets = listOf(1),
                dailyRangeTo = null,
                startTimes = listOf(LocalTime.of(13, 15), LocalTime.of(14, 0), LocalTime.of(14, 45)),
                dinnerLinked = false,
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
                dailyRangeTo = null,
                startTimes = listOf(LocalTime.of(22, 30)),
                dinnerLinked = false,
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
                dailyRangeTo = null,
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
                dailyRangeTo = null,
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
                startTimes = listOf(LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30)),
                dinnerLinked = false,
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
                startTimes = listOf(LocalTime.of(18, 15), LocalTime.of(20, 45)),
                dinnerLinked = false,
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
                dinnerLinked = false,
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
                dinnerLinked = false,
                defaultWaitMinutes = 45,
                note = "达菲与朋友们的主题秀，非每日固定，请查阅 Navigator App。建议提前 45 分钟候场。",
            ),
        )
        assertEquals(expected, DISNEY_ADVENTURE_SHOWS)
    }

    @Test
    fun timelineAutoGeneratedShows_containsExactlyThreeShows() {
        assertEquals(3, TIMELINE_AUTO_GENERATED_SHOWS.size)
        val keys = TIMELINE_AUTO_GENERATED_SHOWS.map { it.key }
        assertEquals(listOf("show_set_sail", "show_lion_king", "show_seas_adventure"), keys)

        val lionKing = TIMELINE_AUTO_GENERATED_SHOWS.first { it.key == "show_lion_king" }
        assertEquals("阳光泳池 Sunnyside Pool", lionKing.placeName)
        assertEquals("烟花在右舷，可以在这个泳池旁边的空地或者18甲左舷走廊（人比较多）", lionKing.note)
    }

    @Test
    fun wishlistSeedShows_containsExactlyFiveFloatingShows() {
        assertEquals(5, WISHLIST_SEED_SHOWS.size)
        val keys = WISHLIST_SEED_SHOWS.map { it.key }
        assertEquals(
            listOf(
                "show_remember",
                "show_moana",
                "show_avengers",
                "show_baymax_expo",
                "show_duffy_friend_ship",
            ),
            keys,
        )
        WISHLIST_SEED_SHOWS.forEach { show ->
            assertTrue("${show.key} should have a helpful note", !show.note.isNullOrBlank())
        }
    }

    @Test
    fun presetShowsSummary_listsOnlyTimelineAutoGeneratedShows() {
        val summary = presetShowsSummary()
        val lines = summary.split("\n")
        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("扬帆起航 Let's Set Sail"))
        assertTrue(lines[1].contains("狮子王烟花"))
        assertTrue(lines[2].contains("海洋奇遇"))
    }

    @Test
    fun catalog_containsEightShows_withUniqueKeysAndValidDecks() {
        assertEquals(8, DISNEY_ADVENTURE_SHOWS.size)
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
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_avengers" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_baymax_expo" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_duffy_friend_ship" }.dinnerLinked)
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
    fun disneyAdventureShows_contains8ShowsWithEarliestTimes() {
        val shows = DISNEY_ADVENTURE_SHOWS
        assertEquals(8, shows.size)

        val baymax = shows.first { it.key == "show_baymax_expo" }
        assertEquals("大白超级健身世博会 Baymax Super Exercise Expo", baymax.title)
        assertEquals(10, baymax.deck)
        assertEquals(ShipZone.MID, baymax.zone)
        assertEquals("花园舞台 Garden Stage", baymax.placeName)
        assertEquals(30, baymax.durationMinutes)
        assertEquals(LocalTime.of(8, 45), baymax.startTimes.first())

        val duffy = shows.first { it.key == "show_duffy_friend_ship" }
        assertEquals("达菲与友谊之船 Duffy and The Friend Ship", duffy.title)
        assertEquals(6, duffy.deck)
        assertEquals(ShipZone.BOW, duffy.zone)
        assertEquals("华特迪士尼剧院 Walt Disney Theatre", duffy.placeName)
        assertEquals(30, duffy.durationMinutes)
        assertEquals(LocalTime.of(9, 45), duffy.startTimes.first())
    }

    @Test
    fun defaultWaitMinutes_matchesSpecRules() {
        assertEquals(45, resolveDefaultWaitMinutes("华特迪士尼剧院 Walt Disney Theatre", deck = 6))
        assertEquals(45, resolveDefaultWaitMinutes("露天泳池", deck = 17, title = "狮子王烟花"))
        assertEquals(15, resolveDefaultWaitMinutes("大白剧院 Baymax Cinemas", deck = 7))
        assertEquals(15, resolveDefaultWaitMinutes("花园舞台 Garden Stage", deck = 10))
        assertEquals(30, resolveDefaultWaitMinutes("寻路者湾 Wayfinder Bay", deck = 10))
        assertEquals(30, resolveDefaultWaitMinutes("未知地点", deck = 5))
    }

    @Test
    fun day2_showsLaneAssignment_earlyDinner_avengersAdaptsToLane2() {
        val boardDate = LocalDate.of(2026, 8, 10)
        val dinnerTime = LocalTime.of(17, 45) // Remember is 20:30
        val day2 = boardDate.plusDays(1)
        val occupied = mutableListOf<Triple<Int, Int, Int>>()
        val day2Events = mutableListOf<Pair<String, Int>>()

        DISNEY_ADVENTURE_SHOWS.forEach { show ->
            if (day2 in show.resolveDates(boardDate, 4)) {
                val times = if (show.dinnerLinked) listOf(pickDinnerLinkedTime(dinnerTime)) else show.startTimes
                times.forEach { startTime ->
                    val startMin = startTime.hour * 60 + startTime.minute
                    val effStart = startMin - show.defaultWaitMinutes
                    val effEnd = startMin + show.durationMinutes
                    val lane = assignShowLane(effStart, effEnd, occupied)
                    occupied += Triple(lane, effStart, effEnd)
                    day2Events += show.key to lane
                }
            }
        }

        val avengersLanes = day2Events.filter { it.first == "show_avengers" }.map { it.second }
        // 18:15 进第 2 栏 (lane=1)，20:45 与《回忆》(20:30, 45m wait) 冲突进第 3 栏 (lane=2)
        assertEquals(listOf(1, 2), avengersLanes)
    }

    @Test
    fun day2_showsLaneAssignment_lateDinner_avengersAdaptsToLane2() {
        val boardDate = LocalDate.of(2026, 8, 10)
        val dinnerTime = LocalTime.of(20, 15) // Remember is 18:00
        val day2 = boardDate.plusDays(1)
        val occupied = mutableListOf<Triple<Int, Int, Int>>()
        val day2Events = mutableListOf<Pair<String, Int>>()

        DISNEY_ADVENTURE_SHOWS.forEach { show ->
            if (day2 in show.resolveDates(boardDate, 4)) {
                val times = if (show.dinnerLinked) listOf(pickDinnerLinkedTime(dinnerTime)) else show.startTimes
                times.forEach { startTime ->
                    val startMin = startTime.hour * 60 + startTime.minute
                    val effStart = startMin - show.defaultWaitMinutes
                    val effEnd = startMin + show.durationMinutes
                    val lane = assignShowLane(effStart, effEnd, occupied)
                    occupied += Triple(lane, effStart, effEnd)
                    day2Events += show.key to lane
                }
            }
        }

        val avengersLanes = day2Events.filter { it.first == "show_avengers" }.map { it.second }
        // 18:15 与《回忆》(18:00, 45m wait) 冲突进第 3 栏 (lane=2)，20:45 进第 2 栏 (lane=1)
        assertEquals(listOf(2, 1), avengersLanes)
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
    fun showEffectiveRange_zeroWait_equalsOriginalRange() {
        // 不候场（waitMinutes=0）的演出：扩展区间退化为原时段，不向前占位
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(20, 30),
            durationMinutes = 60,
            categoryId = 1,
            title = "演出",
            waitMinutes = 0,
        )
        assertEquals((20 * 60 + 30) to (20 * 60 + 30 + 60), showEffectiveRange(event, isShow = true))
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

    @Test
    fun formatWaitStart_regularShow_subtracts30Minutes() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(20, 30),
            durationMinutes = 60,
            categoryId = 1,
            title = "晚间大秀",
        )
        assertEquals("20:00", formatWaitStart(event))
    }

    @Test
    fun formatWaitStart_morningShow_formatsLeadingZeros() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(9, 30),
            durationMinutes = 30,
            categoryId = 1,
            title = "早间演出",
        )
        assertEquals("09:00", formatWaitStart(event))
    }

    @Test
    fun formatWaitStart_midnightCrossing_wrapsAroundCleanly() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(0, 15),
            durationMinutes = 30,
            categoryId = 1,
            title = "午夜派对",
        )
        assertEquals("23:45", formatWaitStart(event))
    }

    @Test
    fun formatWaitStart_zeroWait_returnsShowStartTimeItself() {
        // 不候场（waitMinutes=0）：入场时间就是开场时间本身
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 10),
            startTime = LocalTime.of(20, 30),
            durationMinutes = 60,
            categoryId = 1,
            title = "晚间大秀",
            waitMinutes = 0,
        )
        assertEquals("20:30", formatWaitStart(event))
    }
}
