# Event Active/Inactive Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an active/inactive state toggle to events so users can temporarily "park" alternative plans without deleting them — inactive events stay visible (greyed out) but are excluded from conflict detection, lane assignment, reminders, and ongoing/next banners.

**Architecture:** Add an `isActive` boolean column to the event table (DB migration v1→v2). Filter inactive events out of conflict detection, lane assignment, and reminder scheduling at the call sites (no domain function signature changes except `findConflicts`). Add a toggle menu item to the long-press dropdown and grey out inactive cards visually.

**Tech Stack:** Android / Kotlin / Room / Jetpack Compose / Material3

## Global Constraints

- New field default: `isActive = true` (all existing events remain active after migration)
- Migration SQL: `ALTER TABLE event ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1`
- DB version: 1 → 2
- Toggle scope: THIS_DAY_ONLY (only the pressed event's row, `recurrenceGroupId` untouched)
- Menu item hidden for system events (`isSystemGenerated`) and on finalized days
- Inactive card visual: alpha 0.45f + grey text color

---

### Task 1: Add `isActive` field to domain model, entity, mappers, and DB migration

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/Models.kt:55-69`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt:31-44`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`

**Interfaces:**
- Consumes: nothing new
- Produces: `CruiseEvent.isActive: Boolean`, `EventEntity.isActive: Boolean`, DB v2 with migration

- [ ] **Step 1: Add `isActive` to `CruiseEvent`**

In `app/src/main/java/com/cruise/assistant/domain/Models.kt`, change the `CruiseEvent` data class:

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
)
```

- [ ] **Step 2: Add `isActive` to `EventEntity`**

In `app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt`, add the field at the end:

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
)
```

- [ ] **Step 3: Update mappers**

In `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt`, update both `EventEntity.toDomain()` and `CruiseEvent.toEntity()`:

```kotlin
fun EventEntity.toDomain() = CruiseEvent(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck,
    zone = zone?.let { runCatching { ShipZone.valueOf(it) }.getOrNull() },
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
    isActive = isActive,
)

fun CruiseEvent.toEntity() = EventEntity(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck, zone = zone?.name,
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
    isActive = isActive,
)
```

- [ ] **Step 4: Bump DB version and add migration**

In `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`:

Change `version = 1` to `version = 2`, and add a `MIGRATION_1_2` val + register it in the builder:

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VoyageEntity::class,
        CategoryEntity::class,
        EventEntity::class,
        DayPlanEntity::class,
        LocationPresetEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CruiseDatabase : RoomDatabase() {
    abstract fun voyageDao(): VoyageDao
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun locationPresetDao(): LocationPresetDao

    companion object {
        @Volatile private var instance: CruiseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE event ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun get(context: Context): CruiseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CruiseDatabase::class.java,
                    "cruise.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
```

- [ ] **Step 5: Compile to verify no breakage**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/Models.kt app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt app/src/main/java/com/cruise/assistant/data/db/Mappers.kt app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt
git commit -m "feat: add isActive field to event model with DB migration v1→v2"
```

---

### Task 2: Add `setActive` to DAO and Repository, filter inactive events from lane assignment

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/db/dao/EventDao.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt`

**Interfaces:**
- Consumes: `CruiseEvent.isActive` (from Task 1), `assignLane` (existing)
- Produces: `EventDao.setActive(Long, Boolean)`, `EventRepository.setActive(Long, Boolean)`

- [ ] **Step 1: Add `setActive` query to EventDao**

In `app/src/main/java/com/cruise/assistant/data/db/dao/EventDao.kt`, add after the `moveToLane`-style queries (before `conflictsOn`):

```kotlin
    @Query("UPDATE event SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
```

- [ ] **Step 2: Add `setActive` to EventRepository and filter inactive events in addEvent/updateEvent**

In `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt`, add the `setActive` method (after `moveToLane`):

