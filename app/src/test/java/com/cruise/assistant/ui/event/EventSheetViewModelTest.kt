package com.cruise.assistant.ui.event

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
import com.cruise.assistant.data.repo.LocationPresetRepository
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.reminder.ReminderRescheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * EventSheetViewModel 候场时间保存行为。
 * ReminderScheduler 依赖 Android Context 无法在 JVM 构造，save() 依赖的
 * 重排回调用 ReminderRescheduler lambda 替身；协程作用域走 WishlistViewModel
 * 同款注入模式（Unconfined 让 save() 同步跑完）。
 */
class EventSheetViewModelTest {

    private val showCategory = CategoryEntity(
        id = 1L,
        name = "演出",
        colorArgb = 0xFFF5B301.toInt(),
        iconKey = "show",
        parentId = null,
        isBuiltIn = true,
        isArchived = false,
        sortOrder = 0,
    )

    private val mealCategory = CategoryEntity(
        id = 2L,
        name = "餐食",
        colorArgb = 0xFF9B5DE5.toInt(),
        iconKey = "meal",
        parentId = null,
        isBuiltIn = true,
        isArchived = false,
        sortOrder = 1,
    )

    /** 带内存 store 的 Fake：addEvent/updateEvent 的 lane 重算需要 getByDate/getById 读回真实数据 */
    private class FakeEventDao : EventDao {
        val inserted = mutableListOf<EventEntity>()
        val updated = mutableListOf<EventEntity>()
        private val store = LinkedHashMap<Long, EventEntity>()
        private var nextId = 1L

        fun seed(entity: EventEntity) {
            store[entity.id] = entity
            nextId = maxOf(nextId, entity.id + 1L)
        }

        override fun observeByDate(date: LocalDate): Flow<List<EventEntity>> = emptyFlow()
        override suspend fun getByDate(date: LocalDate): List<EventEntity> =
            store.values.filter { it.date == date }

        override suspend fun getAll(): List<EventEntity> = store.values.toList()
        override suspend fun getById(id: Long): EventEntity? = store[id]
        override suspend fun getByGroup(groupId: String): List<EventEntity> =
            store.values.filter { it.recurrenceGroupId == groupId }

        override suspend fun getByPresetKey(key: String): List<EventEntity> =
            store.values.filter { it.presetKey == key }

        override fun observeDatesWithEvents(): Flow<List<LocalDate>> = emptyFlow()

        override suspend fun insert(event: EventEntity): Long {
            val withId = event.copy(id = nextId++)
            store[withId.id] = withId
            inserted += withId
            return withId.id
        }

        override suspend fun insertAll(events: List<EventEntity>): List<Long> = events.map { insert(it) }

        override suspend fun update(event: EventEntity) {
            store[event.id] = event
            updated += event
        }

        override suspend fun updateAll(events: List<EventEntity>) = events.forEach { update(it) }
        override suspend fun delete(event: EventEntity) {}
        override suspend fun deleteGroup(groupId: String) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {}
        override suspend fun setActive(id: Long, active: Boolean) {}
        override suspend fun countByCategory(categoryId: Long): Int = 0
        override suspend fun clearAll() {}
    }

    private class FakeCategoryDao(
        private val items: List<CategoryEntity>,
    ) : CategoryDao {
        override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(items)
        override suspend fun observeAllOnce(): List<CategoryEntity> = items
        override fun observeActive(): Flow<List<CategoryEntity>> = flowOf(items.filter { !it.isArchived })
        override suspend fun getById(id: Long): CategoryEntity? = items.firstOrNull { it.id == id }
        override suspend fun count(): Int = items.size
        override suspend fun insert(category: CategoryEntity): Long = 1L
        override suspend fun insertAll(categories: List<CategoryEntity>) {}
        override suspend fun update(category: CategoryEntity) {}
        override suspend fun delete(category: CategoryEntity) {}
        override suspend fun clearAll() {}
    }

