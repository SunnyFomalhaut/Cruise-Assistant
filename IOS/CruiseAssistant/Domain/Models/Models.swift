import Foundation

/// Number of lanes in the timeline layout.
public let LANE_COUNT: Int = 3

/// Cruise ship physical zone.
public enum ShipZone: String, Codable, CaseIterable, Sendable {
    case bow = "BOW"
    case mid = "MID"
    case stern = "STERN"
    public static let BOW = ShipZone.bow
    public static let MID = ShipZone.mid
    public static let STERN = ShipZone.stern

    public var label: String {
        switch self {
        case .bow: return "船头"
        case .mid: return "船中"
        case .stern: return "船尾"
        }
    }

    public var displayName: String {
        label
    }
}

/// A lightweight, calendar-independent date representation (Year-Month-Day).
public struct SimpleDate: Hashable, Comparable, Codable, CustomStringConvertible, Sendable {
    public let year: Int
    public let month: Int
    public let day: Int

    public init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    /// Construct a date from days since Unix epoch (1970-01-01).
    public init(epochDay: Int) {
        let z = epochDay + 719468
        let era = (z >= 0 ? z : z - 146096) / 146097
        let doe = z - era * 146097
        let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        let y = yoe + era * 400
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        self.year = y + (m <= 2 ? 1 : 0)
        self.month = m
        self.day = d
    }

    /// Construct a date from a "YYYY-MM-DD" formatted string.
    public init?(_ string: String) {
        let parts = string.split(separator: "-")
        guard parts.count == 3,
              let y = Int(parts[0]),
              let m = Int(parts[1]),
              let d = Int(parts[2]),
              m >= 1 && m <= 12,
              d >= 1 && d <= 31 else {
            return nil
        }
        self.year = y
        self.month = m
        self.day = d
    }

    public init?(from string: String) {
        self.init(string)
    }

    /// Days since Unix epoch (1970-01-01).
    public var epochDay: Int {
        var y = year
        var m = month
        if m <= 2 {
            y -= 1
            m += 12
        }
        let era = (y >= 0 ? y : y - 399) / 400
        let yoe = y - era * 400
        let doy = (153 * (m - 3) + 2) / 5 + day - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    public func plusDays(_ days: Int) -> SimpleDate {
        SimpleDate(epochDay: self.epochDay + days)
    }

    public func daysBetween(_ other: SimpleDate) -> Int {
        other.epochDay - self.epochDay
    }

    public func isBefore(_ other: SimpleDate) -> Bool {
        self < other
    }

    public func isAfter(_ other: SimpleDate) -> Bool {
        self > other
    }

    public var description: String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }

    public static func < (lhs: SimpleDate, rhs: SimpleDate) -> Bool {
        if lhs.year != rhs.year { return lhs.year < rhs.year }
        if lhs.month != rhs.month { return lhs.month < rhs.month }
        return lhs.day < rhs.day
    }

    // Codable support using formatted string
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let str = try container.decode(String.self)
        guard let date = SimpleDate(str) else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Invalid date format: \(str), expected YYYY-MM-DD"
            )
        }
        self = date
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(description)
    }
}

/// A lightweight time representation (Hour:Minute).
public struct SimpleTime: Hashable, Comparable, Codable, CustomStringConvertible, Sendable {
    public let hour: Int
    public let minute: Int

    public init(hour: Int, minute: Int) {
        self.hour = max(0, min(23, hour))
        self.minute = max(0, min(59, minute))
    }

    public init(totalMinutes: Int) {
        let normalized = ((totalMinutes % 1440) + 1440) % 1440
        self.hour = normalized / 60
        self.minute = normalized % 60
    }

    public init?(_ string: String) {
        let parts = string.split(separator: ":")
        guard parts.count == 2,
              let h = Int(parts[0]),
              let m = Int(parts[1]),
              h >= 0 && h <= 23,
              m >= 0 && m <= 59 else {
            return nil
        }
        self.hour = h
        self.minute = m
    }

    public init?(from string: String) {
        self.init(string)
    }

    public var totalMinutes: Int {
        hour * 60 + minute
    }

    public func plusMinutes(_ minutes: Int) -> SimpleTime {
        SimpleTime(totalMinutes: totalMinutes + minutes)
    }

