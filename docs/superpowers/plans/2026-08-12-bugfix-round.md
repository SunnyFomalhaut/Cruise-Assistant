# 代码审查缺陷修复实施计划（15 项，按严重度排序）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 修复三轮代码审查确认的 15 项缺陷（严重 4 → 中等 8 → 轻微 3）。版本号不变（8/"1.7"）。

**Architecture:** 10 个任务，按严重度顺序；同文件缺陷合并为单任务避免冲突。修复均为局部小改。最终全量测试 + 模拟器冒烟验证。

**Tech Stack:** Kotlin, Jetpack Compose, Room, JUnit 4。

## Global Constraints

- 只修改各任务列出的文件；提交时只 `git add` 本任务文件（工作区有无关 APK/截图等未提交改动）。
- 版本号不变（build.gradle.kts 不许动）。
- 每任务完成前跑 `./gradlew :app:compileDebugKotlin`；涉及测试的任务跑对应测试。
- **#12（旧版本孤儿闹钟）接受为一次性历史问题，不修代码**：armed 记账已覆盖升级后的一切路径；旧版遗留的孤儿闹钟最多响一次（autoCancel 自清），无法用 AlarmManager API 枚举修复。在最终报告中如实说明。

---

### Task 1（严重）：EventSheetViewModel 两处缺陷（#2 + #4）

**Files:** `app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`

**#2 修复（编辑事件时 categoryIconKey 不写回）**：编辑加载块的第二个 `_state.update`（约 :157-171，只写 `locationPresetMode`）改为同时写回 `categoryIconKey`：

```kotlin
                    _state.update { current ->
                        val iconKey = current.categories
                            .firstOrNull { it.id == current.selectedCategoryId }?.iconKey
                        current.copy(
                            categoryIconKey = iconKey,
                            locationPresetMode = resolvePresetMode(
                                isEditing = true,
                                builtInShip = current.builtInShip,
                                iconKey = iconKey,
                                currentMode = current.locationPresetMode,
                                deck = current.deck,
                                zone = current.zone,
                                placeName = current.placeName,
                            ),
                        )
                    }
```

（原因：categories collect 首次发射时 selectedCategoryId 尚为 null，`categoryIconKey` 被写成第一个类别（meal）；编辑加载设置类别后必须覆盖写回，否则演出事件的弹窗显示餐厅分组下拉。）

**#4 修复（onNoLocationToggle 幽灵地点）**：把：

```kotlin
    fun onNoLocationToggle(value: Boolean) = _state.update {
        if (it.isSystemEvent) {
            it.copy(noLocation = true, deck = null)
        } else if (value) {
            it.copy(noLocation = true, deck = null)
        } else {
            it.copy(noLocation = false)
        }
    }
```

替换为：

```kotlin
    fun onNoLocationToggle(value: Boolean) = _state.update {
        if (it.isSystemEvent) {
            it.copy(noLocation = true, deck = null)
        } else if (value) {
            it.copy(noLocation = true, deck = null)
        } else {
            // 重新启用地点：deck 已被清空且没有备份，zone/placeName 一并清掉，
            // 否则保存时 deck=null + zone/placeName 残留会写出"幽灵地点"
            // （卡片不显示地点、再编辑时"不设地点"又被勾上）。
            if (it.deck == null) {
                it.copy(noLocation = false, zone = null, placeName = "")
            } else {
                it.copy(noLocation = false)
            }
        }
    }
```

验证：`./gradlew :app:compileDebugKotlin`。提交消息：`fix: event sheet shows correct preset options on edit; no ghost location after toggle`

---

### Task 2（严重）：编辑事件静默复活已停用事件（#1）

**Files:** `app/src/main/java/com/cruise/assistant/data/repo/EventRepository.kt`（updateEvent 的 merged 构造，约 :84-102）

**修复**：`merged = event.copy(...)` 保留目标行的 `isActive`（与 `isSystemGenerated` 同模式）：

在 `isSystemGenerated = target.isSystemGenerated,` 之后加：

