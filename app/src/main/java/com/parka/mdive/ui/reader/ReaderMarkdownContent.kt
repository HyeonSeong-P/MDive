package com.parka.mdive.ui.reader

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.parka.mdive.domain.document.MarkdownAlignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.*
import com.parka.mdive.domain.document.MarkdownBlock
import com.parka.mdive.domain.document.MarkdownDocument
import com.parka.mdive.domain.document.MarkdownRun
import com.parka.mdive.domain.document.MarkdownMermaid

@Composable
internal fun ReaderMarkdownContent(document: MarkdownDocument, modifier: Modifier = Modifier, onCopyCode: (String) -> Unit = {}, onViewMathSource: (String) -> Unit = {}, onViewMermaidSource: (String) -> Unit = {}, onExpandMermaid: (MarkdownMermaid) -> Unit = {}, mermaidCache: MermaidSessionCache = remember(document) { MermaidSessionCache() }, images: ReaderImages = ReaderImages(), onLink: (String) -> Unit = {}) {
    // Measure once outside quote's intrinsic layout; nested blocks receive the remaining width.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val contentWidth = (maxWidth - 32.dp).coerceAtLeast(0.dp)
        LazyColumn(Modifier.fillMaxWidth().testTag("markdown-body"), contentPadding = PaddingValues(16.dp)) {
            items(document.blocks.size) { index ->
                val block = document.blocks[index]
                Column(Modifier.fillMaxWidth().padding(top = spaceBefore(document.blocks, index))) {
                    Block(block, contentWidth, onCopyCode, onViewMathSource, onViewMermaidSource, onExpandMermaid, mermaidCache, images, onLink)
                }
            }
        }
    }
}

private fun spaceBefore(blocks: List<MarkdownBlock>, index: Int): Dp =
    if (index == 0) 0.dp else if (blocks[index] is MarkdownBlock.Heading) 32.dp
    else if (blocks[index - 1] is MarkdownBlock.Heading) 8.dp else 16.dp

@Composable
private fun Blocks(blocks: List<MarkdownBlock>, availableWidth: Dp, onCopyCode: (String) -> Unit, onViewMathSource: (String) -> Unit, onViewMermaidSource: (String) -> Unit, onExpandMermaid: (MarkdownMermaid) -> Unit, mermaidCache: MermaidSessionCache, images: ReaderImages, onLink: (String) -> Unit) {
    blocks.forEachIndexed { index, block ->
        Column(Modifier.fillMaxWidth().padding(top = spaceBefore(blocks, index))) { Block(block, availableWidth, onCopyCode, onViewMathSource, onViewMermaidSource, onExpandMermaid, mermaidCache, images, onLink) }
    }
}

