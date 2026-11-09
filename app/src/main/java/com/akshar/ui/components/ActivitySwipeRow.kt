package com.akshar.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.abs

internal enum class ActivitySwipeAction { EDIT, DELETE }

internal fun activitySwipeAction(distance: Float, threshold: Float): ActivitySwipeAction? = when {
    distance >= threshold -> ActivitySwipeAction.EDIT
    distance <= -threshold -> ActivitySwipeAction.DELETE
    else -> null
}

/**
 * Physical right/left gestures. Release snaps to rest, so no settling or fling animation is
 * scheduled, including when reduced motion is enabled.
 */
@Composable
fun ActivitySwipeRow(onEdit: () -> Unit, onDelete: () -> Unit, content: @Composable (Modifier) -> Unit) {
    val edit by rememberUpdatedState(onEdit)
    val delete by rememberUpdatedState(onDelete)
    var width by remember { mutableFloatStateOf(0f) }
    var distance by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val threshold = minOf(width * 0.35f, with(density) { 120.dp.toPx() })
    val offset = distance
    Box(Modifier.fillMaxWidth().clipToBounds().onSizeChanged { width = it.width.toFloat() }
        .pointerInput(threshold) {
            detectHorizontalDragGestures(
                onDragStart = { distance = 0f },
                onHorizontalDrag = { change, delta ->
                    change.consume()
                    distance = (distance + delta).coerceIn(-width, width)
                },
                onDragCancel = { distance = 0f },
                onDragEnd = {
                    val action = if (threshold > 0f) activitySwipeAction(distance, threshold) else null
                    distance = 0f
                    when (action) {
                        ActivitySwipeAction.EDIT -> edit()
                        ActivitySwipeAction.DELETE -> delete()
                        null -> Unit
                    }
                },
            )
        }) {
        if (offset != 0f) {
            // The hint is an overlay so large fonts cannot resize a row during a drag.
            Box(Modifier.matchParentSize().clearAndSetSemantics { }) {
                Box(Modifier.align(if (offset > 0) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                    .width(with(density) { abs(offset).toDp() }).clipToBounds()) {
                    Text(
                        if (offset > 0) "Edit" else "Delete",
                        color = if (offset > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.align(if (offset > 0) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight).padding(16.dp),
                    )
                }
            }
        }
        Box(Modifier.absoluteOffset { IntOffset(offset.roundToInt(), 0) }) {
            content(Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Edit") { edit(); true },
                    CustomAccessibilityAction("Delete") { delete(); true },
                )
            })
        }
    }
}