```kotlin
                // 保留目标行的激活状态：UI 层构造的 event 不带 isActive（恒为 true），
                // 若不保留，编辑会静默"复活"被停用的事件（WHOLE_GROUP 时整组已停用成员一起复活）。
                isActive = target.isActive,
```

验证：`./gradlew :app:compileDebugKotlin` + 全量测试。提交消息：`fix: editing an event no longer reactivates deactivated events`

---

### Task 3（严重）：新建事件弹窗状态残留（#3）

**Files:** `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`

**修复**：给 EventSheetRequest 加唯一 requestId，每次打开弹窗用新 key，强制新 ViewModel：

1. 文件末尾的 data class 改为：

```kotlin
/** FAB/编辑入口 → 事件弹窗 的请求参数。editingId==0L 表示新建。requestId 每次打开递增，保证 ViewModel 不复用。 */
data class EventSheetRequest(
    val date: LocalDate,
    val startMinute: Int,
    val editingId: Long,
    val requestId: Long,
)
```

2. TIMELINE composable 内 `var sheetRequest by remember ...` 附近加计数器：

```kotlin
            var sheetSeq by remember { mutableStateOf(0L) }
```

3. `onAddEvent` 与 `onEditEvent` 两个回调改为先递增再构造：

```kotlin
                    onAddEvent = { startMinute ->
                        sheetSeq++
                        sheetRequest = EventSheetRequest(current.selectedDate, startMinute, 0L, sheetSeq)
                    },
                    onEditEvent = { event ->
                        sheetSeq++
                        sheetRequest = EventSheetRequest(event.date, 0, event.id, sheetSeq)
                    },
```

4. `key = "event-sheet-${request.date}-${request.editingId}-${request.startMinute}"` 改为：

```kotlin
                key = "event-sheet-${request.requestId}",
```

（注释同步说明 key 只依赖 requestId 保证不复用。）

验证：`./gradlew :app:compileDebugKotlin`。提交消息：`fix: fresh event sheet per open (unique requestId key, no stale draft)`

---

### Task 4（严重）：长按菜单锚定到卡片（#5）

**Files:** `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`、`app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt`

**修复**：把长按菜单从 TimelineScreen 根级（锚定在屏幕左上角）移进 LaneCanvas 的每张卡片 Box 内（锚定到被按的卡片位置）。

1. **LaneCanvas** 签名加两个参数：

```kotlin
    /** 长按选中的事件（用于在其卡片位置弹出菜单）；null 表示无菜单 */
    menuFor: CruiseEvent? = null,
    /** 菜单内容槽：渲染在被按卡片上的 DropdownMenu */
    menuContent: @Composable (CruiseEvent) -> Unit = {},
```

2. **LaneCanvas** 的卡片渲染处（`EventCard(...)` 调用）把"偏移+尺寸"包一层 Box，菜单作为该 Box 的最后一个子项（DropdownMenu 锚定父 Box 即卡片位置）：

```kotlin
            Box(
                Modifier
                    .offset(x = offsetX, y = top.dp)
                    .width(cardWidth)
                    .height(heightDp),
            ) {
                EventCard(
                    event = event,
                    category = categories[event.categoryId],
                    heightDp = heightDp,
                    waitSegmentHeightDp = waitHeightDp,
                    hasConflict = event.id in conflictIds,
                    onClick = { onEventClick(event) },
                    onLongClick = { onEventLongClick(event) },
                    modifier = Modifier.fillMaxSize(),
                    narrow = narrow,
                )
                if (menuFor?.id == event.id) {
                    menuContent(event)
                }
            }
```

（EventCard 的 modifier 从 `Modifier.offset(...).width(...).height(...)` 改为 `Modifier.fillMaxSize()`；heightDp 仍传入用于内部排版。）

3. **TimelineScreen**：把根级 `menuEvent?.let { event -> DropdownMenu(...) { ... } }` 整块删除；LaneCanvas 调用处加：

