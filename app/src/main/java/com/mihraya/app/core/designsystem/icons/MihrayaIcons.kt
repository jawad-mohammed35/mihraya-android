package com.mihraya.app.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object MihrayaIcons {

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.stroke(width: Float = 1.6f, pathBuilder: PathBuilder.() -> Unit) = path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )

    private fun ImageVector.Builder.solid(pathBuilder: PathBuilder.() -> Unit) =
        path(fill = SolidColor(Color.Black), pathBuilder = pathBuilder)

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, true, true, cx + r, cy)
        arcTo(r, r, 0f, true, true, cx - r, cy)
        close()
    }

    private fun PathBuilder.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        moveTo(x + r, y)
        lineTo(x + w - r, y)
        arcTo(r, r, 0f, false, true, x + w, y + r)
        lineTo(x + w, y + h - r)
        arcTo(r, r, 0f, false, true, x + w - r, y + h)
        lineTo(x + r, y + h)
        arcTo(r, r, 0f, false, true, x, y + h - r)
        lineTo(x, y + r)
        arcTo(r, r, 0f, false, true, x + r, y)
        close()
    }

    val Home: ImageVector by lazy {
        icon("Mihrab") {
            stroke {
                moveTo(5f, 20.5f)
                lineTo(5f, 11.4f)
                curveTo(5f, 7.8f, 7.9f, 5.1f, 12f, 3f)
                curveTo(16.1f, 5.1f, 19f, 7.8f, 19f, 11.4f)
                lineTo(19f, 20.5f)
                moveTo(3.5f, 20.5f)
                lineTo(20.5f, 20.5f)
            }
            stroke(1.2f) {
                moveTo(12f, 7.6f)
                lineTo(12f, 10.6f)
            }
            solid {
                moveTo(10.3f, 10.8f)
                lineTo(13.7f, 10.8f)
                lineTo(13.05f, 13.4f)
                curveTo(12.85f, 14.1f, 11.15f, 14.1f, 10.95f, 13.4f)
                close()
            }
        }
    }

    val Quran: ImageVector by lazy {
        icon("Quran") {
            stroke {
                moveTo(12f, 6.6f)
                curveTo(10f, 5.1f, 7.1f, 4.6f, 3.5f, 5.2f)
                lineTo(3.5f, 18.6f)
                curveTo(7.1f, 18f, 10f, 18.4f, 12f, 19.9f)
                curveTo(14f, 18.4f, 16.9f, 18f, 20.5f, 18.6f)
                lineTo(20.5f, 5.2f)
                curveTo(16.9f, 4.6f, 14f, 5.1f, 12f, 6.6f)
                close()
                moveTo(12f, 6.6f)
                lineTo(12f, 19.9f)
            }
        }
    }

    val Listen: ImageVector by lazy {
        icon("Listen") {
            stroke {
                moveTo(4f, 15.5f)
                lineTo(4f, 12.6f)
                curveTo(4f, 8.1f, 7.6f, 4.6f, 12f, 4.6f)
                curveTo(16.4f, 4.6f, 20f, 8.1f, 20f, 12.6f)
                lineTo(20f, 15.5f)
            }
            stroke {
                roundRect(4f, 13.6f, 3.6f, 6.2f, 1.5f)
                roundRect(16.4f, 13.6f, 3.6f, 6.2f, 1.5f)
            }
        }
    }

    val Qibla: ImageVector by lazy {
        icon("Qibla") {
            stroke { circle(12f, 12f, 8.6f) }
            stroke(1.4f) {
                moveTo(15.4f, 8.6f)
                lineTo(13.4f, 13.4f)
                lineTo(8.6f, 15.4f)
                lineTo(10.6f, 10.6f)
                close()
            }
        }
    }

    val More: ImageVector by lazy {
        icon("More") {
            stroke {
                roundRect(4f, 4f, 6.5f, 6.5f, 2f)
                roundRect(13.5f, 4f, 6.5f, 6.5f, 2f)
                roundRect(4f, 13.5f, 6.5f, 6.5f, 2f)
                roundRect(13.5f, 13.5f, 6.5f, 6.5f, 2f)
            }
        }
    }

    val Tasbih: ImageVector by lazy {
        icon("Tasbih") {
            solid {
                for (i in 1 until 10) {
                    val angle = Math.toRadians(90.0 + i * 36.0)
                    circle(12f + (6.3f * Math.cos(angle)).toFloat(), 9.4f + (6.3f * Math.sin(angle)).toFloat(), 1.55f)
                }
                circle(12f, 17.6f, 1.25f)
            }
            stroke(1.5f) {
                moveTo(12f, 18.9f)
                lineTo(12f, 21.6f)
            }
        }
    }

    val Kaaba: ImageVector by lazy {
        icon("Kaaba") {
            solid {
                moveTo(4.5f, 7.6f)
                lineTo(12f, 4.5f)
                lineTo(19.5f, 7.6f)
                lineTo(19.5f, 17.2f)
                lineTo(12f, 20.4f)
                lineTo(4.5f, 17.2f)
                close()
            }
        }
    }

    val Crescent: ImageVector by lazy {
        icon("Crescent") {
            solid {
                moveTo(14.5f, 3.2f)
                curveTo(10f, 3.6f, 6.5f, 7.4f, 6.5f, 12f)
                curveTo(6.5f, 16.6f, 10f, 20.4f, 14.5f, 20.8f)
                curveTo(11.6f, 19.3f, 9.6f, 15.9f, 9.6f, 12f)
                curveTo(9.6f, 8.1f, 11.6f, 4.7f, 14.5f, 3.2f)
                close()
            }
        }
    }
}
