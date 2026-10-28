package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GfmParserTest {
    private fun parse(source: String) = runBlocking { CommonMarkParser().parse(source).blocks }
    private fun List<MarkdownRun>.text() = joinToString("") { it.text }

    @Test fun tableAlignmentMissingAndExtraCellsAndInlineStyles() {
        val table = parse("| 왼쪽 | 중앙 | 오른쪽 |\n| :-- | :-: | --: |\n| **굵게** | ~~삭제~~ | `코드` |\n| 부족 |\n| 1 | 2 | 3 | 초과 |").single() as MarkdownBlock.Table
        assertEquals(listOf(MarkdownAlignment.Left, MarkdownAlignment.Center, MarkdownAlignment.Right), table.columns.map { it.alignment })
        assertEquals(listOf("부족", "", ""), table.rows[1].map { it.text() })
        assertEquals(listOf("1", "2", "3"), table.rows[2].map { it.text() })
        assertTrue(table.rows[0][0].single().strong)
        assertTrue(table.rows[0][1].single().strike)
        assertTrue(table.rows[0][2].single().code)
    }

    @Test fun mismatchedHeaderAndDelimiterStayBasicBodyIncludingNestedContext() {
        for (source in listOf("| A |\n| - | - |\n| 1 | 2 |", "| A | B |\n| - |\n| 1 |")) {
            val paragraph = parse(source).single() as MarkdownBlock.Paragraph
            assertEquals(source, paragraph.runs.text())
        }
        val blocks = (parse("> | A |\n> | - | - |\n> | 1 | 2 |\n>\n> 뒤").single() as MarkdownBlock.Quote).blocks
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertEquals("뒤", (blocks[1] as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun tableEscapedPipesIncludingCodeAndRawLinkOffsets() {
        val table = parse("| A | B |\n| - | - |\n| a\\|b [**링크**](a.md) | `a\\|b` |\n| ![그\\|림](img.png) 뒤 | 끝 |").single() as MarkdownBlock.Table
        assertEquals("a|b [**링크**](a.md)", table.rows[0][0].text())
        assertEquals("a|b", table.rows[0][1].text())
        assertEquals("![그\\|림](img.png) 뒤", table.rows[1][0].text())
    }

    @Test fun tableInsideQuoteAndListKeepsFollowingBodyAndEmptyTableIsValid() {
        val blocks = parse("> | A | B |\n> | - | - |\n> | 1 | 2 |\n\n- 목록\n\n  | H |\n  | - |\n\n뒤 본문")
        assertTrue((blocks[0] as MarkdownBlock.Quote).blocks.single() is MarkdownBlock.Table)
        val nested = (blocks[1] as MarkdownBlock.ListBlock).items.single()[1] as MarkdownBlock.Table
        assertTrue(nested.rows.isEmpty())
        assertEquals("뒤 본문", (blocks[2] as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun tasksAreReadOnlyStatesForOrderedUnorderedAndNestedItems() {
        val list = parse("3. [x] **완료**\n   - [ ] 미완료\n   - [X] 대문자\n4. 일반\n5. [ ] 마지막").single() as MarkdownBlock.ListBlock
        assertEquals(3, list.start)
        assertEquals(listOf(true, null, false), list.tasks)
        val first = list.items[0][0] as MarkdownBlock.Paragraph
        assertEquals("완료", first.runs.text())
        assertTrue(first.runs.single().strong)
        assertFalse(first.runs.single().strike)
        assertEquals(listOf(false, true), (list.items[0][1] as MarkdownBlock.ListBlock).tasks)
    }

    @Test fun taskSourceRecognitionRejectsEscapesEntitiesWrongPositionAndIncompleteMarkers() {
        val list = parse("- \\[x] escape\n- &#91;x] entity\n- [x]no-space\n- 앞 [x] middle\n- [y] wrong\n- [x\n- [x]\n- `[x]` code").single() as MarkdownBlock.ListBlock
        assertTrue(list.tasks.all { it == null })
        assertEquals("[x] escape", (list.items[0].single() as MarkdownBlock.Paragraph).runs.text())
        assertEquals("[x] entity", (list.items[1].single() as MarkdownBlock.Paragraph).runs.text())
        assertEquals("[x]no-space", (list.items[2].single() as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun taskOnlyFirstParagraphAndWhitespaceContinuation() {
        val list = parse("- 첫 문단\n\n  [x] 둘째 문단\n- [ ]\n  다음 줄\n- [X]\t탭").single() as MarkdownBlock.ListBlock
        assertEquals(listOf(null, false, true), list.tasks)
        assertEquals("[x] 둘째 문단", (list.items[0][1] as MarkdownBlock.Paragraph).runs.text())
        assertEquals("다음 줄", (list.items[1].single() as MarkdownBlock.Paragraph).runs.text())
        assertEquals("탭", (list.items[2].single() as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun doubleTildeNestingAndUnsupportedDelimiters() {
        val paragraph = parse("~~**굵게** *강조*~~ ~단일~ ~~~세개~~~ ~~미완성\n\n뒤")[0] as MarkdownBlock.Paragraph
        assertTrue(paragraph.runs.any { it.text == "굵게" && it.strong && it.strike })
        assertTrue(paragraph.runs.any { it.text == "강조" && it.emphasis && it.strike })
        val plain = paragraph.runs.filterNot { it.strike }.text()
        assertTrue(plain.contains("~단일~ ~~~세개~~~ ~~미완성"))
        assertEquals("~~불일치~", (parse("~~불일치~").single() as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun codeAndInertLinkImageHtmlAndFutureSyntaxPreserveSource() {
        val source = "`~~코드~~` [~~링크~~](a.md) ![~~이미지~~](a.png)\n\n```\n- [x] task\n| A |\n| - |\n~~source~~\n```\n\n<div>~~HTML~~</div>\n\n\$수식\$"
        val blocks = parse(source)
        val runs = (blocks[0] as MarkdownBlock.Paragraph).runs
        assertEquals("~~코드~~ [~~링크~~](a.md) ![~~이미지~~](a.png)", runs.text())
        assertTrue(runs.none { it.strike })
        assertEquals("```\n- [x] task\n| A |\n| - |\n~~source~~\n```", (blocks[1] as MarkdownBlock.Source).source)
        assertEquals("<div>~~HTML~~</div>", (blocks[2] as MarkdownBlock.Source).source)
        assertEquals("\$수식\$", (blocks[3] as MarkdownBlock.Paragraph).runs.text())
    }
}
