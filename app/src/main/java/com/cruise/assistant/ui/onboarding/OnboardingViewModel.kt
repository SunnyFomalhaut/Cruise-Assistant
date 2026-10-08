package com.cruise.assistant.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.BUILT_IN_SHIPS
import com.cruise.assistant.reminder.ReminderScheduler
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** 礼宾（Concierge）优先登船时间：不受批次限制，11:00 即可登船 */
private val CONCIERGE_BOARD_TIME = LocalTime.of(11, 0)

/** 星期中文标签（周一首选写法，其余走 Locale 兜底） */
private val DOW_LABELS = mapOf(
    java.time.DayOfWeek.MONDAY to "周一",
    java.time.DayOfWeek.TUESDAY to "周二",
    java.time.DayOfWeek.WEDNESDAY to "周三",
    java.time.DayOfWeek.THURSDAY to "周四",
    java.time.DayOfWeek.FRIDAY to "周五",
    java.time.DayOfWeek.SATURDAY to "周六",
    java.time.DayOfWeek.SUNDAY to "周日",
)

/** 日期的中文星期标签，如"周一"；摘要文案用 */
internal fun dowLabel(date: LocalDate): String =
    DOW_LABELS[date.dayOfWeek]
        ?: date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.CHINA)

/**
 * 交替链的下一程晚数：本船班次只有 3夜（周一登船）与 4夜（周四登船）两种，
 * 联程只能交替拼接（3+4+3+4…或 4+3+4+3…，不限长度），禁止 3+3。
 * 链末（extraLegs 最后一程；无后续程时即首程船期晚数）是 3 → 下一程 4，是 4 → 下一程 3；
 * 未选船期或链末是 3/4 之外的异常值返回 null（编排弹层据此不渲染追加按钮）。
 */
internal fun nextLegNightsFor(scheduleNights: Int?, extraLegs: List<Int>): Int? {
    scheduleNights ?: return null
    return when (extraLegs.lastOrNull() ?: scheduleNights) {
        3 -> 4
        4 -> 3
        else -> null
    }
}

/** 联程摘要的结构化数据：UI 用字符串资源拼成「3+4夜 · 共7晚 · 周一登船 → 下周一7:00下船」 */
data class MultiLegSummary(
    /** 各程晚数标签，如 "3+4" */
    val legsLabel: String,
    /** 总晚数 */
    val totalNights: Int,
    /** 登船日星期标签，如 "周一" */
    val boardDow: String,
    /** 下船日星期标签（跨周自动带"下"前缀，如 "下周一"） */
    val disembarkDow: String,
)

