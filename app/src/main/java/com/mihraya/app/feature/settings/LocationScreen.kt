package com.mihraya.app.feature.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.HairlineDivider
import com.mihraya.app.core.designsystem.components.NoticeCard
import com.mihraya.app.core.designsystem.components.SearchField
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.components.SectionLabel
import com.mihraya.app.core.designsystem.components.SettingsGroup
import com.mihraya.app.core.designsystem.components.SettingsSwitchRow
import com.mihraya.app.core.designsystem.theme.LocalUse24Hour
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.TimeFormat
import com.mihraya.app.data.location.PlaceResult
import com.mihraya.app.data.settings.SavedLocation
import com.mihraya.app.feature.home.IconTile
import com.mihraya.app.feature.home.LabeledValue
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.rememberLocationRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs

@Composable
fun LocationScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val settingsState by container.settings.state.collectAsStateWithLifecycle()
    val settings = settingsState ?: return
    val request = rememberLocationRequest()

    SettingsScaffold("Location", onBack, subtitle = "Used for prayer times and the Qibla") {
        CurrentLocationCard(settings.location)

        SettingsGroup(
            "Automatic",
            footer = "Mihraya asks only for approximate location and refreshes it when you open the app.",
        ) {
            SettingsSwitchRow(
                title = "Detect my location",
                checked = settings.autoLocation && settings.location?.automatic == true,
                onCheckedChange = { enabled ->
                    if (enabled) request.request() else container.settings.edit { it.copy(autoLocation = false) }
                },
                subtitle = if (request.loading) "Finding you…" else "Update when you travel",
                icon = Icons.Rounded.MyLocation,
                enabled = !request.loading,
            )
        }

        SecondaryButton(
            text = if (request.loading) "Locating…" else "Use my current location",
            icon = Icons.Rounded.MyLocation,
            onClick = request::request,
            enabled = !request.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        request.error?.let { message ->
            NoticeCard(
                icon = Icons.Rounded.LocationOff,
                title = "Location unavailable",
                message = message,
                actionText = "Dismiss",
                onAction = request::clearError,
            )
        }

        CitySearch(
            onPick = { place ->
                container.applyLocation(place.toSaved(), keepAutomatic = false)
                onBack()
            },
        )
    }
}

@Composable
private fun CurrentLocationCard(location: SavedLocation?) {
    val colors = Mihraya.colors
    val use24h = LocalUse24Hour.current
    GlassCard(contentPadding = PaddingValues(20.dp)) {
        AnimatedContent(
            targetState = location,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
            label = "location",
        ) { current ->
            if (current == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.LocationOff, size = 44)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("No location yet", style = Mihraya.type.title, color = colors.textPrimary)
                        Text(
                            "Detect it automatically or search for your city below.",
                            style = Mihraya.type.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(if (current.automatic) Icons.Rounded.MyLocation else Icons.Rounded.Place, size = 44)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(current.name, style = Mihraya.type.title, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (current.region.isNotBlank()) {
                                Text(current.region, style = Mihraya.type.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text(
                            if (current.automatic) "AUTO" else "MANUAL",
                            style = Mihraya.type.overline,
                            color = colors.accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.accentSoft)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    HairlineDivider()
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        LabeledValue("Coordinates", coordinates(current.latitude, current.longitude), Modifier.weight(1.4f))
                        LabeledValue("Time zone", current.timeZone.substringAfterLast('/').replace('_', ' '), Modifier.weight(1f))
                        LabeledValue(
                            "Local time",
                            TimeFormat.full(ZonedDateTime.now(current.zoneId), use24h),
                            Modifier.weight(0.9f),
                            alignEnd = true,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface CitySearchState {
    data object Idle : CitySearchState
    data object Loading : CitySearchState
    data class Results(val places: List<PlaceResult>) : CitySearchState
    data class Error(val message: String) : CitySearchState
}

/** City search backed by the Open-Meteo geocoding API. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CitySearch(onPick: (PlaceResult) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val colors = Mihraya.colors
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
    var query by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<CitySearchState>(CitySearchState.Idle) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            state = CitySearchState.Idle
            return@LaunchedEffect
        }
        delay(350)
        state = CitySearchState.Loading
        state = try {
            CitySearchState.Results(container.location.search(trimmed))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CitySearchState.Error("Couldn't reach the search service. Check your connection and try again.")
        }
    }

    LaunchedEffect(state) {
        if (state !is CitySearchState.Idle) bringIntoView.bringIntoView()
    }

    Column(modifier.fillMaxWidth()) {
        SectionLabel("Search for a city")
        Spacer(Modifier.height(10.dp))
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "City or town",
            modifier = Modifier
                .bringIntoViewRequester(bringIntoView)
                .onFocusChanged { focus ->
                    if (focus.isFocused) scope.launch { bringIntoView.bringIntoView() }
                },
            trailing = if (state == CitySearchState.Loading) {
                {
                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                null
            },
        )
        when (val current = state) {
            CitySearchState.Idle, CitySearchState.Loading -> Unit
            is CitySearchState.Error -> Text(
                current.message,
                style = Mihraya.type.bodySmall,
                color = colors.danger,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            )
            is CitySearchState.Results -> {
                Spacer(Modifier.height(12.dp))
                if (current.places.isEmpty()) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SearchOff, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("No places found for “${query.trim()}”", style = Mihraya.type.bodySmall, color = colors.textTertiary)
                    }
                } else {
                    GlassCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                        current.places.forEachIndexed { index, place ->
                            if (index > 0) HairlineDivider(Modifier.padding(horizontal = 16.dp))
                            PlaceRow(place, onClick = { onPick(place) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(place: PlaceResult, onClick: () -> Unit) {
    val colors = Mihraya.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 58.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = Mihraya.type.body.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
            val detail = listOf(place.region, place.country).filter { it.isNotBlank() && it != place.name }.distinct()
            if (detail.isNotEmpty()) {
                Text(detail.joinToString(", "), style = Mihraya.type.caption, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            place.timeZone.substringAfterLast('/').replace('_', ' '),
            style = Mihraya.type.caption,
            color = colors.textTertiary,
            maxLines = 1,
        )
    }
}

private fun coordinates(latitude: Double, longitude: Double): String {
    val lat = String.format(Locale.US, "%.2f°%s", abs(latitude), if (latitude >= 0) "N" else "S")
    val lon = String.format(Locale.US, "%.2f°%s", abs(longitude), if (longitude >= 0) "E" else "W")
    return "$lat $lon"
}