```kotlin
                            LaneCanvas(
                                layout = state.layout,
                                events = state.events,
                                categories = state.categories,
                                conflictIds = conflictIds,
                                singleLane = state.isFinalized,
                                onEventClick = { event -> if (!state.isFinalized) onEditEvent(event) },
                                onEventLongClick = { event -> if (!state.isFinalized) menuEvent = event },
                                menuFor = menuEvent,
                                menuContent = { event ->
                                    // 原 DropdownMenu 内容整体搬入（onDismissRequest = { menuEvent = null }）
                                    DropdownMenu(
                                        expanded = true,
                                        onDismissRequest = { menuEvent = null },
                                    ) {
                                        // ...原有的 换栏/编辑/删除 三个 DropdownMenuItem 原样搬入...
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                            )
```

注意：menuContent 内的菜单项需要访问 `state.isFinalized`、`onMoveToLane`、`onToggleActive`、`onEditEvent`、`onDeleteEvent`——lambda 在 TimelineScreen 作用域内定义，直接捕获即可；`menuEvent = null` 赋值也在 lambda 内完成。菜单项内容与原来逐字节一致（仅位置变化）。

验证：`./gradlew :app:compileDebugKotlin`。提交消息：`fix: long-press menu anchors to the pressed card`

---

### Task 5（中等）：1 夜航程固定天演出越界（#6）

**Files:** `app/src/main/java/com/cruise/assistant/domain/ShowPreset.kt`（resolveDates）、Test: `app/src/test/java/com/cruise/assistant/domain/ShowPresetTest.kt`

**先写失败测试**（ShowPresetTest 末尾追加）：

```kotlin
    @Test
    fun fixedDayShows_clampToLastVoyageDay() {
        // 1 夜航程（dayCount=2）：第 2 天 = 下船日，固定天演出不得生成在下船日
        val lionKing = DISNEY_ADVENTURE_SHOWS.first { it.key == "show_lion_king" }
        assertEquals(emptyList<LocalDate>(), lionKing.resolveDates(boardMonday, dayCount = 2))
        // 常规 4 天航程不受影响
        assertEquals(listOf(boardMonday.plusDays(1)), lionKing.resolveDates(boardMonday, dayCount = 4))
    }
```

**实现**：resolveDates 改为（固定天也截断到 dayCount-1）：

```kotlin
fun ShowPreset.resolveDates(boardDate: LocalDate, dayCount: Int): List<LocalDate> {
    val firstDay = dayOffsets.first()
    val lastDay = dailyRangeTo?.let { minOf(it, dayCount - 1) }
        ?: dayOffsets.last().coerceAtMost(dayCount - 1)
    if (firstDay > lastDay) return emptyList()
    return (firstDay..lastDay).map { boardDate.plusDays((it - 1).toLong()) }
}
```

（KDoc 同步："固定天取 dayOffsets；范围天截断到 dayCount-1" → 补充"固定天同样截断到 dayCount-1（下船日无演出）"。）

验证：`./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.ShowPresetTest"` 全 PASS。提交消息：`fix: clamp fixed-day show offsets to last voyage day`

---

### Task 6（中等）：23:30 后新建事件非法时间区间（#7）

**Files:** `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`（defaultStartMinute）、`app/src/main/java/com/cruise/assistant/ui/event/EventSheetViewModel.kt`（onStartChange）

**修复**：开始时间上限钳到 23:29（1409 分钟），保证默认 30 分钟事件总能以 ≤23:59 结束：

1. TimelineScreen 的 `defaultStartMinute`：

```kotlin
    }.coerceIn(0, 1439)
```

改为：

```kotlin
    // 上限 1409（23:29）：默认 30 分钟事件以 ≤23:59 结束，避免一打开就是"结束早于开始"
    }.coerceIn(0, 1409)
```

2. EventSheetViewModel 的 `onStartChange` 把：

