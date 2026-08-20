# Design: Stateroom Prefix Search & Ranked Auto-Complete (v1.9.10)

- **Author**: opencode
- **Date**: 2026-08-20
- **Target Version**: v1.9.10 (versionCode 26)

## 1. Overview & Requirements

Currently, stateroom search in `DeckMapSearch.kt` only triggers on exact 4 or 5 digit numbers, returning no results for prefixes like `112` or `11`.
This update enables intelligent prefix/partial search across all 2,111 staterooms on Disney Adventure:
1. **Prioritized Result Ranking**:
   - **Rank 1 (Top)**: Matching ship facilities (restaurants, bars, shops, venues) matching the query or deck.
   - **Rank 2 (Followed by)**: All staterooms matching the query prefix (e.g., `112` -> `11200`, `11201`, `11203`, `11207`... `11288`), strictly sorted in ascending numerical order.
   - **Rank 3**: Restrooms / services matching keyword query.
2. **Comprehensive Stateroom Index**:
   - Pre-loaded from `assets/staterooms.json` (2,111 rooms on Decks 9, 10, 11, 12, 13, 15, 16, 17, 18).
3. **Dead-Center Pin & Highlighting**:
   - Tapping any stateroom item immediately switches to the room's deck and places the pulsing highlight and animated map pin (`📍 {roomNumber}`).

---

## 2. Architecture & Data Flow

### 2.1 Stateroom Repository (`StateroomCatalog.kt`)
- Model:
  ```kotlin
  data class StateroomEntry(val roomNumber: String, val deck: Int)
  ```
- Object `StateroomCatalog`:
  - Loads 2,111 staterooms on startup from `assets/staterooms.json`.
  - Exposes `search(prefix: String, limit: Int = 100): List<StateroomEntry>`.
  - Filters by `it.roomNumber.startsWith(prefix)` (or fallback `contains(prefix)` if no prefix matches), sorted numerically by `roomNumber.toInt()`.

### 2.2 Search Engine Ranking (`DeckMapSearch.kt`)
- `searchDeckMap(query: String, currentDeck: Int, ...)`:
  - Facilities matching the query are added first.
  - Staterooms matching digits in `query` (if query contains at least 1-2 digits, e.g. `11` or `112`) are appended in ascending order.
  - Restrooms are added if query matches restroom keywords.

---

## 3. Testing & Verification

1. **Unit Tests**:
   - `StateroomCatalogTest`: Verify loading all 2,111 staterooms, prefix search for `112` returns sorted list starting with `11200`, `11201`..., search for `11` returns all Deck 11 rooms.
   - `DeckMapSearchTest`: Verify searching `11` returns facilities first, then staterooms. Searching `112` returns sorted staterooms `11200..11288`.
2. **Emulator Verification**:
   - Open Deck Map search.
   - Type `112` -> see `11200`, `11201`, `11203`, `11207`, `11208`... listed in order.
   - Type `11` -> see facilities (`城堡典藏`, `帕洛`, `花园酒吧`...) first, then `11100`, `11101`... below.
   - Tap `11216` -> Deck 11 opens, centers on `11216` with pin `📍 11216`.
