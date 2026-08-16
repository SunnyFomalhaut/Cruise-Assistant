# 演出卡片候场与开场时间双重标注实现计划 (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构演出卡片（EventCard）中的时间与地点排版逻辑，消除候场与演出开场时间的歧义，支持四大场景及过渡态下的双重时间与地点智能分发。

**Architecture:** 
- 在 domain 层提供 `formatWaitStart(event: CruiseEvent, waitMinutes: Int = SHOW_WAIT_MINUTES): String` 纯函数。
- 在 UI 层 (`EventCard.kt`)，将卡片分别按候场段高度 `waitSegmentHeightDp` 和演出本体高度 `bodyHeightDp` 评估容量行数（`waitLines`: 1/2/3 行，`bodyLines`: 1/2/3 行），根据组合精确映射至 4 大排版模式。

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Android JUnit.

## Global Constraints

- **候场时间格式**: `"%02d:%02d 候场".format(waitHour, waitMinute)`（例：`20:00 候场`、`09:00 候场`）。
- **演出时间范围格式**: `formatTimeRange(event)`（例：`20:30–21:30 1h`、`09:30–09:45 15m`）。
- **单行组合文案**: 候场单行带地点用圆点分隔 `"%s 候场 · %s".format(waitStart, location)`；演出单行带开场时间用空格分隔 `"%s %s".format(formatStart(event), event.title)`。
- **无破坏性修改**: 非演出类别事件（`hasWaitSegment == false`）维持原有 Tier 0 正常渲染逻辑不变。

---

### Task 1: 候场开始时间格式化函数与单元测试 (Domain Helper & Tests)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt`

**Interfaces:**
- Produces: `fun formatWaitStart(event: CruiseEvent, waitMinutes: Int = SHOW_WAIT_MINUTES): String`

- [ ] **Step 1: 编写 formatWaitStart 单元测试（含跨午夜边界）**

在 `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt` 文件末尾添加：

```kotlin
    @Test
    fun formatWaitStart_regularShow_subtracts30Minutes() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 17),
            startTime = LocalTime.of(20, 30),
            durationMinutes = 60,
            categoryId = 1,
            title = "回忆",
        )
        assertEquals("20:00", formatWaitStart(event))
    }

    @Test
    fun formatWaitStart_morningShow_formatsLeadingZeros() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 18),
            startTime = LocalTime.of(9, 30),
            durationMinutes = 15,
            categoryId = 1,
            title = "海洋奇缘",
        )
        assertEquals("09:00", formatWaitStart(event))
    }

    @Test
    fun formatWaitStart_midnightCrossing_wrapsAroundCleanly() {
        val event = CruiseEvent(
            date = LocalDate.of(2026, 8, 17),
            startTime = LocalTime.of(0, 15),
            durationMinutes = 30,
            categoryId = 1,
            title = "午夜演出",
        )
        assertEquals("23:45", formatWaitStart(event))
    }
```

- [ ] **Step 2: 运行测试验证失败**

运行: `./gradlew testDebugUnitTest --tests "com.cruise.assistant.domain.ShowPresetTest.formatWaitStart*"`
预期: 编译失败 (Unresolved reference: formatWaitStart)

- [ ] **Step 3: 在 ShowPreset.kt 中实现 formatWaitStart 函数**

在 `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt` 末尾添加：

```kotlin
/**
 * 格式化演出开始前的候场时间（默认提前 SHOW_WAIT_MINUTES = 30 分钟）。
 * 例如 20:30 演出返回 "20:00"，09:30 演出返回 "09:00"。
 */
fun formatWaitStart(event: CruiseEvent, waitMinutes: Int = SHOW_WAIT_MINUTES): String {
    val waitMin = ((event.startMinute - waitMinutes) % 1440 + 1440) % 1440
    val hour = waitMin / 60
    val minute = waitMin % 60
    return "%02d:%02d".format(hour, minute)
}
```

- [ ] **Step 4: 运行测试验证通过**

运行: `./gradlew testDebugUnitTest --tests "com.cruise.assistant.domain.ShowPresetTest"`
预期: 全部测试通过 (BUILD SUCCESSFUL)

- [ ] **Step 5: 提交 Task 1 改动**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt
git commit -m "feat(domain): add formatWaitStart helper and unit tests"
```

---

### Task 2: 重构 EventCard 实现双区域自适应排版 (EventCard Refactoring)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`

**Interfaces:**
- Consumes: `formatWaitStart(event)` from `com.cruise.assistant.domain`
- Implements: Case 1 / Case 2 / Case 3 / Case 4 及过渡态的候场段与演出本体段排版

