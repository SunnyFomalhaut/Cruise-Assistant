import SwiftUI

private let CHIP_WIDTH: CGFloat = 58.0
private let CHIP_HEIGHT: CGFloat = 66.0

/// Computes short Chinese weekday name for a `SimpleDate` (e.g. "周一", "周二").
public func weekdayChinese(for date: SimpleDate) -> String {
    let weekdays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]
    var calendar = Calendar(identifier: .gregorian)
    calendar.locale = Locale(identifier: "zh_CN")
    let weekdayIndex = calendar.component(.weekday, from: date.toDate()) - 1
    if weekdayIndex >= 0 && weekdayIndex < weekdays.count {
        return weekdays[weekdayIndex]
    }
    return ""
}

/// Horizontal date selector strip displaying capsule chips for each voyage day.
public struct DateStripView: View {
    public let dates: [SimpleDate]
    public let selectedDate: SimpleDate
    public let finalizedDates: Set<SimpleDate>
    public let theme: AppColorTheme
    public let onSelect: (SimpleDate) -> Void

    public init(
        dates: [SimpleDate],
        selectedDate: SimpleDate,
        finalizedDates: Set<SimpleDate> = [],
        theme: AppColorTheme = .judy_blue,
        onSelect: @escaping (SimpleDate) -> Void
    ) {
        self.dates = dates
        self.selectedDate = selectedDate
        self.finalizedDates = finalizedDates
        self.theme = theme
        self.onSelect = onSelect
    }

    public var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(dates.enumerated()), id: \.element) { index, date in
                        let isSelected = (date == selectedDate)
                        let isFinalized = finalizedDates.contains(date)
                        let dayIndex = index + 1
                        let dayNumber = date.day
                        let weekday = weekdayChinese(for: date)

                        DateChip(
                            dayIndex: dayIndex,
                            dayNumber: dayNumber,
                            weekday: weekday,
                            isSelected: isSelected,
                            isFinalized: isFinalized,
                            theme: theme,
                            onTap: {
                                onSelect(date)
                            }
                        )
                        .id(date)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            .background(SurfaceCard)
            .overlay(
                Divider(),
                alignment: .bottom
            )
            .onChange(of: selectedDate) { _, newDate in
                withAnimation(.easeInOut(duration: 0.25)) {
                    proxy.scrollTo(newDate, anchor: .center)
                }
            }
            .onAppear {
                proxy.scrollTo(selectedDate, anchor: .center)
            }
        }
    }
}

/// Individual capsule date chip component.
private struct DateChip: View {
    let dayIndex: Int
    let dayNumber: Int
    let weekday: String
    let isSelected: Bool
    let isFinalized: Bool
    let theme: AppColorTheme
    let onTap: () -> Void

    private var palette: CruiseColorPalette {
        theme.palette
    }

    private var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: 16, style: .continuous)
    }

    private var backgroundColor: Color {
        isSelected ? palette.primaryContainer : SurfaceCard
    }

    private var borderColor: Color {
        if isSelected {
            return palette.primary
        } else if isFinalized {
            return palette.secondary.opacity(0.4)
        } else {
            return Slate200
        }
    }

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 0) {
                // Day index (e.g. "D1")
                Text("D\(dayIndex)")
                    .font(.system(size: 11, weight: isSelected ? .bold : .medium))
                    .foregroundColor(isSelected ? palette.secondary : Slate400)
                    .lineLimit(1)

                Spacer(minLength: 1)

                // Day of month number
                Text("\(dayNumber)")
                    .font(.system(size: 17, weight: isSelected ? .bold : .semibold))
                    .foregroundColor(isSelected ? palette.secondary : Slate900)
                    .lineLimit(1)

                Spacer(minLength: 1)

                // Weekday label (e.g. "周四")
                Text(weekday)
                    .font(.system(size: 10, weight: isSelected ? .bold : .regular))
                    .foregroundColor(isSelected ? palette.secondary : Slate700)
                    .lineLimit(1)

                Spacer(minLength: 2)

                // Finalized dot indicator
                if isFinalized {
                    Circle()
                        .fill(isSelected ? palette.secondary : palette.primary.opacity(0.7))
                        .frame(width: 4, height: 4)
                } else {
                    Spacer()
                        .frame(height: 4)
                }
            }
            .padding(.vertical, 6)
            .padding(.horizontal, 4)
            .frame(width: CHIP_WIDTH, height: CHIP_HEIGHT)
            .background(backgroundColor)
            .clipShape(shape)
            .overlay(
                shape
                    .stroke(borderColor, lineWidth: isSelected ? 1.5 : 1.0)
            )
            .shadow(
                color: isSelected ? palette.primary.opacity(0.25) : Color.clear,
                radius: 3,
                x: 0,
                y: 1.5
            )
        }
        .buttonStyle(PlainButtonStyle())
        .animation(.easeInOut(duration: 0.2), value: isSelected)
        .animation(.easeInOut(duration: 0.2), value: isFinalized)
    }
}

#Preview {
    DateStripView(
        dates: [
            SimpleDate(year: 2026, month: 9, day: 3),
            SimpleDate(year: 2026, month: 9, day: 4),
            SimpleDate(year: 2026, month: 9, day: 5),
            SimpleDate(year: 2026, month: 9, day: 6),
            SimpleDate(year: 2026, month: 9, day: 7)
        ],
        selectedDate: SimpleDate(year: 2026, month: 9, day: 3),
        finalizedDates: [SimpleDate(year: 2026, month: 9, day: 3)],
        theme: .shelliemay_pink,
        onSelect: { _ in }
    )
}
