# 演出卡片候场时间与演出时间双重标注设计规格 (Design Spec)

## 1. 背景与目标

当前版本在时间轴视图（`TimelineScreen` / `EventCard`）中，演出类别事件会附带 30 分钟的提前候场段（Wait Segment）。
在原有实现中：
- 演出本体空间足够时，候场段仅显示 `"候场"`，演出本体段显示开场与结束时间（如 `20:30-21:30 1h`）；
- 演出本体较短（如 15 分钟或 10 分钟演出）时，候场段显示 `"候场 · 09:30"`。这容易给用户造成歧义，误以为 09:30 是候场时间，而非演出开场时间。

**目标**：
在所有场景下，明确区分并双重标注**候场开始时间**（如 `20:00`）与**演出开场/持续时间**（如 `20:30-21:30` 或 `20:30`），避免任何时间理解歧义。

---

## 2. 界面与排版规则

### 2.1 基础计算与格式化
- **候场开始时间（Wait Start Time）**：`formatWaitStart(event)`
  - 格式：`"%02d:%02d"`.format(waitHour, waitMinute)，即演出开场前 30 分钟（如 `20:00`、`09:00`）。
- **演出开场时间（Show Start Time）**：`formatStart(event)`（如 `20:30`、`09:30`）。
- **演出完整时间范围（Show Time Range）**：`formatTimeRange(event)`（如 `20:30–21:30 1h`、`09:30–09:45 15m`）。
- **地点（Location）**：`formatLocation(event.deck, event.zone, event.placeName, compact = true)`（如 `D6 船头 华特迪士尼剧院...`）。

### 2.2 容量评估（Capacity Determination）
卡片被分为**候场区（Wait Segment）**与**演出本体区（Body Segment）**。
根据各自区域的高度（dp），分别评估可容纳行数：

1. **候场区容量 `waitLines`**（基于 `waitSegmentHeightDp` 与 `labelSmall.lineHeight`）：
   - `waitThreeLineMinDp = (labelLineHeight * 3) + 6.dp`（约 46dp~48dp）
   - `waitTwoLineMinDp = (labelLineHeight * 2) + 4.dp`（约 32dp~34dp）
   - 当 `waitSegmentHeightDp >= waitThreeLineMinDp` 时，`waitLines = 3`
   - 当 `waitSegmentHeightDp >= waitTwoLineMinDp` 时，`waitLines = 2`
   - 否则 `waitLines = 1`

2. **演出本体区容量 `bodyLines`**（基于 `bodyHeightDp = heightDp - waitSegmentHeightDp - 2.dp`）：
   - `THREE_LINE_MIN_DP = 58.dp`
   - `TWO_LINE_MIN_DP = 42.dp`
   - 当 `bodyHeightDp >= THREE_LINE_MIN_DP` 时，`bodyLines = 3`
   - 当 `bodyHeightDp >= TWO_LINE_MIN_DP` 时，`bodyLines = 2`
   - 否则 `bodyLines = 1`

### 2.3 四大排版模式与状态映射

| 模式 | 触发判定 | 候场区排版 | 演出本体区排版 |
| :--- | :--- | :--- | :--- |
| **Case 1（长演出）** | `bodyLines == 3` | **1 行**：<br>`{waitStartTime} 候场`<br>*(例: 20:00 候场)* | **3 行**：<br>① `[类别Chip] 演出名称` (含冲突标记)<br>② `地点`<br>③ `演出时间范围` *(例: 20:30–21:30 1h)* |
| **Case 2（短演出/大候场）** | `bodyLines == 1` 且 `waitLines >= 3` | **3 行**：<br>① `{waitStartTime} 候场`<br>② `地点`<br>③ `演出时间范围` *(例: 09:30–09:45 15m)* | **1 行**：<br>① `[类别Chip] 演出名称` (含冲突标记) |
| **Case 3（双行均衡）** | `bodyLines >= 2` 且 `waitLines >= 2` | **2 行**：<br>① `{waitStartTime} 候场`<br>② `地点` | **2 行**：<br>① `[类别Chip] 演出名称` (含冲突标记)<br>② `演出时间范围` *(例: 20:30–21:00 30m)* |
| **Case 4（极限单行）** | `bodyLines == 1` 且 `waitLines == 1` | **1 行**：<br>`{waitStartTime} 候场 · 地点` | **1 行**：<br>`{startTime} 演出名称` (含冲突标记) |
| **过渡态（候场2行+本体1行）** | `bodyLines == 1` 且 `waitLines == 2` | **2 行**：<br>① `{waitStartTime} 候场`<br>② `地点` | **1 行**：<br>`{startTime} 演出名称` (含冲突标记) |
| **过渡态（候场1行+本体2行）** | `bodyLines == 2` 且 `waitLines == 1` | **1 行**：<br>`{waitStartTime} 候场 · 地点` | **2 行**：<br>① `[类别Chip] 演出名称`<br>② `演出时间范围` |

*注：非演出事件（`hasWaitSegment == false`）维持原有 Tier 0 正常事件卡片逻辑不变。*

---

## 3. 代码影响与组件改动

- **`app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`**：
  - 新增 `formatWaitStart(event: CruiseEvent): String` 辅助函数（开场时间减去 30 分钟）。
  - 重构 `EventCard` 内部分级逻辑：计算 `waitLines` 与 `bodyLines`，替代原有的单一 `showTier` 判定。
  - 候场段（Wait Segment）内部根据计算出的候场行数与内容分支进行排版。
  - 演出本体段（Body Segment）根据计算出的本体行数与内容分支进行排版。
  - 单行演出本体包含开场时间时，文本渲染为 `"%02d:%02d %s".format(...)`，并在有冲突时显示 `ConflictBadge`。

---

## 4. 验证计划

1. **单元测试与回归测试**：
   - 运行 Gradle 测试套件，确保现有 timeline/domain 测试全部通过。
   - 编写针对 `EventCard` 内部辅助计算与不同分级分支的逻辑测试（如 `formatWaitStart` 边界、跨午夜情况）。
2. **模拟器 UI 实机验证**：
   - 在 Pixel 9 模拟器 (`emulator-5554`) 运行应用。
   - 检查 Day 2（20:30 回忆/22:30 狮子王烟花）与 Day 3（09:30 海洋奇缘 15m）等界面的卡片排版。
   - 在设置中调整时间轴小时高度（60dp、90dp、120dp、140dp），验证在不同缩放比例和极端极短情况下 4 种排版状态的平滑切换与无重叠裁剪表现。
