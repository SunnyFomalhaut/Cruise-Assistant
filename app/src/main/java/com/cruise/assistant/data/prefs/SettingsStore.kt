package com.cruise.assistant.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

data class AppSettings(
    val lastViewedDate: LocalDate? = null,
    val displayStartMinute: Int = 7 * 60,
    val displayEndMinute: Int = 24 * 60,
    val hourHeightDp: Int = 60,
    val collapseEmptyGaps: Boolean = false,
    val remindBefore15: Boolean = true,
    val remindBefore5: Boolean = true,
    val remindAtStart: Boolean = true,
    val dinnerTime: LocalTime = LocalTime.of(17, 45),
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cruise_settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val lastViewedDate = stringPreferencesKey("last_viewed_date")
        val displayStart = intPreferencesKey("display_start_minute")
        val displayEnd = intPreferencesKey("display_end_minute")
        val hourHeight = intPreferencesKey("hour_height_dp")
        val collapseGaps = booleanPreferencesKey("collapse_empty_gaps")
        val remindBefore15 = booleanPreferencesKey("remind_before_15")
        val remindBefore5 = booleanPreferencesKey("remind_before_5")
        val remindAtStart = booleanPreferencesKey("remind_at_start")
        val dinnerTime = stringPreferencesKey("dinner_time")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val armedEventIds = stringSetPreferencesKey("armed_event_ids")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            lastViewedDate = prefs[Keys.lastViewedDate]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            displayStartMinute = prefs[Keys.displayStart] ?: defaults.displayStartMinute,
            displayEndMinute = prefs[Keys.displayEnd] ?: defaults.displayEndMinute,
            hourHeightDp = prefs[Keys.hourHeight] ?: defaults.hourHeightDp,
            collapseEmptyGaps = prefs[Keys.collapseGaps] ?: defaults.collapseEmptyGaps,
            remindBefore15 = prefs[Keys.remindBefore15] ?: defaults.remindBefore15,
            remindBefore5 = prefs[Keys.remindBefore5] ?: defaults.remindBefore5,
            remindAtStart = prefs[Keys.remindAtStart] ?: defaults.remindAtStart,
            dinnerTime = prefs[Keys.dinnerTime]
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?: defaults.dinnerTime,
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: defaults.notificationsEnabled,
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
        )
    }

    suspend fun setLastViewedDate(date: LocalDate) =
        edit { it[Keys.lastViewedDate] = date.toString() }

    suspend fun setDisplayRange(startMinute: Int, endMinute: Int) = edit {
        it[Keys.displayStart] = startMinute
        it[Keys.displayEnd] = endMinute
    }

    suspend fun setHourHeight(dp: Int) = edit { it[Keys.hourHeight] = dp }

    suspend fun setCollapseEmptyGaps(enabled: Boolean) = edit { it[Keys.collapseGaps] = enabled }

    suspend fun setRemindBefore15(enabled: Boolean) =
        edit { it[Keys.remindBefore15] = enabled }

    suspend fun setRemindBefore5(enabled: Boolean) =
        edit { it[Keys.remindBefore5] = enabled }

    suspend fun setRemindAtStart(enabled: Boolean) =
        edit { it[Keys.remindAtStart] = enabled }

    suspend fun setDinnerTime(time: LocalTime) =
        edit { it[Keys.dinnerTime] = time.toString() }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        edit { it[Keys.notificationsEnabled] = enabled }

    suspend fun setOnboardingCompleted(completed: Boolean) =
        edit { it[Keys.onboardingCompleted] = completed }

    /**
     * 上一次 rescheduleAll() 实际排上闹钟的事件 id 集合，用于下次重排时精确取消——
     * 不依赖"事件表当前还有哪些行"，因为删除会先于 rescheduleAll() 把行从数据库移除。
     * 存在 DataStore 里而不是内存，是为了在进程被杀死/开机重启后依然可用
     * （BootReceiver、App 启动都会调 rescheduleAll()）。
     */
    suspend fun getArmedEventIds(): Set<Long> =
        context.dataStore.data.first()[Keys.armedEventIds]
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    suspend fun setArmedEventIds(ids: Set<Long>) = edit {
        it[Keys.armedEventIds] = ids.map { id -> id.toString() }.toSet()
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
