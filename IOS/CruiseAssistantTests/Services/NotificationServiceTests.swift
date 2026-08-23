import XCTest
@testable import CruiseAssistant

final class NotificationServiceTests: XCTestCase {

    // MARK: - Normal Event Tests

    func testComputeNotificationPlans_normalEvent_standardSlots() {
        let event = CruiseEvent(
            id: 101,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 14, minute: 0), // 14:00
            durationMinutes: 60,
            categoryId: 1,
            title: "船长见面会",
            deck: 7,
            zone: .mid,
            placeName: "中庭广场"
        )
        let category = Category(
            id: 1,
            name: "见面会",
            colorArgb: 0xFF00A5A5,
            iconKey: "meetup"
        )
        let settings = AppSettings(
            remindBefore15: true,
            remindBefore5: true,
            remindAtStart: true,
            notificationsEnabled: true
        )

        let plans = computeNotificationPlans(for: event, category: category, settings: settings)

        // 3 standard slots: 15m before (13:45), 5m before (13:55), at start (14:00)
        XCTAssertEqual(plans.count, 3)

        // Slot 0: 15m before (13:45)
        let plan15 = plans[0]
        XCTAssertEqual(plan15.identifier, "event_101_15m")
        XCTAssertEqual(plan15.title, "见面会 · 船长见面会")
        XCTAssertEqual(plan15.body, "14:00 · Deck 7 船中 中庭广场 · 还有 15 分钟开始")
        XCTAssertEqual(plan15.targetDateComponents.hour, 13)
        XCTAssertEqual(plan15.targetDateComponents.minute, 45)
        XCTAssertEqual(plan15.targetDateComponents.day, 24)
        XCTAssertEqual(plan15.targetDateComponents.month, 8)
        XCTAssertEqual(plan15.targetDateComponents.year, 2026)

        // Slot 1: 5m before (13:55)
        let plan5 = plans[1]
        XCTAssertEqual(plan5.identifier, "event_101_5m")
        XCTAssertEqual(plan5.title, "见面会 · 船长见面会")
        XCTAssertEqual(plan5.body, "14:00 · Deck 7 船中 中庭广场 · 还有 5 分钟开始")
        XCTAssertEqual(plan5.targetDateComponents.hour, 13)
        XCTAssertEqual(plan5.targetDateComponents.minute, 55)

