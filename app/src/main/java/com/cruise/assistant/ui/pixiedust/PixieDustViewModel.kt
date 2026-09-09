package com.cruise.assistant.ui.pixiedust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.PixieDustRepository
import com.cruise.assistant.data.repo.PixieLevelRepository
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.RoomGroup
import com.cruise.assistant.domain.RoomPair
import com.cruise.assistant.domain.autoReplyTarget
import com.cruise.assistant.domain.fullRoom
import com.cruise.assistant.domain.groupGivenByRoom
import com.cruise.assistant.domain.hasGivenForRoom
import com.cruise.assistant.domain.StateroomValidationResult
import com.cruise.assistant.domain.StateroomValidationStatus
import com.cruise.assistant.domain.validateStateroomInput
import com.cruise.assistant.domain.lastLevelForRoom
import com.cruise.assistant.domain.pairByRoom
import com.cruise.assistant.domain.pairByRoomForSweep
import com.cruise.assistant.domain.sortForSweepDelivery
import com.cruise.assistant.domain.sortReceived
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PixieTab { GIVEN, RECEIVED }

enum class GivenViewMode { GROUPED, FLOW }

enum class PixieFilter { ALL, PENDING_ONLY }

/** 键盘下方房间历史统计与回礼提示 */
@Immutable
data class RoomHistory(
    val lastGivenLevelName: String? = null,
    val givenCount: Int = 0,
    val lastReceivedLevelName: String? = null,
    val receivedCount: Int = 0,
    val unrepliedCount: Int = 0,
)
@Immutable
data class PixieDustUiState(
    val loading: Boolean = true,
    val tab: PixieTab = PixieTab.GIVEN,
    val filter: PixieFilter = PixieFilter.ALL,
    val levels: List<PixieLevel> = emptyList(),
    val levelById: Map<Long, PixieLevel> = emptyMap(),
    /** 楼层可选范围：航次 minDeck..maxDeck，未配置回退 1..20 */
    val deckRange: IntRange = 1..20,
    val deck: Int = 1,
    val roomInput: String = "",
    val validation: StateroomValidationResult = StateroomValidationResult(StateroomValidationStatus.INCOMPLETE),
    val selectedLevelId: Long? = null,
    /** 发出记录，时间倒序原始流 */
    val given: List<PixieDustRecord> = emptyList(),
    /** 收到记录，sortReceived 排好 */
    val received: List<PixieDustRecord> = emptyList(),
    /** 发出归组视图数据 */
    val grouped: List<RoomGroup> = emptyList(),
    val viewMode: GivenViewMode = GivenViewMode.GROUPED,
    val keypadCollapsed: Boolean = false,
    val roomHistory: RoomHistory? = null,
    /** 一次性 toast 事件：刚存入的记录。UI 显示后调 onToastShown() 清空 */
    val savedEvent: PixieDustRecord? = null,
    /** 非 null 时 UI 显示回礼等级弹窗（勾选→弹窗两步流） */
    val pendingReply: PixieDustRecord? = null,
    /** 非 null 时 UI toast 提示联动勾选；显示后调 onAutoReplyToastShown() */
    val autoRepliedRoom: String? = null,
    /** 联动检视图数据 */
    val roomPairs: List<RoomPair> = emptyList(),
)

