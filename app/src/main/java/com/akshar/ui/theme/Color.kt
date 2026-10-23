package com.akshar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// AkSpend "Liquid Glass" palette: warm, calm neutrals with a deep teal accent.
// Every text/background pairing below was checked for WCAG AA (4.5:1) against the
// glass panel colour it sits on, including the fallback panels used without blur.

// --- Light ---
val LightPrimary = Color(0xFF235F57)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFCFEAE3)
val LightOnPrimaryContainer = Color(0xFF0B3A33)
val LightSecondary = Color(0xFF7D4E2E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFF7E1D2)
val LightOnSecondaryContainer = Color(0xFF3A2212)
val LightTertiary = Color(0xFF5A4BA8)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFE6E0FF)
val LightOnTertiaryContainer = Color(0xFF241A5E)
val LightSurface = Color(0xFFFFFCF9)
val LightOnSurface = Color(0xFF1F1A16)
val LightSurfaceVariant = Color(0xFFF0E9E2)
val LightOnSurfaceVariant = Color(0xFF574E46)
val LightOutline = Color(0xFF8A8077)
val LightOutlineVariant = Color(0xFFDDD3C9)
val LightBase = Color(0xFFF8F3ED)
val LightBaseBottom = Color(0xFFF1ECF4)

// --- Dark ---
val DarkPrimary = Color(0xFF8ED4C6)
val DarkOnPrimary = Color(0xFF00382F)
val DarkPrimaryContainer = Color(0xFF1E4C45)
val DarkOnPrimaryContainer = Color(0xFFC8F1E7)
val DarkSecondary = Color(0xFFF0BD98)
val DarkOnSecondary = Color(0xFF472610)
val DarkSecondaryContainer = Color(0xFF5C3A23)
val DarkOnSecondaryContainer = Color(0xFFFFDCC5)
val DarkTertiary = Color(0xFFC9BFFF)
val DarkOnTertiary = Color(0xFF2C2170)
val DarkTertiaryContainer = Color(0xFF433694)
val DarkOnTertiaryContainer = Color(0xFFE7E1FF)
val DarkSurface = Color(0xFF1C1815)
val DarkOnSurface = Color(0xFFF0E9E2)
val DarkSurfaceVariant = Color(0xFF3A332D)
val DarkOnSurfaceVariant = Color(0xFFD2C7BD)
val DarkOutline = Color(0xFF9B9086)
val DarkOutlineVariant = Color(0xFF4B433C)
val DarkBase = Color(0xFF110F0D)
val DarkBaseBottom = Color(0xFF16121B)

/** Meaning-carrying colours that the Material scheme has no slot for. */
@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val info: Color,
    val transfer: Color,
    /** Muted, theme-aware palette for chart series and category badges. */
    val chart: List<Color>,
)

val LightFinanceColors = FinanceColors(
    income = Color(0xFF15734B),
    expense = Color(0xFFB83A33),
    warning = Color(0xFF94560A),
    info = Color(0xFF3557A6),
    transfer = Color(0xFF5A4BA8),
    chart = listOf(
        Color(0xFF2E7D72), Color(0xFFD9825B), Color(0xFF6C5CC4), Color(0xFFC9A227),
        Color(0xFF4D8FCC), Color(0xFFCC5F7A), Color(0xFF7FA35A), Color(0xFF8C7B6B),
    ),
)

val DarkFinanceColors = FinanceColors(
    income = Color(0xFF72DDA8),
    expense = Color(0xFFFF9389),
    warning = Color(0xFFF4BA62),
    info = Color(0xFFA2BBFF),
    transfer = Color(0xFFC9BFFF),
    chart = listOf(
        Color(0xFF7FD3C4), Color(0xFFF2A884), Color(0xFFB4A8FF), Color(0xFFEBCB6B),
        Color(0xFF8EC2F2), Color(0xFFF29BB0), Color(0xFFB2D58C), Color(0xFFC9B8A7),
    ),
)

/** Colours for translucent glass panels and the backdrop they float over. */
@Immutable
data class GlassColors(
    val base: Color,
    val baseBottom: Color,
    val blobs: List<Color>,
    /** Panel fill when the backdrop is blurred behind it (API 31+). */
    val fill: Color,
    /** Panel fill for bars that content scrolls under when real blur is unavailable. */
    val fillOpaque: Color,
    val fillStrong: Color,
    val highlight: Color,
    val edge: Color,
    val sheen: Color,
    val shadow: Color,
)

val LightGlassColors = GlassColors(
    base = LightBase,
    baseBottom = LightBaseBottom,
    blobs = listOf(Color(0xFFFFCFAE), Color(0xFFC6E8D8), Color(0xFFDCD3FF)),
    fill = Color(0xA6FFFFFF),
    fillOpaque = Color(0xF2FFFCF9),
    fillStrong = Color(0xD9FFFFFF),
    highlight = Color(0xE6FFFFFF),
    edge = Color(0x1F5A4030),
    sheen = Color(0x80FFFFFF),
    shadow = Color(0x335A4030),
)

val DarkGlassColors = GlassColors(
    base = DarkBase,
    baseBottom = DarkBaseBottom,
    blobs = listOf(Color(0xFF15342F), Color(0xFF2A1D3C), Color(0xFF33210F)),
    fill = Color(0x1AFFFFFF),
    fillOpaque = Color(0xF2221E1B),
    fillStrong = Color(0x1FFFFFFF),
    highlight = Color(0x3DFFFFFF),
    edge = Color(0x14FFFFFF),
    sheen = Color(0x14FFFFFF),
    shadow = Color(0x66000000),
)
