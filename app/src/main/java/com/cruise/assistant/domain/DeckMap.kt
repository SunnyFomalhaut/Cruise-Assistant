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

    /** 目前已拥有 SVG 图纸的甲板集合 */
    val AVAILABLE_DECKS = setOf(5, 6, 7, 8, 9)

    /** 默认最大甲板层数 */
    const val DEFAULT_MAX_DECK = 19

    /**
     * 获取全船甲板列表（从高层到低层降序排列，如 D19 -> D1）
     */
    fun getDecks(maxDeck: Int = DEFAULT_MAX_DECK): List<DeckInfo> {
        val validMax = maxDeck.coerceAtLeast(1)
        return (validMax downTo 1).map { deck ->
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
