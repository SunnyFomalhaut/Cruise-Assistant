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
}
