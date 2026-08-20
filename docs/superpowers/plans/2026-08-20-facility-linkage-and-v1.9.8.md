# Facility Guide Linkage, UI Unification, Dining Hours & v1.9.8 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Facilities Guide top bar shortcut, modernize top header buttons to rounded squares, unify 6 complimentary main dinner dining schedules, build Facility Guide to Deck Map auto-navigation with SVG visual pulse highlight, and bump version to v1.9.8.

**Architecture:** 
- Standardize `locations.json` operating slots for 6 rotation restaurants.
- Add `additionalDecks` / `allDecks` support to `LocationCatalogEntry` and `FacilityItemUiModel`.
- Update `TimelineScreen` top bar actions: replace Pixie Dust with Facility Guide (`Icons.Outlined.Explore`), styled with `RoundedCornerShape(10.dp)`.
- Enable direct navigation from `FacilityCard` to `DeckMapScreen` with deck and venue name parameters; inject JavaScript in WebView for bounding-box detection, glowing beacon highlight, and smooth scrolling.
- Bump version to `v1.9.8` (versionCode 24) in `build.gradle.kts`.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView, SVG DOM / JavaScript, JUnit4, Gradle.

## Global Constraints
- Target Android SDK 36, minSdk 26.
- Version: `versionCode = 24`, `versionName = "1.9.8"`.
- No broken tests; all existing and new unit tests must pass.
- Maintain existing codebase conventions and color palette (`CruiseAzure`, `CruiseMarine`, `Slate100..900`).

---

### Task 1: Unify Main Dining Schedules in `locations.json`

**Files:**
- Modify: `app/src/main/assets/locations.json`
- Modify / Test: `app/src/test/java/com/cruise/assistant/domain/LocationCatalogLoaderTest.kt`

- [ ] **Step 1: Write test for unified main dining dinner hours**

In `app/src/test/java/com/cruise/assistant/domain/LocationCatalogLoaderTest.kt`, add unit test checking the 6 main dining rooms have exact dinner hours (17:45-22:00 for the 4 17:45 venues; 18:00-22:15 for the 2 18:00 venues).

- [ ] **Step 2: Run test to observe failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.LocationCatalogLoaderTest" --no-daemon`
Expected: FAIL due to legacy split dinner slots.

- [ ] **Step 3: Update `locations.json`**

Standardize slots for the 6 venues:
- `动画师餐厅 Animator's Table Restaurant`
- `魔法盛夏餐厅 Enchanted Summer Restaurant`
- `动画师味觉餐厅 Animator's Palate`
- `好莱坞聚光灯俱乐部 Hollywood Spotlight Club`
Set dinner slot to `17:45 - 22:00` with `lateNight: true`.
- `皮克斯市场餐厅 Pixar Market Restaurant`
- `航海家俱乐部 Navigator's Club Restaurant`
Set dinner slot to `18:00 - 22:15` with `lateNight: true`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.LocationCatalogLoaderTest" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/locations.json app/src/test/java/com/cruise/assistant/domain/LocationCatalogLoaderTest.kt
git commit -m "feat: unify main dining rotation dinner hours in locations.json"
```

---

### Task 2: Multi-Deck Support in Location Catalog & Facility Guide

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/LocationCatalog.kt`
- Modify: `app/src/main/java/com/cruise/assistant/domain/LocationCatalogLoader.kt`
- Modify: `app/src/main/java/com/cruise/assistant/domain/FacilityGuide.kt`
- Modify: `app/src/main/assets/locations.json` (add multi-deck info, e.g. Walt Disney Theatre decks 6, 7; Palo Trattoria decks 10, 11)
- Test: `app/src/test/java/com/cruise/assistant/domain/FacilityGuideTest.kt`

- [ ] **Step 1: Write test for multi-deck locations and model mapping**

In `app/src/test/java/com/cruise/assistant/domain/FacilityGuideTest.kt`, add assertions that multi-deck venues expose `allDecks`, formatted location includes all decks, and `primaryDeck` resolves to the lowest/main deck.

- [ ] **Step 2: Run test to observe failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.FacilityGuideTest" --no-daemon`
Expected: FAIL with missing fields/methods.

- [ ] **Step 3: Implement multi-deck models and loader**

- In `LocationCatalogEntry`:
  ```kotlin
  data class LocationCatalogEntry(
      val deck: Int?,
      val zone: ShipZone?,
      val placeName: String,
      val diningGroup: DiningGroup? = null,
      val category: String = "meal",
      val hours: OperatingHours? = null,
      val additionalDecks: List<Int> = emptyList(),
  ) {
      val allDecks: List<Int> get() = listOfNotNull(deck) + additionalDecks
      val primaryDeck: Int? get() = allDecks.minOrNull() ?: deck
  }
  ```
