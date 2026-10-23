package com.mihraya.app.data.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * The supplication after the Adhan from Ḥiṣn al-Muslim, recited by Hamad al-Duraihim. It is fetched once from
 * hisnmuslim.com and kept on the device, so it plays offline from then on.
 */
class DuaAudio(context: Context, private val http: OkHttpClient) {

    val file = File(context.filesDir, "dua/after_adhan.mp3")
    private val mutex = Mutex()
    private val _state = MutableStateFlow(if (file.length() > 0) State.Ready else State.Missing)
    val state: StateFlow<State> = _state.asStateFlow()

    val isReady: Boolean get() = file.length() > 0

    /** Downloads the recording if needed. Returns null when it isn't available and couldn't be fetched. */
    suspend fun ensure(): File? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isReady) return@withLock file
            _state.value = State.Downloading
            try {
                download()
                _state.value = State.Ready
                file
            } catch (e: IOException) {
                _state.value = State.Failed
                null
            }
        }
    }

    private fun download() {
        file.parentFile?.mkdirs()
        val part = File(file.parentFile, file.name + ".part")
        http.newCall(Request.Builder().url(URL).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            part.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
            val expected = body.contentLength()
            if (part.length() == 0L || (expected > 0 && part.length() != expected)) {
                part.delete()
                throw IOException("Incomplete download")
            }
        }
        if (!part.renameTo(file)) {
            file.delete()
            if (!part.renameTo(file)) throw IOException("Could not save file")
        }
    }

    enum class State { Missing, Downloading, Ready, Failed }

    companion object {
        const val URL = "https://www.hisnmuslim.com/audio/ar/25.mp3"
    }
}
