package com.akshar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akshar.ui.components.GlassCard
import com.akshar.ui.components.WrapRow
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Appearance
import com.akshar.ui.theme.PillShape
import com.akshar.ui.theme.SoftColorMath
import com.akshar.ui.theme.SoftDepth
import com.akshar.ui.theme.SoftPalette
import com.akshar.ui.theme.SoftPalettes
import com.akshar.ui.theme.Spacing
import com.akshar.ui.theme.money
import com.akshar.ui.theme.softInset
import com.akshar.ui.theme.softRaised
import com.akshar.ui.theme.softSurface

/** Curated accent hues offered as one-tap swatches. None sit in the income or expense bands. */
internal val SoftAccentSwatches = listOf(200f, 222f, 240f, 255f, 278f, 300f, 322f, 36f, 50f, 178f)

/**
 * More › Appearance, shown under the style list while Soft is selected: the three palettes,
 * plus "Your colours" with a base and an accent hue. Changes re-theme the app live.
 */
@Composable
internal fun SoftColoursSection() {
    val context = LocalContext.current
    val current = Appearance.softPalette
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            "Soft colours",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.sm).semantics { heading() }
        )
        WrapRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalSpacing = Spacing.md,
            verticalSpacing = Spacing.md,
        ) {
            SoftPalette.entries.forEach { palette ->
                PaletteTile(
                    palette = palette,
                    selected = palette == current,
                    onSelect = { Appearance.selectSoftPalette(context, palette) },
                )
            }
        }
        if (current == SoftPalette.CUSTOM) CustomColours()
        SoftPreview()
    }
}

@Composable
private fun PaletteTile(palette: SoftPalette, selected: Boolean, onSelect: () -> Unit) {
    val soft = AkSpendTheme.soft ?: return
    val swatch = SoftPalettes.resolve(palette, soft.isDark, Appearance.softBaseHue, Appearance.softAccentHue)
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = Modifier
            .width(76.dp)
            .softSurface(shape, soft, distance = SoftDepth.control) { if (selected) -1f else 1f }
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(swatch.background, swatch.background, swatch.accent))),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            palette.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) soft.accent else soft.muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun CustomColours() {
    val context = LocalContext.current
    val base = Appearance.softBaseHue
    val accent = Appearance.softAccentHue
    val dark = AkSpendTheme.isDark
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Base", style = MaterialTheme.typography.labelLarge)
        HueSlider(
            label = "Base colour",
            hue = base,
            onChange = { Appearance.previewSoftHues(it, accent) },
            onDone = { Appearance.setSoftHues(context, Appearance.softBaseHue, Appearance.softAccentHue) },
            spectrum = { h -> Color.hsl(h, 0.22f, if (dark) 0.24f else 0.86f) },
        )
        Spacer(Modifier.height(Spacing.xs))
        Text("Accent", style = MaterialTheme.typography.labelLarge)
        WrapRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalSpacing = Spacing.sm,
            verticalSpacing = Spacing.sm,
        ) {
            SoftAccentSwatches.forEach { hue ->
                AccentSwatch(
                    hue = hue,
                    selected = kotlin.math.abs(hue - accent) < 0.5f,
                    onSelect = { Appearance.setSoftHues(context, base, hue) },
                )
            }
        }
        HueSlider(
            label = "Accent colour",
            hue = accent,
            onChange = { Appearance.previewSoftHues(base, SoftColorMath.safeAccentHue(it)) },
            onDone = { Appearance.setSoftHues(context, Appearance.softBaseHue, Appearance.softAccentHue) },
            spectrum = { h ->
                // Reserved bands show as the page colour so it is clear they are skipped.
                if (SoftColorMath.isReservedHue(h)) Color.Transparent
                else Color.hsl(h, 0.6f, if (dark) 0.7f else 0.5f)
            },
        )
        Text(
            "Greens and reds are kept for money in and out. Text colours adjust to stay readable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccentSwatch(hue: Float, selected: Boolean, onSelect: () -> Unit) {
    val soft = AkSpendTheme.soft ?: return
    val color = SoftPalettes.custom(Appearance.softBaseHue, hue, soft.isDark).accent
    Box(
        modifier = Modifier
            .size(48.dp)
            .softSurface(CircleShape, soft, distance = SoftDepth.small) { if (selected) -1f else 1f }
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .semantics { contentDescription = hueName(hue) },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(if (selected) 18.dp else 24.dp).clip(CircleShape).background(color))
    }
}

/** A hue slider whose track shows the colours it picks from, pressed into the page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HueSlider(
    label: String,
    hue: Float,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
    spectrum: (Float) -> Color,
) {
    val soft = AkSpendTheme.soft ?: return
    val stops = (0..12).map { i -> spectrum(i * 30f).takeIf { it != Color.Transparent } ?: soft.track }
    androidx.compose.material3.Slider(
        value = hue,
        onValueChange = onChange,
        onValueChangeFinished = onDone,
        valueRange = 0f..360f,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget)
            .semantics {
                contentDescription = label
                stateDescription = hueName(hue)
            },
        thumb = {
            Box(
                Modifier.size(28.dp).softRaised(CircleShape, soft, distance = SoftDepth.small),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(spectrum(hue).takeIf { it != Color.Transparent } ?: soft.track))
            }
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .softInset(PillShape, soft, distance = 2.dp)
                    .padding(3.dp)
                    .clip(PillShape)
                    .background(Brush.horizontalGradient(stops))
            )
        },
    )
}

/** A plain-language name for a hue, read by TalkBack instead of a number of degrees. */
internal fun hueName(hue: Float): String = when (SoftColorMath.normalizeHue(hue)) {
    in 0f..15f -> "Red"
    in 15f..45f -> "Orange"
    in 45f..70f -> "Yellow"
    in 70f..165f -> "Green"
    in 165f..195f -> "Teal"
    in 195f..225f -> "Sky blue"
    in 225f..250f -> "Blue"
    in 250f..275f -> "Violet"
    in 275f..300f -> "Purple"
    in 300f..335f -> "Magenta"
    else -> "Rose"
}

/** A small sample of the current palette: a raised card, money in and out, and a filled button. */
@Composable
private fun SoftPreview() {
    val finance = AkSpendTheme.finance
    // A picture of the palette, not controls: TalkBack reads it as one line.
    GlassCard(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Preview of these colours" }) {
        Text("Preview", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Salary", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("+52,000.00", style = MaterialTheme.typography.titleMedium.money(), color = finance.income)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Groceries", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("−420.00", style = MaterialTheme.typography.titleMedium.money(), color = finance.expense)
        }
        Spacer(Modifier.height(Spacing.md))
        val soft = AkSpendTheme.soft
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .then(if (soft != null) Modifier.softRaised(PillShape, soft, fill = soft.accent, distance = SoftDepth.control) else Modifier)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("Add transaction", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
