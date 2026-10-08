package com.cruise.assistant.ui.archive

import com.cruise.assistant.data.db.dao.VoyageArchiveDao
import com.cruise.assistant.data.db.entity.VoyageArchiveEntity
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import com.cruise.assistant.data.repo.VoyageArchiveRepository
import com.cruise.assistant.domain.ArchiveSnapshot
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.Voyage
import com.cruise.assistant.domain.WishlistItem
import com.cruise.assistant.domain.serializeArchiveSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.LocalTime

/**
 * 存档 UI 测试共享夹具：Fake 存档 DAO + 一份固定快照。
 * 详情页只读回放不查业务表，只需 VoyageArchiveDao 即可驱动 ViewModel
 * （快照直接序列化入档，不走 createArchive 的六表采集路径）。
 */
object ArchiveViewModelFixtures {

    /** 固定航次：联程 3+4 礼宾，2026-10-06 登船 / 10-12 下船 */
    val BOARD_DATE: LocalDate = LocalDate.of(2026, 10, 6)
    val DISEMBARK_DATE: LocalDate = LocalDate.of(2026, 10, 12)

    val voyage = Voyage(
        id = 1,
        shipName = "Disney Adventure",
        boardDate = BOARD_DATE,
        boardTime = LocalTime.of(12, 0),
        disembarkDate = DISEMBARK_DATE,
        disembarkTime = LocalTime.of(7, 0),
        maxDeck = 19,
        minDeck = 5,
        isConcierge = true,
        legs = listOf(3, 4),
    )

    val categories = listOf(
        Category(id = 1, name = "重要", colorArgb = 0xFFFF0000.toInt(), iconKey = "important"),
        Category(id = 2, name = "演出", colorArgb = 0xFF00FF00.toInt(), iconKey = "show"),
    )

    /**
     * 登船日两件事：12:00 活动 + 15:00 非活动（登船日定稿，用于验证 inactive 过滤）；
     * 第二天一件 show 事件（未定稿）。
     */
    val events = listOf(
        CruiseEvent(
            id = 1, date = BOARD_DATE, startTime = LocalTime.of(12, 0), durationMinutes = 60,
            categoryId = 1, title = "登船", deck = null, zone = null, placeName = null,
            lane = 0, isActive = true,
        ),
        CruiseEvent(
            id = 2, date = BOARD_DATE, startTime = LocalTime.of(15, 0), durationMinutes = 30,
            categoryId = 1, title = "旧安排（已停用）", deck = 5, zone = ShipZone.MID, placeName = "泳池",
            lane = 1, isActive = false,
        ),
        CruiseEvent(
            id = 3, date = BOARD_DATE.plusDays(1), startTime = LocalTime.of(19, 0), durationMinutes = 50,
            categoryId = 2, title = "狮子王", deck = 6, zone = ShipZone.STERN, placeName = "剧场",
            lane = 1, isActive = true, waitMinutes = 30,
        ),
    )

    val dayPlans = listOf(
        DayPlan(date = BOARD_DATE, finalized = true, finalizedAt = 123L),
        DayPlan(date = BOARD_DATE.plusDays(1), finalized = false, finalizedAt = null),
    )

    val pixieLevels = listOf(
        PixieLevel(id = 1, name = "贵重", colorArgb = 0xFF111111.toInt(), iconKey = "gift", rank = 3),
        PixieLevel(id = 2, name = "普通", colorArgb = 0xFF222222.toInt(), iconKey = "gift", rank = 1),
    )

    /** 同一房间（Deck 10 · 088）发一收一，收到未回礼 */
    val pixieDust = listOf(
        PixieDustRecord(
            id = 1, direction = PixieDustDirection.GIVEN, deck = 10, room = "088", levelId = 1,
            levelNameSnapshot = "贵重", replied = false, note = "", timestamp = 111,
        ),
        PixieDustRecord(
            id = 2, direction = PixieDustDirection.RECEIVED, deck = 10, room = "088", levelId = 2,
            levelNameSnapshot = "普通", replied = false, note = "记得回", timestamp = 222,
        ),
    )

    /** 第二条 categoryId=999 故意无对应类别（渲染兜底灰色） */
    val wishlist = listOf(
        WishlistItem(
            id = 1, title = "看狮子王", categoryId = 2, deck = 6, zone = ShipZone.STERN,
            placeName = "剧场", note = "提前占座", isCompleted = true, createdAt = 333,
        ),
        WishlistItem(
            id = 2, title = "甲板跑步", categoryId = 999, deck = null, zone = null,
            placeName = null, note = null, isCompleted = false, createdAt = 444,
        ),
    )