    public var description: String {
        String(format: "%02d:%02d", hour, minute)
    }

    public static func < (lhs: SimpleTime, rhs: SimpleTime) -> Bool {
        lhs.totalMinutes < rhs.totalMinutes
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let str = try container.decode(String.self)
        guard let time = SimpleTime(str) else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Invalid time format: \(str), expected HH:mm"
            )
        }
        self = time
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(description)
    }
}

/// Voyage configuration and metadata.
public struct Voyage: Identifiable, Codable, Hashable, Sendable {
    public var id: Int
    public var shipName: String?
    public var boardDate: SimpleDate
    public var boardTime: SimpleTime
    public var disembarkDate: SimpleDate
    public var disembarkTime: SimpleTime
    public var maxDeck: Int
    public var minDeck: Int
    public var isConcierge: Bool
    public var legs: [Int]
    public var ashoreLegs: Set<Int>

    public init(
        id: Int = 1,
        shipName: String? = nil,
        boardDate: SimpleDate,
        boardTime: SimpleTime,
        disembarkDate: SimpleDate,
        disembarkTime: SimpleTime = SimpleTime(hour: 7, minute: 0),
        maxDeck: Int = 20,
        minDeck: Int = 1,
        isConcierge: Bool = false,
        legs: [Int] = [],
        ashoreLegs: Set<Int> = []
    ) {
        self.id = id
        self.shipName = shipName
        self.boardDate = boardDate
        self.boardTime = boardTime
        self.disembarkDate = disembarkDate
        self.disembarkTime = disembarkTime
        self.maxDeck = maxDeck
        self.minDeck = minDeck
        self.isConcierge = isConcierge
        self.legs = legs
        self.ashoreLegs = ashoreLegs
    }

    private enum CodingKeys: String, CodingKey {
        case id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck, minDeck, isConcierge, legs, ashoreLegs
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try container.decodeIfPresent(Int.self, forKey: .id) ?? 1
        self.shipName = try container.decodeIfPresent(String.self, forKey: .shipName)
        self.boardDate = try container.decode(SimpleDate.self, forKey: .boardDate)
        self.boardTime = try container.decode(SimpleTime.self, forKey: .boardTime)
        self.disembarkDate = try container.decode(SimpleDate.self, forKey: .disembarkDate)
        self.disembarkTime = try container.decodeIfPresent(SimpleTime.self, forKey: .disembarkTime) ?? SimpleTime(hour: 7, minute: 0)
        self.maxDeck = try container.decodeIfPresent(Int.self, forKey: .maxDeck) ?? 20
        self.minDeck = try container.decodeIfPresent(Int.self, forKey: .minDeck) ?? 1
        self.isConcierge = try container.decodeIfPresent(Bool.self, forKey: .isConcierge) ?? false
        self.legs = try container.decodeIfPresent([Int].self, forKey: .legs) ?? []
        self.ashoreLegs = try container.decodeIfPresent(Set<Int>.self, forKey: .ashoreLegs) ?? []
    }

    /// Total number of days in the voyage, inclusive of embarkation and disembarkation days.
    public var dayCount: Int {
        max(1, disembarkDate.epochDay - boardDate.epochDay + 1)
    }

    /// All dates belonging to this voyage in order.
    public func datesInVoyage() -> [SimpleDate] {
        (0..<dayCount).map { boardDate.plusDays($0) }
    }

    /// Boarding date of each leg in a multi-leg voyage. Single-leg returns `[boardDate]`.
    public func legBoardDates() -> [SimpleDate] {
        if legs.isEmpty { return [boardDate] }
        var result: [SimpleDate] = []
        var cursor = boardDate
        for nights in legs {
            result.append(cursor)
            cursor = cursor.plusDays(nights)
        }
        return result
    }

    /// Turnaround/junction dates between consecutive legs (empty for single-leg voyages).
    public func junctionDates() -> [SimpleDate] {
        if legs.count <= 1 { return [] }
        var result: [SimpleDate] = []
        var cursor = boardDate
        for nights in legs.dropLast() {
            cursor = cursor.plusDays(nights)
            result.append(cursor)
        }
        return result
    }

