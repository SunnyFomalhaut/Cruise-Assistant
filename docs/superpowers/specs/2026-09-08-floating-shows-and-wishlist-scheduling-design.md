# Design Spec: 迪士尼探险号浮动演出心愿单待定档与日程流转设计

## 1. 业务背景与问题分析

根据游轮实测反馈，迪士尼探险号（Disney Adventure）上的演出并非全部为固定日期的日常演出：
- **固定确定项**：如起航派对《扬帆起航》（Day 1）、启航夜主秀《海洋奇遇》（Day 1，根据晚餐时段错峰）以及《狮子王烟花》（Day 2 晚 22:30）。
- **动态浮动项**：如《回忆：瓦力与伊芙 Remember》、《海洋奇缘 Moana》、《复仇者集结 Avengers Assemble》、《大白超级健身世博会 Baymax》、《达菲与友谊之船 Duffy》。各航次上演日期、每日场次数、开始时间均不固定，游客必须在登船连上游轮 Wi-Fi 后，通过 Disney Cruise Navigator App 方可查阅确切场次。

**原设计痛点**：
- 创建航次时强行在时间轴预塞浮动演出的假定场次，导致用户登船核对时时间轴充斥错误日程，必须逐一手动删除或大幅修改。

**目标解决方案**：
- **时间轴收敛**：创建航次时，时间轴仅默认生成高确定性项目（登船、下船、安全演习、晚餐、扬帆起航、海洋奇遇、狮子王烟花）。
- **更新狮子王烟花**：地点校准为 `阳光泳池 Sunnyside Pool`（Deck 17 船中），并增加观赏攻略预置备注（烟花在右舷，建议在泳池空地或 18 甲左舷走廊）。
- **心愿单作为“待定档池”**：创建航次时，5 部浮动演出自动播种入心愿单（带完整剧院地点、楼层与候场攻略备注），避免遗忘。
- **上船一键定档流转**：用户登船查阅 Navigator 后，在心愿单点击“📅 排入日程”，仅需指定“日期”与“时间”，一键将心愿项落入对应日时间轴并标记心愿打卡完成。
- **时间轴手动新建兜底**：时间轴新建日程的预置演出选择器（`ShowPresetPicker`）保留完整 8 部演出，供手动快速点选。

---

## 2. 演出数据模型与分流规范

### 2.1 狮子王烟花定义更新
- **标识符**：`show_lion_king`
- **标题**：`狮子王烟花 The Lion King: Celebration in the Sky`
- **地点**：`阳光泳池 Sunnyside Pool`（与 `locations.json` 及 `deck_17.svg` 统一对齐）
- **楼层 / 区域**：Deck 17 船中（`ShipZone.MID`）
- **时间**：固定第 2 天（Day 2）22:30（若航次总天数 >= 3，不排在下船日）
- **时长 & 候场**：时长 10 分钟，建议候场 45 分钟（`defaultWaitMinutes = 45`）
- **预设备注（`note`）**：
  > “烟花在右舷，可以在这个泳池旁边的空地或者18甲左舷走廊（人比较多）”

### 2.2 演出分流集合
在 Android (`ShowPreset.kt`) 与 iOS (`ShowPreset.swift`) 中明确区分：

1. **`TIMELINE_AUTO_GENERATED_SHOWS`（创建航次时自动写入时间轴）**：
   - `show_set_sail`（扬帆起航，Day 1 三场）
   - `show_seas_adventure`（海洋奇遇，Day 1 联动晚餐一场）
   - `show_lion_king`（狮子王烟花，Day 2 22:30 一场）

