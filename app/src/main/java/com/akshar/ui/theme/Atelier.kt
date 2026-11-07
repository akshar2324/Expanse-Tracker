package com.akshar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.akshar.R

// AkSpend "Atelier": premium minimalism. Ivory and ink with one quiet champagne accent,
// serif numerals, hairline rules instead of heavy cards. Text pairings meet WCAG AA (4.5:1)
// on the ivory/black backgrounds; champagne is only used for small decorative marks in light
// mode, where a darker bronze carries any text.

/** Instrument Serif (SIL OFL 1.1), bundled in res/font so nothing is fetched at runtime. */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

private val Ivory = Color(0xFFF7F5F0)
private val IvoryRaised = Color(0xFFFCFBF8)
private val Ink = Color(0xFF121212)
private val InkMuted = Color(0xFF66625B)
private val Hairline = Color(0xFFE3DFD6)
private val Bronze = Color(0xFF7A6440)

private val Night = Color(0xFF0C0C0C)
private val NightRaised = Color(0xFF161615)
private val Parchment = Color(0xFFF2EEE6)
private val ParchmentMuted = Color(0xFFA39E95)
private val NightHairline = Color(0xFF282725)
private val Champagne = Color(0xFFC8AE7E)

internal val AtelierLightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Ivory,
    primaryContainer = Color(0xFFECE8E0),
    onPrimaryContainer = Ink,
    secondary = Bronze,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE7D8),
    onSecondaryContainer = Color(0xFF3A2E1A),
    tertiary = Color(0xFF4F5A57),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E8E4),
    onTertiaryContainer = Color(0xFF1C2421),
    background = Color.Transparent,
    onBackground = Ink,
    surface = IvoryRaised,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFECE5),
    onSurfaceVariant = InkMuted,
    surfaceTint = Color.Transparent,
    outline = Color(0xFF9A968E),
    outlineVariant = Hairline,
    error = Color(0xFF9E4630),
    onError = Color.White,
    errorContainer = Color(0xFFF4E2DC),
    onErrorContainer = Color(0xFF3E140A),
    surfaceBright = IvoryRaised,
    surfaceDim = Color(0xFFE9E6DF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = IvoryRaised,
    surfaceContainer = Color(0xFFF3F1EB),
    surfaceContainerHigh = Color(0xFFEFECE5),
    surfaceContainerHighest = Color(0xFFE9E6DF),
)

internal val AtelierDarkColorScheme = darkColorScheme(
    primary = Champagne,
    onPrimary = Night,
    primaryContainer = Color(0xFF2A251C),
    onPrimaryContainer = Color(0xFFEBDDBF),
    secondary = Champagne,
    onSecondary = Night,
    secondaryContainer = Color(0xFF2A251C),
    onSecondaryContainer = Color(0xFFEBDDBF),
    tertiary = Color(0xFFB9C4C0),
    onTertiary = Night,
    tertiaryContainer = Color(0xFF232826),
    onTertiaryContainer = Color(0xFFDCE4E1),
    background = Color.Transparent,
    onBackground = Parchment,
    surface = NightRaised,
    onSurface = Parchment,
    surfaceVariant = Color(0xFF1C1C1B),
    onSurfaceVariant = ParchmentMuted,
    surfaceTint = Color.Transparent,
    outline = Color(0xFF6E6A63),
    outlineVariant = NightHairline,
    error = Color(0xFFE08E78),
    onError = Night,
    errorContainer = Color(0xFF3A1D15),
    onErrorContainer = Color(0xFFF6D5CB),
    surfaceBright = Color(0xFF242423),
    surfaceDim = Night,
    surfaceContainerLowest = Color(0xFF080808),
    surfaceContainerLow = Color(0xFF121211),
    surfaceContainer = NightRaised,
    surfaceContainerHigh = Color(0xFF1C1C1B),
    surfaceContainerHighest = Color(0xFF242423),
)

internal val AtelierLightFinance = FinanceColors(
    income = Color(0xFF3F6A4E),
    expense = Color(0xFF9E4630),
    warning = Color(0xFF7E5F22),
    info = Color(0xFF465B78),
    transfer = Color(0xFF5E5578),
    chart = listOf(
        Color(0xFF2F2F2F), Color(0xFFB29A6C), Color(0xFF7F95A8), Color(0xFFAE8580),
        Color(0xFF93A07E), Color(0xFF75708A), Color(0xFF6F8F86), Color(0xFFB5A48F),
    ),
)

internal val AtelierDarkFinance = FinanceColors(
    income = Color(0xFF9CC3A6),
    expense = Color(0xFFE08E78),
    warning = Color(0xFFDDBD7E),
    info = Color(0xFFA9BCD6),
    transfer = Color(0xFFBFB5DA),
    chart = listOf(
        Color(0xFFE9E4DA), Color(0xFFC8AE7E), Color(0xFF9DB2C4), Color(0xFFCBA39D),
        Color(0xFFB2BF9C), Color(0xFFA49FBA), Color(0xFF8FB0A6), Color(0xFFCDBDA6),
    ),
)

// Panels in Atelier are near-invisible: a whisper of fill and a single even hairline, no sheen,
// no glow, no shadow. The backdrop is one flat colour.
internal val AtelierLightSurfaces = GlassColors(
    base = Ivory,
    baseBottom = Ivory,
    blobs = listOf(Ivory, Ivory, Ivory),
    fill = Color(0x59FFFFFF),
    fillOpaque = Ivory,
    fillStrong = Color(0x99FFFFFF),
    highlight = Hairline,
    edge = Hairline,
    sheen = Color.Transparent,
    shadow = Color.Transparent,
)

internal val AtelierDarkSurfaces = GlassColors(
    base = Night,
    baseBottom = Night,
    blobs = listOf(Night, Night, Night),
    fill = Color(0x0AFFFFFF),
    fillOpaque = Night,
    fillStrong = Color(0x12FFFFFF),
    highlight = NightHairline,
    edge = NightHairline,
    sheen = Color.Transparent,
    shadow = Color.Transparent,
)

private fun serif(size: Int, line: Int, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking,
)

private fun sans(size: Int, line: Int, weight: FontWeight, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking,
)

// Display and headline sizes are set in the serif, a touch larger because Instrument Serif has a
// small x-height. Everything a user reads or taps stays in Inter, one weight lighter than the
// glass style so the page feels quieter.
internal val AtelierTypography = Typography(
    displayLarge = serif(60, 64, (-0.01).em),
    displayMedium = serif(50, 56, (-0.01).em),
    displaySmall = serif(40, 46),
    headlineLarge = serif(36, 42),
    headlineMedium = serif(30, 36),
    headlineSmall = serif(26, 32),
    titleLarge = sans(19, 26, FontWeight.Medium, (-0.01).em),
    titleMedium = sans(16, 24, FontWeight.Medium),
    titleSmall = sans(14, 20, FontWeight.Medium),
    bodyLarge = sans(16, 24, FontWeight.Normal),
    bodyMedium = sans(14, 21, FontWeight.Normal),
    bodySmall = sans(13, 18, FontWeight.Normal),
    labelLarge = sans(14, 20, FontWeight.Medium, 0.02.em),
    labelMedium = sans(12, 16, FontWeight.Medium, 0.04.em),
    labelSmall = sans(11, 16, FontWeight.Medium, 0.08.em),
)

// Quieter, tighter corners than glass. Pills stay fully round.
internal val AtelierShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)
