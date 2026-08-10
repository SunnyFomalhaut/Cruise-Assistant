# Ship Presets & Built-in Activities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade onboarding to a ship dropdown (Disney Adventure preset + custom), bind ship schedules (4-night/5-night presets + custom), auto-generate built-in activities (safety drill on day 1 + daily dinners), and add a dinner-time toggle in Settings that immediately overwrites existing dinner events.

**Architecture:** New `ShipPreset`/`SchedulePreset` domain models with a static registry of built-in ships. `presetKey` field added to events (DB v3 migration) to identify preset-generated events for batch dinner-time updates. Onboarding ViewModel gains ship/schedule selection state; `createVoyage` gains `dinnerTime` parameter and inserts safety drill + dinner events for built-in ships only.

**Tech Stack:** Android / Kotlin / Room / Jetpack Compose / Material3 / DataStore

## Global Constraints

- Built-in ship: "Disney Adventure" with two schedules: 四天三夜 (Mon→Thu, 12:00/07:00) and 五天四夜 (Thu→Mon, 12:00/07:00)
- Custom ships: no schedule dropdown, no built-in activity generation (only board/disembark)
- Safety drill: day-1 only, 15:45–16:20 (35 min), deck=6, zone=MID, category="重要事项" (iconKey="important"), title="安全演习", presetKey="safety_drill", isSystemGenerated=false
- Dinner: every voyage day except disembark day, dinnerTime–dinnerTime+75min, category="餐食" (iconKey="meal"), title="晚餐", presetKey="dinner", isSystemGenerated=false
- Dinner default time: 17:45; alternate: 20:00
- Dinner duration: 75 minutes (hardcoded, not user-configurable)
- Settings dinner toggle immediately overwrites all existing dinner events via presetKey="dinner" lookup
- `presetKey` column: TEXT, nullable; board/disembark events have presetKey=null
- DB migration: v2 → v3, `ALTER TABLE event ADD COLUMN presetKey TEXT`
- Schedule date computation: "next Monday from today" = if today is Monday, use today; otherwise the next Monday

---

### Task 1: Add `presetKey` field to models, entity, mappers, and DB migration v2→v3

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/Models.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`

**Interfaces:**
- Consumes: nothing new
- Produces: `CruiseEvent.presetKey: String?`, `EventEntity.presetKey: String?`, DB v3 with migration

- [ ] **Step 1: Add `presetKey` to `CruiseEvent`**

In `app/src/main/java/com/cruise/assistant/domain/Models.kt`, add `presetKey` as the last field of `CruiseEvent`:

```kotlin
data class CruiseEvent(
    val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val categoryId: Long,
    val title: String,
    val deck: Int? = null,
    val zone: ShipZone? = null,
    val placeName: String? = null,
    val lane: Int = 0,
    val recurrenceGroupId: String? = null,
    val reminderMinutesBefore: Int? = null,
    val isSystemGenerated: Boolean = false,
    val isActive: Boolean = true,
    val presetKey: String? = null,
)
```

- [ ] **Step 2: Add `presetKey` to `EventEntity`**

In `app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt`, add `presetKey` as the last field:

```kotlin
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val categoryId: Long,
    val title: String,
    val deck: Int?,
    val zone: String?,
    val placeName: String?,
    val lane: Int,
    val recurrenceGroupId: String?,
    val reminderMinutesBefore: Int?,
    val isSystemGenerated: Boolean,
    val isActive: Boolean = true,
    val presetKey: String? = null,
)
```

- [ ] **Step 3: Update mappers**

In `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt`, add `presetKey = presetKey,` to both `EventEntity.toDomain()` and `CruiseEvent.toEntity()`:

```kotlin
fun EventEntity.toDomain() = CruiseEvent(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck,
    zone = zone?.let { runCatching { ShipZone.valueOf(it) }.getOrNull() },
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
    isActive = isActive,
    presetKey = presetKey,
)

fun CruiseEvent.toEntity() = EventEntity(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck, zone = zone?.name,
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
    isActive = isActive,
    presetKey = presetKey,
)
```

- [ ] **Step 4: Bump DB version to 3 and add migration**

In `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`:

Change `version = 2` to `version = 3`. Add `MIGRATION_2_3` and register it:

```kotlin
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE event ADD COLUMN presetKey TEXT")
            }
        }
