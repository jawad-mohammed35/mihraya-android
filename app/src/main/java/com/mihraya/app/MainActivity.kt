package com.mihraya.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.MihrayaRoot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val openPlayerRequests = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = appContainer
        splash.setKeepOnScreenCondition { container.settings.state.value == null }
        enableEdgeToEdge()
        clearSystemBarScrim()
        handleIntent(intent)

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
                    MihrayaRoot(settings = current, openPlayerRequests = openPlayerRequests)
                }
            }
        }

        lifecycleScope.launch { container.refreshAutomaticLocation() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) {
            intent.removeExtra(EXTRA_OPEN_PLAYER)
            openPlayerRequests.value += 1
        }
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
    }
}
