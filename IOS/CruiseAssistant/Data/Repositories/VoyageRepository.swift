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

        // 4. Built-in ship preset events (Safety drill and daily dinners)
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
        }

        return voyage
    }

    public func updateVoyage(_ voyage: Voyage) {
        database.saveVoyage(voyage)

        let validDates = Set(voyage.datesInVoyage())
        let existingPlans = database.getDayPlans()

        // Add missing day plans
        for date in validDates where existingPlans[date] == nil {
            database.saveDayPlan(DayPlan(date: date, finalized: false, finalizedAt: nil))
        }

        // Remove out-of-range day plans
        for date in existingPlans.keys where !validDates.contains(date) {
            database.deleteDayPlan(for: date)
        }

        // Handle events: update system events, remove out-of-range non-system events
        let allEvents = database.getAllEvents()

        // 1. Remove non-system events outside new date range
        let outOfRangeEvents = allEvents.filter { !validDates.contains($0.date) && !$0.isSystemGenerated }
        database.deleteEvents(ids: outOfRangeEvents.map(\.id))

        // 2. Move system events to updated times
        for var event in allEvents where event.isSystemGenerated {
            if event.title == "登船" || event.title == "上船" {
                event.title = "登船"
                event.date = voyage.boardDate
                event.startTime = voyage.boardTime
                database.updateEvent(event)
            } else if event.title == "下船" {
                event.date = voyage.disembarkDate
                event.startTime = voyage.disembarkTime
                database.updateEvent(event)
            }
        }
    }

    public func resetVoyage() {
        database.clearEvents()
        database.clearDayPlans()
        database.clearVoyage()
        database.clearLocationPresets()
        database.clearPixieDustRecords()
        database.clearPixieLevels()

        // Reseed default 3 pixie levels
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
