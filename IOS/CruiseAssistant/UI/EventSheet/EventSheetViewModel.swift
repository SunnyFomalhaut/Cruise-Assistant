import Foundation
import Combine

/// View model managing event creation and edit flows, presets auto-fill, location picking, and persistence.
public final class EventSheetViewModel: ObservableObject, @unchecked Sendable {
    private let eventRepository: EventRepositoryProtocol
    private let categoryRepository: CategoryRepositoryProtocol
    private let locationPresetRepository: LocationPresetRepositoryProtocol
    private let voyageRepository: VoyageRepositoryProtocol
    private let notificationService: NotificationServiceProtocol?
    private let settingsStore: SettingsStore

    // MARK: - Published State

    @Published public var editingId: Int64 = 0
    @Published public var date: SimpleDate
    @Published public var categories: [Category] = []
    @Published public var selectedCategoryId: Int64?
    @Published public var title: String = ""
    @Published public var deck: Int?
    @Published public var zone: ShipZone? = .mid
    @Published public var placeName: String = ""
    @Published public var noLocation: Bool = true
    @Published public var startTime: SimpleTime
    @Published public var endTime: SimpleTime
    @Published public var recurring: Bool = false
    @Published public var presets: [LocationPreset] = []
    @Published public var maxDeck: Int = 20
    @Published public var minDeck: Int = 1
    @Published public var disembarkDate: SimpleDate?
    @Published public var isSystemEvent: Bool = false
    @Published public var isRecurrenceMember: Bool = false
    @Published public var recurrenceGroupId: String?
    @Published public var errorMessage: String?
    @Published public var saving: Bool = false

    @Published public var categoryIconKey: String?
    @Published public var builtInShip: Bool = false
    @Published public var locationPresetMode: Bool = false
    @Published public var showPresetMode: Bool = false
    @Published public var selectedMealPeriod: MealPeriod?
    @Published public var showMealPeriodRow: Bool = false

    @Published public var hoursCheckPending: Bool = false
    @Published public var hoursCheckMessage: String?
    @Published public var hoursCheckOverride: Bool = false
    @Published public var waitMinutes: Int = 30

    // MARK: - Computed Properties

    public var isEditing: Bool {
        editingId != 0
    }

    public var recurUntil: SimpleDate? {
        disembarkDate?.plusDays(-1)
    }

    public var durationMinutes: Int {
        max(0, endTime.totalMinutes - startTime.totalMinutes)
    }

    public var selectedCategory: Category? {
        categories.first { $0.id == selectedCategoryId }
    }

    // MARK: - Initialization

    public init(
        date: SimpleDate,
        defaultStartMinute: Int = 540,
        editingEventId: Int64 = 0,
        eventRepository: EventRepositoryProtocol = EventRepository(),
        categoryRepository: CategoryRepositoryProtocol = CategoryRepository(),
        locationPresetRepository: LocationPresetRepositoryProtocol = LocationPresetRepository(),
        voyageRepository: VoyageRepositoryProtocol = VoyageRepository(),
        notificationService: NotificationServiceProtocol? = NotificationService.shared,
        settingsStore: SettingsStore = .shared
    ) {
        self.date = date
        self.editingId = editingEventId
        self.eventRepository = eventRepository
        self.categoryRepository = categoryRepository
        self.locationPresetRepository = locationPresetRepository
        self.voyageRepository = voyageRepository
        self.notificationService = notificationService
        self.settingsStore = settingsStore

        let startHour = defaultStartMinute / 60
        let startMin = defaultStartMinute % 60
        self.startTime = SimpleTime(hour: startHour, minute: startMin)

        let endTotal = min(defaultStartMinute + 30, 23 * 60 + 59)
        self.endTime = SimpleTime(hour: endTotal / 60, minute: endTotal % 60)

        loadData()
    }

    // MARK: - Data Loading

    public func loadData() {
        if LocationCatalog.getAllEntries().isEmpty {
            LocationCatalog.loadDefault()
        }
        let voyage = voyageRepository.getVoyage()
        let isBuiltIn = (voyage?.shipName != nil && BUILT_IN_SHIPS.contains(where: { $0.name == voyage?.shipName }))
            || (voyage != nil && (voyage?.shipName?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true))

        self.maxDeck = voyage?.maxDeck ?? 20
        self.minDeck = voyage?.minDeck ?? 1
        self.disembarkDate = voyage?.disembarkDate
        self.builtInShip = isBuiltIn

        self.categories = categoryRepository.getActiveCategories()
        self.presets = locationPresetRepository.getPresets()

        if editingId != 0 {
            if let event = eventRepository.getEvent(byId: editingId) {
                self.date = event.date
                self.selectedCategoryId = event.categoryId
                self.title = event.title
                self.deck = event.deck
                self.zone = event.zone ?? .mid
                self.placeName = event.placeName ?? ""
                self.noLocation = (event.deck == nil && event.zone == nil && (event.placeName == nil || event.placeName!.isEmpty))
                self.startTime = event.startTime
                self.endTime = event.endTime
                self.isSystemEvent = event.isSystemGenerated
                self.isRecurrenceMember = (event.recurrenceGroupId != nil)
                self.recurrenceGroupId = event.recurrenceGroupId
                self.waitMinutes = event.waitMinutes ?? event.effectiveWaitMinutes
            }
        } else {
            if selectedCategoryId == nil, let first = categories.first {
                selectedCategoryId = first.id
            }
        }

        let resolvedCategory = categories.first { $0.id == selectedCategoryId }
        let iconKey = resolvedCategory?.iconKey
        self.categoryIconKey = iconKey
        self.showMealPeriodRow = builtInShip && iconKey == "meal"
        self.locationPresetMode = resolveLocationPresetMode(iconKey: iconKey)
        self.showPresetMode = resolveShowPresetMode(iconKey: iconKey, title: title)
    }

