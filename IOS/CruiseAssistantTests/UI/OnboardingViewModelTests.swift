import XCTest
import SwiftUI
@testable import CruiseAssistant

final class OnboardingViewModelTests: XCTestCase {
    private var database: CruiseDatabase!
    private var categoryRepo: CategoryRepository!
    private var voyageRepo: VoyageRepository!
    private var settingsStore: SettingsStore!

    override func setUp() {
        super.setUp()
        database = CruiseDatabase.inMemory()
        categoryRepo = CategoryRepository(database: database)
        voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        settingsStore = SettingsStore.inMemory()
    }

    override func tearDown() {
        database = nil
        categoryRepo = nil
        voyageRepo = nil
        settingsStore = nil
        super.tearDown()
    }

    // MARK: - ShipPreset & Weekday Tests

    func testNextOccurrenceOfDayOfWeek() {
        // 2026-08-10 is Monday
        let monday = SimpleDate(year: 2026, month: 8, day: 10)
        XCTAssertEqual(monday.dayOfWeek, .monday)
        XCTAssertEqual(nextOccurrenceOfDayOfWeek(target: .monday, today: monday), monday)

        // 2026-08-11 is Tuesday; next Monday is 2026-08-17
        let tuesday = SimpleDate(year: 2026, month: 8, day: 11)
        XCTAssertEqual(tuesday.dayOfWeek, .tuesday)
        XCTAssertEqual(
            nextOccurrenceOfDayOfWeek(target: .monday, today: tuesday),
            SimpleDate(year: 2026, month: 8, day: 17)
        )

        // 2026-08-12 is Wednesday; next Thursday is 2026-08-13
        let wednesday = SimpleDate(year: 2026, month: 8, day: 12)
        XCTAssertEqual(wednesday.dayOfWeek, .wednesday)
        XCTAssertEqual(
            nextOccurrenceOfDayOfWeek(target: .thursday, today: wednesday),
            SimpleDate(year: 2026, month: 8, day: 13)
        )
    }

    func testBuiltInShipsDefinition() {
        XCTAssertEqual(BUILT_IN_SHIPS.count, 1)
        let disney = BUILT_IN_SHIPS[0]
        XCTAssertEqual(disney.name, "Disney Adventure")
        XCTAssertEqual(disney.maxDeck, 19)
        XCTAssertEqual(disney.minDeck, 5)
        XCTAssertEqual(disney.schedules.count, 2)

        let s1 = disney.schedules[0]
        XCTAssertEqual(s1.label, "四天三夜")
        XCTAssertEqual(s1.nights, 3)
        XCTAssertEqual(s1.boardDayOfWeek, .monday)
        XCTAssertEqual(s1.disembarkDayOfWeek, .thursday)
        XCTAssertEqual(s1.boardTime, SimpleTime(hour: 12, minute: 0))

        let s2 = disney.schedules[1]
        XCTAssertEqual(s2.label, "五天四夜")
        XCTAssertEqual(s2.nights, 4)
        XCTAssertEqual(s2.boardDayOfWeek, .thursday)
        XCTAssertEqual(s2.disembarkDayOfWeek, .monday)
        XCTAssertEqual(s2.boardTime, SimpleTime(hour: 12, minute: 0))
    }

    func testBoardingSlots() {
        let slots = boardingSlots()
        XCTAssertEqual(slots.count, 15)
        XCTAssertEqual(slots.first, SimpleTime(hour: 11, minute: 45))
        XCTAssertEqual(slots[1], SimpleTime(hour: 12, minute: 0))
        XCTAssertEqual(slots[13], SimpleTime(hour: 15, minute: 0))
        XCTAssertEqual(slots.last, SimpleTime(hour: 15, minute: 15))
    }

    // MARK: - Theme & SimpleDate Date Conversion Tests

    func testThemeColorInitializersAndCardColors() {
        let colorFromHex = Color(hex: "#0284C7")
        XCTAssertNotNil(colorFromHex)

        let colorFromArgb = Color(argb: 0xFF0284C7 as Int64)
        XCTAssertNotNil(colorFromArgb)

        let bg = cardBackgroundFor(colorArgb: 0xFFF5B301)
        XCTAssertNotNil(bg)

        let textCol = cardTextColorFor(colorArgb: 0xFFF5B301)
        XCTAssertNotNil(textCol)

        let theme = AppColorTheme.judy_blue
        XCTAssertEqual(theme.primaryArgb, 0xFF0284C7)
        XCTAssertNotNil(theme.primaryColor)
        XCTAssertNotNil(theme.secondaryColor)
        XCTAssertNotNil(theme.containerColor)
        XCTAssertNotNil(theme.palette)
    }

