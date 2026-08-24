import SwiftUI

/// Main timeline screen composing the date selector, time ruler, multi-lane event canvas, and action controls.
public struct TimelineView: View {
    @ObservedObject public var viewModel: TimelineViewModel

    public let onAddEvent: ((Int) -> Void)?
    public let onEditEvent: ((CruiseEvent) -> Void)?
    public let onOpenSettings: (() -> Void)?
    public let onOpenWishlist: (() -> Void)?
    public let onOpenPixieDust: (() -> Void)?
    public let onOpenFacilities: (() -> Void)?
    public let onOpenDeckMap: (() -> Void)?
    public let onNavigateToDeckMap: ((Int, String) -> Void)?

    @State private var selectedDetailEvent: CruiseEvent? = nil

    public init(
        viewModel: TimelineViewModel,
        onAddEvent: ((Int) -> Void)? = nil,
        onEditEvent: ((CruiseEvent) -> Void)? = nil,
        onOpenSettings: (() -> Void)? = nil,
        onOpenWishlist: (() -> Void)? = nil,
        onOpenPixieDust: (() -> Void)? = nil,
        onOpenFacilities: (() -> Void)? = nil,
        onOpenDeckMap: (() -> Void)? = nil,
        onNavigateToDeckMap: ((Int, String) -> Void)? = nil
    ) {
        self.viewModel = viewModel
        self.onAddEvent = onAddEvent
        self.onEditEvent = onEditEvent
        self.onOpenSettings = onOpenSettings
        self.onOpenWishlist = onOpenWishlist
        self.onOpenPixieDust = onOpenPixieDust
        self.onOpenFacilities = onOpenFacilities
        self.onOpenDeckMap = onOpenDeckMap
        self.onNavigateToDeckMap = onNavigateToDeckMap
    }

    private var palette: CruiseColorPalette {
        viewModel.settings.colorTheme.palette
    }

    private var dayIndexText: String {
        guard let voyage = viewModel.voyage,
              let index = voyage.dayIndexOf(date: viewModel.selectedDate) else {
            return ""
        }
        return "第\(index)天"
    }

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            // Background Wallpaper Watermark (Viewport Fixed)
            if viewModel.settings.wallpaperEnabled {
                wallpaperBackgroundLayer
            }

            VStack(spacing: 0) {
                // Top Header Bar
                topHeaderBar

                // Date Strip
                DateStripView(
                    dates: viewModel.datesInVoyage,
                    selectedDate: viewModel.selectedDate,
                    finalizedDates: viewModel.finalizedDates,
                    theme: viewModel.settings.colorTheme,
                    onSelect: { date in
                        viewModel.selectDate(date)
                    }
                )

                // Sticky Now Banner (when viewing today)
                if viewModel.isToday {
                    NowBannerView(
                        ongoing: viewModel.ongoing,
                        next: viewModel.next,
                        categories: viewModel.categories,
                        theme: viewModel.settings.colorTheme,
                        onSelectEvent: { event in
                            if viewModel.isFinalized {
                                selectedDetailEvent = event
                            } else {
                                onEditEvent?(event)
                            }
                        }
                    )
                }

                // Main Timeline Scroll Canvas
                timelineCanvas
            }

            // Floating Action Buttons Overlay
            floatingActionButtons
                .padding(.trailing, 16)
                .padding(.bottom, 20)

