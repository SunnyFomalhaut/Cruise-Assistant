package com.cruise.assistant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cruise.assistant.CruiseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 需求8/提醒：ReminderScheduler 排闹钟时用 ZoneId.systemDefault() 把本地墙钟时间换算成
 * 绝对触发时刻。如果设备时区在航程中途变了（下船前跨时区很常见），已经排好的绝对
 * triggerMs 就不再对应用户原本想要的本地时间——早到或晚到差一个时区差。CruiseApp.onCreate
 * 每次开 App 都会重排，缓解了大部分情况，但如果时区在深夜变了、用户在变更后有个早班事件
 * 且变更后没重开过 App，闹钟就会按旧时区偏移触发。
 *
 * 同样监听 TIME_SET：用户手动调整系统时间也可能让已排的绝对触发时刻偏离预期。
 *
 * 镜像 BootReceiver 的写法：goAsync() + finally 里 pending.finish()，异常吞掉不让 receiver 崩。
 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED && intent.action != Intent.ACTION_TIME_CHANGED) {
            return
        }
        val app = context.applicationContext as? CruiseApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.container.reminderScheduler.rescheduleAll()
            } catch (e: Exception) {
                // 重排失败不应让 receiver 崩溃；下次 App 启动/开机时会再补排一次。
            } finally {
                pending.finish()
            }
        }
    }
}
