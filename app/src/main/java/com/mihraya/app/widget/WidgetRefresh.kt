package com.mihraya.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val widgets = listOf(
        PrayerTimetableWidget(),
        NextPrayerWidget(),
        QiblaWidget(),
        VerseWidget(),
        LastReadWidget(),
        BookmarksWidget(),
        AudioWidget(),
        TasbihWidget(),
    )

    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch {
            widgets.forEach { widget -> runCatching { widget.updateAll(app) } }
        }
    }
}
