# Design: Finalized Event Detail Dialog & Map Location Linkage

- **Author**: opencode
- **Date**: 2026-08-20
- **Target Version**: v1.10.4 (versionCode 30)

## 1. Overview & Goals

When a cruise schedule is finalized (`isFinalized == true`), guest activities should be locked for editing while remaining convenient to view and explore. This feature replaces the inactive click on finalized event cards with an interactive, beautifully designed **Event Detail Dialog (`EventDetailDialog`)**.

Key deliverables:
1. **Rich Event Information Display**:
   - Event Title & Category Chip (with icon and custom category color).
   - Time range & duration calculation (e.g., `17:45 – 19:15 (1h 30m)`).
   - Waiting/boarding recommendation hint for shows (e.g., `17:15 建议候场 (提前30分钟)`).
   - Formatted location with deck, zone, and venue name.
   - User notes / dining reminders (if present).
2. **Interactive Map Linkage ("📍 定位")**:
   - For events with a deck or venue name, a prominent "定位" button directly navigates to `DeckMapScreen`, switching to the matching deck and highlighting/focusing the venue pin.
3. **Future-Proof Poster/Watermark Architecture**:
   - Extensible background slot for subtle translucent poster illustrations (e.g. Disney official dining and theater posters).
4. **Version Bump**: `v1.10.4` (versionCode 30).

---

## 2. Architecture & Components

### 2.1 New Component: `EventDetailDialog.kt`
- Location: `app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt`
- Signature:
  ```kotlin
  @Composable
  fun EventDetailDialog(
      event: CruiseEvent,
      onNavigateToMap: (deck: Int, target: String) -> Unit,
      onDismiss: () -> Unit,
  )
  ```
- Visual elements:
  - Dialog container: `Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Slate200))`
  - Category Badge: `CategoryChip(category = event.category)`
  - Title: Large bold heading.
  - Time Section: Clock icon + start/end time + duration string.
  - Waiting Section (if show): AutoAwesome / Schedule icon + waiting time badge.
  - Location Section: Location pin icon + formatted location text + "定位" Button.
  - Note Section (if present): Text in a rounded light slate container.
  - Dismiss: Top-right close icon + bottom "关闭" button.

### 2.2 TimelineScreen Integration
- In `TimelineScreen.kt`:
  - Introduce `var detailEvent by remember { mutableStateOf<CruiseEvent?>(null) }`.
  - When `onEventClick(event)` is fired:
    - If `state.isFinalized`: set `detailEvent = event`.
    - If `!state.isFinalized`: call `onEditEvent(event)`.
  - When `detailEvent != null`:
    ```kotlin
    EventDetailDialog(
        event = detailEvent!!,
        onNavigateToMap = { deck, target ->
            detailEvent = null
            onNavigateToMap(deck, target)
        },
        onDismiss = { detailEvent = null },
    )
    ```

### 2.3 CruiseNavHost Routing
- In `CruiseNavHost.kt`:
  - Pass `onNavigateToMap = { deck, venue -> navController.navigate(Routes.deckMap(deck, venue)) }` to `TimelineScreen`.

---

## 3. Verification Plan

1. **Unit Tests**:
   - Verify timeline event click behavior and navigation parameters.
2. **Emulator Verification**:
   - Finalize schedule on Day 3.
   - Click on a finalized event card (e.g. "海洋奇缘 Moana: Call of the Sea" on Deck 10 or "晚餐" on Deck 6).
   - Verify that the `EventDetailDialog` pops up with full details.
   - Click "定位" and verify that it opens `DeckMapScreen` focused on Deck 10 / Deck 6 with the pin highlighted.
