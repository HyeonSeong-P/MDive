package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.DesignTokens

// Retained for compatibility; whole-screen skeleton composition belongs to the screen host.
enum class MDiveStateKind { Empty, Failure, Loading }
enum class MDiveStateActionWidth { Full, Compact }

/** A page/panel placeholder. Hosts retain navigation and decide which recovery actions to offer. */
@Composable
fun MDiveStatePanel(
    title: String,
    detail: String,
    kind: MDiveStateKind,
    modifier: Modifier = Modifier,
    showSymbol: Boolean = true,
    actionWidth: MDiveStateActionWidth = MDiveStateActionWidth.Full,
    contentPadding: PaddingValues = PaddingValues(horizontal = DesignTokens.Spacing.getValue("page"), vertical = DesignTokens.Spacing.getValue("xl")),
    decorationSpacing: Dp = DesignTokens.Spacing.getValue("md"),
    titleSpacing: Dp = DesignTokens.Spacing.getValue("md"),
    decoration: (@Composable () -> Unit)? = null,
    context: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(contentPadding)
            .semantics {
                if (kind == MDiveStateKind.Loading) stateDescription = "불러오는 중"
                if (kind == MDiveStateKind.Failure) liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showSymbol) Box(
            Modifier.size(DesignTokens.Sizes.getValue("stateSymbol"))
                .background(colors.surfaceVariant, RoundedCornerShape(DesignTokens.Radii.getValue("lg"))),
            contentAlignment = Alignment.Center,
        ) {
            if (kind == MDiveStateKind.Loading) MDiveLoadingMark(tint = colors.primary)
            else {
                MDiveIcon("file", tint = colors.primary, size = 32.dp)
                if (kind == MDiveStateKind.Failure) MDiveIcon("close", Modifier.align(Alignment.BottomEnd).background(colors.surface, RoundedCornerShape(DesignTokens.Radii.getValue("sm"))).padding(4.dp), tint = colors.error, size = 16.dp)
            }
        }
        if (showSymbol && decoration != null) Spacer(Modifier.height(DesignTokens.Spacing.getValue("md")))
        decoration?.invoke()
        if (showSymbol || decoration != null) Spacer(Modifier.height(decorationSpacing))
        Text(title, style = mdiveTextStyle("section"), color = colors.onSurface, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(titleSpacing))
        Text(detail, style = mdiveTextStyle("ui"), color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        if (context != null) {
            Spacer(Modifier.height(DesignTokens.Spacing.getValue("md")))
            context()
        }
        if (actions != null) Spacer(Modifier.height(DesignTokens.Spacing.getValue("md")))
        if (actions != null) Column(
            (if (actionWidth == MDiveStateActionWidth.Full) Modifier.fillMaxWidth() else Modifier)
                .padding(top = DesignTokens.Spacing.getValue("sm")),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("sm")),
        ) { actions() }
    }
}

/** Primary/secondary recovery actions wrap naturally; tertiary escape stays on its own row. */
@Composable
fun MDiveRecoveryActions(
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tertiary: (@Composable () -> Unit)? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            primary()
            secondary()
        }
        tertiary?.invoke()
    }
}

/** A local resource failure. The surrounding readable content remains visible and usable. */
@Composable
fun MDiveInlineNotice(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    loading: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(DesignTokens.Radii.getValue("panel"))
    Row(
        modifier.fillMaxWidth().background(colors.surface, shape)
            .border(1.dp, colors.outline.copy(alpha = DesignTokens.Opacities.getValue("divider")), shape)
            .padding(DesignTokens.Spacing.getValue("md")),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("control")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) MDiveLoadingMark()
        else MDiveIcon(if (error) "close" else "file", tint = if (error) colors.error else colors.onSurfaceVariant, size = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("sm"))) {
            Text(title, style = mdiveTextStyle("uiTitle"), color = colors.onSurface)
            Text(detail, style = mdiveTextStyle("ui"), color = colors.onSurfaceVariant)
            action?.invoke()
        }
    }
}

/** Persistent recovery information. A trailing action wraps below the message when space is tight. */
@Composable
fun MDiveRecoveryBanner(
    title: String,
    detail: String? = null,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    icon: String = "warning",
    action: (@Composable () -> Unit)? = null,
    loading: Boolean = false,
    error: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .background(colors.surfaceVariant, RoundedCornerShape(DesignTokens.Radii.getValue("control")))
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        val effectiveWidth = maxWidth / fontScale
        val actionBreakpoint = DesignTokens.Sizes.getValue(if (onDismiss == null) "bannerActionBreakpoint" else "bannerDismissActionBreakpoint")
        val wrapAction = action != null && effectiveWidth < actionBreakpoint
        Column(
            Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.getValue("md"), vertical = DesignTokens.Spacing.getValue("control")),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("sm")),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("control")), verticalAlignment = Alignment.CenterVertically) {
                if (loading) MDiveLoadingMark()
                else MDiveIcon(icon, tint = if (error) colors.error else colors.primary, size = 20.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("xs"))) {
                    Text(title, style = mdiveTextStyle("label"), color = colors.onSurface)
                    if (!detail.isNullOrBlank()) Text(detail, style = mdiveTextStyle("caption"), color = colors.onSurfaceVariant)
                }
                if (!wrapAction && action != null) Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("xs")), horizontalAlignment = Alignment.End) { action() }
                if (onDismiss != null) MDiveIconButton("close", "안내 닫기", onDismiss)
            }
            if (wrapAction) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("xs")), horizontalAlignment = Alignment.End) { action?.invoke() }
            }
        }
    }
}

/** Informational completion only. Timing, queueing and visibility belong to the host. */
@Composable
fun MDiveToast(message: String, modifier: Modifier = Modifier, icon: String = "check") {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.background(colors.surfaceVariant, RoundedCornerShape(DesignTokens.Radii.getValue("control")))
            .padding(horizontal = DesignTokens.Spacing.getValue("md"), vertical = DesignTokens.Spacing.getValue("control"))
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MDiveIcon(icon, tint = colors.primary, size = 20.dp)
        Spacer(Modifier.width(DesignTokens.Spacing.getValue("sm")))
        Text(message, color = colors.onSurface, style = mdiveTextStyle("ui"))
    }
}
