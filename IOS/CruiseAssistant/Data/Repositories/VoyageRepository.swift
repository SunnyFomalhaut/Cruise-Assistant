import Foundation

/// Interface for voyage data access and lifecycle operations.
public protocol VoyageRepositoryProtocol: Sendable {
    func getVoyage() -> Voyage?
    func createVoyage(
        shipName: String?,
        boardDate: SimpleDate,
        boardTime: SimpleTime,
        disembarkDate: SimpleDate,
        disembarkTime: SimpleTime,
        maxDeck: Int,
        minDeck: Int,
        dinnerTime: SimpleTime,
        shipPresetName: String?
    ) -> Voyage
    func updateVoyage(_ voyage: Voyage)
    func resetVoyage()
    func getDayPlans() -> [SimpleDate: DayPlan]
    func setFinalized(date: SimpleDate, finalized: Bool, nowMs: Int64)
    func addPresetShows(dinnerTime: SimpleTime?)
}

/// Fixed duration and schedule constants matching cruise specifications.
private let BOARDING_DURATION_MINUTES = 15
private let DISEMBARK_DURATION_MINUTES = 30
private let SAFETY_DRILL_DURATION_MINUTES = 35
private let DINNER_DURATION_MINUTES = 75
private let SAFETY_DRILL_TIME = SimpleTime(hour: 15, minute: 45)

/// Repository managing voyage configuration, day plans, and lifecycle.
public final class VoyageRepository: VoyageRepositoryProtocol, @unchecked Sendable {
    private let database: CruiseDatabase
    private let categoryRepository: CategoryRepositoryProtocol

    public init(
        database: CruiseDatabase = .shared,
        categoryRepository: CategoryRepositoryProtocol? = nil
    ) {
        self.database = database
        self.categoryRepository = categoryRepository ?? CategoryRepository(database: database)
    }

    public func getVoyage() -> Voyage? {
        database.getVoyage()
    }

