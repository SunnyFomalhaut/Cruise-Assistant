import Foundation

/// 联动检视模型：按房间配对双侧（发出与收到）记录
public struct RoomPair: Identifiable, Hashable, Codable, Sendable {
    public var id: String { "\(deck)-\(room)" }
    public let deck: Int
    public let room: String
    public let given: [PixieDustRecord]
    public let received: [PixieDustRecord]
    public let unrepliedReceived: [PixieDustRecord]
    public let lastInteractTs: Int64

    public init(
        deck: Int,
        room: String,
        given: [PixieDustRecord] = [],
        received: [PixieDustRecord] = [],
        unrepliedReceived: [PixieDustRecord]? = nil,
        lastInteractTs: Int64? = nil
    ) {
        self.deck = deck
        self.room = room
        self.given = given
        self.received = received
        self.unrepliedReceived = unrepliedReceived ?? received.filter { !$0.replied }
        let allTs = (given.map(\.timestamp) + received.map(\.timestamp))
        self.lastInteractTs = lastInteractTs ?? (allTs.max() ?? 0)
    }

    /// 是否为双向互赠（双方均有记录）
    public var isMutual: Bool {
        !given.isEmpty && !received.isEmpty
    }

    /// 发出总数
    public var totalGivenCount: Int {
        given.count
    }

    /// 收到总数
    public var totalReceivedCount: Int {
        received.count
    }

    /// 格式化房间号，如 "10120"
    public var fullRoom: String {
        "\(deck)\(room)"
    }
}

extension RoomGroup: Identifiable {
    public var id: String { "\(deck)-\(room)" }
    public var fullRoom: String { "\(deck)\(room)" }
}

/// 方向一联动：发出记录保存后，同房间未回礼的收到记录中最早（timestamp 最小）的一条。
/// 没有则返回 nil（静默，无联动）。
public func autoReplyTarget(received: [PixieDustRecord], deck: Int, room: String) -> PixieDustRecord? {
    received
        .filter {
            $0.direction == .received &&
            !$0.replied &&
            $0.deck == deck &&
            $0.room == room
        }
        .min(by: { $0.timestamp < $1.timestamp })
}

/// 方向一镜像：该房间是否已有发出记录。
/// 登记收到时判断要不要自动把刚登记的这条勾成已回礼。
public func hasGivenForRoom(given: [PixieDustRecord], deck: Int, room: String) -> Bool {
    given.contains { $0.direction == .given && $0.deck == deck && $0.room == room }
}

/// 联动检视：按房间（deck+room）配对双侧记录。
/// 排序：
/// 1. 有未回礼的最前（未回礼中最高等级 rank 降序；同 rank 按最近互动时间倒序）
/// 2. 不平衡（发出数 != 收到数）按最近互动时间倒序
/// 3. 平衡按最近互动时间倒序
public func pairByRoom(
    given: [PixieDustRecord],
    received: [PixieDustRecord],
    levelRankOf: (PixieDustRecord) -> Int = { _ in 0 }
) -> [RoomPair] {
    struct RoomKey: Hashable {
        let deck: Int
        let room: String
    }

    var roomKeySet = Set<RoomKey>()
    for r in given { roomKeySet.insert(RoomKey(deck: r.deck, room: r.room)) }
    for r in received { roomKeySet.insert(RoomKey(deck: r.deck, room: r.room)) }

    let pairs: [RoomPair] = roomKeySet.map { key in
        let g = given.filter { $0.deck == key.deck && $0.room == key.room }
        let r = received.filter { $0.deck == key.deck && $0.room == key.room }
        let unreplied = r.filter { !$0.replied }
        let allTs = g.map(\.timestamp) + r.map(\.timestamp)
        let lastTs = allTs.max() ?? 0
        return RoomPair(
            deck: key.deck,
            room: key.room,
            given: g,
            received: r,
            unrepliedReceived: unreplied,
            lastInteractTs: lastTs
        )
    }

    let hasUnreplied = pairs
        .filter { !$0.unrepliedReceived.isEmpty }
        .sorted { p1, p2 in
            let r1 = p1.unrepliedReceived.map(levelRankOf).max() ?? -1
            let r2 = p2.unrepliedReceived.map(levelRankOf).max() ?? -1
            if r1 != r2 {
                return r1 > r2
            }
            return p1.lastInteractTs > p2.lastInteractTs
        }

    let unbalanced = pairs
        .filter { $0.unrepliedReceived.isEmpty && $0.given.count != $0.received.count }
        .sorted { $0.lastInteractTs > $1.lastInteractTs }

    let balanced = pairs
        .filter { $0.unrepliedReceived.isEmpty && $0.given.count == $0.received.count }
        .sorted { $0.lastInteractTs > $1.lastInteractTs }

    return hasUnreplied + unbalanced + balanced
}

/// Convenience wrapper for building room pairs with level ranks.
public func buildRoomPairs(
    given: [PixieDustRecord],
    received: [PixieDustRecord],
    levels: [PixieLevel] = PixieLevel.BUILT_IN_PIXIE_LEVELS
) -> [RoomPair] {
    let rankMap = Dictionary(uniqueKeysWithValues: levels.map { ($0.id, $0.rank) })
    return pairByRoom(given: given, received: received, levelRankOf: { rankMap[$0.levelId] ?? 0 })
}

/// Convenience overload for groupGivenByRoom using built-in levels if not provided.
public func groupGivenByRoom(records: [PixieDustRecord]) -> [RoomGroup] {
    groupGivenByRoom(records: records, levels: PixieLevel.BUILT_IN_PIXIE_LEVELS)
}

/// Convenience overload for sortReceived using built-in levels if not provided.
public func sortReceived(records: [PixieDustRecord]) -> [PixieDustRecord] {
    sortReceived(records: records, levels: PixieLevel.BUILT_IN_PIXIE_LEVELS)
}
