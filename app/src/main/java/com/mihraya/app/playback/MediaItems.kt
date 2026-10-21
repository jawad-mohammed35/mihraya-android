package com.mihraya.app.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mihraya.app.R
import com.mihraya.app.core.designsystem.art.ArtworkRenderer
import com.mihraya.app.data.audio.AudioDownloads
import com.mihraya.app.data.audio.Reciter
import com.mihraya.app.data.audio.Reciters
import com.mihraya.app.data.quran.QuranMeta
import java.io.File
import java.io.FileOutputStream

data class MediaKey(val reciterId: String, val surah: Int)

object MediaIds {
    fun of(reciterId: String, surah: Int): String = "$reciterId:$surah"

    fun parse(id: String?): MediaKey? {
        val parts = id?.split(':') ?: return null
        if (parts.size != 2) return null
        val surah = parts[1].toIntOrNull()?.takeIf { it in 1..114 } ?: return null
        return MediaKey(parts[0], surah)
    }
}

object MediaItems {

    fun build(reciter: Reciter, surah: Int, downloads: AudioDownloads, artwork: Uri?): MediaItem {
        val info = QuranMeta.surah(surah)
        val local = downloads.file(reciter.id, surah)
        val uri = if (local.exists()) Uri.fromFile(local) else Uri.parse(reciter.urlFor(surah))
        val metadata = MediaMetadata.Builder()
            .setTitle(info.name)
            .setSubtitle(reciter.name)
            .setArtist(reciter.name)
            .setAlbumTitle("The Holy Quran · ${reciter.shortName}")
            .setDescription(info.meaning)
            .setTrackNumber(surah)
            .setTotalTrackCount(114)
            .setArtworkUri(artwork)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()
        return MediaItem.Builder()
            .setMediaId(MediaIds.of(reciter.id, surah))
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playlist(context: Context, reciter: Reciter, downloads: AudioDownloads): List<MediaItem> {
        val artwork = ArtworkCache.uri(context, reciter)
        return (1..114).map { build(reciter, it, downloads, artwork) }
    }

    fun resolve(context: Context, item: MediaItem, downloads: AudioDownloads): MediaItem {
        val key = MediaIds.parse(item.mediaId) ?: return item
        val reciter = Reciters.find(key.reciterId) ?: return item
        return build(reciter, key.surah, downloads, ArtworkCache.cachedUri(context, reciter))
    }
}

object ArtworkCache {
    private const val SIZE = 512

    private fun file(context: Context, reciter: Reciter) = File(context.cacheDir, "artwork/${reciter.id}_v1.png")

    fun cachedUri(context: Context, reciter: Reciter): Uri? =
        file(context, reciter).takeIf { it.exists() }?.let(Uri::fromFile)

    fun uri(context: Context, reciter: Reciter): Uri? = runCatching {
        val target = file(context, reciter)
        if (!target.exists()) {
            target.parentFile?.mkdirs()
            val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            ArtworkRenderer.draw(
                canvas = android.graphics.Canvas(bitmap),
                width = SIZE.toFloat(),
                height = SIZE.toFloat(),
                hue = reciter.hue.toInt(),
                seed = 0,
                title = reciter.arabicName,
                typeface = ResourcesCompat.getFont(context, R.font.amiri),
            )
            FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        Uri.fromFile(target)
    }.getOrNull()
}