    fun buildSnapshot(): ArchiveSnapshot = ArchiveSnapshot(
        voyage = voyage,
        categories = categories,
        events = events,
        dayPlans = dayPlans,
        pixieLevels = pixieLevels,
        pixieDust = pixieDust,
        wishlist = wishlist,
    )

    /** 造一档：直接把快照序列化入 Fake DAO（createdAt=1_000 供概要页展示） */
    fun insertArchive(dao: FakeVoyageArchiveDao, id: Long = 1L): Long {
        val entity = VoyageArchiveEntity(
            id = id,
            shipName = voyage.shipName,
            boardDate = voyage.boardDate,
            disembarkDate = voyage.disembarkDate,
            nightsSummary = "3+4夜 · 共7晚",
            createdAt = 1_000L,
            payloadJson = serializeArchiveSnapshot(buildSnapshot()),
        )
        dao.rows.add(entity)
        dao.sync()
        return id
    }

    fun newRepository(dao: FakeVoyageArchiveDao): VoyageArchiveRepository =
        VoyageArchiveRepository(
            voyageDao = EmptyVoyageDao(),
            eventDao = EmptyEventDao(),
            categoryDao = EmptyCategoryDao(),
            pixieDustDao = EmptyPixieDustDao(),
            pixieLevelDao = EmptyPixieLevelDao(),
            wishlistDao = EmptyWishlistDao(),
            voyageArchiveDao = dao,
        )

    // ---- 只读回放用不到业务表采集，以下 DAO 全部空实现（接口必须全量实现） ----

    class EmptyVoyageDao : com.cruise.assistant.data.db.dao.VoyageDao {
        override fun observeVoyage(): Flow<com.cruise.assistant.data.db.entity.VoyageEntity?> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getVoyage(): com.cruise.assistant.data.db.entity.VoyageEntity? = null
        override suspend fun upsertVoyage(voyage: com.cruise.assistant.data.db.entity.VoyageEntity) {}
        override suspend fun clearVoyage() {}
        override fun observeDayPlans(): Flow<List<com.cruise.assistant.data.db.entity.DayPlanEntity>> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getDayPlansOnce(): List<com.cruise.assistant.data.db.entity.DayPlanEntity> =
            emptyList()
        override fun observeDayPlan(date: LocalDate): Flow<com.cruise.assistant.data.db.entity.DayPlanEntity?> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun upsertDayPlans(plans: List<com.cruise.assistant.data.db.entity.DayPlanEntity>) {}
        override suspend fun upsertDayPlan(plan: com.cruise.assistant.data.db.entity.DayPlanEntity) {}
        override suspend fun deleteDayPlan(plan: com.cruise.assistant.data.db.entity.DayPlanEntity) {}
        override suspend fun clearDayPlans() {}
    }

    class EmptyEventDao : com.cruise.assistant.data.db.dao.EventDao {
        override fun observeByDate(date: LocalDate): Flow<List<com.cruise.assistant.data.db.entity.EventEntity>> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getByDate(date: LocalDate): List<com.cruise.assistant.data.db.entity.EventEntity> =
            emptyList()
        override suspend fun getAll(): List<com.cruise.assistant.data.db.entity.EventEntity> = emptyList()
        override suspend fun getById(id: Long): com.cruise.assistant.data.db.entity.EventEntity? = null
        override suspend fun getByGroup(groupId: String): List<com.cruise.assistant.data.db.entity.EventEntity> =
            emptyList()
        override suspend fun getByPresetKey(key: String): List<com.cruise.assistant.data.db.entity.EventEntity> =
            emptyList()
        override fun observeDatesWithEvents(): Flow<List<LocalDate>> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun insert(event: com.cruise.assistant.data.db.entity.EventEntity): Long = 1L
        override suspend fun insertAll(events: List<com.cruise.assistant.data.db.entity.EventEntity>): List<Long> =
            emptyList()
        override suspend fun update(event: com.cruise.assistant.data.db.entity.EventEntity) {}
        override suspend fun updateAll(events: List<com.cruise.assistant.data.db.entity.EventEntity>) {}
        override suspend fun delete(event: com.cruise.assistant.data.db.entity.EventEntity) {}
        override suspend fun deleteGroup(groupId: String) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {}
        override suspend fun setActive(id: Long, active: Boolean) {}
        override suspend fun countByCategory(categoryId: Long): Int = 0
        override suspend fun clearAll() {}
    }

