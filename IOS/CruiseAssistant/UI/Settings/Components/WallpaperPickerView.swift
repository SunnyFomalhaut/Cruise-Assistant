import SwiftUI

/// A component allowing users to choose timeline background wallpapers and adjust preview transparency.
public struct WallpaperPickerView: View {
    public let selectedKey: String
    public let wallpaperEnabled: Bool
    public let wallpaperAlpha: Double
    public let onSelectWallpaper: (String) -> Void
    public let onAlphaChanged: (Double) -> Void
    public let onToggleEnabled: ((Bool) -> Void)?

    private let columns = [
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10)
    ]

    public init(
        selectedKey: String,
        wallpaperEnabled: Bool,
        wallpaperAlpha: Double,
        onSelectWallpaper: @escaping (String) -> Void,
        onAlphaChanged: @escaping (Double) -> Void,
        onToggleEnabled: ((Bool) -> Void)? = nil
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
            // Master Toggle (Optional)
            if let onToggle = onToggleEnabled {
                Toggle(isOn: Binding(
                    get: { wallpaperEnabled },
                    set: { onToggle($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("启用时间线背景壁纸")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Slate800)
                        Text("在日程时间线上显示半透明达菲与朋友们插画")
                            .font(.system(size: 12))
                            .foregroundColor(Slate500)
                    }
                }
                .tint(CruiseAzure)
            }

            // Wallpaper Presets Grid
            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(WallpaperPreset.allCases) { preset in
                    wallpaperCard(preset: preset)
                }
            }

            // Alpha / Transparency Slider (Only relevant when wallpaper is not none)
            if selectedKey != WallpaperPreset.none.rawValue {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("壁纸透明度")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(Slate700)
                        Spacer()
                        Text("\(Int(wallpaperAlpha * 100))%")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(CruiseAzure)
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
                        .tint(CruiseAzure)

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
                            .foregroundColor(CruiseAzure)
                    } else if let resourceName = preset.imageResourceName {
                        Image(resourceName, bundle: CruiseAssistant.bundle)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .clipped()
                            .cornerRadius(8)
                    }

                    if isSelected {
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(CruiseAzure, lineWidth: 2.5)

                        VStack {
                            HStack {
                                Spacer()
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(CruiseAzure)
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
                        .stroke(isSelected ? CruiseAzure : Slate200, lineWidth: isSelected ? 2 : 1)
                )

                Text(preset.displayName)
                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                    .foregroundColor(isSelected ? CruiseAzure : Slate700)
                    .lineLimit(1)
            }
            .padding(4)
        }
        .buttonStyle(.plain)
    }
}
