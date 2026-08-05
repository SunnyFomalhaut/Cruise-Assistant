package com.cruise.assistant

import android.content.Context
import com.cruise.assistant.data.db.CruiseDatabase
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.LocationPresetRepository
import com.cruise.assistant.data.repo.VoyageRepository
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
    )

    val eventRepository = EventRepository(
        eventDao = db.eventDao(),
        voyageDao = db.voyageDao(),
    )

    val categoryRepository = CategoryRepository(
        categoryDao = db.categoryDao(),
        eventDao = db.eventDao(),
    )

    val locationPresetRepository = LocationPresetRepository(db.locationPresetDao())

    val reminderScheduler = ReminderScheduler(
        context = context.applicationContext,
        eventRepository = eventRepository,
        categoryRepository = categoryRepository,
        settingsStore = settingsStore,
    )
}
