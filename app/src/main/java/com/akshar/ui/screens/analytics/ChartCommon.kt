package com.akshar.ui.screens.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Durations

/**
 * One-shot 0..1 reveal for chart marks. Returned as a lambda so Canvas reads it in the draw
 * phase only. Starts at 1 with reduced motion, so values are never hidden.
 */
@Composable
internal fun rememberChartReveal(): () -> Float {
    val reducedMotion = AkSpendTheme.reducedMotion
    val anim = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) anim.snapTo(1f)
        else anim.animateTo(1f, tween(Durations.emphasized * 2, easing = FastOutSlowInEasing))
    }
    return { anim.value }
}

@Composable
internal fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
