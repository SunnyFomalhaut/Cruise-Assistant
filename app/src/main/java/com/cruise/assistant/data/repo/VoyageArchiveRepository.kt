package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.dao.PixieLevelDao
import com.cruise.assistant.data.db.dao.VoyageArchiveDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.dao.WishlistDao
import com.cruise.assistant.data.db.entity.VoyageArchiveEntity
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.domain.ArchiveSnapshot
import com.cruise.assistant.domain.Voyage
import com.cruise.assistant.domain.parseArchiveSnapshot
import com.cruise.assistant.domain.serializeArchiveSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * 存档列表展示用冗余文本：联程如 "3+4夜 · 共7晚"，单程如 "4夜"
 * （dayCount 含登船日与下船日，晚数 = 天数 - 1 = legs 之和）
 */
internal fun nightsSummaryOf(voyage: Voyage): String =
    if (voyage.legs.isEmpty()) {
        "${voyage.dayCount - 1}夜"
    } else {
        "${voyage.legs.joinToString("+")}夜 · 共${voyage.legs.sum()}晚"
    }

/**
 * 航次存档仓库：航次结束时把全部业务表打包成 ArchiveSnapshot 精确快照存入 voyage_archive，
 * 作为"重建航次清空旧数据"后唯一的回忆载体。
 */
class VoyageArchiveRepository(
    private val voyageDao: VoyageDao,
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val pixieDustDao: PixieDustDao,
    private val pixieLevelDao: PixieLevelDao,
    private val wishlistDao: WishlistDao,
    private val voyageArchiveDao: VoyageArchiveDao,
    // 可注入的事务执行器：生产环境由 AppContainer 传 db.withTransaction，
    // 单元测试 Fake DAO 没有 Room 数据库，默认直跑（不包事务）语义正确。
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
) {
    /** 存档列表（轻量元信息，新的在前） */
    fun observeAll(): Flow<List<VoyageArchiveMeta>> = voyageArchiveDao.observeAll()

    /** 读单份完整快照；存档不存在或 payload 损坏（解析失败）返回 null */
    suspend fun getSnapshot(id: Long): ArchiveSnapshot? =
        voyageArchiveDao.getById(id)?.let { parseArchiveSnapshot(it.payloadJson) }

    /**
     * 创建当前航次的存档，返回新存档 id。
     * 无航次（尚未创建/已重建清空）返回 null，交由 UI 提示"没有可存档的航次"。
     * 读全部业务表 + 写一行包进事务：快照不会读到重建航次写到一半的中间态。
     */
    suspend fun createArchive(nowMs: Long = System.currentTimeMillis()): Long? {
        var newId: Long? = null
        runInTransaction {
            val voyage = voyageDao.getVoyage()?.toDomain()
            if (voyage != null) {
                val snapshot = ArchiveSnapshot(
                    voyage = voyage,
                    categories = categoryDao.observeAllOnce().map { it.toDomain() },
                    events = eventDao.getAll().map { it.toDomain() },
                    dayPlans = voyageDao.getDayPlansOnce().map { it.toDomain() },
                    pixieLevels = pixieLevelDao.observeAllOnce().map { it.toDomain() },
                    pixieDust = pixieDustDao.getAll().map { it.toDomain() },
                    wishlist = wishlistDao.getAll().map { it.toDomain() },
                )
                newId = voyageArchiveDao.insert(
                    VoyageArchiveEntity(
                        shipName = voyage.shipName,
                        boardDate = voyage.boardDate,
                        disembarkDate = voyage.disembarkDate,
                        nightsSummary = nightsSummaryOf(voyage),
                        createdAt = nowMs,
                        payloadJson = serializeArchiveSnapshot(snapshot),
                    ),
                )
            }
        }
        return newId
    }

    /** 删除一份存档（快照随行删除，不可恢复） */
    suspend fun deleteArchive(id: Long) {
        voyageArchiveDao.deleteById(id)
    }
}
