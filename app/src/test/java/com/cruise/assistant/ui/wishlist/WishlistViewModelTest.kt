package com.cruise.assistant.ui.wishlist

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.dao.PixieLevelDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.dao.WishlistDao
import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import com.cruise.assistant.data.db.entity.PixieDustEntity
import com.cruise.assistant.data.db.entity.PixieLevelEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.data.db.entity.WishlistItemEntity
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.data.repo.WishlistRepository
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.WishlistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WishlistViewModelTest {

    private class FakeEventDao : EventDao {
        val insertedEvents = mutableListOf<EventEntity>()

        override fun observeByDate(date: LocalDate): Flow<List<EventEntity>> = emptyFlow()
        override suspend fun getByDate(date: LocalDate): List<EventEntity> = emptyList()
        override suspend fun getAll(): List<EventEntity> = emptyList()
        override suspend fun getById(id: Long): EventEntity? = null
        override suspend fun getByGroup(groupId: String): List<EventEntity> = emptyList()
        override suspend fun getByPresetKey(key: String): List<EventEntity> = emptyList()
        override fun observeDatesWithEvents(): Flow<List<LocalDate>> = emptyFlow()

        override suspend fun insert(event: EventEntity): Long {
            insertedEvents.add(event)
            return insertedEvents.size.toLong()
        }

        override suspend fun insertAll(events: List<EventEntity>): List<Long> {
            insertedEvents.addAll(events)
            return events.map { insertedEvents.size.toLong() }
        }

        override suspend fun update(event: EventEntity) {}
        override suspend fun updateAll(events: List<EventEntity>) {}
        override suspend fun delete(event: EventEntity) {}
        override suspend fun deleteGroup(groupId: String) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {}
        override suspend fun setActive(id: Long, active: Boolean) {}
        override suspend fun countByCategory(categoryId: Long): Int = 0
        override suspend fun clearAll() {}
    }

    private class FakeWishlistDao : WishlistDao {
        var toggledId: Long? = null
        var toggledCompleted: Boolean? = null

        override fun observeAll(): Flow<List<WishlistItemEntity>> = emptyFlow()
        override suspend fun getAll(): List<WishlistItemEntity> = emptyList()
        override suspend fun getById(id: Long): WishlistItemEntity? = null
        override suspend fun insert(item: WishlistItemEntity): Long = 1L
        override suspend fun update(item: WishlistItemEntity) {}
        override suspend fun delete(item: WishlistItemEntity) {}
        override suspend fun toggleCompleted(id: Long, completed: Boolean) {
            toggledId = id
            toggledCompleted = completed
        }
    }

    private class FakeCategoryDao : CategoryDao {
        override fun observeAll(): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun observeAllOnce(): List<CategoryEntity> = emptyList()
        override fun observeActive(): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun getById(id: Long): CategoryEntity? = null
        override suspend fun count(): Int = 0
        override suspend fun insert(category: CategoryEntity): Long = 1L
        override suspend fun insertAll(categories: List<CategoryEntity>) {}
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

    private class FakeVoyageDao(
        val voyageFlow: Flow<VoyageEntity?> = emptyFlow(),
    ) : VoyageDao {
        override fun observeVoyage(): Flow<VoyageEntity?> = voyageFlow
        override suspend fun getVoyage(): VoyageEntity? = null
        override suspend fun upsertVoyage(voyage: VoyageEntity) {}
        override suspend fun clearVoyage() {}
        override fun observeDayPlans(): Flow<List<DayPlanEntity>> = emptyFlow()
        override suspend fun observeDayPlansOnce(): List<DayPlanEntity> = emptyList()
        override fun observeDayPlan(date: LocalDate): Flow<DayPlanEntity?> = emptyFlow()
        override suspend fun upsertDayPlans(plans: List<DayPlanEntity>) {}
        override suspend fun upsertDayPlan(plan: DayPlanEntity) {}
        override suspend fun deleteDayPlan(plan: DayPlanEntity) {}
        override suspend fun clearDayPlans() {}
    }

    @Test
    fun convertToEvent_addsEventWithWaitMinutes_andMarksWishlistCompleted() {
        val fakeWishlistDao = FakeWishlistDao()
        val fakeCategoryDao = FakeCategoryDao()
        val fakeEventDao = FakeEventDao()
        val fakeVoyageDao = FakeVoyageDao()
        val fakeLocationPresetDao = FakeLocationPresetDao()
        val fakePixieDustDao = FakePixieDustDao()
        val fakePixieLevelDao = FakePixieLevelDao()

        val wishlistRepo = WishlistRepository(fakeWishlistDao, fakeCategoryDao)
        val categoryRepo = CategoryRepository(fakeCategoryDao, fakeEventDao)
        val eventRepo = EventRepository(fakeEventDao, fakeVoyageDao, fakeCategoryDao)
        val voyageRepo = VoyageRepository(
            voyageDao = fakeVoyageDao,
            eventDao = fakeEventDao,
            categoryDao = fakeCategoryDao,
            locationPresetDao = fakeLocationPresetDao,
            pixieDustDao = fakePixieDustDao,
            pixieLevelDao = fakePixieLevelDao,
            wishlistDao = fakeWishlistDao,
        )

        val vm = WishlistViewModel(
            wishlistRepository = wishlistRepo,
            categoryRepository = categoryRepo,
            eventRepository = eventRepo,
            voyageRepository = voyageRepo,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )

        val item = WishlistItem(
            id = 42L,
            title = "回忆：瓦力与伊芙 Remember",
            categoryId = 3L,
            deck = 6,
            zone = ShipZone.BOW,
            placeName = "华特迪士尼剧院 Walt Disney Theatre",
            note = "建议提前45分钟候场",
            isCompleted = false,
        )

        var successCalled = false
        vm.convertToEvent(
            item = item,
            date = LocalDate.of(2026, 9, 12),
            startTime = LocalTime.of(20, 30),
            durationMinutes = 60,
            onSuccess = { successCalled = true },
        )

        // 验证向 eventRepo 插入了对应的 CruiseEvent
        assertEquals(1, fakeEventDao.insertedEvents.size)
        val inserted = fakeEventDao.insertedEvents.first().toDomain()
        assertEquals(item.title, inserted.title)
        assertEquals(LocalDate.of(2026, 9, 12), inserted.date)
        assertEquals(LocalTime.of(20, 30), inserted.startTime)
        assertEquals(60, inserted.durationMinutes)
        assertEquals(6, inserted.deck)
        assertEquals(ShipZone.BOW, inserted.zone)
        assertEquals("华特迪士尼剧院 Walt Disney Theatre", inserted.placeName)
        assertEquals(45, inserted.waitMinutes) // 华特迪士尼剧院自动解析 45 分钟候场
        assertEquals(item.note, inserted.note)

        // 验证勾选了心愿完成
        assertEquals(42L, fakeWishlistDao.toggledId)
        assertEquals(true, fakeWishlistDao.toggledCompleted)
        assertTrue(successCalled)
    }

    @Test
    fun activeVoyageDates_mapsDatesFromVoyageRepository() {
        val fakeWishlistDao = FakeWishlistDao()
        val fakeCategoryDao = FakeCategoryDao()
        val fakeEventDao = FakeEventDao()
        val fakeLocationPresetDao = FakeLocationPresetDao()
        val fakePixieDustDao = FakePixieDustDao()
        val fakePixieLevelDao = FakePixieLevelDao()
        val voyageFlow = MutableStateFlow<VoyageEntity?>(
            VoyageEntity(
                id = 1,
                shipName = "Disney Adventure",
                boardDate = LocalDate.of(2026, 9, 10),
                boardTime = LocalTime.of(13, 0),
                disembarkDate = LocalDate.of(2026, 9, 13),
                disembarkTime = LocalTime.of(9, 0),
                maxDeck = 18,
            )
        )
        val fakeVoyageDao = FakeVoyageDao(voyageFlow = voyageFlow)

        val wishlistRepo = WishlistRepository(fakeWishlistDao, fakeCategoryDao)
        val categoryRepo = CategoryRepository(fakeCategoryDao, fakeEventDao)
        val eventRepo = EventRepository(fakeEventDao, fakeVoyageDao, fakeCategoryDao)
        val voyageRepo = VoyageRepository(
            voyageDao = fakeVoyageDao,
            eventDao = fakeEventDao,
            categoryDao = fakeCategoryDao,
            locationPresetDao = fakeLocationPresetDao,
            pixieDustDao = fakePixieDustDao,
            pixieLevelDao = fakePixieLevelDao,
            wishlistDao = fakeWishlistDao,
        )

        val vm = WishlistViewModel(
            wishlistRepository = wishlistRepo,
            categoryRepository = categoryRepo,
            eventRepository = eventRepo,
            voyageRepository = voyageRepo,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )

        val collectJob = vm.activeVoyageDates.launchIn(CoroutineScope(Dispatchers.Unconfined))
        val dates = vm.activeVoyageDates.value
        collectJob.cancel()

        assertEquals(4, dates.size)
        assertEquals(LocalDate.of(2026, 9, 10), dates[0])
        assertEquals(LocalDate.of(2026, 9, 11), dates[1])
        assertEquals(LocalDate.of(2026, 9, 12), dates[2])
        assertEquals(LocalDate.of(2026, 9, 13), dates[3])
    }
}
