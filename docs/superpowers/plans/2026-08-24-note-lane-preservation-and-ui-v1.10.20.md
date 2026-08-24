# Note Lane Preservation, UI Redesign & Card Indicator v1.10.20 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix event lane reset bug when updating events with unchanged timing, redesign the Note card in EventDetailDialog using UI-UX-PRO-MAX guidelines, and add a note indicator badge next to CategoryChip on timeline event cards.

**Architecture:** In data layer (`EventRepository.kt`), load existing entity to preserve `target.lane` when timing is unchanged. In UI layer (`EventDetailDialog.kt`), redesign Note card with pastel badge and refined typography. In `EventCard.kt`, render a `NoteIndicator` next to `CategoryChip` when `!event.note.isNullOrBlank()`.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 46`, `versionName = "1.10.20"`
- Lane preservation: When updating an event without changing `startTime` or `durationMinutes`, its existing `lane` must be strictly preserved.
- Note indicator on card: Placed immediately after `CategoryChip` when `event.note` is non-blank.
- 100% of unit tests in `gradlew testDebugUnitTest` must pass.

---

### Task 1: Fix Lane Preservation on Event Edit

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`
- Modify/Test: `app/src/test/java/com/cruise/assistant/data/repo/EventRepositoryTest.kt`

**Interfaces:**
- Consumes: `eventDao.getById(id)`
- Produces: Preserved `target.lane` during event updates when time is unchanged

- [ ] **Step 1: Update `EventSheetViewModel.kt`**
Add `val lane: Int = 0` to `EventSheetState`. In `openForEdit(event)`, set `lane = event.lane`. In `save()`, pass `lane = s.lane` to `CruiseEvent`.

- [ ] **Step 2: Update `EventRepository.kt`**
In `updateEvent(event, scope)`:
```kotlin
        val groupId = event.recurrenceGroupId
        val targets = if (scope == RecurrenceScope.WHOLE_GROUP && groupId != null) {
            val finalizedDates = voyageDao.observeDayPlansOnce()
                .filter { it.finalized }
                .mapTo(mutableSetOf()) { it.date }
            eventDao.getByGroup(groupId).map { it.toDomain() }
                .filter { it.id == event.id || it.date !in finalizedDates }
        } else {
            val existing = eventDao.getById(event.id)?.toDomain()
            listOf(existing ?: event)
        }
```

- [ ] **Step 3: Run unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 4: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt
git commit -m "fix(repo): preserve existing lane when updating event without time changes"
```

---

### Task 2: Redesign Note Section in `EventDetailDialog.kt`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt`

**Interfaces:**
- Consumes: `event.note: String?`
- Produces: Polished note card with pastel circular icon badge, clean typography, and subtle border

- [ ] **Step 1: Update note card styling in `EventDetailDialog.kt`**
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
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = event.note,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = Color(0xFF1E293B),
                                )
                            }
                        }
                    }
```

- [ ] **Step 2: Verify compilation and tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt
git commit -m "style(ui): redesign EventDetailDialog note card with pastel badge and refined typography"
```

---

### Task 3: Timeline Card Note Indicator (`EventCard.kt`)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`

**Interfaces:**
- Consumes: `event.note: String?`
- Produces: NoteIndicator micro-badge rendered next to CategoryChip

- [ ] **Step 1: Add `NoteIndicator` composable in `EventCard.kt`**
```kotlin
@Composable
private fun NoteIndicator(colorArgb: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(colorArgb).copy(alpha = 0.18f),
        modifier = modifier.padding(start = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.EditNote,
                contentDescription = "有备注",
                tint = Color(colorArgb),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Render `NoteIndicator` next to `CategoryChip` across all card variants**
In `EventCard.kt`:
- Finalized compact row: after `CategoryChip`, if `!event.note.isNullOrBlank()`, call `NoteIndicator(colorArgb)`.
- Standard 3-line row: after `CategoryChip`, if `!event.note.isNullOrBlank()`, call `NoteIndicator(colorArgb)`.
- Multi-line wait segment rows: after `CategoryChip`, if `!event.note.isNullOrBlank()`, call `NoteIndicator(colorArgb)`.

- [ ] **Step 3: Run unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 4: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt
git commit -m "feat(ui): add note indicator badge next to category chip on event cards"
```

---

### Task 4: Version Bump, Build & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 46`, `versionName = "1.10.20"`.

- [ ] **Step 2: Build and install APK**
Run `assembleDebug` and install to running emulator.

- [ ] **Step 3: Test on emulator and capture verification screenshots**
- Verify lane preservation (edit show note in lane 1, verify it remains in lane 1 in draft view).
- Verify note indicator on event card next to `[演出]`.
- Verify redesigned note card in `EventDetailDialog`.
- Save screenshots to `人偶效果截图对比/05_事件详情备注展示.png` and `人偶效果截图对比/01_定稿单栏_双人组合与单人点缀.png`.

- [ ] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.20 (46) and add verification screenshots"
```
