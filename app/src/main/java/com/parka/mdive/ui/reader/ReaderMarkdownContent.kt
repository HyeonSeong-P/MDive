package com.parka.mdive.ui.reader

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

@Composable
fun ReaderMarkdownContent(document: MarkdownDocument, modifier: Modifier = Modifier) {
    // Measure once outside quote's intrinsic layout; nested blocks receive the remaining width.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val contentWidth = (maxWidth - 32.dp).coerceAtLeast(0.dp)
        LazyColumn(Modifier.fillMaxWidth().testTag("markdown-body"), contentPadding = PaddingValues(16.dp)) {
            items(document.blocks.size) { index ->
                val block = document.blocks[index]
                Column(Modifier.fillMaxWidth().padding(top = spaceBefore(document.blocks, index))) {
                    Block(block, contentWidth)
                }
            }
        }
    }
}

private fun spaceBefore(blocks: List<MarkdownBlock>, index: Int): Dp =
    if (index == 0) 0.dp else if (blocks[index] is MarkdownBlock.Heading) 32.dp
    else if (blocks[index - 1] is MarkdownBlock.Heading) 8.dp else 16.dp

@Composable
private fun Blocks(blocks: List<MarkdownBlock>, availableWidth: Dp) {
    blocks.forEachIndexed { index, block ->
        Column(Modifier.fillMaxWidth().padding(top = spaceBefore(blocks, index))) { Block(block, availableWidth) }
    }
}

@Composable
private fun Block(block: MarkdownBlock, availableWidth: Dp) {
    val minimumTextWidth = 128.dp * LocalDensity.current.fontScale
    when (block) {
        is MarkdownBlock.Heading -> MDiveMarkdownHeading(block.level, block.runs.presentation())
        is MarkdownBlock.Paragraph -> MDiveMarkdownText(block.runs.presentation())
        is MarkdownBlock.Source -> MDiveMarkdownText(listOf(MDiveTextRun(block.source)))
        MarkdownBlock.Divider -> MDiveMarkdownDivider()
        is MarkdownBlock.Table -> MDiveMarkdownTable(
            block.columns.map { column -> MDiveTableColumn(column.header.presentation(), when (column.alignment) {
                MarkdownAlignment.Left -> TextAlign.Left
                MarkdownAlignment.Center -> TextAlign.Center
                MarkdownAlignment.Right -> TextAlign.Right
            }) },
            block.rows.map { row -> row.map { it.presentation() } },
            Modifier.testTag("markdown-table"),
        )
        is MarkdownBlock.Quote -> {
            val inset = 19.dp // DS quote rail 3dp + documentInset 16dp.
            if (availableWidth - inset < minimumTextWidth) Blocks(block.blocks, availableWidth)
            else MDiveMarkdownQuote { Blocks(block.blocks, availableWidth - inset) }
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
                        Blocks(item, availableWidth)
                    } else MDiveMarkdownListItem(marker) {
                        Blocks(item, availableWidth - markerWidth)
                    }
                }
            }
        }
    }
}

private fun List<MarkdownRun>.presentation() = map {
    MDiveTextRun(it.text, emphasis = it.emphasis, strong = it.strong, code = it.code, strike = it.strike)
}
