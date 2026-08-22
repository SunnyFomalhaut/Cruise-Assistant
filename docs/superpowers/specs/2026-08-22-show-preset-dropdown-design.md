# 演出预置下拉菜单与新演出预置设计规格 (v1.10.6)

## 1. 概述与目标

在日程管理添加/编辑事件弹窗（`EventSheet`）以及随心单编辑弹窗（`WishlistEditSheet`）中，当用户选择类别为 **「演出」（iconKey == "show"）** 时，将事件内容（标题）从纯文本输入框升级为 **预置演出下拉菜单**（类似地点下拉机制），支持：
1. 一键选择探险号预置演出，自动联动填充 **演出名称、预置地点（甲板/区域/地点名）、默认起止时间（最早场次）**；
2. 提供 **「✍️ 自定义演出」** 选项，支持自由输入自定义演出或特殊活动；
3. 新增两部预置大秀：**《Baymax Super Exercise Expo》（大白超级健身世博会）** 与 **《Duffy and The Friend Ship》（达菲与友谊之船）**；
4. 版本号升级为 **`v1.10.6`**（`versionCode = 32`）。

---

## 2. 预置演出目录（共 8 部演出）

在 `ShowPreset.kt` 中维护迪士尼探险号预置演出：

| Key | 演出名称 | 地点 | 甲板/区域 | 默认最早时间 | 时长 | 场次定义 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `show_baymax_expo` | **大白超级健身世博会 Baymax Super Exercise Expo** | 花园舞台 | Deck 10 中部 | **08:45 - 09:15** | 30 分钟 | 08:45 |
| `show_moana` | **海洋奇缘 Moana: Call of the Sea** | 寻路者湾 Wayfinder Bay | Deck 10 船尾 | **09:30 - 09:45** | 15 分钟 | 09:30, 10:00, 10:30 |
| `show_duffy_friend_ship` | **达菲与友谊之船 Duffy and The Friend Ship** | 华特迪士尼剧院 Walt Disney Theatre | Deck 6 船头 | **09:45 - 10:15** | 30 分钟 | 09:45 |
| `show_set_sail` | **扬帆起航 Let's Set Sail** | 花园舞台 | Deck 10 中部 | **13:15 - 13:45** | 30 分钟 | 13:15, 14:00, 14:45 |
| `show_seas_adventure` | **海洋奇遇 Disney Seas The Adventure** | 华特迪士尼剧院 Walt Disney Theatre | Deck 6 船头 | **18:00 - 19:00** | 60 分钟 | 18:00, 20:30 (晚餐联动) |
| `show_remember` | **回忆：瓦力与伊芙 Remember** | 华特迪士尼剧院 Walt Disney Theatre | Deck 6 船头 | **18:00 - 19:00** | 60 分钟 | 18:00, 20:30 (晚餐联动) |
| `show_avengers` | **复仇者集结 Avengers Assemble!** | 花园舞台 | Deck 10 中部 | **18:15 - 18:55** | 40 分钟 | 18:15, 20:45 |
| `show_lion_king` | **狮子王烟花 The Lion King: Celebration in the Sky** | 露天泳池 | Deck 17 中部 | **22:30 - 22:40** | 10 分钟 | 22:30 |

---

## 3. 交互与状态设计

### 3.1 EventSheet 演出下拉模式
- **状态字段**：
  - `showPresetMode: Boolean`（当且仅当 `categoryIconKey == "show"` 且为内置船且当前标题属于预置演出或处于预置模式时为 true，切入演出类别时默认 true）。
- **渲染逻辑**：
  - 若 `showPresetMode == true`：展示 `ExposedDropdownMenuBox`，文本框为只读，点击展开 8 部预置演出列表及底部「✍️ 自定义演出」；
  - 选中预置演出：
    1. 标题填入 `preset.title`；
    2. 地点填入 `preset.deck`、`preset.zone`、`preset.placeName`（`noLocation = false`，`locationPresetMode = true`）；
    3. 时间联动：`startTime = preset.startTimes.first()`，`endTime = startTime.plusMinutes(preset.durationMinutes.toLong())`；
  - 选中「自定义演出」：`showPresetMode = false`，切换为普通的 `OutlinedTextField`，允许用户自由输入。

### 3.2 WishlistEditSheet 演出预置
- 在随心单编辑弹窗中，当选中类别为「演出」时，事件内容同样提供演出预置下拉与联动填入地点（随心单无需时间）。

---

## 4. 验证计划
1. **单元测试**：`ShowPresetTest` 校验 8 部演出完整性、默认时间、场次解析与时长；
2. **UI 编译与运行测试**：在模拟器中新建演出事件，验证从下拉框选择《海洋奇缘》、《大白超级健身世博会》、《达菲与友谊之船》等，名称、地点、时间自动同步联动。