```

Update the builder's `.addMigrations(...)` call:

```kotlin
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
```

- [ ] **Step 5: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/Models.kt app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt app/src/main/java/com/cruise/assistant/data/db/Mappers.kt app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt
git commit -m "feat: add presetKey field to event model with DB migration v2→v3"
```

---

### Task 2: Add `getByPresetKey` DAO query and `updateDinnerEventsTime` to Repository

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/db/dao/EventDao.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt`

**Interfaces:**
- Consumes: `EventEntity.presetKey` (from Task 1)
- Produces: `EventDao.getByPresetKey(key: String): List<EventEntity>`, `EventRepository.updateDinnerEventsTime(newTime: LocalTime)`

- [ ] **Step 1: Add `getByPresetKey` query to EventDao**

In `app/src/main/java/com/cruise/assistant/data/db/dao/EventDao.kt`, add after the `getByGroup` query:

```kotlin
    @Query("SELECT * FROM event WHERE presetKey = :key")
    suspend fun getByPresetKey(key: String): List<EventEntity>
```

- [ ] **Step 2: Add `updateDinnerEventsTime` to EventRepository**

In `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt`, add this method after `setActive` (before `conflictsOn`):

```kotlin
    /**
     * 设置页切换晚餐时段：把所有 presetKey="dinner" 的事件 startTime 统一更新为 newTime。
     * durationMinutes 保持不变（75 分钟），日期不变，lane 不变（晚餐时段通常独占
     * 自己的时间窗）。被删除过的晚餐事件不会批量"复活"——只有当前仍在表里的会被更新。
     */
    suspend fun updateDinnerEventsTime(newTime: LocalTime) {
        val dinners = eventDao.getByPresetKey("dinner")
        dinners.forEach { entity ->
            eventDao.update(entity.copy(startTime = newTime))
        }
    }
```

- [ ] **Step 3: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/data/db/dao/EventDao.kt app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt
git commit -m "feat: add getByPresetKey DAO query and updateDinnerEventsTime to repository"
```

---

### Task 3: Create `ShipPreset` domain model and static registry (TDD)

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/ShipPreset.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/ShipPresetTest.kt`

**Interfaces:**
- Consumes: `java.time.DayOfWeek`, `java.time.LocalDate`, `java.time.LocalTime`
- Produces: `ShipPreset`, `SchedulePreset` data classes, `BUILT_IN_SHIPS` list, `nextOccurrenceOfDayOfWeek()` helper

- [ ] **Step 1: Write failing tests for schedule date computation**

Create `app/src/test/java/com/cruise/assistant/domain/ShipPresetTest.kt`:

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ShipPresetTest {

    @Test
    fun nextOccurrence_sameDay_returnsToday() {
        // 2026-08-10 is a Monday
        val today = LocalDate.of(2026, 8, 10)
        assertEquals(today, nextOccurrenceOfDayOfWeek(DayOfWeek.MONDAY, today))
    }

    @Test
    fun nextOccurrence_futureDay_returnsNextOccurrence() {
        // 2026-08-11 is Tuesday; next Monday is 2026-08-17
        val tuesday = LocalDate.of(2026, 8, 11)
        assertEquals(
            LocalDate.of(2026, 8, 17),
            nextOccurrenceOfDayOfWeek(DayOfWeek.MONDAY, tuesday),
        )
    }

    @Test
    fun nextOccurrence_thursdayFromWednesday_returnsTomorrow() {
        // 2026-08-12 is Wednesday; next Thursday is 2026-08-13
        val wednesday = LocalDate.of(2026, 8, 12)
        assertEquals(
            LocalDate.of(2026, 8, 13),
            nextOccurrenceOfDayOfWeek(DayOfWeek.THURSDAY, wednesday),
        )
    }

    @Test
    fun builtInShips_containsDisneyAdventure() {
        assertEquals(1, BUILT_IN_SHIPS.size)
        assertEquals("Disney Adventure", BUILT_IN_SHIPS[0].name)
        assertEquals(2, BUILT_IN_SHIPS[0].schedules.size)
    }

    @Test
    fun disneyAdventure_schedule1_is3NightsMonToThu() {
        val s = BUILT_IN_SHIPS[0].schedules[0]
        assertEquals("四天三夜", s.label)
        assertEquals(3, s.nights)
        assertEquals(DayOfWeek.MONDAY, s.boardDayOfWeek)
        assertEquals(DayOfWeek.THURSDAY, s.disembarkDayOfWeek)
    }

    @Test
    fun disneyAdventure_schedule2_is4NightsThuToMon() {
        val s = BUILT_IN_SHIPS[0].schedules[1]
        assertEquals("五天四夜", s.label)
        assertEquals(4, s.nights)
        assertEquals(DayOfWeek.THURSDAY, s.boardDayOfWeek)
        assertEquals(DayOfWeek.MONDAY, s.disembarkDayOfWeek)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.ShipPresetTest"`