    private class FakeVoyageDao : VoyageDao {
        override fun observeVoyage(): Flow<VoyageEntity?> = emptyFlow()
        override suspend fun getVoyage(): VoyageEntity? = null
        override suspend fun upsertVoyage(voyage: VoyageEntity) {}
        override suspend fun clearVoyage() {}
        override fun observeDayPlans(): Flow<List<DayPlanEntity>> = emptyFlow()
        override suspend fun getDayPlansOnce(): List<DayPlanEntity> = emptyList()
        override fun observeDayPlan(date: LocalDate): Flow<DayPlanEntity?> = emptyFlow()
        override suspend fun upsertDayPlans(plans: List<DayPlanEntity>) {}
        override suspend fun upsertDayPlan(plan: DayPlanEntity) {}
        override suspend fun deleteDayPlan(plan: DayPlanEntity) {}
        override suspend fun clearDayPlans() {}
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
        override fun observeAll(): Flow<List<WishlistItemEntity>> = emptyFlow()
        override suspend fun getAll(): List<WishlistItemEntity> = emptyList()
        override suspend fun getById(id: Long): WishlistItemEntity? = null
        override suspend fun insert(item: WishlistItemEntity): Long = 1L
        override suspend fun update(item: WishlistItemEntity) {}
        override suspend fun delete(item: WishlistItemEntity) {}
        override suspend fun toggleCompleted(id: Long, completed: Boolean) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {}
    }

    /** 类别列表 show 在前：新建事件的 selectedCategoryId 会 fallback 到第一个类别 */
    private fun buildViewModel(
        eventDao: FakeEventDao = FakeEventDao(),
        editingEventId: Long = 0L,
    ): EventSheetViewModel {
        val fakeCategoryDao = FakeCategoryDao(listOf(showCategory, mealCategory))
        return EventSheetViewModel(
            eventRepository = EventRepository(eventDao, FakeVoyageDao(), fakeCategoryDao),
            categoryRepository = CategoryRepository(fakeCategoryDao, eventDao, FakeWishlistDao()),
            locationPresetRepository = LocationPresetRepository(FakeLocationPresetDao()),
            voyageRepository = VoyageRepository(
                voyageDao = FakeVoyageDao(),
                eventDao = eventDao,
                categoryDao = fakeCategoryDao,
                locationPresetDao = FakeLocationPresetDao(),
                pixieDustDao = FakePixieDustDao(),
                pixieLevelDao = FakePixieLevelDao(),
                wishlistDao = FakeWishlistDao(),
            ),
            reminderScheduler = ReminderRescheduler { /* 测试替身：不排程 */ },
            date = LocalDate.of(2026, 9, 12),
            defaultStartMinute = 10 * 60,
            editingEventId = editingEventId,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )
    }

    @Test
    fun `default waitMinutes is 30`() {
        val vm = buildViewModel()
        assertEquals(30, vm.state.value.waitMinutes)
    }

    @Test
    fun `save with zero wait on show category persists 0`() {
        val eventDao = FakeEventDao()
        val vm = buildViewModel(eventDao = eventDao)

        // 新建事件默认选中 show 类别（类别列表首位），候场选"不候场"
        vm.onWaitMinutesChange(0)
        var done = false
        vm.save(RecurrenceScope.THIS_DAY_ONLY) { done = true }

        assertEquals(1, eventDao.inserted.size)
        val inserted = eventDao.inserted.single().toDomain()
        assertEquals(0, inserted.waitMinutes)
        assertEquals(true, done)
    }

    @Test
    fun `save on non-show category writes null even when waitMinutes is 0`() {
        val eventDao = FakeEventDao()
        val vm = buildViewModel(eventDao = eventDao)

        // 与 WishlistViewModel.convertToEvent 同一约定：非 show 类别持久化 waitMinutes=null，
        // 状态里残留的 0 不得泄露到表单/数据
        vm.onCategorySelect(mealCategory.id)
        vm.onWaitMinutesChange(0)
        vm.save(RecurrenceScope.THIS_DAY_ONLY) {}

        assertEquals(1, eventDao.inserted.size)
        assertNull(eventDao.inserted.single().toDomain().waitMinutes)
    }

    @Test
    fun `edit show event with persisted zero wait keeps 0 instead of derived default`() {
        val eventDao = FakeEventDao()
        eventDao.seed(
            EventEntity(
                id = 7L,
                date = LocalDate.of(2026, 9, 12),
                startTime = LocalTime.of(20, 30),
                durationMinutes = 60,
                categoryId = 1L,
                title = "晚间大秀",
                deck = 6,
                zone = "BOW",
                placeName = "华特迪士尼剧院 Walt Disney Theatre",
                lane = 0,
                recurrenceGroupId = null,
                reminderMinutesBefore = null,
                isSystemGenerated = false,
                waitMinutes = 0, // 用户明确选了"不候场"
            ),
        )

        // `event.waitMinutes ?: effectiveWaitMinutes` 必须把 0 视为"已设置"，
        // 不能回落到地点派生的 45 分钟，否则保存会把用户选择覆盖掉
        val vm = buildViewModel(eventDao = eventDao, editingEventId = 7L)
        assertEquals(0, vm.state.value.waitMinutes)
    }
}
