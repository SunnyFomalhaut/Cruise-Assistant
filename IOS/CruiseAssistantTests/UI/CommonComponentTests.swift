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
}
