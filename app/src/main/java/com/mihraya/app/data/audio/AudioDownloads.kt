package com.mihraya.app.data.audio

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.Locale

sealed interface DownloadStatus {
    data object Queued : DownloadStatus
    data class Running(val progress: Float) : DownloadStatus
    data object Failed : DownloadStatus
}

class AudioDownloads(private val context: Context) {

    private val root = File(context.filesDir, "audio")
    private val workManager by lazy { WorkManager.getInstance(context) }

    private val _downloaded = MutableStateFlow(scan())
    /** Downloaded surah numbers per reciter id. */
    val downloaded: StateFlow<Map<String, Set<Int>>> = _downloaded.asStateFlow()

    /** In-flight or failed downloads, keyed by [key]. */
    val active: Flow<Map<String, DownloadStatus>> by lazy {
        workManager.getWorkInfosByTagFlow(TAG).map { infos ->
            infos.mapNotNull { info ->
                val key = info.tags.firstOrNull { it.startsWith(ITEM_TAG_PREFIX) }?.removePrefix(ITEM_TAG_PREFIX)
                    ?: return@mapNotNull null
                val status = when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.Queued
                    WorkInfo.State.RUNNING -> DownloadStatus.Running(info.progress.getFloat(KEY_PROGRESS, 0f))
                    WorkInfo.State.FAILED -> DownloadStatus.Failed
                    else -> null
                } ?: return@mapNotNull null
                key to status
            }.toMap()
        }
    }

    fun file(reciterId: String, surah: Int): File =
        File(File(root, reciterId), String.format(Locale.US, "%03d.mp3", surah))

    fun isDownloaded(reciterId: String, surah: Int): Boolean =
        _downloaded.value[reciterId]?.contains(surah) == true

    fun enqueue(reciter: Reciter, surah: Int) {
        if (isDownloaded(reciter.id, surah)) return
        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(workDataOf(KEY_RECITER to reciter.id, KEY_SURAH to surah))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(TAG)
            .addTag(RECITER_TAG_PREFIX + reciter.id)
            .addTag(ITEM_TAG_PREFIX + key(reciter.id, surah))
            .build()
        workManager.enqueueUniqueWork(workName(reciter.id, surah), ExistingWorkPolicy.REPLACE, request)
    }

    fun enqueueAll(reciter: Reciter) {
        (1..114).filterNot { isDownloaded(reciter.id, it) }.forEach { enqueue(reciter, it) }
    }

    fun cancel(reciterId: String, surah: Int) {
        workManager.cancelUniqueWork(workName(reciterId, surah))
    }

    fun cancelAll(reciterId: String) {
        workManager.cancelAllWorkByTag(RECITER_TAG_PREFIX + reciterId)
    }

    fun delete(reciterId: String, surah: Int) {
        cancel(reciterId, surah)
        file(reciterId, surah).delete()
        _downloaded.update { map -> map + (reciterId to (map[reciterId].orEmpty() - surah)) }
    }

    fun deleteAll(reciterId: String) {
        cancelAll(reciterId)
        File(root, reciterId).deleteRecursively()
        _downloaded.update { it - reciterId }
    }

    fun bytesFor(reciterId: String): Long =
        File(root, reciterId).listFiles()?.filter { it.name.endsWith(".mp3") }?.sumOf { it.length() } ?: 0L

    internal fun markDownloaded(reciterId: String, surah: Int) {
        _downloaded.update { map -> map + (reciterId to (map[reciterId].orEmpty() + surah)) }
    }

    private fun scan(): Map<String, Set<Int>> =
        root.listFiles()?.filter { it.isDirectory }?.associate { dir ->
            dir.name to (dir.listFiles()
                ?.filter { it.name.endsWith(".mp3") }
                ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
                ?.toSet()
                .orEmpty())
        }.orEmpty()

    companion object {
        const val TAG = "audio-download"
        const val KEY_RECITER = "reciter"
        const val KEY_SURAH = "surah"
        const val KEY_PROGRESS = "progress"
        private const val RECITER_TAG_PREFIX = "reciter:"
        private const val ITEM_TAG_PREFIX = "item:"

        fun key(reciterId: String, surah: Int): String = "$reciterId/$surah"
        private fun workName(reciterId: String, surah: Int) = "audio-$reciterId-$surah"
    }
}
