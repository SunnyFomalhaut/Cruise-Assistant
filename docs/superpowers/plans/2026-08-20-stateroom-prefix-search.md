# Stateroom Prefix Search & Ranked Auto-Complete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement prefix & fuzzy search for all 2,111 staterooms on Disney Adventure with prioritized ranking (facilities at top, followed by numerically sorted staterooms), and bump version to v1.9.10.

**Architecture:**
- Create `StateroomCatalog.kt` to load and index all 2,111 staterooms from `assets/staterooms.json`.
- Update `DeckMapSearch.kt` to rank facilities matching the query or deck at the top, followed by prefix-matched staterooms sorted by numerical value (`roomNumber.toInt()`).
- Initialize `StateroomCatalog` in application startup.
- Bump version to `v1.9.10` (versionCode 26).

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4, Gradle.

## Global Constraints
- Target Android SDK 36, minSdk 26.
- Version: `versionCode = 26`, `versionName = "1.9.10"`.
- All tests must pass.

---

### Task 1: Create `StateroomCatalog.kt` & Tests

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/StateroomCatalog.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/StateroomCatalogTest.kt`
- Ensure: `app/src/main/assets/staterooms.json` exists with 2,111 rooms.

- [ ] **Step 1: Write test for `StateroomCatalog`**

In `app/src/test/java/com/cruise/assistant/domain/StateroomCatalogTest.kt`, test:
- Parsing JSON array of staterooms.
- Prefix search `112` returns sorted rooms starting with `112` (`11200`, `11201`, `11203`, `11207`... `11288`).
- Prefix search `11` returns all Deck 11 rooms sorted numerically.
- Search with non-digit string returns empty list.

- [ ] **Step 2: Run test to observe failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.StateroomCatalogTest" --no-daemon`
Expected: FAIL (unresolved reference StateroomCatalog).

- [ ] **Step 3: Implement `StateroomCatalog.kt`**

```kotlin
package com.cruise.assistant.domain

import org.json.JSONObject

data class StateroomEntry(
    val roomNumber: String,
    val deck: Int,
)

object StateroomCatalog {
    @Volatile
    private var staterooms: List<StateroomEntry> = emptyList()

    fun setStaterooms(rooms: List<StateroomEntry>) {
        staterooms = rooms.sortedWith(compareBy<StateroomEntry> { it.roomNumber.toIntOrNull() ?: 0 })
    }

    fun search(query: String, limit: Int = 100): List<StateroomEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || !trimmed.all { it.isDigit() }) return emptyList()

        val prefixMatches = staterooms.filter { it.roomNumber.startsWith(trimmed) }
        if (prefixMatches.isNotEmpty()) {
            return prefixMatches.take(limit)
        }

        return staterooms.filter { it.roomNumber.contains(trimmed) }.take(limit)
    }

    fun parseStateroomsJson(json: String): List<StateroomEntry> = runCatching {
        val arr = JSONObject(json).getJSONArray("staterooms")
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    StateroomEntry(
                        roomNumber = obj.getString("room"),
                        deck = obj.getInt("deck"),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.StateroomCatalogTest" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/StateroomCatalog.kt app/src/test/java/com/cruise/assistant/domain/StateroomCatalogTest.kt app/src/main/assets/staterooms.json
git commit -m "feat: implement StateroomCatalog indexing 2111 staterooms"
```

---

### Task 2: Update `DeckMapSearch.kt` with Ranked Results & Tests

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/DeckMapSearch.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/DeckMapSearchTest.kt`

- [ ] **Step 1: Write test for ranked search behavior**

In `app/src/test/java/com/cruise/assistant/domain/DeckMapSearchTest.kt`:
- Test that searching `11` returns facilities matching Deck 11 FIRST, followed by staterooms on Deck 11 in numerical order.
- Test that searching `112` returns all staterooms starting with `112` (`11200`, `11201`, `11203`, `11207`... `11288`).

- [ ] **Step 2: Run test to observe failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.DeckMapSearchTest" --no-daemon`
Expected: FAIL.

