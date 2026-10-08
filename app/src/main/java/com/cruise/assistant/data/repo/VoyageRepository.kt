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

/**
 * 联程航次交接手续系统事件：固定 08:30 开始、时长 90 分钟。
 * 交接日（前一程的下船日）早上游客不下船，在船上完成出入境交接手续后继续下一程。
 */
private const val HANDOVER_DURATION_MINUTES = 90
private val HANDOVER_TIME = LocalTime.of(8, 30)

/**
 * 联程交接手续系统事件标题。
 * 注意：与 updateVoyage() 中按标题识别系统事件的逻辑严格耦合——改名必须两处同步，
 * 否则编辑航次后交接事件不会被同步重插。
 */
private const val HANDOVER_TITLE = "办理交接手续"

/**
 * 兰卡威登岸系统事件：09:00 起 480 分钟（09:00–17:00 靠港游玩时段）。
 * 航线背景：明年1月起部分 4夜航线变为 新加坡-兰卡威-新加坡，第三天（该程登船日+2）
 * 停靠马来西亚兰卡威岛，客人可选上岸游玩或留在船上——仅对 4夜程有意义。
 */
private const val ASHORE_DURATION_MINUTES = 480
private val ASHORE_TIME = LocalTime.of(9, 0)

/**
 * 兰卡威登岸系统事件标题。
 * 注意：与 updateVoyage() 中按标题识别系统事件的逻辑严格耦合——改名必须两处同步，
 * 否则编辑航次后登岸事件不会被同步重插。
 */
private const val ASHORE_TITLE = "兰卡威登岸"