- In `LocationCatalogLoader.kt`: parse optional `additionalDecks` JSON array.
- In `FacilityItemUiModel`: add `allDecks: List<Int>` and `primaryDeck: Int?`.
- Update `toFacilityItemUiModel` and `formatLocation` to format multi-deck strings nicely (e.g. `Deck 6, 7 · 船首`).

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.FacilityGuideTest" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/LocationCatalog.kt app/src/main/java/com/cruise/assistant/domain/LocationCatalogLoader.kt app/src/main/java/com/cruise/assistant/domain/FacilityGuide.kt app/src/main/assets/locations.json app/src/test/java/com/cruise/assistant/domain/FacilityGuideTest.kt
git commit -m "feat: add multi-deck support to location catalog and facility guide"
```

---

### Task 3: Header Buttons UI Unification in `TimelineScreen`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`
- Test: `app/src/test/java/com/cruise/assistant/ui/timeline/TimelineScreenTest.kt` (or UI preview verification)

- [ ] **Step 1: Replace Pixie Dust button with Facility Guide and restyle to Rounded Squares**

In `TimelineScreen.kt`:
- Replace parameter `onOpenPixieDust: () -> Unit` in top bar button row with `onOpenFacilities: () -> Unit`.
- Update top bar button group to use `Surface(shape = RoundedCornerShape(10.dp), color = Slate100, border = BorderStroke(1.dp, Slate200), modifier = Modifier.size(38.dp))`:
  1. Facility Guide (`Icons.Outlined.Explore`, tint `CruiseMarine`, tooltip "船上设施指南")
  2. Deck Map (`Icons.Outlined.Map`, tint `CruiseMarine`, tooltip "全船甲板平面图")
  3. Settings (`Icons.Outlined.Settings`, tint `Slate700`, tooltip "设置")
- Keep bottom-right FAB with `onOpenPixieDust` as the exclusive Pixie Dust entry point.
- Update `CruiseNavHost.kt` to bind `onOpenFacilities = { navController.navigate(Routes.FACILITIES) }`.

- [ ] **Step 2: Run unit tests & build check**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: replace top header button with facility guide and modernize to rounded squares"
```

---

### Task 4: Facility Guide to Deck Map Navigation & JS Highlight

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/DeckMap.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/cruise/assistant/domain/DeckMapTest.kt`

- [ ] **Step 1: Write test for DeckMap HTML wrapper highlight injection**

In `app/src/test/java/com/cruise/assistant/domain/DeckMapTest.kt`, test `inlineHtmlWrapperFor` with a target highlight keyword and verify script injection containing the target keyword.

- [ ] **Step 2: Implement JS auto-focus & glowing beacon in `DeckMap.kt`**

- In `inlineHtmlWrapperFor(svgContent: String, highlightTarget: String? = null)`:
  Add JavaScript logic to search `<text>` and `<tspan>` nodes for matching tokens from `highlightTarget`, calculate element bounds, inject a CSS animated beacon / pulse circle at the target location, and call `scrollIntoView` or `window.scrollTo`.
- Add a floating badge in `DeckMapScreen` showing "已定位至：{venueName}" with a dismiss/reset button.

- [ ] **Step 3: Add "查看甲板图 / 定位" Action in `FacilityCard`**

- In `FacilityGuideScreen.kt`:
  - Add `onNavigateToDeckMap: (Int, String) -> Unit` callback.
  - In `FacilityCard`, if `facility.primaryDeck != null`, show a prominent quick-action chip / button:
    `ElevatedAssistChip` / `Surface` with `Icons.Outlined.Map` + "查看甲板图".
  - On click, invoke `onNavigateToDeckMap(facility.primaryDeck, facility.placeName)`.

- [ ] **Step 4: Update `Routes` and `CruiseNavHost`**

- Define `Routes.deckMap(deck: Int, highlight: String? = null) = "deck_map?deck=$deck&highlight=${Uri.encode(highlight.orEmpty())}"`.
- Update `composable(Routes.DECK_MAP)` in `CruiseNavHost` to extract `deck` and `highlight` arguments and pass them to `DeckMapViewModel(initialDeck, highlightTarget)`.

- [ ] **Step 5: Run tests**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/DeckMap.kt app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapViewModel.kt app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapScreen.kt app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideScreen.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt app/src/main/res/values/strings.xml app/src/test/java/com/cruise/assistant/domain/DeckMapTest.kt
git commit -m "feat: implement facility card to deck map navigation with JS highlight and auto-scroll"
```

---

### Task 5: Bump Version to v1.9.8 & Verification on Emulator

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/res/values/strings.xml` (if needed for version or guide strings)

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**

Set `versionCode = 24`, `versionName = "1.9.8"`.

- [ ] **Step 2: Run all unit tests**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: All tests PASS.

- [ ] **Step 3: Build and install debug APK to Android Emulator**

Run: `.\gradlew.bat installDebug --no-daemon`
Expected: `CruiseAssistant-v1.9.8.apk` installed successfully.

- [ ] **Step 4: Verify on Emulator via ADB**

- Launch MainActivity.
- Verify top-right button styling (3 rounded square buttons).
- Tap the top-right Explore button to open Facilities Guide.
- Search for "华特迪士尼剧院", verify multi-deck tag (Deck 6, 7).
- Click "查看甲板图", verify transition to Deck 6 with beacon highlight centered on the theatre.
- Tap back, search for "动画师味觉餐厅", click "查看甲板图", verify transition to Deck 5 centered on Animator's Palate.
- Verify dinner hours on restaurant cards.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to v1.9.8 (versionCode 24)"
```
