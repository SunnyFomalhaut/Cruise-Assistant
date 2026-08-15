# 仙尘反向联动补全 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 登记收到时若该房间已有发出记录，自动把刚登记的收到记录置为已回礼并 toast——补全"先发后收"方向的联动，消除联动检视里"发出1 收到1 [待回礼1]"的矛盾。

**Architecture:** 镜像 v1.8.1 的方向一：domain 加一个纯函数 `hasGivenForRoom`（TDD），ViewModel `saveRecord()` 加对称的 RECEIVED 分支（用 save() 返回的新 id 更新，避免重复插入）。无 schema 变更、无新 UI。

**Tech Stack:** Kotlin + Compose，现有依赖，零新增。

**Spec:** `docs/superpowers/specs/2026-08-16-pixie-dust-reverse-linkage-design.md`

## Global Constraints

- versionCode 16，versionName `"1.8.2"`（三级版本号约定）
- toast 复用现有 `pixie_auto_replied_format` 字符串与 `autoRepliedRoom` 事件流，不新增文案
- 不新增依赖；Room version 保持 5
- domain 纯函数用 JUnit TDD；VM 以编译 + 手动回归为准
- 测试命令 `./gradlew :app:testDebugUnitTest --tests "..."`，编译检查 `./gradlew :app:compileDebugKotlin`

---

### Task 1: hasGivenForRoom 纯函数（TDD）+ saveRecord RECEIVED 分支

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt`（文件末尾追加函数）
- Modify: `app/src/test/java/com/cruise/assistant/domain/PixieDustLinkageTest.kt`（类内追加测试）
- Modify: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt`（saveRecord 的 launch 块）

**Interfaces:**
- Consumes: 现有 `PixieDustRepository.save(record): Long`（返回新插入 id）；`autoRepliedRoom: MutableStateFlow<String?>`；`PixieDustRecord.fullRoom`
- Produces: `fun hasGivenForRoom(given: List<PixieDustRecord>, deck: Int, room: String): Boolean`

- [ ] **Step 1: Write the failing test**

`PixieDustLinkageTest.kt` 类内追加（helper `given`/`received` 已存在于文件顶部）：

```kotlin
    // ---- hasGivenForRoom ----

    @Test
    fun `has given for room true when same deck and room given exists`() {
        val list = listOf(given(1, 11, "550", 1, 100))
        assertTrue(hasGivenForRoom(list, 11, "550"))
    }

    @Test
    fun `has given for room false when no given for room`() {
        val list = listOf(given(1, 11, "551", 1, 100), given(2, 12, "550", 1, 100))
        assertFalse(hasGivenForRoom(list, 11, "550"))
    }

    @Test
    fun `has given for room ignores received records`() {
        val list = listOf(received(1, 11, "550", 1, 100))
        assertFalse(hasGivenForRoom(list, 11, "550"))
    }
```

（文件头补 `import org.junit.Assert.assertFalse`。）

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustLinkageTest"`
Expected: 编译失败（`Unresolved reference: hasGivenForRoom`）

- [ ] **Step 3: Write minimal implementation**

`PixieDustLinkage.kt` 文件末尾追加：

```kotlin
/**
 * 方向一镜像：该房间是否已有发出记录。
 * 登记收到时判断要不要自动把刚登记的这条勾成已回礼（回礼动作在登记前已发生）。
 */
fun hasGivenForRoom(given: List<PixieDustRecord>, deck: Int, room: String): Boolean =
    given.any { it.direction == PixieDustDirection.GIVEN && it.deck == deck && it.room == room }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustLinkageTest"`
Expected: PASS（原 11 个 + 新 3 个 = 14 个全绿）

- [ ] **Step 5: saveRecord 加 RECEIVED 分支**

`PixieDustViewModel.kt` 的 `saveRecord()` launch 块改为（关键：接住 save 返回的新 id，否则 record.id==0 再 save 会重复插入）：

```kotlin
        viewModelScope.launch {
            val newId = dustRepository.save(record)
            savedEvent.value = record
            roomInput.value = ""
            // 联动方向一：仅发出记录触发（收到 tab 记一笔不该自勾自己）
            if (record.direction == PixieDustDirection.GIVEN) {
                val all = dustRepository.observeAll().first()
                val target = autoReplyTarget(
                    all.filter { it.direction == PixieDustDirection.RECEIVED },
                    record.deck, record.room,
                )
                if (target != null) {
                    dustRepository.save(target.copy(replied = true))
                    autoRepliedRoom.value = record.fullRoom
                }
            }
            // 联动方向一镜像：登记收到时该房间已有发出 → 刚登记的这条直接已回礼
            if (record.direction == PixieDustDirection.RECEIVED) {
                val all = dustRepository.observeAll().first()
                val hasGiven = hasGivenForRoom(
                    all.filter { it.direction == PixieDustDirection.GIVEN },
                    record.deck, record.room,
                )
                if (hasGiven) {
                    dustRepository.save(record.copy(id = newId, replied = true))
                    autoRepliedRoom.value = record.fullRoom
                }
            }
        }
```

（import 追加 `com.cruise.assistant.domain.hasGivenForRoom`。两个 if 分支互斥，不会都执行。）

- [ ] **Step 6: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PixieDustLinkage.kt app/src/test/java/com/cruise/assistant/domain/PixieDustLinkageTest.kt app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt
git commit -m "feat: reverse linkage — auto-check newly received record when room already has given"
```

---

### Task 2: 版本 1.8.2 + 全量测试 + APK

**Files:**
- Modify: `app/build.gradle.kts:23-24`

**Interfaces:**
- Consumes: Task 1
- Produces: `versionCode = 16`、`versionName = "1.8.2"`、`app/release/CruiseAssistant-v1.8.2.apk`

- [ ] **Step 1: Bump version**

```kotlin
        versionCode = 16
        versionName = "1.8.2"
```

- [ ] **Step 2: Full test + build**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全部 PASS（127 + 3 = 130 个）

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL，产出 `app/build/outputs/apk/release/CruiseAssistant-v1.8.2.apk`

- [ ] **Step 3: Manual regression（用户装机执行）**

1. v1.8.1 覆盖安装 → 数据完好
2. 先记发出 8234 → 切收到 tab 记 8234 → toast"已自动勾选"，收到列表该记录已是勾选态
3. 联动检视：8234 显示发出1 ↔ 收到1，无待回礼标记
4. 无发出的房间登记收到 → 不自动勾（正常未回礼）
5. 回归方向一（先收后发）与回礼弹窗流程不变

- [ ] **Step 4: Copy APK + commit**

```bash
cp app/build/outputs/apk/release/CruiseAssistant-v1.8.2.apk app/release/
git add app/build.gradle.kts
git commit -m "release: v1.8.2 with reverse pixie dust linkage"
```

---

## Self-Review 记录

- **Spec 覆盖**：hasGivenForRoom（T1 Step 1-4）、RECEIVED 分支含 newId 修正（T1 Step 5，spec"ViewModel 变更"节明确要求接住返回值）、toast 复用（T1 Step 5 用现有 autoRepliedRoom 流）、仅快捷录入触发（T1 只改 saveRecord）、防循环（只置 replied 不建新记录）、历史不修（无任务，符合 spec）、版本 1.8.2（T2）——全覆盖。
- **占位符**：无 TBD；所有代码块完整。
- **类型一致性**：`hasGivenForRoom(given: List<PixieDustRecord>, deck: Int, room: String): Boolean` T1 定义与调用一致；`dustRepository.save(record): Long` 返回值签名与 newId 接收一致；`record.copy(id = newId, replied = true)` 字段名与 PixieDustRecord 一致。
