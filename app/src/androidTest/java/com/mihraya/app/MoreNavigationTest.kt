package com.mihraya.app.feature.more

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.mihraya.app.MainActivity
import com.mihraya.app.MihrayaApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Exercises actual typed navigation, rather than only screen callbacks. Debug app only. */
class MoreNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun homeMoreCalculatorNamesLearnAndHadithNavigateWithoutChangingSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as MihrayaApp
        val before = runBlocking { app.container.settings.current() }
        val expected = before.copy(onboardingDone = true, autoLocation = false, haptics = false)
        runBlocking { app.container.settings.update { expected } }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("More and settings").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithContentDescription("More and settings").performClick()
                compose.onNodeWithText("Zakat Calculator").performClick()
                compose.onNodeWithText("Your annual estimate").assertExists()
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithText("99 Names of Allah").performClick()
                compose.onNodeWithText("Learn").performClick()
                compose.onNodeWithText("Customize your session").assertExists()
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithText("Learn").assertExists()
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithText("More", substring = false).assertExists()
                compose.onNodeWithContentDescription("Back").performClick()
                compose.onNodeWithContentDescription("More and settings").assertExists()
                compose.onNodeWithText("Hadith").performClick()
                compose.onNodeWithText("Random Hadith").assertExists()
                compose.onNodeWithText("Read, reflect, and keep your place").assertDoesNotExist()
                assertEquals(expected, runBlocking { app.container.settings.current() })
            }
        } finally { runBlocking { app.container.settings.update { before } } }
    }
}
