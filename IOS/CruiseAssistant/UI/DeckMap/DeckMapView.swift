import SwiftUI

/// 全船甲板导览与交互式地图主视图
public struct DeckMapView: View {
    @StateObject public var viewModel: DeckMapViewModel
    public let onBack: (() -> Void)?

    public init(
        viewModel: DeckMapViewModel? = nil,
        initialDeck: Int = 10,
        initialHighlight: String? = nil,
        onBack: (() -> Void)? = nil
    ) {
        if let vm = viewModel {
            _viewModel = StateObject(wrappedValue: vm)
        } else {
            _viewModel = StateObject(
                wrappedValue: DeckMapViewModel(
                    initialDeck: initialDeck,
                    initialHighlight: initialHighlight
                )
            )
        }
        self.onBack = onBack
    }

    public var body: some View {
        NavigationStack {
            ZStack(alignment: .top) {
                // 背景
                Slate50.edgesIgnoringSafeArea(.all)

                VStack(spacing: 0) {
                    // 顶部甲板选择器滑动条
                    deckSelectorRail
                        .background(Color.white)
                        .shadow(color: Color.black.opacity(0.04), radius: 3, x: 0, y: 2)
                        .zIndex(10)

                    // 地图交互视口
                    ZStack(alignment: .topLeading) {
                        SVGInteractiveView(
                            deckNumber: viewModel.selectedDeck,
                            highlightTarget: viewModel.highlightTarget,
                            onSelectDeck: { deck in
                                viewModel.selectDeck(deck)
                            }
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                        // 船体方位指示条 (船头 BOW ⬆️ -> 船中 MID -> 船尾 STERN ⬇️)
                        orientationIndicatorStrip
                            .padding(.leading, 12)
                            .padding(.top, 12)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }

                // 悬浮高亮提示条 (如果有选中的设施/房间)
                if let highlight = viewModel.highlightTarget, !highlight.isEmpty {
                    VStack {
                        Spacer()
                        highlightBanner(target: highlight)
                            .padding(.horizontal, 16)
                            .padding(.bottom, 80)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }
                    .zIndex(20)
                }

                // 右下角悬浮操作按钮组 (搜索、缩放、重置)
                VStack {
                    Spacer()
                    HStack(alignment: .bottom) {
                        // 底部快速搜索条
                        searchQuickBar
                            .padding(.leading, 16)

                        Spacer()

                        // 缩放控制微件
                        floatingControlRail
                            .padding(.trailing, 16)
                    }
                    .padding(.bottom, 16)
                }
                .zIndex(20)
            }
            .navigationTitle("Deck \(viewModel.selectedDeck) 平面图")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                if let onBack = onBack {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(action: onBack) {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.left")
                                Text("返回")
                            }
                            .foregroundColor(CruiseAzure)
                        }
                    }
                }

                ToolbarItem(placement: .primaryAction) {
                    Button(action: {
                        viewModel.openSearch()
                    }) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(CruiseAzure)
                    }
                }
            }
            .sheet(isPresented: $viewModel.isSearchSheetPresented) {
                DeckMapSearchSheet(
                    currentDeck: viewModel.selectedDeck,
                    onSelectResult: { result in
                        viewModel.selectResult(result)
                    },
                    onDismiss: {
                        viewModel.closeSearch()
                    }
                )
            }
        }
    }

    // MARK: - Deck Selector Rail

    private var deckSelectorRail: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(AVAILABLE_DECKS, id: \.self) { deck in
                        let isSelected = viewModel.selectedDeck == deck
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                viewModel.selectDeck(deck)
                            }
                        }) {
                            VStack(spacing: 2) {
                                Text("Deck")
                                    .font(.system(size: 9, weight: .medium))
                                    .foregroundColor(isSelected ? .white.opacity(0.85) : Slate400)
                                Text("\(deck)")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(isSelected ? .white : Slate800)
                            }
                            .frame(width: 52, height: 44)
                            .background(isSelected ? CruiseAzure : Slate100)
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(isSelected ? CruiseAzure : Color.clear, lineWidth: 1)
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                        .id(deck)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            .onAppear {
                proxy.scrollTo(viewModel.selectedDeck, anchor: .center)
            }
            .onChange(of: viewModel.selectedDeck) { _, newDeck in
                withAnimation {
                    proxy.scrollTo(newDeck, anchor: .center)
                }
            }
        }
    }

    // MARK: - Orientation Indicator Strip

    private var orientationIndicatorStrip: some View {
        VStack(spacing: 4) {
            HStack(spacing: 3) {
                Image(systemName: "arrow.up")
                    .font(.system(size: 9, weight: .bold))
                Text("船头 BOW")
                    .font(.system(size: 9, weight: .bold))
            }
            .foregroundColor(CruiseMarine)

            Capsule()
                .fill(Slate300)
                .frame(width: 2, height: 16)

            Text("船中 MID")
                .font(.system(size: 8, weight: .semibold))
                .foregroundColor(Slate500)

            Capsule()
                .fill(Slate300)
                .frame(width: 2, height: 16)

            HStack(spacing: 3) {
                Image(systemName: "arrow.down")
                    .font(.system(size: 9, weight: .bold))
                Text("船尾 STERN")
                    .font(.system(size: 9, weight: .bold))
            }
            .foregroundColor(CruiseMarine)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        .background(Color.white.opacity(0.92))
        .cornerRadius(10)
        .shadow(color: Color.black.opacity(0.08), radius: 4, x: 0, y: 2)
    }

    // MARK: - Highlight Banner

    private func highlightBanner(target: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "mappin.circle.fill")
                .foregroundColor(.white)
                .font(.system(size: 18))

            VStack(alignment: .leading, spacing: 2) {
                Text("当前高亮定位")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.white.opacity(0.85))
                Text(target)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(1)
            }

            Spacer()

            Button(action: {
                withAnimation {
                    viewModel.clearHighlight()
                }
            }) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.white.opacity(0.85))
                    .font(.system(size: 18))
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(CruiseAzure)
        .cornerRadius(14)
        .shadow(color: CruiseAzure.opacity(0.35), radius: 8, x: 0, y: 4)
    }

    // MARK: - Search Quick Bar

    private var searchQuickBar: some View {
        Button(action: {
            viewModel.openSearch()
        }) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(CruiseMarine)
                    .font(.system(size: 15))

                Text("搜索地点、设施或客房...")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Slate500)

                Spacer()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .background(Color.white)
            .cornerRadius(24)
            .shadow(color: Color.black.opacity(0.12), radius: 6, x: 0, y: 3)
        }
        .buttonStyle(PlainButtonStyle())
    }

    // MARK: - Floating Control Rail

    private var floatingControlRail: some View {
        VStack(spacing: 8) {
            Button(action: {
                viewModel.zoomIn()
            }) {
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Slate700)
                    .frame(width: 40, height: 40)
                    .background(Color.white)
                    .clipShape(Circle())
                    .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
            }

            Button(action: {
                viewModel.zoomOut()
            }) {
                Image(systemName: "minus")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Slate700)
                    .frame(width: 40, height: 40)
                    .background(Color.white)
                    .clipShape(Circle())
                    .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
            }

            Button(action: {
                viewModel.resetZoom()
            }) {
                Image(systemName: "arrow.counterclockwise")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Slate700)
                    .frame(width: 40, height: 40)
                    .background(Color.white)
                    .clipShape(Circle())
                    .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
            }
        }
    }
}

#Preview {
    DeckMapView(initialDeck: 10, initialHighlight: "Garden Stage")
}
