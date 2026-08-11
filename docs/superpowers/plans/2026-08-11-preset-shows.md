# 预置演出（Preset Shows）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Disney Adventure（探险号）内置 5 场预制演出目录，创建航次时自动把演出加入日程表第二栏（lane 1），选完探险号进入后弹窗提示；设置页改晚餐时段时海洋奇遇/回忆的场次在 18:00 ↔ 20:30 间互换。

**Architecture:** 延续现有 `ShipPreset.kt` 的静态注册表模式——新建 `domain/ShowPreset.kt` 存放演出目录 + 两个纯函数（日期解析、晚餐选场），`VoyageRepository.createVoyage` 在"安全演习 + 晚餐"块内追加演出插入，`EventRepository.resyncShowTimes` + `SettingsViewModel.setDinnerTime` 实现晚餐联动，`CruiseNavHost` 加一次性 AlertDialog。零 DB 迁移。

**Tech Stack:** Kotlin, Jetpack Compose, Room（已有），JUnit 4（已有测试基建 `app/src/test/java/com/cruise/assistant/domain/`）。

## Global Constraints

- 演出仅对内置船生成（`createVoyage` 的 `shipPresetName != null` 分支内）；自定义船名无演出、无弹窗。
- 演出类别 = `iconKey == "show"`（兜底用 `categories.first()`，与晚餐一致）。
- 所有演出 `lane = 1`（第二栏）、`isSystemGenerated = false`、`reminderMinutesBefore = null`、`presetKey` 非空。
- 晚餐选场规则（规格 §2/§3，唯一真源）：`dinnerTime.isBefore(LocalTime.of(19, 0))` → 20:30 场，否则 18:00 场。
- 海洋奇缘日期规则：`dayOffsets = [2]`、`dailyRangeTo = 4`，范围天上限截断到 `dayCount - 1`（下船日无演出）。四天三夜 → 第 2、3 天；五天四夜 → 第 2、3、4 天。
- 扬帆起航三场为 **13:15 / 14:00 / 14:45**（用户确认第三场 14:45，非 14:15），互不重叠。
- `resyncShowTimes` 保护：仅当事件当前 `startTime` ∈ {18:00, 20:30} 时才更新（用户手动改过的演出不覆盖）。
- 弹窗：每次 onboarding 提交完成（`isBuiltInShip`）后显示；"重建航次"后重新走引导会再次显示；自定义船名不显示。
- 不新增数据库表、不新增迁移（DB version 保持 4）。
- 提交时只 `git add` 本任务涉及的具体文件（工作区有无关的 APK 等未提交改动，不要碰）。

---

