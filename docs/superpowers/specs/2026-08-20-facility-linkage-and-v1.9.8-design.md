# Design: Facility Guide Linkage, UI Unification, Dining Hours & v1.9.8

- **Author**: opencode
- **Date**: 2026-08-20
- **Target Version**: v1.9.8 (versionCode 24)

## 1. Overview & Requirements

This release implements four key improvements to Cruise Assistant:
1. **Header Entry Swap**: Replace the top-right Pixie Dust entry in `TimelineScreen` with a direct entry to the **Facilities Guide (船上设施指南)** using the `Explore` compass icon. Pixie Dust access remains solely via the bottom-right floating action button (FAB).
2. **Top-Right Button Styling**: Modernize and unify the 3 top-right header action buttons from circular (`CircleShape`) to rounded rectangle boxes (`RoundedCornerShape(10.dp)`, 38×38dp, `Slate100` background, subtle border) to match the app-wide design language.
3. **Main Dining Restaurant Hours Unification**: Clarify and standardize dinner operating hours for the 6 complimentary main dining rotation restaurants in `locations.json`:
   - 17:45 Seating Group (17:45 – 22:00):
     - `动画师餐厅 Animator's Table Restaurant` (Deck 9)
     - `魔法盛夏餐厅 Enchanted Summer Restaurant` (Deck 6)
     - `动画师味觉餐厅 Animator's Palate` (Deck 5)
     - `好莱坞聚光灯俱乐部 Hollywood Spotlight Club` (Deck 8)
   - 18:00 Seating Group (18:00 – 22:15):
     - `皮克斯市场餐厅 Pixar Market Restaurant` (Deck 17)
     - `航海家俱乐部 Navigator's Club Restaurant` (Deck 6)
4. **Facility Guide to Deck Map Navigation & Visual Highlighting (设施与甲板地图联动)**:
   - Click "查看地图 / 定位" on any facility card in `FacilityGuideScreen` to navigate directly to the `DeckMapScreen` with the targeted deck and facility keyword.
   - For multi-deck facilities (e.g., `Walt Disney Theatre` spanning Decks 5, 6, 7; `Palo Trattoria` spanning Decks 10, 11), the primary navigation defaults to the lower / main entrance deck (e.g., Deck 6 for Theatre, Deck 10 for Palo), while the facility card clearly displays all associated decks.
   - `DeckMapScreen` injects JavaScript into the SVG WebView container to locate the facility's SVG text/group node, apply a pulsing highlight beacon/badge, and smoothly scroll/center the venue in the viewport.

---

## 2. Architecture & Detailed Specifications

### 2.1 Top-Right Header Buttons (`TimelineScreen.kt`)
- Change the header action button group:
  - Button 1: **Facilities Guide** (`Icons.Outlined.Explore`, opens `Routes.FACILITIES`, tooltip "船上设施指南").
  - Button 2: **Deck Map** (`Icons.Outlined.Map`, opens `Routes.DECK_MAP`, tooltip "甲板地图").
  - Button 3: **Settings** (`Icons.Outlined.Settings`, opens Settings dialog, tooltip "设置").
- Style:
  ```kotlin
  Surface(
      shape = RoundedCornerShape(10.dp),
      color = Slate100,
      border = BorderStroke(1.dp, Slate200),
      modifier = Modifier.size(38.dp),
  ) {
      IconButton(onClick = ..., modifier = Modifier.fillMaxSize()) {
          Icon(..., tint = ..., modifier = Modifier.size(20.dp))
      }
  }
  ```

### 2.2 Main Dining Schedule Standardization (`locations.json`)
- Clean up messy sub-slot definitions in `locations.json` for the 6 rotation dining rooms:
  - `Animator's Table Restaurant`: `17:45 - 22:00` (dinner, lateNight: true)
  - `Enchanted Summer Restaurant`: `17:45 - 22:00` (dinner, lateNight: true)
  - `Animator's Palate`: `17:45 - 22:00` (dinner, lateNight: true)
  - `Hollywood Spotlight Club`: `17:45 - 22:00` (dinner, lateNight: true)
  - `Pixar Market Restaurant`: `18:00 - 22:15` (dinner, lateNight: true)
  - `Navigator's Club Restaurant`: `18:00 - 22:15` (dinner, lateNight: true)

### 2.3 Facility & Deck Map Data Models (`LocationCatalog.kt`, `FacilityGuide.kt`)
- `LocationCatalogEntry`:
  - `deck: Int?` (primary deck)
  - `additionalDecks: List<Int> = emptyList()` (multi-deck venues)
  - Helper `allDecks: List<Int>` returns `listOfNotNull(deck) + additionalDecks`
- In `FacilityItemUiModel`:
  - `allDecks: List<Int>`
  - `primaryDeck: Int?`
  - `formattedLocation`: e.g. "Deck 6, 7 · 船首" for multi-deck venues.

### 2.4 FacilityCard & Navigation (`FacilityGuideScreen.kt`, `CruiseNavHost.kt`)
- Add a direct Action Button / Chip in `FacilityCard`:
  - Shows `Icons.Outlined.Map` + "查看甲板图".
  - If `facility.primaryDeck != null`, clicking triggers `onNavigateToMap(deck, facility.placeName)`.
- In `CruiseNavHost.kt`:
  - `Routes.DECK_MAP`: route format `deck_map?deck={deck}&highlight={highlight}`.
  - Passes `initialDeck` and `highlightTarget` into `DeckMapViewModel`.

### 2.5 SVG Highlight & Auto-Focus Mechanism (`DeckMap.kt`, `DeckMapScreen.kt`)
- `inlineHtmlWrapperFor(svgContent, highlightTarget: String?)`:
  - If `highlightTarget` is non-empty, inject an initialization script:
    1. Case-insensitively find `<text>` or `<tspan>` containing keywords from `highlightTarget` (e.g. "Walt Disney Theatre" or "Animator").
    2. Compute element's bounding rect / center point.
    3. Dynamically insert a glowing pulse indicator (`<div class="highlight-beacon">` or SVG marker).
    4. Call `window.scrollTo({ top: targetY - windowHeight / 2, left: targetX - windowWidth / 2, behavior: 'smooth' })`.
  - Also provide a reset zoom / clear highlight action in top bar.

---

## 3. Testing & Verification

1. **Unit Tests**:
   - `LocationCatalogLoaderTest`: Verify that 6 dinner restaurants load with exact unified hours.
   - `FacilityGuideTest`: Verify multi-deck formatting and `toFacilityItemUiModel` mapping.
   - `DeckMapTest`: Verify passenger deck listing and HTML wrapper injection with highlight query.
2. **UI & End-to-End Verification on Android Emulator**:
   - Verify top-right buttons are rounded squares (`RoundedCornerShape(10.dp)`).
   - Verify clicking the compass icon opens `FacilityGuideScreen`.
   - Verify searching or selecting "动画师味觉餐厅" or "华特迪士尼剧院", clicking "查看甲板图", immediately opens Deck 5 / Deck 6 with visual pulse beacon centered on the venue.
   - Verify dinner hours on main dining cards show 17:45-22:00 or 18:00-22:15.
   - Verify version bump to v1.9.8.
