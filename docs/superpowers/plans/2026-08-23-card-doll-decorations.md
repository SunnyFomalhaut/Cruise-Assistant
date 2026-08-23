# 主界面卡片萌趣人偶点缀（Card Doll Decorations）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为日程时间轴的事件卡片引入半透明萌趣人偶立绘点缀系统，支持色彩亲和加权与全员随机双模式，并支持大卡片双偶空间感知排版与设置项控制，最后导出对比截图。

**Architecture:** 
1. 将 7 个角色的 60 张 PNG 导入 Android `assets/dolls/`；
2. 实现 `DollManager` 管理资产索引、位图解码与缓存、HSV 颜色亲和度匹配算法及稳定哈希选偶逻辑；
3. 在 `SettingsStore` 中扩展 `cardDollsEnabled` 与 `cardDollDistribution`，并在 `SettingsScreen` 提供切换控件；
4. 在 `EventCard` 中根据卡片高度与留白几何计算挂载主副人偶半透明背景图层；
5. 在模拟器上运行并导出两种模式的高清截图至根目录 `人偶效果截图对比/` 供用户把关。

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore, JUnit 4, Android Assets.

## Global Constraints
- Android API min 26, target 34
- Compose Multiplatform / Jetpack Compose
- 保持 60/120fps 流畅滑动，位图需进行内存缓存并按需下采样
- 哈希选取算法必须纯函数且确定性，避免重组闪烁

---

### Task 1: 规范化素材导入与资产归档

**Files:**
- Create: `app/src/main/assets/dolls/duffy/*`
- Create: `app/src/main/assets/dolls/shelliemay/*`
- Create: `app/src/main/assets/dolls/stellalou/*`
- Create: `app/src/main/assets/dolls/linabell/*`
- Create: `app/src/main/assets/dolls/gelatoni/*`
- Create: `app/src/main/assets/dolls/cookieann/*`
- Create: `app/src/main/assets/dolls/olumel/*`

**Interfaces:**
- Produces: 60 个标准 PNG 资产文件，位于 `assets/dolls/{character_key}/{filename}.png`

- [ ] **Step 1: 复制并规范化人偶素材至 assets/dolls 目录**

复制根目录下 `人偶素材/` 下所有文件夹和 PNG 文件至 `app/src/main/assets/dolls/`。

- [ ] **Step 2: 验证文件存在且数量为 60**

检查每个角色文件夹下的文件数量：
- duffy: 9
- shelliemay: 10
- stellalou: 10
- linabell: 11
- gelatoni: 6
- cookieann: 7
- olumel: 7
总数: 60

- [ ] **Step 3: 提交资产变更**

```bash
git add app/src/main/assets/dolls/
git commit -m "assets: add duffy and friends doll PNG illustrations"
```

---

### Task 2: 人偶领域模型、颜色亲和矩阵与分配管理器 (DollManager)

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/DollModels.kt`
- Create: `app/src/main/java/com/cruise/assistant/domain/DollManager.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/DollManagerTest.kt`

**Interfaces:**
- Consumes: `CruiseEvent`, `Category`, `DollDistributionMode`
- Produces: 
  - `DuffyCharacter` enum (`DUFFY`, `SHELLIEMAY`, `STELLALOU`, `LINABELL`, `GELATONI`, `COOKIEANN`, `OLUMEL`)
  - `DollDistributionMode` enum (`COLOR_AFFINITY`, `UNIFORM_RANDOM`)
  - `DollManager.selectDollsForCard(eventId: Long, cardColorArgb: Int, cardHeightDp: Float, blankHeightDp: Float, mode: DollDistributionMode): Pair<String?, String?>`
  - `DollManager.loadDollBitmap(context: Context, assetPath: String, maxDimDp: Int): Bitmap?`

- [ ] **Step 1: 编写失败的单元测试 DollManagerTest.kt**

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DollManagerTest {
    @Test
    fun testCompactCard_ReturnsNoDolls() {
        val (primary, secondary) = DollManager.selectDollsForCard(
            eventId = 101L,
            cardColorArgb = 0xFFFFD54F.toInt(),
            cardHeightDp = 40f,
            blankHeightDp = 10f,
            mode = DollDistributionMode.UNIFORM_RANDOM,
        )
        assertNull(primary)
        assertNull(secondary)
    }

    @Test
    fun testNormalCard_ReturnsPrimaryDollOnly() {
        val (primary, secondary) = DollManager.selectDollsForCard(
            eventId = 102L,
            cardColorArgb = 0xFFFFD54F.toInt(),
            cardHeightDp = 70f,
            blankHeightDp = 30f,
            mode = DollDistributionMode.UNIFORM_RANDOM,
        )
        assertNotNull(primary)
        assertNull(secondary)
    }

    @Test
    fun testLargeCard_ReturnsDualDolls() {
        val (primary, secondary) = DollManager.selectDollsForCard(
            eventId = 103L,
            cardColorArgb = 0xFFFFD54F.toInt(),
            cardHeightDp = 120f,
            blankHeightDp = 70f,
            mode = DollDistributionMode.UNIFORM_RANDOM,
        )
        assertNotNull(primary)
        assertNotNull(secondary)
    }

    @Test
    fun testDeterministicSelection() {
        val res1 = DollManager.selectDollsForCard(104L, 0xFFCE93D8.toInt(), 80f, 40f, DollDistributionMode.COLOR_AFFINITY)
        val res2 = DollManager.selectDollsForCard(104L, 0xFFCE93D8.toInt(), 80f, 40f, DollDistributionMode.COLOR_AFFINITY)
        assertEquals(res1, res2)
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

运行 `./gradlew testDebugUnitTest --tests com.cruise.assistant.domain.DollManagerTest`
预期：编译失败（DollManager / DollDistributionMode 未定义）

- [ ] **Step 3: 实现 DollModels.kt 与 DollManager.kt**

编写 `DollModels.kt` 与 `DollManager.kt`，包含 HSV 颜色亲和度加权、种子哈希选取与双偶规则。

- [ ] **Step 4: 运行测试验证通过**

运行 `./gradlew testDebugUnitTest --tests com.cruise.assistant.domain.DollManagerTest`
预期：PASS

- [ ] **Step 5: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/domain/DollModels.kt app/src/main/java/com/cruise/assistant/domain/DollManager.kt app/src/test/java/com/cruise/assistant/domain/DollManagerTest.kt
git commit -m "feat: implement DollManager with color affinity and dual-doll selection"
```

