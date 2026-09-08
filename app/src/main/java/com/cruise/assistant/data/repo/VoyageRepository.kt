package com.cruise.assistant.data.repo

import com.cruise.assistant.data.BUILT_IN_CATEGORIES
import com.cruise.assistant.data.BUILT_IN_PIXIE_LEVELS
import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.dao.PixieLevelDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.dao.WishlistDao
import com.cruise.assistant.data.db.entity.WishlistItemEntity
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.TIMELINE_AUTO_GENERATED_SHOWS
import com.cruise.assistant.domain.WISHLIST_SEED_SHOWS
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.Voyage
import com.cruise.assistant.domain.SHOW_WAIT_MINUTES
import com.cruise.assistant.domain.assignShowLane
import com.cruise.assistant.domain.pickDinnerLinkedTime
import com.cruise.assistant.domain.resolveDates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/** 登船事件固定时长 15 分钟（需求7） */
private const val BOARDING_DURATION_MINUTES = 15
private const val DISEMBARK_DURATION_MINUTES = 30
private const val SAFETY_DRILL_DURATION_MINUTES = 35
private const val DINNER_DURATION_MINUTES = 75
private val SAFETY_DRILL_TIME = LocalTime.of(15, 45)

class VoyageRepository(
    private val voyageDao: VoyageDao,
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val locationPresetDao: LocationPresetDao,
    private val pixieDustDao: PixieDustDao,
    private val pixieLevelDao: PixieLevelDao,
    private val wishlistDao: WishlistDao? = null,
) {
    fun observeVoyage(): Flow<Voyage?> =
        voyageDao.observeVoyage().map { it?.toDomain() }

    suspend fun getVoyage(): Voyage? = voyageDao.getVoyage()?.toDomain()

    fun observeDayPlans(): Flow<Map<LocalDate, DayPlan>> =
        voyageDao.observeDayPlans().map { list ->
            list.associate { it.date to it.toDomain() }
        }

    /**
     * 幂等补缺内置类别：按 iconKey 插入缺失项（升级用户不重建航次也能获得新类别）
     */
    suspend fun seedMissingBuiltInCategories() {
        val existing = categoryDao.observeAllOnce()
        val missing = BUILT_IN_CATEGORIES.filter { builtIn ->
            existing.none { it.iconKey == builtIn.iconKey }
        }
        if (missing.isNotEmpty()) categoryDao.insertAll(missing.map { it.toEntity() })
    }

    /**
     * 需求7：创建航次并生成初始时间轴。
     * 1) 写 voyage  2) 每天建 DayPlan(finalized=false)
     * 3) 播种内置类别（仅当类别表为空）
     * 4) 插入登船（boardTime 起 15 分钟，不设地址）与下船（末日 07:00）两个系统事件
     */
    suspend fun createVoyage(
        shipName: String?,
        boardDate: LocalDate,
        boardTime: LocalTime,
        disembarkDate: LocalDate,
        maxDeck: Int,
        minDeck: Int = 1,
        dinnerTime: LocalTime = LocalTime.of(17, 45),
        shipPresetName: String? = null,
    ): Voyage {
        val voyage = Voyage(
            id = 1,
            shipName = shipName?.trim()?.takeIf { it.isNotEmpty() },
            boardDate = boardDate,
            boardTime = boardTime,
            disembarkDate = disembarkDate,
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = maxDeck,
            minDeck = minDeck,
        )
        voyageDao.upsertVoyage(voyage.toEntity())
        voyageDao.upsertDayPlans(
            voyage.datesInVoyage().map { DayPlan(date = it).toEntity() },
        )

        // 播种内置类别：幂等按 iconKey 补缺（count()==0 时该方法会全量插入，行为等价）。
        seedMissingBuiltInCategories()
        // 只查一次：类别表刚播种过（或早已存在），为空是编程错误，允许 .first() 抛异常。
        val categories = categoryDao.observeAllOnce()
        val importantId = categories.firstOrNull { it.iconKey == "important" }?.id
            ?: categories.first().id

        val systemEvents = listOf(
            CruiseEvent(
                date = boardDate,
                startTime = boardTime,
                durationMinutes = BOARDING_DURATION_MINUTES,
                categoryId = importantId,
                title = "登船",
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
        )
        eventDao.insertAll(systemEvents)

        // 内置船只才生成预置活动（安全演习 + 晚餐）
        if (shipPresetName != null) {
            val mealId = categories.firstOrNull { it.iconKey == "meal" }?.id
                ?: categories.first().id

            val presetEvents = mutableListOf<CruiseEvent>()

            // 安全演习：仅登船日（第一天），15:45–16:20，Deck 6 船中。
            // lane=1（第二栏）：避免 boardTime 选 16:00 时登船（16:00–16:15）与
            // 安全演习同栏重叠（冲突检查会标记 App 自己生成的事件）；与预置演出
            // （下午场 14:45–15:15 扩展后 15:15 结束）及晚餐（18:00+）均不冲突。
            presetEvents += CruiseEvent(
                date = boardDate,
                startTime = SAFETY_DRILL_TIME,
                durationMinutes = SAFETY_DRILL_DURATION_MINUTES,
                categoryId = importantId,
                title = "安全演习",
                deck = 6,
                zone = ShipZone.MID,
                lane = 1,
                isSystemGenerated = false,
                presetKey = "safety_drill",
            )

            // 晚餐：每个航程日除下船日，dinnerTime 起 75 分钟
            voyage.datesInVoyage()
                .filter { it != disembarkDate }
                .forEach { day ->
                    presetEvents += CruiseEvent(
                        date = day,
                        startTime = dinnerTime,
                        durationMinutes = DINNER_DURATION_MINUTES,
                        categoryId = mealId,
                        title = "晚餐",
                        isSystemGenerated = false,
                        presetKey = "dinner",
                    )
                }

            // 预置演出：迪士尼探险号演出目录，默认第二栏；加 30 分钟等待段后同栏冲突的
            // 场次自动岔开第三栏（assignShowLane），与晚餐/安全演习一样可编辑删除
            val showId = categories.firstOrNull { it.iconKey == "show" }?.id
                ?: categories.first().id
            // 按天记录已分配演出的 (lane, 扩展起点, 扩展终点)，供后续场次避让
            val showLanesByDay = mutableMapOf<LocalDate, MutableList<Triple<Int, Int, Int>>>()
            TIMELINE_AUTO_GENERATED_SHOWS.forEach { show ->
                show.resolveDates(boardDate, voyage.dayCount).forEach { date ->
                    val times = if (show.dinnerLinked) {
                        listOf(pickDinnerLinkedTime(dinnerTime))
                    } else {
                        show.startTimes
                    }
                    times.forEach { startTime ->
                        val startMin = startTime.hour * 60 + startTime.minute
                        val effStart = startMin - show.defaultWaitMinutes
                        val effEnd = startMin + show.durationMinutes
                        val lane = assignShowLane(
                            effStart,
                            effEnd,
                            showLanesByDay.getOrPut(date) { mutableListOf() },
                        )
                        showLanesByDay.getValue(date) += Triple(lane, effStart, effEnd)
                        presetEvents += CruiseEvent(
                            date = date,
                            startTime = startTime,
                            durationMinutes = show.durationMinutes,
                            categoryId = showId,
                            title = show.title,
                            deck = show.deck,
                            zone = show.zone,
                            placeName = show.placeName,
                            lane = lane,
                            isSystemGenerated = false,
                            presetKey = show.key,
                            waitMinutes = show.defaultWaitMinutes,
                            note = show.note,
                        )
                    }
                }
            }

            wishlistDao?.let { wDao ->
                val existingTitles = wDao.getAll().map { it.title }.toSet()
                WISHLIST_SEED_SHOWS.forEach { show ->
                    if (show.title !in existingTitles) {
                        wDao.insert(
                            WishlistItemEntity(
                                title = show.title,
                                categoryId = showId,
                                deck = show.deck,
                                zone = show.zone.name,
                                placeName = show.placeName,
                                note = show.note,
                                isCompleted = false,
                            )
                        )
                    }
                }
            }

            eventDao.insertAll(presetEvents.map { it.toEntity() })
        }

        return voyage
    }

    /**
     * 修改航次日期/时间：同步 DayPlan 集合，并把登船/下船系统事件挪到新时间。
     * 落在新范围外的普通事件会被删除（调用方负责先提示用户）。
     */
    suspend fun updateVoyage(voyage: Voyage) {
        voyageDao.upsertVoyage(voyage.toEntity())

        val validDates = voyage.datesInVoyage().toSet()
        val existingPlans = voyageDao.observeDayPlansOnce().map { it.date }.toSet()
        val newPlans = validDates.filter { it !in existingPlans }.map { DayPlan(date = it).toEntity() }
        if (newPlans.isNotEmpty()) voyageDao.upsertDayPlans(newPlans)

        // 移除落出新范围的 DayPlan：否则缩小的日期带着 finalized 状态残留，
        // 再扩大航程时以旧定稿状态"复活"、无操作即锁定。
        voyageDao.observeDayPlansOnce()
            .filter { it.date !in validDates }
            .forEach { voyageDao.deleteDayPlan(it) }

        // 只查一次，供下面两步复用。
        val allEvents = eventDao.getAll()

        // 系统事件（登船/下船）必须排除在删除范围外：它们是"挪到新时间"而不是"落在范围外就删掉"。
        // 若先把它们删了，下面的 update(entity.copy(...)) 是按 id 匹配的 UPDATE，行已不存在，
        // 会静默影响 0 行、不抛异常——事件永久消失却毫无提示（与 LocationPresetDao 文档警告的
        // 同一类"按 id 更新命中 0 行被静默吞掉"问题）。
        allEvents
            .filter { it.date !in validDates && !it.isSystemGenerated }
            .forEach { eventDao.delete(it) }

        // 注意：这里用中文标题 "登船"/"下船" 识别系统事件，与 createVoyage() 中生成时写的标题严格耦合。
        // 后续任务如果要改名这两个系统事件的标题，必须同步修改这里的匹配，否则迁移会静默失效。
        allEvents.forEach { entity ->
            if (!entity.isSystemGenerated) return@forEach
            when (entity.title) {
                // 兼容旧版本数据：v1.7.5 前登船事件标题是"上船"——首次编辑航次时
                // 一并把标题重写为"登船"并移动，避免旧事件卡在旧日期不可修复。
                "登船", "上船" -> eventDao.update(
                    entity.copy(
                        title = "登船",
                        date = voyage.boardDate,
                        startTime = voyage.boardTime,
                    ),
                )
                "下船" -> eventDao.update(
                    entity.copy(date = voyage.disembarkDate, startTime = voyage.disembarkTime),
                )
            }
        }
    }

    /** 设置页"重建航次"：清空一切（类别保留，因为用户可能改过颜色）；
     *  仙尘记录与等级一并清空并重播默认三级。 */
    suspend fun resetVoyage() {
        eventDao.clearAll()
        voyageDao.clearDayPlans()
        voyageDao.clearVoyage()
        locationPresetDao.clearAll()
        pixieDustDao.clearAll()
        pixieLevelDao.clearAll()
        pixieLevelDao.insertAll(BUILT_IN_PIXIE_LEVELS.map { it.toEntity() })
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
