# 卡片萌趣人偶分级自适应与纯右侧点缀实施计划（v1.10.14）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构卡片人偶点缀系统，将 60 张人偶素材按大头、半身、全身三级结构归档并应用到根目录和 assets，卡片纯右侧根据卡片高度自适应填充大头/半身/全身立绘，并升级版本至 v1.10.14。

**Architecture:** 
1. 物理重组根目录 `人偶素材/` 与 `app/src/main/assets/dolls/` 目录结构为 `{character}/{head|half_body|full_body}/{filename}.png`；
2. 升级 `DollModels.kt` 引入 `DollTier` 枚举与分级模型，重构 `DollManager.kt` 采用全员均匀随机 + 高度分级自适应 + 智能回退策略；
3. 精简设置项（移除分配模式选择，仅保留卡片人偶立绘点缀总开关）；
4. 重构 `EventCard.kt` 仅保留右侧单偶大尺寸自适应填充；
5. 更新 `build.gradle.kts` 版本号至 `1.10.14 (40)`；
6. 模拟器运行全量验证并重新生成 5 张对比/展示截图至 `人偶效果截图对比/` 交付。

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Android Assets.

## Global Constraints
- Android API min 26, target 34
- versionCode = 40, versionName = "1.10.14"
- 纯右侧单偶展示，最大化自适应利用卡片高度填满右侧空白
- 均匀随机纯函数算法，确定性稳定，滑动 0 闪烁

---

### Task 1: 根目录人偶素材与 assets 目录三级分级重组

**Files:**
- Modify/Move: `人偶素材/{character}/{head|half_body|full_body}/*.png`
- Modify/Move: `app/src/main/assets/dolls/{character}/{head|half_body|full_body}/*.png`

**Interfaces:**
- Produces: 60 个 PNG 文件按三级分类放置在正确的子目录中。

- [ ] **Step 1: 整理根目录 人偶素材/ 下的文件结构**
- [ ] **Step 2: 整理 app/src/main/assets/dolls/ 下的文件结构**
- [ ] **Step 3: 验证文件总数依然为 60（head: 10, half_body: 7, full_body: 43）**
- [ ] **Step 4: 提交资产归档变更**

```bash
git add "人偶素材/" app/src/main/assets/dolls/
git commit -m "assets: reorganize doll illustrations into head, half_body, and full_body tiers"
```

---

### Task 2: 人偶分级领域模型、回退算法与 DollManager 重构

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/DollModels.kt`
- Modify: `app/src/main/java/com/cruise/assistant/domain/DollManager.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/DollManagerTest.kt`

**Interfaces:**
- Produces:
  - `DollTier` (`HEAD`, `HALF_BODY`, `FULL_BODY`)
  - `DollManager.selectDollForCard(eventId: Long, cardHeightDp: Float): String?`

- [ ] **Step 1: 更新单元测试用例 DollManagerTest.kt 覆盖三级高度与回退逻辑**
- [ ] **Step 2: 实现 DollModels.kt 与 DollManager.kt 的分级注册与选择算法**
- [ ] **Step 3: 运行测试验证通过**

```bash
./gradlew testDebugUnitTest --tests com.cruise.assistant.domain.DollManagerTest
```

- [ ] **Step 4: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ app/src/test/java/com/cruise/assistant/domain/
git commit -m "feat: implement height-adaptive tiered doll selection in DollManager"
```

---

### Task 3: 精简设置项与更新应用版本号至 v1.10.14

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

**Interfaces:**
- Produces: `versionCode = 40`, `versionName = "1.10.14"`, 设置界面仅保留简洁的单开关。

- [ ] **Step 1: 更新 build.gradle.kts 版本号**
- [ ] **Step 2: 移除 SettingsStore 与 ViewModel 中已废弃的 distributionMode**
- [ ] **Step 3: 更新 SettingsScreen 保持设置项简洁清晰**
- [ ] **Step 4: 运行全量单元测试**
- [ ] **Step 5: 提交代码**

```bash
git add app/
git commit -m "feat: simplify doll settings and bump version to 1.10.14"
```

---

### Task 4: 卡片纯右侧大尺寸自适应渲染重构 (EventCard & LaneCanvas)

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`

**Interfaces:**
- Produces: `EventCard` 内部仅挂载单张右侧自适应立绘，高度紧凑贴合，小卡片大头、中卡片半身、大卡片全身。

- [ ] **Step 1: 重构 EventCard 移除 Secondary Doll 相关逻辑**
- [ ] **Step 2: 根据 heightDp 动态计算右侧玩偶高度与内边距，实现最大化填满效果**
- [ ] **Step 3: 调整 LaneCanvas 与 TimelineScreen 调用**
- [ ] **Step 4: 运行单元测试与编译检查**

```bash
./gradlew testDebugUnitTest assembleDebug
```

- [ ] **Step 5: 提交代码**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/
git commit -m "feat: maximize right-aligned doll watermark fill in EventCard"
```

---

### Task 5: 模拟器验证与对比截图重新导出交付

**Files:**
- Modify: `人偶效果截图对比/01_定稿单栏_短中长卡片全景.png`
- Modify: `人偶效果截图对比/02_规划三栏_各尺寸卡片点缀.png`
- Modify: `人偶效果截图对比/03_设置页总开关.png`

- [ ] **Step 1: 部署最新 APK 到模拟器**
- [ ] **Step 2: 捕获单栏日程视图（展示安全演习大头贴、见面会半身贴、晚餐全身立绘）**
- [ ] **Step 3: 捕获三栏规划日程视图**
- [ ] **Step 4: 捕获设置页视图**
- [ ] **Step 5: 保存至 人偶效果截图对比/ 目录并提交**
