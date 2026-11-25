package com.akshar.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akshar.ui.theme.Spacing

/**
 * Places children left to right and wraps to a new line when the next one does not fit.
 * Hand-rolled because the app compiles against foundation-layout 1.7 but runs with 1.9, where
 * the 1.7 FlowRow signature no longer exists (NoSuchMethodError at runtime).
 */
@Composable
fun WrapRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = Spacing.sm,
    verticalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val positions = ArrayList<androidx.compose.ui.unit.IntOffset>(placeables.size)
        var x = 0
        var y = 0
        var rowHeight = 0
        var width = 0
        placeables.forEach { placeable ->
            if (x > 0 && x + placeable.width > maxWidth) {
                x = 0
                y += rowHeight + vGap
                rowHeight = 0
            }
            positions += androidx.compose.ui.unit.IntOffset(x, y)
            width = maxOf(width, x + placeable.width)
            x += placeable.width + hGap
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        val height = if (placeables.isEmpty()) 0 else y + rowHeight
        layout(
            width.coerceAtMost(maxWidth).coerceAtLeast(constraints.minWidth),
            height.coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(positions[index]) }
        }
    }
}
