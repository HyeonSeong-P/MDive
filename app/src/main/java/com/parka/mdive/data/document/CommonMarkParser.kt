package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.*
import org.commonmark.node.*
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

/** CommonMark AST stays inside data; source spans retain syntax for later feature sessions. */
class CommonMarkParser @Inject constructor(private val codeHighlighter: CodeHighlighter, private val mathRenderer: MathRenderer) : MarkdownParser {
    constructor() : this(CodeHighlighter(), MathRenderer())
    constructor(codeHighlighter: CodeHighlighter) : this(codeHighlighter, MathRenderer())
    override suspend fun parse(source: String): MarkdownDocument = withContext(Dispatchers.Default) {
        ensureActive()
        val root = try {
            parser(tables = true).parse(source)
        } catch (error: IllegalArgumentException) {
            // Preserve reading if the table extension's cell safety limit is reached.
            if (error.message?.startsWith("Aborting parsing because maximum number of cells reached") != true) throw error
            ensureActive()
            parser(tables = false).parse(source)
        }
        ensureActive()
        prepareMath(MarkdownDocument(blocks(root, source, CodeSourceReader(source))))
    }


    private suspend fun prepareMath(document: MarkdownDocument): MarkdownDocument {
        var pixels = 0L
        var count = 0
        fun prepare(math: MarkdownMath): MarkdownMath {
            // Bound retained raster memory for the whole document, not just each expression.
            if (++count > 256 || pixels >= 4_000_000) return math
            val raster = mathRenderer.render(math.tex, math.display) ?: return math
            val area = raster.width.toLong() * raster.height
            if (pixels + area > 4_000_000) return math
            pixels += area
            return math.copy(raster = raster)
        }
        fun runs(runs: List<MarkdownRun>) = runs.map { it.copy(math = it.math?.let(::prepare)) }
        suspend fun visit(blocks: List<MarkdownBlock>): List<MarkdownBlock> = blocks.map { block ->
            currentCoroutineContext().ensureActive()
            when (block) {
                is MarkdownBlock.Math -> block.copy(math = prepare(block.math))
                is MarkdownBlock.Paragraph -> block.copy(runs = runs(block.runs))
                is MarkdownBlock.Heading -> block.copy(runs = runs(block.runs))
                is MarkdownBlock.Quote -> block.copy(blocks = visit(block.blocks))
                is MarkdownBlock.ListBlock -> block.copy(items = block.items.map { visit(it) })
                is MarkdownBlock.Table -> block.copy(columns = block.columns.map { it.copy(header = runs(it.header)) }, rows = block.rows.map { row -> row.map(::runs) })
                else -> block
            }
        }
        return MarkdownDocument(visit(document.blocks))
    }

    private fun parser(tables: Boolean): Parser = Parser.builder()
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .customInlineContentParserFactory(MathInlineFactory())
        .customBlockParserFactory(MathBlockFactory())
        .extensions(listOf(StrikethroughExtension.builder().requireTwoTildes(true).build()))
        .apply { if (tables) customBlockParserFactory(GfmTableFactory()) }
        .build()

    private suspend fun blocks(parent: Node, source: String, codeSource: CodeSourceReader, depth: Int = 0, taskPrefix: Int = 0): List<MarkdownBlock> =
        children(parent).mapIndexed { index, node ->
            currentCoroutineContext().ensureActive()
            if (depth >= 128) MarkdownBlock.Source(raw(node, source)) else when (node) {
                is MathBlockNode -> {
                    val original = raw(node, source)
                    val normalized = node.lines.joinToString("\n")
                    val tex = normalized.drop(2).trimEnd().removeSuffix("$$")
                    if (!node.closed || tex.isBlank()) MarkdownBlock.Source(original)
                    else MarkdownBlock.Math(MarkdownMath(tex, original, true))
                }
                is Heading -> MarkdownBlock.Heading(node.level, runs(node, source))
                is Paragraph -> MarkdownBlock.Paragraph(runs(node, source, skipPrefix = if (index == 0) taskPrefix else 0))
                is BlockQuote -> MarkdownBlock.Quote(blocks(node, source, codeSource, depth + 1))
                is BulletList -> list(node, null, source, codeSource, depth)
                is OrderedList -> list(node, node.markerStartNumber, source, codeSource, depth)
                is TableBlock -> table(node, source)
                is FencedCodeBlock -> {
                    val language = node.info.trim().takeWhile { !it.isWhitespace() }.ifEmpty { null }
                    if (language == "math") {
                        if (codeSource.read(node).isBlank() || node.closingFenceLength == null) MarkdownBlock.Source(raw(node, source))
                        else MarkdownBlock.Math(MarkdownMath(codeSource.read(node), raw(node, source), true))
                    } else if (language.equals("mermaid", ignoreCase = true)) MarkdownBlock.Source(raw(node, source))
                    else codeSource.read(node).let { MarkdownBlock.Code(it, language, codeHighlighter.highlight(it, language)) }
                }
                is IndentedCodeBlock -> MarkdownBlock.Code(codeSource.read(node), null)
                is ThematicBreak -> MarkdownBlock.Divider
                else -> MarkdownBlock.Source(raw(node, source))
            }
        }

