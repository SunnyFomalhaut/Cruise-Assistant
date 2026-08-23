import SwiftUI

// MARK: - Color Initializers

extension Color {
    /// Initialize a SwiftUI Color from a 64-bit ARGB integer (e.g. 0xFF0284C7).
    public init(argb: Int64) {
        let a = Double((argb >> 24) & 0xFF) / 255.0
        let r = Double((argb >> 16) & 0xFF) / 255.0
        let g = Double((argb >> 8) & 0xFF) / 255.0
        let b = Double(argb & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }

    /// Initialize a SwiftUI Color from a 32-bit ARGB integer (e.g. 0xFF0284C7).
    public init(argb: UInt32) {
        let a = Double((argb >> 24) & 0xFF) / 255.0
        let r = Double((argb >> 16) & 0xFF) / 255.0
        let g = Double((argb >> 8) & 0xFF) / 255.0
        let b = Double(argb & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }

    /// Initialize a SwiftUI Color from an integer literal.
    public init(argb: Int) {
        self.init(argb: Int64(argb))
    }

    /// Initialize a SwiftUI Color from a hex string (e.g. "#0284C7", "0284C7", "#FFE0F2FE", "#RGB").
    public init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&int)

        let a, r, g, b: Double
        switch cleaned.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (1.0,
                            Double((int >> 8) * 17) / 255.0,
                            Double((int >> 4 & 0xF) * 17) / 255.0,
                            Double((int & 0xF) * 17) / 255.0)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (1.0,
                            Double((int >> 16) & 0xFF) / 255.0,
                            Double((int >> 8) & 0xFF) / 255.0,
                            Double(int & 0xFF) / 255.0)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (Double((int >> 24) & 0xFF) / 255.0,
                            Double((int >> 16) & 0xFF) / 255.0,
                            Double((int >> 8) & 0xFF) / 255.0,
                            Double(int & 0xFF) / 255.0)
        default:
            (a, r, g, b) = (1.0, 0.0, 0.0, 0.0)
        }

        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

// MARK: - Core Neutrals & UI Colors

public let Slate900 = Color(hex: "#0F172A")
public let Slate800 = Color(hex: "#1E293B")
public let Slate700 = Color(hex: "#334155")
public let Slate600 = Color(hex: "#475569")
public let Slate500 = Color(hex: "#64748B")
public let Slate400 = Color(hex: "#94A3B8")
public let Slate300 = Color(hex: "#CBD5E1")
public let Slate200 = Color(hex: "#E2E8F0")
public let Slate100 = Color(hex: "#F1F5F9")
public let Slate50  = Color(hex: "#F8FAFC")

public let SurfaceCard = Color.white
public let GridLine = Color(hex: "#F1F5F9")
public let GridLineHour = Color(hex: "#E2E8F0")
public let NowRed = Color(hex: "#EF4444")

// MARK: - Cruise Palette Definition

public struct CruiseColorPalette: Sendable {
    public let primary: Color
    public let secondary: Color
    public let primaryContainer: Color
    public let onPrimaryContainer: Color
    public let primaryLight: Color
    public let accent: Color
    public let surfaceContainer: Color
    public let surfaceCard: Color

    public init(
        primary: Color,
        secondary: Color,
        primaryContainer: Color,
        onPrimaryContainer: Color,
        primaryLight: Color,
        accent: Color,
        surfaceContainer: Color,
        surfaceCard: Color = SurfaceCard
    ) {
        self.primary = primary
        self.secondary = secondary
        self.primaryContainer = primaryContainer
        self.onPrimaryContainer = onPrimaryContainer
        self.primaryLight = primaryLight
        self.accent = accent
        self.surfaceContainer = surfaceContainer
        self.surfaceCard = surfaceCard
    }
}

// Default Judy Blue palette constants
public let CruiseAzure = Color(argb: 0xFF0284C7)
public let CruiseMarine = Color(argb: 0xFF0369A1)
public let CruiseAzureContainer = Color(argb: 0xFFE0F2FE)
public let CruiseAzureLight = Color(argb: 0xFF38BDF8)
public let CruiseBlue = CruiseAzure
public let CruiseBlueLight = CruiseAzureLight

// MARK: - AppColorTheme Extensions

extension AppColorTheme {
    public var primaryColor: Color {
        Color(argb: primaryArgb)
    }

