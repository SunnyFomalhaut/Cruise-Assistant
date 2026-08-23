import SwiftUI

/// 搜索类别筛选枚举
public enum SearchFilterCategory: String, CaseIterable, Identifiable, Sendable {
    case all = "全部"
    case facility = "设施"
    case stateroom = "舱房"
    case restroom = "洗手间"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .all: return "square.grid.2x2.fill"
        case .facility: return "sparkles"
        case .stateroom: return "door.left.hand.closed"
        case .restroom: return "figure.dress.line.vertical.figure"
        }
    }
}

/// 甲板地图搜索与快速定位底部抽屉
public struct DeckMapSearchSheet: View {
    public let currentDeck: Int
    public let onSelectResult: (DeckSearchResult) -> Void
    public let onDismiss: () -> Void

    @State private var query: String = ""
    @State private var selectedFilter: SearchFilterCategory = .all

    public init(
        currentDeck: Int,
        onSelectResult: @escaping (DeckSearchResult) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.currentDeck = currentDeck
        self.onSelectResult = onSelectResult
        self.onDismiss = onDismiss
    }

    private var allResults: [DeckSearchResult] {
        searchDeckMap(query: query, currentDeck: currentDeck)
    }

    private var filteredResults: [DeckSearchResult] {
        let raw = allResults
        switch selectedFilter {
        case .all:
            return raw
        case .facility:
            return raw.filter { $0.type == .facility }
        case .stateroom:
            return raw.filter { $0.type == .stateroom }
        case .restroom:
            return raw.filter { $0.type == .restroom }
        }
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 顶部搜索框
                searchBarSection
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 10)

                // 筛选分类 Chips
                filterChipsSection
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)

                Divider()

                // 搜索结果列表
                if filteredResults.isEmpty {
                    emptyResultsView
                } else {
                    resultsListView
                }
            }
            .background(Slate50)
            .navigationTitle("搜索全船地点 & 客房")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(Slate400)
                            .font(.system(size: 20))
                    }
                }
            }
        }
    }

    // MARK: - Search Bar

    private var searchBarSection: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(Slate400)
                .font(.system(size: 16))

            TextField("搜索设施名称、客房号（如 10216）或洗手间", text: $query)
                .font(.system(size: 15))
                .textFieldStyle(PlainTextFieldStyle())
                .autocorrectionDisabled()

            if !query.isEmpty {
                Button(action: { query = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(Slate400)
                        .font(.system(size: 16))
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.white)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Slate200, lineWidth: 1)
        )
    }

    // MARK: - Filter Chips

    private var filterChipsSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(SearchFilterCategory.allCases) { filter in
                    let isSelected = selectedFilter == filter
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.15)) {
                            selectedFilter = filter
                        }
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: filter.iconName)
                                .font(.system(size: 12))
                            Text(filter.rawValue)
                                .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(isSelected ? CruiseAzure : Color.white)
                        .foregroundColor(isSelected ? .white : Slate700)
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(isSelected ? CruiseAzure : Slate200, lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
        }
    }

    // MARK: - Results List

    private var resultsListView: some View {
        List {
            Section(header: sectionHeader) {
                ForEach(filteredResults) { result in
                    SearchResultRow(result: result) {
                        onSelectResult(result)
                    }
                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                }
            }
        }
        .listStyle(PlainListStyle())
        .background(Slate50)
    }

    private var sectionHeader: some View {
        HStack {
            Text(query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "快捷推荐" : "搜索结果 (\(filteredResults.count))")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Slate500)
            Spacer()
        }
        .padding(.vertical, 4)
    }

    // MARK: - Empty State

    private var emptyResultsView: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "magnifyingglass")
                .font(.system(size: 40))
                .foregroundColor(Slate300)
            Text("未找到相关设施或客房")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Slate700)
            Text("请尝试搜索设施名称、客房号（如 10216）或洗手间")
                .font(.system(size: 13))
                .foregroundColor(Slate500)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// 搜索条目行视图
public struct SearchResultRow: View {
    public let result: DeckSearchResult
    public let onTap: () -> Void

    public init(result: DeckSearchResult, onTap: @escaping () -> Void) {
        self.result = result
        self.onTap = onTap
    }

    public var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // 左侧类别图标
                iconContainer

                // 中间标题与副标题
                VStack(alignment: .leading, spacing: 3) {
                    Text(result.title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Slate900)
                        .lineLimit(1)

                    Text(result.subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(Slate500)
                        .lineLimit(1)
                }

                Spacer()

                // 右侧甲板层级徽章
                Text("Deck \(result.deckNumber)")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(CruiseMarine)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(CruiseAzureContainer)
                    .cornerRadius(8)

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Slate400)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Slate200, lineWidth: 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private var iconContainer: some View {
        let (iconName, iconColor, bgColor) = iconStyling
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(bgColor)
                .frame(width: 38, height: 38)

            Image(systemName: iconName)
                .font(.system(size: 17))
                .foregroundColor(iconColor)
        }
    }

    private var iconStyling: (iconName: String, iconColor: Color, bgColor: Color) {
        switch result.type {
        case .facility:
            let catKey = result.categoryKey ?? ""
            let icon = categoryIconSystemName(for: catKey)
            return (icon, CruiseAzure, CruiseAzureContainer.opacity(0.6))
        case .stateroom:
            return ("door.left.hand.closed", Color(hex: "#7C3AED"), Color(hex: "#F5F3FF"))
        case .restroom:
            return ("figure.dress.line.vertical.figure", Color(hex: "#059669"), Color(hex: "#ECFDF5"))
        }
    }
}

#Preview {
    DeckMapSearchSheet(
        currentDeck: 10,
        onSelectResult: { _ in },
        onDismiss: {}
    )
}