### Task 1: 演出目录模型与纯函数（`domain/ShowPreset.kt` + 测试）

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt`

**Interfaces:**
- Produces:
  - `data class ShowPreset(key: String, title: String, deck: Int, zone: ShipZone, placeName: String, durationMinutes: Int, dayOffsets: List<Int>, dailyRangeTo: Int? = null, startTimes: List<LocalTime>, dinnerLinked: Boolean = false)`
  - `val DISNEY_ADVENTURE_SHOWS: List<ShowPreset>`（5 场，key 见下表）
  - `fun ShowPreset.resolveDates(boardDate: LocalDate, dayCount: Int): List<LocalDate>`
  - `fun pickDinnerLinkedTime(dinnerTime: LocalTime): LocalTime`

Task 2 消费：`DISNEY_ADVENTURE_SHOWS`、`resolveDates`、`pickDinnerLinkedTime`。Task 3 消费：`pickDinnerLinkedTime`。

- [ ] **Step 1: 写失败测试** `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt`

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ShowPresetTest {

    private val boardMonday = LocalDate.of(2026, 8, 10) // Monday

    @Test
    fun fixedDay_showOccursOnThatDayOffset() {
        val setSail = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_set_sail" }
        assertEquals(listOf(boardMonday), setSail.resolveDates(boardMonday, dayCount = 4))
    }

    @Test
    fun moana_3NightCruise_occursOnDay2And3() {
        // 四天三夜：第 4 天（下船日）无场次
        val moana = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_moana" }
        assertEquals(
            listOf(boardMonday.plusDays(1), boardMonday.plusDays(2)),
            moana.resolveDates(boardMonday, dayCount = 4),
        )
    }

    @Test
    fun moana_4NightCruise_occursOnDay2To4() {
        // 五天四夜：第 2、3、4 天
        val moana = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_moana" }
        assertEquals(
            listOf(boardMonday.plusDays(1), boardMonday.plusDays(2), boardMonday.plusDays(3)),
            moana.resolveDates(boardMonday, dayCount = 5),
        )
    }

    @Test
    fun dinnerLinked_1745_returns2030Show() {
        assertEquals(LocalTime.of(20, 30), pickDinnerLinkedTime(LocalTime.of(17, 45)))
    }

    @Test
    fun dinnerLinked_2000_returns1800Show() {
        assertEquals(LocalTime.of(18, 0), pickDinnerLinkedTime(LocalTime.of(20, 0)))
    }

    @Test
    fun catalog_containsFiveShows_withUniqueKeysAndValidDecks() {
        assertEquals(5, DISNEY_ADVENTURE_SHOWS.size)
        val keys = DISNEY_ADVENTURE_SHOWS.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
        // 迪士尼探险号客用甲板 5-19F
        DISNEY_ADVENTURE_SHOWS.forEach { show ->
            assertTrue("${show.key} deck ${show.deck} out of 5..19", show.deck in 5..19)
        }
    }

    @Test
    fun onlySeasAdventureAndRemember_areDinnerLinked() {
        assertEquals(true, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_seas_adventure" }.dinnerLinked)
        assertEquals(true, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_remember" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_set_sail" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_lion_king" }.dinnerLinked)
        assertEquals(false, DISNEY_ADVENTURE_SHOWS.first { it.key == "show_moana" }.dinnerLinked)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.ShowPresetTest"`
Expected: FAIL——`DISNEY_ADVENTURE_SHOWS` 无法解析（编译错误），或全部测试因缺符号失败。

- [ ] **Step 3: 写最小实现** `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt`

