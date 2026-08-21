# 演出预置下拉与新演出扩充 (v1.10.6) 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在事件编辑弹窗（`EventSheet`）与随心单（`WishlistEditSheet`）中，类别为「演出」时提供预置演出下拉菜单，支持一键联动填充名称、地点与最早场次起止时间，并新增两部预置大秀，版本升级至 `v1.10.6`。

**Architecture:** 
1. 在 `ShowPreset.kt` 中扩充预置演出目录（共 8 部），保持静态注册表模式；
2. 在 `EventSheetViewModel` / `EventSheetState` 中引入 `showPresetMode` 状态与 `onShowPresetSelect` / `onCustomShowSelected` 动作，驱动 UI 在「预置演出下拉」和「自定义演出自由输入」之间无缝切换；
3. 在 `WishlistEditSheet` 中同样为「演出」类别提供下拉选单与地点联动；
4. 更新 `build.gradle.kts` 版本至 `1.10.6` (versionCode 32)。

**Tech Stack:** Kotlin, Jetpack Compose Material3 (`ExposedDropdownMenuBox`), StateFlow, JUnit 4.

## Global Constraints
- `versionCode = 32`, `versionName = "1.10.6"`
- 迪士尼探险号内置演出保持中英文完整标题与场次定义
- 默认时间取该预置演出的最早场次 `startTimes.first()`，结束时间为 `startTimes.first().plusMinutes(durationMinutes)`

---

### Task 1: 扩充预置演出目录（`ShowPreset.kt` + `ShowPresetTest.kt`）

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt`

**Interfaces:**
- Produces: `DISNEY_ADVENTURE_SHOWS` (8 shows, containing `show_baymax_expo` and `show_duffy_friend_ship`)

- [ ] **Step 1: 编写测试用例验证 8 部演出与最早场次时间**

在 `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt` 中添加针对 8 部演出及新增 2 部演出的测试：

```kotlin
@Test
fun disneyAdventureShows_contains8ShowsWithEarliestTimes() {
    val shows = DISNEY_ADVENTURE_SHOWS
    assertEquals(8, shows.size)
    
    val baymax = shows.first { it.key == "show_baymax_expo" }
    assertEquals("大白超级健身世博会 Baymax Super Exercise Expo", baymax.title)
    assertEquals(10, baymax.deck)
    assertEquals(ShipZone.MID, baymax.zone)
    assertEquals("花园舞台", baymax.placeName)
    assertEquals(30, baymax.durationMinutes)
    assertEquals(LocalTime.of(8, 45), baymax.startTimes.first())

    val duffy = shows.first { it.key == "show_duffy_friend_ship" }
    assertEquals("达菲与友谊之船 Duffy and The Friend Ship", duffy.title)
    assertEquals(10, duffy.deck)
    assertEquals(ShipZone.MID, duffy.zone)
    assertEquals("花园舞台", duffy.placeName)
    assertEquals(30, duffy.durationMinutes)
    assertEquals(LocalTime.of(9, 45), duffy.startTimes.first())
}
```

- [ ] **Step 2: 运行测试验证失败**

运行：`.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.ShowPresetTest.disneyAdventureShows_contains8ShowsWithEarliestTimes"`
预期：FAIL（`DISNEY_ADVENTURE_SHOWS` 目前只有 6 部）。

- [ ] **Step 3: 在 `ShowPreset.kt` 中加入新演出定义**

在 `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt` 的 `DISNEY_ADVENTURE_SHOWS` 中添加：

```kotlin
    ShowPreset(
        key = "show_baymax_expo",
        title = "大白超级健身世博会 Baymax Super Exercise Expo",
        deck = 10,
        zone = ShipZone.MID,
        placeName = "花园舞台",
        durationMinutes = 30,
        dayOffsets = listOf(1),
        dailyRangeTo = 4,
        startTimes = listOf(LocalTime.of(8, 45)),
    ),
    ShowPreset(
        key = "show_duffy_friend_ship",
        title = "达菲与友谊之船 Duffy and The Friend Ship",
        deck = 10,
        zone = ShipZone.MID,
        placeName = "花园舞台",
        durationMinutes = 30,
        dayOffsets = listOf(1),
        dailyRangeTo = 4,
        startTimes = listOf(LocalTime.of(9, 45)),
    ),
```

