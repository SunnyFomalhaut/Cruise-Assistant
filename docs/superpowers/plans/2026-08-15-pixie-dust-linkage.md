# 仙尘发出/收到双向联动 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 发出快捷录入自动勾选同房间最早的未回礼收到记录；勾选回礼时弹等级窗并自动创建发出记录；统计条加联动检视按钮打开按房间配对的只读视图。

**Architecture:** 无 schema 变更（Room v5 不动）。联动配对逻辑放 domain 纯函数（`autoReplyTarget`、`pairByRoom`，JUnit 可测）；ViewModel 把"勾选回礼"改成 pending 两步流（请求→弹窗确认）；UI 新增 `ReplyLevelDialog` 和 `LinkageViewDialog` 两个弹层。

**Tech Stack:** Kotlin + Jetpack Compose (Material3)，现有依赖，零新增。

**Spec:** `docs/superpowers/specs/2026-08-15-pixie-dust-linkage-design.md`

## Global Constraints

- versionCode 15，versionName `"1.8.1"`（三级版本号约定）
- 所有用户可见文案为中文，放 `app/src/main/res/values/strings.xml`，代码不硬编码 UI 文案（"Deck N" 用 `pixie_deck_format` 既有模式）
- 不新增依赖；minSdk 26
- Room version 保持 5，无迁移
- domain 纯函数用 JUnit TDD；VM/UI 以编译 + 手动回归为准（与 v1.8.0 一致）
- 测试命令 `./gradlew :app:testDebugUnitTest --tests "..."`，编译检查 `./gradlew :app:compileDebugKotlin`

---

