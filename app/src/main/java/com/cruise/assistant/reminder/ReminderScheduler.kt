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
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.formatLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

const val EXTRA_EVENT_ID = "event_id"
const val EXTRA_TITLE_LINE = "title_line"
const val EXTRA_BODY_LINE = "body_line"

/** 一次最多排这么多个闹钟（每事件最多 3 个槽位），其余等 App 下次启动时补。
 * 500 贴近 Android 每应用 ~500 闹钟的软上限；典型航次（几十事件 × 3 槽）远低于此。 */
private const val MAX_SCHEDULED = 500

/** 提醒槽位：slot 0 = 提前 15 分钟（action null 兼容旧版本）、1 = 5 分钟、2 = 开始时 */
private val SLOT_LEAD_MINUTES = intArrayOf(15, 5, 0)

/** 槽位对应的 intent action；slot 0 用 null 与旧版本一致（旧闹钟被自然覆盖） */
private fun actionForSlot(slot: Int): String? = when (slot) {
    0 -> null
    1 -> "lead_5"
    2 -> "lead_0"
    else -> null
}

class ReminderScheduler(
    private val context: Context,
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
) {
    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    /** 串行化 rescheduleAll：App 启动、BootReceiver、TimeChangeReceiver、UI 回调可能并发触发 */
    private val rescheduleMutex = Mutex()

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
    suspend fun rescheduleAll() = rescheduleMutex.withLock {
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
            (0..2).forEach { slot ->
                lookupExistingPendingIntent(eventId, slot)?.let { manager.cancel(it) }
            }
        }
        if (!settings.notificationsEnabled) {
            settingsStore.setArmedEventIds(emptySet())
            return
        }

        val zone = ZoneId.systemDefault()
        val nowMs = System.currentTimeMillis()

        val armedIds = mutableSetOf<Long>()
        // 按三个开关生成槽位列表：slot 0/1/2 是否启用
        val enabledSlots = buildList {
            if (settings.remindBefore15) add(0)
            if (settings.remindBefore5) add(1)
            if (settings.remindAtStart) add(2)
        }
        // (event, triggerMs, slot) —— 槽位在生成时确定，后续排程直接使用
        val pendingAlarms = mutableListOf<Triple<CruiseEvent, Long, Int>>()
        events.asSequence()
            .filter { it.isActive }
            .forEach { event ->
                val baseMs = event.date.atTime(event.startTime).atZone(zone).toInstant().toEpochMilli()
                enabledSlots.forEach { slot ->
                    val triggerMs = baseMs - SLOT_LEAD_MINUTES[slot] * 60_000L
                    if (triggerMs > nowMs) pendingAlarms += Triple(event, triggerMs, slot)
                }
            }
        pendingAlarms.sortedBy { it.second }.take(MAX_SCHEDULED).forEach { (event, triggerMs, slot) ->
            val categoryName = categories[event.categoryId]?.name.orEmpty()
            val titleLine = if (categoryName.isEmpty()) {
                event.title
            } else {
                context.getString(R.string.notification_title_format, categoryName, event.title)
            }
            val location = formatLocation(event.deck, event.zone, event.placeName)
            val lead = SLOT_LEAD_MINUTES[slot]
            val bodyLine = buildString {
                append("%02d:%02d".format(event.startTime.hour, event.startTime.minute))
                location?.let { append(" · ").append(it) }
                if (lead > 0) {
                    append(" · 还有 ").append(lead).append(" 分钟开始")
                } else {
                    append(" · 现在开始")
                }
            }
            val pending = schedulingPendingIntentFor(event.id, titleLine, bodyLine, slot)
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
    private fun lookupExistingPendingIntent(eventId: Long, slot: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = actionForSlot(slot)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(eventId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 排新闹钟时才真正创建/覆盖 PendingIntent，带上要展示的文案与槽位 action。 */
    private fun schedulingPendingIntentFor(
        eventId: Long,
        titleLine: String,
        bodyLine: String,
        slot: Int,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = actionForSlot(slot)
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