class VoyageRepository(
    private val voyageDao: VoyageDao,
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val locationPresetDao: LocationPresetDao,
    private val pixieDustDao: PixieDustDao,
    private val pixieLevelDao: PixieLevelDao,
    private val wishlistDao: WishlistDao? = null,
    // 可注入的事务执行器：生产环境由 AppContainer 传 db.withTransaction，
    // 单元测试 Fake DAO 没有 Room 数据库，默认直跑（不包事务）语义正确。
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
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
     * 4) 插入系统事件：登船（boardTime 起 15 分钟，仅首程登船日）、联程交接手续
     *    （每个交接日 08:30 起 90 分钟，无地点）、兰卡威登岸（勾选的 4夜程第三天
     *    09:00 起 480 分钟，无地点）、下船（最终下船日 07:00，仅一个）
     * 5) 联程（legs 非空）时预置演出按程生成，每程各自从 day1 重新循环
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
        // 礼宾（Concierge）客户：不受登船批次限制，11:00 即可优先登船。
        // 只作航次标记，登船系统事件本身不加备注、不改标题，仍保持「登船」15 分钟。
        isConcierge: Boolean = false,
        // 联程航次各程晚数（如 [3, 4] = 3夜+4夜 back-to-back），空 = 单程。
        legs: List<Int> = emptyList(),
        // 登岸兰卡威的程下标集合（对 legs 的 0-based 下标）：仅晚数==4 的程生成登岸事件，
        // 3夜程的脏下标静默忽略；空集 = 全程不留岸。
        ashoreLegs: Set<Int> = emptySet(),
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
            isConcierge = isConcierge,
            legs = legs,
            ashoreLegs = ashoreLegs,
        )
        // 各程晚数与登船日：单程 legs 为空时按 [总晚数] / [boardDate]，供按程逻辑（登岸/演出）复用
        val legNightsPerLeg = legs.ifEmpty { listOf(voyage.dayCount - 1) }
        val legBoards = voyage.legBoardDates()
        // 创建航次是多步写入（voyage + DayPlan + 类别播种 + 系统事件 + 预置活动/心愿单），
        // 包进一个事务：任一步失败即整体回滚，避免留下"voyage 已建、事件残缺"的半成品航次。
        // voyage 对象在事务外构造好，事务块内只放写入；无提前 return，最后统一返回。
        runInTransaction {
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

            val systemEvents = buildList {
                // 登船：仅首程登船日一次（联程不在交接日重复生成登船事件，交接日生成的是交接手续）
                add(
                    CruiseEvent(
                        date = boardDate,
                        startTime = boardTime,
                        durationMinutes = BOARDING_DURATION_MINUTES,
                        categoryId = importantId,
                        title = "登船",
                        isSystemGenerated = true,
                    ).toEntity(),
                )
                // 联程交接手续：每个交接日（除最后一程外各程的下船日）一个，08:30 起 90 分钟，
                // 无地点（deck/zone/placeName 全空，游客在船上办手续不涉及具体场所）
                voyage.junctionDates().forEach { junction ->
                    add(
                        CruiseEvent(
                            date = junction,
                            startTime = HANDOVER_TIME,
                            durationMinutes = HANDOVER_DURATION_MINUTES,
                            categoryId = importantId,
                            title = HANDOVER_TITLE,
                            isSystemGenerated = true,
                        ).toEntity(),
                    )
                }
                // 兰卡威登岸：勾选的程里晚数==4 的（新加坡-兰卡威-新加坡 4夜航线第三天靠港），
                // 在该程登船日+2（第三天）生成 09:00–17:00 的上岸游玩时段，无地点（岸上活动
                // 不涉及船上场所）；3夜程的脏下标静默忽略。可编辑不可删的系统事件。
                ashoreLegs.forEach { legIndex ->
                    if (legNightsPerLeg.getOrNull(legIndex) != 4) return@forEach
                    add(
                        CruiseEvent(
                            date = legBoards[legIndex].plusDays(2),
                            startTime = ASHORE_TIME,
                            durationMinutes = ASHORE_DURATION_MINUTES,
                            categoryId = importantId,
                            title = ASHORE_TITLE,
                            isSystemGenerated = true,
                        ).toEntity(),
                    )
                }
                // 下船：仅最终下船日一个（联程的中间下船日不生成下船事件，由交接手续替代）
                add(
                    CruiseEvent(
                        date = disembarkDate,
                        startTime = voyage.disembarkTime,
                        durationMinutes = DISEMBARK_DURATION_MINUTES,
                        categoryId = importantId,
                        title = "下船",
                        isSystemGenerated = true,
                    ).toEntity(),
                )
            }
            eventDao.insertAll(systemEvents)

            // 内置船只才生成预置活动（安全演习 + 晚餐）
            if (shipPresetName != null) {
                val mealId = categories.firstOrNull { it.iconKey == "meal" }?.id
                    ?: categories.first().id

                val presetEvents = mutableListOf<CruiseEvent>()

                // 安全演习：仅登船日（首程第一天），15:45–16:20，Deck 6 船中。
                // 联程下只在首程生成一次——交接日（后续各程的"登船日"）不重复演习，
                // 现状只在 boardDate 生成一次，天然满足。
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

                // 晚餐：每个航程日除最终下船日，dinnerTime 起 75 分钟。
                // 联程的交接日（前一程下船日）不下船、照常用晚餐，filter 天然保留——有意为之。
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
                // 按程生成：每程以该程登船日为起点、该程晚数+1 为天数调用 resolveDates
                //（第二参含下船日，演出被截断到该程下船日前），使每程各自从 day1 重新循环
                //（如两程都有启航秀/狮子王，符合船上真实排期）；单程 legs 为空时按
                // [总晚数] 生成一次，与旧的整航次一次性生成完全等价（legNightsPerLeg/
                // legBoards 已在事务外按同一规则求出）。
                // showLanesByDay 仍按绝对日期记录避让信息，跨程日期不同天然不冲突。
                val showLanesByDay = mutableMapOf<LocalDate, MutableList<Triple<Int, Int, Int>>>()
                TIMELINE_AUTO_GENERATED_SHOWS.forEach { show ->
                    legNightsPerLeg.forEachIndexed { legIndex, nights ->
                        val legBoard = legBoards[legIndex]
                        show.resolveDates(legBoard, nights + 1).forEach { date ->
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
        }

        return voyage
    }

    /**
     * 修改航次日期/时间：同步 DayPlan 集合，并把登船/下船系统事件挪到新时间。
     * 联程「办理交接手续」系统事件按新 junctionDates 先全删再重插（幂等）；
     * 「兰卡威登岸」系统事件按新 ashoreLegs/各程登船日先全删再重插（幂等，同款模式）。
     * 落在新范围外的普通事件会被删除（调用方负责先提示用户）。
     */
    suspend fun updateVoyage(voyage: Voyage) {
        // 删 DayPlan、删越界事件、挪系统事件是一组相关写入，包进事务保证原子性：
        // 中途失败不能出现"DayPlan 删了但系统事件没挪过去"的中间态。
        runInTransaction {
            voyageDao.upsertVoyage(voyage.toEntity())

            val validDates = voyage.datesInVoyage().toSet()
            val existingPlans = voyageDao.getDayPlansOnce().map { it.date }.toSet()
            val newPlans = validDates.filter { it !in existingPlans }.map { DayPlan(date = it).toEntity() }
            if (newPlans.isNotEmpty()) voyageDao.upsertDayPlans(newPlans)

            // 移除落出新范围的 DayPlan：否则缩小的日期带着 finalized 状态残留，
            // 再扩大航程时以旧定稿状态"复活"、无操作即锁定。
            voyageDao.getDayPlansOnce()
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
            // 联程交接事件（"办理交接手续"）不在此处挪动，统一走下面的先删后重插逻辑。
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

            // 联程交接手续事件：先全删、再按新 junctionDates 重插（幂等）。
            // 不走按 id update：它既覆盖"联程改单程要清空交接"也覆盖"单程改联程要新增"
            //（数量随程数变化，挪动逻辑无法表达），还规避按 id 更新命中 0 行被静默吞掉的
            // 陷阱——与上文登船/下船注释里警告的是同一类问题。
            allEvents
                .filter { it.isSystemGenerated && it.title == HANDOVER_TITLE }
                .forEach { eventDao.delete(it) }
            if (voyage.junctionDates().isNotEmpty()) {
                // 与 createVoyage 相同的类别解析模式：important 内置类别必在，兜底取第一个。
                val categories = categoryDao.observeAllOnce()
                val importantId = categories.firstOrNull { it.iconKey == "important" }?.id
                    ?: categories.first().id
                eventDao.insertAll(
                    voyage.junctionDates().map { junction ->
                        CruiseEvent(
                            date = junction,
                            startTime = HANDOVER_TIME,
                            durationMinutes = HANDOVER_DURATION_MINUTES,
                            categoryId = importantId,
                            title = HANDOVER_TITLE,
                            isSystemGenerated = true,
                        ).toEntity()
                    },
                )
            }

            // 兰卡威登岸事件：先全删、再按新航次的 ashoreLegs 重插（幂等）。
            // 与交接手续完全同款的先删后插模式：勾选随编辑变化、登船日挪动时事件日期随之
            // 变化（挪动逻辑无法表达），也规避按 id 更新命中 0 行被静默吞掉的陷阱——
            // 与上文登船/下船注释里警告的是同一类问题。
            allEvents
                .filter { it.isSystemGenerated && it.title == ASHORE_TITLE }
                .forEach { eventDao.delete(it) }
            // 仅晚数==4 的程生成（3夜程的脏下标静默忽略）：4夜航线（新加坡-兰卡威-新加坡）
            // 第三天（该程登船日+2）靠港，09:00–17:00 上岸游玩时段，无地点。
            val ashoreNightsPerLeg = voyage.legs.ifEmpty { listOf(voyage.dayCount - 1) }
            val ashoreLegBoards = voyage.legBoardDates()
            if (voyage.ashoreLegs.isNotEmpty()) {
                // 与 createVoyage 相同的类别解析模式：important 内置类别必在，兜底取第一个。
                val categories = categoryDao.observeAllOnce()
                val importantId = categories.firstOrNull { it.iconKey == "important" }?.id
                    ?: categories.first().id
                eventDao.insertAll(
                    voyage.ashoreLegs.mapNotNull { legIndex ->
                        if (ashoreNightsPerLeg.getOrNull(legIndex) != 4) return@mapNotNull null
                        CruiseEvent(
                            date = ashoreLegBoards[legIndex].plusDays(2),
                            startTime = ASHORE_TIME,
                            durationMinutes = ASHORE_DURATION_MINUTES,
                            categoryId = importantId,
                            title = ASHORE_TITLE,
                            isSystemGenerated = true,
                        ).toEntity()
                    },
                )
            }
        }
    }

    /** 设置页"重建航次"：清空事件/DayPlan/航次/地点预置；类别保留（用户可能改过颜色）；
     *  仙尘记录与等级一并清空并重播默认三级。
     *  注意：wishlist 有意保留、不清空——它是用户手动整理的收藏，不属于"航次数据"；
     *  重建航次后 createVoyage 的心愿单播种按标题跳过已存在的条目，因此不会产生重复。 */
    suspend fun resetVoyage() {
        // 清空 + 重播是一组相关写入，包进事务：中途失败不会留下"清了一半"的脏库。
        runInTransaction {
            eventDao.clearAll()
            voyageDao.clearDayPlans()
            voyageDao.clearVoyage()
            locationPresetDao.clearAll()
            pixieDustDao.clearAll()
            pixieLevelDao.clearAll()
            pixieLevelDao.insertAll(BUILT_IN_PIXIE_LEVELS.map { it.toEntity() })
        }
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
