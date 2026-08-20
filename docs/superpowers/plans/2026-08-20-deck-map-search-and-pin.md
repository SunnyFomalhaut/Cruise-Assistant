# Deck Map Search & Pin Linkage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a search button below D5 on the DeckRail in DeckMapScreen, launching an in-map search sheet that finds facilities, staterooms (e.g. 11770, 6012), and restrooms, automatically switching decks, centering the item dead-center, and dropping an animated map pin, bumping version to v1.9.9.

**Architecture:**
- Create `DeckMapSearch.kt` domain layer to parse queries into facility, stateroom, and restroom search results.
- Implement `DeckMapSearchSheet.kt` (ModalBottomSheet) in UI layer.
- Add search button below D5 in `DeckRail` and connect search selection to `DeckMapViewModel.navigateToFacility(deck, query)`.
- Bump version to `v1.9.9` (versionCode 25).

**Tech Stack:** Kotlin, Jetpack Compose, Material3 BottomSheet, Android WebView, JUnit4.

## Global Constraints
- Target Android SDK 36, minSdk 26.
- Version: `versionCode = 25`, `versionName = "1.9.9"`.
- All tests must pass.

---

### Task 1: Create `DeckMapSearch.kt` Domain Model & Engine

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/DeckMapSearch.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/DeckMapSearchTest.kt`

- [ ] **Step 1: Write test for deck search engine**

In `app/src/test/java/com/cruise/assistant/domain/DeckMapSearchTest.kt`, test:
- 5-digit stateroom number `11770` -> returns Deck 11, query `11770`, type `STATEROOM`.
- 4-digit stateroom number `6012` -> returns Deck 6, query `6012`, type `STATEROOM`.
- Restroom keyword `洗手间` / `toilet` / `restroom` -> returns Restroom results on valid passenger decks.
- Facility keyword `帕洛` / `剧院` -> returns matching facility results from catalog.
- Empty query -> returns default popular facility shortcuts and current deck restroom shortcut.

- [ ] **Step 2: Run test to observe failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.DeckMapSearchTest" --no-daemon`
Expected: FAIL (unresolved reference DeckMapSearch).

- [ ] **Step 3: Implement `DeckMapSearch.kt`**

```kotlin
package com.cruise.assistant.domain

enum class DeckSearchResultType {
    FACILITY,
    STATEROOM,
    RESTROOM,
}

data class DeckSearchResult(
    val title: String,
    val subtitle: String,
    val deckNumber: Int,
    val highlightQuery: String,
    val type: DeckSearchResultType,
)

fun parseStateroomDeck(roomStr: String): Int? {
    val trimmed = roomStr.trim()
    if (!trimmed.all { it.isDigit() }) return null
    return when (trimmed.length) {
        5 -> trimmed.substring(0, 2).toIntOrNull()
        4 -> trimmed.substring(0, 1).toIntOrNull()
        else -> null
    }?.takeIf { it in DeckMapRepository.AVAILABLE_DECKS }
}

fun searchDeckMap(
    query: String,
    currentDeck: Int,
    facilities: List<FacilityItemUiModel> = getAllFacilityItems(),
): List<DeckSearchResult> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        val quickList = mutableListOf<DeckSearchResult>()
        quickList.add(
            DeckSearchResult(
                title = "洗手间 Restroom",
                subtitle = "Deck $currentDeck · 当前甲板",
                deckNumber = currentDeck,
                highlightQuery = "Restroom",
                type = DeckSearchResultType.RESTROOM,
            )
        )
        val popularNames = listOf("华特迪士尼剧院", "宾客服务台", "帕洛意大利餐厅", "动画师味觉餐厅", "魔法盛夏餐厅")
        popularNames.forEach { name ->
            facilities.firstOrNull { it.placeName.contains(name) }?.let { fac ->
                fac.primaryDeck?.let { deck ->
                    quickList.add(
                        DeckSearchResult(
                            title = fac.placeName,
                            subtitle = fac.formattedLocation,
                            deckNumber = deck,
                            highlightQuery = fac.placeName,
                            type = DeckSearchResultType.FACILITY,
                        )
                    )
                }
            }
        }
        return quickList
    }

    val results = mutableListOf<DeckSearchResult>()

    // 1. 房间号识别
    parseStateroomDeck(trimmed)?.let { deck ->
        results.add(
            DeckSearchResult(
                title = "客房 $trimmed",
                subtitle = "Deck $deck 客房区域",
                deckNumber = deck,
                highlightQuery = trimmed,
                type = DeckSearchResultType.STATEROOM,
            )
        )
    }

    // 2. 洗手间识别
    val restroomKeywords = listOf("厕所", "洗手间", "卫生间", "restroom", "wc", "toilet")
    if (restroomKeywords.any { trimmed.lowercase().contains(it) }) {
        results.add(
            DeckSearchResult(
                title = "洗手间 Restroom",
                subtitle = "Deck $currentDeck · 当前甲板",
                deckNumber = currentDeck,
                highlightQuery = "Restroom",
                type = DeckSearchResultType.RESTROOM,
            )
        )
        listOf(5, 6, 7, 8, 10, 11, 17, 18, 19).filter { it != currentDeck }.forEach { d ->
            results.add(
                DeckSearchResult(
                    title = "洗手间 Restroom",
                    subtitle = "Deck $d",
                    deckNumber = d,
                    highlightQuery = "Restroom",
                    type = DeckSearchResultType.RESTROOM,
                )
            )
        }
    }

    // 3. 设施匹配
    val lowerQuery = trimmed.lowercase()
    facilities.filter {
        it.placeName.lowercase().contains(lowerQuery) ||
            it.formattedLocation.lowercase().contains(lowerQuery) ||
            it.categoryLabel.lowercase().contains(lowerQuery)
    }.forEach { fac ->
        fac.primaryDeck?.let { deck ->
            results.add(
                DeckSearchResult(
                    title = fac.placeName,
                    subtitle = fac.formattedLocation,
                    deckNumber = deck,
                    highlightQuery = fac.placeName,
                    type = DeckSearchResultType.FACILITY,
                )
            )
        }
    }

    return results
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.DeckMapSearchTest" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/DeckMapSearch.kt app/src/test/java/com/cruise/assistant/domain/DeckMapSearchTest.kt
git commit -m "feat: add deck map search domain engine supporting staterooms, restrooms, and facilities"
```

