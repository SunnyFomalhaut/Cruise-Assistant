import Foundation
import SwiftUI
import Combine

/// 设施分类筛选选项
public struct FacilityCategoryFilterOption: Identifiable, Hashable, Sendable {
    public let key: String
    public let label: String
    public let iconKey: String

    public var id: String { key }

    public init(key: String, label: String, iconKey: String) {
        self.key = key
        self.label = label
        self.iconKey = iconKey
    }
}

/// 设施指南视图模型：管理地点分类、搜索、营业中筛选以及愿望单联动
@MainActor
public final class FacilityGuideViewModel: ObservableObject {

    // MARK: - Published State

    @Published public var searchQuery: String = ""
    @Published public var selectedCategory: String = "all"
    @Published public var onlyOpenNow: Bool = false
    @Published public var selectedFacility: FacilityItemUiModel? = nil
    @Published public var isDetailSheetPresented: Bool = false
    @Published public var addedWishlistMessage: String? = nil

    // MARK: - Filter Categories

    public let categories: [FacilityCategoryFilterOption] = [
        FacilityCategoryFilterOption(key: "all", label: "全部", iconKey: "sparkle"),
        FacilityCategoryFilterOption(key: "meal", label: "餐食", iconKey: "meal"),
        FacilityCategoryFilterOption(key: "pool", label: "泳池娱乐", iconKey: "pool"),
        FacilityCategoryFilterOption(key: "shop", label: "商店", iconKey: "shop"),
        FacilityCategoryFilterOption(key: "youth", label: "青少年活动", iconKey: "youth"),
        FacilityCategoryFilterOption(key: "show", label: "演出场地", iconKey: "show"),
        FacilityCategoryFilterOption(key: "chore", label: "船上服务", iconKey: "chore"),
    ]

    // MARK: - Data Storage

    public private(set) var allFacilities: [FacilityItemUiModel]

    // MARK: - Computed Properties

    /// 经过分类、搜索词、营业状态多维筛选后的设施列表
    public var filteredFacilities: [FacilityItemUiModel] {
        filterFacilities(
            facilities: allFacilities,
            query: searchQuery,
            categoryKey: selectedCategory,
            onlyOpenNow: onlyOpenNow
        )
    }

    /// 当前筛选结果总数
    public var totalCount: Int {
        filteredFacilities.count
    }

    /// 是否处于任何主动筛选状态
    public var isFiltered: Bool {
        !searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        selectedCategory != "all" ||
        onlyOpenNow
    }

    // MARK: - Initialization

    public init(
        currentTime: SimpleTime = .now(),
        facilities: [FacilityItemUiModel]? = nil
    ) {
        self.allFacilities = facilities ?? getAllFacilityItems(currentTime: currentTime)
    }

    // MARK: - User Intent Actions

    /// 更改搜索关键字
    public func setSearchQuery(_ query: String) {
        self.searchQuery = query
    }

    /// 切换选中分类
    public func selectCategory(_ categoryKey: String) {
        self.selectedCategory = categoryKey
    }

    /// 设置仅看营业中状态
    public func setOnlyOpenNow(_ onlyOpen: Bool) {
        self.onlyOpenNow = onlyOpen
    }

    /// 切换仅看营业中开关
    public func toggleOnlyOpenNow() {
        self.onlyOpenNow.toggle()
    }

    /// 选中设施以打开详情浮层
    public func selectFacility(_ facility: FacilityItemUiModel) {
        self.selectedFacility = facility
        self.isDetailSheetPresented = true
    }

    /// 关闭详情浮层
    public func dismissDetailSheet() {
        self.isDetailSheetPresented = false
        self.selectedFacility = nil
    }

    /// 清空所有搜索与筛选条件
    public func clearFilters() {
        self.searchQuery = ""
        self.selectedCategory = "all"
        self.onlyOpenNow = false
    }

    /// 按新时间刷新设施营业状态
    public func refreshTime(currentTime: SimpleTime = .now()) {
        self.allFacilities = getAllFacilityItems(currentTime: currentTime)
    }

    // MARK: - Wishlist Action

    /// 将指定设施加入愿望单
    @discardableResult
    public func addToWishlist(
        facility: FacilityItemUiModel,
        wishlistRepository: WishlistRepositoryProtocol,
        categoryRepository: CategoryRepositoryProtocol? = nil,
        note: String? = nil
    ) -> WishlistItem {
        let categoryId: Int64
        if let catRepo = categoryRepository {
            let cats = catRepo.getCategories()
            categoryId = cats.first(where: { $0.iconKey == facility.categoryKey })?.id ?? 1
        } else {
            categoryId = BUILT_IN_CATEGORIES.first(where: { $0.iconKey == facility.categoryKey })?.id ?? 1
        }

        let item = wishlistRepository.addItem(
            title: facility.placeName,
            categoryId: categoryId,
            deck: facility.primaryDeck ?? facility.deck,
            zone: facility.zone,
            placeName: facility.placeName,
            note: note
        )
        self.addedWishlistMessage = "已将 \(facility.placeName) 加入愿望单"
        return item
    }
}