Expected: FAIL — `nextOccurrenceOfDayOfWeek` and `BUILT_IN_SHIPS` not defined

- [ ] **Step 3: Create `ShipPreset.kt` with models and helper**

Create `app/src/main/java/com/cruise/assistant/domain/ShipPreset.kt`:

```kotlin
package com.cruise.assistant.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class SchedulePreset(
    val label: String,
    val nights: Int,
    val boardDayOfWeek: DayOfWeek,
    val disembarkDayOfWeek: DayOfWeek,
    val boardTime: LocalTime,
    val disembarkTime: LocalTime,
) {
    /** Compute board date as the next occurrence of boardDayOfWeek from `today` (inclusive). */
    fun boardDateFrom(today: LocalDate): LocalDate =
        nextOccurrenceOfDayOfWeek(boardDayOfWeek, today)

    /** Compute disembark date = board date + nights. */
    fun disembarkDateFrom(today: LocalDate): LocalDate =
        boardDateFrom(today).plusDays(nights.toLong())
}

data class ShipPreset(
    val name: String,
    val schedules: List<SchedulePreset>,
)

/**
 * Returns the date of the next occurrence of `target` on or after `today`.
 * If `today` itself is the target day-of-week, returns `today`.
 */
fun nextOccurrenceOfDayOfWeek(target: DayOfWeek, today: LocalDate): LocalDate {
    val diff = (target.value - today.dayOfWeek.value + 7) % 7
    return today.plusDays(diff.toLong())
}

val BUILT_IN_SHIPS: List<ShipPreset> = listOf(
    ShipPreset(
        name = "Disney Adventure",
        schedules = listOf(
            SchedulePreset(
                label = "四天三夜",
                nights = 3,
                boardDayOfWeek = DayOfWeek.MONDAY,
                disembarkDayOfWeek = DayOfWeek.THURSDAY,
                boardTime = LocalTime.of(12, 0),
                disembarkTime = LocalTime.of(7, 0),
            ),
            SchedulePreset(
                label = "五天四夜",
                nights = 4,
                boardDayOfWeek = DayOfWeek.THURSDAY,
                disembarkDayOfWeek = DayOfWeek.MONDAY,
                boardTime = LocalTime.of(12, 0),
                disembarkTime = LocalTime.of(7, 0),
            ),
        ),
    ),
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.ShipPresetTest"`
Expected: PASS — all 6 tests green

- [ ] **Step 5: Run full test suite**

Run: `.\gradlew.bat test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ShipPreset.kt app/src/test/java/com/cruise/assistant/domain/ShipPresetTest.kt
git commit -m "feat: add ShipPreset domain model, schedule computation, and Disney Adventure registry"
```

---

### Task 4: Generate built-in activities in `createVoyage`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt`

**Interfaces:**
- Consumes: `CruiseEvent.presetKey` (Task 1), `BUILT_IN_SHIPS` (Task 3)
- Produces: `createVoyage(... dinnerTime: LocalTime, shipPreset: ShipPreset?)` that inserts safety drill + dinner events

- [ ] **Step 1: Update `createVoyage` signature and add built-in activity generation**

In `app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt`:

1a. Add imports at the top (after existing imports):

```kotlin
import com.cruise.assistant.domain.ShipZone
import java.time.LocalTime
```

1b. Add constants near the top (after `DISEMBARK_DURATION_MINUTES`):

```kotlin
private const val SAFETY_DRILL_DURATION_MINUTES = 35
private const val DINNER_DURATION_MINUTES = 75
private val SAFETY_DRILL_TIME = LocalTime.of(15, 45)
```

