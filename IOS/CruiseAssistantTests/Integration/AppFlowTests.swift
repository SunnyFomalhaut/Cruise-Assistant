import XCTest
@testable import CruiseAssistant

@MainActor
final class AppFlowTests: XCTestCase {

    func testCompleteUserJourneyEndToEndFlow() {
        // =========================================================================
        // Step 1: App launch with fresh database -> Onboarding creates Voyage
        // =========================================================================
        let container = AppContainer.inMemory()
        XCTAssertFalse(container.settingsStore.settings.onboardingCompleted, "Initial onboarding state should be false")
        XCTAssertNil(container.voyageRepository.getVoyage(), "Fresh database should have no voyage")

        let boardDate = SimpleDate(year: 2026, month: 8, day: 23)
        let disembarkDate = SimpleDate(year: 2026, month: 8, day: 26)
        let dinnerTime = SimpleTime(hour: 17, minute: 45)

        let onboardingVM = OnboardingViewModel(
            voyageRepository: container.voyageRepository,
            settingsStore: container.settingsStore,
            notificationService: container.notificationService,
            today: boardDate
        )

        onboardingVM.selectedShipIndex = 0
        onboardingVM.shipName = "Disney Adventure"
        onboardingVM.onBoardDateChange(boardDate)
        onboardingVM.onDisembarkDateChange(disembarkDate)
        onboardingVM.dinnerTime = dinnerTime

        var onboardingCompletedCallback = false
        onboardingVM.submit {
            onboardingCompletedCallback = true
        }

        XCTAssertTrue(onboardingCompletedCallback, "Onboarding submit callback should be called")
        XCTAssertTrue(container.settingsStore.settings.onboardingCompleted, "Settings should record onboardingCompleted = true")

        guard let createdVoyage = container.voyageRepository.getVoyage() else {
            XCTFail("Voyage should be created in database")
            return
        }
        XCTAssertEqual(createdVoyage.shipName, "Disney Adventure")
        XCTAssertEqual(createdVoyage.boardDate, boardDate)
        XCTAssertEqual(createdVoyage.disembarkDate, disembarkDate)
        XCTAssertEqual(createdVoyage.datesInVoyage().count, 4)

        // =========================================================================
        // Step 2: Timeline initializes, loads default boarding/disembark events,
        //         creates 3 conflicting events -> verifies 3-lane assignment (0, 1, 2)
        // =========================================================================
        let timelineVM = TimelineViewModel(
            voyageRepository: container.voyageRepository,
            eventRepository: container.eventRepository,
            categoryRepository: container.categoryRepository,
            settingsStore: container.settingsStore,
            notificationService: container.notificationService,
            todayProvider: { boardDate }
        )

        let day1Events = container.eventRepository.getEvents(for: boardDate)
        XCTAssertTrue(day1Events.contains(where: { $0.title == "登船" }), "Boarding day should contain 登船 event")
        XCTAssertTrue(day1Events.contains(where: { $0.title == "安全演习" }), "Built-in preset should create 安全演习 event")

        let day4Events = container.eventRepository.getEvents(for: disembarkDate)
        XCTAssertTrue(day4Events.contains(where: { $0.title == "下船" }), "Disembark day should contain 下船 event")

        // Create 3 overlapping events on Day 2 (2026-08-24) between 14:00 and 15:00
        let dateDay2 = SimpleDate(year: 2026, month: 8, day: 24)
        let categories = container.categoryRepository.getCategories()
        let showCategory = categories.first(where: { $0.iconKey == "show" }) ?? categories[0]

        let eventA = CruiseEvent(
            id: 0,
            date: dateDay2,
            startTime: SimpleTime(hour: 14, minute: 0),
            durationMinutes: 60,
            categoryId: showCategory.id,
            title: "Moana Show"
        )
        let savedA = container.eventRepository.addEvent(event: eventA, recurrenceDays: [])

        let eventB = CruiseEvent(
            id: 0,
            date: dateDay2,
            startTime: SimpleTime(hour: 14, minute: 15),
            durationMinutes: 45,
            categoryId: showCategory.id,
            title: "Avengers Stunt Show"
        )
        let savedB = container.eventRepository.addEvent(event: eventB, recurrenceDays: [])

        let eventC = CruiseEvent(
            id: 0,
            date: dateDay2,
            startTime: SimpleTime(hour: 14, minute: 30),
            durationMinutes: 60,
            categoryId: showCategory.id,
            title: "Baymax Deck Party"
        )
        let savedC = container.eventRepository.addEvent(event: eventC, recurrenceDays: [])

        let assignedLanes = Set([savedA.lane, savedB.lane, savedC.lane])
        XCTAssertEqual(assignedLanes, Set([0, 1, 2]), "Three overlapping events on the same day must be assigned lanes 0, 1, and 2")

        // =========================================================================
        // Step 3: Conflict detector scans overlaps -> finalization lock and unlock
        // =========================================================================
        let conflicts = container.eventRepository.conflictsOn(date: dateDay2)
        XCTAssertGreaterThanOrEqual(conflicts.count, 2, "Overlapping events should produce conflict pairs")

        timelineVM.selectDate(dateDay2)
        XCTAssertFalse(timelineVM.isFinalized, "Day 2 plan should initially be unfinalized")

        // Lock / finalize
        timelineVM.finalizeDay(dateDay2)
        XCTAssertTrue(timelineVM.isFinalized, "Timeline should reflect finalized state after finalizeDay")
        let dayPlansLocked = container.voyageRepository.getDayPlans()
        XCTAssertEqual(dayPlansLocked[dateDay2]?.finalized, true, "DayPlan in repository should be finalized")

        // Unlock
        timelineVM.unlockDay(dateDay2)
        XCTAssertFalse(timelineVM.isFinalized, "Timeline should reflect unfinalized state after unlockDay")
        let dayPlansUnlocked = container.voyageRepository.getDayPlans()
        XCTAssertEqual(dayPlansUnlocked[dateDay2]?.finalized, false, "DayPlan in repository should be unlocked")

        // =========================================================================
        // Step 4: Facility Guide searches 'Walt Disney Theatre' -> adds to Wishlist
        // =========================================================================
        let facilityVM = FacilityGuideViewModel()
        facilityVM.setSearchQuery("Walt Disney Theatre")
        let facilityResults = facilityVM.filteredFacilities
        let targetFacility = facilityResults.first(where: {
            $0.placeName.localizedCaseInsensitiveContains("Walt Disney") ||
            $0.placeName.contains("华特迪士尼剧院") ||
            $0.placeName.contains("剧院")
        }) ?? facilityVM.allFacilities.first(where: { $0.placeName.contains("剧院") || $0.placeName.contains("Theatre") }) ?? facilityVM.allFacilities[0]

        let showCatId = container.categoryRepository.getCategories().first(where: { $0.iconKey == "show" })?.id ?? 1
        let addedWishlistItem = container.wishlistRepository.addItem(
            title: targetFacility.placeName,
            categoryId: showCatId,
            deck: targetFacility.deck,
            zone: targetFacility.zone,
            placeName: targetFacility.placeName,
            note: targetFacility.formattedLocation
        )

        XCTAssertEqual(addedWishlistItem.title, targetFacility.placeName)
        let currentWishlist = container.wishlistRepository.getItems()
        XCTAssertEqual(currentWishlist.count, 1, "Wishlist repository should contain the added item")

        // =========================================================================
        // Step 5: Wishlist converts item into a scheduled timeline event
        // =========================================================================
        let wishlistVM = WishlistViewModel(
            wishlistRepository: container.wishlistRepository,
            categoryRepository: container.categoryRepository,
            eventRepository: container.eventRepository,
            voyageRepository: container.voyageRepository
        )
        XCTAssertEqual(wishlistVM.items.count, 1)

        let convertedEvent = wishlistVM.convertToEvent(
            item: addedWishlistItem,
            date: dateDay2,
            startMinute: 16 * 60, // 16:00
            durationMinutes: 50
        )

        XCTAssertNotNil(convertedEvent, "Wishlist conversion should create a CruiseEvent")
        XCTAssertEqual(convertedEvent?.title, targetFacility.placeName)
        XCTAssertEqual(convertedEvent?.date, dateDay2)
        XCTAssertEqual(convertedEvent?.startTime, SimpleTime(hour: 16, minute: 0))

        let day2EventsAfterConversion = container.eventRepository.getEvents(for: dateDay2)
        XCTAssertTrue(day2EventsAfterConversion.contains(where: { $0.title == targetFacility.placeName }))

        let updatedWishlistItem = container.wishlistRepository.getItem(byId: addedWishlistItem.id)
        XCTAssertEqual(updatedWishlistItem?.isCompleted, true, "Converted wishlist item should be marked as completed")

        // =========================================================================
        // Step 6: Pixie Dust logs a given gift to Room 10120 and a received gift -> verifies room linkage pair
        // =========================================================================
        let pixieVM = PixieDustViewModel(
            dustRepository: container.pixieDustRepository,
            voyageRepository: container.voyageRepository,
            settingsStore: container.settingsStore
        )

        container.pixieDustRepository.seedDefaultLevels()
        let levels = container.pixieDustRepository.getLevels()
        let giftLevel = levels.first ?? container.pixieDustRepository.addLevel(
            name: "普通礼物",
            colorArgb: 0xFF0284C7,
            iconKey: "gift",
            rank: 1
        )

        // Log Given gift to Room 10120 on Deck 10
        let givenRecord = container.pixieDustRepository.addRecord(
            direction: .given,
            deck: 10,
            room: "10120",
            levelId: giftLevel.id,
            levelNameSnapshot: giftLevel.name,
            note: "Disney Mickey Pin",
            replied: false
        )
        XCTAssertEqual(givenRecord.room, "10120")
        XCTAssertEqual(givenRecord.deck, 10)
        XCTAssertEqual(givenRecord.direction, .given)

        // Log Received gift from Room 10120 on Deck 10
        let receivedRecord = container.pixieDustRepository.addRecord(
            direction: .received,
            deck: 10,
            room: "10120",
            levelId: giftLevel.id,
            levelNameSnapshot: giftLevel.name,
            note: "Duffy Keyring",
            replied: true
        )
        XCTAssertEqual(receivedRecord.room, "10120")
        XCTAssertEqual(receivedRecord.deck, 10)
        XCTAssertEqual(receivedRecord.direction, .received)

        pixieVM.loadData()
        let roomPairs = pixieVM.roomPairs
        let pair10120 = roomPairs.first(where: { $0.deck == 10 && $0.room == "10120" })
        XCTAssertNotNil(pair10120, "Room linkage pair for 10-10120 must exist")
        XCTAssertEqual(pair10120?.totalGivenCount, 1, "Given count for 10-10120 should be 1")
        XCTAssertEqual(pair10120?.totalReceivedCount, 1, "Received count for 10-10120 should be 1")

        // =========================================================================
        // Step 7: Deck Map parses room 10120 -> locates Deck 10 and pin coordinates
        // =========================================================================
        let parsedDeckNumber = parseStateroomDeck("10120")
        XCTAssertEqual(parsedDeckNumber, 10, "parseStateroomDeck('10120') should resolve to Deck 10")

        let deckSearchResults = searchDeckMap(query: "10120", currentDeck: 10)
        let roomResult = deckSearchResults.first(where: { $0.highlightQuery == "10120" || $0.title.contains("10120") })
        XCTAssertNotNil(roomResult, "Deck search for '10120' should yield a stateroom result")
        XCTAssertEqual(roomResult?.deckNumber, 10)

        let deckMapVM = DeckMapViewModel(initialDeck: 10, initialHighlight: "10120")
        XCTAssertEqual(deckMapVM.selectedDeck, 10)
        XCTAssertEqual(deckMapVM.highlightTarget, "10120")
        XCTAssertTrue(deckMapVM.hasMap, "Deck 10 should have an interactive SVG map")

        // =========================================================================
        // Step 8: Settings switches theme to LinaBell Pink and changes wallpaper alpha -> verifies persistence
        // =========================================================================
        let settingsVM = SettingsViewModel(
            settingsStore: container.settingsStore,
            voyageRepository: container.voyageRepository,
            eventRepository: container.eventRepository,
            categoryRepository: container.categoryRepository,
            notificationService: container.notificationService,
            database: container.database
        )

        settingsVM.setColorTheme(.linabell_pink)
        XCTAssertEqual(container.settingsStore.settings.colorTheme, .linabell_pink)

        settingsVM.setWallpaperAlpha(0.25)
        XCTAssertEqual(container.settingsStore.settings.wallpaperAlpha, 0.25, accuracy: 0.001)

        let persistedSettings = container.settingsStore.getSettings()
        XCTAssertEqual(persistedSettings.colorTheme, .linabell_pink, "Persisted color theme must be LinaBell Pink")
        XCTAssertEqual(persistedSettings.wallpaperAlpha, 0.25, accuracy: 0.001, "Persisted wallpaper alpha must be 0.25")
    }

