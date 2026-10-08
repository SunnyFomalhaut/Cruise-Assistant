package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.dao.PixieLevelDao
import com.cruise.assistant.data.db.dao.VoyageArchiveDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.dao.WishlistDao
import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.PixieDustEntity
import com.cruise.assistant.data.db.entity.PixieLevelEntity
import com.cruise.assistant.data.db.entity.VoyageArchiveEntity
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.data.db.entity.WishlistItemEntity
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toLegNightsJson
import com.cruise.assistant.domain.ArchiveSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class VoyageArchiveRepositoryTest {

    // ---- Fake DAO（与 VoyageRepositoryTest 同款：内存直接操作，无 Room）----

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
        override suspend fun clearDayPlans() { dayPlans.clear() }
        override suspend fun deleteDayPlan(plan: DayPlanEntity) { dayPlans.remove(plan) }
    }

    private class FakeEventDao : EventDao {
        val events = mutableListOf<EventEntity>()
        override fun observeByDate(date: LocalDate): Flow<List<EventEntity>> = emptyFlow()
        override suspend fun getByDate(date: LocalDate): List<EventEntity> = events.filter { it.date == date }
        override suspend fun getAll(): List<EventEntity> = events
        override suspend fun getById(id: Long): EventEntity? = events.find { it.id == id }
        override suspend fun getByGroup(groupId: String): List<EventEntity> = emptyList()
        override suspend fun getByPresetKey(key: String): List<EventEntity> = emptyList()
        override fun observeDatesWithEvents(): Flow<List<LocalDate>> = emptyFlow()
        override suspend fun insert(event: EventEntity): Long {
            val id = (events.size + 1).toLong()
            events.add(event.copy(id = id))
            return id
        }
        override suspend fun insertAll(events: List<EventEntity>): List<Long> = events.map { insert(it) }
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
        val categories = mutableListOf(
            CategoryEntity(
                id = 1, name = "重要", colorArgb = 0xFFFF0000.toInt(), iconKey = "important",
                parentId = null, isBuiltIn = true, isArchived = false, sortOrder = 0,
            ),
            CategoryEntity(
                id = 2, name = "演出", colorArgb = 0xFF00FF00.toInt(), iconKey = "show",
                parentId = null, isBuiltIn = false, isArchived = false, sortOrder = 1,
            ),
        )
        override fun observeAll(): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun observeAllOnce(): List<CategoryEntity> = categories
        override fun observeActive(): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun getById(id: Long): CategoryEntity? = categories.find { it.id == id }
        override suspend fun count(): Int = categories.size
        override suspend fun insert(category: CategoryEntity): Long = 1L
        override suspend fun insertAll(categories: List<CategoryEntity>) {}
        override suspend fun update(category: CategoryEntity) {}
        override suspend fun delete(category: CategoryEntity) {}
        override suspend fun clearAll() {}
    }

    private class FakePixieDustDao : PixieDustDao {
        val records = mutableListOf<PixieDustEntity>()
        override fun observeAll(): Flow<List<PixieDustEntity>> = emptyFlow()
        override suspend fun getById(id: Long): PixieDustEntity? = records.find { it.id == id }
        override suspend fun getAll(): List<PixieDustEntity> = records
        override suspend fun insert(record: PixieDustEntity): Long {
            val id = (records.size + 1).toLong()
            records.add(record.copy(id = id))
            return id
        }
        override suspend fun update(record: PixieDustEntity) {}
        override suspend fun delete(record: PixieDustEntity) { records.remove(record) }
        override suspend fun countByLevel(levelId: Long): Int = 0
        override suspend fun clearAll() { records.clear() }
    }

    private class FakePixieLevelDao : PixieLevelDao {
        val levels = mutableListOf<PixieLevelEntity>()
        override fun observeAll(): Flow<List<PixieLevelEntity>> = emptyFlow()
        override suspend fun observeAllOnce(): List<PixieLevelEntity> = levels
        override suspend fun getById(id: Long): PixieLevelEntity? = levels.find { it.id == id }
        override suspend fun insert(level: PixieLevelEntity): Long = 1L
        override suspend fun insertAll(levels: List<PixieLevelEntity>) {}
        override suspend fun update(level: PixieLevelEntity) {}
        override suspend fun delete(level: PixieLevelEntity) {}
        override suspend fun count(): Int = levels.size
        override suspend fun clearAll() { levels.clear() }
    }

    private class FakeWishlistDao : WishlistDao {
        val items = mutableListOf<WishlistItemEntity>()
        override fun observeAll(): Flow<List<WishlistItemEntity>> = emptyFlow()
        override suspend fun getAll(): List<WishlistItemEntity> = items
        override suspend fun getById(id: Long): WishlistItemEntity? = items.find { it.id == id }
        override suspend fun insert(item: WishlistItemEntity): Long = 1L
        override suspend fun update(item: WishlistItemEntity) {}
        override suspend fun delete(item: WishlistItemEntity) { items.remove(item) }
        override suspend fun toggleCompleted(id: Long, completed: Boolean) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {}
    }

    private class FakeVoyageArchiveDao : VoyageArchiveDao {
        val rows = mutableListOf<VoyageArchiveEntity>()
        private var nextId = 1L
        override fun observeAll(): Flow<List<VoyageArchiveMeta>> = MutableStateFlow(
            rows.map { e ->
                VoyageArchiveMeta(
                    id = e.id, shipName = e.shipName, boardDate = e.boardDate,
                    disembarkDate = e.disembarkDate, nightsSummary = e.nightsSummary,
                    createdAt = e.createdAt,
                )
            },
        )
        override suspend fun getById(id: Long): VoyageArchiveEntity? = rows.find { it.id == id }
        override suspend fun insert(entity: VoyageArchiveEntity): Long {
            val id = nextId++
            rows.add(entity.copy(id = id))
            return id
        }
        override suspend fun deleteById(id: Long) { rows.removeAll { it.id == id } }
    }

    // ---- 夹具 ----

    /** 铺一整套业务数据：联程 3+4 礼宾航次 + 类别 + 事件 + DayPlan 定稿 + 仙尘收发 + 愿望单 */
    private fun seedAll(
        voyageDao: FakeVoyageDao,
        eventDao: FakeEventDao = FakeEventDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        pixieDustDao: FakePixieDustDao = FakePixieDustDao(),
        pixieLevelDao: FakePixieLevelDao = FakePixieLevelDao(),
        wishlistDao: FakeWishlistDao = FakeWishlistDao(),
    ) {
        val board = LocalDate.of(2026, 9, 7)
        voyageDao.voyage = VoyageEntity(
            id = 1,
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(12, 0),
            disembarkDate = board.plusDays(7),
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = 19,
            minDeck = 5,
            isConcierge = true,
            legNights = listOf(3, 4).toLegNightsJson(),
        )
        voyageDao.dayPlans += listOf(
            DayPlanEntity(date = board, finalized = true, finalizedAt = 123L),
            DayPlanEntity(date = board.plusDays(1), finalized = false, finalizedAt = null),
        )
        eventDao.events += listOf(
            EventEntity(
                id = 1, date = board, startTime = LocalTime.of(12, 0), durationMinutes = 15,
                categoryId = 1, title = "登船", deck = null, zone = null, placeName = null,
                lane = 0, recurrenceGroupId = null, reminderMinutesBefore = null,
                isSystemGenerated = true, isActive = true, presetKey = null, waitMinutes = null,
                note = null,
            ),
            EventEntity(
                id = 2, date = board.plusDays(1), startTime = LocalTime.of(19, 0), durationMinutes = 50,
                categoryId = 2, title = "狮子王", deck = 6, zone = "STERN", placeName = "百老汇剧场",
                lane = 1, recurrenceGroupId = "g-1", reminderMinutesBefore = 30,
                isSystemGenerated = false, isActive = false, presetKey = "show_lion_king",
                waitMinutes = 30, note = "提前入场",
            ),
        )
        pixieLevelDao.levels += PixieLevelEntity(
            id = 1, name = "普通", colorArgb = 0xFF111111.toInt(), iconKey = "gift",
            rank = 1, sortOrder = 0,
        )
        pixieDustDao.records += listOf(
            PixieDustEntity(
                id = 1, direction = "GIVEN", deck = 11, room = "216", levelId = 1,
                levelNameSnapshot = "普通", replied = true, note = "回礼了", timestamp = 111,
            ),
            PixieDustEntity(
                id = 2, direction = "RECEIVED", deck = 10, room = "088", levelId = 2,
                levelNameSnapshot = "已被删除的等级", replied = false, note = "", timestamp = 222,
            ),
        )
        wishlistDao.items += listOf(
            WishlistItemEntity(
                id = 1, title = "狮子王", categoryId = 2, deck = 6, zone = "STERN",
                placeName = "百老汇剧场", note = "提前占座", isCompleted = true, createdAt = 333,
            ),
            WishlistItemEntity(
                id = 2, title = "甲板跑步", categoryId = 1, deck = null, zone = null,
                placeName = null, note = null, isCompleted = false, createdAt = 444,
            ),
        )
    }

    private fun newRepo(
        voyageDao: FakeVoyageDao = FakeVoyageDao(),
        eventDao: FakeEventDao = FakeEventDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        pixieDustDao: FakePixieDustDao = FakePixieDustDao(),
        pixieLevelDao: FakePixieLevelDao = FakePixieLevelDao(),
        wishlistDao: FakeWishlistDao = FakeWishlistDao(),
        archiveDao: FakeVoyageArchiveDao = FakeVoyageArchiveDao(),
    ) = VoyageArchiveRepository(
        voyageDao = voyageDao,
        eventDao = eventDao,
        categoryDao = categoryDao,
        pixieDustDao = pixieDustDao,
        pixieLevelDao = pixieLevelDao,
        wishlistDao = wishlistDao,
        voyageArchiveDao = archiveDao,
    )

    // ---- 用例 ----

    @Test
    fun createArchive_storesFullSnapshot_roundTripsViaGetSnapshot() = runBlocking {
        val voyageDao = FakeVoyageDao()
        val eventDao = FakeEventDao()
        val categoryDao = FakeCategoryDao()
        val pixieDustDao = FakePixieDustDao()
        val pixieLevelDao = FakePixieLevelDao()
        val wishlistDao = FakeWishlistDao()
        val archiveDao = FakeVoyageArchiveDao()
        seedAll(voyageDao, eventDao, categoryDao, pixieDustDao, pixieLevelDao, wishlistDao)
        val repo = newRepo(voyageDao, eventDao, categoryDao, pixieDustDao, pixieLevelDao, wishlistDao, archiveDao)

        val id = repo.createArchive(nowMs = 1_000L)
        assertNotNull(id)
        assertEquals(1L, id)

        // 元信息列：联程 3+4 → "3+4夜 · 共7晚"
        val board = LocalDate.of(2026, 9, 7)
        val entity = archiveDao.getById(id!!)!!
        assertEquals("Disney Adventure", entity.shipName)
        assertEquals(board, entity.boardDate)
        assertEquals(board.plusDays(7), entity.disembarkDate)
        assertEquals("3+4夜 · 共7晚", entity.nightsSummary)
        assertEquals(1_000L, entity.createdAt)
        assertEquals(1L, entity.id)

        // 快照逐字段与业务表当前内容一致（含序列化/反解析全程）
        val expected = ArchiveSnapshot(
            voyage = voyageDao.voyage!!.toDomain(),
            categories = categoryDao.categories.map { it.toDomain() },
            events = eventDao.events.map { it.toDomain() },
            dayPlans = voyageDao.dayPlans.map { it.toDomain() },
            pixieLevels = pixieLevelDao.levels.map { it.toDomain() },
            pixieDust = pixieDustDao.records.map { it.toDomain() },
            wishlist = wishlistDao.items.map { it.toDomain() },
        )
        assertEquals(expected, repo.getSnapshot(id))
    }

    @Test
    fun createArchive_withoutVoyage_returnsNull() = runBlocking {
        val archiveDao = FakeVoyageArchiveDao()
        val repo = newRepo(archiveDao = archiveDao)

        // 无航次（未创建/已重建清空）没有内容可存
        assertNull(repo.createArchive(nowMs = 1L))
        assertEquals(0, archiveDao.rows.size)
    }

    @Test
    fun createArchive_singleLeg_nightsSummaryIsNightsOnly() = runBlocking {
        val voyageDao = FakeVoyageDao()
        val board = LocalDate.of(2026, 9, 10)
        // 单程 5 天 = 4 晚
        voyageDao.voyage = VoyageEntity(
            id = 1,
            shipName = "Disney Adventure",
            boardDate = board,
            boardTime = LocalTime.of(13, 0),
            disembarkDate = board.plusDays(4),
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = 19,
            legNights = null,
        )
        val archiveDao = FakeVoyageArchiveDao()
        val repo = newRepo(voyageDao = voyageDao, archiveDao = archiveDao)

        val id = repo.createArchive(nowMs = 1L)
        assertNotNull(id)
        assertEquals("4夜", archiveDao.getById(id!!)!!.nightsSummary)
    }

    @Test
    fun deleteArchive_removesRow() = runBlocking {
        val voyageDao = FakeVoyageDao()
        seedAll(voyageDao)
        val archiveDao = FakeVoyageArchiveDao()
        val repo = newRepo(voyageDao = voyageDao, archiveDao = archiveDao)

        val id = repo.createArchive(nowMs = 1L)!!
        val otherId = repo.createArchive(nowMs = 2L)!!
        assertEquals(2, archiveDao.rows.size)

        repo.deleteArchive(id)
        assertNull(archiveDao.getById(id))
        assertEquals(1, archiveDao.rows.size)
        assertEquals(otherId, archiveDao.rows.single().id)

        // 已删的快照读不到了；不存在的 id 也是 null
        assertNull(repo.getSnapshot(id))
        assertNull(repo.getSnapshot(999L))
    }
}
