package com.parka.mdive.data.document

import org.commonmark.node.CustomNode
import org.commonmark.node.CustomBlock
import org.commonmark.parser.SourceLine
import org.commonmark.parser.beta.*
import org.commonmark.parser.block.*

internal class MathInline(val tex: String, val source: String) : CustomNode()
internal class MathBlockNode : CustomBlock() {
    val lines = mutableListOf<String>()
    var closed = false
}

/** Runs before Markdown emphasis/escape processing, keeping TeX byte-for-byte inside delimiters. */
internal class MathInlineFactory : InlineContentParserFactory {
    override fun getTriggerCharacters() = setOf('$')
    override fun create() = InlineContentParser { state ->
        val scanner = state.scanner()
        val start = scanner.position()
        if (scanner.peekPreviousCodePoint() == '$'.code) return@InlineContentParser ParsedInline.none()
        scanner.next()
        val quoted = scanner.next('`')
        if (!quoted && scanner.peek() == '$') return@InlineContentParser ParsedInline.none()
        val body = scanner.position()
        var escaped = false
        while (scanner.hasNext() && scanner.peek() != '\n') {
            val c = scanner.peek()
            if (!escaped && ((!quoted && c == '$') || (quoted && c == '`'))) {
                val end = scanner.position()
                scanner.next()
                if (quoted && !scanner.next('$')) continue
                if (scanner.peek() == '$') return@InlineContentParser ParsedInline.none()
                val tex = scanner.getSource(body, end).content
                if (tex.isBlank() || Regex("</?[A-Za-z][^>]*>").containsMatchIn(tex)) return@InlineContentParser ParsedInline.none()
                val source = scanner.getSource(start, scanner.position()).content
                return@InlineContentParser ParsedInline.of(MathInline(tex, source), scanner.position())
            }
            scanner.next()
            escaped = if (escaped) false else c == '\\'
        }
        ParsedInline.none()
    }
}

internal class MathBlockFactory : AbstractBlockParserFactory() {
    override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
        if (state.indent >= 4) return BlockStart.none()
        val line = state.line.content.toString().substring(state.nextNonSpaceIndex)
        if (!line.startsWith("$$") || line.startsWith("$$$")) return BlockStart.none()
        return BlockStart.of(MathBlockParser()).atIndex(state.nextNonSpaceIndex)
    }
}

private class MathBlockParser : AbstractBlockParser() {
    private val node = MathBlockNode()
    override fun getBlock() = node
    override fun tryContinue(state: ParserState): BlockContinue? =
        if (node.closed) BlockContinue.none() else BlockContinue.atIndex(state.index)
    override fun addLine(line: SourceLine) {
        val value = line.content.toString()
        node.lines += value
        val content = if (node.lines.size == 1) value.drop(2) else value
        // Only an unescaped closing pair at end of line closes a display block.
        val trimmed = content.trimEnd()
        if (trimmed.endsWith("$$") && !trimmed.endsWith("$$$") && trimmed.dropLast(2).takeLastWhile { it == '\\' }.length % 2 == 0) node.closed = true
    }
}
