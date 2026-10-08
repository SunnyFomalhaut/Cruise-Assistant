package com.cruise.assistant.ui.archive

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.data.repo.VoyageArchiveRepository
import com.cruise.assistant.data.repo.nightsSummaryOf
import com.cruise.assistant.domain.ArchiveSnapshot
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.RoomPair
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.buildTimelineLayout
import com.cruise.assistant.domain.pairByRoomForSweep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 存档时间轴回放模式：草稿（三栏卡片，含未启用备选）或 日程表（定稿样式，仅生效日程）。
 */
enum class ArchiveTimelineMode { DRAFT, SCHEDULE }

/**
 * 存档详情（只读回放）状态。
 *
 * 时间轴部分与 TimelineUiState 对齐，但有两条铁律：
 * 1. 事件/类别/定稿状态全部来自快照（自包含），不查任何业务表；
 * 2. 显示范围/缩放/折叠等"查看偏好"跟随用户当前 AppSettings——偏好不属于旅程记录。
 * isToday 恒 false（存档里没有"现在"），因此没有进行中/下一个横幅与红线。
 */
@Immutable
data class ArchiveDetailUiState(
    /** 加载中（getSnapshot 未返回） */
    val loading: Boolean = true,
    /** 加载失败：存档不存在或 payload 损坏（getSnapshot 返回 null） */
    val loadFailed: Boolean = false,
    val snapshot: ArchiveSnapshot? = null,
    val selectedDate: LocalDate? = null,
    /** 当日可见事件（日程表模式仅 isActive；草稿模式含未启用备选，渲染层显示为灰色） */
    val events: List<CruiseEvent> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val dayPlans: Map<LocalDate, DayPlan> = emptyMap(),
    val layout: TimelineLayout? = null,
    val settings: AppSettings = AppSettings(),
    /** 仙尘按房间配对（pairByRoomForSweep，按层扫楼序），供仙尘 Tab 渲染 */
    val roomPairs: List<RoomPair> = emptyList(),
    /** 存档时间（来自元信息表；快照本体不含） */
    val createdAt: Long? = null,
    /** 存档时间轴回放模式（会话级查看偏好，chips UI 用） */
    val timelineMode: ArchiveTimelineMode = ArchiveTimelineMode.DRAFT,
) {
    val voyage get() = snapshot?.voyage

    /** 选中日期在快照中的定稿状态（存档真相，徽标用） */
    val isFinalized: Boolean
        get() = selectedDate?.let { dayPlans[it]?.finalized == true } ?: false

    /**
     * 当前渲染形态是否为日程表（定稿样式：单栏满宽、仅活动事件）。
     * 只由 timelineMode 决定、与 isFinalized 解耦——这是"已定稿日能切回草稿模式"的关键：
     * 已定稿日切草稿时此处为 false（三栏规划态、灰色备选可见），徽标仍显示已定稿。
     */
    val renderAsSchedule: Boolean
        get() = timelineMode == ArchiveTimelineMode.SCHEDULE

    /** 概要行的晚数摘要（联程 "3+4夜 · 共7晚"，单程 "4夜"） */
    val nightsSummary: String?
        get() = voyage?.let { nightsSummaryOf(it) }
}

/**
 * 航次存档详情 ViewModel：加载快照后做纯内存的只读回放状态构建。
 *
 * 状态构建严格模仿 ui/timeline/TimelineViewModel 的 combine 逻辑
 * （事件过滤、buildTimelineLayout 参数、show 类别集合），
 * 区别：数据源由仓库 Flow 换成快照，无 nowMinute 轮询、无写回方法。
 *
 * 回放模式切换（timelineMode，会话级查看偏好，不持久化、不写档）：
 * - 日程表（SCHEDULE）：定稿样式，单栏满宽、仅当时生效（isActive）的日程——看"当天实际玩了什么"；
 * - 草稿（DRAFT）：三栏泳道规划态，保留全部事件（含灰色未启用备选）——回看"当时是怎么想的、
 *   怎么排除冲突的"（同主界面草稿阶段的三栏卡片视图）。
 * 两种状态自由切换，与当日定稿事实解耦：已定稿的日期也能切回草稿模式。
 * 初始模式跟随登船日的存档定稿状态（定稿过→日程表，未定稿→草稿）；
 * 切日期不重置模式（用户翻看多天时保持同一视角）。
 */
