package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.parka.mdive.designsystem.generated.DesignTokens

/** Resolved presentation runs, never parser nodes. Styles can be combined on one run. */
data class MDiveTextRun(
    val text: String,
    val emphasis: Boolean = false,
    val strong: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
    val link: String? = null,
    val highlighted: Boolean = false,
    val mark: Boolean = false,
    val baseline: MDiveTextBaseline = MDiveTextBaseline.Normal,
    val inserted: Boolean = false,
    val underline: Boolean = false,
)

enum class MDiveTextBaseline { Normal, Subscript, Superscript }

@Composable
private fun annotatedRuns(runs: List<MDiveTextRun>, onLink: ((String) -> Unit)?): AnnotatedString = buildAnnotatedString {
    runs.forEach { run ->
        val decoration = buildList {
            if (run.strike) add(TextDecoration.LineThrough)
            if (run.link != null || run.underline || run.inserted) add(TextDecoration.Underline)
        }
        val style = SpanStyle(
            fontWeight = if (run.strong) FontWeight.SemiBold else null,
            fontStyle = if (run.emphasis) FontStyle.Italic else null,
            fontFamily = if (run.code) mdiveTextStyle("code").fontFamily else null,
            background = when { run.mark -> LocalOceanColors.current.getValue("documentMarkBackground"); run.code || run.highlighted -> MaterialTheme.colorScheme.surfaceVariant; else -> androidx.compose.ui.graphics.Color.Unspecified },
            baselineShift = when (run.baseline) { MDiveTextBaseline.Normal -> null; MDiveTextBaseline.Subscript -> androidx.compose.ui.text.style.BaselineShift.Subscript; MDiveTextBaseline.Superscript -> androidx.compose.ui.text.style.BaselineShift.Superscript },
            fontSize = if (run.baseline != MDiveTextBaseline.Normal) .75f.em else androidx.compose.ui.unit.TextUnit.Unspecified,
            color = when { run.link != null -> MaterialTheme.colorScheme.primary; run.mark -> LocalOceanColors.current.getValue("documentMarkText"); else -> androidx.compose.ui.graphics.Color.Unspecified },
            textDecoration = if (decoration.isEmpty()) null else TextDecoration.combine(decoration),
        )
        withStyle(style) {
            if (run.link != null && onLink != null) {
                withLink(LinkAnnotation.Clickable(run.link, linkInteractionListener = { onLink(run.link) })) { append(run.text) }
            } else append(run.text)
        }
    }
}

@Composable
fun MDiveMarkdownText(
    runs: List<MDiveTextRun>, modifier: Modifier = Modifier,
    onLink: ((String) -> Unit)? = null,
    style: TextStyle = mdiveTextStyle("body"), textAlign: TextAlign = TextAlign.Start,
) { Text(annotatedRuns(runs, onLink), modifier, style = style, textAlign = textAlign) }

@Composable
fun MDiveMarkdownHeading(level: Int, runs: List<MDiveTextRun>, modifier: Modifier = Modifier, onLink: ((String) -> Unit)? = null) {
    require(level in 1..6) { "Heading level must be 1..6" }
    MDiveMarkdownText(runs, modifier.semantics { heading() }, onLink, mdiveTextStyle("documentH$level"))
}

/** The host owns spacing between sibling blocks; children have no exterior margins. */
@Composable
fun MDiveMarkdownFlow(modifier: Modifier = Modifier, spacing: Dp = DesignTokens.Spacing.getValue("documentBlock"), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}

@Composable
fun MDiveMarkdownDivider(modifier: Modifier = Modifier) { MDiveDivider(modifier) }

@Composable
fun MDiveMarkdownQuote(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Row(modifier.fillMaxWidth().semantics { contentDescription = "인용" }.height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
        Column(Modifier.weight(1f).padding(start = DesignTokens.Spacing.getValue("documentInset")), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("documentNested")), content = content)
    }
}

