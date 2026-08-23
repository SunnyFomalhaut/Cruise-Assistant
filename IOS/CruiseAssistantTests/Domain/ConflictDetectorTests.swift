import XCTest
@testable import CruiseAssistant

final class ConflictDetectorTests: XCTestCase {

    private let day = SimpleDate(year: 2026, month: 9, day: 3)

    private func ev(_ id: Int64, _ hour: Int, _ minute: Int, _ durMin: Int) -> CruiseEvent {
        CruiseEvent(
            id: id,
            date: day,
            startTime: SimpleTime(hour: hour, minute: minute),
            durationMinutes: durMin,
            categoryId: 1,
            title: "e\(id)"
        )
    }

    func testEmptyList_hasNoConflicts() {
        XCTAssertTrue(findConflicts([]).isEmpty)
    }

    func testSingleEvent_hasNoConflicts() {
        XCTAssertTrue(findConflicts([ev(1, 13, 0, 30)]).isEmpty)
    }

    func testSequentialEvents_haveNoConflicts() {
        let events = [ev(1, 12, 0, 30), ev(2, 13, 0, 30), ev(3, 14, 0, 30)]
        XCTAssertTrue(findConflicts(events).isEmpty)
    }

    func testBackToBackEvents_haveNoConflicts() {
        // 13:00-13:30 紧接 13:30-14:00
        let events = [ev(1, 13, 0, 30), ev(2, 13, 30, 30)]
        XCTAssertTrue(findConflicts(events).isEmpty)
    }

    func testOverlappingPair_isReportedOnce() {
        let events = [ev(1, 13, 0, 45), ev(2, 13, 20, 60)]
        let conflicts = findConflicts(events)
        XCTAssertEqual(1, conflicts.count)
        XCTAssertEqual(1, conflicts[0].first.id)
        XCTAssertEqual(2, conflicts[0].second.id)
    }

    func testThreeMutuallyOverlappingEvents_yieldThreePairs() {
        let events = [ev(1, 13, 0, 60), ev(2, 13, 10, 60), ev(3, 13, 20, 60)]
        XCTAssertEqual(3, findConflicts(events).count)
    }

    func testConflicts_areOrderedByStartTime() {
        let events = [ev(2, 15, 0, 60), ev(1, 13, 0, 60), ev(3, 15, 30, 60)]
        let conflicts = findConflicts(events)
        XCTAssertEqual(1, conflicts.count)
        // 先开始的排在 first
        XCTAssertEqual(2, conflicts[0].first.id)
        XCTAssertEqual(3, conflicts[0].second.id)
    }

    func testIdenticalEvents_conflict() {
        let events = [ev(1, 13, 0, 30), ev(2, 13, 0, 30)]
        XCTAssertEqual(1, findConflicts(events).count)
    }

    func testLane_doesNotAffectConflictDetection() {
        // 分在不同栏只是显示上不重叠，时间上仍然算冲突
        var e1 = ev(1, 13, 0, 60)
        e1.lane = 0
        var e2 = ev(2, 13, 10, 60)
        e2.lane = 1
        let events = [e1, e2]
        XCTAssertEqual(1, findConflicts(events).count)
    }

    func testZeroDurationEvent_neverConflicts() {
        let events = [ev(1, 13, 0, 60), ev(2, 13, 30, 0)]
        XCTAssertTrue(findConflicts(events).isEmpty)
    }

    func testInactiveEvents_areExcludedFromConflicts() {
        var e1 = ev(1, 13, 0, 60)
        e1.isActive = false
        var e2 = ev(2, 13, 20, 60)
        e2.isActive = false
        let events = [e1, e2]
        XCTAssertTrue(findConflicts(events).isEmpty)
    }

    func testInactiveEvent_doesNotConflictWithActiveEvent() {
        var e1 = ev(1, 13, 0, 60)
        e1.isActive = true
        var e2 = ev(2, 13, 20, 60)
        e2.isActive = false
        let events = [e1, e2]
        XCTAssertTrue(findConflicts(events).isEmpty)
    }

    func testActiveEvents_stillConflict_whenInactiveEventPresent() {
        var e1 = ev(1, 13, 0, 60)
        e1.isActive = true
        var e2 = ev(2, 13, 20, 60)
        e2.isActive = true
        var e3 = ev(3, 13, 10, 60)
        e3.isActive = false
        let events = [e1, e2, e3]
        // 只有 1 和 2 都是 active 且重叠 → 1 对冲突
        XCTAssertEqual(1, findConflicts(events).count)
    }

    func testNamedArgument_findConflicts() {
        let events = [ev(1, 13, 0, 45), ev(2, 13, 20, 60)]
        let conflicts = findConflicts(events: events)
        XCTAssertEqual(1, conflicts.count)
    }

    func testShowWaitTimeExpansionConflicts_WhenOneSiblingShowDeactivated() {
        let showCatId: Int64 = 2
        let rangeOf = showRangeOf([showCatId])

        // 达菲与友谊之船 09:45-10:15 (wait 45m -> 09:00-10:15)
        var duffy = CruiseEvent(
            id: 32,
            date: SimpleDate(year: 2026, month: 9, day: 4),
            startTime: SimpleTime(hour: 9, minute: 45),
            durationMinutes: 30,
            categoryId: showCatId,
            title: "达菲与友谊之船",
            waitMinutes: 45
        )
        duffy.isActive = true

        // 海洋奇缘 09:30-09:45 (wait 30m -> 09:00-09:45)
        var moana1 = CruiseEvent(
            id: 14,
            date: SimpleDate(year: 2026, month: 9, day: 4),
            startTime: SimpleTime(hour: 9, minute: 30),
            durationMinutes: 15,
            categoryId: showCatId,
            title: "海洋奇缘",
            waitMinutes: 30
        )
        moana1.isActive = true

        // 海洋奇缘 10:00-10:15 (deactivated)
        var moana2 = CruiseEvent(
            id: 15,
            date: SimpleDate(year: 2026, month: 9, day: 4),
            startTime: SimpleTime(hour: 10, minute: 0),
            durationMinutes: 15,
            categoryId: showCatId,
            title: "海洋奇缘",
            waitMinutes: 30
        )
        moana2.isActive = false

        // 大白超级健身 08:45-09:15 (wait 15m -> 08:30-09:15)
        var baymax = CruiseEvent(
            id: 29,
            date: SimpleDate(year: 2026, month: 9, day: 4),
            startTime: SimpleTime(hour: 8, minute: 45),
            durationMinutes: 30,
            categoryId: showCatId,
            title: "大白超级健身",
            waitMinutes: 15
        )
        baymax.isActive = true

        let events = [baymax, moana1, duffy, moana2]
        let conflicts = findConflicts(events, rangeOf: rangeOf)

        // baymax (08:30-09:15) overlaps with moana1 (09:00-09:45) and duffy (09:00-10:15)
        // moana1 (09:00-09:45) overlaps with duffy (09:00-10:15)
        // Total active conflict pairs = 3
        XCTAssertEqual(3, conflicts.count)
        XCTAssertTrue(conflicts.contains { $0.first.id == 29 && $0.second.id == 14 })
        XCTAssertTrue(conflicts.contains { $0.first.id == 29 && $0.second.id == 32 })
        XCTAssertTrue(conflicts.contains { $0.first.id == 14 && $0.second.id == 32 })
    }
}
