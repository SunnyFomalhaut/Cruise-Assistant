import SwiftUI

/// 发出礼物记录列表视图（支持房间归组模式与流水流模式）
public struct GivenListView: View {
    public let records: [PixieDustRecord]
    public let grouped: [RoomGroup]
    public let levels: [PixieLevel]
    public let viewMode: GivenViewMode
    public let onToggleViewMode: () -> Void
    public let onSelectRecord: (PixieDustRecord) -> Void
    public let onSelectRoomGroup: (RoomGroup) -> Void
    public let onEditRecord: (PixieDustRecord) -> Void
    public let onDeleteRecord: (PixieDustRecord) -> Void

    public init(
        records: [PixieDustRecord],
        grouped: [RoomGroup],
        levels: [PixieLevel],
        viewMode: GivenViewMode,
        onToggleViewMode: @escaping () -> Void,
        onSelectRecord: @escaping (PixieDustRecord) -> Void = { _ in },
        onSelectRoomGroup: @escaping (RoomGroup) -> Void = { _ in },
        onEditRecord: @escaping (PixieDustRecord) -> Void,
        onDeleteRecord: @escaping (PixieDustRecord) -> Void
    ) {
        self.records = records
        self.grouped = grouped
        self.levels = levels
        self.viewMode = viewMode
        self.onToggleViewMode = onToggleViewMode
        self.onSelectRecord = onSelectRecord
        self.onSelectRoomGroup = onSelectRoomGroup
        self.onEditRecord = onEditRecord
        self.onDeleteRecord = onDeleteRecord
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Header bar with view mode toggle
            HStack {
                Text("发出记录 (\(records.count))")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Slate600)

                Spacer()

                Button(action: onToggleViewMode) {
                    HStack(spacing: 4) {
                        Image(systemName: viewMode == .grouped ? "square.grid.2x2" : "list.bullet")
                            .font(.system(size: 13, weight: .medium))
                        Text(viewMode == .grouped ? "按房间归组" : "时间流水")
                            .font(.system(size: 13, weight: .medium))
                    }
                    .foregroundColor(CruiseAzure)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(CruiseAzureContainer.opacity(0.4))
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if records.isEmpty {
                emptyView
            } else if viewMode == .grouped {
                groupedListView
            } else {
                flowListView
            }
        }
    }

    // MARK: - Grouped View

    private var groupedListView: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                ForEach(grouped) { group in
                    RoomGroupCard(
                        group: group,
                        levels: levels,
                        onTap: { onSelectRoomGroup(group) }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
    }

    // MARK: - Flow List View

    private var flowListView: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                ForEach(records) { record in
                    GivenRecordCard(
                        record: record,
                        levels: levels,
                        onTap: { onSelectRecord(record) },
                        onEdit: { onEditRecord(record) },
                        onDelete: { onDeleteRecord(record) }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
    }

    // MARK: - Empty View

    private var emptyView: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "gift")
                .font(.system(size: 44))
                .foregroundColor(Slate300)
            Text("暂无发出记录")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(Slate500)
            Text("输入房号并选择礼物等级，点击「记一笔发出」开始记录")
                .font(.system(size: 13))
                .foregroundColor(Slate400)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, minHeight: 200)
    }
}

// MARK: - RoomGroupCard

public struct RoomGroupCard: View {
    public let group: RoomGroup
    public let levels: [PixieLevel]
    public let onTap: () -> Void

    public var body: some View {
        Button(action: onTap) {
            HStack(alignment: .center, spacing: 12) {
                // Room Number Badge
                VStack(alignment: .leading, spacing: 2) {
                    Text(group.fullRoom)
                        .font(.system(size: 17, weight: .bold, design: .rounded))
                        .foregroundColor(Slate900)
                    Text("Deck \(group.deck)")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(Slate500)
                }
                .frame(minWidth: 64, alignment: .leading)

                // Level Counts Summary
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        ForEach(group.levelCounts, id: \.level.id) { item in
                            HStack(spacing: 3) {
                                Circle()
                                    .fill(Color(argb: item.level.colorArgb))
                                    .frame(width: 8, height: 8)
                                Text("\(item.level.name)×\(item.count)")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundColor(Slate700)
                            }
                        }
                    }
                    if group.lastTimestamp > 0 {
                        Text("最近: \(formatDate(group.lastTimestamp))")
                            .font(.system(size: 11))
                            .foregroundColor(Slate400)
                    }
                }

                Spacer()

                // Total Count Badge
                HStack(spacing: 4) {
                    Text("共 \(group.total) 次")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(CruiseMarine)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Slate400)
                }
            }
            .padding(14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Slate200, lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.02), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }

    private func formatDate(_ ts: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(ts) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "M月d日 HH:mm"
        return formatter.string(from: date)
    }
}

// MARK: - GivenRecordCard

public struct GivenRecordCard: View {
    public let record: PixieDustRecord
    public let levels: [PixieLevel]
    public let onTap: () -> Void
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

            // Menu button for edit/delete
            Menu {
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
                    .padding(8)
                    .contentShape(Rectangle())
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Slate200, lineWidth: 1)
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

public enum GivenViewMode: String, CaseIterable, Codable, Sendable {
    case grouped
    case flow
}
