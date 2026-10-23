package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DISNEY_ADVENTURE_SHOWS
import com.cruise.assistant.domain.SHOW_WAIT_MINUTES
import com.cruise.assistant.domain.assignLane
import com.cruise.assistant.domain.assignLanePreserving
import com.cruise.assistant.domain.assignShowLane
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.expandRecurrence
import com.cruise.assistant.domain.findConflicts
import com.cruise.assistant.domain.isSameShowAs
import com.cruise.assistant.domain.pickDinnerLinkedTime
import com.cruise.assistant.domain.resolveDefaultWaitMinutes
import com.cruise.assistant.domain.showEffectiveRange
import com.cruise.assistant.domain.showRangeOf
import com.cruise.assistant.domain.startMinute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** 编辑/删除循环组内事件时的作用域 */
enum class RecurrenceScope { THIS_DAY_ONLY, WHOLE_GROUP }

class EventRepository(
    private val eventDao: EventDao,
    private val voyageDao: VoyageDao,
    private val categoryDao: CategoryDao,
    // 可注入的事务执行器：生产环境由 AppContainer 传 db.withTransaction，
    // 单元测试 Fake DAO 没有 Room 数据库，默认直跑（不包事务）语义正确。
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
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
        val showIds = showCategoryIds()
        // 循环展开出的多天插入包进事务：部分成功会让循环组"缺天"且 groupId 已写死，无法事后补。
        runInTransaction {
            copies.forEach { copy ->
                val sameDay = eventDao.getByDate(copy.date).map { it.toDomain() }.filter { it.isActive }
                // 放置区间也用扩展区间（show 类别）：否则演出与普通事件等待段重叠时会叠进同一栏
                val range = showEffectiveRange(copy, isShow = copy.categoryId in showIds)
                val lane = assignLane(range.first, range.second, sameDay, rangeOf = showRangeOf(showIds))
                eventDao.insert(copy.copy(lane = lane).toEntity())
            }
        }
        return copies.size
    }

    /**
     * 更新事件。WHOLE_GROUP 时把同组所有事件的时间/内容/地点/类别一起改。
     * spec：编辑时若时间未变则保留原 lane，只有时间真的变了才重算——避免用户只改标题/
     * 类别/地点却发现卡片跳栏。WHOLE_GROUP 下每个成员各自比较自己的原时间，
     * 不与某个共享参照事件比较。
     */
    suspend fun updateEvent(event: CruiseEvent, scope: RecurrenceScope) {
        val groupId = event.recurrenceGroupId
        val targets = if (scope == RecurrenceScope.WHOLE_GROUP && groupId != null) {
            // 需求8：已定稿的日子是只读的——WHOLE_GROUP 编辑从别的（仍在规划中的）日子发起时，
            // 不能悄悄改写某个已定稿成员的行，否则破坏"定稿=只读"的承诺，且不会重新触发冲突检查。
            // 但用户直接点开来编辑的那一条必须始终生效——即使它自己所在的日子已定稿；
            // Task 7/8 已经在 UI 层挡掉了"打开已定稿日子的卡片"这条路径，这里只过滤*其他*成员。
            val finalizedDates = voyageDao.getDayPlansOnce()
                .filter { it.finalized }
                .mapTo(mutableSetOf()) { it.date }
            eventDao.getByGroup(groupId).map { it.toDomain() }
                .filter { it.id == event.id || it.date !in finalizedDates }
        } else {
            val existing = eventDao.getById(event.id)?.toDomain()
            listOf(existing ?: event)
        }
        // 整组更新包进事务：WHOLE_GROUP 编辑中途失败会留下"改了一半的循环组"。
        runInTransaction {
            targets.forEach { target ->
                val merged = event.copy(
                    id = target.id,
                    date = target.date,
                    // THIS_DAY_ONLY 编辑必须让这一天从循环组里"脱钩"永久独立：否则这天的自定义
                    // 标题/地点/类别会在之后某次从别的仍属于该组的日子发起的 WHOLE_GROUP 编辑中被
                    // 静默覆盖回去（复现场景：Day2-5 循环早餐，Day3 仅改本日改了地点，随后从 Day2
                    // 发起修改所有重复项——若不脱钩，Day3 的改动会被 Day2 的值覆盖）。非循环事件
                    // target.recurrenceGroupId 本就是 null，这里置 null 无影响；系统事件不受循环
                    // 编辑入口触达，同样安全。
                    recurrenceGroupId = if (scope == RecurrenceScope.THIS_DAY_ONLY) {
                        null
                    } else {
                        target.recurrenceGroupId
                    },
                    isSystemGenerated = target.isSystemGenerated,
                    // 保留目标行的激活状态：UI 层构造的 event 不带 isActive（恒为 true），
                    // 若不保留，编辑会静默"复活"被停用的事件（WHOLE_GROUP 时整组已停用成员一起复活）。
                    isActive = target.isActive,
                )
                val showIds = showCategoryIds()
                val oldRange = showEffectiveRange(target, isShow = target.categoryId in showIds)
                val newRange = showEffectiveRange(merged, isShow = merged.categoryId in showIds)
                val timingUnchanged = oldRange == newRange
                val lane = if (timingUnchanged) {
                    target.lane
                } else {
                    val sameDay = eventDao.getByDate(merged.date).map { it.toDomain() }.filter { it.isActive }
                    assignLane(
                        newRange.first,
                        newRange.second,
                        sameDay,
                        excludeId = merged.id,
                        rangeOf = showRangeOf(showIds),
                    )
                }
                eventDao.update(merged.copy(lane = lane).toEntity())
            }
        }
    }

    /** 删除。系统生成的登船/下船事件不允许删除，静默忽略。 */
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

    /** 切换事件活动/非活动状态（仅本日，不影响循环组其他成员） */
    suspend fun setActive(eventId: Long, active: Boolean) {
        if (active) {
            // 重新激活：优先保留原栏位；只有原栏位与激活期间新增/变动的事件冲突时才重算
            val entity = eventDao.getById(eventId) ?: return
            val sameDay = eventDao.getByDate(entity.date)
                .map { it.toDomain() }
                .filter { it.isActive && it.id != eventId }
            val event = entity.toDomain()
            val showIds = showCategoryIds()
            val rangeOf = showRangeOf(showIds)
            val (start, end) = rangeOf(event)
            val lane = assignLanePreserving(
                start,
                end,
                sameDay,
                preferredLane = event.lane,
                excludeId = event.id,
                rangeOf = rangeOf,
            )
            eventDao.update(entity.copy(isActive = true, lane = lane))
        } else {
            // 停用：保留原车道仅供显示
            eventDao.setActive(eventId, false)
        }
    }

    /**
     * 只看本场：将全航次中与 [target] 同一演出的其他场次全部设为非活动（isActive=false），并将 [target] 设为活动（isActive=true）。
     * 同一演出判断条件：除时间（startTime/durationMinutes/date）外其他一致（categoryId, title, deck, zone, placeName）。
     */
    suspend fun keepOnlyThisShow(target: CruiseEvent) {
        // "停用其他场次 + 激活目标场"包进事务：两步之间失败会让同一演出所有场次都处于停用态。
        runInTransaction {
            val allEvents = eventDao.getAll().map { it.toDomain() }
            val matches = allEvents.filter { it.isSameShowAs(target) }
            // 先停用其他场次（释放它们的栏位，供目标场复活时判断冲突）
            matches.filter { it.id != target.id && it.isActive }.forEach {
                eventDao.setActive(it.id, false)
            }
            // 再激活目标场：统一走 setActive（内含"优先保留原栏位"逻辑）
            setActive(target.id, true)
        }
    }

    /**
     * 设置页切换晚餐时段：把所有 presetKey="dinner" 的事件 startTime 统一更新为 newTime。
     * durationMinutes 保持不变（75 分钟），日期不变，lane 不变（晚餐时段通常独占
     * 自己的时间窗）。被删除过的晚餐事件不会批量"复活"——只有当前仍在表里的会被更新。
     */
    suspend fun updateDinnerEventsTime(newTime: LocalTime) {
        val dinners = eventDao.getByPresetKey("dinner")
        // 批量改时间包进事务：部分成功会出现"一部分晚餐在新时间、一部分在旧时间"的分裂态。
        runInTransaction {
            dinners.forEach { entity ->
                eventDao.update(entity.copy(startTime = newTime))
            }
        }
    }

    /**
     * 设置页切换晚餐时段：把晚餐联动演出（海洋奇遇/回忆）按新晚餐时间换场。
     * 仅当事件当前时间仍是 18:00/20:30 之一才更新——用户手动改过时间的演出不被覆盖。
     * 被删除过的演出不会"复活"（只有当前仍在表里的会被更新，与 updateDinnerEventsTime 一致）。
     */
    suspend fun resyncShowTimes(dinnerTime: LocalTime) {
        // 换场 + 逐日重排 lane 是一组相关写入，包进事务：中途失败会让联动演出
        // "时间换了但栏位没重排"，与晚餐时段视觉重叠。
        runInTransaction {
            val target = pickDinnerLinkedTime(dinnerTime)
            val dinnerLinkedShows = DISNEY_ADVENTURE_SHOWS.filter { it.dinnerLinked }
            val knownShowTimes = dinnerLinkedShows.flatMap { it.startTimes }.toSet()
            val affectedDates = mutableSetOf<LocalDate>()
            dinnerLinkedShows.map { it.key }.forEach { key ->
                eventDao.getByPresetKey(key).forEach { entity ->
                    if (entity.startTime in knownShowTimes) {
                        eventDao.update(entity.copy(startTime = target))
                        affectedDates += entity.date
                    }
                }
            }
            val presetKeyOrder = DISNEY_ADVENTURE_SHOWS.mapIndexed { idx, s -> s.key to idx }.toMap()
            val showIds = showCategoryIds()
            val rangeOf = showRangeOf(showIds)
            for (date in affectedDates) {
                val dayEvents = eventDao.getByDate(date)
                val showPresetsOnDay = dayEvents
                    .filter { it.isActive && it.presetKey != null && it.presetKey in presetKeyOrder }
                    .sortedWith(compareBy({ presetKeyOrder[it.presetKey] ?: Int.MAX_VALUE }, { it.startTime }))
                if (showPresetsOnDay.isNotEmpty()) {
                    val showPresetIds = showPresetsOnDay.mapTo(mutableSetOf()) { it.id }
                    val occupied = mutableListOf<Triple<Int, Int, Int>>()
                    dayEvents.filter { it.isActive && it.id !in showPresetIds }.forEach { other ->
                        val domain = other.toDomain()
                        val (s, e) = rangeOf(domain)
                        occupied += Triple(domain.lane, s, e)
                    }
                    val updated = showPresetsOnDay.map { entity ->
                        val startMin = entity.startTime.hour * 60 + entity.startTime.minute
                        val waitMins = entity.waitMinutes ?: resolveDefaultWaitMinutes(entity.placeName, entity.deck, entity.title, entity.presetKey)
                        val effStart = startMin - waitMins
                        val effEnd = startMin + entity.durationMinutes
                        val newLane = assignShowLane(effStart, effEnd, occupied)
                        occupied += Triple(newLane, effStart, effEnd)
                        entity.copy(lane = newLane)
                    }
                    eventDao.updateAll(updated)
                }
            }
        }
    }

    /** 需求8：定稿前的冲突检查（show 类别按扩展区间参与） */
    suspend fun conflictsOn(date: LocalDate): List<Pair<CruiseEvent, CruiseEvent>> {
        val showIds = showCategoryIds()
        val events = eventDao.getByDate(date).map { it.toDomain() }
        return findConflicts(events, rangeOf = showRangeOf(showIds))
    }

    /** 当前"演出"类别的 id 集合（iconKey == "show" 的类别） */
    private suspend fun showCategoryIds(): Set<Long> =
        categoryDao.observeAllOnce()
            .filter { it.iconKey == "show" }
            .mapTo(mutableSetOf()) { it.id }

    /** 提醒排程用：取所有事件供 scheduler 过滤未来项 */
    suspend fun getAllEvents(): List<CruiseEvent> = eventDao.getAll().map { it.toDomain() }

    suspend fun getByDate(date: LocalDate): List<CruiseEvent> =
        eventDao.getByDate(date).map { it.toDomain() }
}