    public var secondaryColor: Color {
        Color(argb: secondaryArgb)
    }

    public var containerColor: Color {
        Color(argb: containerArgb)
    }


    public var palette: CruiseColorPalette {
        switch self {
        case .judy_blue:
            return CruiseColorPalette(
                primary: Color(argb: 0xFF0284C7),
                secondary: Color(argb: 0xFF0369A1),
                primaryContainer: Color(argb: 0xFFE0F2FE),
                onPrimaryContainer: Color(argb: 0xFF0369A1),
                primaryLight: Color(argb: 0xFF38BDF8),
                accent: Color(argb: 0xFF38BDF8),
                surfaceContainer: Color(argb: 0xFFF8FAFC)
            )
        case .duffy_blue:
            return CruiseColorPalette(
                primary: Color(argb: 0xFF2563EB),
                secondary: Color(argb: 0xFF1E3A8A),
                primaryContainer: Color(argb: 0xFFEFF6FF),
                onPrimaryContainer: Color(argb: 0xFF1E3A8A),
                primaryLight: Color(argb: 0xFF60A5FA),
                accent: Color(argb: 0xFFB45309),
                surfaceContainer: Color(argb: 0xFFF8FAFD)
            )
        case .linabell_pink:
            return CruiseColorPalette(
                primary: Color(argb: 0xFFE5879B),
                secondary: Color(argb: 0xFF8C3349),
                primaryContainer: Color(argb: 0xFFFFF0F3),
                onPrimaryContainer: Color(argb: 0xFF8C3349),
                primaryLight: Color(argb: 0xFFF9A8B8),
                accent: Color(argb: 0xFFA855F7),
                surfaceContainer: Color(argb: 0xFFFDFBFB)
            )
        case .shelliemay_pink:
            return CruiseColorPalette(
                primary: Color(argb: 0xFFC06C84),
                secondary: Color(argb: 0xFF7A2B48),
                primaryContainer: Color(argb: 0xFFFDF1F5),
                onPrimaryContainer: Color(argb: 0xFF7A2B48),
                primaryLight: Color(argb: 0xFFE09BAE),
                accent: Color(argb: 0xFF9CA3AF),
                surfaceContainer: Color(argb: 0xFFFDFBFB)
            )
        case .lulu_purple:
            return CruiseColorPalette(
                primary: Color(argb: 0xFF8B5CF6),
                secondary: Color(argb: 0xFF5B21B6),
                primaryContainer: Color(argb: 0xFFF5F3FF),
                onPrimaryContainer: Color(argb: 0xFF5B21B6),
                primaryLight: Color(argb: 0xFFC4B5FD),
                accent: Color(argb: 0xFF14B8A6),
                surfaceContainer: Color(argb: 0xFFFAFAFE)
            )
        case .gelatoni_green:
            return CruiseColorPalette(
                primary: Color(argb: 0xFF4E8A73),
                secondary: Color(argb: 0xFF1E4636),
                primaryContainer: Color(argb: 0xFFEEF7F2),
                onPrimaryContainer: Color(argb: 0xFF1E4636),
                primaryLight: Color(argb: 0xFF81B8A3),
                accent: Color(argb: 0xFF0284C7),
                surfaceContainer: Color(argb: 0xFFF9FBF9)
            )
        case .olu_green:
            return CruiseColorPalette(
                primary: Color(argb: 0xFF65A30D),
                secondary: Color(argb: 0xFF365314),
                primaryContainer: Color(argb: 0xFFF4FBE8),
                onPrimaryContainer: Color(argb: 0xFF365314),
                primaryLight: Color(argb: 0xFFA3E635),
                accent: Color(argb: 0xFFD97706),
                surfaceContainer: Color(argb: 0xFFFAFDF6)
            )
        case .cookieann_yellow:
            return CruiseColorPalette(
                primary: Color(argb: 0xFFE2A428),
                secondary: Color(argb: 0xFF7A4308),
                primaryContainer: Color(argb: 0xFFFFFDEB),
                onPrimaryContainer: Color(argb: 0xFF7A4308),
                primaryLight: Color(argb: 0xFFFACC15),
                accent: Color(argb: 0xFFF472B6),
                surfaceContainer: Color(argb: 0xFFFDFDF9)
            )
        }
    }
}

// MARK: - SwiftUI Theme Environment

private struct CruiseThemeKey: EnvironmentKey {
    static let defaultValue: AppColorTheme = .judy_blue
}

extension EnvironmentValues {
    public var cruiseTheme: AppColorTheme {
        get { self[CruiseThemeKey.self] }
        set { self[CruiseThemeKey.self] = newValue }
    }

