# Pixie Dust Map Linkage, Direct Entry & Real-time Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the Pixie Dust feature into a sweep-delivery workflow by eliminating the deck dropdown in favor of direct 4-5 digit room entry with real-time room validation against StateroomCatalog, automatically sorting records by deck and room for hallway delivery, redesigning the linkage view into a deck-grouped sheet, and highlighting rooms on the deck map with a dynamic tri-color overlay (unreplied, supplement needed, completed).

**Architecture:** The domain layer parses 4-5 digit room numbers and validates them in real time against the existing 2,111 stateroom index in `StateroomCatalog`. A sweep sorting algorithm arranges records by Deck ascending then Room number ascending. The Linkage view is upgraded from a basic dialog to a deck-grouped `ModalBottomSheet` with per-deck summary badges and map jump triggers. The deck map SVG renderer injects dynamic CSS/JS to display red, yellow, and green pulsing highlights and badges for all relevant rooms on the selected deck.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android Room, StateFlow/Coroutines, Android WebView (SVG DOM script injection), JUnit 4.

## Global Constraints

- Android targetSdk 36, minSdk 26.
- Version bump target: versionCode 51, versionName "1.11.0".
- Strict backward compatibility with `PixieDustEntity` schema (`deck: Int, room: String`).
- No breaking changes to existing `Routes.deckMap` callers.

---

