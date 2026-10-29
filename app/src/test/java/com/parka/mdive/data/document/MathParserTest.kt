package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MathParserTest {
    private fun parse(source: String) = runBlocking { CommonMarkParser().parse(source).blocks }
    private fun math(source: String) = (parse(source).single() as MarkdownBlock.Paragraph).runs.mapNotNull { it.math }

    @Test fun githubInlineFormsProtectTeXFromMarkdownAndPreserveSource() {
        val source = "앞 \$x_1 * y\$ 와 \$`\\frac{a_1}{b} * c`\$ 뒤"
        val formulas = math(source)
        assertEquals(listOf("x_1 * y", "\\frac{a_1}{b} * c"), formulas.map { it.tex })
        assertEquals(listOf("\$x_1 * y\$", "\$`\\frac{a_1}{b} * c`\$"), formulas.map { it.source })
    }
    @Test fun githubDisplayFormsAndContainerNesting() {
        assertEquals("x^2", (parse("\$\$x^2\$\$").single() as MarkdownBlock.Math).math.tex)
        val multiline = (parse("\$\$\n\\frac{1}{2}\n\$\$").single() as MarkdownBlock.Math).math
        assertEquals("\n\\frac{1}{2}\n", multiline.tex)
        val fence = (parse("```math\r\nx+1\r\n```").single() as MarkdownBlock.Math).math
        assertEquals("x+1\r\n", fence.tex)
        assertEquals("```math\r\nx+1\r\n```", fence.source)
        assertTrue((parse("> \$\$x\$\$").single() as MarkdownBlock.Quote).blocks.single() is MarkdownBlock.Math)
    }
    @Test fun incompleteEmptyEscapesAndCodeStayInert() {
        listOf("\$\$", "\$\$\n\$\$", "\$\$x", "```math\nx", "```math\n```").forEach {
            assertEquals(it, (parse(it).single() as MarkdownBlock.Source).source)
        }
        assertTrue(math("\\\$x\\\$ `\$y\$` \$unterminated").isEmpty())
    }
    @Test fun htmlAndLinkSourceNeverBecomeMathOrCrossDelimiterBoundaries() {
        assertTrue(math("<span>\$x\$</span> [\$y\$](a.md)").isEmpty())
        val formulas = math("To split <span>\$</span>100 in half, calculate \$100/2\$")
        assertEquals(listOf("100/2"), formulas.map { it.tex })
    }
    @Test fun repeatedDollarRunsDoNotPartiallyBecomeInlineMath() {
        assertTrue(math("앞 \$\$x\$\$ 뒤").isEmpty())
        assertTrue(math("\$\$\$x\$\$\$").isEmpty())
        assertEquals("\$\$x\$\$\$", (parse("\$\$x\$\$\$").single() as MarkdownBlock.Source).source)
    }
    @Test fun voidHtmlDoesNotSuppressFollowingMathOrCloseNonvoidContainers() {
        assertEquals(listOf("x"), math("앞<br> \$x\$ 뒤").map { it.tex })
        assertTrue(math("<span>앞</br> \$x\$ 뒤</span>").isEmpty())
        assertTrue(math("<span><em>앞</em> \$x\$ 뒤</span>").isEmpty())
    }
    @Test fun evenBackslashesCloseAndFollowingHeadingSurvives() {
        val blocks = parse("\$\$\nA\\\\\$\$\n\n# after")
        assertTrue(blocks[0] is MarkdownBlock.Math)
        assertTrue(blocks[1] is MarkdownBlock.Heading)
    }
    @Test fun stylesTableAndTaskKeepMathAndNeighbors() {
        val paragraph = parse("**\$x\$** ~~\$y\$~~").single() as MarkdownBlock.Paragraph
        assertTrue(paragraph.runs.any { it.math != null && it.strong })
        assertTrue(paragraph.runs.any { it.math != null && it.strike })
        val table = parse("| H |\n| - |\n| \$x\$ |").single() as MarkdownBlock.Table
        assertEquals("x", table.rows.single().single().single().math?.tex)
        val task = parse("- [x] \$x\$ 뒤").single() as MarkdownBlock.ListBlock
        assertEquals(listOf(true), task.tasks)
        assertEquals("x", (task.items.single().single() as MarkdownBlock.Paragraph).runs.first().math?.tex)
    }
    @Test fun finiteProfileRejectsMacrosResourcesAndPathologicalNesting() {
        assertTrue(MathProfile.accepts("\\frac{\\alpha_1}{\\sqrt{x}}+\\sum_{i=0}^n i"))
        assertTrue(MathProfile.accepts("\\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}"))
        listOf("\\newcommand{\\x}{a}", "\\input{file}", "\\includegraphics{x}", "\\href{url}{x}", "\\color{red}{x}", "\\unknown{x}", "\\begin{array}{*{999999}{c}}x\\end{array}", "\\begin{array} {*{999999}{c}}x\\end{array}", "\\begin{array}\n{*{999999}{c}}x\\end{array}", "\\begin{array}%comment\n{*{999999}{c}}x\\end{array}", "\\alpha ".repeat(257), "a&".repeat(129), "{".repeat(33) + "}".repeat(33), "x".repeat(4097)).forEach { assertFalse(it, MathProfile.accepts(it)) }
    }
}
