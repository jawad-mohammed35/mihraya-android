package com.mihraya.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * How far a screen's header has scrolled away. The header leaves as the visible list scrolls forward and comes
 * back once that list returns to its top, so it reads as one scrolling page while each tab keeps its own list.
 */
@Stable
class CollapsingHeaderState(offset: Float = 0f) {
    var offset by mutableFloatStateOf(offset)
        private set

    internal var range = 0f
    internal var canScrollForward: () -> Boolean = { true }

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f || !canScrollForward()) return Offset.Zero
            return Offset(0f, consume(available.y))
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y <= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }
    }

    private fun consume(delta: Float): Float {
        val current = offset.coerceIn(-range, 0f)
        val next = (current + delta).coerceIn(-range, 0f)
        offset = next
        return next - current
    }

    suspend fun expand() {
        animate(offset.coerceIn(-range, 0f), 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ ->
            offset = value
        }
    }

    companion object {
        val Saver: Saver<CollapsingHeaderState, Float> = Saver(save = { it.offset }, restore = { CollapsingHeaderState(it) })
    }
}

@Composable
fun rememberCollapsingHeaderState(): CollapsingHeaderState =
    rememberSaveable(saver = CollapsingHeaderState.Saver) { CollapsingHeaderState() }

/**
 * Stacks [header], [pinned] and [body]. The header scrolls up under the status bar while [pinned] stops just below
 * it. [canScrollForward] reports whether the visible list has more to show, so short lists leave the header alone.
 */
@Composable
fun CollapsingHeaderLayout(
    state: CollapsingHeaderState,
    canScrollForward: () -> Boolean,
    header: @Composable () -> Unit,
    pinned: @Composable () -> Unit,
    body: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    SideEffect { state.canScrollForward = canScrollForward }
    val density = LocalDensity.current
    val topInset = WindowInsets.topChrome.getTop(density)
    val fade = with(density) { StatusBarFade.roundToPx() }
    Layout(
        contents = listOf(header, pinned, body),
        modifier = modifier.nestedScroll(state.connection),
    ) { (headerMeasurables, pinnedMeasurables, bodyMeasurables), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = Constraints(maxWidth = width)
        val headerPlaceable = headerMeasurables.firstOrNull()?.measure(loose)
        val pinnedPlaceable = pinnedMeasurables.firstOrNull()?.measure(loose)
        val headerHeight = headerPlaceable?.height ?: 0
        // The header travels on through the status bar's fade so none of it lingers above the pinned row.
        val range = (headerHeight - topInset + fade).coerceAtLeast(0)
        state.range = range.toFloat()
        val offset = state.offset.coerceIn(-range.toFloat(), 0f).roundToInt()
        val pinnedY = maxOf(headerHeight + offset, minOf(topInset, headerHeight))
        val bodyY = pinnedY + (pinnedPlaceable?.height ?: 0)
        val bodyPlaceable = bodyMeasurables.firstOrNull()?.measure(Constraints.fixed(width, (height - bodyY).coerceAtLeast(0)))
        layout(width, height) {
            bodyPlaceable?.place(0, bodyY)
            headerPlaceable?.place(0, offset)
            pinnedPlaceable?.place(0, pinnedY)
        }
    }
}

/** A [SegmentedControl] bound to [pager]: tapping scrolls the pager and the indicator follows swipes. */
@Composable
fun PagerTabs(options: List<String>, pager: PagerState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    SegmentedControl(
        options = options,
        selectedIndex = pager.targetPage,
        onSelect = { page -> scope.launch { pager.animateScrollToPage(page) } },
        modifier = modifier,
        indicatorPosition = { pager.currentPage + pager.currentPageOffsetFraction },
    )
}
