# 需求规格：点击添加事件自适应屏幕中心就近整点时间 (v1.9.5)

## 1. 目标与背景
用户在时间轴视图（`TimelineScreen`）点击右下角浮动操作按钮（FAB）添加事件时，原逻辑固定以显示范围起点或当前时间的下一刻作为默认开始时间。为了提供更符合直觉的交互体验，新事件的默认开始时间应自适应当前屏幕可见区域中心点所对应的就近整点时间（例如屏幕中心位于 15:30–16:29 之间时，默认时间为 16:00）。
同时将 App 版本号更新至 v1.9.5。

## 2. 详细设计

### 2.1 屏幕中心时间计算
在 `TimelineScreen` 中，时间轴滚动容器为：
```kotlin
Box(Modifier.weight(1f).verticalScroll(scrollState))
```
当用户点击新建事件 FAB 时：
1. **获取视口中心像素位置**：
   `val centerPx = scrollState.value + scrollState.viewportSize / 2f`
2. **转为 dp 偏移**：
   `val centerDp = with(density) { centerPx.toDp().value }`
3. **映射为分钟数**：
   `val centerMinute = state.layout.dpToMinute(centerDp)`
4. **就近取整点（最近的整点）**：
   四舍五入到整点分钟：`val nearestHourMinute = (((centerMinute + 30) / 60) * 60).coerceIn(0, 23 * 60)`
5. **视口未初始化兜底**：
   若 `scrollState.viewportSize <= 0`，回退为 `state.layout.rangeStartMinute` 的就近整点。

### 2.2 核心函数封装与纯函数架构
在 `TimelineLayout.kt` 中添加纯函数扩展：
```kotlin
/**
 * 计算距离给定 dp 偏移最近的整点（以从当天 00:00 起算的分钟数表示，范围 0..1380 即 00:00..23:00）。
 */
fun TimelineLayout.nearestHourAtDp(dp: Float): Int {
    val minute = dpToMinute(dp)
    return (((minute + 30) / 60) * 60).coerceIn(0, 23 * 60)
}
```
并在 `TimelineScreen.kt` 中定义辅助计算：
```kotlin
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

### 2.3 版本号更新
- `app/build.gradle.kts`：
  - `versionCode = 21`
  - `versionName = "1.9.5"`

## 3. 测试策略
1. **单元测试** (`TimelineLayoutTest.kt`)：
   - 验证 `nearestHourAtDp` 在各时间段的取整行为：
     - 15:29 对应 dp -> 15:00 (900 min)
     - 15:30 对应 dp -> 16:00 (960 min)
     - 16:00 对应 dp -> 16:00 (960 min)
     - 16:29 对应 dp -> 16:00 (960 min)
     - 23:45 对应 dp -> 23:00 (1380 min，防越界)
     - 00:15 对应 dp -> 00:00 (0 min)
2. **模拟器交互验证**：
   - 滚动到下午时段（如 16:00 位于屏幕中心），点击 FAB，验证弹出新建卡片的开始时间显示为 `16:00`，结束时间显示为 `16:30`。
   - 滚动到上午时段（如 10:00 位于屏幕中心），点击 FAB，验证弹出卡片开始时间为 `10:00`。
