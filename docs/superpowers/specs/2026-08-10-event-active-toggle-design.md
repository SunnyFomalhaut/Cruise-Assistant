# Event Active/Inactive Toggle Design Specification

## Overview
Add an active/inactive state toggle to each event. Inactive events remain visible on the timeline (greyed out) but are excluded from conflict detection, lane assignment, reminders, and the ongoing/next banners. This lets users temporarily "park" alternative plans (e.g. one of three showtimes) without deleting them, so they can reactivate later without re-entering details.

## Data Layer

### Entity & Domain Model
- `EventEntity`: add `val isActive: Boolean = true`
- `CruiseEvent`: add `val isActive: Boolean = true`
- `Mappers.kt`: map the new field in both `toDomain()` and `toEntity()`

### Database Migration
- Bump `CruiseDatabase` version 1 → 2
- Add migration: `ALTER TABLE event ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1`
- Register migration in `CruiseDatabase.get()` builder

### DAO
- `EventDao`: add `@Query("UPDATE event SET isActive = :active WHERE id = :id") suspend fun setActive(id: Long, active: Boolean)`

### Repository
- `EventRepository.setActive(eventId: Long, active: Boolean)`:
  - Update `isActive` via DAO
  - When reactivating (`active = true`): recompute lane via `assignLane` against same-day active events only, then update the entity's lane
  - When deactivating (`active = false`): keep existing lane unchanged (display-only)
  - After update: call `reminderScheduler.rescheduleAll()` is handled by ViewModel layer

## Domain Logic

### Conflict Detection
- `findConflicts(events)`: filter to `events.filter { it.isActive }` before detecting overlaps
- This makes the finalize-time conflict check automatically exclude inactive events (since `EventRepository.conflictsOn` calls `findConflicts` on the full day's events)

### Lane Assignment
- `assignLane`: no signature change. The filtering happens at call sites:
  - `EventRepository.addEvent`: filter `sameDay` to `.filter { it.isActive }` before passing to `assignLane`
  - `EventRepository.updateEvent`: same filter on `sameDay` for timing-changed events
  - `EventRepository.setActive`: pass only active events (excluding the reactivated event itself via `excludeId`)

### Timeline Layout
- `buildTimelineLayout`: no change. Inactive events still occupy space — this prevents layout jumps when toggling and keeps the event visible for reactivation.

### Reminders
- `ReminderScheduler.rescheduleAll()`: skip events where `!isActive` — do not schedule alarms for inactive events

## UI Layer

### Long-Press Menu (TimelineScreen)
- Add a new `DropdownMenuItem` at the **top** of the existing long-press menu (before move-to-lane / edit / delete):
  - When `event.isActive == true`: label = "设为非活动"
  - When `event.isActive == false`: label = "设为活动"
  - Only show when `!state.isFinalized && !event.isSystemGenerated`
- Callback: `onToggleActive(event.id, !event.isActive)`
- No recurrence scope dialog — toggle is always THIS_DAY_ONLY (only the pressed event's row is updated; `recurrenceGroupId` is untouched)

### EventCard Visual
- When `event.isActive == false`:
  - Apply `Modifier.alpha(0.45f)` to the entire card Row
  - Text color overridden to grey (`MaterialTheme.colorScheme.onSurfaceVariant`) regardless of category color
  - Left color bar also dimmed via alpha
- When `event.isActive == true`: unchanged from current behavior

### TimelineViewModel
- `ongoing` / `next` getters: filter `events.filter { it.isActive }` before computing
- New function: `fun toggleActive(eventId: Long, active: Boolean)` — calls `eventRepository.setActive(eventId, active)` then `reminderScheduler.rescheduleAll()`

### TimelineScreen & CruiseNavHost Wiring
- `TimelineScreen` signature: add `onToggleActive: (Long, Boolean) -> Unit`
- `CruiseNavHost`: wire `onToggleActive = vm::toggleActive`
- `LaneCanvas`: no change needed (EventCard handles its own visual state based on `event.isActive`)

## Edge Cases
- **System events (board/disembark)**: cannot be deactivated — menu item hidden for system events
- **Finalized days**: menu item hidden (consistent with existing edit/delete hiding on finalized days)
- **Reactivation lane collision**: if another event took the same lane while this was inactive, `assignLane` will find the next free lane on reactivation
- **Recurrence group integrity**: toggling does NOT detach from the recurrence group (`recurrenceGroupId` preserved). WHOLE_GROUP edits from another day still propagate time/title/category to this day's inactive event — only its `isActive` flag is untouched.

## Testing Strategy
- `ConflictDetectorTest`: add tests verifying inactive events are excluded from conflict detection
- `LaneAssignerTest`: no change (assignLane itself unchanged; filtering is at call sites)
- Unit test for `setActive` reactivation lane reassignment behavior (requires a fake DAO)

## Out of Scope
- Bulk activate/deactivate (toggle all events of a day)
- Active/inactive summary badges or counters
- Filtering timeline to show only active events
