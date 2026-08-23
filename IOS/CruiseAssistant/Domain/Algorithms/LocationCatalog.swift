import Foundation

/// 餐饮地点的四个分类
public enum DiningGroup: String, CaseIterable, Codable, Sendable {
    case quickService = "快餐 / 简餐"
    case rotation = "轮换正餐餐厅"
    case specialty = "特色付费餐厅"
    case barCafe = "酒吧 / 咖啡厅"

    public var label: String { rawValue }

    public init?(key: String) {
        switch key.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "quick", "quickservice", "quick_service", "快餐 / 简餐", "快餐厅/小吃吧", "快餐/简餐":
            self = .quickService
        case "main", "rotation", "轮换正餐餐厅", "正餐餐厅":
            self = .rotation
        case "paid", "specialty", "特色付费餐厅", "收费餐厅":
            self = .specialty
        case "bar", "barcafe", "bar_cafe", "酒吧 / 咖啡厅", "酒吧", "酒吧/咖啡厅":
            self = .barCafe
        default:
            return nil
        }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let str = try container.decode(String.self)
        if let group = DiningGroup(key: str) {
            self = group
        } else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Unknown DiningGroup: \(str)"
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}

/// 餐次（二级选项）
public enum MealPeriod: String, CaseIterable, Codable, Sendable {
    case breakfast = "早餐"
    case lunch = "午餐"
    case dinner = "晚餐"
    case lateNight = "夜宵"
    case snack = "零食"
    case drink = "酒水"

    public var label: String { rawValue }

    public init?(key: String) {
        switch key.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "breakfast", "早餐":
            self = .breakfast
        case "lunch", "午餐":
            self = .lunch
        case "dinner", "晚餐":
            self = .dinner
        case "latenight", "late_night", "late night", "夜宵":
            self = .lateNight
        case "snack", "零食":
            self = .snack
        case "drink", "酒水":
            self = .drink
        default:
            return nil
        }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let str = try container.decode(String.self)
        if let period = MealPeriod(key: str) {
            self = period
        } else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Unknown MealPeriod: \(str)"
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}

/// 营业时段：end < start 表示跨午夜（次日）；meal 为餐次标签，设施为 nil
public struct OperatingSlot: Hashable, Codable, Sendable {
    public let start: SimpleTime
    public let end: SimpleTime
    public let meal: MealPeriod?
    public let lateNight: Bool

    public init(
        start: SimpleTime,
        end: SimpleTime,
        meal: MealPeriod? = nil,
        lateNight: Bool = false
    ) {
        self.start = start
        self.end = end
        self.meal = meal
        self.lateNight = lateNight
    }

    enum CodingKeys: String, CodingKey {
        case start
        case end
        case meal
        case lateNight
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let startStr = try container.decode(String.self, forKey: .start)
        guard let s = SimpleTime(startStr) else {
            throw DecodingError.dataCorruptedError(forKey: .start, in: container, debugDescription: "Invalid start time: \(startStr)")
        }
        self.start = s

        let endStr = try container.decode(String.self, forKey: .end)
        guard let e = SimpleTime(endStr) else {
            throw DecodingError.dataCorruptedError(forKey: .end, in: container, debugDescription: "Invalid end time: \(endStr)")
        }
        self.end = e

        if let mealStr = try container.decodeIfPresent(String.self, forKey: .meal),
           !mealStr.isEmpty, mealStr != "null" {
            self.meal = MealPeriod(key: mealStr)
        } else {
            self.meal = nil
        }

        self.lateNight = try container.decodeIfPresent(Bool.self, forKey: .lateNight) ?? false
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(start.description, forKey: .start)
        try container.encode(end.description, forKey: .end)
        try container.encodeIfPresent(meal?.rawValue, forKey: .meal)
        try container.encode(lateNight, forKey: .lateNight)
    }
}

/// 营业时间容器
public struct OperatingHours: Hashable, Codable, Sendable {
    public let slots: [OperatingSlot]

    public var isConfigured: Bool {
        !slots.isEmpty
    }

    public init(slots: [OperatingSlot] = []) {
        self.slots = slots
    }
}

/// 预置地点条目（绑定迪士尼探险号）
public struct LocationCatalogEntry: Hashable, Codable, Identifiable, Sendable {
    public let deck: Int?
    public let zone: ShipZone?
    public let placeName: String
    public let diningGroup: DiningGroup?
    public let category: String
    public let hours: OperatingHours?
    public let additionalDecks: [Int]

