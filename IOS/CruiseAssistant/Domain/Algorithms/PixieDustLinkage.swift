import Foundation

/// Room history summary shown beneath the keypad when typing a known stateroom.
public struct RoomHistory: Equatable, Hashable, Sendable {
    public let levelName: String
    public let count: Int
    public let givenCount: Int
    public let lastGivenLevelName: String?
    public let lastGivenLevelId: Int64?
    public let receivedCount: Int
    public let unrepliedCount: Int
    public let lastReceivedLevelName: String?
    public let lastReceivedLevelId: Int64?

    public init(
        levelName: String = "",
        count: Int = 0,
        givenCount: Int = 0,
        lastGivenLevelName: String? = nil,
        lastGivenLevelId: Int64? = nil,
        receivedCount: Int = 0,
        unrepliedCount: Int = 0,
        lastReceivedLevelName: String? = nil,
        lastReceivedLevelId: Int64? = nil
    ) {
        self.levelName = levelName
        self.count = count
        self.givenCount = givenCount
        self.lastGivenLevelName = lastGivenLevelName
        self.lastGivenLevelId = lastGivenLevelId
        self.receivedCount = receivedCount
        self.unrepliedCount = unrepliedCount
        self.lastReceivedLevelName = lastReceivedLevelName
        self.lastReceivedLevelId = lastReceivedLevelId
    }
}

/// Real-time stateroom validation status for Pixie Dust keypad entry.
public enum StateroomValidationStatus: String, Codable, Sendable {
    case incomplete = "INCOMPLETE"
    case valid = "VALID"
    case invalid = "INVALID"
}

/// Validation result for direct stateroom input.
public struct StateroomValidationResult: Equatable, Sendable {
    public let status: StateroomValidationStatus
    public let deck: Int?
    public let room: String?
    public let fullRoom: String?
    public let message: String?

    public init(
        status: StateroomValidationStatus,
        deck: Int? = nil,
        room: String? = nil,
        fullRoom: String? = nil,
        message: String? = nil
    ) {
        self.status = status
        self.deck = deck
        self.room = room
        self.fullRoom = fullRoom
        self.message = message
    }
}

/// Parses a 4 or 5 digit stateroom input into `(deck, room)` (e.g. `"6012"` -> `(6, "012")`, `"11216"` -> `(11, "216")`).
public func parseStateroomInput(_ input: String) -> (deck: Int, room: String)? {
    let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
    guard (4...5).contains(trimmed.count), trimmed.allSatisfy({ $0.isNumber }) else {
        return nil
    }
    let deckStr = String(trimmed.dropLast(3))
    let roomStr = String(trimmed.suffix(3))
    guard let deck = Int(deckStr), (5...19).contains(deck), deck != 14 else {
        return nil
    }
    return (deck: deck, room: roomStr)
}

/// Validates a stateroom input against `StateroomCatalog`.
public func validateStateroomInput(_ input: String) -> StateroomValidationResult {
    let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.count < 4 {
        return StateroomValidationResult(status: .incomplete)
    }
    guard let parsed = parseStateroomInput(trimmed) else {
        return StateroomValidationResult(status: .invalid, message: "无效房号格式")
    }
    let fullRoom = "\(parsed.deck)\(parsed.room)"
    var allStaterooms = StateroomCatalog.getAllStaterooms()
    if allStaterooms.isEmpty {
        _ = StateroomCatalog.loadDefault()
        allStaterooms = StateroomCatalog.getAllStaterooms()
    }
    if !allStaterooms.isEmpty {
        let exists = allStaterooms.contains { $0.roomNumber == fullRoom || $0.roomNumber == trimmed }
        if !exists {
            return StateroomValidationResult(
                status: .invalid,
                deck: parsed.deck,
                room: parsed.room,
                fullRoom: fullRoom,
                message: "地图上无此房间，请核对"
            )
        }
    }
    return StateroomValidationResult(
        status: .valid,
        deck: parsed.deck,
        room: parsed.room,
        fullRoom: fullRoom
    )
}

