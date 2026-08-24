import SwiftUI

private let COMPACT_THRESHOLD: CGFloat = 44.0
private let TWO_LINE_MIN_PT: CGFloat = 42.0
private let THREE_LINE_MIN_PT: CGFloat = 58.0

/// Formats duration in minutes to human-readable string (e.g. "30m", "1h", "1h 30m").
public func formatDuration(minutes: Int) -> String {
    if minutes < 60 {
        return "\(minutes)m"
    } else if minutes % 60 == 0 {
        return "\(minutes / 60)h"
    } else {
        return "\(minutes / 60)h \(minutes % 60)m"
    }
}

/// Formats start time of an event as "HH:mm".
public func formatStart(event: CruiseEvent) -> String {
    String(format: "%02d:%02d", event.startTime.hour, event.startTime.minute)
}

/// Formats time range and duration of an event (e.g. "13:15–13:45  30m").
public func formatTimeRange(event: CruiseEvent) -> String {
    let endMin = event.endMinute
    let end = String(format: "%02d:%02d", (endMin / 60) % 24, endMin % 60)
    return "\(formatStart(event: event))–\(end)  \(formatDuration(minutes: event.durationMinutes))"
}

/// Small red badge indicating a schedule conflict.
public struct ConflictBadge: View {
    public init() {}

    public var body: some View {
        ZStack {
            Circle()
                .fill(Color(hex: "#EF4444"))
                .frame(width: 13, height: 13)
            Text("!")
                .font(.system(size: 9, weight: .black))
                .foregroundColor(.white)
        }
        .padding(.leading, 2)
    }
}

/// A timeline card representing an individual cruise event.
public struct EventCardView: View {
    public let event: CruiseEvent
    public let category: Category?
    public let heightPt: CGFloat
    public let waitSegmentHeightPt: CGFloat
    public let hasConflict: Bool
    public let narrow: Bool
    public let onClick: (() -> Void)?
    public let onLongClick: (() -> Void)?
    public let onMoveToLane: ((Int) -> Void)?
    public let onToggleActive: (() -> Void)?
    public let onKeepOnlyThisShow: (() -> Void)?
    public let onEdit: (() -> Void)?
    public let onDelete: (() -> Void)?

    public init(
        event: CruiseEvent,
        category: Category?,
        heightPt: CGFloat,
        waitSegmentHeightPt: CGFloat = 0.0,
        hasConflict: Bool = false,
        narrow: Bool = false,
        onClick: (() -> Void)? = nil,
        onLongClick: (() -> Void)? = nil,
        onMoveToLane: ((Int) -> Void)? = nil,
        onToggleActive: (() -> Void)? = nil,
        onKeepOnlyThisShow: (() -> Void)? = nil,
        onEdit: (() -> Void)? = nil,
        onDelete: (() -> Void)? = nil
    ) {
        self.event = event
        self.category = category
        self.heightPt = heightPt
        self.waitSegmentHeightPt = waitSegmentHeightPt
        self.hasConflict = hasConflict
        self.narrow = narrow
        self.onClick = onClick
        self.onLongClick = onLongClick
        self.onMoveToLane = onMoveToLane
        self.onToggleActive = onToggleActive
        self.onKeepOnlyThisShow = onKeepOnlyThisShow
        self.onEdit = onEdit
        self.onDelete = onDelete
    }

    private var colorArgb: Int64 {
        category?.colorArgb ?? 0xFF9E9E9E
    }

    private var active: Bool {
        event.isActive
    }

    private var textColor: Color {
        active ? cardTextColorFor(colorArgb: colorArgb) : Slate500
    }

    private var isCompact: Bool {
        heightPt < COMPACT_THRESHOLD
    }

    private var hasWaitSegment: Bool {
        waitSegmentHeightPt > 0
    }

    private var locationText: String? {
        formatLocation(deck: event.deck, zone: event.zone, placeName: event.placeName, compact: true)
    }