    /// Checks if a date falls within the voyage range.
    public func contains(date: SimpleDate) -> Bool {
        date >= boardDate && date <= disembarkDate
    }

    public func containsDate(_ date: SimpleDate) -> Bool {
        contains(date: date)
    }

    /// 1-based day index of the date within the voyage; returns nil if outside.
    public func dayIndexOf(date: SimpleDate) -> Int? {
        guard contains(date: date) else { return nil }
        return date.epochDay - boardDate.epochDay + 1
    }

    /// Clamps an arbitrary date to the voyage range.
    public func clamp(date: SimpleDate) -> SimpleDate {
        if date < boardDate { return boardDate }
        if date > disembarkDate { return disembarkDate }
        return date
    }

    public func clampDate(_ date: SimpleDate) -> SimpleDate {
        clamp(date: date)
    }
}

/// An individual scheduled event on the cruise.
public struct CruiseEvent: Identifiable, Codable, Hashable, Sendable {
    public var id: Int64
    public var date: SimpleDate
    public var startTime: SimpleTime
    public var durationMinutes: Int
    public var categoryId: Int64
    public var title: String
    public var deck: Int?
    public var zone: ShipZone?
    public var placeName: String?
    public var lane: Int
    public var recurrenceGroupId: String?
    public var reminderMinutesBefore: Int?
    public var isSystemGenerated: Bool
    public var isActive: Bool
    public var presetKey: String?
    public var waitMinutes: Int?
    public var note: String?

    public init(
        id: Int64 = 0,
        date: SimpleDate,
        startTime: SimpleTime,
        durationMinutes: Int,
        categoryId: Int64,
        title: String,
        deck: Int? = nil,
        zone: ShipZone? = nil,
        placeName: String? = nil,
        lane: Int = 0,
        recurrenceGroupId: String? = nil,
        reminderMinutesBefore: Int? = nil,
        isSystemGenerated: Bool = false,
        isActive: Bool = true,
        presetKey: String? = nil,
        waitMinutes: Int? = nil,
        note: String? = nil
    ) {
        self.id = id
        self.date = date
        self.startTime = startTime
        self.durationMinutes = durationMinutes
        self.categoryId = categoryId
        self.title = title
        self.deck = deck
        self.zone = zone
        self.placeName = placeName
        self.lane = lane
        self.recurrenceGroupId = recurrenceGroupId
        self.reminderMinutesBefore = reminderMinutesBefore
        self.isSystemGenerated = isSystemGenerated
        self.isActive = isActive
        self.presetKey = presetKey
        self.waitMinutes = waitMinutes
        self.note = note
    }

    public var startMinute: Int {
        startTime.totalMinutes
    }

    public var endMinute: Int {
        startMinute + durationMinutes
    }

    public var endTime: SimpleTime {
        SimpleTime(totalMinutes: endMinute)
    }
}

/// Daily plan finalization status.
public struct DayPlan: Identifiable, Codable, Hashable, Sendable {
    public var date: SimpleDate
    public var finalized: Bool
    public var finalizedAt: Int64?

    public var id: SimpleDate { date }

    public init(
        date: SimpleDate,
        finalized: Bool = false,
        finalizedAt: Int64? = nil
    ) {
        self.date = date
        self.finalized = finalized
        self.finalizedAt = finalizedAt
    }
}

/// Saved user location preset.
public struct LocationPreset: Identifiable, Codable, Hashable, Sendable {
    public var id: Int64
    public var deck: Int
    public var zone: ShipZone
    public var placeName: String
    public var lastUsedEpochMs: Int64
    public var useCount: Int

    public init(
        id: Int64 = 0,
        deck: Int,
        zone: ShipZone,
        placeName: String,
        lastUsedEpochMs: Int64 = 0,
        useCount: Int = 0
    ) {
        self.id = id
        self.deck = deck
        self.zone = zone
        self.placeName = placeName
        self.lastUsedEpochMs = lastUsedEpochMs
        self.useCount = useCount
    }
}

/// Direction of Pixie Dust gift exchange.
public enum PixieDustDirection: String, Codable, CaseIterable, Sendable {
    case given = "GIVEN"
    case received = "RECEIVED"
}

