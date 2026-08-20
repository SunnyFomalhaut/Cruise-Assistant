# Multi-Theme Color Palette (朱迪蓝 / 贝儿粉 / 星黛露紫) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement dynamic theme switching across the entire Android app with three Disney-inspired palettes: 朱迪蓝 (Judy Blue), 贝儿粉 (LinaBell Pink), and 星黛露紫 (StellaLou Purple), configured from the Settings screen and persisted in DataStore.

**Architecture:** A centralized `AppColorTheme` enum with color token definitions (`CruiseColorPalette`), stored in DataStore via `SettingsStore`, broadcast to Compose via `SettingsViewModel` and `MainActivity`, and injected dynamically via `CruiseTheme` (`MaterialTheme.colorScheme` & `LocalCruiseColors`). All screen components reference dynamic theme tokens.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, AndroidX DataStore Preferences, JUnit4.

## Global Constraints
- Target version: v1.11.0
- Palette 1: 朱迪蓝 (`primary = #0284C7`, `secondary = #0369A1`, `container = #E0F2FE`, `light = #38BDF8`)
- Palette 2: 贝儿粉 (`primary = #E05375`, `secondary = #9E1C3F`, `container = #FCE7F3`, `light = #F472B6`, `accent = #A855F7`)
- Palette 3: 星黛露紫 (`primary = #8B5CF6`, `secondary = #5B21B6`, `container = #EDE9FE`, `light = #C4B5FD`, `accent = #14B8A6`)
- All UI colors must dynamically respond to theme change in real-time without app restarts.

---

### Task 1: Data Model, String Resources, and SettingsStore Persistence for AppColorTheme

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/AppColorTheme.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/AppColorThemeTest.kt`

**Interfaces:**
- Consumes: Android string resources, DataStore preferences.
- Produces: `AppColorTheme` enum with `fromKey()`, `AppSettings.colorTheme`, `SettingsStore.setColorTheme(AppColorTheme)`.

- [ ] **Step 1: Write string resources in `strings.xml`**

In `app/src/main/res/values/strings.xml`, add:
```xml
    <!-- Theme Palette -->
    <string name="theme_section_title">主题配色</string>
    <string name="theme_section_desc">选择您喜爱的主题配色风格</string>
    <string name="theme_judy_blue">朱迪蓝</string>
    <string name="theme_judy_blue_desc">经典海洋探索风格</string>
    <string name="theme_linabell_pink">贝儿粉</string>
    <string name="theme_linabell_pink_desc">甜美活泼的小狐狸</string>
    <string name="theme_stellalou_purple">星黛露紫</string>
    <string name="theme_stellalou_purple_desc">优雅梦幻的芭蕾舞者</string>
```

- [ ] **Step 2: Create failing unit test for `AppColorTheme`**

Create `app/src/test/java/com/cruise/assistant/domain/AppColorThemeTest.kt`:
```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppColorThemeTest {

    @Test
    fun testFromKeyMatching() {
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey("judy_blue"))
        assertEquals(AppColorTheme.LINABELL_PINK, AppColorTheme.fromKey("linabell_pink"))
        assertEquals(AppColorTheme.STELLALOU_PURPLE, AppColorTheme.fromKey("stellalou_purple"))
    }

    @Test
    fun testFromKeyFallback() {
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey(null))
        assertEquals(AppColorTheme.JUDY_BLUE, AppColorTheme.fromKey("unknown_key"))
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.AppColorThemeTest"`
Expected: FAIL (unresolved reference `AppColorTheme`).

- [ ] **Step 4: Implement `AppColorTheme.kt` and update `SettingsStore.kt`**

