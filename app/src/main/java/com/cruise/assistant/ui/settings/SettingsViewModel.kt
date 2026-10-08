package com.cruise.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.VoyageArchiveRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Context
import android.net.Uri
import com.cruise.assistant.domain.WallpaperManager
import com.cruise.assistant.domain.WallpaperPreset

@Immutable
data class SettingsUiState(
    val settings: AppSettings,
    val voyage: Voyage?,
    val rangeErrorResId: Int? = null,
    /** 存档数量：航次存档入口行的数量徽标 */
    val archiveCount: Int = 0,
    /** 一次性存档结果（resId），UI 消费后调 onArchiveResultShown 清除 */
    val archiveResultResId: Int? = null,
)

/** 显示范围最小跨度，和 domain 层保持一致 */
private const val MIN_RANGE_MINUTES = 120

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val voyageRepository: VoyageRepository,
    private val eventRepository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
    /** 航次存档仓库：存档动作 + 入口行数量徽标 */
    private val voyageArchiveRepository: VoyageArchiveRepository? = null,
) : ViewModel() {

    private val rangeError = MutableStateFlow<Int?>(null)

    /** 一次性存档结果反馈 */
    private val archiveResult = MutableStateFlow<Int?>(null)

    val state: StateFlow<SettingsUiState?> = combine(
        settingsStore.settings,
        voyageRepository.observeVoyage(),
        rangeError,
        voyageArchiveRepository?.observeAll() ?: flowOf(emptyList<VoyageArchiveMeta>()),
        archiveResult,
    ) { settings, voyage, error, archives, archiveEvent ->
        SettingsUiState(
            settings = settings,
            voyage = voyage,
            rangeErrorResId = error,
            archiveCount = archives.size,
            archiveResultResId = archiveEvent,
        )
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
        viewModelScope.launch {
            voyageRepository.updateVoyage(voyage)
            // 改航次日期会把登船/下船系统事件挪到新时间、并删除落在新范围外的普通事件，
            // 旧闹钟必须重排，否则仍按旧的事件时间响（与 setDinnerTime 等入口同一模式）。
            reminderScheduler.rescheduleAll()
        }
    }

    fun resetVoyage(onDone: () -> Unit) {
        viewModelScope.launch {
            voyageRepository.resetVoyage()
            settingsStore.setOnboardingCompleted(false)
            reminderScheduler.rescheduleAll()
            onDone()
        }
    }

    /** 存档此航次：结果（已存档/当前无航次）走 archiveResultResId 一次性反馈 */
    fun archiveVoyage() {
        val repository = voyageArchiveRepository ?: return
        viewModelScope.launch {
            val newId = repository.createArchive()
            archiveResult.value =
                if (newId != null) R.string.archive_saved_toast else R.string.archive_no_voyage_toast
        }
    }

    /** 存档后重建：先 createArchive（无航次自然存不了，不阻断重建），再走重建清空 */
    fun archiveAndResetVoyage(onDone: () -> Unit) {
        viewModelScope.launch {
            voyageArchiveRepository?.createArchive()
            voyageRepository.resetVoyage()
            settingsStore.setOnboardingCompleted(false)
            reminderScheduler.rescheduleAll()
            onDone()
        }
    }

    /** UI 消费完存档结果反馈后清除，避免旋转后重复弹 */
    fun onArchiveResultShown() {
        archiveResult.value = null
    }
}
