import XCTest
@testable import CruiseAssistant

final class DomainModelTests: XCTestCase {

    // MARK: - SimpleDate Tests

    func testSimpleDateCreationAndFormatting() {
        let date = SimpleDate(year: 2026, month: 8, day: 23)
        XCTAssertEqual(date.year, 2026)
        XCTAssertEqual(date.month, 8)
        XCTAssertEqual(date.day, 23)
        XCTAssertEqual(date.description, "2026-08-23")

        let parsed = SimpleDate("2026-08-23")
        XCTAssertEqual(parsed, date)

        let invalid1 = SimpleDate("2026-13-01")
        XCTAssertNil(invalid1)

        let invalid2 = SimpleDate("2026-08-32")
        XCTAssertNil(invalid2)

        let invalid3 = SimpleDate("not-a-date")
        XCTAssertNil(invalid3)
    }

    func testSimpleDateEpochDayRoundtrip() {
        let testDates = [
            SimpleDate(year: 1970, month: 1, day: 1),
            SimpleDate(year: 2000, month: 1, day: 1),
            SimpleDate(year: 2000, month: 2, day: 29), // Leap year 2000
            SimpleDate(year: 2024, month: 2, day: 29), // Leap year 2024
            SimpleDate(year: 2026, month: 8, day: 23),
            SimpleDate(year: 2099, month: 12, day: 31),
        ]

        for original in testDates {
            let epoch = original.epochDay
            let reconstructed = SimpleDate(epochDay: epoch)
            XCTAssertEqual(original, reconstructed, "Epoch roundtrip failed for \(original)")
        }

        XCTAssertEqual(SimpleDate(year: 1970, month: 1, day: 1).epochDay, 0)
    }

    func testSimpleDateArithmeticAndComparison() {
        let d1 = SimpleDate(year: 2026, month: 8, day: 23)
        let d2 = d1.plusDays(5)
        XCTAssertEqual(d2, SimpleDate(year: 2026, month: 8, day: 28))
        XCTAssertEqual(d1.daysBetween(d2), 5)
        XCTAssertEqual(d2.daysBetween(d1), -5)

        let monthEnd = SimpleDate(year: 2026, month: 8, day: 31)
        let nextMonth = monthEnd.plusDays(1)
        XCTAssertEqual(nextMonth, SimpleDate(year: 2026, month: 9, day: 1))

        let yearEnd = SimpleDate(year: 2026, month: 12, day: 31)
        let nextYear = yearEnd.plusDays(1)
        XCTAssertEqual(nextYear, SimpleDate(year: 2027, month: 1, day: 1))

        XCTAssertTrue(d1.isBefore(d2))
        XCTAssertTrue(d2.isAfter(d1))
        XCTAssertTrue(d1 < d2)
    }

    func testSimpleDateCodable() throws {
        let date = SimpleDate(year: 2026, month: 8, day: 23)
        let encoder = JSONEncoder()
        let data = try encoder.encode(date)
        let jsonStr = String(data: data, encoding: .utf8)
        XCTAssertEqual(jsonStr, "\"2026-08-23\"")

        let decoder = JSONDecoder()
        let decoded = try decoder.decode(SimpleDate.self, from: data)
        XCTAssertEqual(decoded, date)
    }

    // MARK: - SimpleTime Tests

    func testSimpleTimeCreationAndFormatting() {
        let time = SimpleTime(hour: 14, minute: 30)
        XCTAssertEqual(time.hour, 14)
        XCTAssertEqual(time.minute, 30)
        XCTAssertEqual(time.totalMinutes, 870)
        XCTAssertEqual(time.description, "14:30")

        let parsed = SimpleTime("07:05")
        XCTAssertEqual(parsed?.hour, 7)
        XCTAssertEqual(parsed?.minute, 5)
        XCTAssertEqual(parsed?.description, "07:05")

        XCTAssertNil(SimpleTime("25:00"))
        XCTAssertNil(SimpleTime("12:60"))
        XCTAssertNil(SimpleTime("invalid"))
    }

