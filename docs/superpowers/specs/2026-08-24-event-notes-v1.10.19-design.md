# Design Spec: Event Notes & Detail Dialog Presentation v1.10.19

## 1. Background & Objectives
1. **Event Notes (备注) Feature**:
   - Users need to attach personal notes to schedule events (e.g. dress code reminders, reservation codes, meeting notes).
   - Notes should be editable during event creation and editing (`EventSheet.kt`).
   - Notes should be prominently displayed in the event detail popup (`EventDetailDialog.kt`) directly below the location card.
   - Wishlist items already support notes, and their presentation should remain aligned with the new event notes design.
2. **Database & Persistence**:
   - Add `note: String? = null` to `CruiseEvent` and `EventEntity`.
   - Update `CruiseDatabase` version to 8 with Room `AutoMigration(from = 7, to = 8)`.

## 2. Technical Architecture & Component Design

### 2.1 Domain & Data Layer
- **`Models.kt`**:
  - `data class CruiseEvent(..., val note: String? = null)`
- **`EventEntity.kt`**:
  - `data class EventEntity(..., val note: String? = null)`
- **`Mappers.kt`**:
  - Bi-directional mapping between `CruiseEvent.note` and `EventEntity.note`.
- **`CruiseDatabase.kt`**:
  - `version = 8`
  - `autoMigrations = [..., AutoMigration(from = 7, to = 8)]`

### 2.2 ViewModel & State (`EventSheetViewModel.kt`)
- `EventSheetState`: add `val note: String = ""`
- Methods:
  - `openForCreate`: initializes `note = ""`
  - `openForEdit(event)`: populates `note = event.note.orEmpty()`
  - `onNoteChange(value: String)`: updates `note` state
  - `save(...)`: constructs `CruiseEvent(..., note = s.note.trim().takeIf { it.isNotEmpty() })`

### 2.3 UI Layer
- **`EventSheet.kt`**:
  - Add Notes Section below Location / Wait segments:
    - Card container with `Icons.Outlined.EditNote` and SectionHeader "备注".
    - `OutlinedTextField` with `placeholder = "添加备注信息（选填）"`, `minLines = 2`, `maxLines = 4`.
- **`EventDetailDialog.kt`**:
  - Directly below Location Section (`if (!event.note.isNullOrBlank())`):
    - Rounded card (`Slate100` background, `Slate200` border, `12.dp` corners).
    - `Icons.Outlined.Notes` icon.
    - Column with "备注" label (`Slate600`, bold) and note text (`Slate800`, `14.sp`).

### 2.4 Localization & Versioning
- `res/values/strings.xml`:
  - `<string name="note_label">备注</string>`
  - `<string name="note_hint">添加备注信息（选填）</string>`
- `app/build.gradle.kts`:
  - `versionCode = 45`, `versionName = "1.10.19"`.

## 3. Verification Plan
- Unit tests for DB mapping, ViewModel state, and event repository CRUD operations.
- Emulator verification: create/edit event with note, open detail dialog, verify display below location card.