Create `app/src/main/java/com/cruise/assistant/domain/AppColorTheme.kt`:
```kotlin
package com.cruise.assistant.domain

import com.cruise.assistant.R

enum class AppColorTheme(
    val key: String,
    val displayNameRes: Int,
    val descriptionRes: Int,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val containerArgb: Long,
) {
    JUDY_BLUE(
        key = "judy_blue",
        displayNameRes = R.string.theme_judy_blue,
        descriptionRes = R.string.theme_judy_blue_desc,
        primaryArgb = 0xFF0284C7,
        secondaryArgb = 0xFF0369A1,
        containerArgb = 0xFFE0F2FE,
    ),
    LINABELL_PINK(
        key = "linabell_pink",
        displayNameRes = R.string.theme_linabell_pink,
        descriptionRes = R.string.theme_linabell_pink_desc,
        primaryArgb = 0xFFE05375,
        secondaryArgb = 0xFF9E1C3F,
        containerArgb = 0xFFFCE7F3,
    ),
    STELLALOU_PURPLE(
        key = "stellalou_purple",
        displayNameRes = R.string.theme_stellalou_purple,
        descriptionRes = R.string.theme_stellalou_purple_desc,
        primaryArgb = 0xFF8B5CF6,
        secondaryArgb = 0xFF5B21B6,
        containerArgb = 0xFFEDE9FE,
    );

    companion object {
        fun fromKey(key: String?): AppColorTheme =
            entries.firstOrNull { it.key == key } ?: JUDY_BLUE
    }
}
```

In `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`:
Update `AppSettings`:
```kotlin
data class AppSettings(
    val lastViewedDate: LocalDate? = null,
    val displayStartMinute: Int = 7 * 60,
    val displayEndMinute: Int = 24 * 60,
    val hourHeightDp: Int = 120,
    val collapseEmptyGaps: Boolean = false,
    val remindBefore15: Boolean = true,
    val remindBefore5: Boolean = true,
    val remindAtStart: Boolean = true,
    val dinnerTime: LocalTime = LocalTime.of(17, 45),
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val lastPixieDeck: Int? = null,
    val colorTheme: com.cruise.assistant.domain.AppColorTheme = com.cruise.assistant.domain.AppColorTheme.JUDY_BLUE,
)
```
In `Keys`:
```kotlin
val colorTheme = stringPreferencesKey("color_theme")
```
In `settings: Flow<AppSettings>` mapping:
```kotlin
colorTheme = prefs[Keys.colorTheme]?.let { com.cruise.assistant.domain.AppColorTheme.fromKey(it) } ?: defaults.colorTheme,
```
Add setter:
```kotlin
suspend fun setColorTheme(theme: com.cruise.assistant.domain.AppColorTheme) = edit {
    it[Keys.colorTheme] = theme.key
}
```

- [ ] **Step 5: Run unit tests and verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.AppColorThemeTest"`
Expected: BUILD SUCCESSFUL and tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/cruise/assistant/domain/AppColorTheme.kt app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt app/src/test/java/com/cruise/assistant/domain/AppColorThemeTest.kt
git commit -m "feat: add AppColorTheme domain enum and DataStore persistence"
```

---

### Task 2: Dynamic Theme Engine & Color Palette Architecture

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/theme/Theme.kt`

**Interfaces:**
- Consumes: `AppColorTheme`
- Produces: `CruiseColorPalette`, `LocalCruiseColors`, dynamic `CruiseTheme(theme: AppColorTheme, content: @Composable () -> Unit)`, `MaterialTheme.cruiseColors`.

- [ ] **Step 1: Define `CruiseColorPalette` and theme token instances in `Color.kt`**

