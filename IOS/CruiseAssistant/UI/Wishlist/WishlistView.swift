import SwiftUI

/// Main Wishlist / Checklist screen displaying user wish items, category filters, completion progress, and quick conversion to timeline events.
public struct WishlistView: View {
    @ObservedObject public var viewModel: WishlistViewModel
    public var onNavigateToDeckMap: ((Int, String) -> Void)?

    @Environment(\.cruisePalette) private var envPalette

    // Local sheet states for Add/Edit and Convert
    @State private var showingEditSheet: Bool = false
    @State private var showingConvertSheet: Bool = false

    public init(
        viewModel: WishlistViewModel,
        onNavigateToDeckMap: ((Int, String) -> Void)? = nil
    ) {
        self.viewModel = viewModel
        self.onNavigateToDeckMap = onNavigateToDeckMap
    }

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                categoryFilterRail
                    .padding(.vertical, 8)
                    .background(Slate50)

                progressBanner
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)

                if viewModel.items.isEmpty {
                    emptyStateView
                } else {
                    itemList
                }
            }

            floatingAddButton
        }
        .background(Slate50.ignoresSafeArea())
        .sheet(isPresented: $showingEditSheet) {
            editSheetContent
        }
        .sheet(isPresented: $showingConvertSheet) {
            convertSheetContent
        }
        .onAppear {
            viewModel.loadData()
        }
    }

    // MARK: - Subviews

    @ViewBuilder
    private var itemList: some View {
        List {
            ForEach(viewModel.items) { item in
                itemRow(for: item)
            }
        }
        .listStyle(.plain)
        .refreshable {
            viewModel.loadData()
        }
    }

    @ViewBuilder
    private func itemRow(for item: WishlistItem) -> some View {
        let category = viewModel.categories.first { $0.id == item.categoryId }

        WishlistCardView(
            item: item,
            category: category,
            onToggleCompleted: {
                viewModel.toggleCompleted(item)
            },
            onConvertToSchedule: {
                viewModel.convertingItem = item
                showingConvertSheet = true
            },
            onTapCard: {
                viewModel.editingItem = item
                showingEditSheet = true
            },
            onNavigateToDeckMap: onNavigateToDeckMap
        )
        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                viewModel.deleteItem(item)
            } label: {
                Label("删除", systemImage: "trash")
            }
        }
    }

    @ViewBuilder
    private var floatingAddButton: some View {
        Button {
            viewModel.editingItem = nil
            showingEditSheet = true
        } label: {
            Image(systemName: "plus")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(envPalette.primary)
                .clipShape(Circle())
                .shadow(color: envPalette.primary.opacity(0.4), radius: 8, x: 0, y: 4)
        }
        .padding(.trailing, 20)
        .padding(.bottom, 20)
    }

    @ViewBuilder
    private var editSheetContent: some View {
        WishlistEditModalSheet(
            viewModel: viewModel,
            editingItem: viewModel.editingItem,
            onDismiss: {
                showingEditSheet = false
                viewModel.editingItem = nil
            }
        )
    }

    @ViewBuilder
    private var convertSheetContent: some View {
        if let item = viewModel.convertingItem {
            WishlistConvertModalSheet(
                viewModel: viewModel,
                item: item,
                onConverted: {
                    showingConvertSheet = false
                    viewModel.convertingItem = nil
                },
                onDismiss: {
                    showingConvertSheet = false
                    viewModel.convertingItem = nil
                }
            )
        } else {
            EmptyView()
        }
    }

    // MARK: - Category Filter Rail

    @ViewBuilder
    private var categoryFilterRail: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // "全部" Chip
                Button {
                    viewModel.selectCategoryFilter(nil)
                } label: {
                    HStack(spacing: 4) {
                        Text("全部")
                            .font(.system(size: 13, weight: .semibold))
                        Text("\(viewModel.totalCount)")
                            .font(.system(size: 11, weight: .bold))
                            .opacity(0.85)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(viewModel.selectedCategoryId == nil ? envPalette.primary : Color.white)
                    )
                    .foregroundColor(viewModel.selectedCategoryId == nil ? .white : Slate700)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(viewModel.selectedCategoryId == nil ? envPalette.primary : Slate200, lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)

                // Category Chips
                ForEach(viewModel.categories) { cat in
                    let isSelected = viewModel.selectedCategoryId == cat.id
                    let catColor = Color(argb: cat.colorArgb)

                    Button {
                        viewModel.selectCategoryFilter(isSelected ? nil : cat.id)
                    } label: {
                        HStack(spacing: 5) {
                            Circle()
                                .fill(isSelected ? Color.white : catColor)
                                .frame(width: 8, height: 8)

                            Text(cat.name)
                                .font(.system(size: 13, weight: .semibold))
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(isSelected ? catColor : Color.white)
                        )
                        .foregroundColor(isSelected ? .white : Slate700)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(isSelected ? catColor : Slate200, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Progress Banner

    @ViewBuilder
    private var progressBanner: some View {
        VStack(spacing: 6) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "checklist")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(envPalette.primary)
                    Text("心愿打卡进度")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Slate800)
                }

                Spacer()

                Text("已完成 \(viewModel.completedCount) / \(viewModel.totalCount) 项")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(viewModel.totalCount > 0 && viewModel.completedCount == viewModel.totalCount ? Color.green : Slate600)
            }

            // Progress Bar
            GeometryReader { geometry in
                let progress = viewModel.totalCount > 0
                    ? CGFloat(viewModel.completedCount) / CGFloat(viewModel.totalCount)
                    : 0.0

                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Slate200)
                        .frame(height: 6)

                    RoundedRectangle(cornerRadius: 4)
                        .fill(
                            LinearGradient(
                                colors: [envPalette.primary, envPalette.secondary],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: max(0, min(geometry.size.width, geometry.size.width * progress)), height: 6)
                }
            }
            .frame(height: 6)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Slate200, lineWidth: 1)
                )
        )
    }

    // MARK: - Empty State View

    @ViewBuilder
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Spacer()

            Image(systemName: "sparkles.rectangle.stack")
                .font(.system(size: 48))
                .foregroundColor(envPalette.primary.opacity(0.6))

            Text(viewModel.selectedCategoryId == nil ? "暂无心愿项目" : "该分类下暂无心愿")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Slate800)

            Text("点击右下方 “+” 按钮，添加您想在邮轮上体验的演出、活动、美食或打卡点。")
                .font(.system(size: 13))
                .foregroundColor(Slate500)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Wishlist Edit Modal Sheet

