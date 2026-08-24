# Duffy Family Card Doll Watermarks v1.10.15 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement WebP asset optimization (auto-crop transparent padding + max 512px + WebP Q90) to reduce APK size by ~54MB, add dual-doll combo watermarks (`duo`) for long empty finalized cards with strict 5-condition triggers, and refine right-offset visual alignment.

**Architecture:** Python PIL script crops transparent bounding boxes and compresses assets to WebP in `app/src/main/assets/dolls/`. Domain layer (`DollModels.kt`, `DollManager.kt`) registers the expanded 91-asset pool and evaluates 5-point criteria for duo triggering vs single-doll fallback. Compose UI (`EventCard.kt`, `LaneCanvas.kt`) passes finalization/complexity context and positions watermarks with consistent `16.dp` right insets.

**Tech Stack:** Kotlin, Jetpack Compose, Android SDK 26+, Android BitmapFactory, Python 3 (PIL/Pillow), Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 41`, `versionName = "1.10.15"`
- All asset files must be WebP format, max dimension <= 512px, lossless bounding-box cropped.
- Opacity remains subtle at $\alpha = 0.24f$.
- Dual-doll triggers ONLY in finalized mode (`isFinalized == true`), duration >= 70min, without wait segment, not a show category, without conflict.
- Backward compatibility: All existing tests in `DollManagerTest.kt` must continue passing or be updated for WebP paths.

---

### Task 1: Batch Asset Auto-Crop, Scale & WebP Conversion Pipeline

**Files:**
- Create: `scripts/optimize_doll_assets.py`
- Modify: `app/src/main/assets/dolls/` (all subdirectories)

**Interfaces:**
- Consumes: Raw PNGs in `人偶素材/` (83 single-character images across 7 characters + 8 dual-character images)
- Produces: 91 optimized WebP images in `app/src/main/assets/dolls/{duffy,shelliemay,stellalou,linabell,gelatoni,cookieann,olumel,duo}/`

- [ ] **Step 1: Write asset optimization script**

```python
# scripts/optimize_doll_assets.py
import os
import shutil
from PIL import Image

SRC_DIR = "人偶素材"
DEST_DIR = "app/src/main/assets/dolls"

FOLDER_MAP = {
    "达菲": "duffy",
    "雪莉玫": "shelliemay",
    "星黛露": "stellalou",
    "玲娜贝儿": "linabell",
    "杰拉多尼": "gelatoni",
    "可琦安": "cookieann",
    "奥乐米拉": "olumel",
    "双人组合": "duo",
}

def process_all():
    if os.path.exists(DEST_DIR):
        shutil.rmtree(DEST_DIR)
    os.makedirs(DEST_DIR, exist_ok=True)

    total_orig = 0
    total_opt = 0
    count = 0

    for root, _, files in os.walk(SRC_DIR):
        rel = os.path.relpath(root, SRC_DIR)
        parts = rel.split(os.sep)
        char_zh = parts[0]
        if char_zh not in FOLDER_MAP:
            continue
        char_en = FOLDER_MAP[char_zh]
        subfolder = parts[1] if len(parts) > 1 else ""

        target_folder = os.path.join(DEST_DIR, char_en, subfolder) if subfolder else os.path.join(DEST_DIR, char_en)
        os.makedirs(target_folder, exist_ok=True)

        for f in files:
            if f.lower().endswith(('.png', '.webp', '.jpg')):
                src_path = os.path.join(root, f)
                total_orig += os.path.getsize(src_path)

                base_name = os.path.splitext(f)[0]
                dest_path = os.path.join(target_folder, f"{base_name}.webp")

                with Image.open(src_path) as img:
                    img = img.convert("RGBA")
                    bbox = img.getbbox()
                    if bbox:
                        img = img.crop(bbox)
                    img.thumbnail((512, 512), Image.Resampling.LANCZOS)
                    img.save(dest_path, format="WEBP", quality=90, method=4)

                total_opt += os.path.getsize(dest_path)
                count += 1

    print(f"Processed {count} images.")
    print(f"Original size: {total_orig / (1024*1024):.2f} MB")
    print(f"Optimized size: {total_opt / (1024*1024):.2f} MB ({(total_opt/total_orig)*100:.2f}%)")

if __name__ == "__main__":
    process_all()
