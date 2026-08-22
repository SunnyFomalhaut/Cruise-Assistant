# 预置演出地点关联与差异化候场时间实施计划 (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复预置演出地点关联及定位（"花园舞台 Garden Stage"），实现按剧场/演出差异化默认候场时间（迪士尼剧院/狮子王烟花 45m、大白剧院/花园舞台 15m、寻路者湾 30m），支持创建演出时选择 10~120 分钟候场档位，升级版本至 1.10.13。

**Architecture:**
- Domain 层：在 `ShowPreset.kt` 中修正花园舞台地点名称，加入 `resolveDefaultWaitMinutes` 解析纯函数；`CruiseEvent` 与 `EventEntity` 扩充 `waitMinutes: Int?`；
- Data 层：Room DB 升级至 v7（`AutoMigration(from = 6, to = 7)`），`VoyageRepository` 写入预置演出默认候场时间；
- UI & Reminder 层：`EventCard`/`LaneCanvas`/`EventDetailDialog`/`ReminderScheduler` 统一消费 `effectiveWaitMinutes`；`EventSheet` 提供 10~120m（步长 5m）候场选择器与自动联动。

**Tech Stack:** Kotlin, Jetpack Compose, Room (SQLite), Android AlarmManager, JUnit4.

## Global Constraints
- `versionCode = 39`, `versionName = "1.10.13"`
- 花园舞台名称：`"花园舞台 Garden Stage"`
- 候场时间规则：
  - 华特迪士尼剧院：45 分钟
  - 狮子王烟花：45 分钟
  - 大白剧院：15 分钟
  - 花园舞台：15 分钟
  - 寻路者湾：30 分钟
  - 兜底：30 分钟
- 候场选择档位：10 ~ 120 分钟（步长 5 分钟，共 23 档）

---

### Task 1: Domain 预置演出地点、默认候场时间与计算函数

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt`
- Modify: `app/src/main/java/com/cruise/assistant/domain/Models.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt`

**Interfaces:**
- Consumes: `LocationCatalogEntry`, `locations.json`
- Produces: `ShowPreset.defaultWaitMinutes`, `resolveDefaultWaitMinutes()`, `CruiseEvent.effectiveWaitMinutes`, `CruiseEvent.waitMinutes`

- [ ] **Step 1: 编写/更新针对新地点与候场时间的单元测试**

在 `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt` 中更新 `catalogRows_pinnedToSpec` 并增加候场时间测试：
```kotlin
    @Test
    fun defaultWaitMinutes_matchesSpecRules() {
        assertEquals(45, resolveDefaultWaitMinutes("华特迪士尼剧院 Walt Disney Theatre", deck = 6))
        assertEquals(45, resolveDefaultWaitMinutes("露天泳池", deck = 17, title = "狮子王烟花"))
        assertEquals(15, resolveDefaultWaitMinutes("大白剧院 Baymax Cinemas", deck = 7))
        assertEquals(15, resolveDefaultWaitMinutes("花园舞台 Garden Stage", deck = 10))
        assertEquals(30, resolveDefaultWaitMinutes("寻路者湾 Wayfinder Bay", deck = 10))
        assertEquals(30, resolveDefaultWaitMinutes("未知地点", deck = 5))
    }
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `.\gradlew testDebugUnitTest --tests com.cruise.assistant.domain.ShowPresetTest`
Expected: FAIL (unresolved reference resolveDefaultWaitMinutes / catalog mismatch)

- [ ] **Step 3: 实现 `ShowPreset.kt` 与 `Models.kt` 的改动**

在 `Models.kt` 中为 `CruiseEvent` 增加 `val waitMinutes: Int? = null`。
在 `ShowPreset.kt` 中：
1. `ShowPreset` 数据类增加 `val defaultWaitMinutes: Int = 30`；
2. `DISNEY_ADVENTURE_SHOWS` 中修正 `show_set_sail`、`show_avengers`、`show_baymax_expo` 的地点为 `"花园舞台 Garden Stage"`，并配置对应 `defaultWaitMinutes`（花园舞台 15，狮子王 45，迪士尼剧院 45，寻路者湾 30）；
3. 添加 `resolveDefaultWaitMinutes(...)` 函数；
4. 添加 `CruiseEvent.effectiveWaitMinutes` 扩展属性；
5. 更新 `showEffectiveRange` 与 `formatWaitStart` 使用 `event.effectiveWaitMinutes`。

