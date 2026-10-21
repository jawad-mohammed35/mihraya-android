package com.mihraya.app.core.designsystem.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws Mihraya's generative cover art: a deep colour field with a geometric
 * khatam lattice and a central Rub el Hizb star. Used both in Compose and for
 * media-notification bitmaps so the two always match.
 */
object ArtworkRenderer {

    private const val INNER_RATIO = 0.7654f

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        hue: Int,
        seed: Int,
        title: String?,
        typeface: Typeface?,
    ) {
        val size = min(width, height)
        val cx = width / 2f
        val cy = height / 2f

        canvas.drawColor(Color.BLACK)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.shader = RadialGradient(
            width * 0.18f,
            height * 0.08f,
            max(width, height) * 1.2f,
            intArrayOf(lighten(hue, 0.08f), blend(hue, Color.BLACK, 0.55f), Color.BLACK),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width, height, fill)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeJoin = Paint.Join.ROUND
        }

        val cell = size / 3.1f
        stroke.strokeWidth = max(1f, size * 0.003f)
        stroke.alpha = 24
        canvas.save()
        canvas.rotate((seed % 8) * 5.625f, cx, cy)
        var y = -cell * 1.5f
        while (y < height + cell * 1.5f) {
            var x = -cell * 1.5f
            while (x < width + cell * 1.5f) {
                canvas.drawPath(starOutline(x, y, cell * 0.36f), stroke)
                canvas.drawPath(diamond(x + cell / 2f, y + cell / 2f, cell * 0.12f), stroke)
                x += cell
            }
            y += cell
        }
        canvas.restore()

        stroke.strokeWidth = max(1.2f, size * 0.0045f)
        stroke.alpha = 70
        canvas.drawPath(square(cx, cy, size * 0.33f, 0f), stroke)
        canvas.drawPath(square(cx, cy, size * 0.33f, 45f), stroke)
        stroke.alpha = 40
        canvas.drawCircle(cx, cy, size * 0.245f, stroke)

        if (!title.isNullOrBlank()) {
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 238
                textAlign = Paint.Align.CENTER
                this.typeface = typeface
                textSize = size * 0.15f
            }
            val maxWidth = size * 0.4f
            val measured = text.measureText(title)
            if (measured > maxWidth) text.textSize *= maxWidth / measured
            val metrics = text.fontMetrics
            canvas.drawText(title, cx, cy - (metrics.ascent + metrics.descent) / 2f, text)
        }
    }

    fun starOutline(cx: Float, cy: Float, radius: Float): Path = Path().apply {
        for (i in 0 until 16) {
            val angle = Math.toRadians(i * 22.5 - 90.0)
            val r = if (i % 2 == 0) radius else radius * INNER_RATIO
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun square(cx: Float, cy: Float, radius: Float, rotation: Float): Path = Path().apply {
        for (i in 0 until 4) {
            val angle = Math.toRadians(i * 90.0 + 45.0 + rotation)
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun diamond(cx: Float, cy: Float, r: Float): Path = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
    }

    private fun blend(from: Int, to: Int, amount: Float): Int {
        val inv = 1f - amount
        return Color.argb(
            255,
            (Color.red(from) * inv + Color.red(to) * amount).toInt(),
            (Color.green(from) * inv + Color.green(to) * amount).toInt(),
            (Color.blue(from) * inv + Color.blue(to) * amount).toInt(),
        )
    }

    private fun lighten(color: Int, amount: Float): Int = blend(color, Color.WHITE, amount)
}
