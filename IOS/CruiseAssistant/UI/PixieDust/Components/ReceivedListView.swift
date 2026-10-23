import SwiftUI

/// 收到礼物记录列表视图（扫楼顺路优先排序，支持全部/仅待回礼筛选与回礼勾选）
public struct ReceivedListView: View {
    public let records: [PixieDustRecord]
    public let allRecords: [PixieDustRecord]
    public let levels: [PixieLevel]
    public let filter: PixieFilter
    public let onSelectFilter: ((PixieFilter) -> Void)?
    public let onSelectRecord: (PixieDustRecord) -> Void
    public let onToggleReplied: (PixieDustRecord) -> Void
    public let onEditRecord: (PixieDustRecord) -> Void
    public let onDeleteRecord: (PixieDustRecord) -> Void

    @Environment(\.cruisePalette) private var palette

    public init(
        records: [PixieDustRecord],
        allRecords: [PixieDustRecord]? = nil,
        levels: [PixieLevel],
        filter: PixieFilter = .all,
        onSelectFilter: ((PixieFilter) -> Void)? = nil,
        onSelectRecord: @escaping (PixieDustRecord) -> Void = { _ in },
        onToggleReplied: @escaping (PixieDustRecord) -> Void,
        onEditRecord: @escaping (PixieDustRecord) -> Void,
        onDeleteRecord: @escaping (PixieDustRecord) -> Void
    ) {
        self.records = records
        self.allRecords = allRecords ?? records
        self.levels = levels
        self.filter = filter
        self.onSelectFilter = onSelectFilter
        self.onSelectRecord = onSelectRecord
        self.onToggleReplied = onToggleReplied
        self.onEditRecord = onEditRecord
        self.onDeleteRecord = onDeleteRecord
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Header stats & filter bar
            HStack(spacing: 8) {
                let unrepliedCount = allRecords.filter { !$0.replied }.count
                Text("收到记录 (\(allRecords.count))")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Slate600)

                if unrepliedCount > 0 {
                    HStack(spacing: 4) {
                        Circle()
                            .fill(NowRed)
                            .frame(width: 6, height: 6)
                        Text("\(unrepliedCount) 待回礼")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(NowRed)
                    }
                    .padding(.horizontal, 7)
                    .padding(.vertical, 2)
                    .background(NowRed.opacity(0.1))
                    .clipShape(Capsule())
                }

                Spacer()

                if let onSelectFilter = onSelectFilter {
                    HStack(spacing: 4) {
                        ForEach(PixieFilter.allCases) { item in
                            Button {
                                onSelectFilter(item)
                            } label: {
                                Text(item.rawValue)
                                    .font(.system(size: 12, weight: filter == item ? .bold : .medium))
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
            .padding(.vertical, 8)

            if records.isEmpty {
                emptyView
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(records) { record in
                            ReceivedRecordCard(
                                record: record,
                                levels: levels,
                                onTap: { onSelectRecord(record) },
                                onToggleReplied: { onToggleReplied(record) },
                                onEdit: { onEditRecord(record) },
                                onDelete: { onDeleteRecord(record) }
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 24)
                }
            }
        }
    }

    private var emptyView: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: filter == .pendingOnly ? "checkmark.seal" : "tray")
                .font(.system(size: 44))
                .foregroundColor(Slate300)
            Text(filter == .pendingOnly ? "当前没有待回礼记录" : "暂无收到记录")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(Slate500)
            Text(filter == .pendingOnly ? "所有收到的仙尘礼物均已回礼" : "收到别间魔法礼物时，在上方录入房号与等级")
                .font(.system(size: 13))
                .foregroundColor(Slate400)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, minHeight: 200)
    }
}

// MARK: - ReceivedRecordCard

public struct ReceivedRecordCard: View {
    public let record: PixieDustRecord
    public let levels: [PixieLevel]
    public let onTap: () -> Void
    public let onToggleReplied: () -> Void
    public let onEdit: () -> Void
    public let onDelete: () -> Void

    public var body: some View {
        HStack(alignment: .center, spacing: 12) {
            // Room badge
            VStack(alignment: .leading, spacing: 2) {
                Text(record.fullRoom)
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundColor(Slate900)
                Text("Deck \(record.deck)")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(Slate500)
            }
            .frame(minWidth: 60, alignment: .leading)

            // Level & Note Info
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Circle()
                        .fill(levelColor)
                        .frame(width: 8, height: 8)
                    Text(displayLevelName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Slate800)

                    StarRatingView(
                        rating: currentRank,
                        maxStars: 3,
                        starSize: 12,
                        spacing: 2,
                        activeColor: Color(hex: "#F59E0B"),
                        inactiveColor: Slate200
                    )
                }

                if !record.note.isEmpty {
                    Text(record.note)
                        .font(.system(size: 12))
                        .foregroundColor(Slate600)
                        .lineLimit(1)
                }

                if record.timestamp > 0 {
                    Text(formatDate(record.timestamp))
                        .font(.system(size: 11))
                        .foregroundColor(Slate400)
                }
            }

            Spacer()

            // Replied Toggle Button
            Button(action: onToggleReplied) {
                HStack(spacing: 4) {
                    Image(systemName: record.replied ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 14, weight: .semibold))
                    Text(record.replied ? "已回礼" : "待回礼")
                        .font(.system(size: 12, weight: .semibold))
                }
                .foregroundColor(record.replied ? Slate500 : CruiseMarine)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(record.replied ? Slate100 : CruiseAzureContainer.opacity(0.6))
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(record.replied ? Slate200 : CruiseAzure.opacity(0.3), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)

            // Menu button for edit/delete
            Menu {
                Button(action: onToggleReplied) {
                    Label(record.replied ? "标记为未回礼" : "标记为已回礼", systemImage: record.replied ? "circle" : "checkmark.circle")
                }
                Button(action: onEdit) {
                    Label("编辑记录", systemImage: "pencil")
                }
                Button(role: .destructive, action: onDelete) {
                    Label("删除记录", systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Slate400)
                    .padding(6)
                    .contentShape(Rectangle())
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(record.replied ? Slate200 : CruiseAzure.opacity(0.35), lineWidth: record.replied ? 1 : 1.5)
        )
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive, action: onDelete) {
                Label("删除", systemImage: "trash")
            }
            Button(action: onEdit) {
                Label("编辑", systemImage: "pencil")
            }
            .tint(CruiseAzure)
        }
        .contextMenu {
            Button(action: onToggleReplied) {
                Label(record.replied ? "标记为未回礼" : "标记为已回礼", systemImage: record.replied ? "circle" : "checkmark.circle")
            }
            Button(action: onEdit) {
                Label("编辑", systemImage: "pencil")
            }
            Button(role: .destructive, action: onDelete) {
                Label("删除", systemImage: "trash")
            }
        }
    }

    private var displayLevelName: String {
        levels.first(where: { $0.id == record.levelId })?.name ?? record.levelNameSnapshot
    }

    private var levelColor: Color {
        if let lvl = levels.first(where: { $0.id == record.levelId }) {
            return Color(argb: lvl.colorArgb)
        }
        return Color(argb: 0xFF9E9E9E)
    }

    private var currentRank: Int {
        levels.first(where: { $0.id == record.levelId })?.rank ?? 1
    }

    private func formatDate(_ ts: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(ts) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "M月d日 HH:mm"
        return formatter.string(from: date)
    }
}
