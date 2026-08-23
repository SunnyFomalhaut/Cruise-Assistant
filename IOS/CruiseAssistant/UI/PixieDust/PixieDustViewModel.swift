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

/// 房间历史小字数据（如"上次：豪华 ×2"）
public struct RoomHistory: Equatable, Sendable {
    public let levelName: String
    public let count: Int

    public init(levelName: String, count: Int) {
        self.levelName = levelName
        self.count = count
    }
}

/// 仙尘魔法礼物追踪视图模型
@MainActor
public final class PixieDustViewModel: ObservableObject {
    @Published public var tab: PixieTab = .given
    @Published public var deck: Int = 10
    @Published public var roomInput: String = ""
    @Published public var selectedLevelId: Int64? = nil
    @Published public var givenRecords: [PixieDustRecord] = []
    @Published public var receivedRecords: [PixieDustRecord] = []
    @Published public var groupedGiven: [RoomGroup] = []
    @Published public var roomPairs: [RoomPair] = []
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

    /// 键盘下方上次历史小字提示
    public var roomHistory: RoomHistory? {
        guard isValidRoomInput(roomInput) else { return nil }
        guard let last = lastLevelForRoom(records: givenRecords, deck: deck, room: roomInput) else {
            return nil
        }
        let name = levels.first(where: { $0.id == last.levelId })?.name ?? last.levelNameSnapshot
        let count = givenRecords.filter { $0.deck == deck && $0.room == roomInput }.count
        return RoomHistory(levelName: name, count: count)
    }

    // MARK: - Keypad Interactions

    /// 数字按键点击
    public func onDigit(_ digit: String) {
        guard digit.allSatisfy(\.isNumber), roomInput.count < 3 else { return }
        roomInput.append(digit)

        // 输满三位数字后，若该房间曾有发出历史，自动预选上次送出的等级
        if roomInput.count == 3 {
            if let last = lastLevelForRoom(records: givenRecords, deck: deck, room: roomInput),
               levels.contains(where: { $0.id == last.levelId }) {
                selectedLevelId = last.levelId
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
        guard isValidRoomInput(roomInput), let levelId = selectedLevelId else { return }
        guard let level = levels.first(where: { $0.id == levelId }) else { return }

        let direction: PixieDustDirection = (tab == .received) ? .received : .given
        if direction == .given {
            // 保存发出记录
            _ = dustRepository.addRecord(
                direction: .given,
                deck: deck,
                room: roomInput,
                levelId: level.id,
                levelNameSnapshot: level.name,
                note: "",
                replied: false
            )

            // 联动方向一：若收到列表中有同房间未回礼的记录，自动将最早一条标记为已回礼
            if let target = autoReplyTarget(received: receivedRecords, deck: deck, room: roomInput) {
                var updated = target
                updated.replied = true
                dustRepository.updateRecord(updated)
                autoRepliedRoom = "\(deck)\(roomInput)"
                toastMessage = "已自动联动标记 \(deck)\(roomInput) 收到礼物为已回礼"
            }
        } else {
            // 联动方向一镜像：若此前已向该房间送出过礼物，刚登记的收到记录直接置为已回礼
            let hasGiven = hasGivenForRoom(given: givenRecords, deck: deck, room: roomInput)
            _ = dustRepository.addRecord(
                direction: .received,
                deck: deck,
                room: roomInput,
                levelId: level.id,
                levelNameSnapshot: level.name,
                note: "",
                replied: hasGiven
            )
            if hasGiven {
                autoRepliedRoom = "\(deck)\(roomInput)"
                toastMessage = "检测到此前已向 \(deck)\(roomInput) 送过礼物，已自动标记为已回礼"
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
            .sorted(by: { $0.timestamp > $1.timestamp })
        self.givenRecords = given

        let received = allRecords.filter { $0.direction == .received }
        self.receivedRecords = sortReceived(records: received, levels: levels)

        self.groupedGiven = groupGivenByRoom(records: given, levels: levels)
        self.roomPairs = buildRoomPairs(given: given, received: received, levels: levels)
    }

    /// 刷新数据
    public func refresh() {
        loadData()
    }
}
