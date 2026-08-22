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
}
