import Foundation
#if canImport(UserNotifications)
import UserNotifications
#endif

/// A computed notification trigger plan ready for platform scheduling.
public struct NotificationTriggerPlan: Hashable, Sendable {
    public let identifier: String
    public let title: String
    public let body: String
    public let targetDateComponents: DateComponents
    public let targetEpochMinutes: Int

    public init(
        identifier: String,
        title: String,
        body: String,
        targetDateComponents: DateComponents,
        targetEpochMinutes: Int
    ) {
        self.identifier = identifier
        self.title = title
        self.body = body
        self.targetDateComponents = targetDateComponents
        self.targetEpochMinutes = targetEpochMinutes
    }
}

/// Pure domain helper that calculates notification trigger plans for an event based on category and settings.
public func computeNotificationPlans(
    for event: CruiseEvent,
    category: Category?,
    settings: AppSettings
) -> [NotificationTriggerPlan] {
    guard settings.notificationsEnabled, event.isActive else {
        return []
    }

    let isShow = category?.iconKey == "show"
    let categoryName = category?.name.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    let eventTitle = event.title.trimmingCharacters(in: .whitespacesAndNewlines)

    let titleLine: String
    if categoryName.isEmpty {
        titleLine = eventTitle
    } else if eventTitle.isEmpty {
        titleLine = categoryName
    } else {
        titleLine = "\(categoryName) · \(eventTitle)"
    }

    let timeStr = String(format: "%02d:%02d", event.startTime.hour, event.startTime.minute)
    let location = formatLocation(deck: event.deck, zone: event.zone, placeName: event.placeName)

    let eventStartEpochMinutes = event.date.epochDay * 1440 + event.startTime.totalMinutes
    let waitMinutes = isShow ? event.effectiveWaitMinutes : 0
    let baseEpochMinutes = isShow ? (eventStartEpochMinutes - waitMinutes) : eventStartEpochMinutes

    var plans: [NotificationTriggerPlan] = []
    var scheduledEpochs: Set<Int> = []

    func createPlan(identifier: String, targetEpochMinutes: Int, suffix: String) -> NotificationTriggerPlan {
        var bodyParts = [timeStr]
        if let loc = location, !loc.isEmpty {
            bodyParts.append(loc)
        }
        let baseBody = bodyParts.joined(separator: " · ")
        let bodyLine = "\(baseBody)\(suffix)"

        let targetEpochDay: Int
        if targetEpochMinutes >= 0 {
            targetEpochDay = targetEpochMinutes / 1440
        } else {
            targetEpochDay = (targetEpochMinutes - 1439) / 1440
        }
        let targetMinuteOfDay = ((targetEpochMinutes % 1440) + 1440) % 1440
        let targetDate = SimpleDate(epochDay: targetEpochDay)
        let targetHour = targetMinuteOfDay / 60
        let targetMinute = targetMinuteOfDay % 60

        var dc = DateComponents()
        dc.year = targetDate.year
        dc.month = targetDate.month
        dc.day = targetDate.day
        dc.hour = targetHour
        dc.minute = targetMinute

        return NotificationTriggerPlan(
            identifier: identifier,
            title: titleLine,
            body: bodyLine,
            targetDateComponents: dc,
            targetEpochMinutes: targetEpochMinutes
        )
    }

    // Slot 0: 15 minutes before wait start (or event start)
    if settings.remindBefore15 {
        let epoch = baseEpochMinutes - 15
        let suffix = isShow ? (waitMinutes > 0 ? " · 还有 15 分钟开始候场" : " · 还有 15 分钟开始") : " · 还有 15 分钟开始"
        plans.append(createPlan(identifier: "event_\(event.id)_15m", targetEpochMinutes: epoch, suffix: suffix))
        scheduledEpochs.insert(epoch)
    }

    // Slot 1: 5 minutes before wait start (or event start)
    if settings.remindBefore5 {
        let epoch = baseEpochMinutes - 5
        let suffix = isShow ? (waitMinutes > 0 ? " · 还有 5 分钟开始候场" : " · 还有 5 分钟开始") : " · 还有 5 分钟开始"
        plans.append(createPlan(identifier: "event_\(event.id)_5m", targetEpochMinutes: epoch, suffix: suffix))
        scheduledEpochs.insert(epoch)
    }

    // Slot 2: At wait start (or event start)
    if settings.remindAtStart {
        let epoch = baseEpochMinutes
        let suffix = isShow ? (waitMinutes > 0 ? " · 现在开始候场" : " · 现在开始") : " · 现在开始"
        plans.append(createPlan(identifier: "event_\(event.id)_start", targetEpochMinutes: epoch, suffix: suffix))
        scheduledEpochs.insert(epoch)
    }

    // Slot 3: Show safety slot (5 minutes before show start, when wait > 5m)
    if isShow && waitMinutes > 5 {
        let openEpoch = eventStartEpochMinutes - 5
        if !scheduledEpochs.contains(openEpoch) {
            plans.append(createPlan(identifier: "event_\(event.id)_show_5m", targetEpochMinutes: openEpoch, suffix: " · 还有 5 分钟开场"))
            scheduledEpochs.insert(openEpoch)
        }
    }

    // Custom reminderMinutesBefore (if specified and not redundant)
    if let customLead = event.reminderMinutesBefore, customLead > 0 {
        let customEpoch = baseEpochMinutes - customLead
        if !scheduledEpochs.contains(customEpoch) {
            let suffix = isShow ? (waitMinutes > 0 ? " · 还有 \(customLead) 分钟开始候场" : " · 还有 \(customLead) 分钟开始") : " · 还有 \(customLead) 分钟开始"
            plans.append(createPlan(identifier: "event_\(event.id)_custom_\(customLead)m", targetEpochMinutes: customEpoch, suffix: suffix))
            scheduledEpochs.insert(customEpoch)
        }
    }

    return plans.sorted { $0.targetEpochMinutes < $1.targetEpochMinutes }
}

