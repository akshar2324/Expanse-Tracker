package com.akshar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.akshar.R

/** Inter (SIL OFL 1.1), bundled in res/font so nothing is fetched at runtime. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking,
)

// Tighter tracking on large sizes and slightly open tracking on small labels, the way
// Inter's optical sizes are meant to be set.
val Typography = Typography(
    displayLarge = style(52, 60, FontWeight.Bold, (-0.03).em),
    displayMedium = style(42, 50, FontWeight.Bold, (-0.025).em),
    displaySmall = style(34, 42, FontWeight.Bold, (-0.02).em),
    headlineLarge = style(30, 38, FontWeight.Bold, (-0.02).em),
    headlineMedium = style(26, 34, FontWeight.SemiBold, (-0.015).em),
    headlineSmall = style(22, 30, FontWeight.SemiBold, (-0.01).em),
    titleLarge = style(20, 28, FontWeight.SemiBold, (-0.01).em),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 21, FontWeight.Normal),
    bodySmall = style(13, 18, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.01.em),
    labelMedium = style(12, 16, FontWeight.Medium, 0.02.em),
    labelSmall = style(11, 16, FontWeight.Medium, 0.04.em),
)

/** Tabular figures keep columns of money aligned and stop totals from jittering as they change. */
fun TextStyle.money(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Small all-caps eyebrow label used above sections and hero numbers. */
fun TextStyle.eyebrow(): TextStyle = copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em)
