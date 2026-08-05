package com.cruise.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.Voyage
import com.cruise.assistant.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings,
    val voyage: Voyage?,
    val rangeErrorResId: Int? = null,
)

/** 显示范围最小跨度，和 domain 层保持一致 */
private const val MIN_RANGE_MINUTES = 120

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val voyageRepository: VoyageRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val rangeError = MutableStateFlow<Int?>(null)

    val state: StateFlow<SettingsUiState?> = combine(
        settingsStore.settings,
        voyageRepository.observeVoyage(),
        rangeError,
    ) { settings, voyage, error ->
        SettingsUiState(settings, voyage, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRange(startMinute: Int, endMinute: Int) {
        if (endMinute - startMinute < MIN_RANGE_MINUTES) {
            rangeError.value = R.string.error_range_too_short
            return
        }
        rangeError.value = null
        viewModelScope.launch { settingsStore.setDisplayRange(startMinute, endMinute) }
    }

    fun setHourHeight(dp: Int) {
        viewModelScope.launch { settingsStore.setHourHeight(dp.coerceIn(36, 140)) }
    }

    fun setCollapse(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setCollapseEmptyGaps(enabled) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setNotificationsEnabled(enabled)
            reminderScheduler.rescheduleAll()
        }
    }

    fun setReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setDefaultReminderMinutes(minutes.coerceIn(0, 240))
            reminderScheduler.rescheduleAll()
        }
    }

    fun updateVoyage(voyage: Voyage) {
        viewModelScope.launch { voyageRepository.updateVoyage(voyage) }
    }

    fun resetVoyage(onDone: () -> Unit) {
        viewModelScope.launch {
            voyageRepository.resetVoyage()
            settingsStore.setOnboardingCompleted(false)
            reminderScheduler.rescheduleAll()
            onDone()
        }
    }
}
