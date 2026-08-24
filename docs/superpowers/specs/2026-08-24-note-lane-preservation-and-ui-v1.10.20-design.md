# Design Spec: Event Notes Polish, Lane Preservation & Card Indicator v1.10.20

## 1. Problem Statement & Objectives
1. **Bug: Lane Reset on Note Edit**:
   - When editing an existing event (e.g., adding or modifying notes), `EventRepository.updateEvent` constructed `targets = listOf(event)` where `event.lane` was unset (default `0`). As a result, non-recurring events in lane 1 or 2 (such as preset shows) were inadvertently reset to lane 0 even when their timing was unchanged.
   - **Fix**: Load the existing event from `eventDao.getById(event.id)` in `EventRepository.updateEvent`, ensuring `target.lane` preserves the original lane assignment when start time and duration are unchanged.
2. **UI/UX Redesign for Notes in EventDetailDialog**:
   - The current notes section looks plain and mismatched with the playful Disney cruise aesthetic.
   - **Redesign**: Create a refined pastel note card with:
     - Rounded corner container (`14.dp`) with soft slate-50 background (`#F8FAFC`) and refined subtle border (`#E2E8F0`).
     - Header badge containing a note icon (`Icons.Outlined.StickyNote2` or `EditNote`) and a crisp "备注" label in purple/marine accent (`#7C3AED` / `CruiseMarine`).
     - Clear readable body text (`#1E293B`, `14.sp`, comfortable line height `20.sp`).
3. **Card Note Indicator on Timeline (`EventCard.kt`)**:
   - Users want to instantly see which events have notes directly on the timeline.
   - **Design**: When `!event.note.isNullOrBlank()`, render a subtle `NoteIndicator` micro-chip / icon immediately to the right of `CategoryChip` (e.g. `[演出] 📝`).
   - Works across all card tiers (finalized compact cards, standard multi-line cards, and cards with wait segments).

## 2. Technical Architecture

### 2.1 Bug Fix in Data Layer (`EventRepository.kt` & `EventSheetViewModel.kt`)
- `EventSheetViewModel.kt`:
  - Preserve `lane = s.lane` in `EventSheetState` and propagate when creating `CruiseEvent`.
- `EventRepository.kt`:
  - When `scope != RecurrenceScope.WHOLE_GROUP || groupId == null`:
    `val existing = eventDao.getById(event.id)?.toDomain()`
    `val targets = if (existing != null) listOf(existing) else listOf(event)`
  - When updating target:
    If `timingUnchanged == true`: preserve `target.lane`.
    If `timingUnchanged == false`: compute new lane via `assignLane(..., excludeId = target.id)`.

### 2.2 UI Layer (`EventDetailDialog.kt`)
- Redesign Notes Section:
```kotlin
if (!event.note.isNullOrBlank()) {
    Spacer(Modifier.height(12.dp))
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.note_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D28D9),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = event.note,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = Color(0xFF1E293B),
            )
        }
    }
}
```

### 2.3 Timeline Card Note Indicator (`EventCard.kt`)
- Create `@Composable private fun NoteIndicator(colorArgb: Int)`:
  A refined micro-badge containing `Icons.Outlined.EditNote` (size `13.dp`, alpha `0.85f`).
- Render immediately to the right of `CategoryChip` when `!event.note.isNullOrBlank()`.

### 2.4 Versioning
- `app/build.gradle.kts`: `versionCode = 46`, `versionName = "1.10.20"`.

## 3. Verification Plan
- Unit test: `EventRepositoryTest` verifying that updating an event (with unchanged time) strictly preserves its existing `lane`.
- Unit test: `MappersTest` and `EventCardTest` if applicable.
- Emulator verification:
  1. Open a lane 1 event (e.g. 14:45 show in lane 1), add a note, save -> verify it stays in lane 1!
  2. Inspect timeline card -> verify `📝` note indicator appears next to `[演出]`.
  3. Click card -> inspect the newly styled Disney-themed note card in `EventDetailDialog`.