    private func resolveLocationPresetMode(iconKey: String?) -> Bool {
        guard builtInShip, let icon = iconKey else { return false }
        return LocationCatalog.venuesByCategory().keys.contains(icon)
    }

    private func resolveShowPresetMode(iconKey: String?, title: String) -> Bool {
        guard builtInShip, iconKey == "show" else { return false }
        if isEditing {
            return DISNEY_ADVENTURE_SHOWS.contains { $0.title == title }
        }
        return true
    }

    // MARK: - User Actions

    public func onCategorySelect(id: Int64) {
        selectedCategoryId = id
        let cat = categories.first { $0.id == id }
        let iconKey = cat?.iconKey
        self.categoryIconKey = iconKey
        self.showMealPeriodRow = builtInShip && iconKey == "meal"
        self.locationPresetMode = resolveLocationPresetMode(iconKey: iconKey)
        self.showPresetMode = resolveShowPresetMode(iconKey: iconKey, title: title)
        self.selectedMealPeriod = nil
        if iconKey == "show" {
            self.waitMinutes = resolveDefaultWaitMinutes(placeName: placeName, deck: deck, title: title)
        }
        self.errorMessage = nil
    }

    public func onMealPeriodSelect(_ period: MealPeriod?) {
        selectedMealPeriod = period
        let entries = LocationCatalog.venuesByCategory()["meal"] ?? []
        let currentVenueOpen = period == nil || entries.first { e in
            e.deck == deck && e.zone == zone && e.placeName == placeName
        }?.isOpenFor(period: period!) != false

        if !currentVenueOpen {
            self.deck = nil
            self.zone = nil
            self.placeName = ""
        }

        let defaultStart: SimpleTime?
        switch period {
        case .breakfast: defaultStart = SimpleTime(hour: 8, minute: 0)
        case .lunch: defaultStart = SimpleTime(hour: 12, minute: 0)
        case .dinner: defaultStart = SimpleTime(hour: 18, minute: 0)
        case .lateNight: defaultStart = SimpleTime(hour: 22, minute: 0)
        default: defaultStart = nil
        }

        if let defaultStart = defaultStart, let period = period {
            let venueSlotStart = entries.first { e in
                e.deck == deck && e.zone == zone && e.placeName == placeName
            }?.hours?.slots.first { s in
                s.meal == period
            }?.start
            let s = venueSlotStart ?? defaultStart
            self.startTime = s
            self.endTime = SimpleTime(totalMinutes: s.totalMinutes + 30)
        }
    }

    public func onTitleChange(_ value: String) {
        self.title = value
        self.errorMessage = nil
    }

    public func onDeckChange(_ value: Int) {
        self.deck = value
        self.noLocation = false
    }

    public func onZoneChange(_ value: ShipZone) {
        self.zone = value
        self.noLocation = false
    }

    public func onPlaceNameChange(_ value: String) {
        self.placeName = value
        self.locationPresetMode = false
        if categoryIconKey == "show" {
            self.waitMinutes = resolveDefaultWaitMinutes(placeName: value, deck: deck, title: title)
        }
    }

    public func onNoLocationToggle(_ value: Bool) {
        if isSystemEvent {
            self.noLocation = true
            self.deck = nil
        } else if value {
            self.noLocation = true
            self.deck = nil
        } else {
            if deck == nil {
                self.noLocation = false
                self.zone = nil
                self.placeName = ""
            } else {
                self.noLocation = false
            }
        }
    }

    public func onSelectPresetLocation(_ entry: LocationCatalogEntry) {
        self.deck = entry.deck
        self.zone = entry.zone
        self.placeName = entry.placeName
        self.noLocation = false
        self.locationPresetMode = true
        if categoryIconKey == "show" {
            self.waitMinutes = resolveDefaultWaitMinutes(placeName: entry.placeName, deck: entry.deck, title: title)
        }
    }

    public func onCustomLocationSelected() {
        self.locationPresetMode = false
    }

    public func onShowPresetSelect(_ preset: ShowPreset) {
        let start = preset.startTimes.first ?? SimpleTime(hour: 10, minute: 0)
        let endTotal = min(start.totalMinutes + preset.durationMinutes, 23 * 60 + 59)
        let end = SimpleTime(hour: endTotal / 60, minute: endTotal % 60)

        self.title = preset.title
        self.deck = preset.deck
        self.zone = preset.zone
        self.placeName = preset.placeName
        self.noLocation = false
        self.locationPresetMode = true
        self.showPresetMode = true
        self.startTime = start
        self.endTime = end
        self.waitMinutes = preset.defaultWaitMinutes
        self.errorMessage = nil
    }