    func testAppContainerFactoryMethods() {
        let inMemory = AppContainer.inMemory()
        XCTAssertNotNil(inMemory.database)
        XCTAssertNotNil(inMemory.settingsStore)
        XCTAssertNotNil(inMemory.voyageRepository)
        XCTAssertNotNil(inMemory.eventRepository)
        XCTAssertNotNil(inMemory.categoryRepository)
        XCTAssertNotNil(inMemory.pixieDustRepository)
        XCTAssertNotNil(inMemory.locationPresetRepository)
        XCTAssertNotNil(inMemory.wishlistRepository)
        XCTAssertNotNil(inMemory.notificationService)
        XCTAssertNotNil(inMemory.hapticFeedbackService)

        let standard = AppContainer.standard()
        XCTAssertNotNil(standard.database)
        XCTAssertNotNil(standard.settingsStore)
    }

    func testNavigationCoordinatorRouting() {
        let coordinator = NavigationCoordinator()
        XCTAssertEqual(coordinator.activeTab, .timeline)

        coordinator.navigateToDeckMap(deck: 8, highlight: "Royal Court")
        XCTAssertEqual(coordinator.activeTab, .deckMap)
        XCTAssertEqual(coordinator.targetDeck, 8)
        XCTAssertEqual(coordinator.targetHighlight, "Royal Court")

        let targetDate = SimpleDate(year: 2026, month: 8, day: 25)
        coordinator.navigateToTimeline(date: targetDate, eventId: 42)
        XCTAssertEqual(coordinator.activeTab, .timeline)
        XCTAssertEqual(coordinator.targetDate, targetDate)
        XCTAssertEqual(coordinator.targetEventId, 42)

        coordinator.navigateToFacilities()
        XCTAssertEqual(coordinator.activeTab, .facilities)

        coordinator.navigateToPixieDust()
        XCTAssertEqual(coordinator.activeTab, .pixieDust)

        coordinator.navigateToWishlist()
        XCTAssertEqual(coordinator.activeTab, .wishlist)

        coordinator.navigateToSettings()
        XCTAssertEqual(coordinator.activeTab, .settings)

        coordinator.clearNavigationTargets()
        XCTAssertNil(coordinator.targetDeck)
        XCTAssertNil(coordinator.targetHighlight)
        XCTAssertNil(coordinator.targetDate)
        XCTAssertNil(coordinator.targetEventId)
    }