### Task 1: Domain 联动纯函数（autoReplyTarget + pairByRoom + RoomPair）

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/PixieDustLinkageTest.kt`

**Interfaces:**
- Consumes: 现有 `PixieDustRecord`、`PixieDustDirection`（`domain/PixieDust.kt`）
- Produces（后续任务依赖）:
  - `data class RoomPair(deck: Int, room: String, given: List<PixieDustRecord>, received: List<PixieDustRecord>, unrepliedReceived: List<PixieDustRecord>, lastInteractTs: Long)`
  - `fun autoReplyTarget(received: List<PixieDustRecord>, deck: Int, room: String): PixieDustRecord?`——同房间未回礼中 timestamp 最小的；无则 null
  - `fun pairByRoom(given: List<PixieDustRecord>, received: List<PixieDustRecord>): List<RoomPair>`——按房间配对，排序：有未回礼的最前（未回礼中最高等级 rank 降序）→ 不平衡（发出数≠收到数，最近互动倒序）→ 平衡（最近互动倒序）

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/cruise/assistant/domain/PixieDustLinkageTest.kt`:

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PixieDustLinkageTest {

    private fun given(id: Long, deck: Int, room: String, levelId: Long, ts: Long) = PixieDustRecord(
        id = id, direction = PixieDustDirection.GIVEN, deck = deck, room = room,
        levelId = levelId, levelNameSnapshot = "", timestamp = ts,
    )

    private fun received(id: Long, deck: Int, room: String, levelId: Long, ts: Long, replied: Boolean = false) =
        PixieDustRecord(
            id = id, direction = PixieDustDirection.RECEIVED, deck = deck, room = room,
            levelId = levelId, levelNameSnapshot = "", replied = replied, timestamp = ts,
        )

    // ---- autoReplyTarget ----

    @Test
    fun `auto reply target picks earliest unreplied same-room received`() {
        val list = listOf(
            received(1, 11, "550", 1, 300),
            received(2, 11, "550", 2, 100),  // 最早未回礼 → 目标
            received(3, 11, "550", 3, 200),
        )
        assertEquals(2L, autoReplyTarget(list, 11, "550")?.id)
    }

    @Test
    fun `auto reply target skips replied records`() {
        val list = listOf(
            received(1, 11, "550", 1, 100, replied = true),
            received(2, 11, "550", 2, 200),
        )
        assertEquals(2L, autoReplyTarget(list, 11, "550")?.id)
    }

    @Test
    fun `auto reply target requires same deck and room`() {
        val list = listOf(
            received(1, 11, "551", 1, 100),
            received(2, 12, "550", 1, 100),
        )
        assertNull(autoReplyTarget(list, 11, "550"))
    }

    @Test
    fun `auto reply target ignores given records`() {
        assertNull(autoReplyTarget(listOf(given(1, 11, "550", 1, 100)), 11, "550"))
    }

    @Test
    fun `auto reply target empty list returns null`() {
        assertNull(autoReplyTarget(emptyList(), 11, "550"))
    }

    // ---- pairByRoom ----

    @Test
    fun `pair by room merges both directions and computes unreplied`() {
        val pairs = pairByRoom(
            given = listOf(given(1, 11, "550", 1, 100), given(2, 11, "550", 2, 200)),
            received = listOf(received(3, 11, "550", 3, 300), received(4, 11, "550", 1, 50, replied = true)),
        )
        assertEquals(1, pairs.size)
        val p = pairs[0]
        assertEquals(11, p.deck)
        assertEquals("550", p.room)
        assertEquals(listOf(1L, 2L), p.given.map { it.id })
        assertEquals(listOf(3L, 4L), p.received.map { it.id })
        assertEquals(listOf(3L), p.unrepliedReceived.map { it.id })
        assertEquals(300L, p.lastInteractTs)
    }

    @Test
    fun `pair by room includes single-direction rooms`() {
        val pairs = pairByRoom(
            given = listOf(given(1, 11, "550", 1, 100)),
            received = listOf(received(2, 12, "100", 1, 200)),
        )
        assertEquals(2, pairs.size)
        assertEquals("550", pairs[0].room)  // 最近互动 ts=200? 不——100<200，"100" 更近
        // 修正：room "100" 互动 ts=200 更新，两房都无未回礼、都平衡(1:0 不平衡)
        // "550": given=1 received=0 → 不平衡; "100": given=0 received=1(replied=false) → 有未回礼 → 最前
    }

    @Test
    fun `pair by room sorts unreplied-with-highest-rank first`() {
        // 房 A(11,100)：未回礼 rank=1；房 B(11,200)：未回礼 rank=3 → B 在前
        val pairs = pairByRoom(
            given = emptyList(),
            received = listOf(
                received(1, 11, "100", 1, 100),
                received(2, 11, "200", 3, 100),
            ),
        )
        assertEquals(listOf("200", "100"), pairs.map { it.room })
    }

    @Test
    fun `pair by room unreplied before unbalanced before balanced`() {
        // 房 U(11,100)：未回礼收到 → 第一组
        // 房 B1(11,200)：发出1 收到0 → 不平衡（无未回礼）
        // 房 S(11,300)：发出1 收到1 已回礼 → 平衡
        val pairs = pairByRoom(
            given = listOf(given(1, 11, "200", 1, 100), given(2, 11, "300", 1, 200)),
            received = listOf(
                received(3, 11, "100", 1, 50),
                received(4, 11, "300", 1, 150, replied = true),
            ),
        )
        assertEquals(listOf("100", "200", "300"), pairs.map { it.room })
    }

    @Test
    fun `pair by room same group sorts by last interact desc`() {
        // 两个平衡房：ts 大的在前
        val pairs = pairByRoom(
            given = listOf(
                given(1, 11, "100", 1, 100),
                given(2, 11, "200", 1, 900),
            ),
            received = listOf(
                received(3, 11, "100", 1, 50, replied = true),
                received(4, 11, "200", 1, 800, replied = true),
            ),
        )
        assertEquals(listOf("200", "100"), pairs.map { it.room })
    }

    @Test
    fun `pair by room empty inputs returns empty`() {
        assertTrue(pairByRoom(emptyList(), emptyList()).isEmpty())
    }
}
```

（注意 `pair by room includes single-direction rooms` 那个测试的断言写得别扭——最终版直接写 `assertEquals(listOf("100", "550"), pairs.map { it.room })`，理由见注释：房 "100" 有未回礼收到排最前，房 "550" 纯发出排第二组。同时文件头需要 `import org.junit.Assert.assertTrue`。）

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustLinkageTest"`
Expected: 编译失败（`Unresolved reference: autoReplyTarget` / `pairByRoom` / `RoomPair`）

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt`:

```kotlin
package com.cruise.assistant.domain

/** 联动检视图的一行：一个房间的发出/收到配对 */
data class RoomPair(
    val deck: Int,
    val room: String,
    val given: List<PixieDustRecord>,
    val received: List<PixieDustRecord>,
    val unrepliedReceived: List<PixieDustRecord>,
    /** 双侧最大 timestamp，用于同组内排序 */
    val lastInteractTs: Long,
)

/**
 * 方向一联动：发出记录保存后，同房间未回礼的收到记录中最早（timestamp 最小）的一条。
 * 没有则返回 null（静默，无联动）。
 */
