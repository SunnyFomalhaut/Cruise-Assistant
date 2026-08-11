package com.cruise.assistant.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cruise.assistant.MainActivity
import com.cruise.assistant.R

private const val CHANNEL_ID = "cruise_events"

object NotificationHelper {

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(
                        android.media.RingtoneManager.TYPE_NOTIFICATION,
                    ),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build(),
                )
            },
        )
    }

    /**
     * @param titleLine 形如 "演出 · 扬帆起航"
     * @param bodyLine  形如 "13:15 · 9F船尾剧场"
     */
    fun notifyEvent(context: Context, eventId: Long, titleLine: String, bodyLine: String) {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // 大图标：从 app 图标取 bitmap，让横幅更醒目
        // getApplicationIcon 返回 Drawable，需转成 Bitmap 才能喂给 setLargeIcon
        val largeIcon = runCatching {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            (drawable as? BitmapDrawable)?.bitmap
        }.getOrNull()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(largeIcon)
            .setContentTitle(titleLine)
            .setContentText(bodyLine)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$titleLine\n$bodyLine"),
            )
            .setColor(context.getColor(com.cruise.assistant.R.color.notification_accent))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        // 显式 try/catch（而不是 runCatching）：lint 的 MissingPermission 检查按 AST 形状
        // 识别 try/catch SecurityException，runCatching 内联包一层它未必能追踪到。
        try {
            NotificationManagerCompat.from(context).notify(eventId.toInt(), notification)
        } catch (e: SecurityException) {
            // 用户在通知发出前撤销了权限；静默丢弃这条提醒，不让 receiver 崩溃。
        }
    }
}
