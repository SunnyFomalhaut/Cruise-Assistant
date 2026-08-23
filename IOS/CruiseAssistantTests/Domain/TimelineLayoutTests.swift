import XCTest
@testable import CruiseAssistant

final class TimelineLayoutTests: XCTestCase {
    private let day = SimpleDate(year: 2026, month: 9, day: 3)
    private let eps = 0.01

    private func ev(hour: Int, minute: Int, durMin: Int, id: Int64 = 0, categoryId: Int64 = 1, title: String = "e") -> CruiseEvent {
        CruiseEvent(
            id: id,
            date: day,
            startTime: SimpleTime(hour: hour, minute: minute),
            durationMinutes: durMin,
            categoryId: categoryId,
            title: title
        )
    }

    // MARK: - 无折叠：纯线性映射

    func testRangeStart_mapsToZero() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.minuteToPt(420), 0.0, accuracy: eps)
    }

    func testOneHourAfterStart_mapsToOneHourHeight() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.minuteToPt(480), 60.0, accuracy: eps)
    }

    func testHalfHour_mapsToHalfHeight() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.minuteToPt(450), 30.0, accuracy: eps)
    }

    func testTotalHeight_matchesRangeTimesHourHeight() {
        // 07:00-24:00 = 17 hours
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.totalHeightPt, 17 * 60.0, accuracy: eps)
    }

    func testCustomHourHeight_scalesProportionally() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 100, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.minuteToPt(480), 100.0, accuracy: eps)
        XCTAssertEqual(l.totalHeightPt, 17 * 100.0, accuracy: eps)
    }

    func testMinutesBeforeRange_clampToZero() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.minuteToPt(0), 0.0, accuracy: eps)
        XCTAssertEqual(l.minuteToPt(300), 0.0, accuracy: eps)
    }

    func testMinutesAfterRange_clampToTotalHeight() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.minuteToPt(1500), l.totalHeightPt, accuracy: eps)
    }

    // MARK: - 反向映射

    func testPtToMinute_isInverseOfMinuteToPt() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        for minute in [420, 500, 780, 1000, 1439] {
            XCTAssertEqual(l.ptToMinute(l.minuteToPt(minute)), minute)
        }
    }

    func testPtToMinute_clampsToRange() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertEqual(l.ptToMinute(-50.0), 420)
        XCTAssertEqual(l.ptToMinute(99999.0), 1440)
    }

    // MARK: - 小时刻度

    func testHourTicks_coverEveryWholeHourInRange() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        let ticks = l.hourTicks()
        // 07:00 to 24:00 inclusive = 18 ticks
        XCTAssertEqual(ticks.count, 18)
        XCTAssertEqual(ticks.first?.minute, 420)
        XCTAssertEqual(ticks.last?.minute, 1440)
        XCTAssertEqual(ticks.first?.pt ?? -1, 0.0, accuracy: eps)
        // 60pt height < 90, no half-hour ticks
        XCTAssertTrue(ticks.allSatisfy { !$0.isHalfHour })
    }

    func testHourTicks_startAtFirstWholeHourAtOrAfterRangeStart() {
        // Starts at 07:30 -> first tick is 08:00 (480)
        let l = buildTimelineLayout(rangeStartMinute: 450, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        let first = l.hourTicks().first
        XCTAssertEqual(first?.minute, 480)
        XCTAssertEqual(first?.isHalfHour, false)
    }

    func testHourTicks_includeHalfHours_whenHourHeight90Plus() {
        let layout = buildTimelineLayout(
            rangeStartMinute: 300,
            rangeEndMinute: 420,
            hourHeightPt: 90,
            events: [],
            collapseEmptyGaps: false
        )
        let ticks = layout.hourTicks()
        // 5:00, 5:30, 6:00, 6:30, 7:00
        XCTAssertEqual(ticks.map { $0.minute }, [300, 330, 360, 390, 420])
        XCTAssertEqual(ticks.map { $0.isHalfHour }, [false, true, false, true, false])
    }

    func testHourTicks_omitHalfHours_below90() {
        let layout = buildTimelineLayout(
            rangeStartMinute: 300,
            rangeEndMinute: 420,
            hourHeightPt: 89,
            events: [],
            collapseEmptyGaps: false
        )
        XCTAssertEqual(layout.hourTicks().map { $0.minute }, [300, 360, 420])
    }

    // MARK: - 折叠空时段

    func testCollapseOff_keepsSingleSegment() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [ev(hour: 13, minute: 0, durMin: 60)], collapseEmptyGaps: false)
        XCTAssertEqual(l.segments.count, 1)
        XCTAssertTrue(l.segments.allSatisfy { !$0.collapsed })
    }

    func testCollapseOn_withNoEvents_collapsesWholeRange() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: true)
        XCTAssertTrue(l.segments.allSatisfy { $0.collapsed })
        XCTAssertEqual(l.totalHeightPt, COLLAPSED_SEGMENT_PT, accuracy: eps)
    }

    func testCollapseOn_shrinksLongEmptyGapBetweenEvents() {
        // 08:00-09:00 one event, 20:00-21:00 one event, 11 hours empty between
        let events = [ev(hour: 8, minute: 0, durMin: 60), ev(hour: 20, minute: 0, durMin: 60)]
        let collapsed = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: true)
        let expanded = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: false)
        XCTAssertTrue(collapsed.totalHeightPt < expanded.totalHeightPt)
        XCTAssertTrue(collapsed.segments.contains { $0.collapsed })
    }

    func testCollapseOn_keepsEventSegmentsAtFullScale() {
        let events = [ev(hour: 8, minute: 0, durMin: 60)]
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: true)
        // 08:00->09:00 interval is still 60pt
        XCTAssertEqual(l.minuteToPt(540) - l.minuteToPt(480), 60.0, accuracy: eps)
    }

    func testCollapseOn_keepsShortGapsExpanded() {
        // Two events with 20 minutes between (8:00-9:00 and 9:20-10:20), gap is under threshold, should not collapse
        let events = [ev(hour: 8, minute: 0, durMin: 60), ev(hour: 9, minute: 20, durMin: 60)]
        let l = buildTimelineLayout(rangeStartMinute: 480, rangeEndMinute: 660, hourHeightPt: 60, events: events, collapseEmptyGaps: true)
        XCTAssertTrue(l.segments.allSatisfy { !$0.collapsed })
    }

    func testEventPadding_preservedAroundEvents() {
        // Event at 10:00-11:00 (600-660), range 07:00-24:00 (420-1440)
        // Preserved span with padding: (600-30 = 570) to (660+30 = 690) -> 120 minutes uncollapsed
        let events = [ev(hour: 10, minute: 0, durMin: 60)]
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: true)

        let uncollapsed = l.segments.filter { !$0.collapsed }
        XCTAssertEqual(uncollapsed.count, 1)
        XCTAssertEqual(uncollapsed[0].startMinute, 570)
        XCTAssertEqual(uncollapsed[0].endMinute, 690)
        XCTAssertEqual(uncollapsed[0].heightPt, 120.0 / 60.0 * 60.0, accuracy: eps)
    }

    func testGapBetweenEvents_collapsesWhenOverThreshold() {
        // Event 1: 08:00-09:00 (keep range 07:30-09:30, minutes 450-570)
        // Event 2: 12:00-13:00 (keep range 11:30-13:30, minutes 690-810)
        // Gap between 570 and 690 is 120 minutes >= 90 -> Collapsed!
        let events = [ev(hour: 8, minute: 0, durMin: 60), ev(hour: 12, minute: 0, durMin: 60)]
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: true)

        let middleGap = l.segments.first { $0.startMinute == 570 }
        XCTAssertNotNil(middleGap)
        XCTAssertEqual(middleGap?.endMinute, 690)
        XCTAssertTrue(middleGap?.collapsed == true)
        XCTAssertEqual(middleGap?.heightPt ?? -1.0, COLLAPSED_SEGMENT_PT, accuracy: eps)
    }

    func testGapUnderThreshold_doesNotCollapse() {
        // Event 1: 08:00-09:00 (keep range 07:30-09:30, minutes 450-570)
        // Event 2: 10:40-11:40 (keep range 10:10-12:10, minutes 610-730)
        // Gap between 570 and 610 is 40 minutes < 90 -> Not collapsed!
        let events = [ev(hour: 8, minute: 0, durMin: 60), ev(hour: 10, minute: 40, durMin: 60)]
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: true)

        // The whole span 420-730 merges into uncollapsed since gap < 90
        let uncollapsed = l.segments.filter { !$0.collapsed }
        XCTAssertTrue(uncollapsed.contains { $0.startMinute <= 450 && $0.endMinute >= 730 })
    }

    // MARK: - 坐标映射与单调性

    func testMinuteToPt_isMonotonic_whenCollapsing() {
        let events = [ev(hour: 8, minute: 0, durMin: 60), ev(hour: 20, minute: 0, durMin: 60)]
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: true)
        var prev = -1.0
        for minute in stride(from: 420, through: 1440, by: 5) {
            let pt = l.minuteToPt(minute)
            XCTAssertTrue(pt >= prev - eps, "pt must be monotonically non-decreasing @minute=\(minute)")
            prev = pt
        }
    }

    func testPtToMinute_isInverse_whenCollapsing() {
        let events = [ev(hour: 8, minute: 0, durMin: 60), ev(hour: 20, minute: 0, durMin: 60)]
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: events, collapseEmptyGaps: true)
        // Uncollapsed spans should round-trip accurately
        for minute in [480, 510, 540, 1200, 1260] {
            XCTAssertEqual(l.ptToMinute(l.minuteToPt(minute)), minute)
        }
    }

    func testMinuteToPt_insideCollapsedSegment_interpolates() {
        // Empty day: single collapsed segment from 420 to 1440 (1020 minutes), totalHeight = 28pt
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: true)
        XCTAssertEqual(l.segments.count, 1)
        XCTAssertTrue(l.segments[0].collapsed)

        // Midpoint of the range (420 + 510 = 930) should map to half of 28pt = 14pt
        let midPt = l.minuteToPt(930)
        XCTAssertEqual(midPt, 14.0, accuracy: eps)
    }

    func testPtToMinute_insideCollapsedSegment_returnsCenter() {
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: true)
        // Halfway down (14pt) should convert back to minute 930
        let midMinute = l.ptToMinute(14.0)
        XCTAssertEqual(midMinute, 930)
    }

    // MARK: - 范围外事件计数

    func testCountsEventsBeforeAndAfterRange() {
        let events = [
            ev(hour: 3, minute: 0, durMin: 60),
            ev(hour: 5, minute: 0, durMin: 60),
            ev(hour: 13, minute: 0, durMin: 60),
            ev(hour: 23, minute: 30, durMin: 60)
        ]
        // Range 07:00-23:00 (420-1380) -> 2 before, 1 after (23:30 starts >= 1380 is false, but ends 24:30? Wait: 23:30 starts at 1410 >= 1380 -> hiddenAfter = 1)
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1380, hourHeightPt: 60, events: events, collapseEmptyGaps: false)
        XCTAssertEqual(l.hiddenBefore, 2)
        XCTAssertEqual(l.hiddenAfter, 1)
    }

    func testEventPartiallyInsideRange_isNotCountedAsHidden() {
        // 06:30-07:30 crosses range start (420), not counted as hiddenBefore
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1380, hourHeightPt: 60, events: [ev(hour: 6, minute: 30, durMin: 60)], collapseEmptyGaps: false)
        XCTAssertEqual(l.hiddenBefore, 0)
    }

    func testHiddenBeforeAndAfter_countedCorrectly() {
        let events = [
            ev(hour: 2, minute: 0, durMin: 60),
            ev(hour: 6, minute: 0, durMin: 60), // ends 7:00 (420) <= 420 -> hiddenBefore
            ev(hour: 8, minute: 0, durMin: 60), // in range
            ev(hour: 23, minute: 0, durMin: 60) // starts 23:00 (1380) >= 1380 -> hiddenAfter
        ]
        let l = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1380, hourHeightPt: 60, events: events, collapseEmptyGaps: true)
        XCTAssertEqual(l.hiddenBefore, 2)
        XCTAssertEqual(l.hiddenAfter, 1)
    }

    // MARK: - 参数校验与规范化

    func testInvalidRange_isNormalizedToMinimumTwoHours() {
        let l = buildTimelineLayout(rangeStartMinute: 600, rangeEndMinute: 600, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        XCTAssertTrue(l.rangeEndMinute - l.rangeStartMinute >= 120)
    }

    func testShowEvent_extendsOccupiedRangeBy30Minutes() {
        let show = CruiseEvent(
            id: 1,
            date: SimpleDate(year: 2026, month: 8, day: 10),
            startTime: SimpleTime(hour: 20, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "演出"
        )
        let ordinary = CruiseEvent(
            id: 2,
            date: SimpleDate(year: 2026, month: 8, day: 10),
            startTime: SimpleTime(hour: 19, minute: 40),
            durationMinutes: 30,
            categoryId: 1,
            title: "普通事件"
        )
        let conflicts = findConflicts([show, ordinary]) { e in
            showEffectiveRange(e, isShow: e.id == 1)
        }
        XCTAssertEqual(conflicts.count, 1)
    }

    func testNearestHourAtPt_roundsToNearestWholeHour() {
        // 420..1440 (07:00..24:00), 60pt/hr -> 1pt = 1min, topPt of 07:00 is 0pt
        let layout = buildTimelineLayout(rangeStartMinute: 420, rangeEndMinute: 1440, hourHeightPt: 60, events: [], collapseEmptyGaps: false)
        // 07:00 is minute 420 -> pt 0
        XCTAssertEqual(layout.nearestHourAtPt(0.0), 7 * 60)
        // 15:29 is minute 929 -> minuteToPt(929) -> nearest 15:00 (900)
        XCTAssertEqual(layout.nearestHourAtPt(layout.minuteToPt(15 * 60 + 29)), 15 * 60)
        // 15:30 is minute 930 -> minuteToPt(930) -> nearest 16:00 (960)
        XCTAssertEqual(layout.nearestHourAtPt(layout.minuteToPt(15 * 60 + 30)), 16 * 60)
        // 16:00 is minute 960 -> minuteToPt(960) -> nearest 16:00 (960)
        XCTAssertEqual(layout.nearestHourAtPt(layout.minuteToPt(16 * 60)), 16 * 60)
        // 16:29 is minute 989 -> minuteToPt(989) -> nearest 16:00 (960)
        XCTAssertEqual(layout.nearestHourAtPt(layout.minuteToPt(16 * 60 + 29)), 16 * 60)
        // 23:45 is minute 1425 -> clamped to 23:00 (1380)
        XCTAssertEqual(layout.nearestHourAtPt(layout.minuteToPt(23 * 60 + 45)), 23 * 60)
    }
}
