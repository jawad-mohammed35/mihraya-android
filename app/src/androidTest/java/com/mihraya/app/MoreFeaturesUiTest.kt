package com.mihraya.app.feature.more

import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.mihraya.app.MihrayaApp
import com.mihraya.app.core.designsystem.theme.*
import com.mihraya.app.data.settings.*
import com.mihraya.app.ui.LocalAppContainer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class MoreFeaturesUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun prepareWindow() { compose.activity.runOnUiThread { compose.activity.actionBar?.hide(); compose.activity.enableEdgeToEdge() } }
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val app get() = context.applicationContext as MihrayaApp
    private val isolated by lazy {
        object : ContextWrapper(compose.activity) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = context.getSharedPreferences("qa_$name", mode)
        }
    }
    private fun content(body: @Composable () -> Unit) {
        isolated.getSharedPreferences("more_tools_v1", Context.MODE_PRIVATE).edit().clear().commit()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated, LocalAppContainer provides app.container) {
                MihrayaTheme(ThemeMode.Dark, AccentColor.Emerald, true) {
                    Box(Modifier.fillMaxSize().background(Mihraya.colors.background)) { body() }
                }
            }
        }
    }
    private fun text(text: String) = compose.onNodeWithText(text, substring = true)
    private fun scrollClick(label: String) { text(label).performScrollTo().performClick() }
    private fun input(label: String, value: String) { compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextReplacement(value) }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(350) // Let Android window and IME transitions finish before PixelCopy.
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "qa/$name.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun moreHeaderAlignmentAndFeatureNavigation() {
        var destination: Any? = null
        var backed = false
        content { com.mihraya.app.feature.more.MoreScreen({ destination = it }, { backed = true }) }
        compose.waitUntil(10000) { compose.onAllNodesWithText("More").fetchSemanticsNodes().isNotEmpty() }
        val bounds = compose.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        val density = context.resources.displayMetrics.density
        assertTrue("Back starts near left edge", bounds.left / density <= 24)
        assertTrue("48 dp touch target", bounds.width / density >= 47.9)
        screenshot("more-oled")
        text("Zakat Calculator").performClick()
        assertEquals(com.mihraya.app.ui.ZakatRoute, destination)
        text("99 Names of Allah").performClick()
        assertEquals(com.mihraya.app.ui.NamesRoute, destination)
        compose.onNodeWithContentDescription("Back").performClick(); assertTrue(backed)
    }
    @Test fun namesSearchPopupAndClipboard() {
        content { NamesScreen({}, {}) }
        val names = NamesData.load(context)
        text(names.first().transliteration).performClick()
        compose.onNode(isDialog()).assertExists()
        screenshot("name-dialog-oled")
        text("Copy").performClick()
        compose.runOnIdle {
            val clip = context.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString()
            assertTrue(clip.contains(names.first().arabic)); assertTrue(clip.contains(names.first().meaning))
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(android.content.IntentFilter(android.content.Intent.ACTION_CHOOSER), android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null), true)
        try { text("Share").performClick(); assertEquals(1, monitor.hits) } finally { instrumentation.removeMonitor(monitor) }
        text("Close").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("رحمن")
        text("Ar-Raḥmān").assertExists()
        compose.onAllNodesWithText("Allāh", substring = false).assertCountEquals(0)
        screenshot("names-search-oled")
    }
    @Test fun namesFontOptionsPersistAndRenderTheSameTextInListAndPopup() {
        content { NamesScreen({}, {}) }
        val name = NamesData.load(context).first()
        text("Source: my-prayers").assertDoesNotExist()
        text("Asma ul Husna ·").assertDoesNotExist()
        for (font in NamesArabicFont.entries) {
            text("Arabic font ·").performScrollTo().performClick()
            compose.onNodeWithText(font.label, substring = false).performClick()
            text("Arabic font · ${font.label}").assertExists()
            compose.runOnIdle { assertEquals(font, MoreStore(isolated).namesFont()) }
            compose.onNodeWithText("1 · ${name.transliteration}").performScrollTo().performClick()
            compose.onNode(hasText(name.arabic, substring = false) and hasAnyAncestor(isDialog())).assertIsDisplayed()
            val pixels = compose.onNode(hasText("Copy", substring = false) and hasAnyAncestor(isDialog())).captureToImage().toPixelMap()
            var ink = 0
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) if (pixels[x, y].luminance() > 0.5f) ink++
            assertTrue("The Copy label must be painted, not only present in semantics", ink > 10)
            screenshot("name-font-${font.name}")
            text("Close").performScrollTo().performClick()
        }
        compose.runOnIdle {
            val store = MoreStore(isolated)
            store.saveZakat(ZakatInput(cash = "2345", silverPrice = "1", hawl = true))
            store.record("font-qa-progress", listOf(1, 2, 3, 4, 5), false, 4)
            store.setNamesFont(NamesArabicFont.Nastaliq)
            val reopened = MoreStore(isolated)
            assertEquals(NamesArabicFont.Nastaliq, reopened.namesFont())
            assertEquals("2345", reopened.loadZakat().cash)
            assertEquals(80, reopened.progress().bestPercent)
        }
    }
    @Test fun nastaliqLongestNameIsReadableAtLargeFontScale() {
        content {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                Box(Modifier.width(320.dp).fillMaxHeight()) { NamesScreen({}, {}) }
            }
        }
        compose.runOnIdle { MoreStore(isolated).setNamesFont(NamesArabicFont.Nastaliq) }
        val name = NamesData.load(context)[84]
        compose.onNode(hasSetTextAction()).performTextInput(name.transliteration)
        compose.onNodeWithText("85 · ${name.transliteration}").performClick()
        text("Share").performScrollTo().assertIsDisplayed()
        screenshot("name-nastaliq-long-large")
        text("Close").performScrollTo().performClick()
    }
    @Test fun zakatManualCalculationSaveResetAndRestore() {
        content { ZakatScreen({}) }
        input("Silver price / gram", "1")
        input("Cash on hand", "1000")
        scrollClick("Hawl requirement has been met")
        text("Estimated Zakat due: $25.00").performScrollTo().assertIsDisplayed()
        text("Guidance & sources").assertDoesNotExist()
        text("Zakat is an obligation").assertDoesNotExist()
        screenshot("zakat-result-oled")
        scrollClick("Save calculation locally")
        compose.runOnIdle { assertEquals("1000", MoreStore(isolated).loadZakat().cash); assertTrue(MoreStore(isolated).loadZakat().hawl) }
        scrollClick("Reset inputs")
        compose.onNodeWithText("Reset", substring = false).performClick()
        text("Enter a positive price").performScrollTo().assertIsDisplayed()
        scrollClick("Load saved calculation")
        text("Estimated Zakat due: $25.00").performScrollTo().assertIsDisplayed()
        text("Your annual estimate").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("$25.00", substring = false).assertIsDisplayed()
    }
    @Test fun quizFourChoicesFeedbackScoreAndSavedProgress() {
        content { NamesLearnScreen({}) }
        scrollClick("5 Names")
        scrollClick("Arabic → English")
        scrollClick("Randomized order")
        scrollClick("Multiple Choice Quiz")
        val names = NamesData.load(context)
        repeat(5) { index ->
            val correct = names[index].meaning
            val choices = names.filter { compose.onAllNodesWithText(it.meaning, substring = false).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(4, choices.size)
            val choice = if (index == 0) choices.first { it.meaning != correct }.meaning else correct
            text(choice).performScrollTo().performClick()
            text("✓ Correct · $correct").assertExists()
            text("Correct: $index · Incorrect: 1").assertExists()
            if (index == 0) screenshot("quiz-feedback-oled")
            scrollClick(if (index == 4) "See results" else "Next question")
        }
        text("4 / 5 · 80%").assertExists()
        compose.waitForIdle()
        compose.runOnIdle { val progress = MoreStore(isolated).progress(); assertEquals(1, progress.quizzes); assertEquals(80, progress.bestPercent) }
        screenshot("quiz-complete-oled")
    }
    @Test fun matchingTenNamesAcrossRoundsAndProgress() {
        content { NamesLearnScreen({}) }
        scrollClick("Arabic → English")
        scrollClick("Randomized order")
        scrollClick("Flashcard Matching")
        val names = NamesData.load(context)
        text(names[0].arabic).performScrollTo().performClick()
        text(names[1].meaning).performScrollTo().performClick()
        text("Not a match. Try again.").assertExists()
        compose.waitUntil(5000) { compose.onAllNodes(hasText(names[0].arabic) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        repeat(10) { index ->
            text(names[index].arabic).performScrollTo().performClick()
            text(names[index].meaning).performScrollTo().performClick()
            compose.waitForIdle()
            if (index == 0) screenshot("matching-oled")
            if (index == 4) scrollClick("Next round")
        }
        text("10 / 10 matched").assertExists()
        text("Incorrect attempts: 1").assertExists()
        compose.runOnIdle { assertEquals(10, MoreStore(isolated).progress().matches) }
    }
    @Test fun preferencesAndProgressPersistWithoutTouchingExistingData() {
        val prefs = isolated.getSharedPreferences("more_tools_v1", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = MoreStore(isolated)
        store.saveZakat(ZakatInput(cash = "123.45", currency = "EUR", method = Nisab.Gold))
        store.record("qa-quiz", (1..5).toList(), false, 4)
        val reopened = MoreStore(isolated)
        assertEquals("123.45", reopened.loadZakat().cash)
        assertEquals(Nisab.Gold, reopened.method()); assertEquals("EUR", reopened.currency())
        assertEquals(80, reopened.progress().bestPercent)
        store.preference(ZakatInput(method = Nisab.Silver, currency = "GBP"))
        assertEquals("GBP", reopened.initialZakat().currency)
        assertEquals("", reopened.initialZakat().cash)
        assertEquals("EUR", reopened.loadZakat().currency)
    }
    @Test fun longestNameDialogRemainsScrollableAtLargeFontOnNarrowContent() {
        content {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                Box(Modifier.width(320.dp).fillMaxHeight()) { NamesScreen({}, {}) }
            }
        }
        val name = NamesData.load(context)[84]
        compose.onNode(hasSetTextAction()).performTextInput(name.transliteration)
        compose.onNodeWithText("85 · ${name.transliteration}").performClick()
        text("Share").performScrollTo().assertIsDisplayed()
        screenshot("long-name-large-font")
        text("Close").performScrollTo().performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
    }
    @Test fun marketRefreshShowsProviderTimestampOrExplicitManualFallback() {
        content { ZakatScreen({}) }
        scrollClick("Refresh market quotes")
        compose.waitUntil(25000) {
            compose.onAllNodesWithText("Quotes received.", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Market pricing unavailable.", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        scrollClick("Save calculation locally")
        compose.runOnIdle {
            val saved = MoreStore(isolated).loadZakat()
            if (saved.goldSource == MetalPrices.SOURCE) {
                assertTrue(saved.goldPrice.toBigDecimal() > java.math.BigDecimal.ZERO)
                assertTrue(saved.silverPrice.toBigDecimal() > java.math.BigDecimal.ZERO)
                assertTrue(saved.goldUpdated > 0 && saved.silverUpdated > 0)
                android.util.Log.i("MorePricingQA", "Provider quotes parsed: gold=${saved.goldPrice}, silver=${saved.silverPrice}, goldTimestamp=${saved.goldUpdated}, silverTimestamp=${saved.silverUpdated}")
            } else {
                assertEquals("", saved.goldPrice)
                android.util.Log.i("MorePricingQA", "Network unavailable; explicit manual fallback verified")
            }
        }
    }
    @Test fun namesAndDialogsRenderInEveryThemeAndAccent() {
        val mode = mutableStateOf(ThemeMode.Light)
        val accent = mutableStateOf(AccentColor.Champagne)
        val oled = mutableStateOf(false)
        compose.setContent {
            MihrayaTheme(mode.value, accent.value, oled.value) {
                Box(Modifier.fillMaxSize().background(Mihraya.colors.background)) { NamesScreen({}, {}) }
            }
        }
        for ((theme, black) in listOf(ThemeMode.Light to false, ThemeMode.Dark to false, ThemeMode.Dark to true, ThemeMode.System to false)) {
            for (color in AccentColor.entries) {
                compose.runOnIdle { mode.value = theme; accent.value = color; oled.value = black }
                compose.waitForIdle()
                text("Allāh").performClick()
                text("Share").assertIsDisplayed()
                screenshot("names-${theme.name}-${black}-${color.name}")
                text("Close").performClick()
            }
        }
    }
}
