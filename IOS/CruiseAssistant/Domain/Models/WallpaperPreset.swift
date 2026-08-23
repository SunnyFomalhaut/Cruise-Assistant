import Foundation

/// Timeline semi-transparent background wallpaper presets.
public enum WallpaperPreset: String, CaseIterable, Identifiable, Codable, Sendable {
    case none = "none"
    case duffy = "duffy"
    case linabell = "linabell"
    case shelliemay = "shelliemay"
    case stellalou = "stellalou"
    case gelatoni = "gelatoni"
    case cookieann = "cookieann"
    case olu = "olu"
    case custom = "custom"

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .none: return "无壁纸"
        case .duffy: return "达菲蓝"
        case .linabell: return "贝儿粉"
        case .shelliemay: return "玫玫粉"
        case .stellalou: return "露露紫"
        case .gelatoni: return "托尼绿"
        case .cookieann: return "饼饼黄"
        case .olu: return "olu绿"
        case .custom: return "自定义"
        }
    }

    public var imageResourceName: String? {
        switch self {
        case .none: return nil
        case .duffy: return "duffy"
        case .linabell: return "linabell"
        case .shelliemay: return "shelliemay"
        case .stellalou: return "stellalou"
        case .gelatoni: return "gelatoni"
        case .cookieann: return "cookieann"
        case .olu: return "olu"
        case .custom: return nil
        }
    }

    public var assetFilename: String? {
        switch self {
        case .none: return nil
        case .duffy: return "duffy.png"
        case .linabell: return "linabell.jpg"
        case .shelliemay: return "shelliemay.jpg"
        case .stellalou: return "stellalou.jpg"
        case .gelatoni: return "gelatoni.jpg"
        case .cookieann: return "cookieann.jpg"
        case .olu: return "olu.jpg"
        case .custom: return "custom_wallpaper.jpg"
        }
    }

    public static let defaultPreset: WallpaperPreset = .duffy

    public static let presets: [WallpaperPreset] = [
        .duffy, .linabell, .shelliemay, .stellalou, .gelatoni, .cookieann, .olu
    ]

    public static let characterPresets = presets

    public static func fromKey(_ key: String?) -> WallpaperPreset {
        guard let key = key else { return .none }
        return WallpaperPreset(rawValue: key) ?? .none
    }
}
