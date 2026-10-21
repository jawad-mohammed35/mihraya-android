package com.mihraya.app.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mihraya.app.MainActivity
import com.mihraya.app.R
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.AlertMode
import com.mihraya.app.data.settings.AppSettings
import java.time.Instant

object PrayerNotifier {

    fun deliver(context: Context, settings: AppSettings, event: AlarmEvent) {
        when (event.kind) {
            AlarmKind.Prayer -> {
                val salah = event.salah ?: return
                when (settings.alerts.modeFor(salah)) {
                    AlertMode.Adhan -> {
                        val started = shouldPlaySound(context, settings) && AdhanService.start(context, salah, event.at)
                        if (!started) {
                            post(context, prayerNotification(context, settings, salah, event.at, Channels.SILENT), prayerId(salah))
                        }
                    }
                    AlertMode.Tone -> post(context, prayerNotification(context, settings, salah, event.at, Channels.TONE), prayerId(salah))
                    AlertMode.Silent -> post(context, prayerNotification(context, settings, salah, event.at, Channels.SILENT), prayerId(salah))
                    AlertMode.Off -> Unit
                }
            }
            AlarmKind.Reminder -> {
                val salah = event.salah ?: return
                val minutes = settings.alerts.reminderMinutes
                val at = event.at.plusSeconds(minutes * 60L)
                val time = TimeFormat.full(at.atZone(settings.zone()), settings.use24Hour)
                val notification = base(context, Channels.REMINDER)
                    .setContentTitle("${salah.label} in $minutes minutes")
                    .setContentText("${salah.label} begins at $time")
                    .build()
                post(context, notification, REMINDER_ID_BASE + salah.ordinal)
            }
            AlarmKind.Qiyam -> {
                val notification = base(context, Channels.REMINDER)
                    .setContentTitle("The last third of the night")
                    .setContentText("A blessed time for Qiyām al-Layl and duʿāʾ.")
                    .build()
                post(context, notification, QIYAM_ID)
            }
        }
    }

    fun prayerNotification(
        context: Context,
        settings: AppSettings,
        salah: Salah,
        at: Instant,
        channel: String,
        ongoingAction: NotificationCompat.Action? = null,
        deleteIntent: PendingIntent? = null,
    ): Notification {
        val zoned = at.atZone(settings.zone())
        val label = salah.labelOn(zoned.toLocalDate())
        val time = TimeFormat.full(zoned, settings.use24Hour)
        val place = settings.location?.name
        val text = when {
            salah == Salah.Sunrise -> "The sun has risen — the time for Fajr has ended."
            place != null -> "It's time for $label · $place"
            else -> "It's time for $label"
        }
        return base(context, channel)
            .setContentTitle("$label · $time")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .apply {
                ongoingAction?.let { addAction(it) }
                deleteIntent?.let { setDeleteIntent(it) }
            }
            .build()
    }

    private fun base(context: Context, channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.champagne))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context))

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @SuppressLint("MissingPermission")
    fun post(context: Context, notification: Notification, id: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) manager.notify(id, notification)
    }

    private fun shouldPlaySound(context: Context, settings: AppSettings): Boolean {
        if (!settings.alerts.respectSilentMode) return true
        val audio = context.getSystemService(AudioManager::class.java) ?: return true
        return audio.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    fun prayerId(salah: Salah): Int = PRAYER_ID_BASE + salah.ordinal

    private const val PRAYER_ID_BASE = 1000
    private const val REMINDER_ID_BASE = 1100
    private const val QIYAM_ID = 1200
}

fun AppSettings.zone() = location?.zoneId ?: java.time.ZoneId.systemDefault()
