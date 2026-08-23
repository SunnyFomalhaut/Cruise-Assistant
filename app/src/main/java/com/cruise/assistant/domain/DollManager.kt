package com.cruise.assistant.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 达菲家族卡片玩偶贴纸管理器：
 * 负责 7 位角色的 60 个玩偶贴纸 asset 注册、HSV 颜色亲和度匹配、按卡片高度和空白高度的确定性单/双玩偶贴纸挑选与位图缓存。
 */
object DollManager {

    private val cache = ConcurrentHashMap<String, Bitmap>()

    val allDolls: Map<DuffyCharacter, List<String>> = mapOf(
        DuffyCharacter.DUFFY to listOf(
            "dolls/duffy/十月素材-达菲家族-P3-8.png",
            "dolls/duffy/十月素材-达菲家族-P3-6.png",
            "dolls/duffy/十月素材-达菲家族-P3-21.png",
            "dolls/duffy/十月素材-达菲家族-P3-19.png",
            "dolls/duffy/十月素材-达菲家族-P3-17.png",
            "dolls/duffy/十月素材-星黛露-P5-68.png",
            "dolls/duffy/十月素材-星黛露-P2-9.png",
            "dolls/duffy/十月素材-星黛露-P2-7.png",
            "dolls/duffy/十月素材-星黛露-P2-31.png",
        ),
        DuffyCharacter.SHELLIEMAY to listOf(
            "dolls/shelliemay/十月素材-达菲家族-P5-31.png",
            "dolls/shelliemay/十月素材-达菲家族-P5-29.png",
            "dolls/shelliemay/十月素材-达菲家族-P5-21.png",
            "dolls/shelliemay/十月素材-达菲家族-P4-32.png",
            "dolls/shelliemay/十月素材-达菲家族-P2-4.png",
            "dolls/shelliemay/十月素材-达菲家族-P2-38.png",
            "dolls/shelliemay/十月素材-达菲家族-P2-27.png",
            "dolls/shelliemay/十月素材-星黛露-P2-43.png",
            "dolls/shelliemay/十月素材-星黛露-P2-37.png",
            "dolls/shelliemay/十月素材-星黛露-P2-10.png",
        ),
        DuffyCharacter.STELLALOU to listOf(
            "dolls/stellalou/十月素材-达菲家族-P4-28.png",
            "dolls/stellalou/十月素材-达菲家族-P4-25.png",
            "dolls/stellalou/十月素材-达菲家族-P4-20.png",
            "dolls/stellalou/十月素材-达菲家族-P4-15.png",
            "dolls/stellalou/十月素材-达菲家族-P4-12.png",
            "dolls/stellalou/十月素材-达菲家族-P4-11.png",
            "dolls/stellalou/十月素材-达菲家族-P3-47.png",
            "dolls/stellalou/十月素材-星黛露-P2-4.png",
            "dolls/stellalou/十月素材-星黛露-P2-24.png",
            "dolls/stellalou/十月素材-星黛露-P2-17.png",
        ),
        DuffyCharacter.LINABELL to listOf(
            "dolls/linabell/十月素材-玲娜贝儿-P2-9.png",
            "dolls/linabell/十月素材-玲娜贝儿-P2-3.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-7.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-51.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-4.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-34.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-33.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-30.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-28.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-21.png",
            "dolls/linabell/十月素材-玲娜贝儿-P1-20.png",
        ),
        DuffyCharacter.GELATONI to listOf(
            "dolls/gelatoni/十月素材-达菲家族-P1-5.png",
            "dolls/gelatoni/十月素材-星黛露-P5-67.png",
            "dolls/gelatoni/十月素材-星黛露-P5-64.png",
            "dolls/gelatoni/十月素材-星黛露-P2-8.png",
            "dolls/gelatoni/十月素材-星黛露-P2-35.png",
            "dolls/gelatoni/十月素材-星黛露-P2-32.png",
        ),
        DuffyCharacter.COOKIEANN to listOf(
            "dolls/cookieann/十月素材-星黛露-P5-11.png",
            "dolls/cookieann/十月素材-星黛露-P5-10.png",
            "dolls/cookieann/十月素材-可琦安-P2-7.png",
            "dolls/cookieann/十月素材-可琦安-P2-5.png",
            "dolls/cookieann/十月素材-可琦安-P2-4.png",
            "dolls/cookieann/十月素材-可琦安-P2-11.png",
            "dolls/cookieann/十月素材-可琦安-P2-1.png",
        ),
        DuffyCharacter.OLUMEL to listOf(
            "dolls/olumel/十月素材-星黛露-P5-13.png",
            "dolls/olumel/十月素材-奥乐米拉-P3-9.png",
            "dolls/olumel/十月素材-奥乐米拉-P3-3.png",
            "dolls/olumel/十月素材-奥乐米拉-P3-10.png",
            "dolls/olumel/十月素材-奥乐米拉-P2-62.png",
            "dolls/olumel/十月素材-奥乐米拉-P2-28.png",
            "dolls/olumel/十月素材-奥乐米拉-P2-25.png",
        ),
    )

    fun getAssetsForCharacter(character: DuffyCharacter): List<String> {
        return allDolls[character] ?: emptyList()
    }

    fun getCharacterForAsset(assetPath: String): DuffyCharacter? {
        return allDolls.entries.firstOrNull { (_, list) -> list.contains(assetPath) }?.key
            ?: DuffyCharacter.entries.firstOrNull { assetPath.startsWith(it.assetDir) }
    }

