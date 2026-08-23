import Foundation
import SwiftUI
import Combine

/// View model managing wishlist items, categorization, filtering, and converting to timeline events.
@MainActor
public final class WishlistViewModel: ObservableObject {
    private let wishlistRepository: WishlistRepositoryProtocol
    private let categoryRepository: CategoryRepositoryProtocol
    private let eventRepository: EventRepositoryProtocol?
    private let voyageRepository: VoyageRepositoryProtocol?

    // MARK: - Published State

    @Published public var allItems: [WishlistItem] = []
    @Published public var items: [WishlistItem] = []
    @Published public var categories: [Category] = []
    @Published public var categoriesById: [Int64: Category] = [:]
    @Published public var selectedCategoryId: Int64? = nil
    @Published public var completedCount: Int = 0
    @Published public var totalCount: Int = 0
    @Published public var activeVoyage: Voyage? = nil

    // Sheet / Action States
    @Published public var isAddingItem: Bool = false
    @Published public var editingItem: WishlistItem? = nil
    @Published public var convertingItem: WishlistItem? = nil

    public init(
        wishlistRepository: WishlistRepositoryProtocol,
        categoryRepository: CategoryRepositoryProtocol,
        eventRepository: EventRepositoryProtocol? = nil,
        voyageRepository: VoyageRepositoryProtocol? = nil
    ) {
        self.wishlistRepository = wishlistRepository
        self.categoryRepository = categoryRepository
        self.eventRepository = eventRepository
        self.voyageRepository = voyageRepository
        loadData()
    }

    // MARK: - Data Loading & Filtering

    public func loadData() {
        let fetchedCategories = categoryRepository.getActiveCategories()
        self.categories = fetchedCategories
        self.categoriesById = Dictionary(uniqueKeysWithValues: fetchedCategories.map { ($0.id, $0) })

        if let voyageRepo = voyageRepository {
            self.activeVoyage = voyageRepo.getVoyage()
        }

        let all = wishlistRepository.getItems()
        self.allItems = all
        self.totalCount = all.count
        self.completedCount = all.filter(\.isCompleted).count

        applyFilter()
    }

    public func selectCategoryFilter(_ categoryId: Int64?) {
        self.selectedCategoryId = categoryId
        applyFilter()
    }

    private func applyFilter() {
        if let filterId = selectedCategoryId {
            self.items = allItems.filter { $0.categoryId == filterId }
        } else {
            self.items = allItems
        }
    }

    // MARK: - Wishlist Mutations

    @discardableResult
    public func addItem(
        title: String,
        categoryId: Int64,
        deck: Int? = nil,
        zone: ShipZone? = nil,
        placeName: String? = nil,
        note: String? = nil
    ) -> WishlistItem {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPlace = placeName?.trimmingCharacters(in: .whitespacesAndNewlines).takeIfNotEmpty()
        let trimmedNote = note?.trimmingCharacters(in: .whitespacesAndNewlines).takeIfNotEmpty()

        let created = wishlistRepository.addItem(
            title: trimmedTitle,
            categoryId: categoryId,
            deck: deck,
            zone: zone,
            placeName: trimmedPlace,
            note: trimmedNote
        )
        loadData()
        return created
    }

    public func updateItem(_ item: WishlistItem) {
        wishlistRepository.updateItem(item)
        loadData()
    }

    public func saveItem(
        id: Int64,
        title: String,
        categoryId: Int64,
        deck: Int? = nil,
        zone: ShipZone? = nil,
        placeName: String? = nil,
        note: String? = nil
    ) {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPlace = placeName?.trimmingCharacters(in: .whitespacesAndNewlines).takeIfNotEmpty()
        let trimmedNote = note?.trimmingCharacters(in: .whitespacesAndNewlines).takeIfNotEmpty()

        if id == 0 {
            addItem(
                title: trimmedTitle,
                categoryId: categoryId,
                deck: deck,
                zone: zone,
                placeName: trimmedPlace,
                note: trimmedNote
            )
        } else if var existing = wishlistRepository.getItem(byId: id) {
            existing.title = trimmedTitle
            existing.categoryId = categoryId
            existing.deck = deck
            existing.zone = zone
            existing.placeName = trimmedPlace
            existing.note = trimmedNote
            wishlistRepository.updateItem(existing)
            loadData()
        }
    }

    public func deleteItem(byId id: Int64) {
        wishlistRepository.deleteItem(id: id)
        loadData()
    }

    public func deleteItem(_ item: WishlistItem) {
        deleteItem(byId: item.id)
    }

    @discardableResult
    public func toggleCompleted(byId id: Int64) -> WishlistItem? {
        let updated = wishlistRepository.toggleCompleted(id: id)
        loadData()
        return updated
    }

    @discardableResult
    public func toggleCompleted(_ item: WishlistItem) -> WishlistItem? {
        toggleCompleted(byId: item.id)
    }

    // MARK: - Convert To Schedule Event

    @discardableResult
    public func convertToEvent(
        item: WishlistItem,
        date: SimpleDate,
        startMinute: Int,
        durationMinutes: Int,
        eventRepository customEventRepo: EventRepositoryProtocol? = nil
    ) -> CruiseEvent? {
        guard let eventRepo = customEventRepo ?? self.eventRepository else {
            return nil
        }

        let event = CruiseEvent(
            id: 0,
            date: date,
            startTime: SimpleTime(totalMinutes: startMinute),
            durationMinutes: max(15, durationMinutes),
            categoryId: item.categoryId,
            title: item.title,
            deck: item.deck,
            zone: item.zone,
            placeName: item.placeName,
            lane: 0,
            isSystemGenerated: false,
            isActive: true
        )

        let createdEvent = eventRepo.addEvent(event: event, recurrenceDays: [])

        // Mark wishlist item as completed
        if var updated = wishlistRepository.getItem(byId: item.id) {
            updated.isCompleted = true
            wishlistRepository.updateItem(updated)
        }

        loadData()
        return createdEvent
    }
}

// MARK: - Private Helpers

private extension String {
    func takeIfNotEmpty() -> String? {
        isEmpty ? nil : self
    }
}
