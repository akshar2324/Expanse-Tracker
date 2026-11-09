package com.akshar.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Durations
import com.akshar.ui.theme.PillShape
import com.akshar.ui.theme.SoftColors
import com.akshar.ui.theme.SoftDepth
import com.akshar.ui.theme.softInset
import com.akshar.ui.theme.softRaised
import com.akshar.ui.theme.softRim
import com.akshar.ui.theme.softSurface

// ---------------------------------------------------------------------------------------------
// Drop-in replacements for the Material 3 controls the screens use, with the same names and
// parameters. Screens import these instead of androidx.compose.material3.*; outside the Soft
// style each one simply calls the Material control, so Atelier and Glass are unchanged.
//
// In Soft every control is neumorphic: buttons, chips and cards are raised and press in when
// touched; fields, tracks and unchecked boxes are wells pressed into the page. Semantics match
// Material's (roles, toggle and selection state, 48dp touch targets) so TalkBack reads the same.
// ---------------------------------------------------------------------------------------------

/** Depth that sinks while pressed and goes flush when disabled. Snaps under reduced motion. */
@Composable
private fun pressDepth(source: MutableInteractionSource, enabled: Boolean, rest: Float = 1f): () -> Float {
    val pressed by source.collectIsPressedAsState()
    val depth by animateFloatAsState(
        targetValue = when {
            !enabled -> 0f
            pressed -> -0.8f
            else -> rest
        },
        animationSpec = motionSpec(Durations.quick),
        label = "softPress",
    )
    return { depth }
}

@Composable
private fun SoftButtonBase(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    fill: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    distance: Dp,
    soft: SoftColors,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val depth = pressDepth(source, enabled)
    val shownFill = if (enabled) fill.compositeOver(soft.background) else soft.background
    val shownContent = if (enabled) contentColor else soft.muted.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .softSurface(shape, soft, shownFill, distance, depth)
            .clip(shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = ButtonDefaults.MinWidth, minHeight = ButtonDefaults.MinHeight)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides shownContent,
            LocalTextStyle provides MaterialTheme.typography.labelLarge,
        ) { content() }
    }
}

// --- Buttons ---------------------------------------------------------------------------------

/** Filled button. In Soft: a raised shape in the accent (or the given container colour). */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.Button(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
            elevation = elevation, border = border, contentPadding = contentPadding,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }, content = content,
        )
        return
    }
    SoftButtonBase(
        onClick, modifier, enabled, shape, colors.containerColor, colors.contentColor, contentPadding,
        interactionSource, SoftDepth.control, soft, content,
    )
}

/** Secondary button. In Soft: a raised shape in the page colour with accent text, no outline. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
            elevation = elevation, border = border, contentPadding = contentPadding,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }, content = content,
        )
        return
    }
    SoftButtonBase(
        onClick, modifier, enabled, shape, colors.containerColor, colors.contentColor, contentPadding,
        interactionSource, SoftDepth.control, soft, content,
    )
}

/** Tonal button. In Soft: a raised shape tinted by its container colour. */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.FilledTonalButton(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
            elevation = elevation, border = border, contentPadding = contentPadding,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }, content = content,
        )
        return
    }
    SoftButtonBase(
        onClick, modifier, enabled, shape, colors.containerColor, colors.contentColor, contentPadding,
        interactionSource, SoftDepth.control, soft, content,
    )
}

/** Text button. In Soft: a small raised pill, so every tappable thing reads as one. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.TextButton(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
            elevation = elevation, border = border, contentPadding = contentPadding,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }, content = content,
        )
        return
    }
    SoftButtonBase(
        onClick, modifier, enabled, PillShape, colors.containerColor, colors.contentColor,
        PaddingValues(horizontal = 16.dp, vertical = 8.dp), interactionSource, SoftDepth.small, soft, content,
    )
}

/** Icon button. In Soft: a raised 40dp circle inside the 48dp touch target. */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.IconButton(
            onClick = onClick, modifier = modifier, enabled = enabled, colors = colors,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }, content = content,
        )
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val depth = pressDepth(source, enabled)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .softSurface(CircleShape, soft, colors.containerColor.compositeOver(soft.background), SoftDepth.small, depth)
            .clip(CircleShape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (enabled) colors.contentColor else soft.muted.copy(alpha = 0.6f)
        CompositionLocalProvider(LocalContentColor provides tint, content = content)
    }
}