    func testSimpleTimeFromMinutesAndArithmetic() {
        let t1 = SimpleTime(totalMinutes: 90) // 01:30
        XCTAssertEqual(t1.hour, 1)
        XCTAssertEqual(t1.minute, 30)

        let t2 = t1.plusMinutes(45) // 02:15
        XCTAssertEqual(t2.hour, 2)
        XCTAssertEqual(t2.minute, 15)

        let t3 = SimpleTime(hour: 10, minute: 0)
        let t4 = SimpleTime(hour: 11, minute: 30)
        XCTAssertTrue(t3 < t4)
    }

    func testSimpleTimeCodable() throws {
        let time = SimpleTime(hour: 20, minute: 15)
        let data = try JSONEncoder().encode(time)
        XCTAssertEqual(String(data: data, encoding: .utf8), "\"20:15\"")

        let decoded = try JSONDecoder().decode(SimpleTime.self, from: data)
        XCTAssertEqual(decoded, time)
    }

    // MARK: - ShipZone Tests

    func testShipZone() {
        XCTAssertEqual(ShipZone.bow.label, "船头")
        XCTAssertEqual(ShipZone.mid.label, "船中")
        XCTAssertEqual(ShipZone.stern.label, "船尾")
        XCTAssertEqual(ShipZone.allCases.count, 3)
    }

    // MARK: - Voyage Tests

    func testVoyageCalculations() {
        let boardDate = SimpleDate(year: 2026, month: 8, day: 23)
        let boardTime = SimpleTime(hour: 13, minute: 0)
        let disembarkDate = SimpleDate(year: 2026, month: 8, day: 27)
        let disembarkTime = SimpleTime(hour: 7, minute: 0)

        let voyage = Voyage(
            id: 1,
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: boardTime,
            disembarkDate: disembarkDate,
            disembarkTime: disembarkTime,
            maxDeck: 20,
            minDeck: 5
        )

        XCTAssertEqual(voyage.dayCount, 5)

        let dates = voyage.datesInVoyage()
        XCTAssertEqual(dates.count, 5)
        XCTAssertEqual(dates[0], SimpleDate(year: 2026, month: 8, day: 23))
        XCTAssertEqual(dates[4], SimpleDate(year: 2026, month: 8, day: 27))

        XCTAssertTrue(voyage.contains(date: SimpleDate(year: 2026, month: 8, day: 23)))
        XCTAssertTrue(voyage.contains(date: SimpleDate(year: 2026, month: 8, day: 25)))
        XCTAssertTrue(voyage.contains(date: SimpleDate(year: 2026, month: 8, day: 27)))
        XCTAssertFalse(voyage.contains(date: SimpleDate(year: 2026, month: 8, day: 22)))
        XCTAssertFalse(voyage.contains(date: SimpleDate(year: 2026, month: 8, day: 28)))

        XCTAssertEqual(voyage.dayIndexOf(date: SimpleDate(year: 2026, month: 8, day: 23)), 1)
        XCTAssertEqual(voyage.dayIndexOf(date: SimpleDate(year: 2026, month: 8, day: 25)), 3)
        XCTAssertEqual(voyage.dayIndexOf(date: SimpleDate(year: 2026, month: 8, day: 27)), 5)
        XCTAssertNil(voyage.dayIndexOf(date: SimpleDate(year: 2026, month: 8, day: 22)))

        XCTAssertEqual(voyage.clamp(date: SimpleDate(year: 2026, month: 8, day: 10)), boardDate)
        XCTAssertEqual(voyage.clamp(date: SimpleDate(year: 2026, month: 8, day: 30)), disembarkDate)
        XCTAssertEqual(voyage.clamp(date: SimpleDate(year: 2026, month: 8, day: 24)), SimpleDate(year: 2026, month: 8, day: 24))
    }

    // MARK: - CruiseEvent Tests

