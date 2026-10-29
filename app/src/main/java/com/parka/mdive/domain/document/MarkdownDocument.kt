package com.parka.mdive.domain.document

/** App-owned reading model. No parser or presentation-library types cross this boundary. */
data class MarkdownDocument(val blocks: List<MarkdownBlock>)

data class MarkdownRun(
    val text: String,
    val emphasis: Boolean = false,
    val strong: Boolean = false,
    val code: Boolean = false,
    val strike: Boolean = false,
    val math: MarkdownMath? = null,
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
    data class Math(val math: MarkdownMath) : MarkdownBlock
    data class Mermaid(val diagram: MarkdownMermaid) : MarkdownBlock
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

/** Prepared monochrome coverage at [emPixels] pixels/em; engine/platform types stay in data. */
class MathRaster(val width: Int, val height: Int, val depth: Int, pixels: IntArray, val emPixels: Int = 64) {
    private val pixels = pixels.copyOf()
    fun copyPixels(): IntArray = pixels.copyOf()
}
data class MarkdownMath(val tex: String, val source: String, val display: Boolean, val raster: MathRaster? = null)

/** Inert app-owned diagram input. Platform rendering never enters the domain contract. */
data class MarkdownMermaid(val code: String, val source: String, val supported: Boolean)
