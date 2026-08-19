# 邮轮助手 (Cruise Assistant) — 设计规格

日期：2026-08-04
状态：已确认，待实施

## 1. 背景与目标

用户下月开始一次邮轮旅行，现处于行前规划阶段。需要一个纯本地的 Android 应用，在
**准备阶段**用来编排每天的行程（会频繁增删改、且经常出现时间冲突需要并排比较），在
**航行阶段**变成一个只读的当日日程表并按时提醒。

船上无稳定网络，因此应用必须 100% 离线可用、无任何网络依赖、无账号。

单用户、单设备、单航次。不做同步、不做分享、不做统计报表。

## 2. 核心概念

### 2.1 两种视图模式（按天独立）

每一天有独立的 `finalized` 状态：

| 状态 | 视图 | 交互 |
|---|---|---|
| 规划中 (planning) | **3 栏**并排布局 | 可增删改、可换栏 |
| 已定稿 (finalized) | **单栏**满宽 | 只读；可"解锁"退回规划中 |

日期条上用实心/空心圆点区分。定稿是逐天进行的，用户可以边规划后面几天、边把前面几天定稿。

### 2.2 3 栏布局（准备阶段的关键设计）

事件区域横向分 3 栏，**栏1 与 栏2 等宽偏左，栏3 明显更窄且贴最右边**。
目的：添加事件时若时间冲突，自动错开并排放置，互不遮挡，方便对比取舍。

```
│        ┌─────────┐ ┌─────────┐ ┌────┐ │
│ 13:00  │ 栏0 (宽) │ │ 栏1 (宽) │ │栏2 │ │
│        └─────────┘ └─────────┘ └────┘ │
      ← 权重 1.0  →  ← 权重 1.0 → ←0.45→
```

**分栏规则**：新增事件时依次尝试栏0 → 栏1 → 栏2，取第一个与该栏已有事件**时间不重叠**的栏。
三栏都冲突时放栏2 并在卡片上打冲突角标。长按卡片可手动换栏。

定稿后的单栏视图忽略 lane 字段，按开始时间顺序满宽渲染。

## 3. 数据模型

Room 数据库 `cruise.db`，version 1。所有时间用 `java.time`（minSdk 26 原生支持）。

### VoyageEntity（单行，id 固定为 1）
```
id: Int = 1
shipName: String?              // 可选
boardDate: LocalDate           // 上船日期
boardTime: LocalTime           // 上船时间（引导页选择）
disembarkDate: LocalDate       // 下船日期
disembarkTime: LocalTime       // 下船时间，默认 07:00
maxDeck: Int = 20              // 最高层数，地点选择器用
```

### CategoryEntity（事项/图标）
```
id: Long (auto)
name: String
colorArgb: Int
iconKey: String                // 映射到内置矢量图标表
parentId: Long?                // 二级分类（截图3 的"二级分类"开关）
isBuiltIn: Boolean
isArchived: Boolean            // 截图3 的"归档"
sortOrder: Int
```

### EventEntity
```
id: Long (auto)
date: LocalDate
startTime: LocalTime
durationMinutes: Int           // >0
categoryId: Long
title: String                  // 第1行：事件内容
deck: Int?                     // 楼层，null = 不设地址
zone: ShipZone?                // BOW(船头)/MID(船中)/STERN(船尾)
placeName: String?             // 地点名，如"花园"
lane: Int                      // 0/1/2
recurrenceGroupId: String?     // 同一循环组共享
reminderMinutesBefore: Int?    // null = 用全局默认
isSystemGenerated: Boolean     // 上船/下船事件标记
```

第2行显示规则：`deck` 与 `zone` 都有时显示 `7F船中花园`（placeName 可空 → `7F船中`）；
`deck` 为 null 时整行不显示（上船事件即为此情形）。

### DayPlanEntity
```
date: LocalDate (PK)
finalized: Boolean
finalizedAt: Long?
```

### LocationPresetEntity（常用地点记忆）
```
id: Long (auto)
deck: Int
zone: ShipZone
placeName: String
lastUsedEpochMs: Long
useCount: Int
```
唯一索引 (deck, zone, placeName)。保存事件时 upsert。地点名输入框下拉按 `useCount DESC, lastUsedEpochMs DESC` 展示。

### DataStore（Preferences）设置项
```
lastViewedDate: String?        // 需求1：优先用它，探测不到用系统当日
displayStartMinute: Int = 420  // 07:00
displayEndMinute: Int = 1440   // 24:00
hourHeightDp: Int = 60         // 缩放
collapseEmptyGaps: Boolean = false
defaultReminderMinutes: Int = 15
notificationsEnabled: Boolean = true
onboardingCompleted: Boolean = false
```

