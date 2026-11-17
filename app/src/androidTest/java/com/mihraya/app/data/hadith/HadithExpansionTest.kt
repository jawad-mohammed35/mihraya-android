package com.mihraya.app.data.hadith

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class HadithExpansionTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun isolated(directory: File): Context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = File(directory, "files").apply { mkdirs() }
        override fun getDatabasePath(name: String): File = File(directory, name)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).path, factory, errorHandler)
    }

    @Test fun allGeneratedPackagesInstallSearchAndPreserveMissingTranslationMetadata() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fixtures = instrumentation.context.assets.list("hadithapi").orEmpty()
        assumeTrue("Run prepare_hadithapi.py and build with -PhadithPackageQa=true", fixtures.size == 13)
        val directory = File(instrumentation.targetContext.filesDir, "expansion-qa-${System.nanoTime()}").apply { mkdirs() }
        val context = isolated(directory)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var corrupt = false
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val file = chain.request().url.pathSegments.last()
            val source = instrumentation.context.assets.open("hadithapi/$file.bin").use { it.readBytes() }
            val bytes = if (corrupt) source.copyOf(source.size - 5) else source
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Pinned QA fixture").body(bytes.toResponseBody()).build()
        }.build()
        val repo = HadithRepository(context, http, json, scope)
        try {
            repo.ensureBundled()
            val newEditions = repo.catalogue().filter { it.source == "https://hadithapi.com/" }
            assertEquals(13, newEditions.size)
            val started = System.nanoTime()
            for (edition in newEditions) {
                repo.install(edition) {}
                val hits = repo.search("", edition.collection, "", edition.language, 1)
                assertTrue(hits.isNotEmpty())
                val first = hits.first()
                assertTrue(first.text.arabic.isNotBlank())
                assertEquals(edition.language, first.language)
                assertEquals(edition.count, repo.chapters(edition.collection).sumOf { it.count })
                assertTrue(repo.chapters(edition.collection).all { it.arabicName.isNotBlank() })
                assertEquals(first.text, repo.read(edition.collection, first.text.id, edition.language)!!.text)
                if (first.text.grade.isNotBlank()) assertTrue(first.text.grade.startsWith("HadithAPI status:"))
            }
            val importMillis = (System.nanoTime() - started) / 1_000_000
            assertEquals(16, repo.installed().size) // 3 bundled + 13 new downloadable editions
            val searchStarted = System.nanoTime()
            assertTrue(repo.search("prayer", "", "", "en", 50).isNotEmpty())
            assertTrue(repo.search("الله", "bukhari", "", "en", 50).isNotEmpty())
            val searchMillis = (System.nanoTime() - searchStarted) / 1_000_000
            assertTrue(repo.chapters("muslim").any { it.id == "0" && it.name == "Introduction" })
            val missing = repo.search("1830", "bukhari", "", "en", 100).single { it.text.number == "1830" }
            assertFalse(missing.text.translationAvailable)
            assertTrue(missing.text.translation.isEmpty())
            assertTrue(missing.text.arabic.isNotBlank())
            assertTrue(repo.read("bukhari", missing.text.id, "ur")!!.text.translationAvailable)
            val mishkat = repo.search("", "mishkat", "", "en", 1).first()
            assertFalse(mishkat.text.translationAvailable)
            assertTrue(repo.read("mishkat", mishkat.text.id, "ur")!!.text.translationAvailable)
            val ids = (1..30).map { repo.random("en")!!.let { it.collection + ":" + it.text.id } }
            assertEquals(30, ids.distinct().size)
            repo.bookmark(missing).join(); repo.markRead(missing).join()
            withTimeout(5000) { repo.preferences.first { it.bookmarks.any { b -> b.id == missing.text.id } } }
            corrupt = true
            try { repo.install(newEditions.first()) {}; fail("Truncated gzip accepted") } catch (expected: IllegalArgumentException) { }
            assertNotNull(repo.read("bukhari", missing.text.id, "en"))
            repo.remove(newEditions.first { it.collection == "bukhari" && it.language == "ur" })
            assertEquals("en", repo.read("bukhari", missing.text.id, "ur")!!.language)
            assertTrue(repo.preferences.value.bookmarks.any { it.id == missing.text.id })
            android.util.Log.i("HadithExpansionQA", "Pinned packages imported in ${importMillis}ms; two bounded searches in ${searchMillis}ms")
            Unit
        } finally { scope.cancel(); directory.deleteRecursively() }
    }

    @Test fun versionOneContentAndBookmarksSurviveSearchMigration() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(base.filesDir, "migration-qa-${System.nanoTime()}").apply { mkdirs() }
        val context = isolated(directory)
        val catalogue = json.decodeFromString<List<HadithEdition>>(base.assets.open("hadith/catalogue.json").bufferedReader().use { it.readText() })
        val edition = catalogue.single { it.collection == "nawawi" && it.language == "en" }
        val lines = base.assets.open(edition.bundledAsset).bufferedReader().useLines { seq -> seq.map { json.decodeFromString<HadithPackageLine>(it) }.toList() }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("hadith-content.db"), null).use { db ->
            db.execSQL("CREATE TABLE editions(collection TEXT NOT NULL,language TEXT NOT NULL,metadata TEXT NOT NULL,PRIMARY KEY(collection,language))")
            db.execSQL("CREATE TABLE chapters(collection TEXT NOT NULL,id TEXT NOT NULL,name TEXT NOT NULL,total INTEGER NOT NULL,position INTEGER NOT NULL,PRIMARY KEY(collection,id))")
            db.execSQL("CREATE TABLE texts(collection TEXT NOT NULL,language TEXT NOT NULL,id TEXT NOT NULL,chapter TEXT NOT NULL,number TEXT NOT NULL,arabic TEXT NOT NULL,translation TEXT NOT NULL,reference TEXT NOT NULL,grade TEXT NOT NULL,narrator TEXT NOT NULL,isnad TEXT NOT NULL,position INTEGER NOT NULL,search TEXT NOT NULL,PRIMARY KEY(collection,language,id))")
            db.execSQL("INSERT INTO editions VALUES(?,?,?)", arrayOf("nawawi", "en", json.encodeToString(edition)))
            lines.mapNotNull { it.chapter }.forEach { db.execSQL("INSERT INTO chapters VALUES(?,?,?,?,?)", arrayOf<Any>("nawawi", it.id, it.name, it.count, 0)) }
            lines.mapNotNull { it.hadith }.forEachIndexed { index, h ->
                db.insertOrThrow("texts", null, ContentValues().apply {
                    put("collection", "nawawi"); put("language", "en"); put("id", h.id); put("chapter", h.chapter); put("number", h.number)
                    put("arabic", h.arabic); put("translation", h.translation); put("reference", h.reference); put("grade", h.grade); put("narrator", h.narrator); put("isnad", h.isnad); put("position", index)
                    put("search", foldHadith("${h.arabic} ${h.translation} ${h.reference} ${h.number}"))
                })
            }
            db.version = 1
        }
        val prefs = HadithPreferences(bookmarks = listOf(HadithBookmark("nawawi", "1", edition.name, "Existing reference")), lastRead = mapOf("nawawi" to "1"), language = "fr", arabicSize = 30)
        File(context.filesDir, "datastore/hadith-user.json").apply { parentFile!!.mkdirs(); writeText(json.encodeToString(prefs)) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repo = HadithRepository(context, OkHttpClient(), json, scope)
            assertEquals(42, repo.search("", "nawawi", "", "en", 100).size)
            assertTrue(repo.search("motives", "nawawi", "", "en", 50).any { it.text.id == "1" })
            assertEquals(lines.mapNotNull { it.hadith }.first().arabic, repo.read("nawawi", "1", "en")!!.text.arabic)
            withTimeout(5000) { repo.preferences.first { it.bookmarks.isNotEmpty() } }
            assertEquals(prefs, repo.preferences.value)
        } finally { scope.cancel(); directory.deleteRecursively() }
    }
}