### Task 1: Stateroom Input Parsing & Real-Time Validation Domain Logic

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/StateroomValidation.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/StateroomInputValidationTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class StateroomValidationStatus { INCOMPLETE, VALID, INVALID }

  data class StateroomValidationResult(
      val status: StateroomValidationStatus,
      val deck: Int? = null,
      val room: String? = null,
      val fullRoom: String? = null,
      val message: String? = null,
  )

  fun parseStateroomInput(input: String): Pair<Int, String>?
  fun validateStateroomInput(
      input: String,
      catalog: List<StateroomEntry> = StateroomCatalog.getAllStaterooms(),
  ): StateroomValidationResult
  ```

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/cruise/assistant/domain/StateroomInputValidationTest.kt`:

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StateroomInputValidationTest {

    @Before
    fun setup() {
        StateroomCatalog.setStaterooms(
            listOf(
                StateroomEntry(roomNumber = "6012", deck = 6),
                StateroomEntry(roomNumber = "11216", deck = 11),
                StateroomEntry(roomNumber = "11200", deck = 11),
                StateroomEntry(roomNumber = "16802", deck = 16),
            )
        )
    }

    @Test
    fun testParseStateroomInput() {
        assertEquals(Pair(6, "012"), parseStateroomInput("6012"))
        assertEquals(Pair(11, "216"), parseStateroomInput("11216"))
        assertEquals(Pair(16, "802"), parseStateroomInput("16802"))
        assertNull(parseStateroomInput("123"))
        assertNull(parseStateroomInput("123456"))
        assertNull(parseStateroomInput("abcd"))
        assertNull(parseStateroomInput("14100")) // Deck 14 doesn't exist
    }

    @Test
    fun testValidateStateroomInput_Incomplete() {
        val result1 = validateStateroomInput("")
        assertEquals(StateroomValidationStatus.INCOMPLETE, result1.status)

        val result2 = validateStateroomInput("112")
        assertEquals(StateroomValidationStatus.INCOMPLETE, result2.status)
    }

    @Test
    fun testValidateStateroomInput_Valid() {
        val result = validateStateroomInput("11216")
        assertEquals(StateroomValidationStatus.VALID, result.status)
        assertEquals(11, result.deck)
        assertEquals("216", result.room)
        assertEquals("11216", result.fullRoom)
    }

    @Test
    fun testValidateStateroomInput_Invalid() {
        val result = validateStateroomInput("11999")
        assertEquals(StateroomValidationStatus.INVALID, result.status)
        assertEquals("地图上无此房间，请核对", result.message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.StateroomInputValidationTest"`
Expected: Compilation failure or FAIL (unresolved symbols `parseStateroomInput`, `validateStateroomInput`).

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/cruise/assistant/domain/StateroomValidation.kt`:

```kotlin
package com.cruise.assistant.domain

enum class StateroomValidationStatus {
    INCOMPLETE,
    VALID,
    INVALID,
}

data class StateroomValidationResult(
    val status: StateroomValidationStatus,
    val deck: Int? = null,
    val room: String? = null,
    val fullRoom: String? = null,
    val message: String? = null,
)

/**
 * 将 4 位 (6012 -> 6, "012") 或 5 位 (11216 -> 11, "216") 房号解析为 Deck 与 Room
 */
fun parseStateroomInput(input: String): Pair<Int, String>? {
    val trimmed = input.trim()
    if (!trimmed.all { it.isDigit() }) return null
    val (deck, room) = when (trimmed.length) {
        4 -> {
            val d = trimmed.substring(0, 1).toIntOrNull() ?: return null
            val r = trimmed.substring(1)
            d to r
        }
        5 -> {
            val d = trimmed.substring(0, 2).toIntOrNull() ?: return null
            val r = trimmed.substring(2)
            d to r
        }
        else -> return null
    }
    if (deck !in DeckMapRepository.AVAILABLE_DECKS) return null
    return deck to room
}

/**
 * 实时校验房号是否存在于客房全量目录中
 */
fun validateStateroomInput(
    input: String,
    catalog: List<StateroomEntry> = StateroomCatalog.getAllStaterooms(),
): StateroomValidationResult {
    val trimmed = input.trim()
    if (trimmed.length < 4) {
        return StateroomValidationResult(
            status = StateroomValidationStatus.INCOMPLETE,
            message = "请输入 4 或 5 位客房号",
        )
    }
    val parsed = parseStateroomInput(trimmed)
    if (parsed == null) {
        return StateroomValidationResult(
            status = StateroomValidationStatus.INVALID,
            message = "地图上无此房间，请核对",
        )
    }
    val (deck, room) = parsed
    val full = "$deck$room"
    val exists = catalog.any { it.roomNumber == full && it.deck == deck }
    return if (exists) {
        StateroomValidationResult(
            status = StateroomValidationStatus.VALID,
            deck = deck,
            room = room,
            fullRoom = full,
            message = "Deck $deck 客房有效",
        )
    } else {
        StateroomValidationResult(
            status = StateroomValidationStatus.INVALID,
            deck = deck,
            room = room,
            fullRoom = full,
            message = "地图上无此房间，请核对",
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.StateroomInputValidationTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/StateroomValidation.kt app/src/test/java/com/cruise/assistant/domain/StateroomInputValidationTest.kt
git commit -m "feat: add stateroom input parsing and real-time validation domain logic"
```

---

### Task 2: PixieDustKeypad Update (Direct 4-5 Digit Entry & Real-Time Validation UI)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustKeypad.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `validateStateroomInput` from `StateroomValidation.kt`
- Updates `PixieDustUiState`:
  ```kotlin
  val validationResult: StateroomValidationResult = StateroomValidationResult(StateroomValidationStatus.INCOMPLETE)
  ```

- [ ] **Step 1: Add new string resources**

In `app/src/main/res/values/strings.xml`, add:
```xml
<string name="pixie_room_valid_format">✓ Deck %1$d 客房有效</string>
<string name="pixie_room_invalid">⚠️ 地图上无此房间，请核对</string>
<string name="pixie_room_hint_direct">输入 4 或 5 位房号</string>
```

- [ ] **Step 2: Update PixieDustViewModel.kt**

1. In `PixieDustUiState`, add `val validation: StateroomValidationResult = StateroomValidationResult(StateroomValidationStatus.INCOMPLETE)`.
2. In `PixieDustViewModel`, update `roomInput.value.length < 5` (allow up to 5 digits).
3. In `onDigit`:
   ```kotlin
   fun onDigit(d: Char) {
       if (!d.isDigit() || roomInput.value.length >= 5) return
       roomInput.value += d
       val validation = validateStateroomInput(roomInput.value)
       if (validation.status == StateroomValidationStatus.VALID) {
           val s = state.value ?: return
           lastLevelForRoom(s.given, validation.deck!!, validation.room!!)
               ?.takeIf { last -> s.levels.any { it.id == last.levelId } }
               ?.let { selectedLevelId.value = it.levelId }
       }
   }
   ```
4. In `saveRecord()`:
   Use `validateStateroomInput(s.roomInput)`:
   ```kotlin
   val validation = validateStateroomInput(s.roomInput)
   if (validation.status != StateroomValidationStatus.VALID || s.selectedLevelId == null) return
   val level = s.levelById[s.selectedLevelId] ?: return
   val record = PixieDustRecord(
       direction = if (s.tab == PixieTab.GIVEN) PixieDustDirection.GIVEN else PixieDustDirection.RECEIVED,
       deck = validation.deck!!,
       room = validation.room!!,
       levelId = level.id,
       levelNameSnapshot = level.name,
       replied = false,
       note = "",
       timestamp = nowMs(),
   )
   ```

- [ ] **Step 3: Update PixieDustKeypad.kt**

1. Remove `DeckDropdown` row from `PixieDustKeypad`.
2. Display big room number:
   ```kotlin
   Text(
       text = state.roomInput.ifEmpty { stringResource(R.string.pixie_room_hint_direct) },
       fontSize = 30.sp,
       fontWeight = FontWeight.Bold,
       letterSpacing = 3.sp,
       color = if (state.roomInput.isEmpty()) Slate400 else Slate900,
   )
   ```
3. Show validation badge/warning under room number:
   ```kotlin
   when (state.validation.status) {
       StateroomValidationStatus.VALID -> {
           Text(
               text = stringResource(R.string.pixie_room_valid_format, state.validation.deck ?: 0),
               style = MaterialTheme.typography.labelSmall,
               color = Color(0xFF16A34A),
               fontWeight = FontWeight.Bold,
           )
       }
       StateroomValidationStatus.INVALID -> {
           Text(
               text = stringResource(R.string.pixie_room_invalid),
               style = MaterialTheme.typography.labelSmall,
               color = MaterialTheme.colorScheme.error,
               fontWeight = FontWeight.Bold,
           )
       }
       StateroomValidationStatus.INCOMPLETE -> {
           Text(
               text = stringResource(R.string.pixie_room_hint_direct),
               style = MaterialTheme.typography.labelSmall,
               color = Slate400,
           )
       }
   }
   ```
4. Update `save` button `enabled` condition to `state.validation.status == StateroomValidationStatus.VALID && state.selectedLevelId != null`.

- [ ] **Step 4: Run unit tests to verify no compilation issues**

Run: `cmd /c gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustKeypad.kt app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt app/src/main/res/values/strings.xml
git commit -m "feat: upgrade pixie dust keypad with direct entry and live room validation"
```

---

### Task 3: Sweep Delivery Sorting & Status Filtering

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PixieDust.kt`
- Modify: `app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/PixieDustSweepSortingTest.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustLists.kt`

**Interfaces:**
- Produces:
  ```kotlin
  fun sortForSweepDelivery(
      records: List<PixieDustRecord>,
      levels: List<PixieLevel>,
  ): List<PixieDustRecord>

  fun pairByRoomForSweep(
      given: List<PixieDustRecord>,
      received: List<PixieDustRecord>,
      levelRankOf: (PixieDustRecord) -> Int = { 0 },
  ): List<RoomPair>
  ```

- [ ] **Step 1: Write failing unit test for sweep delivery sorting**

Create `app/src/test/java/com/cruise/assistant/domain/PixieDustSweepSortingTest.kt`:

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PixieDustSweepSortingTest {

    private val levelStandard = PixieLevel(id = 1, name = "普通", colorArgb = 0, iconKey = "gift", rank = 1)
    private val levelDeluxe = PixieLevel(id = 2, name = "豪华", colorArgb = 0, iconKey = "gift", rank = 2)

    @Test
    fun testSortForSweepDelivery_SortedByDeckAndRoomAscending() {
        val r1 = PixieDustRecord(id = 1, direction = PixieDustDirection.RECEIVED, deck = 11, room = "216", levelId = 1, levelNameSnapshot = "普通", replied = false, timestamp = 100)
        val r2 = PixieDustRecord(id = 2, direction = PixieDustDirection.RECEIVED, deck = 6, room = "012", levelId = 1, levelNameSnapshot = "普通", replied = false, timestamp = 200)
        val r3 = PixieDustRecord(id = 3, direction = PixieDustDirection.RECEIVED, deck = 11, room = "200", levelId = 2, levelNameSnapshot = "豪华", replied = false, timestamp = 300)
        val r4 = PixieDustRecord(id = 4, direction = PixieDustDirection.RECEIVED, deck = 11, room = "200", levelId = 1, levelNameSnapshot = "普通", replied = true, timestamp = 400)

        val sorted = sortForSweepDelivery(listOf(r1, r2, r3, r4), listOf(levelStandard, levelDeluxe))

        // Order should be:
        // 1. Deck 6, Room 012
        // 2. Deck 11, Room 200 (unreplied)
        // 3. Deck 11, Room 200 (replied)
        // 4. Deck 11, Room 216
        assertEquals(listOf(2L, 3L, 4L, 1L), sorted.map { it.id })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustSweepSortingTest"`
Expected: FAIL (unresolved symbol `sortForSweepDelivery`).

- [ ] **Step 3: Implement sortForSweepDelivery and pairByRoomForSweep**

In `app/src/main/java/com/cruise/assistant/domain/PixieDust.kt`:

```kotlin
/**
 * 扫楼回礼排序：优先按 Deck 升序、同层按 Room 数字升序；同房间内未回礼在先（高等级优先）、已回礼沉底
 */
fun sortForSweepDelivery(
    records: List<PixieDustRecord>,
    levels: List<PixieLevel>,
): List<PixieDustRecord> {
    val rank = levels.associate { it.id to it.rank }
    return records
        .sortedWith(
            compareBy<PixieDustRecord> { it.deck }
                .thenBy { it.room.toIntOrNull() ?: 0 }
                .thenBy { if (it.replied) 1 else 0 }
                .thenByDescending { rank[it.levelId] ?: 0 }
                .thenByDescending { it.timestamp }
        )
}
```

In `app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt`:

```kotlin
/**
 * 按楼层和房号顺号配对，方便按楼层扫楼巡查
 */
fun pairByRoomForSweep(
    given: List<PixieDustRecord>,
    received: List<PixieDustRecord>,
    levelRankOf: (PixieDustRecord) -> Int = { 0 },
): List<RoomPair> {
    val rooms = (given.map { it.deck to it.room } + received.map { it.deck to it.room }).toSet()
    return rooms.map { (deck, room) ->
        val g = given.filter { it.deck == deck && it.room == room }
        val r = received.filter { it.deck == deck && it.room == room }
        RoomPair(
            deck = deck,
            room = room,
            given = g,
            received = r,
            unrepliedReceived = r.filter { !it.replied },
            lastInteractTs = (g.map { it.timestamp } + r.map { it.timestamp }).maxOrNull() ?: 0L,
        )
    }.sortedWith(
        compareBy<RoomPair> { it.deck }
            .thenBy { it.room.toIntOrNull() ?: 0 }
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustSweepSortingTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 5: Add filter toggle to ViewModel and Lists UI**

1. In `PixieDustViewModel.kt`:
   - Add filter state: `enum class PixieFilter { ALL, PENDING_ONLY }`.
   - In `PixieDustUiState`, add `val filter: PixieFilter = PixieFilter.ALL`.
   - In state combiner, apply `sortForSweepDelivery` and filter when `PENDING_ONLY`.
2. In `PixieDustLists.kt`:
   - Add filter chips `全部` / `🔴 待回与需补` above the list.

- [ ] **Step 6: Run full test suite**

Run: `cmd /c gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PixieDust.kt app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt app/src/test/java/com/cruise/assistant/domain/PixieDustSweepSortingTest.kt app/src/main/java/com/cruise/assistant/ui/pixiedust/
git commit -m "feat: implement sweep delivery sorting and status filter for pixie dust"
```

---

### Task 4: Linkage View Redesign (Deck Grouped BottomSheet & Map Triggers)

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewSheet.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`
- Delete: `app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewDialog.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable
  fun LinkageViewSheet(
      state: PixieDustUiState,
      onNavigateToDeckMap: (deck: Int) -> Unit,
      onDismiss: () -> Unit,
  )
  ```

- [ ] **Step 1: Implement LinkageViewSheet.kt**

Create `app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewSheet.kt`:
- Use `ModalBottomSheet` with rounded corners.
- Top section: Overall stats summary cards (Total Rooms, 🔴 Unreplied, 🟡 Need Supplement, 🟢 Completed).
- Body: `LazyColumn` grouped by Deck (`state.roomPairs.groupBy { it.deck }`):
  - Card for each deck:
    - Header: `Deck {deck}` badge, status counters (`待回 {u} · 需补 {s} · 已回 {c}`).
    - Action: Button `🗺️ 查看本层地图` calling `onNavigateToDeckMap(deck)`.
    - Inner list of rooms on this deck:
      - Three-color status badge:
        - 🔴 `待回礼` if `pair.unrepliedReceived.isNotEmpty()`.
        - 🟡 `需补礼` if `pair.received.size > pair.given.size`.
        - 🟢 `已回清` otherwise.
      - Level summary comparison `发出 X ↔ 收到 Y`.

- [ ] **Step 2: Update CruiseNavHost.kt**

Replace `LinkageViewDialog` invocation with `LinkageViewSheet`:
```kotlin
if (showLinkage) {
    pixieState?.let { s ->
        LinkageViewSheet(
            state = s,
            onNavigateToDeckMap = { targetDeck ->
                showLinkage = false
                navController.navigate(Routes.deckMap(deck = targetDeck, pixieMode = true))
            },
            onDismiss = { showLinkage = false },
        )
    }
}
```

- [ ] **Step 3: Delete obsolete LinkageViewDialog.kt**

Remove `app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewDialog.kt`.

- [ ] **Step 4: Run unit tests to verify no compilation errors**

Run: `cmd /c gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewSheet.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git rm app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewDialog.kt
git commit -m "feat: redesign linkage view with deck grouping and map linkage"
```

---

### Task 5: DeckMap Multi-Room Tri-Color SVG Overlay Engine

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/DeckMap.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/DeckMapPixieOverlayTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class PixieRoomStatus {
      UNREPLIED,
      NEED_SUPPLEMENT,
      COMPLETED,
  }

  data class PixieRoomMarker(
      val roomNumber: String,
      val status: PixieRoomStatus,
      val unrepliedCount: Int = 0,
      val givenCount: Int = 0,
      val receivedCount: Int = 0,
  )

  fun inlineHtmlWrapperFor(
      svgContent: String,
      highlightTarget: String? = null,
      pixieMarkers: List<PixieRoomMarker> = emptyList(),
  ): String
  ```

- [ ] **Step 1: Write failing unit test for multi-room pixie overlay injection**

Create `app/src/test/java/com/cruise/assistant/domain/DeckMapPixieOverlayTest.kt`:

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class DeckMapPixieOverlayTest {

    @Test
    fun testInlineHtmlWrapperFor_InjectsPixieMarkers() {
        val svg = """<svg><rect id="room-11216" /><text id="number-11216">11216</text></svg>"""
        val markers = listOf(
            PixieRoomMarker(roomNumber = "11216", status = PixieRoomStatus.UNREPLIED, unrepliedCount = 1),
            PixieRoomMarker(roomNumber = "11200", status = PixieRoomStatus.NEED_SUPPLEMENT, givenCount = 1, receivedCount = 2),
            PixieRoomMarker(roomNumber = "11220", status = PixieRoomStatus.COMPLETED, givenCount = 1, receivedCount = 1),
        )

        val html = DeckMapRepository.inlineHtmlWrapperFor(svg, null, markers)

        assertTrue("Should contain pixie-room-marker styles", html.contains(".pixie-marker-unreplied"))
        assertTrue("Should contain pixie-marker-supplement styles", html.contains(".pixie-marker-supplement"))
        assertTrue("Should contain pixie-marker-completed styles", html.contains(".pixie-marker-completed"))
        assertTrue("Should inject room 11216 marker JS", html.contains("11216"))
        assertTrue("Should inject room 11200 marker JS", html.contains("11200"))
        assertTrue("Should inject room 11220 marker JS", html.contains("11220"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.DeckMapPixieOverlayTest"`
Expected: FAIL (unresolved symbol `PixieRoomMarker` or `inlineHtmlWrapperFor` signature mismatch).

- [ ] **Step 3: Implement multi-room tri-color SVG overlay injection in DeckMap.kt**

1. Define `PixieRoomStatus` and `PixieRoomMarker` in `app/src/main/java/com/cruise/assistant/domain/DeckMap.kt`.
2. In `DeckMapRepository.inlineHtmlWrapperFor(...)`:
   - Add parameter `pixieMarkers: List<PixieRoomMarker> = emptyList()`.
   - When `pixieMarkers.isNotEmpty()`, inject CSS:
     - `.pixie-marker-unreplied`: Red `#EF4444`, breathing pulse box-shadow, red pin badge.
     - `.pixie-marker-supplement`: Amber `#F59E0B`, breathing pulse box-shadow, amber pin badge.
     - `.pixie-marker-completed`: Emerald `#10B981`, solid highlight box, emerald pin badge.
   - Inject JS `renderPixieMarkers()`:
     - Iterates over markers JSON.
     - Locates room elements: `document.getElementById('room-' + m.roomNumber)` or `outline-` or `number-`.
     - Injects styled overlay boxes and pins on each room element.
     - Calculates bounding box of all markers and scrolls view into center.

- [ ] **Step 4: Run test to verify it passes**

Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.DeckMapPixieOverlayTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/DeckMap.kt app/src/test/java/com/cruise/assistant/domain/DeckMapPixieOverlayTest.kt
git commit -m "feat: implement multi-room tri-color SVG overlay engine for deck map"
```

---

### Task 6: DeckMap Screen/ViewModel Linkage Route Integration & Top Banner

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapScreen.kt`

**Interfaces:**
- Consumes: `PixieDustRepository`, `PixieRoomMarker`, `PixieRoomStatus`
- Updates `Routes.deckMap`:
  ```kotlin
  fun deckMap(deck: Int? = null, highlight: String? = null, pixieMode: Boolean = false): String
  ```

- [ ] **Step 1: Update Routes.kt in CruiseNavHost.kt**

Add `pixieMode: Boolean = false` to `Routes.deckMap` and update route pattern `deck_map?deck={deck}&highlight={highlight}&pixie={pixie}`.

- [ ] **Step 2: Update DeckMapViewModel.kt**

1. Inject `pixieDustRepository: PixieDustRepository? = null`.
2. Add `pixieMode: Boolean = false` state.
3. Compute `pixieMarkers` for current `_selectedDeck`:
   - Filter records for `deck == _selectedDeck`.
   - Calculate for each room:
     - If `unrepliedReceived > 0` -> `UNREPLIED`
     - Else if `received.size > given.size` -> `NEED_SUPPLEMENT`
     - Else -> `COMPLETED`
4. Expose `pixieMarkers` in `DeckMapUiState`.

- [ ] **Step 3: Update DeckMapScreen.kt**

1. Pass `state.pixieMarkers` to `DeckMapRepository.inlineHtmlWrapperFor(svg, state.highlightTarget, state.pixieMarkers)`.
2. If `state.pixieMarkers.isNotEmpty()`, render floating banner at the top of the map:
   - `Deck {deck} 仙尘回礼 · 🔴待回 {u} · 🟡需补 {s} · 🟢已完成 {c}`.
   - Button `返回仙尘` calling `onBack()`.

- [ ] **Step 4: Run full test suite**

Run: `cmd /c gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt app/src/main/java/com/cruise/assistant/ui/deckmap/
git commit -m "feat: integrate pixie dust deck map linkage route, markers, and floating banner"
```

---

### Task 7: Version Bump to v1.11.0 & Full Verification

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Bump version in app/build.gradle.kts**

Update:
- `versionCode = 51`
- `versionName = "1.11.0"`

- [ ] **Step 2: Run full unit test suite**

Run: `cmd /c gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to v1.11.0 (51)"
```
