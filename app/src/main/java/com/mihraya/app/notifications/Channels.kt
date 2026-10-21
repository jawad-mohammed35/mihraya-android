package com.mihraya.app.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

object Channels {
    const val ADHAN = "prayer_adhan"
    const val TONE = "prayer_tone"
    const val SILENT = "prayer_silent"
    const val REMINDER = "prayer_reminder"
    const val DOWNLOADS = "downloads"

    fun ensure(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(ADHAN, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName("Adhan")
                    .setDescription("Plays the call to prayer at prayer time")
                    .setSound(null, null)
                    .setVibrationEnabled(false)
                    .build(),
                NotificationChannelCompat.Builder(TONE, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName("Prayer alerts")
                    .setDescription("Prayer time alerts with a notification sound")
                    .setVibrationEnabled(true)
                    .build(),
                NotificationChannelCompat.Builder(SILENT, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Silent prayer alerts")
                    .setDescription("Prayer time alerts without sound")
                    .setSound(null, null)
                    .setVibrationEnabled(false)
                    .build(),
                NotificationChannelCompat.Builder(REMINDER, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Reminders")
                    .setDescription("Reminders before prayers and for the last third of the night")
                    .build(),
                NotificationChannelCompat.Builder(DOWNLOADS, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName("Downloads")
                    .setDescription("Progress of offline recitation downloads")
                    .build(),
            ),
        )
    }
}
