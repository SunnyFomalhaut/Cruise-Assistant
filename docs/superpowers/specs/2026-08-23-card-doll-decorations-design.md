# 需求设计文档：主界面卡片萌趣人偶点缀（Card Doll Decorations）

## 1. 概述与目标

在日程时间轴主界面上，各类事件卡片（尤其是单栏定稿视图或持续时间超过 1 小时的餐饮、演出、休闲等卡片）内部往往存在较大的空白区域。
为了增添迪士尼达菲家族（Duffy & Friends）七位宝宝（达菲、雪莉玫、星黛露、玲娜贝儿、杰拉多尼、可琦安、奥乐米拉）的软萌温馨氛围，本项目在主界面卡片空白处引入**半透明萌趣人偶立绘点缀系统**。

### 核心设计目标
1. **空白自适应与双偶互动（Space-Aware Dual-Doll Layout）**：
   - 紧凑小卡片（高度 < 44dp）不显示人偶，保持纯粹易读；
   - 常规卡片（44dp ~ 90dp）在右下角以半透明水印形式点缀 1 位宝宝；
   - 纵向留白充足的大卡片（高度 ≥ 90dp 且底部净留白 ≥ 50dp）激活双偶互动，分别在右下角与左下角展示 2 位不同宝宝，彻底点缀大片空白。
2. **双模式分配引擎（Dual Distribution Modes）**：
   - **全员随机大家庭模式（Uniform Random）**：全员 7 位角色 60 张素材均等概率随机轮播，打造欢聚大乱斗感。
   - **色彩亲和加权模式（Color Affinity Weighted）**：每个角色在对应代表色彩/分类的卡片上享受 4~5 倍权重加成（例如黄色卡片偏向可琦安/达菲、紫色卡片偏向星黛露、绿色偏向奥乐米拉、粉色偏向贝儿/玫玫、青色偏向杰拉多尼），其余宝宝仍保留偶现概率。
3. **稳定散列防闪烁（Deterministic Stable Hash）**：
   - 卡片上人偶的选取由 `eventId` 派生种子，列表滑动或切换日期时保持确定性渲染，不发生闪烁。
4. **轻量设置与一键开关（Settings Integration）**：
   - 在设置页提供总开关与分配模式选择，简单直观。
5. **模拟器效果对比交付（Screenshots Export）**：
   - 自动化导出两种模式下单栏与三栏日程的高清截图至根目录 `人偶效果截图对比/` 文件夹。

---

## 2. 素材资产组织与角色色彩矩阵

### 2.1 素材文件导入规划
将根目录下 `人偶素材/` 的 60 张 PNG 导入 Android 资产目录 `app/src/main/assets/dolls/`：
* `app/src/main/assets/dolls/duffy/` (9 张: `duffy_01.png` ~ `duffy_09.png` 或保留源命名)
* `app/src/main/assets/dolls/shelliemay/` (10 张)
* `app/src/main/assets/dolls/stellalou/` (10 张)
* `app/src/main/assets/dolls/linabell/` (11 张)
* `app/src/main/assets/dolls/gelatoni/` (6 张)
* `app/src/main/assets/dolls/cookieann/` (7 张)
* `app/src/main/assets/dolls/olumel/` (7 张)

### 2.2 角色色彩亲和矩阵 (Color Affinity Matrix)
```kotlin
enum class DuffyCharacter(
    val key: String,
    val displayName: String,
    val assetDir: String,
) {
    DUFFY("duffy", "达菲", "dolls/duffy"),
    SHELLIEMAY("shelliemay", "雪莉玫", "dolls/shelliemay"),
    STELLALOU("stellalou", "星黛露", "dolls/stellalou"),
    LINABELL("linabell", "玲娜贝儿", "dolls/linabell"),
    GELATONI("gelatoni", "杰拉多尼", "dolls/gelatoni"),
    COOKIEANN("cookieann", "可琦安", "dolls/cookieann"),
    OLUMEL("olumel", "奥乐米拉", "dolls/olumel"),
}
```

**色彩判定与加权规则**：
通过将卡片背景色/类别 ARGB 转换为 HSV 色相与饱和度：
* **紫色系 (Hue ∈ [250°, 310°])** $\rightarrow$ 优先加权 `STELLALOU` (星黛露)
* **黄色/暖金系 (Hue ∈ [35°, 65°])** $\rightarrow$ 优先加权 `COOKIEANN` (可琦安)
* **绿色系 (Hue ∈ [80°, 165°])** $\rightarrow$ 优先加权 `OLUMEL` (奥乐米拉)
* **青色/薄荷系 (Hue ∈ [165°, 205°])** $\rightarrow$ 优先加权 `GELATONI` (杰拉多尼)
* **粉色/玫红系 (Hue ∈ [310°, 360°] 或 [0°, 15°])** $\rightarrow$ 优先加权 `LINABELL` (玲娜贝儿) 与 `SHELLIEMAY` (雪莉玫)
* **暖棕/大地色系 (低饱和/暖色调)** $\rightarrow$ 优先加权 `DUFFY` (达菲)