---

### Task 3: 设置项集成与模式切换 (SettingsStore & SettingsScreen)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

**Interfaces:**
- Consumes: `DollDistributionMode`, `SettingsStore`
- Produces: 
  - `AppSettings.cardDollsEnabled: Boolean`
  - `AppSettings.cardDollDistribution: DollDistributionMode`
  - `SettingsViewModel.onCardDollsToggle(enabled: Boolean)`
  - `SettingsViewModel.onCardDollDistributionChange(mode: DollDistributionMode)`

- [ ] **Step 1: 在 strings.xml 中添加多语言文案**
- [ ] **Step 2: 在 AppSettings & SettingsStore 中添加持久化字段**
- [ ] **Step 3: 在 SettingsViewModel & SettingsScreen 中添加开关与模式 FilterChip**
- [ ] **Step 4: 运行全量单元测试**

运行 `./gradlew testDebugUnitTest`
预期：PASS

- [ ] **Step 5: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt app/src/main/java/com/cruise/assistant/ui/settings/ app/src/main/res/
git commit -m "feat: add card doll decorations toggles and mode selector to settings"
```

---

### Task 4: 卡片空间感知双偶排版渲染 (EventCard & LaneCanvas)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt`

**Interfaces:**
- Consumes: `DollManager`, `AppSettings.cardDollsEnabled`, `AppSettings.cardDollDistribution`
- Produces: 在 `EventCard` 内部渲染半透明主偶（右下角）与副偶（左下角）图层

- [ ] **Step 1: 在 EventCard 参数中接收 dollsEnabled 与 dollDistributionMode**
- [ ] **Step 2: 计算 blankHeightDp 并调用 DollManager.selectDollsForCard()**
- [ ] **Step 3: 在卡片背景上层、文字下层通过 Box 渲染右下角与左下角半透明 Image**
- [ ] **Step 4: 在 LaneCanvas 中将 settings.cardDollsEnabled / dollDistribution 传递给 EventCard**
- [ ] **Step 5: 运行全量单元测试与编译验证**

运行 `./gradlew testDebugUnitTest assembleDebug`
预期：BUILD SUCCESSFUL

- [ ] **Step 6: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt
git commit -m "feat: render space-aware dual doll watermarks on event cards"
```

---

### Task 5: 模拟器验证与对比截图导出交付

**Files:**
- Create: `人偶效果截图对比/01_色彩亲和加权_定稿单栏.png`
- Create: `人偶效果截图对比/02_色彩亲和加权_规划三栏.png`
- Create: `人偶效果截图对比/03_全员完全随机_定稿单栏.png`
- Create: `人偶效果截图对比/04_全员完全随机_规划三栏.png`
- Create: `人偶效果截图对比/05_设置页开关与切换.png`

- [ ] **Step 1: 启动 Android 模拟器并安装运行最新 APK**
- [ ] **Step 2: 切换到色彩亲和加权模式并截取单栏、三栏日程图**
- [ ] **Step 3: 切换到全员完全随机模式并截取单栏、三栏日程图**
- [ ] **Step 4: 截取设置页图**
- [ ] **Step 5: 验证截图清晰度并在根目录归档**
