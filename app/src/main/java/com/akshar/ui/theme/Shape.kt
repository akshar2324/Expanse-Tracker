package com.akshar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Generous, continuous-feeling corners. Glass panels use `large`; chips and fields use `small`.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Fully rounded capsule used by the tab bar, segmented controls and pill buttons. */
val PillShape = RoundedCornerShape(percent = 50)
