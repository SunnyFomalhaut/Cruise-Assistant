# Design Spec: 8-Theme Adaptive Note Styling v1.10.21

## 1. Background & Objective
In v1.10.20, the Note card in `EventDetailDialog.kt` used hardcoded purple hex values (`#EDE9FE`, `#7C3AED`, `#6D28D9`). When users switch the global app theme to any of the other 7 color themes (e.g. Olu Green, LinaBell Pink, Duffy Blue, CookieAnn Yellow, etc.), the purple note badge looks mismatched and clashes with the dialog's other theme-adapted components.

**Objective**:
- Adapt the Note card styling in `EventDetailDialog.kt` and `EventSheet.kt` to dynamically use the active `CruiseTheme` color tokens (`CruiseAzureContainer`, `CruiseAzure`, `CruiseMarine`).
- Ensure all 8 global themes (Judy Blue, Duffy Blue, LinaBell Pink, ShellieMay Pink, StellaLou Purple, Gelatoni Green, Olu Green, CookieAnn Yellow) render harmoniously.

## 2. Technical Specifications
- **`EventDetailDialog.kt`**:
  - Badge container background: `CruiseAzureContainer` (adapts to selected theme's light pastel primaryContainer).
  - Note icon tint: `CruiseAzure` / `CruiseMarine`.
  - "备注" title label: `CruiseMarine` (selected theme's darker accent).
  - Note card container: `Surface(shape = RoundedCornerShape(14.dp), color = Slate100, border = BorderStroke(1.dp, Slate200))` matching the Location card directly above it.
  - Body text: `Slate800` (`#1E293B`, `14.sp`, `lineHeight = 20.sp`).
- **`EventSheet.kt`**:
  - Set `OutlinedTextFieldDefaults.colors(focusedBorderColor = CruiseAzure, focusedLabelColor = CruiseAzure)`.
- **`app/build.gradle.kts`**:
  - Upgrade to `versionCode = 47`, `versionName = "1.10.21"`.

## 3. Verification Plan
- Unit tests pass 100%.
- Deploy to Android emulator.
- Switch theme to Olu Green / LinaBell Pink / Judy Blue in Settings, open EventDetailDialog, and verify that the Note card badge and accents adapt perfectly to the theme.