@Immutable
data class OnboardingState(
    val shipName: String = BUILT_IN_SHIPS[0].name,
    val selectedShipIndex: Int = 0,
    val selectedScheduleIndex: Int = -1,
    val dinnerTime: LocalTime = LocalTime.of(17, 45),
    val boardDate: LocalDate,
    val boardTime: LocalTime = LocalTime.of(12, 0),
    val disembarkDate: LocalDate,
    val maxDeck: Int = BUILT_IN_SHIPS[0].maxDeck ?: 20,
    val minDeck: Int = BUILT_IN_SHIPS[0].minDeck ?: 1,
    /** 礼宾（Concierge）客户：开启后不受登船批次限制，11:00 优先登船 */
    val isConcierge: Boolean = false,
    /** 上一次选择的批次登船时间：关闭礼宾时恢复用它（默认 12:00 与批次默认一致） */
    val lastBatchBoardTime: LocalTime = LocalTime.of(12, 0),
    /**
     * 联程航次（back-to-back）：第2程及以后各程晚数。班次只有 3夜/4夜两种，链强制交替
     *（3+4+3+4…或 4+3+4+3…），仅可从末尾追加/缩短；首程取所选船期晚数，空 = 单程。
     */
    val extraLegs: List<Int> = emptyList(),
    /** 登岸兰卡威的程下标集合（全链 0-based）：仅对 4夜程有意义（第三天停靠兰卡威可选上岸） */
    val ashoreLegs: Set<Int> = emptySet(),
    val saving: Boolean = false,
    /** 已本地化的错误提示 resId，null 表示无错误 */
    val errorResId: Int? = null,
) {
    /** 是否选中了内置船（selectedShipIndex >= 0） */
    val isBuiltInShip: Boolean get() = selectedShipIndex >= 0

    /** 所选船期的晚数；未选内置船期返回 null（联程首程与编排弹层第1程标签共用） */
    internal val scheduleNights: Int?
        get() = BUILT_IN_SHIPS.getOrNull(selectedShipIndex)
            ?.schedules?.getOrNull(selectedScheduleIndex)?.nights

    /** 是否联程：追加过任意后续航程即为联程 */
    val isMultiLeg: Boolean
        get() = extraLegs.isNotEmpty()

    /**
     * 联程各程晚数：首程 = 所选船期晚数，其后为用户追加的 extraLegs
     *（强制 3/4 交替，仅可从末尾追加/缩短）。未追加后续程或未选船期为空 = 单程。
     */
    val legs: List<Int>
        get() {
            val first = scheduleNights ?: return emptyList()
            if (extraLegs.isEmpty()) return emptyList()
            return listOf(first) + extraLegs
        }

    /** 交替链的下一程晚数（供编排弹层「添加X夜」按钮标签）；未选船期或链末异常为 null */
    val nextLegNights: Int? get() = nextLegNightsFor(scheduleNights, extraLegs)

    /** 航程总晚数：联程 = 各程之和；单程 = 船期晚数；未选船期为 null */
    val totalNights: Int?
        get() = legs.takeIf { it.isNotEmpty() }?.sum() ?: scheduleNights

    /** 联程开启时按总晚数推算的下船日；未开联程（legs 为空）返回 null */
    fun multiLegDisembarkDate(board: LocalDate): LocalDate? =
        legs.takeIf { it.isNotEmpty() }?.sum()?.let { board.plusDays(it.toLong()) }

    /** 联程摘要参数；未开联程返回 null */
    val multiLegSummary: MultiLegSummary?
        get() {
            val legList = legs
            if (legList.isEmpty()) return null
            val total = legList.sum()
            val disembark = boardDate.plusDays(total.toLong())
            val dow = dowLabel(disembark)
            // 跨周显示"下周X"，跨两周显示"下下周五"：每满 7 天叠一个"下"
            val weeks = ((disembark.toEpochDay() - boardDate.toEpochDay()) / 7).toInt()
            val prefixed = if (weeks > 0) "下".repeat(weeks) + dow else dow
            return MultiLegSummary(
                legsLabel = legList.joinToString("+"),
                totalNights = total,
                boardDow = dowLabel(boardDate),
                disembarkDow = prefixed,
            )
        }
}

