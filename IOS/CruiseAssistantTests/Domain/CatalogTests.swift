import XCTest
@testable import CruiseAssistant

final class CatalogTests: XCTestCase {

    // MARK: - StateroomCatalog Tests

    func testStateroomCatalogLoadDefault() {
        let loaded = StateroomCatalog.loadDefault()
        XCTAssertTrue(loaded, "StateroomCatalog should load default staterooms.json")

        let staterooms = StateroomCatalog.getAllStaterooms()
        XCTAssertGreaterThanOrEqual(staterooms.count, 1700, "Should have loaded at least 1700 staterooms")

        // Verify sorted numerically
        for i in 0..<(staterooms.count - 1) {
            let current = Int(staterooms[i].roomNumber) ?? 0
            let next = Int(staterooms[i + 1].roomNumber) ?? 0
            XCTAssertLessThanOrEqual(current, next, "Staterooms should be sorted numerically")
        }
    }

    func testStateroomSearchPrefixAndSubstring() {
        let sample = [
            StateroomEntry(roomNumber: "10100", deck: 10),
            StateroomEntry(roomNumber: "10101", deck: 10),
            StateroomEntry(roomNumber: "11200", deck: 11),
            StateroomEntry(roomNumber: "11216", deck: 11),
            StateroomEntry(roomNumber: "12100", deck: 12),
        ]
        let catalog = StateroomCatalog(staterooms: sample)

        // Prefix match
        let prefixResults = catalog.search(query: "10")
        XCTAssertEqual(prefixResults.count, 2)
        XCTAssertEqual(prefixResults[0].roomNumber, "10100")
        XCTAssertEqual(prefixResults[1].roomNumber, "10101")

        // Substring match fallback (query "216" does not match any prefix, but matches substring of 11216)
        let substringResults = catalog.search(query: "216")
        XCTAssertEqual(substringResults.count, 1)
        XCTAssertEqual(substringResults[0].roomNumber, "11216")

        // Invalid query
        XCTAssertTrue(catalog.search(query: "").isEmpty)
        XCTAssertTrue(catalog.search(query: "abc").isEmpty)
        XCTAssertTrue(catalog.search(query: " 10a ").isEmpty)
    }

    func testStateroomJsonParsing() {
        let json = """
        {
            "staterooms": [
                {"room": "6012", "deck": 6},
                {"room": "9100", "deck": 9}
            ]
        }
        """
        let parsed = StateroomCatalog.parseStaterooms(json: json)
        XCTAssertEqual(parsed.count, 2)
        XCTAssertEqual(parsed[0].roomNumber, "6012")
        XCTAssertEqual(parsed[0].deck, 6)
        XCTAssertEqual(parsed[1].roomNumber, "9100")
        XCTAssertEqual(parsed[1].deck, 9)

        // Direct list JSON
        let listJson = """
        [
            {"room": "5001", "deck": 5}
        ]
        """
        let parsedList = StateroomCatalog.parseStaterooms(json: listJson)
        XCTAssertEqual(parsedList.count, 1)
        XCTAssertEqual(parsedList[0].roomNumber, "5001")
    }

    // MARK: - LocationCatalog Tests

    func testLocationCatalogLoadDefault() {
        let loaded = LocationCatalog.loadDefault()
        XCTAssertTrue(loaded, "LocationCatalog should load default locations.json")

        let entries = LocationCatalog.getAllEntries()
        XCTAssertGreaterThanOrEqual(entries.count, 50, "Should have loaded at least 50 venues")
    }

    func testLocationCatalogVenuesByCategory() {
        LocationCatalog.loadDefault()
        let byCategory = LocationCatalog.venuesByCategory()

        XCTAssertTrue(byCategory.keys.contains("meal"), "Should contain meal category")
        XCTAssertTrue(byCategory.keys.contains("show"), "Should contain show category")
        XCTAssertTrue(byCategory.keys.contains("chore"), "Should contain chore category")
        XCTAssertTrue(byCategory.keys.contains("pool"), "Should contain pool category")
        XCTAssertTrue(byCategory.keys.contains("shop"), "Should contain shop category")
        XCTAssertTrue(byCategory.keys.contains("youth"), "Should contain youth category")

        let meals = byCategory["meal"] ?? []
        XCTAssertFalse(meals.isEmpty)
    }

