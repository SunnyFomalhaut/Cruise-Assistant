import Foundation

/// Fixed height in points for collapsed empty timeline gaps.
public let COLLAPSED_SEGMENT_PT: Double = 28.0

/// Threshold in minutes for a gap to be collapsible.
public let MIN_COLLAPSIBLE_GAP_MINUTES: Int = 90

/// Padding in minutes preserved before and after each event when collapsing.
public let EVENT_PADDING_MINUTES: Int = 30

/// Minimum timeline range span in minutes (2 hours).
public let MIN_RANGE_MINUTES: Int = 120

/// Default show waiting duration before start time.
public let SHOW_WAIT_MINUTES: Int = 30

/// Resolves the default wait minutes for show arrival:
/// - Deck 6 Walt Disney Theatre (or any Disney Theatre show): 45 min
/// - Lion King Fireworks: 45 min
/// - Baymax Cinema: 15 min
/// - Garden Stage: 15 min
/// - Wayfinder Bay: 30 min
/// - Fallback: 30 min
public func resolveDefaultWaitMinutes(
    placeName: String? = nil,
    deck: Int? = nil,
    title: String? = nil,
    presetKey: String? = nil
) -> Int {
    let text = "\(placeName ?? "") \(title ?? "") \(presetKey ?? "")"
    if text.contains("迪士尼剧院") || text.contains("Disney Theatre") || text.contains("Disney Theater") || (deck == 6 && text.contains("剧院")) {
        return 45
    }
    if text.contains("狮子王") || text.contains("Lion King") || text.contains("show_lion_king") {
        return 45
    }
    if text.contains("大白剧院") || text.contains("Baymax Cinema") || text.contains("Baymax Cinemas") {
        return 15
    }
    if text.contains("花园舞台") || text.contains("Garden Stage") {
        return 15
    }
    if text.contains("寻路者湾") || text.contains("Wayfinder Bay") {
        return 30
    }
    return 30
}

extension CruiseEvent {
    /// Effective wait minutes before show start; uses custom waitMinutes if set, otherwise derives from venue/preset.
    public var effectiveWaitMinutes: Int {
        waitMinutes ?? resolveDefaultWaitMinutes(placeName: placeName, deck: deck, title: title, presetKey: presetKey)
    }
}

/// Effective range of an event in minutes: [start, end).
/// Show category events extend earlier by effectiveWaitMinutes to account for arrival time.
public func showEffectiveRange(_ event: CruiseEvent, isShow: Bool) -> (start: Int, end: Int) {
    if isShow {
        return (event.startMinute - event.effectiveWaitMinutes, event.endMinute)
    } else {
        return (event.startMinute, event.endMinute)
    }
}

/// Constructs a closure mapping an event to its effective time range based on show category IDs.
public func showRangeOf(_ showCategoryIds: Set<Int64>) -> (CruiseEvent) -> (start: Int, end: Int) {
    return { event in
        showEffectiveRange(event, isShow: showCategoryIds.contains(event.categoryId))
    }
}

/// A segment of the timeline. Collapsed segments have a fixed height; uncollapsed segments scale proportionally.
public struct Segment: Hashable, Sendable {
    public let startMinute: Int
    public let endMinute: Int
    public let topPt: Double
    public let heightPt: Double
    public let collapsed: Bool

    public init(
        startMinute: Int,
        endMinute: Int,
        topPt: Double,
        heightPt: Double,
        collapsed: Bool
    ) {
        self.startMinute = startMinute
        self.endMinute = endMinute
        self.topPt = topPt
        self.heightPt = heightPt
        self.collapsed = collapsed
    }

    public var spanMinutes: Int {
        endMinute - startMinute
    }
}

/// Computed timeline layout containing segments, total height, and coordinate conversions.
public struct TimelineLayout: Hashable, Sendable {
    public let hourHeightPt: Double
    public let rangeStartMinute: Int
    public let rangeEndMinute: Int
    public let segments: [Segment]
    public let totalHeightPt: Double
    public let hiddenBefore: Int
    public let hiddenAfter: Int

    public init(
        hourHeightPt: Double,
        rangeStartMinute: Int,
        rangeEndMinute: Int,
        segments: [Segment],
        totalHeightPt: Double,
        hiddenBefore: Int,
        hiddenAfter: Int
    ) {
        self.hourHeightPt = hourHeightPt
        self.rangeStartMinute = rangeStartMinute
        self.rangeEndMinute = rangeEndMinute
        self.segments = segments
        self.totalHeightPt = totalHeightPt
        self.hiddenBefore = hiddenBefore
        self.hiddenAfter = hiddenAfter
    }

