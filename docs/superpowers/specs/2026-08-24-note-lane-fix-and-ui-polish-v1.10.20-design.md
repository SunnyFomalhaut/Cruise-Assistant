# Design Spec: Event Edit Lane Persistence, Note UI Redesign & Card Note Badge v1.10.20

## 1. Background & Problem Statement
1. **Lane Jump Bug on Edit**:
   - When editing an existing event (e.g. adding a note to a preset show like "扬帆起航" in lane 1), `EventSheetViewModel` created a new `CruiseEvent` with default `lane = 0` and `EventRepository.updateEvent` used `target.lane` from the passed object instead of the database entity. This caused edited events to jump back to Lane 0.
2. **Note Card UI Polish in EventDetailDialog**:
   - The current note card in `EventDetailDialog` uses a generic `Slate100` gray background with a 3-line hamburger-like icon, looking cold and unpolished compared to the rest of the Disney cruise UI.
3. **Timeline Card Note Indicator**:
   - Users cannot see from the timeline view whether a card contains notes without clicking into it. An indicator icon/badge is needed to the right of `CategoryChip`.

## 2. Technical Specifications

### 2.1 Lane Persistence & Edit Fix (`EventSheetViewModel.kt` & `EventRepository.kt`)
- **`EventSheetState`**:
  - Add `val originalLane: Int = 0`
  - Add `val presetKey: String? = null`
- **`EventSheetViewModel.kt`**:
  - In `openForEdit(event)`: record `originalLane = event.lane, presetKey = event.presetKey`.
  - In `save()`: pass `lane = s.originalLane, presetKey = s.presetKey` when instantiating `CruiseEvent`.
- **`EventRepository.kt`**:
  - In `updateEvent(event, scope)`: when `scope != WHOLE_GROUP`, resolve `target` by reading `eventDao.getById(event.id)?.toDomain() ?: event`. If event timing is unchanged (`startTime` and `durationMinutes`), keep `target.lane`.

### 2.2 Redesigned Note Card in `EventDetailDialog.kt`
- Styled as a warm, delightful Disney travel notebook card:
  - Container: `Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFFDF5), border = BorderStroke(1.dp, Color(0xFFFDE68A)))`
  - Leading Icon: `Icons.Outlined.StickyNote2` in warm amber (`Color(0xFFD97706)`).
  - Header Tag: `Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFEF3C7))` with text `"备注"` in `Color(0xFF92400E)` bold `11.sp`.
  - Note Content: `Text(text = event.note, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1E293B))` with 1.4x line height and comfortable padding.

### 2.3 Note Indicator on Timeline Event Cards (`EventCard.kt`)
- Component: `@Composable private fun NoteBadge(colorArgb: Int, tint: Color)`
  - A subtle, polished note pill/icon displayed immediately to the right of `CategoryChip`:
    `Surface(shape = RoundedCornerShape(4.dp), color = Color(colorArgb).copy(alpha = 0.15f))` containing `Icon(Icons.Outlined.StickyNote2, tint = tint, modifier = Modifier.size(11.dp))`.
  - Rendered across all `EventCard` layouts (normal multi-line, compact finalized, and show waiting segments) whenever `!event.note.isNullOrBlank()`.

### 2.4 Versioning
- `app/build.gradle.kts`: `versionCode = 46`, `versionName = "1.10.20"`.

## 3. Verification Plan
1. Unit test: edit event with note -> verify `lane` is preserved in `EventRepository` and `EventSheetViewModel`.
2. Unit test: all 207+ existing tests pass.
3. Build and install `CruiseAssistant-v1.10.20.apk` on emulator.
4. Verify:
   - Edit "扬帆起航" on Day 1 (lane 1), add note -> stays in lane 1.
   - Event card on timeline shows `[演出] 📝` note badge.
   - Click card -> detail dialog displays beautiful warm amber note card.
