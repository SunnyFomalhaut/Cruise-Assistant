package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PosterManagerTest {

    @Test
    fun getEventPosterPath_byPresetKey_matchesAll8Shows() {
        assertEquals("posters/shows/show_set_sail.jpg", PosterManager.getEventPosterPath("任意标题", "show_set_sail"))
        assertEquals("posters/shows/show_lion_king.jpg", PosterManager.getEventPosterPath("任意标题", "show_lion_king"))
        assertEquals("posters/shows/show_seas_adventure.jpg", PosterManager.getEventPosterPath("任意标题", "show_seas_adventure"))
        assertEquals("posters/shows/show_remember.jpg", PosterManager.getEventPosterPath("任意标题", "show_remember"))
        assertEquals("posters/shows/show_moana.jpg", PosterManager.getEventPosterPath("任意标题", "show_moana"))
        assertEquals("posters/shows/show_avengers.jpg", PosterManager.getEventPosterPath("任意标题", "show_avengers"))
        assertEquals("posters/shows/show_baymax_expo.jpg", PosterManager.getEventPosterPath("任意标题", "show_baymax_expo"))
        assertEquals("posters/shows/show_duffy_friend_ship.jpg", PosterManager.getEventPosterPath("任意标题", "show_duffy_friend_ship"))
    }

    @Test
    fun getEventPosterPath_byTitleAndKeywords_matchesAll8Shows() {
        assertEquals("posters/shows/show_set_sail.jpg", PosterManager.getEventPosterPath("扬帆起航 Let's Set Sail", null))
        assertEquals("posters/shows/show_lion_king.jpg", PosterManager.getEventPosterPath("狮子王烟花 The Lion King: Celebration in the Sky", null))
        assertEquals("posters/shows/show_seas_adventure.jpg", PosterManager.getEventPosterPath("海洋奇遇 Disney Seas The Adventure", null))
        assertEquals("posters/shows/show_remember.jpg", PosterManager.getEventPosterPath("回忆：瓦力与伊芙 Remember", null))
        assertEquals("posters/shows/show_moana.jpg", PosterManager.getEventPosterPath("海洋奇缘 Moana: Call of the Sea", null))
        assertEquals("posters/shows/show_avengers.jpg", PosterManager.getEventPosterPath("复仇者集结 Avengers Assemble!", null))
        assertEquals("posters/shows/show_baymax_expo.jpg", PosterManager.getEventPosterPath("大白超级健身世博会 Baymax Super Exercise Expo", null))
        assertEquals("posters/shows/show_duffy_friend_ship.jpg", PosterManager.getEventPosterPath("达菲与友谊之船 Duffy and The Friend Ship", null))

        // 简写/关键词匹配
        assertEquals("posters/shows/show_lion_king.jpg", PosterManager.getEventPosterPath("狮子王烟花", null))
        assertEquals("posters/shows/show_duffy_friend_ship.jpg", PosterManager.getEventPosterPath("达菲", null))
        assertEquals("posters/shows/show_baymax_expo.jpg", PosterManager.getEventPosterPath("大白健身", null))
        assertEquals("posters/shows/show_remember.jpg", PosterManager.getEventPosterPath("瓦力与伊芙", null))
        assertEquals("posters/shows/show_seas_adventure.jpg", PosterManager.getEventPosterPath("海洋奇遇", null))
    }

    @Test
    fun getEventPosterPath_nonShowEvent_fallsBackToNull() {
        assertNull(PosterManager.getEventPosterPath("自由活动", null))
    }

    @Test
    fun getFacilityPosterPath_all14Shops_matchCorrectly() {
        assertEquals("posters/shops/3_wishes.jpg", PosterManager.getFacilityPosterPath("三个愿望 3 Wishes"))
        assertEquals("posters/shops/bibbidi_bobbidi_boutique.jpg", PosterManager.getFacilityPosterPath("魔法变身精品店 Bibbidi Bobbidi Boutique"))
        assertEquals("posters/shops/castle_collection.jpg", PosterManager.getFacilityPosterPath("城堡典藏 Castle Collection"))
        assertEquals("posters/shops/diamonds_and_wishes.jpg", PosterManager.getFacilityPosterPath("钻石与愿望 Diamonds and Wishes"))
        assertEquals("posters/shops/disney_royal_studio.jpg", PosterManager.getFacilityPosterPath("迪士尼影楼 Disney Studio"))
        assertEquals("posters/shops/disney_royal_studio.jpg", PosterManager.getFacilityPosterPath("皇家影楼 Royal Studio"))
        assertEquals("posters/shops/duffy_and_friends_shop.jpg", PosterManager.getFacilityPosterPath("达菲与朋友商店 Duffy and Friends Shop"))
        assertEquals("posters/shops/marvel_style_studio.jpg", PosterManager.getFacilityPosterPath("漫威造型工作室 Marvel Style Studio"))
        assertEquals("posters/shops/national_geographic.jpg", PosterManager.getFacilityPosterPath("国家地理 National Geographic"))
        assertEquals("posters/shops/palace_treasures.jpg", PosterManager.getFacilityPosterPath("宫廷珍宝 Palace Treasures"))
        assertEquals("posters/shops/pics_photo_shop.jpg", PosterManager.getFacilityPosterPath("快照摄影店 Pics Photo Shop"))
        assertEquals("posters/shops/treasures_untold.jpg", PosterManager.getFacilityPosterPath("未诉珍宝 Treasures Untold"))
        assertEquals("posters/shops/world_of_disney.jpg", PosterManager.getFacilityPosterPath("迪士尼世界 World of Disney"))
        assertEquals("posters/shops/world_of_disney_too.jpg", PosterManager.getFacilityPosterPath("迪士尼世界二号店 World of Disney Too"))
    }

    @Test
    fun getFacilityPosterPath_all22DiningVenues_matchCorrectly() {
        assertEquals("posters/dining/alley_cat_cafe.jpg", PosterManager.getFacilityPosterPath("流浪猫咖啡馆 Alley Cat Café"))
        assertEquals("posters/dining/bacha_coffee.jpg", PosterManager.getFacilityPosterPath("巴哈咖啡 Bacha Coffee"))
        assertEquals("posters/dining/bewitching_boba_and_brews.jpg", PosterManager.getFacilityPosterPath("迷魂珍珠奶茶 Bewitching Boba & Brews"))
        assertEquals("posters/dining/buccaneer_bar.jpg", PosterManager.getFacilityPosterPath("海盗酒吧 Buccaneer Bar"))
        assertEquals("posters/dining/cosmic_kebabs.jpg", PosterManager.getFacilityPosterPath("宇宙烤肉串 Cosmic Kebabs"))
        assertEquals("posters/dining/garden_bar.jpg", PosterManager.getFacilityPosterPath("花园酒吧 Garden Bar"))
        assertEquals("posters/dining/gramma_talas_kitchen.jpg", PosterManager.getFacilityPosterPath("塔拉奶奶的厨房 Gramma Talas Kitchen"))
        assertEquals("posters/dining/infinity_bar.jpg", PosterManager.getFacilityPosterPath("无边际酒吧 Infinity Bar"))
        assertEquals("posters/dining/market_bar.jpg", PosterManager.getFacilityPosterPath("市场酒吧 Market Bar"))
        assertEquals("posters/dining/mike_and_sulleys_flavors_of_asia.jpg", PosterManager.getFacilityPosterPath("怪兽亚洲风味 Mike & Sulley's Flavors of Asia"))
        assertEquals("posters/dining/mowglis_eatery.jpg", PosterManager.getFacilityPosterPath("莫格利餐厅 Mowgli's Eatery"))
        assertEquals("posters/dining/palo_cafe.jpg", PosterManager.getFacilityPosterPath("帕洛咖啡 Palo Café"))
        assertEquals("posters/dining/palo_trattoria.jpg", PosterManager.getFacilityPosterPath("帕洛意大利餐厅 Palo Trattoria"))
        assertEquals("posters/dining/pixar_market_restaurant.jpg", PosterManager.getFacilityPosterPath("皮克斯市场餐厅 Pixar Market Restaurant"))
        assertEquals("posters/dining/room_service.jpg", PosterManager.getFacilityPosterPath("客房送餐 Room Service"))
        assertEquals("posters/dining/spellbound.jpg", PosterManager.getFacilityPosterPath("咒语酒廊 Spellbound"))
        assertEquals("posters/dining/stitchs_ohana_grill.jpg", PosterManager.getFacilityPosterPath("史迪奇奥哈纳烧烤 Stitch's Ohana Grill"))
        assertEquals("posters/dining/twg_tea.jpg", PosterManager.getFacilityPosterPath("TWG 茶室 TWG Tea"))
        assertEquals("posters/dining/taverna_portorosso.jpg", PosterManager.getFacilityPosterPath("波尔托罗索小酒馆 Taverna Portorosso"))
        assertEquals("posters/dining/tianas_bayou_lounge.jpg", PosterManager.getFacilityPosterPath("蒂安娜河口酒廊 Tiana's Bayou Lounge"))
        assertEquals("posters/dining/wayfinder_bar.jpg", PosterManager.getFacilityPosterPath("寻路人酒吧 Wayfinder Bar"))
        assertEquals("posters/dining/wheezys_freezies.jpg", PosterManager.getFacilityPosterPath("威希冰品 Wheezy's Freezies"))
    }

    @Test
    fun getFacilityPosterPath_majorVenues_matchCorrectly() {
        assertEquals("posters/facilities/hollywood_spotlight_club.jpg", PosterManager.getFacilityPosterPath("好莱坞聚光灯俱乐部 Hollywood Spotlight Club"))
        assertEquals("posters/facilities/animators_palate.jpg", PosterManager.getFacilityPosterPath("动画师味觉餐厅 Animator's Palate"))
        assertEquals("posters/facilities/enchanted_summer.jpg", PosterManager.getFacilityPosterPath("魔法盛夏餐厅 Enchanted Summer Restaurant"))
        assertEquals("posters/facilities/navigators_club.jpg", PosterManager.getFacilityPosterPath("航海家俱乐部 Navigator's Club"))
        assertEquals("posters/facilities/garden_stage.jpg", PosterManager.getFacilityPosterPath("花园舞台 Garden Stage"))
        assertEquals("posters/facilities/wayfinder_bay.jpg", PosterManager.getFacilityPosterPath("寻路者湾 Wayfinder Bay"))
        assertEquals("posters/facilities/marvel_landing.jpg", PosterManager.getFacilityPosterPath("格鲁特银河旋风 Groot Galaxy Spin"))
    }

    @Test
    fun getFacilityPosterPath_barsAndStores_matchOwnPosters_notGenericAreaPosters() {
        // 漫威造型工作室是商店，应匹配其专属商店图，而非漫威游乐区域图
        assertEquals("posters/shops/marvel_style_studio.jpg", PosterManager.getFacilityPosterPath("漫威造型工作室 Marvel Style Studio"))
        // 寻路人酒吧、花园酒吧是酒吧，应匹配专属酒吧图，而非露天泳池海湾/大舞台图
        assertEquals("posters/dining/wayfinder_bar.jpg", PosterManager.getFacilityPosterPath("寻路人酒吧 Wayfinder Bar"))
        assertEquals("posters/dining/garden_bar.jpg", PosterManager.getFacilityPosterPath("花园酒吧 Garden Bar"))
    }
}
