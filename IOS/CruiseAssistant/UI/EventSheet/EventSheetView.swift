import SwiftUI

/// Modern sheet UI for creating and editing cruise scheduled events.
public struct EventSheetView: View {
    @ObservedObject public var viewModel: EventSheetViewModel
    public let onDismiss: () -> Void
    public let onManageCategories: (() -> Void)?

    @State private var activeTimeTab: TimeTab = .start

    public enum TimeTab {
        case start
        case end
    }

    public init(
        viewModel: EventSheetViewModel,
        onDismiss: @escaping () -> Void,
        onManageCategories: (() -> Void)? = nil
    ) {
        self.viewModel = viewModel
        self.onDismiss = onDismiss
        self.onManageCategories = onManageCategories
    }

    private let cardShape = RoundedRectangle(cornerRadius: 16, style: .continuous)

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Card 1: Category & Title
                    categoryAndTitleCard

                    // Card 2: Meal Period (if meal category and built-in ship)
                    if !viewModel.isSystemEvent && viewModel.showMealPeriodRow {
                        mealPeriodCard
                    }

                    // Card 3: Location
                    if !viewModel.isSystemEvent {
                        locationCard
                    }

                    // Card 4: Show Wait Minutes (if show category)
                    if !viewModel.isSystemEvent && viewModel.categoryIconKey == "show" {
                        waitMinutesCard
                    }

                    // Card 5: Time Selection
                    timeSelectionCard

                    // Card 6: Repeat Daily (if creating new event)
                    if !viewModel.isEditing {
                        repeatDailyCard
                    }