        // Slot 2: At start (14:00)
        let planStart = plans[2]
        XCTAssertEqual(planStart.identifier, "event_101_start")
        XCTAssertEqual(planStart.title, "见面会 · 船长见面会")
        XCTAssertEqual(planStart.body, "14:00 · Deck 7 船中 中庭广场 · 现在开始")
        XCTAssertEqual(planStart.targetDateComponents.hour, 14)
        XCTAssertEqual(planStart.targetDateComponents.minute, 0)
    }

    // MARK: - Show Event Tests with Wait Time

    func testComputeNotificationPlans_showEvent_withWaitTime() {
        // Lion King show at 20:30, default wait time is 45 minutes (Wait start: 19:45)
        let event = CruiseEvent(
            id: 202,
            date: SimpleDate(year: 2026, month: 8, day: 25),
            startTime: SimpleTime(hour: 20, minute: 30), // 20:30
            durationMinutes: 60,
            categoryId: 2,
            title: "狮子王盛典",
            deck: 9,
            zone: .stern,
            placeName: "华特迪士尼剧院",
            presetKey: "show_lion_king",
            waitMinutes: 45 // Wait start: 19:45
        )
        let category = Category(
            id: 2,
            name: "演出",
            colorArgb: 0xFFF5B301,
            iconKey: "show"
        )
        let settings = AppSettings(
            remindBefore15: true,
            remindBefore5: true,
            remindAtStart: true,
            notificationsEnabled: true
        )

        let plans = computeNotificationPlans(for: event, category: category, settings: settings)

        // Expected slots:
        // 1. 15m before wait start (19:45 - 15m = 19:30) -> "还有 15 分钟开始候场"
        // 2. 5m before wait start (19:45 - 5m = 19:40) -> "还有 5 分钟开始候场"
        // 3. At wait start (19:45) -> "现在开始候场"
        // 4. Show safety slot (20:30 - 5m = 20:25) -> "还有 5 分钟开场"
        XCTAssertEqual(plans.count, 4)

        // Slot 0: 19:30
        XCTAssertEqual(plans[0].identifier, "event_202_15m")
        XCTAssertEqual(plans[0].title, "演出 · 狮子王盛典")
        XCTAssertEqual(plans[0].body, "20:30 · Deck 9 船尾 华特迪士尼剧院 · 还有 15 分钟开始候场")
        XCTAssertEqual(plans[0].targetDateComponents.hour, 19)
        XCTAssertEqual(plans[0].targetDateComponents.minute, 30)

        // Slot 1: 19:40
        XCTAssertEqual(plans[1].identifier, "event_202_5m")
        XCTAssertEqual(plans[1].body, "20:30 · Deck 9 船尾 华特迪士尼剧院 · 还有 5 分钟开始候场")
        XCTAssertEqual(plans[1].targetDateComponents.hour, 19)
        XCTAssertEqual(plans[1].targetDateComponents.minute, 40)

        // Slot 2: 19:45
        XCTAssertEqual(plans[2].identifier, "event_202_start")
        XCTAssertEqual(plans[2].body, "20:30 · Deck 9 船尾 华特迪士尼剧院 · 现在开始候场")
        XCTAssertEqual(plans[2].targetDateComponents.hour, 19)
        XCTAssertEqual(plans[2].targetDateComponents.minute, 45)

        // Slot 3: 20:25 (5m before show start)
        XCTAssertEqual(plans[3].identifier, "event_202_show_5m")
        XCTAssertEqual(plans[3].body, "20:30 · Deck 9 船尾 华特迪士尼剧院 · 还有 5 分钟开场")
        XCTAssertEqual(plans[3].targetDateComponents.hour, 20)
        XCTAssertEqual(plans[3].targetDateComponents.minute, 25)
    }

    // MARK: - Inactive Event Tests

    func testComputeNotificationPlans_inactiveEvent_producesEmptyPlans() {
        let event = CruiseEvent(
            id: 303,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 10, minute: 0),
            durationMinutes: 30,
            categoryId: 1,
            title: "已取消活动",
            isActive: false
        )
        let category = Category(id: 1, name: "琐事", colorArgb: 0xFF4A7DFF, iconKey: "chore")
        let settings = AppSettings(notificationsEnabled: true)

        let plans = computeNotificationPlans(for: event, category: category, settings: settings)
        XCTAssertTrue(plans.isEmpty)
    }

    // MARK: - Notifications Disabled Tests

    func testComputeNotificationPlans_notificationsDisabled_producesEmptyPlans() {
        let event = CruiseEvent(
            id: 304,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 10, minute: 0),
            durationMinutes: 30,
            categoryId: 1,
            title: "活动",
            isActive: true
        )
        let category = Category(id: 1, name: "琐事", colorArgb: 0xFF4A7DFF, iconKey: "chore")
        let settings = AppSettings(notificationsEnabled: false)

        let plans = computeNotificationPlans(for: event, category: category, settings: settings)
        XCTAssertTrue(plans.isEmpty)
    }

    // MARK: - Text Formatting Tests

    func testComputeNotificationPlans_textFormattingVariations() {
        // 1. Without deck / placeName
        let eventNoLocation = CruiseEvent(
            id: 401,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 9, minute: 0),
            durationMinutes: 30,
            categoryId: 1,
            title: "集合准备",
            deck: nil,
            placeName: nil
        )
        let category = Category(id: 1, name: "重要事项", colorArgb: 0xFFE8563F, iconKey: "important")
        let settings = AppSettings(remindBefore15: false, remindBefore5: false, remindAtStart: true)

        let plans1 = computeNotificationPlans(for: eventNoLocation, category: category, settings: settings)
        XCTAssertEqual(plans1.count, 1)
        XCTAssertEqual(plans1[0].title, "重要事项 · 集合准备")
        XCTAssertEqual(plans1[0].body, "09:00 · 现在开始")

        // 2. Blank category name (fallback to event title)
        let eventNoCatName = CruiseEvent(
            id: 402,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 12, minute: 30),
            durationMinutes: 45,
            categoryId: 99,
            title: "自由午餐",
            deck: 11,
            zone: .mid,
            placeName: "自助餐厅"
        )
        let emptyCategory = Category(id: 99, name: "", colorArgb: 0, iconKey: "meal")
        let plans2 = computeNotificationPlans(for: eventNoCatName, category: emptyCategory, settings: settings)
        XCTAssertEqual(plans2.count, 1)
        XCTAssertEqual(plans2[0].title, "自由午餐")
        XCTAssertEqual(plans2[0].body, "12:30 · Deck 11 船中 自助餐厅 · 现在开始")

        // 3. Blank event title (fallback to category name)
        let eventNoTitle = CruiseEvent(
            id: 403,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 18, minute: 0),
            durationMinutes: 60,
            categoryId: 1,
            title: "   ",
            deck: 4,
            placeName: "皇家宴会厅"
        )
        let plans3 = computeNotificationPlans(for: eventNoTitle, category: category, settings: settings)
        XCTAssertEqual(plans3.count, 1)
        XCTAssertEqual(plans3[0].title, "重要事项")
        XCTAssertEqual(plans3[0].body, "18:00 · Deck 4 皇家宴会厅 · 现在开始")
    }

    // MARK: - Custom ReminderMinutesBefore Tests

    func testComputeNotificationPlans_customReminderMinutesBefore() {
        let event = CruiseEvent(
            id: 501,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 16, minute: 0), // 16:00
            durationMinutes: 60,
            categoryId: 1,
            title: "特殊活动",
            reminderMinutesBefore: 30 // Custom 30m before
        )
        let category = Category(id: 1, name: "娱乐项目", colorArgb: 0xFF9B5DE5, iconKey: "fun")
        let settings = AppSettings(
            remindBefore15: true,
            remindBefore5: false,
            remindAtStart: false,
            notificationsEnabled: true
        )

        let plans = computeNotificationPlans(for: event, category: category, settings: settings)

        // Expected: 30m before (15:30) + 15m before (15:45)
        XCTAssertEqual(plans.count, 2)
        XCTAssertEqual(plans[0].identifier, "event_501_custom_30m")
        XCTAssertEqual(plans[0].body, "16:00 · 还有 30 分钟开始")
        XCTAssertEqual(plans[0].targetDateComponents.hour, 15)
        XCTAssertEqual(plans[0].targetDateComponents.minute, 30)

        XCTAssertEqual(plans[1].identifier, "event_501_15m")
        XCTAssertEqual(plans[1].body, "16:00 · 还有 15 分钟开始")
        XCTAssertEqual(plans[1].targetDateComponents.hour, 15)
        XCTAssertEqual(plans[1].targetDateComponents.minute, 45)
    }

    // MARK: - Cross-Midnight Midnight Rollover Tests

    func testComputeNotificationPlans_crossMidnightRollover() {
        // Event at 00:05 on August 25. 15m before should be 23:50 on August 24.
        let event = CruiseEvent(
            id: 601,
            date: SimpleDate(year: 2026, month: 8, day: 25),
            startTime: SimpleTime(hour: 0, minute: 5), // 00:05
            durationMinutes: 30,
            categoryId: 1,
            title: "深夜派对"
        )
        let category = Category(id: 1, name: "娱乐项目", colorArgb: 0xFF9B5DE5, iconKey: "fun")
        let settings = AppSettings(
            remindBefore15: true,
            remindBefore5: false,
            remindAtStart: false,
            notificationsEnabled: true
        )

        let plans = computeNotificationPlans(for: event, category: category, settings: settings)
        XCTAssertEqual(plans.count, 1)

        let plan = plans[0]
        XCTAssertEqual(plan.targetDateComponents.year, 2026)
        XCTAssertEqual(plan.targetDateComponents.month, 8)
        XCTAssertEqual(plan.targetDateComponents.day, 24)
        XCTAssertEqual(plan.targetDateComponents.hour, 23)
        XCTAssertEqual(plan.targetDateComponents.minute, 50)
    }

    // MARK: - Haptic Feedback Service Tests

    func testHapticFeedbackService_triggersSafely() {
        let service = HapticFeedbackService.shared

        // Ensure triggers execute safely on all platforms without crashing
        service.triggerKeypadTap()
        service.triggerSelection()
        service.triggerSuccess()
        service.triggerWarning()

        let customService = HapticFeedbackService()
        customService.triggerKeypadTap()
        customService.triggerSelection()
        customService.triggerSuccess()
        customService.triggerWarning()
    }

    // MARK: - Notification Service Protocol & Cancellation Tests

    func testNotificationService_methodsExecuteSafely() async {
        let service = NotificationService.shared

        let event = CruiseEvent(
            id: 701,
            date: SimpleDate(year: 2026, month: 8, day: 24),
            startTime: SimpleTime(hour: 10, minute: 0),
            durationMinutes: 30,
            categoryId: 1,
            title: "测试通知"
        )
        let category = Category(id: 1, name: "测试", colorArgb: 0, iconKey: "test")
        let settings = AppSettings()

        service.scheduleReminders(for: event, category: category, settings: settings)
        service.cancelReminders(for: 701)
        service.rescheduleAll(events: [event], categories: [1: category], settings: settings)

        _ = await service.requestAuthorization()
    }
}
