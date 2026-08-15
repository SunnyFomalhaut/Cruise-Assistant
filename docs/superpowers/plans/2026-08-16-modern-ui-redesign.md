# Modern UI/UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the Cruise Assistant Android UI into a clean, modern, ocean-inspired travel companion interface while preserving 100% of the underlying business logic, database schemas, and 3-lane timeline algorithms.

**Architecture:** Update Compose theme tokens, typography, and color math; rebuild timeline cards, date capsule strips, and live banners; modernize onboarding, keypad, sheets, and settings into Material 3 grouped surfaces.

**Tech Stack:** Kotlin 2.1.21, Jetpack Compose (BOM 2026.06.01), Material 3, Android SDK 36.

## Global Constraints

- Never change database schemas or entity definitions.
- Keep all 55 JVM unit tests passing without errors.
- Fixed Light Theme (as specified in domain architecture — no dark mode).
- Target minimum tap target >= 44x44dp for all interactive elements.
- Verify UI on emulator using adb screencap and fix any line breaks, overflow, or clipping issues.

---

### Task 1: Theme & Design Tokens System

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/theme/Theme.kt`

- [ ] **Step 1: Update Color.kt with Modern Ocean tokens and refined card color math**

```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Ocean Azure Theme Tokens
val CruiseAzure = Color(0xFF0284C7)
val CruiseMarine = Color(0xFF0369A1)
val CruiseAzureLight = Color(0xFF38BDF8)
val SurfaceContainer = Color(0xFFF8FAFC)
val SurfaceCard = Color(0xFFFFFFFF)

val Slate900 = Color(0xFF0F172A)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)

val GridLine = Color(0xFFF1F5F9)
val GridLineHour = Color(0xFFE2E8F0)
val NowRed = Color(0xFFEF4444)

fun Int.toComposeColor(): Color = Color(this)

/** 卡片底色：类别色 8% 叠在纯白底上，明亮清爽 */
fun cardBackgroundFor(colorArgb: Int): Color =
    lerp(Color.White, Color(colorArgb), 0.08f)

/** 卡片文字色：类别色往 Slate900 深蓝黑压 45%，保证高可读性与对比度 */
fun cardTextColorFor(colorArgb: Int): Color =
    lerp(Color(colorArgb), Slate900, 0.45f)

/** 卡片文字色的通用别名 */
fun Color.onSurfaceVariantFor(): Color = lerp(this, Slate900, 0.45f)
```

- [ ] **Step 2: Update Type.kt with refined typography hierarchy**

```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CruiseTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    ),
)
```

- [ ] **Step 3: Update Theme.kt to bind new colorScheme**

```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CruiseAzure,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
    onPrimaryContainer = CruiseMarine,
    secondary = CruiseMarine,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = SurfaceContainer,
    onBackground = Slate900,
    surface = SurfaceCard,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate200,
    outlineVariant = Slate100,
    error = NowRed,
)

@Composable
fun CruiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CruiseTypography,
        content = content,
    )
}
```

- [ ] **Step 4: Run tests to verify compilation and baseline logic**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: 55 passed.

- [ ] **Step 5: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/theme/
git commit -m "feat(ui): update color tokens, typography scales, and LightColors scheme"
```

---

### Task 2: Timeline Core Components (DateStrip, NowBanner, TimeRuler, LaneCanvas)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/DateStrip.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/NowBanner.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimeRuler.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt`

- [ ] **Step 1: Modernize DateStrip.kt with capsule cards, clear typography, and finalized badges**
- [ ] **Step 2: Modernize NowBanner.kt with floating pill card, status indicators, and clear layout**
- [ ] **Step 3: Refine TimeRuler.kt with crisp typography and subtle hour/half-hour grid lines**
- [ ] **Step 4: Refine LaneCanvas.kt and NowIndicator with pulsing dot and clean indicator line**
- [ ] **Step 5: Run tests and commit**

Run: `.\gradlew.bat testDebugUnitTest`
Commit: `git commit -m "feat(ui): modernize DateStrip, NowBanner, TimeRuler, and LaneCanvas"`

---

### Task 3: Event Card Modernization (EventCard & CategoryChip)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/common/CategoryChip.kt`

