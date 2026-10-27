package com.parka.mdive.designsystem

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.DesignTokens

/** Put beside the trigger in a Box; caller owns expansion, actions and dismiss requests. */
@Composable
fun MDiveDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 180.dp, max = 320.dp),
        shape = RoundedCornerShape(DesignTokens.Radii.getValue("control")),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        content = content,
    )
}

@Composable
fun MDiveDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = { Text(text, style = mdiveTextStyle("ui")) },
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = DesignTokens.Sizes.getValue("touch")),
        enabled = enabled,
        leadingIcon = icon?.let { { MDiveIcon(it, tint = if (enabled) colors.primary else colors.onSurfaceVariant, size = 20.dp) } },
        contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.getValue("md"), vertical = DesignTokens.Spacing.getValue("sm")),
        colors = MenuDefaults.itemColors(textColor = colors.onSurface, disabledTextColor = colors.onSurfaceVariant),
    )
}
