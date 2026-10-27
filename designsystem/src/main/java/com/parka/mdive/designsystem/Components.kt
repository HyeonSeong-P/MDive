package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.DesignTokens
import com.parka.mdive.designsystem.generated.CatalogSamples

@Composable
fun MDiveDocumentRow(title: String, detail: String, onClick: () -> Unit, modifier: Modifier = Modifier, favorite: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(DesignTokens.Radii.getValue("panel")))
                .background(if (pressed || focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
                .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MDiveIcon("file", tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            MDiveIcon(if (favorite) "star" else "chevron", tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
        }
        MDiveDivider()
    }
}

@Composable
fun MDiveSearchBar(
    query: String,
    result: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    navigationEnabled: Boolean = true,
) {
    val inputInteraction = remember { MutableInteractionSource() }
    val inputFocused by inputInteraction.collectIsFocusedAsState()
    val inputShape = RoundedCornerShape(DesignTokens.Radii.getValue("input"))
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = DesignTokens.Sizes.getValue("topBar")), verticalAlignment = Alignment.CenterVertically) {
            MDiveIconButton("back", "검색 닫기", onClose)
            BasicTextField(
                value = query, onValueChange = onQueryChange, singleLine = true,
                modifier = Modifier.weight(1f).heightIn(min = DesignTokens.Sizes.getValue("touch"))
                    .semantics { contentDescription = "문서 내 검색" },
                interactionSource = inputInteraction,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { field ->
                    val inset = DesignTokens.Spacing.getValue("searchInset")
                    Box(Modifier.padding(vertical = inset)) {
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = DesignTokens.Sizes.getValue("touch") - inset * 2)
                                .clip(inputShape).background(colors.surfaceVariant)
                                .border(if (inputFocused) 1.5.dp else 1.dp, if (inputFocused) colors.primary else colors.outline, inputShape)
                                .padding(horizontal = DesignTokens.Spacing.getValue("sm"), vertical = DesignTokens.Spacing.getValue("control") - inset),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (query.isEmpty()) Text("문서 검색", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            field()
                        }
                    }
                },
            )
            Text(result, modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MDiveIconButton("up", "이전 검색 결과", onPrevious, enabled = navigationEnabled)
            MDiveIconButton("down", "다음 검색 결과", onNext, enabled = navigationEnabled)
        }
        MDiveDivider()
    }
}

@Composable
fun MDiveNotice(title: String, detail: String, kind: String, modifier: Modifier = Modifier) {
    MDiveInlineNotice(title, detail, modifier, error = kind == "error", loading = kind == "loading")
}

/** Catalog examples of the data-driven APIs. */
@Composable
fun MDiveCodeSample(modifier: Modifier = Modifier, onCopy: ((String) -> Unit)? = null) {
    MDiveCodeBlock(CatalogSamples.code, modifier, language = "KOTLIN", onCopy = onCopy)
}

@Composable
fun MDiveTableSample(modifier: Modifier = Modifier) {
    MDiveMarkdownTable(
        CatalogSamples.table.first().map { MDiveTableColumn(listOf(MDiveTextRun(it))) },
        CatalogSamples.table.drop(1).map { row -> row.map { listOf(MDiveTextRun(it)) } },
        modifier,
    )
}