    /**
     * 将 ARGB Int 纯数学转换为 HSV (Hue 0°..360°, Saturation 0..1, Value 0..1)
     */
    fun colorToHsv(color: Int): Triple<Float, Float, Float> {
        val r = ((color ushr 16) and 0xFF) / 255f
        val g = ((color ushr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val v = max
        val s = if (max == 0f) 0f else delta / max
        val h = if (delta == 0f) {
            0f
        } else {
            val rawH = when (max) {
                r -> ((g - b) / delta) % 6f
                g -> ((b - r) / delta) + 2f
                else -> ((r - g) / delta) + 4f
            }
            val angle = rawH * 60f
            if (angle < 0f) angle + 360f else angle
        }
        return Triple(h, s, v)
    }

    /**
     * 根据颜色 HSV 计算亲和优先角色列表
     */
    fun getPreferredCharacters(cardColorArgb: Int): List<DuffyCharacter> {
        val (h, s, _) = colorToHsv(cardColorArgb)
        return when {
            // 紫色 (Hue 240°..300°) -> 星黛露
            h in 240.0f..300.0f -> listOf(DuffyCharacter.STELLALOU)
            // 粉色 / 洋红 (Hue 300°..360° 或 0°..15° 且饱和度 > 0.12f) -> 玲娜贝儿、雪莉玫
            (h in 300.0f..360.0f || h in 0.0f..15.0f) && s > 0.12f -> listOf(DuffyCharacter.LINABELL, DuffyCharacter.SHELLIEMAY)
            // 黄色 / 暖金 (Hue 35°..65°) -> 可琦安
            h in 35.0f..65.0f -> listOf(DuffyCharacter.COOKIEANN)
            // 绿色 (Hue 80°..165°) -> 奥乐米拉
            h in 80.0f..165.0f -> listOf(DuffyCharacter.OLUMEL)
            // 薄荷绿 / 青色 (Hue 165°..205°) -> 杰拉多尼
            h in 165.0f..205.0f -> listOf(DuffyCharacter.GELATONI)
            // 棕色 / 暖大地色 / 其他默认 -> 达菲
            else -> listOf(DuffyCharacter.DUFFY)
        }
    }

    /**
     * 为日程卡片挑选贴纸玩偶（确定性算法，同一 eventId 在相同模式下结果恒定）
     * @param eventId 事件 ID 作为随机数种子
     * @param cardColorArgb 卡片背景色（用于颜色亲和度匹配）
     * @param cardHeightDp 卡片总高度（dp）
     * @param blankHeightDp 预估卡片下方空白可用高度（dp）
     * @param mode 玩偶分布模式（颜色亲和 / 均匀随机）
     * @return Pair(primaryDollAssetPath, secondaryDollAssetPath)
     */
    fun selectDollsForCard(
        eventId: Long,
        cardColorArgb: Int,
        cardHeightDp: Float,
        blankHeightDp: Float,
        mode: DollDistributionMode = DollDistributionMode.COLOR_AFFINITY
    ): Pair<String?, String?> {
        // 紧凑卡片 (< 44dp) 不贴玩偶
        if (cardHeightDp < 44f) {
            return Pair(null, null)
        }

        val preferred = if (mode == DollDistributionMode.COLOR_AFFINITY) {
            getPreferredCharacters(cardColorArgb)
        } else {
            emptyList()
        }

        val allCharacters = DuffyCharacter.entries
        val random = Random(eventId)

        // 1. 挑选主玩偶角色（加权或均匀随机）
        val primaryWeights = allCharacters.map { char ->
            if (mode == DollDistributionMode.COLOR_AFFINITY && char in preferred) 5 else 1
        }
        val totalPrimaryWeight = primaryWeights.sum()
        val primaryRoll = random.nextInt(totalPrimaryWeight)
        var accum = 0
        var primaryChar = allCharacters.first()
        for (i in allCharacters.indices) {
            accum += primaryWeights[i]
            if (primaryRoll < accum) {
                primaryChar = allCharacters[i]
                break
            }
        }

        val primaryAssets = allDolls[primaryChar] ?: emptyList()
        if (primaryAssets.isEmpty()) {
            return Pair(null, null)
        }
        val primaryAsset = primaryAssets[random.nextInt(primaryAssets.size)]

        // 2. 超大卡片且空白充裕 (>= 90dp 且 blank >= 50dp) 挑选次玩偶（从其余 6 个不同角色中选出）
        val secondaryAsset = if (cardHeightDp >= 90f && blankHeightDp >= 50f) {
            val remainingCharacters = allCharacters.filter { it != primaryChar }
            val secondaryWeights = remainingCharacters.map { char ->
                if (mode == DollDistributionMode.COLOR_AFFINITY && char in preferred) 5 else 1
            }
            val totalSecondaryWeight = secondaryWeights.sum()
            val secondaryRoll = random.nextInt(totalSecondaryWeight)
            var secAccum = 0
            var secChar = remainingCharacters.first()
            for (i in remainingCharacters.indices) {
                secAccum += secondaryWeights[i]
                if (secondaryRoll < secAccum) {
                    secChar = remainingCharacters[i]
                    break
                }
            }
            val secAssets = allDolls[secChar] ?: emptyList()
            if (secAssets.isNotEmpty()) {
                secAssets[random.nextInt(secAssets.size)]
            } else {
                null
            }
        } else {
            null
        }

        return Pair(primaryAsset, secondaryAsset)
    }

    /**
     * 采样加载玩偶 PNG 贴纸并缓存
     */
    fun loadDollBitmap(context: Context, assetPath: String, maxDimDp: Int = 120): Bitmap? {
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