```kotlin
    fun onStartChange(value: LocalTime) = _state.update { current ->
        // 开始时间往后越过结束时间时，结束时间跟着推 30 分钟
        val end = if (!current.endTime.isAfter(value)) {
            value.plusMinutes(30).takeIf { it.isAfter(value) } ?: LocalTime.of(23, 59)
        } else {
            current.endTime
        }
        current.copy(startTime = value, endTime = end, errorResId = null)
    }
```

替换为：

```kotlin
    fun onStartChange(value: LocalTime) = _state.update { current ->
        // 开始时间钳到 23:29：保证默认 30 分钟事件以 ≤23:59 结束，
        // 否则滚到 23:30+ 后结束时间回卷、保存必报"结束早于开始"
        val clamped = if (value.isAfter(LocalTime.of(23, 29))) LocalTime.of(23, 29) else value
        val end = if (!current.endTime.isAfter(clamped)) {
            clamped.plusMinutes(30)
        } else {
            current.endTime
        }
        current.copy(startTime = clamped, endTime = end, errorResId = null)
    }
```

验证：`./gradlew :app:compileDebugKotlin`。提交消息：`fix: cap event start at 23:29 so default 30-min events are always valid`

---

### Task 7（中等）：提醒排程并发竞态 + 启动路径无异常保护（#8 + #9）

**Files:** `app/src/main/java/com/cruise/assistant/reminder/ReminderScheduler.kt`、`app/src/main/java/com/cruise/assistant/CruiseApp.kt`

**#8 修复（Mutex 互斥）**：ReminderScheduler 加锁：

1. import 加 `kotlinx.coroutines.sync.Mutex` 与 `kotlinx.coroutines.sync.withLock`
2. 类体加 `private val rescheduleMutex = Mutex()`
3. `suspend fun rescheduleAll() {` 改为 `suspend fun rescheduleAll() = rescheduleMutex.withLock {`，函数体结尾的 `}` 前加一层闭合 `}`（即整个原函数体包进 withLock 块；注意 Kotlin 表达式函数体写法：`= rescheduleMutex.withLock { ...原体... }`）。

**#9 修复（启动保护）**：CruiseApp.kt 把：

```kotlin
        CoroutineScope(Dispatchers.Default).launch {
            container.reminderScheduler.rescheduleAll()
        }
```

替换为：

```kotlin
        CoroutineScope(Dispatchers.Default).launch {
            // 与 BootReceiver/TimeChangeReceiver 一致：排程异常不得让 App 启动即崩。
            // DataStore 损坏等异常在此吞掉，UI 仍可进入让用户在设置页恢复。
            runCatching { container.reminderScheduler.rescheduleAll() }
        }
```

验证：`./gradlew :app:compileDebugKotlin`。提交消息：`fix: serialize reminder rescheduling and guard app-start reschedule`

---

### Task 8（中等）：缩小航程后定稿状态复活（#10）

**Files:** `app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt`（updateVoyage）、`app/src/main/java/com/cruise/assistant/data/db/dao/VoyageDao.kt`

**修复**：updateVoyage 删除落在新范围外的 DayPlan：

在：

```kotlin
        val existingPlans = voyageDao.observeDayPlansOnce().map { it.date }.toSet()
        val newPlans = validDates.filter { it !in existingPlans }.map { DayPlan(date = it).toEntity() }
        if (newPlans.isNotEmpty()) voyageDao.upsertDayPlans(newPlans)
```

之后加：

```kotlin
        // 移除落出新范围的 DayPlan：否则缩小的日期带着 finalized 状态残留，
        // 再扩大航程时以旧定稿状态"复活"、无操作即锁定。
        voyageDao.observeDayPlansOnce()
            .filter { it.date !in validDates }
            .forEach { voyageDao.deleteDayPlan(it) }
```

VoyageDao 若无 `deleteDayPlan`，加：

```kotlin
    @Delete
    suspend fun deleteDayPlan(plan: DayPlanEntity)
```

（import DayPlanEntity 若缺失。也可复用现有 `upsertDayPlan` 之外的既有删除方法——先读 DAO 确认。）

