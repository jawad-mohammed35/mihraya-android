package com.mihraya.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mihraya.app.appContainer
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                val settings = container.settings.current()
                val now = Instant.now()
                val scheduledFor = intent.getLongExtra(EXTRA_AT, now.toEpochMilli()).let(Instant::ofEpochMilli)
                val windowStart = maxOf(scheduledFor.minusSeconds(90), now.minus(STALE_AFTER))
                container.alarms.events(settings, windowStart, now.plusSeconds(30)).forEach { event ->
                    PrayerNotifier.deliver(context, settings, event)
                }
                container.alarms.reschedule(settings)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_AT = "at"
        private val STALE_AFTER: Duration = Duration.ofMinutes(15)
    }
}

class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                var settings = container.settings.current()
                val location = settings.location
                val systemZone = ZoneId.systemDefault().id
                if (intent.action == Intent.ACTION_TIMEZONE_CHANGED && location?.automatic == true &&
                    location.timeZone != systemZone
                ) {
                    settings = container.settings.update { it.copy(location = location.copy(timeZone = systemZone)) }
                }
                container.alarms.reschedule(settings)
            } finally {
                pending.finish()
            }
        }
    }
}
