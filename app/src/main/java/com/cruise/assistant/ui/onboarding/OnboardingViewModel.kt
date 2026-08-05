package com.cruise.assistant.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.VoyageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class OnboardingState(
    val shipName: String = "",
    val boardDate: LocalDate,
    val boardTime: LocalTime = LocalTime.of(12, 0),
    val disembarkDate: LocalDate,
    val maxDeck: Int = 20,
    val saving: Boolean = false,
    /** 已本地化的错误提示 resId，null 表示无错误 */
    val errorResId: Int? = null,
)

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

    fun onBoardDateChange(value: LocalDate) = _state.update { current ->
        // 上船日往后挪时，下船日至少跟着不早于它
        val disembark = if (current.disembarkDate.isBefore(value)) value else current.disembarkDate
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
            voyageRepository.createVoyage(
                shipName = current.shipName,
                boardDate = current.boardDate,
                boardTime = current.boardTime,
                disembarkDate = current.disembarkDate,
                maxDeck = current.maxDeck,
            )
            settingsStore.setLastViewedDate(current.boardDate)
            settingsStore.setOnboardingCompleted(true)
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
