package com.parka.mdive.domain.document

/** App-owned reading model. No parser or presentation-library types cross this boundary. */
data class MarkdownDocument(val blocks: List<MarkdownBlock>)

data class MarkdownRun(
    val text: String,
    val emphasis: Boolean = false,
    val strong: Boolean = false,
    val code: Boolean = false,
    val strike: Boolean = false,
)

sealed interface MarkdownBlock {
    data class Paragraph(val runs: List<MarkdownRun>) : MarkdownBlock
    data class Heading(val level: Int, val runs: List<MarkdownRun>) : MarkdownBlock
    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock
    data class ListBlock(
        val start: Int?, val items: List<List<MarkdownBlock>>,
        /** null is a regular list item; false/true are inert task states. */
        val tasks: List<Boolean?> = List(items.size) { null },
    ) : MarkdownBlock
    data class Table(val columns: List<MarkdownTableColumn>, val rows: List<List<List<MarkdownRun>>>) : MarkdownBlock
    data class Code(val source: String, val language: String?, val highlights: List<CodeHighlight> = emptyList()) : MarkdownBlock
    data object Divider : MarkdownBlock
    /** Unsupported syntax remains readable, inert source in the document flow. */
    data class Source(val source: String) : MarkdownBlock
}

enum class MarkdownAlignment { Left, Center, Right }
data class MarkdownTableColumn(val header: List<MarkdownRun>, val alignment: MarkdownAlignment)

interface MarkdownParser {
    suspend fun parse(source: String): MarkdownDocument
}

/** UTF-16 offsets into the unchanged code body; no engine or UI types. */
data class CodeHighlight(val start: Int, val end: Int, val tone: CodeTone)
enum class CodeTone { Keyword, String, Plain }
