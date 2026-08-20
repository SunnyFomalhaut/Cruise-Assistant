# Design: Multi-Theme Color Palette (朱迪蓝 / 贝儿粉 / 星黛露紫)

- **Author**: opencode
- **Date**: 2026-08-20
- **Target Version**: v1.11.0

## 1. Overview & Goals

As the Disney Adventure cruise sails as the premier vessel featuring Duffy & Friends, personalization and visual charm are central to the guest experience. This feature introduces a comprehensive dynamic theme switching system offering three Disney-inspired palettes:

1. **朱迪蓝 (Judy Blue)** *(Default)*: Classic crisp ocean azure palette (`#0284C7`), representing classic cruise exploration and Officer Judy Hopps.
2. **贝儿粉 (LinaBell Pink)**: Warm, delicate blossom & fox pink palette (`#E05375`), capturing LinaBell's curiosity, sweetness, and orchid floral tones.
3. **星黛露紫 (StellaLou Purple)**: Dreamy ballet lavender violet palette (`#8B5CF6`), embodying StellaLou's Broadway dance dreams and mint ribbon accents.

All application components—including timeline headers, floating action buttons, search bars, active category filters, bottom sheets, dialogs, map deck selectors, wishlist cards, pixie dust keypads, and settings—will dynamically adapt in real time to the selected theme.

---

## 2. Color System & Palette Specifications

### 2.1 Theme Palette Matrix

| Token Name | 朱迪蓝 (Judy Blue) | 贝儿粉 (LinaBell Pink) | 星黛露紫 (StellaLou Purple) | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **primary** | `#0284C7` (Sky Azure) | `#E05375` (LinaBell Rose) | `#8B5CF6` (Ballet Lavender) | Main brand accents, primary buttons, active toggles, FABs |
| **secondary / marine** | `#0369A1` (Deep Marine) | `#9E1C3F` (Deep Berry Rose) | `#5B21B6` (Royal Ballet Purple) | Dark text/icons on container backgrounds, subtitles, section titles |
| **primaryContainer** | `#E0F2FE` (Ice Blue Tint) | `#FCE7F3` (Soft Pink Mist) | `#EDE9FE` (Lilac Cloud Tint) | Chip backgrounds, banner containers, icon bubble backgrounds |
| **onPrimaryContainer**| `#0369A1` (Deep Marine) | `#9E1C3F` (Deep Berry Rose) | `#5B21B6` (Royal Ballet Purple) | Contrast foreground on container |
| **primaryLight** | `#38BDF8` | `#F472B6` | `#C4B5FD` | Light accents, slider tracks, subtle borders |
| **accent** | `#38BDF8` | `#A855F7` (Orchid Purple) | `#14B8A6` (Mint Green) | Role-specific specialty highlight / badges |
| **surfaceContainer** | `#F8FAFC` | `#FFF9FA` | `#FAF8FF` | Page background tint |
| **surfaceCard** | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | Card surface |

Neutral slate grays (`Slate900`, `Slate700`, `Slate500`, `Slate400`, `Slate200`, `Slate100`) and the timeline event category color engine (`cardBackgroundFor`, `cardTextColorFor`) remain universally clear, accessible, and legible across all themes.

---

## 3. Architecture & Data Flow

### 3.1 Domain & Preferences Layer
- **Enum `AppColorTheme`**:
  ```kotlin
  enum class AppColorTheme(
      val key: String,
      val displayNameRes: Int,
      val descriptionRes: Int,
      val previewPrimary: Long,
      val previewSecondary: Long,
      val previewContainer: Long,
  ) {
      JUDY_BLUE(
          key = "judy_blue",
          displayNameRes = R.string.theme_judy_blue,
          descriptionRes = R.string.theme_judy_blue_desc,
          previewPrimary = 0xFF0284C7,
          previewSecondary = 0xFF0369A1,
          previewContainer = 0xFFE0F2FE,
      ),
      LINABELL_PINK(
          key = "linabell_pink",
          displayNameRes = R.string.theme_linabell_pink,
          descriptionRes = R.string.theme_linabell_pink_desc,
          previewPrimary = 0xFFE05375,
          previewSecondary = 0xFF9E1C3F,
          previewContainer = 0xFFFCE7F3,
      ),
      STELLALOU_PURPLE(
          key = "stellalou_purple",
          displayNameRes = R.string.theme_stellalou_purple,
          descriptionRes = R.string.theme_stellalou_purple_desc,
          previewPrimary = 0xFF8B5CF6,
          previewSecondary = 0xFF5B21B6,
          previewContainer = 0xFFEDE9FE,
      );

      companion object {
          fun fromKey(key: String?): AppColorTheme =
              entries.firstOrNull { it.key == key } ?: JUDY_BLUE
      }
  }
  ```
