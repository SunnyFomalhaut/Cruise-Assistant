import XCTest

/// End-to-end UI tests for the main user flows.
///
/// The app is launched with an in-memory store, so every test starts at onboarding and never touches
/// the persisted data on the simulator or device.
@MainActor
final class CruiseAssistantUITests: XCTestCase {
    /// Must match `CruiseAssistantApp.uiTestingInMemoryArgument`.
    private static let inMemoryArgument = "-UITestingInMemoryStore"
    private static let importedArchiveName = "UITest Import 01"

    private var app: XCUIApplication!

    private var springboard: XCUIApplication {
        XCUIApplication(bundleIdentifier: "com.apple.springboard")
    }

    // MARK: - Tests

    func testPixieDustGivenReceivedAndLinkage() throws {
        launchAndCompleteOnboarding()

        openTab("仙尘")
        XCTAssertTrue(button("发出", exact: true).waitForExistence(timeout: 5))
        recordGift(room: "10200", submitPrefix: "记一笔发出")
        recordGift(room: "10101", submitPrefix: "记一笔发出")
        XCTAssertTrue(label("发出记录 (2)").waitForExistence(timeout: 3))
        XCTAssertTrue(label("10200").exists, "Room 10200 missing from given list")
        screenshot("pixie-given")

        let viewModeToggle = app.buttons
            .matching(NSPredicate(format: "label CONTAINS %@ OR label CONTAINS %@", "按房间归组", "时间流水"))
            .firstMatch
        XCTAssertTrue(viewModeToggle.waitForExistence(timeout: 3))
        viewModeToggle.tap()
        XCTAssertTrue(label("10101").waitForExistence(timeout: 3), "Room 10101 missing after switching view mode")
        screenshot("pixie-given-toggled")

        button("收到", exact: true).tap()
        recordGift(room: "10103", submitPrefix: "记一笔收到")
        XCTAssertTrue(label("1 待回礼").waitForExistence(timeout: 3), "Unreplied counter missing in received list")
        screenshot("pixie-received")

        button("互赠明细", exact: true).tap()
        XCTAssertTrue(label("全船共 3 间").waitForExistence(timeout: 5), "Linkage summary missing")
        // Rooms are grouped per deck and collapsed by default.
        button("(3 间)").tap()
        XCTAssertTrue(label("10103").waitForExistence(timeout: 3), "Room 10103 missing in linkage view")
        XCTAssertTrue(label("待回礼 1").exists, "Unreplied badge missing on room card")
        screenshot("pixie-linkage")
    }

    func testArchiveExportAndImportWithCustomName() throws {
        launchAndCompleteOnboarding()
        openTab("设置")

        scrollToHittable(button("将当前航程存入历史存档")).tap()
        let archiveRow = button("仙尘 0")
        scrollToHittable(archiveRow)
        screenshot("archive-list")

        archiveRow.tap()
        let detailBar = app.navigationBars["航程存档详情"]
        XCTAssertTrue(detailBar.waitForExistence(timeout: 5))
        XCTAssertTrue(label("Disney Adventure").exists, "Unnamed archive should fall back to the ship name")
        screenshot("archive-detail")

        scrollToHittable(button("导出此存档")).tap()
        let copy = button("复制全部内容")
        XCTAssertTrue(copy.waitForExistence(timeout: 5), "Export sheet did not appear")
        XCTAssertTrue(label("\"voyage\"").exists, "Exported archive JSON is empty")
        copy.tap()
        XCTAssertTrue(label("已复制到剪贴板").waitForExistence(timeout: 3))
        app.navigationBars["导出数据"].buttons["完成"].tap()

        scrollToHittable(button("导入航程存档")).tap()
        let nameField = app.textFields.firstMatch
        XCTAssertTrue(nameField.waitForExistence(timeout: 5), "Import sheet did not appear")
        nameField.tap()
        nameField.typeText(Self.importedArchiveName)
        pasteIntoTextView(app.textViews.firstMatch)
        let confirm = button("确认导入存档")
        waitUntilEnabled(confirm)
        confirm.tap()

        let importedRow = button(Self.importedArchiveName)
        scrollToHittable(importedRow)
        screenshot("archive-list-imported")

        importedRow.tap()
        XCTAssertTrue(detailBar.waitForExistence(timeout: 5))
        XCTAssertTrue(label(Self.importedArchiveName).exists, "Custom archive name not shown in detail")
        screenshot("archive-imported-detail")
        detailBar.buttons["关闭"].tap()
    }

    func testFullBackupExportShowsJSON() throws {
        launchAndCompleteOnboarding()
        openTab("设置")

        scrollToHittable(button("导出完整数据库备份")).tap()
        XCTAssertTrue(button("复制全部内容").waitForExistence(timeout: 5), "Backup sheet did not appear")
        XCTAssertTrue(label("\"voyageArchives\"").exists, "Full backup JSON is empty")
        screenshot("full-backup-export")
        app.navigationBars["导出数据"].buttons["完成"].tap()
    }

