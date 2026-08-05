package com.cruise.assistant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cruise.assistant.CruiseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? CruiseApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.container.reminderScheduler.rescheduleAll()
            } catch (e: Exception) {
                // 开机重排失败不应让 receiver 崩溃；下次 App 启动时 CruiseApp.onCreate 会再补排一次。
            } finally {
                pending.finish()
            }
        }
    }
}
