package com.parka.mdive.data.document

import org.commonmark.node.CustomNode
import org.commonmark.node.Node
import org.commonmark.node.SourceSpans
import org.commonmark.parser.beta.*
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

internal class StyledInline(val style: Char) : CustomNode()
internal class EmojiInline(val unicode: String, val source: String) : CustomNode()

/** Uses the parser's delimiter/escape machinery instead of mutating source or decoded text. */
internal class InlineStyleDelimiter(private val marker: Char, private val count: Int) : DelimiterProcessor {
    override fun getOpeningCharacter() = marker
    override fun getClosingCharacter() = marker
    override fun getMinLength() = count
    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
        if (openingRun.length() != count || closingRun.length() != count) return 0
        val opener = openingRun.opener
        if (opener.next === closingRun.closer) return 0
        val styled = StyledInline(marker)
        val spans = SourceSpans()
        spans.addAllFrom(openingRun.getOpeners(count))
        var node: Node? = opener.next
        while (node != null && node !== closingRun.closer) {
            val next = node.next
            styled.appendChild(node)
            spans.addAll(node.sourceSpans)
            node = next
        }
        spans.addAllFrom(closingRun.getClosers(count))
        styled.sourceSpans = spans.sourceSpans
        opener.insertAfter(styled)
        return count
    }
}

internal class EmojiInlineFactory : InlineContentParserFactory {
    override fun getTriggerCharacters() = setOf(':')
    override fun create() = InlineContentParser { state ->
        val scanner = state.scanner()
        val start = scanner.position()
        scanner.next()
        val name = StringBuilder()
        while (scanner.hasNext() && name.length <= 64) {
            val c = scanner.peek()
            if (c == ':') {
                scanner.next()
                val emoji = EmojiAliases.values[name.toString()] ?: return@InlineContentParser ParsedInline.none()
                return@InlineContentParser ParsedInline.of(EmojiInline(emoji, scanner.getSource(start, scanner.position()).content), scanner.position())
            }
            if (!(c in 'a'..'z' || c in '0'..'9' || c in "+_-")) break
            name.append(c)
            scanner.next()
        }
        ParsedInline.none()
    }
}

/** Carry outer reference definitions into nested Markdown without appending them to code/source. */
internal class InheritedReferenceLinks(source: String) : org.commonmark.parser.beta.LinkProcessor {
    private val definitions = buildMap<String, org.commonmark.node.LinkReferenceDefinition> {
        val root = org.commonmark.parser.Parser.builder().build().parse(source)
        var node = root.firstChild
        while (node != null) {
            if (node is org.commonmark.node.LinkReferenceDefinition) putIfAbsent(normalize(node.label), node)
            node = node.next
        }
    }
    override fun process(info: LinkInfo, scanner: Scanner, context: org.commonmark.parser.InlineParserContext): LinkResult? {
        if (info.destination() != null) return LinkResult.none()
        val label = info.label()?.takeIf { it.isNotEmpty() } ?: info.text(scanner)
        if (context.getDefinition(org.commonmark.node.LinkReferenceDefinition::class.java, label) != null) return LinkResult.none()
        val definition = definitions[normalize(label)] ?: return LinkResult.none()
        return if (info.marker()?.literal == "!")
            LinkResult.wrapTextIn(org.commonmark.node.Image(definition.destination, definition.title), scanner.position()).includeMarker()
        else LinkResult.wrapTextIn(org.commonmark.node.Link(definition.destination, definition.title), scanner.position())
    }
    private companion object {
        fun normalize(label: String): String = org.commonmark.internal.util.Escaping.normalizeLabelContent(label)
    }
}
