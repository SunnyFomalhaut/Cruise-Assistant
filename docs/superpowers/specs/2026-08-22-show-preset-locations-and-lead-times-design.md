# 预置演出地点关联与差异化候场时间设计规格 (Design Spec)

**Date:** 2026-08-22  
**Target Version:** 1.10.13 (versionCode: 39)

---

## 1. 目标与背景

当前应用中存在两个待改进的演出与时间轴体验问题：
1. **预置演出地点未关联数据库**：主界面预置的“扬帆起航”、“复仇者集结”、“大白超级健身世博会”演出在 `ShowPreset.kt` 中硬编码为 `"花园舞台"`，而地点库 `locations.json` 及甲板 SVG 中的标准地点名为 `"花园舞台 Garden Stage"`。导致这些演出无法在地点下拉中自动匹配选中，在详情弹窗中点击“定位”也无法在甲板地图中命中高亮。
2. **演出候场时间单一（固定30分钟）**：
   - 华特迪士尼剧院（Deck 6）由于座位紧俏，需提前 **45 分钟** 候场；
   - 狮子王烟花（Deck 17 露天泳池）极受欢迎，需提前 **45 分钟** 候场；
   - 大白剧院（Deck 7 Baymax Cinemas）为影院模式，候场 **15 分钟** 即可；
   - 花园舞台（Deck 10 Garden Stage）为开阔室外/中庭舞台，候场 **15 分钟** 即可；
   - 寻路者湾（Deck 10 Wayfinder Bay，如海洋奇缘）保持 **30 分钟** 候场；
   - 新建/编辑演出时，需为用户提供 **10 分钟 ～ 120 分钟**（以 5 分钟为一个档位）的可选候场时间，并在选择预置演出或剧场地点时自动联动填入推荐候场时间。

---

## 2. 详细设计

### 2.1 预置演出地点修正与关联
在 `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt` 中：
- `show_set_sail`（扬帆起航）：`placeName = "花园舞台 Garden Stage"`, `deck = 10`, `zone = ShipZone.MID`
- `show_avengers`（复仇者集结）：`placeName = "花园舞台 Garden Stage"`, `deck = 10`, `zone = ShipZone.MID`
- `show_baymax_expo`（大白超级健身世博会）：`placeName = "花园舞台 Garden Stage"`, `deck = 10`, `zone = ShipZone.MID`

更新后与 `locations.json` 中的条目 `花园舞台 Garden Stage` 严格一致，详情弹窗中点击“定位”可精确定位至 Deck 10 SVG 中的 Garden Stage。

### 2.2 差异化默认候场时间规则
定义统一的候场时间解析函数与规则：
```kotlin
fun resolveDefaultWaitMinutes(
    placeName: String?,
    deck: Int? = null,
    title: String? = null,
    presetKey: String? = null,
): Int {
    val text = "${placeName.orEmpty()} ${title.orEmpty()} ${presetKey.orEmpty()}"
    if (text.contains("迪士尼剧院") || text.contains("Disney Theatre") || text.contains("Disney Theater") || (deck == 6 && text.contains("剧院"))) {
        return 45
    }
    if (text.contains("狮子王") || text.contains("Lion King") || text.contains("show_lion_king")) {
        return 45
    }
    if (text.contains("大白剧院") || text.contains("Baymax Cinema") || text.contains("Baymax Cinemas")) {
        return 15
    }
    if (text.contains("花园舞台") || text.contains("Garden Stage")) {
        return 15
    }
    if (text.contains("寻路者湾") || text.contains("Wayfinder Bay")) {
        return 30
    }
    return 30 // 默认兜底 30 分钟
}
```

在 `ShowPreset` 数据类中补充 `defaultWaitMinutes: Int = 30` 字段：
- `show_set_sail` (花园舞台): `15`
- `show_lion_king` (露天泳池): `45`
- `show_seas_adventure` (华特迪士尼剧院): `45`
- `show_remember` (华特迪士尼剧院): `45`
- `show_moana` (寻路者湾): `30`
- `show_avengers` (花园舞台): `15`
- `show_baymax_expo` (花园舞台): `15`
- `show_duffy_friend_ship` (华特迪士尼剧院): `45`

