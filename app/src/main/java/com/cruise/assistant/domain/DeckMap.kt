package com.cruise.assistant.domain

/** 甲板信息模型 */
data class DeckInfo(
    val deckNumber: Int,
    val name: String,
    val shortLabel: String,
    val hasMap: Boolean,
    val assetPath: String,
)

/** 仙尘客房回礼状态 */
enum class PixieRoomStatus {
    UNREPLIED,
    NEED_SUPPLEMENT,
    COMPLETED,
    GIVEN_PENDING,
}

/** 仙尘客房标记数据模型 */
data class PixieRoomMarker(
    val roomNumber: String,
    val status: PixieRoomStatus,
    val unrepliedCount: Int = 0,
    val givenCount: Int = 0,
    val receivedCount: Int = 0,
)

/** 全船甲板地图管理仓储 */
object DeckMapRepository {

    /** 目前已拥有 SVG 图纸的甲板集合（1~4为轮机舱/船员舱不开放，14层不存在） */
    val AVAILABLE_DECKS = setOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19)

    /** 乘客开放甲板列表（从高层到低层降序排列，排除 1~4 层与不存在的 14 层） */
    val PASSENGER_DECKS = listOf(19, 18, 17, 16, 15, 13, 12, 11, 10, 9, 8, 7, 6, 5)

    /** 默认最大甲板层数 */
    const val DEFAULT_MAX_DECK = 19

    /**
     * 获取全船对客开放甲板列表（从高层到低层降序排列，如 D19 -> D5，排除 14 层）
     */
    fun getDecks(maxDeck: Int = DEFAULT_MAX_DECK): List<DeckInfo> {
        return PASSENGER_DECKS.filter { it <= maxDeck }.map { deck ->
            val hasMap = hasDeckMap(deck)
            DeckInfo(
                deckNumber = deck,
                name = "Deck $deck",
                shortLabel = "D$deck",
                hasMap = hasMap,
                assetPath = "decks/deck_$deck.svg",
            )
        }
    }

    /**
     * 判断指定甲板是否具备平面图纸
     */
    fun hasDeckMap(deckNumber: Int): Boolean = deckNumber in AVAILABLE_DECKS

    /**
     * 获取指定甲板的 Android Asset 本地 URL
     */
    fun assetUrlFor(deckNumber: Int): String = "file:///android_asset/decks/deck_$deckNumber.svg"

    /**
     * 将 SVG 字符串内联到响应式 HTML 容器中，免受安全沙箱限制；支持高亮与地图钉定位指定设施
     */
    fun inlineHtmlWrapperFor(
        svgContent: String,
        highlightTarget: String? = null,
        pixieMarkers: List<PixieRoomMarker> = emptyList(),
    ): String {
        val sanitizedHighlight = highlightTarget?.trim()?.takeIf { it.isNotEmpty() }
        val expandedTarget = sanitizedHighlight?.let { target ->
            val allEntries = LocationCatalog.allEntries()
            allEntries.firstOrNull { it.placeName == target }?.placeName
                ?: allEntries.firstOrNull { it.placeName.contains(target) }?.placeName
                ?: allEntries.firstOrNull { target.contains(it.placeName) }?.placeName
                ?: allEntries.firstOrNull { entry ->
                    val zhName = entry.placeName.split(" ").firstOrNull() ?: ""
                    zhName.length >= 2 && target.contains(zhName)
                }?.placeName
                ?: target
        }
        val highlightScript = if (expandedTarget != null) {
            val jsTarget = expandedTarget
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", " ")
                .replace("\n", " ")
            """
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
                function normalizeText(str) {
                    if (!str) return '';
                    var s = str.toLowerCase();
                    s = s.replace(/['’‘“”`"]/g, '');
                    s = s.replace(/[\s·,\/'"()\-_]+/g, ' ');
                    return s.trim();
                }

                function centerFacilityTarget() {
                    var target = '$jsTarget';
                    var svg = document.querySelector('svg');
                    if (!svg) return;
                    var textNodes = Array.from(svg.querySelectorAll('text, tspan'));
                    if (!textNodes || textNodes.length === 0) return;

                    var normTarget = normalizeText(target);
                    var rawWords = normTarget.split(' ');
                    var stopWords = ['deck', 'the', 'and', 'restaurant', 'club'];
                    var words = rawWords.filter(function(w) {
                        return w.length > 1 && stopWords.indexOf(w) === -1;
                    });

                    if (target.indexOf('航海家') !== -1) { words.push('navigator'); }
                    if (target.indexOf('盛宴') !== -1) { words.push('enchanted', 'summer'); }
                    if (target.indexOf('动画师') !== -1) { words.push('animator'); }
                    if (target.indexOf('聚光灯') !== -1) { words.push('spotlight'); }
                    if (target.indexOf('亚洲风味') !== -1 || target.indexOf('怪兽') !== -1) { words.push('flavors', 'asia', 'mike', 'sulley'); }
                    if (target.indexOf('帕洛') !== -1) { words.push('palo', 'trattoria'); }
                    if (target.indexOf('花园舞台') !== -1) { words.push('garden', 'stage'); }
                    if (target.indexOf('迪士尼剧院') !== -1) { words.push('walt', 'disney', 'theatre', 'theater'); }
                    if (target.indexOf('寻路者') !== -1) { words.push('wayfinder'); }
                    if (target.indexOf('大白') !== -1) { words.push('baymax'); }
                    if (target.indexOf('泳池') !== -1) { words.push('pool', 'sunnyside'); }

                    if (words.length === 0) {
                        words = [normTarget];
                    }

                    var bestNode = null;
                    var bestScore = 0;

                    for (var i = 0; i < textNodes.length; i++) {
                        var node = textNodes[i];
                        var rawContent = node.textContent || '';
                        var content = normalizeText(rawContent);
                        if (!content) continue;
                        var score = 0;
                        for (var j = 0; j < words.length; j++) {
                            var w = normalizeText(words[j]);
                            if (w && content.indexOf(w) !== -1) {
                                score += w.length * 2;
                            }
                        }
                        if (normTarget && content.indexOf(normTarget) !== -1) {
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
                    var safeHtmlTarget = target.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                    pin.innerHTML = '<div class="pin-content"><span>📍</span><span>' + safeHtmlTarget + '</span></div><div class="pin-arrow"></div>';
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
            """.trimIndent()
        } else ""

        val pixieScript = if (pixieMarkers.isNotEmpty()) {
            val markersJson = pixieMarkers.joinToString(prefix = "[", postfix = "]") { marker ->
                val label = when (marker.status) {
                    PixieRoomStatus.UNREPLIED -> if (marker.unrepliedCount > 0) "🔴 待回 ${marker.unrepliedCount}" else "🔴 待回"
                    PixieRoomStatus.NEED_SUPPLEMENT -> "🟡 补礼"
                    PixieRoomStatus.COMPLETED -> "🟢 持平"
                    PixieRoomStatus.GIVEN_PENDING -> "🔵 已发"
                }
                val safeRoom = marker.roomNumber.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                val safeLabel = label.replace("\\", "\\\\").replace("\"", "\\\"")
                """{"room":"$safeRoom","status":"${marker.status.name}","label":"$safeLabel"}"""
            }
            """
            <style>
                .pixie-highlight-box {
                    position: absolute;
                    pointer-events: none;
                    border-radius: 6px;
                    z-index: 1000;
                }
                .pixie-box-unreplied {
                    border: 2.5px solid #EF4444;
                    background: rgba(239, 68, 68, 0.22);
                    box-shadow: 0 0 12px rgba(239, 68, 68, 0.85);
                    animation: pixieRedPulse 1.8s infinite ease-in-out;
                }
                .pixie-box-supplement {
                    border: 2.5px solid #F59E0B;
                    background: rgba(245, 158, 11, 0.22);
                    box-shadow: 0 0 12px rgba(245, 158, 11, 0.85);
                    animation: pixieYellowPulse 1.8s infinite ease-in-out;
                }
                .pixie-box-completed {
                    border: 2px solid #10B981;
                    background: rgba(16, 185, 129, 0.18);
                    box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
                    z-index: 999;
                }
                .pixie-box-given-pending {
                    border: 2px solid #0284C7;
                    background: rgba(2, 132, 199, 0.20);
                    box-shadow: 0 0 8px rgba(2, 132, 199, 0.5);
                    z-index: 999;
                }
                @keyframes pixieRedPulse {
                    0% {
                        box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.85);
                        transform: scale(0.99);
                    }
                    50% {
                        box-shadow: 0 0 0 8px rgba(239, 68, 68, 0);
                        transform: scale(1.01);
                    }
                    100% {
                        box-shadow: 0 0 0 0 rgba(239, 68, 68, 0);
                        transform: scale(0.99);
                    }
                }
                @keyframes pixieYellowPulse {
                    0% {
                        box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.85);
                        transform: scale(0.99);
                    }
                    50% {
                        box-shadow: 0 0 0 8px rgba(245, 158, 11, 0);
                        transform: scale(1.01);
                    }
                    100% {
                        box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);
                        transform: scale(0.99);
                    }
                }
                .pixie-pin-badge {
                    position: absolute;
                    pointer-events: none;
                    z-index: 1002;
                    transform: translate(-50%, -100%);
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                }
                .pixie-pin-content {
                    font-size: 11px;
                    font-weight: 700;
                    padding: 2px 7px;
                    border-radius: 12px;
                    color: #FFFFFF;
                    white-space: nowrap;
                    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.35);
                }
                .pixie-marker-unreplied {
                    background: #EF4444;
                }
                .pixie-marker-supplement {
                    background: #F59E0B;
                }
                .pixie-marker-completed {
                    background: #10B981;
                }
                .pixie-marker-given-pending {
                    background: #0284C7;
                }
                .pixie-pin-arrow {
                    width: 0;
                    height: 0;
                    border-left: 4px solid transparent;
                    border-right: 4px solid transparent;
                    margin-top: -1px;
                }
                .pixie-arrow-unreplied {
                    border-top: 5px solid #EF4444;
                }
                .pixie-arrow-supplement {
                    border-top: 5px solid #F59E0B;
                }
                .pixie-arrow-completed {
                    border-top: 5px solid #10B981;
                }
                .pixie-arrow-given-pending {
                    border-top: 5px solid #0284C7;
                }
            </style>
            <script>
                var pixieMarkersData = $markersJson;

                function renderPixieMarkers() {
                    if (!pixieMarkersData || pixieMarkersData.length === 0) return;
                    var existing = document.querySelectorAll('.pixie-highlight-box, .pixie-pin-badge');
                    for (var i = 0; i < existing.length; i++) {
                        existing[i].remove();
                    }

                    var scrollLeft = window.pageXOffset || document.documentElement.scrollLeft || 0;
                    var scrollTop = window.pageYOffset || document.documentElement.scrollTop || 0;

                    var priorityBounds = null;
                    var allBounds = null;

                    function updateBounds(bounds, rect, absLeft, absTop) {
                        if (!bounds) {
                            return {
                                minX: absLeft,
                                 maxX: absLeft + rect.width,
                                 minY: absTop,
                                 maxY: absTop + rect.height
                            };
                        }
                        bounds.minX = Math.min(bounds.minX, absLeft);
                        bounds.maxX = Math.max(bounds.maxX, absLeft + rect.width);
                        bounds.minY = Math.min(bounds.minY, absTop);
                        bounds.maxY = Math.max(bounds.maxY, absTop + rect.height);
                        return bounds;
                    }

                    for (var m = 0; m < pixieMarkersData.length; m++) {
                        var item = pixieMarkersData[m];
                        var elem = document.getElementById('room-' + item.room) ||
                                   document.getElementById('outline-' + item.room) ||
                                   document.getElementById('number-' + item.room) ||
                                   document.querySelector('[data-finder-group-id="' + item.room + '"]');
                        if (!elem) continue;

                        var rect = elem.getBoundingClientRect();
                        if (rect.width === 0 && rect.height === 0) continue;

                        var absLeft = rect.left + scrollLeft;
                        var absTop = rect.top + scrollTop;

                        if (item.status === 'UNREPLIED' || item.status === 'NEED_SUPPLEMENT') {
                            priorityBounds = updateBounds(priorityBounds, rect, absLeft, absTop);
                        }
                        allBounds = updateBounds(allBounds, rect, absLeft, absTop);

                        var box = document.createElement('div');
                        var boxClass = 'pixie-highlight-box';
                        var markerClass = 'pixie-marker-unreplied';
                        var arrowClass = 'pixie-arrow-unreplied';

                        if (item.status === 'UNREPLIED') {
                            boxClass += ' pixie-box-unreplied';
                            markerClass = 'pixie-marker-unreplied';
                            arrowClass = 'pixie-arrow-unreplied';
                        } else if (item.status === 'NEED_SUPPLEMENT') {
                            boxClass += ' pixie-box-supplement';
                            markerClass = 'pixie-marker-supplement';
                            arrowClass = 'pixie-arrow-supplement';
                        } else if (item.status === 'GIVEN_PENDING') {
                            boxClass += ' pixie-box-given-pending';
                            markerClass = 'pixie-marker-given-pending';
                            arrowClass = 'pixie-arrow-given-pending';
                        } else {
                            boxClass += ' pixie-box-completed';
                            markerClass = 'pixie-marker-completed';
                            arrowClass = 'pixie-arrow-completed';
                        }
                        box.className = boxClass;
                        box.style.left = (absLeft - 2) + 'px';
                        box.style.top = (absTop - 2) + 'px';
                        box.style.width = (rect.width + 4) + 'px';
                        box.style.height = (rect.height + 4) + 'px';
                        document.body.appendChild(box);

                        var pin = document.createElement('div');
                        pin.className = 'pixie-pin-badge';
                        pin.style.left = (absLeft + rect.width / 2) + 'px';
                        pin.style.top = (absTop - 2) + 'px';
                        pin.innerHTML = '<div class="pixie-pin-content ' + markerClass + '">' + item.label + '</div><div class="pixie-pin-arrow ' + arrowClass + '"></div>';
                        document.body.appendChild(pin);
                    }

                    var targetBounds = priorityBounds || allBounds;
                    if (targetBounds) {
                        var centerX = (targetBounds.minX + targetBounds.maxX) / 2;
                        var centerY = (targetBounds.minY + targetBounds.maxY) / 2;
                        var targetX = centerX - (window.innerWidth / 2);
                        var targetY = centerY - (window.innerHeight / 2);
                        window.scrollTo({
                            left: Math.max(0, targetX),
                            top: Math.max(0, targetY),
                            behavior: 'smooth'
                        });
                    }
                }

                window.addEventListener('DOMContentLoaded', function() {
                    setTimeout(renderPixieMarkers, 80);
                    setTimeout(renderPixieMarkers, 250);
                    setTimeout(renderPixieMarkers, 600);
                });
                window.addEventListener('load', function() {
                    setTimeout(renderPixieMarkers, 120);
                });
            </script>
            """.trimIndent()
        } else ""

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
                $highlightScript
                $pixieScript
            </head>
            <body>
                <div id="svg-wrapper">
                    $svgContent
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * 获取 HTML 包装内容，以实现响应式缩放与透明背景渲染
     */
    fun htmlWrapperFor(svgUrl: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=8.0, user-scalable=yes">
                <style>
                    html, body {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background-color: transparent;
                        display: flex;
                        justify-content: center;
                        align-items: flex-start;
                        overflow: auto;
                    }
                    img {
                        width: 100%;
                        max-width: 600px;
                        height: auto;
                        display: block;
                        margin: 16px auto;
                    }
                </style>
            </head>
            <body>
                <img src="$svgUrl" alt="Deck Map" />
            </body>
            </html>
        """.trimIndent()
    }
}
