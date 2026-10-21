package com.mihraya.app.feature.qibla

import android.hardware.GeomagneticField
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.mihraya.app.core.designsystem.components.GlassCard
import com.mihraya.app.core.designsystem.components.PrimaryButton
import com.mihraya.app.core.designsystem.components.ScreenHeader
import com.mihraya.app.core.designsystem.components.SecondaryButton
import com.mihraya.app.core.designsystem.icons.MihrayaIcons
import com.mihraya.app.core.designsystem.theme.Manrope
import com.mihraya.app.core.designsystem.theme.Mihraya
import com.mihraya.app.core.util.rememberHaptics
import com.mihraya.app.ui.LocalAppContainer
import com.mihraya.app.ui.LocalBottomInset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ALIGN_TOLERANCE = 3f

@Composable
fun QiblaScreen(onSetLocation: () -> Unit) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val location = settings?.location
    val colors = Mihraya.colors

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = LocalBottomInset.current + 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(
            "Qibla",
            subtitle = location?.let { "From ${it.name}" } ?: "Direction of the Kaʿbah",
            modifier = Modifier.widthIn(max = 560.dp),
        )
        if (location == null) {
            GlassCard(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(top = 24.dp), contentPadding = PaddingValues(24.dp)) {
                Icon(MihrayaIcons.Qibla, null, tint = colors.accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(16.dp))
                Text("Location needed", style = Mihraya.type.title, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "The Qibla is calculated from your position using the great-circle bearing to Makkah.",
                    style = Mihraya.type.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Set location", onClick = onSetLocation, icon = Icons.Rounded.LocationOn)
            }
            return@Column
        }

        val bearing = remember(location.latitude, location.longitude) {
            container.prayerCalculator.qiblaBearing(location.latitude, location.longitude).toFloat()
        }
        val distance = remember(location.latitude, location.longitude) {
            container.prayerCalculator.distanceToKaabaKm(location.latitude, location.longitude)
        }
        val declination = remember(location.latitude, location.longitude) {
            GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        }
        QiblaCompass(bearing = bearing, declination = declination, distanceKm = distance)
    }
}