                    // Error Message Banner (if any)
                    if let error = viewModel.errorMessage {
                        HStack(spacing: 6) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundColor(.red)
                            Text(error)
                                .font(.footnote)
                                .foregroundColor(.red)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 4)
                    }

                    // Delete Event Button (if editing)
                    if viewModel.isEditing && !viewModel.isSystemEvent {
                        deleteEventButton
                    }
                }
                .padding(16)
            }
            .background(Slate50.ignoresSafeArea())
            .navigationTitle(viewModel.isEditing ? "编辑活动" : "添加活动")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        onDismiss()
                    }
                    .foregroundColor(Slate700)
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        viewModel.save {
                            onDismiss()
                        }
                    }
                    .fontWeight(.semibold)
                    .foregroundColor(CruiseAzure)
                    .disabled(viewModel.saving)
                }
            }
            .alert("营业时间提示", isPresented: $viewModel.hoursCheckPending) {
                Button("仍要保存", role: .none) {
                    viewModel.onHoursCheckOverride()
                    viewModel.save {
                        onDismiss()
                    }
                }
                Button("返回修改", role: .cancel) {
                    viewModel.onHoursCheckDismiss()
                }
            } message: {
                Text(viewModel.hoursCheckMessage ?? "")
            }
        }
    }

    // MARK: - Card 1: Category & Title

    private var categoryAndTitleCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(title: "活动类别与名称", systemImage: "square.grid.2x2")

            CategorySelector(
                categories: viewModel.categories,
                selectedCategoryId: viewModel.selectedCategoryId,
                isEnabled: !viewModel.isSystemEvent,
                onSelectCategory: { cat in
                    viewModel.onCategorySelect(id: cat.id)
                },
                onManageCategories: onManageCategories
            )

            if viewModel.categoryIconKey == "show" && viewModel.showPresetMode && !viewModel.isSystemEvent {
                ShowPresetPicker(
                    selectedShowTitle: viewModel.title,
                    isPresetMode: viewModel.showPresetMode,
                    isEnabled: !viewModel.isSystemEvent,
                    onSelectPreset: { preset in
                        viewModel.onShowPresetSelect(preset)
                    },
                    onCustomShow: {
                        viewModel.onCustomShowSelected()
                    }
                )
            } else {
                HStack {
                    TextField("活动名称（如：午餐、看日落等）", text: Binding(
                        get: { viewModel.title },
                        set: { viewModel.onTitleChange($0) }
                    ))
                    .font(.system(size: 15))
                    .disabled(viewModel.isSystemEvent)

                    if !viewModel.title.isEmpty && !viewModel.isSystemEvent {
                        Button(action: { viewModel.onTitleChange("") }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(Slate400)
                                .font(.system(size: 15))
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(Slate200, lineWidth: 1)
                )
            }

            if viewModel.isSystemEvent {
                Text("系统生成的航程事件，仅支持修改时间")
                    .font(.caption)
                    .foregroundColor(Slate500)
            }
        }
        .padding(16)
        .background(SurfaceCard)
        .clipShape(cardShape)
        .overlay(cardShape.stroke(Slate200, lineWidth: 1))
    }

    // MARK: - Card 2: Meal Period

    private var mealPeriodCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "餐次类型", systemImage: "fork.knife")

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    let allSelected = viewModel.selectedMealPeriod == nil
                    Button(action: { viewModel.onMealPeriodSelect(nil) }) {
                        Text("全部")
                            .font(.system(size: 14, weight: allSelected ? .bold : .medium))
                            .foregroundColor(allSelected ? CruiseMarine : Slate700)
                            .lineLimit(1)
                            .fixedSize(horizontal: true, vertical: false)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(allSelected ? CruiseAzureContainer : Slate100)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(allSelected ? CruiseAzure : Slate200, lineWidth: allSelected ? 1.5 : 1)
                            )
                    }
                    .buttonStyle(PlainButtonStyle())

                    ForEach(MealPeriod.allCases, id: \.self) { period in
                        let isSelected = viewModel.selectedMealPeriod == period
                        Button(action: { viewModel.onMealPeriodSelect(period) }) {
                            Text(period.label)
                                .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                                .foregroundColor(isSelected ? CruiseMarine : Slate700)
                                .lineLimit(1)
                                .fixedSize(horizontal: true, vertical: false)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(isSelected ? CruiseAzureContainer : Slate100)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(isSelected ? CruiseAzure : Slate200, lineWidth: isSelected ? 1.5 : 1)
                                )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.vertical, 2)
            }
        }
        .padding(16)
        .background(SurfaceCard)
        .clipShape(cardShape)
        .overlay(cardShape.stroke(Slate200, lineWidth: 1))
    }

    // MARK: - Card 3: Location

    private var locationCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "活动地点", systemImage: "mappin.and.ellipse")

            let presetOptions = LocationCatalog.locationPresetOptions(
                iconKey: viewModel.categoryIconKey ?? "",
                mealPeriod: viewModel.selectedMealPeriod
            )

            LocationPicker(
                maxDeck: viewModel.maxDeck,
                minDeck: viewModel.minDeck,
                deck: viewModel.deck,
                zone: viewModel.zone,
                placeName: viewModel.placeName,
                presets: viewModel.presets,
                noLocation: viewModel.noLocation,
                presetOptions: presetOptions,
                presetMode: viewModel.locationPresetMode,
                isEnabled: !viewModel.isSystemEvent,
                onDeckChange: { viewModel.onDeckChange($0) },
                onZoneChange: { viewModel.onZoneChange($0) },
                onPlaceNameChange: { viewModel.onPlaceNameChange($0) },
                onNoLocationToggle: { viewModel.onNoLocationToggle($0) },
                onSelectPreset: { viewModel.onSelectPresetLocation($0) },
                onCustomLocation: { viewModel.onCustomLocationSelected() }
            )
        }
        .padding(16)
        .background(SurfaceCard)
        .clipShape(cardShape)
        .overlay(cardShape.stroke(Slate200, lineWidth: 1))
    }

    // MARK: - Card 4: Show Wait Minutes

    private var waitMinutesCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "提前候场时间", systemImage: "clock.badge.checkmark")

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    let waitOptions = [10, 15, 30, 45, 60, 90, 120]
                    ForEach(waitOptions, id: \.self) { mins in
                        let isSelected = viewModel.waitMinutes == mins
                        Button(action: {
                            viewModel.onWaitMinutesChange(mins)
                        }) {
                            Text("\(mins)分钟")
                                .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                                .foregroundColor(isSelected ? CruiseMarine : Slate700)
                                .lineLimit(1)
                                .fixedSize(horizontal: true, vertical: false)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(isSelected ? CruiseAzureContainer : Slate100)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(isSelected ? CruiseAzure : Slate200, lineWidth: isSelected ? 1.5 : 1)
                                )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.vertical, 2)
            }

            Text("演出开场前留出入场和占位时间，时间轴将自动向前延伸候场色块")
                .font(.caption)
                .foregroundColor(Slate500)
        }
        .padding(16)
        .background(SurfaceCard)
        .clipShape(cardShape)
        .overlay(cardShape.stroke(Slate200, lineWidth: 1))
    }

    // MARK: - Card 5: Time Selection (Refined Dual-Tab Architecture)

    private var timeSelectionCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                SectionHeader(title: "起止时间", systemImage: "clock")

                Spacer()

                HStack(spacing: 4) {
                    Text("\(viewModel.startTime.description)–\(viewModel.endTime.description)")
                    Text("(\(viewModel.durationMinutes)分钟)")
                        .foregroundColor(CruiseMarine)
                }
                .font(.system(size: 13, weight: .bold, design: .monospaced))
                .foregroundColor(Slate700)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(CruiseAzureContainer)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }

            // Dual Tab Buttons for Start Time and End Time
            HStack(spacing: 12) {
                Button(action: { activeTimeTab = .start }) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("开始时间")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(activeTimeTab == .start ? CruiseMarine : Slate500)

                        Text(viewModel.startTime.description)
                            .font(.system(size: 24, weight: .bold, design: .rounded))
                            .foregroundColor(activeTimeTab == .start ? CruiseMarine : Slate800)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(activeTimeTab == .start ? CruiseAzureContainer : Slate100)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(activeTimeTab == .start ? CruiseAzure : Slate200, lineWidth: activeTimeTab == .start ? 2 : 1)
                    )
                }
                .buttonStyle(PlainButtonStyle())

                Button(action: { activeTimeTab = .end }) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("结束时间")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(activeTimeTab == .end ? CruiseMarine : Slate500)

                        Text(viewModel.endTime.description)
                            .font(.system(size: 24, weight: .bold, design: .rounded))
                            .foregroundColor(activeTimeTab == .end ? CruiseMarine : Slate800)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(activeTimeTab == .end ? CruiseAzureContainer : Slate100)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(activeTimeTab == .end ? CruiseAzure : Slate200, lineWidth: activeTimeTab == .end ? 2 : 1)
                    )
                }
                .buttonStyle(PlainButtonStyle())
            }

            // Full-Width Wheel Picker for the Active Time Tab
            VStack(spacing: 8) {
                if activeTimeTab == .start {
                    TimeWheelPicker(
                        time: Binding(
                            get: { viewModel.startTime },
                            set: { viewModel.onStartTimeChange($0) }
                        ),
                        minuteStep: 5
                    )
                } else {
                    TimeWheelPicker(
                        time: Binding(
                            get: { viewModel.endTime },
                            set: { viewModel.onEndTimeChange($0) }
                        ),
                        minuteStep: 5
                    )
                }

                // Quick Duration Adjustment Chips
                HStack(spacing: 8) {
                    Text("快速设时长:")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(Slate500)

                    ForEach([15, 30, 45, 60, 90], id: \.self) { duration in
                        Button(action: {
                            let endMin = min(1440, viewModel.startTime.totalMinutes + duration)
                            viewModel.onEndTimeChange(SimpleTime(totalMinutes: endMin))
                        }) {
                            Text("+\(duration)分")
                                .font(.system(size: 12, weight: viewModel.durationMinutes == duration ? .bold : .regular))
                                .foregroundColor(viewModel.durationMinutes == duration ? CruiseMarine : Slate700)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(viewModel.durationMinutes == duration ? CruiseAzureContainer : Slate100)
                                .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.top, 4)
            }
            .padding(.vertical, 4)
        }
        .padding(16)
        .background(SurfaceCard)
        .clipShape(cardShape)
        .overlay(cardShape.stroke(Slate200, lineWidth: 1))
    }

    // MARK: - Card 6: Repeat Daily

    private var repeatDailyCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Toggle(isOn: Binding(
                get: { viewModel.recurring },
                set: { viewModel.onRecurringToggle($0) }
            )) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .foregroundColor(CruiseAzure)
                        .font(.system(size: 16, weight: .medium))

                    Text("每日循环")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(Slate900)
                }
            }
            .tint(CruiseAzure)

            if viewModel.recurring {
                Text("将在航程中的每一天（除离船日外）自动添加此活动")
                    .font(.caption)
                    .foregroundColor(Slate500)
            }
        }
        .padding(16)
        .background(SurfaceCard)
        .clipShape(cardShape)
        .overlay(cardShape.stroke(Slate200, lineWidth: 1))
    }

    // MARK: - Delete Event Button

    private var deleteEventButton: some View {
        Button(role: .destructive, action: {
            viewModel.delete {
                onDismiss()
            }
        }) {
            HStack {
                Image(systemName: "trash.fill")
                Text("删除此活动")
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundColor(.red)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(SurfaceCard)
            .clipShape(cardShape)
            .overlay(cardShape.stroke(Color.red.opacity(0.3), lineWidth: 1))
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Section Header Component

private struct SectionHeader: View {
    let title: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(CruiseAzure)
                .frame(width: 28, height: 28)
                .background(CruiseAzureContainer)
                .clipShape(Circle())

            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Slate900)
        }
    }
}

#Preview {
    EventSheetPreviewWrapper()
}

private struct EventSheetPreviewWrapper: View {
    var body: some View {
        let container = AppContainer.inMemory()

        let vm = EventSheetViewModel(
            date: SimpleDate(year: 2026, month: 9, day: 3),
            defaultStartMinute: 480,
            editingEventId: 0,
            eventRepository: container.eventRepository,
            categoryRepository: container.categoryRepository,
            locationPresetRepository: container.locationPresetRepository,
            voyageRepository: container.voyageRepository,
            notificationService: container.notificationService,
            settingsStore: container.settingsStore
        )

        return EventSheetView(viewModel: vm, onDismiss: {})
    }
}
