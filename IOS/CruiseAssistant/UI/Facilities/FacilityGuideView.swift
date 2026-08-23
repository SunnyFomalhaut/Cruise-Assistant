import SwiftUI
import Foundation

/// 设施指南主视图：包含搜索栏、分类与营业状态筛选横滑条、双列海报卡片流与详情浮层
public struct FacilityGuideView: View {
    @StateObject public var viewModel: FacilityGuideViewModel
    public let onBack: (() -> Void)?
    public let onOpenDeckMap: (() -> Void)?
    public let onNavigateToDeckMap: ((_ deck: Int, _ placeName: String) -> Void)?
    public let wishlistRepository: WishlistRepositoryProtocol?
    public let categoryRepository: CategoryRepositoryProtocol?

    public init(
        viewModel: FacilityGuideViewModel? = nil,
        onBack: (() -> Void)? = nil,
        onOpenDeckMap: (() -> Void)? = nil,
        onNavigateToDeckMap: ((_ deck: Int, _ placeName: String) -> Void)? = nil,
        wishlistRepository: WishlistRepositoryProtocol? = nil,
        categoryRepository: CategoryRepositoryProtocol? = nil
    ) {
        if let vm = viewModel {
            _viewModel = StateObject(wrappedValue: vm)
        } else {
            _viewModel = StateObject(wrappedValue: FacilityGuideViewModel())
        }
        self.onBack = onBack
        self.onOpenDeckMap = onOpenDeckMap
        self.onNavigateToDeckMap = onNavigateToDeckMap
        self.wishlistRepository = wishlistRepository
        self.categoryRepository = categoryRepository
    }

    @Environment(\.cruisePalette) private var palette

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 1. 顶部搜索与筛选控制面板
                topControlPanel
                    .background(Color.white)
                    .shadow(color: Color.black.opacity(0.04), radius: 3, x: 0, y: 2)
                    .zIndex(10)

                // 2. 双列海报卡片网格 / 空状态视图
                ZStack {
                    Slate50.ignoresSafeArea()

                    if viewModel.filteredFacilities.isEmpty {
                        emptyStateView
                    } else {
                        facilityGridView
                    }
                }
            }
            .navigationTitle("设施指南")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                if let onBack = onBack {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(action: onBack) {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 14, weight: .bold))
                                Text("返回")
                                    .font(.system(size: 15))
                            }
                            .foregroundColor(CruiseMarine)
                        }
                    }
                }

                if let onOpenDeckMap = onOpenDeckMap {
                    ToolbarItem(placement: .primaryAction) {
                        Button(action: onOpenDeckMap) {
                            HStack(spacing: 4) {
                                Image(systemName: "map.fill")
                                    .font(.system(size: 13))
                                Text("甲板地图")
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            .foregroundColor(CruiseMarine)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(CruiseAzureContainer)
                            .cornerRadius(6)
                        }
                    }
                }
            }
            .sheet(item: $viewModel.selectedFacility) { facility in
                FacilityDetailSheet(
                    facility: facility,
                    wishlistRepository: wishlistRepository,
                    categoryRepository: categoryRepository,
                    onNavigateToDeckMap: onNavigateToDeckMap,
                    onAddToWishlist: { item in
                        if let wishlistRepo = wishlistRepository {
                            viewModel.addToWishlist(
                                facility: item,
                                wishlistRepository: wishlistRepo,
                                categoryRepository: categoryRepository
                            )
                        }
                    }
                )
            }
        }
    }

    // MARK: - Subviews

    @ViewBuilder
    private var topControlPanel: some View {
        VStack(spacing: 8) {
            // 搜索框
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 15))
                    .foregroundColor(Slate400)

                TextField("搜索设施、餐厅、甲板(如 Deck 6)或区域...", text: $viewModel.searchQuery)
                    .font(.system(size: 14))
                    .foregroundColor(Slate900)
                    .autocorrectionDisabled(true)

                if !viewModel.searchQuery.isEmpty {
                    Button(action: {
                        viewModel.setSearchQuery("")
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 15))
                            .foregroundColor(Slate400)
                    }
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(Slate100)
            .cornerRadius(10)
            .padding(.horizontal, 16)
            .padding(.top, 8)

            // 横向滑动筛选栏：仅看营业中 + 分类 FilterChips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    // "仅看营业中" 切换芯片
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.15)) {
                            viewModel.toggleOnlyOpenNow()
                        }
                    }) {
                        HStack(spacing: 4) {
                            if viewModel.onlyOpenNow {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 10, weight: .bold))
                            }
                            Text("仅看营业中")
                                .font(.system(size: 12, weight: .semibold))
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .foregroundColor(viewModel.onlyOpenNow ? Color(hex: "#137333") : Slate700)
                        .background(viewModel.onlyOpenNow ? Color(hex: "#E6F4EA") : Slate100)
                        .cornerRadius(16)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(viewModel.onlyOpenNow ? Color(hex: "#137333").opacity(0.3) : Color.clear, lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())

                    // 各大分类选项
                    ForEach(viewModel.categories) { option in
                        let isSelected = viewModel.selectedCategory == option.key
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.15)) {
                                viewModel.selectCategory(option.key)
                            }
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: categoryIconSystemName(for: option.iconKey))
                                    .font(.system(size: 11, weight: .semibold))
                                Text(option.label)
                                    .font(.system(size: 12, weight: .semibold))
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .foregroundColor(isSelected ? palette.secondary : Slate700)
                            .background(isSelected ? palette.primaryContainer : Slate100)
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(isSelected ? palette.primary : Color.clear, lineWidth: 1)
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
        }
    }

    @ViewBuilder
    private var facilityGridView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                // 数量统计提示条
                HStack {
                    Text("共找到 \(viewModel.totalCount) 处设施地点")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(Slate500)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)

                // 双列自适应网格
                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: 10),
                        GridItem(.flexible(), spacing: 10)
                    ],
                    spacing: 12
                ) {
                    ForEach(viewModel.filteredFacilities) { facility in
                        FacilityCardView(
                            facility: facility,
                            onTap: {
                                viewModel.selectFacility(facility)
                            }
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
    }

    @ViewBuilder
    private var emptyStateView: some View {
        VStack(spacing: 14) {
            Image(systemName: "building.2.crop.circle")
                .font(.system(size: 56))
                .foregroundColor(Slate300)

            VStack(spacing: 4) {
                Text("未找到匹配的设施地点")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Slate800)

                Text("请尝试更换搜索关键字或重置分类与营业状态筛选")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Slate500)
                    .multilineTextAlignment(.center)
            }

            if viewModel.isFiltered {
                Button(action: {
                    withAnimation {
                        viewModel.clearFilters()
                    }
                }) {
                    Text("清空所有筛选")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(CruiseMarine)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(CruiseAzureContainer)
                        .cornerRadius(8)
                }
                .padding(.top, 6)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
