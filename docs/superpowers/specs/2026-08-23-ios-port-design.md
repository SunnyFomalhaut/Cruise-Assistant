# Cruise Assistant (iOS 版本) — 架构与移植设计规格

- **日期**：2026-08-23
- **状态**：已确认待落地
- **目标目录**：`IOS/`
- **目标平台**：iOS 17.0+ (iPhone / iPad)
- **开发语言与框架**：Swift 6, SwiftUI, SwiftData / GRDB.swift, Observation, UNUserNotificationCenter

---

## 1. 背景与目标

当前 `Cruise Assistant` 是一款针对大型邮轮（以迪士尼探险号 Disney Adventure 为首发样板）打造的**全离线、公海无网环境行程规划与生活助手**。
现计划在项目根目录下的 `IOS/` 目录中，完整构建对应的 iOS 原生版本，实现与 Android 版本功能 100% 对齐，并在 iOS 平台上获得极致流畅的 120Hz 离线交互体验。

### 核心约束
1. **100% 离线可用**：零外部网络依赖、零后端服务、零账号体系。
2. **算法 1:1 像素级对齐**：3 车道排版算法、冲突检测算法、空档折叠算法、房号/设施离线检索与 Android 端产出完全一致。
3. **独立工程结构**：所有 iOS 代码、Xcode 工程配置文件、资产包均严格收敛在 `IOS/` 目录下。

---

## 2. 系统技术选型

- **UI 框架**：SwiftUI (利用 iOS 17+ `@Observable` 宏进行状态驱动，声明式构建界面)。
- **持久化层**：
  - 首选 **SwiftData**（原生、现代化数据模型对象图框架）或 **GRDB.swift / SQLite3**。
  - 配置偏好使用 `@AppStorage` / `UserDefaults`。
- **并发与异步**：Swift Structured Concurrency (`async/await`, `Task`, `AsyncStream`)。
- **矢量图形 (SVG)**：`WebKit` 离线极速渲染 / 原生 CoreGraphics 矢量路径解析与 SwiftUI `Canvas` 渲染。
- **本地通知**：`UserNotifications` (`UNUserNotificationCenter`) 离线日历触发器。
- **触觉反馈**：`UIImpactFeedbackGenerator` / `CoreHaptics` 模拟实体按键触感。

---

## 3. `IOS/` 目录工程结构设计

