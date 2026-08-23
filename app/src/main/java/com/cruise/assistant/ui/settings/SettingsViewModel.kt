package com.cruise.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.AppColorTheme
import com.cruise.assistant.domain.Voyage
import java.time.LocalTime
import com.cruise.assistant.reminder.ReminderScheduler
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Context
import android.net.Uri
import com.cruise.assistant.domain.DollDistributionMode
import com.cruise.assistant.domain.WallpaperManager
import com.cruise.assistant.domain.WallpaperPreset

@Immutable
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
    private val eventRepository: EventRepository,
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

    fun setColorTheme(theme: AppColorTheme) {
        viewModelScope.launch { settingsStore.setColorTheme(theme) }
    }

    fun setWallpaperEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setWallpaperEnabled(enabled) }
    }

    fun setWallpaperKey(key: String) {
        viewModelScope.launch {
            settingsStore.setWallpaperKey(key)
            if (key != WallpaperPreset.NONE.key) {
                settingsStore.setWallpaperEnabled(true)
            }
        }
    }

    fun setWallpaperAlpha(alpha: Float) {
        viewModelScope.launch { settingsStore.setWallpaperAlpha(alpha) }
    }

    fun setCardDollsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setCardDollsEnabled(enabled) }
    }

    fun setCardDollDistribution(mode: DollDistributionMode) {
        viewModelScope.launch { settingsStore.setCardDollDistribution(mode) }
    }

    fun saveCustomWallpaper(context: Context, uri: Uri) {
        viewModelScope.launch {
            val success = WallpaperManager.saveCustomWallpaper(context, uri)
            if (success) {
                settingsStore.setWallpaperKey(WallpaperPreset.CUSTOM.key)
                settingsStore.setWallpaperEnabled(true)
            }
        }
    }

    fun setRange(startMinute: Int, endMinute: Int) {
        if (endMinute - startMinute < MIN_RANGE_MINUTES) {
            rangeError.value = R.string.error_range_too_short
            return
        }
        rangeError.value = null
        viewModelScope.launch { settingsStore.setDisplayRange(startMinute, endMinute) }
    }

    fun setHourHeight(dp: Int) {
        viewModelScope.launch { settingsStore.setHourHeight(dp.coerceIn(60, 180)) }
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

    fun setRemindBefore15(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setRemindBefore15(enabled)
            reminderScheduler.rescheduleAll()
        }
    }

    fun setRemindBefore5(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setRemindBefore5(enabled)
            reminderScheduler.rescheduleAll()
        }
    }

    fun setRemindAtStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setRemindAtStart(enabled)
            reminderScheduler.rescheduleAll()
        }
    }

    fun setDinnerTime(time: LocalTime) {
        viewModelScope.launch {
            settingsStore.setDinnerTime(time)
            eventRepository.updateDinnerEventsTime(time)
            eventRepository.resyncShowTimes(time)
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
