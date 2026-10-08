package com.cruise.assistant.data.repo

import com.cruise.assistant.data.BUILT_IN_CATEGORIES
import com.cruise.assistant.data.db.dao.*
import com.cruise.assistant.data.db.entity.*
import com.cruise.assistant.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class VoyageRepositoryTest {
    private class FakeVoyageDao : VoyageDao {
        var voyage: VoyageEntity? = null
        val dayPlans = mutableListOf<DayPlanEntity>()
        override fun observeVoyage(): Flow<VoyageEntity?> = emptyFlow()
        override suspend fun getVoyage(): VoyageEntity? = voyage
        override suspend fun upsertVoyage(voyage: VoyageEntity) { this.voyage = voyage }
        override suspend fun clearVoyage() { voyage = null }
        override fun observeDayPlans(): Flow<List<DayPlanEntity>> = emptyFlow()
        override suspend fun getDayPlansOnce(): List<DayPlanEntity> = dayPlans
        override fun observeDayPlan(date: LocalDate): Flow<DayPlanEntity?> = emptyFlow()
        override suspend fun upsertDayPlans(plans: List<DayPlanEntity>) { dayPlans.addAll(plans) }
        override suspend fun upsertDayPlan(plan: DayPlanEntity) { dayPlans.add(plan) }
        override suspend fun deleteDayPlan(plan: DayPlanEntity) { dayPlans.remove(plan) }
        override suspend fun clearDayPlans() { dayPlans.clear() }
    }

    private class FakeEventDao : EventDao {
        val events = mutableListOf<EventEntity>()
        override fun observeByDate(date: LocalDate): Flow<List<EventEntity>> = emptyFlow()
        override suspend fun getByDate(date: LocalDate): List<EventEntity> = events.filter { it.date == date }
        override suspend fun getAll(): List<EventEntity> = events
        override suspend fun getById(id: Long): EventEntity? = events.find { it.id == id }
        override suspend fun getByGroup(groupId: String): List<EventEntity> = emptyList()
        override suspend fun getByPresetKey(key: String): List<EventEntity> = events.filter { it.presetKey == key }
        override fun observeDatesWithEvents(): Flow<List<LocalDate>> = emptyFlow()
        override suspend fun insert(event: EventEntity): Long {
            val id = (events.size + 1).toLong()
            events.add(event.copy(id = id))
            return id
        }
        override suspend fun insertAll(events: List<EventEntity>): List<Long> {
            return events.map { insert(it) }
        }
        override suspend fun update(event: EventEntity) {}
        override suspend fun updateAll(events: List<EventEntity>) {}
        override suspend fun delete(event: EventEntity) { events.remove(event) }
        override suspend fun deleteGroup(groupId: String) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {}
        override suspend fun setActive(id: Long, active: Boolean) {}
        override suspend fun countByCategory(categoryId: Long): Int = 0
        override suspend fun clearAll() { events.clear() }
    }

    private class FakeCategoryDao : CategoryDao {
        val categories = BUILT_IN_CATEGORIES.mapIndexed { idx, cat -> cat.copy(id = idx + 1L).toEntity() }.toMutableList()
        override fun observeAll(): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun observeAllOnce(): List<CategoryEntity> = categories
        override fun observeActive(): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun count(): Int = categories.size
        override suspend fun insert(category: CategoryEntity): Long = 1L
        override suspend fun insertAll(categories: List<CategoryEntity>) {}
        override suspend fun getById(id: Long): CategoryEntity? = categories.find { it.id == id }
        override suspend fun update(category: CategoryEntity) {}
        override suspend fun delete(category: CategoryEntity) {}
        override suspend fun clearAll() {}
    }

    private class FakeLocationPresetDao : LocationPresetDao {
        override fun observeAll(): Flow<List<LocationPresetEntity>> = emptyFlow()
        override suspend fun find(deck: Int, zone: String, placeName: String): LocationPresetEntity? = null
        override suspend fun upsert(preset: LocationPresetEntity) {}
        override suspend fun delete(preset: LocationPresetEntity) {}
        override suspend fun clearAll() {}
    }

    private class FakePixieDustDao : PixieDustDao {
        override fun observeAll(): Flow<List<PixieDustEntity>> = emptyFlow()
        override suspend fun getById(id: Long): PixieDustEntity? = null
        override suspend fun getAll(): List<PixieDustEntity> = emptyList()
        override suspend fun insert(record: PixieDustEntity): Long = 1L
        override suspend fun update(record: PixieDustEntity) {}
        override suspend fun delete(record: PixieDustEntity) {}
        override suspend fun countByLevel(levelId: Long): Int = 0
        override suspend fun clearAll() {}
    }

    private class FakePixieLevelDao : PixieLevelDao {
        override fun observeAll(): Flow<List<PixieLevelEntity>> = emptyFlow()
        override suspend fun observeAllOnce(): List<PixieLevelEntity> = emptyList()
        override suspend fun getById(id: Long): PixieLevelEntity? = null
        override suspend fun insert(level: PixieLevelEntity): Long = 1L
        override suspend fun insertAll(levels: List<PixieLevelEntity>) {}
        override suspend fun update(level: PixieLevelEntity) {}
        override suspend fun delete(level: PixieLevelEntity) {}
        override suspend fun count(): Int = 0
        override suspend fun clearAll() {}
    }

    private class FakeWishlistDao : WishlistDao {
        val items = mutableListOf<WishlistItemEntity>()
        override fun observeAll(): Flow<List<WishlistItemEntity>> = emptyFlow()
        override suspend fun getAll(): List<WishlistItemEntity> = items
        override suspend fun getById(id: Long): WishlistItemEntity? = items.find { it.id == id }
        override suspend fun insert(item: WishlistItemEntity): Long {
            val id = (items.size + 1).toLong()
            items.add(item.copy(id = id))
            return id
        }
        override suspend fun update(item: WishlistItemEntity) {}
        override suspend fun delete(item: WishlistItemEntity) { items.remove(item) }
        override suspend fun toggleCompleted(id: Long, completed: Boolean) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {
            // 测试 Fake：无实现
        }
    }

    @Test
    fun createVoyage_seedsOnlyThreeShowsOnTimeline_andSeedsFiveShowsInWishlist() = runBlocking {
        val eventDao = FakeEventDao()
        val wishlistDao = FakeWishlistDao()
        val repo = VoyageRepository(
            voyageDao = FakeVoyageDao(),
            eventDao = eventDao,
            categoryDao = FakeCategoryDao(),
            locationPresetDao = FakeLocationPresetDao(),
            pixieDustDao = FakePixieDustDao(),
            pixieLevelDao = FakePixieLevelDao(),
            wishlistDao = wishlistDao,
        )

        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = LocalDate.of(2026, 9, 10),
            boardTime = LocalTime.of(13, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
        )

        // 检查时间轴生成的演出事件（presetKey 以 show_ 开头）
        val showEvents = eventDao.events.filter { it.presetKey?.startsWith("show_") == true }
        // 登船日 3 场扬帆起航 + 1 场海洋奇遇 + 第2晚 1 场狮子王烟花 = 5 场事件（3 部演出）
        assertEquals(5, showEvents.size)
        val showKeys = showEvents.map { it.presetKey }.toSet()
        assertEquals(setOf("show_set_sail", "show_seas_adventure", "show_lion_king"), showKeys)

        val lionKing = showEvents.first { it.presetKey == "show_lion_king" }
        assertEquals("阳光泳池 Sunnyside Pool", lionKing.placeName)
        assertEquals("烟花在右舷，可以在这个泳池旁边的空地或者18甲左舷走廊（人比较多）", lionKing.note)

        // 检查心愿单播种的 5 部演出
        val wishlistItems = wishlistDao.items
        assertEquals(5, wishlistItems.size)
        val titles = wishlistItems.map { it.title }.toSet()
        assertTrue(titles.contains("回忆：瓦力与伊芙 Remember"))
        assertTrue(titles.contains("海洋奇缘 Moana: Call of the Sea"))
        assertTrue(titles.contains("复仇者集结 Avengers Assemble!"))
        assertTrue(titles.contains("大白超级健身世博会 Baymax Super Exercise Expo"))
        assertTrue(titles.contains("达菲与友谊之船 Duffy and The Friend Ship"))

        // 幂等性验证：再次调用 createVoyage，心愿单不增加重复项
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = LocalDate.of(2026, 9, 10),
            boardTime = LocalTime.of(13, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
        )
        assertEquals(5, wishlistDao.items.size)
    }

    @Test
    fun createVoyage_withConcierge_marksVoyageRow() = runBlocking {
        val voyageDao = FakeVoyageDao()
        val repo = VoyageRepository(
            voyageDao = voyageDao,
            eventDao = FakeEventDao(),
            categoryDao = FakeCategoryDao(),
            locationPresetDao = FakeLocationPresetDao(),
            pixieDustDao = FakePixieDustDao(),
            pixieLevelDao = FakePixieLevelDao(),
        )

        // 礼宾客户：不受登船批次限制，11:00 优先登船——航次行要带上该标记
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = LocalDate.of(2026, 9, 10),
            boardTime = LocalTime.of(11, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
            maxDeck = 19,
            isConcierge = true,
        )

        val voyage = voyageDao.voyage
        assertNotNull(voyage)
        assertTrue(voyage!!.isConcierge)
    }

    @Test
    fun createVoyage_withoutConcierge_defaultsToFalse() = runBlocking {
        val voyageDao = FakeVoyageDao()
        val repo = VoyageRepository(
            voyageDao = voyageDao,
            eventDao = FakeEventDao(),
            categoryDao = FakeCategoryDao(),
            locationPresetDao = FakeLocationPresetDao(),
            pixieDustDao = FakePixieDustDao(),
            pixieLevelDao = FakePixieLevelDao(),
        )

        // 不传 isConcierge：默认普通游客，标记为 false
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = LocalDate.of(2026, 9, 10),
            boardTime = LocalTime.of(13, 0),
            disembarkDate = LocalDate.of(2026, 9, 14),
            maxDeck = 19,
        )

        val voyage = voyageDao.voyage
        assertNotNull(voyage)
        assertFalse(voyage!!.isConcierge)
    }

    // ---- 联程航次（back-to-back）----

    /** 测试用联程航次仓库（含心愿单播种路径，shipPresetName 非空走预置活动生成） */
    private fun newRepo(
        eventDao: FakeEventDao,
        voyageDao: FakeVoyageDao = FakeVoyageDao(),
        wishlistDao: FakeWishlistDao? = null,
    ) = VoyageRepository(
        voyageDao = voyageDao,
        eventDao = eventDao,
        categoryDao = FakeCategoryDao(),
        locationPresetDao = FakeLocationPresetDao(),
        pixieDustDao = FakePixieDustDao(),
        pixieLevelDao = FakePixieLevelDao(),
        wishlistDao = wishlistDao,
    )

    @Test
    fun createVoyage_multiLeg3Plus4_singleHandoverAndDisembark() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        // 2026-09-07 是周一：3夜（周一登船）+ 4夜（周四交接）back-to-back
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
        )

        // 交接事件恰 1 个：boardDate+3（周四交接日），08:30 起 90 分钟，无地点，系统生成
        val handovers = eventDao.events.filter { it.title == "办理交接手续" }
        assertEquals(1, handovers.size)
        val handover = handovers.first()
        assertEquals(board.plusDays(3), handover.date)
        assertEquals(LocalTime.of(8, 30), handover.startTime)
        assertEquals(90, handover.durationMinutes)
        assertNull(handover.deck)
        assertNull(handover.zone)
        assertNull(handover.placeName)
        assertTrue(handover.isSystemGenerated)

        // 下船事件恰 1 个：boardDate+7（最终下船日）07:00
        val disembarks = eventDao.events.filter { it.title == "下船" }
        assertEquals(1, disembarks.size)
        assertEquals(board.plusDays(7), disembarks.first().date)
        assertEquals(LocalTime.of(7, 0), disembarks.first().startTime)

        // 登船事件恰 1 个：仅首程登船日
        val boardings = eventDao.events.filter { it.title == "登船" }
        assertEquals(1, boardings.size)
        assertEquals(board, boardings.first().date)
    }

    @Test
    fun createVoyage_multiLeg3Plus4Plus3_twoHandoversOneDisembark() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        // 3夜+4夜+3夜：交接在 +3、+7，最终下船在 +10
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(10),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4, 3),
        )

        val handovers = eventDao.events.filter { it.title == "办理交接手续" }
        assertEquals(2, handovers.size)
        assertEquals(
            setOf(board.plusDays(3), board.plusDays(7)),
            handovers.map { it.date }.toSet(),
        )

        val disembarks = eventDao.events.filter { it.title == "下船" }
        assertEquals(1, disembarks.size)
        assertEquals(board.plusDays(10), disembarks.first().date)

        assertEquals(1, eventDao.events.count { it.title == "登船" })
    }

    @Test
    fun createVoyage_multiLeg_showsRegeneratePerLeg() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
        )

        // 扬帆起航（dayOffsets=[1]，3 场/天）：每程 day1 各重演一遍
        // 首程 day1 = board，第二程 day1 = board+3（交接日，也是第二程登船日）
        val setSail = eventDao.events.filter { it.presetKey == "show_set_sail" }
        assertEquals(2 * 3, setSail.size)
        assertEquals(3, setSail.count { it.date == board })
        assertEquals(3, setSail.count { it.date == board.plusDays(3) })

        // 狮子王烟花（dayOffsets=[2]，1 场/天）：首程 day2 = board+1，第二程 day2 = board+4
        val lionKing = eventDao.events.filter { it.presetKey == "show_lion_king" }
        assertEquals(2, lionKing.size)
        assertEquals(1, lionKing.count { it.date == board.plusDays(1) })
        assertEquals(1, lionKing.count { it.date == board.plusDays(4) })

        // 演出不落到第二程下船日（board+7）及之外
        assertTrue(setSail.none { it.date >= board.plusDays(7) })
    }

    @Test
    fun createVoyage_multiLeg_singleSafetyDrillOnFirstLegDay1() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
        )

        // 安全演习仅 1 个，在首程 day1（联程不在交接日重复演习）
        val drills = eventDao.events.filter { it.presetKey == "safety_drill" }
        assertEquals(1, drills.size)
        assertEquals(board, drills.first().date)
    }

    @Test
    fun createVoyage_multiLeg_dinnerOnJunction_noneOnFinalDisembark() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
        )

        val dinners = eventDao.events.filter { it.presetKey == "dinner" }
        // 除最终下船日外每天都有：8 天航次 → 7 顿晚餐
        assertEquals(7, dinners.size)
        // 交接日（board+3）不下船、照常用晚餐——有意为之
        assertTrue(dinners.any { it.date == board.plusDays(3) })
        // 最终下船日（board+7）无晚餐
        assertTrue(dinners.none { it.date == board.plusDays(7) })
    }

    @Test
    fun createVoyage_emptyLegs_keepsSingleLegBehavior() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        val board = LocalDate.of(2026, 9, 7)
        // 显式传 legs = emptyList()：行为与旧单程完全一致（无交接事件）
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(4),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = emptyList(),
        )

        assertEquals(0, eventDao.events.count { it.title == "办理交接手续" })
        assertEquals(1, eventDao.events.count { it.title == "登船" })
        assertEquals(1, eventDao.events.count { it.title == "下船" })
        // 单程演出仍只生成一次（boardDate 上 3 场扬帆起航，不重复）
        val setSail = eventDao.events.filter { it.presetKey == "show_set_sail" }
        assertEquals(3, setSail.size)
        assertTrue(setSail.all { it.date == board })
    }

    @Test
    fun updateVoyage_multiLeg_reinsertsHandoversAtNewJunctions() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
        )
        assertEquals(1, eventDao.events.count { it.title == "办理交接手续" })

        // 改成 3+4+3 三程、总长延到 10 天：交接事件应重插为 2 个（+3、+7）
        repo.updateVoyage(
            com.cruise.assistant.domain.Voyage(
                id = 1,
                shipName = "Disney Adventure",
                boardDate = board,
                boardTime = LocalTime.of(12, 0),
                disembarkDate = board.plusDays(10),
                legs = listOf(3, 4, 3),
            ),
        )
        val handovers = eventDao.events.filter { it.title == "办理交接手续" }
        assertEquals(2, handovers.size)
        assertEquals(
            setOf(board.plusDays(3), board.plusDays(7)),
            handovers.map { it.date }.toSet(),
        )
        assertTrue(handovers.all { it.isSystemGenerated })
        assertEquals(90, handovers.first().durationMinutes)
    }

    @Test
    fun updateVoyage_toSingleLeg_removesHandovers() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
        )

        // 改回单程（legs 空）：交接事件应全部清除
        repo.updateVoyage(
            com.cruise.assistant.domain.Voyage(
                id = 1,
                shipName = "Disney Adventure",
                boardDate = board,
                boardTime = LocalTime.of(12, 0),
                disembarkDate = board.plusDays(4),
            ),
        )
        assertEquals(0, eventDao.events.count { it.title == "办理交接手续" })
    }

    // ---- 兰卡威登岸（ashoreLegs）----

    @Test
    fun createVoyage_ashoreOn4NightLeg_eventOnThirdDayOfThatLeg() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        // 2026-09-07 是周一：3夜 + 4夜，第二程（下标1，4夜）登船日 = board+3（周四），
        // 第三天 = board+5（周六）靠港兰卡威
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
            ashoreLegs = setOf(1),
        )

        // 登岸事件恰 1 个：boardDate+5（第2程登船日+3+2），09:00 起 480 分钟，
        // 系统生成、无地点（岸上活动不涉及船上场所）
        val ashoreEvents = eventDao.events.filter { it.title == "兰卡威登岸" }
        assertEquals(1, ashoreEvents.size)
        val ashore = ashoreEvents.first()
        assertEquals(board.plusDays(5), ashore.date)
        assertEquals(LocalTime.of(9, 0), ashore.startTime)
        assertEquals(480, ashore.durationMinutes)
        assertTrue(ashore.isSystemGenerated)
        assertNull(ashore.deck)
        assertNull(ashore.zone)
        assertNull(ashore.placeName)
    }

    @Test
    fun createVoyage_ashoreOn3NightLeg_silentlyIgnored() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        val board = LocalDate.of(2026, 9, 7)
        // 勾在第 1 程（3夜程，非兰卡威航线）与越界下标：都不生成
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
            ashoreLegs = setOf(0, 5),
        )

        assertEquals(0, eventDao.events.count { it.title == "兰卡威登岸" })
    }

    @Test
    fun createVoyage_single4NightLeg_ashoreOnBoardPlus2() = runBlocking {
        val eventDao = FakeEventDao()
        val repo = newRepo(eventDao)
        // 2026-09-10 是周四：单程 4夜（legs 空，按总晚数 4 判定），第三天 = board+2
        val board = LocalDate.of(2026, 9, 10)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(4),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            ashoreLegs = setOf(0),
        )

        val ashoreEvents = eventDao.events.filter { it.title == "兰卡威登岸" }
        assertEquals(1, ashoreEvents.size)
        assertEquals(board.plusDays(2), ashoreEvents.first().date)
        assertEquals(LocalTime.of(9, 0), ashoreEvents.first().startTime)
    }

    @Test
    fun updateVoyage_newBoardDate_reinsertsAshoreAtNewDate() = runBlocking {
        val eventDao = FakeEventDao()
        val voyageDao = FakeVoyageDao()
        val repo = newRepo(eventDao, voyageDao)
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
            ashoreLegs = setOf(1),
        )
        assertEquals(1, eventDao.events.count { it.title == "兰卡威登岸" })

        // 登船日推迟一周：登岸事件应随新航次重插到新日期（新 board+5），不残留旧日期
        val newBoard = board.plusDays(7)
        repo.updateVoyage(
            com.cruise.assistant.domain.Voyage(
                id = 1,
                shipName = "Disney Adventure",
                boardDate = newBoard,
                boardTime = LocalTime.of(12, 0),
                disembarkDate = newBoard.plusDays(7),
                legs = listOf(3, 4),
                ashoreLegs = setOf(1),
            ),
        )
        val ashoreEvents = eventDao.events.filter { it.title == "兰卡威登岸" }
        assertEquals(1, ashoreEvents.size)
        assertEquals(newBoard.plusDays(5), ashoreEvents.first().date)
        assertTrue(ashoreEvents.first().isSystemGenerated)
        assertEquals(480, ashoreEvents.first().durationMinutes)
    }

    @Test
    fun createVoyage_ashoreLegs_persistedOnVoyageRow() = runBlocking {
        val voyageDao = FakeVoyageDao()
        val repo = newRepo(FakeEventDao(), voyageDao)
        val board = LocalDate.of(2026, 9, 7)
        repo.createVoyage(
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            maxDeck = 19,
            shipPresetName = "Disney Adventure",
            legs = listOf(3, 4),
            ashoreLegs = setOf(1),
        )

        // ashoreLegs 持久化到 voyage 行（JSON int 数组文本），重装后可读回
        val voyage = voyageDao.voyage
        assertNotNull(voyage)
        assertEquals("[1]", voyage!!.ashoreLegs)
    }
}
