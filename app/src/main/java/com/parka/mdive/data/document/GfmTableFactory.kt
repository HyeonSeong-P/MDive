package com.parka.mdive.data.document

import org.commonmark.ext.gfm.tables.internal.TableBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

/** Compatibility gate for the pinned 0.31.0 parser, whose factory accepts extra delimiter columns. */
internal class GfmTableFactory : AbstractBlockParserFactory() {
    private val delegate = TableBlockParser.Factory(1_000_000)
    override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
        val header = matchedBlockParser.paragraphLines.lines.lastOrNull()?.content ?: return BlockStart.none()
        val delimiter = state.line.content.subSequence(state.index, state.line.content.length)
        if (cellCount(header) != cellCount(delimiter)) return BlockStart.none()
        return delegate.tryStart(state, matchedBlockParser)
    }

    private fun cellCount(value: CharSequence): Int {
        val text = value.toString().trim(' ', '\t')
        if (text.isEmpty() || text == "|") return 0
        var count = 1
        var index = if (text.startsWith('|')) 1 else 0
        while (index < text.length) {
            when {
                text[index] == '\\' && index + 1 < text.length && text[index + 1] == '|' -> index += 2
                text[index] == '|' -> { if (index != text.lastIndex) count++; index++ }
                else -> index++
            }
        }
        return count
    }
}
