import XCTest
@testable import CruiseAssistant

@MainActor
final class PixieDustViewModelTests: XCTestCase {
    private var database: CruiseDatabase!
    private var pixieDustRepo: PixieDustRepository!
    private var voyageRepo: VoyageRepository!
    private var settingsStore: SettingsStore!
    private var viewModel: PixieDustViewModel!

    override func setUp() {
        super.setUp()
        database = CruiseDatabase.inMemory()
        pixieDustRepo = PixieDustRepository(database: database)
        voyageRepo = VoyageRepository(database: database)
        settingsStore = SettingsStore.inMemory()

        // Seed default levels in database
        pixieDustRepo.seedDefaultLevels()

        viewModel = PixieDustViewModel(
            dustRepository: pixieDustRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )
    }

    override func tearDown() {
        viewModel = nil
        pixieDustRepo = nil
        voyageRepo = nil
        settingsStore = nil
        database = nil
        super.tearDown()
    }

    // MARK: - Keypad Typing Tests

    func testKeypadDigitsInputAndLimit() {
        XCTAssertEqual(viewModel.roomInput, "")

        viewModel.onDigit("5")
        XCTAssertEqual(viewModel.roomInput, "5")

        viewModel.onDigit("5")
        XCTAssertEqual(viewModel.roomInput, "55")

        viewModel.onDigit("0")
        XCTAssertEqual(viewModel.roomInput, "550")

        // 4th digit should be ignored
        viewModel.onDigit("1")
        XCTAssertEqual(viewModel.roomInput, "550")

        // Non-digit should be ignored
        viewModel.onDigit("A")
        XCTAssertEqual(viewModel.roomInput, "550")
    }

    func testKeypadBackspaceAndClear() {
        viewModel.onDigit("1")
        viewModel.onDigit("2")
        viewModel.onDigit("3")
        XCTAssertEqual(viewModel.roomInput, "123")

        viewModel.onBackspace()
        XCTAssertEqual(viewModel.roomInput, "12")

        viewModel.onBackspace()
        XCTAssertEqual(viewModel.roomInput, "1")

        viewModel.onClear()
        XCTAssertEqual(viewModel.roomInput, "")

        // Backspace on empty is a no-op
        viewModel.onBackspace()
        XCTAssertEqual(viewModel.roomInput, "")
    }

    // MARK: - Deck and Level Selection Tests

    func testSelectDeckAndPersistence() {
        viewModel.onDigit("5")
        viewModel.onDigit("5")
        XCTAssertEqual(viewModel.roomInput, "55")

        viewModel.selectDeck(12)
        XCTAssertEqual(viewModel.deck, 12)
        XCTAssertEqual(viewModel.roomInput, "", "Changing deck must clear room input")
        XCTAssertEqual(settingsStore.settings.lastPixieDeck, 12)
    }

    func testSelectLevel() {
        guard let firstLevel = viewModel.levels.first,
              let lastLevel = viewModel.levels.last else {
            XCTFail("Levels should not be empty")
            return
        }

        viewModel.selectLevel(lastLevel.id)
        XCTAssertEqual(viewModel.selectedLevelId, lastLevel.id)

        viewModel.selectLevel(firstLevel.id)
        XCTAssertEqual(viewModel.selectedLevelId, firstLevel.id)
    }

    // MARK: - Record Submission Tests

    func testSubmitGivenRecord() {
        viewModel.selectDeck(10)
        viewModel.onDigit("5")
        viewModel.onDigit("5")
        viewModel.onDigit("0")

        guard let levelId = viewModel.selectedLevelId else {
            XCTFail("Selected level id must not be nil")
            return
        }

        viewModel.submitRecord()

        XCTAssertEqual(viewModel.roomInput, "", "Room input should be cleared after submit")
        XCTAssertEqual(viewModel.selectedLevelId, levelId, "Level selection should be retained for rapid entry")
        XCTAssertEqual(viewModel.givenRecords.count, 1)

        let saved = viewModel.givenRecords.first!
        XCTAssertEqual(saved.deck, 10)
        XCTAssertEqual(saved.room, "550")
        XCTAssertEqual(saved.fullRoom, "10550")
        XCTAssertEqual(saved.direction, .given)
        XCTAssertEqual(saved.levelId, levelId)
        XCTAssertFalse(saved.replied)

        // Verify grouped view
        XCTAssertEqual(viewModel.groupedGiven.count, 1)
        XCTAssertEqual(viewModel.groupedGiven.first?.total, 1)
    }

