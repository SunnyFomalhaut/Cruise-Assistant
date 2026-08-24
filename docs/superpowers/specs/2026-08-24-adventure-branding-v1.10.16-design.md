# Design Spec: Disney Adventure Branding & Dinner Hours Update v1.10.16

## 1. Background & Objectives
1. **Dinner Operating Hours Inconsistency**:
   - Several main dining venues in `locations.json` were set to `18:00 - 22:15` or `18:00 - 20:30`, causing false "outside operating hours" validation warnings when creating or editing default 17:45 dinner events.
   - Requirement: Align all dinner opening hours to `17:45`.
2. **App Icon & Visual Branding (Disney Adventure)**:
   - The original launcher icon had generic yellow funnels and white hull.
   - Requirement: Redesign launcher vector icon (`ic_launcher_foreground.xml` and `ic_launcher_background.xml`) to accurately depict the Disney Adventure cruise ship:
     - Iconic red funnels (`#E51821`) with black tops and white Mickey silhouette logo.
     - Multi-tier white superstructure with yellow lifeboat deck accents (`#FFC72C`).
     - Deep navy/black hull (`#161B22`) with gold upper trim and red waterline keel (`#E51821`).
3. **App Chinese Name**:
   - Update string resources to change app name from "邮轮助手" to "探险号助手" (Disney Adventure Assistant).

## 2. Detailed Technical Specifications

### 2.1 Venue Hours & ViewModel Logic
- `app/src/main/assets/locations.json`:
  - `航海家俱乐部 Navigator's Club Restaurant`: dinner slot start `18:00` -> `17:45`.
  - `皮克斯市场餐厅 Pixar Market Restaurant`: dinner slot start `18:00` -> `17:45`.
  - `怪兽亚洲风味 Mike & Sulley's Flavors of Asia`: dinner/snack slots start `18:00` -> `17:45`.
  - `帕洛意大利餐厅 Palo Trattoria`: first dinner slot start `18:00` -> `17:45` (17:45 - 20:00).
- `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`:
  - `MealPeriod.DINNER -> LocalTime.of(17, 45)`.

### 2.2 App Icon Vector Assets
- `app/src/main/res/drawable/ic_launcher_foreground.xml`:
  - Viewport 108x108.
  - Funnels: Twin red funnels with black caps and white Mickey circle silhouettes (`head` circle + 2 `ear` circles).
  - Superstructure: Crisp white multi-deck layers with bridge windows and yellow lifeboat stripes.
  - Hull: Sleek dark navy/black ship hull with gold belt stripe and red keel line over turquoise ocean wave accents.
- `app/src/main/res/drawable/ic_launcher_background.xml`:
  - Premium deep marine gradient (`#0B192C` -> `#1E3E62`) with subtle sea wave overlays.

### 2.3 String Resources & Versioning
- `app/src/main/res/values/strings.xml`: `<string name="app_name">探险号助手</string>`.
- `app/src/main/res/values-zh-rCN/strings.xml`: `<string name="app_name">探险号助手</string>`.
- `app/build.gradle.kts`: `versionCode = 42`, `versionName = "1.10.16"`.

## 3. Verification & Quality Gates
1. Unit tests covering `LocationCatalogTest`, `EventSheetViewModelTest`, and all existing 207 tests.
2. Build and install APK to Android emulator.
3. Capture emulator home screen verifying new icon & app title "探险号助手", and event sheet venue selection without 17:45 warning.
