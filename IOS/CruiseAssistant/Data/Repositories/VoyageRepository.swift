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
        shipPresetName: String?,
        isConcierge: Bool,
        legs: [Int],
        ashoreLegs: Set<Int>
    ) -> Voyage
    func updateVoyage(_ voyage: Voyage)
    func resetVoyage()
    func getDayPlans() -> [SimpleDate: DayPlan]
    func setFinalized(date: SimpleDate, finalized: Bool, nowMs: Int64)
    func addPresetShows(dinnerTime: SimpleTime?)
    func getArchives() -> [VoyageArchiveEntry]
    @discardableResult
    func archiveCurrentVoyage() -> VoyageArchiveEntry?
    func deleteArchive(id: Int64)
    func restoreArchive(_ entry: VoyageArchiveEntry)
    @discardableResult
    func importArchive(json: String) -> VoyageArchiveEntry?
}

public extension VoyageRepositoryProtocol {
    func createVoyage(
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
        createVoyage(
            shipName: shipName,
            boardDate: boardDate,
            boardTime: boardTime,
            disembarkDate: disembarkDate,
            disembarkTime: disembarkTime,
            maxDeck: maxDeck,
            minDeck: minDeck,
            dinnerTime: dinnerTime,
            shipPresetName: shipPresetName,
            isConcierge: false,
            legs: [],
            ashoreLegs: []
        )
    }
}

/// Fixed duration and schedule constants matching cruise specifications.
private let BOARDING_DURATION_MINUTES = 15
private let DISEMBARK_DURATION_MINUTES = 30
private let SAFETY_DRILL_DURATION_MINUTES = 35
private let DINNER_DURATION_MINUTES = 75
private let SAFETY_DRILL_TIME = SimpleTime(hour: 15, minute: 45)
private let JUNCTION_HANDOVER_START_TIME = SimpleTime(hour: 8, minute: 30)
private let JUNCTION_HANDOVER_DURATION_MINUTES = 90
private let ASHORE_START_TIME = SimpleTime(hour: 9, minute: 0)
private let ASHORE_DURATION_MINUTES = 480

