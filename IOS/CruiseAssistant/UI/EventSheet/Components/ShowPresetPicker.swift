import SwiftUI

/// Preset show picker for selecting pre-configured Disney Adventure shows or custom show entry.
public struct ShowPresetPicker: View {
    public let shows: [ShowPreset]
    public let selectedShowTitle: String
    public let isPresetMode: Bool
    public let isEnabled: Bool
    public let onSelectPreset: (ShowPreset) -> Void
    public let onCustomShow: () -> Void

    public init(
        shows: [ShowPreset] = DISNEY_ADVENTURE_SHOWS,
        selectedShowTitle: String,
        isPresetMode: Bool = true,
        isEnabled: Bool = true,
        onSelectPreset: @escaping (ShowPreset) -> Void,
        onCustomShow: @escaping () -> Void
    ) {
        self.shows = shows
        self.selectedShowTitle = selectedShowTitle
        self.isPresetMode = isPresetMode
        self.isEnabled = isEnabled
        self.onSelectPreset = onSelectPreset
        self.onCustomShow = onCustomShow
    }

    public var body: some View {
        Menu {
            ForEach(shows) { show in
                Button(action: {
                    onSelectPreset(show)
                }) {
                    HStack {
                        Text(show.title)
                        if isPresetMode && selectedShowTitle == show.title {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }

            Divider()

            Button(action: onCustomShow) {
                HStack {
                    Text("✍️ 自定义演出")
                    if !isPresetMode {
                        Image(systemName: "checkmark")
                    }
                }
            }
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(isPresetMode ? (selectedShowTitle.isEmpty ? "选择预置演出" : selectedShowTitle) : "自定义演出")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(Slate900)
                        .lineLimit(1)

                    if isPresetMode, let current = shows.first(where: { $0.title == selectedShowTitle }) {
                        Text("Deck \(current.deck) \(current.zone.label) · \(current.placeName)")
                            .font(.system(size: 12))
                            .foregroundColor(Slate500)
                            .lineLimit(1)
                    }
                }

                Spacer()

                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption)
                    .foregroundColor(Slate400)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(Slate200, lineWidth: 1)
            )
        }
        .disabled(!isEnabled)
    }
}

#Preview {
    VStack(spacing: 16) {
        ShowPresetPicker(
            selectedShowTitle: DISNEY_ADVENTURE_SHOWS[0].title,
            isPresetMode: true,
            onSelectPreset: { _ in },
            onCustomShow: {}
        )

        ShowPresetPicker(
            selectedShowTitle: "",
            isPresetMode: false,
            onSelectPreset: { _ in },
            onCustomShow: {}
        )
    }
    .padding()
}