    func testSubmitReceivedRecord() {
        viewModel.selectTab(.received)
        viewModel.selectDeck(9)
        viewModel.onDigit("1")
        viewModel.onDigit("2")
        viewModel.onDigit("0")

        viewModel.submitRecord()

        XCTAssertEqual(viewModel.receivedRecords.count, 1)
        let saved = viewModel.receivedRecords.first!
        XCTAssertEqual(saved.deck, 9)
        XCTAssertEqual(saved.room, "120")
        XCTAssertEqual(saved.direction, .received)
        XCTAssertFalse(saved.replied)
    }

    // MARK: - Auto-Reply Linkage Tests

    func testAutoReplyLinkageDirection1_GivenAutoRepliesEarliestReceived() {
        // Step 1: Add two unreplied received records for 11-550
        _ = pixieDustRepo.addRecord(
            direction: .received,
            deck: 11,
            room: "550",
            levelId: 1,
            levelNameSnapshot: "普通",
            note: "Gift 1",
            replied: false
        )
        // Ensure distinct timestamp
        let laterRecord = PixieDustRecord(
            id: 0,
            direction: .received,
            deck: 11,
            room: "550",
            levelId: 2,
            levelNameSnapshot: "精致",
            replied: false,
            note: "Gift 2",
            timestamp: Int64(Date().timeIntervalSince1970 * 1000) + 1000
        )
        pixieDustRepo.updateRecord(laterRecord)
        viewModel.loadData()

        XCTAssertEqual(viewModel.receivedRecords.filter { !$0.replied }.count, 2)

        // Step 2: Submit GIVEN record for 11-550
        viewModel.selectTab(.given)
        viewModel.selectDeck(11)
        viewModel.onDigit("5")
        viewModel.onDigit("5")
        viewModel.onDigit("0")
        viewModel.submitRecord()

        // Step 3: Verify earliest received record was auto-replied
        let updatedReceived = pixieDustRepo.getRecords(direction: .received)
        let repliedCount = updatedReceived.filter { $0.replied }.count
        let unrepliedCount = updatedReceived.filter { !$0.replied }.count

        XCTAssertEqual(repliedCount, 1)
        XCTAssertEqual(unrepliedCount, 1)
        XCTAssertEqual(viewModel.autoRepliedRoom, "11550")
    }

    func testAutoReplyLinkageDirection1Mirror_ReceivedAutoRepliedIfAlreadyGiven() {
        // Step 1: Send gift to 10-200
        viewModel.selectTab(.given)
        viewModel.selectDeck(10)
        viewModel.onDigit("2")
        viewModel.onDigit("0")
        viewModel.onDigit("0")
        viewModel.submitRecord()

        XCTAssertEqual(viewModel.givenRecords.count, 1)

        // Step 2: Receive gift from 10-200
        viewModel.selectTab(.received)
        viewModel.selectDeck(10)
        viewModel.onDigit("2")
        viewModel.onDigit("0")
        viewModel.onDigit("0")
        viewModel.submitRecord()

        // Step 3: Newly recorded received gift should be auto-marked as replied!
        XCTAssertEqual(viewModel.receivedRecords.count, 1)
        let received = viewModel.receivedRecords.first!
        XCTAssertTrue(received.replied, "Received record should be auto-replied because GIVEN already exists for this room")
        XCTAssertEqual(viewModel.autoRepliedRoom, "10200")
    }

    // MARK: - Two-Step Reply Confirmation Flow Tests

    func testTwoStepReplyFlowWithLevelCreatesGivenRecord() {
        // Add received record
        let received = pixieDustRepo.addRecord(
            direction: .received,
            deck: 8,
            room: "300",
            levelId: 1,
            levelNameSnapshot: "普通",
            note: "",
            replied: false
        )
        viewModel.loadData()

        // Toggle replied -> triggers pending dialog
        viewModel.toggleReplied(record: received)
        XCTAssertNotNil(viewModel.pendingReplyRecord)
        XCTAssertEqual(viewModel.pendingReplyRecord?.id, received.id)

        // Confirm reply with Level 3 (豪华)
        let luxuryLevel = viewModel.levels.first(where: { $0.rank == 3 })
        viewModel.confirmReply(record: received, replyLevel: luxuryLevel)

        XCTAssertNil(viewModel.pendingReplyRecord)

        // Check received is replied
        let updatedReceived = viewModel.receivedRecords.first(where: { $0.id == received.id })
        XCTAssertTrue(updatedReceived?.replied ?? false)

        // Check given record was created
        let newGiven = viewModel.givenRecords.first(where: { $0.deck == 8 && $0.room == "300" })
        XCTAssertNotNil(newGiven)
        XCTAssertEqual(newGiven?.levelId, luxuryLevel?.id)
    }

