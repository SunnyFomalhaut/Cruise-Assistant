import SwiftUI

/// Location picker supporting preset catalog dropdown, custom manual input (deck, zone, placeName), and no location.
public struct LocationPicker: View {
    public let maxDeck: Int
    public let minDeck: Int
    public let deck: Int?
    public let zone: ShipZone?
    public let placeName: String
    public let presets: [LocationPreset]
    public let noLocation: Bool
    public let presetOptions: [LocationPresetOption]
    public let presetMode: Bool
    public let isEnabled: Bool

    public let onDeckChange: (Int) -> Void
    public let onZoneChange: (ShipZone) -> Void
    public let onPlaceNameChange: (String) -> Void
    public let onNoLocationToggle: (Bool) -> Void
    public let onSelectPreset: (LocationCatalogEntry) -> Void
    public let onCustomLocation: () -> Void

    public init(
        maxDeck: Int = 20,
        minDeck: Int = 1,
        deck: Int?,
        zone: ShipZone?,
        placeName: String,
        presets: [LocationPreset] = [],
        noLocation: Bool = false,
        presetOptions: [LocationPresetOption] = [],
        presetMode: Bool = false,
        isEnabled: Bool = true,
        onDeckChange: @escaping (Int) -> Void,
        onZoneChange: @escaping (ShipZone) -> Void,
        onPlaceNameChange: @escaping (String) -> Void,
        onNoLocationToggle: @escaping (Bool) -> Void,
        onSelectPreset: @escaping (LocationCatalogEntry) -> Void,
        onCustomLocation: @escaping () -> Void
    ) {
        self.maxDeck = maxDeck
        self.minDeck = minDeck
        self.deck = deck
        self.zone = zone
        self.placeName = placeName
        self.presets = presets
        self.noLocation = noLocation
        self.presetOptions = presetOptions
        self.presetMode = presetMode
        self.isEnabled = isEnabled
        self.onDeckChange = onDeckChange
        self.onZoneChange = onZoneChange
        self.onPlaceNameChange = onPlaceNameChange
        self.onNoLocationToggle = onNoLocationToggle
        self.onSelectPreset = onSelectPreset
        self.onCustomLocation = onCustomLocation
    }

    // Suggestions sorted by same deck / zone priority
    private var suggestions: [LocationPreset] {
        presets.sorted { p1, p2 in
            let score1 = (p1.deck == deck ? 2 : 0) + (p1.zone == zone ? 1 : 0)
            let score2 = (p2.deck == deck ? 2 : 0) + (p2.zone == zone ? 1 : 0)
            if score1 != score2 {
                return score1 > score2
            }
            return p1.useCount > p2.useCount
        }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if presetMode && !presetOptions.isEmpty {
                presetDropdownView
            } else {
                customLocationView
            }

            // No location checkbox toggle
            Button(action: {
                if isEnabled {
                    onNoLocationToggle(!noLocation)
                }
            }) {
                HStack(spacing: 8) {
                    Image(systemName: noLocation ? "checkmark.square.fill" : "square")
                        .font(.system(size: 16))
                        .foregroundColor(noLocation ? CruiseAzure : Slate400)

                    Text("不设地点")
                        .font(.system(size: 13))
                        .foregroundColor(Slate700)

                    Spacer()
                }
            }
            .buttonStyle(PlainButtonStyle())
            .disabled(!isEnabled)
        }
    }

    // MARK: - Preset Dropdown View

    private var presetDropdownView: some View {
        Menu {
            ForEach(Array(presetOptions.enumerated()), id: \.offset) { _, option in
                switch option {
                case .group(let title):
                    Text("—— \(title) ——")
                case .entry(let entry):
                    let isSelected = entry.deck == deck && entry.zone == zone && entry.placeName == placeName
                    Button(action: {
                        onSelectPreset(entry)
                    }) {
                        HStack {
                            Text(formatEntryLabel(entry))
                            if isSelected {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                case .custom:
                    Divider()
                    Button(action: onCustomLocation) {
                        Text("✍️ 自定义地点")
                    }
                }
            }
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(noLocation ? "不设地点" : (placeName.isEmpty ? "选择预置地点" : placeName))
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(noLocation ? Slate400 : (placeName.isEmpty ? Slate400 : Slate900))
                        .lineLimit(1)

                    if !noLocation, let deck = deck {
                        Text("Deck \(deck) \(zone?.label ?? "")")
                            .font(.system(size: 12))
                            .foregroundColor(Slate500)
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
        .disabled(!isEnabled || noLocation)
        .opacity(noLocation ? 0.6 : 1.0)
    }

    private func formatEntryLabel(_ entry: LocationCatalogEntry) -> String {
        var parts: [String] = []
        if let d = entry.deck {
            parts.append("Deck \(d)")
        }
        if let z = entry.zone {
            parts.append(z.label)
        }
        parts.append(entry.placeName)
        return parts.joined(separator: " ")
    }

    // MARK: - Custom Location View

    private var customLocationView: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                // Deck Picker Menu
                Menu {
                    ForEach(minDeck...maxDeck, id: \.self) { d in
                        Button("Deck \(d)") {
                            onDeckChange(d)
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Text(deck != nil ? "Deck \(deck!)" : "选择楼层")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(deck != nil ? Slate900 : Slate500)
                        Image(systemName: "chevron.down")
                            .font(.caption2)
                            .foregroundColor(Slate400)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .stroke(Slate200, lineWidth: 1)
                    )
                }
                .disabled(!isEnabled || noLocation)

                // Zone Picker (Segment / Chips)
                HStack(spacing: 4) {
                    ForEach(ShipZone.allCases, id: \.self) { z in
                        let isSelected = zone == z
                        Button(action: {
                            onZoneChange(z)
                        }) {
                            Text(z.label)
                                .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
                                .foregroundColor(isSelected ? CruiseMarine : Slate700)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 7)
                                .background(isSelected ? CruiseAzureContainer : Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .stroke(isSelected ? CruiseAzure : Slate200, lineWidth: 1)
                                )
                        }
                        .buttonStyle(PlainButtonStyle())
                        .disabled(!isEnabled || noLocation)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Place name text field + History suggestions menu
            HStack {
                TextField("地点名称（如：大堂、剧院等）", text: Binding(
                    get: { placeName },
                    set: { onPlaceNameChange($0) }
                ))
                .font(.system(size: 14))
                .disabled(!isEnabled || noLocation)

                if !placeName.isEmpty && !noLocation {
                    Button(action: { onPlaceNameChange("") }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(Slate400)
                            .font(.system(size: 14))
                    }
                }

                if !suggestions.isEmpty && !noLocation {
                    Menu {
                        ForEach(suggestions.prefix(8)) { preset in
                            Button("Deck \(preset.deck) \(preset.zone.label) \(preset.placeName)") {
                                onDeckChange(preset.deck)
                                onZoneChange(preset.zone)
                                onPlaceNameChange(preset.placeName)
                            }
                        }
                    } label: {
                        Image(systemName: "clock.arrow.circlepath")
                            .foregroundColor(CruiseAzure)
                            .font(.system(size: 15))
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(Slate200, lineWidth: 1)
            )
        }
        .opacity(noLocation ? 0.6 : 1.0)
    }
}

#Preview {
    VStack(spacing: 16) {
        LocationPicker(
            deck: 6,
            zone: .mid,
            placeName: "宴会厅",
            noLocation: false,
            onDeckChange: { _ in },
            onZoneChange: { _ in },
            onPlaceNameChange: { _ in },
            onNoLocationToggle: { _ in },
            onSelectPreset: { _ in },
            onCustomLocation: {}
        )
    }
    .padding()
}