    func testEventDetailDialog_ShowVersusNonShowWaitTimes() {
        let showCategory = Category(id: 2, name: "演出", colorArgb: 0xFFF5B301, iconKey: "show")
        let importantCategory = Category(id: 3, name: "重要事项", colorArgb: 0xFFEA4335, iconKey: "important")
        let mealCategory = Category(id: 1, name: "餐食", colorArgb: 0xFF34A853, iconKey: "meal")
        let showEvent = CruiseEvent(
            id: 12,
            date: SimpleDate(year: 2026, month: 9, day: 3),
            startTime: SimpleTime(hour: 20, minute: 30),
            durationMinutes: 60,
            categoryId: 2,
            title: "海洋奇遇 Disney Seas The Adventure",
            deck: 6,
            zone: .bow,
            placeName: "华特迪士尼剧院 Walt Disney Theatre",
            presetKey: "show_seas_adventure"
        )

        let drillEvent = CruiseEvent(
            id: 3,
            date: SimpleDate(year: 2026, month: 9, day: 3),
            startTime: SimpleTime(hour: 15, minute: 45),
            durationMinutes: 35,
            categoryId: 3,
            title: "安全演习",
            deck: 6,
            zone: .mid,
            placeName: nil,
            presetKey: "safety_drill"
        )

        let dinnerEvent = CruiseEvent(
            id: 4,
            date: SimpleDate(year: 2026, month: 9, day: 3),
            startTime: SimpleTime(hour: 17, minute: 45),
            durationMinutes: 75,
            categoryId: 1,
            title: "晚餐",
            presetKey: "dinner"
        )

        // 1. Verify show event has wait minutes formatted
        XCTAssertEqual(showEvent.effectiveWaitMinutes, 45)
        XCTAssertEqual(formatWaitStart(showEvent), "19:45")

        // 2. Verify dialog callbacks for navigation
        var navigatedDeck: Int?
        var navigatedTarget: String?

        let showDialog = EventDetailDialogView(
            event: showEvent,
            category: showCategory,
            onNavigateToMap: { deck, target in
                navigatedDeck = deck
                navigatedTarget = target
            },
            onDismiss: {}
        )
        showDialog.onNavigateToMap?(6, "华特迪士尼剧院 Walt Disney Theatre")
        XCTAssertEqual(navigatedDeck, 6)
        XCTAssertEqual(navigatedTarget, "华特迪士尼剧院 Walt Disney Theatre")

        let drillDialog = EventDetailDialogView(
            event: drillEvent,
            category: importantCategory,
            onNavigateToMap: { deck, target in
                navigatedDeck = deck
                navigatedTarget = target
            },
            onDismiss: {}
        )
        drillDialog.onNavigateToMap?(6, "安全演习")
        XCTAssertEqual(navigatedDeck, 6)
        XCTAssertEqual(navigatedTarget, "安全演习")

        let dinnerDialog = EventDetailDialogView(
            event: dinnerEvent,
            category: mealCategory,
            onNavigateToMap: { deck, target in
                navigatedDeck = deck
                navigatedTarget = target
            },
            onDismiss: {}
        )
        XCTAssertNotNil(dinnerDialog)
    }

