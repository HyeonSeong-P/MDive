package com.parka.mdive.domain.document

/** App-owned reading model. No parser or presentation-library types cross this boundary. */
data class MarkdownDocument(val blocks: List<MarkdownBlock>)

data class MarkdownRun(
    val text: String,
    val emphasis: Boolean = false,
    val strong: Boolean = false,
    val code: Boolean = false,
)

sealed interface MarkdownBlock {
    data class Paragraph(val runs: List<MarkdownRun>) : MarkdownBlock
    data class Heading(val level: Int, val runs: List<MarkdownRun>) : MarkdownBlock
    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock
    data class ListBlock(val start: Int?, val items: List<List<MarkdownBlock>>) : MarkdownBlock
    data object Divider : MarkdownBlock
    /** Unsupported syntax remains readable, inert source in the document flow. */
    data class Source(val source: String) : MarkdownBlock
}

interface MarkdownParser {
    suspend fun parse(source: String): MarkdownDocument
}
