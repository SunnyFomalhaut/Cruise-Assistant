import XCTest
@testable import CruiseAssistant

final class EventSheetViewModelTests: XCTestCase {
    private var database: CruiseDatabase!
    private var categoryRepo: CategoryRepository!
    private var voyageRepo: VoyageRepository!
    private var eventRepo: EventRepository!
    private var locationPresetRepo: LocationPresetRepository!
    private var settingsStore: SettingsStore!

    private let sampleDate = SimpleDate(year: 2026, month: 8, day: 24)

    override func setUp() {
        super.setUp()
        database = CruiseDatabase.inMemory()
        categoryRepo = CategoryRepository(database: database)
        categoryRepo.seedBuiltInCategories()
        voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        eventRepo = EventRepository(database: database)
        locationPresetRepo = LocationPresetRepository(database: database)
        settingsStore = SettingsStore(userDefaults: UserDefaults(suiteName: "Test_EventSheet_\(UUID().uuidString)")!)

        // Seed a voyage for Disney Adventure
        _ = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: sampleDate,
            boardTime: SimpleTime(hour: 12, minute: 0),
            disembarkDate: sampleDate.plusDays(3),
            disembarkTime: SimpleTime(hour: 7, minute: 0),
            maxDeck: 19,
            minDeck: 5,
            dinnerTime: SimpleTime(hour: 18, minute: 0)
        )
        LocationCatalog.loadDefault()
    }

    override func tearDown() {
        database = nil
        categoryRepo = nil
        voyageRepo = nil
        eventRepo = nil
        locationPresetRepo = nil
        settingsStore = nil
        super.tearDown()
    }

    // MARK: - Initial State Tests

    func testCreateInitialState() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            defaultStartMinute: 600, // 10:00
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        XCTAssertFalse(vm.isEditing)
        XCTAssertEqual(vm.editingId, 0)
        XCTAssertEqual(vm.date, sampleDate)
        XCTAssertEqual(vm.startTime, SimpleTime(hour: 10, minute: 0))
        XCTAssertEqual(vm.endTime, SimpleTime(hour: 10, minute: 30))
        XCTAssertEqual(vm.durationMinutes, 30)
        XCTAssertFalse(vm.categories.isEmpty)
        XCTAssertNotNil(vm.selectedCategoryId)
        XCTAssertTrue(vm.builtInShip)
        XCTAssertEqual(vm.maxDeck, 19)
        XCTAssertEqual(vm.minDeck, 5)
        XCTAssertFalse(vm.recurring)
        XCTAssertNil(vm.errorMessage)
    }

    func testEditInitialState() {
        let categories = categoryRepo.getCategories()
        let funCategory = categories.first { $0.iconKey == "fun" } ?? categories[0]

        let existing = CruiseEvent(
            id: 0,
            date: sampleDate,
            startTime: SimpleTime(hour: 14, minute: 0),
            durationMinutes: 45,
            categoryId: funCategory.id,
            title: "漫威探索",
            deck: 11,
            zone: .mid,
            placeName: "漫威体验区",
            lane: 1,
            recurrenceGroupId: "group_123"
        )
        let saved = database.saveEvent(existing)

        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: saved.id,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        XCTAssertTrue(vm.isEditing)
        XCTAssertEqual(vm.editingId, saved.id)
        XCTAssertEqual(vm.title, "漫威探索")
        XCTAssertEqual(vm.selectedCategoryId, funCategory.id)
        XCTAssertEqual(vm.deck, 11)
        XCTAssertEqual(vm.zone, .mid)
        XCTAssertEqual(vm.placeName, "漫威体验区")
        XCTAssertFalse(vm.noLocation)
        XCTAssertEqual(vm.startTime, SimpleTime(hour: 14, minute: 0))
        XCTAssertEqual(vm.endTime, SimpleTime(hour: 14, minute: 45))
        XCTAssertEqual(vm.durationMinutes, 45)
        XCTAssertTrue(vm.isRecurrenceMember)
        XCTAssertEqual(vm.recurrenceGroupId, "group_123")
    }

    // MARK: - Category Selection & Presets

    func testCategorySelectionUpdatesUIModes() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let showCat = vm.categories.first { $0.iconKey == "show" }!
        vm.onCategorySelect(id: showCat.id)

        XCTAssertEqual(vm.selectedCategoryId, showCat.id)
        XCTAssertEqual(vm.categoryIconKey, "show")
        XCTAssertTrue(vm.showPresetMode)
        XCTAssertTrue(vm.locationPresetMode)
        XCTAssertFalse(vm.showMealPeriodRow)

        let choreCat = vm.categories.first { $0.iconKey == "chore" }!
        vm.onCategorySelect(id: choreCat.id)

        XCTAssertEqual(vm.selectedCategoryId, choreCat.id)
        XCTAssertEqual(vm.categoryIconKey, "chore")
        XCTAssertFalse(vm.showPresetMode)
        XCTAssertFalse(vm.showMealPeriodRow)
    }

    func testShowPresetSelectionAutoFill() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let showCat = vm.categories.first { $0.iconKey == "show" }!
        vm.onCategorySelect(id: showCat.id)

        let preset = DISNEY_ADVENTURE_SHOWS.first { $0.key == "show_lion_king" }!
        vm.onShowPresetSelect(preset)

        XCTAssertEqual(vm.title, preset.title)
        XCTAssertEqual(vm.deck, preset.deck)
        XCTAssertEqual(vm.zone, preset.zone)
        XCTAssertEqual(vm.placeName, preset.placeName)
        XCTAssertFalse(vm.noLocation)
        XCTAssertEqual(vm.startTime, preset.startTimes.first)
        XCTAssertEqual(vm.durationMinutes, preset.durationMinutes)
        XCTAssertEqual(vm.waitMinutes, preset.defaultWaitMinutes)
    }

    // MARK: - Meal Period Selection

    func testMealPeriodSelectionAdjustsTimes() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let mealCat = vm.categories.first { $0.iconKey == "meal" }!
        vm.onCategorySelect(id: mealCat.id)
        XCTAssertTrue(vm.showMealPeriodRow)

        // Select Breakfast
        vm.onMealPeriodSelect(.breakfast)
        XCTAssertEqual(vm.selectedMealPeriod, .breakfast)
        XCTAssertEqual(vm.startTime, SimpleTime(hour: 8, minute: 0))
        XCTAssertEqual(vm.endTime, SimpleTime(hour: 8, minute: 30))

        // Select Lunch
        vm.onMealPeriodSelect(.lunch)
        XCTAssertEqual(vm.selectedMealPeriod, .lunch)
        XCTAssertEqual(vm.startTime, SimpleTime(hour: 12, minute: 0))
        XCTAssertEqual(vm.endTime, SimpleTime(hour: 12, minute: 30))

        // Select Dinner
        vm.onMealPeriodSelect(.dinner)
        XCTAssertEqual(vm.selectedMealPeriod, .dinner)
        XCTAssertEqual(vm.startTime, SimpleTime(hour: 18, minute: 0))
        XCTAssertEqual(vm.endTime, SimpleTime(hour: 18, minute: 30))
    }

    // MARK: - Time and Duration Calculations

    func testTimeChangesAndDurationComputation() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            defaultStartMinute: 600,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        vm.onStartTimeChange(SimpleTime(hour: 10, minute: 15))
        vm.onEndTimeChange(SimpleTime(hour: 11, minute: 45))

        XCTAssertEqual(vm.startTime, SimpleTime(hour: 10, minute: 15))
        XCTAssertEqual(vm.endTime, SimpleTime(hour: 11, minute: 45))
        XCTAssertEqual(vm.durationMinutes, 90)

        // Start time moved past current end time auto-pushes end time by 30 mins
        vm.onStartTimeChange(SimpleTime(hour: 12, minute: 0))
        XCTAssertEqual(vm.startTime, SimpleTime(hour: 12, minute: 0))
        XCTAssertEqual(vm.endTime, SimpleTime(hour: 12, minute: 30))
        XCTAssertEqual(vm.durationMinutes, 30)
    }

    // MARK: - Location Selection

    func testLocationCustomInputAndNoLocationToggle() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        vm.onDeckChange(7)
        vm.onZoneChange(.bow)
        vm.onPlaceNameChange("船头甲板")

        XCTAssertEqual(vm.deck, 7)
        XCTAssertEqual(vm.zone, .bow)
        XCTAssertEqual(vm.placeName, "船头甲板")
        XCTAssertFalse(vm.noLocation)

        // Toggle No Location ON
        vm.onNoLocationToggle(true)
        XCTAssertTrue(vm.noLocation)
        XCTAssertNil(vm.deck)

        // Toggle No Location OFF
        vm.onNoLocationToggle(false)
        XCTAssertFalse(vm.noLocation)
    }

    // MARK: - Save Event Tests

    func testSaveNewEvent() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            defaultStartMinute: 900, // 15:00
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let choreCat = vm.categories.first { $0.iconKey == "chore" }!
        vm.onCategorySelect(id: choreCat.id)
        vm.onTitleChange("甲板散步")
        vm.onDeckChange(10)
        vm.onZoneChange(.stern)
        vm.onPlaceNameChange("船尾慢跑道")
        vm.onStartTimeChange(SimpleTime(hour: 15, minute: 0))
        vm.onEndTimeChange(SimpleTime(hour: 15, minute: 45))

        var doneCalled = false
        vm.save {
            doneCalled = true
        }

        XCTAssertTrue(doneCalled)

        let events = eventRepo.getEvents(for: sampleDate)
        let created = events.first { $0.title == "甲板散步" }
        XCTAssertNotNil(created)
        XCTAssertEqual(created?.startTime, SimpleTime(hour: 15, minute: 0))
        XCTAssertEqual(created?.durationMinutes, 45)
        XCTAssertEqual(created?.deck, 10)
        XCTAssertEqual(created?.zone, .stern)
        XCTAssertEqual(created?.placeName, "船尾慢跑道")

        // Location preset recorded
        let presets = locationPresetRepo.getPresets()
        XCTAssertTrue(presets.contains { $0.placeName == "船尾慢跑道" })
    }

    func testSaveValidationErrors() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            defaultStartMinute: 600,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        // Invalid: End time <= Start time
        vm.onStartTimeChange(SimpleTime(hour: 14, minute: 0))
        vm.onEndTimeChange(SimpleTime(hour: 13, minute: 0))

        var doneCalled = false
        vm.save {
            doneCalled = true
        }

        XCTAssertFalse(doneCalled)
        XCTAssertEqual(vm.errorMessage, "结束时间必须晚于开始时间")
    }

    func testSaveRecurringEvent() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            defaultStartMinute: 480, // 08:00
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let funCat = vm.categories.first { $0.iconKey == "fun" }!
        vm.onCategorySelect(id: funCat.id)
        vm.onTitleChange("晨间瑜伽")
        vm.onStartTimeChange(SimpleTime(hour: 8, minute: 0))
        vm.onEndTimeChange(SimpleTime(hour: 8, minute: 45))
        vm.onRecurringToggle(true)

        var doneCalled = false
        vm.save {
            doneCalled = true
        }

        XCTAssertTrue(doneCalled)

        // Voyage has 4 days (sampleDate + 3 days). Day 1, 2, 3 should have the event (disembark day excluded).
        let day1Events = eventRepo.getEvents(for: sampleDate)
        let day2Events = eventRepo.getEvents(for: sampleDate.plusDays(1))
        let day3Events = eventRepo.getEvents(for: sampleDate.plusDays(2))

        XCTAssertTrue(day1Events.contains { $0.title == "晨间瑜伽" })
        XCTAssertTrue(day2Events.contains { $0.title == "晨间瑜伽" })
        XCTAssertTrue(day3Events.contains { $0.title == "晨间瑜伽" })
    }

    // MARK: - Delete Event Tests

    func testDeleteExistingEvent() {
        let categories = categoryRepo.getCategories()
        let cat = categories[0]
        let event = CruiseEvent(
            date: sampleDate,
            startTime: SimpleTime(hour: 16, minute: 0),
            durationMinutes: 30,
            categoryId: cat.id,
            title: "临时会议"
        )
        let saved = database.saveEvent(event)

        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: saved.id,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        var doneCalled = false
        vm.delete {
            doneCalled = true
        }

        XCTAssertTrue(doneCalled)
        XCTAssertNil(eventRepo.getEvent(byId: saved.id))
    }

    func testMealFallbackTitle() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            defaultStartMinute: 720, // 12:00
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let mealCat = vm.categories.first { $0.iconKey == "meal" }!
        vm.onCategorySelect(id: mealCat.id)
        vm.onMealPeriodSelect(.lunch)
        // Leave title empty
        vm.onTitleChange("")

        var doneCalled = false
        vm.save {
            doneCalled = true
        }

        XCTAssertTrue(doneCalled)
        let events = eventRepo.getEvents(for: sampleDate)
        let savedMeal = events.first { $0.categoryId == mealCat.id }
        XCTAssertNotNil(savedMeal)
        XCTAssertEqual(savedMeal?.title, "午餐")
    }

    func testSelectPresetLocation() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let entry = LocationCatalogEntry(
            deck: 6,
            zone: .bow,
            placeName: "华特迪士尼剧院 Walt Disney Theatre",
            diningGroup: nil,
            category: "show"
        )

        vm.onSelectPresetLocation(entry)

        XCTAssertEqual(vm.deck, 6)
        XCTAssertEqual(vm.zone, .bow)
        XCTAssertEqual(vm.placeName, "华特迪士尼剧院 Walt Disney Theatre")
        XCTAssertFalse(vm.noLocation)
        XCTAssertTrue(vm.locationPresetMode)

        vm.onCustomLocationSelected()
        XCTAssertFalse(vm.locationPresetMode)
    }

    func testCustomShowSelection() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let showCat = vm.categories.first { $0.iconKey == "show" }!
        vm.onCategorySelect(id: showCat.id)
        XCTAssertTrue(vm.showPresetMode)

        vm.onCustomShowSelected()
        XCTAssertFalse(vm.showPresetMode)
    }

    func testOperatingHoursCheckAndOverride() {
        let vm = EventSheetViewModel(
            date: sampleDate,
            defaultStartMinute: 300, // 05:00 AM (closed)
            editingEventId: 0,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        let mealCat = vm.categories.first { $0.iconKey == "meal" }!
        vm.onCategorySelect(id: mealCat.id)

        // Find a venue with configured hours in LocationCatalog (e.g. 帕洛)
        if let palo = LocationCatalog.venuesByCategory()["meal"]?.first(where: { $0.hours?.isConfigured == true }) {
            vm.onSelectPresetLocation(palo)
            vm.onStartTimeChange(SimpleTime(hour: 5, minute: 0))
            vm.onEndTimeChange(SimpleTime(hour: 5, minute: 30))

            var doneCalled = false
            vm.save {
                doneCalled = true
            }

            // Should be stopped by hours check
            XCTAssertFalse(doneCalled)
            XCTAssertTrue(vm.hoursCheckPending)
            XCTAssertNotNil(vm.hoursCheckMessage)

            // User overrides
            vm.onHoursCheckOverride()
            XCTAssertFalse(vm.hoursCheckPending)
            XCTAssertTrue(vm.hoursCheckOverride)

            vm.save {
                doneCalled = true
            }
            XCTAssertTrue(doneCalled)
        }
    }

    func testUpdateEventWholeGroupScope() {
        let categories = categoryRepo.getCategories()
        let cat = categories[0]
        let initialEvent = CruiseEvent(
            date: sampleDate,
            startTime: SimpleTime(hour: 10, minute: 0),
            durationMinutes: 30,
            categoryId: cat.id,
            title: "早间讲座"
        )

        let createdEvents = eventRepo.addEvent(event: initialEvent, recurring: true)
        XCTAssertGreaterThan(createdEvents.count, 1)
        let firstId = createdEvents[0].id

        let vm = EventSheetViewModel(
            date: sampleDate,
            editingEventId: firstId,
            eventRepository: eventRepo,
            categoryRepository: categoryRepo,
            locationPresetRepository: locationPresetRepo,
            voyageRepository: voyageRepo,
            settingsStore: settingsStore
        )

        vm.onTitleChange("早间健康讲座")

        var doneCalled = false
        vm.save(scope: .wholeGroup) {
            doneCalled = true
        }
        XCTAssertTrue(doneCalled)

        let day2Events = eventRepo.getEvents(for: sampleDate.plusDays(1))
        XCTAssertTrue(day2Events.contains { $0.title == "早间健康讲座" })
    }
}
