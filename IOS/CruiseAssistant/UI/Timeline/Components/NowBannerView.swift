import SwiftUI

/// Sticky banner displayed at top of timeline when viewing today's schedule.
/// Shows current ongoing event and upcoming next event with quick-jump action.
public struct NowBannerView: View {
    public let ongoing: CruiseEvent?
    public let next: CruiseEvent?
    public let categories: [Int64: Category]
    public let onSelectEvent: ((CruiseEvent) -> Void)?

    public init(
        ongoing: CruiseEvent?,
        next: CruiseEvent?,
        categories: [Int64: Category],
        onSelectEvent: ((CruiseEvent) -> Void)? = nil
    ) {
        self.ongoing = ongoing
        self.next = next
        self.categories = categories
        self.onSelectEvent = onSelectEvent
    }

    public var body: some View {
        if ongoing != nil || next != nil {
            VStack(spacing: 6) {
                if let ongoingEvent = ongoing {
                    BannerRow(
                        label: "正在进行",
                        isOngoing: true,
                        event: ongoingEvent,
                        category: categories[ongoingEvent.categoryId],
                        onTap: { onSelectEvent?(ongoingEvent) }
                    )
                }

                if let nextEvent = next {
                    BannerRow(
                        label: "接下来",
                        isOngoing: false,
                        event: nextEvent,
                        category: categories[nextEvent.categoryId],
                        onTap: { onSelectEvent?(nextEvent) }
                    )
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(SurfaceCard)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(CruiseAzureContainer, lineWidth: 1)
            )
            .padding(.horizontal, 14)
            .padding(.vertical, 4)
        }
    }
}

/// A row inside the NowBanner presenting a single event summary.
private struct BannerRow: View {
    let label: String
    let isOngoing: Bool
    let event: CruiseEvent
    let category: Category?
    let onTap: () -> Void

    private var locationText: String? {
        formatLocation(deck: event.deck, zone: event.zone, placeName: event.placeName, compact: true)
    }

    private var displayTitle: String {
        event.title.isEmpty ? (category?.name ?? "未命名活动") : event.title
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                // Status badge
                HStack(spacing: 4) {
                    Circle()
                        .fill(isOngoing ? Color(hex: "#10B981") : Slate400)
                        .frame(width: 6, height: 6)

                    Text(label)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(isOngoing ? CruiseMarine : Slate500)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(isOngoing ? CruiseAzureContainer : Slate100)
                .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))

                // Time label
                Text(formatStart(event: event))
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(isOngoing ? CruiseMarine : Slate900)

                // Title
                Text(displayTitle)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(Slate900)
                    .lineLimit(1)

                // Location
                if let loc = locationText {
                    Spacer(minLength: 4)
                    Text(loc)
                        .font(.system(size: 11, weight: .regular))
                        .foregroundColor(Slate500)
                        .lineLimit(1)
                } else {
                    Spacer(minLength: 0)
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}
