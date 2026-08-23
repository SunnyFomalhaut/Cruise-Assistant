import Foundation

/// Interface for Pixie Dust gift tracking and gift levels.
public protocol PixieDustRepositoryProtocol: Sendable {
    func getRecords(direction: PixieDustDirection?) -> [PixieDustRecord]
    func getRecord(byId id: Int64) -> PixieDustRecord?
    func addRecord(
        direction: PixieDustDirection,
        deck: Int,
        room: String,
        levelId: Int64,
        levelNameSnapshot: String,
        note: String,
        replied: Bool
    ) -> PixieDustRecord
    func updateRecord(_ record: PixieDustRecord)
    func deleteRecord(id: Int64)
    func getLevels() -> [PixieLevel]
    func getLevel(byId id: Int64) -> PixieLevel?
    func addLevel(name: String, colorArgb: Int64, iconKey: String, rank: Int) -> PixieLevel
    func updateLevel(_ level: PixieLevel)
    func deleteLevel(id: Int64)
    func seedDefaultLevels()
}

/// Repository managing Pixie Dust gift exchanges and gift tiers.
public final class PixieDustRepository: PixieDustRepositoryProtocol, @unchecked Sendable {
    private let database: CruiseDatabase

    public init(database: CruiseDatabase = .shared) {
        self.database = database
    }

    // MARK: - Records

    public func getRecords(direction: PixieDustDirection? = nil) -> [PixieDustRecord] {
        database.getPixieDustRecords(direction: direction)
    }

    public func getRecord(byId id: Int64) -> PixieDustRecord? {
        database.getPixieDustRecord(byId: id)
    }

    public func addRecord(
        direction: PixieDustDirection,
        deck: Int,
        room: String,
        levelId: Int64,
        levelNameSnapshot: String,
        note: String = "",
        replied: Bool = false
    ) -> PixieDustRecord {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let record = PixieDustRecord(
            id: 0,
            direction: direction,
            deck: deck,
            room: room,
            levelId: levelId,
            levelNameSnapshot: levelNameSnapshot,
            replied: replied,
            note: note,
            timestamp: nowMs
        )
        return database.savePixieDustRecord(record)
    }

    public func updateRecord(_ record: PixieDustRecord) {
        database.savePixieDustRecord(record)
    }

    public func deleteRecord(id: Int64) {
        database.deletePixieDustRecord(id: id)
    }

    // MARK: - Levels

    public func getLevels() -> [PixieLevel] {
        database.getPixieLevels()
    }

    public func getLevel(byId id: Int64) -> PixieLevel? {
        database.getPixieLevel(byId: id)
    }

    public func addLevel(name: String, colorArgb: Int64, iconKey: String, rank: Int) -> PixieLevel {
        let existing = database.getPixieLevels()
        let maxSort = existing.map(\.sortOrder).max() ?? -1
        let level = PixieLevel(
            id: 0,
            name: name,
            colorArgb: colorArgb,
            iconKey: iconKey,
            rank: rank,
            sortOrder: maxSort + 1
        )
        return database.savePixieLevel(level)
    }

    public func updateLevel(_ level: PixieLevel) {
        database.savePixieLevel(level)
    }

    public func deleteLevel(id: Int64) {
        database.deletePixieLevel(id: id)
    }

    public func seedDefaultLevels() {
        let existing = database.getPixieLevels()
        if existing.isEmpty {
            for level in PixieLevel.BUILT_IN_PIXIE_LEVELS {
                database.savePixieLevel(level)
            }
        }
    }
}
