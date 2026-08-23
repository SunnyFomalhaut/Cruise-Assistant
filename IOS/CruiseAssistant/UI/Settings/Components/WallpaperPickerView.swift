import SwiftUI

/// A component allowing the user to select from Duffy wallpapers or custom uploads, toggle wallpaper on/off, and adjust alpha transparency.
public struct WallpaperPickerView: View {
    public let selectedKey: String
    public let wallpaperEnabled: Bool
    public let wallpaperAlpha: Double
    public let onSelectWallpaper: (String) -> Void
    public let onAlphaChanged: (Double) -> Void
    public let onToggleEnabled: (Bool) -> Void

    @Environment(\.cruisePalette) private var palette

    private let columns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8)
    ]

    public init(
        selectedKey: String,
        wallpaperEnabled: Bool,
        wallpaperAlpha: Double,
        onSelectWallpaper: @escaping (String) -> Void,
        onAlphaChanged: @escaping (Double) -> Void,
        onToggleEnabled: @escaping (Bool) -> Void
    ) {
        self.selectedKey = selectedKey
        self.wallpaperEnabled = wallpaperEnabled
        self.wallpaperAlpha = wallpaperAlpha
        self.onSelectWallpaper = onSelectWallpaper
        self.onAlphaChanged = onAlphaChanged
        self.onToggleEnabled = onToggleEnabled
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Master Toggle
            Toggle(isOn: Binding(
                get: { wallpaperEnabled },
                set: { onToggleEnabled($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("启用时间线背景壁纸")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Slate800)
                    Text("在时间线网格后方展示达菲与朋友专属半透明壁纸")
                        .font(.system(size: 11))
                        .foregroundColor(Slate500)
                }
            }
            .tint(palette.primary)

            // Wallpaper Presets Grid
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(WallpaperPreset.allCases) { preset in
                    wallpaperCard(preset: preset)
                }
            }
            .opacity(wallpaperEnabled ? 1.0 : 0.45)
            .disabled(!wallpaperEnabled)

            // Alpha Opacity Slider
            if wallpaperEnabled {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("壁纸透明度")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(Slate700)
                        Spacer()
                        Text("\(Int(wallpaperAlpha * 100))%")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(palette.primary)
                    }

                    HStack(spacing: 8) {
                        Text("淡 (5%)")
                            .font(.system(size: 10))
                            .foregroundColor(Slate400)

                        Slider(
                            value: Binding(
                                get: { wallpaperAlpha },
                                set: { onAlphaChanged($0) }
                            ),
                            in: 0.05...0.40,
                            step: 0.01
                        )
                        .tint(palette.primary)

                        Text("浓 (40%)")
                            .font(.system(size: 10))
                            .foregroundColor(Slate400)
                    }
                }
                .padding(.top, 4)
            }
        }
    }

    @ViewBuilder
    private func wallpaperCard(preset: WallpaperPreset) -> some View {
        let isSelected = selectedKey == preset.rawValue

        Button {
            onSelectWallpaper(preset.rawValue)
        } label: {
            VStack(spacing: 6) {
                // Thumbnail Preview
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Slate100)

                    if preset == .none {
                        Image(systemName: "slash.circle")
                            .font(.system(size: 20))
                            .foregroundColor(Slate400)
                    } else if preset == .custom {
                        Image(systemName: "photo.badge.plus")
                            .font(.system(size: 20))
                            .foregroundColor(palette.primary)
                    } else if let img = loadWallpaperImage(for: preset) {
                        #if canImport(UIKit)
                        Image(uiImage: img)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .clipped()
                            .cornerRadius(8)
                        #elseif canImport(AppKit)
                        Image(nsImage: img)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .clipped()
                            .cornerRadius(8)
                        #endif
                    }

                    if isSelected {
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(palette.primary, lineWidth: 2.5)

                        VStack {
                            HStack {
                                Spacer()
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(palette.primary)
                                    .background(Circle().fill(Color.white).padding(2))
                            }
                            Spacer()
                        }
                        .padding(4)
                    }
                }
                .frame(height: 60)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isSelected ? palette.primary : Slate200, lineWidth: isSelected ? 2 : 1)
                )

                Text(preset.displayName)
                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                    .foregroundColor(isSelected ? palette.primary : Slate700)
                    .lineLimit(1)
            }
            .padding(4)
        }
        .buttonStyle(.plain)
    }
}