验证：`./gradlew :app:compileDebugKotlin`。提交消息：`fix: delete day plans outside new voyage range on update`

---

### Task 9（中等）：提醒上限 300 静默截断（#11）

**Files:** `app/src/main/java/com/cruise/assistant/reminder/ReminderScheduler.kt`

**修复**：`private const val MAX_SCHEDULED = 300` 改为：

```kotlin
/** 一次最多排这么多个闹钟（每事件最多 3 个槽位），其余等 App 下次启动时补。
 * 500 贴近 Android 每应用 ~500 闹钟的软上限；典型航次（几十事件 × 3 槽）远低于此。 */
private const val MAX_SCHEDULED = 500
```

验证：`./gradlew :app:compileDebugKotlin`。提交消息：`fix: raise reminder cap to 500 to cover 3-slot scheduling`

---

### Task 10（轻微）：三处收尾（#13 + #14 + #15）

**Files:** `app/src/main/java/com/cruise/assistant/ui/timeline/EventCard.kt`、`app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`、`app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`

**#13（tier-4 阈值低估行高）**：EventCard 的派生公式把 fontSize 回退改为按 1.25 倍估算行高（补偿字体 padding）：

```kotlin
    val labelSmall = MaterialTheme.typography.labelSmall
    val labelLineHeight = labelSmall.lineHeight.takeIf { it != TextUnit.Unspecified }
        ?: (labelSmall.fontSize * 1.25f)
```

（注释同步："未设 lineHeight 时按 fontSize×1.25 估算实际行高，避免两行在 26dp 段被裁剪"。）

**#14（hourHeight 加载不 clamp）**：SettingsStore 的 settings 映射：

```kotlin
            hourHeightDp = prefs[Keys.hourHeight] ?: defaults.hourHeightDp,
```

改为：

```kotlin
            // 旧版本允许 36..140，存量值可能低于新下限 60——加载时钳到新范围，
            // 避免渲染与设置页滑块不一致（滑块钳 60，渲染仍用旧值）。
            hourHeightDp = prefs[Keys.hourHeight]?.coerceIn(60, 180) ?: defaults.hourHeightDp,
```

**#15（弹窗标志 saveable）**：CruiseNavHost 把：

```kotlin
    var showPresetShowsDialog by remember { mutableStateOf(false) }
```

改为：

```kotlin
    // rememberSaveable：旋转/配置变更不丢一次性弹窗（引导提交后丢失则永不重现）
    var showPresetShowsDialog by rememberSaveable { mutableStateOf(false) }
```

import 加 `androidx.compose.runtime.saveable.rememberSaveable`（若缺失）。

验证：`./gradlew :app:compileDebugKotlin` + 全量测试。提交消息：`fix: minor polish — tier threshold estimate, hourHeight load clamp, saveable dialog flag`

---

### Task 11：全量验证

- [ ] `./gradlew :app:testDebugUnitTest` 全 PASS
- [ ] `./gradlew :app:assembleDebug` BUILD SUCCESSFUL
- [ ] 模拟器冒烟（release/debug APK）：引导 → 生成时间轴 → 长按卡片菜单出现在卡片旁 → 新建事件表单干净（连续两次打开无残留）→ 编辑演出事件地点下拉为舞台分组 → 23:30 后点 + 不再报错

---

## 提交顺序汇总

1. `fix: event sheet shows correct preset options on edit; no ghost location after toggle`
2. `fix: editing an event no longer reactivates deactivated events`
3. `fix: fresh event sheet per open (unique requestId key, no stale draft)`
4. `fix: long-press menu anchors to the pressed card`
5. `fix: clamp fixed-day show offsets to last voyage day`
6. `fix: cap event start at 23:29 so default 30-min events are always valid`
7. `fix: serialize reminder rescheduling and guard app-start reschedule`
8. `fix: delete day plans outside new voyage range on update`
9. `fix: raise reminder cap to 500 to cover 3-slot scheduling`
10. `fix: minor polish — tier threshold estimate, hourHeight load clamp, saveable dialog flag`
