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

    public init(
        viewModel: TimelineViewModel,
        onAddEvent: ((Int) -> Void)? = nil,
        onEditEvent: ((CruiseEvent) -> Void)? = nil,
        onOpenSettings: (() -> Void)? = nil,
        onOpenWishlist: (() -> Void)? = nil,
        onOpenPixieDust: (() -> Void)? = nil,
        onOpenFacilities: (() -> Void)? = nil,
        onOpenDeckMap: (() -> Void)? = nil
    ) {
        self.viewModel = viewModel
        self.onAddEvent = onAddEvent
        self.onEditEvent = onEditEvent
        self.onOpenSettings = onOpenSettings
        self.onOpenWishlist = onOpenWishlist
        self.onOpenPixieDust = onOpenPixieDust
        self.onOpenFacilities = onOpenFacilities
        self.onOpenDeckMap = onOpenDeckMap
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
            VStack(spacing: 0) {
                // Top Header Bar
                topHeaderBar

                // Date Strip
                DateStripView(
                    dates: viewModel.datesInVoyage,
                    selectedDate: viewModel.selectedDate,
                    finalizedDates: viewModel.finalizedDates,
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
                        onSelectEvent: { event in
                            onEditEvent?(event)
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
        }
        .background(Slate50.ignoresSafeArea())
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
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(viewModel.voyage?.shipName ?? "邮轮小助手")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(Slate900)

                    if !dayIndexText.isEmpty {
                        Text(dayIndexText)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(CruiseMarine)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(CruiseAzureContainer)
                            .clipShape(Capsule())
                    }
                }

                Text(viewModel.selectedDate.chineseFormatted)
                    .font(.system(size: 12))
                    .foregroundColor(Slate500)
            }

            Spacer()

            // Full Day Toggle
            Button(action: {
                viewModel.toggleFullDay()
            }) {
                Text(viewModel.fullDayOverride ? "收起" : "全天")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(viewModel.fullDayOverride ? .white : CruiseMarine)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(viewModel.fullDayOverride ? CruiseAzure : CruiseAzureContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }

            // Finalize / Unlock Button
            Button(action: {
                if viewModel.isFinalized {
                    viewModel.unlockDay(viewModel.selectedDate)
                } else {
                    viewModel.requestFinalizeDay()
                }
            }) {
                HStack(spacing: 4) {
                    Image(systemName: viewModel.isFinalized ? "lock.fill" : "lock.open")
                        .font(.system(size: 11))
                    Text(viewModel.isFinalized ? "修改" : "定稿")
                        .font(.system(size: 12, weight: .semibold))
                }
                .foregroundColor(viewModel.isFinalized ? Slate700 : .white)
                .padding(.horizontal, 9)
                .padding(.vertical, 5)
                .background(viewModel.isFinalized ? Slate200 : CruiseAzure)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
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
        .background(SurfaceCard)
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
                            .foregroundColor(CruiseMarine)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(CruiseAzureContainer)
                            .clipShape(Capsule())
                            .padding(.top, 6)
                        }
                    }

                    // Ruler + Canvas Grid
                    HStack(alignment: .top, spacing: 0) {
                        // Left Time Ruler
                        TimeRulerView(layout: viewModel.layout)

                        // Right Canvas Area
                        ZStack(alignment: .topLeading) {
                            // Wallpaper Background Layer
                            if viewModel.settings.wallpaperEnabled {
                                wallpaperLayer
                            }

                            // Grid Lines
                            TimelineGridView(layout: viewModel.layout)

                            // Event Cards Canvas
                            LaneCanvasView(
                                layout: viewModel.layout,
                                events: viewModel.visibleEvents,
                                categories: viewModel.categories,
                                conflictIds: viewModel.conflictIds,
                                singleLane: viewModel.isFinalized,
                                onEventClick: { event in
                                    onEditEvent?(event)
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
                                    onEditEvent?(event)
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
                        .frame(maxWidth: .infinity)
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
                            .foregroundColor(CruiseMarine)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(CruiseAzureContainer)
                            .clipShape(Capsule())
                            .padding(.bottom, 6)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Wallpaper Layer

    @ViewBuilder
    private var wallpaperLayer: some View {
        let preset = WallpaperPreset.fromKey(viewModel.settings.wallpaperKey)
        if preset != .none, let imageName = preset.imageResourceName {
            Image(imageName)
                .resizable()
                .scaledToFill()
                .opacity(viewModel.settings.wallpaperAlpha)
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
                    .foregroundColor(CruiseMarine)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 7)
                    .background(SurfaceCard)
                    .clipShape(Capsule())
                    .shadow(color: Color.black.opacity(0.12), radius: 4, x: 0, y: 2)
                }
            }

            // Wishlist Quick Access
            if let onOpenWishlist = onOpenWishlist {
                Button(action: onOpenWishlist) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(CruiseMarine)
                        .frame(width: 44, height: 44)
                        .background(SurfaceCard)
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.12), radius: 4, x: 0, y: 2)
                }
            }

            // Add Event Primary FAB
            Button(action: {
                let defaultStart = viewModel.nowMinute
                onAddEvent?(defaultStart)
            }) {
                Image(systemName: "plus")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 52, height: 52)
                    .background(CruiseAzure)
                    .clipShape(Circle())
                    .shadow(color: CruiseAzure.opacity(0.4), radius: 6, x: 0, y: 3)
            }
        }
    }
}