sealed interface MDiveListMarker {
    data object Bullet : MDiveListMarker
    data class Ordered(val number: Int) : MDiveListMarker
    data class Task(val checked: Boolean) : MDiveListMarker
}

/** Nest items through the content slot, so indentation follows the actual composition. */
@Composable
fun MDiveMarkdownListItem(marker: MDiveListMarker, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = if (marker is MDiveListMarker.Task) androidx.compose.ui.Alignment.CenterVertically else androidx.compose.ui.Alignment.Top) {
        val label = when (marker) {
            MDiveListMarker.Bullet -> "•"
            is MDiveListMarker.Ordered -> "${marker.number}."
            is MDiveListMarker.Task -> ""
        }
        if (marker is MDiveListMarker.Task) {
            Box(Modifier.width(DesignTokens.Spacing.getValue("documentMarker")).padding(end = 8.dp)) {
                MDiveCheckbox(marker.checked, onCheckedChange = null)
            }
        } else Text(label, Modifier.widthIn(min = DesignTokens.Spacing.getValue("documentMarker")).padding(end = 8.dp), style = mdiveTextStyle("body"), textAlign = TextAlign.End)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("documentNested")), content = content)
    }
}

data class MDiveCodeHighlight(val start: Int, val end: Int, val kind: MDiveCodeTone)
enum class MDiveCodeTone { Keyword, String, Plain }

/** Invalid highlight ranges are ignored; source and whitespace always survive. */
@Composable
fun MDiveCodeBlock(source: String, modifier: Modifier = Modifier, language: String? = null, highlights: List<MDiveCodeHighlight> = emptyList(), onCopy: ((String) -> Unit)? = null) {
    val colors = LocalOceanColors.current
    Column(modifier.fillMaxWidth().background(colors.getValue("codeBackground"), RoundedCornerShape(DesignTokens.Radii.getValue("code")))) {
        if (language != null || onCopy != null) Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(language.orEmpty(), Modifier.weight(1f).padding(vertical = 14.dp), color = colors.getValue("codeText"), style = mdiveTextStyle("caption"))
            if (onCopy != null) MDiveIconButton("copy", "코드 복사", { onCopy(source) }, tint = colors.getValue("codeText"), pressedBackground = colors.getValue("buttonPressedCode"), pressedTint = colors.getValue("codeText"))
        }
        val annotated = buildAnnotatedString {
            append(source)
            highlights.filter { it.start >= 0 && it.end <= source.length && it.start < it.end }.forEach {
                addStyle(SpanStyle(color = colors.getValue(when (it.kind) { MDiveCodeTone.Keyword -> "codeKeyword"; MDiveCodeTone.String -> "codeString"; MDiveCodeTone.Plain -> "codeText" })), it.start, it.end)
            }
        }
        Text(annotated, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp), color = colors.getValue("codeText"), style = mdiveTextStyle("code"), softWrap = false)
    }
}

data class MDiveTableColumn(val header: List<MDiveTextRun>, val alignment: TextAlign = TextAlign.Start, val width: Dp = 160.dp)

/** Column width is an explicit visual input, not a Markdown wrapping policy. */
@Composable
fun MDiveMarkdownTable(columns: List<MDiveTableColumn>, rows: List<List<List<MDiveTextRun>>>, modifier: Modifier = Modifier, onLink: ((String) -> Unit)? = null) {
    require(columns.isNotEmpty()) { "A table needs columns" }
    require(columns.all { it.width > 0.dp }) { "Column widths must be positive" }
    require(rows.all { it.size == columns.size }) { "Every row must match the columns" }
    Column(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).semantics { collectionInfo = CollectionInfo(rows.size + 1, columns.size) }) {
        (listOf(columns.map { it.header }) + rows).forEachIndexed { rowIndex, cells ->
            Row(Modifier.background(if (rowIndex == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)) {
                cells.forEachIndexed { columnIndex, cell ->
                    val column = columns[columnIndex]
                    MDiveMarkdownText(cell, Modifier.width(column.width).padding(DesignTokens.Spacing.getValue("tableCell")).semantics {
                        collectionItemInfo = CollectionItemInfo(rowIndex, 1, columnIndex, 1)
                        if (rowIndex == 0) heading() else contentDescription = column.header.joinToString("") { it.text } + ": " + cell.joinToString("") { it.text }
                    }, onLink, mdiveTextStyle("documentTable"), column.alignment)
                }
            }
            MDiveDivider()
        }
    }
}

