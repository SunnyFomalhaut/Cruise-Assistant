import Foundation

/// Internal state container for the offline Cruise Assistant database.
public struct CruiseDatabaseState: Codable, Sendable {
    public var voyage: Voyage?
    public var dayPlans: [DayPlan] = []
    public var categories: [Category] = []
    public var events: [CruiseEvent] = []
    public var locationPresets: [LocationPreset] = []
    public var pixieDustRecords: [PixieDustRecord] = []
    public var pixieLevels: [PixieLevel] = []
    public var wishlistItems: [WishlistItem] = []

    // Auto-incrementing sequence counters
    public var nextEventId: Int64 = 1
    public var nextCategoryId: Int64 = 100
    public var nextLocationPresetId: Int64 = 1
    public var nextPixieDustId: Int64 = 1
    public var nextPixieLevelId: Int64 = 10
    public var nextWishlistId: Int64 = 1

    public init() {}
}

/// Thread-safe offline persistent database for Cruise Assistant.
public final class CruiseDatabase: @unchecked Sendable {
    private let lock = NSRecursiveLock()
    private let fileURL: URL?
    private var state = CruiseDatabaseState()

    /// Default shared database instance with disk persistence in Application Support.
    public static let shared: CruiseDatabase = {
        let fileManager = FileManager.default
        let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dirURL = appSupport.appendingPathComponent("CruiseAssistant", isDirectory: true)
        try? fileManager.createDirectory(at: dirURL, withIntermediateDirectories: true)
        let dbURL = dirURL.appendingPathComponent("cruise_database.json")
        return CruiseDatabase(fileURL: dbURL)
    }()

    /// Creates an in-memory database instance (ideal for unit testing).
    public static func inMemory() -> CruiseDatabase {
        CruiseDatabase(fileURL: nil)
    }

    /// Initializes database with an optional file URL for disk persistence.
    public init(fileURL: URL? = nil) {
        self.fileURL = fileURL
        loadFromDisk()
    }

    // MARK: - Disk Persistence

    private func loadFromDisk() {
        guard let url = fileURL, FileManager.default.fileExists(atPath: url.path) else { return }
        lock.lock()
        defer { lock.unlock() }
        do {
            let data = try Data(contentsOf: url)
            let decoder = JSONDecoder()
            self.state = try decoder.decode(CruiseDatabaseState.self, from: data)
        } catch {
            print("[CruiseDatabase] Failed to load database from \(url.path): \(error)")
        }
    }

    private func saveToDisk() {
        guard let url = fileURL else { return }
        do {
            let encoder = JSONEncoder()
            encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
            let data = try encoder.encode(state)
            try data.write(to: url, options: .atomic)
        } catch {
            print("[CruiseDatabase] Failed to save database to \(url.path): \(error)")
        }
    }

    // MARK: - Voyage Operations

    public func getVoyage() -> Voyage? {
        lock.lock()
        defer { lock.unlock() }
        return state.voyage
    }

    public func saveVoyage(_ voyage: Voyage) {
        lock.lock()
        defer { lock.unlock() }
        state.voyage = voyage
        saveToDisk()
    }

    public func clearVoyage() {
        lock.lock()
        defer { lock.unlock() }
        state.voyage = nil
        saveToDisk()
    }

    // MARK: - DayPlan Operations

    public func getDayPlans() -> [SimpleDate: DayPlan] {
        lock.lock()
        defer { lock.unlock() }
        var result: [SimpleDate: DayPlan] = [:]
        for plan in state.dayPlans {
            result[plan.date] = plan
        }
        return result
    }

    public func getDayPlan(for date: SimpleDate) -> DayPlan? {
        lock.lock()
        defer { lock.unlock() }
        return state.dayPlans.first { $0.date == date }
    }

    public func saveDayPlan(_ plan: DayPlan) {
        lock.lock()
        defer { lock.unlock() }
        if let idx = state.dayPlans.firstIndex(where: { $0.date == plan.date }) {
            state.dayPlans[idx] = plan
        } else {
            state.dayPlans.append(plan)
        }
        saveToDisk()
    }

