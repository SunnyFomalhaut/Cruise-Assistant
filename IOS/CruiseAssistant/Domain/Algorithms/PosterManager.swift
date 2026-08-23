import Foundation

/// 设施与事件海报底图管理器：
/// 为各类餐厅、演出剧场、角色见面会等固定设施与事件预留底图加载与路径查找机制。
public final class PosterManager: @unchecked Sendable {
    public static let shared = PosterManager()

    private let lock = NSLock()

    /// 8 部演出专属海报 asset 路径映射表（presetKey / 标题 / 核心关键词）
    private let showPosterRegistry: [String: String] = [
        "show_set_sail": "posters/shows/show_set_sail.jpg",
        "show_lion_king": "posters/shows/show_lion_king.jpg",
        "show_seas_adventure": "posters/shows/show_seas_adventure.jpg",
        "show_remember": "posters/shows/show_remember.jpg",
        "show_moana": "posters/shows/show_moana.jpg",
        "show_avengers": "posters/shows/show_avengers.jpg",
        "show_baymax_expo": "posters/shows/show_baymax_expo.jpg",
        "show_duffy_friend_ship": "posters/shows/show_duffy_friend_ship.jpg",
    ]

    private let showKeywordRegistry: [([String], String)] = [
        // 1. 扬帆起航
        (["show_set_sail", "扬帆起航", "let's set sail", "lets set sail", "set sail"], "posters/shows/show_set_sail.jpg"),
        // 2. 狮子王烟花
        (["show_lion_king", "狮子王", "烟花", "celebration in the sky", "lion king"], "posters/shows/show_lion_king.jpg"),
        // 3. 海洋奇遇
        (["show_seas_adventure", "海洋奇遇", "seas the adventure", "disney seas"], "posters/shows/show_seas_adventure.jpg"),
        // 4. 回忆：瓦力与伊芙
        (["show_remember", "回忆", "瓦力", "伊芙", "remember"], "posters/shows/show_remember.jpg"),
        // 5. 海洋奇缘
        (["show_moana", "海洋奇缘", "moana", "call of the sea"], "posters/shows/show_moana.jpg"),
        // 6. 复仇者集结
        (["show_avengers", "复仇者", "avengers"], "posters/shows/show_avengers.jpg"),
        // 7. 大白超级健身世博会
        (["show_baymax_expo", "大白", "健身", "baymax", "exercise expo"], "posters/shows/show_baymax_expo.jpg"),
        // 8. 达菲与友谊之船
        (["show_duffy_friend_ship", "达菲", "友谊之船", "duffy", "friend ship"], "posters/shows/show_duffy_friend_ship.jpg"),
    ]