- [ ] **Step 4: 运行测试并确认通过**

Run: `.\gradlew testDebugUnitTest --tests com.cruise.assistant.domain.ShowPresetTest`
Expected: PASS

- [ ] **Step 5: 提交改动**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt app/src/main/java/com/cruise/assistant/domain/Models.kt app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt
git commit -m "feat(domain): update show preset locations and dynamic wait time resolver"
```

---

### Task 2: Database 实体、Room 迁移与航次生成预置数据

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt`

**Interfaces:**
- Consumes: `CruiseEvent.waitMinutes`, `ShowPreset.defaultWaitMinutes`
- Produces: Room Schema v7 with AutoMigration(from = 6, to = 7), `EventEntity.waitMinutes`

- [ ] **Step 1: 修改 `EventEntity.kt` 和 `Mappers.kt`**

在 `EventEntity` 中添加 `val waitMinutes: Int? = null`。
在 `Mappers.kt` 的 `EventEntity.toDomain()` 和 `CruiseEvent.toEntity()` 中映射 `waitMinutes`。

- [ ] **Step 2: 更新 `CruiseDatabase.kt`**

升级版本号为 `version = 7`，在 `autoMigrations` 中添加 `AutoMigration(from = 6, to = 7)`。

- [ ] **Step 3: 更新 `VoyageRepository.kt`**

在 `createVoyage` 生成预置演出事件时，将 `waitMinutes = show.defaultWaitMinutes` 传入新建的 `EventEntity`，且 `effStart` 使用 `startMin - show.defaultWaitMinutes`。

- [ ] **Step 4: 编译并验证 DB Schema 生成**

Run: `.\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL，并在 `app/schemas/.../7.json` 生成最新 schema。

- [ ] **Step 5: 运行相关单元测试**

Run: `.\gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: 提交改动**

```bash
git add app/src/main/java/com/cruise/assistant/data/db/entity/EventEntity.kt app/src/main/java/com/cruise/assistant/data/db/Mappers.kt app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt app/schemas/
git commit -m "feat(data): add waitMinutes to EventEntity and migrate Room DB to v7"
```

---