1c. Change the `createVoyage` signature to add `dinnerTime` and `shipPresetName` parameters:

From:
```kotlin
    suspend fun createVoyage(
        shipName: String?,
        boardDate: LocalDate,
        boardTime: LocalTime,
        disembarkDate: LocalDate,
        maxDeck: Int,
    ): Voyage {
```

To:
```kotlin
    suspend fun createVoyage(
        shipName: String?,
        boardDate: LocalDate,
        boardTime: LocalTime,
        disembarkDate: LocalDate,
        maxDeck: Int,
        dinnerTime: LocalTime = LocalTime.of(17, 45),
        shipPresetName: String? = null,
    ): Voyage {
```

1d. After the existing `eventDao.insertAll(listOf(board, disembark))` block (the `listOf(...)` creating 上船/下船), add the built-in activity generation logic. Find the existing `eventDao.insertAll(...)` call that inserts board + disembark events, and replace it with a version that also includes preset activities. Change:

From:
```kotlin
        eventDao.insertAll(
            listOf(
                CruiseEvent(
                    date = boardDate,
                    startTime = boardTime,
                    durationMinutes = BOARDING_DURATION_MINUTES,
                    categoryId = importantId,
                    title = "上船",
                    isSystemGenerated = true,
                ).toEntity(),
                CruiseEvent(
                    date = disembarkDate,
                    startTime = voyage.disembarkTime,
                    durationMinutes = DISEMBARK_DURATION_MINUTES,
                    categoryId = importantId,
                    title = "下船",
                    isSystemGenerated = true,
                ).toEntity(),
            ),
        )
        return voyage
```

To:
```kotlin
        val systemEvents = listOf(
            CruiseEvent(
                date = boardDate,
                startTime = boardTime,
                durationMinutes = BOARDING_DURATION_MINUTES,
                categoryId = importantId,
                title = "上船",
                isSystemGenerated = true,
            ).toEntity(),
            CruiseEvent(
                date = disembarkDate,
                startTime = voyage.disembarkTime,
                durationMinutes = DISEMBARK_DURATION_MINUTES,
                categoryId = importantId,
                title = "下船",
                isSystemGenerated = true,
            ).toEntity(),
        )
        eventDao.insertAll(systemEvents)

        // 内置船只才生成预置活动（安全演习 + 晚餐）
        if (shipPresetName != null) {
            val mealId = categories.firstOrNull { it.iconKey == "meal" }?.id
                ?: categories.first().id

            val presetEvents = mutableListOf<CruiseEvent>()

            // 安全演习：仅上船日（第一天），15:45–16:20，6F 船中
            presetEvents += CruiseEvent(
                date = boardDate,
                startTime = SAFETY_DRILL_TIME,
                durationMinutes = SAFETY_DRILL_DURATION_MINUTES,
                categoryId = importantId,
                title = "安全演习",
                deck = 6,
                zone = ShipZone.MID,
                isSystemGenerated = false,
                presetKey = "safety_drill",
            )

            // 晚餐：每个航程日除下船日，dinnerTime 起 75 分钟
            voyage.datesInVoyage()
                .filter { it != disembarkDate }
                .forEach { day ->
                    presetEvents += CruiseEvent(
                        date = day,
                        startTime = dinnerTime,
                        durationMinutes = DINNER_DURATION_MINUTES,
                        categoryId = mealId,
                        title = "晚餐",
                        isSystemGenerated = false,
                        presetKey = "dinner",
                    )
                }

            eventDao.insertAll(presetEvents.map { it.toEntity() })
        }

        return voyage
```

- [ ] **Step 2: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run full test suite**

Run: `.\gradlew.bat test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt
git commit -m "feat: generate safety drill and dinner preset events for built-in ships"
```

---

### Task 5: Add `dinnerTime` to `AppSettings` and `SettingsStore`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`

**Interfaces:**
- Consumes: nothing new
- Produces: `AppSettings.dinnerTime: LocalTime`, `SettingsStore.setDinnerTime(LocalTime)`

- [ ] **Step 1: Add `dinnerTime` to `AppSettings` and DataStore**

In `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`:

1a. Add `dinnerTime` to `AppSettings` data class (add after `defaultReminderMinutes`):