- [ ] **Step 4: 运行全部单元测试验证通过**

运行：`.\gradlew.bat testDebugUnitTest`
预期：PASS。

- [ ] **Step 5: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt
git commit -m "feat(shows): add Baymax Expo and Duffy and The Friend Ship preset shows"
```

---

### Task 2: 状态机与 ViewModel 联动（`EventSheetViewModel.kt`）

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`

**Interfaces:**
- Consumes: `DISNEY_ADVENTURE_SHOWS`, `ShowPreset`
- Produces: `EventSheetState.showPresetMode`, `onShowPresetSelect(preset: ShowPreset)`, `onCustomShowSelected()`

- [ ] **Step 1: 在 `EventSheetState` 中增加 `showPresetMode` 字段**

```kotlin
    /** 是否处于演出预置下拉模式；false = 自由输入 */
    val showPresetMode: Boolean = false,
```

- [ ] **Step 2: 在 `EventSheetViewModel` 中添加状态推断与回调方法**

```kotlin
    fun onShowPresetSelect(preset: com.cruise.assistant.domain.ShowPreset) = _state.update {
        val start = preset.startTimes.first()
        val endMinutes = start.hour * 60 + start.minute + preset.durationMinutes
        val end = LocalTime.of((endMinutes / 60).coerceAtMost(23), endMinutes % 60)
        it.copy(
            title = preset.title,
            deck = preset.deck,
            zone = preset.zone,
            placeName = preset.placeName,
            noLocation = false,
            locationPresetMode = true,
            startTime = start,
            endTime = end,
            showPresetMode = true,
            errorResId = null,
        )
    }

    fun onCustomShowSelected() = _state.update {
        it.copy(showPresetMode = false)
    }
```
并在 `onCategorySelect` 中，当 `iconKey == "show"` 且为内置船时，自动设 `showPresetMode = true`。

- [ ] **Step 3: 运行单元测试**

运行：`.\gradlew.bat testDebugUnitTest`
预期：PASS。

- [ ] **Step 4: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt
git commit -m "feat(event-sheet): add showPresetMode and onShowPresetSelect in ViewModel"
```

---

### Task 3: 改造 `EventSheet.kt` 演出内容为下拉选单

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt`

- [ ] **Step 1: 在 `EventSheet.kt` 中渲染演出下拉框**

当 `state.categoryIconKey == "show" && state.showPresetMode && !state.isSystemEvent` 时：
使用 `ExposedDropdownMenuBox` 展示事件内容，下拉列表中展示 8 部预置演出（包含演出名称与地点摘要），并在底部提供「✍️ 自定义演出」入口。

- [ ] **Step 2: 编译与测试验证**

运行：`.\gradlew.bat testDebugUnitTest`
预期：PASS。

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt
git commit -m "feat(ui): implement show preset dropdown in EventSheet"
```

---

### Task 4: 随心单 `WishlistEditSheet.kt` 适配演出预置下拉

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistEditSheet.kt`

- [ ] **Step 1: 改造 `WishlistEditSheet.kt`**

在随心单编辑弹窗中，当选定类别为「演出」时，事件内容同样提供 8 部预置演出下拉菜单与「✍️ 自定义演出」，选中后自动带入演出名称和地点。

- [ ] **Step 2: 运行测试**

运行：`.\gradlew.bat testDebugUnitTest`
预期：PASS。

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistEditSheet.kt
git commit -m "feat(wishlist): add show preset dropdown in WishlistEditSheet"
```

---

### Task 5: 升级版本号至 v1.10.6 与真机/模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新 `app/build.gradle.kts` 版本号**

`versionCode = 32`, `versionName = "1.10.6"`

- [ ] **Step 2: 构建并安装到模拟器**

运行：`.\gradlew.bat installDebug`
启动 Activity 并截屏验证在新建事件时切到「演出」，标题变为下拉菜单，选中《海洋奇缘》自动变为 09:30-09:45 及对应地点；选中《大白超级健身世博会》自动变为 08:45-09:15；选中《达菲与友谊之船》自动变为 09:45-10:15。

- [ ] **Step 3: 提交并打标**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to v1.10.6 (versionCode 32)"
```
