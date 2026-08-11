# 预置演出（Preset Shows）设计规格

## Overview
为 Disney Adventure（探险号）内置预制演出目录，在创建航次时自动把 5 场演出加入日程表第二栏（lane 1），并在选完探险号进入主界面后弹窗提示。演出与晚餐时段联动：改晚餐时段时，海洋奇遇/回忆的场次在 18:00 ↔ 20:30 之间互换。

## 1. 演出目录模型（新文件 `domain/ShowPreset.kt`）

延续 `ShipPreset.kt` 的静态注册表模式（"内置数据库"= 代码内静态目录，不建 Room 表、不加迁移）。

```kotlin
data class ShowPreset(
    val key: String,               // presetKey，如 "show_set_sail"
    val title: String,             // 完整标题，如 "扬帆起航 Let's Set Sail"
    val deck: Int,                 // 10 / 17 / 6
    val zone: ShipZone,            // MID / BOW / STERN
    val placeName: String,         // 花园舞台 / 露天泳池 / 迪士尼剧院 / 泳池舞台
    val durationMinutes: Int,
    val dayOffsets: List<Int>,     // 相对上船日的天（第1天 = 1）
    val dailyRangeTo: Int? = null, // 非空 = 从 dayOffsets 首日起每天都有，直到 min(该值, 航次天数-1)
    val startTimes: List<LocalTime>,
    val dinnerLinked: Boolean = false, // true = 按晚餐时间只取一场
)

val DISNEY_ADVENTURE_SHOWS: List<ShowPreset>
```

### 内置数据（迪士尼探险号）

| key | 标题 | 天 | 场次 | 时长 | 地点 |
|---|---|---|---|---|---|
| show_set_sail | 扬帆起航 Let's Set Sail | 第1天 | 13:15 / 14:00 / 14:45 | 30 | 10F 船中 花园舞台 |
| show_lion_king | 狮子王烟花 The Lion King: Celebration in the Sky | 第2天 | 22:30 | 10 | 17F 船中 露天泳池 |
| show_seas_adventure | 海洋奇遇 Disney Seas The Adventure | 第1天 | 18:00 / 20:30（按晚餐取一场） | 60 | 6F 船头 迪士尼剧院 |
| show_remember | 回忆：瓦力与伊芙 Remember | 第2天 | 18:00 / 20:30（按晚餐取一场） | 60 | 6F 船头 迪士尼剧院 |
| show_moana | 海洋奇缘 Moana: Call of the Sea | 第2天起每天 | 9:30 / 10:00 / 10:30 | 15 | 10F 船尾 泳池舞台 |

- 海洋奇缘日期规则：`dayOffsets = [2]`、`dailyRangeTo = 4`。生成时上限截断到 `航次天数 - 1`（不含下船日）：
  - 四天三夜（4 天）：第 2、3 天（第 4 天下船，无场次）
  - 五天四夜（5 天）：第 2、3、4 天
- 扬帆起航三场 13:15–13:45 / 14:00–14:30 / 14:45–15:15，互不重叠（用户确认第三场为 14:45，非 14:15）。
- 全航次无任何时间重叠，定稿冲突检查不误报。

## 2. 生成逻辑（`VoyageRepository.createVoyage` 改动）

在现有"安全演习 + 晚餐"生成块（`shipPresetName != null` 分支）内追加：

1. 取"演出"类别：`categories.firstOrNull { it.iconKey == "show" }`，兜底用第一个类别（与晚餐一致）。
2. 遍历 `DISNEY_ADVENTURE_SHOWS`，按 preset 计算日期集合：
   - 固定天：`dayOffsets.map { boardDate.plusDays(it - 1) }`
   - 范围天：从 `boardDate.plusDays(dayOffsets.first() - 1)` 起，到 `boardDate.plusDays(min(dailyRangeTo, dayCount - 1) - 1)`。
3. 每个日期 × 每个场次：
   - `dinnerLinked = false`：全部场次插入。
   - `dinnerLinked = true`：只插入与晚餐匹配的一场。规则：`dinnerTime < 19:00`（即 17:45）→ 20:30 场；否则 → 18:00 场（选中的恰好是与晚餐不冲突的那场）。
4. 字段约定（与晚餐事件一致）：
   - `lane = 1`（第二栏）
   - `isSystemGenerated = false`（用户可编辑/删除）
   - `presetKey` = 上表 key
   - `reminderMinutesBefore = null`（无提醒）
   - `deck` / `zone` / `placeName` 填入演出地点

## 3. 晚餐时段联动（`EventRepository` + `SettingsViewModel` 改动）

1. 新增 `suspend fun resyncShowTimes(dinnerTime: LocalTime)`，紧邻 `updateDinnerEventsTime`：
   - 查 `presetKey` ∈ {`show_seas_adventure`, `show_remember`} 的事件（DAO 已有 `getByPresetKey`，按 key 各查一次即可）
   - 目标时间 = 同一条规则：`dinnerTime < 19:00` → 20:30，否则 → 18:00
   - 保护：仅当事件当前 `startTime` 是 18:00 或 20:30 之一时才更新——用户手动改过时间的演出不被覆盖
2. `SettingsViewModel.setDinnerTime`：`updateDinnerEventsTime(time)` 后追加 `resyncShowTimes(time)`，再 `reminderScheduler.rescheduleAll()`（提醒重排自然覆盖被挪动的演出）。
3. 被用户删除的演出不会"复活"（与晚餐批量更新行为一致，只更新表中现存行）。

## 4. 选完探险号后的提示弹窗（`CruiseNavHost`）

onboarding 提交回调：`state.isBuiltInShip == true` 时置 `showPresetShowsDialog` 标记，导航进 TIMELINE 后弹 `AlertDialog`：

- 标题："已添加预置演出"
- 内容：列出 5 场演出摘要（含场次数），说明"已添加到日程表第二栏"，提示可长按卡片编辑/删除
- 确认按钮："知道了"，点掉后本次进入不再显示
- 时机：每次 onboarding 提交完成都弹（含"重建航次"后重新走引导——演出会重新生成）；自定义船名不弹

字符串全部加入 `strings.xml`。

## 5. 测试策略

沿现有 `app/src/test/java/com/cruise/assistant/domain/` 模式，新增 `ShowPresetTest.kt`（纯 domain 函数测试）：

1. 日期规则：四天三夜 → 海洋奇缘第 2、3 天；五天四夜 → 第 2、3、4 天；固定天演出不受影响
2. 晚餐选场：`dinnerTime = 17:45` → 20:30；`20:00` → 18:00
3. 目录完整性：5 场齐全、presetKey 唯一、地点甲板在探险号 minDeck(5)..maxDeck(19) 范围内

仓库层（createVoyage 插入、resyncShowTimes 互换）沿用现有模式——不做集成测试，构建 + 真机手动验证。

## 6. 范围决策
- 演出仅对内置船（Disney Adventure）生成；自定义船名无演出、无弹窗。
- 演出归类为"演出"（iconKey = "show"）。
- 演出与晚餐/安全演习一样：可编辑、可删除、无默认提醒。
- 弹窗每次 onboarding 提交后显示，无"仅首次"持久化标记。
- 不新增数据库表、不新增迁移（DB 版本保持 4）。

## 7. 边界情况
- 用户删除某场演出 → 不复活（除非"重建航次"）。
- 用户手动改过演出时间 → 晚餐联动不覆盖。
- 四天三夜下船日（第 4 天）无任何演出。
- 22:30 的狮子王烟花在默认显示范围（07:00–24:00）内可见。
