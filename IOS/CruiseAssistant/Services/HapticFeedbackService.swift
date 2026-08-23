import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Protocol defining haptic tactile feedback triggers.
public protocol HapticFeedbackServiceProtocol: Sendable {
    func triggerKeypadTap()
    func triggerSelection()
    func triggerSuccess()
    func triggerWarning()
}

/// Service providing offline tactile haptic feedback for user interactions.
/// Safeguarded for non-iOS, simulator, and macOS environments.
public final class HapticFeedbackService: HapticFeedbackServiceProtocol, @unchecked Sendable {
    public static let shared = HapticFeedbackService()

    public init() {}

    /// Light tactile feedback for 9-key keypad buttons.
    public func triggerKeypadTap() {
        #if os(iOS)
        DispatchQueue.main.async {
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.prepare()
            generator.impactOccurred()
        }
        #endif
    }

    /// Selection changed feedback for pickers and segmented controls.
    public func triggerSelection() {
        #if os(iOS)
        DispatchQueue.main.async {
            let generator = UISelectionFeedbackGenerator()
            generator.prepare()
            generator.selectionChanged()
        }
        #endif
    }

    /// Success notification feedback.
    public func triggerSuccess() {
        #if os(iOS)
        DispatchQueue.main.async {
            let generator = UINotificationFeedbackGenerator()
            generator.prepare()
            generator.notificationOccurred(.success)
        }
        #endif
    }

    /// Warning notification feedback.
    public func triggerWarning() {
        #if os(iOS)
        DispatchQueue.main.async {
            let generator = UINotificationFeedbackGenerator()
            generator.prepare()
            generator.notificationOccurred(.warning)
        }
        #endif
    }
}