## 4. 界面

### 4.1 引导页（首次打开，需求7）

仅当 `onboardingCompleted == false` 时显示。

字段：船名（可选）→ 上船日期 → 上船时间 → 下船日期 → 最高层数（默认20）。
校验：下船日期 ≥ 上船日期；同日航次也允许。

点"生成时间轴"后：
1. 写入 `VoyageEntity`
2. 为 `boardDate..disembarkDate` 每天创建 `DayPlanEntity(finalized=false)`
3. 播种 6 个内置 `CategoryEntity`
4. 插入 2 个系统事件：
   - 上船：`date=boardDate, startTime=boardTime, duration=15, title="上船", deck=null, zone=null, category=重要事项, isSystemGenerated=true`
   - 下船：`date=disembarkDate, startTime=07:00, duration=30, title="下船", deck=null, category=重要事项, isSystemGenerated=true`
5. `onboardingCompleted = true`，跳时间轴主页并定位到 `boardDate`

### 4.2 时间轴主页（需求1–5，核心）

```
┌──────────────────────────────────────────┐
│ 8月17日 周日   Day 3/6          ⏱   ⚙    │
├──────────────────────────────────────────┤
│ ●1  ●2  ○3  ○4  ○5  ○6                   │  ● 已定稿 ○ 规划中
├──────────────────────────────────────────┤
│ 进行中 · 午餐  12:15–12:45                │  仅当 date == today
│ 下一个 · 演出  13:15  9F船尾剧场           │
├──────────────────────────────────────────┤
│         ┌────────┐ ┌────────┐ ┌────┐    │
│  13:00  │▎演出  ⋮│ │▎娱乐   │ │▎琐 │    │
│         │扬帆起航 │ │游泳     │ │事  │    │
│         │9F船尾剧场│ │11F船中泳│ │    │    │
│         │13:15–13:│ │13:20 1h│ │13:3│    │
│ ━━━━━━━━━━━━ 现在 13:02 ━━━━━━━━━━━━━━  │  红线，仅今天
│  14:00  └────────┘ └────────┘ └────┘    │
│                                      ⊕   │
└──────────────────────────────────────────┘
```

- 启动日期 = `lastViewedDate` ?: 系统当日；若该日期不在航次范围内，取最近的边界日
- 左侧固定宽度时间刻度列，整体垂直滚动
- 左右滑动 / 点日期条切换日期
- 卡片三行（截图1 的结构）：
  1. `[类别chip] 事件内容`
  2. `7F船中花园`（无 deck 时省略此行）
  3. `13:15–13:45  30m`（跨行 <44dp 时压缩为单行 `事件内容 · 13:15 30m`）
- 卡片左边一条 4dp 圆角色条，卡片底色为类别色 12% alpha，文字为类别色深化版
- 点击 → 编辑弹窗；长按 → 菜单（换到栏0/1/2、编辑、删除）
- 系统生成事件（上船/下船）不可删除，只能改时间
- FAB `⊕` → 添加事件弹窗，默认开始时间 = 当前滚动位置对应的整刻
- 已定稿的日期：无 FAB、卡片只读、顶部显示"已定稿 · 解锁"按钮
- 顶栏 `⏱` → 显示时间范围面板；`⚙` → 设置页

### 4.3 添加/编辑事件弹窗（需求6，基于截图2）

底部 ModalBottomSheet：

```
补录时间 / 添加事件                      ✕
────────────────────────────────────────
选择事项
 [🍽]  [▶]  [⚠]  [▦]  [🏃]  [👥]  [＋]
 餐食  演出 重要  琐事 娱乐 见面会 编辑
────────────────────────────────────────
┌──────────────────────────────────────┐
│ 事件内容                              │
└──────────────────────────────────────┘
────────────────────────────────────────
地点                              ← 新增
┌──────┐  ┌────┐┌────┐┌────┐
│ 7F  ▾│  │船头││船中││船尾│
└──────┘  └────┘└─██─┘└────┘
┌──────────────────────────────────────┐
│ 地点名（可选）                     ▾  │  ← 可输入 / 选历史
└──────────────────────────────────────┘
   ☐ 不设地点
────────────────────────────────────────
开始时间              结束时间
 ‹ 8月15日 ›          ‹ 8月15日 ›
 ┌──────────┐        ┌──────────┐
 │ 13 : 15  │        │ 13 : 45  │
 └──────────┘        └──────────┘
────────────────────────────────────────
 每日同一时间循环                 ◯──   ← 新增
 循环至 8月19日（下船日 8/20 不含）
────────────────────────────────────────
        [      确  认      ]
```