@Composable
private fun QiblaCompass(bearing: Float, declination: Float, distanceKm: Double) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val colors = Mihraya.colors
    val haptics = rememberHaptics()
    val sensor = remember { CompassSensor(context) }

    val readings = remember(sensor, lifecycle) {
        sensor.readings { view.display?.rotation ?: 0 }.flowWithLifecycle(lifecycle)
    }
    val reading by readings.collectAsState(initial = null)

    var unwrapped by remember { mutableFloatStateOf(0f) }
    var aligned by remember { mutableStateOf(false) }
    var showCalibration by remember { mutableStateOf(false) }

    val heading = reading?.let { ((it.azimuth + declination) % 360f + 360f) % 360f }
    LaunchedEffect(heading) {
        if (heading != null) {
            val delta = ((heading - unwrapped) % 360f + 540f) % 360f - 180f
            unwrapped += delta
        }
    }
    val dialRotation by animateFloatAsState(
        targetValue = -unwrapped,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow),
        label = "dial",
    )

    val offset = heading?.let { ((bearing - it) % 360f + 540f) % 360f - 180f }
    val nowAligned = offset != null && abs(offset) <= ALIGN_TOLERANCE && reading?.tilted == false
    LaunchedEffect(nowAligned) {
        if (nowAligned && !aligned) haptics.confirm()
        aligned = nowAligned
    }
    val lowAccuracy = (reading?.accuracy ?: SensorManager.SENSOR_STATUS_ACCURACY_HIGH) <= SensorManager.SENSOR_STATUS_ACCURACY_LOW

    val glow by animateFloatAsState(if (nowAligned) 1f else 0f, tween(400), label = "glow")
    val pointerColor by animateColorAsState(if (nowAligned) colors.accent else colors.textPrimary, tween(300), label = "pointer")
    val measurer = rememberTextMeasurer()

    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(18.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            val accent = colors.accent
            val ink = colors.textPrimary
            val faint = colors.textTertiary
            val ring = colors.glassBorder
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f * 0.86f
                val center = Offset(size.width / 2f, size.height / 2f)

                if (glow > 0f) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.28f * glow), Color.Transparent),
                            center = center,
                            radius = radius * 1.2f,
                        ),
                        radius = radius * 1.2f,
                        center = center,
                    )
                }

                drawCircle(ring, radius = radius, center = center, style = Stroke(1.dp.toPx()))
                drawCircle(ring.copy(alpha = ring.alpha * 0.6f), radius = radius * 0.62f, center = center, style = Stroke(1.dp.toPx()))

                rotate(dialRotation, center) {
                    for (i in 0 until 72) {
                        val angle = i * 5f
                        val major = i % 6 == 0
                        val length = if (major) 14.dp.toPx() else 7.dp.toPx()
                        val rad = Math.toRadians(angle - 90.0)
                        val start = Offset(
                            center.x + (radius - length) * cos(rad).toFloat(),
                            center.y + (radius - length) * sin(rad).toFloat(),
                        )
                        val end = Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
                        drawLine(
                            if (i == 0) accent else if (major) ink.copy(alpha = 0.7f) else faint,
                            start,
                            end,
                            strokeWidth = if (major) 1.6.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }

                    val qiblaRad = Math.toRadians(bearing - 90.0)
                    val markerCenter = Offset(
                        center.x + (radius + 2.dp.toPx()) * cos(qiblaRad).toFloat(),
                        center.y + (radius + 2.dp.toPx()) * sin(qiblaRad).toFloat(),
                    )
                    drawLine(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.85f)), center, markerCenter),
                        center,
                        markerCenter,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawKaaba(markerCenter, 22.dp.toPx(), accent, -dialRotation)
                }

                listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (label, angle) ->
                    val rad = Math.toRadians((angle + dialRotation) - 90.0)
                    val pos = Offset(
                        center.x + radius * 0.8f * cos(rad).toFloat(),
                        center.y + radius * 0.8f * sin(rad).toFloat(),
                    )
                    val layout = measurer.measure(
                        label,
                        TextStyle(
                            fontFamily = Manrope,
                            fontWeight = if (label == "N") FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (label == "N") accent else ink.copy(alpha = 0.75f),
                        ),
                    )
                    drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
                }

                val pointer = Path().apply {
                    moveTo(center.x, center.y - radius - 18.dp.toPx())
                    lineTo(center.x - 9.dp.toPx(), center.y - radius - 34.dp.toPx())
                    lineTo(center.x + 9.dp.toPx(), center.y - radius - 34.dp.toPx())
                    close()
                }
                drawPath(pointer, pointerColor)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    heading?.let { "${it.roundToInt() % 360}°" } ?: "—",
                    style = Mihraya.type.display.copy(fontSize = 52.sp),
                    color = colors.textPrimary,
                )
                Text(
                    heading?.let { cardinal(it) } ?: if (sensor.isAvailable) "Calibrating" else "No compass",
                    style = Mihraya.type.label,
                    color = colors.textTertiary,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            when {
                !sensor.isAvailable -> "This device has no compass sensor"
                heading == null -> "Hold your phone flat"
                reading?.tilted == true -> "Hold your phone flat"
                nowAligned -> "You're facing the Qibla"
                offset != null && offset > 0 -> "Turn right ${offset.roundToInt()}°"
                offset != null -> "Turn left ${(-offset).roundToInt()}°"
                else -> ""
            },
            style = Mihraya.type.title,
            color = if (nowAligned) colors.accent else colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            String.format(Locale.US, "Qibla %d° %s · %,d km to Makkah", bearing.roundToInt(), cardinal(bearing), distanceKm.roundToInt()),
            style = Mihraya.type.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))

        AnimatedVisibility(
            visible = lowAccuracy || showCalibration,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            CalibrationCard(onDone = { showCalibration = false }, lowAccuracy = lowAccuracy)
        }
        if (!lowAccuracy && !showCalibration && sensor.isAvailable) {
            SecondaryButton("Calibrate compass", onClick = { showCalibration = true }, icon = Icons.Rounded.Explore)
        }
        if (reading?.tilted == true) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ScreenRotation, null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Lay the phone flat, screen facing up", style = Mihraya.type.caption, color = colors.textTertiary)
            }
        }
    }
}

private fun DrawScope.drawKaaba(center: Offset, size: Float, accent: Color, counterRotation: Float) {
    rotate(counterRotation, center) {
        val half = size / 2f
        drawRoundRect(
            color = Color(0xFF0B0B0B),
            topLeft = Offset(center.x - half, center.y - half),
            size = Size(size, size),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.14f),
        )
        drawRect(
            color = accent,
            topLeft = Offset(center.x - half, center.y - half + size * 0.26f),
            size = Size(size, size * 0.14f),
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.7f),
            topLeft = Offset(center.x - half, center.y - half),
            size = Size(size, size),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.14f),
            style = Stroke(1.2f),
        )
    }
}

@Composable
private fun CalibrationCard(onDone: () -> Unit, lowAccuracy: Boolean) {
    val colors = Mihraya.colors
    val transition = rememberInfiniteTransition(label = "figure8")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(width = 96.dp, height = 56.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val a = size.width * 0.42f
                val path = Path()
                for (i in 0..100) {
                    val th = i / 100f * 2f * Math.PI.toFloat()
                    val x = cx + a * sin(th)
                    val y = cy + a * 0.55f * sin(th) * cos(th)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, colors.glassBorder, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                val th = t * 2f * Math.PI.toFloat()
                drawCircle(colors.accent, 5.dp.toPx(), Offset(cx + a * sin(th), cy + a * 0.55f * sin(th) * cos(th)))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (lowAccuracy) "Compass needs calibration" else "Calibrate your compass",
                    style = Mihraya.type.titleSmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Move your phone in a figure-eight a few times, away from magnets, laptops and metal surfaces.",
                    style = Mihraya.type.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        if (!lowAccuracy) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SecondaryButton("Done", onClick = onDone)
            }
        }
    }
}

private fun cardinal(degrees: Float): String {
    val names = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return names[(((degrees % 360f) + 360f) % 360f / 45f).roundToInt() % 8]
}