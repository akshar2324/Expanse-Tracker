package com.akshar.ui.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Capability tiers. Every effect below is progressive enhancement:
//   API 33+  AGSL liquid backdrop + real backdrop blur under floating bars
//   API 31-32 gradient backdrop + real backdrop blur under floating bars
//   API 24-30 gradient backdrop + near-opaque frosted fills (no blur)
// Reduced motion only removes the one-off settle drift; nothing in this file loops forever.
// ---------------------------------------------------------------------------------------------

object GlassCapabilities {
    val backdropBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val runtimeShader: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/**
 * The warm, softly lit field every screen floats on. On open the colour pools drift into place
 * over a few seconds and then stop, so there is motion to greet the user but no ongoing work.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val glass = AkSpendTheme.glass
    val reducedMotion = AkSpendTheme.reducedMotion
    val phase = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) phase.snapTo(1f)
        else phase.animateTo(1f, tween(Durations.settle, easing = FastOutSlowInEasing))
    }

    val shaderModifier = if (GlassCapabilities.runtimeShader) {
        Modifier.liquidShaderBackdrop(glass) { phase.value }
    } else {
        Modifier.gradientBackdrop(glass) { phase.value }
    }
    Box(modifier.fillMaxSize().then(shaderModifier))
}

private fun Modifier.gradientBackdrop(glass: GlassColors, phase: () -> Float) = drawBehind {
    val p = phase()
    drawRect(Brush.verticalGradient(listOf(glass.base, glass.baseBottom)))
    val w = size.width
    val h = size.height
    val centers = listOf(
        Offset(w * (0.12f + 0.10f * p), h * (0.08f + 0.04f * p)),
        Offset(w * (1.00f - 0.12f * p), h * 0.36f),
        Offset(w * 0.30f, h * (0.94f - 0.06f * p)),
    )
    val radius = maxOf(w, h) * 0.55f
    glass.blobs.zip(centers).forEach { (color, center) ->
        drawRect(
            Brush.radialGradient(
                0f to color.copy(alpha = 0.85f),
                0.55f to color.copy(alpha = 0.35f),
                1f to color.copy(alpha = 0f),
                center = center,
                radius = radius,
            )
        )
    }
}

private const val LIQUID_AGSL = """
uniform float2 size;
uniform float phase;
layout(color) uniform half4 base;
layout(color) uniform half4 baseBottom;
layout(color) uniform half4 c0;
layout(color) uniform half4 c1;
layout(color) uniform half4 c2;

float pool(float2 p, float2 c, float r) {
    float d = distance(p, c) / r;
    return exp(-d * d * 2.4);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / size;
    float aspect = size.x / size.y;
    float t = phase * 3.14159;
    // Low-frequency warp bends the colour field the way light bends through thick glass.
    float2 warp = float2(sin(uv.y * 5.0 + t), cos(uv.x * 4.0 - t * 0.8)) * 0.03;
    float2 p = float2((uv.x + warp.x) * aspect, uv.y + warp.y);

    half3 col = mix(base.rgb, baseBottom.rgb, half(uv.y));
    col = mix(col, c0.rgb, half(pool(p, float2((0.12 + 0.10 * phase) * aspect, 0.08 + 0.04 * phase), 0.62)) * 0.85);
    col = mix(col, c1.rgb, half(pool(p, float2((1.00 - 0.12 * phase) * aspect, 0.36), 0.58)) * 0.80);
    col = mix(col, c2.rgb, half(pool(p, float2(0.30 * aspect, 0.94 - 0.06 * phase), 0.66)) * 0.80);

    // A faint diagonal caustic adds the specular glint of a curved glass surface.
    float band = uv.x + uv.y * 0.6 - 0.55 - 0.1 * phase;
    col += half3(exp(-band * band * 36.0) * 0.035);
    return half4(col, 1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.liquidShaderBackdrop(glass: GlassColors, phase: () -> Float) = drawWithCache {
    val shader = RuntimeShader(LIQUID_AGSL)
    val brush = ShaderBrush(shader)
    shader.setColorUniform("base", glass.base.toArgb())
    shader.setColorUniform("baseBottom", glass.baseBottom.toArgb())
    shader.setColorUniform("c0", glass.blobs[0].toArgb())
    shader.setColorUniform("c1", glass.blobs[1].toArgb())
    shader.setColorUniform("c2", glass.blobs[2].toArgb())
    shader.setFloatUniform("size", size.width, size.height)
    onDrawBehind {
        shader.setFloatUniform("phase", phase())
        drawRect(brush)
    }
}

/**
 * A frosted glass panel: translucent fill, a top sheen and a light-catching rim. No elevation
 * shadow, because Android shadows show through translucent fills as a dark smudge.
 */
fun Modifier.glassPanel(
    shape: Shape,
    glass: GlassColors,
    fill: Color = glass.fill,
    rimWidth: Dp = 1.dp,
): Modifier = this
    .clip(shape)
    .background(fill)
    .drawWithCache {
        val sheen = Brush.verticalGradient(0f to glass.sheen, 0.5f to Color.Transparent)
        val rim = Brush.linearGradient(
            0f to glass.highlight,
            0.45f to glass.edge,
            0.8f to glass.edge,
            1f to glass.highlight.copy(alpha = glass.highlight.alpha * 0.4f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        val outline = shape.createOutline(size, layoutDirection, this)
        val stroke = Stroke(rimWidth.toPx() * 2f)
        onDrawWithContent {
            drawRect(sheen)
            drawContent()
            drawOutline(outline, rim, style = stroke)
        }
    }

// --- Real backdrop blur (API 31+) ------------------------------------------------------------

/**
 * Shares what the app content draws with floating bars so they can blur it. The content is
 * recorded into a GraphicsLayer; a second layer draws it again through a RenderEffect blur.
 * RenderNodes are drawn by reference, so the bar picks up scrolling content without redrawing.
 */
@Stable
class GlassBlurState internal constructor(
    internal val content: GraphicsLayer,
    internal val blurred: GraphicsLayer,
    internal val radius: Dp,
) {
    internal var sourceOffset by mutableStateOf(Offset.Zero)
    internal var appliedRadiusPx = -1f
}

/** Returns null below API 31, where callers fall back to an opaque frosted fill. */
@Composable
fun rememberGlassBlurState(radius: Dp = 30.dp): GlassBlurState? {
    if (!GlassCapabilities.backdropBlur) return null
    val content = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    return remember(content, blurred, radius) { GlassBlurState(content, blurred, radius) }
}

fun Modifier.glassBlurSource(state: GlassBlurState?): Modifier {
    if (state == null) return this
    return this
        .onGloballyPositioned { state.sourceOffset = it.positionInRoot() }
        .drawWithContent {
            state.content.record { this@drawWithContent.drawContent() }
            val radiusPx = state.radius.toPx()
            if (state.appliedRadiusPx != radiusPx) {
                state.blurred.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
                state.appliedRadiusPx = radiusPx
            }
            state.blurred.record { drawLayer(state.content) }
            drawLayer(state.content)
        }
}

/**
 * Background for a floating bar: blurred content plus a tint when [state] is available, or a
 * near-opaque frosted fill otherwise so text scrolling underneath never hurts legibility.
 */
fun Modifier.glassBarBackground(state: GlassBlurState?, shape: Shape, glass: GlassColors): Modifier {
    if (state == null) return this.glassPanel(shape, glass, fill = glass.fillOpaque)
    var selfOffset = Offset.Zero
    return this
        .onGloballyPositioned { selfOffset = it.positionInRoot() }
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            clipPath(path) {
                drawRect(glass.base)
                val delta = state.sourceOffset - selfOffset
                translate(delta.x, delta.y) { drawLayer(state.blurred) }
                // Calm the blurred content so labels on the bar keep AA contrast whatever scrolls by.
                drawRect(glass.base.copy(alpha = 0.55f))
            }
        }
        .glassPanel(shape, glass, fill = glass.fillStrong)
}
