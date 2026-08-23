import XCTest
@testable import CruiseAssistant

@MainActor
final class WishlistViewModelTests: XCTestCase {
    private var database: CruiseDatabase!
    private var wishlistRepo: WishlistRepository!
    private var categoryRepo: CategoryRepository!
    private var eventRepo: EventRepository!
    private var voyageRepo: VoyageRepository!
    private var viewModel: WishlistViewModel!

    override func setUp() {
        super.setUp()
        database = CruiseDatabase.inMemory()
        wishlistRepo = WishlistRepository(database: database)
        categoryRepo = CategoryRepository(database: database)
        eventRepo = EventRepository(database: database)
        voyageRepo = VoyageRepository(database: database, categoryRepository: categoryRepo)

        // Seed default categories
        categoryRepo.seedBuiltInCategories()

        // Create a test voyage
        _ = voyageRepo.createVoyage(
            shipName: "迪士尼探险号",
            boardDate: SimpleDate(year: 2026, month: 8, day: 24),
            boardTime: SimpleTime(hour: 12, minute: 0),
            disembarkDate: SimpleDate(year: 2026, month: 8, day: 28),
            disembarkTime: SimpleTime(hour: 8, minute: 0),
            maxDeck: 20,
            minDeck: 1,
            dinnerTime: SimpleTime(hour: 17, minute: 45)
        )

        viewModel = WishlistViewModel(
            wishlistRepository: wishlistRepo,
            categoryRepository: categoryRepo,
            eventRepository: eventRepo,
            voyageRepository: voyageRepo
        )
    }

    override func tearDown() {
        viewModel = nil
        wishlistRepo = nil
        categoryRepo = nil
        eventRepo = nil
        voyageRepo = nil
        database = nil
        super.tearDown()
    }

    // MARK: - Add & Retrieve Items

    func testAddItemUpdatesListAndCounts() {
        XCTAssertEqual(viewModel.allItems.count, 0)
        XCTAssertEqual(viewModel.totalCount, 0)
        XCTAssertEqual(viewModel.completedCount, 0)

        let item = viewModel.addItem(
            title: "看狮子王音乐剧",
            categoryId: 1,
            deck: 4,
            zone: .bow,
            placeName: "沃尔特剧院",
            note: "记得提前 20 分钟入场"
        )

        XCTAssertEqual(item.title, "看狮子王音乐剧")
        XCTAssertEqual(item.deck, 4)
        XCTAssertEqual(item.zone, .bow)
        XCTAssertEqual(item.placeName, "沃尔特剧院")
        XCTAssertFalse(item.isCompleted)

        XCTAssertEqual(viewModel.allItems.count, 1)
        XCTAssertEqual(viewModel.items.count, 1)
        XCTAssertEqual(viewModel.totalCount, 1)
        XCTAssertEqual(viewModel.completedCount, 0)
    }

    // MARK: - Edit Item

    func testSaveExistingItem() {
        let created = viewModel.addItem(
            title: "品尝米奇冰淇淋",
            categoryId: 2,
            deck: 9,
            placeName: "冰淇淋吧"
        )

        viewModel.saveItem(
            id: created.id,
            title: "品尝米奇特色华夫饼",
            categoryId: 2,
            deck: 9,
            zone: .mid,
            placeName: "甜品屋",
            note: "早晨供应"
        )

        XCTAssertEqual(viewModel.allItems.count, 1)
        let updated = viewModel.allItems.first!
        XCTAssertEqual(updated.title, "品尝米奇特色华夫饼")
        XCTAssertEqual(updated.placeName, "甜品屋")
        XCTAssertEqual(updated.note, "早晨供应")
    }

    // MARK: - Toggle Completed

