package com.mihraya.app.data.hadith

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/** Deliberately synthetic strings: these fixtures are not religious texts or production content. */
class HadithValidationTest {
    private val edition = HadithEdition("test-only", "TEST ONLY", "Synthetic test fixture", revision = "test-1",
        source = "https://example.invalid/test", attribution = "TEST ONLY", license = "TEST ONLY",
        numbering = "Synthetic IDs", verification = "Structural test only", url = "https://example.invalid/package",
        sha256 = "a".repeat(64), bytes = 100, count = 2, chapters = 1)
    private val chapter = HadithPackageLine("chapter", chapter = HadithChapter("c", "TEST ONLY", 2))
    private fun text(id: String) = HadithText(id, "c", id, "اختبار", "TEST ONLY - not a Hadith", "TEST ONLY $id")
    private fun validator() = HadithPackageValidator(edition).apply {
        accept(HadithPackageLine("header", edition = edition.key, revision = edition.revision))
        accept(chapter)
    }
    private fun add(v: HadithPackageValidator, id: String) = v.accept(HadithPackageLine("hadith", hadith = text(id)))
    private fun rejected(block: () -> Unit) {
        try { block(); fail("Invalid package accepted") } catch (expected: IllegalArgumentException) { }
    }
    @Test fun acceptsCompleteStructureWithNonconsecutiveSourceIds() {
        edition.validate()
        validator().apply { add(this, "1a"); add(this, "7"); finish() }
    }
    @Test fun rejectsTruncatedPackage() { rejected { validator().apply { add(this, "1"); finish() } } }
    @Test fun rejectsDuplicateReferencesByStableId() { rejected { validator().apply { add(this, "1"); add(this, "1") } } }
    @Test fun rejectsUnknownChapter() {
        rejected { validator().accept(HadithPackageLine("hadith", hadith = text("1").copy(chapter = "missing"))) }
    }
    @Test fun rejectsMissingArabicOrTranslation() {
        rejected { text("1").copy(arabic = "").validate() }
        rejected { text("1").copy(translation = "").validate() }
    }
    @Test fun rejectsUnicodeReplacementCharacters() {
        rejected { text("1").copy(arabic = "اختبار\ufffd").validate() }
        rejected { text("1").copy(translation = "TEST\ufffd").validate() }
    }
    @Test fun rejectsHeaderMismatch() {
        rejected { HadithPackageValidator(edition).accept(HadithPackageLine("header", edition = "other:en", revision = edition.revision)) }
    }
    @Test fun rejectsUnsupportedSchema() {
        rejected { HadithPackageValidator(edition).accept(HadithPackageLine("header", schema = 2, edition = edition.key, revision = edition.revision)) }
    }
    @Test fun rejectsOversizedOrUnpinnedOrInsecureEdition() {
        rejected { edition.copy(bytes = MAX_PACKAGE_BYTES + 1).validate() }
        rejected { edition.copy(sha256 = "").validate() }
        rejected { edition.copy(url = "http://example.invalid").validate() }
        rejected { edition.copy(license = "").validate() }
        rejected { edition.copy(verification = "").validate() }
    }
    @Test fun rejectsChapterCountMismatch() {
        rejected { HadithPackageValidator(edition).apply {
            accept(HadithPackageLine("header", edition = edition.key, revision = edition.revision))
            accept(chapter.copy(chapter = chapter.chapter!!.copy(count = 1)))
            add(this, "1"); add(this, "2"); finish()
        } }
    }
    @Test fun rejectsChapterReorderingAfterText() {
        rejected { validator().apply { add(this, "1"); accept(chapter) } }
    }
    @Test fun searchFoldHandlesArabicMarksAndTatweelAndEnglishAccents() {
        assertEquals(foldHadith("اختبار"), foldHadith("اِخْتِبَـار"))
        assertEquals("cafe", foldHadith("CAFÉ"))
    }
    @Test fun randomHistoryAlwaysLeavesOneCandidateAndBoundsMemory() {
        val history = HadithRandomHistory(3)
        history.record("a"); history.record("b"); history.record("c"); history.record("d")
        assertEquals(listOf("b", "c", "d"), history.exclusions(20))
        assertEquals(listOf("d"), history.exclusions(2))
        assertTrue(history.exclusions(1).isEmpty())
        assertTrue(history.exclusions(0).isEmpty())
    }
    @Test fun userDataRoundTripsWithoutTouchingQuranLibrary() {
        val original = HadithPreferences(listOf(HadithBookmark("test-only", "1a", "TEST ONLY", "TEST ONLY")), mapOf("test-only" to "1a"), "ur", 32, 21)
        assertEquals(original, Json.decodeFromString<HadithPreferences>(Json.encodeToString(original)))
    }
    @Test fun catalogueContainsOnlyValidatedEditionsAndNoUnreviewedTexts() {
        val file = java.io.File("src/main/assets/hadith/catalogue.json")
        val editions = Json.decodeFromString<List<HadithEdition>>(file.readText())
        editions.forEach { it.validate() }
        assertEquals(editions.size, editions.map { it.key }.distinct().size)
    }
    @Test fun bundledPackagesMatchPinnedDigestsAndPassAllStructuralChecks() {
        val assets = java.io.File("src/main/assets")
        val editions = Json.decodeFromString<List<HadithEdition>>(java.io.File(assets, "hadith/catalogue.json").readText())
        var total = 0
        editions.filter { it.bundledAsset.isNotBlank() }.forEach { edition ->
            val bytes = java.io.File(assets, edition.bundledAsset).readBytes()
            assertEquals(edition.bytes, bytes.size.toLong())
            assertEquals(edition.sha256, java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
            val validator = HadithPackageValidator(edition)
            bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.forEach {
                validator.accept(Json.decodeFromString<HadithPackageLine>(it))
            }
            validator.finish()
            total += edition.count
        }
        assertEquals(122, total)
    }
}
