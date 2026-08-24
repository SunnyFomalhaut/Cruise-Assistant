package com.cruise.assistant.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * 达菲家族卡片玩偶贴纸管理器：
 * 负责 7 位角色及双人组合的 91 个玩偶贴纸 asset 注册（按 head, half_body, full_body 及 duo 分级形态归档）、
 * 按卡片高度自适应选择合适分级的人偶立绘，并提供位图采样加载与缓存。
 */
object DollManager {

    private val cache = ConcurrentHashMap<String, Bitmap>()

    val DUO_ASSETS: List<String> = listOf(
        "dolls/duo/十月素材-可琦安-P2-2.webp",
        "dolls/duo/十月素材-奥乐米拉-P2-126.webp",
        "dolls/duo/十月素材-玲娜贝儿-P1-8.webp",
        "dolls/duo/十月素材-玲娜贝儿-P1-40.webp",
        "dolls/duo/十月素材-达菲家族-P2-5.webp",
        "dolls/duo/十月素材-达菲家族-P2-8.webp",
        "dolls/duo/十月素材-达菲家族-P2-21.webp",
        "dolls/duo/十月素材-达菲家族-P2-22.webp",
    )

    val tieredDolls: Map<DuffyCharacter, Map<DollTier, List<String>>> = mapOf(
        DuffyCharacter.DUFFY to mapOf(
            DollTier.HEAD to listOf(
                "dolls/duffy/head/十月素材-星黛露-P2-7.webp",
                "dolls/duffy/head/十月素材-星黛露-P2-9.webp",
                "dolls/duffy/head/十月素材-达菲家族-P6-5.webp",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/duffy/half_body/十月素材-星黛露-P1-8.webp",
                "dolls/duffy/half_body/十月素材-达菲家族-P2-13.webp",
                "dolls/duffy/half_body/十月素材-达菲家族-P6-2.webp",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/duffy/full_body/十月素材-星黛露-P2-31.webp",
                "dolls/duffy/full_body/十月素材-星黛露-P5-68.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P2-45.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P2-46.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P2-48.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-6.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-8.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-17.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-19.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-21.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P6-3.webp",
                "dolls/duffy/full_body/十月素材-达菲家族-P6-4.webp",
            ),
        ),
        DuffyCharacter.SHELLIEMAY to mapOf(
            DollTier.HEAD to listOf(
                "dolls/shelliemay/head/十月素材-星黛露-P2-10.webp",
                "dolls/shelliemay/head/十月素材-达菲家族-P5-21.webp",
            ),
            DollTier.HALF_BODY to emptyList(),
            DollTier.FULL_BODY to listOf(
                "dolls/shelliemay/full_body/十月素材-星黛露-P2-32.webp",
                "dolls/shelliemay/full_body/十月素材-星黛露-P2-37.webp",
                "dolls/shelliemay/full_body/十月素材-星黛露-P2-43.webp",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P2-4.webp",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P2-27.webp",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P2-38.webp",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P4-32.webp",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P5-29.webp",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P5-31.webp",
            ),
        ),
        DuffyCharacter.STELLALOU to mapOf(
            DollTier.HEAD to listOf(
                "dolls/stellalou/head/十月素材-达菲家族-P4-12.webp",
                "dolls/stellalou/head/十月素材-达菲家族-P5-26.webp",
            ),
            DollTier.HALF_BODY to emptyList(),
            DollTier.FULL_BODY to listOf(
                "dolls/stellalou/full_body/十月素材-星黛露-P2-4.webp",
                "dolls/stellalou/full_body/十月素材-星黛露-P2-17.webp",
                "dolls/stellalou/full_body/十月素材-星黛露-P2-24.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P3-47.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P3-57.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-11.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-15.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-20.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-25.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-28.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P5-10.webp",
                "dolls/stellalou/full_body/十月素材-达菲家族-P5-18.webp",
            ),
        ),
        DuffyCharacter.LINABELL to mapOf(
            DollTier.HEAD to listOf(
                "dolls/linabell/head/十月素材-玲娜贝儿-P1-33.webp",
                "dolls/linabell/head/十月素材-玲娜贝儿-P1-34.webp",
                "dolls/linabell/head/十月素材-玲娜贝儿-P1-42.webp",
                "dolls/linabell/head/十月素材-玲娜贝儿-P1-45.webp",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/linabell/half_body/十月素材-玲娜贝儿-P1-4.webp",
                "dolls/linabell/half_body/十月素材-玲娜贝儿-P1-7.webp",
                "dolls/linabell/half_body/十月素材-玲娜贝儿-P1-44.webp",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-20.webp",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-21.webp",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-28.webp",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-30.webp",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-43.webp",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-51.webp",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P2-3.webp",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P2-9.webp",
            ),
        ),
        DuffyCharacter.GELATONI to mapOf(
            DollTier.HEAD to listOf(
                "dolls/gelatoni/head/十月素材-星黛露-P2-8.webp",
                "dolls/gelatoni/head/十月素材-达菲家族-P1-4.webp",
                "dolls/gelatoni/head/十月素材-达菲家族-P1-16.webp",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/gelatoni/half_body/十月素材-达菲家族-P1-5.webp",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/gelatoni/full_body/十月素材-星黛露-P2-35.webp",
                "dolls/gelatoni/full_body/十月素材-星黛露-P5-64.webp",
                "dolls/gelatoni/full_body/十月素材-星黛露-P5-67.webp",
                "dolls/gelatoni/full_body/十月素材-达菲家族-P1-22.webp",
                "dolls/gelatoni/full_body/十月素材-达菲家族-P1-23.webp",
                "dolls/gelatoni/full_body/十月素材-达菲家族-P1-24.webp",
            ),
        ),
        DuffyCharacter.COOKIEANN to mapOf(
            DollTier.HEAD to listOf(
                "dolls/cookieann/head/十月素材-可琦安-P2-5.webp",
                "dolls/cookieann/head/十月素材-可琦安-P2-10.webp",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/cookieann/half_body/十月素材-可琦安-P2-4.webp",
                "dolls/cookieann/half_body/十月素材-可琦安-P2-11.webp",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/cookieann/full_body/十月素材-可琦安-P2-1.webp",
                "dolls/cookieann/full_body/十月素材-可琦安-P2-7.webp",
                "dolls/cookieann/full_body/十月素材-星黛露-P5-10.webp",
                "dolls/cookieann/full_body/十月素材-星黛露-P5-11.webp",
            ),
        ),
        DuffyCharacter.OLUMEL to mapOf(
            DollTier.HEAD to listOf(
                "dolls/olumel/head/十月素材-奥乐米拉-P2-28.webp",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/olumel/half_body/十月素材-星黛露-P5-13.webp",
                "dolls/olumel/half_body/十月素材-奥乐米拉-P2-62.webp",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/olumel/full_body/十月素材-奥乐米拉-P2-25.webp",
                "dolls/olumel/full_body/十月素材-奥乐米拉-P3-3.webp",
                "dolls/olumel/full_body/十月素材-奥乐米拉-P3-9.webp",
                "dolls/olumel/full_body/十月素材-奥乐米拉-P3-10.webp",
            ),
        ),
    )

