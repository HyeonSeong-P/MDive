package com.parka.mdive.data.document

import com.parka.mdive.domain.document.MarkdownBlock
import com.parka.mdive.domain.document.MarkdownDocument
import com.parka.mdive.domain.document.MarkdownParser
import com.parka.mdive.domain.document.MarkdownRun
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.commonmark.node.*
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

/** CommonMark AST stays inside data; source spans retain syntax for later feature sessions. */
class CommonMarkParser @Inject constructor() : MarkdownParser {
    override suspend fun parse(source: String): MarkdownDocument = withContext(Dispatchers.Default) {
        ensureActive()
        val root = Parser.builder().includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES).build().parse(source)
        ensureActive()
        MarkdownDocument(blocks(root, source))
    }

    private suspend fun blocks(parent: Node, source: String, depth: Int = 0): List<MarkdownBlock> =
        children(parent).map { node ->
            currentCoroutineContext().ensureActive()
            // Bound adapter recursion without discarding deeply nested input.
            if (depth >= 128) MarkdownBlock.Source(raw(node, source)) else when (node) {
                is Heading -> MarkdownBlock.Heading(node.level, runs(node, source))
                is Paragraph -> MarkdownBlock.Paragraph(runs(node, source))
                is BlockQuote -> MarkdownBlock.Quote(blocks(node, source, depth + 1))
                is BulletList -> MarkdownBlock.ListBlock(null, children(node).map { blocks(it, source, depth + 1) })
                is OrderedList -> MarkdownBlock.ListBlock(node.markerStartNumber, children(node).map { blocks(it, source, depth + 1) })
                is ThematicBreak -> MarkdownBlock.Divider
                else -> MarkdownBlock.Source(raw(node, source))
            }
        }

    private fun runs(parent: Node, source: String): List<MarkdownRun> {
        val result = mutableListOf<MarkdownRun>()
        fun visit(node: Node, emphasis: Boolean = false, strong: Boolean = false, depth: Int = 0) {
            when {
                depth >= 128 -> result += MarkdownRun(raw(node, source), emphasis, strong)
                node is Text -> result += MarkdownRun(node.literal, emphasis, strong)
                node is Code -> result += MarkdownRun(node.literal, emphasis, strong, code = true)
                node is SoftLineBreak -> result += MarkdownRun("\n", emphasis, strong)
                node is HardLineBreak -> result += MarkdownRun("\n", emphasis, strong)
                node is Emphasis -> children(node).forEach { visit(it, true, strong, depth + 1) }
                node is StrongEmphasis -> children(node).forEach { visit(it, emphasis, true, depth + 1) }
                else -> result += MarkdownRun(raw(node, source), emphasis, strong)
            }
        }
        children(parent).forEach { visit(it) }
        return result
    }

    private fun children(node: Node): List<Node> = buildList {
        var child = node.firstChild
        while (child != null) { add(child); child = child.next }
    }

    private fun raw(node: Node, source: String): String {
        val spans = node.sourceSpans
        if (spans.isEmpty()) return ""
        val first = spans.first()
        val last = spans.last()
        return source.substring(first.inputIndex, last.inputIndex + last.length)
    }
}