    public var allDecks: [Int] {
        let primary = [deck].compactMap { $0 }
        return primary + additionalDecks
    }

    public var primaryDeck: Int? {
        allDecks.min() ?? deck
    }

    public var id: String {
        "\(placeName)_\(deck.map(String.init) ?? "nil")"
    }

    public init(
        deck: Int? = nil,
        zone: ShipZone? = nil,
        placeName: String,
        diningGroup: DiningGroup? = nil,
        category: String = "meal",
        hours: OperatingHours? = nil,
        additionalDecks: [Int] = []
    ) {
        self.deck = deck
        self.zone = zone
        self.placeName = placeName
        self.diningGroup = diningGroup
        self.category = category
        self.hours = hours
        self.additionalDecks = additionalDecks
    }

    enum CodingKeys: String, CodingKey {
        case deck
        case zone
        case placeName
        case name
        case diningGroup
        case group
        case category
        case hours
        case slots
        case additionalDecks
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        self.deck = try container.decodeIfPresent(Int.self, forKey: .deck)

        if let zoneStr = try container.decodeIfPresent(String.self, forKey: .zone),
           !zoneStr.isEmpty, zoneStr != "null" {
            self.zone = ShipZone(rawValue: zoneStr.uppercased())
        } else {
            self.zone = nil
        }

        if let name = try container.decodeIfPresent(String.self, forKey: .placeName) {
            self.placeName = name
        } else if let name = try container.decodeIfPresent(String.self, forKey: .name) {
            self.placeName = name
        } else {
            throw DecodingError.keyNotFound(
                CodingKeys.placeName,
                DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Missing name or placeName")
            )
        }

        if let groupStr = try container.decodeIfPresent(String.self, forKey: .diningGroup) ?? container.decodeIfPresent(String.self, forKey: .group),
           !groupStr.isEmpty, groupStr != "null" {
            self.diningGroup = DiningGroup(key: groupStr)
        } else {
            self.diningGroup = nil
        }

        self.category = try container.decodeIfPresent(String.self, forKey: .category) ?? "meal"

        if let directHours = try container.decodeIfPresent(OperatingHours.self, forKey: .hours) {
            self.hours = directHours
        } else if let slotList = try container.decodeIfPresent([OperatingSlot].self, forKey: .slots), !slotList.isEmpty {
            self.hours = OperatingHours(slots: slotList)
        } else {
            self.hours = nil
        }

        self.additionalDecks = try container.decodeIfPresent([Int].self, forKey: .additionalDecks) ?? []
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(deck, forKey: .deck)
        try container.encodeIfPresent(zone?.rawValue, forKey: .zone)
        try container.encode(placeName, forKey: .placeName)
        try container.encodeIfPresent(diningGroup?.rawValue, forKey: .diningGroup)
        try container.encode(category, forKey: .category)
        try container.encodeIfPresent(hours, forKey: .hours)
        try container.encode(additionalDecks, forKey: .additionalDecks)
    }

    /// 某时刻是否在任一营业时段内；跨午夜时段（end <= start）：time >= start || time < end；未配置恒营业
    public func isOpenAt(time: SimpleTime) -> Bool {
        guard let h = hours, h.isConfigured else { return true }
        return h.slots.contains { slot in
            if slot.end > slot.start {
                return time >= slot.start && time < slot.end
            } else {
                return time >= slot.start || time < slot.end
            }
        }
    }

    /// 餐次是否营业：按时段标签匹配；LATE_NIGHT 看任一 lateNight；未配置恒营业
    public func isOpenFor(period: MealPeriod) -> Bool {
        guard let h = hours, h.isConfigured else { return true }
        return h.slots.contains { slot in
            switch period {
            case .breakfast:
                return slot.meal == .breakfast
            case .lunch:
                return slot.meal == .lunch
            case .dinner:
                return slot.meal == .dinner
            case .lateNight:
                return slot.lateNight
            case .snack:
                return slot.meal == .snack
            case .drink:
                return slot.meal == .drink
            }
        }
    }
}

/// 地点下拉的选项：分组标题（餐饮四组）/ 预置条目 / 自定义入口
public enum LocationPresetOption: Hashable, Sendable {
    case group(String)
    case entry(LocationCatalogEntry)
    case custom
}

private struct LocationsEnvelope: Decodable {
    let venues: [LocationCatalogEntry]
}

