import XCTest
@testable import CruiseAssistant

final class FacilityGuideViewModelTests: XCTestCase {

    override func setUpWithError() throws {
        _ = LocationCatalog.shared
    }

    // MARK: - parseFacilityTitle Tests

    func testParseFacilityTitleDualLanguage() {
        let parts1 = parseFacilityTitle("寻路者湾 Wayfinder Bay")
        XCTAssertEqual(parts1.chinese, "寻路者湾")
        XCTAssertEqual(parts1.english, "Wayfinder Bay")

        let parts2 = parseFacilityTitle("华特迪士尼剧院 Walt Disney Theatre")
        XCTAssertEqual(parts2.chinese, "华特迪士尼剧院")
        XCTAssertEqual(parts2.english, "Walt Disney Theatre")

        let parts3 = parseFacilityTitle("米奇烟囱披萨 Mickey's Smokeyard BBQ / Pizza")
        XCTAssertEqual(parts3.chinese, "米奇烟囱披萨")
        XCTAssertEqual(parts3.english, "Mickey's Smokeyard BBQ / Pizza")

        let parts4 = parseFacilityTitle("童话洗衣房 Fairytale Fresh Laundry")
        XCTAssertEqual(parts4.chinese, "童话洗衣房")
        XCTAssertEqual(parts4.english, "Fairytale Fresh Laundry")
    }

    func testParseFacilityTitleSingleLanguageOrNumbers() {
        let parts1 = parseFacilityTitle("1923")
        XCTAssertEqual(parts1.chinese, "1923")
        XCTAssertNil(parts1.english)

        let parts2 = parseFacilityTitle("Palo Trattoria")
        XCTAssertEqual(parts2.chinese, "Palo Trattoria")
        XCTAssertNil(parts2.english)

        let parts3 = parseFacilityTitle("童话洗衣房")
        XCTAssertEqual(parts3.chinese, "童话洗衣房")
        XCTAssertNil(parts3.english)

        let empty = parseFacilityTitle("")
        XCTAssertEqual(empty.chinese, "")
        XCTAssertNil(empty.english)
    }

    // MARK: - Category Filtering Tests

    @MainActor
    func testCategoryFiltering() {
        let testFacilities: [FacilityItemUiModel] = [
            FacilityItemUiModel(
                placeName: "米奇烟囱披萨 Mickey's BBQ",
                categoryKey: "meal",
                categoryLabel: "餐食",
                deck: 11,
                zone: .stern,
                formattedLocation: "Deck 11 船尾"
            ),
            FacilityItemUiModel(
                placeName: "无边际泳池 Infinity Pool",
                categoryKey: "pool",
                categoryLabel: "泳池娱乐",
                deck: 12,
                zone: .bow,
                formattedLocation: "Deck 12 船头"
            ),
            FacilityItemUiModel(
                placeName: "迪士尼皇家摄影馆 Disney Royal Studio",
                categoryKey: "shop",
                categoryLabel: "商店",
                deck: 5,
                zone: .mid,
                formattedLocation: "Deck 5 船中"
            ),
            FacilityItemUiModel(
                placeName: "边缘俱乐部 Edge",
                categoryKey: "youth",
                categoryLabel: "青少年活动",
                deck: 13,
                zone: .stern,
                formattedLocation: "Deck 13 船尾"
            ),
            FacilityItemUiModel(
                placeName: "华特迪士尼剧院 Walt Disney Theatre",
                categoryKey: "show",
                categoryLabel: "演出场地",
                deck: 6,
                zone: .bow,
                formattedLocation: "Deck 6 船头"
            ),
            FacilityItemUiModel(
                placeName: "童话洗衣房 Fairytale Fresh Laundry",
                categoryKey: "chore",
                categoryLabel: "船上服务",
                deck: 16,
                zone: .mid,
                formattedLocation: "Deck 16 船中"
            )
        ]

        let viewModel = FacilityGuideViewModel(facilities: testFacilities)
        XCTAssertEqual(viewModel.totalCount, 6)

        // 1. 筛选餐食
        viewModel.selectCategory("meal")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.categoryKey, "meal")

