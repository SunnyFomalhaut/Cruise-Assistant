import Foundation

/// Half-open interval overlap check: [aStart, aEnd) and [bStart, bEnd).
/// Adjacent intervals (one's end equals another's start) do NOT overlap.
/// Empty intervals (start == end, such as zero-duration events) do NOT overlap anything.
public func overlaps(_ aStart: Int, _ aEnd: Int, _ bStart: Int, _ bEnd: Int) -> Bool {
    aStart < aEnd && bStart < bEnd && aStart < bEnd && bStart < aEnd
}

/// Assigns a lane for a new or updated event.
/// Tries lane 0 -> 1 -> 2 in order, returning the first lane with no overlapping events.
/// If all lanes have conflicts, returns the last lane (LANE_COUNT - 1) as overflow.
///
/// - Parameters:
///   - startMinute: Start minute of the event (inclusive).
///   - endMinute: End minute of the event (exclusive).
///   - existing: List of existing events on the same day.
///   - excludeId: Optional event ID to exclude (useful when editing an existing event).
///   - rangeOf: Optional custom extractor for an event's time interval (defaults to startMinute..<endMinute).
/// - Returns: Assigned lane index (0, 1, or 2).
public func assignLane(
    startMinute: Int,
    endMinute: Int,
    existing: [CruiseEvent],
    excludeId: Int64? = nil,
    rangeOf: ((CruiseEvent) -> (start: Int, end: Int))? = nil
) -> Int {
    let getRange = rangeOf ?? { ($0.startMinute, $0.endMinute) }
    let candidates = existing.filter { $0.id != excludeId }
    for lane in 0..<LANE_COUNT {
        let busy = candidates.contains { other in
            let range = getRange(other)
            return other.lane == lane && overlaps(startMinute, endMinute, range.start, range.end)
        }
        if !busy {
            return lane
        }
    }
    return LANE_COUNT - 1
}

/// Preserves the preferred lane if it is free from conflicts among existing events;
/// otherwise falls back to standard `assignLane` (0 -> 1 -> 2 search).
///
/// - Parameters:
///   - startMinute: Start minute of the event (inclusive).
///   - endMinute: End minute of the event (exclusive).
///   - existing: List of existing events on the same day.
///   - preferredLane: The original or preferred lane index to preserve if possible.
///   - excludeId: Optional event ID to exclude from conflict checks.
///   - rangeOf: Optional custom extractor for an event's time interval.
/// - Returns: Assigned lane index.
public func assignLanePreserving(
    startMinute: Int,
    endMinute: Int,
    existing: [CruiseEvent],
    preferredLane: Int,
    excludeId: Int64? = nil,
    rangeOf: ((CruiseEvent) -> (start: Int, end: Int))? = nil
) -> Int {
    let getRange = rangeOf ?? { ($0.startMinute, $0.endMinute) }
    let candidates = existing.filter { $0.id != excludeId }
    let preferredBusy = candidates.contains { other in
        let range = getRange(other)
        return other.lane == preferredLane && overlaps(startMinute, endMinute, range.start, range.end)
    }
    if !preferredBusy {
        return preferredLane
    } else {
        return assignLane(
            startMinute: startMinute,
            endMinute: endMinute,
            existing: existing,
            excludeId: excludeId,
            rangeOf: rangeOf
        )
    }
}

/// Assigns a lane for preset show generation.
/// Priority order: Lane 1 -> Lane 2 -> Lane 0.
///
/// - Parameters:
///   - startMinute: Start minute of the show (effective start including lead-in).
///   - endMinute: End minute of the show (effective end including buffer).
///   - occupied: List of already assigned show intervals `(lane, start, end)`.
/// - Returns: Chosen lane index (1, 2, or fallback 0).
public func assignShowLane(
    startMinute: Int,
    endMinute: Int,
    occupied: [(lane: Int, start: Int, end: Int)]
) -> Int {
    for lane in [1, 2, 0] {
        let busy = occupied.contains { item in
            item.lane == lane && overlaps(startMinute, endMinute, item.start, item.end)
        }
        if !busy {
            return lane
        }
    }
    return 0
}
