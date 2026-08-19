# 屏幕中心就近整点添加事件 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 点击时间轴右下角新建事件按钮时，自适应屏幕中心最近的整点作为默认时间，并将版本更新到 1.9.5。

**Architecture:** 
1. 在 `TimelineLayout` 中提供纯函数扩展 `nearestHourAtDp(dp: Float): Int`，基于 `dpToMinute` 进行四舍五入到整点并钳制在 `0..1380`。
2. 在 `TimelineScreen` 中结合 `scrollState.value`、`scrollState.viewportSize` 与 `LocalDensity` 计算视口中心并调用 `onAddEvent`。
3. 更新 `app/build.gradle.kts` 的 `versionCode` 与 `versionName`。

**Tech Stack:** Kotlin, Jetpack Compose, Android SDK.

---

### Task 1: Domain 算法与单测 (TimelineLayout.nearestHourAtDp)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/TimelineLayout.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/TimelineLayoutTest.kt`

- [ ] **Step 1: Write failing unit tests in `TimelineLayoutTest.kt`**

```kotlin
    @Test
    fun nearestHourAtDp_roundsToNearestWholeHour() {
        // 420..1440 (07:00..24:00), 60dp/hr -> 1dp = 1min, topDp of 07:00 is 0dp
        val layout = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        // 07:00 is minute 420 -> dp 0
        assertEquals(7 * 60, layout.nearestHourAtDp(0f))
        // 15:29 is minute 929 -> minuteToDp(929) -> nearest 15:00 (900)
        assertEquals(15 * 60, layout.nearestHourAtDp(layout.minuteToDp(15 * 60 + 29)))
        // 15:30 is minute 930 -> minuteToDp(930) -> nearest 16:00 (960)
        assertEquals(16 * 60, layout.nearestHourAtDp(layout.minuteToDp(15 * 60 + 30)))
        // 16:00 is minute 960 -> minuteToDp(960) -> nearest 16:00 (960)
        assertEquals(16 * 60, layout.nearestHourAtDp(layout.minuteToDp(16 * 60)))
        // 16:29 is minute 989 -> minuteToDp(989) -> nearest 16:00 (960)
        assertEquals(16 * 60, layout.nearestHourAtDp(layout.minuteToDp(16 * 60 + 29)))
        // 23:45 is minute 1425 -> clamped to 23:00 (1380)
        assertEquals(23 * 60, layout.nearestHourAtDp(layout.minuteToDp(23 * 60 + 45)))
    }
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement `nearestHourAtDp` in `TimelineLayout.kt`**

```kotlin
/**
 * 计算距离给定 dp 偏移最近的整点（以从当天 00:00 起算的分钟数表示，范围 0..1380 即 00:00..23:00）。
 */
fun TimelineLayout.nearestHourAtDp(dp: Float): Int {
    val minute = dpToMinute(dp)
    return (((minute + 30) / 60) * 60).coerceIn(0, 23 * 60)
}
```

- [ ] **Step 4: Run test to verify it passes**

---

### Task 2: UI 集成 (TimelineScreen FAB 点击计算)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`

- [ ] **Step 1: Update `defaultStartMinute` in `TimelineScreen.kt`**

```kotlin
/** FAB 默认开始时间：就近屏幕可视区域中心点的整点时间 */
fun defaultStartMinute(
    layout: TimelineLayout,
    scrollPx: Int,
    viewportHeightPx: Int,
    density: Density,
): Int {
    if (viewportHeightPx <= 0) {
        return (((layout.rangeStartMinute + 30) / 60) * 60).coerceIn(0, 23 * 60)
    }
    val centerDp = with(density) { (scrollPx + viewportHeightPx / 2f).toDp().value }
    return layout.nearestHourAtDp(centerDp)
}
```

并在 FAB 的 `onClick` 中调用：
```kotlin
val density = LocalDensity.current
// ...
FloatingActionButton(
    onClick = {
        val startMin = defaultStartMinute(
            layout = state.layout,
            scrollPx = scrollState.value,
            viewportHeightPx = scrollState.viewportSize,
            density = density,
        )
        onAddEvent(startMin)
    },
    containerColor = CruiseAzure,
    contentColor = Color.White,
    shape = RoundedCornerShape(16.dp),
) {
    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
}
```

- [ ] **Step 2: Run tests**

---

### Task 3: 版本号更新到 1.9.5 与模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Bump versionCode to 21, versionName to "1.9.5"**
- [ ] **Step 2: Run `gradlew.bat testDebugUnitTest` and `gradlew.bat installDebug`**
- [ ] **Step 3: Verify on emulator that scrolling to 16:00 and clicking FAB opens with 16:00**
