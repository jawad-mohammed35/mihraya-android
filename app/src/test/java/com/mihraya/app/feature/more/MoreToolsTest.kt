package com.mihraya.app.feature.more

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.io.File
import kotlinx.serialization.json.Json
import kotlin.random.Random

class MoreToolsTest {
    @Test fun namesFontFallbackAndMetricSpacing() {
        assertEquals(NamesArabicFont.Amiri, NamesArabicFont.fromStored(null))
        assertEquals(NamesArabicFont.Amiri, NamesArabicFont.fromStored("unsupported"))
        NamesArabicFont.entries.forEach { assertEquals(it, NamesArabicFont.fromStored(it.name)) }
        assertTrue(NamesArabicFont.Nastaliq.lineSpacing >= 2.5f)
        assertTrue(NamesArabicFont.Amiri.lineSpacing >= 1.758f)
        assertTrue(NamesArabicFont.NotoNaskh.lineSpacing >= 1.703f)
    }
    private val base = ZakatInput(goldPrice = "100", silverPrice = "1", hawl = true)
    private fun equal(expected: String, actual: BigDecimal) { assertEquals(0, BigDecimal(expected).compareTo(actual)) }
    private fun result(input: ZakatInput) = requireNotNull(ZakatCalculator.calculate(input))
    private fun names(): List<AllahName> = Json.decodeFromString(File("src/main/assets/names/names.json").readText().removePrefix("\uFEFF"))
    @Test fun allAssetCategoriesAndDeductions() {
        val r = result(base.copy(cash = "1000", bank = "2000", gold = "10", silver = "200", investments = "3000", business = "4000", receivables = "500", other = "600", deductions = "1300"))
        equal("12300", r.assets); equal("1300", r.deductions); equal("11000", r.net); equal("275", r.due)
    }
    @Test fun nisabStandardsAndExactBoundary() {
        equal("8748", result(base.copy(method = Nisab.Gold)).nisab)
        equal("612.36", result(base).nisab)
        assertFalse(result(base.copy(cash = "612.35")).meetsNisab)
        assertTrue(result(base.copy(cash = "612.36")).meetsNisab)
        equal("15.31", result(base.copy(cash = "612.36")).due)
        equal("0", result(base.copy(cash = "1000", method = Nisab.Gold)).due)
        equal("25", result(base.copy(cash = "1000", method = Nisab.Silver)).due)
    }
    @Test fun hawlZeroAndExcessDeductions() {
        equal("0", result(base).due)
        equal("0", result(base.copy(cash = "10000", hawl = false)).due)
        val r = result(base.copy(cash = "1000", deductions = "2000"))
        equal("0", r.net); equal("0", r.due); assertFalse(r.meetsNisab)
    }
    @Test fun weightsPurityAndJewelryAssumptions() {
        equal("7500", result(base.copy(gold = "100", goldKarat = 18)).assets)
        equal("2200", result(base.copy(gold = "2400", goldKarat = 22, goldPrice = "1")).assets)
        equal("3110.34768", result(base.copy(gold = "1", unit = MetalUnit.TroyOunces)).assets)
        equal("1166.38038", result(base.copy(gold = "1", unit = MetalUnit.Tola)).assets)
        equal("1200", result(base.copy(jewelryGold = "10", jewelrySilver = "200")).assets)
        equal("0", result(base.copy(jewelryGold = "10", jewelrySilver = "200", includeJewelry = false)).assets)
    }
    @Test fun invalidAmountsAndMissingRequiredPricesNeverSilentlyBecomeZero() {
        for (bad in listOf("-1", "NaN", "Infinity", "1e9", "1,000", "12.1234567", "9999999999999999", ".", "abc")) {
            assertNull(bad, ZakatCalculator.calculate(base.copy(bank = bad)))
        }
        assertNull(ZakatCalculator.calculate(base.copy(silverPrice = "")))
        assertNull(ZakatCalculator.calculate(base.copy(goldPrice = "", gold = "1")))
        assertNotNull(ZakatCalculator.calculate(base.copy(goldPrice = "")))
        assertNull(ZakatCalculator.calculate(base.copy(goldKarat = 0)))
    }
    @Test fun decimalPrecisionAndLargeValues() {
        equal("25000000000000.00", result(base.copy(cash = "999999999999999.99")).due)
        equal("25.00", result(base.copy(cash = "1000.10", deductions = "0.10")).due)
    }
    @Test fun inputSerializationPreservesAllPreferencesAndValues() {
        val input = base.copy(cash = "1234.56", method = Nisab.Gold, currency = "EUR", unit = MetalUnit.Tola, goldKarat = 22, includeJewelry = false, goldSource = MetalPrices.SOURCE, goldUpdated = 12345)
        assertEquals(input, Json.decodeFromString<ZakatInput>(Json.encodeToString(ZakatInput.serializer(), input)))
    }
    @Test fun bundledNamesAreCompleteAndSearchIgnoresMarks() {
        val all = names(); NamesData.validate(all)
        assertEquals("Allāh", all.first().transliteration)
        assertEquals("Aṣ-Ṣabūr", all.last().transliteration)
        assertEquals("الرحمن", NamesData.fold("ٱلرَّحْمَٰنُ"))
        assertEquals(99, all.map { it.meaning }.distinct().size)
        all.forEach { assertTrue(it.shareText().contains(it.arabic)); assertTrue(it.shareText().contains(it.meaning)) }
    }
    @Test fun everyGameSizeAndDirectionHaveUniqueQuestionsAndExactlyFourValidChoices() {
        val all = names()
        for (seed in 0..19) for (count in listOf(5, 10, 20, 50, 99)) for (direction in Direction.entries) {
            val ids = NamesGames.session(all, count, true, Random(seed))
            assertEquals(count, ids.size); assertEquals(count, ids.distinct().size)
            val questions = NamesGames.questions(all, ids, direction, Random(seed))
            assertEquals(ids, questions.map { it.nameId })
            questions.forEach { q ->
                assertEquals(4, q.choices.size); assertEquals(4, q.choices.distinct().size)
                assertEquals(1, q.choices.count(q::correct))
                assertTrue(q.choices.all { id -> all.any { it.number == id } })
                assertEquals(4, q.choices.map { id -> all.first { it.number == id }.let { if (q.arabicPrompt) it.meaning else it.arabic } }.distinct().size)
                if (direction == Direction.ArabicToEnglish) assertTrue(q.arabicPrompt)
                if (direction == Direction.EnglishToArabic) assertFalse(q.arabicPrompt)
            }
            val rounds = ids.chunked(5)
            assertTrue(rounds.all { it.size in 1..5 }); assertEquals(ids, rounds.flatten())
        }
        assertEquals((1..20).toList(), NamesGames.session(all, 20, false))
    }
    @Test fun scoringAndProgressAreAccurateAndIdempotent() {
        val ids = (1..10).toList()
        var progress = LearningProgress().record("quiz1", ids, false, 7)
        assertEquals(1, progress.quizzes); assertEquals(70, progress.bestPercent)
        assertEquals(progress, progress.record("quiz1", ids, false, 7))
        progress = progress.record("quiz2", ids, false, 3).record("match1", listOf(50), true, 1)
        assertEquals(2, progress.quizzes); assertEquals(70, progress.bestPercent); assertEquals(1, progress.matches)
        assertEquals(50, progress.recent.first()); assertEquals(10, progress.recent.size)
        assertEquals(progress, Json.decodeFromString<LearningProgress>(Json.encodeToString(LearningProgress.serializer(), progress)))
    }
    @Test fun marketQuotesValidateCurrencySymbolAndFreshness() {
        val now = java.time.Instant.parse("2026-11-16T15:00:00Z").toEpochMilli()
        val body = """{"symbol":"XAU","currency":"USD","price":3110.34768,"updatedAt":"2026-11-16T14:59:00Z"}"""
        equal("100", MetalPrices.parse(body, "XAU", now).pricePerGram.toBigDecimal())
        assertTrue(MetalPrices.isFresh(now - 60000, now))
        assertFalse(MetalPrices.isFresh(now - 86400001, now))
        assertFalse(MetalPrices.isFresh(now + 1, now))
        for (invalid in listOf(body.replace("USD", "EUR"), body.replace("XAU", "XAG"), body.replace("3110.34768", "-1"), body.replace("14:59:00", "16:00:00"))) {
            assertTrue(runCatching { MetalPrices.parse(invalid, "XAU", now) }.isFailure)
        }
    }
}
