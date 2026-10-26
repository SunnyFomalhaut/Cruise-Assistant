# Cruise Assistant（游轮行程助手）

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-blue.svg)]()
[![Android](https://img.shields.io/badge/Android-8.0%2B%20%7C%20API%2026-green.svg)](https://developer.android.com)
[![iOS](https://img.shields.io/badge/iOS-17.0%2B-lightgrey.svg)](https://developer.apple.com/ios/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-purple.svg)](https://kotlinlang.org)
[![SwiftUI](https://img.shields.io/badge/SwiftUI-iOS%2017%2B-orange.svg)](https://developer.apple.com/xcode/swiftui/)
[![Version](https://img.shields.io/badge/Version-v1.12.1%20(build%2053)-ff69b4.svg)](https://github.com/SunnyFomalhaut/Cruise-Assistant/releases)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

一款专为大型游轮（如迪士尼探险号 Disney Adventure）航程打造的**全离线、双平台（Android / iOS 原生）行程规划与邮轮生活助手**。

在公海无网络环境下，应用提供零延迟的 3 车道时间线编排、SVG 矢量甲板图导航、航次存档、联程航次、5 大类设施海报指南、仙尘交换（Pixie Dust）互赠记录与甲板图联动、演出自动候场计算与离线精确本地通知。Android 与 iOS 两端均为原生实现，功能完全对齐。

---

## 🌟 核心功能特性

### 1. 动态 3 车道时间线（Timeline & LaneCanvas）
- **智能多车道排版**：3 车道动态排版算法（车道 0/1 等宽居左，车道 2 窄幅靠右），时间重叠的活动自动并排展示，彻底告别遮挡。
- **候场时间段自适应**：演出/活动支持配置提前候场时间（0–120 分钟，0 即不候场），时间线上以半透明紧凑色块与虚线无缝连接。
- **实时指示与今天锚定**：红色高亮「当前时间线」与「正在进行 / 即将开始」提示条，支持多日滑动与快速回弹。
- **冲突检测与定稿锁定**：一键生成定稿计划表，自动扫描时间重叠冲突；定稿后切换为单列只读防误触模式，支持随时解锁修改。

### 2. 联程航次与登船编排（Onboarding）
- **多段联程航次**：支持不限段数的多段航程（如 3+4 夜强制交替编排），每个交接日自动生成 08:30 交接手续事件、仅末日保留 07:00 下船事件；演出按程重启、自动跳过重复安全演习、交接日照常晚餐。
- **礼宾优先登船**：礼宾（Concierge）舱位一键启用 11:00 固定优先登船时间，隐藏普通批次网格，设置页与存档显示专属徽标。
- **登岸标记**：4 夜航程可标记登岸兰卡威，自动生成第 3 天 09:00–17:00 登岸系统事件，全链路入库并纳入存档快照。

### 3. 航次存档（Voyage Archive）
- **全量快照**：航次、事件、类别、定稿状态、仙尘记录、愿望单一键归档为自包含 JSON，不随预置配置变化；支持自定义存档名称。
- **存档详情四 Tab**：概要 / 时间轴 / 仙尘 / 心愿四个维度回看完整航次；时间轴支持「草稿 / 日程表」双模式自由切换（草稿含当时停用的灰色备选，已定稿日亦可回看排程思路）。
- **重建与恢复**：设置页随时存档，重建航次三选；完整备份 JSON 支持导出 / 导入。

### 4. 迪士尼探险号甲板地图与离线检索（DeckMap）
- **矢量甲板图（Deck 5 – Deck 19）**：离线内置 14 张高精度 SVG 矢量甲板图，支持手势缩放、拖拽与楼层切换。
- **房号与设施检索**：离线房号数据库（`staterooms.json`）与设施地点库（`locations.json`），输入房号或设施名称即刻高亮定位船头/船中/船尾与所在甲板。

### 5. 预设演出库与一键编排（ShowPreset）
- **一键导入官方大秀**：内置《海洋奇缘》、《超能陆战队》、《复仇者联盟》、《狮子王》等热门演出预设。
- **智能填充参数**：自动填入官方演出时长、默认举办场地（如华特迪士尼剧院）、建议提前候场时间。

### 6. 邮轮设施海报指南（FacilityGuide）
- **5 大类设施画廊**：覆盖公共设施、餐饮美馔、青少年俱乐部、精品商店、精彩大秀 50+ 处高清海报。
- **多维度筛选与详情**：支持按分类、甲板、船体区域检索，点击卡片直达甲板定位与愿望单收藏。

### 7. 仙尘交换与房号九宫格（PixieDust）
- **专属九宫格房号键盘**：专为快速录入 4-5 位邮轮房号设计的数字键盘，支持触觉震动反馈。
- **互赠礼品记录**：清晰记录送出（Given）与收到（Received）的互赠礼品、回赠等级与收到礼物的历史。
- **房号关联与状态标记**：按房号关联全部互动，四种状态（待回礼 / 需补礼 / 已持平 / 已发未回）直观标记；联动检视按甲板分层折叠，默认收起、多层同开，收起态可直接跳转本层地图。
- **甲板图联动**：联动路线、房号标记与悬浮横幅直连甲板地图，互赠动线一目了然。

### 8. 愿望单与行程转化（Wishlist）
- 浏览设施与演出时随时加入愿望单，支持按优先级排序与一键生成至指定日期的时间线。

### 9. 个性化主题与壁纸（Customization）
- **达菲家族精选壁纸**：内置 7 款达菲与朋友们（Duffy, ShellieMay, StellaLou, Gelatoni, CookieAnn, Olu Mel, LinaBell）专属壁纸与自定义上传。
- **多套色彩主题**：支持切换柔和、海洋、庆典等多套设计系统调色板。

### 10. 离线精确闹钟提醒（Local Notifications）
- Android 基于系统底层精确闹钟（`AlarmManager`）、iOS 基于本地通知，支持提前 5/10/15/30/60 分钟离线推送，即便锁屏、离线或重启设备亦能精准提醒。

---

## 🏗️ 架构设计

两端均遵循 Clean Architecture + MVVM 架构，业务领域逻辑完全解耦，易于跨端移植。

### Android（Kotlin + Jetpack Compose）

```
com.cruise.assistant/
├── ui/                 # Jetpack Compose 声明式 UI 层
│   ├── timeline/       # 3 车道时间线 (LaneCanvas, EventCard, TimeRuler)
│   ├── event/          # 事件编辑 Sheet、位置选择器、候场时间选择
│   ├── deckmap/        # SVG 甲板交互图与全船搜索
│   ├── facilities/     # 设施/餐饮/商店/演出海报指南
│   ├── pixiedust/      # 仙尘交换、房号九宫格键盘与联动检视
│   ├── wishlist/       # 愿望单列表与转日程
│   ├── archive/        # 航次存档列表与四 Tab 详情
│   ├── settings/       # 航次管理、壁纸切换、数据备份/恢复/存档
│   └── theme/          # 颜色、字体与 Material3 主题
├── domain/             # 纯 Kotlin 业务逻辑与算法 (零 Android Framework 依赖)
│   ├── LaneAssigner.kt # 3 车道动态排版算法
│   ├── ConflictDetector# 时间冲突检测算法
│   ├── ArchiveSnapshot # 航次存档快照模型
│   ├── ShowPreset.kt   # 演出预设与候场逻辑
│   ├── StateroomCatalog# 房号离线索引
│   └── Models.kt       # 领域核心实体
├── data/               # 数据持久层
│   ├── db/             # Room 数据库 (9 张实体表与 DAO, schema v12)
│   ├── repo/           # Repository 仓库层
│   └── prefs/          # DataStore 用户配置
└── reminder/           # 离线精确本地通知与系统广播接收器
```

### iOS（SwiftUI）

```
IOS/CruiseAssistant/
├── App/                # 应用入口与 AppContainer (依赖注入)
├── UI/                 # SwiftUI 视图层 (与 Android 屏幕一一对应)
│   ├── Timeline/  DeckMap/  EventSheet/  Facilities/
│   ├── PixieDust/  Wishlist/  Onboarding/  Settings/  Common/
├── Domain/             # 纯 Swift 业务逻辑
│   ├── Algorithms/     # 车道排版、冲突检测等算法
│   └── Models/         # 领域核心实体
├── Data/               # 数据持久层
│   ├── Database/       # Codable 结构化离线存储
│   ├── Repositories/   # Repository 仓库层
│   └── Preferences/    # 用户配置
└── Services/           # 本地通知、触觉反馈等服务
```

Xcode 工程文件由 `IOS/generate_xcodeproj.py` 从源码目录自动生成（含单元测试与 XCUITest UI 测试目标）。

---

## 🛠️ 环境要求与构建

### Android

- **IDE**：Android Studio Ladybug / Meerkat（或最新稳定版）
- **JDK**：JDK 21（推荐使用 Android Studio 内置 JBR 21）
- **Android SDK**：Compile SDK 36, Target SDK 36, Min SDK 26（Android 8.0+）
- **构建工具**：Gradle 8.13, AGP 8.9.3, Kotlin 2.1.21, Compose BOM 2026.06.01

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 执行全量单元测试 (40 个测试文件, 311 个用例, 涵盖车道排版、冲突算法、存档快照等)
./gradlew testDebugUnitTest

# 编译 Release 签名包 (需在仓库根目录配置 keystore.properties)
./gradlew assembleRelease

# 直接安装到已连接的手机或模拟器
./gradlew installDebug
```

### iOS

- **IDE**：Xcode 15+（Swift 5.9，iOS 17.0+）

```bash
# 如需重新生成 Xcode 工程
cd IOS && python generate_xcodeproj.py

# 运行单元测试 + XCUITest UI 测试
xcodebuild test -project IOS/CruiseAssistant.xcodeproj \
  -scheme CruiseAssistant \
  -destination 'platform=iOS Simulator,name=<模拟器名称>'
```

---

## 📄 许可协议

本项目为个人私有项目，保留所有权利。未经许可不得公开分发或商用。