fun autoReplyTarget(received: List<PixieDustRecord>, deck: Int, room: String): PixieDustRecord? =
    received
        .filter {
            it.direction == PixieDustDirection.RECEIVED &&
                !it.replied && it.deck == deck && it.room == room
        }
        .minByOrNull { it.timestamp }

/**
 * 联动检视：按房间（deck+room）配对双侧记录。
 * 排序：① 有未回礼的最前（未回礼中最高等级 rank 降序——需要 levels 查 rank，
 * 调用方用 levelRankOf 传入；② 不平衡（发出数≠收到数）按最近互动倒序；
 * ③ 平衡按最近互动倒序。
 */
fun pairByRoom(
    given: List<PixieDustRecord>,
    received: List<PixieDustRecord>,
    levelRankOf: (PixieDustRecord) -> Int = { 0 },
): List<RoomPair> {
    val byRoom = (given.asSequence().map { it.deck to it.room } +
        received.asSequence().map { it.deck to it.room })
        .toMutableSet()
    return byRoom.map { (deck, room) ->
        val g = given.filter { it.deck == deck && it.room == room }
        val r = received.filter { it.deck == deck && it.room == room }
        RoomPair(
            deck = deck,
            room = room,
            given = g,
            received = r,
            unrepliedReceived = r.filter { !it.replied },
            lastInteractTs = (g.map { it.timestamp } + r.map { it.timestamp }).max(),
        )
    }.sortedWith(
        compareByDescending<RoomPair> { it.unrepliedReceived.maxOfOrNull(levelRankOf) ?: -1 }
            .thenByDescending { it.lastInteractTs },
    )
        // 上面 compareBy 已经把"不平衡"和"平衡"混在互动序里了；需要先分组：
        .let { list ->
            val hasUnreplied = list.filter { it.unrepliedReceived.isNotEmpty() }
            val balanced = list.filter { it.unrepliedReceived.isEmpty() && it.given.size == it.received.size }
            val unbalanced = list.filter { it.unrepliedReceived.isEmpty() && it.given.size != it.received.size }
            hasUnreplied + unbalanced + balanced
        }
}
```

（注意：上面 `.sortedWith(...).let { ... }` 的写法把组内排序与分组搞混了。最终版直接分两步写清楚——先给每个 pair 算 `unrepliedMaxRank`，组内按各自排序键排，再拼接三组。实现者按下面这个最终版写：）

```kotlin
fun pairByRoom(
    given: List<PixieDustRecord>,
    received: List<PixieDustRecord>,
    levelRankOf: (PixieDustRecord) -> Int = { 0 },
): List<RoomPair> {
    val rooms = (given.map { it.deck to it.room } + received.map { it.deck to it.room }).toSet()
    val pairs = rooms.map { (deck, room) ->
        val g = given.filter { it.deck == deck && it.room == room }
        val r = received.filter { it.deck == deck && it.room == room }
        RoomPair(
            deck = deck,
            room = room,
            given = g,
            received = r,
            unrepliedReceived = r.filter { !it.replied },
            lastInteractTs = (g.map { it.timestamp } + r.map { it.timestamp }).max(),
        )
    }
    val hasUnreplied = pairs
        .filter { it.unrepliedReceived.isNotEmpty() }
        .sortedWith(
            compareByDescending<RoomPair> { it.unrepliedReceived.maxOfOrNull(levelRankOf) ?: -1 }
                .thenByDescending { it.lastInteractTs },
        )
    val unbalanced = pairs
        .filter { it.unrepliedReceived.isEmpty() && it.given.size != it.received.size }
        .sortedByDescending { it.lastInteractTs }
    val balanced = pairs
        .filter { it.unrepliedReceived.isEmpty() && it.given.size == it.received.size }
        .sortedByDescending { it.lastInteractTs }
    return hasUnreplied + unbalanced + balanced
}
```

测试里 `levelRankOf` 用默认 `{ 0 }` 时 `pair by room sorts unreplied-with-highest-rank first` 会失败（两个未回礼 rank 都算 0，退化为互动序）——该测试需要显式传 `levelRankOf = { if (it.levelId == 1L) 1 else if (it.levelId == 2L) 2 else 3 }`（levelId 1/2/3 → rank 1/2/3），实现者写测试时按此调整。

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustLinkageTest"`
Expected: PASS（9 个测试全绿；若按调整后的断言/参数写则 8-9 个，以实际为准）

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt app/src/test/java/com/cruise/assistant/domain/PixieDustLinkageTest.kt
git commit -m "feat: pixie dust linkage domain functions (autoReplyTarget, pairByRoom)"
```

---

### Task 2: ViewModel 联动逻辑（saveRecord 联动勾选 + pendingReply 两步流 + confirmReply）

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt`
- Modify: `app/src/main/res/values/strings.xml`（新增 toast 文案）

