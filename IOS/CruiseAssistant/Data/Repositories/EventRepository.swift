import Foundation

/// Recurrence scope when editing or deleting recurring events.
public enum RecurrenceScope: String, Codable, Sendable {
    case thisDayOnly
    case wholeGroup
}

/// Interface for cruise event queries and mutations.
public protocol EventRepositoryProtocol: Sendable {
    func getEvents(for date: SimpleDate) -> [CruiseEvent]
    func getAllEvents() -> [CruiseEvent]
    func getEvent(byId id: Int64) -> CruiseEvent?
    func addEvent(event: CruiseEvent, recurrenceDays: [SimpleDate]) -> CruiseEvent
    func addEvent(event: CruiseEvent, recurring: Bool) -> [CruiseEvent]
    func updateEvent(event: CruiseEvent, scope: RecurrenceScope) -> [CruiseEvent]
    func deleteEvent(id: Int64, scope: RecurrenceScope)
    func toggleEventActive(id: Int64) -> CruiseEvent?
    func setEventLane(id: Int64, lane: Int)
    func updateDinnerEventsTime(newTime: SimpleTime)
    func conflictsOn(date: SimpleDate) -> [(CruiseEvent, CruiseEvent)]
    func keepOnlyThisShow(target: CruiseEvent)
}

/// Repository managing scheduled cruise events and timeline lane calculations.
public final class EventRepository: EventRepositoryProtocol, @unchecked Sendable {
    private let database: CruiseDatabase

    public init(database: CruiseDatabase = .shared) {
        self.database = database
    }

    public func getEvents(for date: SimpleDate) -> [CruiseEvent] {
        database.getEvents(for: date)
    }

    public func getAllEvents() -> [CruiseEvent] {
        database.getAllEvents()
    }

    public func getEvent(byId id: Int64) -> CruiseEvent? {
        database.getEvent(byId: id)
    }

    // MARK: - Lane Assignment Helper

    private func assignLane(
        startMinute: Int,
        endMinute: Int,
        sameDayEvents: [CruiseEvent],
        preferredLane: Int? = nil,
        excludeId: Int64? = nil
    ) -> Int {
        let activeEvents = sameDayEvents.filter { $0.isActive && $0.id != excludeId }

        // If preferred lane is available, keep it
        if let pref = preferredLane, pref >= 0, pref < LANE_COUNT {
            let inPref = activeEvents.filter { $0.lane == pref }
            let hasOverlap = inPref.contains { ev in
                max(startMinute, ev.startMinute) < min(endMinute, ev.endMinute)
            }
            if !hasOverlap {
                return pref
            }
        }

        // Find first free lane
        for lane in 0..<LANE_COUNT {
            let inLane = activeEvents.filter { $0.lane == lane }
            let hasOverlap = inLane.contains { ev in
                max(startMinute, ev.startMinute) < min(endMinute, ev.endMinute)
            }
            if !hasOverlap {
                return lane
            }
        }
        return 0
    }

    // MARK: - Add Event

    @discardableResult
    public func addEvent(event: CruiseEvent, recurrenceDays: [SimpleDate] = []) -> CruiseEvent {
        if recurrenceDays.isEmpty {
            let sameDay = database.getEvents(for: event.date)
            let lane = assignLane(startMinute: event.startMinute, endMinute: event.endMinute, sameDayEvents: sameDay)
            var toSave = event
            toSave.lane = lane
            return database.saveEvent(toSave)
        } else {
            let groupId = event.recurrenceGroupId ?? UUID().uuidString
            var savedFirst: CruiseEvent?
            let allDates = [event.date] + recurrenceDays.filter { $0 != event.date }

            for date in allDates {
                let sameDay = database.getEvents(for: date)
                let lane = assignLane(startMinute: event.startMinute, endMinute: event.endMinute, sameDayEvents: sameDay)
                var copy = event
                copy.id = 0
                copy.date = date
                copy.lane = lane
                copy.recurrenceGroupId = groupId
                let saved = database.saveEvent(copy)
                if savedFirst == nil {
                    savedFirst = saved
                }
            }
            return savedFirst ?? event
        }
    }

    @discardableResult
    public func addEvent(event: CruiseEvent, recurring: Bool) -> [CruiseEvent] {
        if !recurring {
            let saved = addEvent(event: event, recurrenceDays: [])
            return [saved]
        }

        let voyage = database.getVoyage()
        let recurrenceDays: [SimpleDate]
        if let voyage = voyage {
            // Expand from event.date + 1 up to (and excluding) disembarkDate
            let startDay = event.date.plusDays(1)
            let endDay = voyage.disembarkDate.plusDays(-1)
            if startDay <= endDay {
                let count = endDay.epochDay - startDay.epochDay + 1
                recurrenceDays = (0..<count).map { startDay.plusDays($0) }
            } else {
                recurrenceDays = []
            }
        } else {
            recurrenceDays = []
        }

        let groupId = UUID().uuidString
        var result: [CruiseEvent] = []
        let allDates = [event.date] + recurrenceDays

        for date in allDates {
            let sameDay = database.getEvents(for: date)
            let lane = assignLane(startMinute: event.startMinute, endMinute: event.endMinute, sameDayEvents: sameDay)
            var copy = event
            copy.id = 0
            copy.date = date
            copy.lane = lane
            copy.recurrenceGroupId = groupId
            result.append(database.saveEvent(copy))
        }
        return result
    }

