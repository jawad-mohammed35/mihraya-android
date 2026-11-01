package com.mihraya.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mihraya.app.AppContainer
import com.mihraya.app.core.designsystem.components.fadeAbove
import com.mihraya.app.core.designsystem.components.topChrome
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.settings.AppSettings
import com.mihraya.app.feature.home.AmbientGlow
import com.mihraya.app.feature.home.HomeActions
import com.mihraya.app.feature.home.HomeScreen
import com.mihraya.app.feature.listen.ListenScreen
import com.mihraya.app.feature.listen.PlayerScreen
import com.mihraya.app.feature.more.MoreScreen
import com.mihraya.app.feature.more.TourScreen
import com.mihraya.app.feature.prayer.TimetableScreen
import com.mihraya.app.feature.qibla.QiblaScreen
import com.mihraya.app.feature.quran.QuranScreen
import com.mihraya.app.feature.quran.ReaderScreen
import com.mihraya.app.feature.settings.AboutScreen
import com.mihraya.app.feature.settings.AppearanceScreen
import com.mihraya.app.feature.settings.DownloadsScreen
import com.mihraya.app.feature.settings.LocationScreen
import com.mihraya.app.feature.settings.NotificationSettingsScreen
import com.mihraya.app.feature.settings.PrayerSettingsScreen
import com.mihraya.app.feature.settings.ReadingSettingsScreen
import com.mihraya.app.feature.settings.TranslationsScreen
import com.mihraya.app.feature.tasbih.TasbihScreen
import com.mihraya.app.feature.welcome.WelcomeScreen
import com.mihraya.app.widget.Destinations
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/**
 * [onLaunchHandled] fires once the launch request present at start-up has been acted on.
 * [backgroundOnHomeBack] makes Back on Home leave the app running, as Android does after an icon launch.
 */
@Composable
fun MihrayaRoot(
    settings: AppSettings,
    launches: StateFlow<LaunchRequest>,
    onLaunchHandled: () -> Unit,
    backgroundOnHomeBack: Boolean = false,
) {
    val container = LocalAppContainer.current
    if (!settings.onboardingDone) {
        LaunchedEffect(Unit) { onLaunchHandled() }
        WelcomeScreen(onDone = { container.settings.edit { it.copy(onboardingDone = true) } })
        return
    }

    val colors = Mihraya.colors
    val navController = rememberNavController()
    val hazeState = remember { HazeState() }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var quranScroll by remember { mutableIntStateOf(0) }
    var listenScroll by remember { mutableIntStateOf(0) }
    val playback by container.player.state.collectAsStateWithLifecycle()
    val launch by launches.collectAsStateWithLifecycle()
    val startupNonce = remember { launches.value.nonce }
    var instantEntryId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(launch.nonce) {
        if (launch.nonce == 0) {
            onLaunchHandled()
            return@LaunchedEffect
        }
        if (launch.player) playerExpanded = true
        val tab = when (launch.dest) {
            Destinations.QURAN -> TopLevel.Quran
            Destinations.LISTEN -> TopLevel.Listen
            Destinations.TASBIH -> TopLevel.Tasbih
            else -> null
        }
        when {
            tab != null -> {
                playerExpanded = false
                navController.navigateTopLevel(tab)
            }
            launch.dest == Destinations.TIMETABLE -> navController.navigate(TimetableRoute) { launchSingleTop = true }
            launch.dest == Destinations.QIBLA -> navController.navigate(QiblaRoute) { launchSingleTop = true }
            launch.dest == Destinations.READER -> navController.navigate(ReaderRoute(launch.surah, launch.ayah)) { launchSingleTop = true }
            launch.dest == Destinations.PLAYER -> playerExpanded = true
        }
        if (launch.nonce != startupNonce) return@LaunchedEffect
        // Opened straight into a destination: it replaces Home behind the splash screen, without a transition.
        instantEntryId = navController.currentBackStackEntry?.id
        repeat(3) { withFrameNanos { } }
        onLaunchHandled()
        delay(1_000)
        instantEntryId = null
    }

    val entry by navController.currentBackStackEntryAsState()
    val reading = entry?.destination?.hasRoute<ReaderRoute>() == true
    val context = LocalContext.current
    DisposableEffect(reading, playerExpanded) {
        (context as? Activity)?.requestedOrientation = if (reading && !playerExpanded) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose { }
    }
    val currentTop = TopLevel.entries.firstOrNull { top ->
        entry?.destination?.hierarchy?.any { it.hasRoute(top.route::class) } == true
    }
    val showBottomBar = currentTop != null
    val showMini = playback.hasMedia && !playerExpanded
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset by animateDpAsState(
        targetValue = navInset + (if (showBottomBar) 86.dp else 12.dp) + (if (showMini) 74.dp else 0.dp),
        animationSpec = tween(280),
        label = "bottomInset",
    )

    val onHome = entry?.destination?.hasRoute<HomeRoute>() == true
    val glow by animateFloatAsState(if (onHome) 1f else 0f, tween(300), label = "homeGlow")
    val topInset = WindowInsets.topChrome.asPaddingValues().calculateTopPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (glow > 0f) AmbientGlow(Modifier.graphicsLayer { alpha = glow })
        CompositionLocalProvider(LocalBottomInset provides bottomInset) {
            MihrayaNavHost(
                navController = navController,
                openPlayer = { playerExpanded = true },
                quranScroll = quranScroll,
                listenScroll = listenScroll,
                instantEntryId = { instantEntryId },
                modifier = Modifier
                    .fillMaxSize()
                    .fadeAbove(topInset)
                    .hazeSource(hazeState),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(
                visible = showMini,
                enter = slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(240)),
                exit = slideOutVertically(tween(260)) { it / 2 } + fadeOut(tween(200)),
            ) {
                MiniPlayer(state = playback, hazeState = hazeState, onExpand = { playerExpanded = true })
            }
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(320)) { it } + fadeIn(tween(240)),
                exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
            ) {
                BottomBar(
                    current = currentTop,
                    onSelect = { item ->
                        if (item == currentTop && item == TopLevel.Quran) {
                            quranScroll += 1
                        } else if (item == currentTop && item == TopLevel.Listen) {
                            listenScroll += 1
                        } else {
                            navController.navigateTopLevel(item)
                        }
                    },
                    hazeState = hazeState,
                )
            }
        }

        AnimatedVisibility(
            visible = playerExpanded && playback.hasMedia,
            enter = slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(340, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(300)),
        ) {
            PlayerScreen(
                onCollapse = { playerExpanded = false },
                onOpenReader = { surah ->
                    playerExpanded = false
                    navController.navigate(ReaderRoute(surah)) { launchSingleTop = true }
                },
            )
        }
    }

    BackHandler(enabled = backgroundOnHomeBack && onHome && !playerExpanded) {
        (context as? Activity)?.moveTaskToBack(true)
    }
    BackHandler(enabled = playerExpanded) { playerExpanded = false }
}

