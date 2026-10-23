import SwiftUI

/// Main Settings view providing configuration for voyage schedule, timeline display, Duffy themes, wallpapers, notifications, and voyage archives.
public struct SettingsView: View {
    @ObservedObject public var viewModel: SettingsViewModel

    @State private var showingEditVoyageSheet: Bool = false
    @State private var showingResetAlert: Bool = false
    @State private var showingBackupSheet: Bool = false
    @State private var showingImportArchiveSheet: Bool = false
    @State private var selectedArchiveEntry: VoyageArchiveEntry? = nil
    @State private var exportedJSONText: String = ""

    public init(viewModel: SettingsViewModel) {
        self.viewModel = viewModel
    }

    private var palette: CruiseColorPalette {
        viewModel.settings.colorTheme.palette
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

            // Section 5: 历史航程存档 (Voyage Archives)
            voyageArchivesSection

            // Section 6: 数据与备份 (Data & Backup)
            dataManagementSection

            // Section 7: 关于 (About & Version)
            aboutSection
        }
        .navigationTitle("设置")
        .inlineNavigationBarTitle()
        .sheet(isPresented: $showingEditVoyageSheet) {
            if let voyage = viewModel.voyage {
                EditVoyageSheet(
                    voyage: voyage,
                    initialDinnerTime: viewModel.settings.dinnerTime,
                    onSave: { shipName, boardDate, boardTime, disembarkDate, disembarkTime, dinnerTime, isConcierge, legs, ashoreLegs in
                        viewModel.updateVoyage(
                            shipName: shipName,
                            boardDate: boardDate,
                            boardTime: boardTime,
                            disembarkDate: disembarkDate,
                            disembarkTime: disembarkTime,
                            dinnerTime: dinnerTime,
                            isConcierge: isConcierge,
                            legs: legs,
                            ashoreLegs: ashoreLegs
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
        .sheet(isPresented: $showingImportArchiveSheet) {
            ArchiveImportSheet(
                onImport: { json, name in
                    _ = viewModel.importArchiveJSON(json, customName: name)
                    showingImportArchiveSheet = false
                },
                onDismiss: {
                    showingImportArchiveSheet = false
                }
            )
        }
        .sheet(item: $selectedArchiveEntry) { entry in
            ArchiveDetailSheet(
                entry: entry,
                onRestore: { archiveCurrentFirst in
                    _ = viewModel.restoreArchive(id: entry.id, archiveCurrentFirst: archiveCurrentFirst)
                    selectedArchiveEntry = nil
                },
                onExportJSON: {
                    exportedJSONText = entry.payloadJson
                    selectedArchiveEntry = nil
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showingBackupSheet = true
                    }
                },
                onDelete: {
                    viewModel.deleteArchive(id: entry.id)
                    selectedArchiveEntry = nil
                },
                onDismiss: {
                    selectedArchiveEntry = nil
                }
            )
        }
        .alert("重置当前航程？", isPresented: $showingResetAlert) {
            Button("先存档再重置") {
                viewModel.archiveAndResetVoyage()
            }
            Button("直接重置", role: .destructive) {
                viewModel.resetVoyage()
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("重置后将清空当前航程的日程、愿望单和仙尘记录，并重新进入航程设置向导。建议先存入历史存档。")
        }
        .overlay(alignment: .bottom) {
            if let msg = viewModel.toastMessage {
                Text(msg)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Slate800.opacity(0.92))
                    .clipShape(Capsule())
                    .padding(.bottom, 24)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                            withAnimation {
                                viewModel.toastMessage = nil
                            }
                        }
                    }
            }
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
                    Text("登船时间")
                        .foregroundColor(Slate700)
                    Spacer()
                    if voyage.isConcierge {
                        Text("11:00 (礼宾优先登船)")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(palette.primary)
                    } else {
                        Text(voyage.boardTime.description)
                            .foregroundColor(Slate900)
                    }
                }

                HStack {
                    Text("离船日期")
                        .foregroundColor(Slate700)
                    Spacer()
                    Text("\(voyage.disembarkDate.description) (\(voyage.disembarkDate.dayOfWeekShort))")
                        .foregroundColor(Slate900)
                }

                if !voyage.legs.isEmpty {
                    HStack {
                        Text("联程编排")
                            .foregroundColor(Slate700)
                        Spacer()
                        Text("\(voyage.legs.map(String.init).joined(separator: "+"))夜联程 · 共\(voyage.legs.reduce(0, +))晚")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(palette.secondary)
                    }
                }

                HStack {
                    Text("晚餐席次时间")
                        .foregroundColor(Slate700)
                    Spacer()
                    Text(viewModel.settings.dinnerTime.description)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(palette.primary)
                }

                Button {
                    showingEditVoyageSheet = true
                } label: {
                    HStack {
                        Image(systemName: "pencil")
                        Text("修改船期、联程与晚餐时间")
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(palette.primary)
                }

                Button {
                    viewModel.addPresetShows()
                } label: {
                    HStack {
                        Image(systemName: "sparkles.tv")
                        Text("一键预添加迪士尼探险号官方演出")
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(palette.primary)
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
                        .foregroundColor(palette.primary)
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
                    .tint(palette.primary)

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
            .tint(palette.primary)

            // Card Dolls Watermark Toggle
            Toggle(isOn: Binding(
                get: { viewModel.settings.cardDollsEnabled },
                set: { viewModel.setCardDollsEnabled($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("卡片玩偶水印")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate800)
                    Text("在日程卡片右下角展示达菲与朋友主题玩偶贴纸")
                        .font(.system(size: 11))
                        .foregroundColor(Slate500)
                }
            }
            .tint(palette.primary)
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
            .tint(palette.primary)

            if viewModel.settings.notificationsEnabled {
                Toggle("提前 15 分钟提醒", isOn: Binding(
                    get: { viewModel.settings.remindBefore15 },
                    set: { viewModel.setRemindBefore15($0) }
                ))
                .tint(palette.primary)
                .padding(.leading, 12)

                Toggle("提前 5 分钟提醒", isOn: Binding(
                    get: { viewModel.settings.remindBefore5 },
                    set: { viewModel.setRemindBefore5($0) }
                ))
                .tint(palette.primary)
                .padding(.leading, 12)

                Toggle("活动开始时提醒", isOn: Binding(
                    get: { viewModel.settings.remindAtStart },
                    set: { viewModel.setRemindAtStart($0) }
                ))
                .tint(palette.primary)
                .padding(.leading, 12)
            }
        }
    }

    // MARK: - Section 5: 历史航程存档

    @ViewBuilder
    private var voyageArchivesSection: some View {
        Section(header: Label("历史航程存档", systemImage: "archivebox.fill")) {
            if viewModel.voyage != nil {
                Button {
                    viewModel.archiveCurrentVoyage()
                } label: {
                    HStack {
                        Image(systemName: "tray.and.arrow.down.fill")
                            .foregroundColor(palette.primary)
                        Text("将当前航程存入历史存档")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(palette.primary)
                        Spacer()
                    }
                }
            }

            Button {
                showingImportArchiveSheet = true
            } label: {
                HStack {
                    Image(systemName: "square.and.arrow.down")
                        .foregroundColor(palette.secondary)
                    Text("导入航程存档 (JSON)")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate800)
                    Spacer()
                }
            }

            if viewModel.archives.isEmpty {
                Text("暂无已保存的历史航程存档")
                    .font(.system(size: 13))
                    .foregroundColor(Slate400)
            } else {
                ForEach(viewModel.archives) { entry in
                    Button {
                        selectedArchiveEntry = entry
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(entry.name)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(Slate900)
                                Text("\(entry.boardDate.description) ~ \(entry.disembarkDate.description) · 日程 \(entry.eventCount) · 仙尘 \(entry.pixieCount)")
                                    .font(.system(size: 11))
                                    .foregroundColor(Slate500)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12))
                                .foregroundColor(Slate400)
                        }
                        .padding(.vertical, 2)
                    }
                }
            }
        }
    }

