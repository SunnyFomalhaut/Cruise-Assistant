package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.assignLane
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.expandRecurrence
import com.cruise.assistant.domain.findConflicts
import com.cruise.assistant.domain.startMinute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/** 编辑/删除循环组内事件时的作用域 */
enum class RecurrenceScope { THIS_DAY_ONLY, WHOLE_GROUP }

class EventRepository(
    private val eventDao: EventDao,
    private val voyageDao: VoyageDao,
) {
    fun observeByDate(date: LocalDate): Flow<List<CruiseEvent>> =
        eventDao.observeByDate(date).map { list -> list.map { it.toDomain() } }

    fun observeDatesWithEvents(): Flow<List<LocalDate>> = eventDao.observeDatesWithEvents()

    suspend fun getById(id: Long): CruiseEvent? = eventDao.getById(id)?.toDomain()

    /**
     * 新增事件。recurring=true 时按需求6 展开到下船日前一天（下船日不含）。
     * 每一天各自跑 assignLane，保证 3 栏不重叠。
     * @return 实际写入的条数
     */
    suspend fun addEvent(event: CruiseEvent, recurring: Boolean): Int {
        val voyage = voyageDao.getVoyage()?.toDomain()
        val copies = if (recurring && voyage != null) {
            expandRecurrence(event, voyage.disembarkDate, UUID.randomUUID().toString())
        } else {
            listOf(event.copy(id = 0))
        }
        copies.forEach { copy ->
            val sameDay = eventDao.getByDate(copy.date).map { it.toDomain() }
            val lane = assignLane(copy.startMinute, copy.endMinute, sameDay)
            eventDao.insert(copy.copy(lane = lane).toEntity())
        }
        return copies.size
    }

    /**
     * 更新事件。WHOLE_GROUP 时把同组所有事件的时间/内容/地点/类别一起改，
     * 但各自重算 lane（不同日期的邻居不同）。
     */
    suspend fun updateEvent(event: CruiseEvent, scope: RecurrenceScope) {
        val groupId = event.recurrenceGroupId
        val targets = if (scope == RecurrenceScope.WHOLE_GROUP && groupId != null) {
            eventDao.getByGroup(groupId).map { it.toDomain() }
        } else {
            listOf(event)
        }
        targets.forEach { target ->
            val merged = event.copy(
                id = target.id,
                date = target.date,
                recurrenceGroupId = target.recurrenceGroupId,
                isSystemGenerated = target.isSystemGenerated,
            )
            val sameDay = eventDao.getByDate(merged.date).map { it.toDomain() }
            val lane = assignLane(merged.startMinute, merged.endMinute, sameDay, excludeId = merged.id)
            eventDao.update(merged.copy(lane = lane).toEntity())
        }
    }

    /** 删除。系统生成的上船/下船事件不允许删除，静默忽略。 */
    suspend fun deleteEvent(event: CruiseEvent, scope: RecurrenceScope) {
        if (event.isSystemGenerated) return
        val groupId = event.recurrenceGroupId
        if (scope == RecurrenceScope.WHOLE_GROUP && groupId != null) {
            eventDao.deleteGroup(groupId)
        } else {
            eventDao.delete(event.toEntity())
        }
    }

    /** 长按卡片手动换栏（需求4） */
    suspend fun moveToLane(eventId: Long, lane: Int) {
        val entity = eventDao.getById(eventId) ?: return
        eventDao.update(entity.copy(lane = lane))
    }

    /** 需求8：定稿前的冲突检查 */
    suspend fun conflictsOn(date: LocalDate): List<Pair<CruiseEvent, CruiseEvent>> =
        findConflicts(eventDao.getByDate(date).map { it.toDomain() })

    /** 提醒排程用：取所有事件供 scheduler 过滤未来项 */
    suspend fun getAllEvents(): List<CruiseEvent> = eventDao.getAll().map { it.toDomain() }

    suspend fun getByDate(date: LocalDate): List<CruiseEvent> =
        eventDao.getByDate(date).map { it.toDomain() }
}