加权算法：命中角色的抽样权重设为 5，其余 6 位角色的权重各为 1。

---

## 3. 架构设计与核心组件

### 3.1 `DollManager` 单例
负责素材路径索引、位图解码与缓存、以及人偶选择逻辑：
```kotlin
object DollManager {
    // 内存位图缓存 (LRU / ConcurrentHashMap)
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    
    // 初始化注册所有 60 张立绘 asset 路径
    private val characterAssets: Map<DuffyCharacter, List<String>>
    
    fun loadDollBitmap(context: Context, assetPath: String, maxDimDp: Int = 120): Bitmap?
    
    /**
     * 根据卡片属性与分配模式，确定卡片上展示的人偶（主位、副位）
     */
    fun selectDollsForCard(
        eventId: Long,
        cardColorArgb: Int,
        cardHeightDp: Float,
        blankHeightDp: Float,
        mode: DollDistributionMode,
    ): Pair<String?, String?>
}
```

### 3.2 空间感知排版计算 (`EventCard.kt`)
* **文字区高度估计 $H_{text}$**：
  $$H_{text} = H_{wait} + \text{bodyLineCount} \times 18\text{dp} + \text{Padding}$$
* **底部剩余高度 $H_{blank}$**：
  $$H_{blank} = H_{card} - H_{text}$$
* **点缀渲染层**：
  在 `EventCard` 的 `Row/Column` 内容层最底层（背景之上、文字之下），通过 `Box(Modifier.fillMaxSize())` 挂载半透明 `Image`：
  - **右下角人偶 (Primary Doll)**：
    - `Alignment.BottomEnd`，透明度 $\alpha = 0.24f$
    - 尺寸自适应：$W, H \le \min(H_{card} \times 0.65, 84\text{dp})$，右下角内边距 $2\text{dp}$。
  - **左下角人偶 (Secondary Doll)**：
    - 触发条件：$H_{card} \ge 90\text{dp}$ 且 $H_{blank} \ge 50\text{dp}$
    - `Alignment.BottomStart`，透明度 $\alpha = 0.20f$
    - 尺寸自适应：$W, H \le \min(H_{blank} \times 0.85, 72\text{dp})$，左下角内边距 $10\text{dp}$（避开左侧色条）。

### 3.3 数据存储与设置 (`SettingsStore.kt` & `SettingsScreen.kt`)
```kotlin
enum class DollDistributionMode {
    COLOR_AFFINITY,  // 色彩亲和加权
    UNIFORM_RANDOM,  // 全员完全随机
}

data class AppSettings(
    // ... 原有配置
    val cardDollsEnabled: Boolean = true,
    val cardDollDistribution: DollDistributionMode = DollDistributionMode.COLOR_AFFINITY,
)
```

在 `SettingsScreen` 的“外观与主题”区域中增加设置卡片：
- **卡片人偶点缀**（Switch 开关）
- **分配模式切换**（FilterChip / Segmented：色彩亲和加权 vs 全员完全随机）

---

## 4. 截图对比交付流程

在 Android 模拟器上，对包含多类型事件（餐食、演出、活动、长短卡片）的典型日程（如 Day 6 / Day 1）：
1. 切换至 **色彩亲和加权模式**，分别截取：
   - 定稿单栏日程视图
   - 规划三栏日程视图
2. 切换至 **全员完全随机模式**，分别截取：
   - 定稿单栏日程视图
   - 规划三栏日程视图
3. 截取 **设置页卡片配置视图**。
4. 将 5 张截图保存至项目根目录 `人偶效果截图对比/` 文件夹。

---

## 5. 自审检查（Self-Review）
- [x] **无占位符 (No TBD/TODO)**：所有算法、尺寸阈值、色彩对应表均完整定义。
- [x] **无内部冲突**：文字排版与人偶图层分离（人偶置于背景层上方、文本层下方），既不影响文字清晰度，又不干扰点击事件。
- [x] **范围明确**：专注于卡片半透明点缀与模式对比，不影响原有海报详情与壁纸系统。
- [x] **性能保障**：资产按需采样解码并加入内存缓存，稳定种子保证滑动不闪烁、无多余重组开销。
