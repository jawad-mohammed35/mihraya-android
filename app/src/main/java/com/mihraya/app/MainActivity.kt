package com.mihraya.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mihraya.app.core.designsystem.theme.LocalHapticsEnabled
import com.mihraya.app.core.designsystem.theme.LocalUse24Hour
import com.mihraya.app.core.designsystem.theme.MihrayaTheme
import com.mihraya.app.core.designsystem.theme.isDarkTheme
import com.mihraya.app.ui.LaunchRequest
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.MihrayaRoot
import com.mihraya.app.widget.Destinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val launches = MutableStateFlow(LaunchRequest())
    private var launchPending = false
    private var startedFromShortcut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = appContainer
        val createdAt = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            container.settings.state.value == null ||
                (launchPending && SystemClock.uptimeMillis() - createdAt < MAX_LAUNCH_HOLD_MS)
        }
        enableEdgeToEdge()
        clearSystemBarScrim()
        if (savedInstanceState == null) {
            if (intent?.getBooleanExtra(Destinations.KEEP_TASBIH, false) != true) container.library.startFreshTasbih()
            startedFromShortcut = intent?.getBooleanExtra(Destinations.FROM_SHORTCUT, false) == true
            launchPending = handleIntent(intent)
        } else {
            startedFromShortcut = savedInstanceState.getBoolean(STATE_FROM_SHORTCUT)
        }

        setContent {
            val settings by container.settings.state.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val dark = isDarkTheme(current.theme)

            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
                clearSystemBarScrim()
                onDispose { }
            }

            MihrayaTheme(mode = current.theme, accent = current.accent, pureBlack = current.pureOledBlack) {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalHapticsEnabled provides current.haptics,
                    LocalUse24Hour provides current.use24Hour,
                ) {
                    MihrayaRoot(
                        settings = current,
                        launches = launches,
                        onLaunchHandled = { launchPending = false },
                        backgroundOnHomeBack = startedFromShortcut,
                    )
                }
            }
        }

        lifecycleScope.launch { container.refreshAutomaticLocation() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FROM_SHORTCUT, startedFromShortcut)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Returns whether the intent asked for a destination. */
    private fun handleIntent(intent: Intent?): Boolean {
        val player = intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true
        val dest = intent?.getStringExtra(Destinations.EXTRA)
        if (!player && dest == null) return false
        if (player) intent?.removeExtra(EXTRA_OPEN_PLAYER)
        val current = launches.value
        launches.value = LaunchRequest(
            nonce = current.nonce + 1,
            player = player || dest == Destinations.PLAYER,
            dest = dest,
            surah = intent?.getIntExtra(Destinations.SURAH, 1) ?: 1,
            ayah = intent?.getIntExtra(Destinations.AYAH, 1) ?: 1,
        )
        return true
    }

    override fun onStart() {
        super.onStart()
        appContainer.player.connect()
    }

    override fun onStop() {
        appContainer.player.disconnect()
        super.onStop()
    }

    private fun clearSystemBarScrim() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
        private const val MAX_LAUNCH_HOLD_MS = 1_500L
        private const val STATE_FROM_SHORTCUT = "from_shortcut"
    }
}