    public func saveDayPlans(_ plans: [DayPlan]) {
        lock.lock()
        defer { lock.unlock() }
        for plan in plans {
            if let idx = state.dayPlans.firstIndex(where: { $0.date == plan.date }) {
                state.dayPlans[idx] = plan
            } else {
                state.dayPlans.append(plan)
            }
        }
        saveToDisk()
    }

    public func deleteDayPlan(for date: SimpleDate) {
        lock.lock()
        defer { lock.unlock() }
        state.dayPlans.removeAll { $0.date == date }
        saveToDisk()
    }

    public func clearDayPlans() {
        lock.lock()
        defer { lock.unlock() }
        state.dayPlans.removeAll()
        saveToDisk()
    }

    // MARK: - Category Operations

    public func getCategories() -> [Category] {
        lock.lock()
        defer { lock.unlock() }
        return state.categories.sorted { $0.sortOrder < $1.sortOrder }
    }

    public func getActiveCategories() -> [Category] {
        lock.lock()
        defer { lock.unlock() }
        return state.categories.filter { !$0.isArchived }.sorted { $0.sortOrder < $1.sortOrder }
    }

    public func getCategory(byId id: Int64) -> Category? {
        lock.lock()
        defer { lock.unlock() }
        return state.categories.first { $0.id == id }
    }

    @discardableResult
    public func saveCategory(_ category: Category) -> Category {
        lock.lock()
        defer { lock.unlock() }
        var cat = category
        if cat.id == 0 {
            cat.id = state.nextCategoryId
            state.nextCategoryId += 1
            state.categories.append(cat)
        } else {
            if let idx = state.categories.firstIndex(where: { $0.id == cat.id }) {
                state.categories[idx] = cat
            } else {
                state.categories.append(cat)
                state.nextCategoryId = max(state.nextCategoryId, cat.id + 1)
            }
        }
        saveToDisk()
        return cat
    }

    @discardableResult
    public func saveCategories(_ categories: [Category]) -> [Category] {
        lock.lock()
        defer { lock.unlock() }
        var result: [Category] = []
        for category in categories {
            result.append(saveCategory(category))
        }
        return result
    }

    public func deleteCategory(id: Int64) {
        lock.lock()
        defer { lock.unlock() }
        state.categories.removeAll { $0.id == id }
        saveToDisk()
    }

    public func reassignCategory(from oldId: Int64, to newId: Int64) {
        lock.lock()
        defer { lock.unlock() }
        for i in 0..<state.events.count {
            if state.events[i].categoryId == oldId {
                state.events[i].categoryId = newId
            }
        }
        for i in 0..<state.wishlistItems.count {
            if state.wishlistItems[i].categoryId == oldId {
                state.wishlistItems[i].categoryId = newId
            }
        }
        saveToDisk()
    }

    public func clearCategories() {
        lock.lock()
        defer { lock.unlock() }
        state.categories.removeAll()
        saveToDisk()
    }

    // MARK: - Event Operations

    public func getAllEvents() -> [CruiseEvent] {
        lock.lock()
        defer { lock.unlock() }
        return state.events.sorted {
            if $0.date != $1.date { return $0.date < $1.date }
            if $0.startTime != $1.startTime { return $0.startTime < $1.startTime }
            return $0.id < $1.id
        }
    }

    public func getEvents(for date: SimpleDate) -> [CruiseEvent] {
        lock.lock()
        defer { lock.unlock() }
        return state.events.filter { $0.date == date }.sorted {
            if $0.startTime != $1.startTime { return $0.startTime < $1.startTime }
            return $0.id < $1.id
        }
    }

    public func getEvent(byId id: Int64) -> CruiseEvent? {
        lock.lock()
        defer { lock.unlock() }
        return state.events.first { $0.id == id }
    }

    public func getEvents(byGroupId groupId: String) -> [CruiseEvent] {
        lock.lock()
        defer { lock.unlock() }
        return state.events.filter { $0.recurrenceGroupId == groupId }
    }

    public func getEvents(byPresetKey key: String) -> [CruiseEvent] {
        lock.lock()
        defer { lock.unlock() }
        return state.events.filter { $0.presetKey == key }
    }

