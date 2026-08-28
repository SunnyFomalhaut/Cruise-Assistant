# Meal Period Selection & Deck Map Location Matching Fixes v1.10.22 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix dining events locked to "Dinner" (晚餐) bug in EventSheetViewModel, and fix Navigator's Club and preset location map centering in DeckMap.kt.

**Architecture:** In `EventSheetViewModel.kt`, properly resolve `selectedMealPeriod` on edit, sync `title` when switching meal periods, and manage `presetKey` lifecycle. In `DeckMap.kt`, expand Chinese location targets via `LocationCatalog` and normalize quotes/aliases in deck SVG text search.

**Tech Stack:** Kotlin, JavaScript, Jetpack Compose, JSON, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 48`, `versionName = "1.10.22"`
- Meal period switching: Tapping a meal chip (`[早餐]`, `[午餐]`, `[晚餐]`, `[夜宵]`) must update default title and time without reverting to dinner.
- Deck map matching: All 74 preset venues in `locations.json` (including Navigator's Club / 航海家俱乐部) must successfully highlight on their respective deck SVGs.
- 100% of unit tests in `gradlew testDebugUnitTest` must pass.

---

### Task 1: Fix Meal Period Selection & Synchronization in `EventSheetViewModel.kt`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`
- Modify/Test: `app/src/test/java/com/cruise/assistant/ui/event/EventSheetStateTest.kt`

**Interfaces:**
- Consumes: `MealPeriod`, `event.title`, `event.startTime`, `event.presetKey`
- Produces: Correct `selectedMealPeriod`, updated `title` when switching meal chips, and clean `presetKey`

- [x] **Step 1: Update `EventSheetViewModel.kt`**
1. Add `val presetKey: String? = null` to `EventSheetState`.
2. In `openForEdit(event)`:
   - `presetKey = event.presetKey`
   - `val resolvedMealPeriod = MealPeriod.entries.firstOrNull { it.label == event.title } ?: when { event.startTime.isBefore(LocalTime.of(10, 30)) -> MealPeriod.BREAKFAST; event.startTime.isBefore(LocalTime.of(15, 0)) -> MealPeriod.LUNCH; event.startTime.isBefore(LocalTime.of(21, 30)) -> MealPeriod.DINNER; else -> MealPeriod.LATE_NIGHT }`
   - Set `selectedMealPeriod = if (iconKey == "meal" || event.presetKey == "dinner") resolvedMealPeriod else null`.
3. In `onMealPeriodSelect(period)`:
   - `val isDefaultMealTitle = it.title.isBlank() || MealPeriod.entries.any { p -> p.label == it.title }`
   - `val newTitle = if (isDefaultMealTitle && period != null) period.label else it.title`
   - Update `title = newTitle`, `selectedMealPeriod = period`.
4. In `save()`:
   - `val newPresetKey = if (s.categoryIconKey == "meal") { if (s.selectedMealPeriod == MealPeriod.DINNER) "dinner" else null } else s.presetKey`
   - Pass `presetKey = newPresetKey`.

- [x] **Step 2: Run and add unit tests**
Add test in `EventSheetStateTest.kt` verifying meal period switching updates title and clears dinner presetKey.
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [x] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt app/src/test/java/com/cruise/assistant/ui/event/EventSheetStateTest.kt
git commit -m "fix(event): fix meal period resolution, title sync and presetKey management in EventSheet"
```

---

### Task 2: Fix Navigator's Club & Preset Location Map Matching in `DeckMap.kt` & `locations.json`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/DeckMap.kt`
- Modify: `app/src/main/assets/locations.json`

**Interfaces:**
- Consumes: `LocationCatalog`, `highlightTarget`, `deck_*.svg`
- Produces: 100% successful location pin placement and centering on deck maps

- [x] **Step 1: Update `DeckMap.kt`**
1. In Kotlin `inlineHtmlWrapperFor(svgContent, highlightTarget)`:
   Expand `highlightTarget` by looking up in `LocationCatalog.allEntries()` to match Chinese names to full bilingual venue names.
2. In JavaScript `centerFacilityTarget()`:
   - Normalize quotes: `.replace(/[\u2018\u2019\u201B\u2032\u0027\']/g, '')`
   - Normalize spaces and punctuation.
   - Add Chinese-to-English keyword expansions (`航海家 -> navigator`, `盛宴 -> enchanted summer`, `动画师 -> animator`, etc.).
   - Search normalized text nodes.

- [x] **Step 2: Update `locations.json`**
Change Deck 17 `露天泳池` to `阳光泳池 Sunnyside Pool`.

- [x] **Step 3: Run verification script**
Run: `python scripts/test_location_matching.py`
Expected: 74/74 venues matched (0 unmatched).

- [x] **Step 4: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/domain/DeckMap.kt app/src/main/assets/locations.json
git commit -m "fix(map): support quote-insensitive and bilingual location matching for deck maps"
```

---

### Task 3: Version Bump, Build & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [x] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 48`, `versionName = "1.10.22"`.

- [x] **Step 2: Build and install APK**
Run `assembleDebug` and install to running emulator.

- [x] **Step 3: Test on emulator**
- Edit a dinner event -> switch meal chip to "早餐" -> confirm title updates to "早餐" and time to 08:00, save and verify it stays "早餐".
- Click "定位" on Navigator's Club (航海家俱乐部) -> confirm Deck 6 opens and centers/highlights "Navigator’s Club Restaurant".
- Capture verification screenshot.

- [x] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.22 (48) and add verification screenshots"
```
