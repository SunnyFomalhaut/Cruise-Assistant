import SwiftUI

public let RULER_WIDTH: CGFloat = 52.0

/// 3-lane equal weight fractions
private let LANE_WEIGHTS: [Double] = [1.0, 1.0, 1.0]

/// Returns vertical lane boundary fractions (e.g. [1/3, 2/3]) for drawing grid lines.
public func laneBoundaryFractions() -> [Double] {
    let total = LANE_WEIGHTS.reduce(0.0, +)
    guard total > 0 else { return [] }
    var acc = 0.0
    return LANE_WEIGHTS.dropLast().map { weight in
        acc += weight
        return acc / total
    }
}

/// Left vertical time ruler displaying hour and half-hour labels alongside collapsed gap indicators.
public struct TimeRulerView: View {
    public let layout: TimelineLayout

    public init(layout: TimelineLayout) {
        self.layout = layout
    }

    public var body: some View {
        let ticks = layout.hourTicks()
        let collapsedSegments = layout.segments.filter { $0.collapsed }

        ZStack(alignment: .topLeading) {
            // Ruler container size
            Color.clear
                .frame(width: RULER_WIDTH, height: CGFloat(layout.totalHeightPt))

            // Time tick labels
            ForEach(Array(ticks.enumerated()), id: \.offset) { _, tick in
                let hour = tick.minute / 60
                let minute = tick.minute % 60
                let text = tick.isHalfHour ? "\(hour):\(String(format: "%02d", minute))" : "\(String(format: "%02d", hour)):00"

                Text(text)
                    .font(.system(size: tick.isHalfHour ? 10 : 12, weight: tick.isHalfHour ? .regular : .semibold))
                    .foregroundColor(tick.isHalfHour ? Slate400 : Slate700)
                    .offset(x: 6, y: CGFloat(tick.pt) - 8)
            }

            // Collapsed segment indicators
            ForEach(Array(collapsedSegments.enumerated()), id: \.offset) { _, segment in
                Text("⋯")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Slate400)
                    .offset(
                        x: 18,
                        y: CGFloat(segment.topPt + segment.heightPt / 2.0) - 8
                    )
            }
        }
        .frame(width: RULER_WIDTH, height: CGFloat(layout.totalHeightPt), alignment: .topLeading)
    }
}

/// Background grid rendering horizontal hour/half-hour lines and vertical lane divider lines.
public struct TimelineGridView: View {
    public let layout: TimelineLayout
    public let laneBoundaries: [Double]

    public init(
        layout: TimelineLayout,
        laneBoundaries: [Double] = laneBoundaryFractions()
    ) {
        self.layout = layout
        self.laneBoundaries = laneBoundaries
    }

    public var body: some View {
        let ticks = layout.hourTicks()

        Canvas { context, size in
            let width = size.width
            let height = size.height

            // Horizontal grid lines (hour and half-hour ticks)
            for tick in ticks {
                let y = CGFloat(tick.pt)
                var path = Path()
                path.move(to: CGPoint(x: 0, y: y))
                path.addLine(to: CGPoint(x: width, y: y))

                let strokeColor = tick.isHalfHour ? GridLine : GridLineHour
                let lineWidth: CGFloat = tick.isHalfHour ? 1.0 : 1.5
                context.stroke(path, with: .color(strokeColor), lineWidth: lineWidth)
            }

            // Vertical lane boundary lines
            for fraction in laneBoundaries {
                let x = width * CGFloat(fraction)
                var path = Path()
                path.move(to: CGPoint(x: x, y: 0))
                path.addLine(to: CGPoint(x: x, y: height))

                context.stroke(path, with: .color(GridLineHour.opacity(0.8)), lineWidth: 1.0)
            }
        }
        .frame(height: CGFloat(layout.totalHeightPt))
    }
}
