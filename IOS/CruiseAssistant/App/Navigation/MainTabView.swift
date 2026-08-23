import SwiftUI

/// Container model for presenting the event creation/edit sheet.
private struct ActiveEventSheetParams: Identifiable {
    let id = UUID()
    let date: SimpleDate
    let startMinute: Int
    let editingEventId: Int64
}

/// Root tab navigation interface for the Cruise Assistant iOS app.
/// Hosts Timeline, Deck Map, Facility Guide, Pixie Dust, Wishlist, and Settings tabs.
public struct MainTabView: View {
    public let container: AppContainer
    @ObservedObject public var coordinator: NavigationCoordinator
    @ObservedObject private var settingsStore: SettingsStore

    @StateObject private var timelineViewModel: TimelineViewModel
    @StateObject private var deckMapViewModel: DeckMapViewModel
    @StateObject private var facilityGuideViewModel: FacilityGuideViewModel
    @StateObject private var pixieDustViewModel: PixieDustViewModel
    @StateObject private var wishlistViewModel: WishlistViewModel
    @StateObject private var settingsViewModel: SettingsViewModel

    @State private var activeEventSheetParams: ActiveEventSheetParams? = nil

    public init(container: AppContainer, coordinator: NavigationCoordinator) {
        self.container = container
        self.coordinator = coordinator
        self.settingsStore = container.settingsStore

        _timelineViewModel = StateObject(
            wrappedValue: TimelineViewModel(
                voyageRepository: container.voyageRepository,
                eventRepository: container.eventRepository,
                categoryRepository: container.categoryRepository,
                settingsStore: container.settingsStore,
                notificationService: container.notificationService
            )
        )
        _deckMapViewModel = StateObject(
            wrappedValue: DeckMapViewModel(
                initialDeck: coordinator.targetDeck ?? 10,
                initialHighlight: coordinator.targetHighlight
            )
        )
        _facilityGuideViewModel = StateObject(
            wrappedValue: FacilityGuideViewModel()
        )
        _pixieDustViewModel = StateObject(
            wrappedValue: PixieDustViewModel(
                dustRepository: container.pixieDustRepository,
                voyageRepository: container.voyageRepository,
                settingsStore: container.settingsStore
            )
        )
        _wishlistViewModel = StateObject(
            wrappedValue: WishlistViewModel(
                wishlistRepository: container.wishlistRepository,
                categoryRepository: container.categoryRepository,
                eventRepository: container.eventRepository,
                voyageRepository: container.voyageRepository
            )
        )
        _settingsViewModel = StateObject(
            wrappedValue: SettingsViewModel(
                settingsStore: container.settingsStore,
                voyageRepository: container.voyageRepository,
                eventRepository: container.eventRepository,
                categoryRepository: container.categoryRepository,
                notificationService: container.notificationService,
                database: container.database
            )
        )
    }

    public var body: some View {
        TabView(selection: $coordinator.activeTab) {
            // Tab 1: 行程 (Timeline)
            TimelineView(
                viewModel: timelineViewModel,
                onAddEvent: { minute in
                    activeEventSheetParams = ActiveEventSheetParams(
                        date: timelineViewModel.selectedDate,
                        startMinute: minute,
                        editingEventId: 0
                    )
                },
                onEditEvent: { event in
                    activeEventSheetParams = ActiveEventSheetParams(
                        date: event.date,
                        startMinute: event.startMinute,
                        editingEventId: event.id
                    )
                },
                onOpenSettings: { coordinator.navigateToSettings() },
                onOpenWishlist: { coordinator.navigateToWishlist() },
                onOpenPixieDust: { coordinator.navigateToPixieDust() },
                onOpenFacilities: { coordinator.navigateToFacilities() },
                onOpenDeckMap: { coordinator.navigateToDeckMap() }
            )
            .tabItem {
                Label(AppTab.timeline.title, systemImage: AppTab.timeline.iconName)
            }
            .tag(AppTab.timeline)

            // Tab 2: 甲板图 (Deck Map)
            DeckMapView(
                viewModel: deckMapViewModel,
                onBack: nil
            )
            .tabItem {
                Label(AppTab.deckMap.title, systemImage: AppTab.deckMap.iconName)
            }
            .tag(AppTab.deckMap)

            // Tab 3: 指南 (Facilities)
            FacilityGuideView(
                viewModel: facilityGuideViewModel,
                onBack: nil,
                onOpenDeckMap: { coordinator.navigateToDeckMap() },
                onNavigateToDeckMap: { deck, placeName in
                    coordinator.navigateToDeckMap(deck: deck, highlight: placeName)
                },
                wishlistRepository: container.wishlistRepository,
                categoryRepository: container.categoryRepository
            )
            .tabItem {
                Label(AppTab.facilities.title, systemImage: AppTab.facilities.iconName)
            }
            .tag(AppTab.facilities)

            // Tab 4: 仙尘 (Pixie Dust)
            PixieDustView(viewModel: pixieDustViewModel)
                .tabItem {
                    Label(AppTab.pixieDust.title, systemImage: AppTab.pixieDust.iconName)
                }
                .tag(AppTab.pixieDust)

            // Tab 5: 愿望单 (Wishlist)
            NavigationStack {
                WishlistView(
                    viewModel: wishlistViewModel,
                    onNavigateToDeckMap: { deck, placeName in
                        coordinator.navigateToDeckMap(deck: deck, highlight: placeName)
                    }
                )
                .navigationTitle("愿望单")
                .inlineNavigationBarTitle()
            }
            .tabItem {
                Label(AppTab.wishlist.title, systemImage: AppTab.wishlist.iconName)
            }
            .tag(AppTab.wishlist)

            // Tab 6: 设置 (Settings)
            NavigationStack {
                SettingsView(viewModel: settingsViewModel)
            }
            .tabItem {
                Label(AppTab.settings.title, systemImage: AppTab.settings.iconName)
            }
            .tag(AppTab.settings)
        }
        .tint(settingsStore.settings.colorTheme.primaryColor)
        .sheet(item: $activeEventSheetParams) { params in
            let vm = EventSheetViewModel(
                date: params.date,
                defaultStartMinute: params.startMinute,
                editingEventId: params.editingEventId,
                eventRepository: container.eventRepository,
                categoryRepository: container.categoryRepository,
                locationPresetRepository: container.locationPresetRepository,
                voyageRepository: container.voyageRepository,
                notificationService: container.notificationService,
                settingsStore: container.settingsStore
            )
            EventSheetView(
                viewModel: vm,
                onDismiss: {
                    activeEventSheetParams = nil
                    timelineViewModel.loadData()
                    wishlistViewModel.loadData()
                }
            )
        }
        .onChange(of: coordinator.targetDeck) { _, newDeck in
            if let deck = newDeck {
                deckMapViewModel.selectDeck(deck)
                if let highlight = coordinator.targetHighlight {
                    deckMapViewModel.highlightTarget = highlight
                    deckMapViewModel.search(query: highlight)
                }
            }
        }
        .onChange(of: coordinator.targetDate) { _, newDate in
            if let date = newDate {
                timelineViewModel.selectDate(date)
            }
        }
    }
}
