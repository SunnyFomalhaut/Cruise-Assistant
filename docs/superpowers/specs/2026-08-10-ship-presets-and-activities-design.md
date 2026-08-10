# Ship Presets & Built-in Activities Design Specification

## Overview
Upgrade the onboarding flow to support ship selection via a dropdown (with Disney Adventure preset + custom), bind ship schedules (4-night/5-night presets + custom schedule), and auto-generate built-in activities (safety drill + daily dinner) on voyage creation. Also add a dinner-time toggle in Settings that immediately overwrites existing dinner events.

## 1. Ship Preset Model

### Domain Models (new file: `domain/ShipPreset.kt`)
```kotlin
data class SchedulePreset(
    val label: String,               // "四天三夜"
    val nights: Int,                 // 3 or 4
    val boardDayOfWeek: DayOfWeek,   // MONDAY or THURSDAY
    val disembarkDayOfWeek: DayOfWeek,
    val boardTime: LocalTime,
    val disembarkTime: LocalTime,
)

data class ShipPreset(
    val name: String,                 // "Disney Adventure"
    val schedules: List<SchedulePreset>,
)
```

### Static Registry (in same file)
```kotlin
val BUILT_IN_SHIPS: List<ShipPreset> = listOf(
    ShipPreset(
        name = "Disney Adventure",
        schedules = listOf(
            SchedulePreset("四天三夜", 3, DayOfWeek.MONDAY, DayOfWeek.THURSDAY,
                LocalTime.of(12, 0), LocalTime.of(7, 0)),
            SchedulePreset("五天四夜", 4, DayOfWeek.THURSDAY, DayOfWeek.MONDAY,
                LocalTime.of(12, 0), LocalTime.of(7, 0)),
        ),
    ),
)
```
Adding future ships = append to this list. No DB change needed.

## 2. Onboarding UI Changes

### Ship Name Control
Replace the `OutlinedTextField` for `shipName` with an `ExposedDropdownMenuBox`:
- Items: each `BUILT_IN_SHIPS` name + "自定义船名…" as the last entry
- When a built-in ship is selected: `shipName = preset.name`, hide manually typed field, show schedule dropdown (see below)
- When "自定义船名…" is selected: reveal an `OutlinedTextField` for free-text ship name, hide the schedule dropdown, show the existing free-form date/time pickers directly

### Schedule Dropdown (shown only for built-in ships)
A second `ExposedDropdownMenuBox` listing that ship's `schedules` + "自定义船期…" as the last entry:
- When a preset schedule is selected:
  - Compute `boardDate` = next occurrence of `boardDayOfWeek` from today (or today if today is that weekday)
  - Compute `disembarkDate` = `boardDate.plusDays(nights)`
  - Set `boardTime` / `disembarkTime` from the preset
  - Hide the free-form date pickers and board-time wheel (they become read-only display, or hidden)
- When "自定义船期…" is selected: show the existing free-form date pickers + board-time wheel, user controls everything

### Custom Ship Behavior
When ship = custom: no schedule dropdown at all. All date/time fields are user-controlled, exactly as the current onboarding works today.

### Dinner Time Selection
After a built-in ship schedule is chosen, show a "晚餐时段" row with two `FilterChip` options: `17:45` and `20:00`, default `17:45`. Store selection in `OnboardingState.dinnerTime`; pass to `createVoyage`. Custom ships do NOT show this row (no built-in dinners generated).

### State Changes (`OnboardingState`)
- Add `selectedShipIndex: Int = -1` (−1 = custom)
- Add `selectedScheduleIndex: Int = -1` (−1 = custom schedule)
- Add `dinnerTime: LocalTime = LocalTime.of(17, 45)`
- Keep existing `shipName`, `boardDate`, `boardTime`, `disembarkDate`, `maxDeck`

### ViewModel Changes (`OnboardingViewModel`)
- `onShipSelected(index: Int)`: sets `selectedShipIndex`, populates `shipName` from preset (or clears it for custom), resets `selectedScheduleIndex = -1`
- `onScheduleSelected(index: Int)`: sets `selectedScheduleIndex`, computes `boardDate`/`disembarkDate`/`boardTime` from the preset
- `onDinnerTimeChange(time: LocalTime)`: sets `dinnerTime`
- `submit()` passes `dinnerTime` to `voyageRepository.createVoyage(...)`

## 3. Built-in Activities Generation

### `createVoyage` Signature Change
Add parameter `dinnerTime: LocalTime` (default `17:45`).

### Activities Inserted
After existing board/disembark system events, insert:

**Safety Drill (安全演习)**
- Day: `boardDate` (day 1 only)
- Time: 15:45 – 16:20 (35 min)
- Location: deck=6, zone=MID, placeName=null
- Title: "安全演习"
- Category: "重要事项" (iconKey="important")
- `isSystemGenerated = false`, `presetKey = "safety_drill"`

