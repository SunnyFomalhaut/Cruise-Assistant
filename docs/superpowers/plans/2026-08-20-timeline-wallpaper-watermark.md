# Timeline Translucent Wallpaper / Background Watermark Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a customizable translucent wallpaper watermark behind the main schedule timeline, featuring 7 built-in Duffy & Friends official wallpapers, custom gallery photo selection, and adjustable transparency.

**Architecture:** Wallpapers bundled under `assets/wallpapers/`, persisted state in DataStore (`SettingsStore`), managed via `WallpaperManager` and `SettingsViewModel`, rendered in `TimelineScreen` with configurable alpha (`0.05f..0.40f`), and configured via `SettingsScreen`.

**Tech Stack:** Kotlin, Jetpack Compose, Android Assets, AndroidX Photo Picker, DataStore Preferences.

## Global Constraints
- Target version: v1.10.2 (versionCode 28)
- 7 Built-in presets: `duffy`, `linabell`, `shelliemay`, `stellalou`, `gelatoni`, `cookieann`, `olu`
- Alpha range: `0.05f..0.40f` (default `0.15f` / 15%)
- Custom wallpaper stored securely in internal sandbox `context.filesDir/custom_wallpaper.jpg`

---

### Task 1: Asset Packaging and String Resources

**Files:**
- Create: `app/src/main/assets/wallpapers/duffy.png`
- Create: `app/src/main/assets/wallpapers/linabell.jpg`
- Create: `app/src/main/assets/wallpapers/shelliemay.jpg`
- Create: `app/src/main/assets/wallpapers/stellalou.jpg`
- Create: `app/src/main/assets/wallpapers/gelatoni.jpg`
- Create: `app/src/main/assets/wallpapers/cookieann.jpg`
- Create: `app/src/main/assets/wallpapers/olu.jpg`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Copy wallpaper assets to `app/src/main/assets/wallpapers/`**
- [ ] **Step 2: Add wallpaper strings to `strings.xml`**
- [ ] **Step 3: Commit**

---

### Task 2: Domain Enum `WallpaperPreset` and DataStore Persistence

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/WallpaperPreset.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/WallpaperPresetTest.kt`

- [ ] **Step 1: Write failing unit tests in `WallpaperPresetTest.kt`**
- [ ] **Step 2: Implement `WallpaperPreset.kt`**
- [ ] **Step 3: Update `SettingsStore.kt` and `AppSettings`**
- [ ] **Step 4: Run unit tests and verify they pass**
- [ ] **Step 5: Commit**

---

### Task 3: WallpaperManager & Sandbox Image Loader

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/WallpaperManager.kt`

- [ ] **Step 1: Implement `WallpaperManager` for loading bitmaps from assets/filesDir and copying URIs safely**
- [ ] **Step 2: Commit**

---

### Task 4: Settings Screen Wallpaper Card & Photo Picker Integration

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`

- [ ] **Step 1: Add wallpaper mutators in `SettingsViewModel.kt`**
- [ ] **Step 2: Implement Wallpaper Settings Card in `SettingsScreen.kt`**
- [ ] **Step 3: Connect photo picker and callbacks in `CruiseNavHost.kt`**
- [ ] **Step 4: Run unit tests**
- [ ] **Step 5: Commit**

---

### Task 5: TimelineScreen Background Layer & End-to-End Verification

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`

- [ ] **Step 1: Render translucent wallpaper in `TimelineScreen.kt`**
- [ ] **Step 2: Run all unit tests (`testDebugUnitTest`)**
- [ ] **Step 3: Install on emulator and test live wallpaper switching & alpha slider**
- [ ] **Step 4: Commit**