    // MARK: - Section 6: 数据与备份

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
                        .foregroundColor(palette.primary)
                    Text("导出完整数据库备份 (JSON)")
                        .foregroundColor(Slate900)
                    Spacer()
                }
            }

            Button(role: .destructive) {
                showingResetAlert = true
            } label: {
                HStack {
                    Image(systemName: "trash.fill")
                    Text("重置当前航程")
                }
            }
        }
    }

    // MARK: - Section 7: 关于

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
    let onSave: (String?, SimpleDate, SimpleTime, SimpleDate, SimpleTime, SimpleTime, Bool, [Int], Set<Int>) -> Void
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
    @State private var isConcierge: Bool
    @State private var legs: [Int]
    @State private var ashoreLegs: Set<Int>

    init(
        voyage: Voyage,
        initialDinnerTime: SimpleTime,
        onSave: @escaping (String?, SimpleDate, SimpleTime, SimpleDate, SimpleTime, SimpleTime, Bool, [Int], Set<Int>) -> Void,
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
        _isConcierge = State(initialValue: voyage.isConcierge)
        _legs = State(initialValue: voyage.legs)
        _ashoreLegs = State(initialValue: voyage.ashoreLegs)
    }

    private var effectiveSingleOrMultiLegs: [Int] {
        if !legs.isEmpty {
            return legs
        }
        let nights = boardDate.daysUntil(disembarkDate)
        if nights == 3 || nights == 4 {
            return [nights]
        }
        return []
    }

    private var nextLegToAppend: Int? {
        guard let last = effectiveSingleOrMultiLegs.last else { return nil }
        return last == 3 ? 4 : (last == 4 ? 3 : nil)
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("邮轮基本信息")) {
                    TextField("邮轮名称", text: $shipName)
                }

                Section(header: Text("登船时间与礼宾服务")) {
                    DatePicker(
                        "登船日期",
                        selection: Binding(
                            get: { boardDate.toDate() },
                            set: { newDate in
                                boardDate = SimpleDate.fromDate(newDate)
                                if !legs.isEmpty {
                                    disembarkDate = boardDate.plusDays(legs.reduce(0, +))
                                }
                            }
                        ),
                        displayedComponents: .date
                    )

                    Toggle("礼宾 (Concierge) 11:00 优先登船", isOn: $isConcierge)

                    if !isConcierge {
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
                }

                if !effectiveSingleOrMultiLegs.isEmpty {
                    Section(header: Text("联程编排与兰卡威登岸")) {
                        ForEach(Array(effectiveSingleOrMultiLegs.enumerated()), id: \.offset) { idx, nights in
                            HStack {
                                Text("第 \(idx + 1) 程 · \(nights) 夜")
                                    .font(.system(size: 14, weight: .semibold))
                                if nights == 4 {
                                    let isAshore = ashoreLegs.contains(idx)
                                    Button {
                                        if isAshore {
                                            ashoreLegs.remove(idx)
                                        } else {
                                            ashoreLegs.insert(idx)
                                        }
                                    } label: {
                                        HStack(spacing: 4) {
                                            Image(systemName: isAshore ? "checkmark.square.fill" : "square")
                                            Text("兰卡威登岸")
                                        }
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundColor(isAshore ? CruiseMarine : Slate600)
                                    }
                                    .buttonStyle(.plain)
                                }
                                Spacer()
                                if idx > 0 && idx == effectiveSingleOrMultiLegs.count - 1 {
                                    Button(role: .destructive) {
                                        ashoreLegs.remove(idx)
                                        if legs.count <= 2 {
                                            let firstNights = legs.first ?? 3
                                            legs = []
                                            disembarkDate = boardDate.plusDays(firstNights)
                                        } else {
                                            legs.removeLast()
                                            disembarkDate = boardDate.plusDays(legs.reduce(0, +))
                                        }
                                    } label: {
                                        Image(systemName: "minus.circle.fill")
                                            .foregroundColor(NowRed)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }

                        if let nextNights = nextLegToAppend {
                            Button {
                                if legs.isEmpty {
                                    let first = boardDate.daysUntil(disembarkDate)
                                    legs = [first, nextNights]
                                } else {
                                    legs.append(nextNights)
                                }
                                disembarkDate = boardDate.plusDays(legs.reduce(0, +))
                            } label: {
                                Label("续订下一程 (\(nextNights)夜)", systemImage: "plus.circle.fill")
                            }
                        }
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
                    .disabled(!legs.isEmpty)

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

                Section(header: Text("晚餐主餐时间")) {
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
                        let boardTime = isConcierge ? CONCIERGE_BOARD_TIME : SimpleTime(hour: boardHour, minute: boardMinute)
                        let disembarkTime = SimpleTime(hour: disembarkHour, minute: disembarkMinute)
                        let dinnerTime = SimpleTime(hour: dinnerHour, minute: dinnerMinute)
                        onSave(shipName, boardDate, boardTime, disembarkDate, disembarkTime, dinnerTime, isConcierge, legs, ashoreLegs)
                    }
                }
            }
        }
    }
}

// MARK: - Archive Detail Sheet

private struct ArchiveDetailSheet: View {
    let entry: VoyageArchiveEntry
    let onRestore: (Bool) -> Void
    let onExportJSON: () -> Void
    let onDelete: () -> Void
    let onDismiss: () -> Void

    @State private var showingRestoreConfirm: Bool = false
    @State private var showingDeleteConfirm: Bool = false

    private var snapshot: ArchiveSnapshot? {
        parseArchiveSnapshot(entry.payloadJson)
    }

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("存档概要")) {
                    HStack {
                        Text("存档名称")
                        Spacer()
                        Text(entry.name)
                            .fontWeight(.semibold)
                    }
                    HStack {
                        Text("邮轮名称")
                        Spacer()
                        Text(entry.shipName)
                    }
                    HStack {
                        Text("航程日期")
                        Spacer()
                        Text("\(entry.boardDate.description) ~ \(entry.disembarkDate.description)")
                    }
                    HStack {
                        Text("统计")
                        Spacer()
                        Text("日程 \(entry.eventCount) 项 · 仙尘 \(entry.pixieCount) 条")
                    }
                }

                if let snap = snapshot {
                    Section(header: Text("日程预览 (\(snap.events.count))")) {
                        ForEach(snap.events.prefix(15)) { ev in
                            HStack {
                                Text("\(ev.date.monthDayFormatted) \(ev.startTime.description)")
                                    .font(.system(size: 12, design: .monospaced))
                                    .foregroundColor(Slate500)
                                Text(ev.title)
                                    .font(.system(size: 14, weight: .medium))
                                Spacer()
                            }
                        }
                        if snap.events.count > 15 {
                            Text("... 共 \(snap.events.count) 项日程")
                                .font(.caption)
                                .foregroundColor(Slate400)
                        }
                    }
                }

                Section {
                    Button {
                        showingRestoreConfirm = true
                    } label: {
                        Label("恢复此航程存档", systemImage: "arrow.counterclockwise.circle.fill")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(CruiseAzure)
                    }

                    Button {
                        onExportJSON()
                    } label: {
                        Label("导出此存档 (JSON)", systemImage: "square.and.arrow.up")
                            .foregroundColor(Slate800)
                    }

                    Button(role: .destructive) {
                        showingDeleteConfirm = true
                    } label: {
                        Label("删除此存档", systemImage: "trash")
                    }
                }
            }
            .navigationTitle("航程存档详情")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("关闭", action: onDismiss)
                }
            }
            .alert("恢复历史航程存档？", isPresented: $showingRestoreConfirm) {
                Button("先存档当前航程再恢复") {
                    onRestore(true)
                }
                Button("直接覆盖恢复", role: .destructive) {
                    onRestore(false)
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("恢复存档将替换当前正在进行的航程数据。建议先将当前航程存入历史存档。")
            }
            .alert("确定删除此存档？", isPresented: $showingDeleteConfirm) {
                Button("删除", role: .destructive, action: onDelete)
                Button("取消", role: .cancel) {}
            }
        }
    }
}

