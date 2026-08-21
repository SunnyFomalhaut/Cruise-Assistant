package com.cruise.assistant.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cruise.assistant.domain.AppColorTheme
import com.cruise.assistant.domain.WallpaperPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

data class AppSettings(
    val lastViewedDate: LocalDate? = null,
    val displayStartMinute: Int = 7 * 60,
    val displayEndMinute: Int = 24 * 60,
    val hourHeightDp: Int = 120,
    val collapseEmptyGaps: Boolean = false,
    val remindBefore15: Boolean = true,
    val remindBefore5: Boolean = true,
    val remindAtStart: Boolean = true,
    val dinnerTime: LocalTime = LocalTime.of(17, 45),
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val lastPixieDeck: Int? = null,
    val colorTheme: AppColorTheme = AppColorTheme.JUDY_BLUE,
    val wallpaperEnabled: Boolean = false,
    val wallpaperKey: String = WallpaperPreset.NONE.key,
    val wallpaperAlpha: Float = 0.10f,
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
        val lastPixieDeck = intPreferencesKey("last_pixie_deck")
        val armedEventIds = stringSetPreferencesKey("armed_event_ids")
        val colorTheme = stringPreferencesKey("color_theme")
        val wallpaperEnabled = booleanPreferencesKey("wallpaper_enabled")
        val wallpaperKey = stringPreferencesKey("wallpaper_key")
        val wallpaperAlpha = floatPreferencesKey("wallpaper_alpha")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            lastViewedDate = prefs[Keys.lastViewedDate]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            displayStartMinute = prefs[Keys.displayStart] ?: defaults.displayStartMinute,
            displayEndMinute = prefs[Keys.displayEnd] ?: defaults.displayEndMinute,
            // 旧版本允许 36..140，存量值可能低于新下限 60——加载时钳到新范围，
            // 避免渲染与设置页滑块不一致（滑块钳 60，渲染仍用旧值）。
            hourHeightDp = prefs[Keys.hourHeight]?.coerceIn(60, 180) ?: defaults.hourHeightDp,
            collapseEmptyGaps = prefs[Keys.collapseGaps] ?: defaults.collapseEmptyGaps,
            remindBefore15 = prefs[Keys.remindBefore15] ?: defaults.remindBefore15,
            remindBefore5 = prefs[Keys.remindBefore5] ?: defaults.remindBefore5,
            remindAtStart = prefs[Keys.remindAtStart] ?: defaults.remindAtStart,
            dinnerTime = prefs[Keys.dinnerTime]
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?: defaults.dinnerTime,
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: defaults.notificationsEnabled,
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
            lastPixieDeck = prefs[Keys.lastPixieDeck],
            colorTheme = prefs[Keys.colorTheme]?.let { AppColorTheme.fromKey(it) } ?: defaults.colorTheme,
            wallpaperEnabled = prefs[Keys.wallpaperEnabled] ?: defaults.wallpaperEnabled,
            wallpaperKey = prefs[Keys.wallpaperKey] ?: defaults.wallpaperKey,
            wallpaperAlpha = prefs[Keys.wallpaperAlpha]?.coerceIn(0.05f, 0.40f) ?: defaults.wallpaperAlpha,
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

    suspend fun setLastPixieDeck(deck: Int) =
        edit { it[Keys.lastPixieDeck] = deck }

    suspend fun setColorTheme(theme: AppColorTheme) =
        edit { it[Keys.colorTheme] = theme.key }

    suspend fun setWallpaperEnabled(enabled: Boolean) =
        edit { it[Keys.wallpaperEnabled] = enabled }

    suspend fun setWallpaperKey(key: String) =
        edit { it[Keys.wallpaperKey] = key }

    suspend fun setWallpaperAlpha(alpha: Float) =
        edit { it[Keys.wallpaperAlpha] = alpha.coerceIn(0.05f, 0.40f) }

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
