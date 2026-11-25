package com.akshar.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Small, meaningful vibrations for key moments. They go through the View, so they follow the
 * phone's own touch-feedback setting and do nothing when the user has turned haptics off.
 */
class AppHaptics(private val view: View) {
    /** Something was saved. */
    fun confirm() { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }

    /** The input was rejected, for example an invalid amount. */
    fun reject() { view.performHapticFeedback(HapticFeedbackConstants.REJECT) }

    /** Something was deleted, or a limit was crossed. A firmer tick than [confirm]. */
    fun heavy() { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = LocalView.current
    return remember(view) { AppHaptics(view) }
}
