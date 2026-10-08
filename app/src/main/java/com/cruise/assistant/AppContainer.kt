package com.cruise.assistant

import android.content.Context
import androidx.room.withTransaction
import com.cruise.assistant.data.db.CruiseDatabase
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.LocationPresetRepository
import com.cruise.assistant.data.repo.PixieDustRepository
import com.cruise.assistant.data.repo.PixieLevelRepository
import com.cruise.assistant.data.repo.VoyageArchiveRepository
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.data.repo.WishlistRepository
import com.cruise.assistant.reminder.ReminderScheduler

/** 手写依赖容器，不引入 DI 框架 */
class AppContainer(context: Context) {

    private val db = CruiseDatabase.get(context)

    val settingsStore = SettingsStore(context)

    val voyageRepository = VoyageRepository(
        voyageDao = db.voyageDao(),
        eventDao = db.eventDao(),
        categoryDao = db.categoryDao(),
        locationPresetDao = db.locationPresetDao(),
        pixieDustDao = db.pixieDustDao(),
        pixieLevelDao = db.pixieLevelDao(),
        wishlistDao = db.wishlistDao(),
        // 仓库内多步写入包进真实 Room 事务（Room 事务不能硬编码在仓库里，
        // 否则单测的 Fake DAO 场景无法构造）。
        runInTransaction = { block -> db.withTransaction(block) },
    )

    val eventRepository = EventRepository(
        eventDao = db.eventDao(),
        voyageDao = db.voyageDao(),
        categoryDao = db.categoryDao(),
        // 同上：真实事务由这里注入，仓库保持可测。
        runInTransaction = { block -> db.withTransaction(block) },
    )

    val categoryRepository = CategoryRepository(
        categoryDao = db.categoryDao(),
        eventDao = db.eventDao(),
        // 删除自定义类别时把关联心愿单条目一并迁移（与 eventDao.reassignCategory 同思路）。
        wishlistDao = db.wishlistDao(),
    )

    val locationPresetRepository = LocationPresetRepository(db.locationPresetDao())

    val pixieDustRepository = PixieDustRepository(db.pixieDustDao())

    val pixieLevelRepository = PixieLevelRepository(db.pixieLevelDao())

    val wishlistRepository = WishlistRepository(db.wishlistDao(), db.categoryDao())

    val voyageArchiveRepository = VoyageArchiveRepository(
        voyageDao = db.voyageDao(),
        eventDao = db.eventDao(),
        categoryDao = db.categoryDao(),
        pixieDustDao = db.pixieDustDao(),
        pixieLevelDao = db.pixieLevelDao(),
        wishlistDao = db.wishlistDao(),
        voyageArchiveDao = db.voyageArchiveDao(),
        // 同上：真实事务由这里注入，仓库保持可测。
        runInTransaction = { block -> db.withTransaction(block) },
    )

    val reminderScheduler = ReminderScheduler(
        context = context.applicationContext,
        eventRepository = eventRepository,
        categoryRepository = categoryRepository,
        settingsStore = settingsStore,
    )
}
