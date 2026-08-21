package com.cruise.assistant.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 设施与事件海报底图管理器：
 * 为各类餐厅、演出剧场、角色见面会等固定设施与事件预留底图加载与缓存机制。
 */
object PosterManager {

    private val cache = ConcurrentHashMap<String, Bitmap>()

    /**
     * 设施名称 -> 海报 asset 路径映射表（后续可在此持续扩充各餐厅/设施官方海报）
     */
    private val facilityPosterRegistry = mutableMapOf<String, String>(
        // 示例：预留常见固定餐厅与设施海报插槽
        // "Navigator's Club" to "posters/facilities/navigators_club.jpg",
        // "Animator's Palate" to "posters/facilities/animators_palate.jpg",
        // "Royal Court" to "posters/facilities/royal_court.jpg",
        // "Palo Trattoria" to "posters/facilities/palo.jpg",
    )

    /**
     * 事件标题 / presetKey -> 海报 asset 路径映射表（预留各演出与特色活动海报插槽）
     */
    private val eventPosterRegistry = mutableMapOf<String, String>(
        // 示例：预留常见演出剧目海报插槽
        // "moana" to "posters/events/moana.jpg",
        // "avengers" to "posters/events/avengers.jpg",
        // "disney_seas" to "posters/events/seas_the_adventure.jpg",
    )

    /**
     * 注册设施海报
     */
    fun registerFacilityPoster(placeName: String, assetPath: String) {
        facilityPosterRegistry[placeName.trim().lowercase()] = assetPath
    }

    /**
     * 注册事件海报
     */
    fun registerEventPoster(keyOrTitle: String, assetPath: String) {
        eventPosterRegistry[keyOrTitle.trim().lowercase()] = assetPath
    }

    /**
     * 加载设施对应的背景海报位图（若存在）
     */
    fun loadFacilityPoster(context: Context, placeName: String): Bitmap? {
        val trimmed = placeName.trim().lowercase()
        val path = facilityPosterRegistry[trimmed]
            ?: facilityPosterRegistry.entries.firstOrNull { trimmed.contains(it.key) }?.value
            ?: return null

        return loadFromAsset(context, path)
    }

    /**
     * 加载事件对应的背景海报位图（若存在）
     */
    fun loadEventPoster(context: Context, eventTitle: String, presetKey: String?): Bitmap? {
        val key = presetKey?.lowercase()
        val title = eventTitle.trim().lowercase()

        val path = (key?.let { eventPosterRegistry[it] })
            ?: eventPosterRegistry[title]
            ?: eventPosterRegistry.entries.firstOrNull { title.contains(it.key) }?.value
            ?: return null

        return loadFromAsset(context, path)
    }

    private fun loadFromAsset(context: Context, path: String): Bitmap? {
        cache[path]?.let { if (!it.isRecycled) return it }

        return try {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)?.also { bitmap ->
                    cache[path] = bitmap
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