- 事项图标网格末位 `＋` → 打开类别编辑弹窗
- 楼层下拉 `1F..{maxDeck}F`
- 结束时间 ≤ 开始时间 → 视为跨日，禁止并提示（本应用不支持跨日事件）
- 循环开关（需求6 + 用户明确澄清）：从**当前日期**到**下船日前一天**每天生成同一时间的事件；
  **下船日不生成**（下船日只有 07:00 下船一件事）
- 编辑循环组内的事件时，弹确认："仅修改本日" / "修改所有重复项"
- 保存时调用 `assignLane` 决定栏位；编辑时若时间未变则保留原 lane

### 4.4 类别编辑弹窗（需求6，基于截图3，去掉计时模式）

```
添加/编辑图标                            ✕
────────────────────────────────────────
名称            [二级分类 ◯──]  [归档]
┌──────────────────────────────────────┐
│ 输入名称                              │
└──────────────────────────────────────┘
────────────────────────────────────────
时间轴主题色
 ● ● ● ● ● ⬤ ● ●        ← 8 色，当前选中带描边
────────────────────────────────────────
选择图标
 [🔍][▦][🏃][💼][📖][文A][⚠]
 [🗑][⏱][🍽][👥][▶][💪][🐱]
────────────────────────────────────────
        [    保存修改    ]
```

- **不含"计时模式"**（用户明确要求删除）
- "二级分类"开关打开后出现父分类下拉
- 内置类别可改色/改图标，不可删除；自定义类别长按删除
- 删除有事件引用的类别时：提示并把事件迁移到"琐事"

### 4.5 显示时间范围面板（需求2）

顶栏 `⏱` 弹出：

```
显示时间范围
 起  ‹ 07 › : ‹ 00 ›
 止  ‹ 24 › : ‹ 00 ›

每小时高度
 ────────●────────   60dp

 ☐ 自动隐藏无事件的空时段
```

全局生效、持久化。范围外的事件在时间轴外侧显示"↑ 上方还有 2 件"提示条，点击可临时展开全天。
校验：止 > 起，且区间 ≥ 2 小时。

### 4.6 设置页

- 显示时间范围（同 4.5）
- 提醒：总开关 + 默认提前分钟（5/10/15/30/60 或自定义）
- 航次：修改上下船日期/时间、最高层数；"重建航次"（二次确认，清空所有事件）
- 类别管理：列表 + 排序 + 归档
- 关于

## 5. 定稿流程（需求8）

用户在某天点"生成计划表"：

1. 运行 `findConflicts(该日事件)`
2. **有冲突** → 弹窗列出每一对冲突（`演出 13:15–13:45  ⇄  游泳 13:20–14:20`），
   提供"去修改"（关闭弹窗并高亮）/"忽略冲突继续定稿"两个选项
3. **无冲突** → 写 `DayPlanEntity(finalized=true)`，视图切单栏，重排该日提醒

"解锁"按钮把 `finalized` 改回 false，退回 3 栏视图。

## 6. 纯逻辑（单元测试重点）

放在 `domain/` 下，无 Android 依赖，可用 JVM 单测覆盖：

```kotlin
// 需求4：3 栏分配
fun assignLane(startMin: Int, durMin: Int, existing: List<LaneSlot>): Int

// 需求8：冲突检测。重叠条件 startA < endB && startB < endA（相邻不算冲突）
fun findConflicts(events: List<Event>): List<Pair<Event, Event>>

// 需求6：循环展开 —— 从 fromDate 到 disembarkDate 前一天（含），下船日排除
fun expandRecurrence(template: Event, fromDate: LocalDate, disembarkDate: LocalDate): List<Event>

// 需求2：时间 → 垂直偏移（含隐藏空时段的分段映射）
fun minuteToDp(minute: Int, layout: TimelineLayout): Float
fun buildTimelineLayout(rangeStart: Int, rangeEnd: Int, hourHeightDp: Int,
                        events: List<Event>, collapseGaps: Boolean): TimelineLayout

// 地点格式化
fun formatLocation(deck: Int?, zone: ShipZone?, placeName: String?): String?
```

边界用例：durationMinutes=0、跨日、事件完全重合、时间范围外事件、
`collapseEmptyGaps` 开启时的偏移单调性、下船日循环排除。