    /// 设施名称 -> 海报 asset 路径映射表
    private var facilityPosterRegistry: [String: String] = [
        // 1. 寻路者湾
        "寻路者湾": "posters/facilities/wayfinder_bay.jpg",
        "wayfinder bay": "posters/facilities/wayfinder_bay.jpg",
        "寻路人海滩": "posters/facilities/wayfinder_bay.jpg",
        "wayfinder beach": "posters/facilities/wayfinder_bay.jpg",

        // 华特迪士尼剧院
        "华特迪士尼剧院": "posters/facilities/walt_disney_theatre.jpg",
        "walt disney theatre": "posters/facilities/walt_disney_theatre.jpg",
        "walt disney theater": "posters/facilities/walt_disney_theatre.jpg",

        // 无边际泳池（严格区分于无边际酒吧）
        "无边际泳池": "posters/facilities/infinity_pool.jpg",
        "infinity pool": "posters/facilities/infinity_pool.jpg",

        // 水疗与礼宾服务
        "无限幸福水疗": "posters/facilities/infinite_bliss_spa.jpg",
        "infinite bliss spa": "posters/facilities/infinite_bliss_spa.jpg",
        "infinite bliss": "posters/facilities/infinite_bliss_spa.jpg",
        "奢华水疗": "posters/facilities/opulence_spa.jpg",
        "opulence spa": "posters/facilities/opulence_spa.jpg",
        "礼宾休息室": "posters/facilities/concierge_lounge.jpg",
        "礼宾户外休息室": "posters/facilities/concierge_lounge.jpg",
        "concierge lounge": "posters/facilities/concierge_lounge.jpg",
        "concierge lounge outdoor": "posters/facilities/concierge_lounge.jpg",
        "礼宾阳光甲板": "posters/facilities/concierge_sundeck.jpg",
        "concierge sundeck": "posters/facilities/concierge_sundeck.jpg",

        // 2. 花园舞台
        "花园舞台": "posters/facilities/garden_stage.jpg",
        "garden stage": "posters/facilities/garden_stage.jpg",
        "迪士尼探索庭园": "posters/facilities/garden_stage.jpg",
        "imagination garden": "posters/facilities/garden_stage.jpg",

        // 3. 玩具总动员区域
        "阳光泳池": "posters/facilities/toy_story_place.jpg",
        "sunnyside pool": "posters/facilities/toy_story_place.jpg",
        "sunnyside": "posters/facilities/toy_story_place.jpg",
        "狂野滑梯": "posters/facilities/toy_story_place.jpg",
        "狂野滑道": "posters/facilities/toy_story_place.jpg",
        "wild slides": "posters/facilities/toy_story_place.jpg",
        "飞碟戏水区": "posters/facilities/toy_story_place.jpg",
        "flying saucer": "posters/facilities/toy_story_place.jpg",
        "玩具总动员戏水区": "posters/facilities/toy_story_place.jpg",
        "toy story splash": "posters/facilities/toy_story_place.jpg",
        "玩具总动员": "posters/facilities/toy_story_place.jpg",
        "toy story": "posters/facilities/toy_story_place.jpg",
        "威希冰品": "posters/dining/wheezys_freezies.jpg",
        "wheezy": "posters/facilities/toy_story_place.jpg",

        // 4. 漫威区域
        "格鲁特银河旋风": "posters/facilities/marvel_landing.jpg",
        "groot galaxy spin": "posters/facilities/marvel_landing.jpg",
        "格鲁特": "posters/facilities/marvel_landing.jpg",
        "皮姆量子赛车": "posters/facilities/marvel_landing.jpg",
        "pym quantum racers": "posters/facilities/marvel_landing.jpg",
        "漫威着陆区": "posters/facilities/marvel_landing.jpg",
        "marvel landing": "posters/facilities/marvel_landing.jpg",

        // 5. 旧金山街道
        "旧金山街道": "posters/facilities/san_fransokyo_street.jpg",
        "san fransokyo street": "posters/facilities/san_fransokyo_street.jpg",
        "大白剧院": "posters/facilities/san_fransokyo_street.jpg",
        "baymax theatre": "posters/facilities/san_fransokyo_street.jpg",

        // 6. 披萨星球
        "披萨星球": "posters/facilities/pizza_planet.jpg",
        "pizza planet": "posters/facilities/pizza_planet.jpg",

        // 7. 航海家俱乐部
        "航海家俱乐部": "posters/facilities/navigators_club.jpg",
        "领航者俱乐部": "posters/facilities/navigators_club.jpg",
        "navigator's club": "posters/facilities/navigators_club.jpg",
        "navigators club": "posters/facilities/navigators_club.jpg",

        // 8. 好莱坞聚光灯俱乐部
        "好莱坞聚光灯俱乐部": "posters/facilities/hollywood_spotlight_club.jpg",
        "好莱坞聚光灯": "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight club": "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight": "posters/facilities/hollywood_spotlight_club.jpg",

        // 9. 魔法盛夏餐厅
        "魔法盛夏餐厅": "posters/facilities/enchanted_summer.jpg",
        "魔法盛夏": "posters/facilities/enchanted_summer.jpg",
        "魔法夏日": "posters/facilities/enchanted_summer.jpg",
        "enchanted summer": "posters/facilities/enchanted_summer.jpg",

        // 10. 动画师餐厅 / 动画师味觉餐厅
        "动画师味觉餐厅": "posters/facilities/animators_palate.jpg",
        "动画师餐厅": "posters/facilities/animators_palate.jpg",
        "animator's palate": "posters/facilities/animators_palate.jpg",
        "animator's table": "posters/facilities/animators_palate.jpg",
        "animators palate": "posters/facilities/animators_palate.jpg",

        // 11-24. 商店专属海报映射
        "三个愿望": "posters/shops/3_wishes.jpg",
        "3 wishes": "posters/shops/3_wishes.jpg",
        "魔法变身精品店": "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "bibbidi bobbidi boutique": "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "bibbidi bobbidi": "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "城堡典藏": "posters/shops/castle_collection.jpg",
        "castle collection": "posters/shops/castle_collection.jpg",
        "钻石与愿望": "posters/shops/diamonds_and_wishes.jpg",
        "diamonds and wishes": "posters/shops/diamonds_and_wishes.jpg",
        "迪士尼影楼": "posters/shops/disney_royal_studio.jpg",
        "disney studio": "posters/shops/disney_royal_studio.jpg",
        "皇家影楼": "posters/shops/disney_royal_studio.jpg",
        "royal studio": "posters/shops/disney_royal_studio.jpg",
        "达菲与朋友商店": "posters/shops/duffy_and_friends_shop.jpg",
        "duffy and friends shop": "posters/shops/duffy_and_friends_shop.jpg",
        "达菲商店": "posters/shops/duffy_and_friends_shop.jpg",
        "漫威造型工作室": "posters/shops/marvel_style_studio.jpg",
        "marvel style studio": "posters/shops/marvel_style_studio.jpg",
        "国家地理": "posters/shops/national_geographic.jpg",
        "national geographic": "posters/shops/national_geographic.jpg",
        "宫廷珍宝": "posters/shops/palace_treasures.jpg",
        "palace treasures": "posters/shops/palace_treasures.jpg",
        "快照摄影店": "posters/shops/pics_photo_shop.jpg",
        "pics photo shop": "posters/shops/pics_photo_shop.jpg",
        "未诉珍宝": "posters/shops/treasures_untold.jpg",
        "treasures untold": "posters/shops/treasures_untold.jpg",
        "迪士尼世界二号店": "posters/shops/world_of_disney_too.jpg",
        "world of disney too": "posters/shops/world_of_disney_too.jpg",
        "迪士尼世界": "posters/shops/world_of_disney.jpg",
        "world of disney": "posters/shops/world_of_disney.jpg",

        // 25-46. 餐饮与客房送餐专属海报映射
        "流浪猫咖啡馆": "posters/dining/alley_cat_cafe.jpg",
        "alley cat cafe": "posters/dining/alley_cat_cafe.jpg",
        "alley cat café": "posters/dining/alley_cat_cafe.jpg",
        "巴哈咖啡": "posters/dining/bacha_coffee.jpg",
        "bacha coffee": "posters/dining/bacha_coffee.jpg",
        "迷魂珍珠奶茶": "posters/dining/bewitching_boba_and_brews.jpg",
        "bewitching boba & brews": "posters/dining/bewitching_boba_and_brews.jpg",
        "bewitching boba and brews": "posters/dining/bewitching_boba_and_brews.jpg",
        "海盗酒吧": "posters/dining/buccaneer_bar.jpg",
        "buccaneer bar": "posters/dining/buccaneer_bar.jpg",
        "宇宙烤肉串": "posters/dining/cosmic_kebabs.jpg",
        "cosmic kebabs": "posters/dining/cosmic_kebabs.jpg",
        "花园酒吧": "posters/dining/garden_bar.jpg",
        "garden bar": "posters/dining/garden_bar.jpg",
        "塔拉奶奶的厨房": "posters/dining/gramma_talas_kitchen.jpg",
        "gramma talas kitchen": "posters/dining/gramma_talas_kitchen.jpg",
        "gramma tala's kitchen": "posters/dining/gramma_talas_kitchen.jpg",
        "无边际酒吧": "posters/dining/infinity_bar.jpg",
        "infinity bar": "posters/dining/infinity_bar.jpg",
        "市场酒吧": "posters/dining/market_bar.jpg",
        "market bar": "posters/dining/market_bar.jpg",
        "怪兽亚洲风味": "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "mike & sulley's flavors of asia": "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "mike and sulleys flavors of asia": "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "莫格利餐厅": "posters/dining/mowglis_eatery.jpg",
        "mowgli's eatery": "posters/dining/mowglis_eatery.jpg",
        "mowglis eatery": "posters/dining/mowglis_eatery.jpg",
        "帕洛咖啡": "posters/dining/palo_cafe.jpg",
        "palo café": "posters/dining/palo_cafe.jpg",
        "palo cafe": "posters/dining/palo_cafe.jpg",
        "帕洛意大利餐厅": "posters/dining/palo_trattoria.jpg",
        "palo trattoria": "posters/dining/palo_trattoria.jpg",
        "皮克斯市场餐厅": "posters/dining/pixar_market_restaurant.jpg",
        "pixar market restaurant": "posters/dining/pixar_market_restaurant.jpg",
        "客房送餐": "posters/dining/room_service.jpg",
        "room service": "posters/dining/room_service.jpg",
        "咒语酒廊": "posters/dining/spellbound.jpg",
        "spellbound": "posters/dining/spellbound.jpg",
        "史迪奇奥哈纳烧烤": "posters/dining/stitchs_ohana_grill.jpg",
        "stitch's ohana grill": "posters/dining/stitchs_ohana_grill.jpg",
        "stitchs ohana grill": "posters/dining/stitchs_ohana_grill.jpg",
        "波尔托罗索小酒馆": "posters/dining/taverna_portorosso.jpg",
        "taverna portorosso": "posters/dining/taverna_portorosso.jpg",
        "蒂安娜河口酒廊": "posters/dining/tianas_bayou_lounge.jpg",
        "tiana's bayou lounge": "posters/dining/tianas_bayou_lounge.jpg",
        "tianas bayou lounge": "posters/dining/tianas_bayou_lounge.jpg",
        "twg 茶室": "posters/dining/twg_tea.jpg",
        "twg tea": "posters/dining/twg_tea.jpg",
        "twg": "posters/dining/twg_tea.jpg",
        "寻路人酒吧": "posters/dining/wayfinder_bar.jpg",
        "wayfinder bar": "posters/dining/wayfinder_bar.jpg",
        "wheezy's freezies": "posters/dining/wheezys_freezies.jpg",
        "wheezys freezies": "posters/dining/wheezys_freezies.jpg",

        // 47-51. 青少年与儿童活动空间专属海报映射
        "小小世界托儿所": "posters/youth/its_a_small_world_nursery.jpg",
        "it's a small world nursery": "posters/youth/its_a_small_world_nursery.jpg",
        "its a small world nursery": "posters/youth/its_a_small_world_nursery.jpg",
        "米奇米妮船长甲板": "posters/youth/mickey_minnie_captains_deck.jpg",
        "mickey & minnie captain's deck": "posters/youth/mickey_minnie_captains_deck.jpg",
        "mickey and minnie captain's deck": "posters/youth/mickey_minnie_captains_deck.jpg",
        "captain's deck": "posters/youth/mickey_minnie_captains_deck.jpg",
        "迪士尼海洋探险家俱乐部": "posters/youth/disneys_oceaneer_club.jpg",
        "海洋探险家俱乐部": "posters/youth/disneys_oceaneer_club.jpg",
        "disney's oceaneer club": "posters/youth/disneys_oceaneer_club.jpg",
        "disneys oceaneer club": "posters/youth/disneys_oceaneer_club.jpg",
        "oceaneer club": "posters/youth/disneys_oceaneer_club.jpg",
        "边缘少年俱乐部": "posters/youth/edge.jpg",
        "edge": "posters/youth/edge.jpg",
        "活力少年俱乐部": "posters/youth/vibe.jpg",
        "vibe": "posters/youth/vibe.jpg",
    ]

