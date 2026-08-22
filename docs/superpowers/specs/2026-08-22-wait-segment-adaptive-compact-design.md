# 极限缩放下候场段高度自适应与防裁切规格 (Design Spec)

**Date:** 2026-08-22  
**Target Version:** 1.10.13

---

## 1. 目标与背景

在时间轴最小高度（`60dp/h`）或短候场时间（10~15 分钟）的极限场景下，15 分钟候场段按比例换算高度仅为 `15.dp`。
由于卡片等待段内默认存在 `2.dp` 纵向 padding（占用 4dp），加上 `labelSmall` 字体在部分机型上的行高与上下边距，导致文字顶部被轻微裁切。

本优化在保留常规缩放（如 120dp/h）原有布局表现的前提下，专门针对此类极限压缩场景进行自适应优化，确保文字完整显示。

---

## 2. 详细设计

### 2.1 `LaneCanvas.kt` 候场段最小高度保底
在计算 `waitHeightDp` 时，为非零等待段提供 `16.dp` 的最小保底高度：
```kotlin
val waitHeightDp = if (isShow) {
    val band = (layout.minuteToDp(effStart + event.effectiveWaitMinutes) - top).dp
    val minWaitHeight = if (band > 0.dp) 16.dp else 0.dp
    band.coerceAtLeast(minWaitHeight)
        .coerceAtMost(heightDp - TITLE_RESERVE_DP - SPACER_DP)
        .coerceAtLeast(0.dp)
} else {
    0.dp
}
```

### 2.2 `EventCard.kt` 自适应紧凑字体与内边距调整
1. 判定条件：`val isExtremelyCompactWait = waitSegmentHeightDp < 20.dp`
2. 等待段内边距：`vertical = if (isExtremelyCompactWait) 0.dp else 2.dp`
3. 等待段字体样式：
```kotlin
val waitSingleLineStyle = if (isExtremelyCompactWait) {
    MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.5.sp,
        lineHeight = 11.sp,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    )
} else {
    MaterialTheme.typography.labelSmall
}
```
4. 将 Case 1 / Case 4 单行候场文本应用 `waitSingleLineStyle`，并保持 `verticalArrangement = Arrangement.Center`。

---

## 3. 验证计划
1. 运行 `./gradlew testDebugUnitTest` 确保全部测试通过。
2. 在模拟器中将缩放调至 60dp/h，验证 Day 1 扬帆起航（15 分钟候场段）的 `13:00 候场 · D10 船中 花园舞台` 文本顶部完整显示，无裁切。
3. 验证 120dp/h 默认比例下，所有演出卡片与候场段仍保持原有样式与标准字号。
