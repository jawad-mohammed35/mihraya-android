package com.mihraya.app.data.hadith

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.work.*
import com.mihraya.app.appContainer
import com.mihraya.app.data.settings.jsonDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/** Content has its own disposable DB; bookmarks/preferences use a separately backed-up DataStore. */
class HadithRepository(
    private val context: Context,
    private val http: OkHttpClient,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val database = HadithDatabase(context)
    private val writeLock = Mutex()
    private val randomHistory = HadithRandomHistory()
    val changes = MutableStateFlow(0)
    val initializationError = MutableStateFlow<String?>(null)
    private val store = jsonDataStore(context, "hadith-user.json", json, HadithPreferences.serializer(), HadithPreferences(), scope)
    val preferences = store.data.stateIn(scope, SharingStarted.Eagerly, HadithPreferences())
    val downloads = WorkManager.getInstance(context).getWorkInfosByTagFlow("hadith")
    private var cachedCatalogue: List<HadithEdition>? = null

    suspend fun catalogue(): List<HadithEdition> = withContext(Dispatchers.IO) {
        cachedCatalogue ?: json.decodeFromString<List<HadithEdition>>(
            context.assets.open("hadith/catalogue.json").bufferedReader().use { it.readText() },
        ).also { editions ->
            editions.forEach { it.validate() }
            require(editions.map { it.key }.distinct().size == editions.size)
            cachedCatalogue = editions
        }
    }

    suspend fun ensureBundled() {
        try {
            val existing = installed().associateBy { it.key }
            catalogue().filter { it.bundledAsset.isNotBlank() && existing[it.key]?.sha256 != it.sha256 }
                .forEach { install(it) {} }
            initializationError.value = null
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { initializationError.value = "The bundled library could not be prepared. Please retry." }
    }

    fun edit(transform: (HadithPreferences) -> HadithPreferences) = scope.launch { store.updateData(transform) }
    fun bookmark(hit: HadithHit) = edit { prefs ->
        val existing = prefs.bookmarks.any { it.collection == hit.collection && it.id == hit.text.id }
        prefs.copy(bookmarks = if (existing) prefs.bookmarks.filterNot { it.collection == hit.collection && it.id == hit.text.id }
        else listOf(HadithBookmark(hit.collection, hit.text.id, hit.name, hit.text.reference)) + prefs.bookmarks)
    }
    fun removeBookmark(bookmark: HadithBookmark) = edit { it.copy(bookmarks = it.bookmarks - bookmark) }
    fun markRead(hit: HadithHit) = edit { it.copy(lastRead = it.lastRead + (hit.collection to hit.text.id)) }

    suspend fun installed(): List<HadithEdition> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery("SELECT metadata FROM editions ORDER BY rowid", null).use { c ->
            buildList { while (c.moveToNext()) add(json.decodeFromString<HadithEdition>(c.getString(0))) }
        }
    }

    suspend fun chapters(collection: String): List<HadithChapter> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery("SELECT id,name,total FROM chapters WHERE collection=? ORDER BY position", arrayOf(collection)).use { c ->
            buildList { while (c.moveToNext()) add(HadithChapter(c.getString(0), c.getString(1), c.getInt(2))) }
        }
    }

    private suspend fun languageFor(collection: String, preferred: String): HadithEdition? {
        val editions = installed().filter { it.collection == collection }
        return editions.firstOrNull { it.language == preferred } ?: editions.firstOrNull { it.language == "en" }
    }

    suspend fun read(collection: String, id: String, language: String): HadithHit? = withContext(Dispatchers.IO) {
        val edition = languageFor(collection, language) ?: return@withContext null
        database.readableDatabase.rawQuery("SELECT * FROM texts WHERE collection=? AND id=? AND language=?", arrayOf(collection, id, edition.language)).use { c ->
            if (c.moveToFirst()) HadithHit(collection, edition.name, c.text(), edition.language) else null
        }
    }

    /** Bound results and fetch another page on demand. LIKE arguments are escaped, never interpolated. */
    suspend fun search(query: String, collection: String, chapter: String, language: String, limit: Int): List<HadithHit> = withContext(Dispatchers.IO) {
        val editions = installed().groupBy { it.collection }.values.mapNotNull { group ->
            group.firstOrNull { it.language == language } ?: group.firstOrNull { it.language == "en" }
        }.filter { collection.isBlank() || it.collection == collection }
        val hits = mutableListOf<HadithHit>()
        val needle = "%" + foldHadith(query.trim()).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        for (edition in editions) {
            val args = mutableListOf(edition.collection, edition.language)
            var where = "collection=? AND language=?"
            if (chapter.isNotBlank()) { where += " AND chapter=?"; args += chapter }
            if (query.isNotBlank()) { where += " AND search LIKE ? ESCAPE '\\'"; args += needle }
            database.readableDatabase.rawQuery("SELECT * FROM texts WHERE $where ORDER BY position LIMIT ?", (args + (limit + 1).toString()).toTypedArray()).use { c ->
                while (c.moveToNext() && hits.size <= limit) hits += HadithHit(edition.collection, edition.name, c.text(), edition.language)
            }
            if (hits.size > limit) break
        }
        hits
    }

    suspend fun random(language: String): HadithHit? = withContext(Dispatchers.IO) {
        // Select only IDs from canonical English records; no full-collection text load.
        val db = database.readableDatabase
        val total = db.rawQuery("SELECT count(*) FROM texts WHERE language='en'", null).use { it.moveToFirst(); it.getInt(0) }
        val excluded = randomHistory.exclusions(total)
        val clause = if (excluded.isEmpty()) "" else " AND (collection || ':' || id) NOT IN (${excluded.joinToString { "?" }})"
        val key = db.rawQuery("SELECT collection,id FROM texts WHERE language='en'$clause ORDER BY RANDOM() LIMIT 1", excluded.toTypedArray()).use { c ->
            if (c.moveToFirst()) c.getString(0) to c.getString(1) else null
        } ?: return@withContext null
        randomHistory.record("${key.first}:${key.second}")
        read(key.first, key.second, language)
    }

    fun download(edition: HadithEdition) {
        val request = OneTimeWorkRequestBuilder<HadithDownloadWorker>()
            .setInputData(workDataOf("edition" to edition.key))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .addTag("hadith").addTag(edition.key).build()
        WorkManager.getInstance(context).enqueueUniqueWork("hadith:${edition.key}", ExistingWorkPolicy.KEEP, request)
    }
    fun cancel(edition: HadithEdition) { WorkManager.getInstance(context).cancelUniqueWork("hadith:${edition.key}") }

    suspend fun remove(edition: HadithEdition) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            val db = database.writableDatabase
            db.beginTransaction()
            try {
                db.delete("texts", "collection=? AND language=?", arrayOf(edition.collection, edition.language))
                db.delete("editions", "collection=? AND language=?", arrayOf(edition.collection, edition.language))
                if (edition.language == "en") {
                    db.delete("texts", "collection=?", arrayOf(edition.collection))
                    db.delete("editions", "collection=?", arrayOf(edition.collection))
                    db.delete("chapters", "collection=?", arrayOf(edition.collection))
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            db.execSQL("VACUUM")
            changes.value++
        }
    }

    /** Download to private temporary storage, verify bytes/digest, then atomically install. */
    suspend fun install(edition: HadithEdition, progress: suspend (Int) -> Unit) = withContext(Dispatchers.IO) {
        edition.validate()
        require(catalogue().contains(edition)) { "Edition is not approved" }
        val temp = File.createTempFile("hadith-", ".jsonl", context.cacheDir)
        try {
            suspend fun copy(input: java.io.InputStream) {
                val digest = MessageDigest.getInstance("SHA-256")
                var received = 0L
                var reported = -1
                input.use { temp.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        received += n
                        require(received <= edition.bytes) { "Package exceeds declared size" }
                        output.write(buffer, 0, n); digest.update(buffer, 0, n)
                        val percent = (received * 100 / edition.bytes).toInt()
                        if (percent != reported) { progress(percent); reported = percent }
                    }
                } }
                require(received == edition.bytes) { "Incomplete package" }
                require(digest.digest().joinToString("") { "%02x".format(it) } == edition.sha256) { "Package checksum mismatch" }
            }
            if (edition.bundledAsset.isNotBlank()) copy(context.assets.open(edition.bundledAsset))
            else http.newCall(Request.Builder().url(edition.url).build()).execute().use { response ->
                if (response.code == 408 || response.code == 429 || response.code >= 500) throw java.io.IOException("Temporary download failure")
                check(response.isSuccessful) { "Download failed (${response.code})" }
                copy(checkNotNull(response.body).byteStream())
            }
            writeLock.withLock { importPackage(edition, temp) }
            changes.value++
        } finally { temp.delete() }
    }

    private suspend fun importPackage(edition: HadithEdition, file: File) {
        val validator = HadithPackageValidator(edition)
        val db = database.writableDatabase
        val base = installed().firstOrNull { it.collection == edition.collection && it.language == "en" }
        require(edition.language == "en" || base != null) { "Download English first" }
        // Revision upgrades require a reviewed coordinated migration, preserving stable bookmark IDs.
        require(base == null || base.revision == edition.revision) { "Edition revision requires migration" }
        val chapters = linkedMapOf<String, HadithChapter>()
        val chapterCounts = mutableMapOf<String, Int>()
        var count = 0
        db.beginTransaction()
        try {
            db.delete("texts", "collection=? AND language=?", arrayOf(edition.collection, edition.language))
            if (edition.language == "en") db.delete("chapters", "collection=?", arrayOf(edition.collection))
            file.bufferedReader(Charsets.UTF_8).use { input ->
                // Bound a single line before decoding, including malformed packages without newlines.
                fun line(): String? {
                    val result = StringBuilder()
                    while (true) {
                        val c = input.read()
                        if (c == -1) return result.toString().takeIf { it.isNotEmpty() }
                        if (c == 10) return result.toString()
                        require(result.length < 512_000) { "Package record too large" }
                        result.append(c.toChar())
                    }
                }
                val header = json.decodeFromString<HadithPackageLine>(checkNotNull(line()))
                validator.accept(header)
                require(header.type == "header" && header.schema == 1 && header.edition == edition.key && header.revision == edition.revision)
                while (true) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val raw = line() ?: break
                    val row = json.decodeFromString<HadithPackageLine>(raw)
                    validator.accept(row)
                    when (row.type) {
                        "chapter" -> {
                            require(count == 0)
                            val ch = checkNotNull(row.chapter)
                            require(ch.id.isNotBlank() && ch.name.isNotBlank() && ch.count > 0 && chapters.put(ch.id, ch) == null)
                            if (edition.language == "en") db.insertOrThrow("chapters", null, ContentValues().apply {
                                put("collection", edition.collection); put("id", ch.id); put("name", ch.name); put("total", ch.count); put("position", chapters.size)
                            })
                        }
                        "hadith" -> {
                            val h = checkNotNull(row.hadith); h.validate()
                            require(chapters.containsKey(h.chapter))
                            if (edition.language != "en") {
                                db.rawQuery("SELECT arabic,reference,chapter,number FROM texts WHERE collection=? AND id=? AND language='en'", arrayOf(edition.collection, h.id)).use { c ->
                                    require(c.moveToFirst() && c.getString(0) == h.arabic && c.getString(1) == h.reference && c.getString(2) == h.chapter && c.getString(3) == h.number) { "Translation reference mismatch" }
                                }
                            }
                            db.insertOrThrow("texts", null, ContentValues().apply {
                                put("collection", edition.collection); put("language", edition.language); put("id", h.id)
                                put("chapter", h.chapter); put("number", h.number); put("arabic", h.arabic); put("translation", h.translation)
                                put("reference", h.reference); put("grade", h.grade); put("narrator", h.narrator); put("isnad", h.isnad)
                                put("position", count); put("search", foldHadith("${h.arabic} ${h.translation} ${h.reference} ${h.number}"))
                            })
                            count++; require(count <= edition.count)
                            chapterCounts[h.chapter] = (chapterCounts[h.chapter] ?: 0) + 1
                        }
                        else -> error("Unknown package record")
                    }
                }
            }
            require(count == edition.count && chapters.size == edition.chapters)
            validator.finish()
            require(chapters.values.all { chapterCounts[it.id] == it.count }) { "Chapter counts mismatch" }
            require(base == null || base.count == count) { "Translation is incomplete" }
            db.insertWithOnConflict("editions", null, ContentValues().apply {
                put("collection", edition.collection); put("language", edition.language); put("metadata", json.encodeToString(edition))
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}

private fun Cursor.text(): HadithText {
    fun field(name: String) = getString(getColumnIndexOrThrow(name))
    return HadithText(field("id"), field("chapter"), field("number"), field("arabic"), field("translation"), field("reference"), field("grade"), field("narrator"), field("isnad"))
}

private class HadithDatabase(context: Context) : SQLiteOpenHelper(context, "hadith-content.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE editions(collection TEXT NOT NULL, language TEXT NOT NULL, metadata TEXT NOT NULL, PRIMARY KEY(collection,language))")
        db.execSQL("CREATE TABLE chapters(collection TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, total INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(collection,id))")
        db.execSQL("CREATE TABLE texts(collection TEXT NOT NULL, language TEXT NOT NULL, id TEXT NOT NULL, chapter TEXT NOT NULL, number TEXT NOT NULL, arabic TEXT NOT NULL, translation TEXT NOT NULL, reference TEXT NOT NULL, grade TEXT NOT NULL, narrator TEXT NOT NULL, isnad TEXT NOT NULL, position INTEGER NOT NULL, search TEXT NOT NULL, PRIMARY KEY(collection,language,id))")
        db.execSQL("CREATE INDEX chapter_lookup ON texts(collection,language,chapter,position)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Hadith schema migration required; never discard content implicitly")
    }
}

class HadithDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = applicationContext.appContainer.hadith
        return try {
            val edition = repo.catalogue().firstOrNull { it.key == inputData.getString("edition") }
                ?: return Result.failure(workDataOf("error" to "Edition is unavailable"))
            repo.install(edition) { setProgress(workDataOf("percent" to it)) }
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: java.io.IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure(workDataOf("error" to "Download interrupted. Please retry."))
        } catch (error: Exception) { Result.failure(workDataOf("error" to "Package could not be verified. Please retry later.")) }
    }
}
