package com.cruise.assistant.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class AppSettings(
    val lastViewedDate: LocalDate? = null,
    val displayStartMinute: Int = 7 * 60,
    val displayEndMinute: Int = 24 * 60,
    val hourHeightDp: Int = 60,
    val collapseEmptyGaps: Boolean = false,
    val defaultReminderMinutes: Int = 15,
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
        val reminderMinutes = intPreferencesKey("default_reminder_minutes")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
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
            defaultReminderMinutes = prefs[Keys.reminderMinutes] ?: defaults.defaultReminderMinutes,
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

    suspend fun setDefaultReminderMinutes(minutes: Int) =
        edit { it[Keys.reminderMinutes] = minutes }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        edit { it[Keys.notificationsEnabled] = enabled }

    suspend fun setOnboardingCompleted(completed: Boolean) =
        edit { it[Keys.onboardingCompleted] = completed }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