class ArchiveDetailViewModel(
    private val repository: VoyageArchiveRepository,
    /** 查看偏好（显示范围/缩放/折叠/人偶）跟随当前设置，不属快照 */
    private val settingsFlow: Flow<AppSettings>,
    private val archiveId: Long,
    // 与 WishlistViewModel 同一模式：测试注入 Unconfined 作用域，生产缺省走 viewModelScope
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val snapshot = MutableStateFlow<ArchiveSnapshot?>(null)
    private val loadFailed = MutableStateFlow(false)
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    /** "看全天"临时撑满 0–1440，不写盘；切日期自动重置（同 TimelineViewModel） */
    private val fullDayOverride = MutableStateFlow(false)

    /** 回放模式：日程表（仅生效日程）或草稿（含灰色未启用备选）；会话级，不持久化 */
    private val timelineMode = MutableStateFlow(ArchiveTimelineMode.DRAFT)

    init {
        scope.launch {
            val loaded = repository.getSnapshot(archiveId)
            if (loaded == null) {
                loadFailed.value = true
            } else {
                snapshot.value = loaded
                // 初始模式按登船日的存档定稿状态定：定稿过→日程表（当天实际玩了什么），
                // 未定稿→草稿（回看当时怎么排的）。之后模式会话级保持，切日期不重置。
                val boardFinalized = loaded.dayPlans
                    .firstOrNull { it.date == loaded.voyage.boardDate }?.finalized == true
                timelineMode.value =
                    if (boardFinalized) ArchiveTimelineMode.SCHEDULE else ArchiveTimelineMode.DRAFT
                // 初始选中登船日，从旅程第一天开始回忆
                selectedDate.value = loaded.voyage.boardDate
            }
        }
    }

    val state: StateFlow<ArchiveDetailUiState> = combine(
        snapshot,
        selectedDate,
        settingsFlow,
        fullDayOverride,
        repository.observeAll(),
        loadFailed,
        timelineMode,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val snap = values[0] as ArchiveSnapshot?
        val date = values[1] as LocalDate?
        val settings = values[2] as AppSettings
        val fullDay = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val metas = values[4] as List<VoyageArchiveMeta>
        val failed = values[5] as Boolean
        val mode = values[6] as ArchiveTimelineMode

        // 失败或尚未加载完：只带标记，其余字段空默认
        if (snap == null || date == null) {
            return@combine ArchiveDetailUiState(
                loading = !failed,
                loadFailed = failed,
                timelineMode = mode,
            )
        }

        // ---- 以下复算逻辑与 TimelineViewModel.state 的 combine 一致 ----
        val dayPlans = snap.dayPlans.associateBy { it.date }

        // 渲染形态只由回放模式决定、与当日定稿事实解耦：日程表模式只显示 isActive 事件
        // ——既不渲染也不参与空档折叠布局；草稿模式保留全部事件（灰色未启用备选可见），
        // 与实时时间轴规划态同现逻辑。isFinalized 本身仍是存档真相（徽标用），不随模式变化。
        val renderAsSchedule = mode == ArchiveTimelineMode.SCHEDULE
        val dayEvents = snap.events.filter { it.date == date }
        val visibleEvents = if (renderAsSchedule) dayEvents.filter { it.isActive } else dayEvents

        ArchiveDetailUiState(
            loading = false,
            loadFailed = false,
            snapshot = snap,
            selectedDate = date,
            events = visibleEvents,
            categories = snap.categories.associateBy { it.id },
            dayPlans = dayPlans,
            layout = buildTimelineLayout(
                rangeStartMinute = if (fullDay) 0 else settings.displayStartMinute,
                rangeEndMinute = if (fullDay) 1440 else settings.displayEndMinute,
                hourHeightDp = settings.hourHeightDp,
                events = visibleEvents,
                collapseEmptyGaps = settings.collapseEmptyGaps,
                // 演出类别集合也来自快照：未来配置改了图标 key，旧档仍按当时状态回放
                showCategoryIds = snap.categories
                    .filter { it.iconKey == "show" }
                    .mapTo(mutableSetOf()) { it.id },
            ),
            settings = settings,
            roomPairs = pairByRoomForSweep(
                given = snap.pixieDust.filter { it.direction == PixieDustDirection.GIVEN },
                received = snap.pixieDust.filter { it.direction == PixieDustDirection.RECEIVED },
            ) { record ->
                // 等级 rank 从快照 levels 反查；查不到（等级后来被删）兜底 0
                snap.pixieLevels.firstOrNull { it.id == record.levelId }?.rank ?: 0
            },
            createdAt = metas.firstOrNull { it.id == archiveId }?.createdAt,
            timelineMode = mode,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ArchiveDetailUiState())

    /** 切换回放日期并重算当日事件；换日期重置"看全天"，但【不重置回放模式】（翻看多天保持视角） */
    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        fullDayOverride.value = false
    }

    /** "看全天"临时把显示范围撑到 00:00–24:00（同 TimelineViewModel.toggleFullDay） */
    fun toggleFullDay() {
        fullDayOverride.value = !fullDayOverride.value
    }

    /**
     * 切换回放模式（会话级查看偏好，不持久化、不写档）：
     * 日程表=定稿样式（单栏满宽+仅活动事件，看当天实际日程）；
     * 草稿=三栏规划态（含灰色未启用备选，回看当时怎么排的）。
     * 与当日定稿事实解耦：已定稿的日期也能切回草稿模式。
     */
    fun setTimelineMode(mode: ArchiveTimelineMode) {
        timelineMode.value = mode
    }
}
