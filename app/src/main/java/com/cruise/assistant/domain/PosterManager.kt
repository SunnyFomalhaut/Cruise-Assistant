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
     * 设施名称 -> 海报 asset 路径映射表
     */
    private val facilityPosterRegistry = mutableMapOf<String, String>(
        // 1. 寻路者湾
        "寻路者湾" to "posters/facilities/wayfinder_bay.jpg",
        "wayfinder bay" to "posters/facilities/wayfinder_bay.jpg",
        "寻路人海滩" to "posters/facilities/wayfinder_bay.jpg",
        "wayfinder beach" to "posters/facilities/wayfinder_bay.jpg",
        "寻路人酒吧" to "posters/facilities/wayfinder_bay.jpg",
        "wayfinder bar" to "posters/facilities/wayfinder_bay.jpg",

        // 2. 花园舞台
        "花园舞台" to "posters/facilities/garden_stage.jpg",
        "garden stage" to "posters/facilities/garden_stage.jpg",
        "迪士尼探索庭园" to "posters/facilities/garden_stage.jpg",
        "imagination garden" to "posters/facilities/garden_stage.jpg",
        "花园酒吧" to "posters/facilities/garden_stage.jpg",
        "garden bar" to "posters/facilities/garden_stage.jpg",

        // 3. 玩具总动员区域（Sunnyside Pool, Wild Slides, Flying Saucer, Toy Story Splash Pad 等共用）
        "阳光泳池" to "posters/facilities/toy_story_place.jpg",
        "sunnyside" to "posters/facilities/toy_story_place.jpg",
        "狂野滑梯" to "posters/facilities/toy_story_place.jpg",
        "狂野滑道" to "posters/facilities/toy_story_place.jpg",
        "wild slides" to "posters/facilities/toy_story_place.jpg",
        "woody" to "posters/facilities/toy_story_place.jpg",
        "飞碟戏水区" to "posters/facilities/toy_story_place.jpg",
        "flying saucer" to "posters/facilities/toy_story_place.jpg",
        "玩具总动员戏水区" to "posters/facilities/toy_story_place.jpg",
        "toy story splash" to "posters/facilities/toy_story_place.jpg",
        "玩具总动员" to "posters/facilities/toy_story_place.jpg",
        "toy story" to "posters/facilities/toy_story_place.jpg",
        "威希冰品" to "posters/facilities/toy_story_place.jpg",
        "wheezy" to "posters/facilities/toy_story_place.jpg",

        // 4. 漫威区域（Groot Galaxy Spin, Pym Quantum Racers 等共用）
        "格鲁特" to "posters/facilities/marvel_landing.jpg",
        "groot" to "posters/facilities/marvel_landing.jpg",
        "皮姆量子赛车" to "posters/facilities/marvel_landing.jpg",
        "pym" to "posters/facilities/marvel_landing.jpg",
        "漫威造型工作室" to "posters/facilities/marvel_landing.jpg",
        "marvel" to "posters/facilities/marvel_landing.jpg",
        "漫威" to "posters/facilities/marvel_landing.jpg",

        // 5. 旧金山街道
        "旧金山街道" to "posters/facilities/san_fransokyo_street.jpg",
        "san fransokyo" to "posters/facilities/san_fransokyo_street.jpg",
        "大白剧院" to "posters/facilities/san_fransokyo_street.jpg",
        "baymax" to "posters/facilities/san_fransokyo_street.jpg",

        // 6. 披萨星球
        "披萨星球" to "posters/facilities/pizza_planet.jpg",
        "pizza planet" to "posters/facilities/pizza_planet.jpg",

        // 7. 航海家俱乐部
        "航海家俱乐部" to "posters/facilities/navigators_club.jpg",
        "领航者俱乐部" to "posters/facilities/navigators_club.jpg",
        "navigator's club" to "posters/facilities/navigators_club.jpg",
        "navigators club" to "posters/facilities/navigators_club.jpg",

        // 8. 好莱坞聚光灯俱乐部
        "好莱坞聚光灯俱乐部" to "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight" to "posters/facilities/hollywood_spotlight_club.jpg",

        // 9. 魔法盛夏餐厅
        "魔法盛夏餐厅" to "posters/facilities/enchanted_summer.jpg",
        "魔法夏日" to "posters/facilities/enchanted_summer.jpg",
        "enchanted summer" to "posters/facilities/enchanted_summer.jpg",

        // 10. 动画师餐厅 / 动画师味觉餐厅
        "动画师味觉餐厅" to "posters/facilities/animators_palate.jpg",
        "动画师餐厅" to "posters/facilities/animators_palate.jpg",
        "animator's palate" to "posters/facilities/animators_palate.jpg",
        "animator's table" to "posters/facilities/animators_palate.jpg",
        "animators palate" to "posters/facilities/animators_palate.jpg",
        "动画师" to "posters/facilities/animators_palate.jpg",
    )

    /**
     * 事件标题 / presetKey -> 海报 asset 路径映射表
     */
    private val eventPosterRegistry = mutableMapOf<String, String>(
        "moana" to "posters/facilities/wayfinder_bay.jpg",
        "海洋奇缘" to "posters/facilities/wayfinder_bay.jpg",
        "avengers" to "posters/facilities/marvel_landing.jpg",
        "复仇者" to "posters/facilities/marvel_landing.jpg",
        "漫威" to "posters/facilities/marvel_landing.jpg",
        "baymax" to "posters/facilities/san_fransokyo_street.jpg",
        "大白" to "posters/facilities/san_fransokyo_street.jpg",
        "航海家" to "posters/facilities/navigators_club.jpg",
        "navigator" to "posters/facilities/navigators_club.jpg",
        "动画师" to "posters/facilities/animators_palate.jpg",
        "animator" to "posters/facilities/animators_palate.jpg",
        "魔法盛夏" to "posters/facilities/enchanted_summer.jpg",
        "enchanted summer" to "posters/facilities/enchanted_summer.jpg",
        "好莱坞聚光灯" to "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight" to "posters/facilities/hollywood_spotlight_club.jpg",
        "花园舞台" to "posters/facilities/garden_stage.jpg",
        "garden stage" to "posters/facilities/garden_stage.jpg",
        "玩具总动员" to "posters/facilities/toy_story_place.jpg",
        "toy story" to "posters/facilities/toy_story_place.jpg",
        "披萨星球" to "posters/facilities/pizza_planet.jpg",
        "pizza planet" to "posters/facilities/pizza_planet.jpg",
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