### Task 3: 时间轴、卡片详情与提醒排程接入动态候场时间

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt`
- Modify: `app/src/main/java/com/cruise/assistant/reminder/ReminderScheduler.kt`

**Interfaces:**
- Consumes: `CruiseEvent.effectiveWaitMinutes`, `formatWaitStart(event)`
- Produces: Dynamic wait segment height in `LaneCanvas`, dynamic wait text in `EventDetailDialog`, dynamic reminder timing in `ReminderScheduler`

- [ ] **Step 1: 更新 `LaneCanvas.kt`**

在 `LaneCanvas.kt` 中：
```kotlin
val isShow = categories[event.categoryId]?.iconKey == "show"
val waitMinutes = if (isShow) event.effectiveWaitMinutes else 0
val (effStart, effEnd) = showEffectiveRange(event, isShow)
...
val waitHeightDp = if (isShow) {
    val band = (layout.minuteToDp(effStart + waitMinutes) - top).dp
    band.coerceAtMost(heightDp - TITLE_RESERVE_DP - SPACER_DP).coerceAtLeast(0.dp)
} else {
    0.dp
}
```

- [ ] **Step 2: 更新 `EventDetailDialog.kt`**

在 `EventDetailDialog.kt` 中：
将第 231 行的硬编码“提前 30 分钟”改为：
```kotlin
val waitMinutes = event.effectiveWaitMinutes
Text(
    text = "建议候场：$waitStartStr 入场（提前 ${waitMinutes} 分钟）",
    style = MaterialTheme.typography.bodySmall,
    color = Slate700,
)
```

- [ ] **Step 3: 更新 `ReminderScheduler.kt`**

在 `ReminderScheduler.kt` 中：
```kotlin
val isShow = categories[event.categoryId]?.iconKey == "show"
val queueLeadMinutes = if (isShow) event.effectiveWaitMinutes else 0
val baseMs = if (isShow) showMs - queueLeadMinutes * 60_000L else showMs
```

- [ ] **Step 4: 运行单元测试**

Run: `.\gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交改动**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt app/src/main/java/com/cruise/assistant/ui/timeline/EventDetailDialog.kt app/src/main/java/com/cruise/assistant/reminder/ReminderScheduler.kt
git commit -m "feat(ui): connect dynamic lead time to LaneCanvas, EventDetailDialog, and ReminderScheduler"
```

---

### Task 4: 创建/编辑事件页面（EventSheet）支持 10~120 分钟候场时间选择与联动

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `WAIT_MINUTE_OPTIONS = (10..120 step 5)`, `resolveDefaultWaitMinutes`
- Produces: `EventSheetState.waitMinutes`, `onWaitMinutesChange(Int)`, 候场时间选择卡片

- [ ] **Step 1: 更新 `strings.xml`**

在 `app/src/main/res/values/strings.xml` 中添加候场时间相关文案：
```xml
<string name="lead_time_label">提前候场时间</string>
<string name="lead_time_format">%d 分钟</string>
<string name="lead_time_hint">建议提前候场以获取较好座位</string>
```

- [ ] **Step 2: 更新 `EventSheetViewModel.kt`**

1. `EventSheetState` 增加 `val waitMinutes: Int = 30`；
2. 加载已有事件时（`editingEventId != 0L`），读取 `event.waitMinutes ?: event.effectiveWaitMinutes`；
3. `onCategorySelect` 切换到演出时，按当前选择的地点/演出解析推荐候场时间；
4. `onShowPresetSelect` 选中预置演出时，更新 `waitMinutes = preset.defaultWaitMinutes`；
5. `onSelectPresetLocation` 选中地点且当前类别为 show 时，更新 `waitMinutes = resolveDefaultWaitMinutes(entry.placeName, entry.deck)`；
6. 提供 `onWaitMinutesChange(value: Int)` 更新 `state.waitMinutes`；
7. `save()` 保存时，将 `waitMinutes = if (s.categoryIconKey == "show") s.waitMinutes else null` 赋给 `CruiseEvent`。

- [ ] **Step 3: 更新 `EventSheet.kt`**

当 `state.categoryIconKey == "show"` 时，在起止时间卡片上方（或下方）渲染提前候场时间选择卡片：
- 展示当前选中的候场时间（如 `45 分钟`）；
- 使用 `ExposedDropdownMenuBox` 提供 10~120 分钟（5 分钟一档）的下拉选择列表；
- 附带说明文案（如“建议提前候场以获取较好座位”）。

- [ ] **Step 4: 运行单元测试**

Run: `.\gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: 提交改动**

```bash
git add app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt app/src/main/java/com/cruise/assistant/ui/event/EventSheet.kt app/src/main/res/values/strings.xml
git commit -m "feat(ui): add wait time selector (10-120m) and auto-fill in EventSheet"
```

---

### Task 5: 升级版本号至 1.10.13 与模拟器真机验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 升级版本号**

在 `app/build.gradle.kts` 中：
- `versionCode = 39`
- `versionName = "1.10.13"`

- [ ] **Step 2: 运行全部单元测试**

Run: `.\gradlew testDebugUnitTest`
Expected: 100% PASS

- [ ] **Step 3: 安装到模拟器**

Run: `.\gradlew installDebug`
Expected: BUILD SUCCESSFUL & Installed

- [ ] **Step 4: 模拟器功能真机自动化验证**

使用 adb 命令与 uiautomator dump：
1. 启动应用：`adb shell am start -n com.cruise.assistant.debug/com.cruise.assistant.MainActivity`；
2. 验证主界面第一天“扬帆起航”演出卡片显示 `D10 船中 花园舞台 Garden Stage`，候场段为 15 分钟高度；
3. 点击“扬帆起航”卡片弹出 `EventDetailDialog`，验证文案为“建议候场：13:00 入场（提前 15 分钟）”，点击“定位”验证跳转至 Deck 10 并成功定位 Garden Stage；
4. 验证第二天“狮子王烟花”卡片候场段为 45 分钟；
5. 点击 FAB 打开新建事件，选择“演出”类别，验证候场时间选择器，测试选择预置演出及手动切换候场时间档位并保存，验证时间轴渲染正确。

- [ ] **Step 5: 提交改动**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to 1.10.13 (versionCode 39)"
```
