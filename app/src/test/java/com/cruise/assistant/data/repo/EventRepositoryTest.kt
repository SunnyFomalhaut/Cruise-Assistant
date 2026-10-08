package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.domain.CruiseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventRepositoryTest {

    private class FakeEventDao : EventDao {
        val events = mutableMapOf<Long, EventEntity>()

        override fun observeByDate(date: LocalDate): Flow<List<EventEntity>> = emptyFlow()
        override suspend fun getByDate(date: LocalDate): List<EventEntity> =
            events.values.filter { it.date == date }

        override suspend fun getAll(): List<EventEntity> = events.values.toList()
        override suspend fun getById(id: Long): EventEntity? = events[id]
        override suspend fun getByGroup(groupId: String): List<EventEntity> =
            events.values.filter { it.recurrenceGroupId == groupId }

        override suspend fun getByPresetKey(key: String): List<EventEntity> =
            events.values.filter { it.presetKey == key }

        override fun observeDatesWithEvents(): Flow<List<LocalDate>> = emptyFlow()

        override suspend fun insert(event: EventEntity): Long {
            val id = if (event.id == 0L) (events.keys.maxOrNull() ?: 0L) + 1 else event.id
            events[id] = event.copy(id = id)
            return id
        }

        override suspend fun insertAll(events: List<EventEntity>): List<Long> =
            events.map { insert(it) }

        override suspend fun update(event: EventEntity) {
            events[event.id] = event
        }

        override suspend fun updateAll(events: List<EventEntity>) {
            events.forEach { update(it) }
        }

        override suspend fun delete(event: EventEntity) {
            events.remove(event.id)
        }

        override suspend fun deleteGroup(groupId: String) {
            val toRemove = events.values.filter { it.recurrenceGroupId == groupId }.map { it.id }
            toRemove.forEach { events.remove(it) }
        }

        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {
            events.values.filter { it.categoryId == fromCategoryId }.forEach {
                events[it.id] = it.copy(categoryId = toCategoryId)
            }
        }

        override suspend fun setActive(id: Long, active: Boolean) {
            events[id]?.let { events[id] = it.copy(isActive = active) }
        }

        override suspend fun countByCategory(categoryId: Long): Int =
            events.values.count { it.categoryId == categoryId }

        override suspend fun clearAll() {
            events.clear()
        }
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
        override suspend fun clearDayPlans() {}
        override suspend fun deleteDayPlan(plan: DayPlanEntity) {}
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

    @Test
    fun `updateEvent preserves existing lane when timing is unchanged`() = runBlocking {
        val eventDao = FakeEventDao()
        val voyageDao = FakeVoyageDao()
        val categoryDao = FakeCategoryDao()
        val repo = EventRepository(eventDao, voyageDao, categoryDao)

        val date = LocalDate.of(2026, 8, 24)
        val initialEntity = EventEntity(
            id = 42L,
            date = date,
            startTime = LocalTime.of(14, 0),
            durationMinutes = 45,
            categoryId = 1L,
            title = "迪士尼音乐会",
            deck = 4,
            zone = "MID",
            placeName = "华特迪士尼剧院",
            lane = 1, // Currently in lane 1
            recurrenceGroupId = null,
            reminderMinutesBefore = null,
            isSystemGenerated = false,
        )
        eventDao.insert(initialEntity)

        // Updated event has note added and title updated, lane passed as 0, but time unchanged
        val updatedDomain = CruiseEvent(
            id = 42L,
            date = date,
            startTime = LocalTime.of(14, 0),
            durationMinutes = 45,
            categoryId = 1L,
            title = "迪士尼音乐会 - 修改版",
            deck = 4,
            zone = com.cruise.assistant.domain.ShipZone.MID,
            placeName = "华特迪士尼剧院",
            lane = 0,
            note = "提前到达选座",
        )

        repo.updateEvent(updatedDomain, RecurrenceScope.THIS_DAY_ONLY)

        val saved = eventDao.getById(42L)
        assertEquals("迪士尼音乐会 - 修改版", saved?.title)
        assertEquals("提前到达选座", saved?.note)
        assertEquals(1, saved?.lane) // Lane 1 must be preserved!
    }
}