    func testTwoStepReplyFlowWithoutLevelOnlyMarksReplied() {
        let received = pixieDustRepo.addRecord(
            direction: .received,
            deck: 8,
            room: "400",
            levelId: 1,
            levelNameSnapshot: "普通",
            note: "",
            replied: false
        )
        viewModel.loadData()

        viewModel.toggleReplied(record: received)
        XCTAssertNotNil(viewModel.pendingReplyRecord)

        // Confirm reply with nil level (only mark checked)
        viewModel.confirmReply(record: received, replyLevel: nil)

        XCTAssertNil(viewModel.pendingReplyRecord)
        let updated = viewModel.receivedRecords.first(where: { $0.id == received.id })
        XCTAssertTrue(updated?.replied ?? false)
        XCTAssertEqual(viewModel.givenRecords.filter { $0.deck == 8 && $0.room == "400" }.count, 0)
    }

    func testCancelReplyFlow() {
        let received = pixieDustRepo.addRecord(
            direction: .received,
            deck: 8,
            room: "500",
            levelId: 1,
            levelNameSnapshot: "普通",
            note: "",
            replied: false
        )
        viewModel.loadData()

        viewModel.toggleReplied(record: received)
        XCTAssertNotNil(viewModel.pendingReplyRecord)

        viewModel.cancelReply()
        XCTAssertNil(viewModel.pendingReplyRecord)

        let updated = viewModel.receivedRecords.first(where: { $0.id == received.id })
        XCTAssertFalse(updated?.replied ?? true)
    }

    // MARK: - Edit and Delete Tests

    func testEditRecord() {
        viewModel.selectDeck(10)
        viewModel.onDigit("5")
        viewModel.onDigit("5")
        viewModel.onDigit("0")
        viewModel.submitRecord()

        guard var record = viewModel.givenRecords.first else {
            XCTFail("Record should exist")
            return
        }

        record.deck = 12
        record.room = "600"
        record.note = "Updated note"
        viewModel.updateRecord(record)

        let updated = viewModel.givenRecords.first!
        XCTAssertEqual(updated.deck, 12)
        XCTAssertEqual(updated.room, "600")
        XCTAssertEqual(updated.note, "Updated note")
    }

    func testDeleteRecord() {
        viewModel.selectDeck(10)
        viewModel.onDigit("5")
        viewModel.onDigit("5")
        viewModel.onDigit("0")
        viewModel.submitRecord()

        XCTAssertEqual(viewModel.givenRecords.count, 1)
        let record = viewModel.givenRecords.first!

        viewModel.deleteRecord(id: record.id)
        XCTAssertEqual(viewModel.givenRecords.count, 0)
    }

    // MARK: - Room History Preselection Tests

    func testRoomHistoryPreselection() {
        // Send a luxury gift (rank 3) to 10-550
        guard let luxuryLevel = viewModel.levels.first(where: { $0.rank == 3 }) else {
            XCTFail("Luxury level should exist")
            return
        }
        viewModel.selectDeck(10)
        viewModel.selectLevel(luxuryLevel.id)
        viewModel.onDigit("5")
        viewModel.onDigit("5")
        viewModel.onDigit("0")
        viewModel.submitRecord()

        // Reset level to normal (rank 1)
        if let normalLevel = viewModel.levels.first(where: { $0.rank == 1 }) {
            viewModel.selectLevel(normalLevel.id)
        }

        // Now type 10-550 again
        viewModel.onDigit("5")
        viewModel.onDigit("5")
        XCTAssertNil(viewModel.roomHistory, "History hint only visible on full 3 digits")

        viewModel.onDigit("0")
        // Level should be auto-preselected to luxury level (rank 3)
        XCTAssertEqual(viewModel.selectedLevelId, luxuryLevel.id)
        XCTAssertNotNil(viewModel.roomHistory)
        XCTAssertEqual(viewModel.roomHistory?.levelName, luxuryLevel.name)
        XCTAssertEqual(viewModel.roomHistory?.count, 1)
    }

    // MARK: - Domain Linkage Algorithms Tests

