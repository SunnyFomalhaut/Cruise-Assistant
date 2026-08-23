import SwiftUI

/// A two-column wheel picker for selecting hours (00-23) and minutes (00-59 with configurable step).
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
                if minuteStep > 1 {
                    return (current / minuteStep) * minuteStep
                }
                return current
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
        HStack(spacing: 8) {
            // Hours Wheel
            HStack(spacing: 4) {
                let hourPicker = Picker("", selection: $hour) {
                    ForEach(0..<24, id: \.self) { h in
                        Text(String(format: "%02d", h))
                            .font(.system(size: 20, weight: .semibold, design: .monospaced))
                            .tag(h)
                    }
                }
                #if os(iOS)
                hourPicker
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)
                    .clipped()
                #else
                hourPicker
                    .pickerStyle(.automatic)
                    .frame(maxWidth: .infinity)
                #endif

                if showLabels {
                    Text("时")
                        .font(.subheadline)
                        .foregroundColor(Slate500)
                }
            }

            Text(":")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Slate400)
                .padding(.horizontal, 2)

            // Minutes Wheel
            HStack(spacing: 4) {
                let minutePicker = Picker("", selection: $minute) {
                    ForEach(minuteOptions, id: \.self) { m in
                        Text(String(format: "%02d", m))
                            .font(.system(size: 20, weight: .semibold, design: .monospaced))
                            .tag(m)
                    }
                }
                #if os(iOS)
                minutePicker
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)
                    .clipped()
                #else
                minutePicker
                    .pickerStyle(.automatic)
                    .frame(maxWidth: .infinity)
                #endif

                if showLabels {
                    Text("分")
                        .font(.subheadline)
                        .foregroundColor(Slate500)
                }
            }
        }
        .padding(.horizontal, 16)
    }
}

#Preview {
    TimeWheelPicker(
        time: .constant(SimpleTime(hour: 12, minute: 30)),
        minuteStep: 5
    )
}