```kotlin
package com.cruise.assistant.domain

import java.time.LocalDate
import java.time.LocalTime

/**
 * 预置演出：绑定在船名和航次上的演出数据（如迪士尼探险号）。
 * 由 VoyageRepository.createVoyage 生成事件，全部放进第二栏（lane=1）。
 */
data class ShowPreset(
    /** 写入事件的 presetKey，如 "show_set_sail" */
    val key: String,
    /** 完整标题（中文 + 英文） */
    val title: String,
    val deck: Int,
    val zone: ShipZone,
    /** 演出地点名，如 "花园舞台" */
    val placeName: String,
    val durationMinutes: Int,
    /** 相对上船日的天数（第1天 = 1） */
    val dayOffsets: List<Int>,
    /** 非空 = 从 dayOffsets 首日起每天都有，直到 min(该值, 航次天数-1)（不含下船日） */
    val dailyRangeTo: Int? = null,
    /** 一天内的所有场次 */
    val startTimes: List<LocalTime>,
    /** true = 与晚餐时间绑定，只取与晚餐不冲突的那一场 */
    val dinnerLinked: Boolean = false,
)

/** 演出在航次中的全部日期。固定天只取 dayOffsets；范围天截断到 dayCount-1（下船日无演出）。 */
fun ShowPreset.resolveDates(boardDate: LocalDate, dayCount: Int): List<LocalDate> {
    val firstDay = dayOffsets.first()
    val lastDay = dailyRangeTo?.let { minOf(it, dayCount - 1) } ?: dayOffsets.last()
    return (firstDay..lastDay).map { boardDate.plusDays((it - 1).toLong()) }
}

/**
 * 晚餐联动演出选场：17:45 晚餐（早于 19:00）→ 20:30 场；20:00 晚餐 → 18:00 场。
 * 选中的恰好是与晚餐不冲突的那场（17:45–19:00 vs 20:30 场；20:00–21:15 vs 18:00 场）。
 */
fun pickDinnerLinkedTime(dinnerTime: LocalTime): LocalTime =
    if (dinnerTime.isBefore(LocalTime.of(19, 0))) {
        LocalTime.of(20, 30)
    } else {
        LocalTime.of(18, 0)
    }

/** 迪士尼探险号预置演出目录（规格 §1 数据表） */
val DISNEY_ADVENTURE_SHOWS: List<ShowPreset> = listOf(
    ShowPreset(
        key = "show_set_sail",
        title = "扬帆起航 Let's Set Sail",
        deck = 10,
        zone = ShipZone.MID,
        placeName = "花园舞台",
        durationMinutes = 30,
        dayOffsets = listOf(1),
        startTimes = listOf(
            LocalTime.of(13, 15),
            LocalTime.of(14, 0),
            LocalTime.of(14, 45),
        ),
    ),
    ShowPreset(
        key = "show_lion_king",
        title = "狮子王烟花 The Lion King: Celebration in the Sky",
        deck = 17,
        zone = ShipZone.MID,
        placeName = "露天泳池",
        durationMinutes = 10,
        dayOffsets = listOf(2),
        startTimes = listOf(LocalTime.of(22, 30)),
    ),
    ShowPreset(
        key = "show_seas_adventure",
        title = "海洋奇遇 Disney Seas The Adventure",
        deck = 6,
        zone = ShipZone.BOW,
        placeName = "迪士尼剧院",
        durationMinutes = 60,
        dayOffsets = listOf(1),
        startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
        dinnerLinked = true,
    ),
    ShowPreset(
        key = "show_remember",
        title = "回忆：瓦力与伊芙 Remember",
        deck = 6,
        zone = ShipZone.BOW,
        placeName = "迪士尼剧院",
        durationMinutes = 60,
        dayOffsets = listOf(2),
        startTimes = listOf(LocalTime.of(18, 0), LocalTime.of(20, 30)),
        dinnerLinked = true,
    ),
    ShowPreset(
        key = "show_moana",
        title = "海洋奇缘 Moana: Call of the Sea",
        deck = 10,
        zone = ShipZone.STERN,
        placeName = "泳池舞台",
        durationMinutes = 15,
        dayOffsets = listOf(2),
        dailyRangeTo = 4,
        startTimes = listOf(
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            LocalTime.of(10, 30),
        ),
    ),
)
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.ShowPresetTest"`
Expected: 7 个测试全 PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt
git commit -m "feat: add preset show catalog for Disney Adventure (5 shows, lane 2)"
```

---

### Task 2: createVoyage 生成演出事件

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt:101-137`（`shipPresetName != null` 块内，晚餐生成之后）

**Interfaces:**
- Consumes: `DISNEY_ADVENTURE_SHOWS`、`ShowPreset.resolveDates(boardDate, dayCount)`、`pickDinnerLinkedTime(dinnerTime)`（Task 1）
- Produces: 演出事件写入 event 表（lane=1、presetKey、categoryId=演出类别、deck/zone/placeName 齐全）

- [ ] **Step 1: 加 import**

`VoyageRepository.kt` 顶部 import 区（现有 `import com.cruise.assistant.domain.CruiseEvent` 附近）追加：

```kotlin
import com.cruise.assistant.domain.DISNEY_ADVENTURE_SHOWS
import com.cruise.assistant.domain.pickDinnerLinkedTime
import com.cruise.assistant.domain.resolveDates
```

- [ ] **Step 2: 在晚餐生成后追加演出生成**

`VoyageRepository.kt` 中晚餐 `forEach` 结束之后、`eventDao.insertAll(presetEvents.map { it.toEntity() })` 之前（即现有 `presetEvents += CruiseEvent(...)` 晚餐块的 `}` 之后），插入：

```kotlin
            // 预置演出：迪士尼探险号演出目录，全部放进第二栏（lane=1），与晚餐/安全演习一样可编辑删除
            val showId = categories.firstOrNull { it.iconKey == "show" }?.id
                ?: categories.first().id
            val showEvents = mutableListOf<CruiseEvent>()
            DISNEY_ADVENTURE_SHOWS.forEach { show ->
                show.resolveDates(boardDate, voyage.dayCount).forEach { date ->
                    val times = if (show.dinnerLinked) {
                        listOf(pickDinnerLinkedTime(dinnerTime))
                    } else {
                        show.startTimes
                    }
                    times.forEach { startTime ->
                        showEvents += CruiseEvent(
                            date = date,
                            startTime = startTime,
                            durationMinutes = show.durationMinutes,
                            categoryId = showId,
                            title = show.title,
                            deck = show.deck,
                            zone = show.zone,
                            placeName = show.placeName,
                            lane = 1,
                            isSystemGenerated = false,
                            presetKey = show.key,
                        )
                    }
                }
            }
            eventDao.insertAll(showEvents.map { it.toEntity() })
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt
git commit -m "feat: generate preset shows on voyage creation (lane 2, dinner-linked)"
```

