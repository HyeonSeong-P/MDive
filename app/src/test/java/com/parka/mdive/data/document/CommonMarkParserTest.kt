package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CommonMarkParserTest {
    private fun parse(source: String) = runBlocking { CommonMarkParser().parse(source).blocks }

    @Test fun basicBlocksAndNestedInlineStyles() {
        val blocks = parse("# 제목\n\n문단 ***둘 다*** ` a  b ` &amp; \\*별\\*\n줄  \n다음\n\n> 인용\n>\n> - 안쪽\n\n3. 셋\n4. 넷\n\n---")
        assertEquals(MarkdownBlock.Heading(1, listOf(MarkdownRun("제목"))), blocks[0])
        val runs = (blocks[1] as MarkdownBlock.Paragraph).runs
        assertTrue(runs.contains(MarkdownRun("둘 다", emphasis = true, strong = true)))
        assertTrue(runs.contains(MarkdownRun("a  b", code = true)))
        assertEquals("문단 둘 다 a  b & *별*\n줄\n다음", runs.joinToString("") { it.text })
        assertTrue((blocks[2] as MarkdownBlock.Quote).blocks[1] is MarkdownBlock.ListBlock)
        val list = blocks[3] as MarkdownBlock.ListBlock
        assertEquals(3, list.start)
        assertEquals(2, list.items.size)
        assertEquals(MarkdownBlock.Divider, blocks[4])
        assertEquals(1, (parse("제목\n===").single() as MarkdownBlock.Heading).level)
    }

    @Test fun unsupportedSourceKeepsDestinationsMarkupAndUnicodeOffsets() {
        val source = "😀 [**이름**](https://example.com \"제목\") ![그림](a.png) <https://example.org>\r\n다음\n\n```kotlin\r\n a < b\r\n```\n\n<div>\r\n **실행 안 함**\r\n</div>\n\n끝"
        val blocks = parse(source)
        val text = (blocks[0] as MarkdownBlock.Paragraph).runs.joinToString("") { it.text }
        assertEquals("😀 이름 ![그림](a.png) https://example.org\n다음", text)
        assertEquals(" a < b\r\n", (blocks[1] as MarkdownBlock.Code).source)
        assertEquals("<div>\r\n **실행 안 함**\r\n</div>", (blocks[2] as MarkdownBlock.Source).source)
        assertEquals("끝", (blocks[3] as MarkdownBlock.Paragraph).runs.single().text)
    }

    @Test fun incompleteAndUnrecognizedExtensionsRemainText() {
        val blocks = parse("**닫히지 않음\n\n~~취소선~~\n\n| A | B |\n| - | - |\n| 1 | 2 |\n\n- [x] task\n\n\$수식\$\n\n[^foot]: 원문")
        assertEquals("**닫히지 않음", (blocks[0] as MarkdownBlock.Paragraph).runs.single().text)
        assertEquals(MarkdownRun("취소선", strike = true), (blocks[1] as MarkdownBlock.Paragraph).runs.single())
        assertTrue(blocks[2] is MarkdownBlock.Table)
        val task = ((blocks[3] as MarkdownBlock.ListBlock).items.single().single() as MarkdownBlock.Paragraph)
        assertEquals("task", task.runs.joinToString("") { it.text })
        assertEquals(listOf(true), (blocks[3] as MarkdownBlock.ListBlock).tasks)
        assertEquals("\$수식\$", (blocks[4] as MarkdownBlock.Paragraph).runs.single().text)
        assertEquals("[^foot]: 원문", (blocks[5] as MarkdownBlock.Source).source)
    }

    @Test fun multilineUnsupportedLinkWithinQuotePreservesSourceAndSurroundings() {
        val blocks = parse("> 앞 [링크\r\n> 다음](a.md) 뒤\n\n[ref]: a.md\n\n[참조][ref]")
        val quote = (blocks[0] as MarkdownBlock.Quote).blocks.single() as MarkdownBlock.Paragraph
        assertEquals("앞 링크\n다음 뒤", quote.runs.joinToString("") { it.text })
        assertEquals("a.md", quote.runs.first { it.text == "링크" }.link)
        assertEquals(MarkdownRun("참조", link = "a.md"), (blocks[1] as MarkdownBlock.Paragraph).runs.single())
    }
    @Test fun referenceDeclarationsAreHiddenWhileIncompleteAndFootnoteSourceRemainReadable() {
        assertEquals(emptyList<MarkdownBlock>(), parse(" \r\n\t"))
        assertEquals(emptyList<MarkdownBlock>(), parse("[a]: /a\n[b]: /b"))
        assertEquals(listOf(MarkdownBlock.Source("[^foot]: 원문")), parse("[^foot]: 원문"))
        val quoted = (parse("> [a]: /a\n>\n> 본문").single() as MarkdownBlock.Quote).blocks
        assertEquals(listOf(MarkdownBlock.Paragraph(listOf(MarkdownRun("본문")))), quoted)
        val mixed = parse("[a]: /a \"열리지 않음\n\n뒤 본문")
        assertEquals("뒤 본문", (mixed.last() as MarkdownBlock.Paragraph).runs.single().text)
    }

    @Test fun parserNestingLimitsKeepPathologicalInputReadable() {
        val source = "> ".repeat(200) + "깊은 본문"
        fun text(block: MarkdownBlock): String = when (block) {
            is MarkdownBlock.Paragraph -> block.runs.joinToString("") { it.text }
            is MarkdownBlock.Quote -> block.blocks.joinToString("") { text(it) }
            is MarkdownBlock.ListBlock -> block.items.flatten().joinToString("") { text(it) }
            is MarkdownBlock.Source -> block.source
            else -> ""
        }
        assertTrue(parse(source).joinToString("") { text(it) }.contains("깊은 본문"))
        val extension = (parse("~~**굵게**~~").single() as MarkdownBlock.Paragraph).runs
        assertEquals("굵게", extension.joinToString("") { it.text })
        assertTrue(extension.all { it.strike })
        assertTrue(extension.any { it.text == "굵게" && it.strong })
    }

}
