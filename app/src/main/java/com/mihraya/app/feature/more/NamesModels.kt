package com.mihraya.app.feature.more

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer
import kotlin.random.Random

@Serializable
data class AllahName(val number: Int, val arabic: String, val transliteration: String, val meaning: String) {
    fun shareText() = "$number. $arabic\n$transliteration\n$meaning\n\n99 Names of Allah · Mihraya\nSource: my-prayers/muslim-data (Apache-2.0). Enumerations and English renderings vary."
}
object NamesData {
    fun load(context: Context): List<AllahName> = context.assets.open("names/names.json").bufferedReader().use {
        Json.decodeFromString<List<AllahName>>(it.readText().removePrefix("\uFEFF"))
    }.also(::validate)
    fun validate(names: List<AllahName>) {
        require(names.size == 99 && names.map { it.number } == (1..99).toList())
        require(names.map { it.arabic }.distinct().size == 99)
        require(names.all { it.arabic.isNotBlank() && it.transliteration.isNotBlank() && it.meaning.isNotBlank() && '\uFFFD' !in it.arabic })
    }
    fun fold(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase().replace("ٱ", "ا").replace(Regex("[^\\p{L}\\p{N}]"), "")
}
enum class Direction(val label: String) { ArabicToEnglish("Arabic → English"), EnglishToArabic("English → Arabic"), Mixed("Mixed") }
@Serializable
data class QuizQuestion(val nameId: Int, val arabicPrompt: Boolean, val choices: List<Int>) {
    fun correct(answerId: Int) = answerId == nameId
}
object NamesGames {
    fun session(names: List<AllahName>, count: Int, randomized: Boolean, random: Random = Random.Default): List<Int> {
        require(count in listOf(5, 10, 20, 50, 99))
        return (if (randomized) names.shuffled(random) else names).take(count).map { it.number }
    }
    fun questions(names: List<AllahName>, ids: List<Int>, direction: Direction, random: Random = Random.Default): List<QuizQuestion> = ids.map { id ->
        val prompt = when (direction) { Direction.ArabicToEnglish -> true; Direction.EnglishToArabic -> false; Direction.Mixed -> random.nextBoolean() }
        val correct = names.first { it.number == id }
        // Some Names share an English rendering. Never offer two identical displayed answers.
        val distractors = names.filter { it.number != id && (prompt.not() || it.meaning != correct.meaning) }
            .distinctBy { if (prompt) it.meaning else it.arabic }.shuffled(random).take(3).map { it.number }
        require(distractors.size == 3)
        QuizQuestion(id, prompt, (distractors + id).shuffled(random))
    }
}
@Serializable
data class LearningProgress(val quizzes: Int = 0, val bestPercent: Int = 0, val matches: Int = 0, val recent: List<Int> = emptyList(), val recorded: List<String> = emptyList()) {
    fun record(sessionId: String, ids: List<Int>, matching: Boolean, correct: Int): LearningProgress {
        if (sessionId in recorded) return this
        return copy(quizzes = quizzes + if (matching) 0 else 1,
            bestPercent = if (matching) bestPercent else maxOf(bestPercent, correct * 100 / ids.size),
            matches = matches + if (matching) correct else 0,
            recent = (ids.reversed() + recent).distinct().take(10), recorded = (recorded + sessionId).takeLast(100))
    }
}
/** Separate storage: never modifies the app's existing settings, bookmarks, downloads, or history. */
class MoreStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("more_tools_v1", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    fun loadZakat() = runCatching { json.decodeFromString<ZakatInput>(prefs.getString("zakat", null) ?: "") }.getOrDefault(ZakatInput(method = method(), currency = currency()))
    fun initialZakat(): ZakatInput = loadZakat().let {
        if (it.currency == currency()) it.copy(method = method()) else ZakatInput(method = method(), currency = currency())
    }
    fun saveZakat(input: ZakatInput) { prefs.edit().putString("zakat", json.encodeToString(ZakatInput.serializer(), input)).putLong("saved_at", System.currentTimeMillis()).apply(); preference(input) }
    fun preference(input: ZakatInput) { prefs.edit().putString("method", input.method.name).putString("currency", input.currency).apply() }
    fun method() = runCatching { Nisab.valueOf(prefs.getString("method", "Silver")!!) }.getOrDefault(Nisab.Silver)
    fun currency() = prefs.getString("currency", "USD") ?: "USD"
    fun savedAt() = prefs.getLong("saved_at", 0)
    fun progress() = runCatching { json.decodeFromString<LearningProgress>(prefs.getString("progress", null) ?: "") }.getOrDefault(LearningProgress())
    fun record(sessionId: String, ids: List<Int>, matching: Boolean, correct: Int): LearningProgress = synchronized(lock) {
        progress().record(sessionId, ids, matching, correct).also { prefs.edit().putString("progress", json.encodeToString(LearningProgress.serializer(), it)).apply() }
    }
    companion object { private val lock = Any() }
}
