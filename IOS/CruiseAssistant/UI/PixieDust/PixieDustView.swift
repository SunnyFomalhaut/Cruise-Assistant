import SwiftUI

/// 仙尘（Pixie Dust）礼物互赠追踪主界面
public struct PixieDustView: View {
    @StateObject private var viewModel: PixieDustViewModel
    public let onNavigateToDeckMap: ((Int, [PixieRoomMarker]) -> Void)?

    public init(
        viewModel: PixieDustViewModel? = nil,
        onNavigateToDeckMap: ((Int, [PixieRoomMarker]) -> Void)? = nil
    ) {
        if let vm = viewModel {
            _viewModel = StateObject(wrappedValue: vm)
        } else {
            _viewModel = StateObject(wrappedValue: PixieDustViewModel())
        }
        self.onNavigateToDeckMap = onNavigateToDeckMap
    }

    @Environment(\.cruisePalette) private var palette

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Tab Picker
                tabPickerHeader

                // Main Content
                if viewModel.tab == .linkage {
                    // Full-screen Linkage View
                    LinkageViewDialog(
                        roomPairs: viewModel.roomPairs,
                        allRoomPairs: viewModel.allRoomPairs,
                        levels: viewModel.levels,
                        filter: viewModel.filter,
                        onSelectFilter: { viewModel.selectFilter($0) },
                        selectedPair: nil,
                        onNavigateToDeckMap: onNavigateToDeckMap,
                        onDismiss: { viewModel.selectTab(.given) }
                    )
                } else {
                    // Given or Received with Keypad & List
                    VStack(spacing: 0) {
                        // Quick Entry Top Area
                        quickEntryArea

                        Divider()

                        // Record List
                        if viewModel.tab == .given {
                            GivenListView(
                                records: viewModel.givenRecords,
                                grouped: viewModel.groupedGiven,
                                levels: viewModel.levels,
                                viewMode: viewModel.viewMode,
                                onToggleViewMode: { viewModel.toggleViewMode() },
                                onSelectRecord: { record in
                                    if let pair = viewModel.allRoomPairs.first(where: { $0.deck == record.deck && $0.room == record.room }) {
                                        viewModel.viewingPair = pair
                                    }
                                },
                                onSelectRoomGroup: { group in
                                    if let pair = viewModel.allRoomPairs.first(where: { $0.deck == group.deck && $0.room == group.room }) {
                                        viewModel.viewingPair = pair
                                    }
                                },
                                onEditRecord: { viewModel.editingRecord = $0 },
                                onDeleteRecord: { viewModel.deleteRecord($0) }
                            )
                        } else {
                            ReceivedListView(
                                records: viewModel.receivedRecords,
                                allRecords: viewModel.allReceivedRecords,
                                levels: viewModel.levels,
                                filter: viewModel.filter,
                                onSelectFilter: { viewModel.selectFilter($0) },
                                onSelectRecord: { record in
                                    if let pair = viewModel.allRoomPairs.first(where: { $0.deck == record.deck && $0.room == record.room }) {
                                        viewModel.viewingPair = pair
                                    }
                                },
                                onToggleReplied: { viewModel.toggleReplied(record: $0) },
                                onEditRecord: { viewModel.editingRecord = $0 },
                                onDeleteRecord: { viewModel.deleteRecord($0) }
                            )
                        }
                    }
                }
            }
            .background(Slate50)
            .navigationTitle("仙尘追踪")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    if viewModel.tab != .linkage {
                        Button {
                            viewModel.selectTab(.linkage)
                        } label: {
                            Image(systemName: "arrow.triangle.swap")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(palette.primary)
                        }
                        .accessibilityLabel("查看互赠明细")
                    }
                }
            }
            .sheet(item: $viewModel.editingRecord) { record in
                PixieDustEditDialog(
                    record: record,
                    levels: viewModel.levels,
                    availableDecks: viewModel.availableDecks,
                    onSave: { viewModel.updateRecord($0) },
                    onDelete: { viewModel.deleteRecord($0) },
                    onDismiss: { viewModel.editingRecord = nil }
                )
            }
            .sheet(item: $viewModel.pendingReplyRecord) { record in
                ReplyLevelDialog(
                    record: record,
                    levels: viewModel.levels,
                    onConfirm: { level in
                        viewModel.confirmReply(record: record, replyLevel: level)
                    },
                    onDismiss: { viewModel.cancelReply() }
                )
            }
            .sheet(item: $viewModel.viewingPair) { pair in
                LinkageViewDialog(
                    roomPairs: viewModel.roomPairs,
                    allRoomPairs: viewModel.allRoomPairs,
                    levels: viewModel.levels,
                    filter: viewModel.filter,
                    onSelectFilter: { viewModel.selectFilter($0) },
                    selectedPair: pair,
                    onNavigateToDeckMap: onNavigateToDeckMap,
                    onDismiss: { viewModel.viewingPair = nil }
                )
            }
            .overlay(alignment: .bottom) {
                if let message = viewModel.toastMessage {
                    toastBanner(message)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
        }
    }

    // MARK: - Tab Picker Header

    private var tabPickerHeader: some View {
        HStack(spacing: 0) {
            ForEach(PixieTab.allCases) { tabItem in
                Button {
                    viewModel.selectTab(tabItem)
                } label: {
                    VStack(spacing: 6) {
                        Text(tabItem.rawValue)
                            .font(.system(size: 15, weight: viewModel.tab == tabItem ? .bold : .medium))
                            .foregroundColor(viewModel.tab == tabItem ? palette.secondary : Slate600)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 10)

                        Rectangle()
                            .fill(viewModel.tab == tabItem ? palette.primary : Color.clear)
                            .frame(height: 2.5)
                            .cornerRadius(1.5)
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .background(Color.white)
        .overlay(
            Divider(), alignment: .bottom
        )
    }

    // MARK: - Quick Entry Area

    private var quickEntryArea: some View {
        let v = viewModel.validation
        return VStack(spacing: 10) {
            // Direct 4~5 Digit Stateroom Input & Real-Time Validation Badge
            HStack(spacing: 10) {
                HStack(spacing: 8) {
                    Image(systemName: "door.left.hand.closed")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(palette.primary)

                    if viewModel.roomInput.isEmpty {
                        Text("直输 4~5 位房号 (如 8502 / 10200)")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(Slate400)
                    } else {
                        Text(viewModel.roomInput)
                            .font(.system(size: 24, weight: .bold, design: .rounded))
                            .foregroundColor(v.status == .invalid ? NowRed : Slate900)
                    }

                    Spacer()

                    // Validation Status Pill
                    switch v.status {
                    case .valid:
                        if let validDeck = v.deck {
                            Text("✓ Deck \(validDeck)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(Color(hex: "#16A34A"))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color(hex: "#DCFCE7"))
                                .clipShape(Capsule())
                        }
                    case .invalid:
                        Text(v.message ?? "地图上无此房间，请核对")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(Color(hex: "#DC2626"))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(hex: "#FEE2E2"))
                            .clipShape(Capsule())
                    case .incomplete:
                        if !viewModel.roomInput.isEmpty {
                            Text("继续输入...")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(Slate400)
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity)
                .background(Slate100)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(
                            v.status == .invalid
                                ? Color(hex: "#DC2626").opacity(0.5)
                                : (v.status == .valid ? Color(hex: "#16A34A").opacity(0.4) : Color.clear),
                            lineWidth: 1.2
                        )
                )

                // Collapse/Expand Keypad Button
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                        viewModel.toggleKeypad()
                    }
                } label: {
                    Image(systemName: viewModel.isKeypadCollapsed ? "keyboard" : "chevron.up.circle.fill")
                        .font(.system(size: 22))
                        .foregroundColor(Slate400)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)

            // Room History Hint Banner (if available)
            if let history = viewModel.roomHistory {
                HStack(spacing: 8) {
                    if history.givenCount > 0 {
                        let givenLabel = history.lastGivenLevelName != nil
                            ? "已送 \(history.givenCount) 次 (上次: \(history.lastGivenLevelName!))"
                            : "已送 \(history.givenCount) 次"
                        Text(givenLabel)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(palette.secondary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(palette.primaryContainer.opacity(0.7))
                            .clipShape(Capsule())
                    }
                    if history.receivedCount > 0 {
                        let recLabel = history.lastReceivedLevelName != nil
                            ? "收到 \(history.receivedCount) 次 (上次: \(history.lastReceivedLevelName!))"
                            : "收到 \(history.receivedCount) 次"
                        Text(recLabel)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(Slate700)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Slate100)
                            .clipShape(Capsule())
                    }
                    if history.unrepliedCount > 0 {
                        Text("待回礼 \(history.unrepliedCount)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(Color(hex: "#DC2626"))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color(hex: "#FEE2E2"))
                            .clipShape(Capsule())
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
            }

            // Gift Level Chips Selector
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(viewModel.levels) { level in
                        Button {
                            viewModel.selectLevel(level.id)
                        } label: {
                            HStack(spacing: 6) {
                                Circle()
                                    .fill(Color(argb: level.colorArgb))
                                    .frame(width: 10, height: 10)
                                Text(level.name)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(viewModel.selectedLevelId == level.id ? palette.secondary : Slate700)

                                StarRatingView(
                                    rating: level.rank,
                                    maxStars: 3,
                                    starSize: 11,
                                    spacing: 2,
                                    activeColor: Color(hex: "#F59E0B"),
                                    inactiveColor: Slate200
                                )
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(viewModel.selectedLevelId == level.id ? palette.primaryContainer.opacity(0.6) : Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(viewModel.selectedLevelId == level.id ? palette.primary : Slate200, lineWidth: 1.2)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
            }

            // Submit Button
            let canSubmit = (v.status == .valid) && viewModel.selectedLevelId != nil
            let roomLabel = v.fullRoom ?? (viewModel.roomInput.isEmpty ? "____" : viewModel.roomInput)
            Button {
                viewModel.submitRecord()
            } label: {
                HStack {
                    Image(systemName: viewModel.tab == .given ? "paperplane.fill" : "tray.and.arrow.down.fill")
                        .font(.system(size: 14))
                    Text(viewModel.tab == .given ? "记一笔发出 (\(roomLabel))" : "记一笔收到 (\(roomLabel))")
                        .font(.system(size: 15, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .background(canSubmit ? palette.primary : Slate300)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .animation(.easeInOut(duration: 0.15), value: canSubmit)
            }
            .disabled(!canSubmit)
            .padding(.horizontal, 16)

            // 9-Key Tactile Keypad (Collapsible)
            if !viewModel.isKeypadCollapsed {
                PixieKeypadView(
                    onDigit: { viewModel.onDigit($0) },
                    onBackspace: { viewModel.onBackspace() },
                    onClear: { viewModel.onClear() }
                )
                .padding(.horizontal, 12)
                .padding(.bottom, 6)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(Color.white)
        .padding(.bottom, 6)
    }

    // MARK: - Toast Banner

    private func toastBanner(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Slate800.opacity(0.92))
            .clipShape(Capsule())
            .shadow(color: Color.black.opacity(0.15), radius: 8, x: 0, y: 4)
            .padding(.bottom, 24)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                    withAnimation {
                        viewModel.toastMessage = nil
                    }
                }
            }
    }
}

#Preview {
    PixieDustView()
}