```kotlin
data class AppSettings(
    val lastViewedDate: LocalDate? = null,
    val displayStartMinute: Int = 7 * 60,
    val displayEndMinute: Int = 24 * 60,
    val hourHeightDp: Int = 60,
    val collapseEmptyGaps: Boolean = false,
    val defaultReminderMinutes: Int = 15,
    val dinnerTime: LocalTime = LocalTime.of(17, 45),
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
)
```

Add import at top: `import java.time.LocalTime`

1b. Add a DataStore key in the `Keys` object:

```kotlin
        val dinnerTime = stringPreferencesKey("dinner_time")
```

1c. In the `settings` flow mapping, add `dinnerTime`:

```kotlin
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
            dinnerTime = prefs[Keys.dinnerTime]
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?: defaults.dinnerTime,
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: defaults.notificationsEnabled,
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
        )
    }
```

1d. Add the setter method (after `setDefaultReminderMinutes`):

```kotlin
    suspend fun setDinnerTime(time: LocalTime) =
        edit { it[Keys.dinnerTime] = time.toString() }
```

- [ ] **Step 2: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt
git commit -m "feat: add dinnerTime to AppSettings and SettingsStore"
```

---

### Task 6: Add `setDinnerTime` to `SettingsViewModel`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `EventRepository.updateDinnerEventsTime` (Task 2), `SettingsStore.setDinnerTime` (Task 5)
- Produces: `SettingsViewModel.setDinnerTime(LocalTime)`

**Note:** This task adds `EventRepository` as a dependency to `SettingsViewModel`. The constructor currently does not take it, so the constructor signature changes.

- [ ] **Step 1: Add `EventRepository` dependency and `setDinnerTime` method**

In `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt`:

1a. Add import:

```kotlin
import com.cruise.assistant.data.repo.EventRepository
import java.time.LocalTime
```

1b. Change the constructor to add `eventRepository`:

From:
```kotlin
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val voyageRepository: VoyageRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
```

To:
```kotlin
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val voyageRepository: VoyageRepository,
    private val eventRepository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
```

1c. Add the `setDinnerTime` method (after `setReminderMinutes`):

```kotlin
    fun setDinnerTime(time: LocalTime) {
        viewModelScope.launch {
            settingsStore.setDinnerTime(time)
            eventRepository.updateDinnerEventsTime(time)
            reminderScheduler.rescheduleAll()
        }
    }
```

- [ ] **Step 2: Update `CruiseNavHost` to pass `eventRepository` to `SettingsViewModel`**

In `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`, find the `SettingsViewModel` construction (around line 301):

From:
```kotlin
                    SettingsViewModel(
                        container.settingsStore,
                        container.voyageRepository,
                        container.reminderScheduler,
                    )
```

To:
```kotlin
                    SettingsViewModel(
                        container.settingsStore,
                        container.voyageRepository,
                        container.eventRepository,
                        container.reminderScheduler,
                    )
```

- [ ] **Step 3: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: add setDinnerTime to SettingsViewModel with immediate overwrite"
```

---

### Task 7: Add dinner time toggle UI to `SettingsScreen`

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`

**Interfaces:**
- Consumes: `AppSettings.dinnerTime` (Task 5), `SettingsViewModel.setDinnerTime` (Task 6)
- Produces: dinner time `FilterChip` section in settings UI

- [ ] **Step 1: Add string resources**

In `app/src/main/res/values/strings.xml`, add after the `reminder_section` string:

```xml
    <string name="dinner_time_section">晚餐时段</string>
    <string name="dinner_time_1745">17:45</string>
    <string name="dinner_time_2000">20:00</string>
```

- [ ] **Step 2: Add `onDinnerTimeChange` parameter to `SettingsScreen`**

In `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`, add to the `SettingsScreen` signature (after `onReminderMinutesChange`):

```kotlin
    onReminderMinutesChange: (Int) -> Unit,
    onDinnerTimeChange: (java.time.LocalTime) -> Unit,
    onResetVoyage: () -> Unit,
```

- [ ] **Step 3: Add dinner time UI section**

In `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`, find the section after the reminder block's closing (after the `}` that closes `if (state.settings.notificationsEnabled) { ... }`), and before the `// ---- 航次 ----` section. Insert:

```kotlin
        Spacer(Modifier.height(20))
        HorizontalDivider()
        Spacer(Modifier.height(20))

        // ---- 晚餐时段 ----
        Text(stringResource(R.string.dinner_time_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                java.time.LocalTime.of(17, 45) to R.string.dinner_time_1745,
                java.time.LocalTime.of(20, 0) to R.string.dinner_time_2000,
            ).forEach { (time, labelRes) ->
                FilterChip(
                    selected = state.settings.dinnerTime == time,
                    onClick = { onDinnerTimeChange(time) },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
```

- [ ] **Step 4: Wire `onDinnerTimeChange` in CruiseNavHost**

In `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`, in the `SettingsScreen(...)` call, add after `onReminderMinutesChange = vm::setReminderMinutes,`:

```kotlin
                    onReminderMinutesChange = vm::setReminderMinutes,
                    onDinnerTimeChange = vm::setDinnerTime,
                    onResetVoyage = {
```

- [ ] **Step 5: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: add dinner time toggle to settings screen"
```

---

### Task 8: Add ship/schedule selection state to `OnboardingViewModel`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingViewModel.kt`

**Interfaces:**
- Consumes: `BUILT_IN_SHIPS`, `SchedulePreset`, `nextOccurrenceOfDayOfWeek` (Task 3)
- Produces: `OnboardingState.selectedShipIndex`, `selectedScheduleIndex`, `dinnerTime`; `onShipSelected`, `onScheduleSelected`, `onDinnerTimeChange`

- [ ] **Step 1: Update `OnboardingState` with new fields**

In `app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingViewModel.kt`, change `OnboardingState`:

From:
```kotlin
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
```

To:
```kotlin
data class OnboardingState(
    val shipName: String = "",
    val selectedShipIndex: Int = -1,
    val selectedScheduleIndex: Int = -1,
    val dinnerTime: LocalTime = LocalTime.of(17, 45),
    val boardDate: LocalDate,
    val boardTime: LocalTime = LocalTime.of(12, 0),
    val disembarkDate: LocalDate,
    val maxDeck: Int = 20,
    val saving: Boolean = false,
    /** 已本地化的错误提示 resId，null 表示无错误 */
    val errorResId: Int? = null,
) {
    /** 是否选中了内置船（selectedShipIndex >= 0） */
    val isBuiltInShip: Boolean get() = selectedShipIndex >= 0
}
```

- [ ] **Step 2: Add imports and ship selection handlers**

Add imports at the top of `OnboardingViewModel.kt`:

```kotlin
import com.cruise.assistant.domain.BUILT_IN_SHIPS
import com.cruise.assistant.domain.ShipPreset
```

Add these methods to the `OnboardingViewModel` class (after `onShipNameChange`):

```kotlin
    fun onShipSelected(index: Int) = _state.update { current ->
        if (index < 0 || index >= BUILT_IN_SHIPS.size) {
            // 自定义船名
            current.copy(
                selectedShipIndex = -1,
                selectedScheduleIndex = -1,
                shipName = "",
            )
        } else {
            val ship = BUILT_IN_SHIPS[index]
            current.copy(
                selectedShipIndex = index,
                selectedScheduleIndex = -1,
                shipName = ship.name,
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
```

- [ ] **Step 3: Update `submit()` to pass `dinnerTime` and `shipPresetName`**

Change the `submit()` method's `voyageRepository.createVoyage(...)` call:

From:
```kotlin
            voyageRepository.createVoyage(
                shipName = current.shipName,
                boardDate = current.boardDate,
                boardTime = current.boardTime,
                disembarkDate = current.disembarkDate,
                maxDeck = current.maxDeck,
            )
```

To:
```kotlin
            val shipPresetName = if (current.isBuiltInShip) current.shipName else null
            voyageRepository.createVoyage(
                shipName = current.shipName,
                boardDate = current.boardDate,
                boardTime = current.boardTime,
                disembarkDate = current.disembarkDate,
                maxDeck = current.maxDeck,
                dinnerTime = current.dinnerTime,
                shipPresetName = shipPresetName,
            )
```

- [ ] **Step 4: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingViewModel.kt
git commit -m "feat: add ship/schedule selection and dinner time to onboarding ViewModel"
```

---

### Task 9: Redesign `OnboardingScreen` with ship dropdown, schedule dropdown, and dinner time picker

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingScreen.kt`