2. **`WISHLIST_SEED_SHOWS`（创建航次时自动播种进心愿单）**：
   - `show_remember`（回忆：瓦力与伊芙，华特迪士尼剧院，时长 60m，候场 45m）
     - 预设备注：*“主秀之一，各航次日期场次请连船上 Wi-Fi 查阅 Navigator App。剧院演出建议提前 45 分钟候场。”*
   - `show_moana`（海洋奇缘，寻路者湾，时长 15m，候场 30m）
     - 预设备注：*“露天互动音乐剧，各航次时间以 Navigator App 为准。建议提前 30 分钟候场。”*
   - `show_avengers`（复仇者集结，花园舞台，时长 40m，候场 15m）
     - 预设备注：*“漫威英雄集结舞台秀，各航次时间以 Navigator App 为准。建议提前 15 分钟候场。”*
   - `show_baymax_expo`（大白超级健身世博会，花园舞台，时长 30m，候场 15m）
     - 预设备注：*“大白晨间互动健身，非每日固定，请查阅 Navigator App。建议提前 15 分钟候场。”*
   - `show_duffy_friend_ship`（达菲与友谊之船，华特迪士尼剧院，时长 30m，候场 45m）
     - 预设备注：*“达菲与朋友们的主题秀，非每日固定，请查阅 Navigator App。建议提前 45 分钟候场。”*

3. **`DISNEY_ADVENTURE_SHOWS`（总目录，8 部演出保持完整）**：
   - 供时间轴新建事件时的 `ShowPresetPicker` 下拉选择。

---

## 3. 架构与具体流转设计

### 3.1 航次创建与播种（`VoyageRepository`）
- **`createVoyage(...)` 流程**：
  1. 写入 `VoyageEntity` 与 `DayPlanEntity`。
  2. 播种系统事件：登船（Boarding）与下船（Disembark）。
  3. 若 `shipPresetName != null`（内置迪士尼探险号）：
     - 生成安全演习（Day 1 15:45–16:20）。
     - 生成每日晚餐（根据选定时段）。
     - 遍历 `TIMELINE_AUTO_GENERATED_SHOWS`，生成预置演出事件，排入时间轴（利用候场时间算法分配 Lane）。特别注意为《狮子王烟花》写入指定 `note`。
     - 播种心愿单：查询当前心愿单已存在项目的标题，将 `WISHLIST_SEED_SHOWS` 中尚未存在于心愿单的演出，以“演出（show）”分类批量插入心愿单表。
- **`resetVoyage()` 重建航次流程**：
  - 清理心愿单中未打卡的预置演出，或在重建航次后重新走一遍上述播种逻辑。

### 3.2 心愿单“转为日程”交互规范（Android & iOS 双端对齐）

#### Android 端补齐与升级
- **卡片入口**：
  - 在 `WishlistCard` 底部右侧添加操作胶囊按钮：`📅 排入日程`（与地图“定位”并列或在右下角独立放置）。
- **转为日程弹窗（`WishlistConvertDialog`）**：
  - **展示信息（只读/预填）**：标题、分类徽标、地点、默认时长（来自心愿项或预置，可微调）。
  - **输入字段**：
    - 日期选择：航程内日期单选（默认第 1 天或当前选中日期）。
    - 时间选择：起飞时间（`LocalTime`，默认 10:00 或就近整点）。
    - 时长调节（Stepper 或快捷选项：15m / 30m / 45m / 60m）。
  - **确认回调**：
    - 调用 `EventRepository.addEvent`，带上心愿单的 `title`、`categoryId`、`deck`、`zone`、`placeName`、`note`，以及解析所得的 `waitMinutes`。
    - 自动标记该 `WishlistItem.isCompleted = true`。
    - 弹出轻量提示（如 Snackbar：“已加入第 X 天日程”）。

#### iOS 端优化
- iOS `WishlistConvertModalSheet` 目前已有转换逻辑，进一步确保：
  - 继承 `item.note` 写入新建的 `CruiseEvent.note`。
  - 根据演出类型正确配置 `waitMinutes` 并使用 `assignShowLane` 计算 Lane。
  - 在 `CruiseEvent` 数据模型与数据库持久化中正式加入 `note: String?` 支持。

---

## 4. 文件变动清单

