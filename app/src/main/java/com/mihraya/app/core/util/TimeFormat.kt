package com.mihraya.app.core.util

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object TimeFormat {
    private val clock12 = DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH)
    private val clock24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val meridiem = DateTimeFormatter.ofPattern("a", Locale.ENGLISH)
    private val weekdayDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun clock(time: ZonedDateTime, use24h: Boolean): String = time.format(if (use24h) clock24 else clock12)

    fun suffix(time: ZonedDateTime, use24h: Boolean): String = if (use24h) "" else time.format(meridiem)

    fun full(time: ZonedDateTime, use24h: Boolean): String =
        if (use24h) clock(time, true) else "${clock(time, false)} ${suffix(time, false)}"

    fun longDate(time: ZonedDateTime): String = time.format(weekdayDate)

    fun shortDate(time: ZonedDateTime): String = time.format(shortDate)

    fun monthYear(time: ZonedDateTime): String = time.format(monthYear)

    fun countdown(duration: Duration): String {
        val total = duration.seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun humanize(duration: Duration): String {
        val minutes = duration.toMinutes().coerceAtLeast(0)
        return when {
            minutes < 1 -> "less than a minute"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    fun media(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun bytes(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
        bytes >= 1_000_000 -> String.format(Locale.US, "%.0f MB", bytes / 1e6)
        bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1e3)
        else -> "$bytes B"
    }
}

/** Emits the current instant on every wall-clock boundary of [periodMs]. */
fun tickerFlow(periodMs: Long = 1_000L): Flow<Instant> = flow {
    while (true) {
        val now = Instant.now()
        emit(now)
        delay(periodMs - now.toEpochMilli() % periodMs)
    }
}