- [ ] **Step 1: 更新 EventCard.kt 内部容量与排版判定逻辑**

在 `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt` 中：
1. 导入 `com.cruise.assistant.domain.formatWaitStart`。
2. 候场区多行高度阈值计算：
   ```kotlin
   val waitTwoLineMinDp = with(LocalDensity.current) {
       (labelLineHeight * 2).toDp() + 4.dp
   }
   val waitThreeLineMinDp = with(LocalDensity.current) {
       (labelLineHeight * 3).toDp() + 6.dp
   }
   val waitLines = when {
       waitSegmentHeightDp >= waitThreeLineMinDp -> 3
       waitSegmentHeightDp >= waitTwoLineMinDp -> 2
       else -> 1
   }
   val bodyLines = when {
       bodyHeightDp >= THREE_LINE_MIN_DP -> 3
       bodyHeightDp >= TWO_LINE_MIN_DP -> 2
       else -> 1
   }
   ```
3. 候场段（Wait Segment）排版：
   - 当 `bodyLines == 3` 时（Case 1）：
     单行显示：`"${formatWaitStart(event)} 候场"`
   - 当 `bodyLines == 1 && waitLines >= 3` 时（Case 2）：
     3 行显示：
     Line 1: `"${formatWaitStart(event)} 候场"`
     Line 2: `location`
     Line 3: `formatTimeRange(event)`
   - 当 `bodyLines >= 2 && waitLines >= 2` 或 `bodyLines == 1 && waitLines == 2` 时（Case 3 及过渡）：
     2 行显示：
     Line 1: `"${formatWaitStart(event)} 候场"`
     Line 2: `location`
   - 当 `waitLines == 1` 时（Case 4 及极限单行）：
     单行显示：
     `location` 非空时为 `"${formatWaitStart(event)} 候场 · $location"`，否则为 `"${formatWaitStart(event)} 候场"`
4. 演出本体段（Body Segment）排版：
   - 当 `!hasWaitSegment` 时（Tier 0）：原有普通事件卡片渲染不变。
   - 当 `bodyLines == 3` 时（Case 1）：
     Line 1: `CategoryChip` + `event.title` + `ConflictBadge`
     Line 2: `location`
     Line 3: `formatTimeRange(event)`
   - 当 `bodyLines == 2` 时（Case 3）：
     Line 1: `CategoryChip` + `event.title` + `ConflictBadge`
     Line 2: `formatTimeRange(event)`
   - 当 `bodyLines == 1` 时：
     - 若 `waitLines >= 3`（Case 2）：候场区已放了时间段，本体单行显示 `CategoryChip`（可选）+ `event.title` + `ConflictBadge`。
     - 若 `waitLines < 3`（Case 4 或过渡）：候场区未放时间段，本体单行显示 `"${formatStart(event)} ${event.title}"` + `ConflictBadge`。

- [ ] **Step 2: 运行所有单元测试**

运行: `./gradlew testDebugUnitTest`
预期: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 Task 2 改动**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt
git commit -m "feat(ui): implement dual capacity slot layout for show wait time and show start time"
```

---

### Task 3: 实机/模拟器测试与 UI 多比例验证 (Emulator UI Verification)

**Files:**
- Verification only

- [ ] **Step 1: 安装并启动应用至 Pixel 9 模拟器 (emulator-5554)**

运行:
```powershell
./gradlew installDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell am start -n com.cruise.assistant/.MainActivity
```

- [ ] **Step 2: 截取并检查时间轴 Day 2 与 Day 3 演出展示**

运行截屏命令并导出图片验证：
1. 切换到 Day 2：
   - 验证 20:30 回忆（60min，本体 3 行）：候场区为 `20:00 候场`，本体为 `回忆 / 地点 / 20:30-21:30 1h`。
   - 验证 22:30 狮子王烟花（10min）：候场区为 `22:00 候场 / 地点`，本体为 `22:30 狮子王烟花`。
2. 切换到 Day 3：
   - 验证 09:30 海洋奇缘（15min）：候场区为 `09:00 候场 / D10 船尾 寻路者湾 / 09:30–09:45 15m`，本体为 `海洋奇缘 Moana...`。

- [ ] **Step 3: 调整时间轴高度缩放测试极端场景**

在设置中将每小时高度调至最小（60dp）以及最大（140dp），分别截屏验证：
- 60dp 极端紧凑下 Case 4 的排版表现（无文字重叠截断）。
- 140dp 展开下 Case 1 / Case 2 / Case 3 的各行文本清晰度。

- [ ] **Step 4: 确认所有功能与视觉无异常**