## 7. 提醒

- `AlarmManager.setExactAndAllowWhileIdle` 逐事件排闹钟 → `ReminderReceiver` → 通知渠道 `cruise_events`
- 通知内容：标题 `类别 · 事件内容`，正文 `13:15 · 9F船尾剧场`，点击打开对应日期
- 重排触发点：事件增删改、设置变更、定稿/解锁、`BOOT_COMPLETED`、App 启动
- 只为**未来**事件排闹钟；上限 100 个最近的（超出的在 App 启动时补排）
- 权限：`POST_NOTIFICATIONS`（运行时请求）、`SCHEDULE_EXACT_ALARM`、`RECEIVE_BOOT_COMPLETED`
- 精确闹钟不可用时降级为 `setWindow`，并在设置页提示

应用内横幅（需求：进行中/下一个）只在查看当天时显示，每分钟 tick 刷新。

## 8. 内置类别配色

| 类别 | 颜色 | iconKey |
|---|---|---|
| 餐食 | `#34A853` 绿 | `meal` |
| 演出 | `#F5B301` 琥珀 | `show` |
| 重要事项 | `#E8563F` 朱红 | `important` |
| 琐事 | `#4A7DFF` 蓝 | `chore` |
| 娱乐项目 | `#9B5DE5` 紫 | `fun` |
| 见面会 | `#00A5A5` 青 | `meetup` |

调色板（截图3 的 8 色）：珊瑚红 `#EE6C7A`、橙 `#F2994A`、黄 `#F2C94C`、
绿 `#4ECD8F`、青 `#45C4B8`、蓝 `#5B8DEF`、紫 `#A97BF0`、深灰蓝 `#4A5568`。

图标全部用 Material Symbols 内置矢量或自绘 `ImageVector`，不引入图标包依赖。

## 9. 技术栈与构建

全部版本已在本机 Gradle 缓存中，构建无需大量下载：

| 组件 | 版本 |
|---|---|
| Gradle | 8.13（wrapper 自动下载；8.4 已缓存可回退） |
| AGP | 8.9.3 ✅缓存 |
| Kotlin / Compose 编译插件 | 2.1.21 ✅缓存 |
| KSP | 2.1.21-2.0.2 ✅缓存 |
| Room | 2.8.4 ✅缓存 |
| Compose BOM | 2026.06.01 ✅缓存 |
| compileSdk / targetSdk | 36 ✅已装 |
| minSdk | 26 |
| JDK | Android Studio 内置 JBR 21（`org.gradle.java.home` 显式指定，因系统 java 为 19） |

- 单 module `:app`，包名 `com.cruise.assistant`
- 架构：Compose UI → ViewModel(StateFlow) → Repository → Room DAO / DataStore
- 无网络库、无 DI 框架（手写 `AppContainer`）、无图片加载库
- release `signingConfig` 从项目根 `keystore.properties` 读取；该文件与 `*.jks` 加入
  `.gitignore`，由用户在 Android Studio 中自行创建（本项目不生成密钥）
- 中文为唯一界面语言，字符串全部走 `strings.xml`

## 10. 明确不做（YAGNI）

跨日事件、多航次、云同步、账号、分享导出、统计报表、深色模式专门设计（跟随系统即可）、
平板布局、横屏专门布局、Widget、番茄钟/计时功能（截图3 中已明确删除）。

## 11. 验收标准

1. 首次启动 → 引导页 → 选 8/15 上船 12:00、8/20 下船 → 生成后时间轴 Day1 有 `12:00 上船 15m`，Day6 有 `07:00 下船`
2. 同一天加两个时间重叠的事件 → 自动分列栏0/栏1，不遮挡；第三个重叠 → 进窄栏2
3. 事件卡片三行内容与地点格式 `7F船中花园` 正确
4. 改显示范围为 09:00–23:00 → 刻度与卡片位置同步变化，重启后保持
5. 开循环开关创建 07:30 早餐 → Day2..Day5 有，Day6（下船日）无
6. 有冲突时点"生成计划表" → 列出冲突对；忽略后定稿 → 该日变单栏只读
7. 杀掉进程重开 → 回到上次查看的日期
8. 排一个 2 分钟后的事件、提前 1 分钟 → 收到系统通知
9. `gradlew testDebugUnitTest` 全绿；`gradlew assembleDebug` 成功
10. 在 Pixel_9 (API 36) 模拟器上安装运行，各界面截图确认