/// A gift exchange record.
public struct PixieDustRecord: Identifiable, Codable, Hashable, Sendable {
    public var id: Int64
    public var direction: PixieDustDirection
    public var deck: Int
    public var room: String
    public var levelId: Int64
    public var levelNameSnapshot: String
    public var replied: Bool
    public var note: String
    public var timestamp: Int64

    public init(
        id: Int64 = 0,
        direction: PixieDustDirection,
        deck: Int,
        room: String,
        levelId: Int64,
        levelNameSnapshot: String,
        replied: Bool = false,
        note: String = "",
        timestamp: Int64 = 0
    ) {
        self.id = id
        self.direction = direction
        self.deck = deck
        self.room = room
        self.levelId = levelId
        self.levelNameSnapshot = levelNameSnapshot
        self.replied = replied
        self.note = note
        self.timestamp = timestamp
    }

    /// Formatted stateroom number e.g. "11550".
    public var fullRoom: String {
        "\(deck)\(room)"
    }
}

/// Gift tier/level.
public struct PixieLevel: Identifiable, Codable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var colorArgb: Int64
    public var iconKey: String
    public var rank: Int
    public var sortOrder: Int

    public init(
        id: Int64 = 0,
        name: String,
        colorArgb: Int64,
        iconKey: String,
        rank: Int,
        sortOrder: Int = 0
    ) {
        self.id = id
        self.name = name
        self.colorArgb = colorArgb
        self.iconKey = iconKey
        self.rank = rank
        self.sortOrder = sortOrder
    }

    public static let BUILT_IN_PIXIE_LEVELS: [PixieLevel] = [
        PixieLevel(id: 1, name: "普通", colorArgb: 0xFF9E9E9E, iconKey: "gift", rank: 1, sortOrder: 0),
        PixieLevel(id: 2, name: "精致", colorArgb: 0xFF5B8DEF, iconKey: "sparkle", rank: 2, sortOrder: 1),
        PixieLevel(id: 3, name: "豪华", colorArgb: 0xFFF2C94C, iconKey: "diamond", rank: 3, sortOrder: 2)
    ]
}

/// An unscheduled / flexible checklist item.
public struct WishlistItem: Identifiable, Codable, Hashable, Sendable {
    public var id: Int64
    public var title: String
    public var categoryId: Int64
    public var deck: Int?
    public var zone: ShipZone?
    public var placeName: String?
    public var note: String?
    public var isCompleted: Bool
    public var createdAt: Int64

    public init(
        id: Int64 = 0,
        title: String,
        categoryId: Int64,
        deck: Int? = nil,
        zone: ShipZone? = nil,
        placeName: String? = nil,
        note: String? = nil,
        isCompleted: Bool = false,
        createdAt: Int64 = 0
    ) {
        self.id = id
        self.title = title
        self.categoryId = categoryId
        self.deck = deck
        self.zone = zone
        self.placeName = placeName
        self.note = note
        self.isCompleted = isCompleted
        self.createdAt = createdAt
    }
}

/// Snapshot of a full voyage archive (matches Android `ArchiveSnapshot`).
public struct ArchiveSnapshot: Codable, Hashable, Sendable {
    public var version: Int
    public var voyage: Voyage
    public var categories: [Category]
    public var events: [CruiseEvent]
    public var dayPlans: [DayPlan]
    public var pixieLevels: [PixieLevel]
    public var pixieDust: [PixieDustRecord]
    public var wishlist: [WishlistItem]

    public init(
        version: Int = 1,
        voyage: Voyage,
        categories: [Category] = [],
        events: [CruiseEvent] = [],
        dayPlans: [DayPlan] = [],
        pixieLevels: [PixieLevel] = [],
        pixieDust: [PixieDustRecord] = [],
        wishlist: [WishlistItem] = []
    ) {
        self.version = version
        self.voyage = voyage
        self.categories = categories
        self.events = events
        self.dayPlans = dayPlans
        self.pixieLevels = pixieLevels
        self.pixieDust = pixieDust
        self.wishlist = wishlist
    }