```

- [ ] **Step 2: Run optimization script**

Run: `python scripts/optimize_doll_assets.py`
Expected: Processed 91 images, total size <= 2.5 MB.

- [ ] **Step 3: Verify output integrity**

Run: `python -c "import os; print(len([os.path.join(r, f) for r, _, fs in os.walk('app/src/main/assets/dolls') for f in fs if f.endswith('.webp')]))"`
Expected: 91

- [ ] **Step 4: Commit assets and script**

```bash
git add scripts/optimize_doll_assets.py app/src/main/assets/dolls/
git commit -m "feat(assets): crop transparent padding and convert doll assets to WebP (512px Q90)"
```

---

### Task 2: Domain Layer Extension (`DollModels.kt` & `DollManager.kt`)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/DollModels.kt`
- Modify: `app/src/main/java/com/cruise/assistant/domain/DollManager.kt`

**Interfaces:**
- Consumes: 91 WebP assets in `app/src/main/assets/dolls/`
- Produces:
  - `DollTier.DUO`
  - `DollCharacter.DUO`
  - `DollManager.selectDollForCard(eventId: Long, cardHeightDp: Float, durationMinutes: Int, isFinalized: Boolean, isComplex: Boolean): String?`
  - `DollManager.loadDollBitmap(context: Context, assetPath: String, targetSize: Int): Bitmap?`

- [ ] **Step 1: Update `DollModels.kt`**
Add `DUO` to `DollTier` and `DollCharacter`, and define card selection criteria helper.

- [ ] **Step 2: Update `DollManager.kt`**
Register all 91 WebP asset paths, register the `DUO` pool, and implement the dual-doll selection algorithm with single-doll fallback.

- [ ] **Step 3: Commit domain layer changes**

```bash
git add app/src/main/java/com/cruise/assistant/domain/DollModels.kt app/src/main/java/com/cruise/assistant/domain/DollManager.kt
git commit -m "feat(domain): register duo doll assets and add 5-condition trigger selection logic"
```

---

### Task 3: Unit Testing (`DollManagerTest.kt`)

**Files:**
- Modify: `app/src/test/java/com/cruise/assistant/domain/DollManagerTest.kt`

**Interfaces:**
- Consumes: `DollManager` selection functions
- Produces: 100% passing tests for thresholds, duo triggers, fallbacks, and asset path validity.

- [ ] **Step 1: Write failing/updated unit tests**
Add test cases for:
1. All asset paths in `DollManager` end with `.webp` and are non-empty.
2. Finalized + 75min + clean card triggers `DUO` asset.
3. Draft mode + 75min triggers `FULL_BODY` single doll (not DUO).
4. Show category / wait segment + 75min triggers `FULL_BODY` single doll (not DUO).
5. Conflict + 75min triggers `FULL_BODY` single doll (not DUO).
6. 30min card triggers `HEAD` single doll.
7. 45min card triggers `HALF_BODY` single doll.

- [ ] **Step 2: Run unit tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: All tests PASS.

- [ ] **Step 3: Commit unit tests**

```bash
git add app/src/test/java/com/cruise/assistant/domain/DollManagerTest.kt
git commit -m "test: add comprehensive unit tests for duo trigger, fallback, and WebP assets"
```

---

### Task 4: UI Layout & Card Alignment Refinement (`EventCard.kt` & `LaneCanvas.kt`)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt`

**Interfaces:**
- Consumes: `isFinalized: Boolean` from `LaneCanvas` (`singleLane`)
- Produces: Cleanly inset doll watermarks (`padding(end = 16.dp, bottom = 4.dp)`), accurate dual/single doll selection.

- [ ] **Step 1: Update `EventCard.kt` parameter and selection logic**
Add `isFinalized: Boolean = false` parameter to `EventCard`.
Compute `isComplex = (hasWaitSegment || (category?.iconKey == "show") || hasConflict)`.
Call `DollManager.selectDollForCard(event.id, heightDp.value, event.durationMinutes, isFinalized, isComplex)`.
Set `Image` padding to `padding(end = 16.dp, bottom = 4.dp)`.

- [ ] **Step 2: Update `LaneCanvas.kt`**
Pass `isFinalized = singleLane` to `EventCard`.

- [ ] **Step 3: Commit UI changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt
git commit -m "feat(ui): pass finalization state to EventCard and adjust right-inset padding to 16dp"
```

---

### Task 5: Version Bump, APK Size Check & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 41`, `versionName = "1.10.15"`.

- [ ] **Step 2: Run build and verify APK size reduction**
Run assembleDebug, check size of output APK in `app/build/outputs/apk/debug/`.
Verify APK size is <= 20 MB (down from 71 MB).

- [ ] **Step 3: Install APK on Android Emulator and capture screenshots**
Capture finalized mode screenshot showing duo art on dinner and single dolls on other cards.
Capture draft mode screenshot showing fallback to single dolls.

- [ ] **Step 4: Commit build and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.15 (41) and update verification screenshots"
```