@Composable
private fun Block(block: MarkdownBlock, availableWidth: Dp, onCopyCode: (String) -> Unit, onViewMathSource: (String) -> Unit, onViewMermaidSource: (String) -> Unit, onExpandMermaid: (MarkdownMermaid) -> Unit, mermaidCache: MermaidSessionCache, images: ReaderImages, onLink: (String) -> Unit) {
    val minimumTextWidth = 128.dp * LocalDensity.current.fontScale
    when (block) {
        is MarkdownBlock.Mermaid -> ReaderMermaidBlock(block.diagram, availableWidth, onViewMermaidSource, onExpandMermaid, mermaidCache)
        is MarkdownBlock.Math -> ReaderMathBlock(block.math, onViewMathSource)
        is MarkdownBlock.Heading -> if (block.runs.any { it.image != null }) ReaderResourceText(block.runs, availableWidth, onViewMathSource, images, Modifier.semantics { heading() }, mdiveTextStyle("documentH${block.level}"), onLink = onLink) else if (block.runs.any { it.math != null }) ReaderMathText(block.runs, availableWidth, onViewMathSource, Modifier.semantics { heading() }, mdiveTextStyle("documentH${block.level}"), onLink = onLink) else MDiveMarkdownHeading(block.level, block.runs.presentation(), onLink = onLink)
        is MarkdownBlock.Paragraph -> if (block.runs.any { it.image != null }) ReaderResourceText(block.runs, availableWidth, onViewMathSource, images, onLink = onLink) else if (block.runs.any { it.math != null }) ReaderMathText(block.runs, availableWidth, onViewMathSource, onLink = onLink) else MDiveMarkdownText(block.runs.presentation(), onLink = onLink)
        is MarkdownBlock.Source -> MDiveMarkdownText(listOf(MDiveTextRun(block.source)))
        is MarkdownBlock.Code -> MDiveCodeBlock(block.source, Modifier.testTag("markdown-code"),
            language = block.language,
            highlights = block.highlights.map { MDiveCodeHighlight(it.start, it.end, when (it.tone) {
                com.parka.mdive.domain.document.CodeTone.Keyword -> MDiveCodeTone.Keyword
                com.parka.mdive.domain.document.CodeTone.String -> MDiveCodeTone.String
                com.parka.mdive.domain.document.CodeTone.Plain -> MDiveCodeTone.Plain
            }) }, onCopy = onCopyCode)
        MarkdownBlock.Divider -> MDiveMarkdownDivider()
        is MarkdownBlock.Table -> MDiveMarkdownTable(
            block.columns.map { column -> MDiveTableColumn(column.header.presentation(), when (column.alignment) {
                MarkdownAlignment.Left -> TextAlign.Left
                MarkdownAlignment.Center -> TextAlign.Center
                MarkdownAlignment.Right -> TextAlign.Right
            }) },
            block.rows.map { row -> row.map { it.presentation() } },
            Modifier.testTag("markdown-table"), onLink = onLink,
            cellContent = if (block.columns.any { c -> c.header.any { it.math != null || it.image != null } } || block.rows.any { row -> row.any { cell -> cell.any { it.math != null || it.image != null } } }) { row, column, modifier ->
                val runs = if (row == 0) block.columns[column].header else block.rows[row - 1][column]
                ReaderResourceText(runs, 128.dp, onViewMathSource, images, modifier, mdiveTextStyle("documentTable"), when (block.columns[column].alignment) { MarkdownAlignment.Left -> TextAlign.Left; MarkdownAlignment.Center -> TextAlign.Center; MarkdownAlignment.Right -> TextAlign.Right }, onLink = onLink)
            } else null,
        )
        is MarkdownBlock.Quote -> {
            val inset = 19.dp // DS quote rail 3dp + documentInset 16dp.
            if (availableWidth - inset < minimumTextWidth) Blocks(block.blocks, availableWidth, onCopyCode, onViewMathSource, onViewMermaidSource, onExpandMermaid, mermaidCache, images, onLink)
            else MDiveMarkdownQuote { Blocks(block.blocks, availableWidth - inset, onCopyCode, onViewMathSource, onViewMermaidSource, onExpandMermaid, mermaidCache, images, onLink) }
        }
        is MarkdownBlock.ListBlock -> {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val style = MaterialTheme.typography.bodyLarge // MDive theme maps this to the body role.
            val markerWidth = block.items.indices.maxOfOrNull { index ->
                if (block.tasks.getOrNull(index) != null) return@maxOfOrNull 32.dp
                val label = block.start?.let { "${it + index}." } ?: "•"
                with(density) { measurer.measure(label, style).size.width.toDp() } + 8.dp
            }?.coerceAtLeast(32.dp) ?: 0.dp
            val flatten = availableWidth - markerWidth < minimumTextWidth
            MDiveMarkdownFlow(spacing = 8.dp) {
                block.items.forEachIndexed { index, item ->
                    val task = block.tasks.getOrNull(index)
                    val marker = task?.let { MDiveListMarker.Task(it) }
                        ?: block.start?.let { MDiveListMarker.Ordered(it + index) } ?: MDiveListMarker.Bullet
                    if (flatten) {
                        if (task != null) MDiveCheckbox(task, onCheckedChange = null)
                        else MDiveMarkdownText(listOf(MDiveTextRun(block.start?.let { "${it + index}." } ?: "•")))
                        Blocks(item, availableWidth, onCopyCode, onViewMathSource, onViewMermaidSource, onExpandMermaid, mermaidCache, images, onLink)
                    } else MDiveMarkdownListItem(marker) {
                        Blocks(item, availableWidth - markerWidth, onCopyCode, onViewMathSource, onViewMermaidSource, onExpandMermaid, mermaidCache, images, onLink)
                    }
                }
            }
        }
    }
}

private fun List<MarkdownRun>.presentation() = map {
    MDiveTextRun(it.text, emphasis = it.emphasis, strong = it.strong, code = it.code, strike = it.strike, link = it.link)
}