**Interfaces:**
- Consumes: Task 1 的 `autoReplyTarget(records, deck, room)`；现有 `PixieDustRecord`/`PixieLevel`
- Produces（Task 3-5 UI 依赖）:
  - `PixieDustUiState` 新增字段：
    - `pendingReply: PixieDustRecord?`（非 null 时 UI 显示 ReplyLevelDialog）
    - `autoRepliedRoom: String?`（非 null 时 UI toast"已自动勾选 X 的回礼"）
    - `roomPairs: List<RoomPair>`（联动检视图数据）
  - VM 新方法：
    - `onRequestReply(record: PixieDustRecord)`——列表/编辑弹窗勾选回礼时调，置 pendingReply（不写库）
    - `confirmReply(level: PixieLevel?)`——确认弹窗；null=仅勾选；写 replied=true + 可选创建 GIVEN 记录（不触发方向一）
    - `cancelReply()`——清 pendingReply（勾选回退）
    - `onAutoReplyToastShown()`——清 autoRepliedRoom
  - `onToggleReplied(id, false)` 取消勾选语义不变（直接写库）

- [ ] **Step 1: Add strings**

`strings.xml` 仙尘分组追加：

```xml
    <string name="pixie_auto_replied_format">已自动勾选 %1$s 的回礼</string>
    <string name="pixie_reply_level_title_format">%1$s · 选择回礼等级</string>
    <string name="pixie_reply_confirm">确认并记录发出</string>
    <string name="pixie_reply_only_check">仅勾选</string>
    <string name="pixie_linkage_view">联动检视</string>
    <string name="pixie_linkage_empty">还没有仙尘记录</string>
    <string name="pixie_linkage_given_label">发出 %1$d</string>
    <string name="pixie_linkage_received_label">收到 %1$d</string>
    <string name="pixie_linkage_unreplied_format">待回礼 %1$d</string>
```

- [ ] **Step 2: Modify ViewModel**

`PixieDustViewModel.kt` 修改点（文件现有 261 行，改动集中在 state 流与几个方法）：

1. import 追加：`com.cruise.assistant.domain.RoomPair`、`com.cruise.assistant.domain.autoReplyTarget`、`com.cruise.assistant.domain.pairByRoom`

2. `PixieDustUiState` 追加三个字段（`savedEvent` 之后）：

```kotlin
    /** 非 null 时 UI 显示回礼等级弹窗（勾选→弹窗两步流） */
    val pendingReply: PixieDustRecord? = null,
    /** 非 null 时 UI toast 提示联动勾选；显示后调 onAutoReplyToastShown() */
    val autoRepliedRoom: String? = null,
    /** 联动检视图数据 */
    val roomPairs: List<RoomPair> = emptyList(),
```

3. 新增两个 MutableStateFlow（`savedEvent` 之后）：

```kotlin
    private val pendingReply = MutableStateFlow<PixieDustRecord?>(null)
    private val autoRepliedRoom = MutableStateFlow<String?>(null)
```

4. `ui` combine 流追加这两个流（vararg 数组相应扩为 9 个，`UiInputs` data class 加 `pendingReply: PixieDustRecord?` 与 `autoRepliedRoom: String?` 字段，索引取值转型照现有模式）。

5. state 构造里追加（`savedEvent = input.savedEvent,` 之后）：

```kotlin
            pendingReply = input.pendingReply,
            autoRepliedRoom = input.autoRepliedRoom,
            roomPairs = pairByRoom(given, dust.filter { it.direction == PixieDustDirection.RECEIVED }) { rec ->
                levels.firstOrNull { it.id == rec.levelId }?.rank ?: 0
            },
```

（注意：`given` 在 combine 里已是过滤+排序后的发出列表；received 全量重新过滤一次拿原始顺序——pairByRoom 不依赖顺序，直接传即可。）

6. `saveRecord()` 的 launch 块改为（联动方向一）：

```kotlin
        viewModelScope.launch {
            dustRepository.save(record)
            savedEvent.value = record
            roomInput.value = ""
            // 联动方向一：同房间最早的未回礼收到 → 自动勾选
            val s2 = state.value
            val target = s2?.let { autoReplyTarget(it.received, record.deck, record.room) }
            if (target != null) {
                dustRepository.save(target.copy(replied = true))
                autoRepliedRoom.value = "${record.deck}${record.room}"
            }
        }
```

