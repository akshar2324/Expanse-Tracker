package com.akshar.ui.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Durations
import com.akshar.ui.theme.PillShape
import com.akshar.ui.theme.Spacing
import com.akshar.ui.theme.eyebrow
import com.akshar.ui.theme.glassPanel

/**
 * Extra bottom space a scrolling screen adds so its last item can scroll clear of the floating
 * tab bar. Zero on routes where the bar is hidden and on wide layouts with a navigation rail.
 */
val LocalFloatingBarInset = compositionLocalOf { 0.dp }

/** Tween that collapses to an instant snap when the user has turned animations off. */
@Composable
fun <T> motionSpec(durationMillis: Int = Durations.standard): FiniteAnimationSpec<T> =
    if (AkSpendTheme.reducedMotion) snap() else tween(durationMillis, easing = FastOutSlowInEasing)

/** A frosted glass container. Pass [onClick] to make the whole card one large touch target. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    fill: Color = AkSpendTheme.glass.fill,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = AkSpendTheme.glass
    Column(
        modifier = modifier
            .glassPanel(shape, glass, fill)
            .then(
                if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/**
 * Large-title screen header in the spirit of iOS navigation bars. Draws no background, so the
 * glass backdrop runs edge to edge behind the status bar.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    insetHorizontally: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (insetHorizontally) Spacing.gutter else 0.dp,
                end = if (insetHorizontally) Spacing.md else 0.dp,
                top = Spacing.sm,
                bottom = Spacing.md
            )
    ) {
        if (onBack != null || actions != null) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                }
                Spacer(Modifier.weight(1f))
                if (actions != null) Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
            Spacer(Modifier.height(Spacing.sm))
        } else {
            Spacer(Modifier.height(Spacing.lg))
        }
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.eyebrow(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.xxs))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Round glass icon button with a 48dp touch target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(Spacing.touchTarget)
            .glassPanel(CircleShape, AkSpendTheme.glass, AkSpendTheme.glass.fillStrong)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Section title with an optional trailing text action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Tinted circular icon. Decorative by default; pass [contentDescription] when it carries meaning. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = if (AkSpendTheme.isDark) 0.20f else 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Two-letter monogram badge, used for categories that have no icon. */
@Composable
fun MonogramBadge(text: String, tint: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = if (AkSpendTheme.isDark) 0.20f else 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

/**
 * Capsule segmented control with a sliding glass thumb. Every segment is a 48dp-tall
 * selectable so TalkBack reads "selected, 1 of 3".
 */
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = AkSpendTheme.glass
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.touchTarget + Spacing.xs)
            .glassPanel(PillShape, glass, glass.fill)
            .padding(Spacing.xxs)
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(segmentWidth * selectedIndex, motionSpec(), label = "segment")
        Box(
            Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .glassPanel(PillShape, glass, MaterialTheme.colorScheme.primary)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(PillShape)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Friendly empty state shown inside a glass card. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.xxl)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            IconBadge(icon = icon, tint = MaterialTheme.colorScheme.primary, size = 56.dp)
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (action != null) {
                Spacer(Modifier.height(Spacing.xs))
                action()
            }
        }
    }
}

/** Compact glass stat tile: tinted icon, small label and a tabular value. */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, contentPadding = PaddingValues(Spacing.lg)) {
        IconBadge(icon = icon, tint = tint, size = 36.dp)
        Spacer(Modifier.height(Spacing.md))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Glass list row with a leading badge, title/subtitle and trailing slot; whole row is tappable. */
@Composable
fun GlassListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

/** Outlined text field colours that sit well on glass: translucent fill, AA-contrast outline. */
@Composable
fun glassTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AkSpendTheme.glass.fillStrong,
    unfocusedContainerColor = AkSpendTheme.glass.fill,
    disabledContainerColor = AkSpendTheme.glass.fill,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
)

/** Card colours for Material cards placed on the glass backdrop. */
@Composable
fun glassCardColors(container: Color = AkSpendTheme.glass.fill) =
    androidx.compose.material3.CardDefaults.cardColors(containerColor = container)

/** Hairline rim for Material cards so they read as glass panels. */
@Composable
fun glassBorder() = androidx.compose.foundation.BorderStroke(1.dp, AkSpendTheme.glass.highlight.copy(alpha = AkSpendTheme.glass.highlight.alpha * 0.6f))