    fun getAssets(character: DuffyCharacter, tier: DollTier): List<String> {
        return tieredDolls[character]?.get(tier) ?: emptyList()
    }

    fun getAssetsForCharacter(character: DuffyCharacter): List<String> {
        return tieredDolls[character]?.values?.flatten() ?: emptyList()
    }

    fun getAllAssets(): List<String> {
        return tieredDolls.values.flatMap { tierMap -> tierMap.values.flatten() } + DUO_ASSETS
    }

    fun getCharacterForAsset(assetPath: String): DuffyCharacter? {
        return DuffyCharacter.entries.firstOrNull { assetPath.startsWith(it.assetDir) }
    }

    fun getTierForAsset(assetPath: String): DollTier? {
        return when {
            assetPath.contains("/head/") -> DollTier.HEAD
            assetPath.contains("/half_body/") -> DollTier.HALF_BODY
            assetPath.contains("/full_body/") -> DollTier.FULL_BODY
            assetPath.startsWith("dolls/duo/") || assetPath.contains("/duo/") -> DollTier.DUO
            else -> null
        }
    }

    /**
     * 为日程卡片确定性挑选人偶立绘贴纸
     *
     * 触发规则：
     * 1. 满足双人组合条件（已确定日程 + 时长 >= 70分钟 + 非复杂卡片），从 DUO_ASSETS 中挑选双人组合贴纸。
     * 2. 否则按卡片高度确定分级（HEAD, HALF_BODY, FULL_BODY），挑选单人角色并带降级回退。
     *
     * @param eventId 事件 ID
     * @param cardHeightDp 卡片高度（dp）
     * @param durationMinutes 事件持续时长（分钟）
     * @param isFinalized 是否为已确定/锁定日程（单泳道）
     * @param isComplex 是否为复杂卡片（包含等待段、演出类型、存在冲突等）
     * @return 匹配的人偶素材 asset 路径
     */
    fun selectDollForCard(
        eventId: Long,
        cardHeightDp: Float,
        durationMinutes: Int = 0,
        isFinalized: Boolean = false,
        isComplex: Boolean = false,
    ): String? {
        if (isFinalized && durationMinutes >= DollTier.DUO_MIN_DURATION_MINUTES && !isComplex && DUO_ASSETS.isNotEmpty()) {
            val hash = (eventId * 31).hashCode()
            val duoIndex = (abs(hash) % DUO_ASSETS.size).let { if (it < 0) 0 else it }
            return DUO_ASSETS[duoIndex]
        }

        val characters = DuffyCharacter.entries
        val charIndex = ((eventId % characters.size + characters.size) % characters.size).toInt()
        val character = characters[charIndex]

        val targetTier = DollTier.forCardHeight(cardHeightDp)
        val fallbackOrder = when (targetTier) {
            DollTier.HEAD -> listOf(DollTier.HEAD, DollTier.HALF_BODY, DollTier.FULL_BODY)
            DollTier.HALF_BODY -> listOf(DollTier.HALF_BODY, DollTier.HEAD, DollTier.FULL_BODY)
            DollTier.FULL_BODY -> listOf(DollTier.FULL_BODY, DollTier.HALF_BODY, DollTier.HEAD)
            DollTier.DUO -> listOf(DollTier.FULL_BODY, DollTier.HALF_BODY, DollTier.HEAD)
        }

        val availableAssets = fallbackOrder.asSequence()
            .map { getAssets(character, it) }
            .firstOrNull { it.isNotEmpty() }
            ?: return null

        val assetIndex = ((eventId / characters.size % availableAssets.size + availableAssets.size) % availableAssets.size).toInt()
        return availableAssets[assetIndex]
    }

    @Deprecated(
        message = "Use selectDollForCard instead",
        replaceWith = ReplaceWith("Pair(selectDollForCard(eventId, cardHeightDp), null)")
    )
    fun selectDollsForCard(
        eventId: Long,
        cardColorArgb: Int = 0,
        cardHeightDp: Float = 60f,
        blankHeightDp: Float = 0f,
        mode: DollDistributionMode = DollDistributionMode.UNIFORM_RANDOM
    ): Pair<String?, String?> {
        return Pair(selectDollForCard(eventId, cardHeightDp), null)
    }

    /**
     * 采样加载玩偶 WebP/PNG 贴纸并缓存
     */
    fun loadDollBitmap(context: Context, assetPath: String, maxDimDp: Int = 160): Bitmap? {
        cache[assetPath]?.let { if (!it.isRecycled) return it }

        return try {
            val density = context.resources.displayMetrics.density
            val reqPx = (maxDimDp * density).toInt().coerceAtLeast(1)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > reqPx || options.outWidth > reqPx) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqPx && halfWidth / inSampleSize >= reqPx) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inJustDecodeBounds = false
            }
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)?.also {
                    cache[assetPath] = it
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
