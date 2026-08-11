package com.cruise.assistant.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.BUILT_IN_SHIPS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

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
    val saving: Boolean = false,
    /** 已本地化的错误提示 resId，null 表示无错误 */
    val errorResId: Int? = null,
) {
    /** 是否选中了内置船（selectedShipIndex >= 0） */
    val isBuiltInShip: Boolean get() = selectedShipIndex >= 0
}

class OnboardingViewModel(
    private val voyageRepository: VoyageRepository,
    private val settingsStore: SettingsStore,
    today: LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingState(boardDate = today, disembarkDate = today.plusDays(5)),
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onShipNameChange(value: String) = _state.update { it.copy(shipName = value) }

    fun onShipSelected(index: Int) = _state.update { current ->
        if (index < 0 || index >= BUILT_IN_SHIPS.size) {
            // 自定义船名
            current.copy(
                selectedShipIndex = -1,
                selectedScheduleIndex = -1,
                shipName = "",
                maxDeck = 20,
                minDeck = 1,
            )
        } else {
            val ship = BUILT_IN_SHIPS[index]
            current.copy(
                selectedShipIndex = index,
                selectedScheduleIndex = -1,
                shipName = ship.name,
                maxDeck = ship.maxDeck ?: current.maxDeck,
                minDeck = ship.minDeck ?: 1,
            )
        }
    }

    fun onScheduleSelected(scheduleIndex: Int) = _state.update { current ->
        val shipIndex = current.selectedShipIndex
        if (shipIndex < 0 || shipIndex >= BUILT_IN_SHIPS.size) return@update current
        val ship = BUILT_IN_SHIPS[shipIndex]
        if (scheduleIndex < 0 || scheduleIndex >= ship.schedules.size) {
            // 自定义船期
            current.copy(selectedScheduleIndex = -1)
        } else {
            val schedule = ship.schedules[scheduleIndex]
            val today = current.boardDate
            val boardDate = schedule.boardDateFrom(today)
            val disembarkDate = schedule.disembarkDateFrom(today)
            current.copy(
                selectedScheduleIndex = scheduleIndex,
                boardDate = boardDate,
                disembarkDate = disembarkDate,
                boardTime = schedule.boardTime,
            )
        }
    }

    fun onDinnerTimeChange(time: LocalTime) = _state.update { it.copy(dinnerTime = time) }

    fun onBoardDateChange(value: LocalDate) = _state.update { current ->
        // 选了船期时按 nights 自动算下船日；否则只做不早于上船的保护。
        val disembark = if (current.selectedScheduleIndex >= 0) {
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

    fun onBoardTimeChange(value: LocalTime) = _state.update { it.copy(boardTime = value) }

    fun onDisembarkDateChange(value: LocalDate) = _state.update {
        it.copy(disembarkDate = value, errorResId = null)
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
            voyageRepository.createVoyage(
                shipName = current.shipName,
                boardDate = current.boardDate,
                boardTime = current.boardTime,
                disembarkDate = current.disembarkDate,
                maxDeck = current.maxDeck,
                minDeck = current.minDeck,
                dinnerTime = current.dinnerTime,
                shipPresetName = shipPresetName,
            )
            settingsStore.setLastViewedDate(current.boardDate)
            settingsStore.setOnboardingCompleted(true)
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
