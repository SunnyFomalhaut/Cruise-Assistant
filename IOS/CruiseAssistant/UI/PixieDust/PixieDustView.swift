import SwiftUI

/// 仙尘（Pixie Dust）礼物互赠追踪主界面
public struct PixieDustView: View {
    @StateObject private var viewModel: PixieDustViewModel

    public init(viewModel: PixieDustViewModel? = nil) {
        if let vm = viewModel {
            _viewModel = StateObject(wrappedValue: vm)
        } else {
            _viewModel = StateObject(wrappedValue: PixieDustViewModel())
        }
    }

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
                        levels: viewModel.levels,
                        selectedPair: nil,
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
                                    if let pair = viewModel.roomPairs.first(where: { $0.deck == record.deck && $0.room == record.room }) {
                                        viewModel.viewingPair = pair
                                    }
                                },
                                onSelectRoomGroup: { group in
                                    if let pair = viewModel.roomPairs.first(where: { $0.deck == group.deck && $0.room == group.room }) {
                                        viewModel.viewingPair = pair
                                    }
                                },
                                onEditRecord: { viewModel.editingRecord = $0 },
                                onDeleteRecord: { viewModel.deleteRecord($0) }
                            )
                        } else {
                            ReceivedListView(
                                records: viewModel.receivedRecords,
                                levels: viewModel.levels,
                                onSelectRecord: { record in
                                    if let pair = viewModel.roomPairs.first(where: { $0.deck == record.deck && $0.room == record.room }) {
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
                                .foregroundColor(CruiseAzure)
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
                    levels: viewModel.levels,
                    selectedPair: pair,
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
                            .foregroundColor(viewModel.tab == tabItem ? CruiseMarine : Slate600)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 10)

                        Rectangle()
                            .fill(viewModel.tab == tabItem ? CruiseAzure : Color.clear)
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
        VStack(spacing: 10) {
            // Deck & Room input display
            HStack(spacing: 12) {
                // Deck Menu Picker
                Menu {
                    ForEach(viewModel.availableDecks, id: \.self) { d in
                        Button {
                            viewModel.selectDeck(d)
                        } label: {
                            HStack {
                                Text("Deck \(d)")
                                if viewModel.deck == d {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Text("D\(viewModel.deck)")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(CruiseMarine)
                        Image(systemName: "chevron.down")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(CruiseMarine)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(CruiseAzureContainer.opacity(0.6))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }

                // Room Display Slots
                HStack(spacing: 6) {
                    Text("\(viewModel.deck)")
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                        .foregroundColor(Slate400)

                    let digits = Array(viewModel.roomInput)
                    ForEach(0..<3, id: \.self) { idx in
                        Text(idx < digits.count ? String(digits[idx]) : "•")
                            .font(.system(size: 24, weight: .bold, design: .rounded))
                            .foregroundColor(idx < digits.count ? Slate900 : Slate300)
                            .frame(minWidth: 20)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Slate100)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                // Room History Hint (if available)
                if let history = viewModel.roomHistory {
                    VStack(alignment: .leading, spacing: 1) {
                        Text("上次: \(history.levelName)")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(CruiseMarine)
                        Text("已送 \(history.count) 次")
                            .font(.system(size: 10))
                            .foregroundColor(Slate500)
                    }
                    .padding(.horizontal, 6)
                }

                Spacer()

                // Collapse/Expand Keypad Button
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                        viewModel.toggleKeypad()
                    }
                } label: {
                    Image(systemName: viewModel.isKeypadCollapsed ? "keyboard" : "chevron.up.circle.fill")
                        .font(.system(size: 20))
                        .foregroundColor(Slate400)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)

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
                                    .foregroundColor(viewModel.selectedLevelId == level.id ? CruiseMarine : Slate700)

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
                            .background(viewModel.selectedLevelId == level.id ? CruiseAzureContainer.opacity(0.6) : Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(viewModel.selectedLevelId == level.id ? CruiseAzure : Slate200, lineWidth: 1.2)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
            }

            // Submit Button
            let canSubmit = isValidRoomInput(viewModel.roomInput) && viewModel.selectedLevelId != nil
            Button {
                viewModel.submitRecord()
            } label: {
                HStack {
                    Image(systemName: viewModel.tab == .given ? "paperplane.fill" : "tray.and.arrow.down.fill")
                        .font(.system(size: 14))
                    Text(viewModel.tab == .given ? "记一笔发出 (\(viewModel.deck)\(viewModel.roomInput.isEmpty ? "___" : viewModel.roomInput))" : "记一笔收到 (\(viewModel.deck)\(viewModel.roomInput.isEmpty ? "___" : viewModel.roomInput))")
                        .font(.system(size: 15, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .background(canSubmit ? CruiseAzure : Slate300)
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
