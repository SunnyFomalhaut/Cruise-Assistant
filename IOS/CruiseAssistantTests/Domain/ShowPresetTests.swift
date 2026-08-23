import XCTest
@testable import CruiseAssistant

final class ShowPresetTests: XCTestCase {

    private let boardMonday = SimpleDate(year: 2026, month: 8, day: 10) // Monday

    func testFixedDay_showOccursOnThatDayOffset() {
        guard let setSail = DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_set_sail" }) else {
            XCTFail("show_set_sail not found")
            return
        }
        XCTAssertEqual(setSail.resolveDates(boardDate: boardMonday, dayCount: 4), [boardMonday])
    }

    func testMoana_3NightCruise_occursOnDay2And3() {
        // 四天三夜：第 4 天（下船日）无场次
        guard let moana = DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_moana" }) else {
            XCTFail("show_moana not found")
            return
        }
        XCTAssertEqual(
            moana.resolveDates(boardDate: boardMonday, dayCount: 4),
            [boardMonday.plusDays(1), boardMonday.plusDays(2)]
        )
    }

    func testMoana_4NightCruise_occursOnDay2To4() {
        // 五天四夜：第 2、3、4 天
        guard let moana = DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_moana" }) else {
            XCTFail("show_moana not found")
            return
        }
        XCTAssertEqual(
            moana.resolveDates(boardDate: boardMonday, dayCount: 5),
            [boardMonday.plusDays(1), boardMonday.plusDays(2), boardMonday.plusDays(3)]
        )
    }

    func testDinnerLinked_1745_returns2030Show() {
        XCTAssertEqual(
            pickDinnerLinkedTime(dinnerTime: SimpleTime(hour: 17, minute: 45)),
            SimpleTime(hour: 20, minute: 30)
        )
    }

    func testDinnerLinked_2015_returns1800Show() {
        XCTAssertEqual(
            pickDinnerLinkedTime(dinnerTime: SimpleTime(hour: 20, minute: 15)),
            SimpleTime(hour: 18, minute: 0)
        )
    }

    func testDinnerLinked_at1900_returns1800Show() {
        XCTAssertEqual(
            pickDinnerLinkedTime(dinnerTime: SimpleTime(hour: 19, minute: 0)),
            SimpleTime(hour: 18, minute: 0)
        )
    }

    func testDinnerLinked_1859_returns2030Show() {
        XCTAssertEqual(
            pickDinnerLinkedTime(dinnerTime: SimpleTime(hour: 18, minute: 59)),
            SimpleTime(hour: 20, minute: 30)
        )
    }

    func testFixedDayWithMultipleOffsets_returnsOnlyListedDays() {
        // 固定天契约：只取 dayOffsets 列出的天，不填充中间天（1、3 不产生第 2 天）
        let show = ShowPreset(
            key: "test_multi_offset",
            title: "测试 Test",
            deck: 10,
            zone: .mid,
            placeName: "花园舞台",
            durationMinutes: 30,
            dayOffsets: [1, 3],
            dailyRangeTo: nil,
            startTimes: [SimpleTime(hour: 14, minute: 0)],
            dinnerLinked: false
        )
        XCTAssertEqual(
            show.resolveDates(boardDate: boardMonday, dayCount: 5),
            [boardMonday, boardMonday.plusDays(2)]
        )
    }

    func testCatalogRows_pinnedToSpec() {
        let expected = [
            ShowPreset(
                key: "show_set_sail",
                title: "扬帆起航 Let's Set Sail",
                deck: 10,
                zone: .mid,
                placeName: "花园舞台 Garden Stage",
                durationMinutes: 30,
                dayOffsets: [1],
                dailyRangeTo: nil,
                startTimes: [
                    SimpleTime(hour: 13, minute: 15),
                    SimpleTime(hour: 14, minute: 0),
                    SimpleTime(hour: 14, minute: 45),
                ],
                dinnerLinked: false,
                defaultWaitMinutes: 15
            ),
            ShowPreset(
                key: "show_lion_king",
                title: "狮子王烟花 The Lion King: Celebration in the Sky",
                deck: 17,
                zone: .mid,
                placeName: "露天泳池",
                durationMinutes: 10,
                dayOffsets: [2],
                dailyRangeTo: nil,
                startTimes: [SimpleTime(hour: 22, minute: 30)],
                dinnerLinked: false,
                defaultWaitMinutes: 45
            ),
            ShowPreset(
                key: "show_seas_adventure",
                title: "海洋奇遇 Disney Seas The Adventure",
                deck: 6,
                zone: .bow,
                placeName: "华特迪士尼剧院 Walt Disney Theatre",
                durationMinutes: 60,
                dayOffsets: [1],
                dailyRangeTo: nil,
                startTimes: [SimpleTime(hour: 18, minute: 0), SimpleTime(hour: 20, minute: 30)],
                dinnerLinked: true,
                defaultWaitMinutes: 45
            ),
            ShowPreset(
                key: "show_remember",
                title: "回忆：瓦力与伊芙 Remember",
                deck: 6,
                zone: .bow,
                placeName: "华特迪士尼剧院 Walt Disney Theatre",
                durationMinutes: 60,
                dayOffsets: [2],
                dailyRangeTo: nil,
                startTimes: [SimpleTime(hour: 18, minute: 0), SimpleTime(hour: 20, minute: 30)],
                dinnerLinked: true,
                defaultWaitMinutes: 45
            ),
            ShowPreset(
                key: "show_moana",
                title: "海洋奇缘 Moana: Call of the Sea",
                deck: 10,
                zone: .stern,
                placeName: "寻路者湾 Wayfinder Bay",
                durationMinutes: 15,
                dayOffsets: [2],
                dailyRangeTo: 4,
                startTimes: [
                    SimpleTime(hour: 9, minute: 30),
                    SimpleTime(hour: 10, minute: 0),
                    SimpleTime(hour: 10, minute: 30),
                ],
                dinnerLinked: false,
                defaultWaitMinutes: 30
            ),
            ShowPreset(
                key: "show_avengers",
                title: "复仇者集结 Avengers Assemble!",
                deck: 10,
                zone: .mid,
                placeName: "花园舞台 Garden Stage",
                durationMinutes: 40,
                dayOffsets: [2],
                dailyRangeTo: 4,
                startTimes: [
                    SimpleTime(hour: 18, minute: 15),
                    SimpleTime(hour: 20, minute: 45),
                ],
                dinnerLinked: false,
                defaultWaitMinutes: 15
            ),
            ShowPreset(
                key: "show_baymax_expo",
                title: "大白超级健身世博会 Baymax Super Exercise Expo",
                deck: 10,
                zone: .mid,
                placeName: "花园舞台 Garden Stage",
                durationMinutes: 30,
                dayOffsets: [2],
                dailyRangeTo: 4,
                startTimes: [SimpleTime(hour: 8, minute: 45)],
                dinnerLinked: false,
                defaultWaitMinutes: 15
            ),
            ShowPreset(
                key: "show_duffy_friend_ship",
                title: "达菲与友谊之船 Duffy and The Friend Ship",
                deck: 6,
                zone: .bow,
                placeName: "华特迪士尼剧院 Walt Disney Theatre",
                durationMinutes: 30,
                dayOffsets: [2],
                dailyRangeTo: 4,
                startTimes: [SimpleTime(hour: 9, minute: 45)],
                dinnerLinked: false,
                defaultWaitMinutes: 45
            ),
        ]
        XCTAssertEqual(expected, DISNEY_ADVENTURE_SHOWS)
    }

    func testCatalog_containsEightShows_withUniqueKeysAndValidDecks() {
        XCTAssertEqual(8, DISNEY_ADVENTURE_SHOWS.count)
        let keys = DISNEY_ADVENTURE_SHOWS.map { $0.key }
        XCTAssertEqual(keys.count, Set(keys).count)
        // 迪士尼探险号客用甲板 Deck 5-19
        for show in DISNEY_ADVENTURE_SHOWS {
            XCTAssertTrue((5...19).contains(show.deck), "\(show.key) deck \(show.deck) out of 5..19")
        }
    }

    func testOnlySeasAdventureAndRemember_areDinnerLinked() {
        XCTAssertEqual(true, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_seas_adventure" })?.dinnerLinked)
        XCTAssertEqual(true, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_remember" })?.dinnerLinked)
        XCTAssertEqual(false, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_set_sail" })?.dinnerLinked)
        XCTAssertEqual(false, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_lion_king" })?.dinnerLinked)
        XCTAssertEqual(false, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_moana" })?.dinnerLinked)
        XCTAssertEqual(false, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_avengers" })?.dinnerLinked)
        XCTAssertEqual(false, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_baymax_expo" })?.dinnerLinked)
        XCTAssertEqual(false, DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_duffy_friend_ship" })?.dinnerLinked)
    }

    func testPresetShowsSummary_listsAllShowsWithDayAndCount() {
        let summary = presetShowsSummary()
        let lines = summary.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        XCTAssertEqual(8, lines.count)
        XCTAssertTrue(lines[0].contains("扬帆起航 Let's Set Sail"))
        XCTAssertTrue(lines[0].contains("第1天"))
        XCTAssertTrue(lines[0].contains("3场"))
        XCTAssertTrue(lines[1].contains("狮子王烟花"))
        XCTAssertTrue(lines[1].contains("第2天"))
        XCTAssertTrue(lines[1].contains("1场"))
        XCTAssertTrue(lines[2].contains("海洋奇遇") && lines[2].contains("按晚餐时段"))
        XCTAssertTrue(lines[3].contains("回忆") && lines[3].contains("按晚餐时段"))
        XCTAssertTrue(lines[4].contains("海洋奇缘"))
        XCTAssertTrue(lines[4].contains("第2天起每天"))
        XCTAssertTrue(lines[4].contains("3场"))
        XCTAssertTrue(lines[5].contains("复仇者集结 Avengers Assemble!"))
        XCTAssertTrue(lines[5].contains("第2天起每天"))
        XCTAssertTrue(lines[5].contains("2场"))
        XCTAssertTrue(lines[6].contains("大白超级健身世博会 Baymax Super Exercise Expo"))
        XCTAssertTrue(lines[6].contains("第2天起每天"))
        XCTAssertTrue(lines[6].contains("1场"))
        XCTAssertTrue(lines[7].contains("达菲与友谊之船 Duffy and The Friend Ship"))
        XCTAssertTrue(lines[7].contains("第2天起每天"))
        XCTAssertTrue(lines[7].contains("1场"))
    }

    func testDisneyAdventureShows_contains8ShowsWithEarliestTimes() {
        let shows = DISNEY_ADVENTURE_SHOWS
        XCTAssertEqual(8, shows.count)

        guard let baymax = shows.first(where: { $0.key == "show_baymax_expo" }) else {
            XCTFail("baymax not found")
            return
        }
        XCTAssertEqual("大白超级健身世博会 Baymax Super Exercise Expo", baymax.title)
        XCTAssertEqual(10, baymax.deck)
        XCTAssertEqual(ShipZone.mid, baymax.zone)
        XCTAssertEqual("花园舞台 Garden Stage", baymax.placeName)
        XCTAssertEqual(30, baymax.durationMinutes)
        XCTAssertEqual(SimpleTime(hour: 8, minute: 45), baymax.startTimes.first)

        guard let duffy = shows.first(where: { $0.key == "show_duffy_friend_ship" }) else {
            XCTFail("duffy not found")
            return
        }
        XCTAssertEqual("达菲与友谊之船 Duffy and The Friend Ship", duffy.title)
        XCTAssertEqual(6, duffy.deck)
        XCTAssertEqual(ShipZone.bow, duffy.zone)
        XCTAssertEqual("华特迪士尼剧院 Walt Disney Theatre", duffy.placeName)
        XCTAssertEqual(30, duffy.durationMinutes)
        XCTAssertEqual(SimpleTime(hour: 9, minute: 45), duffy.startTimes.first)
    }

    func testDefaultWaitMinutes_matchesSpecRules() {
        XCTAssertEqual(45, resolveDefaultWaitMinutes(placeName: "华特迪士尼剧院 Walt Disney Theatre", deck: 6))
        XCTAssertEqual(45, resolveDefaultWaitMinutes(placeName: "露天泳池", deck: 17, title: "狮子王烟花"))
        XCTAssertEqual(15, resolveDefaultWaitMinutes(placeName: "大白剧院 Baymax Cinemas", deck: 7))
        XCTAssertEqual(15, resolveDefaultWaitMinutes(placeName: "花园舞台 Garden Stage", deck: 10))
        XCTAssertEqual(30, resolveDefaultWaitMinutes(placeName: "寻路者湾 Wayfinder Bay", deck: 10))
        XCTAssertEqual(30, resolveDefaultWaitMinutes(placeName: "未知地点", deck: 5))
    }

    func testDay2_showsLaneAssignment_earlyDinner_avengersAdaptsToLane2() {
        let boardDate = SimpleDate(year: 2026, month: 8, day: 10)
        let dinnerTime = SimpleTime(hour: 17, minute: 45) // Remember is 20:30
        let day2 = boardDate.plusDays(1)
        var occupied: [(lane: Int, start: Int, end: Int)] = []
        var day2Events: [(key: String, lane: Int)] = []

        for show in DISNEY_ADVENTURE_SHOWS {
            if show.resolveDates(boardDate: boardDate, dayCount: 4).contains(day2) {
                let times = show.dinnerLinked ? [pickDinnerLinkedTime(dinnerTime: dinnerTime)] : show.startTimes
                for startTime in times {
                    let startMin = startTime.totalMinutes
                    let effStart = startMin - show.defaultWaitMinutes
                    let effEnd = startMin + show.durationMinutes
                    let lane = assignShowLane(startMinute: effStart, endMinute: effEnd, occupied: occupied)
                    occupied.append((lane: lane, start: effStart, end: effEnd))
                    day2Events.append((key: show.key, lane: lane))
                }
            }
        }

        let avengersLanes = day2Events.filter { $0.key == "show_avengers" }.map { $0.lane }
        // 18:15 进第 2 栏 (lane=1)，20:45 与《回忆》(20:30, 45m wait) 冲突进第 3 栏 (lane=2)
        XCTAssertEqual([1, 2], avengersLanes)
    }

    func testDay2_showsLaneAssignment_lateDinner_avengersAdaptsToLane2() {
        let boardDate = SimpleDate(year: 2026, month: 8, day: 10)
        let dinnerTime = SimpleTime(hour: 20, minute: 15) // Remember is 18:00
        let day2 = boardDate.plusDays(1)
        var occupied: [(lane: Int, start: Int, end: Int)] = []
        var day2Events: [(key: String, lane: Int)] = []

        for show in DISNEY_ADVENTURE_SHOWS {
            if show.resolveDates(boardDate: boardDate, dayCount: 4).contains(day2) {
                let times = show.dinnerLinked ? [pickDinnerLinkedTime(dinnerTime: dinnerTime)] : show.startTimes
                for startTime in times {
                    let startMin = startTime.totalMinutes
                    let effStart = startMin - show.defaultWaitMinutes
                    let effEnd = startMin + show.durationMinutes
                    let lane = assignShowLane(startMinute: effStart, endMinute: effEnd, occupied: occupied)
                    occupied.append((lane: lane, start: effStart, end: effEnd))
                    day2Events.append((key: show.key, lane: lane))
                }
            }
        }

        let avengersLanes = day2Events.filter { $0.key == "show_avengers" }.map { $0.lane }
        // 18:15 与《回忆》(18:00, 45m wait) 冲突进第 3 栏 (lane=2)，20:45 进第 2 栏 (lane=1)
        XCTAssertEqual([2, 1], avengersLanes)
    }

    func testShowEffectiveRange_showEvent_extendsStartBy30() {
        let event = CruiseEvent(
            date: SimpleDate(year: 2026, month: 8, day: 10),
            startTime: SimpleTime(hour: 20, minute: 30),
            durationMinutes: 60,
            categoryId: 1,
            title: "演出"
        )
        let range = showEffectiveRange(event, isShow: true)
        XCTAssertEqual(20 * 60 + 30 - 30, range.start)
        XCTAssertEqual(20 * 60 + 30 + 60, range.end)
    }

    func testShowEffectiveRange_nonShowEvent_keepsOriginalRange() {
        let event = CruiseEvent(
            date: SimpleDate(year: 2026, month: 8, day: 10),
            startTime: SimpleTime(hour: 9, minute: 0),
            durationMinutes: 30,
            categoryId: 1,
            title: "早餐"
        )
        let range = showEffectiveRange(event, isShow: false)
        XCTAssertEqual(9 * 60, range.start)
        XCTAssertEqual(9 * 60 + 30, range.end)
    }

    func testShowRangeOf_matchesByCategoryId_notEventId() {
        // C1 回归测试：判定必须按 categoryId，不能按 eventId（事件 id 与类别 id 各自自增会撞值）
        let showCategoryIds: Set<Int64> = [1]          // show 类别的 id = 1
        let showEvent = CruiseEvent(
            id: 5, date: SimpleDate(year: 2026, month: 8, day: 10),   // 事件 id = 5（故意不同）
            startTime: SimpleTime(hour: 20, minute: 0), durationMinutes: 60,
            categoryId: 1, title: "演出"
        )
        let ordinaryEvent = CruiseEvent(
            id: 1, date: SimpleDate(year: 2026, month: 8, day: 10),   // 事件 id = 1（撞类别 id！）
            startTime: SimpleTime(hour: 19, minute: 45), durationMinutes: 10,
            categoryId: 2, title: "普通事件"
        )
        let rangeOf = showRangeOf(showCategoryIds)
        // showEvent 按扩展区间（19:30 起），ordinaryEvent 原始区间——两者重叠 → 冲突。
        // 19:45–19:55 落在 showEvent 的扩展区间（19:30 起）内、但落在其原始区间（20:00 起）外，
        // 因此 buggy 逻辑（按事件 id 判定：showEvent id=5 ∉ {1} 不扩展、ordinaryEvent id=1 ∈ {1}
        // 被误扩展为 19:15–19:55）下两者不重叠 → 冲突数为 0，测试失败。能区分正确/错误实现。
        let conflicts = findConflicts([showEvent, ordinaryEvent], rangeOf: rangeOf)
        XCTAssertEqual(1, conflicts.count)
    }

    func testFixedDayShows_clampToLastVoyageDay() {
        // 1 夜航程（dayCount=2）：第 2 天 = 下船日，固定天演出不得生成在下船日
        guard let lionKing = DISNEY_ADVENTURE_SHOWS.first(where: { $0.key == "show_lion_king" }) else {
            XCTFail("lionKing not found")
            return
        }
        XCTAssertEqual([], lionKing.resolveDates(boardDate: boardMonday, dayCount: 2))
        // 常规 4 天航程不受影响
        XCTAssertEqual([boardMonday.plusDays(1)], lionKing.resolveDates(boardDate: boardMonday, dayCount: 4))
    }

    func testIsSameShowAs_matchesWhenOnlyTimeAndDateDiffer() {
        let showSession1 = CruiseEvent(
            id: 10,
            date: SimpleDate(year: 2026, month: 8, day: 16),
            startTime: SimpleTime(hour: 9, minute: 30),
            durationMinutes: 15,
            categoryId: 2,
            title: "海洋奇缘 Moana: Call of the Sea",
            deck: 10,
            zone: .stern,
            placeName: "寻路者湾 Wayfinder Bay"
        )
        let showSession2 = CruiseEvent(
            id: 15,
            date: SimpleDate(year: 2026, month: 8, day: 17),
            startTime: SimpleTime(hour: 10, minute: 30),
            durationMinutes: 15,
            categoryId: 2,
            title: "海洋奇缘 Moana: Call of the Sea",
            deck: 10,
            zone: .stern,
            placeName: "寻路者湾 Wayfinder Bay"
        )
        XCTAssertTrue(showSession1.isSameShowAs(showSession2))
    }

    func testIsSameShowAs_differentTitleOrLocation_returnsFalse() {
        let showSession1 = CruiseEvent(
            id: 10,
            date: SimpleDate(year: 2026, month: 8, day: 16),
            startTime: SimpleTime(hour: 9, minute: 30),
            durationMinutes: 15,
            categoryId: 2,
            title: "海洋奇缘 Moana: Call of the Sea",
            deck: 10,
            zone: .stern,
            placeName: "寻路者湾 Wayfinder Bay"
        )
        var differentTitle = showSession1
        differentTitle.title = "扬帆起航 Let's Set Sail"
        var differentDeck = showSession1
        differentDeck.deck = 6
        var differentCategory = showSession1
        differentCategory.categoryId = 3

        XCTAssertFalse(showSession1.isSameShowAs(differentTitle))
        XCTAssertFalse(showSession1.isSameShowAs(differentDeck))
        XCTAssertFalse(showSession1.isSameShowAs(differentCategory))
    }

    func testFormatWaitStart_regularShow_subtracts30Minutes() {
        let event = CruiseEvent(
            date: SimpleDate(year: 2026, month: 8, day: 10),
            startTime: SimpleTime(hour: 20, minute: 30),
            durationMinutes: 60,
            categoryId: 1,
            title: "晚间大秀"
        )
        XCTAssertEqual("20:00", formatWaitStart(event))
    }

    func testFormatWaitStart_morningShow_formatsLeadingZeros() {
        let event = CruiseEvent(
            date: SimpleDate(year: 2026, month: 8, day: 10),
            startTime: SimpleTime(hour: 9, minute: 30),
            durationMinutes: 30,
            categoryId: 1,
            title: "早间演出"
        )
        XCTAssertEqual("09:00", formatWaitStart(event))
    }

    func testFormatWaitStart_midnightCrossing_wrapsAroundCleanly() {
        let event = CruiseEvent(
            date: SimpleDate(year: 2026, month: 8, day: 10),
            startTime: SimpleTime(hour: 0, minute: 15),
            durationMinutes: 30,
            categoryId: 1,
            title: "午夜派对"
        )
        XCTAssertEqual("23:45", formatWaitStart(event))
    }
}
