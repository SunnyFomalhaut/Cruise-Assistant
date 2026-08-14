# Pixie Dust（仙尘）交换记录 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增独立的仙尘交换记录界面：数字键盘快捷记录"发给谁"，收到列表按"未回礼+贵重优先"排序提醒回礼，等级可自定义增删改。

**Architecture:** 单表 `pixie_dust`（方向字段区分发出/收到）+ `pixie_level` 等级表，Room schema v4→v5（AutoMigration，纯增表）。排序/归组/统计逻辑放 domain 层纯函数（可 JUnit 测试），UI 走现有 `viewModelFactory` + 单 Activity 导航模式，新路由 `pixiedust`，时间线加两个入口（顶栏图标 + FAB 上方小按钮）。

**Tech Stack:** Kotlin + Jetpack Compose (Material3) + Room + DataStore，现有依赖，零新增。

**Spec:** `docs/superpowers/specs/2026-08-14-pixie-dust-design.md`

## Global Constraints

- versionCode 14，versionName `"1.8.0"`（三级版本号约定，大功能进 minor 位）
- 所有用户可见文案为中文，放 `app/src/main/res/values/strings.xml`，代码里不硬编码 UI 文案
- 不新增任何依赖；minSdk 26（java.time 直接可用）
- Room version = 5，用 `AutoMigration(from = 4, to = 5)`（只增表，无需手写 SQL）；schema JSON 生成在 `app/schemas/` 但**不提交**（仓库现状 2/3/4.json 均未提交）
- 遵循现有模式：手写 DI（`AppContainer`）、`ui/common viewModelFactory`、Repository 包装 DAO、domain 模型 + `data/db/Mappers.kt` 映射
- 单元测试只有纯 JVM JUnit（无 Robolectric/androidTest）——只有 domain 层纯函数写测试；数据层/UI 任务以编译通过 + 手动验证为准
- 测试命令统一 `./gradlew :app:testDebugUnitTest --tests "..."`，编译检查 `./gradlew :app:compileDebugKotlin`

---

