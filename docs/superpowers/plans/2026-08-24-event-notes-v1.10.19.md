# Event Notes Feature v1.10.19 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a note (备注) field to schedule events with Room v8 auto-migration, event creation/edit sheet input, and prominent display in EventDetailDialog directly below the location card.

**Architecture:** Domain model `CruiseEvent` and entity `EventEntity` add `note: String?`. Room database updates to `version = 8` with `AutoMigration(from = 7, to = 8)`. `EventSheetViewModel` and `EventSheet` support note editing. `EventDetailDialog` renders a dedicated notes card when `event.note` is non-blank.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database 2.6+, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 45`, `versionName = "1.10.19"`
- Database version: `8` with `AutoMigration(from = 7, to = 8)`
- Note field is optional (`String?`, default `null` / empty string)
- In `EventDetailDialog`, note card is rendered ONLY when `!event.note.isNullOrBlank()`
- 100% of unit tests in `gradlew testDebugUnitTest` must pass

---

### Task 1: Domain, Database Entity & Auto-Migration (v8)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/Models.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`
- Modify/Test: `app/src/test/java/com/cruise/assistant/data/db/MappersTest.kt` (or domain tests)

**Interfaces:**
- Consumes: Room `@Database`, `@Entity`
- Produces: `CruiseEvent.note`, `EventEntity.note`, Room DB v8 migration

- [x] **Step 1: Update `Models.kt` and `EventEntity.kt`**
Add `val note: String? = null` to `CruiseEvent` and `EventEntity`.

- [x] **Step 2: Update `Mappers.kt`**
Add `note = note` to `EventEntity.toDomain()` and `CruiseEvent.toEntity()`.

- [x] **Step 3: Update `CruiseDatabase.kt`**
Bump `version = 8` and add `AutoMigration(from = 7, to = 8)`.

- [x] **Step 4: Run compilation and tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [x] **Step 5: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/domain/Models.kt app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt app/src/main/java/com/cruise/assistant/data/db/Mappers.kt app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt app/schemas/
git commit -m "feat(db): add note column to event table and bump database to version 8"
```

---

### Task 2: EventSheet Note Input & ViewModel Support

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt` (or EventSheet wrapper if needed)

**Interfaces:**
- Consumes: `EventSheetState.note`, `onNoteChange: (String) -> Unit`
- Produces: UI card for entering and editing notes in EventSheet

- [x] **Step 1: Add string resources**
Add `note_label` ("备注") and `note_hint` ("添加备注信息（选填）") to `strings.xml` and `values-zh-rCN/strings.xml`.

- [x] **Step 2: Update `EventSheetViewModel.kt`**
- Add `val note: String = ""` to `EventSheetState`.
- Update `openForCreate`: `note = ""`.
- Update `openForEdit(event)`: `note = event.note.orEmpty()`.
- Add `fun onNoteChange(value: String) = _state.update { it.copy(note = value) }`.
- In `save(...)`: pass `note = s.note.trim().takeIf { it.isNotEmpty() }` to `CruiseEvent(...)`.

- [x] **Step 3: Update `EventSheet.kt`**
Add `onNoteChange: (String) -> Unit` parameter and render Notes Card below location/wait segment.

- [x] **Step 4: Run unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [x] **Step 5: Commit changes**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt
git commit -m "feat(ui): add note input section to EventSheet"
```

---

### Task 3: EventDetailDialog Notes Section Display

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt`

**Interfaces:**
- Consumes: `event.note: String?`
- Produces: Clean notes card rendered directly below the location card

- [x] **Step 1: Update `EventDetailDialog.kt`**
Below Location Section:
```kotlin
if (!event.note.isNullOrBlank()) {
    Spacer(Modifier.height(10.dp))
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate100.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.Notes,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.note_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate600,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate800,
                )
            }
        }
    }
}
```

- [x] **Step 2: Run unit tests and verify compile**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [x] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt
git commit -m "feat(ui): display note card in EventDetailDialog below location card"
```

---

### Task 4: Version Bump, Build & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [x] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 45`, `versionName = "1.10.19"`.

- [x] **Step 2: Build and install APK**
Run `assembleDebug` and install to running emulator.

- [x] **Step 3: Test on emulator and capture verification screenshot**
- Open EventDetailDialog for an event with note to verify presentation.
- Capture screenshot and save to `人偶效果截图对比/05_事件详情备注展示.png`.

- [x] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.19 (45) and add verification screenshots"
```
