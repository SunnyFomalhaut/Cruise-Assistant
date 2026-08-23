import SwiftUI
import Foundation

/// 设施详情浮层面板：展示高清海报、多语言地点名称、营业时段、甲板联动与加入愿望单
public struct FacilityDetailSheet: View {
    public let facility: FacilityItemUiModel
    public let wishlistRepository: WishlistRepositoryProtocol?
    public let categoryRepository: CategoryRepositoryProtocol?
    public let onNavigateToDeckMap: ((_ deck: Int, _ placeName: String) -> Void)?
    public let onAddToWishlist: ((FacilityItemUiModel) -> Void)?

    @Environment(\.dismiss) private var dismiss
    @State private var isAddedToWishlist: Bool = false
    @State private var showWishlistToast: Bool = false

    public init(
        facility: FacilityItemUiModel,
        wishlistRepository: WishlistRepositoryProtocol? = nil,
        categoryRepository: CategoryRepositoryProtocol? = nil,
        onNavigateToDeckMap: ((_ deck: Int, _ placeName: String) -> Void)? = nil,
        onAddToWishlist: ((FacilityItemUiModel) -> Void)? = nil
    ) {
        self.facility = facility
        self.wishlistRepository = wishlistRepository
        self.categoryRepository = categoryRepository
        self.onNavigateToDeckMap = onNavigateToDeckMap
        self.onAddToWishlist = onAddToWishlist
    }

    private var titleParts: FacilityTitleParts {
        parseFacilityTitle(facility.placeName)
    }

