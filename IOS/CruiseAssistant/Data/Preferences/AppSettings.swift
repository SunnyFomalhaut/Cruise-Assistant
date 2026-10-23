import Foundation
import Combine

/// User preferences and application configuration.
public struct AppSettings: Codable, Equatable, Sendable {
    public var lastViewedDate: SimpleDate? = nil
    public var displayStartMinute: Int = 420 // 07:00
    public var displayEndMinute: Int = 1440 // 24:00
    public var hourHeightPt: Double = 120.0
    public var collapseEmptyGaps: Bool = false
    public var remindBefore15: Bool = true
    public var remindBefore5: Bool = true
    public var remindAtStart: Bool = true
    public var dinnerTime: SimpleTime = SimpleTime(hour: 17, minute: 45)
    public var notificationsEnabled: Bool = true
    public var onboardingCompleted: Bool = false
    public var lastPixieDeck: Int? = nil
    public var armedEventIds: Set<Int64> = []
    public var colorTheme: AppColorTheme = .judy_blue
    public var wallpaperEnabled: Bool = false
    public var wallpaperKey: String = WallpaperPreset.none.rawValue
    public var wallpaperAlpha: Double = 0.10
    public var cardDollsEnabled: Bool = true

    public init(
        lastViewedDate: SimpleDate? = nil,
        displayStartMinute: Int = 420,
        displayEndMinute: Int = 1440,
        hourHeightPt: Double = 120.0,
        collapseEmptyGaps: Bool = false,
        remindBefore15: Bool = true,
        remindBefore5: Bool = true,
        remindAtStart: Bool = true,
        dinnerTime: SimpleTime = SimpleTime(hour: 17, minute: 45),
        notificationsEnabled: Bool = true,
        onboardingCompleted: Bool = false,
        lastPixieDeck: Int? = nil,
        armedEventIds: Set<Int64> = [],
        colorTheme: AppColorTheme = .judy_blue,
        wallpaperEnabled: Bool = false,
        wallpaperKey: String = WallpaperPreset.none.rawValue,
        wallpaperAlpha: Double = 0.10,
        cardDollsEnabled: Bool = true
    ) {
        self.lastViewedDate = lastViewedDate
        self.displayStartMinute = displayStartMinute
        self.displayEndMinute = displayEndMinute
        self.hourHeightPt = hourHeightPt
        self.collapseEmptyGaps = collapseEmptyGaps
        self.remindBefore15 = remindBefore15
        self.remindBefore5 = remindBefore5
        self.remindAtStart = remindAtStart
        self.dinnerTime = dinnerTime
        self.notificationsEnabled = notificationsEnabled
        self.onboardingCompleted = onboardingCompleted
        self.lastPixieDeck = lastPixieDeck
        self.armedEventIds = armedEventIds
        self.colorTheme = colorTheme
        self.wallpaperEnabled = wallpaperEnabled
        self.wallpaperKey = wallpaperKey
        self.wallpaperAlpha = wallpaperAlpha
        self.cardDollsEnabled = cardDollsEnabled
    }

    private enum CodingKeys: String, CodingKey {
        case lastViewedDate, displayStartMinute, displayEndMinute, hourHeightPt, collapseEmptyGaps
        case remindBefore15, remindBefore5, remindAtStart, dinnerTime, notificationsEnabled
        case onboardingCompleted, lastPixieDeck, armedEventIds, colorTheme
        case wallpaperEnabled, wallpaperKey, wallpaperAlpha, cardDollsEnabled
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.lastViewedDate = try container.decodeIfPresent(SimpleDate.self, forKey: .lastViewedDate)
        self.displayStartMinute = try container.decodeIfPresent(Int.self, forKey: .displayStartMinute) ?? 420
        self.displayEndMinute = try container.decodeIfPresent(Int.self, forKey: .displayEndMinute) ?? 1440
        self.hourHeightPt = try container.decodeIfPresent(Double.self, forKey: .hourHeightPt) ?? 120.0
        self.collapseEmptyGaps = try container.decodeIfPresent(Bool.self, forKey: .collapseEmptyGaps) ?? false
        self.remindBefore15 = try container.decodeIfPresent(Bool.self, forKey: .remindBefore15) ?? true
        self.remindBefore5 = try container.decodeIfPresent(Bool.self, forKey: .remindBefore5) ?? true
        self.remindAtStart = try container.decodeIfPresent(Bool.self, forKey: .remindAtStart) ?? true
        self.dinnerTime = try container.decodeIfPresent(SimpleTime.self, forKey: .dinnerTime) ?? SimpleTime(hour: 17, minute: 45)
        self.notificationsEnabled = try container.decodeIfPresent(Bool.self, forKey: .notificationsEnabled) ?? true
        self.onboardingCompleted = try container.decodeIfPresent(Bool.self, forKey: .onboardingCompleted) ?? false
        self.lastPixieDeck = try container.decodeIfPresent(Int.self, forKey: .lastPixieDeck)
        self.armedEventIds = try container.decodeIfPresent(Set<Int64>.self, forKey: .armedEventIds) ?? []
        self.colorTheme = try container.decodeIfPresent(AppColorTheme.self, forKey: .colorTheme) ?? .judy_blue
        self.wallpaperEnabled = try container.decodeIfPresent(Bool.self, forKey: .wallpaperEnabled) ?? false
        self.wallpaperKey = try container.decodeIfPresent(String.self, forKey: .wallpaperKey) ?? WallpaperPreset.none.rawValue
        self.wallpaperAlpha = try container.decodeIfPresent(Double.self, forKey: .wallpaperAlpha) ?? 0.10
        self.cardDollsEnabled = try container.decodeIfPresent(Bool.self, forKey: .cardDollsEnabled) ?? true
    }
}

