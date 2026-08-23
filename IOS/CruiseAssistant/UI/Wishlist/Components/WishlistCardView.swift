import SwiftUI

/// A card view representing a single wishlist item.
/// Supports checking off, converting to scheduled event, viewing location on map, and editing.
public struct WishlistCardView: View {
    public let item: WishlistItem
    public let category: Category?
    public let onToggleCompleted: () -> Void
    public let onConvertToSchedule: () -> Void
    public let onTapCard: (() -> Void)?
    public let onNavigateToDeckMap: ((Int, String) -> Void)?

    public init(
        item: WishlistItem,
        category: Category? = nil,
        onToggleCompleted: @escaping () -> Void,
        onConvertToSchedule: @escaping () -> Void,
        onTapCard: (() -> Void)? = nil,
        onNavigateToDeckMap: ((Int, String) -> Void)? = nil
    ) {
        self.item = item
        self.category = category
        self.onToggleCompleted = onToggleCompleted
        self.onConvertToSchedule = onConvertToSchedule
        self.onTapCard = onTapCard
        self.onNavigateToDeckMap = onNavigateToDeckMap
    }

    private var categoryColor: Color {
        if let argb = category?.colorArgb {
            return Color(argb: argb)
        }
        return CruiseAzure
    }

    private var formattedLocationString: String? {
        formatLocation(deck: item.deck, zone: item.zone, placeName: item.placeName)
    }

    public var body: some View {
        Button {
            onTapCard?()
        } label: {
            HStack(alignment: .top, spacing: 12) {
                // Checkbox Button
                Button(action: onToggleCompleted) {
                    ZStack {
                        Circle()
                            .fill(item.isCompleted ? categoryColor : Color.white)
                            .frame(width: 24, height: 24)
                            .overlay(
                                Circle()
                                    .stroke(item.isCompleted ? categoryColor : Slate300, lineWidth: 1.5)
                            )

                        if item.isCompleted {
                            Image(systemName: "checkmark")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                    .padding(4)
                }
                .buttonStyle(.plain)

                // Main Content
                VStack(alignment: .leading, spacing: 6) {
                    // Category Chip & Title
                    HStack(alignment: .center, spacing: 6) {
                        CategoryChip(
                            name: category?.name ?? "项目",
                            colorArgb: category?.colorArgb ?? 0xFF0284C7,
                            showDot: true,
                            isSelected: false
                        )

                        Text(item.title)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(item.isCompleted ? Slate400 : Slate900)
                            .strikethrough(item.isCompleted, color: Slate400)
                            .lineLimit(2)

                        Spacer()
                    }

                    // Location Info
                    if let loc = formattedLocationString, !loc.isEmpty {
                        HStack(alignment: .center, spacing: 4) {
                            Image(systemName: "mappin.and.ellipse")
                                .font(.system(size: 11))
                                .foregroundColor(item.isCompleted ? Slate400 : categoryColor)

                            Text(loc)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(item.isCompleted ? Slate400 : Slate700)
                                .lineLimit(1)

                            Spacer()

                            // Map Navigation Button
                            if let deck = item.deck, let onNavigateToDeckMap = onNavigateToDeckMap {
                                Button {
                                    let query = item.placeName?.trimmingCharacters(in: .whitespacesAndNewlines)
                                    let target = (query?.isEmpty == false) ? query! : item.title
                                    onNavigateToDeckMap(deck, target)
                                } label: {
                                    HStack(spacing: 2) {
                                        Image(systemName: "map")
                                            .font(.system(size: 10, weight: .semibold))
                                        Text("定位")
                                            .font(.system(size: 11, weight: .bold))
                                    }
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2.5)
                                    .background(CruiseAzureContainer)
                                    .foregroundColor(CruiseMarine)
                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    // Note Text
                    if let note = item.note?.trimmingCharacters(in: .whitespacesAndNewlines), !note.isEmpty {
                        Text(note)
                            .font(.system(size: 12))
                            .foregroundColor(item.isCompleted ? Slate400 : Slate500)
                            .lineLimit(2)
                    }

                    // Bottom Action Bar: Convert to Schedule Button
                    HStack {
                        Spacer()

                        Button(action: onConvertToSchedule) {
                            HStack(spacing: 4) {
                                Image(systemName: "calendar.badge.plus")
                                    .font(.system(size: 11, weight: .semibold))
                                Text("📅 转为行程")
                                    .font(.system(size: 12, weight: .semibold))
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(item.isCompleted ? Slate100 : categoryColor.opacity(0.12))
                            .foregroundColor(item.isCompleted ? Slate400 : categoryColor)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(item.isCompleted ? Slate200 : categoryColor.opacity(0.3), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(item.isCompleted ? Slate100.opacity(0.6) : SurfaceCard)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(item.isCompleted ? Slate200.opacity(0.6) : Slate200, lineWidth: 1)
            )
            .shadow(color: item.isCompleted ? Color.clear : Color.black.opacity(0.03), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}