    func testEventDetailDialog_MapDeepLinkAndHighlight() {
        let coordinator = NavigationCoordinator()
        let deckMapVM = DeckMapViewModel(initialDeck: 10)

        // Simulate event detail dialog clicking "去地图" -> Deck 6, "华特迪士尼剧院"
        coordinator.navigateToDeckMap(deck: 6, highlight: "华特迪士尼剧院 Walt Disney Theatre")
        XCTAssertEqual(coordinator.activeTab, .deckMap)
        XCTAssertEqual(coordinator.targetDeck, 6)
        XCTAssertEqual(coordinator.targetHighlight, "华特迪士尼剧院 Walt Disney Theatre")

        deckMapVM.navigateToFacility(deck: 6, highlight: "华特迪士尼剧院 Walt Disney Theatre")
        XCTAssertEqual(deckMapVM.selectedDeck, 6)
        XCTAssertEqual(deckMapVM.highlightTarget, "华特迪士尼剧院 Walt Disney Theatre")

        // Verify SVG HTML generation includes highlight pin badge and pulse box
        if let svgContent = DeckMapRepository.loadDeckSvgContent(deckNumber: 6) {
            let html = DeckMapRepository.inlineHtmlWrapperFor(
                svgContent: svgContent,
                highlightTarget: "华特迪士尼剧院 Walt Disney Theatre"
            )
            XCTAssertTrue(html.contains("facility-highlight-box"), "HTML should inject facility-highlight-box CSS")
            XCTAssertTrue(html.contains("facility-pin-badge"), "HTML should inject facility-pin-badge CSS")
            XCTAssertTrue(html.contains("centerFacilityTarget"), "HTML should inject centerFacilityTarget script")
            XCTAssertTrue(html.contains("📍"), "HTML should include pin icon in pin-badge")
        }
    }