```
IOS/
├── CruiseAssistant.xcodeproj               # Xcode 项目主工程
├── CruiseAssistant/
│   ├── App/
│   │   ├── CruiseAssistantApp.swift         # SwiftUI App 启动入口
│   │   ├── AppContainer.swift              # 依赖注入容器 (Repositories, Services)
│   │   └── Navigation/
│   │       ├── AppRoute.swift              # 统一导航路由定义
│   │       └── NavigationCoordinator.swift # 路由导航状态持有者
│   │
│   ├── Domain/                             # 纯 Swift 领域模型与核心算法 (零 UI 依赖)
│   │   ├── Models/
│   │   │   ├── Voyage.swift                # 航次模型
│   │   │   ├── CruiseEvent.swift           # 事件模型 (含车道、候场、提醒等)
│   │   │   ├── DayPlan.swift               # 日程定稿状态模型
│   │   │   ├── Category.swift              # 类别与调色板定义
│   │   │   ├── LocationPreset.swift        # 常用地点预设
│   │   │   ├── PixieDust.swift             # 鱼袋互赠与评级模型
│   │   │   ├── Wishlist.swift              # 愿望单模型
│   │   │   └── ColorTheme.swift            # 8 款达菲色彩主题定义
│   │   ├── Algorithms/
│   │   │   ├── LaneAssigner.swift          # 3 车道动态排版分配算法
│   │   │   ├── TimelineLayout.swift        # 时间轴折叠与刻度计算
│   │   │   ├── ConflictDetector.swift      # 时间冲突检测器
│   │   │   ├── ShowPreset.swift            # 官方演出库与参数预填
│   │   │   ├── StateroomCatalog.swift      # 1700+ 间舱房离线检索器
│   │   │   ├── LocationCatalog.swift       # 设施地点检索器
│   │   │   └── PixieDustLinkage.swift      # 鱼袋与房号关联算法
│   │   └── Wallpapers/
│   │       └── WallpaperManager.swift      # 壁纸预设与透明度管理
│   │
│   ├── Data/                               # 数据持久化与仓储层
│   │   ├── Database/
│   │   │   ├── CruiseDataContainer.swift   # SwiftData ModelContainer / SQLite 初始化
│   │   │   ├── Entities/
│   │   │   │   ├── VoyageEntity.swift
│   │   │   │   ├── EventEntity.swift
│   │   │   │   ├── DayPlanEntity.swift
│   │   │   │   ├── CategoryEntity.swift
│   │   │   │   ├── LocationPresetEntity.swift
│   │   │   │   ├── PixieDustEntity.swift
│   │   │   │   ├── PixieLevelEntity.swift
│   │   │   │   └── WishlistItemEntity.swift
│   │   │   └── Mappers/                    # 实体与领域模型互转
│   │   ├── Repositories/
│   │   │   ├── EventRepository.swift
│   │   │   ├── VoyageRepository.swift
│   │   │   ├── CategoryRepository.swift
│   │   │   ├── PixieDustRepository.swift
│   │   │   └── WishlistRepository.swift
│   │   └── Preferences/
│   │       └── AppSettings.swift           # 用户配置偏好存储
│   │
│   ├── UI/                                 # SwiftUI 视图与 ViewModels
│   │   ├── Timeline/                       # 3 车道主时间线界面
│   │   │   ├── TimelineView.swift
│   │   │   ├── TimelineViewModel.swift
│   │   │   ├── Components/
│   │   │   │   ├── LaneCanvasView.swift    # 3 车道 Canvas 与卡片布局
│   │   │   │   ├── EventCardView.swift     # 事件卡片 (含候场条与虚线)
│   │   │   │   ├── TimeRulerView.swift     # 刻度尺与折叠段标识
│   │   │   │   ├── DateStripView.swift     # 顶部航次日期切换条
│   │   │   │   ├── NowBannerView.swift     # 实时游标与正在进行指示
│   │   │   │   └── ConflictDialogView.swift# 冲突提示弹窗
│   │   ├── EventSheet/                     # 活动添加 / 编辑 Sheet
│   │   │   ├── EventSheetView.swift
│   │   │   ├── EventSheetViewModel.swift
│   │   │   └── Components/
│   │   │       ├── TimeWheelPicker.swift   # 滚轮时间选择器
│   │   │       ├── LocationPicker.swift    # 智能地点选择与自动补全
│   │   │       └── CategorySelector.swift  # 类别单选与色块
│   │   ├── DeckMap/                        # SVG 矢量甲板图与搜索
│   │   │   ├── DeckMapView.swift
│   │   │   ├── DeckMapViewModel.swift
│   │   │   └── Components/
│   │   │       ├── SVGInteractiveView.swift# 双指缩放平移 SVG 渲染器
│   │   │       └── DeckMapSearchSheet.swift# 房号与设施检索面板
│   │   ├── Facilities/                     # 设施画廊海报指南
│   │   │   ├── FacilityGuideView.swift
│   │   │   ├── FacilityGuideViewModel.swift
│   │   │   └── Components/
│   │   │       └── FacilityCardView.swift  # 海报卡片、标签与跳转
│   │   ├── PixieDust/                      # 鱼袋互赠与房号九宫格
│   │   │   ├── PixieDustView.swift
│   │   │   ├── PixieDustViewModel.swift
│   │   │   └── Components/
│   │   │       ├── PixieKeypadView.swift   # 自定义 9 键触感房号键盘
│   │   │       ├── GivenListView.swift     # 送出列表
│   │   │       ├── ReceivedListView.swift  # 收到列表
│   │   │       └── LinkageViewDialog.swift # 房号历史互动记录
│   │   ├── Wishlist/                       # 愿望单与转日程
│   │   │   ├── WishlistView.swift
│   │   │   ├── WishlistViewModel.swift
│   │   │   └── Components/
│   │   │       └── WishlistCardView.swift
│   │   ├── Settings/                       # 设置、航次与主题管理
│   │   │   ├── SettingsView.swift
│   │   │   ├── SettingsViewModel.swift
│   │   │   └── Components/
│   │   │       ├── ThemePickerView.swift   # 8 款主题切换
│   │   │       └── WallpaperPickerView.swift# 壁纸切换与透明度滑块
│   │   ├── Onboarding/                     # 首次启动引导
│   │   │   ├── OnboardingView.swift
│   │   │   └── OnboardingViewModel.swift
│   │   └── Common/                         # 通用原子组件
│   │       ├── CategoryChip.swift
│   │       ├── StarRatingView.swift
│   │       └── ToastModifier.swift
│   │
│   ├── Services/                           # 系统能力封装
│   │   ├── NotificationService.swift       # 离线本地通知调度器
│   │   └── HapticFeedbackService.swift     # 触感震动反馈管理器
│   │
│   └── Resources/                          # 资源文件
│       ├── Assets.xcassets                 # 色彩资产、壁纸图、海报图、图标
│       ├── Raw/
│       │   ├── staterooms.json             # 舱房离线索引
│       │   ├── locations.json              # 设施离线索引
│       │   └── Decks/                      # 14 张 SVG 甲板矢量图
│       └── Localizable.xcstrings           # 中文多语言字典
│
└── CruiseAssistantTests/                   # 单元测试目标
    ├── LaneAssignerTests.swift             # 3 车道算法测试
    ├── TimelineLayoutTests.swift           # 布局与刻度折叠测试
    ├── ConflictDetectorTests.swift         # 冲突检测测试
    ├── StateroomCatalogTests.swift         # 舱房搜索测试
    └── ShowPresetTests.swift               # 演出预设测试
```

