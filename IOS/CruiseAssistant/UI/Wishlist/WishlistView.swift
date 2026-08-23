import SwiftUI

/// Main Wishlist / Checklist screen displaying user wish items, category filters, completion progress, and quick conversion to timeline events.
public struct WishlistView: View {
    @ObservedObject public var viewModel: WishlistViewModel
    public var onNavigateToDeckMap: ((Int, String) -> Void)?

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
        WishlistCardView(
            item: item,
            category: viewModel.categoriesById[item.categoryId],
            onToggleCompleted: {
                withAnimation(.easeInOut(duration: 0.2)) {
                    _ = viewModel.toggleCompleted(item)
                }
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
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                withAnimation {
                    viewModel.deleteItem(item)
                }
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
                .background(CruiseAzure)
                .clipShape(Circle())
                .shadow(color: CruiseAzure.opacity(0.4), radius: 8, x: 0, y: 4)
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
                item: item,
                category: viewModel.categoriesById[item.categoryId],
                activeVoyage: viewModel.activeVoyage,
                onConfirm: { date, startMinute, durationMinutes in
                    viewModel.convertToEvent(
                        item: item,
                        date: date,
                        startMinute: startMinute,
                        durationMinutes: durationMinutes
                    )
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
                            .fill(viewModel.selectedCategoryId == nil ? CruiseAzure : Color.white)
                    )
                    .foregroundColor(viewModel.selectedCategoryId == nil ? .white : Slate700)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(viewModel.selectedCategoryId == nil ? CruiseAzure : Slate200, lineWidth: 1)
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
                        .foregroundColor(CruiseAzure)
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
                                colors: [CruiseAzure, CruiseMarine],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: geometry.size.width * progress, height: 6)
                        .animation(.easeInOut(duration: 0.3), value: progress)
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
                .foregroundColor(CruiseAzure.opacity(0.6))

            Text(viewModel.selectedCategoryId == nil ? "暂无心愿项目" : "该分类下暂无心愿")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Slate800)

            Text("点击右下方 “+” 按钮，添加您想在邮轮上体验的演出、活动、美食或打卡点。")
                .font(.system(size: 14))
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
            let initialCat = viewModel.selectedCategoryId ?? viewModel.categories.first?.id ?? 1
            _selectedCategoryId = State(initialValue: initialCat)
        }
    }

    var body: some View {
        NavigationView {
            Form {
                // Section: Basic Info
                Section(header: Text("项目信息")) {
                    TextField("心愿名称（如：狮子王演出、旋转滑梯）", text: $title)

                    // Category Picker
                    Picker("所属类别", selection: $selectedCategoryId) {
                        ForEach(viewModel.categories) { cat in
                            HStack {
                                Circle()
                                    .fill(Color(argb: cat.colorArgb))
                                    .frame(width: 8, height: 8)
                                Text(cat.name)
                            }
                            .tag(cat.id)
                        }
                    }
                }

                // Section: Location Info
                Section(header: Text("位置信息（可选）")) {
                    // Deck Picker
                    Picker("甲板楼层", selection: $selectedDeck) {
                        Text("未指定甲板").tag(Optional<Int>.none)
                        ForEach(1...20, id: \.self) { (deck: Int) in
                            Text("Deck \(deck)").tag(Optional<Int>.some(deck))
                        }
                    }

                    // Zone Picker
                    Picker("船体方位", selection: $selectedZone) {
                        Text("未指定方位").tag(Optional<ShipZone>.none)
                        ForEach(ShipZone.allCases, id: \.self) { (zone: ShipZone) in
                            Text(zone.displayName).tag(Optional<ShipZone>.some(zone))
                        }
                    }

                    TextField("场所/餐厅名称（如：沃尔特剧院）", text: $placeName)
                }

                // Section: Notes
                Section(header: Text("备注信息（可选）")) {
                    TextField("记录注意事项、着装要求或预约提示", text: $note, axis: .vertical)
                        .lineLimit(3...6)
                }

                // Section: Delete (if editing)
                if let item = editingItem {
                    Section {
                        Button(role: .destructive) {
                            viewModel.deleteItem(item)
                            onDismiss()
                        } label: {
                            HStack {
                                Spacer()
                                Image(systemName: "trash")
                                Text("删除此心愿")
                                Spacer()
                            }
                        }
                    }
                }
            }
            .navigationTitle(editingItem == nil ? "添加心愿" : "编辑心愿")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        onDismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        saveAction()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }

    private func saveAction() {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedTitle.isEmpty else { return }

        let trimmedPlace = placeName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedNote = note.trimmingCharacters(in: .whitespacesAndNewlines)

        viewModel.saveItem(
            id: editingItem?.id ?? 0,
            title: trimmedTitle,
            categoryId: selectedCategoryId,
            deck: selectedDeck,
            zone: selectedZone,
            placeName: trimmedPlace.isEmpty ? nil : trimmedPlace,
            note: trimmedNote.isEmpty ? nil : trimmedNote
        )
        onDismiss()
    }
}