    private enum CodingKeys: String, CodingKey {
        case version, voyage, categories, events, dayPlans, pixieLevels, pixieDust, wishlist
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.version = try container.decodeIfPresent(Int.self, forKey: .version) ?? 1
        self.voyage = try container.decode(Voyage.self, forKey: .voyage)
        self.categories = try container.decodeIfPresent([Category].self, forKey: .categories) ?? []
        self.events = try container.decodeIfPresent([CruiseEvent].self, forKey: .events) ?? []
        self.dayPlans = try container.decodeIfPresent([DayPlan].self, forKey: .dayPlans) ?? []
        self.pixieLevels = try container.decodeIfPresent([PixieLevel].self, forKey: .pixieLevels) ?? []
        self.pixieDust = try container.decodeIfPresent([PixieDustRecord].self, forKey: .pixieDust) ?? []
        self.wishlist = try container.decodeIfPresent([WishlistItem].self, forKey: .wishlist) ?? []
    }
}

/// A saved historical voyage archive entry.
public struct VoyageArchiveEntry: Identifiable, Codable, Hashable, Sendable {
    public var id: Int64
    public var shipName: String
    public var boardDate: SimpleDate
    public var disembarkDate: SimpleDate
    public var archivedAt: Int64
    public var eventCount: Int
    public var pixieGivenCount: Int
    public var pixieReceivedCount: Int
    public var snapshot: ArchiveSnapshot
    /// User-supplied archive name; optional so archives saved before this field existed still decode.
    public var customName: String?

    /// Display name: the custom name when set, otherwise the ship name.
    public var name: String {
        if let customName, !customName.isEmpty { return customName }
        return shipName
    }

    public var pixieCount: Int { pixieGivenCount + pixieReceivedCount }

    public init(
        id: Int64 = 0,
        shipName: String,
        boardDate: SimpleDate,
        disembarkDate: SimpleDate,
        archivedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        eventCount: Int,
        pixieGivenCount: Int,
        pixieReceivedCount: Int,
        snapshot: ArchiveSnapshot,
        customName: String? = nil
    ) {
        self.id = id
        self.shipName = shipName
        self.boardDate = boardDate
        self.disembarkDate = disembarkDate
        self.archivedAt = archivedAt
        self.eventCount = eventCount
        self.pixieGivenCount = pixieGivenCount
        self.pixieReceivedCount = pixieReceivedCount
        self.snapshot = snapshot
        self.customName = customName
    }
}

public func serializeArchiveSnapshot(_ snapshot: ArchiveSnapshot) -> String {
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
    guard let data = try? encoder.encode(snapshot),
          let str = String(data: data, encoding: .utf8) else {
        return "{}"
    }
    return str
}

public func parseArchiveSnapshot(_ json: String) -> ArchiveSnapshot? {
    guard let data = json.data(using: .utf8) else { return nil }
    return try? JSONDecoder().decode(ArchiveSnapshot.self, from: data)
}

// MARK: - Pixie Dust Domain Utilities

public struct LevelCount: Hashable, Codable, Sendable {
    public let level: PixieLevel
    public let count: Int

    public init(level: PixieLevel, count: Int) {
        self.level = level
        self.count = count
    }
}

public struct RoomGroup: Identifiable, Hashable, Codable, Sendable {
    public let deck: Int
    public let room: String
    public let lastTimestamp: Int64
    public let total: Int
    public let levelCounts: [LevelCount]

    public var id: String { "\(deck)-\(room)" }
    public var fullRoom: String {
        room.hasPrefix("\(deck)") && room.count >= 4 ? room : "\(deck)\(room)"
    }

    public init(deck: Int, room: String, lastTimestamp: Int64, total: Int, levelCounts: [LevelCount]) {
        self.deck = deck
        self.room = room
        self.lastTimestamp = lastTimestamp
        self.total = total
        self.levelCounts = levelCounts
    }
}

public func isValidRoomInput(_ input: String) -> Bool {
    input.count == 3 && input.allSatisfy { $0.isNumber }
}

private let DELETED_LEVEL_COLOR: Int64 = 0xFF9E9E9E