    func testLocationPresetOptions() {
        let testVenues = [
            LocationCatalogEntry(
                deck: 9, zone: .stern, placeName: "动画师餐厅 Animator's Table",
                diningGroup: .rotation, category: "meal",
                hours: OperatingHours(slots: [
                    OperatingSlot(start: SimpleTime(hour: 17, minute: 45), end: SimpleTime(hour: 22, minute: 0), meal: .dinner)
                ])
            ),
            LocationCatalogEntry(
                deck: 10, zone: .bow, placeName: "莫格利餐厅 Mowgli's Eatery",
                diningGroup: .quickService, category: "meal",
                hours: OperatingHours(slots: [
                    OperatingSlot(start: SimpleTime(hour: 11, minute: 0), end: SimpleTime(hour: 18, minute: 0), meal: .lunch)
                ])
            ),
            LocationCatalogEntry(
                deck: 6, zone: .bow, placeName: "华特迪士尼剧院 Walt Disney Theatre",
                category: "show"
            )
        ]

        let catalog = LocationCatalog(venues: testVenues)

        // Non-meal iconKey
        let showOptions = catalog.locationPresetOptions(iconKey: "show")
        XCTAssertEqual(showOptions.count, 2) // 1 Entry + 1 Custom
        if case .entry(let entry) = showOptions[0] {
            XCTAssertEqual(entry.placeName, "华特迪士尼剧院 Walt Disney Theatre")
        } else {
            XCTFail("Expected entry option")
        }
        XCTAssertEqual(showOptions[1], .custom)

        // Meal with dinner filter
        let dinnerOptions = catalog.locationPresetOptions(iconKey: "meal", mealPeriod: .dinner)
        // Should contain rotation group, rotation entry, and custom
        XCTAssertTrue(dinnerOptions.contains(.group(DiningGroup.rotation.label)))
        XCTAssertTrue(dinnerOptions.contains(.custom))
        // Quick service restaurant (only lunch) should NOT be in dinner options
        let hasMowgli = dinnerOptions.contains { opt in
            if case .entry(let e) = opt { return e.placeName.contains("Mowgli") }
            return false
        }
        XCTAssertFalse(hasMowgli)
    }

    func testLocationIsOpenAtNormalSlot() {
        let venue = LocationCatalogEntry(
            deck: 18, zone: .bow, placeName: "礼宾健身中心 Concierge Fitness Center",
            category: "chore",
            hours: OperatingHours(slots: [
                OperatingSlot(start: SimpleTime(hour: 6, minute: 0), end: SimpleTime(hour: 22, minute: 0))
            ])
        )

        XCTAssertTrue(venue.isOpenAt(time: SimpleTime(hour: 6, minute: 0)))
        XCTAssertTrue(venue.isOpenAt(time: SimpleTime(hour: 12, minute: 30)))
        XCTAssertTrue(venue.isOpenAt(time: SimpleTime(hour: 21, minute: 59)))

        XCTAssertFalse(venue.isOpenAt(time: SimpleTime(hour: 5, minute: 59)))
        XCTAssertFalse(venue.isOpenAt(time: SimpleTime(hour: 22, minute: 0)))
        XCTAssertFalse(venue.isOpenAt(time: SimpleTime(hour: 23, minute: 0)))
        XCTAssertFalse(venue.isOpenAt(time: SimpleTime(hour: 2, minute: 0)))
    }

    func testLocationIsOpenAtCrossMidnightSlot() {
        // Edge 9:00 to 01:00 (cross midnight)
        let edge = LocationCatalogEntry(
            deck: 7, zone: .stern, placeName: "Edge", category: "youth",
            hours: OperatingHours(slots: [
                OperatingSlot(start: SimpleTime(hour: 9, minute: 0), end: SimpleTime(hour: 1, minute: 0))
            ])
        )

        XCTAssertTrue(edge.isOpenAt(time: SimpleTime(hour: 9, minute: 0)))
        XCTAssertTrue(edge.isOpenAt(time: SimpleTime(hour: 23, minute: 0)))
        XCTAssertTrue(edge.isOpenAt(time: SimpleTime(hour: 0, minute: 30)))
        XCTAssertFalse(edge.isOpenAt(time: SimpleTime(hour: 1, minute: 0)))
        XCTAssertFalse(edge.isOpenAt(time: SimpleTime(hour: 2, minute: 0)))
        XCTAssertFalse(edge.isOpenAt(time: SimpleTime(hour: 8, minute: 59)))
    }

