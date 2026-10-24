package com.mihraya.app.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.PrimaryButton
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.Mihraya
import kotlinx.coroutines.launch

private data class TourPage(val icon: ImageVector, val title: String, val body: String)

private val pages = listOf(
    TourPage(
        MihrayaIcons.Home,
        "Prayer times",
        "Your day is calculated on this phone from your location and the method you choose. The next prayer stays at the top of Home.",
    ),
    TourPage(
        MihrayaIcons.Quran,
        "Read",
        "Open any sūrah, change the script and type size, and pick up where you left off. Bookmarks stay on this device.",
    ),
    TourPage(
        MihrayaIcons.Listen,
        "Listen",
        "Choose a reciter, stream a sūrah, or download it for later. Playback continues from the mini player.",
    ),
    TourPage(
        MihrayaIcons.Qibla,
        "Qibla",
        "The compass on Home points toward the Kaʿbah and shows how far Makkah is, in kilometres or miles.",
    ),
    TourPage(
        MihrayaIcons.Tasbih,
        "Tasbih",
        "Count with a target of 33, 100, 1000, or your own number. Completed rounds are kept in history until you clear them.",
    ),
)

@Composable
fun TourScreen(onDone: () -> Unit) {
    val colors = Mihraya.colors
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SecondaryButton("Skip", onClick = onDone)
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).widthIn(max = 520.dp).fillMaxWidth(),
        ) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize().padding(bottom = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp)) {
                    IconTile(page.icon)
                    Spacer(Modifier.height(22.dp))
                    Text(page.title, style = Mihraya.type.headline, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text(page.body, style = Mihraya.type.body, color = colors.textSecondary)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            pages.indices.forEach { index ->
                Box(
                    Modifier
                        .size(if (index == pager.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (index == pager.currentPage) colors.accent else colors.textTertiary.copy(alpha = 0.4f)),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = if (last) "Done" else "Next",
            onClick = {
                if (last) onDone()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun IconTile(icon: ImageVector) {
    val colors = Mihraya.colors
    Box(
        Modifier.size(64.dp).clip(CircleShape).background(colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(icon, null, tint = colors.accent, modifier = Modifier.size(30.dp))
    }
}