---

## 4. 领域层与核心算法（1:1 映射规格）

### 4.1 3 车道排版算法 (`LaneAssigner.swift`)
- **车道权重与布局**：
  - 车道 0 (Lane 0)：宽度比例 1.0 (等宽居左)；
  - 车道 1 (Lane 1)：宽度比例 1.0 (等宽居中)；
  - 车道 2 (Lane 2)：宽度比例 0.45 (窄幅靠最右，作为溢出/次要车道)。
- **分配逻辑 (`assignLane`)**：
  - 采用半开区间 `[startMinute, endMinute)` 判定重叠；
  - 依次尝试 0 → 1 → 2，命中首个无冲突的车道；若三栏皆冲突，降级进入车道 2 并在 UI 标记冲突角标。
- **演出优先分配 (`assignShowLane`)**：
  - 演出优先放置于第二栏（Lane 1），冲突时进第三栏（Lane 2），再冲突回退第一栏（Lane 0）。

### 4.2 时间轴折叠与刻度计算 (`TimelineLayout.swift`)
- **空档折叠阈值**：空档时间 $\ge 90$ 分钟时触发折叠（折叠后高度固定为 28 pt）；
- **事件缓冲留白**：事件起止点前后各保留 30 分钟缓冲；
- **刻度计算**：每小时高度支持缩放（60pt ~ 180pt，默认 120pt）；当 `hourHeightPt >= 90` 时展示半点刻度。

### 4.3 离线数据包索引
- **舱房数据 (`staterooms.json`)**：解析 1700+ 间客舱，提供前缀搜索（如输入 `10` 快速联想 10 层所有房间，并返回 `deck` 与 `zone`：船头 BOW / 船中 MID / 船尾 STERN）。
- **设施数据 (`locations.json`)**：解析游轮 7 大主题区域（如 Disney Discovery Reef, San Fransokyo Street, Wayfinder Bay 等）的 50+ 处公共设施、餐厅、剧院与商店。

---

## 5. 数据持久层与实体设计

采用 **SwiftData** (或兼容模式 SQLite/GRDB) 构建 8 张核心实体表：

