import SwiftUI

/// 自定义按键按压样式，提供缩放与背景高亮动画
public struct KeypadButtonStyle: ButtonStyle {
    public let backgroundColor: Color
    public let pressedColor: Color
    public let cornerRadius: CGFloat

    public init(
        backgroundColor: Color = Slate100,
        pressedColor: Color = Slate200,
        cornerRadius: CGFloat = 12
    ) {
        self.backgroundColor = backgroundColor
        self.pressedColor = pressedColor
        self.cornerRadius = cornerRadius
    }

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? pressedColor : backgroundColor)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

/// 仙尘 9 键触感数字键盘（3x4 布局）
public struct PixieKeypadView: View {
    public let onDigit: (String) -> Void
    public let onBackspace: () -> Void
    public let onClear: () -> Void

    public init(
        onDigit: @escaping (String) -> Void,
        onBackspace: @escaping () -> Void,
        onClear: @escaping () -> Void
    ) {
        self.onDigit = onDigit
        self.onBackspace = onBackspace
        self.onClear = onClear
    }

    public var body: some View {
        VStack(spacing: 8) {
            // Row 1: 1, 2, 3
            HStack(spacing: 8) {
                keypadButton(label: "1") { handleDigit("1") }
                keypadButton(label: "2") { handleDigit("2") }
                keypadButton(label: "3") { handleDigit("3") }
            }

            // Row 2: 4, 5, 6
            HStack(spacing: 8) {
                keypadButton(label: "4") { handleDigit("4") }
                keypadButton(label: "5") { handleDigit("5") }
                keypadButton(label: "6") { handleDigit("6") }
            }

            // Row 3: 7, 8, 9
            HStack(spacing: 8) {
                keypadButton(label: "7") { handleDigit("7") }
                keypadButton(label: "8") { handleDigit("8") }
                keypadButton(label: "9") { handleDigit("9") }
            }

            // Row 4: 清空 (C), 0, ⌫
            HStack(spacing: 8) {
                // Clear button
                Button(action: handleClear) {
                    Text("C")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(Slate600)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(KeypadButtonStyle(backgroundColor: Slate100, pressedColor: Slate200))
                .accessibilityLabel("清空房号")

                // 0 button
                keypadButton(label: "0") { handleDigit("0") }

                // Backspace button
                Button(action: handleBackspace) {
                    Image(systemName: "delete.backward.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Slate700)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(KeypadButtonStyle(backgroundColor: Slate100, pressedColor: Slate200))
                .accessibilityLabel("删除一位")
            }
        }
        .padding(.horizontal, 4)
    }

    @ViewBuilder
    private func keypadButton(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .foregroundColor(Slate900)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
        }
        .buttonStyle(KeypadButtonStyle(backgroundColor: Color.white, pressedColor: Slate100))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Slate200, lineWidth: 1)
        )
    }

    private func handleDigit(_ digit: String) {
        HapticFeedbackService.shared.triggerKeypadTap()
        onDigit(digit)
    }

    private func handleBackspace() {
        HapticFeedbackService.shared.triggerKeypadTap()
        onBackspace()
    }

    private func handleClear() {
        HapticFeedbackService.shared.triggerKeypadTap()
        onClear()
    }
}

#Preview {
    PixieKeypadView(
        onDigit: { print("Digit: \($0)") },
        onBackspace: { print("Backspace") },
        onClear: { print("Clear") }
    )
    .padding()
    .background(Slate50)
}
