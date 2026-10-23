import Foundation
import SwiftUI

/// 甲板地图视图模型
@MainActor
public final class DeckMapViewModel: ObservableObject {
    @Published public var selectedDeck: Int
    @Published public var searchQuery: String = ""
    @Published public var searchResults: [DeckSearchResult] = []
    @Published public var highlightTarget: String? = nil
    @Published public var pixieMode: Bool = false
    @Published public var pixieMarkers: [PixieRoomMarker] = []
    @Published public var zoomScale: CGFloat = 1.0
    @Published public var isSearchSheetPresented: Bool = false

    public let availableDecks: [Int] = AVAILABLE_DECKS
    public let decks: [DeckInfo]
    public let facilities: [FacilityItemUiModel]

    public var hasMap: Bool {
        DeckMapRepository.hasDeckMap(deckNumber: selectedDeck)
    }

    public init(
        initialDeck: Int = 10,
        initialHighlight: String? = nil,
        maxDeck: Int = DeckMapRepository.defaultMaxDeck,
        facilities: [FacilityItemUiModel] = getAllFacilityItems()
    ) {
        let validatedDeck = DeckMapRepository.hasDeckMap(deckNumber: initialDeck) ? initialDeck : 10
        self.selectedDeck = validatedDeck
        self.highlightTarget = initialHighlight
        self.decks = DeckMapRepository.getDecks(maxDeck: maxDeck)
        self.facilities = facilities
        self.searchResults = searchDeckMap(query: "", currentDeck: validatedDeck, facilities: facilities)
    }

    /// 切换当前选中甲板
    public func selectDeck(_ deck: Int) {
        guard selectedDeck != deck else { return }
        selectedDeck = deck
        // 切换甲板时清除上一层的仙尘标记
        if pixieMode {
            pixieMode = false
            pixieMarkers = []
        }
        // 如果当前有搜索推荐，刷新当前甲板快捷推荐
        if searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            searchResults = searchDeckMap(query: "", currentDeck: deck, facilities: facilities)
        }
    }

    /// 进入仙尘地图模式（高亮本层四色房间徽标）
    public func enterPixieMode(deck: Int, markers: [PixieRoomMarker]) {
        self.selectedDeck = DeckMapRepository.hasDeckMap(deckNumber: deck) ? deck : selectedDeck
        self.highlightTarget = nil
        self.pixieMarkers = markers
        self.pixieMode = true
    }

    /// 退出仙尘地图模式
    public func clearPixieMode() {
        self.pixieMode = false
        self.pixieMarkers = []
    }

    /// 执行搜索
    public func search(query: String) {
        self.searchQuery = query
        self.searchResults = searchDeckMap(
            query: query,
            currentDeck: selectedDeck,
            facilities: facilities
        )
    }

    /// 选中搜索结果条目
    public func selectResult(_ result: DeckSearchResult) {
        self.selectedDeck = result.deckNumber
        self.highlightTarget = result.highlightQuery
        self.pixieMode = false
        self.pixieMarkers = []
        self.isSearchSheetPresented = false
    }

    /// 导航定位至指定地点/设施
    public func navigateToFacility(deck: Int, highlight: String) {
        self.selectedDeck = deck
        self.highlightTarget = highlight
        self.pixieMode = false
        self.pixieMarkers = []
    }

    /// 清除当前高亮图钉
    public func clearHighlight() {
        self.highlightTarget = nil
    }

    /// 放大地图
    public func zoomIn() {
        zoomScale = min(zoomScale + 0.5, 5.0)
    }

    /// 缩小地图
    public func zoomOut() {
        zoomScale = max(zoomScale - 0.5, 1.0)
    }

    /// 重置地图缩放
    public func resetZoom() {
        zoomScale = 1.0
    }

    /// 打开搜索面板
    public func openSearch() {
        isSearchSheetPresented = true
    }

    /// 关闭搜索面板
    public func closeSearch() {
        isSearchSheetPresented = false
    }
}
