import SwiftUI

/// Maps a category iconKey to a corresponding SF Symbol name.
public func categoryIconSystemName(for iconKey: String) -> String {
    switch iconKey {
    case "meal": return "fork.knife"
    case "show": return "play.tv.fill"
    case "important": return "exclamationmark.triangle.fill"
    case "chore": return "square.grid.2x2"
    case "fun": return "gamecontroller.fill"
    case "meetup": return "person.2.fill"
    case "boat": return "ferry.fill"
    case "pool": return "figure.pool.swim"
    case "bar": return "wineglass.fill"
    case "spa": return "sparkles"
    case "gym": return "dumbbell.fill"
    case "run": return "figure.run"
    case "shop": return "bag.fill"
    case "photo": return "camera.fill"
    case "book": return "book.fill"
    case "party": return "party.popper.fill"
    case "sleep": return "moon.fill"
    case "work": return "briefcase.fill"
    case "youth": return "figure.2.and.child.holdinghands"
    case "person": return "person.fill"
    case "check": return "checkmark.circle.fill"
    case "trash": return "trash.fill"
    case "gift": return "gift.fill"
    case "sparkle": return "sparkles"
    case "diamond": return "diamond.fill"
    case "star": return "star.fill"
    case "heart": return "heart.fill"
    case "search": return "magnifyingglass"
    default: return "tag.fill"
    }
}

/// A grid-based category selector displaying all available categories with icons and ARGB color highlights.
public struct CategorySelector: View {
    public let categories: [Category]
    public let selectedCategoryId: Int64?
    public let isEnabled: Bool
    public let onSelectCategory: (Category) -> Void
    public let onManageCategories: (() -> Void)?

    private let columns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8)
    ]

    public init(
        categories: [Category],
        selectedCategoryId: Int64?,
        isEnabled: Bool = true,
        onSelectCategory: @escaping (Category) -> Void,
        onManageCategories: (() -> Void)? = nil
    ) {
        self.categories = categories
        self.selectedCategoryId = selectedCategoryId
        self.isEnabled = isEnabled
        self.onSelectCategory = onSelectCategory
        self.onManageCategories = onManageCategories
    }

    public var body: some View {
        LazyVGrid(columns: columns, spacing: 8) {
            ForEach(categories) { category in
                let isSelected = category.id == selectedCategoryId
                let catColor = Color(argb: category.colorArgb)

                Button(action: {
                    if isEnabled {
                        onSelectCategory(category)
                    }
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: categoryIconSystemName(for: category.iconKey))
                            .font(.system(size: 20))
                            .foregroundColor(isSelected ? catColor : Slate700)
                            .frame(height: 24)

                        Text(category.name)
                            .font(.system(size: 11, weight: isSelected ? .bold : .regular))
                            .foregroundColor(isSelected ? Slate900 : Slate700)
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 64)
                    .background(isSelected ? catColor.opacity(0.16) : Slate100)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(isSelected ? catColor : Slate200, lineWidth: isSelected ? 1.5 : 1)
                    )
                }
                .buttonStyle(PlainButtonStyle())
                .disabled(!isEnabled)
            }

            if let onManageCategories = onManageCategories {
                Button(action: {
                    if isEnabled {
                        onManageCategories()
                    }
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: "plus")
                            .font(.system(size: 20, weight: .medium))
                            .foregroundColor(Slate500)
                            .frame(height: 24)

                        Text("管理类别")
                            .font(.system(size: 11))
                            .foregroundColor(Slate500)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 64)
                    .background(Slate100)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(Slate200, lineWidth: 1)
                    )
                }
                .buttonStyle(PlainButtonStyle())
                .disabled(!isEnabled)
            }
        }
    }
}

#Preview {
    CategorySelector(
        categories: BUILT_IN_CATEGORIES,
        selectedCategoryId: BUILT_IN_CATEGORIES.first?.id,
        onSelectCategory: { _ in }
    )
    .padding()
}
