# Cruise Assistant（游轮行程助手）

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-blue.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.06.01-green.svg)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Version-v1.10.13-orange.svg)](https://github.com/SunnyFomalhaut/Cruise-Assistant)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

一款专为大型游轮（如迪士尼探险号 Disney Adventure）航程打造的**全离线、高并发、多车道行程规划与邮轮生活助手**。

在公海无网络环境下，应用提供零延迟的 3 车道时间线编排、SVG 矢量甲板图导航、5 大类设施海报指南、Pixie Dust 互赠礼品房号记录、演出自动候场计算与离线精确本地通知。

---

## 🌟 核心功能特性

### 1. 动态 3 车道时间线（Timeline & LaneCanvas）
- **智能多车道排版**：采用 3 车道动态排版算法（车道 0/1 等宽居左，车道 2 窄幅靠右），时间重叠的活动自动并排展示，彻底告别遮挡。
- **候场时间段自适应**：演出/活动支持配置提前候场时间（10–120 分钟），时间线上以半透明紧凑色块与虚线无缝连接。
- **实时指示与今天锚定**：红色高亮「当前时间线」与「正在进行 / 即将开始」提示条，支持多日滑动与快速回弹。
- **冲突检测与定稿锁定**：一键生成定稿计划表，自动扫描时间重叠冲突；定稿后切换为单列只读防误触模式，支持随时解锁修改。

### 2. 迪士尼探险号甲板地图与离线检索（DeckMap）
- **矢量甲板图（Deck 5 – Deck 19）**：离线内置 14 张高精度 SVG 矢量甲板图，支持手势缩放、拖拽与楼层切换。
- **房号与设施检索**：离线房号数据库（`staterooms.json`）与设施地点库（`locations.json`），输入房号或设施名称即刻高亮定位船头/船中/船尾与所在甲板。

### 3. 预设演出库与一键编排（ShowPreset）
- **一键导入官方大秀**：内置《海洋奇缘》、《超能陆战队》、《复仇者联盟》、《狮子王》等热门演出预设。
- **智能填充参数**：自动填入官方演出时长、默认举办场地（如华特迪士尼剧院）、建议提前候场时间。

### 4. 邮轮设施海报指南（FacilityGuide）
- **5 大类设施画廊**：覆盖公共设施、餐饮美馔、青少年俱乐部、精品商店、精彩大秀 50+ 处高清海报。
- **多维度筛选与详情**：支持按分类、甲板、船体区域检索，点击卡片直达甲板定位与愿望单收藏。

### 5. Pixie Dust 鱼袋互赠与房号九宫格（PixieDust）
- **专属九宫格房号键盘**：专为快速录入 4-5 位邮轮房号设计的数字键盘，支持触觉震动反馈。
- **互赠礼品记录**：清晰记录送出（Given）与收到（Received）的互赠礼品与回赠等级。
- **房号关联与状态标记**：直观查看特定房号的历史互动记录与备注。

### 6. 愿望单与行程转化（Wishlist）
- 浏览设施与演出时随时加入愿望单，支持按优先级排序与一键生成至指定日期的时间线。

### 7. 个性化主题与壁纸（Customization）
- **达菲家族精选壁纸**：内置 7 款达菲与朋友们（Duffy, ShellieMay, StellaLou, Gelatoni, CookieAnn, Olu Mel, LinaBell）专属壁纸与自定义上传。
- **多套色彩主题**：支持切换柔和、海洋、庆典等多套设计系统调色板。

### 8. 离线精确闹钟提醒（Local Notifications）
- 基于系统底层精确闹钟（`AlarmManager`），支持提前 5/10/15/30/60 分钟离线推送，即便锁屏、离线或重启设备亦能精准提醒。

---

## 🏗️ 架构设计

应用遵循 Clean Architecture + MVVM + MVI 架构，业务领域逻辑完全解耦，易于跨端移植：

```
com.cruise.assistant/
├── ui/                 # Jetpack Compose 声明式 UI 层
│   ├── timeline/       # 3 车道时间线 (LaneCanvas, EventCard, TimeRuler)
│   ├── event/          # 事件编辑 Sheet、位置选择器、候场时间选择
│   ├── deckmap/        # SVG 甲板交互图与全船搜索
│   ├── facilities/     # 设施/餐饮/商店/演出海报指南
│   ├── pixiedust/      # 鱼袋互赠、房号九宫格键盘与记录管理
│   ├── wishlist/       # 愿望单列表与转日程
│   ├── settings/       # 航次管理、壁纸切换、数据备份/恢复
│   └── theme/          # 颜色、字体与 Material3 主题
├── domain/             # 纯 Kotlin 业务逻辑与算法 (零 Android Framework 依赖)
│   ├── LaneAssigner.kt # 3 车道动态排版算法
│   ├── ConflictDetector# 时间冲突检测算法
│   ├── ShowPreset.kt   # 演出预设与候场逻辑
│   ├── StateroomCatalog# 房号离线索引
│   └── Models.kt       # 领域核心实体
├── data/               # 数据持久层
│   ├── db/             # Room 数据库 (8 张实体表与 DAO)
│   ├── repo/           # Repository 仓库层
│   └── prefs/          # DataStore 用户配置
└── reminder/           # 离线精确本地通知与系统广播接收器
```

---

## 🛠️ 环境要求与构建

### 开发环境
- **IDE**：Android Studio Ladybug / Meerkat (或最新稳定版)
- **JDK**：JDK 21（推荐使用 Android Studio 内置 JBR 21）
- **Android SDK**：Compile SDK 36, Target SDK 36, Min SDK 26 (Android 8.0+)
- **构建工具**：Gradle 8.13, AGP 8.9.3, Kotlin 2.1.21, Compose BOM 2026.06.01

### 常用编译指令

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 执行全量单元测试 (涵盖车道排版、冲突算法、预设解析等 55+ 单元测试)
./gradlew testDebugUnitTest

# 编译 Release 签名包 (需配置 keystore.properties)
./gradlew assembleRelease

# 直接安装到已连接的手机或模拟器
./gradlew installDebug
```

---

## 📱 iOS 版本规划

本项目已预留 `IOS/` 目录，计划采用 **SwiftUI + SwiftData / GRDB** 1:1 原生实现 iOS 平台版本，提供同等出色的 120Hz 极速离线体验与灵动岛实时日程提醒。

---

## 📄 许可协议

本项目为个人私有项目，保留所有权利。未经许可不得公开分发或商用。
