package com.mihraya.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mihraya.app.data.prayer.PrayerCalculator
import com.mihraya.app.data.prayer.Salah
import com.mihraya.app.data.settings.AlertMode
import com.mihraya.app.data.settings.AppSettings
import java.time.Duration
import java.time.Instant

enum class AlarmKind { Prayer, Reminder, Qiyam }

data class AlarmEvent(val kind: AlarmKind, val salah: Salah?, val at: Instant)

class PrayerAlarmScheduler(
    private val context: Context,
    private val calculator: PrayerCalculator,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    fun events(settings: AppSettings, from: Instant, to: Instant): List<AlarmEvent> {
        val location = settings.location ?: return emptyList()
        val alerts = settings.alerts
        val firstDate = from.atZone(location.zoneId).toLocalDate().minusDays(1)
        val events = mutableListOf<AlarmEvent>()
        for (offset in 0L..3L) {
            val day = calculator.day(location, settings.prayer, firstDate.plusDays(offset)) ?: continue
            for (salah in Salah.entries) {
                if (alerts.modeFor(salah) == AlertMode.Off) continue
                val at = day[salah].toInstant()
                events += AlarmEvent(AlarmKind.Prayer, salah, at)
                if (alerts.reminderMinutes > 0 && salah.isPrayer) {
                    events += AlarmEvent(AlarmKind.Reminder, salah, at.minus(Duration.ofMinutes(alerts.reminderMinutes.toLong())))
                }
            }
            if (alerts.qiyamReminder) events += AlarmEvent(AlarmKind.Qiyam, null, day.lastThird.toInstant())
        }
        return events.filter { !it.at.isBefore(from) && !it.at.isAfter(to) }.sortedBy { it.at }
    }

    fun reschedule(settings: AppSettings) {
        val manager = alarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        manager.cancel(
            PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
        )
        val now = Instant.now()
        val next = events(settings, now.plusSeconds(5), now.plus(Duration.ofDays(2))).firstOrNull() ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent.putExtra(PrayerAlarmReceiver.EXTRA_AT, next.at.toEpochMilli()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = next.at.toEpochMilli()
        if (canScheduleExact()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private companion object {
        const val REQUEST_CODE = 7001
    }
}
