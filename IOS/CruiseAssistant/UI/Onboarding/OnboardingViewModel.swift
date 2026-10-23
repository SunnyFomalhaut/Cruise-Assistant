import Foundation
import Combine

/// 礼宾（Concierge）优先登船时间：不受批次限制，11:00 即可登船
public let CONCIERGE_BOARD_TIME = SimpleTime(hour: 11, minute: 0)

/// 联程摘要结构化数据
public struct MultiLegSummary: Equatable, Sendable {
    public let legsLabel: String
    public let totalNights: Int
    public let boardDow: String
    public let disembarkDow: String

    public init(legsLabel: String, totalNights: Int, boardDow: String, disembarkDow: String) {
        self.legsLabel = legsLabel
        self.totalNights = totalNights
        self.boardDow = boardDow
        self.disembarkDow = disembarkDow
    }
}

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
    @Published public var lastBatchBoardTime: SimpleTime
    @Published public var disembarkDate: SimpleDate
    @Published public var maxDeck: Int
    @Published public var minDeck: Int
    @Published public var isConcierge: Bool
    @Published public var extraLegs: [Int]
    @Published public var ashoreLegs: Set<Int>
    @Published public var saving: Bool
    @Published public var errorMessage: String?

    public var isBuiltInShip: Bool {
        selectedShipIndex >= 0 && selectedShipIndex < BUILT_IN_SHIPS.count
    }

    public var isLockedDeck: Bool {
        isBuiltInShip && BUILT_IN_SHIPS[selectedShipIndex].maxDeck != nil
    }

    /// 所选预置船期晚数；未选内置船期返回 nil
    public var scheduleNights: Int? {
        guard isBuiltInShip else { return nil }
        let schedules = BUILT_IN_SHIPS[selectedShipIndex].schedules
        guard selectedScheduleIndex >= 0 && selectedScheduleIndex < schedules.count else { return nil }
        return schedules[selectedScheduleIndex].nights
    }

    /// 是否已追加联程
    public var isMultiLeg: Bool {
        !extraLegs.isEmpty
    }

    /// 联程各程晚数：首程 = 所选船期晚数，其后为 extraLegs；单程返回 []
    public var legs: [Int] {
        guard let first = scheduleNights, !extraLegs.isEmpty else { return [] }
        return [first] + extraLegs
    }

    /// 当前航程各程列表（含单程只有 1 程的情况，供 UI 渲染每程卡片与兰卡威登岸开关）
    public var displayLegs: [Int] {
        guard let first = scheduleNights else { return [] }
        return [first] + extraLegs
    }

    /// 交替链下一程可追加晚数（3 -> 4，4 -> 3）
    public var nextLegNights: Int? {
        guard let first = scheduleNights else { return nil }
        let last = extraLegs.last ?? first
        switch last {
        case 3: return 4
        case 4: return 3
        default: return nil
        }
    }

    /// 航程总晚数
    public var totalNights: Int? {
        if !legs.isEmpty {
            return legs.reduce(0, +)
        }
        return scheduleNights
    }

    /// 联程推算下船日
    public func multiLegDisembarkDate(from board: SimpleDate) -> SimpleDate? {
        guard !legs.isEmpty else { return nil }
        let sum = legs.reduce(0, +)
        return board.plusDays(sum)
    }

    /// 联程摘要
    public var multiLegSummary: MultiLegSummary? {
        let legList = legs
        guard !legList.isEmpty else { return nil }
        let total = legList.reduce(0, +)
        let disembark = boardDate.plusDays(total)
        let weeks = max(0, boardDate.daysBetween(disembark) / 7)
        let prefix = weeks > 0 ? String(repeating: "下", count: weeks) : ""
        return MultiLegSummary(
            legsLabel: legList.map(String.init).joined(separator: "+"),
            totalNights: total,
            boardDow: boardDate.dayOfWeekShort,
            disembarkDow: prefix + disembark.dayOfWeekShort
        )
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
        self.lastBatchBoardTime = SimpleTime(hour: 12, minute: 0)
        self.disembarkDate = initialToday.plusDays(5)
        self.maxDeck = defaultShip.maxDeck ?? 20
        self.minDeck = defaultShip.minDeck ?? 1
        self.isConcierge = false
        self.extraLegs = []
        self.ashoreLegs = []
        self.saving = false
        self.errorMessage = nil
    }

    public func onShipNameChange(_ value: String) {
        self.shipName = value
    }

    public func onShipSelected(_ index: Int) {
        if index < 0 || index >= BUILT_IN_SHIPS.count {
            self.selectedShipIndex = -1
            self.selectedScheduleIndex = -1
            self.shipName = ""
            self.maxDeck = 20
            self.minDeck = 1
            self.extraLegs = []
            self.ashoreLegs = []
        } else {
            let ship = BUILT_IN_SHIPS[index]
            self.selectedShipIndex = index
            self.selectedScheduleIndex = -1
            self.shipName = ship.name
            self.maxDeck = ship.maxDeck ?? 20
            self.minDeck = ship.minDeck ?? 1
            self.extraLegs = []
            self.ashoreLegs = []
        }
    }

    public func onScheduleSelected(_ scheduleIndex: Int) {
        guard selectedShipIndex >= 0 && selectedShipIndex < BUILT_IN_SHIPS.count else { return }
        let ship = BUILT_IN_SHIPS[selectedShipIndex]

        if scheduleIndex < 0 || scheduleIndex >= ship.schedules.count {
            self.selectedScheduleIndex = -1
            self.extraLegs = []
            self.ashoreLegs = []
        } else {
            let schedule = ship.schedules[scheduleIndex]
            let newBoardDate = schedule.boardDateFrom(today: boardDate)
            self.selectedScheduleIndex = scheduleIndex
            self.boardDate = newBoardDate
            self.lastBatchBoardTime = schedule.boardTime
            self.boardTime = isConcierge ? CONCIERGE_BOARD_TIME : schedule.boardTime
            self.ashoreLegs = []
            if let mlDisembark = multiLegDisembarkDate(from: newBoardDate) {
                self.disembarkDate = mlDisembark
            } else {
                self.disembarkDate = schedule.disembarkDateFrom(today: boardDate)
            }
            self.errorMessage = nil
        }
    }

    public func onDinnerTimeChange(_ time: SimpleTime) {
        self.dinnerTime = time
    }

    public func onBoardDateChange(_ value: SimpleDate) {
        self.boardDate = value
        if let mlDisembark = multiLegDisembarkDate(from: value) {
            self.disembarkDate = mlDisembark
        } else if let nights = scheduleNights {
            self.disembarkDate = value.plusDays(nights)
        } else if self.disembarkDate.isBefore(value) {
            self.disembarkDate = value
        }
        self.errorMessage = nil
    }

    public func onBoardTimeChange(_ time: SimpleTime) {
        self.boardTime = time
        self.lastBatchBoardTime = time
    }

    public func onConciergeToggle(_ enabled: Bool) {
        guard enabled != isConcierge else { return }
        if enabled {
            if boardTime != CONCIERGE_BOARD_TIME {
                lastBatchBoardTime = boardTime
            }
            isConcierge = true
            boardTime = CONCIERGE_BOARD_TIME
        } else {
            isConcierge = false
            boardTime = lastBatchBoardTime
        }
    }

    public func onAddLeg(_ nights: Int) {
        guard nights == nextLegNights else { return }
        extraLegs.append(nights)
        if let mlDisembark = multiLegDisembarkDate(from: boardDate) {
            disembarkDate = mlDisembark
        }
    }

    public func onRemoveLeg(_ index: Int) {
        guard index == extraLegs.count - 1 else { return }
        let removedLegFullIndex = extraLegs.count
        extraLegs.removeLast()
        ashoreLegs.remove(removedLegFullIndex)
        if let mlDisembark = multiLegDisembarkDate(from: boardDate) {
            disembarkDate = mlDisembark
        } else if let nights = totalNights {
            disembarkDate = boardDate.plusDays(nights)
        }
    }

    public func onClearLegs() {
        guard !extraLegs.isEmpty else { return }
        extraLegs = []
        ashoreLegs = []
        if let nights = totalNights {
            disembarkDate = boardDate.plusDays(nights)
        }
    }

    public func onToggleAshore(_ legIndex: Int) {
        let currentLegs = displayLegs
        guard legIndex >= 0 && legIndex < currentLegs.count, currentLegs[legIndex] == 4 else { return }
        if ashoreLegs.contains(legIndex) {
            ashoreLegs.remove(legIndex)
        } else {
            ashoreLegs.insert(legIndex)
        }
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
            shipPresetName: shipPresetName,
            isConcierge: isConcierge,
            legs: legs,
            ashoreLegs: ashoreLegs
        )

        settingsStore.setLastViewedDate(boardDate)
        settingsStore.setOnboardingCompleted(true)

        self.saving = false
        onDone()
    }
}