    func testLocationIsOpenForMealPeriod() {
        let venue = LocationCatalogEntry(
            deck: 9, zone: .stern, placeName: "动画师餐厅 Animator's Table",
            diningGroup: .rotation, category: "meal",
            hours: OperatingHours(slots: [
                OperatingSlot(start: SimpleTime(hour: 8, minute: 0), end: SimpleTime(hour: 10, minute: 0), meal: .breakfast),
                OperatingSlot(start: SimpleTime(hour: 12, minute: 0), end: SimpleTime(hour: 14, minute: 0), meal: .lunch),
                OperatingSlot(start: SimpleTime(hour: 18, minute: 0), end: SimpleTime(hour: 22, minute: 0), meal: .dinner, lateNight: true)
            ])
        )

        XCTAssertTrue(venue.isOpenFor(period: .breakfast))
        XCTAssertTrue(venue.isOpenFor(period: .lunch))
        XCTAssertTrue(venue.isOpenFor(period: .dinner))
        XCTAssertTrue(venue.isOpenFor(period: .lateNight))
        XCTAssertFalse(venue.isOpenFor(period: .snack))
        XCTAssertFalse(venue.isOpenFor(period: .drink))

        // Venue without configured hours is always open
        let stage = LocationCatalogEntry(deck: 6, zone: .bow, placeName: "华特迪士尼剧院 Walt Disney Theatre", category: "show")
        for period in MealPeriod.allCases {
            XCTAssertTrue(stage.isOpenFor(period: period))
        }
    }

    // MARK: - FacilityGuide Tests

    func testToFacilityItemUiModel24Hours() {
        let laundry = LocationCatalogEntry(
            deck: 16, zone: .mid, placeName: "童话洗衣房 Fairytale Fresh Laundry",
            category: "chore",
            hours: OperatingHours(slots: [
                OperatingSlot(start: SimpleTime(hour: 0, minute: 0), end: SimpleTime(hour: 23, minute: 59))
            ])
        )

        let uiModel = laundry.toFacilityItemUiModel(currentTime: SimpleTime(hour: 3, minute: 0))
        XCTAssertTrue(uiModel.is24Hours)
        XCTAssertTrue(uiModel.isOpenNow)
        XCTAssertTrue(uiModel.hasOperatingHours)
        XCTAssertEqual(uiModel.categoryLabel, "船上服务")
        XCTAssertEqual(uiModel.formattedLocation, "Deck 16 船中")
    }

    func testToFacilityItemUiModelMultiDeckAndNoDeck() {
        let theatre = LocationCatalogEntry(
            deck: 6, zone: .bow, placeName: "华特迪士尼剧院 Walt Disney Theatre",
            category: "show",
            additionalDecks: [7]
        )
        let uiModel = theatre.toFacilityItemUiModel()
        XCTAssertEqual(uiModel.allDecks, [6, 7])
        XCTAssertEqual(uiModel.primaryDeck, 6)
        XCTAssertEqual(uiModel.formattedLocation, "Deck 6, 7 船头")

        let roomService = LocationCatalogEntry(
            deck: nil, zone: nil, placeName: "客房送餐 Room Service",
            category: "meal"
        )
        let rsUiModel = roomService.toFacilityItemUiModel()
        XCTAssertEqual(rsUiModel.formattedLocation, "全船/Dial 0")
    }