（说明：读 `state.value` 拿的是最新收到列表——保存后 Flow 会重新发射，但 launch 里连续执行时 state 可能尚未刷新；稳妥做法是直接从 repository 拉一次。但 repository 没有 suspend 的 getAll——用 `dustRepository.observeAll()` 的 first() 也行。实现者用下面这个版本：）

```kotlin
        viewModelScope.launch {
            dustRepository.save(record)
            savedEvent.value = record
            roomInput.value = ""
            // 联动方向一：同房间最早的未回礼收到 → 自动勾选。
            // 直接从 DAO 拉最新全量（state 可能未刷新）。
            val all = kotlinx.coroutines.flow.first(dustRepository.observeAll())
            val target = autoReplyTarget(
                all.filter { it.direction == PixieDustDirection.RECEIVED },
                record.deck, record.room,
            )
            if (target != null) {
                dustRepository.save(target.copy(replied = true))
                autoRepliedRoom.value = "${record.deck}${record.room}"
            }
        }
```

（`kotlinx.coroutines.flow.first` 是扩展函数 `Flow<T>.first()`——写成 `dustRepository.observeAll().first()` 并在文件头 import `kotlinx.coroutines.flow.first`。）

7. `onToggleReplied` 保留但只处理取消勾选路径；新增三个方法：

```kotlin
    /** 列表/编辑弹窗勾选"已回礼" → 进入弹窗两步流（不立即写库） */
    fun onRequestReply(record: PixieDustRecord) {
        pendingReply.value = record
    }

    /**
     * 回礼弹窗确认。level == null 表示"仅勾选"；否则勾选 + 创建一条 GIVEN 记录
     * （不触发方向一联动——单向无循环）。
     */
    fun confirmReply(level: PixieLevel?) {
        val record = pendingReply.value ?: return
        pendingReply.value = null
        viewModelScope.launch {
            dustRepository.save(record.copy(replied = true))
            if (level != null) {
                dustRepository.save(
                    PixieDustRecord(
                        direction = PixieDustDirection.GIVEN,
                        deck = record.deck,
                        room = record.room,
                        levelId = level.id,
                        levelNameSnapshot = level.name,
                        replied = false,
                        note = "",
                        timestamp = nowMs(),
                    ),
                )
            }
        }
    }

    /** 回礼弹窗取消：勾选回退，不写库 */
    fun cancelReply() {
        pendingReply.value = null
    }

    fun onAutoReplyToastShown() {
        autoRepliedRoom.value = null
    }
```

8. `onToggleReplied` 加守卫（勾选路径改走 pending，取消路径直写）：

```kotlin
    fun onToggleReplied(id: Long, replied: Boolean) {
        val record = state.value?.received?.firstOrNull { it.id == id } ?: return
        if (replied) {
            onRequestReply(record)  // 勾选走弹窗两步流
        } else {
            viewModelScope.launch { dustRepository.save(record.copy(replied = false)) }
        }
    }
```

- [ ] **Step 3: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（UI 尚未消费新 state 字段，未用警告可接受）

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt app/src/main/res/values/strings.xml
git commit -m "feat: linkage logic in ViewModel (auto-reply on save, pending reply flow, room pairs)"
```

---

### Task 3: ReplyLevelDialog（回礼等级弹窗）+ 列表/编辑弹窗接线 + toast

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/ReplyLevelDialog.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustLists.kt`（ReceivedList 勾选改走 onRequestReply）
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustEditDialog.kt`（勾选改上报请求）
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`（弹窗挂载 + autoRepliedRoom toast）

**Interfaces:**
- Consumes: Task 2 的 `pendingReply`/`confirmReply(level)`/`cancelReply()`/`onRequestReply(record)`、UiState `levels`/`levelById`
- Produces:
  - `@Composable fun ReplyLevelDialog(record: PixieDustRecord, levels: List<PixieLevel>, initialLevelId: Long?, onConfirm: (PixieLevel?) -> Unit, onDismiss: () -> Unit)`——Task 4 只消费挂载
  - `ReceivedList` 参数变化：`onToggleReplied: (Long, Boolean) -> Unit` 改为 `onRequestReply: (PixieDustRecord) -> Unit` + `onCancelReply: (Long) -> Unit`
  - `PixieDustEditDialog` 参数追加 `onRequestReply: () -> Unit`