    @discardableResult
    public func saveEvent(_ event: CruiseEvent) -> CruiseEvent {
        lock.lock()
        defer { lock.unlock() }
        var ev = event
        if ev.id == 0 {
            ev.id = state.nextEventId
            state.nextEventId += 1
            state.events.append(ev)
        } else {
            if let idx = state.events.firstIndex(where: { $0.id == ev.id }) {
                state.events[idx] = ev
            } else {
                state.events.append(ev)
                state.nextEventId = max(state.nextEventId, ev.id + 1)
            }
        }
        saveToDisk()
        return ev
    }

    @discardableResult
    public func saveEvents(_ events: [CruiseEvent]) -> [CruiseEvent] {
        lock.lock()
        defer { lock.unlock() }
        var result: [CruiseEvent] = []
        for event in events {
            result.append(saveEvent(event))
        }
        return result
    }

    public func updateEvent(_ event: CruiseEvent) {
        lock.lock()
        defer { lock.unlock() }
        if let idx = state.events.firstIndex(where: { $0.id == event.id }) {
            state.events[idx] = event
            saveToDisk()
        }
    }

    public func updateEvents(_ events: [CruiseEvent]) {
        lock.lock()
        defer { lock.unlock() }
        for event in events {
            if let idx = state.events.firstIndex(where: { $0.id == event.id }) {
                state.events[idx] = event
            }
        }
        saveToDisk()
    }

    public func deleteEvent(id: Int64) {
        lock.lock()
        defer { lock.unlock() }
        state.events.removeAll { $0.id == id }
        saveToDisk()
    }

    public func deleteEvents(ids: [Int64]) {
        lock.lock()
        defer { lock.unlock() }
        let idSet = Set(ids)
        state.events.removeAll { idSet.contains($0.id) }
        saveToDisk()
    }

    public func deleteEvents(byGroupId groupId: String) {
        lock.lock()
        defer { lock.unlock() }
        state.events.removeAll { $0.recurrenceGroupId == groupId }
        saveToDisk()
    }

    public func clearEvents() {
        lock.lock()
        defer { lock.unlock() }
        state.events.removeAll()
        saveToDisk()
    }

    // MARK: - LocationPreset Operations

    public func getLocationPresets() -> [LocationPreset] {
        lock.lock()
        defer { lock.unlock() }
        return state.locationPresets.sorted {
            if $0.useCount != $1.useCount { return $0.useCount > $1.useCount }
            return $0.lastUsedEpochMs > $1.lastUsedEpochMs
        }
    }

    public func findLocationPreset(deck: Int, zone: ShipZone, placeName: String) -> LocationPreset? {
        lock.lock()
        defer { lock.unlock() }
        let trimmed = placeName.trimmingCharacters(in: .whitespacesAndNewlines)
        return state.locationPresets.first {
            $0.deck == deck && $0.zone == zone && $0.placeName.caseInsensitiveCompare(trimmed) == .orderedSame
        }
    }

    @discardableResult
    public func saveLocationPreset(_ preset: LocationPreset) -> LocationPreset {
        lock.lock()
        defer { lock.unlock() }
        var p = preset
        if p.id == 0 {
            p.id = state.nextLocationPresetId
            state.nextLocationPresetId += 1
            state.locationPresets.append(p)
        } else {
            if let idx = state.locationPresets.firstIndex(where: { $0.id == p.id }) {
                state.locationPresets[idx] = p
            } else {
                state.locationPresets.append(p)
                state.nextLocationPresetId = max(state.nextLocationPresetId, p.id + 1)
            }
        }
        saveToDisk()
        return p
    }

    public func deleteLocationPreset(id: Int64) {
        lock.lock()
        defer { lock.unlock() }
        state.locationPresets.removeAll { $0.id == id }
        saveToDisk()
    }

    public func clearLocationPresets() {
        lock.lock()
        defer { lock.unlock() }
        state.locationPresets.removeAll()
        saveToDisk()
    }

    // MARK: - PixieDustRecord Operations

    public func getPixieDustRecords(direction: PixieDustDirection? = nil) -> [PixieDustRecord] {
        lock.lock()
        defer { lock.unlock() }
        if let direction = direction {
            return state.pixieDustRecords.filter { $0.direction == direction }
        }
        return state.pixieDustRecords
    }

    public func getPixieDustRecord(byId id: Int64) -> PixieDustRecord? {
        lock.lock()
        defer { lock.unlock() }
        return state.pixieDustRecords.first { $0.id == id }
    }

