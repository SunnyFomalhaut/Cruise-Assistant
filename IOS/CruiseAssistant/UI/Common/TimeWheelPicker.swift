import SwiftUI

/// A clean, full-width two-column wheel picker for selecting hours (00-23) and minutes (00-59 with configurable step).
public struct TimeWheelPicker: View {
    @Binding public var hour: Int
    @Binding public var minute: Int

    public let minuteStep: Int
    public let showLabels: Bool

    public init(
        hour: Binding<Int>,
        minute: Binding<Int>,
        minuteStep: Int = 5,
        showLabels: Bool = true
    ) {
        self._hour = hour
        self._minute = minute
        self.minuteStep = max(1, min(30, minuteStep))
        self.showLabels = showLabels
    }

    public init(
        time: Binding<SimpleTime>,
        minuteStep: Int = 5,
        showLabels: Bool = true
    ) {
        self._hour = Binding(
            get: { time.wrappedValue.hour },
            set: { newHour in
                time.wrappedValue = SimpleTime(hour: newHour, minute: time.wrappedValue.minute)
            }
        )
        self._minute = Binding(
            get: {
                let current = time.wrappedValue.minute
                let step = max(1, min(30, minuteStep))
                return (current / step) * step
            },
            set: { newMinute in
                time.wrappedValue = SimpleTime(hour: time.wrappedValue.hour, minute: newMinute)
            }
        )
        self.minuteStep = max(1, min(30, minuteStep))
        self.showLabels = showLabels
    }

    private var minuteOptions: [Int] {
        Array(stride(from: 0, to: 60, by: minuteStep))
    }

    public var body: some View {
        HStack(spacing: 12) {
            // Hours Wheel
            HStack(spacing: 6) {
                Picker("", selection: $hour) {
                    ForEach(0..<24, id: \.self) { h in
                        Text(String(format: "%02d", h))
                            .font(.system(size: 22, weight: .bold, design: .rounded))
                            .foregroundColor(Slate900)
                            .tag(h)
                    }
                }
                #if os(iOS)
                .pickerStyle(.wheel)
                .frame(width: 85, height: 130)
                .clipped()
                #else
                .pickerStyle(.automatic)
                .frame(width: 85)
                #endif

                if showLabels {
                    Text("时")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Slate600)
                        .fixedSize()
                }
            }

            Text(":")
                .font(.system(size: 24, weight: .heavy, design: .rounded))
                .foregroundColor(Slate400)
                .padding(.horizontal, 4)

            // Minutes Wheel
            HStack(spacing: 6) {
                Picker("", selection: $minute) {
                    ForEach(minuteOptions, id: \.self) { m in
                        Text(String(format: "%02d", m))
                            .font(.system(size: 22, weight: .bold, design: .rounded))
                            .foregroundColor(Slate900)
                            .tag(m)
                    }
                }
                #if os(iOS)
                .pickerStyle(.wheel)
                .frame(width: 85, height: 130)
                .clipped()
                #else
                .pickerStyle(.automatic)
                .frame(width: 85)
                #endif

                if showLabels {
                    Text("分")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Slate600)
                        .fixedSize()
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.vertical, 4)
    }
}

#Preview {
    TimeWheelPicker(
        time: .constant(SimpleTime(hour: 12, minute: 30)),
        minuteStep: 5
    )
}
