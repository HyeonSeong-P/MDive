package com.parka.mdive.data.document

import org.commonmark.node.CustomBlock
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.*

internal class ContainerNode(val details: Boolean, val opener: String) : CustomBlock() {
    val lines = mutableListOf<String>()
    val inputLines = mutableListOf<SourceLine>()
    var closed = false
}

internal class ContainerFactory : AbstractBlockParserFactory() {
    override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
        if (state.indent >= 4) return BlockStart.none()
        val line = state.line.content.toString().substring(state.nextNonSpaceIndex)
        val details = detailsOpening.containsMatchIn(line)
        if (!details && !calloutOpening.matches(line)) return BlockStart.none()
        // Details follows CommonMark type-6 HTML block interruption; app callouts do not.
        if (!details && matchedBlockParser.paragraphLines.lines.isNotEmpty()) return BlockStart.none()
        return BlockStart.of(ContainerParser(details, line)).atIndex(state.nextNonSpaceIndex)
    }
    companion object {
        val detailsOpening = Regex("^<details(?:\\s+open(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+))?)?\\s*>", RegexOption.IGNORE_CASE)
        val calloutOpening = Regex("^(:{3,}) (note|tip|important|warning|caution)(?:[ \\t]+(.*))?[ \\t]*$")
    }
}

private class ContainerParser(details: Boolean, opener: String) : AbstractBlockParser() {
    private val node = ContainerNode(details, opener)
    private var nested = 0
    private val calloutMarkers = mutableListOf(opener.takeWhile { it == ':' }.length)
    private var fence: Char? = null
    private var fenceSize = 0
    override fun getBlock() = node
    override fun tryContinue(state: ParserState): BlockContinue? = if (node.closed) BlockContinue.none() else BlockContinue.atIndex(state.index)
    override fun addLine(line: SourceLine) {
        val value = line.content.toString()
        node.lines += value
        node.inputLines += line
        val trimmed = value.trimStart()
        // Four-space-indented lines are code, not container boundaries. A fenced block
        // may also start immediately after a list marker; its closing delimiter is shielded.
        val indentation = value.takeWhile { it == ' ' || it == '\t' }.sumOf { if (it == '\t') 4 else 1 }
        if (node.lines.size > 1 && indentation >= 4 && fence == null) return
        val fenceText = trimmed.replaceFirst(Regex("^(?:[-+*]|[0-9]{1,9}[.)])[ \\t]+"), "")
        val fenceMatch = Regex("^(`{3,}|~{3,})(.*)$").matchEntire(fenceText)
        if (node.lines.size > 1 && fenceMatch != null) {
            val run = fenceMatch.groupValues[1]
            if (fence == null) {
                // CommonMark forbids backticks in the info string of a backtick fence.
                if (run[0] == '`' && '`' in fenceMatch.groupValues[2]) return
                fence = run[0]; fenceSize = run.length
            }
            else if (run[0] == fence && run.length >= fenceSize && fenceMatch.groupValues[2].isBlank()) fence = null
            return
        }
        if (fence != null) return
        if (node.details) {
            val content = if (node.lines.size == 1) value.substring(ContainerFactory.detailsOpening.find(value)!!.range.last + 1) else trimmed
            // Tags inside inline code are literal; fences have already been excluded above.
            val structural = content.replace(Regex("(`+).*?\\1"), "")
            // A closing tag on a body line must be a block boundary, not list/quote text.
            val boundary = node.lines.size == 1 || structural.startsWith("<details", ignoreCase = true) || structural.startsWith("</details", ignoreCase = true)
            if (!boundary) return
            val tags = Regex("</?details(?:\\s[^>]*)?>", RegexOption.IGNORE_CASE).findAll(structural)
            for (tag in tags) {
                if (!tag.value.startsWith("</")) nested++
                else if (nested > 0) nested--
                else if (structural.substring(tag.range.last + 1).isBlank()) node.closed = true
            }
        } else {
            if (node.lines.size == 1) return
            val opening = ContainerFactory.calloutOpening.matchEntire(trimmed)
            if (opening != null) calloutMarkers += opening.groupValues[1].length
            else if (Regex("^:{3,}\\s*$").matches(trimmed) && trimmed.trimEnd().length >= calloutMarkers.last()) {
                calloutMarkers.removeAt(calloutMarkers.lastIndex)
                if (calloutMarkers.isEmpty()) node.closed = true
            }
        }
    }
}

internal class DefinitionNode(val terms: List<String>) : CustomBlock() {
    val definitions = mutableListOf<MutableList<SourceLine>>()
}

internal class DefinitionFactory : AbstractBlockParserFactory() {
    override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
        val terms = matchedBlockParser.paragraphLines.lines
        if (terms.isEmpty() || state.indent >= 4) return BlockStart.none()
        val line = state.line.content.toString().substring(state.nextNonSpaceIndex)
        if (!Regex("^:[ \\t]+\\S.*$").matches(line)) return BlockStart.none()
        return BlockStart.of(DefinitionParser(terms.map { it.content.toString() }))
            .atIndex(state.nextNonSpaceIndex).replaceParagraphLines(terms.size)
    }
}

private class DefinitionParser(terms: List<String>) : AbstractBlockParser() {
    private val node = DefinitionNode(terms)
    private var newDefinition = true
    override fun getBlock() = node
    override fun tryContinue(state: ParserState): BlockContinue? {
        val line = state.line.content.toString().substring(state.nextNonSpaceIndex)
        newDefinition = state.indent < 4 && Regex("^:[ \\t]+\\S.*$").matches(line)
        return when {
            newDefinition -> BlockContinue.atIndex(state.nextNonSpaceIndex)
            state.isBlank -> BlockContinue.atIndex(state.index)
            state.indent >= 4 -> BlockContinue.atColumn(state.column + 4)
            else -> BlockContinue.none()
        }
    }
    override fun addLine(line: SourceLine) {
        val text = line.content.toString()
        if (newDefinition) node.definitions += mutableListOf(line.substring((text.drop(1).indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0) + 1), text.length))
        else node.definitions.last().add(line)
        newDefinition = false
    }
}
