# 8-Theme Adaptive Note Styling v1.10.21 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adapt Note card in EventDetailDialog and EventSheet to dynamically use the active CruiseTheme palette across all 8 global color themes.

**Architecture:** Replace hardcoded purple hex colors in `EventDetailDialog.kt` with dynamic theme accessors `CruiseAzureContainer`, `CruiseAzure`, `CruiseMarine`, and update `EventSheet.kt` text field colors.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle Kotlin DSL.

## Global Constraints
- Target version: `versionCode = 47`, `versionName = "1.10.21"`
- Note badge and text in `EventDetailDialog.kt` must dynamically adapt to all 8 themes without hardcoded hex colors.
- 100% of unit tests in `gradlew testDebugUnitTest` must pass.

---

### Task 1: Update Note Styling to Active Theme in `EventDetailDialog.kt` and `EventSheet.kt`

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt`

**Interfaces:**
- Consumes: `CruiseAzureContainer`, `CruiseAzure`, `CruiseMarine`
- Produces: Theme-adaptive Note card in EventDetailDialog and focused border in EventSheet

- [ ] **Step 1: Update `EventDetailDialog.kt`**
```kotlin
                    if (!event.note.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(CruiseAzureContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.EditNote,
                                            contentDescription = null,
                                            tint = CruiseAzure,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.note_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CruiseMarine,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = event.note,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = Slate800,
                                )
                            }
                        }
                    }
```

- [ ] **Step 2: Update `EventSheet.kt` note textfield focused color**
Add `colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CruiseAzure)`.

- [ ] **Step 3: Run unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 4: Commit changes**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt
git commit -m "style(ui): adapt note card styling to 8 global color themes"
```

---

### Task 2: Version Bump, Build & Emulator Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify/Create: `人偶效果截图对比/`

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 47`, `versionName = "1.10.21"`.

- [ ] **Step 2: Build and install APK**
Run `assembleDebug` and install to running emulator.

- [ ] **Step 3: Test on emulator across themes and capture screenshot**
- Open EventDetailDialog under current theme (e.g. Olu Green), verify the Note card badge/label matches the green theme seamlessly.
- Save screenshot to `人偶效果截图对比/05_事件详情备注展示.png`.

- [ ] **Step 4: Commit version bump and screenshots**

```bash
git add app/build.gradle.kts 人偶效果截图对比/
git commit -m "chore: bump version to 1.10.21 (47) and add verification screenshots"
```