---

### Task 3: 晚餐时段联动（resyncShowTimes + SettingsViewModel）

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt:142-147`（`updateDinnerEventsTime` 之后加新方法）
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt:77-83`（`setDinnerTime`）

**Interfaces:**
- Consumes: `pickDinnerLinkedTime(dinnerTime)`（Task 1）、`EventDao.getByPresetKey(key)`（已有）
- Produces: `EventRepository.resyncShowTimes(dinnerTime: LocalTime)`——`SettingsViewModel.setDinnerTime` 调用

- [ ] **Step 1: 加 import**

`EventRepository.kt` 顶部 import 区追加：

```kotlin
import com.cruise.assistant.domain.pickDinnerLinkedTime
```

- [ ] **Step 2: 在 `updateDinnerEventsTime` 之后加 `resyncShowTimes`**

```kotlin
    /**
     * 设置页切换晚餐时段：把晚餐联动演出（海洋奇遇/回忆）按新晚餐时间换场。
     * 仅当事件当前时间仍是 18:00/20:30 之一才更新——用户手动改过时间的演出不被覆盖。
     * 被删除过的演出不会"复活"（只有当前仍在表里的会被更新，与 updateDinnerEventsTime 一致）。
     */
    suspend fun resyncShowTimes(dinnerTime: LocalTime) {
        val target = pickDinnerLinkedTime(dinnerTime)
        listOf("show_seas_adventure", "show_remember").forEach { key ->
            eventDao.getByPresetKey(key).forEach { entity ->
                val isKnownShowTime = entity.startTime == LocalTime.of(18, 0) ||
                    entity.startTime == LocalTime.of(20, 30)
                if (isKnownShowTime) {
                    eventDao.update(entity.copy(startTime = target))
                }
            }
        }
    }
```

- [ ] **Step 3: SettingsViewModel.setDinnerTime 追加调用**

`SettingsViewModel.kt` 的 `setDinnerTime` 改为：

```kotlin
    fun setDinnerTime(time: LocalTime) {
        viewModelScope.launch {
            settingsStore.setDinnerTime(time)
            eventRepository.updateDinnerEventsTime(time)
            eventRepository.resyncShowTimes(time)
            reminderScheduler.rescheduleAll()
        }
    }
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt app/src/main/java/com/cruise/assistant/ui/settings/SettingsViewModel.kt
git commit -m "feat: resync dinner-linked show times when dinner time changes"
```

---

### Task 4: 选完探险号后的提示弹窗

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`（状态 + onboarding 提交回调 + TIMELINE 弹窗）
- Modify: `app/src/main/res/values/strings.xml`（追加 3 个字符串）

**Interfaces:**
- Consumes: `OnboardingState.isBuiltInShip`（已有）、`R.string.preset_shows_dialog_*`（本任务新增）
- Produces: 探险号引导完成后 TIMELINE 页一次 AlertDialog

- [ ] **Step 1: strings.xml 追加字符串**

在 `app/src/main/res/values/strings.xml` 中 `<string name="error_disembark_before_board">` 那一行之后插入（注意 XML 转义：撇号 `'` 写成 `\'`，换行用 `\n`）：

```xml
    <string name="preset_shows_dialog_title">已添加预置演出</string>
    <string name="preset_shows_dialog_message">已按迪士尼探险号的演出安排，将以下演出加入日程表第二栏：\n\n· 扬帆起航 Let\'s Set Sail（第1天 3场）\n· 狮子王烟花 The Lion King: Celebration in the Sky（第2天 1场）\n· 海洋奇遇 Disney Seas The Adventure（第1天 1场）\n· 回忆：瓦力与伊芙 Remember（第2天 1场）\n· 海洋奇缘 Moana: Call of the Sea（第2天起每天 3场）\n\n长按演出卡片可编辑或删除。</string>
    <string name="preset_shows_dialog_ok">知道了</string>
```