private struct WishlistEditModalSheet: View {
    @ObservedObject var viewModel: WishlistViewModel
    let editingItem: WishlistItem?
    let onDismiss: () -> Void

    @Environment(\.cruisePalette) private var envPalette

    @State private var title: String = ""
    @State private var selectedCategoryId: Int64 = 1
    @State private var selectedDeck: Int? = nil
    @State private var selectedZone: ShipZone? = nil
    @State private var placeName: String = ""
    @State private var note: String = ""

    init(
        viewModel: WishlistViewModel,
        editingItem: WishlistItem?,
        onDismiss: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.editingItem = editingItem
        self.onDismiss = onDismiss

        if let item = editingItem {
            _title = State(initialValue: item.title)
            _selectedCategoryId = State(initialValue: item.categoryId)
            _selectedDeck = State(initialValue: item.deck)
            _selectedZone = State(initialValue: item.zone)
            _placeName = State(initialValue: item.placeName ?? "")
            _note = State(initialValue: item.note ?? "")
        } else {
            let firstCatId = viewModel.categories.first?.id ?? 1
            _title = State(initialValue: "")
            _selectedCategoryId = State(initialValue: firstCatId)
            _selectedDeck = State(initialValue: nil)
            _selectedZone = State(initialValue: nil)
            _placeName = State(initialValue: "")
            _note = State(initialValue: "")
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("项目信息") {
                    TextField("项目名称 (如: 去剧院看狮子王)", text: $title)

                    Picker("所属类别", selection: $selectedCategoryId) {
                        ForEach(viewModel.categories) { cat in
                            Text(cat.name).tag(cat.id)
                        }
                    }
                }

                Section("位置信息 (选填)") {
                    Picker("甲板楼层", selection: $selectedDeck) {
                        Text("未选择").tag(nil as Int?)
                        ForEach(5...19, id: \.self) { deck in
                            Text("Deck \(deck)").tag(deck as Int?)
                        }
                    }

                    Picker("船体区域", selection: $selectedZone) {
                        Text("未选择").tag(nil as ShipZone?)
                        ForEach(ShipZone.allCases, id: \.self) { zone in
                            Text(zone.label).tag(zone as ShipZone?)
                        }
                    }

                    TextField("地点详情 (如: 华特迪士尼剧院)", text: $placeName)
                }

                Section("备注 (选填)") {
                    TextField("备注说明...", text: $note, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle(editingItem == nil ? "添加心愿" : "编辑心愿")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消", action: onDismiss)
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        if editingItem == nil {
                            viewModel.addItem(
                                title: title,
                                categoryId: selectedCategoryId,
                                deck: selectedDeck,
                                zone: selectedZone,
                                placeName: placeName.isEmpty ? nil : placeName,
                                note: note.isEmpty ? nil : note
                            )
                        } else if let item = editingItem {
                            let updated = WishlistItem(
                                id: item.id,
                                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                                categoryId: selectedCategoryId,
                                deck: selectedDeck,
                                zone: selectedZone,
                                placeName: placeName.isEmpty ? nil : placeName,
                                note: note.isEmpty ? nil : note,
                                isCompleted: item.isCompleted,
                                createdAt: item.createdAt
                            )
                            viewModel.updateItem(updated)
                        }
                        onDismiss()
                    }
                    .fontWeight(.semibold)
                    .foregroundColor(envPalette.primary)
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

// MARK: - Wishlist Convert Modal Sheet

private struct WishlistConvertModalSheet: View {
    @ObservedObject var viewModel: WishlistViewModel
    let item: WishlistItem
    let onConverted: () -> Void
    let onDismiss: () -> Void

    @Environment(\.cruisePalette) private var envPalette

    @State private var selectedDate: SimpleDate
    @State private var startMinute: Int = 10 * 60 // 10:00
    @State private var durationMinutes: Int = 45

    init(
        viewModel: WishlistViewModel,
        item: WishlistItem,
        onConverted: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.item = item
        self.onConverted = onConverted
        self.onDismiss = onDismiss

        let initialDate = viewModel.activeVoyage?.datesInVoyage().first ?? SimpleDate(year: 2026, month: 9, day: 3)
        _selectedDate = State(initialValue: initialDate)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("心愿详情") {
                    HStack {
                        Text("项目")
                        Spacer()
                        Text(item.title)
                            .font(.headline)
                            .foregroundColor(Slate900)
                    }

                    if let loc = formatLocation(deck: item.deck, zone: item.zone, placeName: item.placeName) {
                        HStack {
                            Text("地点")
                            Spacer()
                            Text(loc)
                                .foregroundColor(Slate600)
                        }
                    }
                }

                Section("计划排入日程") {
                    let dates = viewModel.activeVoyage?.datesInVoyage() ?? [SimpleDate(year: 2026, month: 9, day: 3)]
                    Picker("安排日期", selection: $selectedDate) {
                        ForEach(dates, id: \.self) { date in
                            Text(date.description).tag(date)
                        }
                    }

                    HStack {
                        Text("开始时间")
                        Spacer()
                        Text(SimpleTime(totalMinutes: startMinute).description)
                            .fontWeight(.bold)
                            .foregroundColor(envPalette.primary)
                    }

                    HStack {
                        Text("活动时长")
                        Spacer()
                        Text("\(durationMinutes) 分钟")
                            .fontWeight(.semibold)
                    }

                    Stepper(value: $durationMinutes, in: 15...180, step: 15) {
                        Text("调整时长: \(durationMinutes) 分钟")
                    }
                }
            }
            .navigationTitle("转为日程活动")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消", action: onDismiss)
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("确认加入") {
                        viewModel.convertToEvent(
                            item: item,
                            date: selectedDate,
                            startMinute: startMinute,
                            durationMinutes: durationMinutes
                        )
                        onConverted()
                    }
                    .fontWeight(.semibold)
                    .foregroundColor(envPalette.primary)
                }
            }
        }
    }
}