    public var cruisePalette: CruiseColorPalette {
        self.cruiseTheme.palette
    }
}

extension View {
    public func cruiseTheme(_ theme: AppColorTheme) -> some View {
        self.environment(\.cruiseTheme, theme)
    }
}

// MARK: - Card & Category Color Blending Helpers

/// Card background color: 8% category color blended over white.
public func cardBackgroundFor(colorArgb: Int64) -> Color {
    let r = Double((colorArgb >> 16) & 0xFF) / 255.0
    let g = Double((colorArgb >> 8) & 0xFF) / 255.0
    let b = Double(colorArgb & 0xFF) / 255.0
    // Lerp with White (1.0, 1.0, 1.0) with fraction 0.08
    let fraction = 0.08
    let finalR = 1.0 + (r - 1.0) * fraction
    let finalG = 1.0 + (g - 1.0) * fraction
    let finalB = 1.0 + (b - 1.0) * fraction
    return Color(.sRGB, red: finalR, green: finalG, blue: finalB, opacity: 1.0)
}

/// Card text color: category color blended 45% towards Slate900 (0x0F, 0x17, 0x2A).
public func cardTextColorFor(colorArgb: Int64) -> Color {
    let r = Double((colorArgb >> 16) & 0xFF) / 255.0
    let g = Double((colorArgb >> 8) & 0xFF) / 255.0
    let b = Double(colorArgb & 0xFF) / 255.0

    let targetR = Double(0x0F) / 255.0
    let targetG = Double(0x17) / 255.0
    let targetB = Double(0x2A) / 255.0

    let fraction = 0.45
    let finalR = r + (targetR - r) * fraction
    let finalG = g + (targetG - g) * fraction
    let finalB = b + (targetB - b) * fraction
    return Color(.sRGB, red: finalR, green: finalG, blue: finalB, opacity: 1.0)
}

// MARK: - SimpleDate SwiftUI Date Conversions

extension SimpleDate {
    /// Convert SimpleDate to Swift standard Date.
    public func toDate(calendar: Calendar = .current) -> Date {
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = day
        components.hour = 12
        components.minute = 0
        components.second = 0
        return calendar.date(from: components) ?? Date()
    }

    /// Construct SimpleDate from Swift standard Date.
    public init(date: Date, calendar: Calendar = .current) {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        self.init(
            year: components.year ?? 1970,
            month: components.month ?? 1,
            day: components.day ?? 1
        )
    }

    /// Formats as "yyyy年M月d日".
    public var chineseFormatted: String {
        "\(year)年\(month)月\(day)日"
    }

    /// Creates today's date in local calendar.
    public static func today() -> SimpleDate {
        SimpleDate(date: Date())
    }

    /// Construct SimpleDate from Swift standard Date.
    public static func fromDate(_ date: Date, calendar: Calendar = .current) -> SimpleDate {
        SimpleDate(date: date, calendar: calendar)
    }

    /// Returns short Chinese weekday name (e.g. "周一").
    public var dayOfWeekShort: String {
        weekdayChinese(for: self)
    }
}

// MARK: - Navigation Bar Display Mode Helper

extension View {
    @ViewBuilder
    public func inlineNavigationBarTitle() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }
}
