package com.mihraya.app.core.designsystem.art

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bitmap = remember(hue, title, seed, size, typeface) {
        if (size.width <= 0 || size.height <= 0) {
            null
        } else {
            Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888).also { image ->
                ArtworkRenderer.draw(
                    canvas = android.graphics.Canvas(image),
                    width = size.width.toFloat(),
                    height = size.height.toFloat(),
                    hue = hue.toInt(),
                    seed = seed,
                    title = title,
                    typeface = typeface,
                )
            }.asImageBitmap()
        }
    }
    Box(
        modifier
            .onSizeChanged { size = it }
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}