enum class MDiveResourceState { Loading, Ready, AccessRequired, Failure }

/** Image content and recovery are supplied by the host; this component never loads resources. */
@Composable
fun MDiveMarkdownImage(
    state: MDiveResourceState, alt: String, modifier: Modifier = Modifier, title: String? = null,
    onExpand: (() -> Unit)? = null, onConnect: (() -> Unit)? = null, onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            MDiveResourceState.Ready -> {
                Box(Modifier.fillMaxWidth().clearAndSetSemantics { if (alt.isNotEmpty()) { contentDescription = alt; role = Role.Image } }) { content() }
                if (onExpand != null) MDiveButton("이미지 확대", onExpand, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S)
            }
            MDiveResourceState.Loading -> MDiveRecoveryBanner("이미지 준비 중", alt, icon = "file", loading = true)
            MDiveResourceState.AccessRequired -> MDiveRecoveryBanner("이미지 접근 연결 필요", alt, icon = "file", action = { if (onConnect != null) MDiveButton("폴더 연결", onConnect, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S) })
            MDiveResourceState.Failure -> MDiveRecoveryBanner("이미지를 불러오지 못했어요", alt, action = { if (onRetry != null) MDiveButton("다시 시도", onRetry, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S) })
        }
        if (title != null) Text(title, style = mdiveTextStyle("caption"))
    }
}

/** Engine-neutral rendered math/diagram shell. Long prepared content scrolls locally. */
@Composable
fun MDiveRenderedBlock(
    state: MDiveResourceState, description: String, modifier: Modifier = Modifier,
    onViewSource: (() -> Unit)? = null, onExpand: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    require(state != MDiveResourceState.AccessRequired) { "Rendered blocks do not request file access" }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state == MDiveResourceState.Ready) Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).semantics { contentDescription = description }) { content() }
        MDiveRecoveryBanner(
            title = when (state) { MDiveResourceState.Ready -> "렌더링된 콘텐츠"; MDiveResourceState.Loading -> "콘텐츠 렌더링 중"; else -> "콘텐츠를 표시하지 못했어요" },
            detail = description,
            icon = if (state == MDiveResourceState.Failure) "warning" else "file",
            loading = state == MDiveResourceState.Loading,
            action = if (onViewSource == null && (onExpand == null || state != MDiveResourceState.Ready)) null else {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onViewSource != null) MDiveButton("원문 보기", onViewSource, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S)
                        if (state == MDiveResourceState.Ready && onExpand != null) MDiveButton("콘텐츠 확대", onExpand, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S)
                    }
                }
            },
        )
    }
}

@Composable
fun MDiveContentFallback(kind: String, reason: String, modifier: Modifier = Modifier, onViewSource: (() -> Unit)? = null) {
    MDiveRecoveryBanner(kind, reason, modifier, action = { if (onViewSource != null) MDiveButton("원문 보기", onViewSource, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S) })
}

/** Prepared inline rendering placed in the same text flow; no math syntax is interpreted. */
@Composable
fun MDiveMathInline(text: AnnotatedString, inlineContent: Map<String, androidx.compose.foundation.text.InlineTextContent>, modifier: Modifier = Modifier) {
    Text(text, modifier, inlineContent = inlineContent, style = mdiveTextStyle("body"))
}
