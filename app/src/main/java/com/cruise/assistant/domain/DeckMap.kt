package com.cruise.assistant.domain

/** 甲板信息模型 */
data class DeckInfo(
    val deckNumber: Int,
    val name: String,
    val shortLabel: String,
    val hasMap: Boolean,
    val assetPath: String,
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
    fun inlineHtmlWrapperFor(svgContent: String, highlightTarget: String? = null): String {
        val sanitizedHighlight = highlightTarget?.trim()?.takeIf { it.isNotEmpty() }
        val highlightScript = if (sanitizedHighlight != null) {
            val jsTarget = sanitizedHighlight.replace("'", "\\'").replace("\n", " ")
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
                function centerFacilityTarget() {
                    var target = '$jsTarget';
                    var svg = document.querySelector('svg');
                    if (!svg) return;
                    var textNodes = Array.from(svg.querySelectorAll('text, tspan'));
                    if (!textNodes || textNodes.length === 0) return;

                    var words = target.toLowerCase().split(/[\s·,]+/g).filter(function(w) {
                        return w.length > 1 && !['deck', 'the', 'and', 'restaurant', 'club'].includes(w);
                    });
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