### Task 1: Domain 模型与纯函数（排序/归组/统计/校验）

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/PixieDust.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/PixieDustTest.kt`

**Interfaces:**
- Produces（后续任务全部依赖这些签名）:
  - `enum class PixieDustDirection { GIVEN, RECEIVED }`
  - `data class PixieLevel(id: Long = 0, name: String, colorArgb: Int, iconKey: String, rank: Int, sortOrder: Int = 0)`
  - `data class PixieDustRecord(id: Long = 0, direction: PixieDustDirection, deck: Int, room: String, levelId: Long, levelNameSnapshot: String, replied: Boolean = false, note: String = "", timestamp: Long)`
  - `val PixieDustRecord.fullRoom: String`（如 deck=11, room="550" → "11550"）
  - `data class LevelCount(level: PixieLevel, count: Int)`
  - `data class RoomGroup(deck: Int, room: String, lastTimestamp: Long, total: Int, levelCounts: List<LevelCount>)`
  - `fun isValidRoomInput(input: String): Boolean`
  - `fun levelCountsOf(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<LevelCount>`（rank 降序；等级已删时用快照名兜底）
  - `fun groupGivenByRoom(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<RoomGroup>`（按最近发出时间降序）
  - `fun sortReceived(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<PixieDustRecord>`（未回礼在前按 rank 降序、同级时间降序；已回礼沉底按时间降序）
  - `fun lastLevelForRoom(records: List<PixieDustRecord>, deck: Int, room: String): PixieDustRecord?`（该房间最近一次 GIVEN）

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/cruise/assistant/domain/PixieDustTest.kt`:

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixieDustTest {

    private val levels = listOf(
        PixieLevel(id = 1, name = "普通", colorArgb = 0, iconKey = "gift", rank = 1, sortOrder = 0),
        PixieLevel(id = 2, name = "精致", colorArgb = 0, iconKey = "sparkle", rank = 2, sortOrder = 1),
        PixieLevel(id = 3, name = "豪华", colorArgb = 0, iconKey = "diamond", rank = 3, sortOrder = 2),
    )

    private fun given(id: Long, deck: Int, room: String, levelId: Long, ts: Long) = PixieDustRecord(
        id = id, direction = PixieDustDirection.GIVEN, deck = deck, room = room,
        levelId = levelId, levelNameSnapshot = "", timestamp = ts,
    )

    private fun received(id: Long, deck: Int, room: String, levelId: Long, ts: Long, replied: Boolean = false) =
        PixieDustRecord(
            id = id, direction = PixieDustDirection.RECEIVED, deck = deck, room = room,
            levelId = levelId, levelNameSnapshot = "", replied = replied, timestamp = ts,
        )

    @Test
    fun `room input requires exactly three digits`() {
        assertTrue(isValidRoomInput("550"))
        assertFalse(isValidRoomInput("55"))
        assertFalse(isValidRoomInput("5500"))
        assertFalse(isValidRoomInput("5a0"))
        assertFalse(isValidRoomInput(""))
    }

    @Test
    fun `full room label concatenates deck and room`() {
        assertEquals("11550", given(1, 11, "550", 1, 0).fullRoom)
    }

    @Test
    fun `level counts sorted by rank desc`() {
        val counts = levelCountsOf(
            listOf(given(1, 11, "550", 1, 0), given(2, 11, "551", 1, 1), given(3, 11, "552", 3, 2)),
            levels,
        )
        assertEquals(listOf(3 to 1, 1 to 2), counts.map { it.level.id to it.count })
    }

    @Test
    fun `level counts fall back to snapshot name for deleted level`() {
        val counts = levelCountsOf(listOf(given(1, 11, "550", 99, 0).copy(levelNameSnapshot = "绝版")), levels)
        assertEquals(1, counts.size)
        assertEquals("绝版", counts[0].level.name)
        assertEquals(1, counts[0].count)
    }

    @Test
    fun `given grouped by room sorted by last timestamp desc`() {
        val groups = groupGivenByRoom(
            listOf(
                given(1, 11, "550", 1, 100),
                given(2, 11, "550", 3, 300),
                given(3, 12, "100", 2, 200),
            ),
            levels,
        )
        assertEquals(2, groups.size)
        assertEquals("550", groups[0].room)   // 最近一次 ts=300
        assertEquals(2, groups[0].total)
        assertEquals(3, groups[0].levelCounts[0].level.id) // 组内也按 rank 降序
        assertEquals("100", groups[1].room)
    }

    @Test
    fun `received sorted unreplied first by rank desc then replied by time desc`() {
        val sorted = sortReceived(
            listOf(
                received(1, 11, "550", 1, 100, replied = true),
                received(2, 11, "551", 3, 50),          // 豪华未回礼 → 第一
                received(3, 11, "552", 1, 60),          // 普通未回礼 → 第二
                received(4, 11, "553", 2, 70),          // 精致未回礼 → 第三（rank 3>2>1）
                received(5, 11, "554", 3, 80, replied = true),  // 已回礼 ts=80 → 第四
                received(6, 11, "555", 1, 90, replied = true),  // 已回礼 ts=90 → 实际第一（时间降序）
            ),
            levels,
        )
        assertEquals(listOf(6L, 2L, 4L, 3L, 5L, 1L), sorted.map { it.id })
    }

    @Test
    fun `received sorting ignores given records`() {
        val sorted = sortReceived(listOf(given(1, 11, "550", 1, 100)), levels)
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `last level for room returns latest given only`() {
        val records = listOf(
            given(1, 11, "550", 1, 100),
            given(2, 11, "550", 3, 300),
            given(3, 12, "550", 2, 999),   // 不同楼层不算
            given(4, 11, "551", 2, 400),
        )
        assertEquals(2L, lastLevelForRoom(records, 11, "550")?.id)
        assertNull(lastLevelForRoom(records, 11, "999"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustTest"`
Expected: 编译失败（`Unresolved reference: isValidRoomInput` 等）

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/cruise/assistant/domain/PixieDust.kt`:

```kotlin
package com.cruise.assistant.domain

/** 仙尘记录方向：发出（我给别间）或收到（别间给我） */
enum class PixieDustDirection { GIVEN, RECEIVED }

/** 礼物等级。rank 越大越贵重；sortOrder 是键盘/管理里的显示顺序。 */
data class PixieLevel(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    val rank: Int,
    val sortOrder: Int = 0,
)

data class PixieDustRecord(
    val id: Long = 0,
    val direction: PixieDustDirection,
    val deck: Int,
    val room: String,
    val levelId: Long,
    /** 等级被删后历史记录仍显示的名字 */
    val levelNameSnapshot: String,
    val replied: Boolean = false,
    val note: String = "",
    val timestamp: Long,
)

/** deck=11, room="550" → "11550" */
val PixieDustRecord.fullRoom: String get() = "$deck$room"

data class LevelCount(val level: PixieLevel, val count: Int)

/** 发出列表"按房间归组"视图的一行 */
data class RoomGroup(
    val deck: Int,
    val room: String,
    val lastTimestamp: Long,
    val total: Int,
    val levelCounts: List<LevelCount>,
)

/** 房号必须是恰好三位数字 */
fun isValidRoomInput(input: String): Boolean = input.length == 3 && input.all { it.isDigit() }

private const val DELETED_LEVEL_COLOR = 0xFF9E9E9E.toInt()

/** 按等级计数，rank 降序；等级已被删时用记录里的名称快照兜底成一个灰色等级 */
fun levelCountsOf(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<LevelCount> {
    val byId = levels.associateBy { it.id }
    return records
        .groupBy { it.levelId }
        .map { (id, list) ->
            val level = byId[id] ?: PixieLevel(
                id = id,
                name = list.first().levelNameSnapshot,
                colorArgb = DELETED_LEVEL_COLOR,
                iconKey = "gift",
                rank = 0,
            )
            LevelCount(level, list.size)
        }
        .sortedByDescending { it.level.rank }
}

/** 发出记录按房间归组，房间按最近一次发出时间降序 */
fun groupGivenByRoom(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<RoomGroup> =
    records
        .filter { it.direction == PixieDustDirection.GIVEN }
        .groupBy { it.deck to it.room }
        .map { (key, list) ->
            RoomGroup(
                deck = key.first,
                room = key.second,
                lastTimestamp = list.maxOf { it.timestamp },
                total = list.size,
                levelCounts = levelCountsOf(list, levels),
            )
        }
        .sortedByDescending { it.lastTimestamp }

/** 收到排序：未回礼在前按等级 rank 降序（贵重优先）、同级时间降序；已回礼沉底按时间降序 */
fun sortReceived(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<PixieDustRecord> {
    val rank = levels.associate { it.id to it.rank }
    val unreplied = records
        .filter { it.direction == PixieDustDirection.RECEIVED && !it.replied }
        .sortedWith(compareByDescending<PixieDustRecord> { rank[it.levelId] ?: 0 }.thenByDescending { it.timestamp })
    val replied = records
        .filter { it.direction == PixieDustDirection.RECEIVED && it.replied }
        .sortedByDescending { it.timestamp }
    return unreplied + replied
}

/** 该房间最近一次发出（用于"上次等级"预选与历史小字）。不含收到记录。 */
fun lastLevelForRoom(records: List<PixieDustRecord>, deck: Int, room: String): PixieDustRecord? =
    records
        .filter { it.direction == PixieDustDirection.GIVEN && it.deck == deck && it.room == room }
        .maxByOrNull { it.timestamp }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cruise.assistant.domain.PixieDustTest"`
Expected: PASS（8 个测试全绿）

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PixieDust.kt app/src/test/java/com/cruise/assistant/domain/PixieDustTest.kt
git commit -m "feat: pixie dust domain models and sorting/grouping/stats logic"
```

---

### Task 2: Room schema v5——实体、DAO、AutoMigration、映射、内置等级

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/data/db/entity/PixieDustEntity.kt`
- Create: `app/src/main/java/com/cruise/assistant/data/db/entity/PixieLevelEntity.kt`
- Create: `app/src/main/java/com/cruise/assistant/data/db/dao/PixieDustDao.kt`
- Create: `app/src/main/java/com/cruise/assistant/data/db/dao/PixieLevelDao.kt`
- Create: `app/src/main/java/com/cruise/assistant/data/BuiltInPixieLevels.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/common/CategoryIcons.kt`

**Interfaces:**
- Consumes: Task 1 的 `PixieDustRecord` / `PixieLevel` / `PixieDustDirection`
- Produces:
  - `PixieDustDao`: `observeAll(): Flow<List<PixieDustEntity>>`, `getById(id: Long)`, `insert(entity): Long`, `update(entity)`, `delete(entity)`, `countByLevel(levelId): Int`, `clearAll()`
  - `PixieLevelDao`: `observeAll(): Flow<List<PixieLevelEntity>>`, `observeAllOnce(): List<PixieLevelEntity>`, `getById(id)`, `insert(entity): Long`, `insertAll(list)`, `update(entity)`, `delete(entity)`, `count(): Int`, `clearAll()`
  - `CruiseDatabase`: `pixieDustDao()`, `pixieLevelDao()`，`version = 5`
  - Mappers: `PixieDustEntity.toDomain()/PixieDustRecord.toEntity()`, `PixieLevelEntity.toDomain()/PixieLevel.toEntity()`
  - `data.BUILT_IN_PIXIE_LEVELS: List<PixieLevel>`（普通/精致/豪华）
  - `ICON_REGISTRY` 新增 key：`gift`、`sparkle`、`diamond`、`star`、`heart`

- [ ] **Step 1: Create entities**

`app/src/main/java/com/cruise/assistant/data/db/entity/PixieDustEntity.kt`:

```kotlin
package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 单表存发出/收到，direction 字段区分（"GIVEN"/"RECEIVED"） */
@Entity(
    tableName = "pixie_dust",
    indices = [Index("direction"), Index("deck", "room")],
)
data class PixieDustEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String,
    val deck: Int,
    val room: String,
    val levelId: Long,
    val levelNameSnapshot: String,
    val replied: Boolean,
    val note: String,
    val timestamp: Long,
)
```

`app/src/main/java/com/cruise/assistant/data/db/entity/PixieLevelEntity.kt`:

```kotlin
package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pixie_level")
data class PixieLevelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    /** 越大越贵重 */
    val rank: Int,
    val sortOrder: Int,
)
```

- [ ] **Step 2: Create DAOs**

`app/src/main/java/com/cruise/assistant/data/db/dao/PixieDustDao.kt`:

```kotlin
package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.PixieDustEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PixieDustDao {
    @Query("SELECT * FROM pixie_dust ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PixieDustEntity>>

    @Query("SELECT * FROM pixie_dust WHERE id = :id")
    suspend fun getById(id: Long): PixieDustEntity?

    @Insert
    suspend fun insert(record: PixieDustEntity): Long

    @Update
    suspend fun update(record: PixieDustEntity)

    @Delete
    suspend fun delete(record: PixieDustEntity)

    @Query("SELECT COUNT(*) FROM pixie_dust WHERE levelId = :levelId")
    suspend fun countByLevel(levelId: Long): Int

    @Query("DELETE FROM pixie_dust")
    suspend fun clearAll()
}
```

`app/src/main/java/com/cruise/assistant/data/db/dao/PixieLevelDao.kt`:

```kotlin
package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.PixieLevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PixieLevelDao {
    @Query("SELECT * FROM pixie_level ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<PixieLevelEntity>>

    @Query("SELECT * FROM pixie_level ORDER BY sortOrder, id")
    suspend fun observeAllOnce(): List<PixieLevelEntity>

    @Query("SELECT * FROM pixie_level WHERE id = :id")
    suspend fun getById(id: Long): PixieLevelEntity?

    @Insert
    suspend fun insert(level: PixieLevelEntity): Long

    @Insert
    suspend fun insertAll(levels: List<PixieLevelEntity>)

    @Update
    suspend fun update(level: PixieLevelEntity)

    @Delete
    suspend fun delete(level: PixieLevelEntity)

    @Query("SELECT COUNT(*) FROM pixie_level")
    suspend fun count(): Int

    @Query("DELETE FROM pixie_level")
    suspend fun clearAll()
}
```

- [ ] **Step 3: Register in CruiseDatabase with AutoMigration**

`app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`——修改 `@Database` 注解、version、添加抽象 DAO 方法（import 相应类）：

```kotlin
@Database(
    entities = [
        VoyageEntity::class,
        CategoryEntity::class,
        EventEntity::class,
        DayPlanEntity::class,
        LocationPresetEntity::class,
        PixieDustEntity::class,
        PixieLevelEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 4, to = 5)],
)
```

（需要新 import：`androidx.room.AutoMigration`、两个新 entity。类体内添加：）

```kotlin
    abstract fun pixieDustDao(): PixieDustDao
    abstract fun pixieLevelDao(): PixieLevelDao
```

纯新增两张表，AutoMigration 自动建表，无需手写 SQL。`app/schemas/.../5.json` 由 KSP 生成，**不提交**（与 2/3/4.json 现状一致）。

- [ ] **Step 4: Add mappers**

`app/src/main/java/com/cruise/assistant/data/db/Mappers.kt` 末尾追加（并 import 四个类：`entity.PixieDustEntity`、`entity.PixieLevelEntity`、`domain.PixieDustRecord`、`domain.PixieLevel`）：

```kotlin
fun PixieDustEntity.toDomain() = PixieDustRecord(
    id = id, direction = PixieDustDirection.valueOf(direction), deck = deck, room = room,
    levelId = levelId, levelNameSnapshot = levelNameSnapshot, replied = replied, note = note,
    timestamp = timestamp,
)

fun PixieDustRecord.toEntity() = PixieDustEntity(
    id = id, direction = direction.name, deck = deck, room = room,
    levelId = levelId, levelNameSnapshot = levelNameSnapshot, replied = replied, note = note,
    timestamp = timestamp,
)

fun PixieLevelEntity.toDomain() = PixieLevel(id, name, colorArgb, iconKey, rank, sortOrder)

fun PixieLevel.toEntity() = PixieLevelEntity(id, name, colorArgb, iconKey, rank, sortOrder)
```

- [ ] **Step 5: Built-in levels + icons**

`app/src/main/java/com/cruise/assistant/data/BuiltInPixieLevels.kt`:

```kotlin
package com.cruise.assistant.data

import com.cruise.assistant.domain.PixieLevel

/** 默认三级：普通（灰）/精致（蓝）/豪华（金）。用户可自由增删改。 */
val BUILT_IN_PIXIE_LEVELS: List<PixieLevel> = listOf(
    PixieLevel(name = "普通", colorArgb = 0xFF9E9E9E.toInt(), iconKey = "gift", rank = 1, sortOrder = 0),
    PixieLevel(name = "精致", colorArgb = 0xFF5B8DEF.toInt(), iconKey = "sparkle", rank = 2, sortOrder = 1),
    PixieLevel(name = "豪华", colorArgb = 0xFFF2C94C.toInt(), iconKey = "diamond", rank = 3, sortOrder = 2),
)
```

`app/src/main/java/com/cruise/assistant/ui/common/CategoryIcons.kt`——imports 添加：

```kotlin
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Star
```

`ICON_REGISTRY` 列表末尾（`"search" to ...` 之前或之后均可）追加：

```kotlin
    "gift" to Icons.Outlined.CardGiftcard,
    "sparkle" to Icons.Outlined.AutoAwesome,
    "diamond" to Icons.Outlined.Diamond,
    "star" to Icons.Outlined.Star,
    "heart" to Icons.Outlined.Favorite,
```

- [ ] **Step 6: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（Room 校验 AutoMigration 通过，`app/schemas/com.cruise.assistant.data.db.CruiseDatabase/5.json` 生成）

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/data
git commit -m "feat: pixie dust + level Room entities, DAOs, v5 AutoMigration, built-in levels"
```

---

### Task 3: Repository、AppContainer、SettingsStore(lastPixieDeck)、播种与重置航程

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/data/repo/PixieDustRepository.kt`
- Create: `app/src/main/java/com/cruise/assistant/data/repo/PixieLevelRepository.kt`
- Modify: `app/src/main/java/com/cruise/assistant/AppContainer.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/prefs/SettingsStore.kt`
- Modify: `app/src/main/java/com/cruise/assistant/CruiseApp.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/repo/VoyageRepository.kt`

**Interfaces:**
- Consumes: Task 2 的 DAO 与 Mappers；`BUILT_IN_PIXIE_LEVELS`
- Produces:
  - `PixieDustRepository(dao)`: `observeAll(): Flow<List<PixieDustRecord>>`、`save(record): Long`（id==0 插入否则更新）、`delete(record)`、`countByLevel(levelId): Int`、`clearAll()`
  - `PixieLevelRepository(dao)`: `observeAll(): Flow<List<PixieLevel>>`、`save(level): Long`、`delete(level)`、`count(): Int`、`seedBuiltIns()`（按名幂等补缺）、`clearAll()`
  - `AppContainer.pixieDustRepository` / `AppContainer.pixieLevelRepository`
  - `AppSettings.lastPixieDeck: Int?` + `SettingsStore.setLastPixieDeck(deck: Int)`
  - `VoyageRepository.resetVoyage()` 现在也清空 pixie 两表并重播内置等级

- [ ] **Step 1: Repositories**

`app/src/main/java/com/cruise/assistant/data/repo/PixieDustRepository.kt`:

```kotlin
package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.PixieDustRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PixieDustRepository(private val dao: PixieDustDao) {
    fun observeAll(): Flow<List<PixieDustRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** id==0 插入，否则更新。 */
    suspend fun save(record: PixieDustRecord): Long =
        if (record.id == 0L) {
            dao.insert(record.toEntity())
        } else {
            dao.update(record.toEntity())
            record.id
        }

    suspend fun delete(record: PixieDustRecord) = dao.delete(record.toEntity())

    suspend fun countByLevel(levelId: Long): Int = dao.countByLevel(levelId)

    suspend fun clearAll() = dao.clearAll()
}
```

`app/src/main/java/com/cruise/assistant/data/repo/PixieLevelRepository.kt`:

```kotlin
package com.cruise.assistant.data.repo

import com.cruise.assistant.data.BUILT_IN_PIXIE_LEVELS
import com.cruise.assistant.data.db.dao.PixieLevelDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.PixieLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PixieLevelRepository(private val dao: PixieLevelDao) {
    fun observeAll(): Flow<List<PixieLevel>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** id==0 插入，否则更新。 */
    suspend fun save(level: PixieLevel): Long =
        if (level.id == 0L) {
            dao.insert(level.toEntity())
        } else {
            dao.update(level.toEntity())
            level.id
        }

    suspend fun delete(level: PixieLevel) = dao.delete(level.toEntity())

    suspend fun count(): Int = dao.count()

    /** 幂等补缺内置等级（按名字匹配），升级用户也能拿到默认三级 */
    suspend fun seedBuiltIns() {
        val existing = dao.observeAllOnce()
        val missing = BUILT_IN_PIXIE_LEVELS.filter { builtIn ->
            existing.none { it.name == builtIn.name }
        }
        if (missing.isNotEmpty()) dao.insertAll(missing.map { it.toEntity() })
    }
}
```

- [ ] **Step 2: SettingsStore——记住上次楼层**

`AppSettings` data class 添加字段（`onboardingCompleted` 之后）：

```kotlin
    val lastPixieDeck: Int? = null,
```

`Keys` object 添加：

```kotlin
        val lastPixieDeck = intPreferencesKey("last_pixie_deck")
```

`settings` Flow 的 `AppSettings(...)` 构造里添加：

```kotlin
            lastPixieDeck = prefs[Keys.lastPixieDeck],
```

类末尾（`setOnboardingCompleted` 之后）添加：

```kotlin
    suspend fun setLastPixieDeck(deck: Int) =
        edit { it[Keys.lastPixieDeck] = deck }
```

- [ ] **Step 3: AppContainer + CruiseApp 播种**

`AppContainer.kt`——`locationPresetRepository` 之后添加（并 import）：

```kotlin
    val pixieDustRepository = PixieDustRepository(db.pixieDustDao())

    val pixieLevelRepository = PixieLevelRepository(db.pixieLevelDao())
```

`CruiseApp.kt` 的 `seedMissingBuiltInCategories()` 同一个 launch 块里追加一行：

```kotlin
                container.pixieLevelRepository.seedBuiltIns()
```

- [ ] **Step 4: resetVoyage 清空并重播**

`VoyageRepository.kt`——构造函数添加两个参数（放在 `locationPresetDao` 之后）：

```kotlin
    private val pixieDustDao: PixieDustDao,
    private val pixieLevelDao: PixieLevelDao,
```

（import `PixieDustDao`、`PixieLevelDao`、`BUILT_IN_PIXIE_LEVELS`、`toEntity` 已有 `com.cruise.assistant.data.db.toEntity`——确认 `PixieLevel.toEntity` 可解析。）

`resetVoyage()` 改为：

```kotlin
    /** 设置页"重建航次"：清空一切（类别保留，因为用户可能改过颜色）；
     *  仙尘记录与等级一并清空并重播默认三级。 */
    suspend fun resetVoyage() {
        eventDao.clearAll()
        voyageDao.clearDayPlans()
        voyageDao.clearVoyage()
        locationPresetDao.clearAll()
        pixieDustDao.clearAll()
        pixieLevelDao.clearAll()
        pixieLevelDao.insertAll(BUILT_IN_PIXIE_LEVELS.map { it.toEntity() })
    }
```

同时更新 `AppContainer` 里 `VoyageRepository(...)` 构造调用，补上 `pixieDustDao = db.pixieDustDao(), pixieLevelDao = db.pixieLevelDao()`。

- [ ] **Step 5: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant
git commit -m "feat: pixie dust repositories, container wiring, last-deck pref, seeding & voyage reset"
```

---

### Task 4: PixieDustViewModel

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt`

**Interfaces:**
- Consumes: Task 1 全部纯函数；Task 3 的两个 Repository 与 `SettingsStore.setLastPixieDeck`
- Produces（Task 5-9 的 UI 依赖）:
  - `enum class PixieTab { GIVEN, RECEIVED }`
  - `enum class GivenViewMode { GROUPED, FLOW }`
  - `data class RoomHistory(levelName: String, count: Int)`
  - `data class PixieDustUiState(...)`（见代码）
  - `class PixieDustViewModel(dustRepository, levelRepository, voyageRepository, settingsStore, nowMs: () -> Long = System::currentTimeMillis)`
  - 方法：`onTabChange`、`onDeckChange`、`onDigit(Char)`、`onBackspace`、`onClearRoom`、`onLevelSelect(Long)`、`saveRecord()`、`onToggleKeypad`、`onToggleViewMode`、`onToggleReplied(Long, Boolean)`、`saveEdited(PixieDustRecord)`、`deleteRecord(PixieDustRecord)`、`saveLevel(PixieLevel)`、`deleteLevel(PixieLevel)`、`onMoveLevel(PixieLevel, Boolean)`、`suspend countByLevel(Long): Int`、`onToastShown()`

- [ ] **Step 1: Write the ViewModel**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustViewModel.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.PixieDustRepository
import com.cruise.assistant.data.repo.PixieLevelRepository
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.RoomGroup
import com.cruise.assistant.domain.isValidRoomInput
import com.cruise.assistant.domain.groupGivenByRoom
import com.cruise.assistant.domain.lastLevelForRoom
import com.cruise.assistant.domain.sortReceived
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PixieTab { GIVEN, RECEIVED }

enum class GivenViewMode { GROUPED, FLOW }

/** 键盘下方"上次：豪华 ×2"的数据 */
data class RoomHistory(val levelName: String, val count: Int)

data class PixieDustUiState(
    val loading: Boolean = true,
    val tab: PixieTab = PixieTab.GIVEN,
    val levels: List<PixieLevel> = emptyList(),
    val levelById: Map<Long, PixieLevel> = emptyMap(),
    /** 楼层可选范围：航次 minDeck..maxDeck，未配置回退 1..20 */
    val deckRange: IntRange = 1..20,
    val deck: Int = 1,
    val roomInput: String = "",
    val selectedLevelId: Long? = null,
    /** 发出记录，时间倒序原始流 */
    val given: List<PixieDustRecord> = emptyList(),
    /** 收到记录，sortReceived 排好 */
    val received: List<PixieDustRecord> = emptyList(),
    /** 发出归组视图数据 */
    val grouped: List<RoomGroup> = emptyList(),
    val viewMode: GivenViewMode = GivenViewMode.GROUPED,
    val keypadCollapsed: Boolean = false,
    val roomHistory: RoomHistory? = null,
    /** 一次性 toast 事件：刚存入的记录。UI 显示后调 onToastShown() 清空 */
    val savedEvent: PixieDustRecord? = null,
)

class PixieDustViewModel(
    private val dustRepository: PixieDustRepository,
    private val levelRepository: PixieLevelRepository,
    voyageRepository: VoyageRepository,
    private val settingsStore: SettingsStore,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val tab = MutableStateFlow(PixieTab.GIVEN)
    private val viewMode = MutableStateFlow(GivenViewMode.GROUPED)
    private val keypadCollapsed = MutableStateFlow(false)
    private val deck = MutableStateFlow<Int?>(null)
    private val roomInput = MutableStateFlow("")
    private val selectedLevelId = MutableStateFlow<Long?>(null)
    private val savedEvent = MutableStateFlow<PixieDustRecord?>(null)

    init {
        // 等级列表加载/变化时：无选择 → 默认选第一个；当前选择被删 → 回落到第一个
        levelRepository.observeAll().onEach { levels ->
            val current = selectedLevelId.value
            if (levels.isEmpty()) {
                selectedLevelId.value = null
            } else if (current == null || levels.none { it.id == current }) {
                selectedLevelId.value = levels.first().id
            }
        }.launchIn(viewModelScope)
    }

    /** 6 个 UI 输入流打包（combine 类型化重载最多 5 个） */
    private val ui = combine(
        tab, viewMode, keypadCollapsed, roomInput, selectedLevelId, deck, savedEvent,
    ) { arr ->
        UiInputs(
            tab = arr[0] as PixieTab,
            viewMode = arr[1] as GivenViewMode,
            keypadCollapsed = arr[2] as Boolean,
            roomInput = arr[3] as String,
            selectedLevelId = arr[4] as Long?,
            deck = arr[5] as Int?,
            savedEvent = arr[6] as PixieDustRecord?,
        )
    }

    private data class UiInputs(
        val tab: PixieTab,
        val viewMode: GivenViewMode,
        val keypadCollapsed: Boolean,
        val roomInput: String,
        val selectedLevelId: Long?,
        val deck: Int?,
        val savedEvent: PixieDustRecord?,
    )

    val state: StateFlow<PixieDustUiState?> = combine(
        dustRepository.observeAll(),
        levelRepository.observeAll(),
        voyageRepository.observeVoyage(),
        settingsStore.settings,
        ui,
    ) { dust, levels, voyage, settings, input ->
        val deckRange =
            if ((voyage?.maxDeck ?: 0) > 0) voyage!!.minDeck..voyage.maxDeck else 1..20
        val deckValue = input.deck
            ?: settings.lastPixieDeck?.takeIf { it in deckRange }
            ?: deckRange.first
        val given = dust
            .filter { it.direction == PixieDustDirection.GIVEN }
            .sortedByDescending { it.timestamp }
        val roomHistory = if (isValidRoomInput(input.roomInput)) {
            lastLevelForRoom(dust, deckValue, input.roomInput)?.let { last ->
                RoomHistory(
                    levelName = levels.firstOrNull { it.id == last.levelId }?.name
                        ?: last.levelNameSnapshot,
                    count = given.count { it.deck == deckValue && it.room == input.roomInput },
                )
            }
        } else {
            null
        }
        PixieDustUiState(
            loading = false,
            tab = input.tab,
            levels = levels,
            levelById = levels.associateBy { it.id },
            deckRange = deckRange,
            deck = deckValue,
            roomInput = input.roomInput,
            selectedLevelId = input.selectedLevelId,
            given = given,
            received = sortReceived(dust, levels),
            grouped = groupGivenByRoom(dust, levels),
            viewMode = input.viewMode,
            keypadCollapsed = input.keypadCollapsed,
            roomHistory = roomHistory,
            savedEvent = input.savedEvent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onTabChange(newTab: PixieTab) {
        tab.value = newTab
    }

    /** 换楼层后房号指向不同房间，清空重输 */
    fun onDeckChange(newDeck: Int) {
        deck.value = newDeck
        roomInput.value = ""
        viewModelScope.launch { settingsStore.setLastPixieDeck(newDeck) }
    }

    fun onDigit(d: Char) {
        if (!d.isDigit() || roomInput.value.length >= 3) return
        roomInput.value += d
        // 房号输满三位且该房间有历史 → 预选上次等级
        if (roomInput.value.length == 3) {
            val s = state.value ?: return
            lastLevelForRoom(s.given, s.deck, roomInput.value)?.let {
                selectedLevelId.value = it.levelId
            }
        }
    }

    fun onBackspace() {
        if (roomInput.value.isNotEmpty()) roomInput.value = roomInput.value.dropLast(1)
    }

    fun onClearRoom() {
        roomInput.value = ""
    }

    fun onLevelSelect(levelId: Long) {
        selectedLevelId.value = levelId
    }

    /** 快捷录入：校验通过即入库，房号清空、等级保留（连记场景） */
    fun saveRecord() {
        val s = state.value ?: return
        if (!isValidRoomInput(s.roomInput) || s.selectedLevelId == null) return
        val level = s.levelById[s.selectedLevelId] ?: return
        val record = PixieDustRecord(
            direction = if (s.tab == PixieTab.GIVEN) PixieDustDirection.GIVEN else PixieDustDirection.RECEIVED,
            deck = s.deck,
            room = s.roomInput,
            levelId = level.id,
            levelNameSnapshot = level.name,
            replied = false,
            note = "",
            timestamp = nowMs(),
        )
        viewModelScope.launch {
            dustRepository.save(record)
            savedEvent.value = record
            roomInput.value = ""
        }
    }

    fun onToggleKeypad() {
        keypadCollapsed.value = !keypadCollapsed.value
    }

    fun onToggleViewMode() {
        viewMode.value =
            if (viewMode.value == GivenViewMode.GROUPED) GivenViewMode.FLOW else GivenViewMode.GROUPED
    }

    fun onToggleReplied(id: Long, replied: Boolean) {
        val record = state.value?.received?.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { dustRepository.save(record.copy(replied = replied)) }
    }

    /** 编辑弹窗保存：保留原 timestamp（copy 自带） */
    fun saveEdited(record: PixieDustRecord) {
        viewModelScope.launch { dustRepository.save(record) }
    }

    fun deleteRecord(record: PixieDustRecord) {
        viewModelScope.launch { dustRepository.delete(record) }
    }

    fun saveLevel(level: PixieLevel) {
        viewModelScope.launch { levelRepository.save(level) }
    }

    fun deleteLevel(level: PixieLevel) {
        viewModelScope.launch { levelRepository.delete(level) }
    }

    /** 上移/下移等级显示顺序：与相邻项交换 sortOrder */
    fun onMoveLevel(level: PixieLevel, up: Boolean) {
        viewModelScope.launch {
            val list = state.value?.levels ?: return@launch
            val idx = list.indexOfFirst { it.id == level.id }
            val swapWith = if (up) idx - 1 else idx + 1
            if (idx < 0 || swapWith !in list.indices) return@launch
            levelRepository.save(list[idx].copy(sortOrder = swapWith))
            levelRepository.save(list[swapWith].copy(sortOrder = idx))
        }
    }

    /** 等级删除确认用：有多少条记录引用该等级 */
    suspend fun countByLevel(levelId: Long): Int = dustRepository.countByLevel(levelId)

    fun onToastShown() {
        savedEvent.value = null
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust
git commit -m "feat: pixie dust ViewModel with quick-entry state, stats derivation, level ops"
```

---

### Task 5: 路由 + 时间线入口 + 屏幕骨架（顶栏 tab/统计条/返回/齿轮）

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Task 4 的 `PixieDustViewModel` / `PixieDustUiState` / `PixieTab`；Task 1 的 `levelCountsOf`
- Produces:
  - `Routes.PIXIE_DUST = "pixiedust"`
  - `PixieDustScreen(state, onBack, onOpenLevelManager, content: @Composable (PixieDustUiState) -> Unit)`——骨架渲染顶栏+统计条，`content` 槽位由 Task 6/7 填充（本任务先放占位文本）
  - `TimelineScreen` 新增参数 `onOpenPixieDust: () -> Unit`
  - strings（本任务 + 后续任务全部新增，一次加齐避免反复改同一文件）：

```xml
    <!-- 仙尘交换 -->
    <string name="pixie_title">仙尘交换</string>
    <string name="pixie_tab_given">发出</string>
    <string name="pixie_tab_received">收到</string>
    <string name="pixie_manage_levels">等级管理</string>
    <string name="pixie_open">仙尘交换记录</string>
    <string name="pixie_back">返回</string>
    <string name="pixie_deck_label">楼层</string>
    <string name="pixie_room_hint">房间号</string>
    <string name="pixie_keypad_clear">清空</string>
    <string name="pixie_keypad_backspace">退格</string>
    <string name="pixie_save">记一笔</string>
    <string name="pixie_saved_format">已记录：%1$s · %2$s</string>
    <string name="pixie_last_level_format">上次：%1$s ×%2$d</string>
    <string name="pixie_collapse_keypad">收起键盘</string>
    <string name="pixie_expand_keypad">展开键盘</string>
    <string name="pixie_given_count_format">发出 %1$d 件</string>
    <string name="pixie_received_count_format">收到 %1$d 件</string>
    <string name="pixie_unreplied_count_format">待回礼 %1$d 件</string>
    <string name="pixie_view_grouped">按房间</string>
    <string name="pixie_view_flow">流水</string>
    <string name="pixie_group_total_format">共 %1$d 次</string>
    <string name="pixie_replied">已回礼</string>
    <string name="pixie_edit_title">编辑记录</string>
    <string name="pixie_delete">删除</string>
    <string name="pixie_delete_confirm">删除这条记录？</string>
    <string name="pixie_note_label">备注</string>
    <string name="pixie_empty_given">还没有发出记录，输入房间号记一笔</string>
    <string name="pixie_empty_received">还没有收到记录</string>
    <string name="pixie_add_level">新增等级</string>
    <string name="pixie_level_name_label">名称</string>
    <string name="pixie_level_rank_label">贵重程度</string>
    <string name="pixie_level_rank_hint">数字越大越贵重</string>
    <string name="pixie_level_delete_used_format">"%1$s" 被 %2$d 条记录使用，删除后这些记录将保留等级名称。仍要删除？</string>
    <string name="pixie_level_delete_free_format">删除等级"%1$s"？</string>
    <string name="pixie_level_last_one">至少保留一个等级</string>
    <string name="pixie_level_save">保存</string>
    <string name="pixie_level_move_up">上移</string>
    <string name="pixie_level_move_down">下移</string>
    <string name="pixie_cancel">取消</string>
```

- [ ] **Step 1: Add strings**

把上面整块加进 `app/src/main/res/values/strings.xml`（`<!-- 时间轴 -->` 之后的任意合适分组位置，文件末尾前）。

- [ ] **Step 2: Screen skeleton**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustScreen.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.levelCountsOf

/** 顶栏（返回/tab/齿轮）+ 分等级统计条。内容区由 content 槽位填充。 */
@Composable
fun PixieDustScreen(
    state: PixieDustUiState?,
    onBack: () -> Unit,
    onOpenLevelManager: () -> Unit,
    content: @Composable (PixieDustUiState) -> Unit,
) {
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        // ---- 顶栏：返回 / 双 tab / 等级管理齿轮 ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pixie_back))
            }
            TabRow(
                selectedTabIndex = if (state.tab == PixieTab.GIVEN) 0 else 1,
                modifier = Modifier.weight(1f),
            ) {
                Tab(
                    selected = state.tab == PixieTab.GIVEN,
                    onClick = { if (state.tab != PixieTab.GIVEN) onTabRequest() },
                    text = { Text(stringResource(R.string.pixie_tab_given)) },
                )
                Tab(
                    selected = state.tab == PixieTab.RECEIVED,
                    onClick = { if (state.tab != PixieTab.RECEIVED) onTabRequest() },
                    text = { Text(stringResource(R.string.pixie_tab_received)) },
                )
            }
            IconButton(onClick = onOpenLevelManager) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.pixie_manage_levels))
            }
        }

        // ---- 统计条 ----
        PixieStatBar(state)

        // ---- 内容（键盘 + 列表）----
        content(state)
    }
}

private var onTabRequest: () -> Unit = {}

/** 发出tab：发出总数+分等级；收到tab：收到总数+待回礼数（未回礼分等级，贵重在前） */
@Composable
private fun PixieStatBar(state: PixieDustUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.tab == PixieTab.GIVEN) {
            Text(
                stringResource(R.string.pixie_given_count_format, state.given.size),
                style = MaterialTheme.typography.titleSmall,
            )
            LevelCountChips(levelCountsOf(state.given, state.levels))
        } else {
            val unreplied = state.received.count { !it.replied }
            Text(
                stringResource(R.string.pixie_received_count_format, state.received.size) +
                    " · " +
                    stringResource(R.string.pixie_unreplied_count_format, unreplied),
                style = MaterialTheme.typography.titleSmall,
            )
            LevelCountChips(levelCountsOf(state.received.filter { !it.replied }, state.levels))
        }
    }
}

/** "● 3 ● 1" 分等级计数小条（圆点用等级色） */
@Composable
internal fun LevelCountChips(counts: List<com.cruise.assistant.domain.LevelCount>) {
    counts.forEach { c ->
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = androidx.compose.ui.graphics.Color(c.level.colorArgb),
                modifier = Modifier.size(8.dp),
            ) {}
            Spacer(Modifier.width(3.dp))
            Text(
                "${c.count}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

**注意**：上面 `onTabRequest` 的写法不对（不能这样接 lambda）——最终实现把 tab 切换也做成参数。最终签名为：

```kotlin
@Composable
fun PixieDustScreen(
    state: PixieDustUiState?,
    onBack: () -> Unit,
    onTabChange: (PixieTab) -> Unit,
    onOpenLevelManager: () -> Unit,
    content: @Composable (PixieDustUiState) -> Unit,
)
```

Tab 的 `onClick` 直接调 `onTabChange(PixieTab.GIVEN)` / `onTabChange(PixieTab.RECEIVED)`，删掉 `onTabRequest` 顶层变量。`content(state)` 里本任务先放占位：

```kotlin
content = { s ->
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.pixie_empty_given))
    }
}
```

- [ ] **Step 3: Route + entry points**

`CruiseNavHost.kt`：
1. `Routes` 添加 `const val PIXIE_DUST = "pixiedust"`
2. NavHost 里新增 composable（放在 `Routes.SETTINGS` 之前）：

```kotlin
        composable(Routes.PIXIE_DUST) {
            val vm: PixieDustViewModel = viewModel(
                factory = viewModelFactory {
                    PixieDustViewModel(
                        container.pixieDustRepository,
                        container.pixieLevelRepository,
                        container.voyageRepository,
                        container.settingsStore,
                    )
                },
            )
            val pixieState by vm.state.collectAsState()
            PixieDustScreen(
                state = pixieState,
                onBack = { navController.popBackStack() },
                onTabChange = vm::onTabChange,
                onOpenLevelManager = { /* Task 9 接等级管理弹层 */ },
                content = { s ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.pixie_empty_given))
                    }
                },
            )
        }
```

（import `com.cruise.assistant.ui.pixiedust.PixieDustScreen`、`PixieDustViewModel`、`PixieTab`。）

`TimelineScreen.kt`：
1. 参数列表 `onOpenDisplayRange` 之后加 `onOpenPixieDust: () -> Unit,`
2. 顶栏 `IconButton(onClick = onOpenDisplayRange)` **之前**加：

```kotlin
                IconButton(onClick = onOpenPixieDust) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(R.string.pixie_open),
                    )
                }
```

（import `androidx.compose.material.icons.outlined.AutoAwesome`。）

3. FAB 区改为（主 FAB 上方加仙尘小按钮；定稿日主 FAB 隐藏时仙尘入口仍在）：

```kotlin
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = onOpenPixieDust) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(R.string.pixie_open),
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (!state.isFinalized) {
                    FloatingActionButton(onClick = { onAddEvent(defaultStartMinute(state)) }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
                    }
                }
            }
        },
```

（import `androidx.compose.material3.SmallFloatingActionButton`、`androidx.compose.foundation.layout.Column` 已有/按需。）

4. `CruiseNavHost.kt` 里 `TimelineScreen(...)` 调用处补 `onOpenPixieDust = { navController.navigate(Routes.PIXIE_DUST) },`。

- [ ] **Step 4: Compile + manual verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

手动验证（`adb install -r app/build/outputs/apk/debug/app-debug.apk` 后）：
- 时间线顶栏出现仙尘图标，右下角主 FAB 上方出现小仙尘按钮，两个都进得去新页面
- 新页面顶栏 tab 可切换，统计条显示"发出 0 件"，返回键回时间线
- 旧数据升级路径：用 v1.7.5 的数据覆盖安装后启动不崩溃（AutoMigration 生效）

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui app/src/main/res/values/strings.xml
git commit -m "feat: pixie dust route, screen skeleton (tabs + stat bar), timeline entries"
```

---

### Task 6: 快捷录入——楼层下拉、数字键盘、等级按钮、记一笔、历史小字

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustKeypad.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`（content 槽位接入）

**Interfaces:**
- Consumes: Task 4 VM 方法（`onDeckChange`/`onDigit`/`onBackspace`/`onClearRoom`/`onLevelSelect`/`saveRecord`/`onToggleKeypad`）、`state.roomHistory`
- Produces:

```kotlin
@Composable
fun PixieDustKeypad(
    state: PixieDustUiState,
    onDeckChange: (Int) -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearRoom: () -> Unit,
    onLevelSelect: (Long) -> Unit,
    onSave: () -> Unit,
)
```

- [ ] **Step 1: Keypad component**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustKeypad.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.isValidRoomInput
import com.cruise.assistant.ui.common.iconFor

/** 快捷录入区：楼层下拉 + 大房号显示 + 数字键盘 + 等级按钮 + 记一笔 */
@Composable
fun PixieDustKeypad(
    state: PixieDustUiState,
    onDeckChange: (Int) -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearRoom: () -> Unit,
    onLevelSelect: (Long) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // ---- 楼层下拉（范围来自航次，记住上次） ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pixie_deck_label), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            DeckDropdown(
                deck = state.deck,
                deckRange = state.deckRange,
                onDeckChange = onDeckChange,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClearRoom) {
                Text(stringResource(R.string.pixie_keypad_clear))
            }
        }

        // ---- 房号大字显示 + 历史小字 ----
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = state.roomInput.ifEmpty { stringResource(R.string.pixie_room_hint) },
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = if (state.roomInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        state.roomHistory?.let { h ->
            Text(
                stringResource(R.string.pixie_last_level_format, h.levelName, h.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        // ---- 数字键盘：123/456/789/清空 0 退格 ----
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { d ->
                    KeypadKey(text = d, modifier = Modifier.weight(1f)) { onDigit(d[0]) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // 清空整格（文字），0，退格
            OutlinedButton(
                onClick = onClearRoom,
                modifier = Modifier.weight(1f).padding(2.dp).height(56.dp),
            ) { Text(stringResource(R.string.pixie_keypad_clear)) }
            KeypadKey(text = "0", modifier = Modifier.weight(1f)) { onDigit('0') }
            OutlinedButton(
                onClick = onBackspace,
                modifier = Modifier.weight(1f).padding(2.dp).height(56.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.pixie_keypad_backspace),
                )
            }
        }

        // ---- 等级按钮（选中 = 等级色填充） ----
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            state.levels.forEach { level ->
                LevelChip(
                    level = level,
                    selected = state.selectedLevelId == level.id,
                    onClick = { onLevelSelect(level.id) },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                )
            }
        }

        // ---- 记一笔（房号三位且选了等级才可用） ----
        Button(
            onClick = onSave,
            enabled = isValidRoomInput(state.roomInput) && state.selectedLevelId != null,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
        ) {
            Text(stringResource(R.string.pixie_save), fontSize = 16.sp)
        }
    }
}

@Composable
private fun DeckDropdown(deck: Int, deckRange: IntRange, onDeckChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Deck $deck")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            deckRange.forEach { d ->
                DropdownMenuItem(
                    text = { Text("Deck $d") },
                    onClick = {
                        expanded = false
                        onDeckChange(d)
                    },
                )
            }
        }
    }
}

@Composable
private fun KeypadKey(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.padding(2.dp).height(56.dp),
    ) {
        Text(text, fontSize = 22.sp)
    }
}

/** 等级选择大按钮：圆点图标 + 名称，选中时用等级色容器 */
@Composable
internal fun LevelChip(
    level: PixieLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconFor(level.iconKey),
                    contentDescription = null,
                    tint = if (selected) Color(level.colorArgb) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(level.name)
            }
        },
        colors = if (selected) {
            FilterChipDefaults.filterChipColors(
                containerColor = Color(level.colorArgb).copy(alpha = 0.25f),
                selectedContainerColor = Color(level.colorArgb).copy(alpha = 0.35f),
            )
        } else {
            FilterChipDefaults.filterChipColors()
        },
    )
}
```

- [ ] **Step 2: Wire into screen content**

`CruiseNavHost.kt` 里 `Routes.PIXIE_DUST` 的 composable——`content` 槽位改为（列表 Task 7 再接，这里键盘 + 收起按钮）：

```kotlin
                content = { s ->
                    Column(Modifier.fillMaxSize()) {
                        if (!s.keypadCollapsed) {
                            PixieDustKeypad(
                                state = s,
                                onDeckChange = vm::onDeckChange,
                                onDigit = vm::onDigit,
                                onBackspace = vm::onBackspace,
                                onClearRoom = vm::onClearRoom,
                                onLevelSelect = vm::onLevelSelect,
                                onSave = vm::saveRecord,
                            )
                        }
                        TextButton(
                            onClick = vm::onToggleKeypad,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                stringResource(
                                    if (s.keypadCollapsed) R.string.pixie_expand_keypad
                                    else R.string.pixie_collapse_keypad
                                )
                            )
                        }
                    }
                },
```

（import `PixieDustKeypad`。）

同时加 toast：composable 内、`PixieDustScreen(...)` 调用之后：

```kotlin
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(pixieState?.savedEvent) {
                pixieState?.savedEvent?.let { rec ->
                    val levelName = pixieState?.levelById?.get(rec.levelId)?.name ?: rec.levelNameSnapshot
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.pixie_saved_format, rec.fullRoom, levelName),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    vm.onToastShown()
                }
            }
```

（import `rec.fullRoom` 需 `com.cruise.assistant.domain.fullRoom`。）

- [ ] **Step 3: Compile + manual verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

手动验证：
- 楼层下拉只列航次范围（探险号 5..19），选择后持久（杀 App 重进仍是该层）
- 输 3 位房号 → "记一笔"亮起；输第 4 位被忽略；退格/清空可用
- 输入有历史的房号 → 等级自动预选上次用的，下方显示"上次：×N"
- 点"记一笔" → toast"已记录：11550 · 豪华"，房号清空等级保留，可连记
- 收到 tab 同款键盘可用；键盘收起/展开按钮生效

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: pixie dust quick-entry keypad (deck dropdown, digits, level chips, save)"
```

---

### Task 7: 列表——发出双视图（归组/流水）+ 收到列表（回礼勾选）

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustLists.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`（content 槽位接列表 + 展开单条编辑的入口状态）

**Interfaces:**
- Consumes: `state.grouped` / `state.given` / `state.received` / `state.viewMode` / `state.levelById`；VM `onToggleViewMode` / `onToggleReplied`
- Produces:

```kotlin
@Composable
fun GivenList(state: PixieDustUiState, onToggleViewMode: () -> Unit, onEditRecord: (PixieDustRecord) -> Unit)

@Composable
fun ReceivedList(state: PixieDustUiState, onEditRecord: (PixieDustRecord) -> Unit, onToggleReplied: (Long, Boolean) -> Unit)
```

- [ ] **Step 1: Lists component**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustLists.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("M月d日")

internal fun formatTs(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT)

/** 记录显示名：等级还在用等级名，被删用快照 */
internal fun levelNameOf(state: PixieDustUiState, record: PixieDustRecord): String =
    state.levelById[record.levelId]?.name ?: record.levelNameSnapshot

internal fun levelColorOf(state: PixieDustUiState, record: PixieDustRecord): Color =
    Color(state.levelById[record.levelId]?.colorArgb ?: 0xFF9E9E9E.toInt())

/** 发出列表：按房间归组（默认，可展开）/ 时间流水，右上切换 */
@Composable
fun GivenList(
    state: PixieDustUiState,
    onToggleViewMode: () -> Unit,
    onEditRecord: (PixieDustRecord) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(
                    if (state.viewMode == GivenViewMode.GROUPED) R.string.pixie_view_grouped
                    else R.string.pixie_view_flow
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleViewMode) {
                Text(
                    stringResource(
                        if (state.viewMode == GivenViewMode.GROUPED) R.string.pixie_view_flow
                        else R.string.pixie_view_grouped
                    )
                )
            }
        }
        if (state.given.isEmpty()) {
            EmptyHint(stringResource(R.string.pixie_empty_given))
        } else if (state.viewMode == GivenViewMode.GROUPED) {
            GroupedView(state, onEditRecord)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.given, key = { it.id }) { rec ->
                    RecordRow(
                        state = state,
                        record = rec,
                        onClick = { onEditRecord(rec) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun GroupedView(state: PixieDustUiState, onEditRecord: (PixieDustRecord) -> Unit) {
    var expandedKey by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.grouped, key = { "${it.deck}-${it.room}" }) { group ->
            val key = "${group.deck}-${group.room}"
            Column(Modifier.fillMaxWidth().clickable {
                expandedKey = if (expandedKey == key) null else key
            }) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${group.deck}${group.room}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        group.levelCounts.joinToString("  ") { "${it.level.name}×${it.count}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatTs(group.lastTimestamp) + " · " +
                            stringResource(R.string.pixie_group_total_format, group.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (expandedKey == key) {
                    state.given
                        .filter { it.deck == group.deck && it.room == group.room }
                        .forEach { rec ->
                            RecordRow(state = state, record = rec, onClick = { onEditRecord(rec) })
                        }
                }
                HorizontalDivider()
            }
        }
    }
}

/** 收到列表：未回礼在前贵重优先（state.received 已排好），右侧回礼勾选 */
@Composable
fun ReceivedList(
    state: PixieDustUiState,
    onEditRecord: (PixieDustRecord) -> Unit,
    onToggleReplied: (Long, Boolean) -> Unit,
) {
    if (state.received.isEmpty()) {
        EmptyHint(stringResource(R.string.pixie_empty_received))
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.received, key = { it.id }) { rec ->
            Row(
                Modifier.fillMaxWidth().clickable { onEditRecord(rec) }.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelDot(levelColorOf(state, rec))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${rec.deck}${rec.room}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            levelNameOf(state, rec) + " · " + formatTs(rec.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (rec.note.isNotEmpty()) {
                        Text(
                            rec.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Checkbox(
                    checked = rec.replied,
                    onCheckedChange = { onToggleReplied(rec.id, it) },
                )
            }
            HorizontalDivider()
        }
    }
}

/** 流水行：房号 + 等级 + 日期（+备注） */
@Composable
private fun RecordRow(state: PixieDustUiState, record: PixieDustRecord, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelDot(levelColorOf(state, record))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${record.deck}${record.room}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    levelNameOf(state, record) + " · " + formatTs(record.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (record.note.isNotEmpty()) {
                Text(
                    record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun LevelDot(color: Color) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 2: Wire into screen content**

`CruiseNavHost.kt` 的 PIXIE_DUST composable——加编辑目标状态并接入列表：

```kotlin
            var editTarget by remember { mutableStateOf<PixieDustRecord?>(null) }
```

`content` 槽位改为（键盘 + 列表，列表占剩余空间）：

```kotlin
                content = { s ->
                    Column(Modifier.fillMaxSize()) {
                        if (!s.keypadCollapsed) {
                            PixieDustKeypad(
                                state = s,
                                onDeckChange = vm::onDeckChange,
                                onDigit = vm::onDigit,
                                onBackspace = vm::onBackspace,
                                onClearRoom = vm::onClearRoom,
                                onLevelSelect = vm::onLevelSelect,
                                onSave = vm::saveRecord,
                            )
                        }
                        TextButton(
                            onClick = vm::onToggleKeypad,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                stringResource(
                                    if (s.keypadCollapsed) R.string.pixie_expand_keypad
                                    else R.string.pixie_collapse_keypad
                                )
                            )
                        }
                        if (s.tab == PixieTab.GIVEN) {
                            GivenList(
                                state = s,
                                onToggleViewMode = vm::onToggleViewMode,
                                onEditRecord = { editTarget = it },
                            )
                        } else {
                            ReceivedList(
                                state = s,
                                onEditRecord = { editTarget = it },
                                onToggleReplied = vm::onToggleReplied,
                            )
                        }
                    }
                },
```

（import `GivenList`、`ReceivedList`、`PixieDustRecord`、`remember`/`mutableStateOf`/`getValue`/`setValue` 已在文件里。`editTarget` 本任务暂无消费者——Task 8 的编辑弹窗接。）

- [ ] **Step 3: Compile + manual verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

手动验证：
- 记几条发出后：默认按房间归组，点组展开看到单条；右上"流水"切换成时间流
- 归组排序按最近发出在前；组内"普通×2 豪华×1"
- 收到 tab：记几条不同等级 → 未回礼的贵重在前；勾"已回礼"后沉底
- 空态文案正确显示

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: pixie dust lists (grouped/flow given views, received with reply checkbox)"
```

---

### Task 8: 编辑弹窗（改楼层/房号/等级/备注/已回礼 + 删除确认）

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustEditDialog.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`（`editTarget` 接弹窗）

**Interfaces:**
- Consumes: `editTarget`（Task 7 置位）、VM `saveEdited` / `deleteRecord`、`state.levels` / `state.deckRange`
- Produces:

```kotlin
@Composable
fun PixieDustEditDialog(
    initial: PixieDustRecord,
    isReceived: Boolean,
    deckRange: IntRange,
    levels: List<PixieLevel>,
    onSave: (PixieDustRecord) -> Unit,
    onDelete: (PixieDustRecord) -> Unit,
    onDismiss: () -> Unit,
)
```

- [ ] **Step 1: Edit dialog**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieDustEditDialog.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.isValidRoomInput

/** 编辑一条记录：楼层/房号/等级/备注（+收到的已回礼），底部删除。时间戳不可改。 */
@Composable
fun PixieDustEditDialog(
    initial: PixieDustRecord,
    isReceived: Boolean,
    deckRange: IntRange,
    levels: List<PixieLevel>,
    onSave: (PixieDustRecord) -> Unit,
    onDelete: (PixieDustRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    var deck by remember { mutableStateOf(initial.deck) }
    var room by remember { mutableStateOf(initial.room) }
    var levelId by remember { mutableStateOf(initial.levelId) }
    var replied by remember { mutableStateOf(initial.replied) }
    var note by remember { mutableStateOf(initial.note) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun build(): PixieDustRecord = initial.copy(
        deck = deck,
        room = room,
        levelId = levelId,
        levelNameSnapshot = levels.firstOrNull { it.id == levelId }?.name ?: initial.levelNameSnapshot,
        replied = replied,
        note = note,
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.pixie_delete)) },
            text = { Text(stringResource(R.string.pixie_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { onDelete(initial) }) {
                    Text(stringResource(R.string.pixie_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_edit_title)) },
        text = {
            Column {
                // 楼层
                var deckExpanded by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pixie_deck_label))
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { deckExpanded = true }) { Text("Deck $deck") }
                    DropdownMenu(expanded = deckExpanded, onDismissRequest = { deckExpanded = false }) {
                        deckRange.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("Deck $d") },
                                onClick = { deck = d; deckExpanded = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 房号（仅数字，最多三位）
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.pixie_room_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = !isValidRoomInput(room),
                )
                Spacer(Modifier.height(8.dp))
                // 等级
                Row(verticalAlignment = Alignment.CenterVertically) {
                    levels.forEach { level ->
                        LevelChip(
                            level = level,
                            selected = levelId == level.id,
                            onClick = { levelId = level.id },
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
                if (isReceived) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = replied, onCheckedChange = { replied = it })
                        Text(stringResource(R.string.pixie_replied))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.pixie_note_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(build()) },
                enabled = isValidRoomInput(room) && levels.any { it.id == levelId },
            ) { Text(stringResource(R.string.pixie_level_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.pixie_delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            }
        },
    )
}
```

- [ ] **Step 2: Wire editTarget**

`CruiseNavHost.kt` PIXIE_DUST composable 里，`content` 之后（toast LaunchedEffect 旁）：

```kotlin
            editTarget?.let { target ->
                PixieDustEditDialog(
                    initial = target,
                    isReceived = target.direction == PixieDustDirection.RECEIVED,
                    deckRange = pixieState?.deckRange ?: 1..20,
                    levels = pixieState?.levels ?: emptyList(),
                    onSave = {
                        vm.saveEdited(it)
                        editTarget = null
                    },
                    onDelete = {
                        vm.deleteRecord(it)
                        editTarget = null
                    },
                    onDismiss = { editTarget = null },
                )
            }
```

（import `PixieDustEditDialog`、`com.cruise.assistant.domain.PixieDustDirection`。）

- [ ] **Step 3: Compile + manual verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

手动验证：
- 点任意列表项弹编辑窗，改等级/补备注/改楼层保存后列表即时更新
- 收到的记录可勾"已回礼"；房号清成 <3 位时保存禁用
- 删除 → 确认弹窗 → 确认后消失；取消保留
- 编辑不改时间戳（列表日期不变）

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: pixie dust edit dialog (deck/room/level/note/replied, delete confirm)"
```

---

### Task 9: 等级管理弹层（增删改名称/颜色/图标/贵重度/排序）

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieLevelSheet.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`（齿轮接弹层）

**Interfaces:**
- Consumes: VM `saveLevel` / `deleteLevel` / `onMoveLevel` / `countByLevel`、`state.levels`
- Produces:

```kotlin
@Composable
fun PixieLevelSheet(
    levels: List<PixieLevel>,
    onSave: (PixieLevel) -> Unit,
    onDelete: (PixieLevel) -> Unit,
    onMove: (PixieLevel, Boolean) -> Unit,   // (level, up)
    countByLevel: suspend (Long) -> Int,
    onDismiss: () -> Unit,
)
```

- [ ] **Step 1: Level sheet**

`app/src/main/java/com/cruise/assistant/ui/pixiedust/PixieLevelSheet.kt`:

```kotlin
package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.data.CATEGORY_PALETTE
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.ui.common.iconFor
import kotlinx.coroutines.launch

/** 等级管理：列表（上移/下移/点开编辑/删除）+ 新增 */
private val LEVEL_ICON_KEYS = listOf("gift", "sparkle", "diamond", "star", "heart")

@Composable
fun PixieLevelSheet(
    levels: List<PixieLevel>,
    onSave: (PixieLevel) -> Unit,
    onDelete: (PixieLevel) -> Unit,
    onMove: (PixieLevel, Boolean) -> Unit,
    countByLevel: suspend (Long) -> Int,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf<PixieLevel?>(null) }
    var confirmDelete by remember { mutableStateOf<PixieLevel?>(null) }
    val scope = rememberCoroutineScope()

    // ---- 删除确认（区分有无引用） ----
    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.pixie_delete)) },
            text = {
                val used = countByLevel(target.id)
                androidx.compose.runtime.compose {
                    Text(
                        if (used > 0) stringResource(R.string.pixie_level_delete_used_format, target.name, used)
                        else stringResource(R.string.pixie_level_delete_free_format, target.name)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    confirmDelete = null
                }) { Text(stringResource(R.string.pixie_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            },
        )
    }

    // ---- 编辑器 ----
    editing?.let { target ->
        LevelEditorDialog(
            initial = target,
            isNew = target.id == 0L,
            onSave = {
                onSave(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
        return
    }

    // ---- 列表 ----
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_manage_levels)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                levels.forEachIndexed { index, level ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = CircleShape, color = Color(level.colorArgb), modifier = Modifier.size(12.dp)) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            level.name,
                            modifier = Modifier.weight(1f).clickable { editing = level.copy() },
                        )
                        Text(
                            stringResource(R.string.pixie_level_rank_label) + " ${level.rank}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { onMove(level, true) }, enabled = index > 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.pixie_level_move_up))
                        }
                        IconButton(onClick = { onMove(level, false) }, enabled = index < levels.size - 1) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.pixie_level_move_down))
                        }
                        // 只剩一个等级时不许删
                        IconButton(onClick = { confirmDelete = level }, enabled = levels.size > 1) {
                            Text("✕", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                TextButton(onClick = {
                    editing = PixieLevel(
                        name = "",
                        colorArgb = CATEGORY_PALETTE.first(),
                        iconKey = LEVEL_ICON_KEYS.first(),
                        rank = (levels.maxOfOrNull { it.rank } ?: 0) + 1,
                        sortOrder = levels.size,
                    )
                }) { Text(stringResource(R.string.pixie_add_level)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pixie_back)) }
        },
    )
}

/** 单个等级编辑：名称/颜色/图标/贵重度(rank 1..9 步进) */
@Composable
private fun LevelEditorDialog(
    initial: PixieLevel,
    isNew: Boolean,
    onSave: (PixieLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var colorArgb by remember { mutableStateOf(initial.colorArgb) }
    var iconKey by remember { mutableStateOf(initial.iconKey) }
    var rank by remember { mutableStateOf(initial.rank) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isNew) stringResource(R.string.pixie_add_level)
                else stringResource(R.string.pixie_manage_levels)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.pixie_level_name_label)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                // 颜色
                Text(stringResource(R.string.pixie_level_name_label).let { "" } + "", style = MaterialTheme.typography.labelMedium) // 占位可删
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_PALETTE.forEach { c ->
                        val selected = c == colorArgb
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(c), CircleShape)
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier.clickable { colorArgb = c }
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 图标
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LEVEL_ICON_KEYS.forEach { key ->
                        val selected = key == iconKey
                        IconButton(onClick = { iconKey = key }) {
                            Icon(
                                iconFor(key),
                                contentDescription = key,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 贵重度步进
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pixie_level_rank_label))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (rank > 1) rank-- }) { Text("−") }
                    Text("$rank", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (rank < 9) rank++ }) { Text("＋") }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.pixie_level_rank_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(initial.copy(name = name.trim(), colorArgb = colorArgb, iconKey = iconKey, rank = rank)) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.pixie_level_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pixie_cancel)) }
        },
    )
}
```

**注意两处要清理**：
1. 删除确认里 `androidx.compose.runtime.compose { }` 不是真实 API——`text` 直接写：

```kotlin
            text = {
                val used = countByLevel(target.id)
                Text(
                    if (used > 0) stringResource(R.string.pixie_level_delete_used_format, target.name, used)
                    else stringResource(R.string.pixie_level_delete_free_format, target.name)
                )
            },
