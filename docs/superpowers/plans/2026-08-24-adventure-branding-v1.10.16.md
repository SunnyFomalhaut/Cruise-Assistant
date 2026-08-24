# Disney Adventure Branding & Dinner Hours Update v1.10.16 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update all dinner venue operating hours from 18:00 to 17:45 in `locations.json` and `EventSheetViewModel.kt`, redesign launcher vector icon to accurately depict Disney Adventure (twin red funnels with white Mickey silhouette, white decks, dark navy hull with gold stripe & red keel), update Chinese app name to "探险号助手", and bump version to `1.10.16 (42)`.

**Architecture:** Data layer updates `locations.json` venue slots. ViewModel layer adjusts default dinner start time to 17:45. Android vector resources update `ic_launcher_foreground.xml` and `ic_launcher_background.xml`. Localization resources update `strings.xml`.

**Tech Stack:** Android Vector Drawables, Kotlin, Jetpack Compose, JSON, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 42`, `versionName = "1.10.16"`
- Dinner opening time for all main & specialty restaurants: `17:45`.
- App Chinese Name: `探险号助手`.
- Launcher icon: Must accurately represent Disney Adventure livery (Red funnels with Mickey logo, white decks, dark hull, gold & red waterlines).
- 100% of unit tests in `gradlew testDebugUnitTest` must pass.

---

### Task 1: Update Dinner Venue Operating Hours & Event Sheet Default Time

**Files:**
- Modify: `app/src/main/assets/locations.json`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`
- Modify/Test: `app/src/test/java/com/cruise/assistant/domain/LocationCatalogTest.kt`

**Interfaces:**
- Consumes: JSON venue slots, `MealPeriod.DINNER`
- Produces: 17:45 start time for dinner slots across all dining venues, eliminating false out-of-hours validation.

- [x] **Step 1: Update `locations.json`**
Change dinner slots starting at `18:00` to `17:45` for:
- 航海家俱乐部 Navigator's Club Restaurant
- 皮克斯市场餐厅 Pixar Market Restaurant
- 怪兽亚洲风味 Mike & Sulley's Flavors of Asia
- 帕洛意大利餐厅 Palo Trattoria (first slot: 17:45 - 20:00)

- [x] **Step 2: Update `EventSheetViewModel.kt`**
Update line 280: `MealPeriod.DINNER -> LocalTime.of(17, 45)`.

- [x] **Step 3: Update and run tests in `LocationCatalogTest.kt`**
Verify that tests pass with updated dinner slot start times.

Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [x] **Step 4: Commit changes**

```bash
git add app/src/main/assets/locations.json app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt app/src/test/java/com/cruise/assistant/domain/LocationCatalogTest.kt
git commit -m "fix(venues): change dinner opening hours to 17:45 and update default dinner time"
```

---

### Task 2: Redesign App Icon (Disney Adventure Vector Asset)

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_background.xml`

**Interfaces:**
- Produces: High-quality adaptive vector icon representing Disney Adventure ship colors and Mickey funnels.

- [x] **Step 1: Update `ic_launcher_background.xml`**
Set deep marine gradient (`#0B192C` -> `#1E3E62`) with subtle decorative wave accents.

- [x] **Step 2: Update `ic_launcher_foreground.xml`**
Implement accurate Disney Adventure cruise vector:
- Twin iconic Disney red funnels (`#E51821`) with black top caps.
- Crisp white Mickey head silhouette (1 center circle + 2 ear circles in `#FFFFFF`) on the front funnel.
- White tiered superstructure decks and bridge (`#FFFFFF`).
- Yellow lifeboat deck stripe (`#FFC72C`).
- Dark navy/black hull (`#141923`) with gold waterline stripe (`#FFC72C`) and red keel line (`#E51821`).
- Dynamic turquoise ocean wave splashes (`#00B4D8`, `#90E0EF`).

- [x] **Step 3: Verify vector drawable XML syntax and rendering**
Compile debug sources to ensure no XML parse errors.

- [x] **Step 4: Commit icon assets**

```bash
git add app/src/main/res/drawable/ic_launcher_foreground.xml app/src/main/res/drawable/ic_launcher_background.xml
git commit -m "style(branding): redesign launcher icon with Disney Adventure red funnels and Mickey logo"
```

---

### Task 3: Update App Chinese Name

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

**Interfaces:**
- Produces: App name updated to "探险号助手".

- [x] **Step 1: Update `strings.xml` and `values-zh-rCN/strings.xml`**
Set `<string name="app_name">探险号助手</string>`.

- [x] **Step 2: Verify strings and compilation**

- [x] **Step 3: Commit string changes**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml
git commit -m "chore(branding): rename app Chinese name to 探险号助手"
```

---

### Task 4: Version Bump, Build & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [x] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 42`, `versionName = "1.10.16"`.

- [x] **Step 2: Run all unit tests and build APK**
Run `testDebugUnitTest` and `assembleDebug`.

- [x] **Step 3: Install on emulator and capture verification screenshots**
- Capture timeline / event sheet with 17:45 dinner venue (verify no outside operating hours warning).
- Capture Android launcher showing the new Disney Adventure icon and "探险号助手" label.

- [x] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.16 (42) and add verification screenshots"
```
