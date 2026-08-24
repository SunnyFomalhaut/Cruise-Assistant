# Event Edit Lane Persistence, Note UI Redesign & Card Note Badge v1.10.20 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix event lane jumping back to lane 0 when editing an event with notes, redesign the note card in `EventDetailDialog` into a warm Disney-styled notebook card using UI-UX-PRO-MAX principles, and add a note indicator badge to timeline event cards next to `CategoryChip`.

**Architecture:** Update `EventSheetViewModel` & `EventRepository` to preserve original lane and preset metadata during event updates. In `EventDetailDialog`, replace cold gray card with a warm amber note card. In `EventCard`, render `NoteBadge` whenever `!event.note.isNullOrBlank()`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 46`, `versionName = "1.10.20"`
- Editing an event with unchanged start time and duration MUST preserve its original lane (`target.lane`).
- Note card in `EventDetailDialog` must be warm, clean, and elegant (warm amber / gold accents).
- `EventCard` must display a note indicator badge immediately to the right of `CategoryChip` when `event.note` is non-blank.
- 100% of unit tests in `gradlew testDebugUnitTest` must pass.

---

### Task 1: Fix Event Lane Persistence on Edit (`EventSheetViewModel.kt` & `EventRepository.kt`)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt`
- Modify/Test: `app/src/test/java/com/cruise/assistant/data/repo/EventRepositoryTest.kt` (or ViewModel test)

**Interfaces:**
- Consumes: `event.lane`, `event.presetKey`, `eventDao.getById`
- Produces: Persistent lane across edits

- [ ] **Step 1: Update `EventSheetViewModel.kt`**
- In `EventSheetState`, add `val originalLane: Int = 0` and `val presetKey: String? = null`.
- In `openForEdit(event)`: populate `originalLane = event.lane, presetKey = event.presetKey`.
- In `save()`: pass `lane = s.originalLane, presetKey = s.presetKey` when creating `CruiseEvent`.

- [ ] **Step 2: Update `EventRepository.kt`**
In `updateEvent(event, scope)`:
```kotlin
val targets = if (scope == RecurrenceScope.WHOLE_GROUP && groupId != null) {
    val finalizedDates = voyageDao.observeDayPlansOnce()
        .filter { it.finalized }
        .mapTo(mutableSetOf()) { it.date }
    eventDao.getByGroup(groupId).map { it.toDomain() }
        .filter { it.id == event.id || it.date !in finalizedDates }
} else {
    listOf(eventDao.getById(event.id)?.toDomain() ?: event)
}
```

- [ ] **Step 3: Run unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 4: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt
git commit -m "fix(event): preserve lane and metadata when editing event with notes"
```

---

### Task 2: Redesign Note Card in `EventDetailDialog.kt`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt`

**Interfaces:**
- Consumes: `event.note: String?`
- Produces: Warm Disney-styled note card in detail popup

- [ ] **Step 1: Update `EventDetailDialog.kt` note section**
Replace the cold Slate100 card with:
```kotlin
if (!event.note.isNullOrBlank()) {
    Spacer(Modifier.height(10.dp))
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFFDF5),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.StickyNote2,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFEF3C7),
                ) {
                    Text(
                        text = stringResource(R.string.note_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = Color(0xFF1E293B),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Run unit tests and verify compile**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt
git commit -m "style(ui): redesign EventDetailDialog note card with warm amber Disney notebook style"
```

---

### Task 3: Add Note Indicator Badge to Timeline Event Cards (`EventCard.kt`)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`

**Interfaces:**
- Consumes: `event.note: String?`
- Produces: `NoteBadge` rendered beside `CategoryChip` on cards with notes

- [ ] **Step 1: Add `NoteBadge` composable and wire up in `EventCard.kt`**
```kotlin
@Composable
private fun NoteBadge(colorArgb: Int, tint: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(colorArgb).copy(alpha = 0.18f),
        modifier = Modifier.padding(start = 3.dp),
    ) {
        Icon(
            Icons.Outlined.StickyNote2,
            contentDescription = "有备注",
            tint = tint,
            modifier = Modifier
                .padding(horizontal = 3.dp, vertical = 2.dp)
                .size(11.dp),
        )
    }
}
```
Add `if (!event.note.isNullOrBlank()) { NoteBadge(colorArgb, textColor) }` right next to `CategoryChip` across:
1. `compact && isFinalized`
2. `!hasWaitSegment && !compact` (standard 3-line)
3. `hasWaitSegment` 3-line and 2-line headers
4. `hasWaitSegment` 1-line header

- [ ] **Step 2: Run unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt
git commit -m "feat(ui): add note indicator badge next to CategoryChip on timeline event cards"
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

- [ ] **Step 3: Verify on emulator and capture screenshots**
- Verify show in lane 1 retains lane 1 after adding/modifying note.
- Verify timeline card displays note badge.
- Verify detail dialog displays redesigned warm note card.
- Save screenshot to `人偶效果截图对比/05_事件详情备注展示.png` and `人偶效果截图对比/01_定稿单栏_双人组合与单人点缀.png`.

- [ ] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.20 (46) and add verification screenshots"
```
