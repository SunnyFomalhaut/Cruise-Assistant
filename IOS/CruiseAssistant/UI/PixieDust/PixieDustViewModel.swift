import Foundation
import SwiftUI
import Combine

/// 仙尘界面标签页
public enum PixieTab: String, CaseIterable, Identifiable, Codable, Sendable {
    case given = "发出"
    case received = "收到"
    case linkage = "互赠明细"

    public var id: String { rawValue }
}

/// 收到/联动列表筛选器
public enum PixieFilter: String, CaseIterable, Identifiable, Codable, Sendable {
    case all = "全部"
    case pendingOnly = "仅待回礼"

    public var id: String { rawValue }
}

/// 仙尘魔法礼物追踪视图模型
@MainActor
public final class PixieDustViewModel: ObservableObject {
    @Published public var tab: PixieTab = .given
    @Published public var filter: PixieFilter = .all
    @Published public var deck: Int = 10
    @Published public var roomInput: String = ""
    @Published public var selectedLevelId: Int64? = nil
    @Published public var givenRecords: [PixieDustRecord] = []
    @Published public var receivedRecords: [PixieDustRecord] = []
    @Published public var allReceivedRecords: [PixieDustRecord] = []
    @Published public var groupedGiven: [RoomGroup] = []
    @Published public var roomPairs: [RoomPair] = []
    @Published public var allRoomPairs: [RoomPair] = []
    @Published public var isKeypadCollapsed: Bool = false
    @Published public var viewMode: GivenViewMode = .grouped
    @Published public var levels: [PixieLevel] = []
    @Published public var availableDecks: [Int] = []
    @Published public var pendingReplyRecord: PixieDustRecord? = nil
    @Published public var editingRecord: PixieDustRecord? = nil
    @Published public var viewingPair: RoomPair? = nil
    @Published public var toastMessage: String? = nil
    @Published public var autoRepliedRoom: String? = nil

    private let dustRepository: PixieDustRepositoryProtocol
    private let voyageRepository: VoyageRepositoryProtocol
    private let settingsStore: SettingsStore
    private var saveInFlight: Bool = false

    public init(
        dustRepository: PixieDustRepositoryProtocol = PixieDustRepository(),
        voyageRepository: VoyageRepositoryProtocol = VoyageRepository(),
        settingsStore: SettingsStore = .shared
    ) {
        self.dustRepository = dustRepository
        self.voyageRepository = voyageRepository
        self.settingsStore = settingsStore

        // 确定可用甲板范围
        let voyage = voyageRepository.getVoyage()
        let minD = (voyage?.minDeck ?? 0) > 0 ? voyage!.minDeck : 1
        let maxD = (voyage?.maxDeck ?? 0) > 0 ? voyage!.maxDeck : 20
        self.availableDecks = Array(minD...maxD)

        // 初始楼层：偏好设置 > 航次范围首选 > 默认 10
        let savedDeck = settingsStore.settings.lastPixieDeck
        if let saved = savedDeck, availableDecks.contains(saved) {
            self.deck = saved
        } else if availableDecks.contains(10) {
            self.deck = 10
        } else {
            self.deck = availableDecks.first ?? 1
        }

        loadData()
    }

    // MARK: - Computed Helpers

    /// 房号实时校验结果（支持 4~5 位完整房号，如 8502 / 10200）
    public var validation: StateroomValidationResult {
        validateStateroomInput(roomInput)
    }

    /// 键盘下方房间历史统计与回礼提示
    public var roomHistory: RoomHistory? {
        let v = validation
        guard v.status == .valid, let d = v.deck, let r = v.room else { return nil }
        let roomGiven = givenRecords.filter { $0.deck == d && $0.room == r }
        let roomReceived = allReceivedRecords.filter { $0.deck == d && $0.room == r }
        guard !roomGiven.isEmpty || !roomReceived.isEmpty else { return nil }

        let lastGiven = roomGiven.max(by: { $0.timestamp < $1.timestamp })
        let lastReceived = roomReceived.max(by: { $0.timestamp < $1.timestamp })
        let unreplied = roomReceived.filter { !$0.replied }.count

        let lastGivenName = lastGiven.map { rec in
            levels.first(where: { $0.id == rec.levelId })?.name ?? rec.levelNameSnapshot
        }
        let lastReceivedName = lastReceived.map { rec in
            levels.first(where: { $0.id == rec.levelId })?.name ?? rec.levelNameSnapshot
        }

        return RoomHistory(
            levelName: lastGivenName ?? lastReceivedName ?? "",
            count: roomGiven.count,
            givenCount: roomGiven.count,
            lastGivenLevelName: lastGivenName,
            lastGivenLevelId: lastGiven?.levelId,
            receivedCount: roomReceived.count,
            unrepliedCount: unreplied,
            lastReceivedLevelName: lastReceivedName,
            lastReceivedLevelId: lastReceived?.levelId
        )
    }

