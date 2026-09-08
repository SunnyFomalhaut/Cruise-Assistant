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
        override suspend fun observeDayPlansOnce(): List<DayPlanEntity> = dayPlans
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
}