    public func onCustomShowSelected() {
        self.showPresetMode = false
    }

    public func onWaitMinutesChange(_ value: Int) {
        self.waitMinutes = value
    }

    public func onStartTimeChange(_ value: SimpleTime) {
        let clamped: SimpleTime
        if value > SimpleTime(hour: 23, minute: 29) {
            clamped = SimpleTime(hour: 23, minute: 29)
        } else {
            clamped = value
        }
        let end: SimpleTime
        if endTime <= clamped {
            let nextTotal = min(clamped.totalMinutes + 30, 23 * 60 + 59)
            end = SimpleTime(hour: nextTotal / 60, minute: nextTotal % 60)
        } else {
            end = endTime
        }
        self.startTime = clamped
        self.endTime = end
        self.errorMessage = nil
    }

    public func onEndTimeChange(_ value: SimpleTime) {
        self.endTime = value
        self.errorMessage = nil
    }

    public func onRecurringToggle(_ value: Bool) {
        self.recurring = value
    }

    public func onHoursCheckOverride() {
        self.hoursCheckPending = false
        self.hoursCheckMessage = nil
        self.hoursCheckOverride = true
    }

    public func onHoursCheckDismiss() {
        self.hoursCheckPending = false
        self.hoursCheckMessage = nil
    }

    // MARK: - Save & Delete

    public func save(scope: RecurrenceScope = .thisDayOnly, onDone: @escaping () -> Void) {
        if saving || hoursCheckPending { return }

        guard let catId = selectedCategoryId else {
            self.errorMessage = "请选择活动类别"
            return
        }

        let startMin = startTime.totalMinutes
        let endMin = endTime.totalMinutes
        if endMin <= startMin {
            self.errorMessage = "结束时间必须晚于开始时间"
            return
        }

        // Operating hours check
        if !hoursCheckOverride {
            let matchedVenue: LocationCatalogEntry?
            if !noLocation, let deck = deck, let catKey = categoryIconKey {
                matchedVenue = LocationCatalog.venuesByCategory()[catKey]?.first { entry in
                    entry.deck == deck && entry.zone == zone && entry.placeName == placeName
                }
            } else {
                matchedVenue = nil
            }

            if let venue = matchedVenue, let hours = venue.hours, hours.isConfigured {
                if !venue.isOpenAt(time: startTime) {
                    let hoursText = hours.slots.map { "\($0.start)-\($0.end)" }.joined(separator: " / ")
                    self.hoursCheckPending = true
                    self.hoursCheckMessage = "该地点 \(startTime) 不营业，营业时间：\(hoursText)"
                    return
                }
            }
        }

        let fallbackTitle = (categoryIconKey == "meal") ? (selectedMealPeriod?.label ?? "") : ""
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let finalTitle = trimmedTitle.isEmpty ? fallbackTitle : trimmedTitle

        let finalPlaceName: String?
        if noLocation {
            finalPlaceName = nil
        } else {
            let trimmedPlace = placeName.trimmingCharacters(in: .whitespacesAndNewlines)
            finalPlaceName = trimmedPlace.isEmpty ? nil : trimmedPlace
        }

        let event = CruiseEvent(
            id: editingId,
            date: date,
            startTime: startTime,
            durationMinutes: endMin - startMin,
            categoryId: catId,
            title: finalTitle,
            deck: noLocation ? nil : deck,
            zone: noLocation ? nil : zone,
            placeName: finalPlaceName,
            lane: 0,
            recurrenceGroupId: recurrenceGroupId,
            isSystemGenerated: isSystemEvent,
            isActive: true,
            waitMinutes: (categoryIconKey == "show") ? waitMinutes : nil
        )

        self.saving = true

        if isEditing {
            _ = eventRepository.updateEvent(event: event, scope: scope)
        } else {
            _ = eventRepository.addEvent(event: event, recurring: recurring)
        }

        if let d = event.deck, let z = event.zone, let p = event.placeName {
            locationPresetRepository.recordUsage(deck: d, zone: z, placeName: p)
        }

        rescheduleNotifications()
        self.saving = false
        onDone()
    }

    public func delete(scope: RecurrenceScope = .thisDayOnly, onDone: @escaping () -> Void) {
        guard isEditing && !isSystemEvent else { return }
        eventRepository.deleteEvent(id: editingId, scope: scope)
        rescheduleNotifications()
        onDone()
    }

    private func rescheduleNotifications() {
        guard let notificationService = notificationService else { return }
        let allEvents = eventRepository.getAllEvents()
        let allCats = categoryRepository.getCategories()
        let catMap = Dictionary(uniqueKeysWithValues: allCats.map { ($0.id, $0) })
        notificationService.rescheduleAll(
            events: allEvents,
            categories: catMap,
            settings: settingsStore.settings
        )
    }
}
