import XCTest
import SwiftUI
@testable import CruiseAssistant

final class TimelineViewModelTests: XCTestCase {
    private var database: CruiseDatabase!
    private var categoryRepo: CategoryRepository!
    private var voyageRepo: VoyageRepository!
    private var eventRepo: EventRepository!
    private var settingsStore: SettingsStore!

    private let boardDate = SimpleDate(year: 2026, month: 9, day: 3)
    private let day2Date = SimpleDate(year: 2026, month: 9, day: 4)
    private let disembarkDate = SimpleDate(year: 2026, month: 9, day: 7)

    override func setUp() {
        super.setUp()
        database = CruiseDatabase.inMemory()
        categoryRepo = CategoryRepository(database: database)
        categoryRepo.seedBuiltInCategories()
        voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        eventRepo = EventRepository(database: database)
        settingsStore = SettingsStore.inMemory()

        _ = voyageRepo.createVoyage(
            shipName: "迪士尼愿望号",
            boardDate: boardDate,
            boardTime: SimpleTime(hour: 12, minute: 30),
            disembarkDate: disembarkDate,
            disembarkTime: SimpleTime(hour: 7, minute: 0),
            maxDeck: 14,
            minDeck: 1,
            dinnerTime: SimpleTime(hour: 17, minute: 45),
            shipPresetName: "wish"
        )
    }

    override func tearDown() {
        database = nil
        categoryRepo = nil
        voyageRepo = nil
        eventRepo = nil
        settingsStore = nil
        super.tearDown()
    }

    // MARK: - Initial Date Selection Tests

