# Finalized Compact Card Layout v1.10.18 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement rich single-line layout for compact cards in finalized mode (CategoryChip + Title + Location + TimeRange) while preserving the simple compact layout for draft mode.

**Architecture:** In `EventCard.kt`, branch `compact && !hasWaitSegment` by `isFinalized`. If `isFinalized == true`, render `CategoryChip`, `Title` (weight fill=false), `Location` (weight fill=false), `formatTimeRange(event)`, and `ConflictBadge`.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 44`, `versionName = "1.10.18"`
- Draft mode compact layout must NOT be modified.
- Finalized mode compact layout must show `[CategoryChip] [Title] [Location] [TimeRange]`.
- Long titles or locations must truncate with `...` while preserving time display.
- 100% of unit tests in `gradlew testDebugUnitTest` must pass.

---

### Task 1: Update Compact Card Layout in `EventCard.kt`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`

**Interfaces:**
- Consumes: `isFinalized: Boolean`, `category: Category?`, `event: CruiseEvent`, `location: String?`
- Produces: Rich single-line row in finalized compact cards

- [ ] **Step 1: Update `EventCard.kt` compact branch**
```kotlin
                if (compact) {
                    if (isFinalized) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            category?.let {
                                CategoryChip(it.name, colorArgb)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (!location.isNullOrBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = location,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = textColor.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formatTimeRange(event),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = textColor.copy(alpha = 0.75f),
                                maxLines = 1,
                            )
                            if (hasConflict) {
                                Spacer(Modifier.width(2.dp))
                                ConflictBadge()
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (hasConflict) {
                                ConflictBadge()
                            }
                            if (!narrow) {
                                Text(
                                    text = formatStart(event),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = textColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 2.dp),
                                )
                            }
                        }
                    }
                }
```

- [ ] **Step 2: Verify compilation and unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt
git commit -m "feat(ui): display category, title, location and time range in finalized compact cards"
```

---

### Task 2: Version Bump, Build & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 44`, `versionName = "1.10.18"`.

- [ ] **Step 2: Build and install APK**
Run `assembleDebug` and install to running emulator.

- [ ] **Step 3: Capture screenshot of finalized mode showing short cards**
Verify that "登船" and "11" display `[重要事项]`, title, location (if any), and time range.

- [ ] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.18 (44) and update verification screenshots"
```
