package com.parka.mdive.designsystem

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.parka.mdive.designsystem.generated.DesignTokens
import kotlinx.coroutines.launch

/** Confirmation above the current context. No file or repository behavior lives here. */
@Composable
fun MDiveConfirmationDialog(
    title: String,
    detail: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "취소",
    busy: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    MDiveOverlay(title, onDismissRequest, dismissEnabled = !busy) {
        val colors = MaterialTheme.colorScheme
        val shape = RoundedCornerShape(DesignTokens.Radii.getValue("lg"))
        Column(
            modifier.widthIn(max = DesignTokens.Sizes.getValue("dialogMaxWidth")).fillMaxWidth()
                .heightIn(max = maxHeight * .9f)
                .clip(shape).background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = DesignTokens.Opacities.getValue("divider")), shape)
                .padding(DesignTokens.Spacing.getValue("lg"))
                .pointerInput(Unit) { detectTapGestures {} },
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("md")),
        ) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("control"))) {
                Text(title, style = mdiveTextStyle("section"), color = colors.onSurface, modifier = Modifier.semantics { heading() })
                Text(detail, style = mdiveTextStyle("ui"), color = colors.onSurfaceVariant)
                content?.invoke()
            }
            // Vertical actions retain full labels at large font scales and narrow widths.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("sm"))) {
                MDiveButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), state = if (busy) MDiveButtonState.Loading else MDiveButtonState.Normal)
                MDiveButton(dismissLabel, onDismissRequest, Modifier.fillMaxWidth(), variant = MDiveButtonVariant.Quiet, state = if (busy) MDiveButtonState.Disabled else MDiveButtonState.Normal)
            }
        }
    }
}

/** Material motion/gestures with MDive chrome. Hosts remove the sheet after onDismissRequest. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MDiveBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(requestDismiss: () -> Unit) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val radius = DesignTokens.Radii.getValue("lg")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismissRequest)
    var closing by remember { mutableStateOf(false) }
    var dismissSent by remember { mutableStateOf(false) }
    val notifyDismiss = {
        if (!dismissSent) {
            dismissSent = true
            dismiss()
        }
    }
    val requestClose = {
        if (!closing && !dismissSent) {
            closing = true
            scope.launch {
                try {
                    sheetState.hide()
                    if (!sheetState.isVisible) notifyDismiss()
                } finally {
                    closing = false
                }
            }
        }
        Unit
    }
    ModalBottomSheet(
        onDismissRequest = notifyDismiss,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = DesignTokens.Sizes.getValue("sheetMaxWidth"),
        shape = RoundedCornerShape(topStart = radius, topEnd = radius),
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        scrimColor = colors.scrim.copy(alpha = DesignTokens.Opacities.getValue("scrim")),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth()
                .semantics { contentDescription = "패널 닫기" }
                .clickable(enabled = !closing, role = Role.Button, onClick = requestClose)
                .padding(vertical = DesignTokens.Spacing.getValue("sheetHandleY")), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = DesignTokens.Spacing.getValue("xl"), height = DesignTokens.Spacing.getValue("xs"))
                    .background(colors.outline, RoundedCornerShape(DesignTokens.Radii.getValue("sm"))))
            }
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight * .9f)) {
                Text(title, Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.getValue("page"), vertical = DesignTokens.Spacing.getValue("sm")).semantics { heading() },
                    style = mdiveTextStyle("uiTitle"), color = colors.onSurface, textAlign = TextAlign.Center)
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(DesignTokens.Spacing.getValue("page")),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("md")),
                    content = { content(requestClose) },
                )
            }
        }
    }
}

@Composable
private fun MDiveOverlay(
    title: String,
    onDismissRequest: () -> Unit,
    dismissEnabled: Boolean = true,
    alignment: Alignment = Alignment.Center,
    content: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissEnabled) onDismissRequest() },
        properties = DialogProperties(dismissOnBackPress = dismissEnabled, dismissOnClickOutside = dismissEnabled, usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = DesignTokens.Opacities.getValue("scrim"))
        Box(
            Modifier.fillMaxSize().background(scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = dismissEnabled, onClickLabel = "닫기", onClick = onDismissRequest),
        )
        BoxWithConstraints(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .padding(horizontal = if (alignment == Alignment.Center) DesignTokens.Spacing.getValue("lg") else 0.dp)
                .semantics { paneTitle = title },
            contentAlignment = alignment,
            content = content,
        )
    }
}
