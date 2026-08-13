package com.cruise.assistant.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.LocationPresetRepository
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.BUILT_IN_LOCATION_CATALOG
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class EventSheetState(
    val editingId: Long = 0,
    val date: LocalDate,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val title: String = "",
    val deck: Int? = null,
    val zone: ShipZone? = ShipZone.MID,
    val placeName: String = "",
    val noLocation: Boolean = true,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val recurring: Boolean = false,
    val presets: List<LocationPreset> = emptyList(),
    val maxDeck: Int = 20,
    val minDeck: Int = 1,
    val disembarkDate: LocalDate? = null,
    val isSystemEvent: Boolean = false,
    val isRecurrenceMember: Boolean = false,
    // 编辑已有事件时从数据库读到的原始 recurrenceGroupId——save() 必须把它带回去，
    // 否则 EventRepository.updateEvent() 收到的 event.recurrenceGroupId 永远是 null，
    // WHOLE_GROUP 分支的 groupId 判断就失效，"修改所有重复项" 在真实 UI 路径上会
    // 静默退化成只改当前这一条（仅测试直接调用 repository 才会走到 fan-out 分支）。
    val recurrenceGroupId: String? = null,
    val errorResId: Int? = null,
    val saving: Boolean = false,
    /** 当前选中类别的 iconKey（决定是否有预置地点下拉） */
    val categoryIconKey: String? = null,
    /** 是否内置船（探险号），内置才有预置地点 */
    val builtInShip: Boolean = false,
    /** 是否处于预置地点下拉模式；false = 自由输入 */
    val locationPresetMode: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != 0L

    /** 循环终止日 = 下船日前一天（需求6 澄清：下船日不含） */
    val recurUntil: LocalDate? get() = disembarkDate?.minusDays(1)
}

