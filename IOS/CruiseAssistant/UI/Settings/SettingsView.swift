import SwiftUI

/// Main Settings view providing configuration for voyage schedule, timeline display, Duffy themes, wallpapers, notifications, and data backup.
public struct SettingsView: View {
    @ObservedObject public var viewModel: SettingsViewModel

    @State private var showingEditVoyageSheet: Bool = false
    @State private var showingResetAlert: Bool = false
    @State private var showingBackupSheet: Bool = false
    @State private var exportedJSONText: String = ""

    public init(viewModel: SettingsViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        Form {
            // Section 1: 航次与船期 (Voyage & Schedule)
            voyageSection

            // Section 2: 时间线偏好 (Timeline Preferences)
            timelinePreferencesSection

            // Section 3: 达菲主题与壁纸 (Themes & Wallpapers)
            themeAndWallpaperSection

            // Section 4: 本地提醒 (Notifications)
            notificationsSection

            // Section 5: 数据与备份 (Data & Backup)
            dataManagementSection

            // Section 6: 关于 (About & Version)
            aboutSection
        }
        .navigationTitle("设置")
        .inlineNavigationBarTitle()
        .sheet(isPresented: $showingEditVoyageSheet) {
            if let voyage = viewModel.voyage {
                EditVoyageSheet(
                    voyage: voyage,
                    initialDinnerTime: viewModel.settings.dinnerTime,
                    onSave: { shipName, boardDate, boardTime, disembarkDate, disembarkTime, dinnerTime in
                        viewModel.updateVoyage(
                            shipName: shipName,
                            boardDate: boardDate,
                            boardTime: boardTime,
                            disembarkDate: disembarkDate,
                            disembarkTime: disembarkTime,
                            dinnerTime: dinnerTime
                        )
                        showingEditVoyageSheet = false
                    },
                    onDismiss: {
                        showingEditVoyageSheet = false
                    }
                )
            }
        }
        .sheet(isPresented: $showingBackupSheet) {
            BackupExportSheet(
                jsonText: exportedJSONText,
                onDismiss: {
                    showingBackupSheet = false
                }
            )
        }
        .alert("确定重置航次数据？", isPresented: $showingResetAlert) {
            Button("取消", role: .cancel) {}
            Button("确认重置", role: .destructive) {
                viewModel.resetVoyage()
            }
        } message: {
            Text("所有日程事件、打卡心愿和仙尘互赠记录都将被清除，并将重新开始航次引导。")
        }
        .onAppear {
            viewModel.loadData()
        }
    }

    // MARK: - Section 1: 航次与船期

    @ViewBuilder
    private var voyageSection: some View {
        Section(header: Label("航次与船期", systemImage: "ferry.fill")) {
            if let voyage = viewModel.voyage {
                HStack {
                    Text("邮轮名称")
                        .foregroundColor(Slate700)
                    Spacer()
                    Text(voyage.shipName ?? "迪士尼探险号")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Slate900)
                }

                HStack {
                    Text("登船日期")
                        .foregroundColor(Slate700)
                    Spacer()
                    Text("\(voyage.boardDate.description) (\(voyage.boardDate.dayOfWeekShort))")
                        .foregroundColor(Slate900)
                }

                HStack {
                    Text("离船日期")
                        .foregroundColor(Slate700)
                    Spacer()
                    Text("\(voyage.disembarkDate.description) (\(voyage.disembarkDate.dayOfWeekShort))")
                        .foregroundColor(Slate900)
                }

                HStack {
                    Text("晚餐席次时间")
                        .foregroundColor(Slate700)
                    Spacer()
                    Text(viewModel.settings.dinnerTime.description)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(CruiseAzure)
                }

                Button {
                    showingEditVoyageSheet = true
                } label: {
                    HStack {
                        Image(systemName: "pencil")
                        Text("修改船期与晚餐时间")
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(CruiseAzure)
                }
            } else {
                Text("暂无有效航次数据")
                    .foregroundColor(Slate500)
            }
        }
    }

    // MARK: - Section 2: 时间线偏好

