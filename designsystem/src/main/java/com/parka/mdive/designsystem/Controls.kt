package com.parka.mdive.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.CatalogIcons
import com.parka.mdive.designsystem.generated.DesignTokens

enum class MDiveButtonVariant { Primary, Secondary, Outlined, Quiet }
enum class MDiveButtonSize { S, M, L }
enum class MDiveButtonState { Normal, Pressed, Disabled, Loading }

@Composable
fun MDiveIcon(
    name: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = DesignTokens.Sizes.getValue("icon"),
) {
    val data = CatalogIcons.paths.getValue(name)
    val path = remember(data) { PathParser().parsePathString(data).toPath() }
    Canvas(modifier.size(size).then(if (description == null) Modifier else Modifier.semantics { contentDescription = description })) {
        withTransform({ scale(this@Canvas.size.width / CatalogIcons.viewport, this@Canvas.size.height / CatalogIcons.viewport, pivot = Offset.Zero) }) {
            if (name in CatalogIcons.filled) drawPath(path, tint)
            else drawPath(path, tint, style = Stroke(width = CatalogIcons.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** MDive controls draw their own pressed/focus treatment rather than inheriting Material shapes. */
@Composable
fun MDiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MDiveButtonVariant = MDiveButtonVariant.Primary,
    state: MDiveButtonState = MDiveButtonState.Normal,
    icon: String? = null,
    size: MDiveButtonSize = MDiveButtonSize.M,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val held by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val enabled = state != MDiveButtonState.Disabled && state != MDiveButtonState.Loading
    val pressed = state == MDiveButtonState.Pressed || held
    val primary = variant == MDiveButtonVariant.Primary
    val showPrimary = primary && state != MDiveButtonState.Disabled
    val shape = RoundedCornerShape(DesignTokens.Radii.getValue(if (primary) "primaryButton" else "control"))
    val base = when (variant) {
        MDiveButtonVariant.Primary -> if (showPrimary) colors.primary else colors.surfaceVariant
        MDiveButtonVariant.Secondary -> colors.surfaceVariant
        MDiveButtonVariant.Outlined, MDiveButtonVariant.Quiet -> Color.Transparent
    }
    val stateColors = LocalOceanColors.current
    val background = if (pressed && enabled) stateColors.getValue(if (primary) "buttonPressedPrimary" else "buttonPressedSurface") else base
    val foreground = when {
        pressed && enabled && !primary -> stateColors.getValue("buttonPressedText")
        showPrimary -> colors.onPrimary
        !enabled -> colors.onSurfaceVariant
        variant == MDiveButtonVariant.Primary -> colors.onPrimary
        else -> colors.primary
    }
    val top = LocalOceanColors.current.getValue("buttonTop")
    val topColor = if (pressed && enabled) background else top
    val fill = if (showPrimary) Modifier.background(Brush.verticalGradient(listOf(topColor, background)))
        .border(1.dp, Color.White.copy(alpha = DesignTokens.Opacities.getValue("buttonEdge")), shape) else Modifier.background(background)
        .then(if (variant == MDiveButtonVariant.Outlined) Modifier.border(1.dp, if (enabled) colors.outline else colors.outline.copy(alpha = .38f), shape) else Modifier)
    // The outer host owns input and semantics; the inner surface owns visual size.
    BoxWithConstraints(
        modifier.defaultMinSize(minWidth = DesignTokens.Sizes.getValue("touch"), minHeight = DesignTokens.Sizes.getValue("touch"))
            .clip(shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { if (state == MDiveButtonState.Loading) stateDescription = "처리 중" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(min = minWidth)
                .defaultMinSize(minHeight = DesignTokens.Sizes.getValue("button${size.name}"))
                .clip(shape).then(fill)
                .then(if (focused) Modifier.border(2.dp, if (showPrimary) colors.onPrimary else colors.primary, shape) else Modifier)
                .padding(horizontal = DesignTokens.Spacing.getValue(if (text.isEmpty()) "control" else "button${size.name}X"), vertical = DesignTokens.Spacing.getValue("button${size.name}Y")),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == MDiveButtonState.Loading) MDiveLoadingMark(tint = foreground)
            else if (icon != null) MDiveIcon(icon, tint = foreground, size = if (text.isEmpty()) 24.dp else 20.dp)
            if (text.isNotEmpty()) Text(text, color = foreground, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Floating placement and the content clearance belong to the screen, not this control. */
@Composable
fun MDiveFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String = "파일 열기",
    state: MDiveButtonState = MDiveButtonState.Normal,
) {
    require(label == null || label.isNotBlank()) { "A visible floating action label must not be blank." }
    require(description.isNotBlank()) { "A floating action requires an accessible name." }
    val shape = if (label == null) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(DesignTokens.Radii.getValue("primaryButton"))
    MDiveButton(
        text = label.orEmpty(),
        onClick = onClick,
        modifier = modifier
            .then(if (label == null) Modifier.size(DesignTokens.Sizes.getValue("touch")).semantics { contentDescription = description } else Modifier)
            .shadow(6.dp, shape).clip(shape),
        icon = "file-add",
        size = MDiveButtonSize.L,
        state = state,
    )
}

@Composable
fun MDiveIconButton(
    icon: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    pressedBackground: Color = LocalOceanColors.current.getValue("buttonPressedSurface"),
    pressedTint: Color = LocalOceanColors.current.getValue("buttonPressedText"),
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(DesignTokens.Radii.getValue("control"))
    Box(
        modifier.size(DesignTokens.Sizes.getValue("touch")).clip(shape)
            .background(if (pressed && enabled) pressedBackground else Color.Transparent)
            .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        MDiveIcon(icon, tint = (if (pressed && enabled) pressedTint else tint).copy(alpha = if (enabled) 1f else DesignTokens.Opacities.getValue("disabledIcon")))
    }
}

@Composable
fun MDiveTextTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    Box(
        modifier.height(DesignTokens.Sizes.getValue("touch"))
            .clip(RoundedCornerShape(DesignTokens.Radii.getValue("control")))
            .background(if (pressed) colors.surfaceVariant else Color.Transparent)
            .drawBehind { if (selected || focused) drawLine(colors.primary, Offset(12.dp.toPx(), size.height - 1.dp.toPx()), Offset(size.width - 12.dp.toPx(), size.height - 1.dp.toPx()), 2.dp.toPx()) }
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) colors.primary else colors.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun MDiveLoadingMark(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.size(18.dp)) { drawArc(tint, startAngle = -90f, sweepAngle = 270f, useCenter = false, style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round)) }
}

@Composable
fun MDiveDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = DesignTokens.Opacities.getValue("divider"))))
}