    func testSimpleDateDateBridge() {
        let simpleDate = SimpleDate(year: 2026, month: 8, day: 23)
        let swiftDate = simpleDate.toDate()
        let convertedBack = SimpleDate(date: swiftDate)

        XCTAssertEqual(convertedBack.year, 2026)
        XCTAssertEqual(convertedBack.month, 8)
        XCTAssertEqual(convertedBack.day, 23)
        XCTAssertEqual(simpleDate.chineseFormatted, "2026年8月23日")
    }

    // MARK: - OnboardingViewModel State Tests

    func testOnboardingViewModelInitialState() {
        let fixedToday = SimpleDate(year: 2026, month: 8, day: 10)
        let vm = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore,
            today: fixedToday
        )

        XCTAssertEqual(vm.shipName, "Disney Adventure")
        XCTAssertEqual(vm.selectedShipIndex, 0)
        XCTAssertEqual(vm.selectedScheduleIndex, -1)
        XCTAssertTrue(vm.isBuiltInShip)
        XCTAssertTrue(vm.isLockedDeck)
        XCTAssertEqual(vm.maxDeck, 19)
        XCTAssertEqual(vm.minDeck, 5)
        XCTAssertEqual(vm.boardDate, fixedToday)
        XCTAssertEqual(vm.disembarkDate, fixedToday.plusDays(5))
        XCTAssertEqual(vm.boardTime, SimpleTime(hour: 12, minute: 0))
        XCTAssertEqual(vm.dinnerTime, SimpleTime(hour: 17, minute: 45))
        XCTAssertFalse(vm.saving)
        XCTAssertNil(vm.errorMessage)
    }

    func testShipSelectionCustomAndBuiltIn() {
        let vm = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        // Select custom ship
        vm.onShipSelected(-1)
        XCTAssertEqual(vm.selectedShipIndex, -1)
        XCTAssertEqual(vm.shipName, "")
        XCTAssertFalse(vm.isBuiltInShip)
        XCTAssertFalse(vm.isLockedDeck)
        XCTAssertEqual(vm.maxDeck, 20)
        XCTAssertEqual(vm.minDeck, 1)

        vm.onShipNameChange("海洋光谱号")
        XCTAssertEqual(vm.shipName, "海洋光谱号")

        // Switch back to built-in ship
        vm.onShipSelected(0)
        XCTAssertEqual(vm.selectedShipIndex, 0)
        XCTAssertEqual(vm.shipName, "Disney Adventure")
        XCTAssertTrue(vm.isBuiltInShip)
        XCTAssertTrue(vm.isLockedDeck)
        XCTAssertEqual(vm.maxDeck, 19)
        XCTAssertEqual(vm.minDeck, 5)
    }

    func testScheduleSelectionUpdatesDates() {
        // Start on Tuesday 2026-08-11
        let tuesday = SimpleDate(year: 2026, month: 8, day: 11)
        let vm = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore,
            today: tuesday
        )

        // Select 4-day 3-night schedule (Mon-Thu) from Tuesday 08-11
        vm.onScheduleSelected(0)
        XCTAssertEqual(vm.selectedScheduleIndex, 0)
        XCTAssertEqual(vm.boardDate, SimpleDate(year: 2026, month: 8, day: 17)) // Next Monday
        XCTAssertEqual(vm.disembarkDate, SimpleDate(year: 2026, month: 8, day: 20)) // Thu (+3 days)
        XCTAssertEqual(vm.boardTime, SimpleTime(hour: 12, minute: 0))

        // Select 5-day 4-night schedule (Thu-Mon) from Monday 08-17 -> next Thursday is 08-20
        vm.onScheduleSelected(1)
        XCTAssertEqual(vm.selectedScheduleIndex, 1)
        XCTAssertEqual(vm.boardDate, SimpleDate(year: 2026, month: 8, day: 20)) // Next Thursday after 08-17
        XCTAssertEqual(vm.disembarkDate, SimpleDate(year: 2026, month: 8, day: 24)) // Mon (+4 days)

        // Changing board date preserves 4 nights automatically
        let newBoardDate = SimpleDate(year: 2026, month: 8, day: 27)
        vm.onBoardDateChange(newBoardDate)
        XCTAssertEqual(vm.boardDate, newBoardDate)
        XCTAssertEqual(vm.disembarkDate, SimpleDate(year: 2026, month: 8, day: 31))

        // Fresh VM starting on Tuesday 08-11 selecting schedule 1 gives 08-13 (next Thursday)
        let vm2 = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore,
            today: tuesday
        )
        vm2.onScheduleSelected(1)
        XCTAssertEqual(vm2.boardDate, SimpleDate(year: 2026, month: 8, day: 13))
        XCTAssertEqual(vm2.disembarkDate, SimpleDate(year: 2026, month: 8, day: 17))
    }

    func testValidationDisembarkBeforeBoardDate() {
        let vm = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let board = SimpleDate(year: 2026, month: 8, day: 20)
        let invalidDisembark = SimpleDate(year: 2026, month: 8, day: 15)

        vm.onBoardDateChange(board)
        vm.onDisembarkDateChange(invalidDisembark)

        var completed = false
        vm.submit {
            completed = true
        }

        XCTAssertFalse(completed)
        XCTAssertEqual(vm.errorMessage, "下船日期不能早于登船日期")
        XCTAssertNil(voyageRepo.getVoyage())
        XCTAssertFalse(settingsStore.getSettings().onboardingCompleted)
    }

    func testSuccessfulSubmission() {
        let vm = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore,
            today: SimpleDate(year: 2026, month: 8, day: 10)
        )

        // Select 4-day 3-night schedule
        vm.onScheduleSelected(0)
        vm.onDinnerTimeChange(SimpleTime(hour: 20, minute: 15))
        vm.onBoardTimeChange(SimpleTime(hour: 13, minute: 15))

        var callbackInvoked = false
        vm.submit {
            callbackInvoked = true
        }

        XCTAssertTrue(callbackInvoked)
        XCTAssertFalse(vm.saving)
        XCTAssertNil(vm.errorMessage)

        // Check settings store updated
        let settings = settingsStore.getSettings()
        XCTAssertTrue(settings.onboardingCompleted)
        XCTAssertEqual(settings.dinnerTime, SimpleTime(hour: 20, minute: 15))
        XCTAssertEqual(settings.lastViewedDate, SimpleDate(year: 2026, month: 8, day: 10))

        // Check voyage created
        let voyage = voyageRepo.getVoyage()
        XCTAssertNotNil(voyage)
        XCTAssertEqual(voyage?.shipName, "Disney Adventure")
        XCTAssertEqual(voyage?.boardDate, SimpleDate(year: 2026, month: 8, day: 10))
        XCTAssertEqual(voyage?.disembarkDate, SimpleDate(year: 2026, month: 8, day: 13))
        XCTAssertEqual(voyage?.boardTime, SimpleTime(hour: 13, minute: 15))
        XCTAssertEqual(voyage?.maxDeck, 19)
        XCTAssertEqual(voyage?.minDeck, 5)

        // Check DayPlans created
        let dayPlans = voyageRepo.getDayPlans()
        XCTAssertEqual(dayPlans.count, 4) // 10, 11, 12, 13

        // Check events generated (Boarding, Disembark, Safety Drill, Dinners)
        let events = database.getAllEvents()
        XCTAssertTrue(events.contains(where: { $0.title == "登船" }))
        XCTAssertTrue(events.contains(where: { $0.title == "下船" }))
        XCTAssertTrue(events.contains(where: { $0.title == "安全演习" }))
        let dinnerEvents = events.filter { $0.title == "晚餐" }
        XCTAssertEqual(dinnerEvents.count, 3) // 3 dinners for 4-day 3-night cruise
        XCTAssertTrue(dinnerEvents.allSatisfy { $0.startTime == SimpleTime(hour: 20, minute: 15) })
    }

    func testMaxDeckClamping() {
        let vm = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        vm.onShipSelected(-1) // Custom ship (minDeck 1)
        vm.onMaxDeckChange(35)
        XCTAssertEqual(vm.maxDeck, 30) // Clamped to 30

        vm.onMaxDeckChange(0)
        XCTAssertEqual(vm.maxDeck, 1) // Clamped to minDeck (1)

        vm.onMaxDeckChange(15)
        XCTAssertEqual(vm.maxDeck, 15)
    }
}