    func testInitialDateSelection_todayInVoyage() {
        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.day2Date }
        )

        XCTAssertEqual(vm.selectedDate, day2Date)
        XCTAssertTrue(vm.isToday)
        XCTAssertTrue(vm.todayInVoyage)
    }

    func testInitialDateSelection_todayOutsideVoyage() {
        let outsideDate = SimpleDate(year: 2026, month: 1, day: 1)
        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { outsideDate }
        )

        XCTAssertEqual(vm.selectedDate, boardDate)
        XCTAssertFalse(vm.isToday)
        XCTAssertFalse(vm.todayInVoyage)
    }

    // MARK: - Date Selection & Navigation Tests

    func testDateSelection_loadsEventsAndUpdatesSelectedDate() {
        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: day2Date,
                startTime: SimpleTime(hour: 10, minute: 0),
                durationMinutes: 60,
                categoryId: 1,
                title: "第二天早晨剧场"
            ),
            recurrenceDays: []
        )

        vm.selectDate(day2Date)

        XCTAssertEqual(vm.selectedDate, day2Date)
        XCTAssertEqual(settingsStore.settings.lastViewedDate, day2Date)
        XCTAssertTrue(vm.events.contains(where: { $0.title == "第二天早晨剧场" }))
    }

    func testToggleFullDay_recomputesLayoutRange() {
        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        XCTAssertFalse(vm.fullDayOverride)
        XCTAssertEqual(vm.layout.rangeStartMinute, settingsStore.settings.displayStartMinute)
        XCTAssertEqual(vm.layout.rangeEndMinute, settingsStore.settings.displayEndMinute)

        vm.toggleFullDay()

        XCTAssertTrue(vm.fullDayOverride)
        XCTAssertEqual(vm.layout.rangeStartMinute, 0)
        XCTAssertEqual(vm.layout.rangeEndMinute, 1440)

        // Switching dates should reset fullDayOverride
        vm.selectDate(day2Date)
        XCTAssertFalse(vm.fullDayOverride)
        XCTAssertEqual(vm.layout.rangeStartMinute, settingsStore.settings.displayStartMinute)
    }

    // MARK: - Event Mutation Tests

    func testMoveToLane_updatesEventLane() {
        let added = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 14, minute: 0),
                durationMinutes: 60,
                categoryId: 1,
                title: "泳池派对",
                lane: 0
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        XCTAssertEqual(vm.events.first(where: { $0.id == added.id })?.lane, 0)

        vm.moveToLane(eventId: added.id, lane: 2)

        XCTAssertEqual(vm.events.first(where: { $0.id == added.id })?.lane, 2)
        XCTAssertEqual(eventRepo.getEvent(byId: added.id)?.lane, 2)
    }

    func testToggleActive_togglesEventState() {
        let added = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 14, minute: 0),
                durationMinutes: 60,
                categoryId: 1,
                title: "泳池派对",
                isActive: true
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        XCTAssertEqual(vm.events.first(where: { $0.id == added.id })?.isActive, true)

        vm.toggleActive(eventId: added.id)

        XCTAssertEqual(vm.events.first(where: { $0.id == added.id })?.isActive, false)
        XCTAssertEqual(eventRepo.getEvent(byId: added.id)?.isActive, false)
    }

    func testKeepOnlyThisShow_deactivatesOtherShows() {
        let show1 = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 18, minute: 0),
                durationMinutes: 60,
                categoryId: 2,
                title: "阿拉丁音乐剧",
                isActive: true,
                presetKey: "aladdin"
            ),
            recurrenceDays: []
        )
        let show2 = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 20, minute: 30),
                durationMinutes: 60,
                categoryId: 2,
                title: "阿拉丁音乐剧",
                isActive: true,
                presetKey: "aladdin"
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        XCTAssertEqual(vm.events.filter { $0.title == "阿拉丁音乐剧" && $0.isActive }.count, 2)

        vm.keepOnlyThisShow(show2)

        XCTAssertEqual(eventRepo.getEvent(byId: show1.id)?.isActive, false)
        XCTAssertEqual(eventRepo.getEvent(byId: show2.id)?.isActive, true)
    }

    func testDeleteEvent_removesEvent() {
        let event = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 10, minute: 0),
                durationMinutes: 30,
                categoryId: 3,
                title: "临时会议"
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        XCTAssertTrue(vm.events.contains(where: { $0.id == event.id }))

        vm.deleteEvent(id: event.id)

        XCTAssertFalse(vm.events.contains(where: { $0.id == event.id }))
        XCTAssertNil(eventRepo.getEvent(byId: event.id))
    }

    // MARK: - Finalization & Conflict Tests

    func testFinalizeDay_withoutConflicts_finalizesDirectly() {
        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        XCTAssertFalse(vm.isFinalized)

        vm.requestFinalizeDay()

        XCTAssertTrue(vm.isFinalized)
        XCTAssertFalse(vm.showConflictDialog)
        XCTAssertTrue(vm.finalizedDates.contains(boardDate))
    }

    func testFinalizeDay_withConflicts_showsConflictDialog() {
        // Add overlapping active events on the same day
        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 14, minute: 0),
                durationMinutes: 60,
                categoryId: 1,
                title: "活动 A"
            ),
            recurrenceDays: []
        )
        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 14, minute: 30),
                durationMinutes: 60,
                categoryId: 2,
                title: "活动 B"
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        XCTAssertFalse(vm.isFinalized)
        XCTAssertFalse(vm.showConflictDialog)

        vm.requestFinalizeDay()

        XCTAssertFalse(vm.isFinalized)
        XCTAssertTrue(vm.showConflictDialog)
        XCTAssertEqual(vm.conflicts.count, 1)

        // If user chooses "仍然定稿"
        vm.finalizeDay(boardDate)

        XCTAssertTrue(vm.isFinalized)
        XCTAssertFalse(vm.showConflictDialog)
    }

    func testUnlockDay_clearsFinalizedState() {
        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        vm.finalizeDay(boardDate)
        XCTAssertTrue(vm.isFinalized)

        vm.unlockDay(boardDate)
        XCTAssertFalse(vm.isFinalized)
    }

    func testVisibleEvents_whenFinalized_hidesInactiveEvents() {
        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 10, minute: 0),
                durationMinutes: 60,
                categoryId: 1,
                title: "活动 1 (有效)",
                isActive: true
            ),
            recurrenceDays: []
        )
        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 11, minute: 0),
                durationMinutes: 60,
                categoryId: 1,
                title: "活动 2 (无效)",
                isActive: false
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 0) },
            todayProvider: { self.boardDate }
        )

        // In planning mode, both active and inactive events are present
        XCTAssertTrue(vm.visibleEvents.contains(where: { $0.title == "活动 1 (有效)" }))
        XCTAssertTrue(vm.visibleEvents.contains(where: { $0.title == "活动 2 (无效)" }))

        // In finalized mode, inactive events are hidden
        vm.finalizeDay(boardDate)
        XCTAssertTrue(vm.visibleEvents.contains(where: { $0.title == "活动 1 (有效)" }))
        XCTAssertFalse(vm.visibleEvents.contains(where: { $0.title == "活动 2 (无效)" }))
    }

    // MARK: - Ongoing & Next Event Calculation Tests

    func testOngoingAndNextEvent_onToday() {
        // Events:
        // Event 1: 13:00 - 15:00 (ongoing at 14:15)
        // Event 2: 15:30 - 16:30 (next)
        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 13, minute: 0),
                durationMinutes: 120,
                categoryId: 1,
                title: "甲板烧烤"
            ),
            recurrenceDays: []
        )
        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: boardDate,
                startTime: SimpleTime(hour: 15, minute: 30),
                durationMinutes: 60,
                categoryId: 2,
                title: "魔术秀"
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 15) },
            todayProvider: { self.boardDate }
        )

        XCTAssertNotNil(vm.ongoing)
        XCTAssertEqual(vm.ongoing?.title, "甲板烧烤")
        XCTAssertNotNil(vm.next)
        XCTAssertEqual(vm.next?.title, "魔术秀")
    }

    func testOngoingAndNextEvent_nilOnOtherDays() {
        _ = eventRepo.addEvent(
            event: CruiseEvent(
                date: day2Date,
                startTime: SimpleTime(hour: 13, minute: 0),
                durationMinutes: 120,
                categoryId: 1,
                title: "甲板烧烤"
            ),
            recurrenceDays: []
        )

        let vm = TimelineViewModel(
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            settingsStore: settingsStore,
            nowProvider: { SimpleTime(hour: 14, minute: 15) },
            todayProvider: { self.boardDate }
        )

        vm.selectDate(day2Date)

        XCTAssertFalse(vm.isToday)
        XCTAssertNil(vm.ongoing)
        XCTAssertNil(vm.next)
    }

    // MARK: - Formatting & Helper Function Tests

    func testDateStripWeekdayChineseHelper() {
        // 2026-09-03 is Thursday -> "周四"
        let date = SimpleDate(year: 2026, month: 9, day: 3)
        XCTAssertEqual(weekdayChinese(for: date), "周四")

        // 2026-09-04 is Friday -> "周五"
        let dateFriday = SimpleDate(year: 2026, month: 9, day: 4)
        XCTAssertEqual(weekdayChinese(for: dateFriday), "周五")
    }

    func testEventCardFormattingHelpers() {
        XCTAssertEqual(formatDuration(minutes: 30), "30m")
        XCTAssertEqual(formatDuration(minutes: 60), "1h")
        XCTAssertEqual(formatDuration(minutes: 90), "1h 30m")
        XCTAssertEqual(formatDuration(minutes: 120), "2h")

        let event = CruiseEvent(
            date: boardDate,
            startTime: SimpleTime(hour: 14, minute: 5),
            durationMinutes: 45,
            categoryId: 1,
            title: "活动"
        )
        XCTAssertEqual(formatStart(event: event), "14:05")
        XCTAssertEqual(formatTimeRange(event: event), "14:05–14:50  45m")
    }

    func testLaneBoundaryFractions() {
        let fractions = laneBoundaryFractions()
        XCTAssertEqual(fractions.count, 2)
        XCTAssertEqual(fractions[0], 1.0 / 3.0, accuracy: 0.001)
        XCTAssertEqual(fractions[1], 2.0 / 3.0, accuracy: 0.001)
    }
}