    // MARK: - Keypad Interactions

    /// 数字按键点击（支持 4~5 位完整船舱号直输）
    public func onDigit(_ digit: String) {
        guard digit.allSatisfy(\.isNumber), roomInput.count < 5 else { return }
        roomInput.append(digit)

        if roomInput.count >= 4 {
            let v = validateStateroomInput(roomInput)
            if v.status == .valid, let d = v.deck, let r = v.room {
                deck = d
                let lastGiven = lastLevelForRoom(records: givenRecords, deck: d, room: r)
                let targetLevelId = lastGiven?.levelId
                    ?? allReceivedRecords.first(where: { $0.deck == d && $0.room == r && !$0.replied })?.levelId
                if let targetId = targetLevelId, levels.contains(where: { $0.id == targetId }) {
                    selectedLevelId = targetId
                }
            }
        }
    }

    /// 兼顾 brief 命名
    public func onKeypadPress(digit: String) {
        onDigit(digit)
    }

    /// 回退退格
    public func onBackspace() {
        if !roomInput.isEmpty {
            roomInput.removeLast()
            if roomInput.count >= 4 {
                let v = validateStateroomInput(roomInput)
                if v.status == .valid, let d = v.deck {
                    deck = d
                }
            }
        }
    }

    /// 兼顾 brief 命名
    public func onKeypadBackspace() {
        onBackspace()
    }

    /// 清空房号输入
    public func onClear() {
        roomInput = ""
    }

    /// 兼顾 brief 命名
    public func onKeypadClear() {
        onClear()
    }

    // MARK: - Selection Actions

    /// 选择甲板
    public func selectDeck(_ newDeck: Int) {
        guard deck != newDeck else { return }
        deck = newDeck
        roomInput = ""
        settingsStore.setLastPixieDeck(newDeck)
        HapticFeedbackService.shared.triggerSelection()
    }

    /// 选择礼物等级
    public func selectLevel(_ levelId: Int64) {
        selectedLevelId = levelId
        HapticFeedbackService.shared.triggerSelection()
    }

    /// 切换 Tab
    public func selectTab(_ newTab: PixieTab) {
        tab = newTab
        HapticFeedbackService.shared.triggerSelection()
    }

    /// 切换筛选器（全部 / 仅待回礼）
    public func selectFilter(_ newFilter: PixieFilter) {
        guard filter != newFilter else { return }
        filter = newFilter
        applyFilters()
        HapticFeedbackService.shared.triggerSelection()
    }

    /// 切换键盘折叠
    public func toggleKeypad() {
        isKeypadCollapsed.toggle()
        HapticFeedbackService.shared.triggerSelection()
    }

    /// 切换发出列表模式（按房间归组 / 时间流水）
    public func toggleViewMode() {
        viewMode = (viewMode == .grouped) ? .flow : .grouped
        HapticFeedbackService.shared.triggerSelection()
    }

    // MARK: - Record Submission & Linkage

    /// 快捷录入：保存一条记录（含双向自动回礼联动）
    public func submitRecord() {
        guard !saveInFlight else { return }
        let v = validation
        guard v.status == .valid, let targetDeck = v.deck, let targetRoom = v.room, let levelId = selectedLevelId else {
            return
        }
        guard let level = levels.first(where: { $0.id == levelId }) else { return }

        saveInFlight = true
        defer { saveInFlight = false }

        deck = targetDeck
        settingsStore.setLastPixieDeck(targetDeck)

        let direction: PixieDustDirection = (tab == .received) ? .received : .given
        if direction == .given {
            // 保存发出记录
            _ = dustRepository.addRecord(
                direction: .given,
                deck: targetDeck,
                room: targetRoom,
                levelId: level.id,
                levelNameSnapshot: level.name,
                note: "",
                replied: false
            )

            // 联动方向一：若收到列表中有同房间未回礼的记录，自动将最早一条标记为已回礼
            if let target = autoReplyTarget(received: allReceivedRecords, deck: targetDeck, room: targetRoom) {
                var updated = target
                updated.replied = true
                dustRepository.updateRecord(updated)
                autoRepliedRoom = "\(targetDeck)\(targetRoom)"
                toastMessage = "已自动联动标记 \(targetDeck)\(targetRoom) 收到礼物为已回礼"
            }
        } else {
            // 联动方向一镜像：若此前向该房间送出的次数多于已消费回礼数，刚登记的收到记录直接置为已回礼
            let existingRepliedCount = allReceivedRecords.filter {
                $0.deck == targetDeck && $0.room == targetRoom && $0.replied
            }.count
            let hasGiven = hasGivenForRoom(
                given: givenRecords,
                deck: targetDeck,
                room: targetRoom,
                existingRepliedCount: existingRepliedCount
            )
            _ = dustRepository.addRecord(
                direction: .received,
                deck: targetDeck,
                room: targetRoom,
                levelId: level.id,
                levelNameSnapshot: level.name,
                note: "",
                replied: hasGiven
            )
            if hasGiven {
                autoRepliedRoom = "\(targetDeck)\(targetRoom)"
                toastMessage = "检测到此前已向 \(targetDeck)\(targetRoom) 送过礼物，已自动标记为已回礼"
            }
        }

        // 清空房号输入，保留当前选中等级（便于连记）
        roomInput = ""
        HapticFeedbackService.shared.triggerSuccess()
        loadData()
    }

