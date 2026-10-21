package com.mihraya.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.data.settings.AppSettings
import com.mihraya.app.feature.home.HomeActions
import com.mihraya.app.feature.home.HomeScreen
import com.mihraya.app.feature.listen.ListenScreen
import com.mihraya.app.feature.listen.PlayerScreen
import com.mihraya.app.feature.more.MoreScreen
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.StateFlow

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

@Composable
fun MihrayaRoot(settings: AppSettings, openPlayerRequests: StateFlow<Int>) {
    val container = LocalAppContainer.current
    if (!settings.onboardingDone) {
        WelcomeScreen(onDone = { container.settings.edit { it.copy(onboardingDone = true) } })
        return
    }

    val colors = Mihraya.colors
    val navController = rememberNavController()
    val hazeState = remember { HazeState() }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    val playback by container.player.state.collectAsStateWithLifecycle()
    val playerRequests by openPlayerRequests.collectAsStateWithLifecycle()

    LaunchedEffect(playerRequests) {
        if (playerRequests > 0) playerExpanded = true
    }

    val entry by navController.currentBackStackEntryAsState()
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

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        CompositionLocalProvider(LocalBottomInset provides bottomInset) {
            MihrayaNavHost(
                navController = navController,
                openPlayer = { playerExpanded = true },
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
            )
        }

        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBar + 14.dp)
                .background(
                    Brush.verticalGradient(
                        0f to colors.background,
                        statusBar / (statusBar + 14.dp) to colors.background.copy(alpha = 0.92f),
                        1f to colors.background.copy(alpha = 0f),
                    ),
                ),
        )

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
                    onSelect = { navController.navigateTopLevel(it) },
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
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { fadeIn(tween(260)) + scaleIn(tween(300), initialScale = 0.985f) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.985f) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                HomeActions(
                    openReader = { surah, ayah -> navController.navigate(ReaderRoute(surah, ayah)) },
                    openTimetable = { navController.navigate(TimetableRoute) },
                    openLocation = { navController.navigate(LocationRoute) },
                    openNotifications = { navController.navigate(NotificationSettingsRoute) },
                    openTasbih = { navController.navigate(TasbihRoute) },
                    openQibla = { navController.navigateTopLevel(TopLevel.Qibla) },
                    openListen = { navController.navigateTopLevel(TopLevel.Listen) },
                ),
            )
        }
        composable<QuranRoute> {
            QuranScreen(onOpenReader = { surah, ayah -> navController.navigate(ReaderRoute(surah, ayah)) })
        }
        composable<ListenRoute> {
            ListenScreen(onOpenPlayer = openPlayer, onOpenDownloads = { navController.navigate(DownloadsRoute) })
        }
        composable<QiblaRoute> {
            QiblaScreen(onSetLocation = { navController.navigate(LocationRoute) })
        }
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
        composable<TasbihRoute> { TasbihScreen(onBack = back) }
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
    }
}