In `app/src/main/java/com/cruise/assistant/ui/theme/Color.kt`:
Define the palette data class and specific palette instances:
```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.cruise.assistant.domain.AppColorTheme

// Core Neutrals
val Slate900 = Color(0xFF0F172A)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)

val SurfaceCard = Color(0xFFFFFFFF)
val GridLine = Color(0xFFF1F5F9)
val GridLineHour = Color(0xFFE2E8F0)
val NowRed = Color(0xFFEF4444)

// Cruise Color Palette Specification
data class CruiseColorPalette(
    val primary: Color,
    val secondary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryLight: Color,
    val accent: Color,
    val surfaceContainer: Color,
    val surfaceCard: Color = SurfaceCard,
)

// 1. 朱迪蓝 (Judy Blue)
val JudyBluePalette = CruiseColorPalette(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF0369A1),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    primaryLight = Color(0xFF38BDF8),
    accent = Color(0xFF38BDF8),
    surfaceContainer = Color(0xFFF8FAFC),
)

// 2. 贝儿粉 (LinaBell Pink)
val LinaBellPinkPalette = CruiseColorPalette(
    primary = Color(0xFFE05375),
    secondary = Color(0xFF9E1C3F),
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF9E1C3F),
    primaryLight = Color(0xFFF472B6),
    accent = Color(0xFFA855F7), // LinaBell flower purple
    surfaceContainer = Color(0xFFFFF9FA),
)

// 3. 星黛露紫 (StellaLou Purple)
val StellaLouPurplePalette = CruiseColorPalette(
    primary = Color(0xFF8B5CF6),
    secondary = Color(0xFF5B21B6),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF5B21B6),
    primaryLight = Color(0xFFC4B5FD),
    accent = Color(0xFF14B8A6), // StellaLou flower mint
    surfaceContainer = Color(0xFFFAF8FF),
)

fun paletteFor(theme: AppColorTheme): CruiseColorPalette = when (theme) {
    AppColorTheme.JUDY_BLUE -> JudyBluePalette
    AppColorTheme.LINABELL_PINK -> LinaBellPinkPalette
    AppColorTheme.STELLALOU_PURPLE -> StellaLouPurplePalette
}

val LocalCruiseColors = staticCompositionLocalOf { JudyBluePalette }

// MaterialTheme extensions & Top-level Composable helpers for reactive access
val androidx.compose.material3.MaterialTheme.cruiseColors: CruiseColorPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalCruiseColors.current

// Dynamic color accessors
val CruiseAzure: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primary
val CruiseMarine: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.secondary
val CruiseAzureLight: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primaryLight
val CruiseAzureContainer: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primaryContainer
val CruiseBlue: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primary
val CruiseBlueLight: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primaryLight
val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.surfaceContainer
val SurfaceGray: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.surfaceContainer

fun Int.toComposeColor(): Color = Color(this)

/** 卡片底色：类别色 8% 叠在白底上，明亮清爽 */
fun cardBackgroundFor(colorArgb: Int): Color =
    lerp(Color.White, Color(colorArgb), 0.08f)

/** 卡片文字色：类别色往 Slate900 深蓝黑压 45%，保证在浅底上对比度足够且视觉统一 */
fun cardTextColorFor(colorArgb: Int): Color =
    lerp(Color(colorArgb), Slate900, 0.45f)

/** 卡片文字色的通用别名：给 onSurfaceVariant 语义下的类别色文字用 */
fun Color.onSurfaceVariantFor(): Color = lerp(this, Slate900, 0.45f)
```

- [ ] **Step 2: Update `CruiseTheme` in `Theme.kt`**

In `app/src/main/java/com/cruise/assistant/ui/theme/Theme.kt`:
```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.cruise.assistant.domain.AppColorTheme

@Composable
fun CruiseTheme(
    theme: AppColorTheme = AppColorTheme.JUDY_BLUE,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(theme)
    val colorScheme = lightColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.secondary,
        onSecondary = Color.White,
        secondaryContainer = palette.primaryContainer,
        onSecondaryContainer = palette.secondary,
        background = palette.surfaceContainer,
        onBackground = Slate900,
        surface = palette.surfaceCard,
        onSurface = Slate900,
        surfaceVariant = Slate100,
        onSurfaceVariant = Slate500,
        surfaceContainer = palette.surfaceContainer,
        surfaceContainerHigh = Color.White,
        outline = Slate200,
        outlineVariant = Slate100,
        error = NowRed,
        onError = Color.White,
    )

    CompositionLocalProvider(LocalCruiseColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CruiseTypography,
            content = content,
        )
    }
}
```

- [ ] **Step 3: Update `MainActivity.kt` to observe `colorTheme`**

In `app/src/main/java/com/cruise/assistant/MainActivity.kt`:
Collect settings state and pass `settings.colorTheme` to `CruiseTheme`:
```kotlin
        val container = (application as CruiseApp).container
        setContent {
            val settings by container.settingsStore.settings.collectAsState(initial = com.cruise.assistant.data.prefs.AppSettings())
            CruiseTheme(theme = settings.colorTheme) {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .consumeWindowInsets(WindowInsets.safeDrawing),
                ) {
                    CruiseNavHost(container)
                }
            }
        }
```

- [ ] **Step 4: Run unit tests to check compilation**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/theme/Color.kt app/src/main/java/com/cruise/assistant/ui/theme/Theme.kt app/src/main/java/com/cruise/assistant/MainActivity.kt
git commit -m "feat: implement dynamic theme engine and color palette system"
```

---

### Task 3: Settings Screen Theme Selector Card

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`

**Interfaces:**
- Consumes: `SettingsViewModel.setColorTheme(AppColorTheme)`, `SettingsUiState.settings.colorTheme`.
- Produces: Visual Theme Selector Card with color preview dots, title, subtitle, and check indicator.

