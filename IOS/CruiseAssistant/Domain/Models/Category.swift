import Foundation

/// Event category model.
public struct Category: Identifiable, Codable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var colorArgb: Int64
    public var iconKey: String
    public var parentId: Int64?
    public var isBuiltIn: Bool
    public var isArchived: Bool
    public var sortOrder: Int

    public init(
        id: Int64 = 0,
        name: String,
        colorArgb: Int64,
        iconKey: String,
        parentId: Int64? = nil,
        isBuiltIn: Bool = false,
        isArchived: Bool = false,
        sortOrder: Int = 0
    ) {
        self.id = id
        self.name = name
        self.colorArgb = colorArgb
        self.iconKey = iconKey
        self.parentId = parentId
        self.isBuiltIn = isBuiltIn
        self.isArchived = isArchived
        self.sortOrder = sortOrder
    }
}

/// 9 pre-built default categories matching Android BuiltInCategories.kt.
public let BUILT_IN_CATEGORIES: [Category] = [
    Category(name: "餐食", colorArgb: 0xFF34A853, iconKey: "meal", isBuiltIn: true, sortOrder: 0),
    Category(name: "演出", colorArgb: 0xFFF5B301, iconKey: "show", isBuiltIn: true, sortOrder: 1),
    Category(name: "重要事项", colorArgb: 0xFFE8563F, iconKey: "important", isBuiltIn: true, sortOrder: 2),
    Category(name: "琐事", colorArgb: 0xFF4A7DFF, iconKey: "chore", isBuiltIn: true, sortOrder: 3),
    Category(name: "娱乐项目", colorArgb: 0xFF9B5DE5, iconKey: "fun", isBuiltIn: true, sortOrder: 4),
    Category(name: "见面会", colorArgb: 0xFF00A5A5, iconKey: "meetup", isBuiltIn: true, sortOrder: 5),
    Category(name: "泳池娱乐", colorArgb: 0xFF45C4B8, iconKey: "pool", isBuiltIn: true, sortOrder: 6),
    Category(name: "商店", colorArgb: 0xFFF2994A, iconKey: "shop", isBuiltIn: true, sortOrder: 7),
    Category(name: "青少年活动", colorArgb: 0xFFA97BF0, iconKey: "youth", isBuiltIn: true, sortOrder: 8),
]

/// 8 standard palette colors for custom categories.
public let CATEGORY_PALETTE: [Int64] = [
    0xFFEE6C7A, // 珊瑚红
    0xFFF2994A, // 橙
    0xFFF2C94C, // 黄
    0xFF4ECD8F, // 绿
    0xFF45C4B8, // 青
    0xFF5B8DEF, // 蓝
    0xFFA97BF0, // 紫
    0xFF4A5568, // 深灰蓝
]

/// Fallback category name when a referenced category is not found or deleted.
public let FALLBACK_CATEGORY_NAME = "琐事"
