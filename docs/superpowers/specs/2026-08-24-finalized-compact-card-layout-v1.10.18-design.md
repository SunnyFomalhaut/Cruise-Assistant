# Design Spec: Enhanced Finalized Compact Card Layout v1.10.18

## 1. Background & Problem Statement
In compact cards (duration < 30min / height < 44dp, e.g. 10m "登船" or "11"):
- In **draft 3-lane mode**, the width is narrow (~124dp), so displaying `[Title] [Time]` on a single line is compact and clean.
- In **finalized 1-lane mode**, the width is full screen (~380dp). Currently, it only displays `Title` on the left and `Time` on the far right, losing vital information such as Category Chip (`[重要事项]`), Location (`D6 船中`), and duration (`10m`).

## 2. Requirements & Goals
1. **Draft Mode (`isFinalized == false`)**:
   - Keep compact 3-lane layout unchanged (`[Title] [StartTime]`).
2. **Finalized Mode (`isFinalized == true`)**:
   - Display a rich single-line layout:
     `[CategoryChip] [Title] [Location] [TimeRange] [ConflictBadge]`
   - Dynamic truncation:
     - `CategoryChip`: intrinsic size (e.g., `[重要事项]`).
     - `Title`: bold, single line with `TextOverflow.Ellipsis`, weight(1f, fill=false).
     - `Location`: formatted location (e.g., `D6 船中`), single line with `TextOverflow.Ellipsis`, weight(1f, fill=false).
     - `TimeRange`: `formatTimeRange(event)` (e.g., `11:50–12:00 10m`), single line, sits cleanly after location.
     - `ConflictBadge`: displayed if `hasConflict == true`.
3. **Version Bump**:
   - `versionCode = 44`, `versionName = "1.10.18"`.

## 3. Implementation Details
- In `EventCard.kt`:
  When `compact && !hasWaitSegment`:
  - If `isFinalized`:
    Render rich row with CategoryChip, Title, Location, and TimeRange.
  - Else:
    Render standard draft compact row with Title and StartTime.