    public func createVoyage(
        shipName: String?,
        boardDate: SimpleDate,
        boardTime: SimpleTime,
        disembarkDate: SimpleDate,
        disembarkTime: SimpleTime = SimpleTime(hour: 7, minute: 0),
        maxDeck: Int = 20,
        minDeck: Int = 1,
        dinnerTime: SimpleTime = SimpleTime(hour: 17, minute: 45),
        shipPresetName: String? = nil
    ) -> Voyage {
        let trimmedShipName = shipName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedShipName = (trimmedShipName?.isEmpty == false) ? trimmedShipName : nil

        let voyage = Voyage(
            id: 1,
            shipName: cleanedShipName,
            boardDate: boardDate,
            boardTime: boardTime,
            disembarkDate: disembarkDate,
            disembarkTime: disembarkTime,
            maxDeck: maxDeck,
            minDeck: minDeck
        )
        database.clearEvents()
        database.clearDayPlans()
        database.saveVoyage(voyage)
        // 1. Create DayPlan for each day
        let validDates = voyage.datesInVoyage()
        for date in validDates {
            database.saveDayPlan(DayPlan(date: date, finalized: false, finalizedAt: nil))
        }

        // 2. Seed built-in categories idempotently
        categoryRepository.seedBuiltInCategories()
        let categories = database.getCategories()
        let importantId = categories.first(where: { $0.iconKey == "important" })?.id
            ?? categories.first?.id
            ?? 1

        // 3. System events: Boarding and Disembarking
        let boardingEvent = CruiseEvent(
            id: 0,
            date: boardDate,
            startTime: boardTime,
            durationMinutes: BOARDING_DURATION_MINUTES,
            categoryId: importantId,
            title: "登船",
            isSystemGenerated: true
        )
        database.saveEvent(boardingEvent)

        let disembarkEvent = CruiseEvent(
            id: 0,
            date: disembarkDate,
            startTime: disembarkTime,
            durationMinutes: DISEMBARK_DURATION_MINUTES,
            categoryId: importantId,
            title: "下船",
            isSystemGenerated: true
        )
        database.saveEvent(disembarkEvent)

        // 4. Built-in ship preset events (Safety drill, daily dinners, and preset shows)
        if shipPresetName != nil {
            let mealId = categories.first(where: { $0.iconKey == "meal" })?.id ?? importantId

            // Safety Drill on embarkation day, 15:45-16:20, Deck 6 MID, lane 1
            let safetyDrill = CruiseEvent(
                id: 0,
                date: boardDate,
                startTime: SAFETY_DRILL_TIME,
                durationMinutes: SAFETY_DRILL_DURATION_MINUTES,
                categoryId: importantId,
                title: "安全演习",
                deck: 6,
                zone: .mid,
                placeName: nil,
                lane: 1,
                isSystemGenerated: false,
                presetKey: "safety_drill"
            )
            database.saveEvent(safetyDrill)

            // Dinners on all voyage dates except disembarkation day
            for date in validDates where date != disembarkDate {
                let dinner = CruiseEvent(
                    id: 0,
                    date: date,
                    startTime: dinnerTime,
                    durationMinutes: DINNER_DURATION_MINUTES,
                    categoryId: mealId,
                    title: "晚餐",
                    isSystemGenerated: false,
                    presetKey: "dinner"
                )
                database.saveEvent(dinner)
            }

            // 5. Official Disney Adventure Shows (scheduled if ship is Disney Adventure)
            let isAdventure = shipPresetName?.localizedCaseInsensitiveContains("adventure") == true
                || shipPresetName?.localizedCaseInsensitiveContains("探险") == true
                || shipName?.localizedCaseInsensitiveContains("adventure") == true
                || shipName?.localizedCaseInsensitiveContains("探险") == true

            if isAdventure {
                let showId = categories.first(where: { $0.iconKey == "show" })?.id ?? importantId
                var showLanesByDay: [SimpleDate: [(lane: Int, start: Int, end: Int)]] = [:]
                for show in AUTO_GENERATED_DISNEY_ADVENTURE_SHOWS {
                    let showDates = show.resolveDates(boardDate: boardDate, dayCount: voyage.dayCount)
                    for date in showDates {
                        let times: [SimpleTime]
                        if show.dinnerLinked {
                            times = [pickDinnerLinkedTime(dinnerTime: dinnerTime)]
                        } else {
                            times = show.startTimes
                        }

                        for startTime in times {
                            let startMin = startTime.totalMinutes
                            let effStart = startMin - show.defaultWaitMinutes
                            let effEnd = startMin + show.durationMinutes
                            let occupied = showLanesByDay[date] ?? []
                            let lane = assignShowLane(startMinute: effStart, endMinute: effEnd, occupied: occupied)
                            showLanesByDay[date, default: []].append((lane: lane, start: effStart, end: effEnd))

                            let showEvent = CruiseEvent(
                                id: 0,
                                date: date,
                                startTime: startTime,
                                durationMinutes: show.durationMinutes,
                                categoryId: showId,
                                title: show.title,
                                deck: show.deck,
                                zone: show.zone,
                                placeName: show.placeName,
                                lane: lane,
                                isSystemGenerated: false,
                                presetKey: show.key,
                                waitMinutes: show.defaultWaitMinutes,
                                note: show.note
                            )
                            database.saveEvent(showEvent)
                        }
                    }
                }

                let existingWishlistTitles = Set(database.getWishlistItems().map { $0.title })
                for show in WISHLIST_SEED_SHOWS {
                    if !existingWishlistTitles.contains(show.title) {
                        let item = WishlistItem(
                            id: 0,
                            title: show.title,
                            categoryId: showId,
                            deck: show.deck,
                            zone: show.zone,
                            placeName: show.placeName,
                            note: show.note,
                            isCompleted: false,
                            createdAt: Int64(Date().timeIntervalSince1970 * 1000)
                        )
                        database.saveWishlistItem(item)
                    }
                }
            }
        }

        return voyage
    }

