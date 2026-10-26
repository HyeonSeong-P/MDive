package com.parka.mdive.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.DesignTokens
import kotlin.math.roundToInt

/** Labeled text input; callers own validation and persistence. */
@Composable
fun MDiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(DesignTokens.Radii.getValue("input"))
    val foreground = if (enabled) colors.onSurface else colors.onSurfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("sm"))) {
        Text(label, color = colors.onSurface, style = mdiveTextStyle("uiTitle"))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            interactionSource = source,
            textStyle = mdiveTextStyle("ui").copy(color = foreground),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = DesignTokens.Sizes.getValue("touch"))
                .background(colors.surfaceVariant, shape)
                .border(if (focused) 2.dp else 1.dp, if (errorMessage != null) colors.error else if (focused) colors.primary else colors.outline, shape)
                .semantics { contentDescription = label; if (errorMessage != null) error(errorMessage) },
            decorationBox = { field -> Box(Modifier.padding(DesignTokens.Spacing.getValue("control")), contentAlignment = Alignment.CenterStart) { field() } },
        )
        val description = errorMessage ?: supportingText
        if (description != null) Text(description, style = mdiveTextStyle("caption"), color = if (errorMessage != null) colors.error else colors.onSurfaceVariant)
    }
}

/** A Foundation slider. No domain range, unit or saved preference is implied by this control. */
@Composable
fun MDiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    valueLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
) {
    require(value.isFinite() && valueRange.start.isFinite() && valueRange.endInclusive.isFinite())
    require(valueRange.endInclusive > valueRange.start && steps >= 0)
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val insetPx = with(LocalDensity.current) { DesignTokens.Sizes.getValue("sliderInset").toPx() }
    val currentValue by rememberUpdatedState(value.coerceIn(valueRange))
    val change by rememberUpdatedState(onValueChange)
    var width by remember { mutableIntStateOf(1) }
    var dragValue by remember { mutableFloatStateOf(value) }
    val extent = valueRange.endInclusive - valueRange.start
    val increment = if (steps > 0) extent / (steps + 1) else extent / 20f
    fun normalized(candidate: Float): Float {
        val bounded = candidate.coerceIn(valueRange)
        if (steps == 0) return bounded
        val tick = ((bounded - valueRange.start) / extent * (steps + 1)).roundToInt()
        return valueRange.start + extent * tick / (steps + 1)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("xs"))) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = mdiveTextStyle("uiTitle"), color = colors.onSurface)
            Text(valueLabel, style = mdiveTextStyle("ui"), color = colors.onSurfaceVariant)
        }
        val shape = RoundedCornerShape(DesignTokens.Radii.getValue("control"))
        Canvas(
            Modifier.fillMaxWidth().height(DesignTokens.Sizes.getValue("touch"))
                .onSizeChanged { width = it.width.coerceAtLeast(1) }
                .then(if (focused) Modifier.border(2.dp, colors.primary, shape) else Modifier)
                .semantics {
                    contentDescription = label
                    stateDescription = valueLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(currentValue, valueRange, steps)
                    if (!enabled) disabled()
                    else setProgress { requested ->
                        if (!requested.isFinite()) false
                        else { change(normalized(requested)); true }
                    }
                }
                .pointerInput(enabled, valueRange, steps, rtl) {
                    if (enabled) detectTapGestures { position ->
                        val inset = insetPx
                        val fraction = ((position.x - inset) / (width - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        change(normalized(valueRange.start + extent * if (rtl) 1f - fraction else fraction))
                    }
                }
                .draggable(
                    state = rememberDraggableState { delta ->
                        dragValue = (dragValue + delta / (width - 2 * insetPx).coerceAtLeast(1f) * extent * if (rtl) -1f else 1f).coerceIn(valueRange)
                        change(normalized(dragValue))
                    },
                    onDragStarted = { dragValue = currentValue },
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    interactionSource = source,
                )
                .onKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown) false
                    else {
                        val direction = when (event.key) {
                            Key.DirectionRight -> if (rtl) -1 else 1
                            Key.DirectionLeft -> if (rtl) 1 else -1
                            Key.DirectionUp -> 1
                            Key.DirectionDown -> -1
                            else -> 0
                        }
                        if (direction == 0) false else { change(normalized(currentValue + direction * increment)); true }
                    }
                }
                .focusable(enabled, source),
        ) {
            val inset = insetPx
            val fraction = (currentValue - valueRange.start) / extent
            val position = inset + (size.width - inset * 2) * if (rtl) 1f - fraction else fraction
            val center = size.height / 2
            val tint = if (enabled) colors.primary else colors.onSurfaceVariant
            drawLine(colors.outline.copy(alpha = DesignTokens.Opacities.getValue("divider")), Offset(inset, center), Offset(size.width - inset, center), DesignTokens.Sizes.getValue("sliderTrack").toPx(), StrokeCap.Round)
            drawLine(tint, Offset(if (rtl) size.width - inset else inset, center), Offset(position, center), DesignTokens.Sizes.getValue("sliderTrack").toPx(), StrokeCap.Round)
            drawCircle(colors.surface, radius = DesignTokens.Sizes.getValue("sliderThumb").toPx() / 2, center = Offset(position, center))
            drawCircle(tint, radius = DesignTokens.Sizes.getValue("sliderThumbCore").toPx() / 2, center = Offset(position, center))
        }
    }
}
