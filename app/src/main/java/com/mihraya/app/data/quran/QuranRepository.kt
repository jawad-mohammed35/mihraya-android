package com.mihraya.app.data.quran

import android.content.Context
import android.util.LruCache
import com.mihraya.app.data.settings.QuranScript
import com.mihraya.app.data.settings.ReaderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.text.Normalizer

data class VerseTranslation(val translation: TranslationInfo, val text: String)

data class Verse(
    val ref: VerseRef,
    val id: Int,
    val arabic: String,
    val transliteration: String?,
    val translations: List<VerseTranslation>,
) {
    val surah: Int get() = ref.surah
    val ayah: Int get() = ref.ayah
}

data class SearchHit(val ref: VerseRef, val text: String)

sealed interface TranslationDownload {
    data object Running : TranslationDownload
    data class Failed(val message: String) : TranslationDownload
}

class QuranRepository(
    private val context: Context,
    private val http: OkHttpClient,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val dir = File(context.filesDir, "quran").apply { mkdirs() }
    private val editions = LruCache<String, Array<String>>(6)
    private val normalized = LruCache<String, Array<String>>(2)
    private val loadLock = Mutex()

    private val _installed = MutableStateFlow(scanInstalled())
    val installedTranslations: StateFlow<Set<Int>> = _installed.asStateFlow()

    private val _downloads = MutableStateFlow<Map<Int, TranslationDownload>>(emptyMap())
    val downloads: StateFlow<Map<Int, TranslationDownload>> = _downloads.asStateFlow()

    private fun scanInstalled(): Set<Int> = buildSet {
        TranslationCatalog.all.filter { it.bundled }.forEach { add(it.id) }
        dir.listFiles()
            ?.filter { it.name.startsWith("tr_") && it.name.endsWith(".txt") }
            ?.forEach { file -> file.name.removePrefix("tr_").removeSuffix(".txt").toIntOrNull()?.let(::add) }
    }

    fun isAvailable(id: Int): Boolean = id in _installed.value

    private suspend fun edition(name: String): Array<String> {
        editions.get(name)?.let { return it }
        return loadLock.withLock {
            editions.get(name) ?: withContext(Dispatchers.IO) { read(name) }.also { editions.put(name, it) }
        }
    }

    private fun read(name: String): Array<String> {
        val local = File(dir, "$name.txt")
        val stream = if (local.exists()) local.inputStream() else context.assets.open("quran/$name.txt")
        val lines = stream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
        check(lines.size == QuranMeta.VERSE_COUNT) { "Edition $name is incomplete" }
        return lines.toTypedArray()
    }

    private fun scriptEdition(script: QuranScript): String = when (script) {
        QuranScript.Uthmani -> "uthmani"
        QuranScript.IndoPak -> "indopak"
    }

    suspend fun surahVerses(surah: Int, prefs: ReaderPreferences): List<Verse> {
        val arabic = edition(scriptEdition(prefs.script))
        val transliteration =
            if (prefs.showTransliteration) edition("tr_${TranslationCatalog.TRANSLITERATION_ID}") else null
        val translations = if (prefs.showTranslation) {
            prefs.translations.filter(::isAvailable).mapNotNull { id ->
                TranslationCatalog.byId(id)?.let { info -> info to edition("tr_$id") }
            }
        } else {
            emptyList()
        }
        val first = QuranMeta.firstVerseId(surah)
        return (0 until QuranMeta.surah(surah).verseCount).map { offset ->
            val index = first - 1 + offset
            Verse(
                ref = VerseRef(surah, offset + 1),
                id = first + offset,
                arabic = arabic[index],
                transliteration = transliteration?.get(index),
                translations = translations.map { (info, text) -> VerseTranslation(info, text[index]) },
            )
        }
    }

    suspend fun bismillah(script: QuranScript): String = edition(scriptEdition(script))[0]

    suspend fun verse(ref: VerseRef, script: QuranScript, translationId: Int): Verse {
        val index = QuranMeta.verseId(ref.surah, ref.ayah) - 1
        val id = if (isAvailable(translationId)) translationId else 20
        val info = TranslationCatalog.byId(id)
        return Verse(
            ref = ref,
            id = index + 1,
            arabic = edition(scriptEdition(script))[index],
            transliteration = null,
            translations = listOfNotNull(info?.let { VerseTranslation(it, edition("tr_$id")[index]) }),
        )
    }

    suspend fun search(query: String, translationId: Int, limit: Int = 60): List<SearchHit> =
        withContext(Dispatchers.Default) {
            val trimmed = query.trim()
            val arabic = trimmed.any { it in '\u0600'..'\u06FF' }
            val needle = if (arabic) normalizeArabic(trimmed) else normalizeLatin(trimmed)
            if (needle.length < 3) return@withContext emptyList()
            val name = if (arabic) "uthmani" else "tr_${if (isAvailable(translationId)) translationId else 20}"
            val source = edition(name)
            val haystack = normalized.get(name) ?: source
                .map { if (arabic) normalizeArabic(it) else normalizeLatin(it) }
                .toTypedArray()
                .also { normalized.put(name, it) }
            val hits = ArrayList<SearchHit>()
            for (i in haystack.indices) {
                if (haystack[i].contains(needle)) {
                    hits += SearchHit(QuranMeta.refOf(i + 1), source[i])
                    if (hits.size >= limit) break
                }
            }
            hits
        }

    fun downloadTranslation(id: Int) {
        if (_downloads.value[id] == TranslationDownload.Running || isAvailable(id)) return
        _downloads.update { it + (id to TranslationDownload.Running) }
        scope.launch {
            val result = runCatching { fetchTranslation(id) }
            if (result.isSuccess) {
                _installed.update { it + id }
                _downloads.update { it - id }
            } else {
                val message = result.exceptionOrNull()?.message ?: "Download failed"
                _downloads.update { it + (id to TranslationDownload.Failed(message)) }
            }
        }
    }

    private suspend fun fetchTranslation(id: Int) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.quran.com/api/v4/quran/translations/$id")
            .header("Accept", "application/json")
            .build()
        val body = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Server responded with ${response.code}")
            response.body?.string() ?: throw IOException("Empty response")
        }
        val payload = json.decodeFromString(TranslationPayload.serializer(), body)
        val lines = payload.translations.map { cleanTranslation(it.text) }
        if (lines.size != QuranMeta.VERSE_COUNT) throw IOException("Incomplete translation")
        val part = File(dir, "tr_$id.txt.part")
        part.writeText(lines.joinToString("\n"), Charsets.UTF_8)
        val target = File(dir, "tr_$id.txt")
        if (!part.renameTo(target)) {
            target.delete()
            if (!part.renameTo(target)) throw IOException("Could not save translation")
        }
        editions.remove("tr_$id")
        normalized.remove("tr_$id")
    }

    fun deleteTranslation(id: Int) {
        if (TranslationCatalog.byId(id)?.bundled == true) return
        File(dir, "tr_$id.txt").delete()
        editions.remove("tr_$id")
        normalized.remove("tr_$id")
        _installed.update { it - id }
    }

    fun clearDownloadError(id: Int) {
        _downloads.update { if (it[id] is TranslationDownload.Failed) it - id else it }
    }

    fun downloadedBytes(): Long =
        dir.listFiles()?.filter { it.name.endsWith(".txt") }?.sumOf { it.length() } ?: 0L

    @Serializable
    private data class TranslationPayload(val translations: List<TranslationItem> = emptyList())

    @Serializable
    private data class TranslationItem(val text: String = "")

    private companion object {
        val supTag = Regex("<sup[^>]*>.*?</sup>", RegexOption.DOT_MATCHES_ALL)
        val anyTag = Regex("<[^>]+>")
        val whitespace = Regex("\\s+")
        val numericEntity = Regex("&#(\\d+);")
        val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
        val combiningMarks = Regex("\\p{Mn}+")

        fun cleanTranslation(raw: String): String = raw
            .replace(supTag, "")
            .replace(anyTag, "")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(numericEntity) { match ->
                match.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) }.orEmpty()
            }
            .replace("&amp;", "&")
            .replace(whitespace, " ")
            .trim()

        fun normalizeArabic(text: String): String = text
            .replace(arabicMarks, "")
            .replace('ٱ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')

        fun normalizeLatin(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()
    }
}
