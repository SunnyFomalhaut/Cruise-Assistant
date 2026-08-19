# v1.9.6 船上设施与运营时间指南 设计文档

## 1. 概述与目标

在设置界面顶部新增“船上设施指南”入口。点击后进入专门的设施查询页面，按类别展示迪士尼探险号所有内置设施（餐饮、泳池娱乐、商店、青少年活动、演出场地、船上服务等）的位置（甲板与区域）、运营时间及当前营业状态，并支持按类别筛选与关键词即时搜索。同时将应用版本更新升级至 `v1.9.6` (versionCode 22)。

## 2. 交互与 UI 设计

### 2.1 设置页入口 (`SettingsScreen.kt`)
- **位置**：设置页面最顶部，卡片 1（时间轴显示范围）上方。
- **样式**：
  - 卡片式容器，圆角 16dp，边框与背景契合主色调。
  - 左侧：圆形底色图标（`Storefront` / `DirectionsBoat`）。
  - 中间：主标题“船上设施指南”，副标题“查询全船餐饮、泳池、商店、服务等运营时间与位置”。
  - 右侧：向右箭头图标（`ChevronRight`）。
- **交互**：点击触发 `onNavigateToFacilities`，通过 Compose Navigation 路由导航至 `Routes.FACILITIES`。

### 2.2 设施指南全屏页面 (`FacilityGuideScreen.kt`)
- **顶部栏 (TopAppBar)**：
  - 左侧：返回导航图标（Back Navigation）。
  - 中间标题：“船上设施指南”。
  - 副信息/统计：展示当前匹配设施总数（如“共 74 处设施”）。
- **搜索栏 (Search Bar)**：
  - 位于顶部栏下方，支持一键清空。
  - 占位文案：“搜索设施名称、甲板 (如 Deck 6) 或区域...”。
  - 匹配逻辑：名称（中/英文）、甲板号（如 `6`、`10`）、区域（`船首`、`船中`、`船尾`）不区分大小写模糊匹配。
- **分类筛选 Tab 栏 (Category Filter Chips)**：
  - 横向滑动 Chip 组：
    - 全部 (`all`)
    - 餐食 (`meal`，包含正餐、快餐、收费、酒吧等)
    - 泳池娱乐 (`pool`)
    - 商店 (`shop`)
    - 青少年活动 (`youth`)
    - 演出场地 (`show`)
    - 船上服务 (`chore`)
- **快捷筛选 (Toggle)**：
  - “仅看营业中” 切换开关（仅在有固定营业时间的设施中筛选当前时刻处于营业时段的条目）。
- **设施卡片列表 (LazyColumn)**：
  - 每一个设施项采用圆角卡片展示：
    - **顶部行**：设施名称（粗体，中英双语展示），右侧展示所属类别彩色胶囊（如绿色的“餐食”、黄色的“商店”等，带对应类别图标）。
    - **位置信息**：甲板与区域 Badge（如 `Deck 10 · 船中`；无甲板时展示 `无固定甲板`）。
    - **运营时间信息**：
      - 24 小时设施：展示 `24小时全天开放`。
      - 普通单/多时段设施：按行/标签展示各时段（如 `08:00 - 22:00`；餐饮若有餐次标注，则显示 `早餐 07:00-10:00`、`午餐 12:00-14:00`、`晚餐 17:45-21:00` 等）。
      - 演出场地（未固定时段）：展示 `以演出时刻表为准`。
    - **实时营业状态**：
      - 若设施配置有时段：当前处于营业时间展示绿色徽标“营业中”，非营业时间展示灰色徽标“休息中”。
      - 若设施未配置固定时段：不展示营业/休息徽标。

## 3. 架构与数据流

### 3.1 数据源
- 直接复用 `LocationCatalog.venuesByCategory()` 内存字典（由 `locations.json` 在应用启动时解析加载完成）。
- 将所有分组扁平化映射为统一的设施视图模型列表 `List<FacilityItemUiModel>`。

### 3.2 领域与表现模型 (`FacilityItemUiModel`)
```kotlin
data class FacilityItemUiModel(
    val placeName: String,
    val categoryKey: String,
    val categoryLabel: String,
    val deck: Int?,
    val zone: ShipZone?,
    val formattedLocation: String,
    val slots: List<OperatingSlot>,
    val is24Hours: Boolean,
    val isOpenNow: Boolean,
    val hasOperatingHours: Boolean,
    val diningGroupLabel: String?,
)
```

### 3.3 状态管理与 ViewModel (`FacilityGuideViewModel.kt`)
- `FacilityGuideUiState`：
  - `searchQuery: String`
  - `selectedCategory: String`（默认 `"all"`）
  - `onlyOpenNow: Boolean`（默认 `false`）
  - `filteredFacilities: List<FacilityItemUiModel>`
  - `totalCount: Int`
- `currentTime`：通过 `LocalTime.now()` 在计算 `isOpenNow` 时动态评估。

### 3.4 路由注册 (`CruiseNavHost.kt`)
- `Routes.FACILITIES = "facilities"`
- 注册 `composable(Routes.FACILITIES)`，支持从设置页平滑跳转与返回。

## 4. 版本与发布配置
- `app/build.gradle.kts`:
  - `versionCode = 22`
  - `versionName = "1.9.6"`

## 5. 验证计划
1. **单元测试与数据解析验证**：
   - 验证 `LocationCatalog` 中所有 category（meal, pool, shop, youth, show, chore）均能正确解析并转换为设施卡片。
   - 验证搜索逻辑（中英文匹配、甲板数字匹配、区域匹配）。
   - 验证营业时间判断逻辑（24小时、跨午夜时段、当前时间在营业期内外）。
2. **UI 交互验证**：
   - 编译运行，进入设置界面，确认顶部新增“船上设施指南”入口卡片。
   - 点击入口进入设施指南页面，验证分类切换、搜索栏输入、状态过滤和返回按键操作顺畅。