- **DataStore Storage in `SettingsStore`**:
  - Key: `color_theme = stringPreferencesKey("color_theme")`
  - In `AppSettings`: `val colorTheme: AppColorTheme = AppColorTheme.JUDY_BLUE`
  - Mutation: `suspend fun setColorTheme(theme: AppColorTheme)`
- **ViewModel Integration in `SettingsViewModel`**:
  - `fun setColorTheme(theme: AppColorTheme)` delegating to `settingsStore.setColorTheme(theme)`.

### 3.2 Dynamic Jetpack Compose Theme Engine
- **`CruiseColorPalette`**:
  ```kotlin
  data class CruiseColorPalette(
      val primary: Color,
      val secondary: Color,
      val primaryContainer: Color,
      val onPrimaryContainer: Color,
      val primaryLight: Color,
      val accent: Color,
      val surfaceContainer: Color,
      val surfaceCard: Color,
  )
  ```
- **`LocalCruiseColors`**:
  - `val LocalCruiseColors = staticCompositionLocalOf { JudyBluePalette }`
  - Extension property `MaterialTheme.cruiseColors: CruiseColorPalette` for ergonomic and reactive access in composables.
- **Root `CruiseTheme`**:
  - Accepts `colorTheme: AppColorTheme = AppColorTheme.JUDY_BLUE`.
  - Dynamically switches both `MaterialTheme(colorScheme = ...)` and `CompositionLocalProvider(LocalCruiseColors provides palette)`.
  - In `MainActivity`, collects `settings.colorTheme` via `StateFlow` and passes it to `CruiseTheme`.

---

## 4. UI / UX Design in Settings Screen

### 4.1 Theme Selection Card
- Located prominently near the top of `SettingsScreen` (below Title or above feature entrances).
- Displays a clean Section Header with icon (`Icons.Outlined.Palette`) and title "主题配色".
- Contains 3 side-by-side (or responsive flow) Theme Selection Cards:
  1. **Color Badge**: Circular preview badge showing a dual-color split/gradient of the theme's Primary and Secondary colors on the Container background.
  2. **Theme Name**: e.g., "朱迪蓝", "贝儿粉", "星黛露紫" (Bold font).
  3. **Selection Indicator**: 
     - Active theme displays a colored highlight border (2.dp), container tint background, and a checkmark badge.
     - Inactive theme displays standard neutral slate border (`Slate200`) and plain card background.
- Selecting any option immediately updates DataStore and re-renders the entire app with zero lag or screen flashes.

---

## 5. Screen Refactoring & Token Unification

All UI screens will migrate from static global color constants to dynamic theme tokens:
1. `CruiseAzure` -> `MaterialTheme.colorScheme.primary` or `MaterialTheme.cruiseColors.primary`
2. `CruiseMarine` -> `MaterialTheme.colorScheme.secondary` or `MaterialTheme.cruiseColors.secondary`
3. `CruiseAzureContainer` -> `MaterialTheme.colorScheme.primaryContainer` or `MaterialTheme.cruiseColors.primaryContainer`
4. `CruiseAzureLight` -> `MaterialTheme.cruiseColors.primaryLight`

This covers:
- `TimelineScreen.kt`, `NowBanner.kt`, `TimeRuler.kt`, `ConflictDialog.kt`
- `WishlistScreen.kt`, `WishlistEditSheet.kt`
- `DeckMapScreen.kt`, `DeckMapSearchSheet.kt`
- `FacilityGuideScreen.kt`
- `PixieDustScreen.kt`, `PixieDustKeypad.kt`, `PixieDustLists.kt`, `PixieDustEditDialog.kt`, `PixieLevelSheet.kt`, `ReplyLevelDialog.kt`, `LinkageViewDialog.kt`
- `EventSheet.kt`, `LocationPicker.kt`
- `CategorySheet.kt`, `OnboardingScreen.kt`, `SettingsScreen.kt`

---

## 6. Verification Plan

1. **Unit Tests**:
   - Verify `AppColorTheme.fromKey()` parses valid and fallback keys correctly.
   - Verify `SettingsStore` properly persists and reads `colorTheme`.
   - Verify `SettingsViewModel.setColorTheme()` updates the state flow.
2. **UI & Theme Switching Verification**:
   - Run Gradle unit tests `./gradlew testDebugUnitTest`.
   - Build debug/release APK `./gradlew assembleDebug` to ensure zero compilation or styling errors.
   - Test switching across all 3 themes (Judy Blue -> LinaBell Pink -> StellaLou Purple) and verify that Timeline, Facility Guide, Deck Map, Wishlist, Pixie Dust, and Settings dynamically update to matching character tones.