class PixieDustViewModel(
    private val dustRepository: PixieDustRepository,
    private val levelRepository: PixieLevelRepository,
    voyageRepository: VoyageRepository,
    private val settingsStore: SettingsStore,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val tab = MutableStateFlow(PixieTab.GIVEN)
    private val viewMode = MutableStateFlow(GivenViewMode.GROUPED)
    private val keypadCollapsed = MutableStateFlow(false)
    private val deck = MutableStateFlow<Int?>(null)
    private val roomInput = MutableStateFlow("")
    private val selectedLevelId = MutableStateFlow<Long?>(null)
    private val savedEvent = MutableStateFlow<PixieDustRecord?>(null)
    private val pendingReply = MutableStateFlow<PixieDustRecord?>(null)
    private val autoRepliedRoom = MutableStateFlow<String?>(null)
    private val filter = MutableStateFlow(PixieFilter.ALL)

    // 快捷录入防重入守卫（仿 EventSheetViewModel 的 saving 挡板）：
    // 入库协程在途时忽略再次点击，避免连点把同一条记录重复写库
    private var saveInFlight = false

    init {
        // 等级列表加载/变化时：无选择 → 默认选第一个；当前选择被删 → 回落到第一个
        levelRepository.observeAll().onEach { levels ->
            val current = selectedLevelId.value
            if (levels.isEmpty()) {
                selectedLevelId.value = null
            } else if (current == null || levels.none { it.id == current }) {
                selectedLevelId.value = levels.first().id
            }
        }.launchIn(viewModelScope)
    }

    /** 10 个 UI 输入流打包（combine 类型化重载最多 5 个） */
    private val ui = combine(
        tab, viewMode, keypadCollapsed, roomInput, selectedLevelId, deck, savedEvent,
        pendingReply, autoRepliedRoom, filter,
    ) { arr ->
        UiInputs(
            tab = arr[0] as PixieTab,
            viewMode = arr[1] as GivenViewMode,
            keypadCollapsed = arr[2] as Boolean,
            roomInput = arr[3] as String,
            selectedLevelId = arr[4] as Long?,
            deck = arr[5] as Int?,
            savedEvent = arr[6] as PixieDustRecord?,
            pendingReply = arr[7] as PixieDustRecord?,
            autoRepliedRoom = arr[8] as String?,
            filter = arr[9] as PixieFilter,
        )
    }

    private data class UiInputs(
        val tab: PixieTab,
        val viewMode: GivenViewMode,
        val keypadCollapsed: Boolean,
        val roomInput: String,
        val selectedLevelId: Long?,
        val deck: Int?,
        val savedEvent: PixieDustRecord?,
        val pendingReply: PixieDustRecord?,
        val autoRepliedRoom: String?,
        val filter: PixieFilter,
    )

    val state: StateFlow<PixieDustUiState?> = combine(
        dustRepository.observeAll(),
        levelRepository.observeAll(),
        voyageRepository.observeVoyage(),
        settingsStore.settings,
        ui,
    ) { dust, levels, voyage, settings, input ->
        val deckRange =
            if ((voyage?.maxDeck ?: 0) > 0) voyage!!.minDeck..voyage.maxDeck else 1..20
        val deckValue = input.deck
            ?: settings.lastPixieDeck?.takeIf { it in deckRange }
            ?: deckRange.first
        val validation = validateStateroomInput(input.roomInput)
        val deckToUse = validation.deck ?: deckValue
        val given = dust
            .filter { it.direction == PixieDustDirection.GIVEN }
            .sortedWith(
                compareBy<PixieDustRecord> { it.deck }
                    .thenBy { it.room.toIntOrNull() ?: 0 }
                    .thenByDescending { it.timestamp }
            )
        val roomHistory = if (validation.status == StateroomValidationStatus.VALID) {
            val d = validation.deck!!
            val r = validation.room!!
            val roomGiven = dust.filter { it.direction == PixieDustDirection.GIVEN && it.deck == d && it.room == r }
            val roomReceived = dust.filter { it.direction == PixieDustDirection.RECEIVED && it.deck == d && it.room == r }
            val lastGiven = roomGiven.maxByOrNull { it.timestamp }
            val lastReceived = roomReceived.maxByOrNull { it.timestamp }
            val unreplied = roomReceived.count { !it.replied }
            if (roomGiven.isNotEmpty() || roomReceived.isNotEmpty()) {
                RoomHistory(
                    lastGivenLevelName = lastGiven?.let { levels.firstOrNull { l -> l.id == it.levelId }?.name ?: it.levelNameSnapshot },
                    givenCount = roomGiven.size,
                    lastReceivedLevelName = lastReceived?.let { levels.firstOrNull { l -> l.id == it.levelId }?.name ?: it.levelNameSnapshot },
                    receivedCount = roomReceived.size,
                    unrepliedCount = unreplied,
                )
            } else {
                null
            }
        } else {
            null
        }
        val sortedReceived = sortForSweepDelivery(
            dust.filter { it.direction == PixieDustDirection.RECEIVED },
            levels,
        )
        val received = if (input.filter == PixieFilter.PENDING_ONLY) {
            sortedReceived.filter { !it.replied }
        } else {
            sortedReceived
        }
        val sweepPairs = pairByRoomForSweep(
            given = given,
            received = dust.filter { it.direction == PixieDustDirection.RECEIVED },
        ) { rec ->
            levels.firstOrNull { it.id == rec.levelId }?.rank ?: 0
        }
        val roomPairs = if (input.filter == PixieFilter.PENDING_ONLY) {
            sweepPairs.filter { it.unrepliedReceived.isNotEmpty() || it.given.size != it.received.size }
        } else {
            sweepPairs
        }
        PixieDustUiState(
            loading = false,
            tab = input.tab,
            filter = input.filter,
            levels = levels,
            levelById = levels.associateBy { it.id },
            deckRange = deckRange,
            deck = deckToUse,
            roomInput = input.roomInput,
            validation = validation,
            selectedLevelId = input.selectedLevelId,
            given = given,
            received = received,
            grouped = groupGivenByRoom(dust, levels),
            viewMode = input.viewMode,
            keypadCollapsed = input.keypadCollapsed,
            roomHistory = roomHistory,
            savedEvent = input.savedEvent,
            pendingReply = input.pendingReply,
            autoRepliedRoom = input.autoRepliedRoom,
            roomPairs = roomPairs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onTabChange(newTab: PixieTab) {
        tab.value = newTab
    }


    fun onFilterChange(newFilter: PixieFilter) {
        filter.value = newFilter
    }
    /** 换楼层后房号指向不同房间，清空重输 */
    fun onDeckChange(newDeck: Int) {
        deck.value = newDeck
        roomInput.value = ""
        viewModelScope.launch { settingsStore.setLastPixieDeck(newDeck) }
    }

    fun onDigit(d: Char) {
        if (!d.isDigit() || roomInput.value.length >= 5) return
        roomInput.value += d
        if (roomInput.value.length >= 4) {
            val v = validateStateroomInput(roomInput.value)
            if (v.status == StateroomValidationStatus.VALID) {
                val s = state.value ?: return
                val lastGiven = lastLevelForRoom(s.given, v.deck!!, v.room!!)
                val targetLevelId = lastGiven?.levelId
                    ?: s.received.firstOrNull { it.deck == v.deck && it.room == v.room && !it.replied }?.levelId
                targetLevelId
                    ?.takeIf { id -> s.levels.any { it.id == id } }
                    ?.let { selectedLevelId.value = it }
            }
        }
    }

    fun onBackspace() {
        if (roomInput.value.isNotEmpty()) roomInput.value = roomInput.value.dropLast(1)
    }

    fun onClearRoom() {
        roomInput.value = ""
    }

    fun onLevelSelect(levelId: Long) {
        selectedLevelId.value = levelId
    }

    /** 快捷录入：校验通过即入库，房号清空、等级保留（连记场景） */
    fun saveRecord() {
        if (saveInFlight) return
        val s = state.value ?: return
        val v = s.validation
        if (v.status != StateroomValidationStatus.VALID || s.selectedLevelId == null) return
        val level = s.levelById[s.selectedLevelId] ?: return
        val record = PixieDustRecord(
            direction = if (s.tab == PixieTab.GIVEN) PixieDustDirection.GIVEN else PixieDustDirection.RECEIVED,
            deck = v.deck!!,
            room = v.room!!,
            levelId = level.id,
            levelNameSnapshot = level.name,
            replied = false,
            note = "",
            timestamp = nowMs(),
        )
        // 校验全部通过、即将入库才置位：置早了校验失败提前 return 会把守卫永久卡死
        saveInFlight = true
        viewModelScope.launch {
            try {
                val newId = dustRepository.save(record)
                savedEvent.value = record
                roomInput.value = ""
                // 联动方向一：仅发出记录触发（收到 tab 记一笔不该自勾自己）
                if (record.direction == PixieDustDirection.GIVEN) {
                    val all = dustRepository.observeAll().first()
                    val target = autoReplyTarget(
                        all.filter { it.direction == PixieDustDirection.RECEIVED },
                        record.deck, record.room,
                    )
                    if (target != null) {
                        dustRepository.save(target.copy(replied = true))
                        autoRepliedRoom.value = record.fullRoom
                    }
                }
                // 联动方向一镜像：登记收到时该房间已有发出 → 刚登记的这条直接已回礼
                if (record.direction == PixieDustDirection.RECEIVED) {
                    val all = dustRepository.observeAll().first()
                    val hasGiven = hasGivenForRoom(
                        all.filter { it.direction == PixieDustDirection.GIVEN },
                        record.deck, record.room,
                    )
                    if (hasGiven) {
                        dustRepository.save(record.copy(id = newId, replied = true))
                        autoRepliedRoom.value = record.fullRoom
                    }
                }
            } finally {
                // 无论成功/取消/异常都复位，保证守卫不会永久挡住后续保存
                saveInFlight = false
            }
        }
    }

    fun onToggleKeypad() {
        keypadCollapsed.value = !keypadCollapsed.value
    }

    fun onToggleViewMode() {
        viewMode.value =
            if (viewMode.value == GivenViewMode.GROUPED) GivenViewMode.FLOW else GivenViewMode.GROUPED
    }

    fun onToggleReplied(id: Long, replied: Boolean) {
        val record = state.value?.received?.firstOrNull { it.id == id } ?: return
        if (replied) {
            onRequestReply(record)  // 勾选走弹窗两步流
        } else {
            viewModelScope.launch { dustRepository.save(record.copy(replied = false)) }
        }
    }

    /** 列表/编辑弹窗勾选"已回礼" → 进入弹窗两步流（不立即写库） */
    fun onRequestReply(record: PixieDustRecord) {
        pendingReply.value = record
    }

    /**
     * 回礼弹窗确认。level == null 表示"仅勾选"；否则勾选 + 创建一条 GIVEN 记录
     * （不触发方向一联动——单向无循环）。
     */
    fun confirmReply(level: PixieLevel?) {
        val record = pendingReply.value ?: return
        pendingReply.value = null
        viewModelScope.launch {
            dustRepository.save(record.copy(replied = true))
            if (level != null) {
                dustRepository.save(
                    PixieDustRecord(
                        direction = PixieDustDirection.GIVEN,
                        deck = record.deck,
                        room = record.room,
                        levelId = level.id,
                        levelNameSnapshot = level.name,
                        replied = false,
                        note = "",
                        timestamp = nowMs(),
                    ),
                )
            }
        }
    }

    /** 回礼弹窗取消：勾选回退，不写库 */
    fun cancelReply() {
        pendingReply.value = null
    }

    fun onAutoReplyToastShown() {
        autoRepliedRoom.value = null
    }

    /** 编辑弹窗保存：保留原 timestamp（copy 自带） */
    fun saveEdited(record: PixieDustRecord) {
        viewModelScope.launch { dustRepository.save(record) }
    }

    fun deleteRecord(record: PixieDustRecord) {
        viewModelScope.launch { dustRepository.delete(record) }
    }

    fun saveLevel(level: PixieLevel) {
        viewModelScope.launch { levelRepository.save(level) }
    }

    fun deleteLevel(level: PixieLevel) {
        viewModelScope.launch { levelRepository.delete(level) }
    }

    /** 上移/下移等级显示顺序：与相邻项交换 sortOrder */
    fun onMoveLevel(level: PixieLevel, up: Boolean) {
        viewModelScope.launch {
            val list = state.value?.levels ?: return@launch
            val idx = list.indexOfFirst { it.id == level.id }
            val swapWith = if (up) idx - 1 else idx + 1
            if (idx < 0 || swapWith !in list.indices) return@launch
            levelRepository.save(list[idx].copy(sortOrder = swapWith))
            levelRepository.save(list[swapWith].copy(sortOrder = idx))
        }
    }

    /** 等级删除确认用：有多少条记录引用该等级 */
    suspend fun countByLevel(levelId: Long): Int = dustRepository.countByLevel(levelId)

    fun onToastShown() {
        savedEvent.value = null
    }
}
