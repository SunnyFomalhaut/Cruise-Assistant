import XCTest
@testable import CruiseAssistant

final class LaneAssignerTests: XCTestCase {

    private let day = SimpleDate(year: 2026, month: 9, day: 3)

    private func ev(id: Int64, hour: Int, minute: Int, durMin: Int, lane: Int) -> CruiseEvent {
        CruiseEvent(
            id: id,
            date: day,
            startTime: SimpleTime(hour: hour, minute: minute),
            durationMinutes: durMin,
            categoryId: 1,
            title: "e\(id)",
            lane: lane
        )
    }

    func testOverlaps_isTrue_whenRangesIntersect() {
        XCTAssertTrue(overlaps(100, 200, 150, 250))
        XCTAssertTrue(overlaps(150, 250, 100, 200))
        XCTAssertTrue(overlaps(100, 300, 150, 200)) // Completely contained
    }

    func testAdjacentRanges_doNotOverlap() {
        // 13:00-13:30 and 13:30-14:00 are adjacent, not conflicting
        XCTAssertFalse(overlaps(780, 810, 810, 840))
        XCTAssertFalse(overlaps(810, 840, 780, 810))
    }

    func testIdenticalRanges_overlap() {
        XCTAssertTrue(overlaps(780, 810, 780, 810))
    }

    func testFirstEvent_goesToLane0() {
        XCTAssertEqual(0, assignLane(startMinute: 780, endMinute: 810, existing: []))
    }

    func testNonOverlappingEvent_reusesLane0() {
        let existing = [ev(id: 1, hour: 13, minute: 0, durMin: 30, lane: 0)]
        // 14:00-14:30 does not overlap with 13:00-13:30 in lane 0
        XCTAssertEqual(0, assignLane(startMinute: 840, endMinute: 870, existing: existing))
    }

    func testOverlappingEvent_fallsToLane1() {
        let existing = [ev(id: 1, hour: 13, minute: 0, durMin: 60, lane: 0)]
        // 13:15-13:45 overlaps with lane 0
        XCTAssertEqual(1, assignLane(startMinute: 795, endMinute: 825, existing: existing))
    }

    func testTwoOverlappingEvents_pushThirdToNarrowLane2() {
        let existing = [
            ev(id: 1, hour: 13, minute: 0, durMin: 60, lane: 0),
            ev(id: 2, hour: 13, minute: 10, durMin: 60, lane: 1),
        ]
        XCTAssertEqual(2, assignLane(startMinute: 800, endMinute: 830, existing: existing))
    }

    func testAllThreeLanesBusy_returnsLane2AsOverflow() {
        let existing = [
            ev(id: 1, hour: 13, minute: 0, durMin: 60, lane: 0),
            ev(id: 2, hour: 13, minute: 0, durMin: 60, lane: 1),
            ev(id: 3, hour: 13, minute: 0, durMin: 60, lane: 2),
        ]
        XCTAssertEqual(LANE_COUNT - 1, assignLane(startMinute: 790, endMinute: 820, existing: existing))
    }

    func testExcludeId_letsEventKeepItsOwnLane_whenEditing() {
        let existing = [ev(id: 7, hour: 13, minute: 0, durMin: 60, lane: 0)]
        // Editing event 7 should ignore itself -> stays in lane 0
        XCTAssertEqual(0, assignLane(startMinute: 780, endMinute: 840, existing: existing, excludeId: 7))
    }

    func testHigherLaneEvents_areIgnored_whenLowerLaneIsFree() {
        let existing = [ev(id: 1, hour: 13, minute: 0, durMin: 60, lane: 2)]
        XCTAssertEqual(0, assignLane(startMinute: 780, endMinute: 840, existing: existing))
    }

    func testShowLane_free_returnsLane1() {
        XCTAssertEqual(1, assignShowLane(startMinute: 540, endMinute: 585, occupied: []))
    }

    func testShowLane_overlappingInLane1_movesToLane2() {
        // Moana show: 9:00-9:45 occupies lane 1, 9:30-10:15 overlaps -> lane 2
        let occupied = [(lane: 1, start: 540, end: 585)]
        XCTAssertEqual(2, assignShowLane(startMinute: 570, endMinute: 615, occupied: occupied))
    }

    func testShowLane_thirdShow_returnsToLane1_whenLane1FreeAgain() {
        // 10:00-10:45 does not overlap with 9:00-9:45 (lane 1), overlaps with 9:30-10:15 (lane 2) -> back to lane 1
        let occupied = [(lane: 1, start: 540, end: 585), (lane: 2, start: 570, end: 615)]
        XCTAssertEqual(1, assignShowLane(startMinute: 600, endMinute: 645, occupied: occupied))
    }

    func testShowLane_bothLanesBusy_fallsBackToLane0() {
        let occupied = [(lane: 1, start: 540, end: 700), (lane: 2, start: 540, end: 700)]
        XCTAssertEqual(0, assignShowLane(startMinute: 600, endMinute: 645, occupied: occupied))
    }

    func testAssignLanePreserving_keepsPreferredLane_whenItIsFree() {
        // Preferred lane (1) has no conflict, even though lane 0 is free, it should keep preferred lane 1
        let existing = [ev(id: 1, hour: 15, minute: 0, durMin: 60, lane: 2)]
        XCTAssertEqual(1, assignLanePreserving(startMinute: 780, endMinute: 840, existing: existing, preferredLane: 1))
    }

    func testAssignLanePreserving_emptyDay_returnsPreferredLane() {
        XCTAssertEqual(2, assignLanePreserving(startMinute: 780, endMinute: 840, existing: [], preferredLane: 2))
    }

    func testAssignLanePreserving_fallsBack_whenPreferredLaneBusy() {
        // Preferred lane 1 is occupied 13:00-14:00, 13:15-13:45 overlaps -> falls back to lane 0
        let existing = [ev(id: 1, hour: 13, minute: 0, durMin: 60, lane: 1)]
        XCTAssertEqual(0, assignLanePreserving(startMinute: 795, endMinute: 825, existing: existing, preferredLane: 1))
    }

    func testAssignLanePreserving_excludesSelf_whenCheckingPreferredLane() {
        // Checking own preferred lane should exclude self (editing / revival scenario with excludeId)
        let existing = [ev(id: 7, hour: 13, minute: 0, durMin: 60, lane: 1)]
        XCTAssertEqual(1, assignLanePreserving(startMinute: 780, endMinute: 840, existing: existing, preferredLane: 1, excludeId: 7))
    }
}
