package com.mihraya.app.data.hadith

import kotlinx.serialization.Serializable
import java.text.Normalizer

/** Reviewed source editions with explicit attribution and rights limitations. */
@Serializable
data class HadithEdition(
    val collection: String,
    val name: String,
    val description: String,
    val language: String = "en",
    val languageName: String = "English",
    val rtl: Boolean = false,
    val revision: String,
    val source: String,
    val attribution: String,
    val license: String,
    val numbering: String,
    val verification: String,
    val url: String,
    val sha256: String,
    val bytes: Long,
    val count: Int,
    val chapters: Int,
    val bundledAsset: String = "",
    val translatedCount: Int = count,
    val compression: String = "none",
    val uncompressedBytes: Long = bytes,
) {
    val key get() = "$collection:$language"
    fun validate() {
        require(collection.matches(Regex("[a-z0-9-]{1,60}")))
        require(language.matches(Regex("[a-z]{2,3}(-[A-Za-z0-9]+)?")))
        require(listOf(name, revision, attribution, license, numbering, verification).all { it.isNotBlank() })
        require(source.startsWith("https://") && url.startsWith("https://"))
        require(sha256.matches(Regex("[a-f0-9]{64}")))
        require(bytes in 1..MAX_PACKAGE_BYTES && count in 1..100_000 && chapters in 1..10_000)
        require(translatedCount in 0..count)
        require(compression in listOf("none", "gzip") && uncompressedBytes in 1..MAX_PACKAGE_BYTES)
        require(compression != "none" || uncompressedBytes == bytes)
        require(bundledAsset.isBlank() || bundledAsset.matches(Regex("hadith/[a-z0-9-]+\\.jsonl")))
    }
}

const val MAX_PACKAGE_BYTES = 100L * 1024 * 1024

@Serializable
data class HadithChapter(val id: String, val name: String, val count: Int, val arabicName: String = "")

/** JSONL lines: header, chapters, then records. No inferred references or grades. */
@Serializable
data class HadithPackageLine(
    val type: String,
    val schema: Int = 1,
    val edition: String = "",
    val revision: String = "",
    val chapter: HadithChapter? = null,
    val hadith: HadithText? = null,
)

@Serializable
data class HadithText(
    val id: String,
    val chapter: String,
    val number: String,
    val arabic: String,
    val translation: String,
    val reference: String,
    val grade: String = "",
    val narrator: String = "",
    val isnad: String = "",
    val translationAvailable: Boolean = true,
    val heading: String = "",
    val headingArabic: String = "",
) {
    fun validate() {
        require(listOf(id, chapter, number, arabic, reference).all { it.isNotBlank() })
        require(if (translationAvailable) translation.isNotBlank() else translation.isEmpty())
        require(arabic.any { it in '\u0621'..'\u064a' })
        require('\ufffd' !in arabic && '\ufffd' !in translation) { "Damaged source text" }
    }
}

@Serializable
data class HadithBookmark(val collection: String, val id: String, val name: String, val reference: String)

@Serializable
data class HadithPreferences(
    val bookmarks: List<HadithBookmark> = emptyList(),
    val lastRead: Map<String, String> = emptyMap(),
    val language: String = "en",
    val arabicSize: Int = 26,
    val translationSize: Int = 18,
)

data class HadithHit(val collection: String, val name: String, val text: HadithText, val language: String)

fun foldHadith(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "").replace("ـ", "").lowercase(java.util.Locale.ROOT)

/** A bounded no-repeat window, independent of collection numbering and gaps. */
class HadithRandomHistory(private val capacity: Int = 30) {
    private val recent = ArrayDeque<String>()
    fun exclusions(total: Int): List<String> = recent.takeLast((total - 1).coerceAtLeast(0))
    fun record(key: String) {
        recent.remove(key)
        recent.addLast(key)
        while (recent.size > capacity) recent.removeFirst()
    }
}

/** Structural checks supplement, and never replace, the editorial audit in the catalogue. */
class HadithPackageValidator(private val edition: HadithEdition) {
    private var header = false
    private val chapters = linkedMapOf<String, HadithChapter>()
    private val ids = hashSetOf<String>()
    private val counts = mutableMapOf<String, Int>()
    private var translated = 0
    fun accept(line: HadithPackageLine) {
        if (!header) {
            require(line.type == "header" && line.schema == 1 && line.edition == edition.key && line.revision == edition.revision)
            header = true
            return
        }
        when (line.type) {
            "chapter" -> {
                require(ids.isEmpty())
                val c = requireNotNull(line.chapter)
                require(c.id.isNotBlank() && c.name.isNotBlank() && c.count > 0)
                require(chapters.put(c.id, c) == null && chapters.size <= edition.chapters)
            }
            "hadith" -> {
                val h = requireNotNull(line.hadith)
                h.validate()
                if (h.translationAvailable) translated++
                require(h.chapter in chapters && ids.add(h.id) && ids.size <= edition.count)
                counts[h.chapter] = (counts[h.chapter] ?: 0) + 1
                require(counts.getValue(h.chapter) <= chapters.getValue(h.chapter).count)
            }
            else -> error("Unknown or repeated package header")
        }
    }
    fun finish() {
        require(header && ids.size == edition.count && chapters.size == edition.chapters)
        require(translated == edition.translatedCount)
        require(chapters.values.all { counts[it.id] == it.count })
    }
}