1. **`VoyageEntity`**：航次基础信息（上船日期、下船日期、最低客用甲板 `minDeck=5`、最高甲板 `maxDeck=19`）。
2. **`DayPlanEntity`**：单日计划状态（`date`, `finalized`, `finalizedAt`）。
3. **`EventEntity`**：日程活动（`date`, `startTime`, `durationMinutes`, `categoryId`, `title`, `deck`, `zone`, `placeName`, `lane`, `waitMinutes`, `reminderMinutesBefore`, `isActive` 等）。
4. **`CategoryEntity`**：活动分类（9 大内置分类：餐食、演出、重要事项、琐事、娱乐项目、见面会、泳池娱乐、商店、青少年活动）。
5. **`LocationPresetEntity`**：地点历史与使用频次。
6. **`PixieDustEntity`**：鱼袋互赠（`direction` = GIVEN/RECEIVED, `deck`, `room`, `levelId`, `note`, `timestamp`）。
7. **`PixieLevelEntity`**：礼品星级（普通/精致/豪华）。
8. **`WishlistItemEntity`**：愿望单待办。

---

## 6. UI 交互体验与细节还原

### 6.1 主界面时间线 (`TimelineView`)
- **日期切换条 (`DateStripView`)**：顶部水平滑动展示航次全天数，高亮当前选中天，圆点标识定稿状态。
- **多车道画布 (`LaneCanvasView`)**：
  - 规划中状态：3 车道并排卡片，支持长按手动换栏；
  - 已定稿状态：单栏满宽只读卡片，支持一键「解锁」回到规划状态；
  - 候场条连接：演出卡片顶部延伸半透明候场条，用 `[4, 4]` 虚线与主体连接。
- **实时指示与今天锚定**：红色当前时间线 + 正在进行提示 Banner + 快速回到当前时间悬浮按钮。

### 6.2 SVG 甲板地图 (`DeckMapView`)
- **手势引擎**：支持双指无级缩放（1.0x ~ 5.0x）与平移拖拽，限制边界；
- **Pin 标记与区域高亮**：根据搜索到的舱房或设施，动态计算甲板图相对坐标并展示动画 Pin 点。

### 6.3 鱼袋房号 9 键键盘 (`PixieKeypadView`)
- 专为游轮房号（4-5位数字）设计的九宫格大按键界面；
- 每次轻触触发 `UIImpactFeedbackGenerator(style: .light)`，提供机械打字机般的敲击手感。

### 6.4 达菲家族 8 色主题与壁纸
- 内置 8 套主题配色：
  - **Judy Blue** (默认经典蓝)
  - **Duffy Blue** (达菲蓝)
  - **LinaBell Pink** (贝儿粉)
  - **ShellieMay Pink** (玫玫粉)
  - **StellaLou Purple** (星黛露紫)
  - **Gelatoni Green** (托尼绿)
  - **Olu Green** (Olu绿)
  - **CookieAnn Yellow** (可琦安黄)
- 7 张高清背景壁纸支持透明度调节（0.05 ~ 0.30，默认 0.10）。

---

## 7. 离线通知系统 (`NotificationService`)

- 使用 `UNUserNotificationCenter` 的 `UNCalendarNotificationTrigger`；
- 根据用户设置在活动开始前 5min、15min、30min 或候场前精准触发本地通知；
- 在设备关机重启、静音无网时依然由系统内核定时推送。

---

## 8. 实施路径与里程碑

1. **里程碑 1 (M1 - 基础框架)**：在 `IOS/` 下完成 Xcode 工程初始化、Assets 导入、JSON 资源加载与数据容器配置。
2. **里程碑 2 (M2 - 领域算法)**：完成 Swift 领域模型移植与全部 50+ 单元测试。
3. **里程碑 3 (M3 - 核心时间线)**：实现 DateStrip、3-Lane Canvas、EventCard、候场条与定稿锁定。
4. **里程碑 4 (M4 - 地图与海报)**：实现 SVG 甲板图手势缩放、搜索高亮与设施海报画廊。
5. **里程碑 5 (M5 - 鱼袋与辅助功能)**：实现 Pixie Dust 9 键键盘、愿望单、主题壁纸与本地通知。
6. **里程碑 6 (M6 - 联调与验收)**：真机联调、深浅色模式测试、性能调优与完整交付。
