import Foundation

/// 目前已拥有 SVG 图纸的甲板集合（1~4为轮机舱/船员舱不开放，14层不存在）
public let AVAILABLE_DECKS: [Int] = [5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19]

/// 乘客开放甲板列表（从高层到低层降序排列，排除 1~4 层与不存在的 14 层）
public let PASSENGER_DECKS: [Int] = [19, 18, 17, 16, 15, 13, 12, 11, 10, 9, 8, 7, 6, 5]

/// 搜索结果类别
public enum DeckSearchResultType: String, CaseIterable, Codable, Sendable {
    case facility = "设施"
    case stateroom = "客房"
    case restroom = "洗手间"
}

/// 甲板地图搜索条目模型
public struct DeckSearchResult: Identifiable, Hashable, Codable, Sendable {
    public let id: String
    public let title: String
    public let subtitle: String
    public let deckNumber: Int
    public let highlightQuery: String
    public let type: DeckSearchResultType
    public let categoryKey: String?

    public init(
        id: String? = nil,
        title: String,
        subtitle: String,
        deckNumber: Int,
        highlightQuery: String,
        type: DeckSearchResultType,
        categoryKey: String? = nil
    ) {
        self.id = id ?? "\(type.rawValue)_\(deckNumber)_\(title)_\(highlightQuery)"
        self.title = title
        self.subtitle = subtitle
        self.deckNumber = deckNumber
        self.highlightQuery = highlightQuery
        self.type = type
        self.categoryKey = categoryKey
    }
}

/// 甲板基本信息模型
public struct DeckInfo: Hashable, Codable, Identifiable, Sendable {
    public let deckNumber: Int
    public let name: String
    public let shortLabel: String
    public let hasMap: Bool
    public let assetPath: String

    public var id: Int { deckNumber }

    public init(
        deckNumber: Int,
        name: String,
        shortLabel: String,
        hasMap: Bool,
        assetPath: String
    ) {
        self.deckNumber = deckNumber
        self.name = name
        self.shortLabel = shortLabel
        self.hasMap = hasMap
        self.assetPath = assetPath
    }
}

/// 全船甲板仓储与帮助类
public enum DeckMapRepository {
    public static let availableDecks: [Int] = AVAILABLE_DECKS
    public static let passengerDecks: [Int] = PASSENGER_DECKS
    public static let defaultMaxDeck: Int = 19

    /// 判断指定甲板是否具备平面图纸
    public static func hasDeckMap(deckNumber: Int) -> Bool {
        AVAILABLE_DECKS.contains(deckNumber)
    }

    /// 获取对客开放甲板列表（降序）
    public static func getDecks(maxDeck: Int = defaultMaxDeck) -> [DeckInfo] {
        PASSENGER_DECKS
            .filter { $0 <= maxDeck }
            .map { d in
                DeckInfo(
                    deckNumber: d,
                    name: "Deck \(d)",
                    shortLabel: "D\(d)",
                    hasMap: hasDeckMap(deckNumber: d),
                    assetPath: "deck_\(d).svg"
                )
            }
    }

    /// 从 Bundle 加载指定甲板的 SVG 字符串内容
    public static func loadDeckSvgContent(deckNumber: Int, bundle: Bundle? = nil) -> String? {
        let targetBundle = bundle ?? CruiseAssistant.bundle
        let name = "deck_\(deckNumber)"
        let url = targetBundle.url(forResource: name, withExtension: "svg")
            ?? targetBundle.url(forResource: name, withExtension: "svg", subdirectory: "Raw/Decks")
            ?? targetBundle.url(forResource: name, withExtension: "svg", subdirectory: "Decks")
            ?? targetBundle.url(forResource: name, withExtension: "svg", subdirectory: "Raw")
        guard let url = url, let content = try? String(contentsOf: url, encoding: .utf8) else {
            return nil
        }
        return content
    }