### 2.3 数据持久化与 Room 数据库迁移
1. `CruiseEvent` 与 `EventEntity` 增加字段：
   ```kotlin
   val waitMinutes: Int? = null
   ```
2. 提供扩展属性与辅助方法：
   ```kotlin
   val CruiseEvent.effectiveWaitMinutes: Int
       get() = waitMinutes ?: resolveDefaultWaitMinutes(placeName, deck, title, presetKey)
   ```
3. 扩展区间与候场时间函数改写：
   ```kotlin
   fun showEffectiveRange(event: CruiseEvent, isShow: Boolean): Pair<Int, Int> =
       if (isShow) (event.startMinute - event.effectiveWaitMinutes) to event.endMinute
       else event.startMinute to event.endMinute

   fun formatWaitStart(event: CruiseEvent, waitMinutes: Int = event.effectiveWaitMinutes): String {
       val waitMin = ((event.startMinute - waitMinutes) % 1440 + 1440) % 1440
       val hour = waitMin / 60
       val minute = waitMin % 60
       return "%02d:%02d".format(hour, minute)
   }
   ```
4. Room 数据库升级：
   - `CruiseDatabase` `version = 7`
   - 添加 `AutoMigration(from = 6, to = 7)`
   - 新建航次时（`VoyageRepository` 生成预置演出），将每个演出的 `waitMinutes` 写入数据库。

### 2.4 时间轴与提醒排程适配
1. `LaneCanvas.kt`：
   - 计算等待段高度 `waitHeightDp` 时，使用 `event.effectiveWaitMinutes`（而非硬编码的 30 分钟）。
2. `EventDetailDialog.kt`：
   - 候场提示文案由固定的“提前 30 分钟”改为 `"建议候场：$waitStartStr 入场（提前 ${event.effectiveWaitMinutes} 分钟）"`。
3. `ReminderScheduler.kt`：
   - 演出候场提醒基准时间由固定的 30 分钟改为 `event.effectiveWaitMinutes`。

### 2.5 创建/编辑时刻的 UI 交互 (`EventSheet`)
1. `EventSheetState` 增加 `waitMinutes: Int = 30`。
2. 候场时间档位列表常量：`WAIT_MINUTE_OPTIONS = (10..120 step 5).toList()`（共 23 个档位：10, 15, 20, 25, 30, ..., 120）。
3. 联动机制：
   - 切换到演出类别（`iconKey == "show"`）时，初始化 `waitMinutes` 为当前地点/演出的推荐默认值；
   - 选中预置演出（`onShowPresetSelect`）时，自动更新 `waitMinutes = preset.defaultWaitMinutes`；
   - 选中预置地点（`onSelectPresetLocation`）时，若当前为演出类别，自动将 `waitMinutes` 更新为该地点的默认候场时间；
   - 用户可随时在下拉菜单中选择 10～120 分钟内的任意档位；
4. `save()` 保存时，将 `waitMinutes = if (categoryIconKey == "show") s.waitMinutes else null` 保存到 `CruiseEvent` 中。

---

## 3. 版本号升级
- `app/build.gradle.kts`:
  - `versionCode = 39`
  - `versionName = "1.10.13"`

---

## 4. 验证计划
1. **单元测试**：
   - `ShowPresetTest`：验证 8 个预置演出的地点和默认候场时间配置；
   - `EventRepositoryTest` / `LocationFormatterTest`：运行全部单元测试（`./gradlew testDebugUnitTest`），确保 100% 通过。
2. **模拟器真机验证 (`emulator-5554`)**：
   - 安装并启动 App；
   - 验证主界面第一天 3 场“扬帆起航”卡片地点显示为 `D10 船中 花园舞台 Garden Stage`，候场段为 15 分钟高度；
   - 点击“扬帆起航”详情弹窗，点击“定位”，验证成功跳转至 Deck 10 并正确定位/高亮 Garden Stage；
   - 验证第二天“狮子王烟花”候场为 45 分钟，“海洋奇遇”等华特迪士尼剧院演出候场为 45 分钟；
   - 打开创建事件页面，选择演出类别，验证候场时间选择器提供 10~120 分钟（5 分钟步长）选项，选择不同预置或地点时自动联动推荐值，创建自定义演出保存后时间轴布局与候场区间展示正确。
