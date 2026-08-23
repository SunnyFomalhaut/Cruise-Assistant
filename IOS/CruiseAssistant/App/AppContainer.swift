import Foundation

/// Central Dependency Injection container for Cruise Assistant.
/// Encapsulates repositories, local persistent database, preferences store, and device services.
public final class AppContainer: @unchecked Sendable {
    public let database: CruiseDatabase
    public let settingsStore: SettingsStore
    public let voyageRepository: VoyageRepositoryProtocol
    public let eventRepository: EventRepositoryProtocol
    public let categoryRepository: CategoryRepositoryProtocol
    public let pixieDustRepository: PixieDustRepositoryProtocol
    public let locationPresetRepository: LocationPresetRepositoryProtocol
    public let wishlistRepository: WishlistRepositoryProtocol
    public let notificationService: NotificationServiceProtocol
    public let hapticFeedbackService: HapticFeedbackServiceProtocol

    public init(
        database: CruiseDatabase,
        settingsStore: SettingsStore,
        voyageRepository: VoyageRepositoryProtocol,
        eventRepository: EventRepositoryProtocol,
        categoryRepository: CategoryRepositoryProtocol,
        pixieDustRepository: PixieDustRepositoryProtocol,
        locationPresetRepository: LocationPresetRepositoryProtocol,
        wishlistRepository: WishlistRepositoryProtocol,
        notificationService: NotificationServiceProtocol,
        hapticFeedbackService: HapticFeedbackServiceProtocol
    ) {
        self.database = database
        self.settingsStore = settingsStore
        self.voyageRepository = voyageRepository
        self.eventRepository = eventRepository
        self.categoryRepository = categoryRepository
        self.pixieDustRepository = pixieDustRepository
        self.locationPresetRepository = locationPresetRepository
        self.wishlistRepository = wishlistRepository
        self.notificationService = notificationService
        self.hapticFeedbackService = hapticFeedbackService

        if LocationCatalog.getAllEntries().isEmpty {
            LocationCatalog.loadDefault()
        }
        if StateroomCatalog.getAllStaterooms().isEmpty {
            StateroomCatalog.loadDefault()
        }
    }

    /// Standard production container connected to disk persistence and system services.
    public static func standard() -> AppContainer {
        let database = CruiseDatabase.shared
        let settingsStore = SettingsStore.shared
        let categoryRepo = CategoryRepository(database: database)
        let voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        let eventRepo = EventRepository(database: database)
        let pixieDustRepo = PixieDustRepository(database: database)
        let locationPresetRepo = LocationPresetRepository(database: database)
        let wishlistRepo = WishlistRepository(database: database)
        let notificationService = NotificationService.shared
        let hapticFeedbackService = HapticFeedbackService.shared

        return AppContainer(
            database: database,
            settingsStore: settingsStore,
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            pixieDustRepository: pixieDustRepo,
            locationPresetRepository: locationPresetRepo,
            wishlistRepository: wishlistRepo,
            notificationService: notificationService,
            hapticFeedbackService: hapticFeedbackService
        )
    }

    /// In-memory container isolated from disk, suitable for previews, unit tests, and integration flows.
    public static func inMemory(initialSettings: AppSettings = AppSettings()) -> AppContainer {
        let database = CruiseDatabase.inMemory()
        let settingsStore = SettingsStore.inMemory(initialSettings: initialSettings)
        let categoryRepo = CategoryRepository(database: database)
        let voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        let eventRepo = EventRepository(database: database)
        let pixieDustRepo = PixieDustRepository(database: database)
        let locationPresetRepo = LocationPresetRepository(database: database)
        let wishlistRepo = WishlistRepository(database: database)
        let notificationService = NotificationService()
        let hapticFeedbackService = HapticFeedbackService()

        return AppContainer(
            database: database,
            settingsStore: settingsStore,
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            pixieDustRepository: pixieDustRepo,
            locationPresetRepository: locationPresetRepo,
            wishlistRepository: wishlistRepo,
            notificationService: notificationService,
            hapticFeedbackService: hapticFeedbackService
        )
    }
}