/// Thread-safe settings store saving to `UserDefaults` with Combine / `@Published` reactivity.
public final class SettingsStore: ObservableObject, @unchecked Sendable {
    private let lock = NSRecursiveLock()
    private let userDefaults: UserDefaults?
    private let storageKey: String

    @Published public private(set) var settings: AppSettings

    /// Default singleton instance using standard UserDefaults.
    public static let shared = SettingsStore()

    /// Creates an in-memory settings store for unit testing.
    public static func inMemory(initialSettings: AppSettings = AppSettings()) -> SettingsStore {
        SettingsStore(userDefaults: nil, initialSettings: initialSettings)
    }

    public init(
        userDefaults: UserDefaults? = .standard,
        storageKey: String = "cruise_settings",
        initialSettings: AppSettings? = nil
    ) {
        self.userDefaults = userDefaults
        self.storageKey = storageKey

        if let initial = initialSettings {
            self.settings = initial
        } else if let userDefaults = userDefaults,
                  let data = userDefaults.data(forKey: storageKey),
                  let loaded = try? JSONDecoder().decode(AppSettings.self, from: data) {
            self.settings = loaded
        } else {
            self.settings = AppSettings()
        }
    }

    private func persistLocked() {
        guard let userDefaults = userDefaults else { return }
        if let data = try? JSONEncoder().encode(settings) {
            userDefaults.set(data, forKey: storageKey)
        }
    }

    public func getSettings() -> AppSettings {
        lock.lock()
        defer { lock.unlock() }
        return settings
    }

    public func updateSettings(_ mutate: (inout AppSettings) -> Void) {
        lock.lock()
        var updated = settings
        mutate(&updated)
        // Bounds checking
        updated.hourHeightPt = min(max(updated.hourHeightPt, 60.0), 180.0)
        updated.wallpaperAlpha = min(max(updated.wallpaperAlpha, 0.05), 0.40)
        settings = updated
        persistLocked()
        lock.unlock()
    }

    public func setLastViewedDate(_ date: SimpleDate?) {
        updateSettings { $0.lastViewedDate = date }
    }

    public func setDisplayRange(startMinute: Int, endMinute: Int) {
        updateSettings {
            $0.displayStartMinute = startMinute
            $0.displayEndMinute = endMinute
        }
    }

    public func setHourHeight(_ pt: Double) {
        updateSettings { $0.hourHeightPt = pt }
    }

    public func setCollapseEmptyGaps(_ enabled: Bool) {
        updateSettings { $0.collapseEmptyGaps = enabled }
    }

    public func setRemindBefore15(_ enabled: Bool) {
        updateSettings { $0.remindBefore15 = enabled }
    }

    public func setRemindBefore5(_ enabled: Bool) {
        updateSettings { $0.remindBefore5 = enabled }
    }

    public func setRemindAtStart(_ enabled: Bool) {
        updateSettings { $0.remindAtStart = enabled }
    }

    public func setDinnerTime(_ time: SimpleTime) {
        updateSettings { $0.dinnerTime = time }
    }

    public func setNotificationsEnabled(_ enabled: Bool) {
        updateSettings { $0.notificationsEnabled = enabled }
    }

    public func setOnboardingCompleted(_ completed: Bool) {
        updateSettings { $0.onboardingCompleted = completed }
    }

    public func setLastPixieDeck(_ deck: Int?) {
        updateSettings { $0.lastPixieDeck = deck }
    }

    public func setColorTheme(_ theme: AppColorTheme) {
        updateSettings { $0.colorTheme = theme }
    }

    public func setWallpaperEnabled(_ enabled: Bool) {
        updateSettings { $0.wallpaperEnabled = enabled }
    }

    public func setWallpaperKey(_ key: String) {
        updateSettings { $0.wallpaperKey = key }
    }

    public func setWallpaperAlpha(_ alpha: Double) {
        updateSettings { $0.wallpaperAlpha = alpha }
    }

    public func setCardDollsEnabled(_ enabled: Bool) {
        updateSettings { $0.cardDollsEnabled = enabled }
    }

    public func getArmedEventIds() -> Set<Int64> {
        lock.lock()
        defer { lock.unlock() }
        return settings.armedEventIds
    }

    public func setArmedEventIds(_ ids: Set<Int64>) {
        updateSettings { $0.armedEventIds = ids }
    }

    public func reset() {
        lock.lock()
        settings = AppSettings()
        persistLocked()
        lock.unlock()
    }
}