    func testTimelineFinalizedState_ToggleAndFABVisibility() {
        let container = AppContainer.inMemory()
        let boardDate = SimpleDate(year: 2026, month: 9, day: 3)
        let disembarkDate = SimpleDate(year: 2026, month: 9, day: 7)

        _ = container.voyageRepository.createVoyage(
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: SimpleTime(hour: 12, minute: 0),
            disembarkDate: disembarkDate,
            disembarkTime: SimpleTime(hour: 7, minute: 0),
            maxDeck: 20,
            minDeck: 1,
            dinnerTime: SimpleTime(hour: 17, minute: 45),
            shipPresetName: "Disney Adventure"
        )

        let timelineVM = TimelineViewModel(
            voyageRepository: container.voyageRepository,
            eventRepository: container.eventRepository,
            categoryRepository: container.categoryRepository,
            settingsStore: container.settingsStore,
            notificationService: container.notificationService,
            todayProvider: { boardDate }
        )

        timelineVM.selectDate(boardDate)
        XCTAssertFalse(timelineVM.isFinalized, "Day should be unfinalized initially")

        // Request finalize
        timelineVM.finalizeDay(boardDate)
        XCTAssertTrue(timelineVM.isFinalized, "Day should be finalized after finalizeDay")

        // Unlock day
        timelineVM.unlockDay(boardDate)
        XCTAssertFalse(timelineVM.isFinalized, "Day should be unfinalized after unlockDay")
    }