    func testCruiseEventTimeCalculations() {
        let event = CruiseEvent(
            id: 101,
            date: SimpleDate(year: 2026, month: 8, day: 23),
            startTime: SimpleTime(hour: 14, minute: 30),
            durationMinutes: 45,
            categoryId: 2,
            title: "Lion King Show",
            deck: 4,
            zone: .mid,
            placeName: "Walt Disney Theatre",
            lane: 1
        )

        XCTAssertEqual(event.startMinute, 14 * 60 + 30) // 870
        XCTAssertEqual(event.endMinute, 870 + 45) // 915
        XCTAssertEqual(event.endTime, SimpleTime(hour: 15, minute: 15))
        XCTAssertEqual(event.lane, 1)
        XCTAssertTrue(event.isActive)
    }

    // MARK: - DayPlan Tests

    func testDayPlan() {
        let plan = DayPlan(date: SimpleDate(year: 2026, month: 8, day: 24), finalized: true, finalizedAt: 123456789)
        XCTAssertEqual(plan.id, SimpleDate(year: 2026, month: 8, day: 24))
        XCTAssertTrue(plan.finalized)
        XCTAssertEqual(plan.finalizedAt, 123456789)
    }

    // MARK: - Category Tests

    func testBuiltInCategories() {
        XCTAssertEqual(BUILT_IN_CATEGORIES.count, 9)
        XCTAssertEqual(BUILT_IN_CATEGORIES[0].name, "餐食")
        XCTAssertEqual(BUILT_IN_CATEGORIES[0].iconKey, "meal")
        XCTAssertEqual(BUILT_IN_CATEGORIES[0].colorArgb, 0xFF34A853)
        XCTAssertTrue(BUILT_IN_CATEGORIES[0].isBuiltIn)

        XCTAssertEqual(BUILT_IN_CATEGORIES[1].name, "演出")
        XCTAssertEqual(BUILT_IN_CATEGORIES[2].name, "重要事项")
        XCTAssertEqual(BUILT_IN_CATEGORIES[3].name, "琐事")
        XCTAssertEqual(BUILT_IN_CATEGORIES[4].name, "娱乐项目")
        XCTAssertEqual(BUILT_IN_CATEGORIES[5].name, "见面会")
        XCTAssertEqual(BUILT_IN_CATEGORIES[6].name, "泳池娱乐")
        XCTAssertEqual(BUILT_IN_CATEGORIES[7].name, "商店")
        XCTAssertEqual(BUILT_IN_CATEGORIES[8].name, "青少年活动")

        XCTAssertEqual(CATEGORY_PALETTE.count, 8)
        XCTAssertEqual(FALLBACK_CATEGORY_NAME, "琐事")
    }

    // MARK: - PixieLevel & PixieDust Tests

    func testPixieLevelsAndValidation() {
        XCTAssertEqual(PixieLevel.BUILT_IN_PIXIE_LEVELS.count, 3)
        XCTAssertEqual(PixieLevel.BUILT_IN_PIXIE_LEVELS[0].name, "普通")
        XCTAssertEqual(PixieLevel.BUILT_IN_PIXIE_LEVELS[1].name, "精致")
        XCTAssertEqual(PixieLevel.BUILT_IN_PIXIE_LEVELS[2].name, "豪华")

        XCTAssertTrue(isValidRoomInput("550"))
        XCTAssertTrue(isValidRoomInput("001"))
        XCTAssertFalse(isValidRoomInput("11550"))
        XCTAssertFalse(isValidRoomInput("55"))
        XCTAssertFalse(isValidRoomInput("55A"))
    }