    func testFilterFacilities() {
        let items = [
            FacilityItemUiModel(
                placeName: "动画师餐厅 Animator's Table",
                categoryKey: "meal",
                categoryLabel: "餐食",
                deck: 9,
                zone: .stern,
                formattedLocation: "Deck 9 船尾",
                isOpenNow: true,
                diningGroupLabel: "轮换正餐餐厅"
            ),
            FacilityItemUiModel(
                placeName: "童话洗衣房 Fairytale Fresh Laundry",
                categoryKey: "chore",
                categoryLabel: "船上服务",
                deck: 16,
                zone: .mid,
                formattedLocation: "Deck 16 船中",
                isOpenNow: false
            ),
            FacilityItemUiModel(
                placeName: "三个愿望 3 Wishes",
                categoryKey: "shop",
                categoryLabel: "商店",
                deck: 7,
                zone: .stern,
                formattedLocation: "Deck 7 船尾",
                isOpenNow: true
            )
        ]

        // Category filter
        let mealOnly = filterFacilities(facilities: items, query: "", categoryKey: "meal", onlyOpenNow: false)
        XCTAssertEqual(mealOnly.count, 1)
        XCTAssertEqual(mealOnly[0].placeName, "动画师餐厅 Animator's Table")

        // Only open now filter
        let openOnly = filterFacilities(facilities: items, query: "", categoryKey: "all", onlyOpenNow: true)
        XCTAssertEqual(openOnly.count, 2)

        // Deck filter
        let deck16 = filterFacilities(facilities: items, query: "", categoryKey: "all", onlyOpenNow: false, deck: 16)
        XCTAssertEqual(deck16.count, 1)
        XCTAssertEqual(deck16[0].placeName, "童话洗衣房 Fairytale Fresh Laundry")

        // Search query by name
        let search3Wishes = filterFacilities(facilities: items, query: "wishes", categoryKey: "all", onlyOpenNow: false)
        XCTAssertEqual(search3Wishes.count, 1)
        XCTAssertEqual(search3Wishes[0].placeName, "三个愿望 3 Wishes")

        // Search query by zone label
        let searchStern = filterFacilities(facilities: items, query: "船尾", categoryKey: "all", onlyOpenNow: false)
        XCTAssertEqual(searchStern.count, 2)
    }

    // MARK: - PosterManager Tests

    func testShowPosterMatching() {
        let lionKingPath = PosterManager.getEventPosterPath(eventTitle: "狮子王", presetKey: "show_lion_king")
        XCTAssertEqual(lionKingPath, "posters/shows/show_lion_king.jpg")

        let moanaByTitle = PosterManager.getEventPosterPath(eventTitle: "海洋奇缘 Call of the Sea")
        XCTAssertEqual(moanaByTitle, "posters/shows/show_moana.jpg")
    }

    func testFacilityPosterMatching() {
        XCTAssertEqual(PosterManager.findPosterPath(for: "华特迪士尼剧院"), "posters/facilities/walt_disney_theatre.jpg")
        XCTAssertEqual(PosterManager.findPosterPath(for: "Walt Disney Theatre"), "posters/facilities/walt_disney_theatre.jpg")
        XCTAssertEqual(PosterManager.findPosterPath(for: "流浪猫咖啡馆"), "posters/dining/alley_cat_cafe.jpg")
        XCTAssertEqual(PosterManager.findPosterPath(for: "三个愿望"), "posters/shops/3_wishes.jpg")
        XCTAssertEqual(PosterManager.findPosterPath(for: "活力少年俱乐部"), "posters/youth/vibe.jpg")
    }

    func testCategoryOf() {
        XCTAssertEqual(PosterManager.categoryOf(placeName: "华特迪士尼剧院"), "facilities")
        XCTAssertEqual(PosterManager.categoryOf(placeName: "流浪猫咖啡馆"), "dining")
        XCTAssertEqual(PosterManager.categoryOf(placeName: "三个愿望"), "shops")
        XCTAssertEqual(PosterManager.categoryOf(placeName: "活力少年俱乐部"), "youth")
        XCTAssertNil(PosterManager.categoryOf(placeName: "Unknown Nonexistent Place"))
    }

    func testCustomPosterRegistration() {
        PosterManager.registerFacilityPoster(placeName: "Custom Venue", assetPath: "posters/custom/venue.jpg")
        XCTAssertEqual(PosterManager.findPosterPath(for: "Custom Venue"), "posters/custom/venue.jpg")

        PosterManager.registerEventPoster(keyOrTitle: "Custom Event", assetPath: "posters/custom/event.jpg")
        XCTAssertEqual(PosterManager.getEventPosterPath(eventTitle: "Custom Event"), "posters/custom/event.jpg")
    }
}
