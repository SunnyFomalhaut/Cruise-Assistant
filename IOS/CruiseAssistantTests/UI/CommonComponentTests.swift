import XCTest
import SwiftUI
@testable import CruiseAssistant

final class CommonComponentTests: XCTestCase {

    func testCategoryChipCreation() {
        let chip1 = CategoryChip(name: "演出", colorArgb: 0xFFF5B301)
        XCTAssertEqual(chip1.name, "演出")
        XCTAssertEqual(chip1.colorArgb, 0xFFF5B301)
        XCTAssertFalse(chip1.showDot)
        XCTAssertFalse(chip1.isSelected)
        XCTAssertNil(chip1.onTap)

        let cat = Category(name: "餐食", colorArgb: 0xFF34A853, iconKey: "meal")
        var tapped = false
        let chip2 = CategoryChip(category: cat, showDot: true, isSelected: true, onTap: {
            tapped = true
        })
        XCTAssertEqual(chip2.name, "餐食")
        XCTAssertEqual(chip2.colorArgb, 0xFF34A853)
        XCTAssertTrue(chip2.showDot)
        XCTAssertTrue(chip2.isSelected)
        chip2.onTap?()
        XCTAssertTrue(tapped)
    }

    func testStarRatingViewDisplayAndBinding() {
        let display = StarRatingView(rating: 2, maxStars: 3)
        XCTAssertEqual(display.rating, 2)
        XCTAssertEqual(display.maxStars, 3)
        XCTAssertFalse(display.isInteractive)

        var ratingValue = 1
        var callbackValue = 0
        let binding = Binding<Int>(
            get: { ratingValue },
            set: { ratingValue = $0 }
        )

        let interactive = StarRatingView(
            rating: binding,
            maxStars: 3,
            allowClear: true,
            onRatingChanged: { val in
                callbackValue = val
            }
        )

        XCTAssertEqual(interactive.rating, 1)
        XCTAssertTrue(interactive.isInteractive)
        XCTAssertTrue(interactive.allowClear)
        interactive.onRatingChanged?(3)
        XCTAssertEqual(callbackValue, 3)
    }

    func testTimeWheelPickerInitialization() {
        var hour = 14
        var minute = 30
        let hBinding = Binding<Int>(get: { hour }, set: { hour = $0 })
        let mBinding = Binding<Int>(get: { minute }, set: { minute = $0 })

        let picker1 = TimeWheelPicker(hour: hBinding, minute: mBinding, minuteStep: 15)
        XCTAssertEqual(picker1.hour, 14)
        XCTAssertEqual(picker1.minute, 30)
        XCTAssertEqual(picker1.minuteStep, 15)
        XCTAssertTrue(picker1.showLabels)

        var time = SimpleTime(hour: 8, minute: 45)
        let tBinding = Binding<SimpleTime>(get: { time }, set: { time = $0 })
        let picker2 = TimeWheelPicker(time: tBinding, minuteStep: 5)
        XCTAssertEqual(picker2.hour, 8)
        XCTAssertEqual(picker2.minute, 45)
        XCTAssertEqual(picker2.minuteStep, 5)
    }

    func testEventDetailDialogViewInitializationAndCallbacks() {
        let showCategory = Category(id: 1, name: "演出", colorArgb: 0xFFF5B301, iconKey: "show")
        let importantCategory = Category(id: 2, name: "重要事项", colorArgb: 0xFFEA4335, iconKey: "important")

        let showEvent = CruiseEvent(
            id: 101,
            date: SimpleDate(year: 2026, month: 9, day: 3),
            startTime: SimpleTime(hour: 18, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "海洋奇遇 Disney Seas The Adventure",
            deck: 6,
            zone: .bow,
            placeName: "华特迪士尼剧院 Walt Disney Theatre",
            presetKey: "show_seas_adventure"
        )

        let drillEvent = CruiseEvent(
            id: 102,
            date: SimpleDate(year: 2026, month: 9, day: 3),
            startTime: SimpleTime(hour: 15, minute: 45),
            durationMinutes: 35,
            categoryId: 2,
            title: "安全演习",
            deck: 6,
            zone: .mid,
            placeName: nil,
            presetKey: "safety_drill"
        )

        var navigatedDeck: Int?
        var navigatedTarget: String?
        var dismissed = false

        let dialog1 = EventDetailDialogView(
            event: showEvent,
            category: showCategory,
            onNavigateToMap: { deck, target in
                navigatedDeck = deck
                navigatedTarget = target
            },
            onDismiss: {
                dismissed = true
            }
        )
        XCTAssertEqual(dialog1.event.id, 101)
        dialog1.onNavigateToMap?(6, "华特迪士尼剧院 Walt Disney Theatre")
        XCTAssertEqual(navigatedDeck, 6)
        XCTAssertEqual(navigatedTarget, "华特迪士尼剧院 Walt Disney Theatre")

        let dialog2 = EventDetailDialogView(
            event: drillEvent,
            category: importantCategory,
            onNavigateToMap: { deck, target in
                navigatedDeck = deck
                navigatedTarget = target
            },
            onDismiss: {
                dismissed = true
            }
        )
        XCTAssertEqual(dialog2.event.id, 102)
        dialog2.onDismiss()
        XCTAssertTrue(dismissed)
    }
}
