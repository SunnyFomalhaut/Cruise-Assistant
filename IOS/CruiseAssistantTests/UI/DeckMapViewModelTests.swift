import XCTest
@testable import CruiseAssistant

final class DeckMapViewModelTests: XCTestCase {

    override func setUpWithError() throws {
        _ = StateroomCatalog.loadDefault(bundle: CruiseAssistant.bundle)
        _ = LocationCatalog.loadDefault(bundle: CruiseAssistant.bundle)
    }

    // MARK: - parseStateroomDeck Tests

    func testParseStateroomDeckWith5DigitRoomNumber() {
        XCTAssertEqual(parseStateroomDeck("11770"), 11)
        XCTAssertEqual(parseStateroomDeck("10216"), 10)
        XCTAssertEqual(parseStateroomDeck("15012"), 15)
        XCTAssertNil(parseStateroomDeck("14001"), "Deck 14 doesn't exist on Disney Adventure")
        XCTAssertNil(parseStateroomDeck("04001"), "Deck 4 is not available for passenger maps")
    }

    func testParseStateroomDeckWith4DigitRoomNumber() {
        XCTAssertEqual(parseStateroomDeck("6012"), 6)
        XCTAssertEqual(parseStateroomDeck("5512"), 5)
        XCTAssertEqual(parseStateroomDeck("9100"), 9)
        XCTAssertNil(parseStateroomDeck("4012"), "Deck 4 is not available")
        XCTAssertNil(parseStateroomDeck(""), "Empty room number")
        XCTAssertNil(parseStateroomDeck("abc"), "Non-numeric room number")
        XCTAssertNil(parseStateroomDeck("123"), "3-digit room number not valid length")
    }

    // MARK: - searchDeckMap Tests

    func testSearchDeckMapWithEmptyQueryReturnsShortcuts() {
        let results = searchDeckMap(query: "", currentDeck: 10)
        XCTAssertFalse(results.isEmpty)

        // 第一项应为当前甲板洗手间快捷入口
        let first = results.first!
        XCTAssertEqual(first.type, .restroom)
        XCTAssertEqual(first.deckNumber, 10)
        XCTAssertEqual(first.title, "洗手间 Restroom")
        XCTAssertTrue(first.subtitle.contains("当前甲板"))

        // 应包含核心推荐设施
        let facilityResults = results.filter { $0.type == .facility }
        XCTAssertFalse(facilityResults.isEmpty)
    }

    func testSearchDeckMapWithStateroomNumber() {
        let results = searchDeckMap(query: "10216", currentDeck: 5)
        XCTAssertFalse(results.isEmpty)
        let room = results.first { $0.type == .stateroom && $0.title.contains("10216") }
        XCTAssertNotNil(room)
        XCTAssertEqual(room?.deckNumber, 10)
    }

    func testSearchDeckMapWithRestroomKeywords() {
        let keywords = ["洗手间", "厕所", "wc", "toilet", "restroom"]
        for kw in keywords {
            let results = searchDeckMap(query: kw, currentDeck: 6)
            XCTAssertFalse(results.isEmpty, "Failed for keyword: \(kw)")
            let restroomResults = results.filter { $0.type == .restroom }
            XCTAssertFalse(restroomResults.isEmpty)
            XCTAssertEqual(restroomResults.first?.deckNumber, 6)
        }
    }

    func testSearchDeckMapWithFacilityQuery() {
        let results = searchDeckMap(query: "剧院", currentDeck: 5)
        XCTAssertFalse(results.isEmpty)
        let matched = results.filter { $0.type == .facility }
        XCTAssertFalse(matched.isEmpty)
        XCTAssertTrue(matched.contains { $0.title.contains("剧院") || $0.subtitle.contains("剧院") })
    }

