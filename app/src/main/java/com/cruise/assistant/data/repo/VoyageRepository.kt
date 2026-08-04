package com.cruise.assistant.data.repo

import com.cruise.assistant.data.BUILT_IN_CATEGORIES
import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.Voyage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/** 上船事件固定时长 15 分钟（需求7） */
private const val BOARDING_DURATION_MINUTES = 15
private const val DISEMBARK_DURATION_MINUTES = 30

class VoyageRepository(
    private val voyageDao: VoyageDao,
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val locationPresetDao: LocationPresetDao,
) {
    fun observeVoyage(): Flow<Voyage?> =
        voyageDao.observeVoyage().map { it?.toDomain() }

    suspend fun getVoyage(): Voyage? = voyageDao.getVoyage()?.toDomain()

    fun observeDayPlans(): Flow<Map<LocalDate, DayPlan>> =
        voyageDao.observeDayPlans().map { list ->
            list.associate { it.date to it.toDomain() }
        }

    /**
     * 需求7：创建航次并生成初始时间轴。
     * 1) 写 voyage  2) 每天建 DayPlan(finalized=false)
     * 3) 播种内置类别（仅当类别表为空）
     * 4) 插入上船（boardTime 起 15 分钟，不设地址）与下船（末日 07:00）两个系统事件
     */
    suspend fun createVoyage(
        shipName: String?,
        boardDate: LocalDate,
        boardTime: LocalTime,
        disembarkDate: LocalDate,
        maxDeck: Int,
    ): Voyage {
        val voyage = Voyage(
            id = 1,
            shipName = shipName?.trim()?.takeIf { it.isNotEmpty() },
            boardDate = boardDate,
            boardTime = boardTime,
            disembarkDate = disembarkDate,
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = maxDeck,
        )
        voyageDao.upsertVoyage(voyage.toEntity())
        voyageDao.upsertDayPlans(
            voyage.datesInVoyage().map { DayPlan(date = it).toEntity() },
        )

        if (categoryDao.count() == 0) {
            categoryDao.insertAll(BUILT_IN_CATEGORIES.map { it.toEntity() })
        }
        // 只查一次：类别表刚在上面播种过（或早已存在），为空是编程错误，允许 .first() 抛异常。
        val categories = categoryDao.observeAllOnce()
        val importantId = categories.firstOrNull { it.iconKey == "important" }?.id
            ?: categories.first().id

        eventDao.insertAll(
            listOf(
                CruiseEvent(
                    date = boardDate,
                    startTime = boardTime,
                    durationMinutes = BOARDING_DURATION_MINUTES,
                    categoryId = importantId,
                    title = "上船",
                    isSystemGenerated = true,
                ).toEntity(),
                CruiseEvent(
                    date = disembarkDate,
                    startTime = voyage.disembarkTime,
                    durationMinutes = DISEMBARK_DURATION_MINUTES,
                    categoryId = importantId,
                    title = "下船",
                    isSystemGenerated = true,
                ).toEntity(),
            ),
        )
        return voyage
    }

    /**
     * 修改航次日期/时间：同步 DayPlan 集合，并把上船/下船系统事件挪到新时间。
     * 落在新范围外的普通事件会被删除（调用方负责先提示用户）。
     */
    suspend fun updateVoyage(voyage: Voyage) {
        voyageDao.upsertVoyage(voyage.toEntity())

        val validDates = voyage.datesInVoyage().toSet()
        val existingPlans = voyageDao.observeDayPlansOnce().map { it.date }.toSet()
        val newPlans = validDates.filter { it !in existingPlans }.map { DayPlan(date = it).toEntity() }
        if (newPlans.isNotEmpty()) voyageDao.upsertDayPlans(newPlans)

        // 只查一次，供下面两步复用。
        val allEvents = eventDao.getAll()

        // 系统事件（上船/下船）必须排除在删除范围外：它们是"挪到新时间"而不是"落在范围外就删掉"。
        // 若先把它们删了，下面的 update(entity.copy(...)) 是按 id 匹配的 UPDATE，行已不存在，
        // 会静默影响 0 行、不抛异常——事件永久消失却毫无提示（与 LocationPresetDao 文档警告的
        // 同一类"按 id 更新命中 0 行被静默吞掉"问题）。
        allEvents
            .filter { it.date !in validDates && !it.isSystemGenerated }
            .forEach { eventDao.delete(it) }

        // 注意：这里用中文标题 "上船"/"下船" 识别系统事件，与 createVoyage() 中生成时写的标题严格耦合。
        // 后续任务如果要改名这两个系统事件的标题，必须同步修改这里的匹配，否则迁移会静默失效。
        allEvents.forEach { entity ->
            if (!entity.isSystemGenerated) return@forEach
            when (entity.title) {
                "上船" -> eventDao.update(
                    entity.copy(date = voyage.boardDate, startTime = voyage.boardTime),
                )
                "下船" -> eventDao.update(
                    entity.copy(date = voyage.disembarkDate, startTime = voyage.disembarkTime),
                )
            }
        }
    }

    /** 设置页"重建航次"：清空一切（类别保留，因为用户可能改过颜色） */
    suspend fun resetVoyage() {
        eventDao.clearAll()
        voyageDao.clearDayPlans()
        voyageDao.clearVoyage()
        locationPresetDao.clearAll()
    }

    /** 需求8：标记/取消某天定稿 */
    suspend fun setFinalized(date: LocalDate, finalized: Boolean, nowMs: Long) {
        voyageDao.upsertDayPlan(
            DayPlan(
                date = date,
                finalized = finalized,
                finalizedAt = if (finalized) nowMs else null,
            ).toEntity(),
        )
    }
}
