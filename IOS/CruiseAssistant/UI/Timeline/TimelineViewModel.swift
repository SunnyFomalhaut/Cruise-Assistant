import Foundation
import Combine

/// View model managing state, layout computations, and user actions for the Timeline screen.
public final class TimelineViewModel: ObservableObject, @unchecked Sendable {
    private let voyageRepository: VoyageRepositoryProtocol
    private let eventRepository: EventRepositoryProtocol
    private let categoryRepository: CategoryRepositoryProtocol
    private let settingsStore: SettingsStore
    private let notificationService: NotificationServiceProtocol?
    private let nowProvider: () -> SimpleTime
    private let todayProvider: () -> SimpleDate

    private var cancellables = Set<AnyCancellable>()

    // MARK: - Published State

    @Published public var voyage: Voyage?
    @Published public var selectedDate: SimpleDate
    @Published public var events: [CruiseEvent] = []
    @Published public var categories: [Int64: Category] = [:]
    @Published public var dayPlans: [SimpleDate: DayPlan] = [:]
    @Published public var layout: TimelineLayout
    @Published public var settings: AppSettings
    @Published public var nowMinute: Int
    @Published public var fullDayOverride: Bool = false
    @Published public var showConflictDialog: Bool = false
    @Published public var conflicts: [(first: CruiseEvent, second: CruiseEvent)] = []
    @Published public var conflictIds: Set<Int64> = []
    @Published public var isLoading: Bool = false

    // MARK: - Computed Properties

    public var isToday: Bool {
        selectedDate == todayProvider()
    }

    public var todayInVoyage: Bool {
        voyage?.containsDate(todayProvider()) == true
    }

    public var isFinalized: Bool {
        dayPlans[selectedDate]?.finalized == true
    }

    public var datesInVoyage: [SimpleDate] {
        voyage?.datesInVoyage() ?? [selectedDate]
    }

    public var finalizedDates: Set<SimpleDate> {
        Set(dayPlans.filter { $0.value.finalized }.keys)
    }

    /// Events visible on the timeline. When finalized, inactive events are hidden.
    public var visibleEvents: [CruiseEvent] {
        if isFinalized {
            return events.filter { $0.isActive }
        } else {
            return events
        }
    }

    /// Current ongoing event (only when viewing today).
    public var ongoing: CruiseEvent? {
        guard isToday else { return nil }
        return visibleEvents.filter { $0.isActive }.first { event in
            nowMinute >= event.startMinute && nowMinute < event.endMinute
        }
    }

    /// Next upcoming event (only when viewing today).
    public var next: CruiseEvent? {
        guard isToday else { return nil }
        return visibleEvents
            .filter { $0.isActive && $0.startMinute > nowMinute }
            .min { $0.startMinute < $1.startMinute }
    }

    // MARK: - Initialization

    public init(
        voyageRepository: VoyageRepositoryProtocol = VoyageRepository(),
        eventRepository: EventRepositoryProtocol = EventRepository(),
        categoryRepository: CategoryRepositoryProtocol = CategoryRepository(),
        settingsStore: SettingsStore = .shared,
        notificationService: NotificationServiceProtocol? = NotificationService.shared,
        nowProvider: @escaping () -> SimpleTime = {
            let components = Calendar.current.dateComponents([.hour, .minute], from: Date())
            return SimpleTime(hour: components.hour ?? 0, minute: components.minute ?? 0)
        },
        todayProvider: @escaping () -> SimpleDate = {
            SimpleDate(date: Date())
        }
    ) {
        self.voyageRepository = voyageRepository
        self.eventRepository = eventRepository
        self.categoryRepository = categoryRepository
        self.settingsStore = settingsStore
        self.notificationService = notificationService
        self.nowProvider = nowProvider
        self.todayProvider = todayProvider

        let currentSettings = settingsStore.settings
        self.settings = currentSettings

        let today = todayProvider()
        let currentVoyage = voyageRepository.getVoyage()
        self.voyage = currentVoyage

        // Pick initial selected date: today if in voyage, else saved date, else boardDate, else today
        let initialTarget: SimpleDate
        if let v = currentVoyage, v.containsDate(today) {
            initialTarget = today
        } else if let saved = currentSettings.lastViewedDate, let v = currentVoyage, v.containsDate(saved) {
            initialTarget = saved
        } else if let v = currentVoyage {
            initialTarget = v.boardDate
        } else {
            initialTarget = today
        }
        self.selectedDate = initialTarget

        let currentNow = nowProvider()
        self.nowMinute = currentNow.totalMinutes

        // Initial placeholder layout
        self.layout = buildTimelineLayout(
            rangeStartMinute: currentSettings.displayStartMinute,
            rangeEndMinute: currentSettings.displayEndMinute,
            hourHeightPt: currentSettings.hourHeightPt,
            events: [],
            collapseEmptyGaps: currentSettings.collapseEmptyGaps
        )

        // Observe settings changes
        settingsStore.$settings
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newSettings in
                guard let self = self else { return }
                self.settings = newSettings
                self.recomputeLayoutAndConflicts()
            }
            .store(in: &cancellables)

