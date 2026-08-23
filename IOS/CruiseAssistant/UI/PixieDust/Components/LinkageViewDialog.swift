import SwiftUI

/// 仙尘互赠联动检视视图（显示发出↔收到配对明细与回礼状态）
public struct LinkageViewDialog: View {
    public let roomPairs: [RoomPair]
    public let levels: [PixieLevel]
    public let selectedPair: RoomPair?
    public let onDismiss: () -> Void

    public init(
        roomPairs: [RoomPair],
        levels: [PixieLevel],
        selectedPair: RoomPair? = nil,
        onDismiss: @escaping () -> Void
    ) {
        self.roomPairs = roomPairs
        self.levels = levels
        self.selectedPair = selectedPair
        self.onDismiss = onDismiss
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if let pair = selectedPair {
                    singlePairDetailView(pair)
                } else if roomPairs.isEmpty {
                    emptyView
                } else {
                    allPairsListView
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
                        .foregroundColor(CruiseAzure)
                }
            }
        }
    }

    // MARK: - All Pairs List View

    private var allPairsListView: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(roomPairs) { pair in
                    RoomPairCard(pair: pair, levels: levels)
                }
            }
            .padding(16)
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
                            Text("\(pair.unrepliedReceived.count) 间待回礼")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(NowRed)
                        }
                    }
                    .padding(.top, 4)
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
            Text("暂无互赠记录")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(Slate500)
            Text("发出或收到仙尘礼物后，可在此查看各房间互赠与待回礼明细")
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

                if !pair.unrepliedReceived.isEmpty {
                    Text("\(pair.unrepliedReceived.count) 间待回礼")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(NowRed)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(NowRed.opacity(0.1))
                        .clipShape(Capsule())
                } else if pair.isMutual {
                    Text("双向互赠")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(CruiseMarine)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(CruiseAzureContainer.opacity(0.5))
                        .clipShape(Capsule())
                }
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

    private func levelCountsSummary(_ records: [PixieDustRecord]) -> String {
        let counts = levelCountsOf(records: records, levels: levels)
        let str = counts.map { "\($0.level.name)×\($0.count)" }.joined(separator: " ")
        return str.isEmpty ? "—" : str
    }
}
