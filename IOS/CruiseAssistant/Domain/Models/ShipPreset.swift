import Foundation

/// ISO-8601 Day of week representation (Monday = 1, Sunday = 7).
public enum DayOfWeek: Int, Codable, CaseIterable, Sendable, Comparable {
    case monday = 1
    case tuesday = 2
    case wednesday = 3
    case thursday = 4
    case friday = 5
    case saturday = 6
    case sunday = 7

    public static func < (lhs: DayOfWeek, rhs: DayOfWeek) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}

extension SimpleDate {
    /// Day of week for this date. (1970-01-01 was Thursday, epochDay = 0).
    public var dayOfWeek: DayOfWeek {
        let weekday = ((epochDay + 3) % 7 + 7) % 7 + 1
        return DayOfWeek(rawValue: weekday) ?? .monday
    }
}

/// Returns the date of the next occurrence of `target` on or after `today`.
/// If `today` itself is the target day-of-week, returns `today`.
public func nextOccurrenceOfDayOfWeek(target: DayOfWeek, today: SimpleDate) -> SimpleDate {
    let diff = (target.rawValue - today.dayOfWeek.rawValue + 7) % 7
    return today.plusDays(diff)
}

/// A pre-defined voyage schedule pattern for a ship.
public struct SchedulePreset: Hashable, Identifiable, Codable, Sendable {
    public var id: String { label }
    public let label: String
    public let nights: Int
    public let boardDayOfWeek: DayOfWeek
    public let disembarkDayOfWeek: DayOfWeek
    public let boardTime: SimpleTime
    public let disembarkTime: SimpleTime

    public init(
        label: String,
        nights: Int,
        boardDayOfWeek: DayOfWeek,
        disembarkDayOfWeek: DayOfWeek,
        boardTime: SimpleTime = SimpleTime(hour: 12, minute: 0),
        disembarkTime: SimpleTime = SimpleTime(hour: 7, minute: 0)
    ) {
        self.label = label
        self.nights = nights
        self.boardDayOfWeek = boardDayOfWeek
        self.disembarkDayOfWeek = disembarkDayOfWeek
        self.boardTime = boardTime
        self.disembarkTime = disembarkTime
    }

    /// Compute board date as the next occurrence of boardDayOfWeek from `today` (inclusive).
    public func boardDateFrom(today: SimpleDate) -> SimpleDate {
        nextOccurrenceOfDayOfWeek(target: boardDayOfWeek, today: today)
    }

    /// Compute disembark date = board date + nights.
    public func disembarkDateFrom(today: SimpleDate) -> SimpleDate {
        boardDateFrom(today: today).plusDays(nights)
    }
}

/// Built-in cruise ship model with supported deck limits and preset schedules.
public struct ShipPreset: Hashable, Identifiable, Codable, Sendable {
    public var id: String { name }
    public let name: String
    public let schedules: [SchedulePreset]
    /// Highest guest-accessible deck (nil means unlocked/free input).
    public let maxDeck: Int?
    /// Lowest guest-accessible deck (e.g. 5 for Disney Adventure).
    public let minDeck: Int?

    public init(
        name: String,
        schedules: [SchedulePreset],
        maxDeck: Int? = nil,
        minDeck: Int? = nil
    ) {
        self.name = name
        self.schedules = schedules
        self.maxDeck = maxDeck
        self.minDeck = minDeck
    }
}

/// Boarding time slot options: 11:45 AM to 15:15 PM, 15 minutes each (15 slots total).
public func boardingSlots() -> [SimpleTime] {
    let startMin = 11 * 60 + 45 // 11:45
    let endMin = 15 * 60 + 15   // 15:15
    var slots: [SimpleTime] = []
    var m = startMin
    while m <= endMin {
        slots.append(SimpleTime(hour: m / 60, minute: m % 60))
        m += 15
    }
    return slots
}

/// Built-in ship configurations.
public let BUILT_IN_SHIPS: [ShipPreset] = [
    ShipPreset(
        name: "Disney Adventure",
        schedules: [
            SchedulePreset(
                label: "四天三夜",
                nights: 3,
                boardDayOfWeek: .monday,
                disembarkDayOfWeek: .thursday,
                boardTime: SimpleTime(hour: 12, minute: 0),
                disembarkTime: SimpleTime(hour: 7, minute: 0)
            ),
            SchedulePreset(
                label: "五天四夜",
                nights: 4,
                boardDayOfWeek: .thursday,
                disembarkDayOfWeek: .monday,
                boardTime: SimpleTime(hour: 12, minute: 0),
                disembarkTime: SimpleTime(hour: 7, minute: 0)
            ),
        ],
        maxDeck: 19,
        minDeck: 5
    ),
]