- [ ] **Step 2: CruiseNavHost 加弹窗状态**

`CruiseNavHost.kt` 中 `val settings by container.settingsStore.settings.collectAsState(initial = null)` 之后加：

```kotlin
    // 选完内置船（探险号）进入后的一次性演出提示
    var showPresetShowsDialog by remember { mutableStateOf(false) }
```

（`remember`/`mutableStateOf`/`setValue` 已 import，见文件顶部。）

- [ ] **Step 3: onboarding 提交回调置标记**

`CruiseNavHost.kt` 的 ONBOARDING composable 中 `onSubmit = { ... }` 改为：

```kotlin
                onSubmit = {
                    if (state.isBuiltInShip) showPresetShowsDialog = true
                    vm.submit {
                        navController.navigate(Routes.TIMELINE) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
```

- [ ] **Step 4: TIMELINE composable 渲染弹窗**

`CruiseNavHost.kt` 的 TIMELINE composable 内、最外层（`TimelineScreen(...)` / `TimelineLoading()` 之后、`conflictCheck?.let` 之前）加：

```kotlin
            if (showPresetShowsDialog) {
                AlertDialog(
                    onDismissRequest = { showPresetShowsDialog = false },
                    title = { Text(stringResource(R.string.preset_shows_dialog_title)) },
                    text = { Text(stringResource(R.string.preset_shows_dialog_message)) },
                    confirmButton = {
                        TextButton(onClick = { showPresetShowsDialog = false }) {
                            Text(stringResource(R.string.preset_shows_dialog_ok))
                        }
                    },
                )
            }
```

（`AlertDialog`/`TextButton`/`stringResource` 均已 import。`showPresetShowsDialog` 是 nav host 顶层 remember 状态，TIMELINE composable 离开组合（如去设置页）再回来仍会显示，点"知道了"后本次进入不再显示；"重建航次"回到 ONBOARDING 时整个导航图重置，下次提交会重新弹——符合规格。）

- [ ] **Step 5: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt app/src/main/res/values/strings.xml
git commit -m "feat: show preset-shows dialog after Disney Adventure onboarding"
```

---

### Task 5: 全量验证

**Files:** 无改动。

- [ ] **Step 1: 跑全部单元测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全 PASS（含现有 ConflictDetector/LaneAssigner/ShipPreset/TimelineLayout 等，无回归）。

- [ ] **Step 2: 构建 Debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 手动验证清单（真机）**

1. 引导页选 Disney Adventure + 四天三夜 + 17:45 晚餐 → 提交 → 弹窗出现，内容为 5 场演出摘要。
2. 第 1 天：扬帆起航 13:15/14:00/14:45 三张卡片在第二栏；海洋奇遇 20:30（17:45 晚餐匹配）；安全演习 15:45 在栏 0；晚餐 17:45。
3. 第 2 天：海洋奇缘 9:30/10:00/10:30、回忆 20:30、狮子王烟花 22:30（默认显示范围 07:00–24:00 内可见）。
4. 第 3 天：海洋奇缘三场在；第 4 天（下船日）：无任何演出。
5. 设置页改晚餐时段为 20:00 → 回时间轴：海洋奇遇/回忆变为 18:00 场，晚餐变为 20:00。
6. 长按某场演出卡片 → 可编辑/删除（与普通事件一致）。
7. 重建航次 → 重新走引导（自定义船名）→ 无演出、无弹窗。

---

## 提交顺序汇总

1. `feat: add preset show catalog for Disney Adventure (5 shows, lane 2)`
2. `feat: generate preset shows on voyage creation (lane 2, dinner-linked)`
3. `feat: resync dinner-linked show times when dinner time changes`
4. `feat: show preset-shows dialog after Disney Adventure onboarding`

（当前工作区有无关未提交改动——`app/release/*.apk`、截图等，各步骤只 add 自己的文件，勿 `git add -A`。）