class EventSheetViewModel(
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository,
    private val locationPresetRepository: LocationPresetRepository,
    private val voyageRepository: VoyageRepository,
    private val reminderScheduler: ReminderScheduler,
    date: LocalDate,
    defaultStartMinute: Int,
    private val editingEventId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EventSheetState(
            // 提前用 editingEventId 占位，让 isEditing 从第一次 composition 起就正确
            // （标题/字段仍在 init 里异步加载），避免编辑已有事件时先闪一下"添加事件"UI。
            editingId = editingEventId,
            date = date,
            startTime = LocalTime.of(defaultStartMinute / 60, defaultStartMinute % 60),
            endTime = LocalTime.of(
                ((defaultStartMinute + 30) / 60).coerceAtMost(23),
                (defaultStartMinute + 30) % 60,
            ),
        ),
    )
    val state: StateFlow<EventSheetState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val voyage = voyageRepository.getVoyage()
            val builtIn = voyage?.shipName in com.cruise.assistant.domain.BUILT_IN_SHIPS.map { s -> s.name }
            _state.update {
                it.copy(
                    maxDeck = voyage?.maxDeck ?: 20,
                    minDeck = voyage?.minDeck ?: 1,
                    disembarkDate = voyage?.disembarkDate,
                    builtInShip = builtIn,
                )
            }
        }
        viewModelScope.launch {
            categoryRepository.observeActive().collect { list ->
                _state.update { current ->
                    // 新建事件初始 selectedCategoryId 为 null：用 fallback 到第一个类别的 id
                    // 计算 iconKey，否则默认类别（餐食）永远拿不到下拉模式
                    val resolvedId = current.selectedCategoryId ?: list.firstOrNull()?.id
                    val iconKey = list.firstOrNull { it.id == resolvedId }?.iconKey
                    current.copy(
                        categories = list,
                        categoryIconKey = iconKey,
                        selectedCategoryId = resolvedId,
                        locationPresetMode = resolvePresetMode(
                            isEditing = current.isEditing,
                            builtInShip = current.builtInShip,
                            iconKey = iconKey,
                            currentMode = current.locationPresetMode,
                            deck = current.deck,
                            zone = current.zone,
                            placeName = current.placeName,
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            locationPresetRepository.observeAll().collect { presets ->
                _state.update { it.copy(presets = presets) }
            }
        }
        if (editingEventId != 0L) {
            viewModelScope.launch {
                eventRepository.getById(editingEventId)?.let { event ->
                    val endMinutes = event.startTime.hour * 60 + event.startTime.minute +
                        event.durationMinutes
                    _state.update {
                        it.copy(
                            editingId = event.id,
                            date = event.date,
                            selectedCategoryId = event.categoryId,
                            title = event.title,
                            deck = event.deck,
                            zone = event.zone ?: ShipZone.MID,
                            placeName = event.placeName.orEmpty(),
                            noLocation = event.deck == null,
                            startTime = event.startTime,
                            endTime = LocalTime.of(
                                (endMinutes / 60).coerceAtMost(23),
                                endMinutes % 60,
                            ),
                            isSystemEvent = event.isSystemGenerated,
                            isRecurrenceMember = event.recurrenceGroupId != null,
                            recurrenceGroupId = event.recurrenceGroupId,
                        )
                    }
                    _state.update { current ->
                        val iconKey = current.categories
                            .firstOrNull { it.id == current.selectedCategoryId }?.iconKey
                        current.copy(
                            categoryIconKey = iconKey,
                            locationPresetMode = resolvePresetMode(
                                isEditing = true,
                                builtInShip = current.builtInShip,
                                iconKey = iconKey,
                                currentMode = current.locationPresetMode,
                                deck = current.deck,
                                zone = current.zone,
                                placeName = current.placeName,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun onCategorySelect(id: Long) = _state.update {
        val iconKey = it.categories.firstOrNull { c -> c.id == id }?.iconKey
        val presetAvailable = it.builtInShip && iconKey in BUILT_IN_LOCATION_CATALOG.keys
        it.copy(
            selectedCategoryId = id,
            categoryIconKey = iconKey,
            // 切到演出/餐饮时默认进下拉模式；其他类别退回自由输入
            locationPresetMode = presetAvailable,
            errorResId = null,
        )
    }
    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errorResId = null) }
    fun onDeckChange(value: Int) = _state.update { it.copy(deck = value, noLocation = false) }
    fun onZoneChange(value: ShipZone) = _state.update { it.copy(zone = value, noLocation = false) }
    fun onPlaceNameChange(value: String) = _state.update {
        it.copy(placeName = value, locationPresetMode = false)
    }
    fun onRecurringToggle(value: Boolean) = _state.update { it.copy(recurring = value) }

    fun onSelectPresetLocation(entry: com.cruise.assistant.domain.LocationCatalogEntry) = _state.update {
        it.copy(
            deck = entry.deck,
            zone = entry.zone,
            placeName = entry.placeName,
            noLocation = false,
            locationPresetMode = true,
        )
    }

    fun onCustomLocationSelected() = _state.update { it.copy(locationPresetMode = false) }

    /**
     * Fix round 2 防线二：系统事件（上船/下船）没有地点，UI 已经不渲染 LocationPicker，
     * 但如果日后有人重新接上这个回调（或测试直接调用 ViewModel），这里再挡一层——
     * isSystemEvent 时忽略切换，强制保持 noLocation=true/deck=null，避免 save() 写出
     * zone='MID' 的幽灵地点。
     */
    fun onNoLocationToggle(value: Boolean) = _state.update {
        if (it.isSystemEvent) {
            it.copy(noLocation = true, deck = null)
        } else if (value) {
            it.copy(noLocation = true, deck = null)
        } else {
            // 重新启用地点：deck 已被清空且没有备份，zone/placeName 一并清掉，
            // 否则保存时 deck=null + zone/placeName 残留会写出"幽灵地点"
            // （卡片不显示地点、再编辑时"不设地点"又被勾上）。
            if (it.deck == null) {
                it.copy(noLocation = false, zone = null, placeName = "")
            } else {
                it.copy(noLocation = false)
            }
        }
    }

    fun onStartChange(value: LocalTime) = _state.update { current ->
        // 开始时间钳到 23:29：保证默认 30 分钟事件以 ≤23:59 结束，
        // 否则滚到 23:30+ 后结束时间回卷、保存必报"结束早于开始"
        val clamped = if (value.isAfter(LocalTime.of(23, 29))) LocalTime.of(23, 29) else value
        val end = if (!current.endTime.isAfter(clamped)) {
            clamped.plusMinutes(30)
        } else {
            current.endTime
        }
        current.copy(startTime = clamped, endTime = end, errorResId = null)
    }

    fun onEndChange(value: LocalTime) = _state.update { it.copy(endTime = value, errorResId = null) }

    /**
     * scope 仅在编辑循环组成员时生效。
     * 用 s.saving 挡快速二次点击：保存期间再次调用直接忽略，避免插入两次。
     */
    fun save(scope: RecurrenceScope, onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return

        val categoryId = s.selectedCategoryId
        if (categoryId == null) {
            _state.update { it.copy(errorResId = R.string.error_no_category) }
            return
        }
        if (s.title.isBlank()) {
            _state.update { it.copy(errorResId = R.string.error_title_required) }
            return
        }
        val startMin = s.startTime.hour * 60 + s.startTime.minute
        val endMin = s.endTime.hour * 60 + s.endTime.minute
        if (endMin <= startMin) {
            _state.update { it.copy(errorResId = R.string.error_end_before_start) }
            return
        }

        val event = CruiseEvent(
            id = s.editingId,
            date = s.date,
            startTime = s.startTime,
            durationMinutes = endMin - startMin,
            categoryId = categoryId,
            title = s.title.trim(),
            deck = if (s.noLocation) null else s.deck,
            zone = if (s.noLocation) null else s.zone,
            placeName = if (s.noLocation) null else s.placeName.trim().takeIf { it.isNotEmpty() },
            isSystemGenerated = s.isSystemEvent,
            // 必须带上原始 recurrenceGroupId：EventRepository.updateEvent() 靠这个字段
            // （而不是数据库里 target 行的字段）判断 WHOLE_GROUP 是否要 fan out 到整组。
            recurrenceGroupId = s.recurrenceGroupId,
        )

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (s.isEditing) {
                eventRepository.updateEvent(event, scope)
            } else {
                eventRepository.addEvent(event, recurring = s.recurring)
            }
            // 记住地点，下次可直接选
            val deck = event.deck
            val zone = event.zone
            val place = event.placeName
            if (deck != null && zone != null && place != null) {
                locationPresetRepository.remember(deck, zone, place, System.currentTimeMillis())
            }
            reminderScheduler.rescheduleAll()
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }

    /**
     * 幂等收敛：是否处于预置地点下拉模式。
     * 非内置船或类别无目录 → false；新建事件（演出/餐饮）→ true（默认下拉）；
     * 编辑事件 → 地点与目录某项匹配、或已在下拉模式时为 true，否则 false（自定义）。
     */
    private fun resolvePresetMode(
        isEditing: Boolean,
        builtInShip: Boolean,
        iconKey: String?,
        currentMode: Boolean,
        deck: Int?,
        zone: ShipZone?,
        placeName: String?,
    ): Boolean {
        if (!builtInShip || iconKey !in BUILT_IN_LOCATION_CATALOG.keys) return false
        if (!isEditing) return true
        val key = iconKey ?: return false
        val matched = BUILT_IN_LOCATION_CATALOG.getValue(key).any { entry ->
            entry.deck == deck && entry.zone == zone && entry.placeName == placeName
        }
        return matched || currentMode
    }
}