private fun NavHostController.navigateTopLevel(item: TopLevel) {
    navigate(item.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MihrayaNavHost(
    navController: NavHostController,
    openPlayer: () -> Unit,
    quranScroll: Int,
    listenScroll: Int,
    instantEntryId: () -> String?,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = {
            if (targetState.id == instantEntryId()) EnterTransition.None
            else fadeIn(tween(260)) + scaleIn(tween(300), initialScale = 0.985f)
        },
        exitTransition = { if (targetState.id == instantEntryId()) ExitTransition.None else fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.985f) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                HomeActions(
                    openReader = { surah, ayah -> navController.navigate(ReaderRoute(surah, ayah)) },
                    openTimetable = { navController.navigate(TimetableRoute) },
                    openLocation = { navController.navigate(LocationRoute) },
                    openTasbih = { navController.navigateTopLevel(TopLevel.Tasbih) },
                    openQibla = { navController.navigate(QiblaRoute) { launchSingleTop = true } },
                    openListen = { navController.navigateTopLevel(TopLevel.Listen) },
                ),
            )
        }
        composable<QuranRoute> {
            QuranScreen(
                scrollToTop = quranScroll,
                onOpenReader = { surah, ayah -> navController.navigate(ReaderRoute(surah, ayah)) },
            )
        }
        composable<ListenRoute> {
            ListenScreen(
                scrollToTop = listenScroll,
                onOpenPlayer = openPlayer,
                onOpenDownloads = { navController.navigate(DownloadsRoute) },
            )
        }
        composable<TasbihRoute> { TasbihScreen() }
        composable<MoreRoute> {
            MoreScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable<ReaderRoute> { entry ->
            val route = entry.toRoute<ReaderRoute>()
            ReaderScreen(
                surah = route.surah,
                ayah = route.ayah,
                onBack = back,
                onOpenSettings = { navController.navigate(ReadingSettingsRoute) },
                onManageTranslations = { navController.navigate(TranslationsRoute) },
            )
        }
        composable<TimetableRoute> { TimetableScreen(onBack = back) }
        composable<QiblaRoute> {
            QiblaScreen(onBack = back, onSetLocation = { navController.navigate(LocationRoute) })
        }
        composable<PrayerSettingsRoute> {
            PrayerSettingsScreen(onBack = back, onLocation = { navController.navigate(LocationRoute) })
        }
        composable<NotificationSettingsRoute> { NotificationSettingsScreen(onBack = back) }
        composable<LocationRoute> { LocationScreen(onBack = back) }
        composable<AppearanceRoute> { AppearanceScreen(onBack = back) }
        composable<ReadingSettingsRoute> {
            ReadingSettingsScreen(onBack = back, onManageTranslations = { navController.navigate(TranslationsRoute) })
        }
        composable<TranslationsRoute> { TranslationsScreen(onBack = back) }
        composable<DownloadsRoute> {
            DownloadsScreen(onBack = back, onManageTranslations = { navController.navigate(TranslationsRoute) })
        }
        composable<AboutRoute> { AboutScreen(onBack = back) }
        composable<TourRoute> { TourScreen(onDone = back) }
    }
}
