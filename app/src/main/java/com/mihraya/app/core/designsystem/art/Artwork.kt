package com.mihraya.app.core.designsystem.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.mihraya.app.R

@Composable
fun Artwork(
    hue: Long,
    title: String?,
    modifier: Modifier = Modifier,
    seed: Int = 0,
    cornerRadius: Dp = 20.dp,
) {
    val context = LocalContext.current
    val typeface = remember { runCatching { ResourcesCompat.getFont(context, R.font.amiri) }.getOrNull() }
    Canvas(modifier.clip(RoundedCornerShape(cornerRadius))) {
        drawIntoCanvas { canvas ->
            ArtworkRenderer.draw(canvas.nativeCanvas, size.width, size.height, hue.toInt(), seed, title, typeface)
        }
    }
}
