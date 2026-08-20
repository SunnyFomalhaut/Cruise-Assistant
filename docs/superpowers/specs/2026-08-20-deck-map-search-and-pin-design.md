# Design: Deck Map Search & Pin Linkage (v1.9.9)

- **Author**: opencode
- **Date**: 2026-08-20
- **Target Version**: v1.9.9 (versionCode 25)

## 1. Overview & Goals

This feature introduces a comprehensive in-map search capability to the Deck Map screen (`DeckMapScreen`):
1. **Search Entry Button**: A search button placed in the left navigation `DeckRail` directly below `D5`.
2. **Interactive Search Sheet**: A `ModalBottomSheet` that opens on tap, with instant real-time search across:
   - **Ship Facilities**: Restaurants, bars, theatres, shops, swimming pools, youth clubs, guest services.
   - **Stateroom / Cabin Numbers**: Intelligent room number parsing (e.g., `11770` -> Deck 11, `6012` -> Deck 6).
   - **Restrooms / Amenities**: Keywords like `厕所`, `洗手间`, `卫生间`, `Restroom`, `WC` listing available restrooms by deck.
3. **Dead-Center Auto-Navigation & Animated Map Pin**:
   - Selecting any search result closes the search sheet, switches the map to the target deck, smoothly scrolls the target directly to the dead center of the screen, and displays a glowing highlight box and an animated map pin (`📍 {Target Name / Room Number}`).

---

## 2. Architecture & Detailed Specifications

### 2.1 Search Entry in `DeckRail` (`DeckMapScreen.kt`)
- In `DeckRail`, add a bottom item below the 14 passenger decks:
  ```kotlin
  Surface(
      shape = RoundedCornerShape(10.dp),
      color = Slate100,
      border = BorderStroke(1.dp, Slate200),
      modifier = Modifier
          .size(48.dp, 40.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable { onOpenSearch() },
  ) {
      Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
      ) {
          Icon(Icons.Outlined.Search, contentDescription = "搜索", tint = CruiseMarine, modifier = Modifier.size(17.dp))
          Text(text = "搜索", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CruiseMarine)
      }
  }
  ```

### 2.2 Search Data & Query Matching (`DeckMapSearch.kt`)
- Model:
  ```kotlin
  enum class DeckSearchResultType { FACILITY, STATEROOM, RESTROOM }

  data class DeckSearchResult(
      val title: String,
      val subtitle: String,
      val deckNumber: Int,
      val highlightQuery: String,
      val type: DeckSearchResultType,
  )
  ```
- Search Logic `searchDeckMap(query: String, currentDeck: Int, allFacilities: List<FacilityItemUiModel>): List<DeckSearchResult>`:
  1. If `query` is empty: show popular quick shortcuts (e.g., 当前甲板洗手间, 华特迪士尼剧院, 宾客服务台, 帕洛, 动画师餐厅).
  2. If `query` contains digits (e.g. `11770`):
     - Extract deck number: for 5-digit room (e.g. `11xxx`), deck is first 2 digits (`11`); for 4-digit room (`6xxx`), deck is first 1 digit (`6`).
     - If deck is valid (5..13, 15..19), add stateroom result `客房 $roomNumber · Deck $deck`.
  3. If `query` matches restroom keywords (`厕所`, `洗手间`, `卫生间`, `restroom`, `wc`):
     - Add restroom options on currentDeck and other decks with `Restroom` query.
  4. Match facility items:
     - Filter `allFacilities` by query matching `placeName` or `formattedLocation`.

### 2.3 `DeckMapSearchSheet` UI
- Modal BottomSheet with an `OutlinedTextField` auto-focused, instant results list in a `LazyColumn`.
- Each item shows icon badge (🏢 for facility, 🚪 for stateroom, 🚻 for restroom), title, subtitle with deck badge.
- Clicking an item triggers `onSelectResult(result.deckNumber, result.highlightQuery)`.

### 2.4 State & Map Navigation
- In `DeckMapViewModel`:
  - `navigateToFacility(deck: Int, target: String)` updates `_selectedDeck.value = deck` and `_highlightTarget.value = target`.

---

## 3. Testing & Verification

1. **Unit Tests**:
   - `DeckMapSearchTest`: Verify stateroom parsing (5-digit `11770` -> Deck 11, 4-digit `6012` -> Deck 6), restroom query matching, and facility keyword matching.
2. **Emulator Verification**:
   - Tap the search button below D5 on DeckRail.
   - Search for room `11770`, tap result -> Deck 11 loads, centers on room 11770 with pin.
   - Search for `洗手间`, tap result -> loads target deck, centers on Restroom with pin.
   - Search for `帕洛`, tap result -> loads Deck 10, centers on Palo Trattoria with pin.
   - Version bump to v1.9.9.