        loadData()
    }

    // MARK: - Data Loading

    public func loadData() {
        isLoading = true

        self.voyage = voyageRepository.getVoyage()
        self.dayPlans = voyageRepository.getDayPlans()

        let catList = categoryRepository.getCategories()
        self.categories = Dictionary(uniqueKeysWithValues: catList.map { ($0.id, $0) })

        self.events = eventRepository.getEvents(for: selectedDate)
        self.nowMinute = nowProvider().totalMinutes

        recomputeLayoutAndConflicts()
        isLoading = false
    }

    public func refresh() {
        loadData()
    }

    public func updateNowMinute() {
        self.nowMinute = nowProvider().totalMinutes
    }

    // MARK: - Date Selection & Navigation

    public func selectDate(_ date: SimpleDate) {
        selectedDate = date
        fullDayOverride = false
        settingsStore.setLastViewedDate(date)

        events = eventRepository.getEvents(for: date)
        dayPlans = voyageRepository.getDayPlans()
        recomputeLayoutAndConflicts()
    }

    public func jumpToToday() {
        selectDate(todayProvider())
    }

    public func toggleFullDay() {
        fullDayOverride.toggle()
        recomputeLayoutAndConflicts()
    }

    // MARK: - Event Mutations

    public func moveToLane(eventId: Int64, lane: Int) {
        eventRepository.setEventLane(id: eventId, lane: lane)
        events = eventRepository.getEvents(for: selectedDate)
        recomputeLayoutAndConflicts()
    }

    public func toggleActive(eventId: Int64) {
        _ = eventRepository.toggleEventActive(id: eventId)
        events = eventRepository.getEvents(for: selectedDate)
        rescheduleNotifications()
        recomputeLayoutAndConflicts()
    }

    public func keepOnlyThisShow(_ event: CruiseEvent) {
        eventRepository.keepOnlyThisShow(target: event)
        events = eventRepository.getEvents(for: selectedDate)
        rescheduleNotifications()
        recomputeLayoutAndConflicts()
    }

    public func deleteEvent(id: Int64, scope: RecurrenceScope = .thisDayOnly) {
        eventRepository.deleteEvent(id: id, scope: scope)
        events = eventRepository.getEvents(for: selectedDate)
        rescheduleNotifications()
        recomputeLayoutAndConflicts()
    }

    // MARK: - Finalization & Conflicts

    public func requestFinalizeDay() {
        let detected = eventRepository.conflictsOn(date: selectedDate)
        if !detected.isEmpty {
            conflicts = detected.map { (first: $0.0, second: $0.1) }
            showConflictDialog = true
        } else {
            finalizeDay(selectedDate)
        }
    }

    public func finalizeDay(_ date: SimpleDate) {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        voyageRepository.setFinalized(date: date, finalized: true, nowMs: nowMs)
        showConflictDialog = false
        dayPlans = voyageRepository.getDayPlans()
        rescheduleNotifications()
        recomputeLayoutAndConflicts()
    }

    public func unlockDay(_ date: SimpleDate) {
        voyageRepository.setFinalized(date: date, finalized: false, nowMs: 0)
        dayPlans = voyageRepository.getDayPlans()
        rescheduleNotifications()
        recomputeLayoutAndConflicts()
    }

    // MARK: - Settings Updates

    public func zoom(hourHeightPt: Double) {
        settingsStore.setHourHeight(hourHeightPt)
    }

    public func toggleCollapseGaps() {
        settingsStore.setCollapseEmptyGaps(!settings.collapseEmptyGaps)
    }

    // MARK: - Layout & Conflict Recomputation

    private func recomputeLayoutAndConflicts() {
        let showCatIds = Set(categories.values.filter { $0.iconKey == "show" }.map(\.id))
        let rangeStart = fullDayOverride ? 0 : settings.displayStartMinute
        let rangeEnd = fullDayOverride ? 1440 : settings.displayEndMinute

        self.layout = buildTimelineLayout(
            rangeStartMinute: rangeStart,
            rangeEndMinute: rangeEnd,
            hourHeightPt: settings.hourHeightPt,
            events: visibleEvents,
            collapseEmptyGaps: settings.collapseEmptyGaps,
            showCategoryIds: showCatIds
        )

        // Conflict IDs on visible active events with show wait time expansion
        let rangeOf = showRangeOf(showCatIds)
        let conflictPairs = findConflicts(visibleEvents, rangeOf: rangeOf)
        self.conflictIds = Set(conflictPairs.flatMap { [$0.first.id, $0.second.id] })
    }

    private func rescheduleNotifications() {
        guard let notificationService = notificationService else { return }
        let allEvents = eventRepository.getAllEvents()
        notificationService.rescheduleAll(
            events: allEvents,
            categories: categories,
            settings: settings
        )
    }
}
