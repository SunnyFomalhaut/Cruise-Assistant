import Foundation

/// 客房条目模型
public struct StateroomEntry: Hashable, Codable, Identifiable, Sendable {
    public let roomNumber: String
    public let deck: Int

    public var id: String { roomNumber }

    public init(roomNumber: String, deck: Int) {
        self.roomNumber = roomNumber
        self.deck = deck
    }

    enum CodingKeys: String, CodingKey {
        case roomNumber = "room"
        case roomNumberAlt = "roomNumber"
        case deck
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        if let room = try container.decodeIfPresent(String.self, forKey: .roomNumber) {
            self.roomNumber = room
        } else if let room = try container.decodeIfPresent(String.self, forKey: .roomNumberAlt) {
            self.roomNumber = room
        } else {
            throw DecodingError.keyNotFound(
                CodingKeys.roomNumber,
                DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Missing room or roomNumber key")
            )
        }
        self.deck = try container.decode(Int.self, forKey: .deck)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(roomNumber, forKey: .roomNumber)
        try container.encode(deck, forKey: .deck)
    }
}

private struct StateroomsEnvelope: Codable {
    let staterooms: [StateroomEntry]
}

/// 全船客房全量目录索引仓储
public final class StateroomCatalog: @unchecked Sendable {
    public static let shared = StateroomCatalog()

    private let lock = NSLock()
    private var _staterooms: [StateroomEntry] = []

    public init(staterooms: [StateroomEntry] = []) {
        setStaterooms(staterooms)
    }

    public func getAllStaterooms() -> [StateroomEntry] {
        lock.lock()
        defer { lock.unlock() }
        return _staterooms
    }

    public func setStaterooms(_ rooms: [StateroomEntry]) {
        let sorted = rooms.sorted { (Int($0.roomNumber) ?? 0) < (Int($1.roomNumber) ?? 0) }
        lock.lock()
        _staterooms = sorted
        lock.unlock()
    }

    /// 根据房号前缀或模糊子串进行搜索，按数值升序返回
    public func search(query: String, limit: Int = 100) -> [StateroomEntry] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed.allSatisfy({ $0.isNumber }) else {
            return []
        }

        lock.lock()
        let current = _staterooms
        lock.unlock()

        // 优先前缀全匹配（如 112 -> 11200, 11201...）
        let prefixMatches = current.filter { $0.roomNumber.hasPrefix(trimmed) }
        if !prefixMatches.isEmpty {
            return Array(prefixMatches.prefix(limit))
        }

        // 兜底包含匹配
        let substringMatches = current.filter { $0.roomNumber.contains(trimmed) }
        return Array(substringMatches.prefix(limit))
    }

    /// 解析 staterooms.json Data
    public static func parseStaterooms(data: Data) -> [StateroomEntry] {
        let decoder = JSONDecoder()
        if let envelope = try? decoder.decode(StateroomsEnvelope.self, from: data) {
            return envelope.staterooms
        }
        if let list = try? decoder.decode([StateroomEntry].self, from: data) {
            return list
        }
        return []
    }

    /// 解析 staterooms.json 字符串
    public static func parseStaterooms(json: String) -> [StateroomEntry] {
        guard let data = json.data(using: .utf8) else { return [] }
        return parseStaterooms(data: data)
    }

    /// 从 Bundle 加载默认客房数据
    @discardableResult
    public func loadDefault(bundle: Bundle? = nil) -> Bool {
        let targetBundle = bundle ?? .module
        let url = targetBundle.url(forResource: "staterooms", withExtension: "json")
            ?? targetBundle.url(forResource: "staterooms", withExtension: "json", subdirectory: "Raw")
        guard let url = url, let data = try? Data(contentsOf: url) else {
            return false
        }
        let parsed = StateroomCatalog.parseStaterooms(data: data)
        setStaterooms(parsed)
        return !parsed.isEmpty
    }

    // MARK: - Static Convenience Forwarders

    public static func getAllStaterooms() -> [StateroomEntry] {
        shared.getAllStaterooms()
    }

    public static func setStaterooms(_ rooms: [StateroomEntry]) {
        shared.setStaterooms(rooms)
    }

    public static func search(query: String, limit: Int = 100) -> [StateroomEntry] {
        shared.search(query: query, limit: limit)
    }

    @discardableResult
    public static func loadDefault(bundle: Bundle? = nil) -> Bool {
        shared.loadDefault(bundle: bundle)
    }
}