    /// 其他非演出类事件标题 / 俱乐部等映射
    private var eventPosterRegistry: [String: String] = [
        "航海家": "posters/facilities/navigators_club.jpg",
        "navigator": "posters/facilities/navigators_club.jpg",
        "动画师": "posters/facilities/animators_palate.jpg",
        "animator": "posters/facilities/animators_palate.jpg",
        "魔法盛夏": "posters/facilities/enchanted_summer.jpg",
        "enchanted summer": "posters/facilities/enchanted_summer.jpg",
        "好莱坞聚光灯": "posters/facilities/hollywood_spotlight_club.jpg",
        "hollywood spotlight": "posters/facilities/hollywood_spotlight_club.jpg",
        "披萨星球": "posters/facilities/pizza_planet.jpg",
        "pizza planet": "posters/facilities/pizza_planet.jpg",

        // 商店
        "三个愿望": "posters/shops/3_wishes.jpg",
        "3 wishes": "posters/shops/3_wishes.jpg",
        "魔法变身精品店": "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "bibbidi bobbidi": "posters/shops/bibbidi_bobbidi_boutique.jpg",
        "城堡典藏": "posters/shops/castle_collection.jpg",
        "castle collection": "posters/shops/castle_collection.jpg",
        "钻石与愿望": "posters/shops/diamonds_and_wishes.jpg",
        "diamonds and wishes": "posters/shops/diamonds_and_wishes.jpg",
        "迪士尼影楼": "posters/shops/disney_royal_studio.jpg",
        "disney studio": "posters/shops/disney_royal_studio.jpg",
        "皇家影楼": "posters/shops/disney_royal_studio.jpg",
        "royal studio": "posters/shops/disney_royal_studio.jpg",
        "达菲与朋友商店": "posters/shops/duffy_and_friends_shop.jpg",
        "duffy and friends shop": "posters/shops/duffy_and_friends_shop.jpg",
        "漫威造型工作室": "posters/shops/marvel_style_studio.jpg",
        "marvel style studio": "posters/shops/marvel_style_studio.jpg",
        "国家地理": "posters/shops/national_geographic.jpg",
        "national geographic": "posters/shops/national_geographic.jpg",
        "宫廷珍宝": "posters/shops/palace_treasures.jpg",
        "palace treasures": "posters/shops/palace_treasures.jpg",
        "快照摄影店": "posters/shops/pics_photo_shop.jpg",
        "pics photo shop": "posters/shops/pics_photo_shop.jpg",
        "未诉珍宝": "posters/shops/treasures_untold.jpg",
        "treasures untold": "posters/shops/treasures_untold.jpg",
        "迪士尼世界二号店": "posters/shops/world_of_disney_too.jpg",
        "world of disney too": "posters/shops/world_of_disney_too.jpg",
        "迪士尼世界": "posters/shops/world_of_disney.jpg",
        "world of disney": "posters/shops/world_of_disney.jpg",

        // 餐饮
        "流浪猫咖啡馆": "posters/dining/alley_cat_cafe.jpg",
        "alley cat cafe": "posters/dining/alley_cat_cafe.jpg",
        "巴哈咖啡": "posters/dining/bacha_coffee.jpg",
        "bacha coffee": "posters/dining/bacha_coffee.jpg",
        "迷魂珍珠奶茶": "posters/dining/bewitching_boba_and_brews.jpg",
        "bewitching boba": "posters/dining/bewitching_boba_and_brews.jpg",
        "海盗酒吧": "posters/dining/buccaneer_bar.jpg",
        "buccaneer bar": "posters/dining/buccaneer_bar.jpg",
        "宇宙烤肉串": "posters/dining/cosmic_kebabs.jpg",
        "cosmic kebabs": "posters/dining/cosmic_kebabs.jpg",
        "花园酒吧": "posters/dining/garden_bar.jpg",
        "garden bar": "posters/dining/garden_bar.jpg",
        "塔拉奶奶的厨房": "posters/dining/gramma_talas_kitchen.jpg",
        "gramma tala": "posters/dining/gramma_talas_kitchen.jpg",
        "无边际酒吧": "posters/dining/infinity_bar.jpg",
        "infinity bar": "posters/dining/infinity_bar.jpg",
        "市场酒吧": "posters/dining/market_bar.jpg",
        "market bar": "posters/dining/market_bar.jpg",
        "怪兽亚洲风味": "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "mike & sulley": "posters/dining/mike_and_sulleys_flavors_of_asia.jpg",
        "莫格利餐厅": "posters/dining/mowglis_eatery.jpg",
        "mowgli": "posters/dining/mowglis_eatery.jpg",
        "帕洛咖啡": "posters/dining/palo_cafe.jpg",
        "palo cafe": "posters/dining/palo_cafe.jpg",
        "帕洛意大利餐厅": "posters/dining/palo_trattoria.jpg",
        "palo trattoria": "posters/dining/palo_trattoria.jpg",
        "皮克斯市场餐厅": "posters/dining/pixar_market_restaurant.jpg",
        "pixar market": "posters/dining/pixar_market_restaurant.jpg",
        "客房送餐": "posters/dining/room_service.jpg",
        "room service": "posters/dining/room_service.jpg",
        "咒语酒廊": "posters/dining/spellbound.jpg",
        "spellbound": "posters/dining/spellbound.jpg",
        "史迪奇奥哈纳烧烤": "posters/dining/stitchs_ohana_grill.jpg",
        "stitch's ohana grill": "posters/dining/stitchs_ohana_grill.jpg",
        "波尔托罗索小酒馆": "posters/dining/taverna_portorosso.jpg",
        "taverna portorosso": "posters/dining/taverna_portorosso.jpg",
        "蒂安娜河口酒廊": "posters/dining/tianas_bayou_lounge.jpg",
        "tiana's bayou lounge": "posters/dining/tianas_bayou_lounge.jpg",
        "twg 茶室": "posters/dining/twg_tea.jpg",
        "twg tea": "posters/dining/twg_tea.jpg",
        "寻路人酒吧": "posters/dining/wayfinder_bar.jpg",
        "wayfinder bar": "posters/dining/wayfinder_bar.jpg",
        "威希冰品": "posters/dining/wheezys_freezies.jpg",
        "wheezy's freezies": "posters/dining/wheezys_freezies.jpg",

        // 青少年
        "小小世界托儿所": "posters/youth/its_a_small_world_nursery.jpg",
        "it's a small world nursery": "posters/youth/its_a_small_world_nursery.jpg",
        "米奇米妮船长甲板": "posters/youth/mickey_minnie_captains_deck.jpg",
        "mickey & minnie captain's deck": "posters/youth/mickey_minnie_captains_deck.jpg",
        "迪士尼海洋探险家俱乐部": "posters/youth/disneys_oceaneer_club.jpg",
        "海洋探险家俱乐部": "posters/youth/disneys_oceaneer_club.jpg",
        "disney's oceaneer club": "posters/youth/disneys_oceaneer_club.jpg",
        "disneys oceaneer club": "posters/youth/disneys_oceaneer_club.jpg",
        "oceaneer club": "posters/youth/disneys_oceaneer_club.jpg",
        "边缘少年俱乐部": "posters/youth/edge.jpg",
        "edge": "posters/youth/edge.jpg",
        "活力少年俱乐部": "posters/youth/vibe.jpg",
        "vibe": "posters/youth/vibe.jpg",

        // 水疗与礼宾
        "无限幸福水疗": "posters/facilities/infinite_bliss_spa.jpg",
        "infinite bliss spa": "posters/facilities/infinite_bliss_spa.jpg",
        "奢华水疗": "posters/facilities/opulence_spa.jpg",
        "opulence spa": "posters/facilities/opulence_spa.jpg",
        "礼宾休息室": "posters/facilities/concierge_lounge.jpg",
        "礼宾户外休息室": "posters/facilities/concierge_lounge.jpg",
        "concierge lounge": "posters/facilities/concierge_lounge.jpg",
        "礼宾阳光甲板": "posters/facilities/concierge_sundeck.jpg",
        "concierge sundeck": "posters/facilities/concierge_sundeck.jpg",
    ]