    private suspend fun list(node: Node, start: Int?, source: String, codeSource: CodeSourceReader, depth: Int): MarkdownBlock.ListBlock {
        val items = children(node)
        val tasks = items.map { item ->
            val paragraph = item.firstChild as? Paragraph
            val first = paragraph?.sourceSpans?.firstOrNull()?.inputIndex
            val end = paragraph?.sourceSpans?.lastOrNull()?.let { it.inputIndex + it.length } ?: 0
            // Inspect source, not decoded Text: escaped/entity brackets must stay ordinary text.
            if (first != null && first + 3 < end && source[first] == '[' &&
                source[first + 1] in " xX" && source[first + 2] == ']' && source[first + 3] in " \t\r\n") {
                source[first + 1] != ' '
            } else null
        }
        return MarkdownBlock.ListBlock(start, items.mapIndexed { index, item ->
            blocks(item, source, codeSource, depth + 1, taskPrefix = if (tasks[index] == null) 0 else 4)
        }, tasks)
    }

    private fun table(node: TableBlock, source: String): MarkdownBlock.Table {
        val head = children(node).filterIsInstance<TableHead>().single()
        val headers = children(head.firstChild).filterIsInstance<TableCell>()
        val columns = headers.map { cell ->
            MarkdownTableColumn(runs(cell, source, tableCell = cell), when (cell.alignment) {
                TableCell.Alignment.CENTER -> MarkdownAlignment.Center
                TableCell.Alignment.RIGHT -> MarkdownAlignment.Right
                else -> MarkdownAlignment.Left
            })
        }
        val rows = children(node).filterIsInstance<TableBody>().flatMap { children(it) }.map { row ->
            children(row).filterIsInstance<TableCell>().map { runs(it, source, tableCell = it) }
        }
        return MarkdownBlock.Table(columns, rows)
    }

    private fun runs(parent: Node, source: String, skipPrefix: Int = 0, tableCell: TableCell? = null): List<MarkdownRun> {
        val result = mutableListOf<MarkdownRun>()
        val htmlTags = mutableListOf<String>()
        fun visit(node: Node, emphasis: Boolean = false, strong: Boolean = false, strike: Boolean = false, depth: Int = 0) {
            when {
                depth >= 128 -> result += MarkdownRun(raw(node, source, tableCell), emphasis, strong, strike = strike)
                node is HtmlInline -> {
                    val literal = raw(node, source, tableCell)
                    val tag = Regex("^<(\\/?)([A-Za-z][A-Za-z0-9-]*)\\b").find(literal)
                    if (tag != null) {
                        val name = tag.groupValues[2].lowercase(java.util.Locale.ROOT)
                        val void = name in setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
                        if (!void && tag.groupValues[1] == "/") {
                            val index = htmlTags.indexOfLast { it == name }
                            if (index >= 0) while (htmlTags.size > index) htmlTags.removeAt(htmlTags.lastIndex)
                        } else if (!void && !literal.endsWith("/>")) htmlTags += name
                    }
                    result += MarkdownRun(literal, emphasis, strong, strike = strike)
                }
                node is MathInline -> result += MarkdownRun(node.source, emphasis, strong, strike = strike, math = if (htmlTags.isEmpty()) raw(node, source, tableCell).ifEmpty { node.source }.let { original -> MarkdownMath(if (original.startsWith("$`")) original.drop(2).dropLast(2) else original.drop(1).dropLast(1), original, false) } else null)
                node is Text -> result += MarkdownRun(node.literal, emphasis, strong, strike = strike)
                node is Code -> result += MarkdownRun(node.literal, emphasis, strong, code = true, strike = strike)
                node is SoftLineBreak || node is HardLineBreak -> result += MarkdownRun("\n", emphasis, strong, strike = strike)
                node is Emphasis -> children(node).forEach { visit(it, true, strong, strike, depth + 1) }
                node is StrongEmphasis -> children(node).forEach { visit(it, emphasis, true, strike, depth + 1) }
                node is Strikethrough -> children(node).forEach { visit(it, emphasis, strong, true, depth + 1) }
                else -> result += MarkdownRun(raw(node, source, tableCell), emphasis, strong, strike = strike)
            }
        }
        children(parent).forEach { visit(it) }
        var remaining = skipPrefix
        return result.mapNotNull { run ->
            val skip = remaining.coerceAtMost(run.text.length)
            remaining -= skip
            run.copy(text = run.text.drop(skip)).takeIf { it.text.isNotEmpty() }
        }
    }

    private fun children(node: Node): List<Node> = buildList {
        var child = node.firstChild
        while (child != null) { add(child); child = child.next }
    }

    private fun raw(node: Node, source: String, tableCell: TableCell? = null): String {
        val spans = node.sourceSpans
        if (spans.isEmpty()) return ""
        val first = spans.first()
        val last = spans.last()
        val cellSpan = tableCell?.sourceSpans?.firstOrNull()
        if (cellSpan == null) return source.substring(first.inputIndex, last.inputIndex + last.length)
        // The table extension removes backslashes before pipes prior to inline parsing.
        // Translate those normalized offsets back to the original cell for inert source runs.
        val cellSource = source.substring(cellSpan.inputIndex, cellSpan.inputIndex + cellSpan.length)
        fun originalOffset(normalized: Int): Int {
            var original = 0
            var offset = 0
            while (offset < normalized && original < cellSource.length) {
                if (cellSource[original] == '\\' && cellSource.getOrNull(original + 1) == '|') original++
                original++; offset++
            }
            return original
        }
        val start = originalOffset(first.inputIndex - cellSpan.inputIndex)
        val end = originalOffset(last.inputIndex + last.length - cellSpan.inputIndex)
        return cellSource.substring(start, end)
    }
}