- [ ] **Step 3: Update `searchDeckMap` in `DeckMapSearch.kt`**

```kotlin
fun searchDeckMap(
    query: String,
    currentDeck: Int,
    facilities: List<FacilityItemUiModel> = getAllFacilityItems(),
    stateroomSearcher: (String) -> List<StateroomEntry> = StateroomCatalog::search,
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
        val popularNames = listOf("华特迪士尼剧院", "宾客服务台", "帕洛意大利餐厅", "动画师味觉餐厅", "魔法盛夏餐厅", "皮克斯市场餐厅", "童话洗衣房")
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
                            categoryKey = fac.categoryKey,
                        )
                    )
                }
            }
        }
        return quickList
    }

    val results = mutableListOf<DeckSearchResult>()
    val lowerQuery = trimmed.lowercase()

    // 1. 设施匹配（置于最前）
    facilities.filter {
        it.placeName.lowercase().contains(lowerQuery) ||
            it.formattedLocation.lowercase().contains(lowerQuery) ||
            it.categoryLabel.lowercase().contains(lowerQuery) ||
            (trimmed.all { c -> c.isDigit() } && it.allDecks.contains(trimmed.toIntOrNull()))
    }.forEach { fac ->
        fac.primaryDeck?.let { deck ->
            results.add(
                DeckSearchResult(
                    title = fac.placeName,
                    subtitle = fac.formattedLocation,
                    deckNumber = deck,
                    highlightQuery = fac.placeName,
                    type = DeckSearchResultType.FACILITY,
                    categoryKey = fac.categoryKey,
                )
            )
        }
    }

    // 2. 客房前缀与数值模糊匹配（排在设施之后，严格升序）
    if (trimmed.all { it.isDigit() }) {
        val matchedRooms = stateroomSearcher(trimmed)
        matchedRooms.forEach { room ->
            results.add(
                DeckSearchResult(
                    title = "客房 ${room.roomNumber}",
                    subtitle = "Deck ${room.deck} · 客房区域",
                    deckNumber = room.deck,
                    highlightQuery = room.roomNumber,
                    type = DeckSearchResultType.STATEROOM,
                )
            )
        }
    }

    // 3. 洗手间/厕所关键词检索
    val restroomKeywords = listOf("厕所", "洗手间", "卫生间", "restroom", "wc", "toilet", "washroom")
    if (restroomKeywords.any { lowerQuery.contains(it) }) {
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
                    subtitle = "Deck $d · 公共区域",
                    deckNumber = d,
                    highlightQuery = "Restroom",
                    type = DeckSearchResultType.RESTROOM,
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
git commit -m "feat: prioritize facility search results followed by stateroom prefix matches in numerical order"
```

---

### Task 3: Initialize `StateroomCatalog`, Bump Version to v1.9.10 & Emulator Verification

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/CruiseApp.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Load staterooms.json in `CruiseApp.kt`**

```kotlin
val stateroomJson = assets.open("staterooms.json").bufferedReader().use { it.readText() }
StateroomCatalog.setStaterooms(StateroomCatalog.parseStateroomsJson(stateroomJson))
```

- [ ] **Step 2: Bump version in `build.gradle.kts`**

Set `versionCode = 26`, `versionName = "1.9.10"`.

- [ ] **Step 3: Run all unit tests**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: All tests PASS.

- [ ] **Step 4: Build & install on emulator**

Run: `.\gradlew.bat installDebug --no-daemon`

- [ ] **Step 5: Verify on emulator**

- Search `11` -> facilities shown first, staterooms on Deck 11 listed below.
- Search `112` -> staterooms starting with 112 (`11200`, `11201`, `11203`...) listed in order.
- Tap `11216` -> switches to Deck 11, centers on room 11216 with pin `📍 11216`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/CruiseApp.kt app/build.gradle.kts
git commit -m "chore: bump version to v1.9.10 (versionCode 26) with stateroom catalog initialization"
```
