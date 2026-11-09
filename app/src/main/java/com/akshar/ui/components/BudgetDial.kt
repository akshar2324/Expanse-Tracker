package com.akshar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akshar.ui.theme.Durations

/**
 * A 270° gauge for how much of a budget is used, with the percentage in the middle. [usedFraction]
 * may exceed 1 (over budget): the arc stays full and the label shows the real percentage.
 */
@Composable
fun BudgetDial(
    usedFraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 7.dp,
    accessibilityLabel: String,
) {
    val sweep by animateFloatAsState(
        targetValue = usedFraction.coerceIn(0f, 1f),
        animationSpec = motionSpec(Durations.emphasized * 2),
        label = "budgetDial"
    )
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics {
                contentDescription = accessibilityLabel
                progressBarRangeInfo = ProgressBarRangeInfo(usedFraction.coerceIn(0f, 1f), 0f..1f)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, StartAngle, FullSweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (sweep > 0f) {
                drawArc(color, StartAngle, FullSweep * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Text(
            text = budgetPercentLabel(usedFraction),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Whole percent, rounded down so "100%" only shows once the budget is really used up. */
fun budgetPercentLabel(usedFraction: Float): String =
    "${(usedFraction.coerceAtLeast(0f) * 100).toInt()}%"

private const val StartAngle = 135f
private const val FullSweep = 270f