    public init() {}

    /// 注册设施海报
    public func registerFacilityPoster(placeName: String, assetPath: String) {
        lock.lock()
        facilityPosterRegistry[placeName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()] = assetPath
        lock.unlock()
    }

    /// 注册事件海报
    public func registerEventPoster(keyOrTitle: String, assetPath: String) {
        lock.lock()
        eventPosterRegistry[keyOrTitle.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()] = assetPath
        lock.unlock()
    }

    /// 获取事件海报的相对路径
    public func getEventPosterPath(eventTitle: String, presetKey: String? = nil) -> String? {
        let key = presetKey?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let title = eventTitle.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

        // 1. PresetKey 精确匹配演出海报
        if let key = key, let path = showPosterRegistry[key] {
            return path
        }

        // 2. 演出关键词/标题匹配
        for (keywords, path) in showKeywordRegistry {
            if keywords.contains(where: { kw in
                title.contains(kw) || (key != nil && key!.contains(kw))
            }) {
                return path
            }
        }

        lock.lock()
        defer { lock.unlock() }

        // 3. 其他非演出事件自定义注册表
        if let key = key, let path = eventPosterRegistry[key] {
            return path
        }
        if let path = eventPosterRegistry[title] {
            return path
        }
        for (regKey, path) in eventPosterRegistry {
            if title.contains(regKey) {
                return path
            }
        }

        return nil
    }