/// 运行时地点目录：CruiseAssistant 启动时从 Bundle 加载；未加载时返回空目录
public final class LocationCatalog: @unchecked Sendable {
    public static let shared = LocationCatalog()

    private let lock = NSLock()
    private var _catalog: [String: [LocationCatalogEntry]] = [:]

    public init(venues: [LocationCatalogEntry] = []) {
        setCatalog(venues)
    }

    public func venuesByCategory() -> [String: [LocationCatalogEntry]] {
        lock.lock()
        defer { lock.unlock() }
        return _catalog
    }

    public func getAllEntries() -> [LocationCatalogEntry] {
        lock.lock()
        defer { lock.unlock() }
        return Array(_catalog.values.joined())
    }

    public func setCatalog(_ venues: [LocationCatalogEntry]) {
        var grouped: [String: [LocationCatalogEntry]] = [:]
        for venue in venues {
            grouped[venue.category, default: []].append(venue)
        }
        lock.lock()
        _catalog = grouped
        lock.unlock()
    }

    /// 按类别 iconKey 展开成下拉选项（meal 按四组分组，组标题不可选）
    public func locationPresetOptions(
        iconKey: String,
        mealPeriod: MealPeriod? = nil
    ) -> [LocationPresetOption] {
        let currentCatalog = venuesByCategory()
        guard let entries = currentCatalog[iconKey] else { return [] }
        if iconKey != "meal" {
            return entries.map { LocationPresetOption.entry($0) } + [.custom]
        }

        // 餐次筛选：选了餐次时只保留该餐次营业的餐厅（未配置营业时间的餐厅恒显示）
        let filtered = (mealPeriod == nil) ? entries : entries.filter { $0.isOpenFor(period: mealPeriod!) }

        var result: [LocationPresetOption] = []
        for group in DiningGroup.allCases {
            let groupEntries = filtered.filter { $0.diningGroup == group }
            if !groupEntries.isEmpty {
                result.append(.group(group.label))
                for entry in groupEntries {
                    result.append(.entry(entry))
                }
            }
        }
        result.append(.custom)
        return result
    }

    /// 解析 locations.json Data
    public static func parseLocations(data: Data) -> [LocationCatalogEntry] {
        let decoder = JSONDecoder()
        if let envelope = try? decoder.decode(LocationsEnvelope.self, from: data) {
            return envelope.venues
        }
        if let list = try? decoder.decode([LocationCatalogEntry].self, from: data) {
            return list
        }
        return []
    }

    /// 解析 locations.json 字符串
    public static func parseLocations(json: String) -> [LocationCatalogEntry] {
        guard let data = json.data(using: .utf8) else { return [] }
        return parseLocations(data: data)
    }

    /// 从 Bundle 加载默认地点数据
    @discardableResult
    public func loadDefault(bundle: Bundle? = nil) -> Bool {
        let targetBundle = bundle ?? CruiseAssistant.bundle
        let url = targetBundle.url(forResource: "locations", withExtension: "json")
            ?? targetBundle.url(forResource: "locations", withExtension: "json", subdirectory: "Raw")
        guard let url = url, let data = try? Data(contentsOf: url) else {
            return false
        }
        let venues = LocationCatalog.parseLocations(data: data)
        setCatalog(venues)
        return !venues.isEmpty
    }

    // MARK: - Static Convenience Forwarders

    public static func venuesByCategory() -> [String: [LocationCatalogEntry]] {
        shared.venuesByCategory()
    }

    public static func getAllEntries() -> [LocationCatalogEntry] {
        shared.getAllEntries()
    }

    public static func setCatalog(_ venues: [LocationCatalogEntry]) {
        shared.setCatalog(venues)
    }

    public static func locationPresetOptions(
        iconKey: String,
        mealPeriod: MealPeriod? = nil
    ) -> [LocationPresetOption] {
        shared.locationPresetOptions(iconKey: iconKey, mealPeriod: mealPeriod)
    }

    @discardableResult
    public static func loadDefault(bundle: Bundle? = nil) -> Bool {
        shared.loadDefault(bundle: bundle)
    }
}

/// 全局函数：按类别 iconKey 展开成下拉选项
public func locationPresetOptions(
    iconKey: String,
    mealPeriod: MealPeriod? = nil
) -> [LocationPresetOption] {
    LocationCatalog.shared.locationPresetOptions(iconKey: iconKey, mealPeriod: mealPeriod)
}