    @ViewBuilder
    private var timelinePreferencesSection: some View {
        Section(header: Label("时间线偏好", systemImage: "calendar.day.timeline.left")) {
            // Hour Height Slider
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("每小时显示高度")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate700)
                    Spacer()
                    Text("\(Int(viewModel.settings.hourHeightPt)) pt")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(CruiseAzure)
                }

                HStack(spacing: 8) {
                    Text("紧凑 (60)")
                        .font(.system(size: 11))
                        .foregroundColor(Slate400)

                    Slider(
                        value: Binding(
                            get: { viewModel.settings.hourHeightPt },
                            set: { viewModel.setHourHeight($0) }
                        ),
                        in: 60...180,
                        step: 5
                    )
                    .tint(CruiseAzure)

                    Text("宽松 (180)")
                        .font(.system(size: 11))
                        .foregroundColor(Slate400)
                }
            }
            .padding(.vertical, 4)

            // Timeline Display Range Pickers
            HStack {
                Text("显示起始时间")
                    .foregroundColor(Slate700)
                Spacer()
                Picker("起始时间", selection: Binding(
                    get: { viewModel.settings.displayStartMinute },
                    set: { viewModel.setDisplayRange(startMinute: $0, endMinute: viewModel.settings.displayEndMinute) }
                )) {
                    ForEach(Array(stride(from: 360, through: 720, by: 60)), id: \.self) { min in
                        Text(String(format: "%02d:00", min / 60)).tag(min)
                    }
                }
                .labelsHidden()
            }

            HStack {
                Text("显示结束时间")
                    .foregroundColor(Slate700)
                Spacer()
                Picker("结束时间", selection: Binding(
                    get: { viewModel.settings.displayEndMinute },
                    set: { viewModel.setDisplayRange(startMinute: viewModel.settings.displayStartMinute, endMinute: $0) }
                )) {
                    ForEach(Array(stride(from: 1200, through: 1440, by: 60)), id: \.self) { min in
                        Text(String(format: "%02d:00", min / 60)).tag(min)
                    }
                }
                .labelsHidden()
            }

            // Collapse Gaps Toggle
            Toggle(isOn: Binding(
                get: { viewModel.settings.collapseEmptyGaps },
                set: { viewModel.setCollapseEmptyGaps($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("折叠空白时段")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate800)
                    Text("当连续无事件超过2小时时自动收拢间隙")
                        .font(.system(size: 11))
                        .foregroundColor(Slate500)
                }
            }
            .tint(CruiseAzure)
        }
    }

    // MARK: - Section 3: 达菲主题与壁纸

    @ViewBuilder
    private var themeAndWallpaperSection: some View {
        Section(header: Label("达菲主题与壁纸", systemImage: "paintpalette.fill")) {
            VStack(alignment: .leading, spacing: 10) {
                Text("主题配色")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Slate800)

                ThemePickerView(
                    selectedTheme: viewModel.settings.colorTheme,
                    onSelectTheme: { theme in
                        viewModel.setColorTheme(theme)
                    }
                )
            }
            .padding(.vertical, 4)

            VStack(alignment: .leading, spacing: 10) {
                Text("时间线背景壁纸")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Slate800)

                WallpaperPickerView(
                    selectedKey: viewModel.settings.wallpaperKey,
                    wallpaperEnabled: viewModel.settings.wallpaperEnabled,
                    wallpaperAlpha: viewModel.settings.wallpaperAlpha,
                    onSelectWallpaper: { key in
                        viewModel.setWallpaperKey(key)
                    },
                    onAlphaChanged: { alpha in
                        viewModel.setWallpaperAlpha(alpha)
                    },
                    onToggleEnabled: { enabled in
                        viewModel.setWallpaperEnabled(enabled)
                    }
                )
            }
            .padding(.vertical, 4)
        }
    }

    // MARK: - Section 4: 本地提醒

    @ViewBuilder
    private var notificationsSection: some View {
        Section(header: Label("本地提醒通知", systemImage: "bell.fill")) {
            Toggle("启用事件提醒", isOn: Binding(
                get: { viewModel.settings.notificationsEnabled },
                set: { viewModel.setNotificationsEnabled($0) }
            ))
            .tint(CruiseAzure)

            if viewModel.settings.notificationsEnabled {
                Toggle("提前 15 分钟提醒", isOn: Binding(
                    get: { viewModel.settings.remindBefore15 },
                    set: { viewModel.setRemindBefore15($0) }
                ))
                .tint(CruiseAzure)
                .padding(.leading, 12)

                Toggle("提前 5 分钟提醒", isOn: Binding(
                    get: { viewModel.settings.remindBefore5 },
                    set: { viewModel.setRemindBefore5($0) }
                ))
                .tint(CruiseAzure)
                .padding(.leading, 12)

                Toggle("活动开始时提醒", isOn: Binding(
                    get: { viewModel.settings.remindAtStart },
                    set: { viewModel.setRemindAtStart($0) }
                ))
                .tint(CruiseAzure)
                .padding(.leading, 12)
            }
        }
    }

    // MARK: - Section 5: 数据与备份

    @ViewBuilder
    private var dataManagementSection: some View {
        Section(header: Label("数据与备份", systemImage: "externaldrive.fill")) {
            Button {
                if let json = viewModel.exportBackupJSON() {
                    exportedJSONText = json
                    showingBackupSheet = true
                }
            } label: {
                HStack {
                    Image(systemName: "square.and.arrow.up")
                        .foregroundColor(CruiseAzure)
                    Text("导出数据备份 (JSON)")
                        .foregroundColor(Slate900)
                    Spacer()
                }
            }

            Button(role: .destructive) {
                showingResetAlert = true
            } label: {
                HStack {
                    Image(systemName: "trash.fill")
                    Text("重置航次数据")
                }
            }
        }
    }

    // MARK: - Section 6: 关于

    @ViewBuilder
    private var aboutSection: some View {
        Section(header: Label("关于应用", systemImage: "info.circle.fill")) {
            HStack {
                Text("应用名称")
                    .foregroundColor(Slate700)
                Spacer()
                Text("迪士尼邮轮助手 (Cruise Assistant)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Slate900)
            }

            HStack {
                Text("版本号")
                    .foregroundColor(Slate700)
                Spacer()
                Text(CruiseAssistant.version)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Slate600)
            }

            HStack {
                Text("运行模式")
                    .foregroundColor(Slate700)
                Spacer()
                HStack(spacing: 4) {
                    Image(systemName: "antenna.radiowaves.left.and.right.slash")
                        .font(.system(size: 12))
                        .foregroundColor(.green)
                    Text("100% 离线单机运行")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.green)
                }
            }
        }
    }
}