    class EmptyCategoryDao : com.cruise.assistant.data.db.dao.CategoryDao {
        override fun observeAll(): Flow<List<com.cruise.assistant.data.db.entity.CategoryEntity>> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun observeAllOnce(): List<com.cruise.assistant.data.db.entity.CategoryEntity> =
            emptyList()
        override fun observeActive(): Flow<List<com.cruise.assistant.data.db.entity.CategoryEntity>> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getById(id: Long): com.cruise.assistant.data.db.entity.CategoryEntity? = null
        override suspend fun count(): Int = 0
        override suspend fun insert(category: com.cruise.assistant.data.db.entity.CategoryEntity): Long = 1L
        override suspend fun insertAll(categories: List<com.cruise.assistant.data.db.entity.CategoryEntity>) {}
        override suspend fun update(category: com.cruise.assistant.data.db.entity.CategoryEntity) {}
        override suspend fun delete(category: com.cruise.assistant.data.db.entity.CategoryEntity) {}
        override suspend fun clearAll() {}
    }

    class EmptyPixieDustDao : com.cruise.assistant.data.db.dao.PixieDustDao {
        override fun observeAll(): Flow<List<com.cruise.assistant.data.db.entity.PixieDustEntity>> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getById(id: Long): com.cruise.assistant.data.db.entity.PixieDustEntity? = null
        override suspend fun getAll(): List<com.cruise.assistant.data.db.entity.PixieDustEntity> = emptyList()
        override suspend fun insert(record: com.cruise.assistant.data.db.entity.PixieDustEntity): Long = 1L
        override suspend fun update(record: com.cruise.assistant.data.db.entity.PixieDustEntity) {}
        override suspend fun delete(record: com.cruise.assistant.data.db.entity.PixieDustEntity) {}
        override suspend fun countByLevel(levelId: Long): Int = 0
        override suspend fun clearAll() {}
    }

    class EmptyPixieLevelDao : com.cruise.assistant.data.db.dao.PixieLevelDao {
        override fun observeAll(): Flow<List<com.cruise.assistant.data.db.entity.PixieLevelEntity>> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun observeAllOnce(): List<com.cruise.assistant.data.db.entity.PixieLevelEntity> =
            emptyList()
        override suspend fun getById(id: Long): com.cruise.assistant.data.db.entity.PixieLevelEntity? = null
        override suspend fun insert(level: com.cruise.assistant.data.db.entity.PixieLevelEntity): Long = 1L
        override suspend fun insertAll(levels: List<com.cruise.assistant.data.db.entity.PixieLevelEntity>) {}
        override suspend fun update(level: com.cruise.assistant.data.db.entity.PixieLevelEntity) {}
        override suspend fun delete(level: com.cruise.assistant.data.db.entity.PixieLevelEntity) {}
        override suspend fun count(): Int = 0
        override suspend fun clearAll() {}
    }

    class EmptyWishlistDao : com.cruise.assistant.data.db.dao.WishlistDao {
        override fun observeAll(): Flow<List<com.cruise.assistant.data.db.entity.WishlistItemEntity>> =
            kotlinx.coroutines.flow.emptyFlow()
        override suspend fun getAll(): List<com.cruise.assistant.data.db.entity.WishlistItemEntity> =
            emptyList()
        override suspend fun getById(id: Long): com.cruise.assistant.data.db.entity.WishlistItemEntity? = null
        override suspend fun insert(item: com.cruise.assistant.data.db.entity.WishlistItemEntity): Long = 1L
        override suspend fun update(item: com.cruise.assistant.data.db.entity.WishlistItemEntity) {}
        override suspend fun delete(item: com.cruise.assistant.data.db.entity.WishlistItemEntity) {}
        override suspend fun toggleCompleted(id: Long, completed: Boolean) {}
        override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long) {}
    }
}

/**
 * Fake 存档 DAO：observeAll 用 MutableStateFlow 持续同步（VM 构造即订阅，后续
 * insert/delete 必须能推送给列表页）。
 */
class FakeVoyageArchiveDao : VoyageArchiveDao {
    val rows = mutableListOf<VoyageArchiveEntity>()
    private val all = MutableStateFlow<List<VoyageArchiveMeta>>(emptyList())

    fun sync() {
        all.value = rows.map { e ->
            VoyageArchiveMeta(
                id = e.id, shipName = e.shipName, boardDate = e.boardDate,
                disembarkDate = e.disembarkDate, nightsSummary = e.nightsSummary,
                createdAt = e.createdAt,
            )
        }
    }

    override fun observeAll(): Flow<List<VoyageArchiveMeta>> = all
    override suspend fun getById(id: Long): VoyageArchiveEntity? = rows.find { it.id == id }
    override suspend fun insert(entity: VoyageArchiveEntity): Long {
        rows.add(entity)
        sync()
        return entity.id
    }
    override suspend fun deleteById(id: Long) {
        rows.removeAll { it.id == id }
        sync()
    }
}
