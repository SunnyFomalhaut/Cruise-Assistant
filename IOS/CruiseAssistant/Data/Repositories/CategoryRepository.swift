import Foundation

/// Interface for category data access and mutation.
public protocol CategoryRepositoryProtocol: Sendable {
    func getCategories() -> [Category]
    func getActiveCategories() -> [Category]
    func getCategory(byId id: Int64) -> Category?
    func addCategory(name: String, colorArgb: Int64, iconKey: String) -> Category
    func updateCategory(_ category: Category)
    func archiveCategory(id: Int64)
    func deleteCategory(id: Int64, fallbackCategoryId: Int64)
    func seedBuiltInCategories()
}

/// Repository managing event categories.
public final class CategoryRepository: CategoryRepositoryProtocol, @unchecked Sendable {
    private let database: CruiseDatabase

    public init(database: CruiseDatabase = .shared) {
        self.database = database
    }

    public func getCategories() -> [Category] {
        database.getCategories()
    }

    public func getActiveCategories() -> [Category] {
        database.getActiveCategories()
    }

    public func getCategory(byId id: Int64) -> Category? {
        database.getCategory(byId: id)
    }

    public func addCategory(name: String, colorArgb: Int64, iconKey: String) -> Category {
        let existing = database.getCategories()
        let maxSortOrder = existing.map(\.sortOrder).max() ?? -1
        let newCategory = Category(
            id: 0,
            name: name,
            colorArgb: colorArgb,
            iconKey: iconKey,
            parentId: nil,
            isBuiltIn: false,
            isArchived: false,
            sortOrder: maxSortOrder + 1
        )
        return database.saveCategory(newCategory)
    }

    public func updateCategory(_ category: Category) {
        database.saveCategory(category)
    }

    public func archiveCategory(id: Int64) {
        guard var category = database.getCategory(byId: id) else { return }
        category.isArchived = true
        database.saveCategory(category)
    }

    public func deleteCategory(id: Int64, fallbackCategoryId: Int64) {
        guard let category = database.getCategory(byId: id), !category.isBuiltIn else { return }
        database.reassignCategory(from: id, to: fallbackCategoryId)
        database.deleteCategory(id: id)
    }

    public func seedBuiltInCategories() {
        let existing = database.getCategories()
        if existing.isEmpty {
            for (index, builtIn) in BUILT_IN_CATEGORIES.enumerated() {
                var cat = builtIn
                cat.id = Int64(index + 1)
                database.saveCategory(cat)
            }
        } else {
            let existingKeys = Set(existing.map(\.iconKey))
            let missing = BUILT_IN_CATEGORIES.filter { !existingKeys.contains($0.iconKey) }
            for builtIn in missing {
                database.saveCategory(builtIn)
            }
        }
    }
}