```kotlin
    /** 切换事件活动/非活动状态（仅本日，不影响循环组其他成员） */
    suspend fun setActive(eventId: Long, active: Boolean) {
        if (active) {
            // 重新激活：重算车道，避免与激活期间新增的事件撞栏
            val entity = eventDao.getById(eventId) ?: return
            val sameDay = eventDao.getByDate(entity.date)
                .map { it.toDomain() }
                .filter { it.isActive && it.id != eventId }
            val event = entity.toDomain()
            val lane = assignLane(event.startMinute, event.endMinute, sameDay)
            eventDao.update(entity.copy(isActive = true, lane = lane))
        } else {
            // 停用：保留原车道仅供显示
            eventDao.setActive(eventId, false)
        }
    }
```

Then update `addEvent` to filter inactive events from the `sameDay` list used for lane assignment. Change the line inside `copies.forEach`:

From:
```kotlin
            val sameDay = eventDao.getByDate(copy.date).map { it.toDomain() }
```
To:
```kotlin
            val sameDay = eventDao.getByDate(copy.date).map { it.toDomain() }.filter { it.isActive }
```

Then update `updateEvent` similarly. In the `else` branch of `timingUnchanged` (where lane is recalculated), change:

From:
```kotlin
                val sameDay = eventDao.getByDate(merged.date).map { it.toDomain() }
                assignLane(merged.startMinute, merged.endMinute, sameDay, excludeId = merged.id)
```
To:
```kotlin
                val sameDay = eventDao.getByDate(merged.date).map { it.toDomain() }.filter { it.isActive }
                assignLane(merged.startMinute, merged.endMinute, sameDay, excludeId = merged.id)
```

- [ ] **Step 3: Compile to verify**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/data/db/dao/EventDao.kt app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt
git commit -m "feat: add setActive to DAO/Repository, filter inactive events from lane assignment"
```

---

### Task 3: Filter inactive events from conflict detection (TDD)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/ConflictDetector.kt:8-23`
- Test: `app/src/test/java/com/cruise/assistant/domain/ConflictDetectorTest.kt`

**Interfaces:**
- Consumes: `CruiseEvent.isActive` (from Task 1)
- Produces: `findConflicts` now ignores `!isActive` events

- [ ] **Step 1: Write failing tests for inactive event exclusion**

Add these tests at the end of `app/src/test/java/com/cruise/assistant/domain/ConflictDetectorTest.kt` (before the closing `}`):

```kotlin
    @Test
    fun inactiveEvents_areExcludedFromConflicts() {
        val events = listOf(
            ev(1, 13, 0, 60).copy(isActive = false),
            ev(2, 13, 20, 60).copy(isActive = false),
        )
        assertTrue(findConflicts(events).isEmpty())
    }

    @Test
    fun inactiveEvent_doesNotConflictWithActiveEvent() {
        val events = listOf(
            ev(1, 13, 0, 60).copy(isActive = true),
            ev(2, 13, 20, 60).copy(isActive = false),
        )
        assertTrue(findConflicts(events).isEmpty())
    }

    @Test
    fun activeEvents_stillConflict_whenInactiveEventPresent() {
        val events = listOf(
            ev(1, 13, 0, 60).copy(isActive = true),
            ev(2, 13, 20, 60).copy(isActive = true),
            ev(3, 13, 10, 60).copy(isActive = false),
        )
        // 只有 1 和 2 都是 active 且重叠 → 1 对冲突
        assertEquals(1, findConflicts(events).size)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.ConflictDetectorTest"`
