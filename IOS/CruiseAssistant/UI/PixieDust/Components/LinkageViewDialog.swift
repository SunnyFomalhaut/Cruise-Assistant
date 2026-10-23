import SwiftUI

/// 仙尘互赠联动检视视图（按甲板折叠分组、四色状态徽标、一键跳转本层仙尘地图）
public struct LinkageViewDialog: View {
    public let roomPairs: [RoomPair]
    public let allRoomPairs: [RoomPair]
    public let levels: [PixieLevel]
    public let filter: PixieFilter
    public let onSelectFilter: ((PixieFilter) -> Void)?
    public let selectedPair: RoomPair?
    public let onNavigateToDeckMap: ((Int, [PixieRoomMarker]) -> Void)?
    public let onDismiss: () -> Void

    @Environment(\.cruisePalette) private var palette
    @State private var expandedDecks: Set<Int> = []

    public init(
        roomPairs: [RoomPair],
        allRoomPairs: [RoomPair]? = nil,
        levels: [PixieLevel],
        filter: PixieFilter = .all,
        onSelectFilter: ((PixieFilter) -> Void)? = nil,
        selectedPair: RoomPair? = nil,
        onNavigateToDeckMap: ((Int, [PixieRoomMarker]) -> Void)? = nil,
        onDismiss: @escaping () -> Void
    ) {
        self.roomPairs = roomPairs
        self.allRoomPairs = allRoomPairs ?? roomPairs
        self.levels = levels
        self.filter = filter
        self.onSelectFilter = onSelectFilter
        self.selectedPair = selectedPair
        self.onNavigateToDeckMap = onNavigateToDeckMap
        self.onDismiss = onDismiss
    }