    /// 别名方法
    public func saveRecord() {
        submitRecord()
    }

    // MARK: - Reply Flow

    /// 收到记录列表中切换回礼状态
    public func toggleReplied(record: PixieDustRecord) {
        if record.replied {
            // 取消已回礼状态
            var updated = record
            updated.replied = false
            dustRepository.updateRecord(updated)
            loadData()
        } else {
            // 勾选已回礼 -> 弹出回礼等级选择确认弹窗（两步流）
            pendingReplyRecord = record
        }
    }

    /// 回礼弹窗确认：标记已回礼，若选择了回礼等级则同时生成一条 GIVEN 发出记录
    public func confirmReply(record: PixieDustRecord, replyLevel: PixieLevel?) {
        pendingReplyRecord = nil
        var updated = record
        updated.replied = true
        dustRepository.updateRecord(updated)

        if let level = replyLevel {
            _ = dustRepository.addRecord(
                direction: .given,
                deck: record.deck,
                room: record.room,
                levelId: level.id,
                levelNameSnapshot: level.name,
                note: "",
                replied: false
            )
        }

        HapticFeedbackService.shared.triggerSuccess()
        loadData()
    }

    /// 取消回礼操作
    public func cancelReply() {
        pendingReplyRecord = nil
    }

    // MARK: - Record Edit & Deletion

    /// 更新记录
    public func updateRecord(_ record: PixieDustRecord) {
        dustRepository.updateRecord(record)
        loadData()
    }

    /// 别名方法
    public func saveEdited(_ record: PixieDustRecord) {
        updateRecord(record)
    }

    /// 删除记录
    public func deleteRecord(id: Int64) {
        dustRepository.deleteRecord(id: id)
        loadData()
    }

    /// 别名方法
    public func deleteRecord(_ record: PixieDustRecord) {
        deleteRecord(id: record.id)
    }

    // MARK: - Data Loading

    /// 加载/刷新仙尘全部数据
    public func loadData() {
        var allLevels = dustRepository.getLevels()
        if allLevels.isEmpty {
            dustRepository.seedDefaultLevels()
            allLevels = dustRepository.getLevels()
        }
        self.levels = allLevels.sorted(by: { $0.sortOrder < $1.sortOrder })

        if selectedLevelId == nil || !levels.contains(where: { $0.id == selectedLevelId }) {
            selectedLevelId = levels.first?.id
        }

        let allRecords = dustRepository.getRecords(direction: nil)
        let given = allRecords
            .filter { $0.direction == .given }
            .sorted { a, b in
                if a.deck != b.deck { return a.deck < b.deck }
                let ra = Int(a.room) ?? 0
                let rb = Int(b.room) ?? 0
                if ra != rb { return ra < rb }
                return a.timestamp > b.timestamp
            }
        self.givenRecords = given

        let received = allRecords.filter { $0.direction == .received }
        self.allReceivedRecords = sortForSweepDelivery(records: received, levels: levels)

        self.groupedGiven = groupGivenByRoom(records: given, levels: levels)
        self.allRoomPairs = pairByRoomForSweep(given: given, received: received, levels: levels)
        applyFilters()
    }

    private func applyFilters() {
        let supplementRoomKeys = Set(
            allRoomPairs
                .filter { $0.received.count > $0.given.count }
                .map { "\($0.deck)-\($0.room)" }
        )
        if filter == .pendingOnly {
            self.receivedRecords = allReceivedRecords.filter {
                !$0.replied || supplementRoomKeys.contains("\($0.deck)-\($0.room)")
            }
            self.roomPairs = allRoomPairs.filter {
                !$0.unrepliedReceived.isEmpty || $0.given.count != $0.received.count
            }
        } else {
            self.receivedRecords = allReceivedRecords
            self.roomPairs = allRoomPairs
        }
    }

    /// 刷新数据
    public func refresh() {
        loadData()
    }
}