- [ ] **Step 1: Update CategoryChip.kt to render clean micro-capsule with icon and colored container**
- [ ] **Step 2: Update EventCard.kt with 12dp rounded corner, 1px subtle border, left capsule pill, location badge, clean time duration, and conflict warning badge**
- [ ] **Step 3: Run tests and commit**

Run: `.\gradlew.bat testDebugUnitTest`
Commit: `git commit -m "feat(ui): modernize EventCard and CategoryChip with layered card design"`

---

### Task 4: Timeline Screen Layout & Top Header Polish

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`

- [ ] **Step 1: Refactor top voyage app bar with voyage badge, sailing day counter, and rounded action buttons (Pixie Dust, Range, Settings)**
- [ ] **Step 2: Polish control bar with centered "Jump to Now" filled tonal capsule button, status badges, and generate plan action**
- [ ] **Step 3: Polish floating action button stack**
- [ ] **Step 4: Run tests and commit**

Run: `.\gradlew.bat testDebugUnitTest`
Commit: `git commit -m "feat(ui): polish TimelineScreen header, action bar, and floating controls"`

---

### Task 5: Onboarding Screen Modernization

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: Add hero welcome banner with cruise badge**
- [ ] **Step 2: Restructure fields into grouped surface cards (Ship & Schedule, Sailing Dates, Deck & Dining)**
- [ ] **Step 3: Modernize deck step counter buttons and date picker triggers**
- [ ] **Step 4: Upgrade submit button to an elevated full-width capsule button**
- [ ] **Step 5: Run tests and commit**

Run: `.\gradlew.bat testDebugUnitTest`
Commit: `git commit -m "feat(ui): modernize OnboardingScreen with grouped setting cards"`

---

### Task 6: Pixie Dust Screen & Keypad Modernization

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustKeypad.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustLists.kt`

- [ ] **Step 1: Modernize top tab bar with segmented pill switcher and stat count chips in PixieDustScreen.kt**
- [ ] **Step 2: Upgrade PixieDustKeypad.kt with rounded key tiles, clear deck indicator, and responsive room input display**
- [ ] **Step 3: Modernize GivenList and ReceivedList record stream cards in PixieDustLists.kt**
- [ ] **Step 4: Run tests and commit**

Run: `.\gradlew.bat testDebugUnitTest`
Commit: `git commit -m "feat(ui): modernize PixieDustScreen, keypad, and record list cards"`

---

### Task 7: Settings Screen & Bottom Sheets (EventSheet, CategorySheet, LocationPicker)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/LocationPicker.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/category/CategorySheet.kt`

- [ ] **Step 1: Rebuild SettingsScreen with Material 3 grouped cards for Display Range, Reminders, Voyage Overview, and Categories**
- [ ] **Step 2: Polish EventSheet modal bottom sheet with category icon tiles and streamlined layout**
- [ ] **Step 3: Refine LocationPicker dropdown and custom deck/zone selection chips**
- [ ] **Step 4: Polish CategorySheet with color palette dots and icon grid tiles**
- [ ] **Step 5: Run tests and commit**

Run: `.\gradlew.bat testDebugUnitTest`
Commit: `git commit -m "feat(ui): polish SettingsScreen and bottom sheets into grouped cards"`

---

### Task 8: End-to-End Build, Emulator Deployment, Screenshot Verification & Layout/Line-break Fixes

**Files:**
- Build & Deploy: Debug APK on emulator `emulator-5554`
- Test & Capture: Timeline (Planning & Finalized), Onboarding, Pixie Dust, Event Sheet, Settings
- Fix any identified layout, wrapping, or stacking issues.

- [ ] **Step 1: Build debug APK and install to running emulator**
- [ ] **Step 2: Capture screenshots of each screen using adb screencap**
- [ ] **Step 3: Inspect for text clipping, button crowding, awkward line wraps, or overlapping elements**
- [ ] **Step 4: Fix any visual defects and verify with updated screenshots**
- [ ] **Step 5: Run full test suite to ensure 100% test coverage and pass rate**
- [ ] **Step 6: Commit all refinements**
