import SwiftUI
import WebKit

#if canImport(UIKit)
import UIKit

/// 基于 WKWebView 实现的高性能矢量 SVG 离线渲染视图
public struct SVGWebView: UIViewRepresentable {
    public let htmlContent: String

    public init(htmlContent: String) {
        self.htmlContent = htmlContent
    }

    public func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        webView.scrollView.bounces = true
        webView.scrollView.showsHorizontalScrollIndicator = false
        webView.scrollView.showsVerticalScrollIndicator = false
        webView.scrollView.minimumZoomScale = 1.0
        webView.scrollView.maximumZoomScale = 6.0
        return webView
    }

    public func updateUIView(_ uiView: WKWebView, context: Context) {
        if context.coordinator.lastHtmlContent != htmlContent {
            context.coordinator.lastHtmlContent = htmlContent
            uiView.loadHTMLString(htmlContent, baseURL: nil)
        }
    }

    public func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    public class Coordinator: NSObject, WKNavigationDelegate {
        var lastHtmlContent: String = ""
    }
}
#elseif canImport(AppKit)
import AppKit

public struct SVGWebView: NSViewRepresentable {
    public let htmlContent: String

    public init(htmlContent: String) {
        self.htmlContent = htmlContent
    }

    public func makeNSView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.setValue(false, forKey: "drawsBackground")
        return webView
    }

    public func updateNSView(_ nsView: WKWebView, context: Context) {
        if context.coordinator.lastHtmlContent != htmlContent {
            context.coordinator.lastHtmlContent = htmlContent
            nsView.loadHTMLString(htmlContent, baseURL: nil)
        }
    }

    public func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    public class Coordinator: NSObject {
        var lastHtmlContent: String = ""
    }
}
#endif

/// 交互式甲板 SVG 地图视图
public struct SVGInteractiveView: View {
    public let deckNumber: Int
    public let highlightTarget: String?
    public let onSelectDeck: ((Int) -> Void)?

    public init(
        deckNumber: Int,
        highlightTarget: String? = nil,
        onSelectDeck: ((Int) -> Void)? = nil
    ) {
        self.deckNumber = deckNumber
        self.highlightTarget = highlightTarget
        self.onSelectDeck = onSelectDeck
    }

    public var body: some View {
        if let svgContent = DeckMapRepository.loadDeckSvgContent(deckNumber: deckNumber) {
            let html = DeckMapRepository.inlineHtmlWrapperFor(
                svgContent: svgContent,
                highlightTarget: highlightTarget
            )
            SVGWebView(htmlContent: html)
                .edgesIgnoringSafeArea(.bottom)
        } else {
            NoDeckMapPlaceholderView(deckNumber: deckNumber)
        }
    }
}

/// 暂无甲板图占位视图
public struct NoDeckMapPlaceholderView: View {
    public let deckNumber: Int

    public init(deckNumber: Int) {
        self.deckNumber = deckNumber
    }

    public var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "map.fill")
                .font(.system(size: 48))
                .foregroundColor(Slate300)

            Text("Deck \(deckNumber) 暂无平面图")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Slate700)

            Text("该甲板为轮机、船员工作区或特殊管制区域，不对普通游客开放平面导览。")
                .font(.system(size: 14))
                .foregroundColor(Slate500)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Slate50)
    }
}

#Preview {
    SVGInteractiveView(deckNumber: 10, highlightTarget: "Garden Stage")
}