    /// 一键预添加或补全迪士尼探险号预置演出
    public func addPresetShows(dinnerTime: SimpleTime? = nil) {
        guard let voyage = getVoyage() else { return }
        let currentDinnerTime = dinnerTime ?? SimpleTime(hour: 17, minute: 45)
        let categories = database.getCategories()
        let showId = categories.first(where: { $0.iconKey == "show" })?.id
            ?? categories.first(where: { $0.name == "演出" })?.id
            ?? 1

        let existingEvents = database.getAllEvents()
        var showLanesByDay: [SimpleDate: [(lane: Int, start: Int, end: Int)]] = [:]
        for show in AUTO_GENERATED_DISNEY_ADVENTURE_SHOWS {
            let showDates = show.resolveDates(boardDate: voyage.boardDate, dayCount: voyage.dayCount)
            for date in showDates {
                let times: [SimpleTime]
                if show.dinnerLinked {
                    times = [pickDinnerLinkedTime(dinnerTime: currentDinnerTime)]
                } else {
                    times = show.startTimes
                }

                for startTime in times {
                    let isAlreadyAdded = existingEvents.contains { ev in
                        ev.date == date && ev.presetKey == show.key && ev.startTime == startTime
                    }
                    if isAlreadyAdded { continue }

                    let startMin = startTime.totalMinutes
                    let effStart = startMin - show.defaultWaitMinutes
                    let effEnd = startMin + show.durationMinutes
                    let occupied = showLanesByDay[date] ?? []
                    let lane = assignShowLane(startMinute: effStart, endMinute: effEnd, occupied: occupied)
                    showLanesByDay[date, default: []].append((lane: lane, start: effStart, end: effEnd))

                    let showEvent = CruiseEvent(
                        id: 0,
                        date: date,
                        startTime: startTime,
                        durationMinutes: show.durationMinutes,
                        categoryId: showId,
                        title: show.title,
                        deck: show.deck,
                        zone: show.zone,
                        placeName: show.placeName,
                        lane: lane,
                        isSystemGenerated: false,
                        presetKey: show.key,
                        waitMinutes: show.defaultWaitMinutes,
                        note: show.note
                    )
                    database.saveEvent(showEvent)
                }
            }
        }
    }

    public func updateVoyage(_ voyage: Voyage) {
        database.saveVoyage(voyage)

        let validDates = Set(voyage.datesInVoyage())
        let existingPlans = database.getDayPlans()
        // Delete non-system events falling outside the new voyage range
        let allEvents = database.getAllEvents()
        for event in allEvents where !event.isSystemGenerated && !validDates.contains(event.date) {
            database.deleteEvent(id: event.id)
        }

        // Delete day plans outside the new voyage range
        for date in existingPlans.keys where !validDates.contains(date) {
            database.deleteDayPlan(for: date)
        }


        // Relocate boarding and disembarkation system events
        for event in database.getAllEvents() where event.isSystemGenerated {
            if event.title == "登船" {
                let updated = CruiseEvent(
                    id: event.id,
                    date: voyage.boardDate,
                    startTime: voyage.boardTime,
                    durationMinutes: event.durationMinutes,
                    categoryId: event.categoryId,
                    title: event.title,
                    isSystemGenerated: true
                )
                database.saveEvent(updated)
            } else if event.title == "下船" {
                let updated = CruiseEvent(
                    id: event.id,
                    date: voyage.disembarkDate,
                    startTime: voyage.disembarkTime,
                    durationMinutes: event.durationMinutes,
                    categoryId: event.categoryId,
                    title: event.title,
                    isSystemGenerated: true
                )
                database.saveEvent(updated)
            }
        }
    }

    public func resetVoyage() {
        database.clearVoyage()
        database.clearDayPlans()
        database.clearEvents()
        database.clearLocationPresets()
        database.clearPixieDustRecords()
        categoryRepository.seedBuiltInCategories()
        for level in PixieLevel.BUILT_IN_PIXIE_LEVELS {
            database.savePixieLevel(level)
        }
    }

    public func getDayPlans() -> [SimpleDate: DayPlan] {
        database.getDayPlans()
    }

    public func setFinalized(date: SimpleDate, finalized: Bool, nowMs: Int64) {
        let plan = DayPlan(
            date: date,
            finalized: finalized,
            finalizedAt: finalized ? nowMs : nil
        )
        database.saveDayPlan(plan)
    }
}
