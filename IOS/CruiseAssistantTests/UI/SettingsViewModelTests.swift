import XCTest
@testable import CruiseAssistant

@MainActor
final class SettingsViewModelTests: XCTestCase {
    private var database: CruiseDatabase!
    private var settingsStore: SettingsStore!
    private var voyageRepo: VoyageRepository!
    private var eventRepo: EventRepository!
    private var categoryRepo: CategoryRepository!
    private var viewModel: SettingsViewModel!

    override func setUp() {
        super.setUp()
        database = CruiseDatabase.inMemory()
        settingsStore = SettingsStore.inMemory()
        categoryRepo = CategoryRepository(database: database)
        categoryRepo.seedBuiltInCategories()
        voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        eventRepo = EventRepository(database: database)

        _ = voyageRepo.createVoyage(
            shipName: "迪士尼探险号",
            boardDate: SimpleDate(year: 2026, month: 8, day: 24),
            boardTime: SimpleTime(hour: 12, minute: 0),
            disembarkDate: SimpleDate(year: 2026, month: 8, day: 28),
            disembarkTime: SimpleTime(hour: 8, minute: 0),
            maxDeck: 20,
            minDeck: 1,
            dinnerTime: SimpleTime(hour: 17, minute: 45)
        )

        viewModel = SettingsViewModel(
            settingsStore: settingsStore,
            voyageRepository: voyageRepo,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            database: database
        )
    }

    override func tearDown() {
        viewModel = nil
        eventRepo = nil
        voyageRepo = nil
        categoryRepo = nil
        settingsStore = nil
        database = nil
        super.tearDown()
    }

    // MARK: - Theme Tests

    func testSetColorTheme() {
        XCTAssertEqual(viewModel.settings.colorTheme, .judy_blue)

        viewModel.setColorTheme(.linabell_pink)
        XCTAssertEqual(settingsStore.getSettings().colorTheme, .linabell_pink)
        XCTAssertEqual(viewModel.settings.colorTheme, .linabell_pink)

        viewModel.setColorTheme(.duffy_blue)
        XCTAssertEqual(settingsStore.getSettings().colorTheme, .duffy_blue)
        XCTAssertEqual(viewModel.settings.colorTheme, .duffy_blue)
    }

    // MARK: - Wallpaper Tests

    func testWallpaperSettings() {
        XCTAssertEqual(viewModel.settings.wallpaperKey, WallpaperPreset.none.rawValue)

        viewModel.setWallpaperKey(WallpaperPreset.duffy.rawValue)
        XCTAssertEqual(settingsStore.getSettings().wallpaperKey, WallpaperPreset.duffy.rawValue)
        XCTAssertTrue(settingsStore.getSettings().wallpaperEnabled)

        viewModel.setWallpaperAlpha(0.25)
        XCTAssertEqual(settingsStore.getSettings().wallpaperAlpha, 0.25, accuracy: 0.001)

        viewModel.setWallpaperEnabled(false)
        XCTAssertFalse(settingsStore.getSettings().wallpaperEnabled)
    }

    // MARK: - Timeline Display Preferences

    func testTimelineDisplayPreferences() {
        viewModel.setHourHeight(150.0)
        XCTAssertEqual(settingsStore.getSettings().hourHeightPt, 150.0)

        viewModel.setCollapseEmptyGaps(true)
        XCTAssertTrue(settingsStore.getSettings().collapseEmptyGaps)

        viewModel.setDisplayRange(startMinute: 480, endMinute: 1320)
        XCTAssertEqual(settingsStore.getSettings().displayStartMinute, 480)
        XCTAssertEqual(settingsStore.getSettings().displayEndMinute, 1320)

        // Invalid short range (< 120 minutes) should be rejected
        viewModel.setDisplayRange(startMinute: 600, endMinute: 660)
        XCTAssertNotNil(viewModel.errorMessage)
        // Range should remain unchanged
        XCTAssertEqual(settingsStore.getSettings().displayStartMinute, 480)
    }

    // MARK: - Notification Preferences

    func testNotificationPreferences() {
        viewModel.setNotificationsEnabled(false)
        XCTAssertFalse(settingsStore.getSettings().notificationsEnabled)

        viewModel.setRemindBefore15(false)
        XCTAssertFalse(settingsStore.getSettings().remindBefore15)

        viewModel.setRemindBefore5(false)
        XCTAssertFalse(settingsStore.getSettings().remindBefore5)

        viewModel.setRemindAtStart(false)
        XCTAssertFalse(settingsStore.getSettings().remindAtStart)
    }

    // MARK: - Dinner Time Updates

    func testSetDinnerTime() {
        let newDinnerTime = SimpleTime(hour: 20, minute: 15)
        viewModel.setDinnerTime(newDinnerTime)

        XCTAssertEqual(settingsStore.getSettings().dinnerTime, newDinnerTime)

        // Verify dinner events in database were updated
        let events = eventRepo.getAllEvents()
        let dinnerEvents = events.filter { $0.title == "主餐厅晚餐" || $0.title == "晚餐" }
        for dinner in dinnerEvents {
            XCTAssertEqual(dinner.startTime, newDinnerTime)
        }
    }

    // MARK: - Voyage Updates

    func testUpdateVoyage() {
        let newBoardDate = SimpleDate(year: 2026, month: 9, day: 1)
        let newDisembarkDate = SimpleDate(year: 2026, month: 9, day: 5)
        let newBoardTime = SimpleTime(hour: 11, minute: 30)
        let newDisembarkTime = SimpleTime(hour: 9, minute: 0)
        let newDinnerTime = SimpleTime(hour: 18, minute: 0)

        viewModel.updateVoyage(
            shipName: "迪士尼愿望号",
            boardDate: newBoardDate,
            boardTime: newBoardTime,
            disembarkDate: newDisembarkDate,
            disembarkTime: newDisembarkTime,
            dinnerTime: newDinnerTime
        )

        let updatedVoyage = voyageRepo.getVoyage()
        XCTAssertEqual(updatedVoyage?.shipName, "迪士尼愿望号")
        XCTAssertEqual(updatedVoyage?.boardDate, newBoardDate)
        XCTAssertEqual(updatedVoyage?.disembarkDate, newDisembarkDate)
        XCTAssertEqual(updatedVoyage?.boardTime, newBoardTime)
        XCTAssertEqual(updatedVoyage?.disembarkTime, newDisembarkTime)
        XCTAssertEqual(settingsStore.getSettings().dinnerTime, newDinnerTime)
    }

    // MARK: - Reset Voyage & Export Backup

    func testResetVoyage() {
        var callbackCalled = false
        viewModel.resetVoyage {
            callbackCalled = true
        }

        XCTAssertTrue(callbackCalled)
        XCTAssertNil(voyageRepo.getVoyage())
        XCTAssertNil(viewModel.voyage)
        XCTAssertFalse(settingsStore.getSettings().onboardingCompleted)
    }

    func testExportBackupJSON() {
        let json = viewModel.exportBackupJSON()
        XCTAssertNotNil(json)
        XCTAssertTrue(json?.contains("voyage") == true)
        XCTAssertTrue(json?.contains("categories") == true)
    }
}