---

### Task 2: Build `DeckMapSearchSheet.kt` UI Component

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapSearchSheet.kt`

- [ ] **Step 1: Create `DeckMapSearchSheet` Composable**

ModalBottomSheet containing:
- Search `OutlinedTextField` with leading search icon and trailing clear icon.
- `LazyColumn` showing results list with custom icon by type (🚪 for stateroom, 🚻 for restroom, category icon for facility).
- On click, triggers callback `onSelectResult(deckNumber: Int, query: String)`.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapSearchSheet.kt
git commit -m "feat: implement DeckMapSearchSheet UI modal bottom sheet"
```

---

### Task 3: Add Search Button in `DeckRail` and Connect in `DeckMapScreen`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapViewModel.kt`

- [ ] **Step 1: Add Search Button to `DeckRail` below D5**

In `DeckRail`, add search button at the bottom:
```kotlin
Surface(
    shape = RoundedCornerShape(10.dp),
    color = Slate100,
    border = BorderStroke(1.dp, Slate200),
    modifier = Modifier
        .size(48.dp, 40.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable { onOpenSearch() },
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = "搜索", tint = CruiseMarine, modifier = Modifier.size(17.dp))
        Text(text = "搜索", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CruiseMarine)
    }
}
```

- [ ] **Step 2: Add `navigateToFacility(deck, highlight)` to `DeckMapViewModel`**

```kotlin
fun navigateToFacility(deckNumber: Int, highlight: String) {
    _selectedDeck.value = deckNumber
    _highlightTarget.value = highlight
}
```

- [ ] **Step 3: Connect `DeckMapSearchSheet` in `DeckMapScreen`**

- State: `var showSearchSheet by remember { mutableStateOf(false) }`.
- When search result selected: call `onSelectFacility(deck, query)` and close sheet.

- [ ] **Step 4: Run unit tests**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapScreen.kt app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapViewModel.kt
git commit -m "feat: integrate search button in DeckRail and wire DeckMapSearchSheet"
```

---

### Task 4: Bump Version to v1.9.9 & Verify on Emulator

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**

Set `versionCode = 25`, `versionName = "1.9.9"`.

- [ ] **Step 2: Run all unit tests**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: PASS.

- [ ] **Step 3: Build & Install on Emulator**

Run: `.\gradlew.bat installDebug --no-daemon`
Expected: `CruiseAssistant-v1.9.9.apk` installed.

- [ ] **Step 4: Verify on Emulator**

- Open Deck Map.
- Tap the search button below D5.
- Search `11770` -> Tap result -> Map switches to Deck 11, centers on room 11770 with pin.
- Tap search again, search `洗手间` -> Tap Deck 6 result -> Map switches to Deck 6, centers on Restroom with pin.
- Tap search again, search `帕洛` -> Tap result -> Map switches to Deck 10, centers on Palo with pin.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to v1.9.9 (versionCode 25)"
```