    func testSearchDeckMapWithPrefix11() {
        let results = searchDeckMap(query: "11", currentDeck: 5)
        XCTAssertFalse(results.isEmpty)

        // 设施应优先展示（如存在），随后列出符合 11 层前缀的客房
        let rooms = results.filter { $0.type == .stateroom }
        XCTAssertFalse(rooms.isEmpty)
        for room in rooms {
            XCTAssertEqual(room.deckNumber, 11)
        }
    }

    func testSearchDeckMapWithPrefix112ReturnsSortedRooms() {
        let results = searchDeckMap(query: "112", currentDeck: 5)
        let rooms = results.filter { $0.type == .stateroom }
        XCTAssertFalse(rooms.isEmpty)
        for room in rooms {
            XCTAssertEqual(room.deckNumber, 11)
            XCTAssertTrue(room.title.contains("112"))
        }
    }

    // MARK: - DeckMapViewModel Tests

    @MainActor
    func testDeckMapViewModelInitializationAndSelection() {
        let vm = DeckMapViewModel(initialDeck: 10, initialHighlight: "Garden Stage")
        XCTAssertEqual(vm.selectedDeck, 10)
        XCTAssertEqual(vm.highlightTarget, "Garden Stage")
        XCTAssertTrue(vm.hasMap)
        XCTAssertEqual(vm.availableDecks.count, 14)

        // 切换甲板
        vm.selectDeck(6)
        XCTAssertEqual(vm.selectedDeck, 6)

        // 选中结果
        let result = DeckSearchResult(
            title: "华特迪士尼剧院",
            subtitle: "Deck 5 · 船头",
            deckNumber: 5,
            highlightQuery: "华特迪士尼剧院",
            type: .facility
        )
        vm.selectResult(result)
        XCTAssertEqual(vm.selectedDeck, 5)
        XCTAssertEqual(vm.highlightTarget, "华特迪士尼剧院")
        XCTAssertFalse(vm.isSearchSheetPresented)

        // 导航至设施
        vm.navigateToFacility(deck: 11, highlight: "泳池")
        XCTAssertEqual(vm.selectedDeck, 11)
        XCTAssertEqual(vm.highlightTarget, "泳池")

        // 清除高亮
        vm.clearHighlight()
        XCTAssertNil(vm.highlightTarget)
    }

    @MainActor
    func testDeckMapViewModelZoomOperations() {
        let vm = DeckMapViewModel(initialDeck: 10)
        XCTAssertEqual(vm.zoomScale, 1.0)

        vm.zoomIn()
        XCTAssertEqual(vm.zoomScale, 1.5)

        vm.zoomIn()
        XCTAssertEqual(vm.zoomScale, 2.0)

        vm.zoomOut()
        XCTAssertEqual(vm.zoomScale, 1.5)

        vm.resetZoom()
        XCTAssertEqual(vm.zoomScale, 1.0)
    }

    @MainActor
    func testDeckMapViewModelSearchAndSheet() {
        let vm = DeckMapViewModel(initialDeck: 10)
        XCTAssertFalse(vm.isSearchSheetPresented)

        vm.openSearch()
        XCTAssertTrue(vm.isSearchSheetPresented)

        vm.search(query: "Palo")
        XCTAssertFalse(vm.searchResults.isEmpty)
        XCTAssertTrue(vm.searchResults.allSatisfy { $0.title.localizedCaseInsensitiveContains("Palo") || $0.subtitle.localizedCaseInsensitiveContains("Palo") || $0.highlightQuery.localizedCaseInsensitiveContains("Palo") })

        vm.closeSearch()
        XCTAssertFalse(vm.isSearchSheetPresented)
    }

    func testAllAvailableDeckMapsLoadSvgContent() {
        let passengerDecks = [5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19]
        for deck in passengerDecks {
            let svg = DeckMapRepository.loadDeckSvgContent(deckNumber: deck)
            XCTAssertNotNil(svg, "Deck \(deck) SVG should load successfully")
            XCTAssertFalse(svg?.isEmpty ?? true, "Deck \(deck) SVG content should not be empty")
        }
    }
}
