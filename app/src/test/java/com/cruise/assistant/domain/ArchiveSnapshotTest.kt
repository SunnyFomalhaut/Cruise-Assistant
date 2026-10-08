package com.cruise.assistant.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ArchiveSnapshotTest {

    /** 覆盖全部类型与可空字段组合的完整快照（联程 + 礼宾 + 双类别 + 各异事件 + 仙尘收发 + 愿望单） */
    private fun fullSnapshot() = ArchiveSnapshot(
        voyage = Voyage(
            id = 1,
            shipName = "Disney Adventure",
            boardDate = LocalDate.of(2026, 9, 7),
            boardTime = LocalTime.of(12, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = 19,
            minDeck = 5,
            isConcierge = true,
            legs = listOf(3, 4),
            // 第二程（4夜）勾了登岸兰卡威：非空集合走完整 round-trip 路径
            ashoreLegs = setOf(1),
        ),
        categories = listOf(
            Category(
                id = 1, name = "重要", colorArgb = 0xFFFF0000.toInt(), iconKey = "important",
                parentId = null, isBuiltIn = true, isArchived = false, sortOrder = 0,
            ),
            Category(
                id = 2, name = "演出", colorArgb = 0xFF00FF00.toInt(), iconKey = "show",
                parentId = 1, isBuiltIn = false, isArchived = true, sortOrder = 3,
            ),
        ),
        events = listOf(
            // 全字段齐活（可空字段全非空）
            CruiseEvent(
                id = 1, date = LocalDate.of(2026, 9, 7), startTime = LocalTime.of(12, 0),
                durationMinutes = 15, categoryId = 1, title = "登船",
                deck = 5, zone = ShipZone.MID, placeName = "四层码头通道",
                lane = 1, recurrenceGroupId = "g-1", reminderMinutesBefore = 30,
                isSystemGenerated = true, isActive = false,
                presetKey = "show_lion_king", waitMinutes = 30, note = "备注甲",
            ),
            // 可空字段全空（省略键路径）+ 边界时间（零点/个位分钟）
            CruiseEvent(
                id = 2, date = LocalDate.of(2026, 9, 10), startTime = LocalTime.of(0, 5),
                durationMinutes = 75, categoryId = 2, title = "晚餐",
                deck = null, zone = null, placeName = null,
                lane = 0, recurrenceGroupId = null, reminderMinutesBefore = null,
                isSystemGenerated = false, isActive = true,
                presetKey = null, waitMinutes = null, note = null,
            ),
        ),
        dayPlans = listOf(
            DayPlan(date = LocalDate.of(2026, 9, 7), finalized = true, finalizedAt = 1_700_000_000_000),
            DayPlan(date = LocalDate.of(2026, 9, 8), finalized = false, finalizedAt = null),
        ),
        pixieLevels = listOf(
            PixieLevel(
                id = 1, name = "普通", colorArgb = 0xFF111111.toInt(),
                iconKey = "gift", rank = 1, sortOrder = 0,
            ),
            PixieLevel(
                id = 2, name = "豪华", colorArgb = 0xFF222222.toInt(),
                iconKey = "star", rank = 5, sortOrder = 1,
            ),
        ),
        pixieDust = listOf(
            // 已回礼 + 带备注的发出记录
            PixieDustRecord(
                id = 1, direction = PixieDustDirection.GIVEN, deck = 11, room = "216",
                levelId = 1, levelNameSnapshot = "普通",
                replied = true, note = "对方回礼了", timestamp = 111,
            ),
            // 未回礼 + 空备注的收到记录（等级已被删，名字快照仍在）
            PixieDustRecord(
                id = 2, direction = PixieDustDirection.RECEIVED, deck = 10, room = "088",
                levelId = 3, levelNameSnapshot = "已被删除的等级",
                replied = false, note = "", timestamp = 222,
            ),
        ),
        wishlist = listOf(
            // 完成 + 带地点
            WishlistItem(
                id = 1, title = "狮子王", categoryId = 2,
                deck = 6, zone = ShipZone.STERN, placeName = "百老汇剧场",
                note = "提前占座", isCompleted = true, createdAt = 333,
            ),
            // 未完成 + 无地点（可空全空）
            WishlistItem(
                id = 2, title = "甲板跑步", categoryId = 1,
                deck = null, zone = null, placeName = null,
                note = null, isCompleted = false, createdAt = 444,
            ),
        ),
    )

    @Test
    fun roundTrip_fullSnapshot_preservesEveryField() {
        val original = fullSnapshot()
        val parsed = parseArchiveSnapshot(serializeArchiveSnapshot(original))
        // 数据类逐字段相等（含全部可空字段、枚举、日期时间、多程 legs、登岸程下标、礼宾标记）
        assertEquals(original, parsed)
        assertEquals(1, parsed!!.version)
        // 非空 ashoreLegs 原样还原
        assertEquals(setOf(1), parsed.voyage.ashoreLegs)
    }

    @Test
    fun roundTrip_emptyVoyageSnapshot() {
        val original = ArchiveSnapshot(
            voyage = Voyage(
                shipName = null,
                boardDate = LocalDate.of(2026, 3, 1),
                boardTime = LocalTime.of(13, 0),
                disembarkDate = LocalDate.of(2026, 3, 5),
                maxDeck = 20,
            ),
            categories = emptyList(),
            events = emptyList(),
            dayPlans = emptyList(),
            pixieLevels = emptyList(),
            pixieDust = emptyList(),
            wishlist = emptyList(),
        )
        assertEquals(original, parseArchiveSnapshot(serializeArchiveSnapshot(original)))
        // 空集 ashoreLegs 省略键（可空约定），旧档缺键解析回空集
        val voyageJson = JSONObject(serializeArchiveSnapshot(original)).getJSONObject("voyage")
        assertTrue(!voyageJson.has("ashoreLegs"))
    }

    @Test
    fun parse_corruptedJson_returnsNull() {
        assertNull(parseArchiveSnapshot(""))
        assertNull(parseArchiveSnapshot("这不是 JSON {{{"))
        assertNull(parseArchiveSnapshot("{\"voyage\":}"))
    }

    @Test
    fun parse_missingVoyageSection_returnsNull() {
        val json = serializeArchiveSnapshot(fullSnapshot())
        val stripped = JSONObject(json).apply { remove("voyage") }.toString()
        assertNull(parseArchiveSnapshot(stripped))
    }

    @Test
    fun parse_eventMissingRequiredField_returnsNull() {
        val json = serializeArchiveSnapshot(fullSnapshot())
        val root = JSONObject(json)
        root.getJSONArray("events").getJSONObject(0).remove("title")
        assertNull(parseArchiveSnapshot(root.toString()))
    }

    @Test
    fun parse_missingOptionalFields_defaultsGracefully() {
        // 向后兼容：可空字段缺失不判损坏，解析后落到对应默认值
        val json = serializeArchiveSnapshot(fullSnapshot())
        val root = JSONObject(json)
        val firstEvent = root.getJSONArray("events").getJSONObject(0)
        listOf(
            "deck", "zone", "placeName", "recurrenceGroupId",
            "reminderMinutesBefore", "presetKey", "waitMinutes", "note",
        ).forEach { firstEvent.remove(it) }
        val dayPlan = root.getJSONArray("dayPlans").getJSONObject(0)
        dayPlan.remove("finalizedAt")

        val parsed = parseArchiveSnapshot(root.toString())!!

        val event = parsed.events.first()
        assertNull(event.deck)
        assertNull(event.zone)
        assertNull(event.placeName)
        assertNull(event.recurrenceGroupId)
        assertNull(event.reminderMinutesBefore)
        assertNull(event.presetKey)
        assertNull(event.waitMinutes)
        assertNull(event.note)
        // 必填字段不受影响
        assertEquals("登船", event.title)
        // DayPlan 的 finalizedAt 缺失还原为 null，finalized 缺失按默认 false
        assertNull(parsed.dayPlans.first().finalizedAt)
    }
}