- [ ] **Step 1: ReplyLevelDialog**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/ReplyLevelDialog.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel

/**
 * 回礼等级弹窗：勾选"已回礼"后弹出。
 * 预选收到记录的同级等级（已删则回落第一个）；确认创建发出记录，"仅勾选"跳过创建。
 */
@Composable
fun ReplyLevelDialog(
    record: PixieDustRecord,
    levels: List<PixieLevel>,
    onConfirm: (PixieLevel?) -> Unit,
    onDismiss: () -> Unit,
) {
    // 预选同级；该等级已被删则回落第一个
    var selectedId by remember(record.id) {
        mutableStateOf(
            levels.firstOrNull { it.id == record.levelId }?.id ?: levels.firstOrNull()?.id,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_reply_level_title_format, "${record.deck}${record.room}")) },
        text = {
            Column {
                Row {
                    levels.forEach { level ->
                        LevelChip(
                            level = level,
                            selected = selectedId == level.id,
                            onClick = { selectedId = level.id },
                        )
                        Spacer(Modifier.padding(2.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(levels.firstOrNull { it.id == selectedId }) },
                enabled = selectedId != null,
            ) { Text(stringResource(R.string.pixie_reply_confirm)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) {
                    Text(stringResource(R.string.pixie_reply_only_check))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            }
        },
    )
}
```

（`LevelChip` 来自 Task 6 of v1.8.0 的 `PixieDustKeypad.kt`，同包 internal 直接用。）

- [ ] **Step 2: ReceivedList 勾选改流**

`PixieDustLists.kt` 的 `ReceivedList` 签名改为：

```kotlin
@Composable
fun ReceivedList(
    state: PixieDustUiState,
    onEditRecord: (PixieDustRecord) -> Unit,
    onRequestReply: (PixieDustRecord) -> Unit,
    onCancelReply: (Long) -> Unit,
)
```

Checkbox 改为：

```kotlin
                Checkbox(
                    checked = rec.replied,
                    onCheckedChange = { checked ->
                        if (checked) onRequestReply(rec)
                        else onCancelReply(rec.id)
                    },
                )
```

- [ ] **Step 3: EditDialog 勾选上报**

`PixieDustEditDialog.kt` 签名追加参数 `onRequestReply: () -> Unit`（放 `onSave` 之前）。勾选处改为：

```kotlin
                if (isReceived) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = replied,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    replied = true
                                    onRequestReply()  // 上报请求，弹窗流程由外层接管
                                } else {
                                    replied = false
                                }
                            },
                        )
                        Text(stringResource(R.string.pixie_replied))
                    }
                }
```

（注意：编辑弹窗内勾选会触发外层 ReplyLevelDialog。外层 confirmReply 用的是**列表里的原记录**（pendingReply 里存的），而编辑弹窗本地可能改了房号/等级——设计规定两处都弹、以列表记录为准（勾的是哪条就回哪条）。编辑弹窗保存时 `build()` 会带上 replied=true（本地已置 true），与 confirmReply 的写库等价幂等。若用户在 ReplyLevelDialog 选了取消，编辑弹窗本地 replied 仍是 true——需要在取消时同步回退。为避免双状态打架，简化处理：**编辑弹窗勾选时先关闭编辑弹窗再弹回礼窗**——即 CruiseNavHost 里 `onRequestReply = { editTarget = null; vm.onRequestReply(target) }`，用弹窗前的 target 原记录走 pending 流。编辑弹窗本身不置本地 replied=true，只上报。实现者按此实现：Checkbox onCheckedChange 里 `if (checked) onRequestReply() else replied = false`，勾选后编辑弹窗被外层关闭。）

- [ ] **Step 4: CruiseNavHost 接线**

`CruiseNavHost.kt` PIXIE_DUST composable：

1. ReceivedList 调用处更新参数：

```kotlin
                            ReceivedList(
                                state = s,
                                onEditRecord = { editTarget = it },
                                onRequestReply = { vm.onRequestReply(it) },
                                onCancelReply = { vm.onToggleReplied(it, false) },
                            )
```

2. `PixieDustEditDialog` 调用处追加（`onSave` 前）：

```kotlin
                    onRequestReply = {
                        val target = target  // 当前编辑对象
                        editTarget = null    // 先关编辑弹窗
                        vm.onRequestReply(target)
                    },