    /// 获取设施海报的相对路径
    public func getFacilityPosterPath(placeName: String) -> String? {
        let trimmed = placeName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

        lock.lock()
        defer { lock.unlock() }

        if let path = facilityPosterRegistry[trimmed] {
            return path
        }

        for (regKey, path) in facilityPosterRegistry {
            if trimmed.contains(regKey) {
                return path
            }
        }

        return nil
    }

    /// 查找地点对应的海报路径
    public func findPosterPath(for placeName: String) -> String? {
        getFacilityPosterPath(placeName: placeName)
    }

    /// 判断地点海报的大类名称 ("dining", "facilities", "shops", "shows", "youth")
    public func categoryOf(placeName: String) -> String? {
        guard let path = findPosterPath(for: placeName) else { return nil }
        let lower = path.lowercased()
        if lower.contains("dining") { return "dining" }
        if lower.contains("facilities") { return "facilities" }
        if lower.contains("shops") { return "shops" }
        if lower.contains("shows") { return "shows" }
        if lower.contains("youth") { return "youth" }
        return nil
    }

    /// 解析 Bundle 中的海报 URL
    public func urlForPoster(path: String, bundle: Bundle? = nil) -> URL? {
        let targetBundle = bundle ?? CruiseAssistant.bundle
        let clean = (path as NSString).lastPathComponent
        let name = (clean as NSString).deletingPathExtension
        let ext = (clean as NSString).pathExtension.isEmpty ? "jpg" : (clean as NSString).pathExtension

        let directoryCandidates = [
            (path as NSString).deletingLastPathComponent,
            "Posters/\((path as NSString).deletingLastPathComponent)",
            "Posters",
            "Resources/Posters/\((path as NSString).deletingLastPathComponent)",
            nil,
        ]

        for sub in directoryCandidates {
            if let url = targetBundle.url(forResource: name, withExtension: ext, subdirectory: sub) {
                return url
            }
        }

        return targetBundle.url(forResource: name, withExtension: ext)
    }

