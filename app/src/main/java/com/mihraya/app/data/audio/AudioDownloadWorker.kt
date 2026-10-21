package com.mihraya.app.data.audio

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mihraya.app.R
import com.mihraya.app.appContainer
import com.mihraya.app.data.quran.QuranMeta
import com.mihraya.app.notifications.Channels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val reciter = Reciters.find(inputData.getString(AudioDownloads.KEY_RECITER)) ?: return Result.failure()
        val surah = inputData.getInt(AudioDownloads.KEY_SURAH, 0).takeIf { it in 1..114 } ?: return Result.failure()
        val downloads = container.audioDownloads
        val target = downloads.file(reciter.id, surah)
        if (target.exists()) {
            downloads.markDownloaded(reciter.id, surah)
            return Result.success()
        }

        runCatching { setForeground(foregroundInfo(reciter, surah)) }

        return try {
            download(reciter.urlFor(surah), target)
            downloads.markDownloaded(reciter.id, surah)
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private suspend fun download(url: String, target: File) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        val resumeFrom = if (part.exists()) part.length() else 0L
        val request = Request.Builder().url(url).apply {
            if (resumeFrom > 0) header("Range", "bytes=$resumeFrom-")
        }.build()

        applicationContext.appContainer.http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            val append = resumeFrom > 0 && response.code == 206
            val total = body.contentLength().let { if (it > 0) it + (if (append) resumeFrom else 0L) else -1L }
            var written = if (append) resumeFrom else 0L
            var lastReported = -1
            FileOutputStream(part, append).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            val percent = ((written * 100) / total).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                setProgress(workDataOf(AudioDownloads.KEY_PROGRESS to percent / 100f))
                            }
                        }
                    }
                }
            }
            if (total > 0 && written < total) throw IOException("Connection interrupted")
        }
        if (!part.renameTo(target)) {
            target.delete()
            if (!part.renameTo(target)) throw IOException("Could not save file")
        }
    }

    private fun foregroundInfo(reciter: Reciter, surah: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, Channels.DOWNLOADS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading recitations")
            .setContentText("${QuranMeta.surah(surah).name} · ${reciter.name}")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val NOTIFICATION_ID = 4100
    }
}
