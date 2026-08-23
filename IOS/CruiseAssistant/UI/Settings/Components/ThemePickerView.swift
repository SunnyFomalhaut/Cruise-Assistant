import SwiftUI

/// A component displaying a grid of Duffy & Friends color themes with swatch previews and active checkmarks.
public struct ThemePickerView: View {
    public let selectedTheme: AppColorTheme
    public let onSelectTheme: (AppColorTheme) -> Void

    private let columns = [
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10)
    ]

    public init(
        selectedTheme: AppColorTheme,
        onSelectTheme: @escaping (AppColorTheme) -> Void
    ) {
        self.selectedTheme = selectedTheme
        self.onSelectTheme = onSelectTheme
    }

    public var body: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            ForEach(AppColorTheme.allCases) { theme in
                themeCard(theme: theme)
            }
        }
    }

    @ViewBuilder
    private func themeCard(theme: AppColorTheme) -> some View {
        let isSelected = selectedTheme == theme
        let primary = Color(argb: theme.primaryArgb)
        let secondary = Color(argb: theme.secondaryArgb)
        let container = Color(argb: theme.containerArgb)

        Button {
            onSelectTheme(theme)
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                // Header with Theme Name and Active Checkmark
                HStack(alignment: .center) {
                    Text(theme.displayName)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(isSelected ? primary : Slate800)

                    Spacer()

                    if isSelected {
                        ZStack {
                            Circle()
                                .fill(primary)
                                .frame(width: 18, height: 18)
                            Image(systemName: "checkmark")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.white)
                        }
                    } else {
                        Circle()
                            .stroke(Slate300, lineWidth: 1.5)
                            .frame(width: 18, height: 18)
                    }
                }

                // Swatch Dots (Primary, Secondary, Container)
                HStack(spacing: 6) {
                    Circle()
                        .fill(primary)
                        .frame(width: 16, height: 16)
                        .overlay(Circle().stroke(Color.white.opacity(0.8), lineWidth: 1))

                    Circle()
                        .fill(secondary)
                        .frame(width: 16, height: 16)
                        .overlay(Circle().stroke(Color.white.opacity(0.8), lineWidth: 1))

                    Circle()
                        .fill(container)
                        .frame(width: 16, height: 16)
                        .overlay(Circle().stroke(Slate300.opacity(0.6), lineWidth: 1))

                    Spacer()
                }

                // Description
                Text(theme.description)
                    .font(.system(size: 11))
                    .foregroundColor(Slate500)
                    .lineLimit(1)
            }
            .padding(10)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isSelected ? container.opacity(0.35) : Color.white)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? primary : Slate200, lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
    }
}
