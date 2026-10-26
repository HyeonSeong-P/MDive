package com.parka.mdive.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.DesignTokens

/** Shared chrome only: callers own navigation, actions, and system insets. */
@Composable
fun MDiveTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: String? = null,
    navigationDescription: String? = null,
    onNavigationClick: () -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val navigationLabel = if (navigationIcon == null) null else requireNotNull(navigationDescription?.takeIf { it.isNotBlank() }) {
        "A navigation icon requires an accessible description."
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = DesignTokens.Sizes.getValue("topBar"))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null) MDiveIconButton(navigationIcon, requireNotNull(navigationLabel), onNavigationClick)
        else if (leading != null) {
            Box(Modifier.size(DesignTokens.Sizes.getValue("touch")), contentAlignment = Alignment.Center) {
                leading()
            }
        }
        Column(
            Modifier.weight(1f).widthIn(min = 0.dp).padding(start = if (navigationIcon == null && leading == null) 12.dp else 4.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Ellipsis affects drawing only: Text semantics retain the complete original title.
            Text(title, style = mdiveTextStyle("uiTitle"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = mdiveTextStyle("caption"), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}
