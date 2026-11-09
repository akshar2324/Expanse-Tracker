package com.akshar.ui.theme

import android.graphics.Paint
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Neumorphic drawing. Shapes are lit from the top-left: a raised shape casts a highlight up-left
// and a shade down-right; a pressed-in well shows the same pair on its inner edges, reversed.
//
// API 28+ draws real blurred shadows (Paint.setShadowLayer is hardware accelerated from P).
// Older phones get a two-tone rim instead, which keeps the raised/pressed reading without blur.
// Everything here is static drawing: nothing animates unless a caller animates `depth`.
// ---------------------------------------------------------------------------------------------

object SoftCapabilities {
    val blurredShadows: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
}

/** How far each kind of element stands off the page. */
object SoftDepth {
    val panel: Dp = 6.dp
    val control: Dp = 4.dp
    val small: Dp = 3.dp
    val well: Dp = 3.dp
}

/**
 * Draws [fill] in [shape] as a neumorphic surface behind the content.
 *
 * [depth] runs from 1 (fully raised by [distance]) through 0 (flush) to -1 (pressed in by
 * [distance]). It is read at draw time, so animating it only redraws, never recomposes.
 */
fun Modifier.softSurface(
    shape: Shape,
    soft: SoftColors,
    fill: Color = soft.background,
    distance: Dp = SoftDepth.panel,
    depth: () -> Float = { 1f },
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    // Small controls get a shallower lift so the shadow never outgrows the shape.
    val maxD = minOf(distance.toPx(), size.minDimension * 0.12f)
    val pad = maxD * 6f + 8f
    val ring = android.graphics.Path().apply {
        fillType = android.graphics.Path.FillType.EVEN_ODD
        addRect(-pad, -pad, size.width + pad, size.height + pad, android.graphics.Path.Direction.CW)
        addPath(path.asAndroidPath())
    }
    val lit = Paint(Paint.ANTI_ALIAS_FLAG)
    val shaded = Paint(Paint.ANTI_ALIAS_FLAG)
    val raisedRim = Brush.linearGradient(
        listOf(soft.highlight, soft.shade), start = Offset.Zero, end = Offset(size.width, size.height)
    )
    val pressedRim = Brush.linearGradient(
        listOf(soft.shade, soft.highlight), start = Offset.Zero, end = Offset(size.width, size.height)
    )
    val rimStroke = Stroke(1.5.dp.toPx())
    val fillArgb = fill.toArgb()

    onDrawBehind {
        val k = depth().coerceIn(-1f, 1f)
        if (!SoftCapabilities.blurredShadows) {
            drawOutline(outline, fill)
            if (k != 0f) drawOutline(outline, if (k > 0f) raisedRim else pressedRim, alpha = kotlin.math.abs(k), style = rimStroke)
            return@onDrawBehind
        }
        val d = maxD * kotlin.math.abs(k)
        val blur = (d * 2f).coerceAtLeast(0.5f)
        if (k > 0.01f) {
            lit.color = fillArgb
            shaded.color = fillArgb
            lit.setShadowLayer(blur, -d, -d, soft.highlight.toArgb())
            shaded.setShadowLayer(blur, d, d, soft.shade.toArgb())
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawOutlineShape(outline, path, shaded)
                canvas.nativeCanvas.drawOutlineShape(outline, path, lit)
            }
        }
        drawOutline(outline, fill)
        if (k < -0.01f) {
            lit.color = soft.highlight.toArgb()
            shaded.color = soft.shade.toArgb()
            // The ring around the shape is clipped away; only its shadow falls inside the edge.
            shaded.setShadowLayer(blur, d, d, soft.shade.toArgb())
            lit.setShadowLayer(blur, -d, -d, soft.highlight.toArgb())
            clipPath(path) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawPath(ring, shaded)
                    canvas.nativeCanvas.drawPath(ring, lit)
                }
            }
        }
    }
}

/** A raised neumorphic surface. */
fun Modifier.softRaised(
    shape: Shape,
    soft: SoftColors,
    fill: Color = soft.background,
    distance: Dp = SoftDepth.panel,
): Modifier = softSurface(shape, soft, fill, distance) { 1f }

/** A pressed-in well, for text fields, tracks and selected states. */
fun Modifier.softInset(
    shape: Shape,
    soft: SoftColors,
    fill: Color = soft.background,
    distance: Dp = SoftDepth.well,
): Modifier = softSurface(shape, soft, fill, distance) { -1f }

/**
 * A two-tone embossed rim with no shadow. For surfaces in their own window (dialogs, menus),
 * where a shadow outside the bounds would be cut off.
 */
fun Modifier.softRim(shape: Shape, soft: SoftColors, width: Dp = 1.5.dp): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val rim = Brush.linearGradient(
        listOf(soft.highlight, soft.shade), start = Offset.Zero, end = Offset(size.width, size.height)
    )
    val stroke = Stroke(width.toPx() * 2f)
    onDrawWithContent {
        drawContent()
        drawOutline(outline, rim, style = stroke)
    }
}

// Rounded rectangles go through drawRoundRect so Skia can use its fast analytic blur.
private fun android.graphics.Canvas.drawOutlineShape(outline: Outline, path: Path, paint: Paint) {
    when (outline) {
        is Outline.Rectangle -> drawRect(
            outline.rect.left, outline.rect.top, outline.rect.right, outline.rect.bottom, paint
        )
        is Outline.Rounded -> {
            val r = outline.roundRect
            if (r.isSimple) {
                drawRoundRect(r.left, r.top, r.right, r.bottom, r.topLeftCornerRadius.x, r.topLeftCornerRadius.y, paint)
            } else {
                drawPath(path.asAndroidPath(), paint)
            }
        }
        is Outline.Generic -> drawPath(path.asAndroidPath(), paint)
    }
}