// MARK: - Edit Voyage Modal Sheet

private struct EditVoyageSheet: View {
    let voyage: Voyage
    let onSave: (String?, SimpleDate, SimpleTime, SimpleDate, SimpleTime, SimpleTime) -> Void
    let onDismiss: () -> Void

    @State private var shipName: String
    @State private var boardDate: SimpleDate
    @State private var boardHour: Int
    @State private var boardMinute: Int
    @State private var disembarkDate: SimpleDate
    @State private var disembarkHour: Int
    @State private var disembarkMinute: Int
    @State private var dinnerHour: Int
    @State private var dinnerMinute: Int

    init(
        voyage: Voyage,
        initialDinnerTime: SimpleTime,
        onSave: @escaping (String?, SimpleDate, SimpleTime, SimpleDate, SimpleTime, SimpleTime) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.voyage = voyage
        self.onSave = onSave
        self.onDismiss = onDismiss

        _shipName = State(initialValue: voyage.shipName ?? "迪士尼探险号")
        _boardDate = State(initialValue: voyage.boardDate)
        _boardHour = State(initialValue: voyage.boardTime.hour)
        _boardMinute = State(initialValue: voyage.boardTime.minute)
        _disembarkDate = State(initialValue: voyage.disembarkDate)
        _disembarkHour = State(initialValue: voyage.disembarkTime.hour)
        _disembarkMinute = State(initialValue: voyage.disembarkTime.minute)
        _dinnerHour = State(initialValue: initialDinnerTime.hour)
        _dinnerMinute = State(initialValue: initialDinnerTime.minute)
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("邮轮基本信息")) {
                    TextField("邮轮名称", text: $shipName)
                }