    @discardableResult
    public func savePixieDustRecord(_ record: PixieDustRecord) -> PixieDustRecord {
        lock.lock()
        defer { lock.unlock() }
        var rec = record
        if rec.id == 0 {
            rec.id = state.nextPixieDustId
            state.nextPixieDustId += 1
            state.pixieDustRecords.append(rec)
        } else {
            if let idx = state.pixieDustRecords.firstIndex(where: { $0.id == rec.id }) {
                state.pixieDustRecords[idx] = rec
            } else {
                state.pixieDustRecords.append(rec)
                state.nextPixieDustId = max(state.nextPixieDustId, rec.id + 1)
            }
        }
        saveToDisk()
        return rec
    }

    public func deletePixieDustRecord(id: Int64) {
        lock.lock()
        defer { lock.unlock() }
        state.pixieDustRecords.removeAll { $0.id == id }
        saveToDisk()
    }

    public func clearPixieDustRecords() {
        lock.lock()
        defer { lock.unlock() }
        state.pixieDustRecords.removeAll()
        saveToDisk()
    }

    // MARK: - PixieLevel Operations

    public func getPixieLevels() -> [PixieLevel] {
        lock.lock()
        defer { lock.unlock() }
        return state.pixieLevels.sorted { $0.sortOrder < $1.sortOrder }
    }

    public func getPixieLevel(byId id: Int64) -> PixieLevel? {
        lock.lock()
        defer { lock.unlock() }
        return state.pixieLevels.first { $0.id == id }
    }

    @discardableResult
    public func savePixieLevel(_ level: PixieLevel) -> PixieLevel {
        lock.lock()
        defer { lock.unlock() }
        var lvl = level
        if lvl.id == 0 {
            lvl.id = state.nextPixieLevelId
            state.nextPixieLevelId += 1
            state.pixieLevels.append(lvl)
        } else {
            if let idx = state.pixieLevels.firstIndex(where: { $0.id == lvl.id }) {
                state.pixieLevels[idx] = lvl
            } else {
                state.pixieLevels.append(lvl)
                state.nextPixieLevelId = max(state.nextPixieLevelId, lvl.id + 1)
            }
        }
        saveToDisk()
        return lvl
    }

    @discardableResult
    public func savePixieLevels(_ levels: [PixieLevel]) -> [PixieLevel] {
        lock.lock()
        defer { lock.unlock() }
        var result: [PixieLevel] = []
        for level in levels {
            result.append(savePixieLevel(level))
        }
        return result
    }

    public func deletePixieLevel(id: Int64) {
        lock.lock()
        defer { lock.unlock() }
        state.pixieLevels.removeAll { $0.id == id }
        saveToDisk()
    }

    public func clearPixieLevels() {
        lock.lock()
        defer { lock.unlock() }
        state.pixieLevels.removeAll()
        saveToDisk()
    }

    // MARK: - WishlistItem Operations

    public func getWishlistItems() -> [WishlistItem] {
        lock.lock()
        defer { lock.unlock() }
        return state.wishlistItems.sorted { $0.createdAt < $1.createdAt }
    }

    public func getWishlistItem(byId id: Int64) -> WishlistItem? {
        lock.lock()
        defer { lock.unlock() }
        return state.wishlistItems.first { $0.id == id }
    }

    @discardableResult
    public func saveWishlistItem(_ item: WishlistItem) -> WishlistItem {
        lock.lock()
        defer { lock.unlock() }
        var itm = item
        if itm.id == 0 {
            itm.id = state.nextWishlistId
            state.nextWishlistId += 1
            state.wishlistItems.append(itm)
        } else {
            if let idx = state.wishlistItems.firstIndex(where: { $0.id == itm.id }) {
                state.wishlistItems[idx] = itm
            } else {
                state.wishlistItems.append(itm)
                state.nextWishlistId = max(state.nextWishlistId, itm.id + 1)
            }
        }
        saveToDisk()
        return itm
    }

    public func deleteWishlistItem(id: Int64) {
        lock.lock()
        defer { lock.unlock() }
        state.wishlistItems.removeAll { $0.id == id }
        saveToDisk()
    }

    public func clearWishlistItems() {
        lock.lock()
        defer { lock.unlock() }
        state.wishlistItems.removeAll()
        saveToDisk()
    }
}