**Interfaces:**
- Consumes: `OnboardingState.selectedShipIndex/selectedScheduleIndex/dinnerTime/isBuiltInShip` (Task 8), `BUILT_IN_SHIPS` (Task 3), new ViewModel callbacks
- Produces: ship dropdown UI, schedule dropdown UI (shown only for built-in ships), dinner time picker (shown only for built-in ships with selected schedule)

- [ ] **Step 1: Add string resources**

In `app/src/main/res/values/strings.xml`, add after `disembark_time_hint`:

```xml
    <string name="custom_ship">自定义船名…</string>
    <string name="custom_schedule">自定义船期…</string>
    <string name="schedule_label">船期</string>
    <string name="dinner_time_label">晚餐时段</string>
    <string name="dinner_time_early">17:45</string>
    <string name="dinner_time_late">20:00</string>
```

- [ ] **Step 2: Update `OnboardingScreen` signature with new callbacks**

In `app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingScreen.kt`, change the composable signature:

From:
```kotlin
@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onShipNameChange: (String) -> Unit,
    onBoardDateChange: (LocalDate) -> Unit,
    onBoardTimeChange: (java.time.LocalTime) -> Unit,
    onDisembarkDateChange: (LocalDate) -> Unit,
    onMaxDeckChange: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
```

To:
```kotlin
@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onShipNameChange: (String) -> Unit,
    onShipSelected: (Int) -> Unit,
    onScheduleSelected: (Int) -> Unit,
    onBoardDateChange: (LocalDate) -> Unit,
    onBoardTimeChange: (java.time.LocalTime) -> Unit,
    onDisembarkDateChange: (LocalDate) -> Unit,
    onMaxDeckChange: (Int) -> Unit,
    onDinnerTimeChange: (java.time.LocalTime) -> Unit,
    onSubmit: () -> Unit,
) {
```

- [ ] **Step 3: Replace ship name `OutlinedTextField` with ship dropdown**

In `OnboardingScreen.kt`, find the existing ship name `OutlinedTextField` (around lines 69-75):

```kotlin
        OutlinedTextField(
            value = state.shipName,
            onValueChange = onShipNameChange,
            label = { Text(stringResource(R.string.ship_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
```

Replace with:

```kotlin
        // ---- 船名下拉 ----
        Text(stringResource(R.string.ship_name_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        var shipMenuExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = shipMenuExpanded,
            onExpandedChange = { shipMenuExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = if (state.isBuiltInShip) state.shipName
                    else if (state.shipName.isEmpty()) stringResource(R.string.custom_ship)
                    else state.shipName,
                onValueChange = { },
                label = { Text(stringResource(R.string.ship_name_label)) },
                singleLine = true,
                readOnly = state.isBuiltInShip,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shipMenuExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = shipMenuExpanded,
                onDismissRequest = { shipMenuExpanded = false },
            ) {
                BUILT_IN_SHIPS.forEachIndexed { index, ship ->
                    DropdownMenuItem(
                        text = { Text(ship.name) },
                        onClick = {
                            onShipSelected(index)
                            shipMenuExpanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.custom_ship)) },
                    onClick = {
                        onShipSelected(-1)
                        shipMenuExpanded = false
                    },
                )
            }
        }
        // 自定义船名时显示输入框
        if (!state.isBuiltInShip) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.shipName,
                onValueChange = onShipNameChange,
                label = { Text(stringResource(R.string.ship_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(20.dp))
```

- [ ] **Step 4: Add schedule dropdown (shown only for built-in ships)**

After the ship name block (after the `Spacer(Modifier.height(20.dp))` added in Step 3), and before the existing board date section, add:

```kotlin
        // ---- 船期下拉（仅内置船显示）----
        if (state.isBuiltInShip) {
            Text(stringResource(R.string.schedule_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            var scheduleMenuExpanded by remember { mutableStateOf(false) }
            val ship = BUILT_IN_SHIPS[state.selectedShipIndex]
            ExposedDropdownMenuBox(
                expanded = scheduleMenuExpanded,
                onExpandedChange = { scheduleMenuExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = if (state.selectedScheduleIndex >= 0)
                        ship.schedules[state.selectedScheduleIndex].label
                    else stringResource(R.string.custom_schedule),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.schedule_label)) },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = scheduleMenuExpanded,
                    onDismissRequest = { scheduleMenuExpanded = false },
                ) {
                    ship.schedules.forEachIndexed { index, schedule ->
                        DropdownMenuItem(
                            text = { Text(schedule.label) },
                            onClick = {
                                onScheduleSelected(index)
                                scheduleMenuExpanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.custom_schedule)) },
                        onClick = {
                            onScheduleSelected(-1)
                            scheduleMenuExpanded = false
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
```

- [ ] **Step 5: Add dinner time picker (shown only for built-in ships with a selected schedule)**

After the schedule dropdown block, add:

```kotlin
        // ---- 晚餐时段（仅内置船 + 选中了预置船期时显示）----
        if (state.isBuiltInShip && state.selectedScheduleIndex >= 0) {
            Text(stringResource(R.string.dinner_time_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    java.time.LocalTime.of(17, 45) to R.string.dinner_time_early,
                    java.time.LocalTime.of(20, 0) to R.string.dinner_time_late,
                ).forEach { (time, labelRes) ->
                    FilterChip(
                        selected = state.dinnerTime == time,
                        onClick = { onDinnerTimeChange(time) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
```

- [ ] **Step 6: Add necessary imports**

Add at the top of `OnboardingScreen.kt`:

```kotlin
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Arrangement
import com.cruise.assistant.domain.BUILT_IN_SHIPS
```

- [ ] **Step 7: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingScreen.kt
git commit -m "feat: redesign onboarding with ship dropdown, schedule dropdown, dinner time picker"
```

---

### Task 10: Wire new onboarding callbacks in `CruiseNavHost`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`

**Interfaces:**
- Consumes: `OnboardingViewModel.onShipSelected`, `onScheduleSelected`, `onDinnerTimeChange` (Task 8)
- Produces: wired `OnboardingScreen` call with all new callbacks

- [ ] **Step 1: Update `OnboardingScreen` call in CruiseNavHost**

In `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`, find the `OnboardingScreen(...)` call (around lines 82-94):

From:
```kotlin
            OnboardingScreen(
                state = state,
                onShipNameChange = vm::onShipNameChange,
                onBoardDateChange = vm::onBoardDateChange,
                onBoardTimeChange = vm::onBoardTimeChange,
                onDisembarkDateChange = vm::onDisembarkDateChange,
                onMaxDeckChange = vm::onMaxDeckChange,
                onSubmit = {
                    vm.submit {
```

To:
```kotlin
            OnboardingScreen(
                state = state,
                onShipNameChange = vm::onShipNameChange,
                onShipSelected = vm::onShipSelected,
                onScheduleSelected = vm::onScheduleSelected,
                onBoardDateChange = vm::onBoardDateChange,
                onBoardTimeChange = vm::onBoardTimeChange,
                onDisembarkDateChange = vm::onDisembarkDateChange,
                onMaxDeckChange = vm::onMaxDeckChange,
                onDinnerTimeChange = vm::onDinnerTimeChange,
                onSubmit = {
                    vm.submit {
```

- [ ] **Step 2: Compile**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run full test suite**

Run: `.\gradlew.bat test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: wire ship/schedule/dinner callbacks in onboarding nav host"
```

---

### Task 11: Build release APK and verify

**Files:**
- Generated: `app/release/CruiseAssistant-v1.3.apk` (new version)

- [ ] **Step 1: Bump version to v1.3**

In `app/build.gradle.kts`, change:

```kotlin
        versionCode = 3
        versionName = "1.2"
```

To:

```kotlin
        versionCode = 4
        versionName = "1.3"
```

- [ ] **Step 2: Build release APK**

Run: `.\gradlew.bat assembleRelease`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Copy APK to release folder**

Run in PowerShell:
```powershell
Copy-Item "app\build\outputs\apk\release\CruiseAssistant-v1.3.apk" "app\release\CruiseAssistant-v1.3.apk" -Force
Test-Path "app\release\CruiseAssistant-v1.3.apk"
```
Expected: True

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/release/CruiseAssistant-v1.3.apk
git commit -m "release: v1.3 with ship presets and built-in activities"
```