    public func posterUrl(for placeName: String, bundle: Bundle? = nil) -> URL? {
        guard let path = findPosterPath(for: placeName) else { return nil }
        return urlForPoster(path: path, bundle: bundle)
    }

    public func posterUrl(forEventTitle eventTitle: String, presetKey: String? = nil, bundle: Bundle? = nil) -> URL? {
        guard let path = getEventPosterPath(eventTitle: eventTitle, presetKey: presetKey) else { return nil }
        return urlForPoster(path: path, bundle: bundle)
    }

    // MARK: - Static Convenience Forwarders

    public static func findPosterPath(for placeName: String) -> String? {
        shared.findPosterPath(for: placeName)
    }

    public static func categoryOf(placeName: String) -> String? {
        shared.categoryOf(placeName: placeName)
    }

    public static func getEventPosterPath(eventTitle: String, presetKey: String? = nil) -> String? {
        shared.getEventPosterPath(eventTitle: eventTitle, presetKey: presetKey)
    }

    public static func getFacilityPosterPath(placeName: String) -> String? {
        shared.getFacilityPosterPath(placeName: placeName)
    }

    public static func registerFacilityPoster(placeName: String, assetPath: String) {
        shared.registerFacilityPoster(placeName: placeName, assetPath: assetPath)
    }

    public static func registerEventPoster(keyOrTitle: String, assetPath: String) {
        shared.registerEventPoster(keyOrTitle: keyOrTitle, assetPath: assetPath)
    }
}