Expected: FAIL — `inactiveEvents_areExcludedFromConflicts` expects 0 but gets 1 (current `findConflicts` doesn't filter by `isActive`)

- [ ] **Step 3: Implement the fix in `findConflicts`**

In `app/src/main/java/com/cruise/assistant/domain/ConflictDetector.kt`, add the filter at the start of `findConflicts`:

```kotlin
fun findConflicts(events: List<CruiseEvent>): List<Pair<CruiseEvent, CruiseEvent>> {
    val activeEvents = events.filter { it.isActive }
    val sorted = activeEvents.sortedWith(compareBy({ it.startMinute }, { it.id }))
    val result = mutableListOf<Pair<CruiseEvent, CruiseEvent>>()
    for (i in sorted.indices) {
        val a = sorted[i]
        for (j in i + 1 until sorted.size) {
            val b = sorted[j]
            if (b.startMinute >= a.endMinute) break
            if (overlaps(a.startMinute, a.endMinute, b.startMinute, b.endMinute)) {
                result += a to b
            }
        }
    }
    return result
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.ConflictDetectorTest"`
Expected: PASS — all tests green

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ConflictDetector.kt app/src/test/java/com/cruise/assistant/domain/ConflictDetectorTest.kt
git commit -m "feat: exclude inactive events from conflict detection"
```

---

### Task 4: Skip inactive events in reminder scheduling

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/reminder/ReminderScheduler.kt:71-78`

**Interfaces:**
- Consumes: `CruiseEvent.isActive` (from Task 1)
- Produces: inactive events get no alarm

- [ ] **Step 1: Add isActive filter to rescheduleAll**

In `app/src/main/java/com/cruise/assistant/reminder/ReminderScheduler.kt`, in the `rescheduleAll` function, change the `events.asSequence()` line:

From:
```kotlin
        events.asSequence()
            .mapNotNull { event ->
```
To:
```kotlin
        events.asSequence()
            .filter { it.isActive }
            .mapNotNull { event ->
```

- [ ] **Step 2: Compile to verify**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/reminder/ReminderScheduler.kt
git commit -m "feat: skip inactive events in reminder scheduling"
```

---

### Task 5: Add `toggleActive` to TimelineViewModel and filter ongoing/next banners

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineViewModel.kt`

**Interfaces:**
- Consumes: `EventRepository.setActive` (from Task 2), `CruiseEvent.isActive` (from Task 1)
- Produces: `TimelineViewModel.toggleActive(Long, Boolean)`, `ongoing`/`next` exclude inactive

- [ ] **Step 1: Add `toggleActive` function and update `ongoing`/`next` getters**

In `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineViewModel.kt`:

Update the `ongoing` getter in `TimelineUiState`:

From:
```kotlin
    val ongoing: CruiseEvent?
        get() = if (!isToday) null else events.firstOrNull {
            nowMinute in it.startMinute until it.endMinute
        }
```
To:
```kotlin
    val ongoing: CruiseEvent?
        get() = if (!isToday) null else events.filter { it.isActive }.firstOrNull {
            nowMinute in it.startMinute until it.endMinute
        }
```

Update the `next` getter:

From:
```kotlin
    val next: CruiseEvent?
        get() = if (!isToday) null else events
            .filter { it.startMinute > nowMinute }
            .minByOrNull { it.startMinute }
```
To:
```kotlin
    val next: CruiseEvent?
        get() = if (!isToday) null else events
            .filter { it.isActive && it.startMinute > nowMinute }
            .minByOrNull { it.startMinute }
```

Add the `toggleActive` function in `TimelineViewModel` (after `moveToLane`, before `deleteEvent`):

```kotlin
    fun toggleActive(eventId: Long, active: Boolean) {
        viewModelScope.launch {
            eventRepository.setActive(eventId, active)
            reminderScheduler.rescheduleAll()
        }
    }
```

- [ ] **Step 2: Compile to verify**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/TimelineViewModel.kt
git commit -m "feat: add toggleActive to TimelineViewModel, filter ongoing/next by isActive"
```

---

### Task 6: Add toggle menu item to long-press dropdown and wire through CruiseNavHost

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt:54-67,222-256`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt:129-158`

**Interfaces:**
- Consumes: `TimelineViewModel.toggleActive` (from Task 5), `CruiseEvent.isActive` (from Task 1)
- Produces: long-press menu shows "设为非活动"/"设为活动" at top

- [ ] **Step 1: Add string resources**

In `app/src/main/res/values/strings.xml`, add after the `edit` string (line 33):

```xml
    <string name="set_inactive">设为非活动</string>
    <string name="set_active">设为活动</string>
```

- [ ] **Step 2: Add `onToggleActive` parameter to TimelineScreen**

In `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`, add the parameter to the `TimelineScreen` composable signature (after `onMoveToLane`):

```kotlin
    onMoveToLane: (Long, Int) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onDeleteEvent: (CruiseEvent, RecurrenceScope) -> Unit,
```

- [ ] **Step 3: Add the toggle menu item at the top of the long-press dropdown**

In the `menuEvent?.let { event ->` block, add the toggle item as the **first** item (before the move-to-lane section). Insert right after `DropdownMenu(expanded = true, onDismissRequest = { menuEvent = null }) {`:

```kotlin
            if (!state.isFinalized && !event.isSystemGenerated) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (event.isActive) R.string.set_inactive else R.string.set_active,
                            ),
                        )
                    },
                    onClick = {
                        onToggleActive(event.id, !event.isActive)
                        menuEvent = null
                    },
                )
            }
