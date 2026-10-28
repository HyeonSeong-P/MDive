package com.parka.mdive.data.document

import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node

/** Restores original line separators/EOF; AST literals normalize both. */
internal class CodeSourceReader(private val source: String) {
    private data class Line(val end: Int, val next: Int)
    private val lines = buildList {
        var start = 0
        var index = 0
        while (index < source.length) {
            if (source[index] == '\r' || source[index] == '\n') {
                val end = index
                if (source[index] == '\r' && source.getOrNull(index + 1) == '\n') index++
                add(Line(end, index + 1))
                start = index + 1
            }
            index++
        }
        if (start < source.length) add(Line(source.length, source.length))
    }

    fun read(node: Node): String {
        val spans = node.sourceSpans
        if (spans.isEmpty()) return ""
        val byLine = spans.associateBy { it.lineIndex }
        val fenced = node as? FencedCodeBlock
        val first = spans.first().lineIndex + if (fenced == null) 0 else 1
        val literal = when (node) {
            is FencedCodeBlock -> node.literal
            is IndentedCodeBlock -> node.literal
            else -> error("Expected code block")
        }
        // The parser knows container/fence/tab-column consumption. Use only its leading
        // whitespace, while keeping original content and CR/LF/EOF from the source spans.
        // In particular a tab partly consumed by a quote/list must become residual spaces.
        val semanticLines = literal.split('\n').dropLast(1)
        return semanticLines.mapIndexed { offset, semantic ->
            val lineIndex = first + offset
            val line = lines[lineIndex]
            val span = byLine[lineIndex]
            val raw = span?.let { source.substring(it.inputIndex, it.inputIndex + it.length) }.orEmpty()
            val prefix = semantic.takeWhile { it == ' ' || it == '\t' }
            prefix + raw.dropWhile { it == ' ' || it == '\t' } + source.substring(line.end, line.next)
        }.joinToString("")
    }
}