/// Repository managing voyage configuration, day plans, archives, and lifecycle.
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
        shipPresetName: String? = nil,
        isConcierge: Bool = false,
        legs: [Int] = [],
        ashoreLegs: Set<Int> = []
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
            minDeck: minDeck,
            isConcierge: isConcierge,
            legs: legs,
            ashoreLegs: ashoreLegs
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

        // 3. System events: Boarding, Disembarking, Junction Handover, and Ashore Langkawi
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

        for junctionDate in voyage.junctionDates() {
            let handoverEvent = CruiseEvent(
                id: 0,
                date: junctionDate,
                startTime: JUNCTION_HANDOVER_START_TIME,
                durationMinutes: JUNCTION_HANDOVER_DURATION_MINUTES,
                categoryId: importantId,
                title: "办理交接手续",
                deck: 5,
                zone: .mid,
                isSystemGenerated: true,
                presetKey: "junction_handover"
            )
            database.saveEvent(handoverEvent)
        }

        let legBoardDates = voyage.legBoardDates()
        for legIdx in voyage.ashoreLegs.sorted() {
            guard legIdx >= 0 && legIdx < legBoardDates.count else { continue }
            let ashoreDate = legBoardDates[legIdx].plusDays(2)
            guard voyage.contains(date: ashoreDate) else { continue }
            let ashoreEvent = CruiseEvent(
                id: 0,
                date: ashoreDate,
                startTime: ASHORE_START_TIME,
                durationMinutes: ASHORE_DURATION_MINUTES,
                categoryId: importantId,
                title: "兰卡威登岸",
                isSystemGenerated: true,
                presetKey: "ashore_langkawi"
            )
            database.saveEvent(ashoreEvent)
        }

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

            // 5. Official Disney Adventure Shows (scheduled per leg if ship is Disney Adventure)
            let isAdventure = shipPresetName?.localizedCaseInsensitiveContains("adventure") == true
                || shipPresetName?.localizedCaseInsensitiveContains("探险") == true
                || shipName?.localizedCaseInsensitiveContains("adventure") == true
                || shipName?.localizedCaseInsensitiveContains("探险") == true

            if isAdventure {
                let showId = categories.first(where: { $0.iconKey == "show" })?.id ?? importantId
                var showLanesByDay: [SimpleDate: [(lane: Int, start: Int, end: Int)]] = [:]

                for (legIdx, legBoardDate) in legBoardDates.enumerated() {
                    let legNights = legIdx < voyage.legs.count ? voyage.legs[legIdx] : (voyage.dayCount - 1)
                    let legDayCount = max(2, legNights + 1)

                    for show in AUTO_GENERATED_DISNEY_ADVENTURE_SHOWS {
                        let showDates = show.resolveDates(boardDate: legBoardDate, dayCount: legDayCount)
                        for date in showDates where voyage.contains(date: date) {
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
        let legBoardDates = voyage.legBoardDates()

        for (legIdx, legBoardDate) in legBoardDates.enumerated() {
            let legNights = legIdx < voyage.legs.count ? voyage.legs[legIdx] : (voyage.dayCount - 1)
            let legDayCount = max(2, legNights + 1)

            for show in AUTO_GENERATED_DISNEY_ADVENTURE_SHOWS {
                let showDates = show.resolveDates(boardDate: legBoardDate, dayCount: legDayCount)
                for date in showDates where voyage.contains(date: date) {
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

        // Delete day plans outside the new voyage range, and add missing day plans
        for date in existingPlans.keys where !validDates.contains(date) {
            database.deleteDayPlan(for: date)
        }
        for date in voyage.datesInVoyage() where existingPlans[date] == nil {
            database.saveDayPlan(DayPlan(date: date, finalized: false, finalizedAt: nil))
        }

        let categories = database.getCategories()
        let importantId = categories.first(where: { $0.iconKey == "important" })?.id
            ?? categories.first?.id
            ?? 1

        // Relocate boarding and disembarkation system events, and rebuild junction/ashore events
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
            } else if event.title == "办理交接手续" || event.presetKey == "junction_handover"
                        || event.title == "兰卡威登岸" || event.presetKey == "ashore_langkawi" {
                database.deleteEvent(id: event.id)
            }
        }

        for junctionDate in voyage.junctionDates() {
            let handoverEvent = CruiseEvent(
                id: 0,
                date: junctionDate,
                startTime: JUNCTION_HANDOVER_START_TIME,
                durationMinutes: JUNCTION_HANDOVER_DURATION_MINUTES,
                categoryId: importantId,
                title: "办理交接手续",
                deck: 5,
                zone: .mid,
                isSystemGenerated: true,
                presetKey: "junction_handover"
            )
            database.saveEvent(handoverEvent)
        }

        let legBoardDates = voyage.legBoardDates()
        for legIdx in voyage.ashoreLegs.sorted() {
            guard legIdx >= 0 && legIdx < legBoardDates.count else { continue }
            let ashoreDate = legBoardDates[legIdx].plusDays(2)
            guard voyage.contains(date: ashoreDate) else { continue }
            let ashoreEvent = CruiseEvent(
                id: 0,
                date: ashoreDate,
                startTime: ASHORE_START_TIME,
                durationMinutes: ASHORE_DURATION_MINUTES,
                categoryId: importantId,
                title: "兰卡威登岸",
                isSystemGenerated: true,
                presetKey: "ashore_langkawi"
            )
            database.saveEvent(ashoreEvent)
        }
    }

    public func resetVoyage() {
        database.clearVoyage()
        database.clearDayPlans()
        database.clearEvents()
        database.clearLocationPresets()
        database.clearPixieDustRecords()
        database.clearPixieLevels()
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

    // MARK: - Archive Operations

    public func getArchives() -> [VoyageArchiveEntry] {
        database.getVoyageArchives()
    }

    @discardableResult
    public func archiveCurrentVoyage() -> VoyageArchiveEntry? {
        database.createArchiveFromCurrentState()
    }

    public func deleteArchive(id: Int64) {
        database.deleteVoyageArchive(id: id)
    }

    public func restoreArchive(_ entry: VoyageArchiveEntry) {
        database.restoreArchive(entry.snapshot)
    }

    @discardableResult
    public func importArchive(json: String) -> VoyageArchiveEntry? {
        guard let snapshot = parseArchiveSnapshot(json) else { return nil }
        let givenCount = snapshot.pixieDust.filter { $0.direction == .given }.count
        let receivedCount = snapshot.pixieDust.filter { $0.direction == .received }.count
        let entry = VoyageArchiveEntry(
            id: 0,
            shipName: snapshot.voyage.shipName ?? "未命名邮轮",
            boardDate: snapshot.voyage.boardDate,
            disembarkDate: snapshot.voyage.disembarkDate,
            archivedAt: Int64(Date().timeIntervalSince1970 * 1000),
            eventCount: snapshot.events.count,
            pixieGivenCount: givenCount,
            pixieReceivedCount: receivedCount,
            snapshot: snapshot
        )
        return database.saveVoyageArchive(entry)
    }
}