    // MARK: - Flow helpers

    /// Launches with a clean store and finishes onboarding with a 4+3 night multi-leg voyage.
    private func launchAndCompleteOnboarding() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = [Self.inMemoryArgument]
        app.launch()

        let generate = button("生成航程时间轴")
        XCTAssertTrue(generate.waitForExistence(timeout: 10), "Onboarding did not appear")
        button("五天四夜").tap()
        scrollToHittable(button("续订下一程")).tap()
        XCTAssertTrue(label("4+3夜 · 共7晚").waitForExistence(timeout: 3), "Multi-leg summary missing")
        screenshot("onboarding-multileg")
        scrollToHittable(generate).tap()
        dismissSystemAlertIfPresent()

        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout: 10), "Main tab bar did not appear")
        dismissSystemAlertIfPresent(timeout: 2)
    }

    private func recordGift(room: String, submitPrefix: String) {
        let clear = app.buttons["清空房号"]
        if clear.exists && clear.isHittable { clear.tap() }
        for digit in room {
            app.buttons[String(digit)].firstMatch.tap()
        }
        let submit = button("\(submitPrefix) (\(room))")
        waitUntilEnabled(submit)
        submit.tap()
    }

    /// Pastes the pasteboard into a text view, trying hardware ⌘V, the QuickType paste suggestion, then the edit menu.
    private func pasteIntoTextView(_ textView: XCUIElement) {
        textView.tap()
        textView.typeKey("v", modifierFlags: .command)
        dismissSystemAlertIfPresent(timeout: 2)
        if didPaste(into: textView) { return }

        let suggestion = app.buttons["RecentPaste"].firstMatch
        if suggestion.waitForExistence(timeout: 2) {
            suggestion.tap()
            dismissSystemAlertIfPresent(timeout: 2)
            if didPaste(into: textView) { return }
        }

        textView.press(forDuration: 1.2)
        let menuPaste = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@ OR label == %@", "粘贴", "Paste"))
            .firstMatch
        if menuPaste.waitForExistence(timeout: 2) {
            menuPaste.tap()
            dismissSystemAlertIfPresent(timeout: 2)
            if didPaste(into: textView) { return }
        }
        XCTFail("Could not paste archive JSON into the import editor")
    }

    private func didPaste(into textView: XCUIElement) -> Bool {
        sleep(1)
        return (textView.value as? String)?.contains("voyage") == true
    }

    // MARK: - Element helpers

    private func button(_ text: String, exact: Bool = false) -> XCUIElement {
        exact
            ? app.buttons[text].firstMatch
            : app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    private func label(_ text: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    private func openTab(_ title: String) {
        let tab = app.tabBars.buttons[title]
        if tab.waitForExistence(timeout: 3) {
            tab.tap()
            return
        }
        // iPhone folds more than five tabs into "More".
        for more in ["More", "更多"] where app.tabBars.buttons[more].exists {
            app.tabBars.buttons[more].tap()
            let row = app.staticTexts[title].firstMatch
            XCTAssertTrue(row.waitForExistence(timeout: 3), "Tab \(title) not found under \(more)")
            row.tap()
            return
        }
        XCTFail("Tab \(title) not found")
    }

    @discardableResult
    private func scrollToHittable(
        _ element: XCUIElement,
        maxSwipes: Int = 15,
        file: StaticString = #filePath,
        line: UInt = #line
    ) -> XCUIElement {
        var swipes = 0
        while !(element.exists && element.isHittable) && swipes < maxSwipes {
            app.swipeUp(velocity: .slow)
            swipes += 1
        }
        XCTAssertTrue(element.exists && element.isHittable, "Could not scroll to \(element)", file: file, line: line)
        return element
    }

    private func waitUntilEnabled(
        _ element: XCUIElement,
        timeout: TimeInterval = 5,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let predicate = NSPredicate(format: "exists == true AND isEnabled == true")
        let expectation = XCTNSPredicateExpectation(predicate: predicate, object: element)
        let result = XCTWaiter().wait(for: [expectation], timeout: timeout)
        XCTAssertEqual(result, .completed, "\(element) never became enabled", file: file, line: line)
    }

    /// Accepts system permission prompts (notifications, paste) if one appears.
    private func dismissSystemAlertIfPresent(timeout: TimeInterval = 3) {
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: timeout) else { return }
        for title in ["Allow", "允许", "Allow Paste", "允许粘贴", "OK", "好"] where alert.buttons[title].exists {
            alert.buttons[title].tap()
            return
        }
    }

    private func screenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
