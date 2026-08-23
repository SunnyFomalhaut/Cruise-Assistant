import Foundation

/// Interface for saved location preset history and autocompletion.
public protocol LocationPresetRepositoryProtocol: Sendable {
    func getPresets() -> [LocationPreset]
    func recordUsage(deck: Int, zone: ShipZone, placeName: String)
    func deletePreset(id: Int64)
}

/// Repository tracking frequently used custom locations.
public final class LocationPresetRepository: LocationPresetRepositoryProtocol, @unchecked Sendable {
    private let database: CruiseDatabase

    public init(database: CruiseDatabase = .shared) {
        self.database = database
    }

    public func getPresets() -> [LocationPreset] {
        database.getLocationPresets()
    }

    public func recordUsage(deck: Int, zone: ShipZone, placeName: String) {
        let trimmed = placeName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        if var existing = database.findLocationPreset(deck: deck, zone: zone, placeName: trimmed) {
            existing.useCount += 1
            existing.lastUsedEpochMs = nowMs
            database.saveLocationPreset(existing)
        } else {
            let newPreset = LocationPreset(
                id: 0,
                deck: deck,
                zone: zone,
                placeName: trimmed,
                lastUsedEpochMs: nowMs,
                useCount: 1
            )
            database.saveLocationPreset(newPreset)
        }
    }

    public func deletePreset(id: Int64) {
        database.deleteLocationPreset(id: id)
    }
}