```

- [ ] **Step 4: Wire `onToggleActive` in CruiseNavHost**

In `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`, in the `TimelineScreen(...)` call, add after `onMoveToLane = vm::moveToLane,`:

```kotlin
                    onMoveToLane = vm::moveToLane,
                    onToggleActive = vm::toggleActive,
                    onDeleteEvent = vm::deleteEvent,
```

- [ ] **Step 5: Compile to verify**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: add active/inactive toggle to long-press menu"
```

---

### Task 7: Grey out inactive events in EventCard

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`

**Interfaces:**
- Consumes: `CruiseEvent.isActive` (from Task 1)
- Produces: inactive cards render with alpha 0.45 + grey text

- [ ] **Step 1: Add alpha and grey text for inactive events**

In `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`, add the `alpha` import:

```kotlin
import androidx.compose.ui.draw.alpha
```

Then in the `EventCard` composable, update the outer `Row` modifier to apply alpha when inactive. Change:

```kotlin
    val colorArgb = category?.colorArgb ?: 0xFF9E9E9E.toInt()
    val textColor = cardTextColorFor(colorArgb)
    val compact = heightDp < COMPACT_THRESHOLD
    val location = formatLocation(event.deck, event.zone, event.placeName)

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundFor(colorArgb))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
```

To:

```kotlin
    val colorArgb = category?.colorArgb ?: 0xFF9E9E9E.toInt()
    val active = event.isActive
    val textColor = if (active) cardTextColorFor(colorArgb) else MaterialTheme.colorScheme.onSurfaceVariant
    val compact = heightDp < COMPACT_THRESHOLD
    val location = formatLocation(event.deck, event.zone, event.placeName)

    Row(
        modifier
            .fillMaxWidth()
            .then(if (active) Modifier else Modifier.alpha(0.45f))
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundFor(colorArgb))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
```

Then update the left color bar to also dim when inactive. Change:

```kotlin
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color(colorArgb)),
        )
```

To:

```kotlin
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .then(if (active) Modifier else Modifier.alpha(0.45f))
                .background(Color(colorArgb)),
        )
```

- [ ] **Step 2: Compile to verify**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt
git commit -m "feat: grey out inactive events in EventCard"
```

---

### Task 8: Build, run tests, and verify release APK

**Files:**
- Generated: `app/release/CruiseAssistant-v1.1.apk` (updated)

- [ ] **Step 1: Run full test suite**

Run: `.\gradlew.bat test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Build release APK**

Run: `.\gradlew.bat assembleRelease`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Copy updated APK to release folder**

Run in PowerShell:
```powershell
Copy-Item "app\build\outputs\apk\release\CruiseAssistant-v1.1.apk" "app\release\CruiseAssistant-v1.1.apk" -Force
Test-Path "app\release\CruiseAssistant-v1.1.apk"
```
Expected: True

- [ ] **Step 4: Commit updated release artifact**

```bash
git add app/release/CruiseAssistant-v1.1.apk
git commit -m "release: rebuild CruiseAssistant-v1.1.apk with active/inactive toggle"
```
