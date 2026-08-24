# Design Spec: Adaptive Watermark Opacity by Finalization State v1.10.17

## 1. Background & Objective
In draft mode (`isFinalized == false`), timeline events are laid out across 3 parallel columns (~124dp width each). Event text (title, location, time) heavily overlaps with the doll watermark.
In finalized mode (`isFinalized == true`), the layout is single-column full-width (~380dp), where text on the left and dolls on the right rarely overlap.

**Objective**:
- Keep finalized mode watermark opacity vibrant: `alpha = 0.38f`.
- Increase draft mode watermark transparency (lower alpha to `0.16f`) so text is clearly legible without background distraction or visual conflict.

## 2. Technical Specifications
- **`EventCard.kt`**:
  - `val dollAlpha = if (isFinalized) 0.38f else 0.16f`
  - Pass `alpha = dollAlpha` to the watermark `Image` composable.
- **`app/build.gradle.kts`**:
  - Upgrade to `versionCode = 43`, `versionName = "1.10.17"`.

## 3. Verification Plan
1. Run all unit tests (`gradlew testDebugUnitTest`).
2. Build APK (`CruiseAssistant-v1.10.17.apk`).
3. Deploy to Android emulator and capture screenshots:
   - Finalized 1-lane view: verify `alpha = 0.38f`.
   - Draft 3-lane view: verify `alpha = 0.16f` with crisp overlapping text legibility.
