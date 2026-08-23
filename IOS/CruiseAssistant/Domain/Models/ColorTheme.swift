import Foundation

/// Application theme colors.
public enum AppColorTheme: String, CaseIterable, Identifiable, Codable, Sendable {
    case judy_blue = "judy_blue"
    case duffy_blue = "duffy_blue"
    case linabell_pink = "linabell_pink"
    case shelliemay_pink = "shelliemay_pink"
    case lulu_purple = "lulu_purple"
    case gelatoni_green = "gelatoni_green"
    case olu_green = "olu_green"
    case cookieann_yellow = "cookieann_yellow"

    public var id: String { rawValue }

    /// Alias for lulu_purple.
    public static let stellalou_purple = AppColorTheme.lulu_purple

    public var displayName: String {
        switch self {
        case .judy_blue: return "朱迪蓝"
        case .duffy_blue: return "达菲蓝"
        case .linabell_pink: return "贝儿粉"
        case .shelliemay_pink: return "玫玫粉"
        case .lulu_purple: return "露露紫"
        case .gelatoni_green: return "托尼绿"
        case .olu_green: return "olu绿"
        case .cookieann_yellow: return "饼饼黄"
        }
    }

    public var themeDescription: String {
        switch self {
        case .judy_blue: return "经典海洋探索风格"
        case .duffy_blue: return "水手服与小熊浅棕"
        case .linabell_pink: return "柔和低饱和蜜桃粉"
        case .shelliemay_pink: return "复古烟熏玫瑰粉"
        case .lulu_purple: return "梦幻芭蕾舞台紫"
        case .gelatoni_green: return "画家水彩薄荷绿"
        case .olu_green: return "热带海岛青柠绿"
        case .cookieann_yellow: return "烘焙暖心蜂蜜黄"
        }
    }

    public var description: String {
        themeDescription
    }

    public var primaryArgb: UInt32 {
        switch self {
        case .judy_blue: return 0xFF0284C7
        case .duffy_blue: return 0xFF2563EB
        case .linabell_pink: return 0xFFE5879B
        case .shelliemay_pink: return 0xFFC06C84
        case .lulu_purple: return 0xFF8B5CF6
        case .gelatoni_green: return 0xFF4E8A73
        case .olu_green: return 0xFF65A30D
        case .cookieann_yellow: return 0xFFE2A428
        }
    }

    public var secondaryArgb: UInt32 {
        switch self {
        case .judy_blue: return 0xFF0369A1
        case .duffy_blue: return 0xFF1E3A8A
        case .linabell_pink: return 0xFF8C3349
        case .shelliemay_pink: return 0xFF7A2B48
        case .lulu_purple: return 0xFF5B21B6
        case .gelatoni_green: return 0xFF1E4636
        case .olu_green: return 0xFF365314
        case .cookieann_yellow: return 0xFF7A4308
        }
    }

    public var containerArgb: UInt32 {
        switch self {
        case .judy_blue: return 0xFFE0F2FE
        case .duffy_blue: return 0xFFEFF6FF
        case .linabell_pink: return 0xFFFFF0F3
        case .shelliemay_pink: return 0xFFFDF1F5
        case .lulu_purple: return 0xFFF5F3FF
        case .gelatoni_green: return 0xFFEEF7F2
        case .olu_green: return 0xFFF4FBE8
        case .cookieann_yellow: return 0xFFFFFDEB
        }
    }

    /// Resolve theme from persistent key, with fallback to judy_blue.
    public static func fromKey(_ key: String?) -> AppColorTheme {
        guard let key = key else { return .judy_blue }
        if key == "stellalou_purple" { return .lulu_purple }
        return AppColorTheme(rawValue: key) ?? .judy_blue
    }
}
