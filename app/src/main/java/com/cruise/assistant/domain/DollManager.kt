package com.cruise.assistant.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 达菲家族卡片玩偶贴纸管理器：
 * 负责 7 位角色的 60 个玩偶贴纸 asset 注册（按 head, half_body, full_body 三级形态归档）、
 * 按卡片高度自适应选择合适分级的人偶立绘，并提供位图采样加载与缓存。
 */
object DollManager {

    private val cache = ConcurrentHashMap<String, Bitmap>()

    val tieredDolls: Map<DuffyCharacter, Map<DollTier, List<String>>> = mapOf(
        DuffyCharacter.DUFFY to mapOf(
            DollTier.HEAD to listOf(
                "dolls/duffy/head/十月素材-星黛露-P2-9.png",
                "dolls/duffy/head/十月素材-星黛露-P2-7.png",
            ),
            DollTier.HALF_BODY to emptyList(),
            DollTier.FULL_BODY to listOf(
                "dolls/duffy/full_body/十月素材-达菲家族-P3-8.png",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-6.png",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-21.png",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-19.png",
                "dolls/duffy/full_body/十月素材-达菲家族-P3-17.png",
                "dolls/duffy/full_body/十月素材-星黛露-P5-68.png",
                "dolls/duffy/full_body/十月素材-星黛露-P2-31.png",
            ),
        ),
        DuffyCharacter.SHELLIEMAY to mapOf(
            DollTier.HEAD to listOf(
                "dolls/shelliemay/head/十月素材-达菲家族-P5-21.png",
                "dolls/shelliemay/head/十月素材-星黛露-P2-10.png",
            ),
            DollTier.HALF_BODY to emptyList(),
            DollTier.FULL_BODY to listOf(
                "dolls/shelliemay/full_body/十月素材-达菲家族-P5-31.png",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P5-29.png",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P4-32.png",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P2-4.png",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P2-38.png",
                "dolls/shelliemay/full_body/十月素材-达菲家族-P2-27.png",
                "dolls/shelliemay/full_body/十月素材-星黛露-P2-43.png",
                "dolls/shelliemay/full_body/十月素材-星黛露-P2-37.png",
            ),
        ),
        DuffyCharacter.STELLALOU to mapOf(
            DollTier.HEAD to listOf(
                "dolls/stellalou/head/十月素材-达菲家族-P4-12.png",
            ),
            DollTier.HALF_BODY to emptyList(),
            DollTier.FULL_BODY to listOf(
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-28.png",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-25.png",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-20.png",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-15.png",
                "dolls/stellalou/full_body/十月素材-达菲家族-P4-11.png",
                "dolls/stellalou/full_body/十月素材-达菲家族-P3-47.png",
                "dolls/stellalou/full_body/十月素材-星黛露-P2-4.png",
                "dolls/stellalou/full_body/十月素材-星黛露-P2-24.png",
                "dolls/stellalou/full_body/十月素材-星黛露-P2-17.png",
            ),
        ),
        DuffyCharacter.LINABELL to mapOf(
            DollTier.HEAD to listOf(
                "dolls/linabell/head/十月素材-玲娜贝儿-P1-34.png",
                "dolls/linabell/head/十月素材-玲娜贝儿-P1-33.png",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/linabell/half_body/十月素材-玲娜贝儿-P1-7.png",
                "dolls/linabell/half_body/十月素材-玲娜贝儿-P1-4.png",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P2-9.png",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P2-3.png",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-51.png",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-30.png",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-28.png",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-21.png",
                "dolls/linabell/full_body/十月素材-玲娜贝儿-P1-20.png",
            ),
        ),
        DuffyCharacter.GELATONI to mapOf(
            DollTier.HEAD to listOf(
                "dolls/gelatoni/head/十月素材-星黛露-P2-8.png",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/gelatoni/half_body/十月素材-达菲家族-P1-5.png",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/gelatoni/full_body/十月素材-星黛露-P5-67.png",
                "dolls/gelatoni/full_body/十月素材-星黛露-P5-64.png",
                "dolls/gelatoni/full_body/十月素材-星黛露-P2-35.png",
                "dolls/gelatoni/full_body/十月素材-星黛露-P2-32.png",
            ),
        ),
        DuffyCharacter.COOKIEANN to mapOf(
            DollTier.HEAD to listOf(
                "dolls/cookieann/head/十月素材-可琦安-P2-5.png",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/cookieann/half_body/十月素材-可琦安-P2-4.png",
                "dolls/cookieann/half_body/十月素材-可琦安-P2-11.png",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/cookieann/full_body/十月素材-星黛露-P5-11.png",
                "dolls/cookieann/full_body/十月素材-星黛露-P5-10.png",
                "dolls/cookieann/full_body/十月素材-可琦安-P2-7.png",
                "dolls/cookieann/full_body/十月素材-可琦安-P2-1.png",
            ),
        ),
        DuffyCharacter.OLUMEL to mapOf(
            DollTier.HEAD to listOf(
                "dolls/olumel/head/十月素材-奥乐米拉-P2-28.png",
            ),
            DollTier.HALF_BODY to listOf(
                "dolls/olumel/half_body/十月素材-星黛露-P5-13.png",
                "dolls/olumel/half_body/十月素材-奥乐米拉-P2-62.png",
            ),
            DollTier.FULL_BODY to listOf(
                "dolls/olumel/full_body/十月素材-奥乐米拉-P3-9.png",
                "dolls/olumel/full_body/十月素材-奥乐米拉-P3-3.png",
                "dolls/olumel/full_body/十月素材-奥乐米拉-P3-10.png",
                "dolls/olumel/full_body/十月素材-奥乐米拉-P2-25.png",
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
        return tieredDolls.values.flatMap { tierMap -> tierMap.values.flatten() }
    }

    fun getCharacterForAsset(assetPath: String): DuffyCharacter? {
        return DuffyCharacter.entries.firstOrNull { assetPath.startsWith(it.assetDir) }
    }

    fun getTierForAsset(assetPath: String): DollTier? {
        return when {
            assetPath.contains("/head/") -> DollTier.HEAD
            assetPath.contains("/half_body/") -> DollTier.HALF_BODY
            assetPath.contains("/full_body/") -> DollTier.FULL_BODY
            else -> null
        }
    }

    /**
     * 为日程卡片确定性挑选单张人偶立绘贴纸
     * @param eventId 事件 ID，按 eventId % 角色数确定角色，并轮换素材
     * @param cardHeightDp 卡片高度（dp），自适应匹配 Head / Half-Body / Full-Body
     * @return 匹配的人偶素材 asset 路径
     */
    fun selectDollForCard(
        eventId: Long,
        cardHeightDp: Float,
    ): String? {
        val characters = DuffyCharacter.entries
        val charIndex = ((eventId % characters.size + characters.size) % characters.size).toInt()
        val character = characters[charIndex]

        val targetTier = DollTier.forCardHeight(cardHeightDp)
        val fallbackOrder = when (targetTier) {
            DollTier.HEAD -> listOf(DollTier.HEAD, DollTier.HALF_BODY, DollTier.FULL_BODY)
            DollTier.HALF_BODY -> listOf(DollTier.HALF_BODY, DollTier.HEAD, DollTier.FULL_BODY)
            DollTier.FULL_BODY -> listOf(DollTier.FULL_BODY, DollTier.HALF_BODY, DollTier.HEAD)
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
     * 采样加载玩偶 PNG 贴纸并缓存
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