/** Floating action button. In Soft: a raised shape in its container colour. */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick, modifier = modifier, shape = shape, containerColor = containerColor,
            contentColor = contentColor, elevation = elevation,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }, content = content,
        )
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val depth = pressDepth(source, enabled = true)
    Box(
        modifier = modifier
            .softSurface(shape, soft, containerColor.compositeOver(soft.background), SoftDepth.panel, depth)
            .clip(shape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

// --- Selection controls ----------------------------------------------------------------------

/** Filter chip. In Soft: raised when off, pressed in (and tinted) when selected. */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.FilterChip(
            selected = selected, onClick = onClick, label = label, modifier = modifier, enabled = enabled,
            leadingIcon = leadingIcon, trailingIcon = trailingIcon, shape = shape, colors = colors,
            elevation = elevation, border = border,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        )
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val depth = pressDepth(source, enabled, rest = if (selected) -1f else 1f)
    // Soft chips ignore Material chip colours (they are not readable) and use one rule: raised
    // and muted when off, pressed into a track-coloured well with accent text when selected.
    // Both pairs are held at AA contrast by SoftColors.guarded().
    val fill = if (selected) soft.track else soft.background
    val labelColor = when {
        !enabled -> soft.muted.copy(alpha = 0.6f)
        selected -> soft.accent
        else -> soft.muted
    }
    val iconColor = labelColor
    Row(
        modifier = modifier
            .semantics { role = Role.Checkbox }
            .minimumInteractiveComponentSize()
            .softSurface(shape, soft, fill, SoftDepth.small, depth)
            .clip(shape)
            .selectable(selected = selected, interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .height(32.dp)
            .padding(start = if (leadingIcon != null) 8.dp else 14.dp, end = if (trailingIcon != null) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leadingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides iconColor) {
                Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { leadingIcon() }
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides labelColor,
            LocalTextStyle provides MaterialTheme.typography.labelLarge,
        ) { label() }
        if (trailingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides labelColor) {
                Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { trailingIcon() }
            }
        }
    }
}

/** Switch. In Soft: a pressed-in track with a raised thumb that turns accent when on. */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.Switch(
            checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, thumbContent = thumbContent,
            enabled = enabled, colors = colors,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        )
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val thumbX by animateDpAsState(if (checked) 24.dp else 4.dp, motionSpec(Durations.quick), label = "softSwitch")
    val trackFill = if (checked) lerp(soft.track, soft.accent, 0.22f) else soft.track
    val thumbFill = when {
        !enabled -> soft.background
        checked -> soft.accent
        else -> soft.background
    }
    Box(
        modifier = modifier
            .then(
                if (onCheckedChange != null) Modifier.toggleable(
                    value = checked, interactionSource = source, indication = null, enabled = enabled,
                    role = Role.Switch, onValueChange = onCheckedChange,
                ) else Modifier
            )
            .minimumInteractiveComponentSize()
            .size(width = 52.dp, height = 32.dp)
            .softInset(PillShape, soft, fill = trackFill),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbX)
                .size(24.dp)
                .softRaised(CircleShape, soft, fill = thumbFill, distance = SoftDepth.small),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbContent != null) {
                CompositionLocalProvider(LocalContentColor provides if (checked) soft.onAccent else soft.muted, content = thumbContent)
            }
        }
    }
}

/** Checkbox. In Soft: an empty well when off, a raised accent tile with a tick when on. */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.Checkbox(
            checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled,
            colors = colors, interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        )
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .then(
                if (onCheckedChange != null) Modifier.toggleable(
                    value = checked, interactionSource = source, indication = null, enabled = enabled,
                    role = Role.Checkbox, onValueChange = onCheckedChange,
                ) else Modifier
            )
            .minimumInteractiveComponentSize()
            .size(22.dp)
            .then(
                if (checked) Modifier.softRaised(shape, soft, fill = if (enabled) soft.accent else soft.muted, distance = SoftDepth.small)
                else Modifier.softInset(shape, soft, fill = soft.track, distance = 2.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = soft.onAccent, modifier = Modifier.size(16.dp))
    }
}

/** Radio button. In Soft: a round well with a raised accent dot when selected. */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.RadioButton(
            selected = selected, onClick = onClick, modifier = modifier, enabled = enabled, colors = colors,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        )
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.selectable(
                    selected = selected, interactionSource = source, indication = null, enabled = enabled,
                    role = Role.RadioButton, onClick = onClick,
                ) else Modifier
            )
            .minimumInteractiveComponentSize()
            .size(24.dp)
            .softInset(CircleShape, soft, fill = soft.track, distance = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(12.dp).softRaised(CircleShape, soft, fill = if (enabled) soft.accent else soft.muted, distance = 2.dp))
        }
    }
}

