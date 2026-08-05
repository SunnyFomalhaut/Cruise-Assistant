package com.cruise.assistant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
        val titleLine = intent.getStringExtra(EXTRA_TITLE_LINE) ?: return
        val bodyLine = intent.getStringExtra(EXTRA_BODY_LINE).orEmpty()
        NotificationHelper.notifyEvent(context, eventId, titleLine, bodyLine)
    }
}
