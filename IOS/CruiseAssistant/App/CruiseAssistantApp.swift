import SwiftUI

/// Root view orchestrating the onboarding gate and the primary application tab navigation.
public struct RootContentView: View {
    public let container: AppContainer
    @ObservedObject public var coordinator: NavigationCoordinator
    @ObservedObject private var settingsStore: SettingsStore

    public init(container: AppContainer, coordinator: NavigationCoordinator) {
        self.container = container
        self.coordinator = coordinator
        self.settingsStore = container.settingsStore
    }

    public var body: some View {
        Group {
            if !settingsStore.settings.onboardingCompleted {
                OnboardingView(
                    viewModel: OnboardingViewModel(
                        voyageRepository: container.voyageRepository,
                        settingsStore: container.settingsStore,
                        notificationService: container.notificationService
                    ),
                    onComplete: {
                        // settingsStore.settings.onboardingCompleted change triggers reactivity
                    }
                )
            } else {
                MainTabView(
                    container: container,
                    coordinator: coordinator
                )
            }
        }
        .tint(settingsStore.settings.colorTheme.primaryColor)
    }
}

/// SwiftUI application entry point for the offline native Cruise Assistant iOS app.
#if !canImport(XCTest)
@main
#endif
public struct CruiseAssistantApp: App {
    @StateObject private var coordinator: NavigationCoordinator
    public let container: AppContainer

    public init() {
        let stdContainer = AppContainer.standard()
        self.container = stdContainer
        _coordinator = StateObject(wrappedValue: NavigationCoordinator())
    }

    public init(container: AppContainer) {
        self.container = container
        _coordinator = StateObject(wrappedValue: NavigationCoordinator())
    }

    public var body: some Scene {
        WindowGroup {
            RootContentView(
                container: container,
                coordinator: coordinator
            )
        }
    }
}
