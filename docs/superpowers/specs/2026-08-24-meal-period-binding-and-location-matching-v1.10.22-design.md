# Design Spec: Meal Period Selection & Deck Map Location Matching Fixes v1.10.22

## 1. Problem Statements & Root Causes

### 1.1 Bug 1: Dining Events Locked to "Dinner" (晚餐)
- **Root Cause**:
  1. `EventSheetState.selectedMealPeriod` defaulted to `MealPeriod.DINNER`.
  2. When opening an existing event for edit in `EventSheetViewModel.init`, `selectedMealPeriod` was never populated from the event's title or start time.
  3. When selecting a meal period chip (`onMealPeriodSelect`), `state.title` was not updated if it was previously "晚餐" or another meal period.
  4. When saving, `presetKey` retained `"dinner"` even if changed to breakfast or lunch, causing future dinner-setting updates to overwrite its time.
- **Solution**:
  - In `openForEdit(event)`: accurately resolve `selectedMealPeriod` from `event.title` or `event.startTime`.
  - In `onMealPeriodSelect(period)`: update `state.title` if the title is blank or an existing meal label (e.g. switching "晚餐" to "早餐").
  - In `save()`: set `presetKey = if (s.selectedMealPeriod == MealPeriod.DINNER) "dinner" else null` for dining events.

### 1.2 Bug 2: Navigator's Club & Preset Location Map Matching
- **Root Cause**:
  1. `deck_6.svg` uses a curly apostrophe (`Navigator’s Club`), whereas `locations.json` uses a straight apostrophe (`Navigator's Club`).
  2. The deck map JavaScript matching split target words by whitespace/commas without normalizing quotes/punctuation.
  3. Targets passed in Chinese only (e.g. `航海家俱乐部`) had no English keyword mapping in JavaScript.
- **Solution**:
  - In `DeckMap.kt`:
    - In Kotlin, resolve `highlightTarget` against `LocationCatalog` to expand Chinese-only targets to bilingual names.
    - In JavaScript, normalize all quote variations (`' ‘ ’ “ ” ` `) and punctuation.
    - Add comprehensive Chinese-to-English keyword mapping (`航海家 -> navigator`, `盛宴 -> enchanted summer`, `动画师 -> animator`, etc.).
  - In `locations.json`: rename Deck 17 `露天泳池` to `阳光泳池 Sunnyside Pool` for complete consistency.

## 2. Technical Architecture & File Changes
- **`app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`**:
  - Fix meal period resolution on edit, title synchronization on period selection, and `presetKey` lifecycle.
- **`app/src/main/java/com/cruise/assistant/domain/DeckMap.kt`**:
  - Enhance `inlineHtmlWrapperFor` with target expansion and quote-normalized search scoring in `centerFacilityTarget()`.
- **`app/src/main/assets/locations.json`**:
  - Align Deck 17 pool name to `阳光泳池 Sunnyside Pool`.
- **`app/build.gradle.kts`**:
  - Bump to `versionCode = 48`, `versionName = "1.10.22"`.

## 3. Verification Plan
- Unit tests:
  - `EventSheetViewModelTest` verifying meal period resolution, title update on chip click, and dinner presetKey handling.
  - Script test verifying 74/74 venues match on deck SVGs.
- Emulator verification:
  1. Edit dinner event -> change to breakfast -> verify title becomes "早餐", time is 08:00, and saving persists "早餐".
  2. Click "定位" on Navigator's Club (航海家俱乐部) -> verify deck 6 opens and centers/highlights "Navigator’s Club Restaurant".