```

2. 颜色区上方那行"占位可删"的 Text 整行删掉。

- [ ] **Step 2: Wire gear**

`CruiseNavHost.kt` PIXIE_DUST composable 里加 `var showLevelSheet by remember { mutableStateOf(false) }`，齿轮回调改为 `{ showLevelSheet = true }`，composable 末尾：

```kotlin
            if (showLevelSheet) {
                PixieLevelSheet(
                    levels = pixieState?.levels ?: emptyList(),
                    onSave = vm::saveLevel,
                    onDelete = vm::deleteLevel,
                    onMove = vm::onMoveLevel,
                    countByLevel = vm::countByLevel,
                    onDismiss = { showLevelSheet = false },
                )
            }
```

（import `PixieLevelSheet`。）

- [ ] **Step 3: Compile + manual verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

手动验证：
- 齿轮 → 等级列表（普通/精致/豪华），点名称进编辑：改名/换色/换图标/调贵重度保存生效（键盘区等级按钮同步变）
- 新增等级出现在键盘区；上移/下移调整键盘显示顺序
- 删除被引用的等级 → 确认文案带引用数；删除后历史记录显示原名称（快照）
- 只剩一个等级时删除按钮禁用

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/pixiedust app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: pixie level management sheet (name/color/icon/rank/sort, delete confirm)"
```