            // Finalized Read-Only Event Detail Dialog
            if let detailEvent = selectedDetailEvent {
                EventDetailDialogView(
                    event: detailEvent,
                    category: viewModel.categories[detailEvent.categoryId],
                    onNavigateToMap: { deck, placeName in
                        selectedDetailEvent = nil
                        if let onNavigateToDeckMap = onNavigateToDeckMap {
                            onNavigateToDeckMap(deck, placeName)
                        } else {
                            onOpenDeckMap?()
                        }
                    },
                    onDismiss: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedDetailEvent = nil
                        }
                    }
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
                .zIndex(100)
            }
        }
        .background((viewModel.settings.wallpaperEnabled ? Color.clear : Slate50).ignoresSafeArea())
        .sheet(isPresented: $viewModel.showConflictDialog) {
            ConflictDialogView(
                conflicts: viewModel.conflicts,
                onGoFix: {
                    viewModel.showConflictDialog = false
                },
                onFinalize: {
                    viewModel.finalizeDay(viewModel.selectedDate)
                },
                onDismiss: {
                    viewModel.showConflictDialog = false
                }
            )
        }
        .onAppear {
            viewModel.loadData()
        }
    }

    // MARK: - Top Header Bar

    private var topHeaderBar: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(viewModel.voyage?.shipName ?? "邮轮小助手")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(Slate900)
                        .lineLimit(2)
                        .minimumScaleFactor(0.85)
                    if !dayIndexText.isEmpty {
                        Text(dayIndexText)
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(palette.secondary)
                            .lineLimit(1)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 2.5)
                            .background(palette.primaryContainer)
                            .clipShape(Capsule())
                    }
                }

                Text(viewModel.selectedDate.chineseFormatted)
                    .font(.system(size: 12))
                    .foregroundColor(Slate500)
                    .lineLimit(1)
            }

            Spacer(minLength: 4)

            // Generate Schedule / Unlock Button (Prominent single-line styling)
            Button(action: {
                if viewModel.isFinalized {
                    viewModel.unlockDay(viewModel.selectedDate)
                } else {
                    viewModel.requestFinalizeDay()
                }
            }) {
                HStack(spacing: 4) {
                    Image(systemName: viewModel.isFinalized ? "lock.fill" : "sparkles")
                        .font(.system(size: viewModel.isFinalized ? 11 : 12, weight: .bold))
                    Text(viewModel.isFinalized ? "修改" : "生成计划表")
                        .font(.system(size: 12.5, weight: .bold))
                        .lineLimit(1)
                }
                .fixedSize(horizontal: true, vertical: false)
                .foregroundColor(viewModel.isFinalized ? Slate700 : .white)
                .padding(.horizontal, viewModel.isFinalized ? 11 : 13)
                .padding(.vertical, 7)
                .background(
                    Group {
                        if viewModel.isFinalized {
                            Slate200
                        } else {
                            LinearGradient(
                                colors: [palette.primary, palette.primary.opacity(0.88)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        }
                    }
)
                .clipShape(Capsule())
                .shadow(
                    color: viewModel.isFinalized ? Color.clear : palette.primary.opacity(0.35),
                    radius: 4,
                    x: 0,
                    y: 2
                )
            }

            // Settings Shortcut
            if let onOpenSettings = onOpenSettings {
                Button(action: onOpenSettings) {
                    Image(systemName: "gearshape")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(Slate700)
                        .padding(6)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(SurfaceCard.opacity(viewModel.settings.wallpaperEnabled ? 0.94 : 1.0))
    }

    // MARK: - Main Timeline Canvas

    private var timelineCanvas: some View {
        ScrollViewReader { proxy in
            ScrollView(.vertical, showsIndicators: true) {
                VStack(spacing: 0) {
                    // Hidden Before Indicator
                    if viewModel.layout.hiddenBefore > 0 {
                        Button(action: { viewModel.toggleFullDay() }) {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.up")
                                Text("顶部有 \(viewModel.layout.hiddenBefore) 个活动被隐藏")
                            }
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(palette.secondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(palette.primaryContainer)
                            .clipShape(Capsule())
                            .padding(.top, 6)
                        }
                    }

                    // Ruler + Canvas Grid
                    HStack(alignment: .top, spacing: 0) {
                        // Left Time Ruler
                        TimeRulerView(layout: viewModel.layout)

                        // Right Canvas Area (Horizontal scroll for 3-lane planning mode)
                        GeometryReader { geo in
                            let viewportWidth = geo.size.width
                            let canvasWidth = viewModel.isFinalized ? viewportWidth : (viewportWidth * 1.2)

                            ScrollView(viewModel.isFinalized ? [] : .horizontal, showsIndicators: false) {
                                ZStack(alignment: .topLeading) {
                                    TimelineGridView(
                                        layout: viewModel.layout,
                                        laneBoundaries: viewModel.isFinalized ? [] : laneBoundaryFractions()
                                    )

                                    // Event Cards Canvas
                                    LaneCanvasView(
                                        layout: viewModel.layout,
                                        events: viewModel.visibleEvents,
                                        categories: viewModel.categories,
                                        conflictIds: viewModel.conflictIds,
                                        singleLane: viewModel.isFinalized,
                                        onEventClick: { event in
                                            if viewModel.isFinalized {
                                                withAnimation(.easeInOut(duration: 0.2)) {
                                                    selectedDetailEvent = event
                                                }
                                            } else {
                                                onEditEvent?(event)
                                            }
                                        },
                                        onEventLongClick: { _ in },
                                        onMoveToLane: { event, laneIndex in
                                            viewModel.moveToLane(eventId: event.id, lane: laneIndex)
                                        },
                                        onToggleActive: { event in
                                            viewModel.toggleActive(eventId: event.id)
                                        },
                                        onKeepOnlyThisShow: { event in
                                            viewModel.keepOnlyThisShow(event)
                                        },
                                        onEditEvent: { event in
                                            if viewModel.isFinalized {
                                                withAnimation(.easeInOut(duration: 0.2)) {
                                                    selectedDetailEvent = event
                                                }
                                            } else {
                                                onEditEvent?(event)
                                            }
                                        },
                                        onDeleteEvent: { event in
                                            viewModel.deleteEvent(id: event.id)
                                        }
                                    )

                                    // Now Indicator Line
                                    if viewModel.isToday {
                                        NowIndicatorView(
                                            layout: viewModel.layout,
                                            nowMinute: viewModel.nowMinute
                                        )
                                        .id("nowIndicator")
                                    }
                                }
                                .frame(width: canvasWidth, height: CGFloat(viewModel.layout.totalHeightPt), alignment: .topLeading)
                            }
                            .frame(width: viewportWidth, height: CGFloat(viewModel.layout.totalHeightPt), alignment: .topLeading)
                            .clipped()
                        }
                        .frame(height: CGFloat(viewModel.layout.totalHeightPt))
                    }

                    // Hidden After Indicator
                    if viewModel.layout.hiddenAfter > 0 {
                        Button(action: { viewModel.toggleFullDay() }) {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.down")
                                Text("底部有 \(viewModel.layout.hiddenAfter) 个活动被隐藏")
                            }
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(palette.secondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(palette.primaryContainer)
                            .clipShape(Capsule())
                            .padding(.bottom, 6)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Viewport-Fixed Wallpaper Layer

    @ViewBuilder
    private var wallpaperBackgroundLayer: some View {
        let preset = WallpaperPreset.fromKey(viewModel.settings.wallpaperKey)
        if preset != .none, let img = loadWallpaperImage(for: preset) {
            GeometryReader { geo in
                #if canImport(UIKit)
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipped()
                    .opacity(viewModel.settings.wallpaperAlpha)
                    .allowsHitTesting(false)
                #elseif canImport(AppKit)
                Image(nsImage: img)
                    .resizable()
                    .scaledToFill()
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipped()
                    .opacity(viewModel.settings.wallpaperAlpha)
                    .allowsHitTesting(false)
                #endif
            }
            .ignoresSafeArea()
            .allowsHitTesting(false)
        }
    }

    // MARK: - Floating Action Buttons

    private var floatingActionButtons: some View {
        VStack(spacing: 12) {
            // Jump to Today / Jump to Now
            if viewModel.todayInVoyage {
                Button(action: {
                    viewModel.jumpToToday()
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 12, weight: .bold))
                        Text("今天")
                            .font(.system(size: 12, weight: .bold))
                    }
                    .foregroundColor(palette.secondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 7)
                    .background(SurfaceCard)
                    .clipShape(Capsule())
                    .shadow(color: Color.black.opacity(0.12), radius: 4, x: 0, y: 2)
                }
            }

            // Wishlist Quick Access (Matching 52x52 size)
            if let onOpenWishlist = onOpenWishlist {
                Button(action: onOpenWishlist) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(palette.primary)
                        .frame(width: 52, height: 52)
                        .background(SurfaceCard)
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.12), radius: 4, x: 0, y: 2)
                }
            }

            // Add Event Primary FAB (Hidden when finalized)
            if !viewModel.isFinalized {
                Button(action: {
                    let defaultStart = viewModel.nowMinute
                    onAddEvent?(defaultStart)
                }) {
                    Image(systemName: "plus")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 52, height: 52)
                        .background(palette.primary)
                        .clipShape(Circle())
                        .shadow(color: palette.primary.opacity(0.4), radius: 6, x: 0, y: 3)
                }
                .transition(.scale.combined(with: .opacity))
            }
        }
    }
}
