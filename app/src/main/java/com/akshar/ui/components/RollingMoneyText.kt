package com.akshar.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import com.akshar.ui.theme.AkSpendTheme

/**
 * Shows [text] (an already formatted amount) and, when it changes, rolls only the characters that
 * changed, like an odometer: up when [value] grew, down when it shrank. Every frame shows a real
 * amount, never an in-between number. The first show and reduced motion are static.
 */
@Composable
fun RollingMoneyText(
    text: String,
    value: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = text,
) {
    val reducedMotion = AkSpendTheme.reducedMotion
    val semanticsModifier = modifier.clearAndSetSemantics {
        contentDescription = accessibilityLabel
        this.text = AnnotatedString(text)
    }
    if (reducedMotion) {
        Text(text = text, style = style, color = color, maxLines = 1, modifier = semanticsModifier)
        return
    }
    val previous = remember { DoubleArray(1) { value } }
    val rising = value >= previous[0]
    previous[0] = value
    Row(semanticsModifier) {
        // Keyed from the right so units stay with units when the number gets longer or shorter.
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val sign = if (rising) 1 else -1
                        (slideInVertically(tween(RollMillis)) { sign * it } + fadeIn(tween(RollMillis)))
                            .togetherWith(slideOutVertically(tween(RollMillis)) { -sign * it } + fadeOut(tween(RollMillis)))
                            .using(SizeTransform(clip = true))
                    },
                    label = "rollingDigit"
                ) { shown ->
                    Text(text = shown.toString(), style = style, color = color, maxLines = 1)
                }
            }
        }
    }
}

private const val RollMillis = 280