**Dinner (晚餐)**
- Every day in `voyage.datesInVoyage()` except disembark day (下船日 07:00 下船，没有晚餐)
- Time: `dinnerTime` – `dinnerTime + 75 min` (1h 15m fixed duration)
- Location: none
- Title: "晚餐"
- Category: "餐食" (iconKey="meal")
- `isSystemGenerated = false`, `presetKey = "dinner"`

### Recurrence Seed
Dinner events are generated as individual rows per day (not via `recurrenceGroupId`), so each dinner can be independently edited/deleted/toggled by the user. They share `presetKey="dinner"` for batch updates.

## 4. `presetKey` Field & DB Migration v2 → v3

### `CruiseEvent` & `EventEntity`
- Add `val presetKey: String? = null`
- Mappers: map `presetKey` in both directions

### Migration
```sql
ALTER TABLE event ADD COLUMN presetKey TEXT
```
Register `MIGRATION_2_3` in `CruiseDatabase.get()` builder with `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)`.

### `EventDao`
Add:
```kotlin
@Query("SELECT * FROM event WHERE presetKey = :key")
suspend fun getByPresetKey(key: String): List<EventEntity>
```

## 5. Dinner Time Setting & Immediate Overwrite

### `AppSettings` & `SettingsStore`
- Add `dinnerTime: LocalTime = LocalTime.of(17, 45)` to `AppSettings`
- Add DataStore key `dinner_time` (store as "HH:mm" string)
- Add `suspend fun setDinnerTime(time: LocalTime)`

### `VoyageRepository.updateDinnerEventsTime(newTime: LocalTime)`
- Query `eventDao.getByPresetKey("dinner")` to get all dinner events
- For each: set `startTime = newTime` (keep `durationMinutes = 75`, keep date), `update()` via DAO
- No lane reassignment needed (dinner events are isolated across days; lane is per-day and likely unique in its time slot)
- Caller (`SettingsViewModel`) triggers `reminderScheduler.rescheduleAll()` after this returns

### Settings UI
- New section "晚餐时段" in `SettingsScreen`, placed between the "提醒" section and "航次" section
- Two `FilterChip`s: 17:45 / 20:00
- On click → `SettingsViewModel.setDinnerTime(newTime)`
- `SettingsViewModel.setDinnerTime`: calls `settingsStore.setDinnerTime(newTime)` then `voyageRepository.updateDinnerEventsTime(newTime)` then `reminderScheduler.rescheduleAll()`

## 6. Editability of Preset Events

### Current Behavior
`isSystemGenerated = true` triggers hard protections:
- `EventSheet`: title field disabled, location picker hidden, only time editable
- Long-press menu: edit hidden, delete hidden
- `EventRepository.deleteEvent`: silently ignored for system events

### New Behavior for Preset Events (safety drill + dinner)
- `isSystemGenerated = false` (not protected as system anchor)
- `presetKey` non-null (identifies them as preset-generated)
- Fully editable and deletable by the user (same as a normal event they created)
- `EventRepository.deleteEvent` and `updateEvent` treat them like ordinary events
- If user deletes a dinner or safety drill, it's just gone (no auto-regeneration except via "重建航次" reset)

### No Change for Board/Disembark
- `isSystemGenerated = true`, `presetKey = null` — hard protection unchanged

## 7. Strings (`strings.xml`)
```xml
<string name="custom_ship">自定义船名…</string>
<string name="custom_schedule">自定义船期…</string>
<string name="schedule_label">船期</string>
<string name="dinner_time_section">晚餐时段</string>
<string name="dinner_time_1745">17:45</string>
<string name="dinner_time_2000">20:00</string>
```

## 8. Edge Cases
- **Schedule date computation**: "next Monday from today" = if today is Monday, use today; otherwise the next Monday
- **Custom ship + custom schedule**: all fields free-form, no presets applied, no built-in activities generated beyond board/disembark (safety drill + dinner should NOT auto-generate for custom ships since we don't know the ship's rules) — wait, user said dinner is for Disney Adventure specifically; for custom ships, skip safety drill and dinner entirely
- **Re-onboarding (reset voyage)**: `resetVoyage()` clears all; new onboarding regenerates from scratch
- **Dinner on disembark day**: excluded (07:00 departure, no dinner that evening)
- **Settings dinner toggle with no voyage**: `updateDinnerEventsTime` finds zero events, no-op, but persists the setting for next voyage creation

## 9. Scope Decisions
- Built-in activities (safety drill + dinner) ONLY generated for built-in ships (Disney Adventure). Custom ships get only board/disembark as today.
- Schedule date computation uses "today" as the reference point for "next Monday/Thursday".
- Dinner duration is hardcoded 75 min (1h 15min); not user-configurable in this version.

## 10. Testing Strategy
- Unit test: schedule date computation (next Monday/Thursday from a known today)
- Unit test: `findConflicts` already filters by isActive — no regression
- No integration tests for VoyageRepository (no existing pattern + no fake DAO infrastructure)
- Manual verification: build release APK, verify onboarding dropdown renders, settings dinner toggle works