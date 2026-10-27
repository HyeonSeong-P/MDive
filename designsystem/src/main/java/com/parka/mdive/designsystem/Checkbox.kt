package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.DesignTokens

/** A null callback is a read-only status marker; interactive controls reserve the touch target. */
@Composable
fun MDiveCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(DesignTokens.Radii.getValue("checkbox"))
    val interaction = if (onCheckedChange != null) {
        Modifier.size(DesignTokens.Sizes.getValue("touch")).toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
    } else Modifier.semantics {
        role = Role.Checkbox
        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        stateDescription = if (checked) "완료 · 읽기 전용" else "미완료 · 읽기 전용"
        if (!enabled) disabled()
    }
    Box(modifier.then(interaction), contentAlignment = Alignment.Center) {
        Box(Modifier.size(DesignTokens.Sizes.getValue("checkbox")).alpha(if (enabled) 1f else .38f)
            .background(if (checked) colors.primary else colors.surface, shape)
            .border(1.5.dp, if (checked) colors.primary else colors.outline, shape), contentAlignment = Alignment.Center) {
            if (checked) MDiveIcon("check", tint = colors.onPrimary, size = 16.dp)
        }
    }
}
