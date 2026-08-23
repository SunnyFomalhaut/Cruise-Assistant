import Foundation

extension SimpleTime {
    /// 获取当前系统时区/日历对应的时间
    public static func now(calendar: Calendar = .current) -> SimpleTime {
        let comp = calendar.dateComponents([.hour, .minute], from: Date())
        return SimpleTime(hour: comp.hour ?? 0, minute: comp.minute ?? 0)
    }

    /// 从 Date 构造 SimpleTime
    public init(date: Date, calendar: Calendar = .current) {
        let comp = calendar.dateComponents([.hour, .minute], from: date)
        self.init(hour: comp.hour ?? 0, minute: comp.minute ?? 0)
    }
}

/// 设施指南 UI 模型
public struct FacilityItemUiModel: Identifiable, Hashable, Sendable {
    public let placeName: String
    public let categoryKey: String
    public let categoryLabel: String
    public let deck: Int?
    public let allDecks: [Int]
    public let primaryDeck: Int?
    public let zone: ShipZone?
    public let formattedLocation: String
    public let slots: [OperatingSlot]
    public let is24Hours: Bool
    public let isOpenNow: Bool
    public let hasOperatingHours: Bool
    public let diningGroupLabel: String?

    public var id: String {
        "\(placeName)_\(deck.map(String.init) ?? "nil")_\(categoryKey)"
    }

    public init(
        placeName: String,
        categoryKey: String,
        categoryLabel: String,
        deck: Int?,
        allDecks: [Int]? = nil,
        primaryDeck: Int? = nil,
        zone: ShipZone?,
        formattedLocation: String,
        slots: [OperatingSlot] = [],
        is24Hours: Bool = false,
        isOpenNow: Bool = true,
        hasOperatingHours: Bool = false,
        diningGroupLabel: String? = nil
    ) {
        self.placeName = placeName
        self.categoryKey = categoryKey
        self.categoryLabel = categoryLabel
        self.deck = deck
        let resolvedAllDecks = allDecks ?? [deck].compactMap { $0 }
        self.allDecks = resolvedAllDecks
        self.primaryDeck = primaryDeck ?? resolvedAllDecks.min() ?? deck
        self.zone = zone
        self.formattedLocation = formattedLocation
        self.slots = slots
        self.is24Hours = is24Hours
        self.isOpenNow = isOpenNow
        self.hasOperatingHours = hasOperatingHours
        self.diningGroupLabel = diningGroupLabel
    }
}

/// 类别 iconKey 到用户可读标签的映射
public func categoryLabelFor(categoryKey: String) -> String {
    switch categoryKey {
    case "meal": return "餐食"
    case "show": return "演出场地"
    case "chore": return "船上服务"
    case "pool": return "泳池娱乐"
    case "shop": return "商店"
    case "youth": return "青少年活动"
    case "fun": return "娱乐项目"
    case "meetup": return "见面会"
    case "important": return "重要事项"
    default: return "其他设施"
    }
}

extension LocationCatalogEntry {
    /// 将 LocationCatalogEntry 转换为 FacilityItemUiModel
    public func toFacilityItemUiModel(currentTime: SimpleTime = .now()) -> FacilityItemUiModel {
        let configuredSlots = hours?.slots ?? []
        let is24H = configuredSlots.contains { slot in
            slot.start == SimpleTime(hour: 0, minute: 0) &&
            (slot.end == SimpleTime(hour: 23, minute: 59) || slot.end == SimpleTime(hour: 0, minute: 0))
        }
        let hasHours = hours?.isConfigured ?? false
        let open = is24H ? true : isOpenAt(time: currentTime)

        let effectiveDecks = allDecks.isEmpty ? [deck].compactMap { $0 } : allDecks
        let locationStr: String
        if !effectiveDecks.isEmpty {
            let deckPart = "Deck " + effectiveDecks.map(String.init).joined(separator: ", ")
            let zonePart = zone.map { " \($0.label)" } ?? ""
            locationStr = deckPart + zonePart
        } else if deck == nil {
            locationStr = "全船/Dial 0"
        } else {
            locationStr = "Deck \(deck!)"
        }

        return FacilityItemUiModel(
            placeName: placeName,
            categoryKey: category,
            categoryLabel: categoryLabelFor(categoryKey: category),
            deck: deck,
            allDecks: effectiveDecks,
            primaryDeck: primaryDeck,
            zone: zone,
            formattedLocation: locationStr,
            slots: configuredSlots,
            is24Hours: is24H,
            isOpenNow: open,
            hasOperatingHours: hasHours,
            diningGroupLabel: diningGroup?.label
        )
    }
}

/// 检索所有内置设施列表
public func getAllFacilityItems(currentTime: SimpleTime = .now()) -> [FacilityItemUiModel] {
    LocationCatalog.shared.venuesByCategory().values.flatMap { $0 }.map {
        $0.toFacilityItemUiModel(currentTime: currentTime)
    }
}

/// 设施多维筛选与搜索匹配
public func filterFacilities(
    facilities: [FacilityItemUiModel],
    query: String,
    categoryKey: String,
    onlyOpenNow: Bool,
    deck: Int? = nil,
    zone: ShipZone? = nil
) -> [FacilityItemUiModel] {
    let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

    return facilities.filter { item in
        // 1. 分类筛选
        let matchesCategory: Bool
        if categoryKey == "all" || categoryKey.isEmpty {
            matchesCategory = true
        } else {
            matchesCategory = (item.categoryKey == categoryKey)
        }

        // 2. 营业中筛选
        let matchesOpen: Bool
        if !onlyOpenNow {
            matchesOpen = true
        } else {
            matchesOpen = item.isOpenNow
        }

        // 3. 楼层筛选
        let matchesDeck: Bool
        if let deck = deck {
            matchesDeck = item.allDecks.contains(deck) || item.deck == deck
        } else {
            matchesDeck = true
        }

        // 4. 区域筛选
        let matchesZone: Bool
        if let zone = zone {
            matchesZone = (item.zone == zone)
        } else {
            matchesZone = true
        }

        // 5. 搜索匹配（名称、甲板、区域、餐饮分组）
        let matchesSearch: Bool
        if trimmedQuery.isEmpty {
            matchesSearch = true
        } else {
            let nameMatch = item.placeName.lowercased().contains(trimmedQuery)
            let locationMatch = item.formattedLocation.lowercased().contains(trimmedQuery)
            let deckMatch = item.deck.map { "deck \($0)".contains(trimmedQuery) || "d\($0)".contains(trimmedQuery) || "\($0)" == trimmedQuery } ?? false
            let zoneMatch = item.zone?.label.contains(trimmedQuery) ?? false
            let diningMatch = item.diningGroupLabel?.lowercased().contains(trimmedQuery) ?? false

            matchesSearch = nameMatch || locationMatch || deckMatch || zoneMatch || diningMatch
        }

        return matchesCategory && matchesOpen && matchesDeck && matchesZone && matchesSearch
    }
}
