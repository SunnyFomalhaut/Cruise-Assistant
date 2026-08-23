import Foundation

/// Interface for wishlist items queries and mutations.
public protocol WishlistRepositoryProtocol: Sendable {
    func getItems() -> [WishlistItem]
    func getItem(byId id: Int64) -> WishlistItem?
    func addItem(
        title: String,
        categoryId: Int64,
        deck: Int?,
        zone: ShipZone?,
        placeName: String?,
        note: String?
    ) -> WishlistItem
    func updateItem(_ item: WishlistItem)
    func deleteItem(id: Int64)
    @discardableResult
    func toggleCompleted(id: Int64) -> WishlistItem?
}

/// Repository managing user wishlist items and checklist status.
public final class WishlistRepository: WishlistRepositoryProtocol, @unchecked Sendable {
    private let database: CruiseDatabase

    public init(database: CruiseDatabase = .shared) {
        self.database = database
    }

    public func getItems() -> [WishlistItem] {
        database.getWishlistItems()
    }

    public func getItem(byId id: Int64) -> WishlistItem? {
        database.getWishlistItem(byId: id)
    }

    public func addItem(
        title: String,
        categoryId: Int64,
        deck: Int? = nil,
        zone: ShipZone? = nil,
        placeName: String? = nil,
        note: String? = nil
    ) -> WishlistItem {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let item = WishlistItem(
            id: 0,
            title: title,
            categoryId: categoryId,
            deck: deck,
            zone: zone,
            placeName: placeName,
            note: note,
            isCompleted: false,
            createdAt: nowMs
        )
        return database.saveWishlistItem(item)
    }

    public func updateItem(_ item: WishlistItem) {
        database.saveWishlistItem(item)
    }

    public func deleteItem(id: Int64) {
        database.deleteWishlistItem(id: id)
    }

    @discardableResult
    public func toggleCompleted(id: Int64) -> WishlistItem? {
        guard var item = database.getWishlistItem(byId: id) else { return nil }
        item.isCompleted.toggle()
        return database.saveWishlistItem(item)
    }
}
