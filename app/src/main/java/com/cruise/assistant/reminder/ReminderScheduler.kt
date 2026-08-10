package com.cruise.assistant.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cruise.assistant.R
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.domain.formatLocation
import kotlinx.coroutines.flow.first
import java.time.ZoneId

const val EXTRA_EVENT_ID = "event_id"
const val EXTRA_TITLE_LINE = "title_line"
const val EXTRA_BODY_LINE = "body_line"

/** 一次最多排这么多个闹钟，其余等 App 下次启动时补 */
private const val MAX_SCHEDULED = 100

class ReminderScheduler(
    private val context: Context,
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
) {
    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    /**
     * 取消所有旧闹钟并按当前数据重排。
     * 事件增删改、设置变更、定稿/解锁、开机、App 启动时都要调。
     */
    suspend fun rescheduleAll() {
        val manager = alarmManager ?: return
        val settings = settingsStore.settings.first()
        val events = eventRepository.getAllEvents()
        val categories = categoryRepository.observeAll().first().associateBy { it.id }

        // 取消依据不能是"当前事件表还有哪些行"：删除会先于 rescheduleAll() 把行从数据库移除，
        // 那样被删事件的 id 根本不在 events 里，闹钟永远不会被取消。
        // 改成取消"上一次实际排上闹钟的 id 集合"（armed set，持久化在 SettingsStore/DataStore
        // 里，能扛过进程死亡、开机重启），并与当前 events 的 id 取并集——
        // 并集是为了把这次改动上线前就已经孤儿化的闹钟（旧版本没做这个记账）一并清掉。
        // 只查已有的 PendingIntent（FLAG_NO_CREATE），不存在就跳过——不落入"为了取消反而
        // 新建一个从未被排过程的 PendingIntent"的坑。
        val previouslyArmed = settingsStore.getArmedEventIds()
        val currentEventIds = events.map { it.id }.toSet()
        val idsToCancel = previouslyArmed + currentEventIds
        idsToCancel.forEach { eventId ->
            lookupExistingPendingIntent(eventId)?.let { manager.cancel(it) }
        }
        if (!settings.notificationsEnabled) {
            settingsStore.setArmedEventIds(emptySet())
            return
        }

        val zone = ZoneId.systemDefault()
        val nowMs = System.currentTimeMillis()

        val armedIds = mutableSetOf<Long>()
        events.asSequence()
            .filter { it.isActive }
            .mapNotNull { event ->
                val lead = event.reminderMinutesBefore ?: settings.defaultReminderMinutes
                val triggerMs = event.date.atTime(event.startTime)
                    .minusMinutes(lead.toLong())
                    .atZone(zone).toInstant().toEpochMilli()
                if (triggerMs <= nowMs) null else event to triggerMs
            }
            .sortedBy { it.second }
            .take(MAX_SCHEDULED)
            .forEach { (event, triggerMs) ->
                val categoryName = categories[event.categoryId]?.name.orEmpty()
                val titleLine = if (categoryName.isEmpty()) {
                    event.title
                } else {
                    context.getString(R.string.notification_title_format, categoryName, event.title)
                }
                val location = formatLocation(event.deck, event.zone, event.placeName)
                val bodyLine = buildString {
                    append("%02d:%02d".format(event.startTime.hour, event.startTime.minute))
                    location?.let { append(" · ").append(it) }
                }
                val pending = schedulingPendingIntentFor(event.id, titleLine, bodyLine)
                if (canScheduleExact()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMs, pending,
                    )
                } else {
                    // 降级：允许系统在 10 分钟窗口内择机触发
                    manager.setWindow(
                        AlarmManager.RTC_WAKEUP, triggerMs, 10 * 60_000L, pending,
                    )
                }
                armedIds += event.id
            }
        settingsStore.setArmedEventIds(armedIds)
    }

    /**
     * requestCode 由事件 id 派生（这里就是 id.toInt()）。Room 的 id 是 Long，
     * 而 PendingIntent/Notification 的 requestCode/id 都是 Int——理论上超大 id 截断后
     * 可能撞车，但这个 App 单机使用、事件量级不会到 2^31，接受这个假设。
     */
    private fun requestCodeFor(eventId: Long): Int = eventId.toInt()

    /** 只查已存在的 PendingIntent，不创建新的；找不到就返回 null（不落盘、不生成对象）。 */
    private fun lookupExistingPendingIntent(eventId: Long): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(eventId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 排新闹钟时才真正创建/覆盖 PendingIntent，带上要展示的文案。 */
    private fun schedulingPendingIntentFor(
        eventId: Long,
        titleLine: String,
        bodyLine: String,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_TITLE_LINE, titleLine)
            putExtra(EXTRA_BODY_LINE, bodyLine)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(eventId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
