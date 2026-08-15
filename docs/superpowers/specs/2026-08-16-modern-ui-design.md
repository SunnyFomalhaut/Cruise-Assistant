# Cruise Assistant UI/UX Redesign Specification
**Document ID:** `2026-08-16-modern-ui-design`  
**Date:** 2026-08-16  
**Status:** Approved  
**Design System Paradigm:** Modern Ocean & Soft Air (ui-ux-pro-max standard)

---

## 1. Overview & Goals

Cruise Assistant is a fully offline Android cruise itinerary planning application featuring a 3-lane timeline, conflict-free finalizing, local alarm notifications, and a Pixie Dust gift-exchange tracker.

While the backend domain logic and SQLite/Room architecture are robust, the frontend UI has been built with basic Android defaults. This redesign elevates the entire interface into a modern, clean, ocean-inspired, highly polished travel companion app without altering any underlying business logic, database schemas, or 3-lane layout algorithms.

---

## 2. Design System Tokens & Foundations

### 2.1 Color Palette
* **Primary (Ocean Azure):** `#0284C7` (Color token `CruiseAzure` / `primary`)
* **Primary Dark (Marine Blue):** `#0369A1` (High contrast text, active borders)
* **Primary Container (Sea Foam):** `#E0F2FE` (Subtle highlights, active pill backgrounds)
* **Background:** `#F8FAFC` (Slate Tint off-white surface background)
* **Card Surface:** `#FFFFFF` (Clean elevated white cards)
* **Text Primary:** `#0F172A` (Slate 900 — titles, headers)
* **Text Secondary:** `#334155` (Slate 700 — body text, descriptions)
* **Text Muted:** `#64748B` (Slate 500 — secondary badges, timestamps)
* **Text Faint:** `#94A3B8` (Slate 400 — grid line labels, empty hints)
* **Grid Lines:**
  * **Hour Line:** `#E2E8F0` (1.5dp crisp stroke)
  * **Half-hour Line:** `#F1F5F9` (1dp subtle stroke)
* **Now Indicator:** `#EF4444` (Vibrant Coral Red, 8dp ruler dot + horizontal glowing pulse)
* **Card Background Formula:** `lerp(Color.White, Color(colorArgb), 0.08f)`
* **Card Text Formula:** `lerp(Color(colorArgb), Color(0xFF0F172A), 0.45f)`

### 2.2 Typography
* `titleLarge`: 20sp, FontWeight.Bold, LetterSpacing = -0.3.sp
* `titleMedium`: 16sp, FontWeight.SemiBold
* `titleSmall`: 14sp, FontWeight.SemiBold
* `bodyLarge`: 15sp, FontWeight.Normal
* `bodyMedium`: 14sp, FontWeight.Normal
* `bodySmall`: 12sp, FontWeight.Normal
* `labelLarge`: 13sp, FontWeight.SemiBold
* `labelMedium`: 12sp, FontWeight.Medium
* `labelSmall`: 11sp, FontWeight.Medium

### 2.3 Radii & Elevation
* Small Components (Chips, badges): `RoundedCornerShape(8.dp)`
* Cards (Event cards, banner): `RoundedCornerShape(12.dp)`
* Large Containers (Date pills, keypads, modals): `RoundedCornerShape(16.dp)`
* Buttons: Capsule / `RoundedCornerShape(24.dp)`

---

## 3. Screen & Component Architecture

### 3.1 Timeline Screen (`TimelineScreen.kt`, `DateStrip.kt`, `NowBanner.kt`, `EventCard.kt`)
1. **Top Voyage Bar:**
   * Clean date summary (`8月17日 · 周一`) with Voyage Badge (`🚢 探险号 · 第 1/4 天`).
   * Action Pill Buttons with glassmorphic touch targets (Pixie Dust ✨, Range ⏰, Settings ⚙️).
2. **Date Strip (`DateStrip.kt`):**
   * Capsule cards (`56dp × 64dp`, `RoundedCornerShape(16dp)`).
   * Active day: `#E0F2FE` container + `#0369A1` bold day index + indicator dot.
   * Finalized day: Checkmark badge / border indicator.
3. **Control Bar & Jump-to-Now:**
   * "Jump to Now" filled tonal pill button with live schedule icon.
   * State indicator ("3 车道规划中" vs "已定稿").
   * Primary action button: "生成计划表" / "解锁".
4. **Now Banner (`NowBanner.kt`):**
   * Floating dual-badge banner (`RoundedCornerShape(12dp)`).
   * Ongoing indicator with glowing green/azure dot; Next indicator with subtle timer chip.
5. **Event Cards (`EventCard.kt`):**
   * `12dp` rounded container, 1px matching border (`color.copy(alpha=0.20f)`).
   * Left pill accent (`3.5dp` wide).
   * Category Chip with vector icon, high-contrast bold title.
   * Location badge (`Deck 9 · 船尾剧场`).
   * Duration and time bounds (`13:15 – 13:45 · 30m`).
   * Show wait segment (30m prior) with subtle diagonal gradient hatch and italic badge.
   * Conflict indicator badge (!) in vibrant coral red.

### 3.2 Onboarding Screen (`OnboardingScreen.kt`)
* Hero card with cruise illustration and welcome copy.
* Grouped setting cards:
  1. Ship Selector & Schedule Chips (5D4N, 4D3N).
  2. Date Selection Cards (Boarding Date, Time Slot, Disembark Date).
  3. Deck & Dining Step Controls (`[−] Deck 16 [+]`, Early 17:45 / Late 20:15).
* Primary action button: Full-width elevated gradient pill button.

### 3.3 Pixie Dust Screen (`PixieDustScreen.kt`, `PixieDustKeypad.kt`, `PixieDustLists.kt`)
* Segmented top tab switcher (Given / Received).
* Summary stats card with colored level count dots (`● 3  ● 1`).
* Modern tactile numeric keypad:
  * Large room number display with deck pill.
  * Rounded white key tiles with subtle borders.
  * Level selection chips tinted with level color.
  * Save button transitioning dynamically on valid 3-digit room input.
* Record card streams with room tag, gift level pill, and reply toggle.

### 3.4 Settings & Sheets (`SettingsScreen.kt`, `EventSheet.kt`, `CategorySheet.kt`, `LocationPicker.kt`)
* Grouped setting cards in Material 3 style:
  * Timeline Display Range Card (with slider & preset chips).
  * Reminder Configuration Card (switches + lead time options).
  * Voyage Info & Reset Card (with destructive warning dialog).
  * Category Management.
* Event & Category Modal Bottom Sheets:
  * Icon grid with rounded square containers.
  * Location picker with hierarchical deck/zone filters.
  * Time wheels with clean selection frames.

---

## 4. Verification & Testing
* All 55 existing JVM unit tests must remain 100% passing.
* Build and install to emulator `emulator-5554` via ADB.
* Validate UI rendering, clickability, safe areas, and responsiveness across all screens.
