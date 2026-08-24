# Adaptive Watermark Opacity by Finalization State v1.10.17 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement adaptive watermark opacity where finalized cards maintain vibrant opacity (`alpha = 0.38f`) and draft cards use faint opacity (`alpha = 0.16f`) to prevent text overlap interference in 3-lane view.

**Architecture:** In `EventCard.kt`, compute `val dollAlpha = if (isFinalized) 0.38f else 0.16f` and bind it to the watermark `Image` composable.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 43`, `versionName = "1.10.17"`
- Finalized card opacity: `0.38f`.
- Draft card opacity: `0.16f`.
- 100% of unit tests in `gradlew testDebugUnitTest` must pass.

---

### Task 1: Update Watermark Opacity in `EventCard.kt`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`

**Interfaces:**
- Consumes: `isFinalized: Boolean`
- Produces: `dollAlpha = if (isFinalized) 0.38f else 0.16f` passed to `Image(..., alpha = dollAlpha)`

- [x] **Step 1: Update `EventCard.kt`**
Compute `val dollAlpha = if (isFinalized) 0.38f else 0.16f` and pass to `Image(..., alpha = dollAlpha)`.

- [x] **Step 2: Verify compilation and tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [x] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt
git commit -m "style(ui): apply adaptive watermark opacity (0.38f finalized, 0.16f draft)"
```

---

### Task 2: Version Bump, Build & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [x] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 43`, `versionName = "1.10.17"`.

- [x] **Step 2: Build and install APK**
Run `assembleDebug` and install to running emulator.

- [x] **Step 3: Capture screenshots**
Capture finalized 1-lane view and draft 3-lane view to verify readability.

- [x] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.17 (43) and add verification screenshots"
```