/// Stateroom Pixie Dust status for Deck Map overlay markers and linkage view badges.
public enum PixieRoomStatus: String, Codable, Sendable {
    /// Received unreplied gift(s): red marker ("待回").
    case unreplied = "UNREPLIED"
    /// Replied all received gifts, but given count < received count: amber marker ("需补").
    case needSupplement = "NEED_SUPPLEMENT"
    /// Replied all received gifts and given count >= received count (with received > 0): green marker ("持平").
    case completed = "COMPLETED"
    /// Sent gift(s) proactively, no received gift yet: blue marker ("已发").
    case givenPending = "GIVEN_PENDING"
}

/// Marker model for rendering Pixie Dust room status on the Deck Map SVG.
public struct PixieRoomMarker: Equatable, Hashable, Codable, Sendable {
    public let roomNumber: String
    public let status: PixieRoomStatus
    public let unrepliedCount: Int
    public let givenCount: Int
    public let receivedCount: Int

    public init(
        roomNumber: String,
        status: PixieRoomStatus,
        unrepliedCount: Int = 0,
        givenCount: Int = 0,
        receivedCount: Int = 0
    ) {
        self.roomNumber = roomNumber
        self.status = status
        self.unrepliedCount = unrepliedCount
        self.givenCount = givenCount
        self.receivedCount = receivedCount
    }
}

/// Aggregated view of a stateroom's given & received Pixie Dust records for the Linkage View.
public struct RoomPair: Identifiable, Equatable, Hashable, Sendable {
    public let deck: Int
    public let room: String
    public let given: [PixieDustRecord]
    public let received: [PixieDustRecord]

    public var id: String { "\(deck)-\(room)" }
    public var fullRoom: String {
        room.hasPrefix("\(deck)") && room.count >= 4 ? room : "\(deck)\(room)"
    }

    public var totalGivenCount: Int { given.count }
    public var totalReceivedCount: Int { received.count }
    public var unrepliedCount: Int { received.filter { !$0.replied }.count }
    public var hasUnreplied: Bool { unrepliedCount > 0 }
    public var isBalanced: Bool { given.count == received.count && !hasUnreplied }

    /// Computes the 4-state PixieRoomStatus for this room pair.
    public var roomStatus: PixieRoomStatus {
        if hasUnreplied {
            return .unreplied
        } else if given.count < received.count {
            return .needSupplement
        } else if received.isEmpty && !given.isEmpty {
            return .givenPending
        } else {
            return .completed
        }
    }

    public func toPixieRoomMarker() -> PixieRoomMarker {
        PixieRoomMarker(
            roomNumber: fullRoom,
            status: roomStatus,
            unrepliedCount: unrepliedCount,
            givenCount: totalGivenCount,
            receivedCount: totalReceivedCount
        )
    }

    public init(deck: Int, room: String, given: [PixieDustRecord], received: [PixieDustRecord]) {
        self.deck = deck
        self.room = room
        self.given = given
        self.received = received
    }
}

/// Direction 1: When recording a GIVEN gift to `(deck, room)`, find the earliest unreplied RECEIVED record
/// from the same stateroom to automatically mark as replied.
public func autoReplyTarget(
    received: [PixieDustRecord],
    deck: Int,
    room: String
) -> PixieDustRecord? {
    received
        .filter { $0.direction == .received && $0.deck == deck && $0.room == room && !$0.replied }
        .min(by: { $0.timestamp < $1.timestamp })
}

/// Direction 1 (mirror): When recording a RECEIVED gift from `(deck, room)`, check whether there is an
/// unconsumed GIVEN gift to that room (`givenCount > existingRepliedCount`).
public func hasGivenForRoom(
    given: [PixieDustRecord],
    deck: Int,
    room: String,
    existingRepliedCount: Int = 0
) -> Bool {
    let givenCount = given.filter { $0.direction == .given && $0.deck == deck && $0.room == room }.count
    return givenCount > existingRepliedCount
}

