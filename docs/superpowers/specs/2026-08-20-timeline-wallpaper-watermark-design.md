# Design: Timeline Translucent Wallpaper / Background Watermark (日程表半透明底图)

- **Author**: opencode
- **Date**: 2026-08-20
- **Target Version**: v1.11.0 (versionCode 29)

## 1. Overview & Goals

This feature introduces a classic schedule/timeline background wallpaper system tailored for the Disney Adventure cruise assistant. Users can set a delicate translucent watermark background behind the main timeline schedule, choosing either from built-in high-definition Duffy & Friends official wallpapers or picking a custom image from the device's photo gallery.

Key features:
1. **7 Built-in Character Wallpaper Presets**: Duffy, LinaBell, ShellieMay, StellaLou, Gelatoni, CookieAnn, and 'Olu Mel.
2. **Custom Gallery Photo Support**: Select any photo from the device with automatic copying and optimization to the app's internal sandbox storage (`filesDir/custom_wallpaper.jpg`).
3. **Adjustable Opacity (5% ~ 40%, default 15%)**: Ensures high schedule readability and zero clash with event cards and text.
4. **Instant Toggle & Live Preview**: Real-time updates without app restarts.

---

## 2. Architecture & Data Model

### 2.1 Domain Layer
- **Enum `WallpaperPreset`**:
  ```kotlin
  enum class WallpaperPreset(
      val key: String,
      val displayNameRes: Int,
      val assetPath: String,
  ) {
      NONE("none", R.string.wallpaper_none, ""),
      DUFFY("duffy", R.string.wallpaper_duffy, "wallpapers/duffy.png"),
      LINABELL("linabell", R.string.wallpaper_linabell, "wallpapers/linabell.jpg"),
      SHELLIEMAY("shelliemay", R.string.wallpaper_shelliemay, "wallpapers/shelliemay.jpg"),
      STELLALOU("stellalou", R.string.wallpaper_stellalou, "wallpapers/stellalou.jpg"),
      GELATONI("gelatoni", R.string.wallpaper_gelatoni, "wallpapers/gelatoni.jpg"),
      COOKIEANN("cookieann", R.string.wallpaper_cookieann, "wallpapers/cookieann.jpg"),
      OLU("olu", R.string.wallpaper_olu, "wallpapers/olu.jpg"),
      CUSTOM("custom", R.string.wallpaper_custom, "custom_wallpaper.jpg");

      companion object {
          fun fromKey(key: String?): WallpaperPreset =
              entries.firstOrNull { it.key == key } ?: NONE
      }
  }
  ```

### 2.2 Preferences & DataStore (`SettingsStore.kt`)
- **`AppSettings` additions**:
  ```kotlin
  val wallpaperEnabled: Boolean = false,
  val wallpaperKey: String = WallpaperPreset.NONE.key,
  val wallpaperAlpha: Float = 0.15f,
  ```
- **Preference Keys & Mutators**:
  - `Keys.wallpaperEnabled`: `booleanPreferencesKey("wallpaper_enabled")`
  - `Keys.wallpaperKey`: `stringPreferencesKey("wallpaper_key")`
  - `Keys.wallpaperAlpha`: `floatPreferencesKey("wallpaper_alpha")`
  - `setWallpaperEnabled(enabled: Boolean)`
  - `setWallpaperKey(key: String)`
  - `setWallpaperAlpha(alpha: Float)`

### 2.3 Image Loader & Caching (`WallpaperManager.kt`)
- Loads bitmap from assets (`context.assets.open(preset.assetPath)`) or app sandbox (`context.filesDir/custom_wallpaper.jpg`) into a cached `Bitmap` / `ImageBitmap`.
- Copies picked URI from `ActivityResultContracts.PickVisualMedia` / `GetContent` into `context.filesDir/custom_wallpaper.jpg` with memory-safe downsampling.

---

## 3. UI / UX Design in Settings & Timeline

### 3.1 Settings Screen (`SettingsCard`)
- **Card Title**: `日程表底图` (`Icons.Outlined.Wallpaper` or `Icons.Outlined.Image`).
- **Main Switch**: `启用日程表底图` (Toggle).
- When enabled:
  - **Opacity Slider**: `透明度: 15%` (`valueRange = 0.05f..0.40f`, `steps = 34`).
  - **Wallpaper Selector**:
    - Horizontal scroll row of wallpaper preview cards (rounded 10.dp, width 70.dp, height 110.dp).
    - Each preset card displays a cropped thumbnail of the wallpaper, character label, and highlight outline when selected.
    - Dedicated **"+ 自定义"** card to open system photo picker. When custom image is set, it displays a thumbnail of the user's custom photo with a small "换图" badge.

### 3.2 Timeline Screen Background Layer
- In `TimelineScreen.kt`, positioned at the background under the scrollable schedule:
  ```kotlin
  Box(modifier = Modifier.fillMaxSize()) {
      if (state.settings.wallpaperEnabled) {
          WallpaperBackground(
              wallpaperKey = state.settings.wallpaperKey,
              alpha = state.settings.wallpaperAlpha,
              modifier = Modifier.fillMaxSize()
          )
      }
      // Timeline content...
  }
  ```

---

## 4. Verification Plan

1. **Unit Tests**:
   - `WallpaperPresetTest`: `fromKey()` lookup and fallbacks.
   - `SettingsStoreTest`: DataStore saving and reading of `wallpaperEnabled`, `wallpaperKey`, `wallpaperAlpha`.
2. **UI & Build Verification**:
   - Run Gradle unit tests `./gradlew testDebugUnitTest`.
   - Install to emulator and test:
     - Toggling wallpaper on/off.
     - Selecting each of the 7 character presets.
     - Adjusting opacity slider between 5% and 40%.
     - Selecting a custom photo from gallery.
