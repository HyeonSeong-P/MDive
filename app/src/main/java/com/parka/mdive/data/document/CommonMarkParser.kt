package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicInteger
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
            parseRoot(source)
        } catch (error: IllegalArgumentException) {
            // Preserve reading if the table extension's cell safety limit is reached.
            if (error.message?.startsWith("Aborting parsing because maximum number of cells reached") != true) throw error
            ensureActive()
            parser(tables = false).parse(source)
        }
        ensureActive()
        prepareMath(MarkdownDocument(blocks(root, source, CodeSourceReader(source), references = referenceSource(root, source))))
    }


    private fun parseRoot(source: String, references: String = ""): Node = try { parser(tables = true, references = references).parse(source) } catch (error: IllegalArgumentException) {
        if (error.message?.startsWith("Aborting parsing because maximum number of cells reached") != true) throw error
        parser(tables = false, references = references).parse(source)
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
                is MarkdownBlock.Callout -> block.copy(blocks = visit(block.blocks))
                is MarkdownBlock.DefinitionList -> block.copy(entries = block.entries.map { it.copy(terms = it.terms.map(::runs), definitions = it.definitions.map { visit(it) }) })
                // Parsing prepares bounded offline math pixels; visibility controls UI/image/Mermaid work.
                is MarkdownBlock.Details -> block.copy(summary = runs(block.summary), blocks = visit(block.blocks))
                is MarkdownBlock.Table -> block.copy(columns = block.columns.map { it.copy(header = runs(it.header)) }, rows = block.rows.map { row -> row.map(::runs) })
                else -> block
            }
        }
        return MarkdownDocument(visit(document.blocks))
    }

    private fun parser(tables: Boolean, references: String = ""): Parser = Parser.builder()
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .customInlineContentParserFactory(MathInlineFactory())
        .customBlockParserFactory(MathBlockFactory())
        .customBlockParserFactory(ContainerFactory())
        .customBlockParserFactory(DefinitionFactory())
        .customInlineContentParserFactory(EmojiInlineFactory())
        .customDelimiterProcessor(InlineStyleDelimiter('=', 2))
        .customDelimiterProcessor(InlineStyleDelimiter('+', 2))
        .customDelimiterProcessor(InlineStyleDelimiter('^', 1))
        .extensions(listOf(StrikethroughExtension.builder().requireTwoTildes(false).build()))
        .apply {
            if (tables) customBlockParserFactory(GfmTableFactory())
            if (references.isNotEmpty()) linkProcessor(InheritedReferenceLinks(references))
        }
        .build()

    private suspend fun blocks(parent: Node, source: String, codeSource: CodeSourceReader, depth: Int = 0, taskPrefix: Int = 0, ids: AtomicInteger = AtomicInteger(), references: String = ""): List<MarkdownBlock> =
        children(parent).mapIndexedNotNull { index, node ->
            currentCoroutineContext().ensureActive()
            if (depth >= 128) MarkdownBlock.Source(raw(node, source)) else when (node) {
                // CommonMark reference declarations are metadata for links, not body blocks.
                // Footnote-shaped declarations remain source until S13 defines their grammar.
                is LinkReferenceDefinition -> if (node.label.startsWith("^")) MarkdownBlock.Source(raw(node, source)) else null
                is MathBlockNode -> {
                    val original = raw(node, source)
                    val normalized = node.lines.joinToString("\n")
                    val tex = normalized.drop(2).trimEnd().removeSuffix("$$")
                    if (!node.closed || tex.isBlank()) MarkdownBlock.Source(original)
                    else MarkdownBlock.Math(MarkdownMath(tex, original, true))
                }
                is Heading -> MarkdownBlock.Heading(node.level, runs(node, source))
                is Paragraph -> MarkdownBlock.Paragraph(runs(node, source, skipPrefix = if (index == 0) taskPrefix else 0))
                is BlockQuote -> alert(node, source, codeSource, depth, ids, references)
                is ContainerNode -> container(node, source, depth, ids, references)
                is DefinitionNode -> MarkdownBlock.DefinitionList(listOf(MarkdownDefinition(
                    node.terms.map { inlineSource(it, references) },
                    node.definitions.map { nestedBlocks(joinLines(it, source), depth + 1, ids, references) },
                )))
                is BulletList -> list(node, null, source, codeSource, depth, ids, references)
                is OrderedList -> list(node, node.markerStartNumber, source, codeSource, depth, ids, references)
                is TableBlock -> table(node, source)
                is FencedCodeBlock -> {
                    val language = node.info.trim().takeWhile { !it.isWhitespace() }.ifEmpty { null }
                    if (language == "math") {
                        if (codeSource.read(node).isBlank() || node.closingFenceLength == null) MarkdownBlock.Source(raw(node, source))
                        else MarkdownBlock.Math(MarkdownMath(codeSource.read(node), raw(node, source), true))
                    } else if (language.equals("mermaid", ignoreCase = true)) {
                        val code = codeSource.read(node)
                        MarkdownBlock.Mermaid(MarkdownMermaid(code, raw(node, source), node.closingFenceLength != null && MermaidProfile.accepts(code)))
                    }
                    else codeSource.read(node).let { MarkdownBlock.Code(it, language, codeHighlighter.highlight(it, language)) }
                }
                is IndentedCodeBlock -> MarkdownBlock.Code(codeSource.read(node), null)
                is ThematicBreak -> MarkdownBlock.Divider
                else -> MarkdownBlock.Source(raw(node, source))
            }
        }

    private suspend fun list(node: Node, start: Int?, source: String, codeSource: CodeSourceReader, depth: Int, ids: AtomicInteger, references: String): MarkdownBlock.ListBlock {
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
            blocks(item, source, codeSource, depth + 1, taskPrefix = if (tasks[index] == null) 0 else 4, ids = ids, references = references)
        }, tasks)
    }

    /** SourceLine strips terminators; recover each original terminator so nested code copy stays exact. */
    private fun joinLines(lines: List<org.commonmark.parser.SourceLine>, source: String): String = buildString {
        var cursor = lines.firstOrNull()?.sourceSpan?.inputIndex ?: 0
        lines.forEachIndexed { index, line ->
            append(line.content)
            val span = line.sourceSpan
            val newline = source.indexOfAny(charArrayOf('\r', '\n'), span?.let { it.inputIndex + it.length } ?: cursor)
            val terminator = when {
                newline < 0 -> "\n"
                source[newline] == '\r' && source.getOrNull(newline + 1) == '\n' -> "\r\n"
                else -> source[newline].toString()
            }
            if (index != lines.lastIndex || newline >= 0) append(terminator)
            if (newline >= 0) cursor = newline + terminator.length
        }
    }

    private fun inlineSource(source: String, references: String = ""): List<MarkdownRun> {
        val root = parser(tables = false, references = references).parse(source)
        val paragraph = root.firstChild ?: return emptyList()
        return runs(paragraph, source)
    }

    private suspend fun nestedBlocks(source: String, depth: Int, ids: AtomicInteger, references: String): List<MarkdownBlock> {
        if (depth >= 128) return listOf(MarkdownBlock.Source(source))
        val root = parseRoot(source, references)
        return blocks(root, source, CodeSourceReader(source), depth, ids = ids,
            references = referenceSource(root, source) + "\n" + references)
    }

    private fun referenceSource(root: Node, source: String): String = buildList {
        val pending = java.util.ArrayDeque<Node>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node is LinkReferenceDefinition && !node.label.startsWith("^")) add(raw(node, source))
            children(node).forEach(pending::addLast)
        }
    }.joinToString("\n")

    private suspend fun container(node: ContainerNode, source: String, depth: Int, ids: AtomicInteger, references: String): MarkdownBlock {
        if (!node.closed) return MarkdownBlock.Source(raw(node, source))
        if (!node.details) {
            val match = ContainerFactory.calloutOpening.matchEntire(node.opener)!!
            val kind = MarkdownCalloutKind.entries.first { it.name.equals(match.groupValues[2], true) }
            return MarkdownBlock.Callout(kind, match.groupValues[3].ifBlank { kind.name },
                nestedBlocks(joinLines(node.inputLines.drop(1).dropLast(1), source), depth + 1, ids, references))
        }
        val id = "details-${ids.getAndIncrement()}"
        val opening = ContainerFactory.detailsOpening.find(node.lines.first())!!
        val full = joinLines(node.inputLines, source)
        val closing = Regex("</details\\s*>", RegexOption.IGNORE_CASE).findAll(full).lastOrNull()
            ?: return MarkdownBlock.Source(raw(node, source))
        var body = full.substring(opening.range.last + 1, closing.range.first).trimStart('\n', '\r')
        val summary = Regex("^ {0,3}<summary>([\\s\\S]*?)</summary>", RegexOption.IGNORE_CASE).find(body)
        val title = summary?.groupValues?.get(1)?.let { inlineSource(it, references) }?.takeIf { runs -> runs.any { it.text.isNotBlank() } }
            ?: listOf(MarkdownRun("자세히 보기"))
        if (summary != null) body = body.substring(summary.range.last + 1).trimStart('\n', '\r')
        return MarkdownBlock.Details(id, title, Regex("\\sopen(?:\\s|=|>)", RegexOption.IGNORE_CASE).containsMatchIn(opening.value), nestedBlocks(body, depth + 1, ids, references))
    }

    private suspend fun alert(node: BlockQuote, source: String, codeSource: CodeSourceReader, depth: Int, ids: AtomicInteger, references: String): MarkdownBlock {
        val first = node.firstChild as? Paragraph
        val firstText = first?.firstChild as? Text
        val marker = firstText?.literal?.let { Regex("^\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]$").matchEntire(it) }
        val lineBreak = firstText?.next
        val allowed = depth == 0 && marker != null && firstText != null && raw(firstText, source) == firstText.literal && (lineBreak == null || lineBreak is SoftLineBreak || lineBreak is HardLineBreak)
        if (!allowed) return MarkdownBlock.Quote(blocks(node, source, codeSource, depth + 1, ids = ids, references = references))
        val kind = MarkdownCalloutKind.entries.first { it.name.equals(marker!!.groupValues[1], true) }
        // Convert once, then remove only the marker run; references/source positions stay from the original AST.
        val body = blocks(node, source, codeSource, depth + 1, ids = ids, references = references).toMutableList()
        val paragraph = body.firstOrNull() as? MarkdownBlock.Paragraph
        if (paragraph != null) {
            val rest = paragraph.runs.drop(1).toMutableList()
            if (rest.firstOrNull()?.text == "\n") rest.removeAt(0)
            if (rest.isEmpty()) body.removeAt(0) else body[0] = paragraph.copy(runs = rest)
        }
        return MarkdownBlock.Callout(kind, kind.name, body)
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
        val activeStyles = mutableListOf<String>()
        val validTags = java.util.IdentityHashMap<Node, String>()
        fun matchTags(parent: Node) {
            val stack = mutableListOf<Pair<String, Node>>()
            children(parent).forEach { child ->
                if (child is HtmlInline) {
                    val match = Regex("^<(/?)(sub|sup|ins|u)>$", RegexOption.IGNORE_CASE).matchEntire(child.literal)
                    if (match != null) {
                        val name = match.groupValues[2].lowercase(java.util.Locale.ROOT)
                        if (match.groupValues[1].isEmpty()) stack += name to child
                        else if (stack.lastOrNull()?.first == name) {
                            val open = stack.removeAt(stack.lastIndex).second
                            validTags[open] = name; validTags[child] = "/$name"
                        }
                    }
                }
                if (child !is Code && child !is MathInline) matchTags(child)
            }
        }
        matchTags(parent)
        fun emit(run: MarkdownRun) {
            result += run.copy(highlight = "=" in activeStyles, subscript = "sub" in activeStyles,
                superscript = "sup" in activeStyles || "^" in activeStyles,
                inserted = "ins" in activeStyles || "+" in activeStyles,
                underline = "ins" in activeStyles || "u" in activeStyles)
        }
        fun visit(node: Node, emphasis: Boolean = false, strong: Boolean = false, strike: Boolean = false, depth: Int = 0) {
            when {
                depth >= 128 -> emit(MarkdownRun(raw(node, source, tableCell), emphasis, strong, strike = strike))
                node is HtmlInline -> {
                    val literal = raw(node, source, tableCell)
                    val style = validTags[node]
                    if (style != null && (htmlTags.isEmpty() || (style.startsWith('/') && style.drop(1) in activeStyles))) {
                        if (style.startsWith('/')) {
                            val index = activeStyles.indexOfLast { it == style.drop(1) }
                            if (index >= 0) activeStyles.removeAt(index)
                        } else activeStyles += style
                        return
                    }
                    val tag = Regex("^<(\\/?)([A-Za-z][A-Za-z0-9-]*)\\b").find(literal)
                    if (tag != null) {
                        val name = tag.groupValues[2].lowercase(java.util.Locale.ROOT)
                        val void = name in setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
                        if (!void && tag.groupValues[1] == "/") {
                            val index = htmlTags.indexOfLast { it == name }
                            if (index >= 0) while (htmlTags.size > index) htmlTags.removeAt(htmlTags.lastIndex)
                        } else if (!void && !literal.endsWith("/>")) htmlTags += name
                    }
                    emit(MarkdownRun(literal, emphasis, strong, strike = strike))
                }
                node is StyledInline && htmlTags.isNotEmpty() -> emit(MarkdownRun(raw(node, source, tableCell), emphasis, strong, strike = strike))
                node is StyledInline -> {
                    activeStyles += node.style.toString()
                    children(node).forEach { visit(it, emphasis, strong, strike, depth + 1) }
                    activeStyles.removeAt(activeStyles.lastIndex)
                }
                node is EmojiInline -> emit(MarkdownRun(if (htmlTags.isEmpty()) node.unicode else node.source, emphasis, strong, strike = strike))
                node is MathInline -> emit(MarkdownRun(node.source, emphasis, strong, strike = strike, math = if (htmlTags.isEmpty()) raw(node, source, tableCell).ifEmpty { node.source }.let { original -> MarkdownMath(if (original.startsWith("$`")) original.drop(2).dropLast(2) else original.drop(1).dropLast(1), original, false) } else null))
                node is Image && htmlTags.isEmpty() -> {
                    fun alt(node: Node): String = children(node).joinToString("") { child -> when (child) {
                        is Text -> child.literal
                        is Code -> child.literal
                        is EmojiInline -> child.unicode
                        is SoftLineBreak, is HardLineBreak -> " "
                        else -> alt(child)
                    } }
                    val image = MarkdownImage(node.destination, alt(node), node.title)
                    emit(MarkdownRun(raw(node, source, tableCell), emphasis, strong, strike = strike, image = image))
                }
                node is Link && htmlTags.isEmpty() -> {
                    val start = result.size
                    children(node).forEach { visit(it, emphasis, strong, strike, depth + 1) }
                    for (index in start until result.size) result[index] = result[index].copy(link = node.destination, math = null)
                }
                node is Text -> emit(MarkdownRun(node.literal, emphasis, strong, strike = strike))
                node is Code -> emit(MarkdownRun(node.literal, emphasis, strong, code = true, strike = strike))
                node is SoftLineBreak || node is HardLineBreak -> emit(MarkdownRun("\n", emphasis, strong, strike = strike))
                node is Emphasis -> children(node).forEach { visit(it, true, strong, strike, depth + 1) }
                node is StrongEmphasis -> children(node).forEach { visit(it, emphasis, true, strike, depth + 1) }
                node is Strikethrough -> children(node).forEach { visit(it, emphasis, strong, true, depth + 1) }
                else -> emit(MarkdownRun(raw(node, source, tableCell), emphasis, strong, strike = strike))
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