/// Direction 3: Group all GIVEN and RECEIVED records by `(deck, room)` for the Linkage View.
/// Ordering:
/// 1. Rooms with unreplied RECEIVED gifts first (sorted by highest unreplied gift rank descending)
/// 2. Unbalanced rooms (`given.count != received.count`) next
/// 3. Balanced rooms last (sorted by latest activity timestamp descending)
public func pairByRoom(
    given: [PixieDustRecord],
    received: [PixieDustRecord],
    levels: [PixieLevel] = []
) -> [RoomPair] {
    let rankById = Dictionary(uniqueKeysWithValues: levels.map { ($0.id, $0.rank) })

    struct RoomKey: Hashable {
        let deck: Int
        let room: String
    }

    let givenFiltered = given.filter { $0.direction == .given }
    let receivedFiltered = received.filter { $0.direction == .received }

    let givenMap = Dictionary(grouping: givenFiltered, by: { RoomKey(deck: $0.deck, room: $0.room) })
    let receivedMap = Dictionary(grouping: receivedFiltered, by: { RoomKey(deck: $0.deck, room: $0.room) })

    let allKeys = Set(givenMap.keys).union(receivedMap.keys)

    let pairs: [RoomPair] = allKeys.map { key in
        let gList = (givenMap[key] ?? []).sorted { $0.timestamp > $1.timestamp }
        let rList = (receivedMap[key] ?? []).sorted { $0.timestamp > $1.timestamp }
        return RoomPair(deck: key.deck, room: key.room, given: gList, received: rList)
    }

    return pairs.sorted { a, b in
        let groupA: Int = a.hasUnreplied ? 0 : (a.given.count != a.received.count ? 1 : 2)
        let groupB: Int = b.hasUnreplied ? 0 : (b.given.count != b.received.count ? 1 : 2)
        if groupA != groupB {
            return groupA < groupB
        }

        let maxUnrepliedRankA = a.received
            .filter { !$0.replied }
            .map { rankById[$0.levelId] ?? 0 }
            .max() ?? 0
        let maxUnrepliedRankB = b.received
            .filter { !$0.replied }
            .map { rankById[$0.levelId] ?? 0 }
            .max() ?? 0
        if maxUnrepliedRankA != maxUnrepliedRankB {
            return maxUnrepliedRankA > maxUnrepliedRankB
        }

        let maxTsA = (a.given + a.received).map(\.timestamp).max() ?? 0
        let maxTsB = (b.given + b.received).map(\.timestamp).max() ?? 0
        return maxTsA > maxTsB
    }
}

/// Groups all GIVEN and RECEIVED records by `(deck, room)` sorted by Deck ascending -> Room ascending for sweep delivery.
public func pairByRoomForSweep(
    given: [PixieDustRecord],
    received: [PixieDustRecord]
) -> [RoomPair] {
    struct RoomKey: Hashable {
        let deck: Int
        let room: String
    }

    let givenFiltered = given.filter { $0.direction == .given }
    let receivedFiltered = received.filter { $0.direction == .received }

    let givenMap = Dictionary(grouping: givenFiltered, by: { RoomKey(deck: $0.deck, room: $0.room) })
    let receivedMap = Dictionary(grouping: receivedFiltered, by: { RoomKey(deck: $0.deck, room: $0.room) })

    let allKeys = Set(givenMap.keys).union(receivedMap.keys)

    return allKeys.map { key in
        let gList = (givenMap[key] ?? []).sorted { $0.timestamp > $1.timestamp }
        let rList = (receivedMap[key] ?? []).sorted { $0.timestamp > $1.timestamp }
        return RoomPair(deck: key.deck, room: key.room, given: gList, received: rList)
    }.sorted { a, b in
        if a.deck != b.deck { return a.deck < b.deck }
        let roomA = Int(a.room) ?? Int.max
        let roomB = Int(b.room) ?? Int.max
        if roomA != roomB { return roomA < roomB }
        return a.room < b.room
    }
}

/// Alias for `pairByRoom` for clarity in ViewModels and tests.
public func buildRoomPairs(
    given: [PixieDustRecord],
    received: [PixieDustRecord],
    levels: [PixieLevel] = []
) -> [RoomPair] {
    pairByRoom(given: given, received: received, levels: levels)
}