---

### Task 10: 版本号 1.8.0 + 全量回归 + 发布 APK

**Files:**
- Modify: `app/build.gradle.kts:23-24`

**Interfaces:**
- Consumes: 全部前序任务
- Produces: `versionCode = 14`、`versionName = "1.8.0"`、`app/release/CruiseAssistant-v1.8.0.apk`

- [ ] **Step 1: Bump version**

`app/build.gradle.kts`:

```kotlin
        versionCode = 14
        versionName = "1.8.0"
```

- [ ] **Step 2: Full unit test + build**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全部测试 PASS（含 Task 1 的 8 个新测试）

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL，产出 `app/build/outputs/apk/release/CruiseAssistant-v1.8.0.apk`

- [ ] **Step 3: Manual regression（装机过一遍主流程）**

安装 release APK 到设备，验证：
1. 旧版升级安装（v1.7.5 数据在）→ 启动不崩溃，旧航次/事件完好，等级自动播种出默认三级
2. 时间线两入口进仙尘页；快捷录入连记 3 条；预选等级与历史小字
3. 收到记录勾回礼沉底；编辑弹窗改记录；删除确认
4. 等级管理增删改；键盘区同步
5. 设置页"重建航次"→ 仙尘记录清空、等级回到默认三级
6. 返回时间线一切如旧