    /// 将 SVG 字符串内嵌到响应式 HTML 容器中，并注入高亮与地图钉定位脚本
    public static func inlineHtmlWrapperFor(svgContent: String, highlightTarget: String? = nil) -> String {
        let sanitizedHighlight = highlightTarget?.trimmingCharacters(in: .whitespacesAndNewlines)
        let highlightScript: String
        if let target = sanitizedHighlight, !target.isEmpty {
            let jsTarget = target
                .replacingOccurrences(of: "\\", with: "\\\\")
                .replacingOccurrences(of: "'", with: "\\'")
                .replacingOccurrences(of: "\n", with: " ")

            highlightScript = """
            <style>
                .facility-highlight-box {
                    position: absolute;
                    pointer-events: none;
                    background: rgba(2, 132, 199, 0.28);
                    border: 2.5px solid #0284C7;
                    border-radius: 8px;
                    box-shadow: 0 0 20px rgba(2, 132, 199, 0.85);
                    animation: facilityPulse 1.8s infinite ease-in-out;
                    z-index: 1000;
                }
                @keyframes facilityPulse {
                    0% {
                        box-shadow: 0 0 0 0 rgba(2, 132, 199, 0.9);
                        transform: scale(0.98);
                    }
                    50% {
                        box-shadow: 0 0 0 14px rgba(2, 132, 199, 0);
                        transform: scale(1.02);
                    }
                    100% {
                        box-shadow: 0 0 0 0 rgba(2, 132, 199, 0);
                        transform: scale(0.98);
                    }
                }
                .facility-pin-badge {
                    position: absolute;
                    pointer-events: none;
                    z-index: 1001;
                    transform: translate(-50%, -100%);
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    animation: pinBounce 1.5s infinite ease-in-out;
                }
                @keyframes pinBounce {
                    0%, 100% {
                        transform: translate(-50%, -100%);
                    }
                    50% {
                        transform: translate(-50%, -112%);
                    }
                }
                .pin-content {
                    background: #0284C7;
                    color: #FFFFFF;
                    font-size: 13px;
                    font-weight: 700;
                    padding: 4px 10px;
                    border-radius: 16px;
                    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
                    white-space: nowrap;
                    display: flex;
                    align-items: center;
                    gap: 4px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                }
                .pin-arrow {
                    width: 0;
                    height: 0;
                    border-left: 6px solid transparent;
                    border-right: 6px solid transparent;
                    border-top: 8px solid #0284C7;
                    margin-top: -1px;
                }
            </style>
            <script>
                function centerFacilityTarget() {
                    var target = '\(jsTarget)';
                    var svg = document.querySelector('svg');
                    if (!svg) return;
                    var textNodes = Array.from(svg.querySelectorAll('text, tspan'));
                    if (!textNodes || textNodes.length === 0) return;

                    var words = target.toLowerCase().split(/[\\s·,]+/g).filter(function(w) {
                        return w.length > 1 && !['deck', 'the', 'and', 'restaurant', 'club'].includes(w);
                    });
                    if (target.indexOf('花园舞台') !== -1) { words.push('garden', 'stage'); }
                    if (target.indexOf('迪士尼剧院') !== -1) { words.push('walt', 'disney', 'theatre', 'theater'); }
                    if (target.indexOf('寻路者') !== -1) { words.push('wayfinder'); }
                    if (target.indexOf('大白剧院') !== -1) { words.push('baymax', 'cinemas', 'cinema'); }
                    if (words.length === 0) words = [target.toLowerCase()];

                    var bestNode = null;
                    var bestScore = 0;

                    for (var i = 0; i < textNodes.length; i++) {
                        var node = textNodes[i];
                        var content = (node.textContent || '').toLowerCase();
                        var score = 0;
                        for (var j = 0; j < words.length; j++) {
                            if (content.indexOf(words[j]) !== -1) {
                                score += words[j].length * 2;
                            }
                        }
                        if (content.indexOf(target.toLowerCase()) !== -1) {
                            score += 50;
                        }
                        if (score > bestScore) {
                            bestScore = score;
                            bestNode = node;
                        }
                    }

                    if (!bestNode) return;

                    var existingMarkers = document.querySelectorAll('.facility-highlight-box, .facility-pin-badge');
                    for (var k = 0; k < existingMarkers.length; k++) {
                        existingMarkers[k].remove();
                    }

                    var rect = bestNode.getBoundingClientRect();
                    var scrollLeft = window.pageXOffset || document.documentElement.scrollLeft || 0;
                    var scrollTop = window.pageYOffset || document.documentElement.scrollTop || 0;

                    var box = document.createElement('div');
                    box.className = 'facility-highlight-box';
                    box.style.left = (rect.left + scrollLeft - 8) + 'px';
                    box.style.top = (rect.top + scrollTop - 6) + 'px';
                    box.style.width = (rect.width + 16) + 'px';
                    box.style.height = (rect.height + 12) + 'px';
                    document.body.appendChild(box);

                    var pin = document.createElement('div');
                    pin.className = 'facility-pin-badge';
                    pin.style.left = (rect.left + scrollLeft + rect.width / 2) + 'px';
                    pin.style.top = (rect.top + scrollTop - 8) + 'px';
                    pin.innerHTML = '<div class="pin-content"><span>📍</span><span>' + target + '</span></div><div class="pin-arrow"></div>';
                    document.body.appendChild(pin);

                    if (typeof bestNode.scrollIntoView === 'function') {
                        bestNode.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
                    }
                    var targetY = (rect.top + scrollTop) - (window.innerHeight / 2) + (rect.height / 2);
                    var targetX = (rect.left + scrollLeft) - (window.innerWidth / 2) + (rect.width / 2);
                    window.scrollTo({
                        top: Math.max(0, targetY),
                        left: Math.max(0, targetX),
                        behavior: 'smooth'
                    });
                }

                window.addEventListener('DOMContentLoaded', function() {
                    setTimeout(centerFacilityTarget, 60);
                    setTimeout(centerFacilityTarget, 220);
                    setTimeout(centerFacilityTarget, 500);
                });
                window.addEventListener('load', function() {
                    setTimeout(centerFacilityTarget, 100);
                });
            </script>
            """
        } else {
            highlightScript = ""
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=10.0, user-scalable=yes">
            <style>
                * { box-sizing: border-box; }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    min-height: 100%;
                    background-color: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: flex-start;
                    overflow: auto;
                    -webkit-touch-callout: none;
                    -webkit-user-select: none;
                    user-select: none;
                }
                #svg-wrapper {
                    position: relative;
                    width: 100%;
                    padding: 12px 4px;
                    display: flex;
                    justify-content: center;
                }
                svg {
                    width: 100%;
                    max-width: 588px;
                    height: auto;
                    display: block;
                }
            </style>
            \(highlightScript)
        </head>
        <body>
            <div id="svg-wrapper">
                \(svgContent)
            </div>
        </body>
        </html>
        """
    }
}

/// 根据客房号智能解析所在甲板层级
public func parseStateroomDeck(_ roomStr: String) -> Int? {
    let trimmed = roomStr.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty, trimmed.allSatisfy({ $0.isNumber }) else { return nil }
    let parsedDeck: Int?
    switch trimmed.count {
    case 5:
        parsedDeck = Int(trimmed.prefix(2))
    case 4:
        parsedDeck = Int(trimmed.prefix(1))
    default:
        parsedDeck = nil
    }
    if let deck = parsedDeck, AVAILABLE_DECKS.contains(deck) {
        return deck
    }
    return nil
}

/// 全船甲板智能检索：支持设施名称、客房号前缀联想、洗手间快捷检索
public func searchDeckMap(
    query: String,
    currentDeck: Int,
    facilities: [FacilityItemUiModel] = getAllFacilityItems(),
    stateroomSearcher: (String) -> [StateroomEntry] = { StateroomCatalog.search(query: $0) }
) -> [DeckSearchResult] {
    let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.isEmpty {
        var quickList: [DeckSearchResult] = []
        // 当前甲板洗手间快捷项
        quickList.append(
            DeckSearchResult(
                title: "洗手间 Restroom",
                subtitle: "Deck \(currentDeck) · 当前甲板",
                deckNumber: currentDeck,
                highlightQuery: "Restroom",
                type: .restroom
            )
        )
        // 常用核心设施推荐
        let popularNames = ["华特迪士尼剧院", "宾客服务台", "帕洛意大利餐厅", "动画师味觉餐厅", "魔法盛夏餐厅", "皮克斯市场餐厅", "童话洗衣房"]
        for name in popularNames {
            if let fac = facilities.first(where: { $0.placeName.contains(name) }),
               let deck = fac.primaryDeck {
                quickList.append(
                    DeckSearchResult(
                        title: fac.placeName,
                        subtitle: fac.formattedLocation,
                        deckNumber: deck,
                        highlightQuery: fac.placeName,
                        type: .facility,
                        categoryKey: fac.categoryKey
                    )
                )
            }
        }
        return quickList
    }

    var results: [DeckSearchResult] = []
    let lowerQuery = trimmed.lowercased()
    let isNumeric = !trimmed.isEmpty && trimmed.allSatisfy { $0.isNumber }
    let numericDeck = isNumeric ? Int(trimmed) : nil

    // 1. 设施匹配（优先展示在最上方）
    let matchedFacilities = facilities.filter { fac in
        fac.placeName.lowercased().contains(lowerQuery) ||
            fac.formattedLocation.lowercased().contains(lowerQuery) ||
            fac.categoryLabel.lowercased().contains(lowerQuery) ||
            (numericDeck != nil && (fac.allDecks.contains(numericDeck!) || fac.deck == numericDeck))
    }
    for fac in matchedFacilities {
        if let deck = fac.primaryDeck {
            results.append(
                DeckSearchResult(
                    title: fac.placeName,
                    subtitle: fac.formattedLocation,
                    deckNumber: deck,
                    highlightQuery: fac.placeName,
                    type: .facility,
                    categoryKey: fac.categoryKey
                )
            )
        }
    }

    // 2. 客房前缀与数值模糊检索（排在设施之后，严格按数值升序呈现）
    if isNumeric {
        let matchedRooms = stateroomSearcher(trimmed)
        if !matchedRooms.isEmpty {
            for room in matchedRooms {
                results.append(
                    DeckSearchResult(
                        title: "客房 \(room.roomNumber)",
                        subtitle: "Deck \(room.deck) · 客房区域",
                        deckNumber: room.deck,
                        highlightQuery: room.roomNumber,
                        type: .stateroom
                    )
                )
            }
        } else {
            // 如果静态目录暂未包含该房间，但符合甲板房号规则，允许单独解析
            if let deck = parseStateroomDeck(trimmed) {
                results.append(
                    DeckSearchResult(
                        title: "客房 \(trimmed)",
                        subtitle: "Deck \(deck) · 客房区域",
                        deckNumber: deck,
                        highlightQuery: trimmed,
                        type: .stateroom
                    )
                )
            }
        }
    }

    // 3. 洗手间/厕所关键词检索
    let restroomKeywords = ["厕所", "洗手间", "卫生间", "restroom", "wc", "toilet", "washroom"]
    if restroomKeywords.contains(where: { lowerQuery.contains($0) }) {
        results.append(
            DeckSearchResult(
                title: "洗手间 Restroom",
                subtitle: "Deck \(currentDeck) · 当前甲板",
                deckNumber: currentDeck,
                highlightQuery: "Restroom",
                type: .restroom
            )
        )
        let otherDecks = [5, 6, 7, 8, 10, 11, 17, 18, 19].filter { $0 != currentDeck }
        for d in otherDecks {
            results.append(
                DeckSearchResult(
                    title: "洗手间 Restroom",
                    subtitle: "Deck \(d) · 公共区域",
                    deckNumber: d,
                    highlightQuery: "Restroom",
                    type: .restroom
                )
            )
        }
    }

    return results
}
