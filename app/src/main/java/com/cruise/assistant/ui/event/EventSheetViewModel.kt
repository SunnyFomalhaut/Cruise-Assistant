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
import com.cruise.assistant.domain.DISNEY_ADVENTURE_SHOWS
import com.cruise.assistant.domain.LocationCatalog
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.MealPeriod
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.ShowPreset
import com.cruise.assistant.domain.effectiveWaitMinutes
import com.cruise.assistant.domain.isOpenAt
import com.cruise.assistant.domain.isOpenFor
import com.cruise.assistant.domain.resolveDefaultWaitMinutes
import com.cruise.assistant.reminder.ReminderScheduler
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Immutable
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
    val presetKey: String? = null,
    val errorResId: Int? = null,
    val saving: Boolean = false,
    /** 当前选中类别的 iconKey（决定是否有预置地点下拉） */
    val categoryIconKey: String? = null,
    /** 是否内置船（探险号），内置才有预置地点 */
    val builtInShip: Boolean = false,
    /** 是否处于预置地点下拉模式；false = 自由输入 */
    val locationPresetMode: Boolean = false,
    /** 是否处于演出预置下拉模式；false = 自由输入 */
    val showPresetMode: Boolean = false,
    /** 餐次二级选项（仅餐食类别显示）；默认晚餐（餐食事件最常见的预设），null = 全部 */
    val selectedMealPeriod: MealPeriod? = MealPeriod.DINNER,
    /** 是否餐食类别且内置船（决定是否显示餐次行） */
    val showMealPeriodRow: Boolean = false,
    /** 营业时间冲突待用户决策 */
    val hoursCheckPending: Boolean = false,
    val hoursCheckMessage: String? = null,
    /** 用户确认"仍要保存"后为 true：跳过本次校验 */
    val hoursCheckOverride: Boolean = false,
    /** 提前候场时间（分钟），默认 30，演出类别时可选 10~120m */
    val waitMinutes: Int = 30,
    val note: String = "",
    val lane: Int = 0,
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
            // 内置船判定：船名匹配内置列表，或船名为空（老版本引导没有"选船"概念、
            // 船名留空——本 App 只有一个内置船，老航次视为探险号以启用预置地点）
            val builtIn = voyage?.shipName in com.cruise.assistant.domain.BUILT_IN_SHIPS.map { s -> s.name }
                || (voyage != null && voyage.shipName.isNullOrBlank())
            _state.update {
                it.copy(
                    maxDeck = voyage?.maxDeck ?: 20,
                    minDeck = voyage?.minDeck ?: 1,
                    disembarkDate = voyage?.disembarkDate,
                    builtInShip = builtIn,
                )
            }
            // builtInShip 更新后补一次收敛：若 categories 流先发射（builtInShip 还是默认
            // false），餐次行/预置下拉就不会显示且不再恢复——这里用最新值重算一遍。
            _state.update { current ->
                val iconKey = current.categories
                    .firstOrNull { it.id == current.selectedCategoryId }?.iconKey
                current.copy(
                    categoryIconKey = current.categoryIconKey ?: iconKey,
                    showMealPeriodRow = current.builtInShip && iconKey == "meal",
                    locationPresetMode = resolvePresetMode(
                        isEditing = current.isEditing,
                        builtInShip = current.builtInShip,
                        iconKey = iconKey,
                        currentMode = current.locationPresetMode,
                        deck = current.deck,
                        zone = current.zone,
                        placeName = current.placeName,
                    ),
                    showPresetMode = resolveShowPresetMode(
                        isEditing = current.isEditing,
                        builtInShip = current.builtInShip,
                        iconKey = current.categoryIconKey ?: iconKey,
                        title = current.title,
                    ),
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
                    val mealPeriod = if (current.isEditing && (iconKey == "meal" || current.presetKey == "dinner") && current.selectedMealPeriod == null) {
                        resolveMealPeriod(current.title, current.startTime)
                    } else {
                        current.selectedMealPeriod
                    }
                    current.copy(
                        categories = list,
                        categoryIconKey = iconKey,
                        selectedCategoryId = resolvedId,
                        selectedMealPeriod = mealPeriod,
                        showMealPeriodRow = current.builtInShip && iconKey == "meal",
                        locationPresetMode = resolvePresetMode(
                            isEditing = current.isEditing,
                            builtInShip = current.builtInShip,
                            iconKey = iconKey,
                            currentMode = current.locationPresetMode,
                            deck = current.deck,
                            zone = current.zone,
                            placeName = current.placeName,
                        ),
                        showPresetMode = resolveShowPresetMode(
                            isEditing = current.isEditing,
                            builtInShip = current.builtInShip,
                            iconKey = iconKey,
                            title = current.title,
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
                    val resolvedMeal = resolveMealPeriod(event.title, event.startTime)
                    val catIconKey = categoryRepository.getById(event.categoryId)?.iconKey
                    _state.update {
                        it.copy(
                            editingId = event.id,
                            date = event.date,
                            selectedCategoryId = event.categoryId,
                            title = event.title,
                            deck = event.deck,
                            zone = event.zone ?: ShipZone.MID,
                            placeName = event.placeName.orEmpty(),
                            noLocation = event.deck == null && event.zone == null && event.placeName == null,
                            startTime = event.startTime,
                            endTime = LocalTime.of(
                                (endMinutes / 60).coerceAtMost(23),
                                endMinutes % 60,
                            ),
                            isSystemEvent = event.isSystemGenerated,
                            isRecurrenceMember = event.recurrenceGroupId != null,
                            recurrenceGroupId = event.recurrenceGroupId,
                            presetKey = event.presetKey,
                            waitMinutes = event.waitMinutes ?: event.effectiveWaitMinutes,
                            note = event.note.orEmpty(),
                            lane = event.lane,
                        )
                    }
                    _state.update { current ->
                        val iconKey = current.categories
                            .firstOrNull { it.id == current.selectedCategoryId }?.iconKey ?: catIconKey
                        current.copy(
                            categoryIconKey = iconKey,
                            selectedMealPeriod = if (iconKey == "meal" || event.presetKey == "dinner") resolvedMeal else null,
                            showMealPeriodRow = current.builtInShip && iconKey == "meal",
                            locationPresetMode = resolvePresetMode(
                                isEditing = true,
                                builtInShip = current.builtInShip,
                                iconKey = iconKey,
                                currentMode = current.locationPresetMode,
                                deck = current.deck,
                                zone = current.zone,
                                placeName = current.placeName,
                            ),
                            showPresetMode = resolveShowPresetMode(
                                isEditing = true,
                                builtInShip = current.builtInShip,
                                iconKey = iconKey,
                                title = current.title,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun onCategorySelect(id: Long) = _state.update {
        val iconKey = it.categories.firstOrNull { c -> c.id == id }?.iconKey
        val presetAvailable = it.builtInShip && LocationCatalog.venuesByCategory().containsKey(iconKey)
        val showPreset = it.builtInShip && iconKey == "show"
        val waitMins = if (iconKey == "show") {
            resolveDefaultWaitMinutes(it.placeName, it.deck, it.title)
        } else {
            it.waitMinutes
        }
        it.copy(
            selectedCategoryId = id,
            categoryIconKey = iconKey,
            // 切到演出/餐饮时默认进下拉模式；其他类别退回自由输入
            locationPresetMode = presetAvailable,
            showPresetMode = showPreset,
            // 餐次行仅餐食类别显示；切类别时重置餐次选择
            selectedMealPeriod = null,
            showMealPeriodRow = it.builtInShip && iconKey == "meal",
            waitMinutes = waitMins,
            errorResId = null,
        )
    }

    fun onMealPeriodSelect(period: MealPeriod?) = _state.update {
        // 切换餐次后若当前已选地点不在新餐次营业列表（或已被过滤掉），清空地点选择，
        // 否则用户看着 stale 的 venue 直接保存。未配置营业时间的 venue 恒 open → 保留。
        val entries = LocationCatalog.venuesByCategory()["meal"].orEmpty()
        val currentVenueOpen = period == null || entries
            .firstOrNull { e -> e.deck == it.deck && e.zone == it.zone && e.placeName == it.placeName }
            ?.isOpenFor(period) != false
        // 餐次联动起止时间：优先从保留地点在该餐次的营业时段取开始时间（如动画师早餐
        // 08:30），否则用规范默认值（早餐 08:00、午餐 12:00、晚餐 17:45、夜宵 22:00，
        // 各 30 分钟）——避免自动时间落在营业时段外（如帕洛午餐 12:00 在外）触发自家校验。
        // 全部/零食/酒水不调整时间。
        val defaultStart = when (period) {
            MealPeriod.BREAKFAST -> LocalTime.of(8, 0)
            MealPeriod.LUNCH -> LocalTime.of(12, 0)
            MealPeriod.DINNER -> LocalTime.of(17, 45)
            MealPeriod.LATE_NIGHT -> LocalTime.of(22, 0)
            else -> null
        }
        val (newStart, newEnd) = if (defaultStart != null && period != null) {
            // isOpenFor(period) 确保 slot 匹配该餐次；venue 已被保留说明营业。
            val venueSlotStart = entries
                .firstOrNull { e -> e.deck == it.deck && e.zone == it.zone && e.placeName == it.placeName }
                ?.let { e -> e.hours?.slots?.firstOrNull { s -> e.isOpenFor(period) && s.meal == period }?.start }
            val s = venueSlotStart ?: defaultStart
            s to s.plusMinutes(30)
        } else {
            it.startTime to it.endTime
        }
        val newTitle = computeMealPeriodTitle(it.title, period)
        it.copy(
            selectedMealPeriod = period,
            title = newTitle,
            deck = if (currentVenueOpen) it.deck else null,
            zone = if (currentVenueOpen) it.zone else null,
            placeName = if (currentVenueOpen) it.placeName else "",
            startTime = newStart,
            endTime = newEnd,
        )
    }
    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errorResId = null) }
    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }
    fun onDeckChange(value: Int) = _state.update { it.copy(deck = value, noLocation = false) }
    fun onZoneChange(value: ShipZone) = _state.update { it.copy(zone = value, noLocation = false) }
    fun onPlaceNameChange(value: String) = _state.update {
        it.copy(placeName = value, locationPresetMode = false)
    }
    fun onRecurringToggle(value: Boolean) = _state.update { it.copy(recurring = value) }

    fun onSelectPresetLocation(entry: com.cruise.assistant.domain.LocationCatalogEntry) = _state.update {
        val waitMins = if (it.categoryIconKey == "show") {
            resolveDefaultWaitMinutes(entry.placeName, entry.deck, it.title)
        } else {
            it.waitMinutes
        }
        it.copy(
            deck = entry.deck,
            zone = entry.zone,
            placeName = entry.placeName,
            noLocation = false,
            locationPresetMode = true,
            waitMinutes = waitMins,
        )
    }

    fun onCustomLocationSelected() = _state.update { it.copy(locationPresetMode = false) }

    fun onShowPresetSelect(preset: ShowPreset) = _state.update {
        val start = preset.startTimes.first()
        val endMinutes = start.hour * 60 + start.minute + preset.durationMinutes
        val end = LocalTime.of((endMinutes / 60).coerceAtMost(23), endMinutes % 60)
        it.copy(
            title = preset.title,
            deck = preset.deck,
            zone = preset.zone,
            placeName = preset.placeName,
            noLocation = false,
            locationPresetMode = true,
            startTime = start,
            endTime = end,
            showPresetMode = true,
            waitMinutes = preset.defaultWaitMinutes,
            errorResId = null,
        )
    }

    fun onCustomShowSelected() = _state.update {
        it.copy(showPresetMode = false)
    }

    fun onWaitMinutesChange(value: Int) = _state.update {
        it.copy(waitMinutes = value)
    }

    /**
     * Fix round 2 防线二：系统事件（登船/下船）没有地点，UI 已经不渲染 LocationPicker，
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

    /** 用户选择"仍要保存"：绕过校验继续 */
    fun onHoursCheckOverride() = _state.update {
        it.copy(hoursCheckPending = false, hoursCheckMessage = null, hoursCheckOverride = true)
    }

    /** 用户选择"返回修改" */
    fun onHoursCheckDismiss() = _state.update {
        it.copy(hoursCheckPending = false, hoursCheckMessage = null)
    }

    /**
     * scope 仅在编辑循环组成员时生效。
     * 用 s.saving 挡快速二次点击：保存期间再次调用直接忽略，避免插入两次。
     */
    fun save(scope: RecurrenceScope, onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        // 营业时间冲突弹窗未决时忽略再次保存（防重复触发）
        if (s.hoursCheckPending) return

        val categoryId = s.selectedCategoryId
        if (categoryId == null) {
            _state.update { it.copy(errorResId = R.string.error_no_category) }
            return
        }
        val startMin = s.startTime.hour * 60 + s.startTime.minute
        val endMin = s.endTime.hour * 60 + s.endTime.minute
        if (endMin <= startMin) {
            _state.update { it.copy(errorResId = R.string.error_end_before_start) }
            return
        }

        // 营业时间校验：任意类别预置地点 + 已配置营业时间 + 开始时间不在营业时段内 → 提示。
        // hoursCheckOverride（用户点过"仍要保存"）时整体跳过，避免反复弹窗。
        if (!s.hoursCheckOverride) {
            val matchedVenue = if (!s.noLocation && s.deck != null && s.categoryIconKey != null) {
                LocationCatalog.venuesByCategory()[s.categoryIconKey]?.firstOrNull { entry ->
                    entry.deck == s.deck && entry.zone == s.zone && entry.placeName == s.placeName
                }
            } else {
                null
            }
            val venue = matchedVenue
            val openNow = venue == null || venue.hours?.isConfigured != true || venue.isOpenAt(s.startTime)
            if (!openNow) {
                val hoursText = venue.hours?.slots?.joinToString(" / ") { "${it.start}-${it.end}" }
                _state.update {
                    it.copy(
                        hoursCheckPending = true,
                        hoursCheckMessage = "该地点 ${s.startTime} 不营业，营业时间：$hoursText",
                    )
                }
                return
            }
        }

        // 空标题允许：餐食类别的空标题兜底为餐次名（主界面直接显示二级标题）；
        // 非餐食类别或无餐次 → 空着（用户要求不再抄餐厅名/类别名）
        val fallbackTitle = if (s.categoryIconKey == "meal") s.selectedMealPeriod?.label.orEmpty() else ""
        val newPresetKey = computePresetKey(s.categoryIconKey, s.selectedMealPeriod, s.presetKey)
        val event = CruiseEvent(
            id = s.editingId,
            date = s.date,
            startTime = s.startTime,
            durationMinutes = endMin - startMin,
            categoryId = categoryId,
            title = s.title.trim().ifEmpty { fallbackTitle },
            deck = if (s.noLocation) null else s.deck,
            zone = if (s.noLocation) null else s.zone,
            placeName = if (s.noLocation) null else s.placeName.trim().takeIf { it.isNotEmpty() },
            isSystemGenerated = s.isSystemEvent,
            // 必须带上原始 recurrenceGroupId：EventRepository.updateEvent() 靠这个字段
            // （而不是数据库里 target 行的字段）判断 WHOLE_GROUP 是否要 fan out 到整组。
            recurrenceGroupId = s.recurrenceGroupId,
            presetKey = newPresetKey,
            waitMinutes = if (s.categoryIconKey == "show") s.waitMinutes else null,
            note = s.note.trim().takeIf { it.isNotEmpty() },
            lane = s.lane,
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
        // 快照一份目录：containsKey 与 getValue 必须看同一份 map，
        // 否则期间 setCatalog 换 map 时 getValue 可能 KeyNotFound
        val catalog = LocationCatalog.venuesByCategory()
        if (!builtInShip || !catalog.containsKey(iconKey)) return false
        // 新建与编辑都默认预置下拉：编辑预置晚餐（无地点）也能直接选餐厅地点，
        // 而非掉回自由输入。选中高亮由 LocationPicker 按 deck/zone/placeName 匹配。
        return true
    }

    /**
     * 幂等收敛：是否处于演出预置下拉模式。
     * 非内置船或非演出类别 → false；新建事件 → true（默认预置下拉）；
     * 编辑已有事件 → 标题匹配 8 部预置之一为 true，否则 false（自定义演出自由输入）。
     */
    private fun resolveShowPresetMode(
        isEditing: Boolean,
        builtInShip: Boolean,
        iconKey: String?,
        title: String,
    ): Boolean {
        if (!builtInShip || iconKey != "show") return false
        return if (isEditing) {
            DISNEY_ADVENTURE_SHOWS.any { it.title == title }
        } else {
            true
        }
    }
}

/**
 * 根据事件标题或开始时间解析所属餐次。
 */
fun resolveMealPeriod(title: String, startTime: LocalTime): MealPeriod {
    return MealPeriod.entries.firstOrNull { it.label == title }
        ?: when {
            startTime.isBefore(LocalTime.of(10, 30)) -> MealPeriod.BREAKFAST
            startTime.isBefore(LocalTime.of(15, 0)) -> MealPeriod.LUNCH
            startTime.isBefore(LocalTime.of(21, 30)) -> MealPeriod.DINNER
            else -> MealPeriod.LATE_NIGHT
        }
}

/**
 * 切换餐次时同步更新标题：若当前标题为空或为任一餐次默认名，更新为新餐次名；否则保留用户自定义标题。
 */
fun computeMealPeriodTitle(currentTitle: String, period: MealPeriod?): String {
    val isDefaultMealTitle = currentTitle.isBlank() || MealPeriod.entries.any { p -> p.label == currentTitle }
    return if (isDefaultMealTitle && period != null) period.label else currentTitle
}

/**
 * 保存时计算 presetKey：餐食类别仅晚餐保留 "dinner"（用于统一批量修改晚餐时间），其余餐次设为 null；非餐食类别保留原值。
 */
fun computePresetKey(categoryIconKey: String?, selectedMealPeriod: MealPeriod?, currentPresetKey: String?): String? {
    return if (categoryIconKey == "meal") {
        if (selectedMealPeriod == MealPeriod.DINNER) "dinner" else null
    } else {
        currentPresetKey
    }
}

