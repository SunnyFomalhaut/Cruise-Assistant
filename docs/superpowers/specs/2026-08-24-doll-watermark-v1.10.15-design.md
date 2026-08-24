# Design Spec: Duffy Family Card Doll Watermarks v1.10.15 (Dual-Doll & Asset Optimization)

## 1. Background & Problem Statement
In v1.10.14, single-character Duffy & Friends watermarks were introduced for timeline event cards with 3-tier duration-based photo hierarchy (`head`, `half_body`, `full_body`).
However:
1. **APK Bloat**: The 60 uncompressed PNGs added ~56MB, expanding the APK from 15MB to 71MB due to print-grade resolutions (up to 8500x8500px).
2. **Transparent Padding Inconsistencies**: Raw assets had 15%~30% empty transparent margins, causing visual misalignment (some dolls appearing shifted left/right/floating).
3. **Empty Long Finalized Cards**: Long cards (>= 70 minutes) in finalized single-lane view often have ample blank horizontal space on the right, which can beautifully host duo (dual-character) combo art.

## 2. Goals & Key Requirements
1. **Lossless-grade WebP Compression (512px / Q90)**:
   - Trim transparent borders using bounding-box cropping (`img.crop(img.getbbox())`).
   - Resize to max dimension of 512px using Lanczos resampling.
   - Convert to WebP format with alpha channel at Quality 90.
   - Shrink total asset payload from ~75MB to < 2.5MB (APK returning to ~17MB).
2. **Dual-Character Combo Art Library (`duo`)**:
   - Register 8 duo combo assets in `assets/dolls/duo/*.webp`.
   - Implement strict 5-condition trigger:
     1. **Finalized Mode Only**: `isFinalized == true` (single lane).
     2. **High Duration Threshold**: Duration >= 70 minutes (`heightDp >= 140.dp` at 120dp/h).
     3. **Empty Card Layout**: No wait segment (`waitSegmentHeightDp <= 0.dp` & `effectiveWaitMinutes == 0`), not a show category (`category?.iconKey != "show"`), no conflicts (`!hasConflict`).
     4. **Draft & Complex Card Fallback**: If unfinalized (draft mode) or complex (show/wait segment), seamlessly fallback to single-doll `FULL_BODY`.
     5. **Uniform Randomization**: Hash-based deterministic uniform selection across duo assets.
3. **Refined Card Alignment ("Right-Aligned with Inset")**:
   - Since all transparent padding is trimmed to exact pixel boundaries, dolls align cleanly to `Alignment.BottomEnd`.
   - Set consistent insets: `padding(end = 16.dp, bottom = 4.dp)` to prevent edge-sticking and maintain aesthetic balance.
4. **Version Bump & Verification**:
   - Upgrade to `1.10.15 (41)`.
   - Unit tests covering duo trigger conditions, fallback rules, and asset path verification.
   - Emulator verification with visual comparison screenshots.

## 3. Architecture & Data Flow

### 3.1 Asset Processing Pipeline
- Source: `人偶素材/` (83 single-doll images across 7 characters + 8 duo images).
- Script: Python PIL script `scripts/optimize_doll_assets.py`.
- Destination: `app/src/main/assets/dolls/`:
  - `dolls/duffy/{head|half_body|full_body}/*.webp`
  - `dolls/shelliemay/{head|half_body|full_body}/*.webp`
  - `dolls/stellalou/{head|half_body|full_body}/*.webp`
  - `dolls/linabell/{head|half_body|full_body}/*.webp`
  - `dolls/gelatoni/{head|half_body|full_body}/*.webp`
  - `dolls/cookieann/{head|half_body|full_body}/*.webp`
  - `dolls/olumel/{head|half_body|full_body}/*.webp`
  - `dolls/duo/*.webp`

### 3.2 Domain Layer (`DollModels.kt` & `DollManager.kt`)
- `DollTier`: `HEAD`, `HALF_BODY`, `FULL_BODY`, `DUO`.
- `DollCharacter`: Extended with `DUO("duo", R.string.character_duo)`.
- `DollSelectionCriteria`:
  - `cardHeightDp: Float`
  - `durationMinutes: Int`
  - `isFinalized: Boolean`
  - `hasWaitSegment: Boolean`
  - `isShowCategory: Boolean`
  - `hasConflict: Boolean`
- Selection Algorithm:
  - If `isFinalized && durationMinutes >= 70 && !hasWaitSegment && !isShowCategory && !hasConflict`:
    - Select from `DUO` assets.
  - Else:
    - Determine single-doll tier based on card height (<= 61dp -> HEAD, <= 111dp -> HALF_BODY, > 111dp -> FULL_BODY).
    - Select character and asset with tier fallback.

### 3.3 UI Layer (`EventCard.kt` & `LaneCanvas.kt`)
- `LaneCanvas` passes `isFinalized = singleLane` to `EventCard`.
- `EventCard` calculates eligibility and queries `DollManager.selectDollForCard(...)`.
- Rendered with `Alignment.BottomEnd`, `padding(end = 16.dp, bottom = 4.dp)`, `alpha = 0.24f`.

## 4. Testing & Quality Assurance
- `DollManagerTest.kt`:
  - Unit test: verify all asset paths exist in assets folder and decode properly.
  - Unit test: duo trigger conditions (finalized + duration >= 70 + clean layout).
  - Unit test: fallback to single-doll in draft mode, in show category, or when has wait segment.
  - Unit test: single-doll tier thresholds (<= 30m HEAD, 31~55m HALF_BODY, > 55m FULL_BODY).
- UI and Build verification:
  - Assemble Release/Debug APK, verify size drops from 71MB back to ~17MB.
  - Launch on Android Emulator, capture screenshots of draft vs finalized states.