### Android
1. `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt`:
   - 更新 `show_lion_king` 的地点为 `阳光泳池 Sunnyside Pool`，配置 note。
   - 定义 `TIMELINE_AUTO_GENERATED_SHOWS` 与 `WISHLIST_SEED_SHOWS`。
   - 定义浮动演出心愿单播种模板。
2. `app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt`:
   - `createVoyage` 中改用 `TIMELINE_AUTO_GENERATED_SHOWS` 生成时间轴事件。
   - 注入/使用 `WishlistDao` 实现心愿单演出的幂等播种。
3. `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistCard.kt` & `WishlistScreen.kt`:
   - 在 `WishlistCard` 增加 `📅 排入日程` 按钮。
   - 增加 `WishlistConvertDialog`。
4. `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistViewModel.kt`:
   - 增加 `convertToEvent(item, date, time, duration)` 逻辑。

### iOS
1. `IOS/CruiseAssistant/Domain/Algorithms/ShowPreset.swift`:
   - 更新 `show_lion_king` 的地点为 `阳光泳池 Sunnyside Pool`，配置 note。
   - 定义 `TIMELINE_AUTO_GENERATED_SHOWS` 与 `WISHLIST_SEED_SHOWS`。
2. `IOS/CruiseAssistant/Domain/Models/Models.swift`:
   - 给 `CruiseEvent` 增加 `public var note: String?`。
3. `IOS/CruiseAssistant/Data/Database/CruiseDatabase.swift`:
   - 保证 `CruiseEvent` 的 `note` 字段在内存/持久化中正确读写。
4. `IOS/CruiseAssistant/Data/Repositories/VoyageRepository.swift`:
   - `createVoyage` 改用 `TIMELINE_AUTO_GENERATED_SHOWS`。
   - 播种 `WISHLIST_SEED_SHOWS` 到心愿单。
5. `IOS/CruiseAssistant/UI/Wishlist/WishlistViewModel.swift` & `WishlistView.swift`:
   - `convertToEvent` 补全 `note` 与 `waitMinutes`。

---

## 5. 验证方案

### 5.1 单元测试
1. `ShowPresetTest` (Android & iOS)：
   - 验证 `TIMELINE_AUTO_GENERATED_SHOWS` 仅含 3 部演出（扬帆起航、海洋奇遇、狮子王烟花）。
   - 验证 `show_lion_king` 地点为 `阳光泳池 Sunnyside Pool`，且携带正确的观赏提示备注。
   - 验证 `WISHLIST_SEED_SHOWS` 包含 5 部浮动演出。
2. `VoyageRepositoryTest` (Android & iOS)：
   - 验证新建航次后：
     - 时间轴上的预置演出事件仅有 3 部（登船日 3 场扬帆起航、1 场海洋奇遇，以及第 2 晚 1 场狮子王烟花）。
     - 心愿单中自动生成 5 条未打卡的演出记录。
     - 重复调用 `createVoyage` 具有幂等性，心愿单不会产生重复演出。

### 5.2 业务流转与真机/模拟器测试
1. 启动 App，创建一个迪士尼探险号航次：
   - 检查时间轴：Day 1 看到扬帆起航、海洋奇遇、安全演习和晚餐；Day 2 看到 22:30 的狮子王烟花（点击详情卡片确认地点为阳光泳池，且有备注）。
   - 检查时间轴：Day 2/Day 3 不再自动出现 Remember、Moana、复仇者集结等假定场次。
2. 进入心愿单：
   - 看到 5 部浮动演出整齐列在心愿单中，分类为“演出”，带各剧院地点和攻略备注。
   - 点击《回忆：瓦力与伊芙》卡片上的“📅 排入日程”：
     - 弹出转换对话框，选择第 3 天 20:30。
     - 确认后，返回心愿单见该项已划线打卡。
     - 切换至时间轴第 3 天，确认 20:30 出现《回忆：瓦力与伊芙》，候场时间为 45 分钟，Lane 避让正常。
