import SwiftUI

/// Read-only detail dialog displayed when tapping an event card on a finalized day.
/// Provides full event metadata, poster banner, show wait notices, and deck map navigation.
public struct EventDetailDialogView: View {
    public let event: CruiseEvent
    public let category: Category?
    public let onNavigateToMap: ((Int, String) -> Void)?
    public let onDismiss: () -> Void

    @Environment(\.cruisePalette) private var palette

    public init(
        event: CruiseEvent,
        category: Category? = nil,
        onNavigateToMap: ((Int, String) -> Void)? = nil,
        onDismiss: @escaping () -> Void
    ) {
        self.event = event
        self.category = category
        self.onNavigateToMap = onNavigateToMap
        self.onDismiss = onDismiss
    }

    private var durationFormatted: String {
        let hours = event.durationMinutes / 60
        let minutes = event.durationMinutes % 60
        if hours > 0 && minutes > 0 {
            return "\(hours)小时\(minutes)分钟"
        } else if hours > 0 {
            return "\(hours)小时"
        } else {
            return "\(minutes)分钟"
        }
    }

    private var isShowEvent: Bool {
        category?.iconKey == "show" || category?.name == "演出" || (event.presetKey != nil && DISNEY_ADVENTURE_SHOWS.contains { $0.key == event.presetKey })
    }

    private var posterUrl: URL? {
        if isShowEvent {
            return PosterManager.shared.posterUrl(forEventTitle: event.title, presetKey: event.presetKey)
        } else if let place = event.placeName, !place.isEmpty {
            return PosterManager.shared.posterUrl(for: place)
        }
        return nil
    }

    private var resolvedDeck: Int? {
        if let d = event.deck {
            return d
        }
        if let place = event.placeName, !place.isEmpty {
            let entry = LocationCatalog.getAllEntries().first {
                $0.placeName == place || $0.placeName.contains(place) || place.contains($0.placeName)
            }
            return entry?.deck
        }
        return nil
    }

    private var mapHighlightTarget: String {
        let place = event.placeName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !place.isEmpty {
            return place
        }
        return event.title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var locationFormatted: String? {
        formatLocation(deck: event.deck, zone: event.zone, placeName: event.placeName, compact: false)
    }

    public var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(spacing: 0) {
                // Poster Header (if available)
                if let url = posterUrl {
                    posterHeaderView(url: url)
                } else {
                    categoryHeaderView
                }

                // Content Body
                VStack(alignment: .leading, spacing: 14) {
                    // Category & Title
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 6) {
                            CategoryChip(
                                name: category?.name ?? "活动",
                                colorArgb: category?.colorArgb ?? 0xFF0284C7,
                                showDot: true
                            )

                            if !event.isActive {
                                Text("已停用")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(Slate500)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Slate100)
                                    .clipShape(Capsule())
                            }

                            Spacer()
                        }

                        Text(event.title.isEmpty ? (category?.name ?? "未命名活动") : event.title)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(Slate900)
                    }

                    Divider()

                    // Time Row
                    HStack(spacing: 10) {
                        Image(systemName: "clock")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(palette.primary)
                            .frame(width: 24)

                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(event.startTime.description) – \(event.endTime.description)")
                                .font(.system(size: 15, weight: .bold, design: .rounded))
                                .foregroundColor(Slate900)

                            Text("时长: \(durationFormatted)")
                                .font(.system(size: 12))
                                .foregroundColor(Slate500)
                        }
                    }

                    // Show Wait Time Notice (only for shows with wait minutes)
                    if isShowEvent && event.effectiveWaitMinutes > 0 {
                        HStack(spacing: 10) {
                            Image(systemName: "bell.badge.fill")
                                .font(.system(size: 15))
                                .foregroundColor(Color(hex: "#D97706"))
                                .frame(width: 24)

                            VStack(alignment: .leading, spacing: 2) {
                                Text("建议提前 \(event.effectiveWaitMinutes) 分钟候场")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(Color(hex: "#92400E"))

                                Text("候场开始时间: \(formatWaitStart(event: event))")
                                    .font(.system(size: 11))
                                    .foregroundColor(Color(hex: "#B45309"))
                            }
                        }
                        .padding(10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: "#FEF3C7"))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }

                    // Location Row
                    if let loc = locationFormatted {
                        HStack(spacing: 10) {
                            Image(systemName: "mappin.and.ellipse")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(palette.primary)
                                .frame(width: 24)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(loc)
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(Slate800)

                                if let deck = resolvedDeck, let onNavigateToMap = onNavigateToMap {
                                    Button(action: {
                                        onDismiss()
                                        onNavigateToMap(deck, mapHighlightTarget)
                                    }) {
                                        HStack(spacing: 4) {
                                            Image(systemName: "map.fill")
                                                .font(.system(size: 11))
                                            Text("在甲板地图中查看")
                                                .font(.system(size: 12, weight: .semibold))
                                        }
                                        .foregroundColor(palette.primary)
                                        .padding(.top, 2)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }

                    // Notes Section (Theme-Adaptive)
                    if let note = event.note?.trimmingCharacters(in: .whitespacesAndNewlines), !note.isEmpty {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "note.text")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(palette.primary)
                                .frame(width: 24)

                            VStack(alignment: .leading, spacing: 3) {
                                Text("备注")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(palette.secondary)
                                Text(note)
                                    .font(.system(size: 13))
                                    .foregroundColor(Slate800)
                            }
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(palette.primaryContainer.opacity(0.65))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(palette.primary.opacity(0.35), lineWidth: 1)
                        )
                    }

                    // Action Buttons
                    HStack(spacing: 12) {
                        Button(action: onDismiss) {
                            Text("关闭")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(Slate700)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Slate100)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        .buttonStyle(.plain)

                        if let deck = resolvedDeck, let onNavigateToMap = onNavigateToMap {
                            Button(action: {
                                onDismiss()
                                onNavigateToMap(deck, mapHighlightTarget)
                            }) {
                                HStack(spacing: 4) {
                                    Image(systemName: "map.fill")
                                    Text("去地图")
                                }
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(palette.primary)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.top, 6)
                }
                .padding(18)
            }
            .frame(maxWidth: 340)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: Color.black.opacity(0.2), radius: 20, x: 0, y: 8)
            .padding(20)
        }
    }

    @ViewBuilder
    private func posterHeaderView(url: URL) -> some View {
        ZStack(alignment: .topTrailing) {
            #if canImport(UIKit)
            if let uiImg = UIImage(contentsOfFile: url.path) {
                Image(uiImage: uiImg)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 140)
                    .clipped()
            }
            #elseif canImport(AppKit)
            if let nsImg = NSImage(contentsOf: url) {
                Image(nsImage: nsImg)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 140)
                    .clipped()
            }
            #endif

            LinearGradient(
                colors: [Color.clear, Color.black.opacity(0.4)],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 140)

            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(Color.white.opacity(0.85))
                    .padding(10)
            }
            .buttonStyle(.plain)
        }
        .frame(height: 140)
        .clipped()
    }

    private var categoryHeaderView: some View {
        HStack {
            Spacer()
            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 22))
                    .foregroundColor(Slate400)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.top, 14)
    }
}