- [ ] **Step 4: Copy APK + commit**

```bash
cp app/build/outputs/apk/release/CruiseAssistant-v1.8.0.apk app/release/
git add app/build.gradle.kts
git commit -m "release: v1.8.0 with pixie dust exchange tracker"
```

（APK 文件按仓库惯例不提交——`app/release/*.apk` 一直是未跟踪状态。）

---

## Self-Review 记录

- **Spec 覆盖**：入口两处（T5）、顶 tab+齿轮（T5/T9）、分等级统计条（T5）、楼层下拉+范围+记忆（T3/T6）、数字键盘+超3位忽略+<3位禁用（T4/T6）、预选上次等级+历史小字（T4/T6）、记一笔即存+toast+连记（T4/T6）、收到排序+勾选（T1/T7）、发出双视图（T7）、编辑+删除确认（T8）、等级管理含快照说明（T9）、时间戳自动+列表日期（T4/T7）、重置航程清理（T3）、schema v5（T2）、版本升级（T10）——全部有对应任务。
- **占位符**：T5/T9 代码块内有两处标注"注意"的实现修正（`onTabRequest` 顶层变量、`compose {}` 假 API、占位 Text）——执行者按注记修正后即为最终代码，不留 TBD。
- **类型一致性**：`PixieDustUiState` 字段与 VM/UI 引用一致；`fullRoom` 是 `PixieDustRecord` 扩展属性（T1 定义，T6 toast 使用需 import `com.cruise.assistant.domain.fullRoom`）；`LevelChip` 在 T6 定义为 `internal` 供 T8 复用；`iconFor` 已存在于 `ui/common/CategoryIcons.kt`。
