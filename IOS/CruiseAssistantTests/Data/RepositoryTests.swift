import XCTest
@testable import CruiseAssistant

final class RepositoryTests: XCTestCase {
    private var database: CruiseDatabase!
    private var categoryRepo: CategoryRepository!
    private var voyageRepo: VoyageRepository!
    private var eventRepo: EventRepository!
    private var pixieDustRepo: PixieDustRepository!
    private var wishlistRepo: WishlistRepository!
    private var locationPresetRepo: LocationPresetRepository!
    private var settingsStore: SettingsStore!

    override func setUp() {
        super.setUp()
        database = CruiseDatabase.inMemory()
        categoryRepo = CategoryRepository(database: database)
        voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)
        eventRepo = EventRepository(database: database)
        pixieDustRepo = PixieDustRepository(database: database)
        wishlistRepo = WishlistRepository(database: database)
        locationPresetRepo = LocationPresetRepository(database: database)
        settingsStore = SettingsStore.inMemory()
    }

    override func tearDown() {
        database = nil
        categoryRepo = nil
        voyageRepo = nil
        eventRepo = nil
        pixieDustRepo = nil
        wishlistRepo = nil
        locationPresetRepo = nil
        settingsStore = nil
        super.tearDown()
    }

    // MARK: - Voyage Repository Tests

    func testVoyageCreationAndDayPlans() {
        let boardDate = SimpleDate(year: 2026, month: 8, day: 23)
        let disembarkDate = SimpleDate(year: 2026, month: 8, day: 26) // 4 days (23, 24, 25, 26)
        let boardTime = SimpleTime(hour: 13, minute: 0)

        let voyage = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: boardTime,
            disembarkDate: disembarkDate,
            disembarkTime: SimpleTime(hour: 7, minute: 0),
            maxDeck: 20,
            minDeck: 1,
            dinnerTime: SimpleTime(hour: 17, minute: 45),
            shipPresetName: "Disney Adventure"
        )

        XCTAssertEqual(voyage.shipName, "Disney Adventure")
        XCTAssertEqual(voyage.dayCount, 4)

        // Verify day plans
        let dayPlans = voyageRepo.getDayPlans()
        XCTAssertEqual(dayPlans.count, 4)
        for date in voyage.datesInVoyage() {
            XCTAssertNotNil(dayPlans[date])
            XCTAssertFalse(dayPlans[date]!.finalized)
        }

        // Verify seeded categories
        let categories = categoryRepo.getCategories()
        XCTAssertEqual(categories.count, 9)
        XCTAssertTrue(categories.contains { $0.iconKey == "important" })
        XCTAssertTrue(categories.contains { $0.iconKey == "meal" })

        // Verify system events
        let allEvents = eventRepo.getAllEvents()
        let boarding = allEvents.first { $0.title == "登船" }
        XCTAssertNotNil(boarding)
        XCTAssertEqual(boarding?.date, boardDate)
        XCTAssertEqual(boarding?.startTime, boardTime)
        XCTAssertEqual(boarding?.durationMinutes, 15)
        XCTAssertTrue(boarding?.isSystemGenerated == true)

        let disembark = allEvents.first { $0.title == "下船" }
        XCTAssertNotNil(disembark)
        XCTAssertEqual(disembark?.date, disembarkDate)
        XCTAssertEqual(disembark?.startTime, SimpleTime(hour: 7, minute: 0))
        XCTAssertEqual(disembark?.durationMinutes, 30)
        XCTAssertTrue(disembark?.isSystemGenerated == true)

        // Verify safety drill preset
        let drill = allEvents.first { $0.presetKey == "safety_drill" }
        XCTAssertNotNil(drill)
        XCTAssertEqual(drill?.date, boardDate)
        XCTAssertEqual(drill?.startTime, SimpleTime(hour: 15, minute: 45))
        XCTAssertEqual(drill?.deck, 6)
        XCTAssertEqual(drill?.zone, .mid)
        XCTAssertEqual(drill?.lane, 1)

        // Verify dinners on non-disembark days (23, 24, 25)
        let dinners = allEvents.filter { $0.presetKey == "dinner" }
        XCTAssertEqual(dinners.count, 3)
        XCTAssertFalse(dinners.contains { $0.date == disembarkDate })
    }

    func testCreateVoyage_seedsThreeTimelineShows_andFiveWishlistShows() {
        let boardDate = SimpleDate(year: 2026, month: 9, day: 10)
        let boardTime = SimpleTime(hour: 13, minute: 0)

        _ = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: boardTime,
            disembarkDate: SimpleDate(year: 2026, month: 9, day: 14),
            maxDeck: 19,
            minDeck: 1,
            dinnerTime: SimpleTime(hour: 17, minute: 45),
            shipPresetName: "Disney Adventure"
        )

        let allEvents = database.getAllEvents()
        let showEvents = allEvents.filter { $0.presetKey?.hasPrefix("show_") == true }
        // 登船日 3 场扬帆起航 + 1 场海洋奇遇 + 第 2 晚 1 场狮子王烟花 = 5 场（3 部）
        XCTAssertEqual(5, showEvents.count)
        let keys = Set(showEvents.compactMap { $0.presetKey })
        XCTAssertEqual(Set(["show_set_sail", "show_seas_adventure", "show_lion_king"]), keys)

        guard let lionKing = showEvents.first(where: { $0.presetKey == "show_lion_king" }) else {
            XCTFail("Lion King show not found")
            return
        }
        XCTAssertEqual("阳光泳池 Sunnyside Pool", lionKing.placeName)
        XCTAssertEqual("烟花在右舷，可以在这个泳池旁边的空地或者18甲左舷走廊（人比较多）", lionKing.note)

        // 验证心愿单播种了 5 部浮动演出
        let wishlistItems = database.getWishlistItems()
        XCTAssertEqual(5, wishlistItems.count)
        let titles = Set(wishlistItems.map { $0.title })
        XCTAssertTrue(titles.contains("回忆：瓦力与伊芙 Remember"))
        XCTAssertTrue(titles.contains("海洋奇缘 Moana: Call of the Sea"))
        XCTAssertTrue(titles.contains("复仇者集结 Avengers Assemble!"))
        XCTAssertTrue(titles.contains("大白超级健身世博会 Baymax Super Exercise Expo"))
        XCTAssertTrue(titles.contains("达菲与友谊之船 Duffy and The Friend Ship"))

        // 验证幂等性：再次调用不重复生成心愿单项
        _ = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: boardTime,
            disembarkDate: SimpleDate(year: 2026, month: 9, day: 14),
            maxDeck: 19,
            minDeck: 1,
            dinnerTime: SimpleTime(hour: 17, minute: 45),
            shipPresetName: "Disney Adventure"
        )
        XCTAssertEqual(5, database.getWishlistItems().count)
    }

    func testVoyageUpdateAndDateTrimming() {
        let boardDate = SimpleDate(year: 2026, month: 8, day: 23)
        let disembarkDate = SimpleDate(year: 2026, month: 8, day: 26)

        var voyage = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: SimpleTime(hour: 13, minute: 0),
            disembarkDate: disembarkDate,
            shipPresetName: "Disney Adventure"
        )

        // Add a custom event on Aug 25
        let customEvent = CruiseEvent(
            id: 0,
            date: SimpleDate(year: 2026, month: 8, day: 25),
            startTime: SimpleTime(hour: 10, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "Pool Party"
        )
        let savedCustom = eventRepo.addEvent(event: customEvent, recurrenceDays: [])

        // Add a custom event on Aug 26
        let customEventAug26 = CruiseEvent(
            id: 0,
            date: SimpleDate(year: 2026, month: 8, day: 26),
            startTime: SimpleTime(hour: 14, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "Shopping"
        )
        _ = eventRepo.addEvent(event: customEventAug26, recurrenceDays: [])

        // Update voyage to shorten: Aug 23 - Aug 25 (removing Aug 26)
        voyage.disembarkDate = SimpleDate(year: 2026, month: 8, day: 25)
        voyage.boardTime = SimpleTime(hour: 14, minute: 0)
        voyageRepo.updateVoyage(voyage)

        // Verify DayPlans trimmed
        let dayPlans = voyageRepo.getDayPlans()
        XCTAssertEqual(dayPlans.count, 3)
        XCTAssertNil(dayPlans[SimpleDate(year: 2026, month: 8, day: 26)])

        // Verify boarding moved to 14:00
        let boarding = eventRepo.getAllEvents().first { $0.title == "登船" }
        XCTAssertEqual(boarding?.startTime, SimpleTime(hour: 14, minute: 0))

        // Verify disembark moved to Aug 25
        let disembark = eventRepo.getAllEvents().first { $0.title == "下船" }
        XCTAssertEqual(disembark?.date, SimpleDate(year: 2026, month: 8, day: 25))

        // Verify custom event on Aug 25 remains, but Aug 26 event is deleted
        let customOn25 = eventRepo.getEvent(byId: savedCustom.id)
        XCTAssertNotNil(customOn25)
        let customOn26 = eventRepo.getAllEvents().first { $0.title == "Shopping" }
        XCTAssertNil(customOn26)
    }

    func testVoyageReset() {
        let boardDate = SimpleDate(year: 2026, month: 8, day: 23)
        let disembarkDate = SimpleDate(year: 2026, month: 8, day: 25)

        _ = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: SimpleTime(hour: 13, minute: 0),
            disembarkDate: disembarkDate,
            shipPresetName: "Disney Adventure"
        )

        // Add pixie record and location preset
        _ = pixieDustRepo.addRecord(direction: .given, deck: 10, room: "001", levelId: 1, levelNameSnapshot: "普通", note: "Gift")
        locationPresetRepo.recordUsage(deck: 11, zone: .stern, placeName: "Cabanas")

        // Reset voyage
        voyageRepo.resetVoyage()

        XCTAssertNil(voyageRepo.getVoyage())
        XCTAssertTrue(voyageRepo.getDayPlans().isEmpty)
        XCTAssertTrue(eventRepo.getAllEvents().isEmpty)
        XCTAssertTrue(pixieDustRepo.getRecords().isEmpty)
        XCTAssertTrue(locationPresetRepo.getPresets().isEmpty)

        // Categories should be preserved
        XCTAssertEqual(categoryRepo.getCategories().count, 9)

        // Default pixie levels should be reseeded
        let levels = pixieDustRepo.getLevels()
        XCTAssertEqual(levels.count, 3)
    }

    func testDayPlanFinalization() {
        let date = SimpleDate(year: 2026, month: 8, day: 23)
        voyageRepo.setFinalized(date: date, finalized: true, nowMs: 123456789)

        let plan = voyageRepo.getDayPlans()[date]
        XCTAssertNotNil(plan)
        XCTAssertTrue(plan!.finalized)
        XCTAssertEqual(plan!.finalizedAt, 123456789)

        voyageRepo.setFinalized(date: date, finalized: false, nowMs: 999999999)
        let unfinalized = voyageRepo.getDayPlans()[date]
        XCTAssertFalse(unfinalized!.finalized)
        XCTAssertNil(unfinalized!.finalizedAt)
    }

    // MARK: - Voyage Archive Tests

    private func createArchiveTestVoyage() {
        _ = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: SimpleDate(year: 2026, month: 8, day: 23),
            boardTime: SimpleTime(hour: 13, minute: 0),
            disembarkDate: SimpleDate(year: 2026, month: 8, day: 26)
        )
    }

    func testArchiveCurrentVoyage_storesCustomName_andRestoresById() {
        createArchiveTestVoyage()
        _ = pixieDustRepo.addRecord(direction: .given, deck: 10, room: "001", levelId: 1, levelNameSnapshot: "普通", note: "Gift")
        let eventCount = eventRepo.getAllEvents().count

        let named = voyageRepo.archiveCurrentVoyage(customName: "八月首航")
        XCTAssertEqual(named?.customName, "八月首航")
        XCTAssertEqual(named?.name, "八月首航")
        XCTAssertEqual(named?.pixieCount, 1)

        let unnamed = voyageRepo.archiveCurrentVoyage(customName: nil)
        XCTAssertEqual(unnamed?.name, "Disney Adventure", "Unnamed archives fall back to the ship name")

        voyageRepo.resetVoyage()
        XCTAssertNil(voyageRepo.getVoyage())

        XCTAssertTrue(voyageRepo.restoreArchive(id: named!.id))
        XCTAssertEqual(voyageRepo.getVoyage()?.shipName, "Disney Adventure")
        XCTAssertEqual(eventRepo.getAllEvents().count, eventCount)
        XCTAssertFalse(voyageRepo.restoreArchive(id: 9999), "Unknown archive id should report failure")
    }

    func testImportArchive_appliesCustomName() {
        createArchiveTestVoyage()
        let source = voyageRepo.archiveCurrentVoyage(customName: nil)!
        let json = serializeArchiveSnapshot(source.snapshot)

        let imported = voyageRepo.importArchive(jsonString: json, customName: "导入的存档")
        XCTAssertEqual(imported?.name, "导入的存档")
        XCTAssertEqual(imported?.eventCount, source.eventCount)
        XCTAssertNil(voyageRepo.importArchive(jsonString: "not json", customName: nil))
    }

    func testArchiveEntry_decodesLegacyJSONWithoutCustomName() throws {
        createArchiveTestVoyage()
        let entry = voyageRepo.archiveCurrentVoyage(customName: "临时")!

        // Simulate an archive persisted before `customName` existed.
        var object = try XCTUnwrap(JSONSerialization.jsonObject(with: JSONEncoder().encode(entry)) as? [String: Any])
        object.removeValue(forKey: "customName")
        let legacyData = try JSONSerialization.data(withJSONObject: object)

        let decoded = try JSONDecoder().decode(VoyageArchiveEntry.self, from: legacyData)
        XCTAssertNil(decoded.customName)
        XCTAssertEqual(decoded.name, decoded.shipName)
    }

    // MARK: - Category Repository Tests

    func testCategoryRepository() {
        categoryRepo.seedBuiltInCategories()
        XCTAssertEqual(categoryRepo.getCategories().count, 9)

        // Idempotent seeding
        categoryRepo.seedBuiltInCategories()
        XCTAssertEqual(categoryRepo.getCategories().count, 9)

        // Add custom category
        let custom = categoryRepo.addCategory(name: "Photo Session", colorArgb: 0xFF5B8DEF, iconKey: "camera")
        XCTAssertGreaterThan(custom.id, 0)
        XCTAssertEqual(custom.name, "Photo Session")
        XCTAssertFalse(custom.isBuiltIn)

        // Update category
        var updatedCustom = custom
        updatedCustom.name = "Character Photos"
        categoryRepo.updateCategory(updatedCustom)
        XCTAssertEqual(categoryRepo.getCategory(byId: custom.id)?.name, "Character Photos")

        // Archive category
        categoryRepo.archiveCategory(id: custom.id)
        XCTAssertTrue(categoryRepo.getCategory(byId: custom.id)?.isArchived == true)
        XCTAssertFalse(categoryRepo.getActiveCategories().contains { $0.id == custom.id })

        // Built-in deletion is blocked
        let builtIn = categoryRepo.getCategories().first { $0.isBuiltIn }!
        categoryRepo.deleteCategory(id: builtIn.id, fallbackCategoryId: 1)
        XCTAssertNotNil(categoryRepo.getCategory(byId: builtIn.id))

        // Custom deletion with reassignment
        let event = CruiseEvent(
            id: 0,
            date: SimpleDate(year: 2026, month: 8, day: 23),
            startTime: SimpleTime(hour: 10, minute: 0),
            durationMinutes: 30,
            categoryId: custom.id,
            title: "Mickey Meet"
        )
        let savedEvent = eventRepo.addEvent(event: event, recurrenceDays: [])
        XCTAssertEqual(savedEvent.categoryId, custom.id)

        let fallbackCatId = builtIn.id
        categoryRepo.deleteCategory(id: custom.id, fallbackCategoryId: fallbackCatId)
        XCTAssertNil(categoryRepo.getCategory(byId: custom.id))

        let reloadedEvent = eventRepo.getEvent(byId: savedEvent.id)
        XCTAssertEqual(reloadedEvent?.categoryId, fallbackCatId)
    }

    // MARK: - Event Repository Tests

    func testEventCRUDAndLaneAssignment() {
        let date = SimpleDate(year: 2026, month: 8, day: 23)

        // Event 1: 09:00 - 10:00 -> Lane 0
        let ev1 = CruiseEvent(
            id: 0,
            date: date,
            startTime: SimpleTime(hour: 9, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "Breakfast"
        )
        let saved1 = eventRepo.addEvent(event: ev1, recurrenceDays: [])
        XCTAssertEqual(saved1.lane, 0)

        // Event 2: 09:30 - 10:30 (overlaps with ev1) -> Lane 1
        let ev2 = CruiseEvent(
            id: 0,
            date: date,
            startTime: SimpleTime(hour: 9, minute: 30),
            durationMinutes: 60,
            categoryId: 2,
            title: "Show"
        )
        let saved2 = eventRepo.addEvent(event: ev2, recurrenceDays: [])
        XCTAssertEqual(saved2.lane, 1)

        // Event 3: 09:45 - 10:15 (overlaps with ev1 and ev2) -> Lane 2
        let ev3 = CruiseEvent(
            id: 0,
            date: date,
            startTime: SimpleTime(hour: 9, minute: 45),
            durationMinutes: 30,
            categoryId: 3,
            title: "Meetup"
        )
        let saved3 = eventRepo.addEvent(event: ev3, recurrenceDays: [])
        XCTAssertEqual(saved3.lane, 2)

        // Event 4: 11:00 - 12:00 (no overlap) -> Lane 0
        let ev4 = CruiseEvent(
            id: 0,
            date: date,
            startTime: SimpleTime(hour: 11, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "Lunch"
        )
        let saved4 = eventRepo.addEvent(event: ev4, recurrenceDays: [])
        XCTAssertEqual(saved4.lane, 0)

        // Update event title only (timing unchanged -> lane unchanged)
        var updated1 = saved1
        updated1.title = "Family Breakfast"
        let updateResult = eventRepo.updateEvent(event: updated1, scope: .thisDayOnly)
        XCTAssertEqual(updateResult.first?.title, "Family Breakfast")
        XCTAssertEqual(updateResult.first?.lane, 0)

        // Toggle active: deactivating releases conflict
        let deactivated = eventRepo.toggleEventActive(id: saved1.id)
        XCTAssertFalse(deactivated!.isActive)

        // Move lane manually
        eventRepo.setEventLane(id: saved4.id, lane: 2)
        XCTAssertEqual(eventRepo.getEvent(byId: saved4.id)?.lane, 2)

        // Conflicts check
        let conflicts = eventRepo.conflictsOn(date: date)
        // saved2 and saved3 overlap while both active
        XCTAssertFalse(conflicts.isEmpty)

        // Delete event
        eventRepo.deleteEvent(id: saved4.id, scope: .thisDayOnly)
        XCTAssertNil(eventRepo.getEvent(byId: saved4.id))
    }

    func testEventRecurrence() {
        let boardDate = SimpleDate(year: 2026, month: 8, day: 23)
        let disembarkDate = SimpleDate(year: 2026, month: 8, day: 26)

        _ = voyageRepo.createVoyage(
            shipName: "Disney Adventure",
            boardDate: boardDate,
            boardTime: SimpleTime(hour: 13, minute: 0),
            disembarkDate: disembarkDate
        )

        // Add daily breakfast (23, 24, 25)
        let recurringBreakfast = CruiseEvent(
            id: 0,
            date: boardDate,
            startTime: SimpleTime(hour: 8, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "Breakfast"
        )
        let events = eventRepo.addEvent(event: recurringBreakfast, recurring: true)
        XCTAssertEqual(events.count, 3) // Aug 23, 24, 25

        let groupId = events.first?.recurrenceGroupId
        XCTAssertNotNil(groupId)

        // Update all events in group (e.g. change time to 08:30)
        var firstEvent = events.first!
        firstEvent.startTime = SimpleTime(hour: 8, minute: 30)
        let updatedGroup = eventRepo.updateEvent(event: firstEvent, scope: .wholeGroup)
        XCTAssertEqual(updatedGroup.count, 3)
        for ev in updatedGroup {
            XCTAssertEqual(ev.startTime, SimpleTime(hour: 8, minute: 30))
            XCTAssertEqual(ev.recurrenceGroupId, groupId)
        }

        // Single day edit (detaches from group)
        var secondDayEvent = updatedGroup[1]
        secondDayEvent.title = "Character Breakfast"
        let detached = eventRepo.updateEvent(event: secondDayEvent, scope: .thisDayOnly)
        XCTAssertEqual(detached.count, 1)
        XCTAssertNil(detached.first?.recurrenceGroupId)

        // Delete remaining whole group
        eventRepo.deleteEvent(id: firstEvent.id, scope: .wholeGroup)
        XCTAssertNil(eventRepo.getEvent(byId: firstEvent.id))
        XCTAssertNil(eventRepo.getEvent(byId: updatedGroup[2].id))
        // Detached event on Day 2 remains
        XCTAssertNotNil(eventRepo.getEvent(byId: secondDayEvent.id))
    }

    // MARK: - Pixie Dust Repository Tests

    func testPixieDustRepository() {
        pixieDustRepo.seedDefaultLevels()
        let levels = pixieDustRepo.getLevels()
        XCTAssertEqual(levels.count, 3)

        // Add custom level
        let customLevel = pixieDustRepo.addLevel(name: "特别礼物", colorArgb: 0xFFFF5722, iconKey: "star", rank: 4)
        XCTAssertEqual(customLevel.name, "特别礼物")
        XCTAssertEqual(pixieDustRepo.getLevels().count, 4)

        // Update level
        var updatedLevel = customLevel
        updatedLevel.name = "至尊礼物"
        pixieDustRepo.updateLevel(updatedLevel)
        XCTAssertEqual(pixieDustRepo.getLevel(byId: customLevel.id)?.name, "至尊礼物")

        // Add records
        let r1 = pixieDustRepo.addRecord(direction: .given, deck: 10, room: "012", levelId: customLevel.id, levelNameSnapshot: "至尊礼物", note: "Card")
        let r2 = pixieDustRepo.addRecord(direction: .received, deck: 11, room: "550", levelId: 1, levelNameSnapshot: "普通", note: "Magnet", replied: true)

        XCTAssertEqual(pixieDustRepo.getRecords().count, 2)
        XCTAssertEqual(pixieDustRepo.getRecords(direction: .given).count, 1)
        XCTAssertEqual(pixieDustRepo.getRecords(direction: .received).count, 1)

        // Update record
        var updatedR1 = r1
        updatedR1.replied = true
        pixieDustRepo.updateRecord(updatedR1)
        XCTAssertTrue(pixieDustRepo.getRecord(byId: r1.id)?.replied == true)

        // Delete record
        pixieDustRepo.deleteRecord(id: r2.id)
        XCTAssertEqual(pixieDustRepo.getRecords().count, 1)

        // Delete level
        pixieDustRepo.deleteLevel(id: customLevel.id)
        XCTAssertNil(pixieDustRepo.getLevel(byId: customLevel.id))
    }

    // MARK: - Wishlist Repository Tests

    func testWishlistRepository() {
        let item1 = wishlistRepo.addItem(
            title: "Try TWG Tea",
            categoryId: 1,
            deck: 7,
            zone: .mid,
            placeName: "TWG Lounge",
            note: "Earl Grey"
        )
        let item2 = wishlistRepo.addItem(
            title: "Buy Mickey Ears",
            categoryId: 2,
            deck: 6,
            zone: .mid,
            placeName: "World of Disney"
        )

        XCTAssertEqual(wishlistRepo.getItems().count, 2)
        XCTAssertFalse(item1.isCompleted)

        // Toggle completed
        let toggled = wishlistRepo.toggleCompleted(id: item1.id)
        XCTAssertTrue(toggled?.isCompleted == true)
        XCTAssertTrue(wishlistRepo.getItem(byId: item1.id)?.isCompleted == true)

        // Update item
        var updated = item2
        updated.note = "Gold Edition"
        wishlistRepo.updateItem(updated)
        XCTAssertEqual(wishlistRepo.getItem(byId: item2.id)?.note, "Gold Edition")

        // Delete item
        wishlistRepo.deleteItem(id: item1.id)
        XCTAssertEqual(wishlistRepo.getItems().count, 1)
        XCTAssertNil(wishlistRepo.getItem(byId: item1.id))
    }

    // MARK: - Location Preset Repository Tests

    func testLocationPresetRepository() {
        locationPresetRepo.recordUsage(deck: 6, zone: .mid, placeName: "Walt Disney Theatre")
        locationPresetRepo.recordUsage(deck: 6, zone: .mid, placeName: "Walt Disney Theatre")
        locationPresetRepo.recordUsage(deck: 10, zone: .stern, placeName: "Garden Stage")

        let presets = locationPresetRepo.getPresets()
        XCTAssertEqual(presets.count, 2)

        // Walt Disney Theatre should be first due to useCount = 2
        XCTAssertEqual(presets[0].placeName, "Walt Disney Theatre")
        XCTAssertEqual(presets[0].useCount, 2)

        XCTAssertEqual(presets[1].placeName, "Garden Stage")
        XCTAssertEqual(presets[1].useCount, 1)

        // Delete preset
        locationPresetRepo.deletePreset(id: presets[1].id)
        XCTAssertEqual(locationPresetRepo.getPresets().count, 1)
    }

    // MARK: - Settings Store Tests

    func testSettingsStore() {
        XCTAssertEqual(settingsStore.settings.displayStartMinute, 420)
        XCTAssertEqual(settingsStore.settings.displayEndMinute, 1440)
        XCTAssertEqual(settingsStore.settings.colorTheme, .judy_blue)

        // Update range
        settingsStore.setDisplayRange(startMinute: 360, endMinute: 1320)
        XCTAssertEqual(settingsStore.settings.displayStartMinute, 360)
        XCTAssertEqual(settingsStore.settings.displayEndMinute, 1320)

        // Update hour height with bounds clamping [60, 180]
        settingsStore.setHourHeight(200.0)
        XCTAssertEqual(settingsStore.settings.hourHeightPt, 180.0)
        settingsStore.setHourHeight(40.0)
        XCTAssertEqual(settingsStore.settings.hourHeightPt, 60.0)

        // Update color theme
        settingsStore.setColorTheme(.linabell_pink)
        XCTAssertEqual(settingsStore.settings.colorTheme, .linabell_pink)

        // Update wallpaper settings
        settingsStore.setWallpaperEnabled(true)
        settingsStore.setWallpaperKey("linabell")
        settingsStore.setWallpaperAlpha(0.25)
        XCTAssertTrue(settingsStore.settings.wallpaperEnabled)
        XCTAssertEqual(settingsStore.settings.wallpaperKey, "linabell")
        XCTAssertEqual(settingsStore.settings.wallpaperAlpha, 0.25)

        // Armed event IDs
        settingsStore.setArmedEventIds([101, 102, 103])
        XCTAssertEqual(settingsStore.getArmedEventIds(), Set([101, 102, 103]))

        // Reset
        settingsStore.reset()
        XCTAssertEqual(settingsStore.settings.colorTheme, .judy_blue)
        XCTAssertEqual(settingsStore.settings.displayStartMinute, 420)
        XCTAssertTrue(settingsStore.getArmedEventIds().isEmpty)
    }
}