public func levelCountsOf(records: [PixieDustRecord], levels: [PixieLevel]) -> [LevelCount] {
    let byId = Dictionary(uniqueKeysWithValues: levels.map { ($0.id, $0) })
    let grouped = Dictionary(grouping: records, by: { $0.levelId })
    return grouped.map { id, list in
        let level = byId[id] ?? PixieLevel(
            id: id,
            name: list.first?.levelNameSnapshot ?? "",
            colorArgb: DELETED_LEVEL_COLOR,
            iconKey: "gift",
            rank: 0
        )
        return LevelCount(level: level, count: list.count)
    }.sorted { $0.level.rank > $1.level.rank }
}

public func groupGivenByRoom(records: [PixieDustRecord], levels: [PixieLevel]) -> [RoomGroup] {
    let given = records.filter { $0.direction == .given }
    struct RoomKey: Hashable {
        let deck: Int
        let room: String
    }
    let grouped = Dictionary(grouping: given, by: { RoomKey(deck: $0.deck, room: $0.room) })
    return grouped.map { key, list in
        let lastTs = list.map(\.timestamp).max() ?? 0
        return RoomGroup(
            deck: key.deck,
            room: key.room,
            lastTimestamp: lastTs,
            total: list.count,
            levelCounts: levelCountsOf(records: list, levels: levels)
        )
    }.sorted { a, b in
        if a.deck != b.deck { return a.deck < b.deck }
        let roomA = Int(a.room) ?? Int.max
        let roomB = Int(b.room) ?? Int.max
        if roomA != roomB { return roomA < roomB }
        return a.room < b.room
    }
}

public func sortReceived(records: [PixieDustRecord], levels: [PixieLevel]) -> [PixieDustRecord] {
    let rankById = Dictionary(uniqueKeysWithValues: levels.map { ($0.id, $0.rank) })
    let unreplied = records
        .filter { $0.direction == .received && !$0.replied }
        .sorted {
            let r0 = rankById[$0.levelId] ?? 0
            let r1 = rankById[$1.levelId] ?? 0
            if r0 != r1 { return r0 > r1 }
            return $0.timestamp > $1.timestamp
        }
    let replied = records
        .filter { $0.direction == .received && $0.replied }
        .sorted { $0.timestamp > $1.timestamp }
    return unreplied + replied
}

/// Sweep delivery sorting: Deck ascending -> Room ascending -> Unreplied first -> Rank descending -> Timestamp descending.
public func sortForSweepDelivery(records: [PixieDustRecord], levels: [PixieLevel]) -> [PixieDustRecord] {
    let rankById = Dictionary(uniqueKeysWithValues: levels.map { ($0.id, $0.rank) })
    return records
        .filter { $0.direction == .received }
        .sorted { a, b in
            if a.deck != b.deck { return a.deck < b.deck }
            let roomA = Int(a.room) ?? Int.max
            let roomB = Int(b.room) ?? Int.max
            if roomA != roomB { return roomA < roomB }
            if a.room != b.room { return a.room < b.room }
            if a.replied != b.replied { return !a.replied && b.replied }
            let rankA = rankById[a.levelId] ?? 0
            let rankB = rankById[b.levelId] ?? 0
            if rankA != rankB { return rankA > rankB }
            return a.timestamp > b.timestamp
        }
}

public func lastLevelForRoom(records: [PixieDustRecord], deck: Int, room: String) -> PixieDustRecord? {
    records
        .filter { $0.direction == .given && $0.deck == deck && $0.room == room }
        .max(by: { $0.timestamp < $1.timestamp })
}

/// Formats a cruise location (e.g. "Deck 7 船中 花园"). When `compact` is true, uses "D7" prefix.
/// Avoids duplicating "Deck X" if `placeName` already starts with "Deck ".
public func formatLocation(
    deck: Int?,
    zone: ShipZone?,
    placeName: String?,
    compact: Bool = false
) -> String? {
    guard let deck = deck else { return nil }
    let cleanPlace = placeName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    let placeAlreadyHasDeck = cleanPlace.lowercased().hasPrefix("deck ")
    var parts: [String] = []
    if !placeAlreadyHasDeck {
        parts.append(compact ? "D\(deck)" : "Deck \(deck)")
    }
    if let zone = zone {
        parts.append(zone.label)
    }
    if !cleanPlace.isEmpty {
        parts.append(cleanPlace)
    }
    if parts.isEmpty {
        parts.append(compact ? "D\(deck)" : "Deck \(deck)")
    }
    return parts.joined(separator: " ")
}
