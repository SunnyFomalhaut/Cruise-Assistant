import SwiftUI

/// A reusable micro-badge / chip representing an event category.
/// Supports both display badge mode and interactive filter selection mode.
public struct CategoryChip: View {
    public let name: String
    public let colorArgb: Int64
    public let showDot: Bool
    public let isSelected: Bool
    public let onTap: (() -> Void)?

    public init(
        name: String,
        colorArgb: Int64,
        showDot: Bool = false,
        isSelected: Bool = false,
        onTap: (() -> Void)? = nil
    ) {
        self.name = name
        self.colorArgb = colorArgb
        self.showDot = showDot
        self.isSelected = isSelected
        self.onTap = onTap
    }

    public init(
        category: Category,
        showDot: Bool = false,
        isSelected: Bool = false,
        onTap: (() -> Void)? = nil
    ) {
        self.init(
            name: category.name,
            colorArgb: category.colorArgb,
            showDot: showDot,
            isSelected: isSelected,
            onTap: onTap
        )
    }

    public var body: some View {
        if let onTap = onTap {
            Button(action: onTap) {
                chipContent
            }
            .buttonStyle(PlainButtonStyle())
        } else {
            chipContent
        }
    }

    @ViewBuilder
    private var chipContent: some View {
        HStack(spacing: 4) {
            if showDot {
                Circle()
                    .fill(isSelected ? Color.white : Color(argb: colorArgb))
                    .frame(width: 6, height: 6)
            }

            Text(name)
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(isSelected ? .white : cardTextColorFor(colorArgb: colorArgb))
                .lineLimit(1)
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 2.5)
        .background(
            RoundedRectangle(cornerRadius: 6)
                .fill(isSelected ? Color(argb: colorArgb) : Color(argb: colorArgb).opacity(0.18))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 6)
                .stroke(isSelected ? Color(argb: colorArgb) : Color.clear, lineWidth: 1)
        )
        .animation(.easeInOut(duration: 0.15), value: isSelected)
    }
}

#Preview {
    VStack(spacing: 8) {
        CategoryChip(name: "演出", colorArgb: 0xFFF5B301)
        CategoryChip(name: "餐食", colorArgb: 0xFF34A853, showDot: true)
        CategoryChip(name: "重要事项", colorArgb: 0xFFE8563F, isSelected: true, onTap: {})
        CategoryChip(name: "娱乐项目", colorArgb: 0xFF9B5DE5, showDot: true, isSelected: true, onTap: {})
    }
    .padding()
}