/** Slider. In Soft: a pressed-in track filled with the accent and a raised round thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val soft = AkSpendTheme.soft
    val source = interactionSource ?: remember { MutableInteractionSource() }
    if (soft == null) {
        androidx.compose.material3.Slider(
            value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
            valueRange = valueRange, steps = steps, onValueChangeFinished = onValueChangeFinished,
            colors = colors, interactionSource = source,
        )
        return
    }
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = source,
        steps = steps,
        valueRange = valueRange,
        thumb = {
            Box(
                Modifier
                    .size(26.dp)
                    .softRaised(CircleShape, soft, distance = SoftDepth.small),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (enabled) soft.accent else soft.muted))
            }
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
            val fillColor = if (enabled) soft.accent else soft.muted
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .softInset(PillShape, soft, fill = soft.track, distance = 2.dp)
                    .drawBehind {
                        val h = size.height
                        drawRoundRect(
                            color = fillColor,
                            size = Size(size.width * fraction, h),
                            cornerRadius = CornerRadius(h / 2f, h / 2f),
                        )
                    }
            )
        },
    )
}

// --- Text fields -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Outlined text field. In Soft it becomes a well pressed into the page with the label inside
 * it; the accent outline appears only while focused (or red in error), so focus stays visible.
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    val soft = AkSpendTheme.soft
    val source = interactionSource ?: remember { MutableInteractionSource() }
    if (soft == null) {
        androidx.compose.material3.OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
            readOnly = readOnly, textStyle = textStyle, label = label, placeholder = placeholder,
            leadingIcon = leadingIcon, trailingIcon = trailingIcon, prefix = prefix, suffix = suffix,
            supportingText = supportingText, isError = isError, visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, singleLine = singleLine,
            maxLines = maxLines, minLines = minLines, interactionSource = source, shape = shape, colors = colors,
        )
        return
    }
    val focused by source.collectIsFocusedAsState()
    val fieldColors = softTextFieldColors(soft)
    val textColor = textStyle.color.takeOrElse { if (enabled) soft.text else soft.muted }
    val fieldShape = MaterialTheme.shapes.medium
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(
            minWidth = OutlinedTextFieldDefaults.MinWidth,
            minHeight = OutlinedTextFieldDefaults.MinHeight,
        ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(if (isError) soft.expense else soft.accent),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = source,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = source,
                isError = isError,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                prefix = prefix,
                suffix = suffix,
                supportingText = supportingText,
                shape = fieldShape,
                colors = fieldColors,
                container = {
                    val ring = when {
                        isError -> soft.expense
                        focused -> soft.accent
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .softInset(fieldShape, soft)
                            .then(if (ring.isSpecified && ring != Color.Transparent) Modifier.border(1.5.dp, ring, fieldShape) else Modifier)
                    )
                },
            )
        },
    )
}

// --- Containers ------------------------------------------------------------------------------

/** Card. In Soft: a raised panel in its container colour over the page. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.Card(modifier, shape, colors, elevation, border, content)
        return
    }
    Column(
        modifier
            .softRaised(shape, soft, fill = colors.containerColor.compositeOver(soft.background))
            .clip(shape)
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) { content() }
    }
}

/** Tappable card. In Soft: raised, and pressed in while touched. */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val soft = AkSpendTheme.soft
    val source = interactionSource ?: remember { MutableInteractionSource() }
    if (soft == null) {
        androidx.compose.material3.Card(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
            elevation = elevation, border = border, interactionSource = source, content = content,
        )
        return
    }
    val depth = pressDepth(source, enabled)
    Column(
        modifier
            .softSurface(shape, soft, colors.containerColor.compositeOver(soft.background), SoftDepth.panel, depth)
            .clip(shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) { content() }
    }
}

/** Alert dialog. In Soft: the page colour with an embossed rim (dialogs sit in their own window). */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val soft = AkSpendTheme.soft
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = if (soft != null) modifier.softRim(shape, soft) else modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = soft?.background ?: containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = if (soft != null) 0.dp else tonalElevation,
        properties = properties,
    )
}

// --- Indicators ------------------------------------------------------------------------------

/** Determinate progress bar. In Soft: a pressed-in track filled with [color]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    drawStopIndicator: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {
        ProgressIndicatorDefaults.drawStopIndicator(
            drawScope = this, stopSize = ProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
            color = color, strokeCap = strokeCap,
        )
    },
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = progress, modifier = modifier, color = color, trackColor = trackColor,
            strokeCap = strokeCap, gapSize = gapSize, drawStopIndicator = drawStopIndicator,
        )
        return
    }
    Box(
        modifier
            .progressSemantics(progress().coerceIn(0f, 1f))
            .softInset(PillShape, soft, fill = soft.track, distance = 2.dp)
            .drawBehind {
                val f = progress().coerceIn(0f, 1f)
                if (f > 0f) {
                    val h = size.height
                    drawRoundRect(color, Offset.Zero, Size(maxOf(h, size.width * f), h), CornerRadius(h / 2f, h / 2f))
                }
            }
    )
}

/** Divider. In Soft: an engraved groove, a shade line over a highlight line. */
@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    val soft = AkSpendTheme.soft
    if (soft == null) {
        androidx.compose.material3.HorizontalDivider(modifier, thickness, color)
        return
    }
    Spacer(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                val px = 1.dp.toPx()
                drawLine(soft.shade, Offset(0f, px / 2f), Offset(size.width, px / 2f), px)
                drawLine(soft.highlight, Offset(0f, px * 1.5f), Offset(size.width, px * 1.5f), px)
            }
    )
}
