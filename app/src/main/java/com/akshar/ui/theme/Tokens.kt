package com.akshar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp-based spacing scale. Prefer these over ad-hoc dp literals in new UI. */
@Immutable
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp

    /** Horizontal gutter between screen edges and content. */
    val gutter: Dp = 20.dp

    /** Minimum accessible touch target. */
    val touchTarget: Dp = 48.dp
}

/** Motion durations in ms. Collapse to zero when the user turns animations off. */
@Immutable
object Durations {
    const val quick = 150
    const val standard = 260
    const val emphasized = 420
    const val settle = 5_000
}