    /// Maps a minute from start of day to a vertical point offset.
    /// Clamps to 0 or totalHeightPt outside the range. Interpolates linearly within segments.
    public func minuteToPt(_ minute: Int) -> Double {
        if minute <= rangeStartMinute {
            return 0.0
        }
        if minute >= rangeEndMinute {
            return totalHeightPt
        }
        guard let seg = segments.first(where: { minute < $0.endMinute }) else {
            return totalHeightPt
        }
        if minute <= seg.startMinute {
            return seg.topPt
        }
        guard seg.spanMinutes > 0 else {
            return seg.topPt
        }
        let ratio = Double(minute - seg.startMinute) / Double(seg.spanMinutes)
        return seg.topPt + ratio * seg.heightPt
    }

    /// Inverse mapping of minuteToPt: vertical point offset to minute from start of day.
    public func ptToMinute(_ pt: Double) -> Int {
        if pt <= 0.0 {
            return rangeStartMinute
        }
        if pt >= totalHeightPt {
            return rangeEndMinute
        }
        guard let seg = segments.first(where: { pt < $0.topPt + $0.heightPt }) else {
            return rangeEndMinute
        }
        if seg.heightPt <= 0.0 {
            return seg.startMinute
        }
        let ratio = (pt - seg.topPt) / seg.heightPt
        let calculated = Int(Double(seg.startMinute) + ratio * Double(seg.spanMinutes))
        return min(max(calculated, seg.startMinute), seg.endMinute)
    }

    /// Hour ticks to render: (minute, pt, isHalfHour).
    /// Starts at the first whole hour >= rangeStartMinute. Collapsed segments are skipped.
    /// Half-hour ticks are included only when hourHeightPt >= 90.0.
    public func hourTicks() -> [(minute: Int, pt: Double, isHalfHour: Bool)] {
        let firstHour = ((rangeStartMinute + 59) / 60) * 60
        let withHalves = hourHeightPt >= 90.0
        let step = withHalves ? 30 : 60
        var ticks: [(minute: Int, pt: Double, isHalfHour: Bool)] = []
        var m = firstHour
        while m <= rangeEndMinute {
            let inCollapsed = segments.contains { seg in
                seg.collapsed && m > seg.startMinute && m < seg.endMinute
            }
            if !inCollapsed {
                let isHalf = withHalves && (m % 60 != 0)
                ticks.append((minute: m, pt: minuteToPt(m), isHalfHour: isHalf))
            }
            m += step
        }
        return ticks
    }

    /// Computes the nearest whole hour (in minutes from 00:00, 0..1380) for a given point offset.
    public func nearestHourAtPt(_ pt: Double) -> Int {
        let minute = ptToMinute(pt)
        let nearest = ((minute + 30) / 60) * 60
        return min(max(nearest, 0), 23 * 60)
    }
}

/// Constructs a TimelineLayout for the specified range, scale, and events.
///
/// - Parameters:
///   - rangeStartMinute: Start of the visible range (0..1440).
///   - rangeEndMinute: End of the visible range (0..1440).
///   - hourHeightPt: Height in points per hour of uncollapsed time.
///   - events: Day's events used to compute collapsible empty gaps and hidden counters.
///   - collapseEmptyGaps: Whether to collapse gaps >= 90 minutes.
///   - showCategoryIds: Set of category IDs representing shows (for arrival buffer expansion).
/// - Returns: A calculated `TimelineLayout`.
public func buildTimelineLayout(
    rangeStartMinute: Int,
    rangeEndMinute: Int,
    hourHeightPt: Double,
    events: [CruiseEvent],
    collapseEmptyGaps: Bool,
    showCategoryIds: Set<Int64> = []
) -> TimelineLayout {
    let start = min(max(rangeStartMinute, 0), 1440 - MIN_RANGE_MINUTES)
    let end = min(max(rangeEndMinute, start + MIN_RANGE_MINUTES), 1440)

    let rangeOf = showRangeOf(showCategoryIds)
    let hiddenBefore = events.filter { rangeOf($0).end <= start }.count
    let hiddenAfter = events.filter { rangeOf($0).start >= end }.count

    let rawSegments: [((start: Int, end: Int), collapsed: Bool)]
    if collapseEmptyGaps {
        rawSegments = buildCollapsedSpans(start: start, end: end, events: events, rangeOf: rangeOf)
    } else {
        rawSegments = [((start: start, end: end), collapsed: false)]
    }

    var cursorPt = 0.0
    var segments: [Segment] = []
    segments.reserveCapacity(rawSegments.count)

    for item in rawSegments {
        let segStart = item.0.start
        let segEnd = item.0.end
        let collapsed = item.collapsed
        let height: Double
        if collapsed {
            height = COLLAPSED_SEGMENT_PT
        } else {
            height = Double(segEnd - segStart) / 60.0 * hourHeightPt
        }
        segments.append(
            Segment(
                startMinute: segStart,
                endMinute: segEnd,
                topPt: cursorPt,
                heightPt: height,
                collapsed: collapsed
            )
        )
        cursorPt += height
    }

    return TimelineLayout(
        hourHeightPt: hourHeightPt,
        rangeStartMinute: start,
        rangeEndMinute: end,
        segments: segments,
        totalHeightPt: cursorPt,
        hiddenBefore: hiddenBefore,
        hiddenAfter: hiddenAfter
    )
}

