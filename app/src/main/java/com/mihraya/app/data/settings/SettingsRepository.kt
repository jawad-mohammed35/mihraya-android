package com.mihraya.app.data.settings

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SettingsRepository(
    context: Context,
    json: Json,
    private val scope: CoroutineScope,
) {
    private val store = jsonDataStore(
        context = context,
        fileName = "settings.json",
        json = json,
        serializer = AppSettings.serializer(),
        default = AppSettings(),
        scope = scope,
    )

    val flow: Flow<AppSettings> = store.data

    /** `null` until the stored settings have been read for the first time. */
    val state: StateFlow<AppSettings?> = store.data.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun current(): AppSettings = store.data.first()

    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings =
        store.updateData(transform)

    fun edit(transform: (AppSettings) -> AppSettings): Job = scope.launch { update(transform) }
}