// MARK: - Wishlist Convert Modal Sheet

private struct WishlistConvertModalSheet: View {
    let item: WishlistItem
    let category: Category?
    let activeVoyage: Voyage?
    let onConfirm: (SimpleDate, Int, Int) -> Void
    let onDismiss: () -> Void

    @State private var selectedDate: SimpleDate
    @State private var startHour: Int = 10
    @State private var startMinute: Int = 0
    @State private var durationMinutes: Int = 30

    init(
        item: WishlistItem,
        category: Category?,
        activeVoyage: Voyage?,
        onConfirm: @escaping (SimpleDate, Int, Int) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.item = item
        self.category = category
        self.activeVoyage = activeVoyage
        self.onConfirm = onConfirm
        self.onDismiss = onDismiss

        let initialDate = activeVoyage?.boardDate ?? SimpleDate.today()
        _selectedDate = State(initialValue: initialDate)
    }

    private var availableDates: [SimpleDate] {
        if let voyage = activeVoyage {
            return voyage.datesInVoyage()
        }
        return [selectedDate]
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("待转入行程项目")) {
                    HStack(spacing: 8) {
                        CategoryChip(
                            name: category?.name ?? "项目",
                            colorArgb: category?.colorArgb ?? 0xFF0284C7,
                            showDot: true
                        )
                        Text(item.title)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Slate900)
                    }
                }

                Section(header: Text("选择安排日期")) {
                    Picker("日期", selection: $selectedDate) {
                        ForEach(availableDates, id: \.self) { date in
                            Text("\(date.description) (\(date.dayOfWeekShort))")
                                .tag(date)
                        }
                    }
                }

                Section(header: Text("安排时间")) {
                    HStack {
                        Text("开始时间")
                        Spacer()
                        Picker("时", selection: $startHour) {
                            ForEach(6...23, id: \.self) { h in
                                Text(String(format: "%02d:00", h)).tag(h)
                            }
                        }
                        .labelsHidden()

                        Picker("分", selection: $startMinute) {
                            ForEach([0, 15, 30, 45], id: \.self) { m in
                                Text(String(format: ":%02d", m)).tag(m)
                            }
                        }
                        .labelsHidden()
                    }

                    Picker("预计时长", selection: $durationMinutes) {
                        Text("15 分钟").tag(15)
                        Text("30 分钟").tag(30)
                        Text("45 分钟").tag(45)
                        Text("60 分钟 (1小时)").tag(60)
                        Text("90 分钟 (1.5小时)").tag(90)
                        Text("120 分钟 (2小时)").tag(120)
                    }
                }
            }
            .navigationTitle("转为日程事件")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        onDismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("确认转入") {
                        let totalStartMinutes = startHour * 60 + startMinute
                        onConfirm(selectedDate, totalStartMinutes, durationMinutes)
                    }
                }
            }
        }
    }
}