/// Splits [start, end) into an alternating sequence of preserved and collapsed spans.
/// Preserved spans are unions of event intervals (with EVENT_PADDING_MINUTES on both sides).
/// Gaps >= MIN_COLLAPSIBLE_GAP_MINUTES between preserved spans are collapsed.
public func buildCollapsedSpans(
    start: Int,
    end: Int,
    events: [CruiseEvent],
    rangeOf: (CruiseEvent) -> (start: Int, end: Int)
) -> [((start: Int, end: Int), collapsed: Bool)] {
    let keepRanges = events
        .filter {
            let r = rangeOf($0)
            return r.end > start && r.start < end && $0.durationMinutes > 0
        }
        .map {
            let r = rangeOf($0)
            return (
                start: max(r.start - EVENT_PADDING_MINUTES, start),
                end: min(r.end + EVENT_PADDING_MINUTES, end)
            )
        }
        .sorted { $0.start < $1.start }
        .reduce(into: [(start: Int, end: Int)]()) { acc, range in
            if let last = acc.last, range.start <= last.end {
                acc[acc.count - 1] = (start: last.start, end: max(last.end, range.end))
            } else {
                acc.append(range)
            }
        }

    if keepRanges.isEmpty {
        return [((start: start, end: end), collapsed: true)]
    }

    var result: [((start: Int, end: Int), collapsed: Bool)] = []
    var cursor = start
    for keep in keepRanges {
        if keep.start > cursor {
            let gap = keep.start - cursor
            result.append(((start: cursor, end: keep.start), collapsed: gap >= MIN_COLLAPSIBLE_GAP_MINUTES))
        }
        result.append(((start: keep.start, end: keep.end), collapsed: false))
        cursor = keep.end
    }
    if cursor < end {
        let gap = end - cursor
        result.append(((start: cursor, end: end), collapsed: gap >= MIN_COLLAPSIBLE_GAP_MINUTES))
    }

    // Merge adjacent non-collapsed segments to prevent fragmentation
    var merged: [((start: Int, end: Int), collapsed: Bool)] = []
    for item in result {
        if let last = merged.last, !last.collapsed, !item.collapsed, last.0.end == item.0.start {
            merged[merged.count - 1] = ((start: last.0.start, end: item.0.end), collapsed: false)
        } else {
            merged.append(item)
        }
    }
    return merged
}

/// Finds all overlapping event pairs on the same day.
public func findConflicts(
    _ events: [CruiseEvent],
    rangeOf: (CruiseEvent) -> (start: Int, end: Int) = { ($0.startMinute, $0.endMinute) }
) -> [(first: CruiseEvent, second: CruiseEvent)] {
    let activeEvents = events.filter { $0.isActive }
    let sorted = activeEvents.sorted {
        let r1 = rangeOf($0)
        let r2 = rangeOf($1)
        if r1.start != r2.start {
            return r1.start < r2.start
        }
        return $0.id < $1.id
    }
    var result: [(first: CruiseEvent, second: CruiseEvent)] = []
    for i in 0..<sorted.count {
        let a = sorted[i]
        let aRange = rangeOf(a)
        for j in (i + 1)..<sorted.count {
            let b = sorted[j]
            let bRange = rangeOf(b)
            if bRange.start >= aRange.end {
                break
            }
            if overlaps(aRange.start, aRange.end, bRange.start, bRange.end) {
                result.append((first: a, second: b))
            }
        }
    }
    return result
}