    func testPixieDustRecordAndGrouping() {
        let levels = PixieLevel.BUILT_IN_PIXIE_LEVELS
        let records = [
            PixieDustRecord(id: 1, direction: .given, deck: 11, room: "550", levelId: 1, levelNameSnapshot: "普通", timestamp: 1000),
            PixieDustRecord(id: 2, direction: .given, deck: 11, room: "550", levelId: 2, levelNameSnapshot: "精致", timestamp: 2000),
            PixieDustRecord(id: 3, direction: .given, deck: 10, room: "123", levelId: 3, levelNameSnapshot: "豪华", timestamp: 3000),
            PixieDustRecord(id: 4, direction: .received, deck: 9, room: "456", levelId: 2, levelNameSnapshot: "精致", replied: false, timestamp: 1500),
            PixieDustRecord(id: 5, direction: .received, deck: 8, room: "789", levelId: 1, levelNameSnapshot: "普通", replied: true, timestamp: 1800),
        ]

        XCTAssertEqual(records[0].fullRoom, "11550")

        let groups = groupGivenByRoom(records: records, levels: levels)
        XCTAssertEqual(groups.count, 2)
        XCTAssertEqual(groups[0].deck, 10) // timestamp 3000 first
        XCTAssertEqual(groups[0].total, 1)
        XCTAssertEqual(groups[1].deck, 11)
        XCTAssertEqual(groups[1].total, 2)

        let received = sortReceived(records: records, levels: levels)
        XCTAssertEqual(received.count, 2)
        XCTAssertEqual(received[0].id, 4) // unreplied first
        XCTAssertEqual(received[1].id, 5) // replied last

        let lastRecord = lastLevelForRoom(records: records, deck: 11, room: "550")
        XCTAssertEqual(lastRecord?.id, 2)
    }

    // MARK: - WishlistItem Tests

    func testWishlistItem() {
        let item = WishlistItem(
            id: 1,
            title: "Try AquaDucky",
            categoryId: 5,
            deck: 12,
            zone: .mid,
            placeName: "Pool Deck",
            note: "Bring goggles"
        )
        XCTAssertEqual(item.title, "Try AquaDucky")
        XCTAssertFalse(item.isCompleted)
        XCTAssertEqual(item.zone, .mid)
    }

    // MARK: - ColorTheme Tests

    func testAppColorTheme() {
        XCTAssertEqual(AppColorTheme.allCases.count, 8)

        XCTAssertEqual(AppColorTheme.fromKey("judy_blue"), .judy_blue)
        XCTAssertEqual(AppColorTheme.fromKey("duffy_blue"), .duffy_blue)
        XCTAssertEqual(AppColorTheme.fromKey("stellalou_purple"), .lulu_purple)
        XCTAssertEqual(AppColorTheme.fromKey("lulu_purple"), .lulu_purple)
        XCTAssertEqual(AppColorTheme.fromKey("unknown_theme"), .judy_blue)
        XCTAssertEqual(AppColorTheme.fromKey(nil), .judy_blue)

        XCTAssertEqual(AppColorTheme.judy_blue.displayName, "朱迪蓝")
        XCTAssertEqual(AppColorTheme.judy_blue.primaryArgb, 0xFF0284C7)
        XCTAssertEqual(AppColorTheme.judy_blue.secondaryArgb, 0xFF0369A1)
        XCTAssertEqual(AppColorTheme.judy_blue.containerArgb, 0xFFE0F2FE)
    }

    // MARK: - WallpaperPreset Tests

    func testWallpaperPreset() {
        XCTAssertEqual(WallpaperPreset.allCases.count, 9)
        XCTAssertEqual(WallpaperPreset.presets.count, 7)

        XCTAssertEqual(WallpaperPreset.fromKey("duffy"), .duffy)
        XCTAssertEqual(WallpaperPreset.fromKey("none"), .none)
        XCTAssertEqual(WallpaperPreset.fromKey("custom"), .custom)
        XCTAssertEqual(WallpaperPreset.fromKey("invalid_preset"), .none)
        XCTAssertEqual(WallpaperPreset.fromKey(nil), .none)

        XCTAssertEqual(WallpaperPreset.duffy.displayName, "达菲蓝")
        XCTAssertEqual(WallpaperPreset.duffy.imageResourceName, "duffy")
        XCTAssertEqual(WallpaperPreset.duffy.assetFilename, "duffy.png")
        XCTAssertNil(WallpaperPreset.none.imageResourceName)
    }
}
