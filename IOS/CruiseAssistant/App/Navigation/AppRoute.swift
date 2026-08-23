import Foundation
import SwiftUI
import Combine

/// Main tab items for the Cruise Assistant bottom navigation bar.
public enum AppTab: String, CaseIterable, Identifiable, Sendable {
    case timeline = "timeline"
    case deckMap = "deckMap"
    case facilities = "facilities"
    case pixieDust = "pixieDust"
    case wishlist = "wishlist"
    case settings = "settings"

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .timeline: return "行程"
        case .deckMap: return "甲板图"
        case .facilities: return "指南"
        case .pixieDust: return "仙尘"
        case .wishlist: return "愿望单"
        case .settings: return "设置"
        }
    }

    public var iconName: String {
        switch self {
        case .timeline: return "calendar.day.timeline.left"
        case .deckMap: return "map"
        case .facilities: return "square.grid.2x2"
        case .pixieDust: return "gift"
        case .wishlist: return "star"
        case .settings: return "gearshape"
        }
    }
}

/// Global navigation coordinator handling cross-tab navigation targets and active view state.
@MainActor
public final class NavigationCoordinator: ObservableObject {
    @Published public var activeTab: AppTab

    // Cross-tab navigation target parameters
    @Published public var targetDeck: Int?
    @Published public var targetHighlight: String?
    @Published public var targetDate: SimpleDate?
    @Published public var targetEventId: Int64?

    public init(activeTab: AppTab = .timeline) {
        self.activeTab = activeTab
        self.targetDeck = nil
        self.targetHighlight = nil
        self.targetDate = nil
        self.targetEventId = nil
    }

    /// Navigates to the Deck Map tab and focuses on a specific deck with optional highlight text.
    public func navigateToDeckMap(deck: Int = 10, highlight: String? = nil) {
        self.targetDeck = deck
        self.targetHighlight = highlight
        self.activeTab = .deckMap
    }

    /// Navigates to the Timeline tab with optional date selection and event highlighting.
    public func navigateToTimeline(date: SimpleDate? = nil, eventId: Int64? = nil) {
        if let date = date {
            self.targetDate = date
        }
        self.targetEventId = eventId
        self.activeTab = .timeline
    }

    /// Navigates to the Facility Guide tab.
    public func navigateToFacilities() {
        self.activeTab = .facilities
    }

    /// Navigates to the Pixie Dust tracking tab.
    public func navigateToPixieDust() {
        self.activeTab = .pixieDust
    }

    /// Navigates to the Wishlist tab.
    public func navigateToWishlist() {
        self.activeTab = .wishlist
    }

    /// Navigates to the Settings tab.
    public func navigateToSettings() {
        self.activeTab = .settings
    }

    /// Clears any consumed transient cross-navigation targets.
    public func clearNavigationTargets() {
        self.targetDeck = nil
        self.targetHighlight = nil
        self.targetDate = nil
        self.targetEventId = nil
    }
}