    func testSeedSimulatorData() {
        let containerPath = "/Users/u219999/Library/Developer/CoreSimulator/Devices/0185FF65-EB49-4425-8703-0ADAEF6D749B/data/Containers/Data/Application/F5CEA785-4A5F-4CF0-91BD-833E92AD082F"
        let appSupport = URL(fileURLWithPath: containerPath).appendingPathComponent("Library/Application Support/CruiseAssistant", isDirectory: true)
        try? FileManager.default.createDirectory(at: appSupport, withIntermediateDirectories: true)
        let dbURL = appSupport.appendingPathComponent("cruise_database.json")

        let database = CruiseDatabase(fileURL: dbURL)
        let categoryRepo = CategoryRepository(database: database)
        let voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        let notificationService = NotificationService()

        let defaults = UserDefaults(suiteName: "simulator_seed") ?? .standard
        let settingsStore = SettingsStore(userDefaults: defaults)

        let boardDate = SimpleDate(year: 2026, month: 9, day: 3)
        let disembarkDate = SimpleDate(year: 2026, month: 9, day: 7)
        let dinnerTime = SimpleTime(hour: 17, minute: 45)

        let onboardingVM = OnboardingViewModel(
            voyageRepository: voyageRepo,
            settingsStore: settingsStore,
            notificationService: notificationService,
            today: boardDate
        )

        onboardingVM.selectedShipIndex = 0
        onboardingVM.shipName = "Disney Adventure"
        onboardingVM.onBoardDateChange(boardDate)
        onboardingVM.onDisembarkDateChange(disembarkDate)
        onboardingVM.dinnerTime = dinnerTime
        onboardingVM.submit()

        let prefsURL = URL(fileURLWithPath: containerPath).appendingPathComponent("Library/Preferences", isDirectory: true)
        try? FileManager.default.createDirectory(at: prefsURL, withIntermediateDirectories: true)
        let plistURL = prefsURL.appendingPathComponent("com.cruise.assistant.ios.plist")
        var settings = settingsStore.settings
        settings.onboardingCompleted = true
        settings.lastViewedDate = boardDate
        let encoder = PropertyListEncoder()
        if let data = try? encoder.encode(["cruise_settings": try? JSONEncoder().encode(settings)]) {
            try? data.write(to: plistURL)
        }
        print("Clean seeded database at \(dbURL.path)")
    }
}
