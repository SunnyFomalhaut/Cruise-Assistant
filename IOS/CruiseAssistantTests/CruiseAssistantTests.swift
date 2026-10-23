import XCTest
@testable import CruiseAssistant

final class CruiseAssistantTests: XCTestCase {
    func testVersion() {
        XCTAssertEqual(CruiseAssistant.version, "1.12.1")
    }

    func testBundleModuleExists() {
        let bundle = CruiseAssistant.bundle
        XCTAssertNotNil(bundle)
    }

    func testStateroomsJsonLoads() throws {
        let bundle = CruiseAssistant.bundle
        let url = bundle.url(forResource: "staterooms", withExtension: "json") 
            ?? bundle.url(forResource: "staterooms", withExtension: "json", subdirectory: "Raw")
        XCTAssertNotNil(url, "staterooms.json must exist in bundle")
        if let url = url {
            let data = try Data(contentsOf: url)
            XCTAssertFalse(data.isEmpty)
        }
    }

    func testLocationsJsonLoads() throws {
        let bundle = CruiseAssistant.bundle
        let url = bundle.url(forResource: "locations", withExtension: "json")
            ?? bundle.url(forResource: "locations", withExtension: "json", subdirectory: "Raw")
        XCTAssertNotNil(url, "locations.json must exist in bundle")
        if let url = url {
            let data = try Data(contentsOf: url)
            XCTAssertFalse(data.isEmpty)
        }
    }

    func testDeckSvgsLoad() throws {
        let bundle = CruiseAssistant.bundle
        let deckNumbers = [5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19]
        for deck in deckNumbers {
            let name = "deck_\(deck)"
            let url = bundle.url(forResource: name, withExtension: "svg")
                ?? bundle.url(forResource: name, withExtension: "svg", subdirectory: "Raw/Decks")
                ?? bundle.url(forResource: name, withExtension: "svg", subdirectory: "Decks")
            XCTAssertNotNil(url, "\(name).svg must exist in bundle")
            if let url = url {
                let content = try String(contentsOf: url, encoding: .utf8)
                XCTAssertTrue(content.contains("<svg"), "\(name).svg should contain <svg tag")
            }
        }
    }
}