        // 2. 筛选泳池
        viewModel.selectCategory("pool")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.categoryKey, "pool")

        // 3. 筛选商店
        viewModel.selectCategory("shop")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.categoryKey, "shop")

        // 4. 筛选青少年
        viewModel.selectCategory("youth")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.categoryKey, "youth")

        // 5. 筛选演出
        viewModel.selectCategory("show")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.categoryKey, "show")

        // 6. 筛选船上服务
        viewModel.selectCategory("chore")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.categoryKey, "chore")

        // 7. 切回全部
        viewModel.selectCategory("all")
        XCTAssertEqual(viewModel.totalCount, 6)
    }

    // MARK: - Search Query Filtering Tests

    @MainActor
    func testSearchQueryFiltering() {
        let testFacilities: [FacilityItemUiModel] = [
            FacilityItemUiModel(
                placeName: "米奇烟囱披萨 Mickey's BBQ",
                categoryKey: "meal",
                categoryLabel: "餐食",
                deck: 11,
                zone: .stern,
                formattedLocation: "Deck 11 船尾",
                diningGroupLabel: "快餐 / 简餐"
            ),
            FacilityItemUiModel(
                placeName: "1923 餐厅",
                categoryKey: "meal",
                categoryLabel: "餐食",
                deck: 6,
                zone: .mid,
                formattedLocation: "Deck 6 船中",
                diningGroupLabel: "轮换正餐餐厅"
            ),
            FacilityItemUiModel(
                placeName: "华特迪士尼剧院 Walt Disney Theatre",
                categoryKey: "show",
                categoryLabel: "演出场地",
                deck: 6,
                zone: .bow,
                formattedLocation: "Deck 6 船头"
            )
        ]

        let viewModel = FacilityGuideViewModel(facilities: testFacilities)

        // 1. 按名称搜索
        viewModel.setSearchQuery("披萨")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.placeName, "米奇烟囱披萨 Mickey's BBQ")

        // 2. 按英文名称搜索
        viewModel.setSearchQuery("theatre")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.placeName, "华特迪士尼剧院 Walt Disney Theatre")

        // 3. 按甲板搜索 "Deck 6"
        viewModel.setSearchQuery("deck 6")
        XCTAssertEqual(viewModel.filteredFacilities.count, 2)

        // 4. 按餐饮分组搜索
        viewModel.setSearchQuery("快餐")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.placeName, "米奇烟囱披萨 Mickey's BBQ")

        // 5. 按区域搜索
        viewModel.setSearchQuery("船尾")
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
    }

    // MARK: - Only Open Now Filtering Tests

    @MainActor
    func testOnlyOpenNowFiltering() {
        let testFacilities: [FacilityItemUiModel] = [
            FacilityItemUiModel(
                placeName: "24H 饮料站",
                categoryKey: "meal",
                categoryLabel: "餐食",
                deck: 11,
                zone: .mid,
                formattedLocation: "Deck 11 船中",
                is24Hours: true,
                isOpenNow: true,
                hasOperatingHours: true
            ),
            FacilityItemUiModel(
                placeName: "午餐餐厅",
                categoryKey: "meal",
                categoryLabel: "餐食",
                deck: 6,
                zone: .mid,
                formattedLocation: "Deck 6 船中",
                is24Hours: false,
                isOpenNow: false,
                hasOperatingHours: true
            )
        ]

        let viewModel = FacilityGuideViewModel(facilities: testFacilities)
        XCTAssertEqual(viewModel.totalCount, 2)

        viewModel.setOnlyOpenNow(true)
        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.placeName, "24H 饮料站")

        viewModel.toggleOnlyOpenNow()
        XCTAssertEqual(viewModel.filteredFacilities.count, 2)
    }

    // MARK: - Wishlist Action Tests

    @MainActor
    func testAddToWishlist() {
        let memoryDatabase = CruiseDatabase.inMemory()
        let wishlistRepo = WishlistRepository(database: memoryDatabase)

        let facility = FacilityItemUiModel(
            placeName: "寻路者湾 Wayfinder Bay",
            categoryKey: "pool",
            categoryLabel: "泳池娱乐",
            deck: 12,
            zone: .bow,
            formattedLocation: "Deck 12 船头"
        )

        let viewModel = FacilityGuideViewModel(facilities: [facility])

        let item = viewModel.addToWishlist(
            facility: facility,
            wishlistRepository: wishlistRepo,
            note: "想去体验无边泳池"
        )

        XCTAssertEqual(item.title, "寻路者湾 Wayfinder Bay")
        XCTAssertEqual(item.placeName, "寻路者湾 Wayfinder Bay")
        XCTAssertEqual(item.deck, 12)
        XCTAssertEqual(item.zone, .bow)
        XCTAssertEqual(item.note, "想去体验无边泳池")
        XCTAssertNotNil(viewModel.addedWishlistMessage)

        let savedItems = wishlistRepo.getItems()
        XCTAssertEqual(savedItems.count, 1)
        XCTAssertEqual(savedItems.first?.title, "寻路者湾 Wayfinder Bay")
    }

    // MARK: - Selection and Filter Reset Tests

    @MainActor
    func testSelectionAndClearFilters() {
        let facility = FacilityItemUiModel(
            placeName: "华特迪士尼剧院 Walt Disney Theatre",
            categoryKey: "show",
            categoryLabel: "演出场地",
            deck: 6,
            zone: .bow,
            formattedLocation: "Deck 6 船头"
        )

        let viewModel = FacilityGuideViewModel(facilities: [facility])

        // 1. 选中设施
        XCTAssertNil(viewModel.selectedFacility)
        XCTAssertFalse(viewModel.isDetailSheetPresented)

        viewModel.selectFacility(facility)
        XCTAssertEqual(viewModel.selectedFacility?.placeName, "华特迪士尼剧院 Walt Disney Theatre")
        XCTAssertTrue(viewModel.isDetailSheetPresented)

        viewModel.dismissDetailSheet()
        XCTAssertNil(viewModel.selectedFacility)
        XCTAssertFalse(viewModel.isDetailSheetPresented)

        // 2. 清空筛选
        viewModel.setSearchQuery("剧院")
        viewModel.selectCategory("show")
        viewModel.setOnlyOpenNow(true)
        XCTAssertTrue(viewModel.isFiltered)

        viewModel.clearFilters()
        XCTAssertEqual(viewModel.searchQuery, "")
        XCTAssertEqual(viewModel.selectedCategory, "all")
        XCTAssertFalse(viewModel.onlyOpenNow)
        XCTAssertFalse(viewModel.isFiltered)
    }

    // MARK: - Combined Multi-Filter Tests

    @MainActor
    func testCombinedMultiFilters() {
        let testFacilities: [FacilityItemUiModel] = [
            FacilityItemUiModel(
                placeName: "寻路者湾 Wayfinder Bay",
                categoryKey: "pool",
                categoryLabel: "泳池娱乐",
                deck: 12,
                zone: .bow,
                formattedLocation: "Deck 12 船头",
                is24Hours: false,
                isOpenNow: true,
                hasOperatingHours: true
            ),
            FacilityItemUiModel(
                placeName: "无边际泳池 Infinity Pool",
                categoryKey: "pool",
                categoryLabel: "泳池娱乐",
                deck: 12,
                zone: .bow,
                formattedLocation: "Deck 12 船头",
                is24Hours: false,
                isOpenNow: false,
                hasOperatingHours: true
            ),
            FacilityItemUiModel(
                placeName: "米奇烟囱披萨 Mickey's BBQ",
                categoryKey: "meal",
                categoryLabel: "餐食",
                deck: 11,
                zone: .stern,
                formattedLocation: "Deck 11 船尾",
                is24Hours: true,
                isOpenNow: true,
                hasOperatingHours: true
            )
        ]

        let viewModel = FacilityGuideViewModel(facilities: testFacilities)

        // 筛选: 泳池 + 营业中 + 搜索"寻路者"
        viewModel.selectCategory("pool")
        viewModel.setOnlyOpenNow(true)
        viewModel.setSearchQuery("寻路者")

        XCTAssertEqual(viewModel.filteredFacilities.count, 1)
        XCTAssertEqual(viewModel.filteredFacilities.first?.placeName, "寻路者湾 Wayfinder Bay")

        // 搜索不存在的关键词
        viewModel.setSearchQuery("披萨")
        XCTAssertEqual(viewModel.filteredFacilities.count, 0)
    }

    // MARK: - Default Initialization & RefreshTime Tests

    @MainActor
    func testDefaultInitializationAndRefreshTime() {
        let viewModel = FacilityGuideViewModel()
        XCTAssertFalse(viewModel.allFacilities.isEmpty)
        XCTAssertEqual(viewModel.selectedCategory, "all")
        XCTAssertEqual(viewModel.searchQuery, "")
        XCTAssertFalse(viewModel.onlyOpenNow)

        let countBefore = viewModel.totalCount
        viewModel.refreshTime(currentTime: SimpleTime(hour: 12, minute: 0))
        XCTAssertEqual(viewModel.totalCount, countBefore)
    }
}