    private var groupedByDeck: [(deck: Int, pairs: [RoomPair])] {
        let grouped = Dictionary(grouping: roomPairs, by: \.deck)
        return grouped.keys.sorted().map { d in
            (deck: d, pairs: grouped[d] ?? [])
        }
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if let pair = selectedPair {
                    singlePairDetailView(pair)
                } else {
                    shipWideSummaryBar
                    if roomPairs.isEmpty {
                        emptyView
                    } else {
                        allPairsListView
                    }
                }
            }
            .background(Slate50)
            .navigationTitle(selectedPair != nil ? "房间互赠明细" : "互赠联动检视")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", action: onDismiss)
                        .foregroundColor(palette.primary)
                }
            }
        }
    }

    // MARK: - Ship-Wide Summary Bar

    private var shipWideSummaryBar: some View {
        let unrepliedCount = allRoomPairs.filter { $0.roomStatus == .unreplied }.count
        let supplementCount = allRoomPairs.filter { $0.roomStatus == .needSupplement }.count
        let completedCount = allRoomPairs.filter { $0.roomStatus == .completed }.count
        let givenPendingCount = allRoomPairs.filter { $0.roomStatus == .givenPending }.count

        return VStack(spacing: 8) {
            HStack(spacing: 8) {
                Text("全船共 \(allRoomPairs.count) 间")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Slate800)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        if unrepliedCount > 0 {
                            statusMiniPill(label: "待回 \(unrepliedCount)", color: Color(hex: "#DC2626"), bg: Color(hex: "#FEE2E2"))
                        }
                        if supplementCount > 0 {
                            statusMiniPill(label: "需补 \(supplementCount)", color: Color(hex: "#D97706"), bg: Color(hex: "#FEF3C7"))
                        }
                        if completedCount > 0 {
                            statusMiniPill(label: "持平 \(completedCount)", color: Color(hex: "#16A34A"), bg: Color(hex: "#DCFCE7"))
                        }
                        if givenPendingCount > 0 {
                            statusMiniPill(label: "已发 \(givenPendingCount)", color: Color(hex: "#0284C7"), bg: Color(hex: "#E0F2FE"))
                        }
                    }
                }

                Spacer()

                if let onSelectFilter = onSelectFilter {
                    HStack(spacing: 4) {
                        ForEach(PixieFilter.allCases) { item in
                            Button {
                                onSelectFilter(item)
                            } label: {
                                Text(item.rawValue)
                                    .font(.system(size: 11, weight: filter == item ? .bold : .medium))
                                    .foregroundColor(filter == item ? palette.secondary : Slate600)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(filter == item ? palette.primaryContainer : Slate100)
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color.white)
            .overlay(Divider(), alignment: .bottom)
        }
    }

    private func statusMiniPill(label: String, color: Color, bg: Color) -> some View {
        HStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 6, height: 6)
            Text(label)
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(color)
        }
        .padding(.horizontal, 7)
        .padding(.vertical, 3)
        .background(bg)
        .clipShape(Capsule())
    }

    // MARK: - Collapsible Deck-Grouped Pairs View

    private var allPairsListView: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(groupedByDeck, id: \.deck) { section in
                    deckSectionView(deck: section.deck, pairs: section.pairs)
                }
            }
            .padding(16)
        }
    }

    private func deckSectionView(deck: Int, pairs: [RoomPair]) -> some View {
        let isExpanded = expandedDecks.contains(deck)
        let unreplied = pairs.filter { $0.roomStatus == .unreplied }.count
        let supplement = pairs.filter { $0.roomStatus == .needSupplement }.count
        let completed = pairs.filter { $0.roomStatus == .completed }.count
        let givenPending = pairs.filter { $0.roomStatus == .givenPending }.count

        return VStack(spacing: 8) {
            // Deck Header Bar
            VStack(spacing: 8) {
                Button {
                    withAnimation(.spring(response: 0.28, dampingFraction: 0.8)) {
                        if isExpanded {
                            expandedDecks.remove(deck)
                        } else {
                            expandedDecks.insert(deck)
                        }
                    }
                } label: {
                    HStack(spacing: 8) {
                        Text("Deck \(deck)")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundColor(Slate900)

                        Text("(\(pairs.count) 间)")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(Slate500)

                        HStack(spacing: 4) {
                            if unreplied > 0 {
                                statusMiniPill(label: "待回 \(unreplied)", color: Color(hex: "#DC2626"), bg: Color(hex: "#FEE2E2"))
                            }
                            if supplement > 0 {
                                statusMiniPill(label: "需补 \(supplement)", color: Color(hex: "#D97706"), bg: Color(hex: "#FEF3C7"))
                            }
                            if completed > 0 {
                                statusMiniPill(label: "持平 \(completed)", color: Color(hex: "#16A34A"), bg: Color(hex: "#DCFCE7"))
                            }
                            if givenPending > 0 {
                                statusMiniPill(label: "已发 \(givenPending)", color: Color(hex: "#0284C7"), bg: Color(hex: "#E0F2FE"))
                            }
                        }

                        Spacer()

                        Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Slate400)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if let onNavigateToDeckMap = onNavigateToDeckMap {
                    let deckAllPairs = allRoomPairs.filter { $0.deck == deck }
                    let markers = (deckAllPairs.isEmpty ? pairs : deckAllPairs).map { $0.toPixieRoomMarker() }
                    Button {
                        onNavigateToDeckMap(deck, markers)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "map.fill")
                                .font(.system(size: 12))
                            Text("去 Deck \(deck) 甲板地图查看四色标记")
                                .font(.system(size: 12, weight: .semibold))
                            Spacer()
                            Image(systemName: "arrow.right")
                                .font(.system(size: 11, weight: .bold))
                        }
                        .foregroundColor(palette.secondary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 7)
                        .background(palette.primaryContainer.opacity(0.65))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(unreplied > 0 ? Color(hex: "#DC2626").opacity(0.35) : Slate200, lineWidth: 1)
            )

            if isExpanded {
                VStack(spacing: 8) {
                    ForEach(pairs) { pair in
                        RoomPairCard(pair: pair, levels: levels)
                            .padding(.leading, 8)
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    // MARK: - Single Pair Detail View

    private func singlePairDetailView(_ pair: RoomPair) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                // Header card
                VStack(spacing: 8) {
                    Text(pair.fullRoom)
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(Slate900)
                    Text("Deck \(pair.deck) 甲板")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate500)

                    HStack(spacing: 16) {
                        HStack(spacing: 4) {
                            Text("发出: \(pair.given.count)")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(CruiseMarine)
                        }
                        Text("•")
                            .foregroundColor(Slate300)
                        HStack(spacing: 4) {
                            Text("收到: \(pair.received.count)")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(Slate700)
                        }
                        if !pair.unrepliedReceived.isEmpty {
                            Text("•")
                            .foregroundColor(Slate300)
                            Text("\(pair.unrepliedReceived.count) 待回礼")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(NowRed)
                        }
                    }
                    .padding(.top, 4)

                    if let onNavigateToDeckMap = onNavigateToDeckMap {
                        let deckAllPairs = allRoomPairs.filter { $0.deck == pair.deck }
                        let markers = (deckAllPairs.isEmpty ? [pair] : deckAllPairs).map { $0.toPixieRoomMarker() }
                        Button {
                            onDismiss()
                            onNavigateToDeckMap(pair.deck, markers)
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: "map.fill")
                                Text("在 Deck \(pair.deck) 地图中定位")
                            }
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(palette.secondary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(palette.primaryContainer)
                            .clipShape(Capsule())
                            .padding(.top, 4)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(20)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Slate200, lineWidth: 1)
                )

                // Given Section
                VStack(alignment: .leading, spacing: 8) {
                    Text("发出记录 (\(pair.given.count))")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Slate800)
                        .padding(.horizontal, 4)

                    if pair.given.isEmpty {
                        Text("尚未向该房间发出过礼物")
                            .font(.system(size: 13))
                            .foregroundColor(Slate400)
                            .padding(12)
                    } else {
                        VStack(spacing: 8) {
                            ForEach(pair.given) { record in
                                pairRecordRow(record: record, isGiven: true)
                            }
                        }
                    }
                }

                // Received Section
                VStack(alignment: .leading, spacing: 8) {
                    Text("收到记录 (\(pair.received.count))")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Slate800)
                        .padding(.horizontal, 4)

                    if pair.received.isEmpty {
                        Text("尚未收到该房间的礼物")
                            .font(.system(size: 13))
                            .foregroundColor(Slate400)
                            .padding(12)
                    } else {
                        VStack(spacing: 8) {
                            ForEach(pair.received) { record in
                                pairRecordRow(record: record, isGiven: false)
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }

    private func pairRecordRow(record: PixieDustRecord, isGiven: Bool) -> some View {
        HStack {
            Circle()
                .fill(levelColor(for: record))
                .frame(width: 8, height: 8)
            Text(displayLevelName(for: record))
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(Slate800)

            if !record.note.isEmpty {
                Text("· \(record.note)")
                    .font(.system(size: 13))
                    .foregroundColor(Slate500)
                    .lineLimit(1)
            }

            Spacer()

            if !isGiven {
                Text(record.replied ? "已回礼" : "待回礼")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(record.replied ? Slate500 : NowRed)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(record.replied ? Slate100 : NowRed.opacity(0.1))
                    .clipShape(Capsule())
            }

            if record.timestamp > 0 {
                Text(formatDate(record.timestamp))
                    .font(.system(size: 11))
                    .foregroundColor(Slate400)
            }
        }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(Slate200, lineWidth: 1)
        )
    }

    // MARK: - Empty View

    private var emptyView: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "arrow.triangle.swap")
                .font(.system(size: 44))
                .foregroundColor(Slate300)
            Text(filter == .pendingOnly ? "暂无待回礼或需补房间" : "暂无互赠记录")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(Slate500)
            Text("发出或收到仙尘礼物后，可在此查看各甲板互赠与待回礼明细")
                .font(.system(size: 13))
                .foregroundColor(Slate400)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
        }
    }

    private func displayLevelName(for record: PixieDustRecord) -> String {
        levels.first(where: { $0.id == record.levelId })?.name ?? record.levelNameSnapshot
    }

    private func levelColor(for record: PixieDustRecord) -> Color {
        if let lvl = levels.first(where: { $0.id == record.levelId }) {
            return Color(argb: lvl.colorArgb)
        }
        return Color(argb: 0xFF9E9E9E)
    }

    private func formatDate(_ ts: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(ts) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "M月d日"
        return formatter.string(from: date)
    }
}

// MARK: - RoomPairCard

public struct RoomPairCard: View {
    public let pair: RoomPair
    public let levels: [PixieLevel]

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Room Header
            HStack {
                Text(pair.fullRoom)
                    .font(.system(size: 17, weight: .bold, design: .rounded))
                    .foregroundColor(Slate900)
                Text("Deck \(pair.deck)")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(Slate500)

                Spacer()

                statusBadge
            }

            Divider()

            // Given vs Received breakdown
            HStack(alignment: .top, spacing: 12) {
                // Given summary
                VStack(alignment: .leading, spacing: 4) {
                    Text("发出 (\(pair.given.count))")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Slate600)
                    Text(levelCountsSummary(pair.given))
                        .font(.system(size: 12))
                        .foregroundColor(Slate700)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Text("↔")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Slate400)
                    .padding(.top, 4)

                // Received summary
                VStack(alignment: .leading, spacing: 4) {
                    Text("收到 (\(pair.received.count))")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Slate600)
                    Text(levelCountsSummary(pair.received))
                        .font(.system(size: 12))
                        .foregroundColor(Slate700)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(pair.unrepliedReceived.isEmpty ? Slate200 : NowRed.opacity(0.3), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.02), radius: 4, x: 0, y: 2)
    }

    @ViewBuilder
    private var statusBadge: some View {
        switch pair.roomStatus {
        case .unreplied:
            Text("🔴 待回礼 \(pair.unrepliedReceived.count)")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(Color(hex: "#DC2626"))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Color(hex: "#FEE2E2"))
                .clipShape(Capsule())
        case .needSupplement:
            Text("🟡 需补 \(pair.received.count - pair.given.count)")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(Color(hex: "#D97706"))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Color(hex: "#FEF3C7"))
                .clipShape(Capsule())
        case .completed:
            Text("🟢 双向持平")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Color(hex: "#16A34A"))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Color(hex: "#DCFCE7"))
                .clipShape(Capsule())
        case .givenPending:
            Text("🔵 已发待收")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Color(hex: "#0284C7"))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Color(hex: "#E0F2FE"))
                .clipShape(Capsule())
        }
    }

    private func levelCountsSummary(_ records: [PixieDustRecord]) -> String {
        let counts = levelCountsOf(records: records, levels: levels)
        let str = counts.map { "\($0.level.name)×\($0.count)" }.joined(separator: " ")
        return str.isEmpty ? "—" : str
    }
}