    // MARK: - Update Event

    @discardableResult
    public func updateEvent(event: CruiseEvent, scope: RecurrenceScope = .thisDayOnly) -> [CruiseEvent] {
        let groupId = event.recurrenceGroupId
        let targets: [CruiseEvent]

        if scope == .wholeGroup, let groupId = groupId {
            let dayPlans = database.getDayPlans()
            let finalizedDates = Set(dayPlans.values.filter(\.finalized).map(\.date))
            targets = database.getEvents(byGroupId: groupId).filter {
                $0.id == event.id || !finalizedDates.contains($0.date)
            }
        } else {
            targets = [event]
        }

        var result: [CruiseEvent] = []
        for target in targets {
            var merged = event
            merged.id = target.id
            merged.date = target.date
            merged.recurrenceGroupId = (scope == .thisDayOnly) ? nil : target.recurrenceGroupId
            merged.isSystemGenerated = target.isSystemGenerated
            merged.isActive = target.isActive

            let timingUnchanged = (merged.startTime == target.startTime && merged.durationMinutes == target.durationMinutes)
            let lane: Int
            if timingUnchanged {
                lane = target.lane
            } else {
                let sameDay = database.getEvents(for: merged.date)
                lane = assignLane(
                    startMinute: merged.startMinute,
                    endMinute: merged.endMinute,
                    sameDayEvents: sameDay,
                    preferredLane: target.lane,
                    excludeId: merged.id
                )
            }
            merged.lane = lane
            database.updateEvent(merged)
            result.append(merged)
        }
        return result
    }

    // MARK: - Delete Event

    public func deleteEvent(id: Int64, scope: RecurrenceScope = .thisDayOnly) {
        guard let event = database.getEvent(byId: id), !event.isSystemGenerated else { return }

        if scope == .wholeGroup, let groupId = event.recurrenceGroupId {
            database.deleteEvents(byGroupId: groupId)
        } else {
            database.deleteEvent(id: id)
        }
    }

    // MARK: - Toggle Active

    public func toggleEventActive(id: Int64) -> CruiseEvent? {
        guard var event = database.getEvent(byId: id) else { return nil }
        event.isActive.toggle()

        if event.isActive {
            let sameDay = database.getEvents(for: event.date)
            event.lane = assignLane(
                startMinute: event.startMinute,
                endMinute: event.endMinute,
                sameDayEvents: sameDay,
                preferredLane: event.lane,
                excludeId: event.id
            )
        }

        database.updateEvent(event)
        return event
    }

    public func setEventLane(id: Int64, lane: Int) {
        guard var event = database.getEvent(byId: id) else { return }
        event.lane = lane
        database.updateEvent(event)
    }

    // MARK: - Show & Dinner Utilities

    public func keepOnlyThisShow(target: CruiseEvent) {
        let allEvents = database.getAllEvents()
        for event in allEvents {
            let isSameShow = (event.categoryId == target.categoryId &&
                              event.title == target.title &&
                              event.deck == target.deck &&
                              event.zone == target.zone &&
                              event.placeName == target.placeName)
            if isSameShow {
                if event.id != target.id && event.isActive {
                    var deactivated = event
                    deactivated.isActive = false
                    database.updateEvent(deactivated)
                }
            }
        }
        _ = toggleEventActive(id: target.id)
        if var activeTarget = database.getEvent(byId: target.id), !activeTarget.isActive {
            activeTarget.isActive = true
            database.updateEvent(activeTarget)
        }
    }

    public func updateDinnerEventsTime(newTime: SimpleTime) {
        let dinners = database.getEvents(byPresetKey: "dinner")
        for var dinner in dinners {
            dinner.startTime = newTime
            database.updateEvent(dinner)
        }
    }

    public func conflictsOn(date: SimpleDate) -> [(CruiseEvent, CruiseEvent)] {
        let activeEvents = database.getEvents(for: date).filter(\.isActive)
        var conflicts: [(CruiseEvent, CruiseEvent)] = []

        for i in 0..<activeEvents.count {
            for j in (i + 1)..<activeEvents.count {
                let e1 = activeEvents[i]
                let e2 = activeEvents[j]
                if max(e1.startMinute, e2.startMinute) < min(e1.endMinute, e2.endMinute) {
                    conflicts.append((e1, e2))
                }
            }
        }
        return conflicts
    }
}