```

3. 挂载回礼弹窗与 toast（`editTarget?.let` 块之后）：

```kotlin
            // ---- 回礼等级弹窗（联动方向二）----
            pixieState?.pendingReply?.let { pending ->
                ReplyLevelDialog(
                    record = pending,
                    levels = pixieState?.levels ?: emptyList(),
                    onConfirm = { vm.confirmReply(it) },
                    onDismiss = { vm.cancelReply() },
                )
            }

            // ---- 联动自动勾选 toast ----
            LaunchedEffect(pixieState?.autoRepliedRoom) {
                pixieState?.autoRepliedRoom?.let { roomLabel ->
                    Toast.makeText(context, context.getString(R.string.pixie_auto_replied_format, roomLabel), Toast.LENGTH_SHORT).show()
                    vm.onAutoReplyToastShown()
                }
            }
```

（`context`/`Toast` 的获取方式参照现有 savedEvent toast 的写法——同 composable 里已有 `val context = LocalContext.current` 与 `android.widget.Toast`，直接复用变量。`LaunchedEffect` 已 import。）

- [ ] **Step 5: Compile + manual verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

手动验证（装机后）：
- 先录一条收到（11550 · 精致），未勾 → 切发出 tab 快捷录入 11550 · 普通 → toast"已记录"随后"已自动勾选 11550 的回礼"，收到列表该记录已勾上
- 收到列表勾另一条未回礼 → 弹回礼窗（预选同级）→ 确认 → 收到勾上 + 发出列表多一条同级记录
- 弹窗选"仅勾选" → 只勾不创建；点取消 → 勾选回退
- 编辑弹窗内勾选 → 编辑窗关、回礼窗开

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: reply level dialog + linkage wiring (auto-reply toast, pending flow)"
```

---

### Task 4: LinkageViewDialog（联动检视图）+ 统计条按钮

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewDialog.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustScreen.kt`（统计条末尾加按钮）
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`（弹窗挂载）

**Interfaces:**
- Consumes: Task 2 的 UiState `roomPairs: List<RoomPair>`；Task 1 的 `RoomPair`；现有 `levelNameOf`/`LevelDot`/`levelCountsOf`
- Produces:
  - `@Composable fun LinkageViewDialog(state: PixieDustUiState, onDismiss: () -> Unit)`
  - `PixieStatBar` 末尾的 IconButton（`Icons.Outlined.Link`）

- [ ] **Step 1: LinkageViewDialog**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/LinkageViewDialog.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.RoomPair
import com.cruise.assistant.domain.levelCountsOf

/** 联动检视：按房间配对显示发出↔收到，只读 */
@Composable
fun LinkageViewDialog(state: PixieDustUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_linkage_view)) },
        text = {
            if (state.roomPairs.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.pixie_linkage_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(state.roomPairs, key = { "${it.deck}-${it.room}" }) { pair ->
                        RoomPairRow(state, pair)
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pixie_back)) }
        },
    )
}