// MARK: - Archive Import Sheet

private struct ArchiveImportSheet: View {
    let onImport: (String, String?) -> Void
    let onDismiss: () -> Void

    @State private var customName: String = ""
    @State private var jsonInput: String = ""

    var body: some View {
        NavigationView {
            VStack(spacing: 12) {
                TextField("存档名称 (选填)", text: $customName)
                    .padding(10)
                    .background(Slate100)
                    .cornerRadius(8)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                Text("请粘贴航程存档 JSON 内容：")
                    .font(.system(size: 13))
                    .foregroundColor(Slate600)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)

                TextEditor(text: $jsonInput)
                    .font(.system(size: 12, design: .monospaced))
                    .padding(8)
                    .background(Slate100)
                    .cornerRadius(8)
                    .padding(.horizontal, 16)

                Button {
                    let trimmedName = customName.trimmingCharacters(in: .whitespacesAndNewlines)
                    onImport(jsonInput, trimmedName.isEmpty ? nil : trimmedName)
                } label: {
                    Text("确认导入存档")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(jsonInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? Slate300 : CruiseAzure)
                        .cornerRadius(10)
                }
                .disabled(jsonInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
            .navigationTitle("导入航程存档")
            .inlineNavigationBarTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消", action: onDismiss)
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
                Text("以下是您的离线数据 JSON，包含航次、日程、心愿与仙尘记录。您可以复制保存。")
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
                        Text(copied ? "已复制到剪贴板" : "复制全部内容")
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
            .navigationTitle("导出数据")
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