    func testAutoReplyTargetAlgorithm() {
        let r1 = PixieDustRecord(id: 1, direction: .received, deck: 11, room: "550", levelId: 1, levelNameSnapshot: "普通", replied: true, timestamp: 100)
        let r2 = PixieDustRecord(id: 2, direction: .received, deck: 11, room: "550", levelId: 2, levelNameSnapshot: "精致", replied: false, timestamp: 200)
        let r3 = PixieDustRecord(id: 3, direction: .received, deck: 11, room: "550", levelId: 3, levelNameSnapshot: "豪华", replied: false, timestamp: 300)
        let rOther = PixieDustRecord(id: 4, direction: .received, deck: 12, room: "550", levelId: 1, levelNameSnapshot: "普通", replied: false, timestamp: 150)

        let target = autoReplyTarget(received: [r1, r2, r3, rOther], deck: 11, room: "550")
        XCTAssertEqual(target?.id, 2, "Should pick earliest unreplied received record for matching room")

        let noTarget = autoReplyTarget(received: [r1], deck: 11, room: "550")
        XCTAssertNil(noTarget, "Should return nil if all matching records are replied")
    }

    func testHasGivenForRoomAlgorithm() {
        let g1 = PixieDustRecord(id: 1, direction: .given, deck: 11, room: "550", levelId: 1, levelNameSnapshot: "普通")
        let r1 = PixieDustRecord(id: 2, direction: .received, deck: 11, room: "550", levelId: 1, levelNameSnapshot: "普通")

        XCTAssertTrue(hasGivenForRoom(given: [g1], deck: 11, room: "550"))
        XCTAssertFalse(hasGivenForRoom(given: [g1], deck: 12, room: "550"))
        XCTAssertFalse(hasGivenForRoom(given: [r1], deck: 11, room: "550"))
    }

    func testPairByRoomAlgorithmOrdering() {
        // Room A (11-100): unreplied received rank 1
        let rA = PixieDustRecord(id: 1, direction: .received, deck: 11, room: "100", levelId: 1, levelNameSnapshot: "普通", replied: false, timestamp: 100)
        // Room B (11-200): unreplied received rank 3
        let rB = PixieDustRecord(id: 2, direction: .received, deck: 11, room: "200", levelId: 3, levelNameSnapshot: "豪华", replied: false, timestamp: 200)
        // Room C (11-300): balanced (1 given, 1 replied received)
        let gC = PixieDustRecord(id: 3, direction: .given, deck: 11, room: "300", levelId: 1, levelNameSnapshot: "普通", replied: false, timestamp: 300)
        let rC = PixieDustRecord(id: 4, direction: .received, deck: 11, room: "300", levelId: 1, levelNameSnapshot: "普通", replied: true, timestamp: 400)
        // Room D (11-400): unbalanced (2 given, 0 received)
        let gD1 = PixieDustRecord(id: 5, direction: .given, deck: 11, room: "400", levelId: 1, levelNameSnapshot: "普通", timestamp: 500)
        let gD2 = PixieDustRecord(id: 6, direction: .given, deck: 11, room: "400", levelId: 2, levelNameSnapshot: "精致", timestamp: 600)

        let pairs = buildRoomPairs(
            given: [gC, gD1, gD2],
            received: [rA, rB, rC],
            levels: PixieLevel.BUILT_IN_PIXIE_LEVELS
        )

        XCTAssertEqual(pairs.count, 4)
        // Unreplied received with highest rank (Room B, rank 3) comes first
        XCTAssertEqual(pairs[0].room, "200")
        // Unreplied received with lower rank (Room A, rank 1) comes second
        XCTAssertEqual(pairs[1].room, "100")
        // Unbalanced (Room D, 2 given vs 0 received) comes third
        XCTAssertEqual(pairs[2].room, "400")
        // Balanced (Room C) comes last
        XCTAssertEqual(pairs[3].room, "300")
    }

    func testSortReceivedAlgorithm() {
        let r1 = PixieDustRecord(id: 1, direction: .received, deck: 10, room: "100", levelId: 1, levelNameSnapshot: "普通", replied: false, timestamp: 100)
        let r2 = PixieDustRecord(id: 2, direction: .received, deck: 10, room: "200", levelId: 3, levelNameSnapshot: "豪华", replied: false, timestamp: 200)
        let r3 = PixieDustRecord(id: 3, direction: .received, deck: 10, room: "300", levelId: 2, levelNameSnapshot: "精致", replied: true, timestamp: 500)

        let sorted = sortReceived(records: [r1, r2, r3], levels: PixieLevel.BUILT_IN_PIXIE_LEVELS)

        XCTAssertEqual(sorted.map(\.id), [2, 1, 3], "Unreplied highest rank (2) -> Unreplied rank 1 (1) -> Replied (3)")
    }
}