class OnboardingViewModel(
    private val voyageRepository: VoyageRepository,
    private val settingsStore: SettingsStore,
    private val reminderScheduler: ReminderScheduler,
    today: LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingState(boardDate = today, disembarkDate = today.plusDays(5)),
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onShipNameChange(value: String) = _state.update { it.copy(shipName = value) }

    fun onShipSelected(index: Int) = _state.update { current ->
        // 切换船只时重置联程状态：联程链首程依赖新船的船期表，旧链不再有效；
        // 首程变化使整条交替链重组，登岸勾选的旧下标一并失效
        if (index < 0 || index >= BUILT_IN_SHIPS.size) {
            // 自定义船名
            current.copy(
                selectedShipIndex = -1,
                selectedScheduleIndex = -1,
                shipName = "",
                maxDeck = 20,
                minDeck = 1,
                extraLegs = emptyList(),
                ashoreLegs = emptySet(),
            )
        } else {
            val ship = BUILT_IN_SHIPS[index]
            current.copy(
                selectedShipIndex = index,
                selectedScheduleIndex = -1,
                shipName = ship.name,
                maxDeck = ship.maxDeck ?: current.maxDeck,
                minDeck = ship.minDeck ?: 1,
                extraLegs = emptyList(),
                ashoreLegs = emptySet(),
            )
        }
    }

    fun onScheduleSelected(scheduleIndex: Int) = _state.update { current ->
        val shipIndex = current.selectedShipIndex
        if (shipIndex < 0 || shipIndex >= BUILT_IN_SHIPS.size) return@update current
        val ship = BUILT_IN_SHIPS[shipIndex]
        if (scheduleIndex < 0 || scheduleIndex >= ship.schedules.size) {
            // 自定义船期：联程首程依赖船期晚数，切到自定义时清空联程链；
            // 首程晚数未知，登岸勾选随旧下标一并失效
            current.copy(
                selectedScheduleIndex = -1,
                extraLegs = emptyList(),
                ashoreLegs = emptySet(),
            )
        } else {
            val schedule = ship.schedules[scheduleIndex]
            val today = current.boardDate
            val boardDate = schedule.boardDateFrom(today)
            // 船期自带批次时间：始终记入 lastBatchBoardTime，供关闭礼宾时恢复；
            // 礼宾开启时 boardTime 保持 11:00 优先登船，不被批次时间覆盖。
            val base = current.copy(
                selectedScheduleIndex = scheduleIndex,
                boardDate = boardDate,
                lastBatchBoardTime = schedule.boardTime,
                boardTime = if (current.isConcierge) CONCIERGE_BOARD_TIME else schedule.boardTime,
                // 选具体船期会改变首程晚数 → 整条交替链重组，旧下标失效，清空登岸勾选
                ashoreLegs = emptySet(),
            )
            // 联程开启：保留已追加的后续程（extraLegs 不动），首程随新船期变化，
            // 下船日一律按新链总晚数重算；单程保持原行为（登船日 + 船期晚数）。
            val disembark = base.multiLegDisembarkDate(boardDate)
                ?: schedule.disembarkDateFrom(today)
            base.copy(disembarkDate = disembark)
        }
    }

    fun onDinnerTimeChange(time: LocalTime) = _state.update { it.copy(dinnerTime = time) }

    fun onBoardDateChange(value: LocalDate) = _state.update { current ->
        // 联程开启：下船日一律 = 登船日 + 联程总晚数（交接日随之联动）。
        // 选了船期时按 nights 自动算下船日；否则只做不早于登船的保护。
        val multiLegDisembark = current.multiLegDisembarkDate(value)
        val disembark = multiLegDisembark ?: if (current.selectedScheduleIndex >= 0) {
            val shipIndex = current.selectedShipIndex
            if (shipIndex in BUILT_IN_SHIPS.indices) {
                val nights = BUILT_IN_SHIPS[shipIndex].schedules[current.selectedScheduleIndex].nights
                value.plusDays(nights.toLong())
            } else {
                // 船期索引有效但船索引异常，回退到下限保护
                if (current.disembarkDate.isBefore(value)) value else current.disembarkDate
            }
        } else {
            if (current.disembarkDate.isBefore(value)) value else current.disembarkDate
        }
        current.copy(boardDate = value, disembarkDate = disembark, errorResId = null)
    }

    fun onBoardTimeChange(value: LocalTime) = _state.update {
        // 批次网格只在礼宾关闭时可见；选中的批次时间同步记入 lastBatchBoardTime，
        // 保证之后开/关礼宾能恢复到用户真正选过的批次。
        it.copy(boardTime = value, lastBatchBoardTime = value)
    }

    /**
     * 礼宾优先登船开关：
     * 开启 → 登船时间固定 11:00（不受批次限制），并把当前批次时间记入 lastBatchBoardTime；
     * 关闭 → 恢复到记忆的批次时间（默认 12:00 与现默认一致）。
     */
    fun onConciergeToggle(enabled: Boolean) = _state.update { current ->
        if (enabled == current.isConcierge) return@update current
        if (enabled) {
            current.copy(
                isConcierge = true,
                // 当前已是 11:00（理论上只在礼宾态出现）就不覆盖记忆，避免记住礼宾时间本身
                lastBatchBoardTime =
                    if (current.boardTime == CONCIERGE_BOARD_TIME) current.lastBatchBoardTime
                    else current.boardTime,
                boardTime = CONCIERGE_BOARD_TIME,
            )
        } else {
            current.copy(isConcierge = false, boardTime = current.lastBatchBoardTime)
        }
    }

    fun onDisembarkDateChange(value: LocalDate) = _state.update {
        it.copy(disembarkDate = value, errorResId = null)
    }

    /**
     * 追加一程联程（强制交替：只接受 state.nextLegNights 指定的晚数，防御 UI 外乱序调用）：
     * 下船日 = 登船日 + 新联程链总晚数（交接日随 createVoyage 的 legs 派生）；
     * 追加不动 ashoreLegs——新程默认不登岸，需用户显式勾选。
     */
    fun onAddLeg(nights: Int) = _state.update { current ->
        if (nights != current.nextLegNights) return@update current
        val base = current.copy(extraLegs = current.extraLegs + nights)
        val disembark = base.multiLegDisembarkDate(base.boardDate)
            ?: base.disembarkDate
        base.copy(disembarkDate = disembark)
    }

    /**
     * 删除最后一程后续程（extraLegs 末位下标，越界/非末位忽略）：
     * 交替链由首程 + 程数完全决定，删中间等价于从末尾缩短，故只留尾部删除。
     * 下船日按删后的联程链重算；删空则恢复单程船期下船日（登船日 + 船期晚数）。
     * 被删的程（全链下标 = extraLegs.size）若勾了登岸兰卡威，一并移除。
     */
    fun onRemoveLeg(index: Int) = _state.update { current ->
        if (index != current.extraLegs.lastIndex) return@update current
        val base = current.copy(
            extraLegs = current.extraLegs.dropLast(1),
            ashoreLegs = current.ashoreLegs - current.extraLegs.size,
        )
        val board = base.boardDate
        val disembark = base.multiLegDisembarkDate(board)
            // 删空（legs 变空）：恢复单程船期下船日
            ?: board.plusDays((base.totalNights ?: 0).toLong())
        base.copy(disembarkDate = disembark)
    }

    /** 清空全部后续程，恢复单程：下船日恢复为登船日 + 船期晚数，登岸勾选随旧下标清空 */
    fun onClearLegs() = _state.update { current ->
        if (current.extraLegs.isEmpty()) return@update current
        val base = current.copy(extraLegs = emptyList(), ashoreLegs = emptySet())
        val board = base.boardDate
        val disembark = base.multiLegDisembarkDate(board)
            ?: board.plusDays((base.totalNights ?: 0).toLong())
        base.copy(disembarkDate = disembark)
    }

    /**
     * 切换某程的「登岸兰卡威」勾选（全链 0-based 下标）：
     * 仅该程晚数==4 才允许（兰卡威登岸只对 4夜程有意义），否则忽略；
     * 不动下船日——登岸只是第三天靠港游玩，不改变航程日期。
     */
    fun onToggleAshore(legIndex: Int) = _state.update { current ->
        // 联程取该下标程晚数；单程（legs 空）取首程 = 船期晚数（仅下标 0 合法）；
        // 自定义船期晚数未知，无法判定，忽略
        val nights = when {
            current.legs.isNotEmpty() -> current.legs.getOrNull(legIndex)
            legIndex == 0 -> current.scheduleNights
            else -> null
        }
        if (nights != 4) return@update current
        val ashoreLegs = if (legIndex in current.ashoreLegs) {
            current.ashoreLegs - legIndex
        } else {
            current.ashoreLegs + legIndex
        }
        current.copy(ashoreLegs = ashoreLegs)
    }

    fun onMaxDeckChange(value: Int) = _state.update { it.copy(maxDeck = value.coerceIn(1, 30)) }

    fun submit(onDone: () -> Unit) {
        val current = _state.value
        // 重入保护：Button 的 enabled = !state.saving 只反映上一次重组时的值，
        // 两次快速点击可能都在下一次重组落地前发出，靠它单独拦不住重复提交。
        if (current.saving) return
        if (current.disembarkDate.isBefore(current.boardDate)) {
            _state.update {
                it.copy(errorResId = com.cruise.assistant.R.string.error_disembark_before_board)
            }
            return
        }
        _state.update { it.copy(saving = true, errorResId = null) }
        viewModelScope.launch {
            val shipPresetName = if (current.isBuiltInShip) current.shipName else null
            // 把引导页选的晚餐时段写入设置：时间轴用 createVoyage 的参数，
            // 设置页读 SettingsStore——不持久化会导致设置页显示旧默认值 17:45。
            settingsStore.setDinnerTime(current.dinnerTime)
            voyageRepository.createVoyage(
                shipName = current.shipName,
                boardDate = current.boardDate,
                boardTime = current.boardTime,
                disembarkDate = current.disembarkDate,
                maxDeck = current.maxDeck,
                minDeck = current.minDeck,
                dinnerTime = current.dinnerTime,
                shipPresetName = shipPresetName,
                isConcierge = current.isConcierge,
                // 联程各程晚数（单程为空列表）：createVoyage 据此生成交接事件、按程生成演出
                legs = current.legs,
                // 登岸兰卡威的程下标：createVoyage 据此在对应 4夜程第三天生成登岸日程
                ashoreLegs = current.ashoreLegs,
            )
            // 引导完成后立即排程：createVoyage 生成的登船/下船/安全演习/晚餐/演出
            // 提醒不等到下次启动才排上（否则新装用户直到重启都收不到任何提醒）。
            reminderScheduler.rescheduleAll()
            settingsStore.setLastViewedDate(current.boardDate)
            settingsStore.setOnboardingCompleted(true)
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