    func testToggleCompletedStatus() {
        let item1 = viewModel.addItem(title: "体验水上滑梯", categoryId: 1)
        _ = viewModel.addItem(title: "购买达菲公仔", categoryId: 3)

        XCTAssertEqual(viewModel.completedCount, 0)
        XCTAssertEqual(viewModel.totalCount, 2)

        // Toggle item1 to completed
        viewModel.toggleCompleted(item1)

        XCTAssertEqual(viewModel.completedCount, 1)
        let updated1 = viewModel.allItems.first { $0.id == item1.id }!
        XCTAssertTrue(updated1.isCompleted)

        // Toggle item1 back to incomplete
        viewModel.toggleCompleted(item1)
        XCTAssertEqual(viewModel.completedCount, 0)
        let toggledBack = viewModel.allItems.first { $0.id == item1.id }!
        XCTAssertFalse(toggledBack.isCompleted)
    }

    // MARK: - Delete Item

    func testDeleteItem() {
        let item1 = viewModel.addItem(title: "项目 A", categoryId: 1)
        let item2 = viewModel.addItem(title: "项目 B", categoryId: 1)
        XCTAssertEqual(viewModel.allItems.count, 2)

        viewModel.deleteItem(item1)

        XCTAssertEqual(viewModel.allItems.count, 1)
        XCTAssertEqual(viewModel.allItems.first?.id, item2.id)
        XCTAssertEqual(viewModel.totalCount, 1)
    }

    // MARK: - Category Filtering

    func testCategoryFilter() {
        let cat1 = viewModel.categories.first?.id ?? 1
        let cat2 = viewModel.categories.dropFirst().first?.id ?? 2

        _ = viewModel.addItem(title: "演出 1", categoryId: cat1)
        _ = viewModel.addItem(title: "演出 2", categoryId: cat1)
        _ = viewModel.addItem(title: "餐厅 1", categoryId: cat2)

        XCTAssertEqual(viewModel.allItems.count, 3)
        XCTAssertEqual(viewModel.items.count, 3)

        // Filter by cat1
        viewModel.selectCategoryFilter(cat1)
        XCTAssertEqual(viewModel.items.count, 2)
        XCTAssertTrue(viewModel.items.allSatisfy { $0.categoryId == cat1 })

        // Filter by cat2
        viewModel.selectCategoryFilter(cat2)
        XCTAssertEqual(viewModel.items.count, 1)
        XCTAssertEqual(viewModel.items.first?.categoryId, cat2)

        // Clear filter
        viewModel.selectCategoryFilter(nil)
        XCTAssertEqual(viewModel.items.count, 3)
    }

    // MARK: - Convert To Schedule Event

    func testConvertToScheduleEvent() {
        let item = viewModel.addItem(
            title: "漫威英雄见面会",
            categoryId: 1,
            deck: 5,
            zone: .mid,
            placeName: "中庭舞台",
            note: "带好签名本"
        )

        let targetDate = SimpleDate(year: 2026, month: 8, day: 25)
        let startMinute = 14 * 60 + 30 // 14:30
        let duration = 45

        let event = viewModel.convertToEvent(
            item: item,
            date: targetDate,
            startMinute: startMinute,
            durationMinutes: duration
        )

        XCTAssertNotNil(event)
        XCTAssertEqual(event?.title, "漫威英雄见面会")
        XCTAssertEqual(event?.date, targetDate)
        XCTAssertEqual(event?.startTime, SimpleTime(hour: 14, minute: 30))
        XCTAssertEqual(event?.durationMinutes, 45)
        XCTAssertEqual(event?.deck, 5)
        XCTAssertEqual(event?.zone, .mid)
        XCTAssertEqual(event?.placeName, "中庭舞台")

        // Verify that in eventRepository the event exists
        let dayEvents = eventRepo.getEvents(for: targetDate)
        XCTAssertTrue(dayEvents.contains { $0.title == "漫威英雄见面会" })

        // Verify wishlist item is marked as completed
        let updatedItem = wishlistRepo.getItem(byId: item.id)
        XCTAssertTrue(updatedItem?.isCompleted == true)
        XCTAssertEqual(viewModel.completedCount, 1)
    }
}
