import SwiftUI
import PhotosUI

/// A component allowing the user to select from Duffy wallpapers or custom uploads, toggle wallpaper on/off, and adjust alpha transparency.
public struct WallpaperPickerView: View {
    public let selectedKey: String
    public let wallpaperEnabled: Bool
    public let wallpaperAlpha: Double
    public let onSelectWallpaper: (String) -> Void
    public let onAlphaChanged: (Double) -> Void
    public let onToggleEnabled: (Bool) -> Void

    @Environment(\.cruisePalette) private var palette
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var customPreviewToken: Int = 0

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
            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(WallpaperPreset.allCases) { preset in
                    if preset == .custom {
                        PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                            wallpaperCardContent(preset: preset)
                        }
                        .buttonStyle(.plain)
                    } else {
                        Button {
                            onSelectWallpaper(preset.rawValue)
                        } label: {
                            wallpaperCardContent(preset: preset)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .opacity(wallpaperEnabled ? 1.0 : 0.45)
            .disabled(!wallpaperEnabled)
            .onChange(of: selectedPhotoItem) { _, newItem in
                guard let newItem = newItem else { return }
                Task {
                    if let data = try? await newItem.loadTransferable(type: Data.self) {
                        await MainActor.run {
                            if saveCustomWallpaperData(data) {
                                customPreviewToken += 1
                                onSelectWallpaper(WallpaperPreset.custom.rawValue)
                            }
                        }
                    }
                }
            }

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
    private func wallpaperCardContent(preset: WallpaperPreset) -> some View {
        let isSelected = selectedKey == preset.rawValue
        let _ = customPreviewToken

        VStack(spacing: 6) {
            ZStack {
                if preset == .none {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Slate100)
                    Image(systemName: "slash.circle")
                        .font(.system(size: 22))
                        .foregroundColor(Slate400)
                } else if let img = loadWallpaperImage(for: preset) {
                    #if canImport(UIKit)
                    Image(uiImage: img)
                        .resizable()
                        .scaledToFill()
                    #elseif canImport(AppKit)
                    Image(nsImage: img)
                        .resizable()
                        .scaledToFill()
                    #endif
                    if preset == .custom {
                        Color.black.opacity(0.2)
                        Image(systemName: "photo.badge.plus")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                    }
                } else if preset == .custom {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Slate100)
                    Image(systemName: "photo.badge.plus")
                        .font(.system(size: 22))
                        .foregroundColor(palette.primary)
                } else {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Slate100)
                }

                if isSelected {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
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
            .frame(height: 72)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(isSelected ? palette.primary : Slate200, lineWidth: isSelected ? 2 : 1)
            )

            Text(preset.displayName)
                .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                .foregroundColor(isSelected ? palette.primary : Slate700)
                .lineLimit(1)
        }
        .padding(2)
    }
}
