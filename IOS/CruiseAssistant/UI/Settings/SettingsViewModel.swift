import Foundation
import SwiftUI
import Combine

/// View model managing application preferences, theme/wallpaper styling, voyage configuration, and data backups.
@MainActor
public final class SettingsViewModel: ObservableObject {
    private let settingsStore: SettingsStore
    private let voyageRepository: VoyageRepositoryProtocol
    private let eventRepository: EventRepositoryProtocol
    private let categoryRepository: CategoryRepositoryProtocol?
    private let notificationService: NotificationServiceProtocol?
    private let database: CruiseDatabase?

    private var cancellables = Set<AnyCancellable>()

    // MARK: - Published State

    @Published public var settings: AppSettings
    @Published public var voyage: Voyage?
    @Published public var backupJSON: String?
    @Published public var errorMessage: String?
    @Published public var showResetConfirmation: Bool = false
    @Published public var showBackupSheet: Bool = false

    public init(
        settingsStore: SettingsStore = .shared,
        voyageRepository: VoyageRepositoryProtocol,
        eventRepository: EventRepositoryProtocol,
        categoryRepository: CategoryRepositoryProtocol? = nil,
        notificationService: NotificationServiceProtocol? = nil,
        database: CruiseDatabase? = nil
    ) {
        self.settingsStore = settingsStore
        self.voyageRepository = voyageRepository
        self.eventRepository = eventRepository
        self.categoryRepository = categoryRepository
        self.notificationService = notificationService
        self.database = database

        self.settings = settingsStore.getSettings()
        self.voyage = voyageRepository.getVoyage()

        // Bind reactive updates from SettingsStore
        settingsStore.$settings
            .sink { [weak self] updated in
                self?.settings = updated
            }
            .store(in: &cancellables)
    }

    public func loadData() {
        self.settings = settingsStore.getSettings()
        self.voyage = voyageRepository.getVoyage()
    }

    // MARK: - Theme & Wallpaper Settings

    public func setColorTheme(_ theme: AppColorTheme) {
        settingsStore.setColorTheme(theme)
        self.settings = settingsStore.getSettings()
    }

    public func setWallpaperKey(_ key: String) {
        settingsStore.setWallpaperKey(key)
        if key != WallpaperPreset.none.rawValue {
            settingsStore.setWallpaperEnabled(true)
        }
        self.settings = settingsStore.getSettings()
    }

    public func setWallpaperAlpha(_ alpha: Double) {
        settingsStore.setWallpaperAlpha(alpha)
        self.settings = settingsStore.getSettings()
    }

    public func setWallpaperEnabled(_ enabled: Bool) {
        settingsStore.setWallpaperEnabled(enabled)
        self.settings = settingsStore.getSettings()
    }

    // MARK: - Timeline Display Settings

    public func setDisplayRange(startMinute: Int, endMinute: Int) {
        guard endMinute - startMinute >= 120 else {
            errorMessage = "显示时间跨度不能少于 2 小时"
            return
        }
        errorMessage = nil
        settingsStore.setDisplayRange(startMinute: startMinute, endMinute: endMinute)
        self.settings = settingsStore.getSettings()
    }

    public func setHourHeight(_ pt: Double) {
        settingsStore.setHourHeight(pt)
        self.settings = settingsStore.getSettings()
    }

    public func setCollapseEmptyGaps(_ enabled: Bool) {
        settingsStore.setCollapseEmptyGaps(enabled)
        self.settings = settingsStore.getSettings()
    }

    // MARK: - Notification Preferences

    public func setNotificationsEnabled(_ enabled: Bool) {
        settingsStore.setNotificationsEnabled(enabled)
        self.settings = settingsStore.getSettings()
        triggerRescheduleNotifications()
    }

    public func setRemindBefore15(_ enabled: Bool) {
        settingsStore.setRemindBefore15(enabled)
        self.settings = settingsStore.getSettings()
        triggerRescheduleNotifications()
    }

    public func setRemindBefore5(_ enabled: Bool) {
        settingsStore.setRemindBefore5(enabled)
        self.settings = settingsStore.getSettings()
        triggerRescheduleNotifications()
    }

    public func setRemindAtStart(_ enabled: Bool) {
        settingsStore.setRemindAtStart(enabled)
        self.settings = settingsStore.getSettings()
        triggerRescheduleNotifications()
    }

    public func setDinnerTime(_ time: SimpleTime) {
        settingsStore.setDinnerTime(time)
        self.settings = settingsStore.getSettings()
        eventRepository.updateDinnerEventsTime(newTime: time)
        triggerRescheduleNotifications()
    }

    // MARK: - Voyage Configuration

    public func updateVoyage(
        shipName: String?,
        boardDate: SimpleDate,
        boardTime: SimpleTime,
        disembarkDate: SimpleDate,
        disembarkTime: SimpleTime,
        dinnerTime: SimpleTime
    ) {
        guard var current = voyage ?? voyageRepository.getVoyage() else { return }
        current.shipName = shipName?.trimmingCharacters(in: .whitespacesAndNewlines)
        current.boardDate = boardDate
        current.boardTime = boardTime
        current.disembarkDate = disembarkDate
        current.disembarkTime = disembarkTime

        voyageRepository.updateVoyage(current)
        settingsStore.setDinnerTime(dinnerTime)
        eventRepository.updateDinnerEventsTime(newTime: dinnerTime)

        self.voyage = current
        triggerRescheduleNotifications()
    }

    // MARK: - Data Management & Backup

    public func resetVoyage(onDone: (() -> Void)? = nil) {
        voyageRepository.resetVoyage()
        settingsStore.setOnboardingCompleted(false)
        self.voyage = nil
        triggerRescheduleNotifications()
        onDone?()
    }

    public func exportBackupJSON() -> String? {
        if let db = database {
            let json = db.exportBackupJSON()
            self.backupJSON = json
            return json
        }
        let json = CruiseDatabase.shared.exportBackupJSON()
        self.backupJSON = json
        return json
    }

    private func triggerRescheduleNotifications() {
        guard let notificationService = notificationService else { return }
        let allEvents = eventRepository.getAllEvents()
        let catMap: [Int64: Category]
        if let catRepo = categoryRepository {
            catMap = Dictionary(uniqueKeysWithValues: catRepo.getCategories().map { ($0.id, $0) })
        } else {
            catMap = [:]
        }
        notificationService.rescheduleAll(events: allEvents, categories: catMap, settings: settingsStore.getSettings())
    }
}