@Composable
private fun RoomPairRow(state: PixieDustUiState, pair: RoomPair) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${pair.deck}${pair.room}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            if (pair.unrepliedReceived.isNotEmpty()) {
                Text(
                    stringResource(R.string.pixie_linkage_unreplied_format, pair.unrepliedReceived.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(Modifier.padding(2.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pixie_linkage_given_label, pair.given.size),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    pair.levelCountsText(state, pair.given),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("↔", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pixie_linkage_received_label, pair.received.size),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    pair.levelCountsText(state, pair.received),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun RoomPair.levelCountsText(state: PixieDustUiState, records: List<com.cruise.assistant.domain.PixieDustRecord>): String =
    levelCountsOf(records, state.levels).joinToString("  ") { "${it.level.name}×${it.count}" }
        .ifEmpty { "—" }
```

（`levelCountsText` 是 private 扩展，放文件底部即可；它只读 `state.levels`，签名里 state 参数够用。注意文件底部扩展函数引用 `RoomPair` 需要 import 已在顶部——实现时确认编译通过，若 `levelCountsText` 放 RoomPairRow 后面且带 state 参数则无前向引用问题。）

- [ ] **Step 2: 统计条加按钮**

`PixieDustScreen.kt` 的 `PixieStatBar`：Row 末尾（`if/else` 之后、Row 闭合之前）加：

```kotlin
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenLinkage) {
            Icon(Icons.Outlined.Link, contentDescription = stringResource(R.string.pixie_linkage_view))
        }
```

`PixieStatBar` 是 private 且只收 state——改为加参数 `onOpenLinkage: () -> Unit`，`PixieDustScreen` 相应加参数 `onOpenLinkage: () -> Unit` 并透传（放在 `onOpenLevelManager` 之后）。import 追加 `androidx.compose.material.icons.outlined.Link`、`androidx.compose.material3.IconButton`、`androidx.compose.material3.Icon`（IconButton/Icon 文件里已有，确认即可）。

- [ ] **Step 3: CruiseNavHost 挂载**

`CruiseNavHost.kt` PIXIE_DUST composable：

1. 加状态 `var showLinkage by remember { mutableStateOf(false) }`
2. `PixieDustScreen(...)` 调用处追加参数 `onOpenLinkage = { showLinkage = true },`
3. composable 末尾（等级管理 sheet 之后）：

```kotlin
            if (showLinkage) {
                LinkageViewDialog(
                    state = pixieState ?: return@composable,
                    onDismiss = { showLinkage = false },
                )
            }
```

（注意 `return@composable` 在 composable lambda 里不能用——`pixieState` 为 null 时跳过渲染用 `pixieState?.let` 包裹整个弹窗块：

```kotlin
            if (showLinkage) {
                pixieState?.let { s ->
                    LinkageViewDialog(state = s, onDismiss = { showLinkage = false })
                }
            }
```

）

- [ ] **Step 4: Compile + manual verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

手动验证：
- 统计条右侧出现链接图标 → 点击打开联动检视
- 录几条发出/收到混合数据 → 配对行正确：房号 + 待回礼红色标记 + 双侧分等级计数
- 排序：有未回礼（贵重优先）在前、不平衡次之、平衡沉底
- 无任何记录时显示空态文案

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: linkage view dialog with per-room pairing + stat bar entry"
```

---

### Task 5: 版本 1.8.1 + 全量回归 + APK

**Files:**
- Modify: `app/build.gradle.kts:26-27`

**Interfaces:**
- Consumes: 全部前序任务
- Produces: `versionCode = 15`、`versionName = "1.8.1"`、`app/release/CruiseAssistant-v1.8.1.apk`

- [ ] **Step 1: Bump version**

```kotlin
        versionCode = 15
        versionName = "1.8.1"
```

- [ ] **Step 2: Full test + build**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全部 PASS（含 Task 1 新增的联动测试）

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL，产出 `app/build/outputs/apk/release/CruiseAssistant-v1.8.1.apk`

- [ ] **Step 3: Manual regression（用户装机执行）**

1. v1.8.0 覆盖安装 → 数据完好（无迁移）
2. 方向一：录收到→快捷录发出同房间→自动勾选+toast
3. 方向二：勾回礼→弹窗（预选同级）→确认创建发出/仅勾选/取消回退
4. 编辑弹窗内勾选→关编辑弹窗开回礼窗
5. 联动检视：排序、待回礼标记、空态
6. 回归 v1.8.0 主流程：快记连记、双视图、等级管理、重置航程

- [ ] **Step 4: Copy APK + commit**

```bash
cp app/build/outputs/apk/release/CruiseAssistant-v1.8.1.apk app/release/
git add app/build.gradle.kts
git commit -m "release: v1.8.1 with pixie dust bidirectional linkage"
```

---

## Self-Review 记录

- **Spec 覆盖**：方向一（T2 saveRecord 联动+toast）、方向二（T2 pending 流 + T3 弹窗接线，两处触发点：列表 T3 Step 2、编辑弹窗 T3 Step 3）、防循环（T2 confirmReply 注释明确不触发方向一）、联动检视（T1 pairByRoom + T4 视图+按钮）、排序三分组（T1 测试覆盖）、同级预选+删级回落（T3 Step 1）、仅勾选跳过（T3）、取消勾选不弹窗不删发出（T2 onToggleReplied else 分支）、空态（T4）、版本 1.8.1（T5）——全覆盖。
- **占位符**：T1 Step 1/3 与 T3 Step 3、T4 Step 3 各有一处"注意"段落标注初稿代码的修正版（测试断言调整、sortedWith 重构、编辑弹窗勾选简化、return@composable 替代）——执行者按修正版实现，无 TBD。
- **类型一致性**：`RoomPair` 字段名（deck/room/given/received/unrepliedReceived/lastInteractTs）T1 定义 T4 消费一致；`pairByRoom(given, received, levelRankOf)` T1 签名与 T2 调用（尾 lambda 传 rank）一致；`onRequestReply(PixieDustRecord)` T2 定义 T3 两处调用一致；`confirmReply(PixieLevel?)` T2 定义 T3 弹窗 onConfirm 一致。
