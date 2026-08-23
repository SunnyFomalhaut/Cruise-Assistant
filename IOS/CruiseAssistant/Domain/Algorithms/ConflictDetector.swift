import Foundation

/**
 * 需求8：找出同一天内所有时间重叠的事件对。
 * 每对只报一次，按开始时间排序后 first 早于（或同时但 id 更小）second。
 * 相邻（前一个结束 == 后一个开始）不算冲突。
 */
public func findConflicts(
    _ events: [CruiseEvent],
    rangeOf: ((CruiseEvent) -> (start: Int, end: Int))? = nil
) -> [(first: CruiseEvent, second: CruiseEvent)] {
    let getRange = rangeOf ?? { ($0.startMinute, $0.endMinute) }
    let activeEvents = events.filter { $0.isActive }
    let sorted = activeEvents.sorted {
        let r1 = getRange($0)
        let r2 = getRange($1)
        if r1.start != r2.start {
            return r1.start < r2.start
        }
        return $0.id < $1.id
    }
    var result: [(first: CruiseEvent, second: CruiseEvent)] = []
    for i in 0..<sorted.count {
        let a = sorted[i]
        let aRange = getRange(a)
        for j in (i + 1)..<sorted.count {
            let b = sorted[j]
            let bRange = getRange(b)
            // 排序后 bRange.start >= aRange.start，一旦不早于 a 的结束就不可能再重叠
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

public func findConflicts(
    events: [CruiseEvent],
    rangeOf: ((CruiseEvent) -> (start: Int, end: Int))? = nil
) -> [(first: CruiseEvent, second: CruiseEvent)] {
    findConflicts(events, rangeOf: rangeOf)
}
