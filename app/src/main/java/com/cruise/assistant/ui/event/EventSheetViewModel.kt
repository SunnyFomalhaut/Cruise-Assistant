package com.cruise.assistant.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.LocationPresetRepository
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone
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
    val disembarkDate: LocalDate? = null,
    val isSystemEvent: Boolean = false,
    val isRecurrenceMember: Boolean = false,
    val errorResId: Int? = null,
    val saving: Boolean = false,
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
            _state.update {
                it.copy(maxDeck = voyage?.maxDeck ?: 20, disembarkDate = voyage?.disembarkDate)
            }
        }
        viewModelScope.launch {
            categoryRepository.observeActive().collect { list ->
                _state.update { current ->
                    current.copy(
                        categories = list,
                        selectedCategoryId = current.selectedCategoryId
                            ?: list.firstOrNull()?.id,
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
                        )
                    }
                }
            }
        }
    }

    fun onCategorySelect(id: Long) = _state.update { it.copy(selectedCategoryId = id, errorResId = null) }
    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errorResId = null) }
    fun onDeckChange(value: Int) = _state.update { it.copy(deck = value, noLocation = false) }
    fun onZoneChange(value: ShipZone) = _state.update { it.copy(zone = value, noLocation = false) }
    fun onPlaceNameChange(value: String) = _state.update { it.copy(placeName = value) }
    fun onRecurringToggle(value: Boolean) = _state.update { it.copy(recurring = value) }

    fun onNoLocationToggle(value: Boolean) = _state.update {
        if (value) it.copy(noLocation = true, deck = null) else it.copy(noLocation = false)
    }

    fun onStartChange(value: LocalTime) = _state.update { current ->
        // 开始时间往后越过结束时间时，结束时间跟着推 30 分钟
        val end = if (!current.endTime.isAfter(value)) {
            value.plusMinutes(30).takeIf { it.isAfter(value) } ?: LocalTime.of(23, 59)
        } else {
            current.endTime
        }
        current.copy(startTime = value, endTime = end, errorResId = null)
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
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
