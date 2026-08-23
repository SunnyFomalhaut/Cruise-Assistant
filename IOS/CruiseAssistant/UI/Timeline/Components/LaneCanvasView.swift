import SwiftUI

private let LANE_WEIGHTS: [Double] = [1.0, 1.0, 1.0]
private let LANE_GAP: CGFloat = 3.0
private let MIN_CARD_HEIGHT: CGFloat = 22.0
private let TITLE_RESERVE_PT: CGFloat = 26.0
private let SPACER_PT: CGFloat = 2.0

/// Red "Now" indicator line spanning across the timeline canvas.
public struct NowIndicatorView: View {
    public let layout: TimelineLayout
    public let nowMinute: Int

    public init(layout: TimelineLayout, nowMinute: Int) {
        self.layout = layout
        self.nowMinute = nowMinute
    }

    public var body: some View {
        if nowMinute >= layout.rangeStartMinute && nowMinute <= layout.rangeEndMinute {
            let y = CGFloat(layout.minuteToPt(nowMinute))

            ZStack(alignment: .leading) {
                // Horizontal line
                Rectangle()
                    .fill(NowRed)
                    .frame(height: 2)

                // Leading dot
                Circle()
                    .fill(NowRed)
                    .frame(width: 8, height: 8)
                    .offset(x: -4)
            }
            .frame(height: 8)
            .offset(y: y - 4)
        }
    }
}

/// Canvas rendering event cards in either 3-lane planning mode or single-lane finalized mode.
public struct LaneCanvasView: View {
    public let layout: TimelineLayout
    public let events: [CruiseEvent]
    public let categories: [Int64: Category]
    public let conflictIds: Set<Int64>
    public let singleLane: Bool
    public let onEventClick: ((CruiseEvent) -> Void)?
    public let onEventLongClick: ((CruiseEvent) -> Void)?
    public let onMoveToLane: ((CruiseEvent, Int) -> Void)?
    public let onToggleActive: ((CruiseEvent) -> Void)?
    public let onKeepOnlyThisShow: ((CruiseEvent) -> Void)?
    public let onEditEvent: ((CruiseEvent) -> Void)?
    public let onDeleteEvent: ((CruiseEvent) -> Void)?

    public init(
        layout: TimelineLayout,
        events: [CruiseEvent],
        categories: [Int64: Category],
        conflictIds: Set<Int64> = [],
        singleLane: Bool = false,
        onEventClick: ((CruiseEvent) -> Void)? = nil,
        onEventLongClick: ((CruiseEvent) -> Void)? = nil,
        onMoveToLane: ((CruiseEvent, Int) -> Void)? = nil,
        onToggleActive: ((CruiseEvent) -> Void)? = nil,
        onKeepOnlyThisShow: ((CruiseEvent) -> Void)? = nil,
        onEditEvent: ((CruiseEvent) -> Void)? = nil,
        onDeleteEvent: ((CruiseEvent) -> Void)? = nil
    ) {
        self.layout = layout
        self.events = events
        self.categories = categories
        self.conflictIds = conflictIds
        self.singleLane = singleLane
        self.onEventClick = onEventClick
        self.onEventLongClick = onEventLongClick
        self.onMoveToLane = onMoveToLane
        self.onToggleActive = onToggleActive
        self.onKeepOnlyThisShow = onKeepOnlyThisShow
        self.onEditEvent = onEditEvent
        self.onDeleteEvent = onDeleteEvent
    }

    public var body: some View {
        GeometryReader { geometry in
            let totalWidth = geometry.size.width
            let weightSum = LANE_WEIGHTS.reduce(0.0, +)

            ZStack(alignment: .topLeading) {
                ForEach(events) { event in
                    let isShow = categories[event.categoryId]?.iconKey == "show"
                    let effRange = showEffectiveRange(event, isShow: isShow)
                    let topPt = layout.minuteToPt(effRange.start)
                    let bottomPt = layout.minuteToPt(effRange.end)

                    // Skip events entirely out of visible canvas range
                    if bottomPt > 0 && topPt < layout.totalHeightPt {
                        let heightPt = max(MIN_CARD_HEIGHT, CGFloat(bottomPt - topPt))

                        // Wait segment height calculation
                        let waitHeightPt: CGFloat = {
                            if isShow {
                                let waitEndPt = layout.minuteToPt(effRange.start + event.effectiveWaitMinutes)
                                let band = CGFloat(waitEndPt - topPt)
                                let minWait: CGFloat = band > 0 ? 16.0 : 0.0
                                let maxWait = max(0.0, heightPt - TITLE_RESERVE_PT - SPACER_PT)
                                return min(max(band, minWait), maxWait)
                            } else {
                                return 0.0
                            }
                        }()

                        let lane = max(0, min(LANE_COUNT - 1, event.lane))
                        let (offsetX, cardWidth): (CGFloat, CGFloat) = {
                            if singleLane {
                                return (0, totalWidth)
                            } else {
                                let leftFraction = LANE_WEIGHTS.prefix(lane).reduce(0.0, +) / weightSum
                                let widthFraction = LANE_WEIGHTS[lane] / weightSum
                                let x = totalWidth * CGFloat(leftFraction)
                                let w = max(20.0, totalWidth * CGFloat(widthFraction) - LANE_GAP)
                                return (x, w)
                            }
                        }()

                        EventCardView(
                            event: event,
                            category: categories[event.categoryId],
                            heightPt: heightPt,
                            waitSegmentHeightPt: waitHeightPt,
                            hasConflict: conflictIds.contains(event.id),
                            narrow: false,
                            onClick: {
                                onEventClick?(event)
                            },
                            onLongClick: {
                                onEventLongClick?(event)
                            },
                            onMoveToLane: { laneIndex in
                                onMoveToLane?(event, laneIndex)
                            },
                            onToggleActive: {
                                onToggleActive?(event)
                            },
                            onKeepOnlyThisShow: {
                                onKeepOnlyThisShow?(event)
                            },
                            onEdit: {
                                onEditEvent?(event)
                            },
                            onDelete: {
                                onDeleteEvent?(event)
                            }
                        )
                        .frame(width: cardWidth, height: heightPt)
                        .offset(x: offsetX, y: CGFloat(topPt))
                    }
                }
            }
            .frame(width: totalWidth, height: CGFloat(layout.totalHeightPt), alignment: .topLeading)
        }
        .frame(height: CGFloat(layout.totalHeightPt))
    }
}