- [ ] **Step 1: Add `setColorTheme` in `SettingsViewModel.kt`**

In `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt`:
Add:
```kotlin
    fun setColorTheme(theme: com.cruise.assistant.domain.AppColorTheme) {
        viewModelScope.launch { settingsStore.setColorTheme(theme) }
    }
```

- [ ] **Step 2: Add Theme Selection Card in `SettingsScreen.kt`**

In `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`:
Add parameter `onColorThemeChange: (AppColorTheme) -> Unit` to `SettingsScreen`.
Add a dedicated Theme Selection Card below the header text:
```kotlin
        // ---- 主题配色卡片 ----
        SettingsCard(
            title = stringResource(R.string.theme_section_title),
            icon = androidx.compose.material.icons.Icons.Outlined.Palette,
            cardShape = cardShape,
        ) {
            Text(
                stringResource(R.string.theme_section_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppColorTheme.entries.forEach { theme ->
                    val isSelected = state.settings.colorTheme == theme
                    val palette = paletteFor(theme)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) palette.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) palette.primary else Slate200,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onColorThemeChange(theme) },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Dual Color Preview Dot / Badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryContainer)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .weight(1f)
                                            .background(palette.primary)
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .weight(1f)
                                            .background(palette.secondary)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(theme.displayNameRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) palette.secondary else Slate900,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(theme.descriptionRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isSelected) palette.secondary.copy(alpha = 0.8f) else Slate500,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
```

- [ ] **Step 3: Wire `onColorThemeChange` in `CruiseNavHost.kt`**

In `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`:
Pass `onColorThemeChange = settingsViewModel::setColorTheme` to `SettingsScreen`.

- [ ] **Step 4: Run unit tests and verify compilation**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: add theme selection card in SettingsScreen"
```

---

### Task 4: UI Screen Token Cleanliness and Visual Harmony

**Files:**
- Audit & Modify:
  - `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`
  - `app/src/main/java/com/cruise/assistant/ui/timeline/NowBanner.kt`
  - `app/src/main/java/com/cruise/assistant/ui/timeline/TimeRuler.kt`
  - `app/src/main/java/com/cruise/assistant/ui/timeline/DateStrip.kt`
  - `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistScreen.kt`
  - `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistEditSheet.kt`
  - `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapScreen.kt`
  - `app/src/main/java/com/cruise/assistant/ui/deckmap/DeckMapSearchSheet.kt`
  - `app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideScreen.kt`
  - `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustScreen.kt`
  - `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustKeypad.kt`
  - `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustLists.kt`
  - `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustEditDialog.kt`
  - `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieLevelSheet.kt`
  - `app/src/main/java/com/cruise/assistant/ui/pixiedust/ReplyLevelDialog.kt`
  - `app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewDialog.kt`
  - `app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt`
  - `app/src/main/java/com/cruise/assistant/ui/event/LocationPicker.kt`
  - `app/src/main/java/com/cruise/assistant/ui/category/CategorySheet.kt`
  - `app/src/main/java/com/cruise/assistant/ui/onboarding/OnboardingScreen.kt`

**Interfaces:**
- Consumes: Dynamic color tokens from `MaterialTheme.colorScheme` and `LocalCruiseColors.current`.
- Produces: All screens adapt seamlessly to Judy Blue, LinaBell Pink, and StellaLou Purple.

- [ ] **Step 1: Check and clean color imports and usage in all UI composables**

Verify that all UI composables resolve `CruiseAzure`, `CruiseMarine`, `CruiseAzureContainer`, `CruiseAzureLight`, `SurfaceContainer`, etc. through dynamic `@Composable` getters or `MaterialTheme.colorScheme` / `LocalCruiseColors.current`. Ensure no hardcoded hex or non-reactive references bypass the theme.

- [ ] **Step 2: Run all unit tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: ALL PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/
git commit -m "feat: polish UI screens with dynamic multi-theme tokens"
```

---

### Task 5: Full Build, APK Generation, and End-to-End Verification

**Files:**
- Modify: `app/build.gradle.kts` (bump versionCode to 28, versionName to "1.11.0" if applicable)

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**

Set `versionCode = 28`, `versionName = "1.11.0"`.

- [ ] **Step 2: Run unit test suite**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Build release / debug APK**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL with apk generated in `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Commit release bump**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to v1.11.0 (versionCode 28)"
```
