package com.mihraya.app.data.hadith

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** A separate app-private directory keeps the user's Mihraya files out of these tests. */
class HadithOfflineTest {
    @Test fun bundledReadingSearchRandomBookmarksAndVerifiedTranslationInstall() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.filesDir, "hadith-instrumentation-${System.nanoTime()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(directory, "files").apply { mkdirs() }
            override fun getDatabasePath(name: String): File = File(directory, name)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?): android.database.sqlite.SQLiteDatabase =
                android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?): android.database.sqlite.SQLiteDatabase =
                android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).path, factory, errorHandler)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var corrupt = false
        val network = OkHttpClient.Builder().addInterceptor { chain ->
            val original = instrumentation.context.assets.open("hadith/nawawi-fr.jsonl").use { it.readBytes() }
            val bytes = if (corrupt) original.copyOf(original.size - 10) else original
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Test transport").body(bytes.toResponseBody()).build()
        }.build()
        val repo = HadithRepository(isolated, network, Json { ignoreUnknownKeys = true; encodeDefaults = true }, scope)
        try {
            repo.ensureBundled()
            assertNull(repo.initializationError.value)
            assertEquals(3, repo.installed().size)
            assertEquals(122, repo.search("", "", "", "en", 200).size)
            assertEquals(42, repo.search("", "nawawi", "1", "en", 100).size)
            assertEquals(42, repo.chapters("nawawi").single().count)
            val first = requireNotNull(repo.read("nawawi", "1", "en"))
            assertTrue(first.text.arabic.isNotBlank())
            assertTrue(first.text.translation.contains("Actions"))
            assertTrue(repo.search("%", "nawawi", "", "en", 100).all { '%' in it.text.translation || '%' in it.text.arabic || '%' in it.text.reference })
            assertTrue(repo.search("' OR 1=1 --", "", "", "en", 100).isEmpty())
            assertTrue(repo.search("motives", "nawawi", "", "en", 100).any { it.text.id == "1" })
            assertTrue(repo.search("الاعمال", "nawawi", "", "en", 100).isNotEmpty())
            val randomIds = (1..30).map { requireNotNull(repo.random("en")).let { it.collection + ":" + it.text.id } }
            assertEquals(30, randomIds.distinct().size)
            repo.bookmark(first).join()
            repo.markRead(first).join()
            withTimeout(5000) { repo.preferences.first { it.bookmarks.size == 1 && it.lastRead["nawawi"] == "1" } }
            val stored = File(isolated.filesDir, "datastore/hadith-user.json").readText()
            assertEquals("1", Json.decodeFromString<HadithPreferences>(stored).bookmarks.single().id)
            val french = repo.catalogue().single { it.collection == "nawawi" && it.language == "fr" }
            var percent = 0
            repo.install(french) { percent = it }
            assertEquals(100, percent)
            assertEquals("fr", repo.read("nawawi", "1", "fr")!!.language)
            assertEquals(first.text.arabic, repo.read("nawawi", "1", "fr")!!.text.arabic)
            corrupt = true
            try { repo.install(french) {}; fail("Truncated download accepted") } catch (expected: IllegalArgumentException) { }
            assertEquals("fr", repo.read("nawawi", "1", "fr")!!.language)
            repo.remove(french)
            assertEquals("en", repo.read("nawawi", "1", "fr")!!.language)
            assertEquals(1, repo.preferences.value.bookmarks.size)
            assertEquals("1", repo.preferences.value.lastRead["nawawi"])
            assertFalse(File(isolated.filesDir, "datastore/library.json").exists())
        } finally {
            scope.cancel()
            // The temporary directory is created above underneath this test's app-private files directory.
            directory.deleteRecursively()
        }
    }
}
