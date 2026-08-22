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
     * 8 部演出专属海报 asset 路径映射表（presetKey / 标题 / 核心关键词）
     */
    private val showPosterRegistry = mapOf(
        "show_set_sail" to "posters/shows/show_set_sail.jpg",
        "show_lion_king" to "posters/shows/show_lion_king.jpg",
        "show_seas_adventure" to "posters/shows/show_seas_adventure.jpg",
        "show_remember" to "posters/shows/show_remember.jpg",
        "show_moana" to "posters/shows/show_moana.jpg",
        "show_avengers" to "posters/shows/show_avengers.jpg",
        "show_baymax_expo" to "posters/shows/show_baymax_expo.jpg",
        "show_duffy_friend_ship" to "posters/shows/show_duffy_friend_ship.jpg",
    )

    private val showKeywordRegistry = listOf(
        // 1. 扬帆起航
        listOf("show_set_sail", "扬帆起航", "let's set sail", "lets set sail", "set sail") to "posters/shows/show_set_sail.jpg",
        // 2. 狮子王烟花
        listOf("show_lion_king", "狮子王", "烟花", "celebration in the sky", "lion king") to "posters/shows/show_lion_king.jpg",
        // 3. 海洋奇遇
        listOf("show_seas_adventure", "海洋奇遇", "seas the adventure", "disney seas") to "posters/shows/show_seas_adventure.jpg",
        // 4. 回忆：瓦力与伊芙
        listOf("show_remember", "回忆", "瓦力", "伊芙", "remember") to "posters/shows/show_remember.jpg",
        // 5. 海洋奇缘
        listOf("show_moana", "海洋奇缘", "moana", "call of the sea") to "posters/shows/show_moana.jpg",
        // 6. 复仇者集结
        listOf("show_avengers", "复仇者", "avengers") to "posters/shows/show_avengers.jpg",
        // 7. 大白超级健身世博会
        listOf("show_baymax_expo", "大白", "健身", "baymax", "exercise expo") to "posters/shows/show_baymax_expo.jpg",
        // 8. 达菲与友谊之船
        listOf("show_duffy_friend_ship", "达菲", "友谊之船", "duffy", "friend ship") to "posters/shows/show_duffy_friend_ship.jpg",
    )

    /**
     * 设施名称 -> 海报 asset 路径映射表
     */
    private val facilityPosterRegistry = mutableMapOf(
        // 1. 寻路者湾
        "寻路者湾" to "posters/facilities/wayfinder_bay.jpg",
        "wayfinder bay" to "posters/facilities/wayfinder_bay.jpg",
        "寻路人海滩" to "posters/facilities/wayfinder_bay.jpg",
        "wayfinder beach" to "posters/facilities/wayfinder_bay.jpg",

        // 华特迪士尼剧院
        "华特迪士尼剧院" to "posters/facilities/walt_disney_theatre.jpg",
        "walt disney theatre" to "posters/facilities/walt_disney_theatre.jpg",
        "walt disney theater" to "posters/facilities/walt_disney_theatre.jpg",

        // 无边际泳池（严格区分于无边际酒吧）
        "无边际泳池" to "posters/facilities/infinity_pool.jpg",
        "infinity pool" to "posters/facilities/infinity_pool.jpg",

        // 水疗与礼宾服务
        "无限幸福水疗" to "posters/facilities/infinite_bliss_spa.jpg",
        "infinite bliss spa" to "posters/facilities/infinite_bliss_spa.jpg",
        "infinite bliss" to "posters/facilities/infinite_bliss_spa.jpg",
        "奢华水疗" to "posters/facilities/opulence_spa.jpg",
        "opulence spa" to "posters/facilities/opulence_spa.jpg",
        "礼宾休息室" to "posters/facilities/concierge_lounge.jpg",
        "礼宾户外休息室" to "posters/facilities/concierge_lounge.jpg",
        "concierge lounge" to "posters/facilities/concierge_lounge.jpg",
        "concierge lounge outdoor" to "posters/facilities/concierge_lounge.jpg",
        "礼宾阳光甲板" to "posters/facilities/concierge_sundeck.jpg",
        "concierge sundeck" to "posters/facilities/concierge_sundeck.jpg",

        // 2. 花园舞台
        "花园舞台" to "posters/facilities/garden_stage.jpg",
        "garden stage" to "posters/facilities/garden_stage.jpg",
        "迪士尼探索庭园" to "posters/facilities/garden_stage.jpg",
        "imagination garden" to "posters/facilities/garden_stage.jpg",

        // 3. 玩具总动员区域（Sunnyside Pool, Wild Slides, Flying Saucer, Toy Story Splash Pad 等共用）
        "阳光泳池" to "posters/facilities/toy_story_place.jpg",
        "sunnyside pool" to "posters/facilities/toy_story_place.jpg",
        "sunnyside" to "posters/facilities/toy_story_place.jpg",
        "狂野滑梯" to "posters/facilities/toy_story_place.jpg",
        "狂野滑道" to "posters/facilities/toy_story_place.jpg",
        "wild slides" to "posters/facilities/toy_story_place.jpg",
        "飞碟戏水区" to "posters/facilities/toy_story_place.jpg",
        "flying saucer" to "posters/facilities/toy_story_place.jpg",
        "玩具总动员戏水区" to "posters/facilities/toy_story_place.jpg",
        "toy story splash" to "posters/facilities/toy_story_place.jpg",
        "玩具总动员" to "posters/facilities/toy_story_place.jpg",
        "toy story" to "posters/facilities/toy_story_place.jpg",
        "威希冰品" to "posters/facilities/toy_story_place.jpg",
        "wheezy" to "posters/facilities/toy_story_place.jpg",

        // 4. 漫威区域（Groot Galaxy Spin, Pym Quantum Racers 等设施共用，不含商店）
        "格鲁特银河旋风" to "posters/facilities/marvel_landing.jpg",
        "groot galaxy spin" to "posters/facilities/marvel_landing.jpg",
        "格鲁特" to "posters/facilities/marvel_landing.jpg",
        "皮姆量子赛车" to "posters/facilities/marvel_landing.jpg",
        "pym quantum racers" to "posters/facilities/marvel_landing.jpg",
        "漫威着陆区" to "posters/facilities/marvel_landing.jpg",
        "marvel landing" to "posters/facilities/marvel_landing.jpg",

        // 5. 旧金山街道
        "旧金山街道" to "posters/facilities/san_fransokyo_street.jpg",
        "san fransokyo street" to "posters/facilities/san_fransokyo_street.jpg",
        "大白剧院" to "posters/facilities/san_fransokyo_street.jpg",
        "baymax theatre" to "posters/facilities/san_fransokyo_street.jpg",

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
        "好莱坞聚光灯" to "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight club" to "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight" to "posters/facilities/hollywood_spotlight_club.jpg",

        // 9. 魔法盛夏餐厅
        "魔法盛夏餐厅" to "posters/facilities/enchanted_summer.jpg",
        "魔法盛夏" to "posters/facilities/enchanted_summer.jpg",
        "魔法夏日" to "posters/facilities/enchanted_summer.jpg",
        "enchanted summer" to "posters/facilities/enchanted_summer.jpg",

        // 10. 动画师餐厅 / 动画师味觉餐厅
        "动画师味觉餐厅" to "posters/facilities/animators_palate.jpg",
        "动画师餐厅" to "posters/facilities/animators_palate.jpg",
        "animator's palate" to "posters/facilities/animators_palate.jpg",
        "animator's table" to "posters/facilities/animators_palate.jpg",
        "animators palate" to "posters/facilities/animators_palate.jpg",

        // 11-24. 14 处商店专属海报映射
        "三个愿望" to "posters/shops/3_wishes.jpg",
        "3 wishes" to "posters/shops/3_wishes.jpg",
        "魔法变身精品店" to "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "bibbidi bobbidi boutique" to "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "bibbidi bobbidi" to "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "城堡典藏" to "posters/shops/castle_collection.jpg",
        "castle collection" to "posters/shops/castle_collection.jpg",
        "钻石与愿望" to "posters/shops/diamonds_and_wishes.jpg",
        "diamonds and wishes" to "posters/shops/diamonds_and_wishes.jpg",
        "迪士尼影楼" to "posters/shops/disney_royal_studio.jpg",
        "disney studio" to "posters/shops/disney_royal_studio.jpg",
        "皇家影楼" to "posters/shops/disney_royal_studio.jpg",
        "royal studio" to "posters/shops/disney_royal_studio.jpg",
        "达菲与朋友商店" to "posters/shops/duffy_and_friends_shop.jpg",
        "duffy and friends shop" to "posters/shops/duffy_and_friends_shop.jpg",
        "达菲商店" to "posters/shops/duffy_and_friends_shop.jpg",
        "漫威造型工作室" to "posters/shops/marvel_style_studio.jpg",
        "marvel style studio" to "posters/shops/marvel_style_studio.jpg",
        "国家地理" to "posters/shops/national_geographic.jpg",
        "national geographic" to "posters/shops/national_geographic.jpg",
        "宫廷珍宝" to "posters/shops/palace_treasures.jpg",
        "palace treasures" to "posters/shops/palace_treasures.jpg",
        "快照摄影店" to "posters/shops/pics_photo_shop.jpg",
        "pics photo shop" to "posters/shops/pics_photo_shop.jpg",
        "未诉珍宝" to "posters/shops/treasures_untold.jpg",
        "treasures untold" to "posters/shops/treasures_untold.jpg",
        "迪士尼世界二号店" to "posters/shops/world_of_disney_too.jpg",
        "world of disney too" to "posters/shops/world_of_disney_too.jpg",
        "迪士尼世界" to "posters/shops/world_of_disney.jpg",
        "world of disney" to "posters/shops/world_of_disney.jpg",

        // 25-46. 22 处新增餐饮与客房送餐专属海报映射
        "流浪猫咖啡馆" to "posters/dining/alley_cat_cafe.jpg",
        "alley cat cafe" to "posters/dining/alley_cat_cafe.jpg",
        "alley cat café" to "posters/dining/alley_cat_cafe.jpg",
        "巴哈咖啡" to "posters/dining/bacha_coffee.jpg",
        "bacha coffee" to "posters/dining/bacha_coffee.jpg",
        "迷魂珍珠奶茶" to "posters/dining/bewitching_boba_and_brews.jpg",
        "bewitching boba & brews" to "posters/dining/bewitching_boba_and_brews.jpg",
        "bewitching boba and brews" to "posters/dining/bewitching_boba_and_brews.jpg",
        "海盗酒吧" to "posters/dining/buccaneer_bar.jpg",
        "buccaneer bar" to "posters/dining/buccaneer_bar.jpg",
        "宇宙烤肉串" to "posters/dining/cosmic_kebabs.jpg",
        "cosmic kebabs" to "posters/dining/cosmic_kebabs.jpg",
        "花园酒吧" to "posters/dining/garden_bar.jpg",
        "garden bar" to "posters/dining/garden_bar.jpg",
        "塔拉奶奶的厨房" to "posters/dining/gramma_talas_kitchen.jpg",
        "gramma talas kitchen" to "posters/dining/gramma_talas_kitchen.jpg",
        "gramma tala's kitchen" to "posters/dining/gramma_talas_kitchen.jpg",
        "无边际酒吧" to "posters/dining/infinity_bar.jpg",
        "infinity bar" to "posters/dining/infinity_bar.jpg",
        "市场酒吧" to "posters/dining/market_bar.jpg",
        "market bar" to "posters/dining/market_bar.jpg",
        "怪兽亚洲风味" to "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "mike & sulley's flavors of asia" to "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "mike and sulleys flavors of asia" to "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "莫格利餐厅" to "posters/dining/mowglis_eatery.jpg",
        "mowgli's eatery" to "posters/dining/mowglis_eatery.jpg",
        "mowglis eatery" to "posters/dining/mowglis_eatery.jpg",
        "帕洛咖啡" to "posters/dining/palo_cafe.jpg",
        "palo café" to "posters/dining/palo_cafe.jpg",
        "palo cafe" to "posters/dining/palo_cafe.jpg",
        "帕洛意大利餐厅" to "posters/dining/palo_trattoria.jpg",
        "palo trattoria" to "posters/dining/palo_trattoria.jpg",
        "皮克斯市场餐厅" to "posters/dining/pixar_market_restaurant.jpg",
        "pixar market restaurant" to "posters/dining/pixar_market_restaurant.jpg",
        "客房送餐" to "posters/dining/room_service.jpg",
        "room service" to "posters/dining/room_service.jpg",
        "咒语酒廊" to "posters/dining/spellbound.jpg",
        "spellbound" to "posters/dining/spellbound.jpg",
        "史迪奇奥哈纳烧烤" to "posters/dining/stitchs_ohana_grill.jpg",
        "stitch's ohana grill" to "posters/dining/stitchs_ohana_grill.jpg",
        "stitchs ohana grill" to "posters/dining/stitchs_ohana_grill.jpg",
        "波尔托罗索小酒馆" to "posters/dining/taverna_portorosso.jpg",
        "taverna portorosso" to "posters/dining/taverna_portorosso.jpg",
        "蒂安娜河口酒廊" to "posters/dining/tianas_bayou_lounge.jpg",
        "tiana's bayou lounge" to "posters/dining/tianas_bayou_lounge.jpg",
        "tianas bayou lounge" to "posters/dining/tianas_bayou_lounge.jpg",
        "twg 茶室" to "posters/dining/twg_tea.jpg",
        "twg tea" to "posters/dining/twg_tea.jpg",
        "twg" to "posters/dining/twg_tea.jpg",
        "寻路人酒吧" to "posters/dining/wayfinder_bar.jpg",
        "wayfinder bar" to "posters/dining/wayfinder_bar.jpg",
        "威希冰品" to "posters/dining/wheezys_freezies.jpg",
        "wheezy's freezies" to "posters/dining/wheezys_freezies.jpg",
        "wheezys freezies" to "posters/dining/wheezys_freezies.jpg",

        // 47-51. 5 处青少年与儿童活动空间专属海报映射
        "小小世界托儿所" to "posters/youth/its_a_small_world_nursery.jpg",
        "it's a small world nursery" to "posters/youth/its_a_small_world_nursery.jpg",
        "its a small world nursery" to "posters/youth/its_a_small_world_nursery.jpg",
        "米奇米妮船长甲板" to "posters/youth/mickey_minnie_captains_deck.jpg",
        "mickey & minnie captain's deck" to "posters/youth/mickey_minnie_captains_deck.jpg",
        "mickey and minnie captain's deck" to "posters/youth/mickey_minnie_captains_deck.jpg",
        "captain's deck" to "posters/youth/mickey_minnie_captains_deck.jpg",
        "迪士尼海洋探险家俱乐部" to "posters/youth/disneys_oceaneer_club.jpg",
        "海洋探险家俱乐部" to "posters/youth/disneys_oceaneer_club.jpg",
        "disney's oceaneer club" to "posters/youth/disneys_oceaneer_club.jpg",
        "disneys oceaneer club" to "posters/youth/disneys_oceaneer_club.jpg",
        "oceaneer club" to "posters/youth/disneys_oceaneer_club.jpg",
        "边缘少年俱乐部" to "posters/youth/edge.jpg",
        "edge" to "posters/youth/edge.jpg",
        "活力少年俱乐部" to "posters/youth/vibe.jpg",
        "vibe" to "posters/youth/vibe.jpg",
    )

    /**
     * 其他非演出类事件标题 / 俱乐部等映射
     */
    private val eventPosterRegistry = mutableMapOf(
        "航海家" to "posters/facilities/navigators_club.jpg",
        "navigator" to "posters/facilities/navigators_club.jpg",
        "动画师" to "posters/facilities/animators_palate.jpg",
        "animator" to "posters/facilities/animators_palate.jpg",
        "魔法盛夏" to "posters/facilities/enchanted_summer.jpg",
        "enchanted summer" to "posters/facilities/enchanted_summer.jpg",
        "好莱坞聚光灯" to "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight" to "posters/facilities/hollywood_spotlight_club.jpg",
        "披萨星球" to "posters/facilities/pizza_planet.jpg",
        "pizza planet" to "posters/facilities/pizza_planet.jpg",

        // 商店
        "三个愿望" to "posters/shops/3_wishes.jpg",
        "3 wishes" to "posters/shops/3_wishes.jpg",
        "魔法变身精品店" to "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "bibbidi bobbidi" to "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "城堡典藏" to "posters/shops/castle_collection.jpg",
        "castle collection" to "posters/shops/castle_collection.jpg",
        "钻石与愿望" to "posters/shops/diamonds_and_wishes.jpg",
        "diamonds and wishes" to "posters/shops/diamonds_and_wishes.jpg",
        "迪士尼影楼" to "posters/shops/disney_royal_studio.jpg",
        "disney studio" to "posters/shops/disney_royal_studio.jpg",
        "皇家影楼" to "posters/shops/disney_royal_studio.jpg",
        "royal studio" to "posters/shops/disney_royal_studio.jpg",
        "达菲与朋友商店" to "posters/shops/duffy_and_friends_shop.jpg",
        "duffy and friends shop" to "posters/shops/duffy_and_friends_shop.jpg",
        "漫威造型工作室" to "posters/shops/marvel_style_studio.jpg",
        "marvel style studio" to "posters/shops/marvel_style_studio.jpg",
        "国家地理" to "posters/shops/national_geographic.jpg",
        "national geographic" to "posters/shops/national_geographic.jpg",
        "宫廷珍宝" to "posters/shops/palace_treasures.jpg",
        "palace treasures" to "posters/shops/palace_treasures.jpg",
        "快照摄影店" to "posters/shops/pics_photo_shop.jpg",
        "pics photo shop" to "posters/shops/pics_photo_shop.jpg",
        "未诉珍宝" to "posters/shops/treasures_untold.jpg",
        "treasures untold" to "posters/shops/treasures_untold.jpg",
        "迪士尼世界二号店" to "posters/shops/world_of_disney_too.jpg",
        "world of disney too" to "posters/shops/world_of_disney_too.jpg",
        "迪士尼世界" to "posters/shops/world_of_disney.jpg",
        "world of disney" to "posters/shops/world_of_disney.jpg",

        // 餐饮
        "流浪猫咖啡馆" to "posters/dining/alley_cat_cafe.jpg",
        "alley cat cafe" to "posters/dining/alley_cat_cafe.jpg",
        "巴哈咖啡" to "posters/dining/bacha_coffee.jpg",
        "bacha coffee" to "posters/dining/bacha_coffee.jpg",
        "迷魂珍珠奶茶" to "posters/dining/bewitching_boba_and_brews.jpg",
        "bewitching boba" to "posters/dining/bewitching_boba_and_brews.jpg",
        "海盗酒吧" to "posters/dining/buccaneer_bar.jpg",
        "buccaneer bar" to "posters/dining/buccaneer_bar.jpg",
        "宇宙烤肉串" to "posters/dining/cosmic_kebabs.jpg",
        "cosmic kebabs" to "posters/dining/cosmic_kebabs.jpg",
        "花园酒吧" to "posters/dining/garden_bar.jpg",
        "garden bar" to "posters/dining/garden_bar.jpg",
        "塔拉奶奶的厨房" to "posters/dining/gramma_talas_kitchen.jpg",
        "gramma tala" to "posters/dining/gramma_talas_kitchen.jpg",
        "无边际酒吧" to "posters/dining/infinity_bar.jpg",
        "infinity bar" to "posters/dining/infinity_bar.jpg",
        "市场酒吧" to "posters/dining/market_bar.jpg",
        "market bar" to "posters/dining/market_bar.jpg",
        "怪兽亚洲风味" to "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "mike & sulley" to "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "莫格利餐厅" to "posters/dining/mowglis_eatery.jpg",
        "mowgli" to "posters/dining/mowglis_eatery.jpg",
        "帕洛咖啡" to "posters/dining/palo_cafe.jpg",
        "palo cafe" to "posters/dining/palo_cafe.jpg",
        "帕洛意大利餐厅" to "posters/dining/palo_trattoria.jpg",
        "palo trattoria" to "posters/dining/palo_trattoria.jpg",
        "皮克斯市场餐厅" to "posters/dining/pixar_market_restaurant.jpg",
        "pixar market" to "posters/dining/pixar_market_restaurant.jpg",
        "客房送餐" to "posters/dining/room_service.jpg",
        "room service" to "posters/dining/room_service.jpg",
        "咒语酒廊" to "posters/dining/spellbound.jpg",
        "spellbound" to "posters/dining/spellbound.jpg",
        "史迪奇奥哈纳烧烤" to "posters/dining/stitchs_ohana_grill.jpg",
        "stitch's ohana grill" to "posters/dining/stitchs_ohana_grill.jpg",
        "波尔托罗索小酒馆" to "posters/dining/taverna_portorosso.jpg",
        "taverna portorosso" to "posters/dining/taverna_portorosso.jpg",
        "蒂安娜河口酒廊" to "posters/dining/tianas_bayou_lounge.jpg",
        "tiana's bayou lounge" to "posters/dining/tianas_bayou_lounge.jpg",
        "twg 茶室" to "posters/dining/twg_tea.jpg",
        "twg tea" to "posters/dining/twg_tea.jpg",
        "寻路人酒吧" to "posters/dining/wayfinder_bar.jpg",
        "wayfinder bar" to "posters/dining/wayfinder_bar.jpg",
        "威希冰品" to "posters/dining/wheezys_freezies.jpg",
        "wheezy's freezies" to "posters/dining/wheezys_freezies.jpg",

        // 青少年
        "小小世界托儿所" to "posters/youth/its_a_small_world_nursery.jpg",
        "it's a small world nursery" to "posters/youth/its_a_small_world_nursery.jpg",
        "米奇米妮船长甲板" to "posters/youth/mickey_minnie_captains_deck.jpg",
        "mickey & minnie captain's deck" to "posters/youth/mickey_minnie_captains_deck.jpg",
        "迪士尼海洋探险家俱乐部" to "posters/youth/disneys_oceaneer_club.jpg",
        "海洋探险家俱乐部" to "posters/youth/disneys_oceaneer_club.jpg",
        "disney's oceaneer club" to "posters/youth/disneys_oceaneer_club.jpg",
        "边缘少年俱乐部" to "posters/youth/edge.jpg",
        "edge" to "posters/youth/edge.jpg",
        "活力少年俱乐部" to "posters/youth/vibe.jpg",
        "vibe" to "posters/youth/vibe.jpg",

        // 水疗与礼宾
        "无限幸福水疗" to "posters/facilities/infinite_bliss_spa.jpg",
        "infinite bliss spa" to "posters/facilities/infinite_bliss_spa.jpg",
        "奢华水疗" to "posters/facilities/opulence_spa.jpg",
        "opulence spa" to "posters/facilities/opulence_spa.jpg",
        "礼宾休息室" to "posters/facilities/concierge_lounge.jpg",
        "礼宾户外休息室" to "posters/facilities/concierge_lounge.jpg",
        "concierge lounge" to "posters/facilities/concierge_lounge.jpg",
        "礼宾阳光甲板" to "posters/facilities/concierge_sundeck.jpg",
        "concierge sundeck" to "posters/facilities/concierge_sundeck.jpg",
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
     * 获取事件海报的 asset 相对路径
     */
    fun getEventPosterPath(eventTitle: String, presetKey: String?): String? {
        val key = presetKey?.trim()?.lowercase()
        val title = eventTitle.trim().lowercase()

        // 1. PresetKey 精确匹配演出海报
        if (key != null) {
            showPosterRegistry[key]?.let { return it }
        }

        // 2. 演出关键词/标题匹配
        for ((keywords, path) in showKeywordRegistry) {
            if (keywords.any { kw -> title.contains(kw) || (key != null && key.contains(kw)) }) {
                return path
            }
        }

        // 3. 其他非演出事件自定义注册表
        if (key != null && eventPosterRegistry.containsKey(key)) {
            return eventPosterRegistry[key]
        }
        val customEventMatch = eventPosterRegistry[title]
            ?: eventPosterRegistry.entries.firstOrNull { title.contains(it.key) }?.value
        if (customEventMatch != null) {
            return customEventMatch
        }

        return null
    }

    /**
     * 获取设施海报的 asset 相对路径
     */
    fun getFacilityPosterPath(placeName: String): String? {
        val trimmed = placeName.trim().lowercase()
        return facilityPosterRegistry[trimmed]
            ?: facilityPosterRegistry.entries.firstOrNull { trimmed.contains(it.key) }?.value
    }

    /**
     * 加载设施对应的背景海报位图（若存在）
     */
    fun loadFacilityPoster(context: Context, placeName: String): Bitmap? {
        val path = getFacilityPosterPath(placeName) ?: return null
        return loadFromAsset(context, path)
    }

    /**
     * 加载事件对应的背景海报位图（若存在）
     */
    fun loadEventPoster(context: Context, eventTitle: String, presetKey: String?): Bitmap? {
        val path = getEventPosterPath(eventTitle, presetKey) ?: return null
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
