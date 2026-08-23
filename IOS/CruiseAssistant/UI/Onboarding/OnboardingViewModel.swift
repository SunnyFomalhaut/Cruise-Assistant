import Foundation
import Combine

/// View model driving the onboarding wizard.
public final class OnboardingViewModel: ObservableObject, @unchecked Sendable {
    private let voyageRepository: VoyageRepositoryProtocol
    private let settingsStore: SettingsStore
    private let notificationService: NotificationServiceProtocol?

    @Published public var shipName: String
    @Published public var selectedShipIndex: Int
    @Published public var selectedScheduleIndex: Int
    @Published public var dinnerTime: SimpleTime
    @Published public var boardDate: SimpleDate
    @Published public var boardTime: SimpleTime
    @Published public var disembarkDate: SimpleDate
    @Published public var maxDeck: Int
    @Published public var minDeck: Int
    @Published public var saving: Bool
    @Published public var errorMessage: String?

    public var isBuiltInShip: Bool {
        selectedShipIndex >= 0 && selectedShipIndex < BUILT_IN_SHIPS.count
    }

    public var isLockedDeck: Bool {
        isBuiltInShip && BUILT_IN_SHIPS[selectedShipIndex].maxDeck != nil
    }

    public init(
        voyageRepository: VoyageRepositoryProtocol = VoyageRepository(),
        settingsStore: SettingsStore = .shared,
        notificationService: NotificationServiceProtocol? = nil,
        today: SimpleDate? = nil
    ) {
        self.voyageRepository = voyageRepository
        self.settingsStore = settingsStore
        self.notificationService = notificationService

        let initialToday = today ?? SimpleDate(date: Date())
        let defaultShip = BUILT_IN_SHIPS[0]

        self.shipName = defaultShip.name
        self.selectedShipIndex = 0
        self.selectedScheduleIndex = -1
        self.dinnerTime = SimpleTime(hour: 17, minute: 45)
        self.boardDate = initialToday
        self.boardTime = SimpleTime(hour: 12, minute: 0)
        self.disembarkDate = initialToday.plusDays(5)
        self.maxDeck = defaultShip.maxDeck ?? 20
        self.minDeck = defaultShip.minDeck ?? 1
        self.saving = false
        self.errorMessage = nil
    }

    public func onShipNameChange(_ value: String) {
        self.shipName = value
    }

    public func onShipSelected(_ index: Int) {
        if index < 0 || index >= BUILT_IN_SHIPS.count {
            // Custom ship
            self.selectedShipIndex = -1
            self.selectedScheduleIndex = -1
            self.shipName = ""
            self.maxDeck = 20
            self.minDeck = 1
        } else {
            // Built-in ship
            let ship = BUILT_IN_SHIPS[index]
            self.selectedShipIndex = index
            self.selectedScheduleIndex = -1
            self.shipName = ship.name
            self.maxDeck = ship.maxDeck ?? 20
            self.minDeck = ship.minDeck ?? 1
        }
    }

    public func onScheduleSelected(_ scheduleIndex: Int) {
        guard selectedShipIndex >= 0 && selectedShipIndex < BUILT_IN_SHIPS.count else { return }
        let ship = BUILT_IN_SHIPS[selectedShipIndex]

        if scheduleIndex < 0 || scheduleIndex >= ship.schedules.count {
            // Custom schedule
            self.selectedScheduleIndex = -1
        } else {
            let schedule = ship.schedules[scheduleIndex]
            self.selectedScheduleIndex = scheduleIndex
            self.boardDate = schedule.boardDateFrom(today: boardDate)
            self.disembarkDate = schedule.disembarkDateFrom(today: boardDate)
            self.boardTime = schedule.boardTime
            self.errorMessage = nil
        }
    }

    public func onDinnerTimeChange(_ time: SimpleTime) {
        self.dinnerTime = time
    }

    public func onBoardDateChange(_ value: SimpleDate) {
        if selectedScheduleIndex >= 0 && selectedShipIndex >= 0 && selectedShipIndex < BUILT_IN_SHIPS.count {
            let nights = BUILT_IN_SHIPS[selectedShipIndex].schedules[selectedScheduleIndex].nights
            self.disembarkDate = value.plusDays(nights)
        } else {
            if self.disembarkDate.isBefore(value) {
                self.disembarkDate = value
            }
        }
        self.boardDate = value
        self.errorMessage = nil
    }

    public func onBoardTimeChange(_ time: SimpleTime) {
        self.boardTime = time
    }

    public func onDisembarkDateChange(_ value: SimpleDate) {
        self.disembarkDate = value
        self.errorMessage = nil
    }

    public func onMaxDeckChange(_ value: Int) {
        self.maxDeck = max(minDeck, min(30, value))
    }

    public func submit(onDone: @escaping () -> Void = {}) {
        guard !saving else { return }

        if disembarkDate.isBefore(boardDate) {
            self.errorMessage = "下船日期不能早于登船日期"
            return
        }

        self.saving = true
        self.errorMessage = nil

        let shipPresetName = isBuiltInShip ? shipName : nil
        settingsStore.setDinnerTime(dinnerTime)

        _ = voyageRepository.createVoyage(
            shipName: shipName,
            boardDate: boardDate,
            boardTime: boardTime,
            disembarkDate: disembarkDate,
            disembarkTime: SimpleTime(hour: 7, minute: 0),
            maxDeck: maxDeck,
            minDeck: minDeck,
            dinnerTime: dinnerTime,
            shipPresetName: shipPresetName
        )

        settingsStore.setLastViewedDate(boardDate)
        settingsStore.setOnboardingCompleted(true)

        self.saving = false
        onDone()
    }
}