/// Protocol managing notification authorization and offline local notification scheduling.
public protocol NotificationServiceProtocol: Sendable {
    func requestAuthorization() async -> Bool
    func scheduleReminders(for event: CruiseEvent, category: Category?, settings: AppSettings)
    func cancelReminders(for eventId: Int64)
    func rescheduleAll(events: [CruiseEvent], categories: [Int64: Category], settings: AppSettings)
}

/// Offline local notification service implementing `NotificationServiceProtocol`.
public final class NotificationService: NotificationServiceProtocol, @unchecked Sendable {
    public static let shared = NotificationService()

    public init() {}

    private var isNotificationCenterAvailable: Bool {
        #if canImport(UserNotifications)
        // UNUserNotificationCenter.current() requires a valid application bundle with an identifier.
        // In CLI test runners (swift test / xctest CLI), accessing currentNotificationCenter throws NSInternalInconsistencyException.
        guard let bundleId = Bundle.main.bundleIdentifier,
              !bundleId.isEmpty,
              !bundleId.hasPrefix("com.apple.dt.xctest") else {
            return false
        }
        return true
        #else
        return false
        #endif
    }

    public func requestAuthorization() async -> Bool {
        #if canImport(UserNotifications)
        guard isNotificationCenterAvailable else { return false }
        do {
            let center = UNUserNotificationCenter.current()
            return try await center.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            return false
        }
        #else
        return false
        #endif
    }

    public func scheduleReminders(for event: CruiseEvent, category: Category?, settings: AppSettings) {
        cancelReminders(for: event.id)
        guard settings.notificationsEnabled, event.isActive else { return }

        let plans = computeNotificationPlans(for: event, category: category, settings: settings)
        #if canImport(UserNotifications)
        guard isNotificationCenterAvailable else { return }
        let center = UNUserNotificationCenter.current()
        for plan in plans {
            let content = UNMutableNotificationContent()
            content.title = plan.title
            content.body = plan.body
            content.sound = .default
            content.userInfo = ["eventId": event.id]

            let trigger = UNCalendarNotificationTrigger(dateMatching: plan.targetDateComponents, repeats: false)
            let request = UNNotificationRequest(identifier: plan.identifier, content: content, trigger: trigger)
            center.add(request)
        }
        #endif
    }

    public func cancelReminders(for eventId: Int64) {
        #if canImport(UserNotifications)
        guard isNotificationCenterAvailable else { return }
        let center = UNUserNotificationCenter.current()
        let deterministicIds = [
            "event_\(eventId)_15m",
            "event_\(eventId)_5m",
            "event_\(eventId)_start",
            "event_\(eventId)_show_5m",
        ]
        center.removePendingNotificationRequests(withIdentifiers: deterministicIds)

        center.getPendingNotificationRequests { requests in
            let matchingIds = requests
                .map(\.identifier)
                .filter { $0.hasPrefix("event_\(eventId)_") }
            if !matchingIds.isEmpty {
                center.removePendingNotificationRequests(withIdentifiers: matchingIds)
            }
        }
        #endif
    }

    public func rescheduleAll(events: [CruiseEvent], categories: [Int64: Category], settings: AppSettings) {
        #if canImport(UserNotifications)
        guard isNotificationCenterAvailable else { return }
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()

        guard settings.notificationsEnabled else { return }

        for event in events where event.isActive {
            let category = categories[event.categoryId]
            let plans = computeNotificationPlans(for: event, category: category, settings: settings)
            for plan in plans {
                let content = UNMutableNotificationContent()
                content.title = plan.title
                content.body = plan.body
                content.sound = .default
                content.userInfo = ["eventId": event.id]

                let trigger = UNCalendarNotificationTrigger(dateMatching: plan.targetDateComponents, repeats: false)
                let request = UNNotificationRequest(identifier: plan.identifier, content: content, trigger: trigger)
                center.add(request)
            }
        }
        #endif
    }
}