    private var posterImage: FacilityPlatformImage? {
        loadFacilityPosterImage(for: facility.placeName)
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // 1. 高清海报头部
                    posterHeaderView

                    // 2. 标题与分类、位置元数据
                    titleAndMetadataSection
                        .padding(.horizontal, 20)

                    Divider()
                        .padding(.horizontal, 20)

                    // 3. 营业时段详情
                    operatingHoursSection
                        .padding(.horizontal, 20)

                    Divider()
                        .padding(.horizontal, 20)

                    // 4. 底部操作按钮组 (跳转甲板地图 & 加入愿望单)
                    actionButtonsSection
                        .padding(.horizontal, 20)
                        .padding(.bottom, 24)
                }
            }
            .background(Slate50.ignoresSafeArea())
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 20))
                            .foregroundColor(Slate400)
                    }
                }
            }
        }
    }

    // MARK: - Subviews

    @ViewBuilder
    private var posterHeaderView: some View {
        ZStack(alignment: .bottomLeading) {
            if let image = posterImage {
                Image(facilityPlatformImage: image)
                    .resizable()
                    .aspectRatio(16 / 9, contentMode: .fill)
                    .frame(maxWidth: .infinity, maxHeight: 220)
                    .clipped()
            } else {
                ZStack {
                    LinearGradient(
                        colors: [
                            Color(argb: categoryColorArgb(for: facility.categoryKey)).opacity(0.4),
                            Color(argb: categoryColorArgb(for: facility.categoryKey)).opacity(0.15)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    VStack(spacing: 8) {
                        Image(systemName: categoryIconSystemName(for: facility.categoryKey))
                            .font(.system(size: 48))
                            .foregroundColor(Color(argb: categoryColorArgb(for: facility.categoryKey)))
                        Text(facility.categoryLabel)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Slate700)
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 180, maxHeight: 220)
            }

            // 状态徽标 (浮层左下角)
            statusBadge
                .padding(14)
        }
    }

    @ViewBuilder
    private var titleAndMetadataSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            // 中英文主副标题
            VStack(alignment: .leading, spacing: 4) {
                Text(titleParts.chinese)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(Slate900)

                if let english = titleParts.english {
                    Text(english)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate500)
                }
            }

            // 分类标签 & 餐饮分组
            HStack(spacing: 8) {
                // 类别徽标
                HStack(spacing: 4) {
                    Image(systemName: categoryIconSystemName(for: facility.categoryKey))
                        .font(.system(size: 11, weight: .semibold))
                    Text(facility.categoryLabel)
                        .font(.system(size: 12, weight: .semibold))
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .foregroundColor(cardTextColorFor(colorArgb: categoryColorArgb(for: facility.categoryKey)))
                .background(cardBackgroundFor(colorArgb: categoryColorArgb(for: facility.categoryKey)))
                .cornerRadius(6)

                // 餐饮分组
                if let dining = facility.diningGroupLabel {
                    Text(dining)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(CruiseMarine)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(CruiseAzureContainer)
                        .cornerRadius(6)
                }

                Spacer()
            }

            // 详细位置与甲板
            HStack(spacing: 6) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(CruiseAzure)

                Text(facility.formattedLocation)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(CruiseMarine)

                if let zone = facility.zone {
                    Text("(\(zone.label) \(zone.rawValue))")
                        .font(.system(size: 12, weight: .regular))
                        .foregroundColor(Slate500)
                }
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(CruiseAzureContainer.opacity(0.5))
            .cornerRadius(8)
        }
    }

    @ViewBuilder
    private var operatingHoursSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: "clock.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Slate700)
                Text("营业与运营时间")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Slate900)
            }

            if facility.is24Hours {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 16))
                        .foregroundColor(CruiseAzure)
                    Text("24 小时全天候开放服务")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate800)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(CruiseAzureContainer.opacity(0.6))
                .cornerRadius(8)
            } else if !facility.slots.isEmpty {
                VStack(spacing: 8) {
                    ForEach(Array(facility.slots.enumerated()), id: \.offset) { _, slot in
                        HStack(spacing: 10) {
                            // 餐次/时段标签
                            if let meal = slot.meal {
                                Text(meal.label)
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(cardTextColorFor(colorArgb: 0xFF34A853))
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 3)
                                    .background(cardBackgroundFor(colorArgb: 0xFF34A853))
                                    .cornerRadius(5)
                                    .frame(minWidth: 44)
                            } else {
                                Text("开放")
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundColor(Slate600)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 3)
                                    .background(Slate100)
                                    .cornerRadius(5)
                                    .frame(minWidth: 44)
                            }

                            // 时间区间
                            Text(String(format: "%02d:%02d – %02d:%02d", slot.start.hour, slot.start.minute, slot.end.hour, slot.end.minute))
                                .font(.system(size: 14, weight: .semibold, design: .monospaced))
                                .foregroundColor(Slate800)

                            if slot.lateNight {
                                Text("夜宵")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(Color(argb: 0xFF9B5DE5))
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(Color(argb: 0xFF9B5DE5).opacity(0.15))
                                    .cornerRadius(4)
                            }

                            Spacer()
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(SurfaceCard)
                        .cornerRadius(8)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Slate200, lineWidth: 0.8)
                        )
                    }
                }
            } else {
                Text("营业时间请以当日船上导航指南 (Navigator) 或现场通知为准。")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Slate500)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Slate100)
                    .cornerRadius(8)
            }
        }
    }

    @ViewBuilder
    private var actionButtonsSection: some View {
        VStack(spacing: 10) {
            // 按钮 1: 在甲板地图中查看
            Button(action: {
                let targetDeck = facility.primaryDeck ?? facility.deck ?? 10
                onNavigateToDeckMap?(targetDeck, facility.placeName)
                dismiss()
            }) {
                HStack(spacing: 8) {
                    Text("🗺️")
                        .font(.system(size: 16))
                    Text("在甲板地图中查看")
                        .font(.system(size: 15, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .background(
                    LinearGradient(
                        colors: [CruiseAzure, CruiseMarine],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .cornerRadius(10)
                .shadow(color: CruiseAzure.opacity(0.25), radius: 4, x: 0, y: 2)
            }

            // 按钮 2: 加入愿望单
            Button(action: {
                handleAddToWishlist()
            }) {
                HStack(spacing: 8) {
                    Text(isAddedToWishlist ? "✓" : "⭐")
                        .font(.system(size: 16))
                    Text(isAddedToWishlist ? "已加入愿望单" : "加入愿望单")
                        .font(.system(size: 15, weight: .bold))
                }
                .foregroundColor(isAddedToWishlist ? Color(hex: "#137333") : Slate800)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .background(isAddedToWishlist ? Color(hex: "#E6F4EA") : SurfaceCard)
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(isAddedToWishlist ? Color(hex: "#137333").opacity(0.3) : Slate300, lineWidth: 1)
                )
            }
            .disabled(isAddedToWishlist)

            if showWishlistToast {
                Text("✓ 已成功将「\(facility.placeName)」添加至愿望单")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Color(hex: "#137333"))
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
    }

    @ViewBuilder
    private var statusBadge: some View {
        if facility.is24Hours {
            Text("24小时")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(CruiseMarine)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(CruiseAzureContainer)
                .cornerRadius(8)
                .shadow(color: Color.black.opacity(0.12), radius: 3, x: 0, y: 1)
        } else if facility.isOpenNow {
            Text("营业中")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(Color(hex: "#137333"))
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color(hex: "#E6F4EA"))
                .cornerRadius(8)
                .shadow(color: Color.black.opacity(0.12), radius: 3, x: 0, y: 1)
        } else {
            Text("休息中")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(Slate500)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Slate100)
                .cornerRadius(8)
                .shadow(color: Color.black.opacity(0.12), radius: 3, x: 0, y: 1)
        }
    }

    // MARK: - Actions

    private func handleAddToWishlist() {
        if let wishlistRepo = wishlistRepository {
            let categoryId: Int64
            if let catRepo = categoryRepository {
                let cats = catRepo.getCategories()
                categoryId = cats.first(where: { $0.iconKey == facility.categoryKey })?.id ?? 1
            } else {
                categoryId = BUILT_IN_CATEGORIES.first(where: { $0.iconKey == facility.categoryKey })?.id ?? 1
            }

            _ = wishlistRepo.addItem(
                title: facility.placeName,
                categoryId: categoryId,
                deck: facility.primaryDeck ?? facility.deck,
                zone: facility.zone,
                placeName: facility.placeName,
                note: nil
            )
        }

        onAddToWishlist?(facility)

        withAnimation {
            isAddedToWishlist = true
            showWishlistToast = true
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
            withAnimation {
                showWishlistToast = false
            }
        }
    }
}