    private var cardShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
    }

    public var body: some View {
        Button(action: {
            onClick?()
        }) {
            HStack(spacing: 0) {
                // Left color stripe
                RoundedRectangle(cornerRadius: 2, style: .continuous)
                    .fill(Color(argb: colorArgb))
                    .frame(width: 3.5)
                    .opacity(active ? 1.0 : 0.45)
                    .padding(.vertical, 4)
                    .padding(.horizontal, 2)

                // Card body content
                VStack(alignment: .leading, spacing: 0) {
                    if hasWaitSegment {
                        waitSegmentView
                        Spacer().frame(height: 2)
                        waitAdaptiveBodyView
                    } else if isCompact {
                        compactBodyView
                    } else {
                        standardBodyView
                    }
                }
                .padding(.horizontal, isCompact ? 3 : 5)
                .padding(.vertical, 2)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .background(cardBackgroundFor(colorArgb: colorArgb))
            .clipShape(cardShape)
            .overlay(
                cardShape
                    .stroke(
                        Color(argb: colorArgb).opacity(active ? 0.38 : 0.2),
                        lineWidth: 1
                    )
            )
            .shadow(color: Color.black.opacity(active ? 0.04 : 0.0), radius: 2, x: 0, y: 1)
            .opacity(active ? 1.0 : 0.45)
        }
        .buttonStyle(PlainButtonStyle())
        .contextMenu {
            contextMenuItems
        }
    }

    // MARK: - Context Menu
    @ViewBuilder
    private var contextMenuItems: some View {
        if let onMoveToLane = onMoveToLane {
            Menu("切换栏位 (当前: 栏\(event.lane + 1))") {
                Button("移至 栏 1") { onMoveToLane(0) }
                Button("移至 栏 2") { onMoveToLane(1) }
                Button("移至 栏 3") { onMoveToLane(2) }
            }
        }

        if let onToggleActive = onToggleActive {
            Button(active ? "设为非活动" : "设为活动") {
                onToggleActive()
            }
        }

        if category?.iconKey == "show", let onKeepOnlyThisShow = onKeepOnlyThisShow {
            Button("只看本场演出") {
                onKeepOnlyThisShow()
            }
        }

        if let onEdit = onEdit {
            Button("编辑活动") {
                onEdit()
            }
        }

        if let onDelete = onDelete {
            Button(role: .destructive) {
                onDelete()
            } label: {
                Text("删除活动")
            }
        }
    }

    // MARK: - Wait Segment View
    @ViewBuilder
    private var waitSegmentView: some View {
        let isExtremelyCompact = waitSegmentHeightPt < 20
        let waitText = "\(formatWaitStart(event)) 候场"

        VStack(alignment: .leading, spacing: 0) {
            Text(waitText)
                .font(.system(size: isExtremelyCompact ? 9.5 : 11, weight: .bold))
                .italic()
                .foregroundColor(active ? (colorArgb == 0xFFF5B301 ? Color(hex: "#92400E") : textColor) : Slate500)
                .lineLimit(1)

            if waitSegmentHeightPt >= 36, let loc = locationText {
                Text(loc)
                    .font(.system(size: 10))
                    .italic()
                    .foregroundColor(active ? Slate700 : Slate500)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: max(14, waitSegmentHeightPt))
        .padding(.horizontal, 4)
        .padding(.vertical, isExtremelyCompact ? 0 : 2)
        .background(
            LinearGradient(
                colors: [
                    Color(argb: colorArgb).opacity(0.24),
                    Color(argb: colorArgb).opacity(0.12),
                    Color(argb: colorArgb).opacity(0.24)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
    }

    // MARK: - Wait Adaptive Body View
    @ViewBuilder
    private var waitAdaptiveBodyView: some View {
        let bodyHeight = heightPt - waitSegmentHeightPt - 2
        let bodyLines = bodyHeight >= THREE_LINE_MIN_PT ? 3 : (bodyHeight >= TWO_LINE_MIN_PT ? 2 : 1)

        VStack(alignment: .leading, spacing: 1) {
            // Line 1: Chip / Title / Conflict
            HStack(spacing: 2) {
                if let cat = category, bodyLines >= 2 {
                    CategoryChip(name: cat.name, colorArgb: colorArgb)
                }
                Text(event.title)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(active ? Slate900 : Slate500)
                    .lineLimit(1)

                if hasConflict {
                    ConflictBadge()
                }
            }

            // Line 2: Location
            if bodyLines == 3, let loc = locationText {
                Text(loc)
                    .font(.system(size: 10))
                    .foregroundColor(active ? Slate700 : Slate500)
                    .lineLimit(1)
            }

            // Line 3: Time Range
            if bodyLines >= 2 {
                Text(formatTimeRange(event: event))
                    .font(.system(size: 10))
                    .foregroundColor(active ? Slate600 : Slate500)
                    .lineLimit(1)
            }
        }
    }

    // MARK: - Compact Body View (< 44pt)
    @ViewBuilder
    private var compactBodyView: some View {
        HStack(spacing: 2) {
            Text(event.title)
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(active ? Slate900 : Slate500)
                .lineLimit(1)

            if hasConflict {
                ConflictBadge()
            }

            if !narrow {
                Spacer(minLength: 2)
                Text(formatStart(event: event))
                    .font(.system(size: 10))
                    .foregroundColor(active ? Slate600 : Slate500)
                    .lineLimit(1)
            }
        }
    }

    // MARK: - Standard Body View (>= 44pt)
    @ViewBuilder
    private var standardBodyView: some View {
        VStack(alignment: .leading, spacing: 2) {
            // Line 1: Category Chip + Title + Conflict Badge
            HStack(spacing: 4) {
                if let cat = category {
                    CategoryChip(name: cat.name, colorArgb: colorArgb)
                }
                Text(event.title)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(active ? Slate900 : Slate500)
                    .lineLimit(1)

                if hasConflict {
                    ConflictBadge()
                }
            }

            // Line 2: Location
            if let loc = locationText {
                Text(loc)
                    .font(.system(size: 10))
                    .foregroundColor(active ? Slate700 : Slate500)
                    .lineLimit(1)
            }

            // Line 3: Time Range
            Text(formatTimeRange(event: event))
                .font(.system(size: 10))
                .foregroundColor(active ? Slate600 : Slate500)
                .lineLimit(1)
        }
    }
}
