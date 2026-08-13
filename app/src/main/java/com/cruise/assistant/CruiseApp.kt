package com.cruise.assistant

import android.app.Application
import com.cruise.assistant.domain.LocationCatalog
import com.cruise.assistant.domain.parseLocationsJson
import com.cruise.assistant.reminder.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CruiseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        // 全船地点目录：从 assets/locations.json 解析；失败兜底空目录（不崩溃）
        runCatching {
            val json = assets.open("locations.json").bufferedReader().use { it.readText() }
            LocationCatalog.setCatalog(parseLocationsJson(json))
        }
        // App 每次启动都补排一次：捡回上次未排上的（超过 100 个上限的部分）、
        // 以及设置/数据在应用未运行时被外部改动（理论上不会，但便宜且安全）的情况。
        CoroutineScope(Dispatchers.Default).launch {
            // 与 BootReceiver/TimeChangeReceiver 一致：排程异常不得让 App 启动即崩。
            // DataStore 损坏等异常在此吞掉，UI 仍可进入让用户在设置页恢复。
            runCatching { container.reminderScheduler.rescheduleAll() }
        }
    }
}
