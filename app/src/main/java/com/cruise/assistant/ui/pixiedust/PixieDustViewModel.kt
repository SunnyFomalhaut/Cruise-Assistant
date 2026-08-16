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
import com.cruise.assistant.domain.isValidRoomInput
import com.cruise.assistant.domain.lastLevelForRoom
import com.cruise.assistant.domain.pairByRoom
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

/** 键盘下方"上次：豪华 ×2"的数据 */
@Immutable
data class RoomHistory(val levelName: String, val count: Int)

@Immutable
data class PixieDustUiState(
    val loading: Boolean = true,
    val tab: PixieTab = PixieTab.GIVEN,
    val levels: List<PixieLevel> = emptyList(),
    val levelById: Map<Long, PixieLevel> = emptyMap(),
    /** 楼层可选范围：航次 minDeck..maxDeck，未配置回退 1..20 */
    val deckRange: IntRange = 1..20,
    val deck: Int = 1,
    val roomInput: String = "",
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

    /** 9 个 UI 输入流打包（combine 类型化重载最多 5 个） */
    private val ui = combine(
        tab, viewMode, keypadCollapsed, roomInput, selectedLevelId, deck, savedEvent,
        pendingReply, autoRepliedRoom,
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
        val given = dust
            .filter { it.direction == PixieDustDirection.GIVEN }
            .sortedByDescending { it.timestamp }
        val roomHistory = if (isValidRoomInput(input.roomInput)) {
            lastLevelForRoom(dust, deckValue, input.roomInput)?.let { last ->
                RoomHistory(
                    levelName = levels.firstOrNull { it.id == last.levelId }?.name
                        ?: last.levelNameSnapshot,
                    count = given.count { it.deck == deckValue && it.room == input.roomInput },
                )
            }
        } else {
            null
        }
        PixieDustUiState(
            loading = false,
            tab = input.tab,
            levels = levels,
            levelById = levels.associateBy { it.id },
            deckRange = deckRange,
            deck = deckValue,
            roomInput = input.roomInput,
            selectedLevelId = input.selectedLevelId,
            given = given,
            received = sortReceived(dust, levels),
            grouped = groupGivenByRoom(dust, levels),
            viewMode = input.viewMode,
            keypadCollapsed = input.keypadCollapsed,
            roomHistory = roomHistory,
            savedEvent = input.savedEvent,
            pendingReply = input.pendingReply,
            autoRepliedRoom = input.autoRepliedRoom,
            roomPairs = pairByRoom(given, dust.filter { it.direction == PixieDustDirection.RECEIVED }) { rec ->
                levels.firstOrNull { it.id == rec.levelId }?.rank ?: 0
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onTabChange(newTab: PixieTab) {
        tab.value = newTab
    }

    /** 换楼层后房号指向不同房间，清空重输 */
    fun onDeckChange(newDeck: Int) {
        deck.value = newDeck
        roomInput.value = ""
        viewModelScope.launch { settingsStore.setLastPixieDeck(newDeck) }
    }

    fun onDigit(d: Char) {
        if (!d.isDigit() || roomInput.value.length >= 3) return
        roomInput.value += d
        // 房号输满三位且该房间有历史 → 预选上次等级
        if (roomInput.value.length == 3) {
            val s = state.value ?: return
            // 等级已删则不再预选（避免记一笔点亮但静默无效）
            lastLevelForRoom(s.given, s.deck, roomInput.value)
                ?.takeIf { last -> s.levels.any { it.id == last.levelId } }
                ?.let { selectedLevelId.value = it.levelId }
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
        val s = state.value ?: return
        if (!isValidRoomInput(s.roomInput) || s.selectedLevelId == null) return
        val level = s.levelById[s.selectedLevelId] ?: return
        val record = PixieDustRecord(
            direction = if (s.tab == PixieTab.GIVEN) PixieDustDirection.GIVEN else PixieDustDirection.RECEIVED,
            deck = s.deck,
            room = s.roomInput,
            levelId = level.id,
            levelNameSnapshot = level.name,
            replied = false,
            note = "",
            timestamp = nowMs(),
        )
        viewModelScope.launch {
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
