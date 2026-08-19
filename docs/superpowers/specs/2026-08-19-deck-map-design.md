# 全船甲板手势地图 (Deck Map) 设计文档

## 1. 概述与目标

在应用中实现交互式的全船甲板平面图浏览功能。用户可以在主界面顶栏、设置页以及设施指南中快速进入甲板地图页面，自由切换 1~19 层甲板。针对已有 SVG 图纸的甲板（Deck 5、6、7、8、9），提供丝滑的双指缩放（Pinch to Zoom）、拖拽平移（Pan）与复位功能；对暂无图纸的甲板（Deck 1~4、10~19）提供优雅的占位提示，并预留后续新增 SVG 文件时的即时自动兼容机制。

## 2. 界面与交互设计

### 2.1 入口布局
- **主界面顶栏 (`TimelineScreen.kt`)**：
  - 在顶栏右上角快捷操作区新增「甲板地图」图标按钮（`Icons.Outlined.Map`），放置在仙尘与设置按钮之间。
  - 点击跳转至 `Routes.DECK_MAP`。
- **设置界面 (`SettingsScreen.kt`)**：
  - 在“船上设施指南”下方新增「全船甲板地图」入口卡片，点击同样导航至 `Routes.DECK_MAP`。
- **设施指南 (`FacilityGuideScreen.kt`)**：
  - 在顶栏右侧新增地图快捷按钮，方便从设施列表直接跳转查阅地图。

### 2.2 甲板地图页面 (`DeckMapScreen.kt`)
- **顶部栏 (TopAppBar)**：
  - 左侧：返回图标按钮。
  - 中间：当前甲板名称（如 `Deck 6 甲板平面图`）。
  - 右侧：缩放复位快捷按钮（`Icons.Outlined.RestartAlt` / `CenterFocusStrong`）。
- **左侧紧凑型甲板切换器 (Deck Selector Rail)**：
  - 纵向悬浮条，自上而下按甲板从高到低排列（`D19`, `D18`, ..., `D1`）。
  - 每个 Chip 显示甲板简称（如 `D6`），已选中甲板呈现高亮背景色（`CruiseAzure`）。
  - 已有 SVG 图纸的甲板展示绿点或正常色；无图纸的甲板展示淡灰色，但依然支持点击查看提示。
- **中央地图视口 (Gesture Map Canvas)**：
  - **有图纸状态**：
    - 采用硬件加速 WebView 容器加载本地 assets 中的 SVG 文件（如 `file:///android_asset/decks/deck_5.svg`）。
    - 视口自适应手机屏幕宽度，居中展示修长船体。
    - 支持流畅的双指手势缩放（Pinch-to-zoom 1.0x ~ 8.0x）、单指拖动平移、双击缩放。
    - 船首/船尾方向标注（船头在上方，船尾在下方）。
  - **无图纸状态**：
    - 展示居中的优雅占位视图（船体线框图标 + “Deck X 平面图采集中，后续版本将持续补充”）。

## 3. 架构与数据管理

### 3.1 资源目录规范
- 将项目根目录下的 `甲板图/` 迁移并标准化命名存入 Android Assets：
  `app/src/main/assets/decks/`
  - `deck_5.svg`
  - `deck_6.svg`
  - `deck_7.svg`
  - `deck_8.svg`
  - `deck_9.svg`
- 命名规则统一为：`deck_{deckNumber}.svg`。

### 3.2 领域与助手类 (`DeckMapManager.kt`)
- 提供甲板信息查询与可用性判断：
  ```kotlin
  data class DeckInfo(
      val deckNumber: Int,
      val name: String,
      val hasMap: Boolean,
      val assetPath: String,
  )

  object DeckMapRepository {
      val AVAILABLE_DECKS = setOf(5, 6, 7, 8, 9)
      fun getDecks(maxDeck: Int = 19): List<DeckInfo>
      fun hasDeckMap(deckNumber: Int): Boolean = deckNumber in AVAILABLE_DECKS
      fun assetPathFor(deckNumber: Int): String = "decks/deck_$deckNumber.svg"
  }
  ```

### 3.3 状态模型与 ViewModel (`DeckMapViewModel.kt`)
- `DeckMapUiState`：
  - `selectedDeck: Int`（默认根据传入参数或默认 `5`）
  - `allDecks: List<DeckInfo>`
  - `hasMap: Boolean`
  - `assetUrl: String`
- 方法：`onSelectDeck(deckNumber: Int)`

### 3.4 路由注册与参数传递
- `Routes.DECK_MAP = "deck_map?deck={deck}"`
- 支持传入初始选中的甲板号（如从设施指南点进指定甲板直接定位）。

## 4. 验证计划
1. **单元测试与数据逻辑验证**：
   - 验证 `DeckMapRepository` 甲板列表生成（1~19层降序排列、可用性匹配、asset 路径格式）。
2. **SVG 渲染与手势交互验证**：
   - 验证 Deck 5、6、7、8、9 的 SVG 能够正确在 WebView / Compose 容器中渲染，矢量字体与图标清晰。
   - 验证双指缩放、拖拽与复位无卡顿。
   - 验证缺省甲板（如 Deck 1、11）能正确展示占位提示。
3. **入口与路由跳转验证**：
   - 验证主界面顶栏、设置页、设施指南入口均可正常打开甲板地图并返回。