                Section(header: Text("登船时间")) {
                    DatePicker(
                        "登船日期",
                        selection: Binding(
                            get: { boardDate.toDate() },
                            set: { boardDate = SimpleDate.fromDate($0) }
                        ),
                        displayedComponents: .date
                    )

                    HStack {
                        Text("登船时刻")
                        Spacer()
                        Picker("时", selection: $boardHour) {
                            ForEach(8...18, id: \.self) { h in
                                Text(String(format: "%02d:00", h)).tag(h)
                            }
                        }
                        .labelsHidden()

                        Picker("分", selection: $boardMinute) {
                            ForEach([0, 15, 30, 45], id: \.self) { m in
                                Text(String(format: ":%02d", m)).tag(m)
                            }
                        }
                        .labelsHidden()
                    }
                }

                Section(header: Text("离船时间")) {
                    DatePicker(
                        "离船日期",
                        selection: Binding(
                            get: { disembarkDate.toDate() },
                            set: { disembarkDate = SimpleDate.fromDate($0) }
                        ),
                        displayedComponents: .date
                    )

                    HStack {
                        Text("离船时刻")
                        Spacer()
                        Picker("时", selection: $disembarkHour) {
                            ForEach(5...14, id: \.self) { h in
                                Text(String(format: "%02d:00", h)).tag(h)
                            }
                        }
                        .labelsHidden()

                        Picker("分", selection: $disembarkMinute) {
                            ForEach([0, 15, 30, 45], id: \.self) { m in
                                Text(String(format: ":%02d", m)).tag(m)
                            }
                        }
                        .labelsHidden()
                    }
                }

                Section(header: Text("晚餐主席时间")) {
                    HStack {
                        Text("固定晚餐时间")
                        Spacer()
                        Picker("时", selection: $dinnerHour) {
                            ForEach(17...21, id: \.self) { h in
                                Text(String(format: "%02d:00", h)).tag(h)
                            }
                        }
                        .labelsHidden()

                        Picker("分", selection: $dinnerMinute) {
                            ForEach([0, 15, 30, 45], id: \.self) { m in
                                Text(String(format: ":%02d", m)).tag(m)
                            }
                        }
                        .labelsHidden()
                    }
                }
            }
            .navigationTitle("修改航次设置")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        onDismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        let boardTime = SimpleTime(hour: boardHour, minute: boardMinute)
                        let disembarkTime = SimpleTime(hour: disembarkHour, minute: disembarkMinute)
                        let dinnerTime = SimpleTime(hour: dinnerHour, minute: dinnerMinute)
                        onSave(shipName, boardDate, boardTime, disembarkDate, disembarkTime, dinnerTime)
                    }
                }
            }
        }
    }
}

// MARK: - Backup Export Sheet

private struct BackupExportSheet: View {
    let jsonText: String
    let onDismiss: () -> Void

    @State private var copied: Bool = false

    var body: some View {
        NavigationView {
            VStack(spacing: 12) {
                Text("以下是您的完整离线数据备份 JSON，包含所有航次、日程、心愿与仙尘记录。您可以复制保存。")
                    .font(.system(size: 13))
                    .foregroundColor(Slate600)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                ScrollView {
                    Text(jsonText)
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundColor(Slate800)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Slate100)
                        .cornerRadius(8)
                }
                .padding(.horizontal, 16)

                Button {
                    #if canImport(UIKit)
                    UIPasteboard.general.string = jsonText
                    #endif
                    withAnimation {
                        copied = true
                    }
                } label: {
                    HStack {
                        Image(systemName: copied ? "checkmark" : "doc.on.doc")
                        Text(copied ? "已复制到剪贴板" : "复制全部备份内容")
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(copied ? Color.green : CruiseAzure)
                    .cornerRadius(10)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
            .navigationTitle("数据备份")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") {
                        onDismiss()
                    }
                }
            }
        }
    }
}
