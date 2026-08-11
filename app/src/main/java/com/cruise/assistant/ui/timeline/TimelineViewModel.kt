package com.cruise.assistant.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.Voyage
import com.cruise.assistant.domain.buildTimelineLayout
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.startMinute
import com.cruise.assistant.reminder.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TimelineUiState(
    val voyage: Voyage?,
    val selectedDate: LocalDate,
    val events: List<CruiseEvent>,
    val categories: Map<Long, Category>,
    val dayPlans: Map<LocalDate, DayPlan>,
    val layout: TimelineLayout,
    val settings: AppSettings,
    val nowMinute: Int,
    val isToday: Boolean,
) {
    val isFinalized: Boolean get() = dayPlans[selectedDate]?.finalized == true

    /** 进行中的事件（仅当天） */
    val ongoing: CruiseEvent?
        get() = if (!isToday) null else events.filter { it.isActive }.firstOrNull {
            nowMinute in it.startMinute until it.endMinute
        }

    /** 下一个事件（仅当天） */
    val next: CruiseEvent?
        get() = if (!isToday) null else events
            .filter { it.isActive && it.startMinute > nowMinute }
            .minByOrNull { it.startMinute }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val voyageRepository: VoyageRepository,
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
    private val reminderScheduler: ReminderScheduler,
    private val nowProvider: () -> LocalTime = { LocalTime.now() },
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val nowMinute = MutableStateFlow(nowProvider().let { it.hour * 60 + it.minute })

    /** 需求2的"看全天"临时展开——不写盘，只在当前选中日期内生效，换日期即重置。 */
    private val fullDayOverride = MutableStateFlow(false)

    init {
        // 需求1：优先用上次退出时看的日期，探测不到用系统当日；再夹到航次范围内
        viewModelScope.launch {
            val saved = settingsStore.settings.first().lastViewedDate
            val voyage = voyageRepository.getVoyage()
            val target = saved ?: todayProvider()
            selectedDate.value = voyage?.clampDate(target) ?: target
        }
        // 每分钟刷新"现在"指示线与横幅
        viewModelScope.launch {
            while (isActive) {
                nowMinute.value = nowProvider().let { it.hour * 60 + it.minute }
                delay(30_000)
            }
        }
    }

    val state: StateFlow<TimelineUiState?> =
        selectedDate.flatMapLatest { date ->
            if (date == null) {
                flowOf(null)
            } else {
                combine(
                    voyageRepository.observeVoyage(),
                    eventRepository.observeByDate(date),
                    categoryRepository.observeAll(),
                    voyageRepository.observeDayPlans(),
                    settingsStore.settings,
                    nowMinute,
                    fullDayOverride,
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val voyage = values[0] as Voyage?
                    @Suppress("UNCHECKED_CAST")
                    val events = values[1] as List<CruiseEvent>
                    @Suppress("UNCHECKED_CAST")
                    val categories = values[2] as List<Category>
                    @Suppress("UNCHECKED_CAST")
                    val plans = values[3] as Map<LocalDate, DayPlan>
                    val settings = values[4] as AppSettings
                    val now = values[5] as Int
                    val fullDay = values[6] as Boolean

                    // 定稿（生成计划表）后隐藏所有非活动事件：
                    // 既不渲染也不参与空档折叠布局，避免它们叠在活动事件底下
                    // 或在 lane2/满宽单栏模式下占住活动事件的时段。
                    // 规划态保留非活动事件可见（灰色），用于用户随时重新激活。
                    val isFinalizedFlag = plans[date]?.finalized == true
                    val visibleEvents =
                        if (isFinalizedFlag) events.filter { it.isActive } else events

                    TimelineUiState(
                        voyage = voyage,
                        selectedDate = date,
                        events = visibleEvents,
                        categories = categories.associateBy { it.id },
                        dayPlans = plans,
                        layout = buildTimelineLayout(
                            rangeStartMinute = if (fullDay) 0 else settings.displayStartMinute,
                            rangeEndMinute = if (fullDay) 1440 else settings.displayEndMinute,
                            hourHeightDp = settings.hourHeightDp,
                            events = visibleEvents,
                            collapseEmptyGaps = settings.collapseEmptyGaps,
                            showEventIds = categories
                                .filter { it.iconKey == "show" }
                                .mapTo(mutableSetOf()) { it.id },
                        ),
                        settings = settings,
                        nowMinute = now,
                        isToday = date == todayProvider(),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        fullDayOverride.value = false
        viewModelScope.launch { settingsStore.setLastViewedDate(date) }
    }

    /** 需求2："看全天"临时把范围撑到 00:00–24:00，不写盘；换日期会自动重置（见 selectDate）。 */
    fun toggleFullDay() {
        fullDayOverride.value = !fullDayOverride.value
    }

    fun moveToLane(eventId: Long, lane: Int) {
        viewModelScope.launch { eventRepository.moveToLane(eventId, lane) }
    }

    fun toggleActive(eventId: Long, active: Boolean) {
        viewModelScope.launch {
            eventRepository.setActive(eventId, active)
            reminderScheduler.rescheduleAll()
        }
    }

    fun deleteEvent(event: CruiseEvent, scope: RecurrenceScope) {
        viewModelScope.launch {
            eventRepository.deleteEvent(event, scope)
            reminderScheduler.rescheduleAll()
        }
    }

    suspend fun conflictsOn(date: LocalDate): List<Pair<CruiseEvent, CruiseEvent>> =
        eventRepository.conflictsOn(date)

    fun finalizeDay(date: LocalDate) {
        viewModelScope.launch {
            voyageRepository.setFinalized(date, true, System.currentTimeMillis())
            reminderScheduler.rescheduleAll()
        }
    }

    fun unlockDay(date: LocalDate) {
        viewModelScope.launch {
            voyageRepository.setFinalized(date, false, 0L)
            reminderScheduler.rescheduleAll()
        }
    }
}
