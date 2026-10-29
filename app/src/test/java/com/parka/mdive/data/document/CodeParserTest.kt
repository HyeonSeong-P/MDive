package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CodeParserTest {
    private fun parse(source: String) = runBlocking { CommonMarkParser().parse(source).blocks }
    private fun code(source: String) = parse(source).single() as MarkdownBlock.Code

    @Test fun originalWhitespaceLineEndingsAndEofSurvive() {
        val body = "\tval x = \"🌊\"  \r\n\r\n \t\r\n"
        assertEquals(body, code("```Kt title=ignored\r\n$body```\r\n").source)
        assertEquals("Kt", code("```Kt title=ignored\nval x = 1\n```").language)
        assertEquals("abc", code("```\nabc").source)
        assertEquals("abc\r\n\r\n", code("```\r\nabc\r\n\r\n").source)
        assertEquals("", code("```\n```").source)
        assertEquals("", code("```").source)
        assertEquals("a\rb\r", code("~~~\ra\rb\r~~~").source)
    }

    @Test fun markdownContainerAndFenceIndentAreRemovedButCodeIndentStays() {
        val quote = parse(">   ```kotlin\r\n>     val x = 1\r\n>   \r\n>   ```").single() as MarkdownBlock.Quote
        assertEquals("  val x = 1\r\n\r\n", (quote.blocks.single() as MarkdownBlock.Code).source)
        val list = parse("- item\n\n  ```\n  \tinside\n  ```").single() as MarkdownBlock.ListBlock
        assertEquals("\tinside\n", (list.items.single().last() as MarkdownBlock.Code).source)
        assertEquals("\tinside\r\n\r\n  after", code("    \tinside\r\n\r\n      after").source)
        assertEquals("inside\n", code("\tinside\n").source)
        val tabQuote = parse(">\t```\r\n>\tfoo\r\n>\t```\r\n").single() as MarkdownBlock.Quote
        assertEquals("  foo\r\n", (tabQuote.blocks.single() as MarkdownBlock.Code).source)
        val tabIndented = parse(">\t    foo\n").single() as MarkdownBlock.Quote
        assertEquals("  foo\n", (tabIndented.blocks.single() as MarkdownBlock.Code).source)
        val tabList = parse("-\t```\n\tfoo\n\t```\n").single() as MarkdownBlock.ListBlock
        assertEquals("foo\n", (tabList.items.single().single() as MarkdownBlock.Code).source)
    }

    @Test fun unsupportedLanguageAndMermaidStayInertAndSurroundingBodyRemains() {
        for (language in listOf("json", "yaml", "sql", "html", "xml", "css", "unknown", "")) {
            val block = code("```$language\n  val x = \"value\"\n```")
            assertEquals("  val x = \"value\"\n", block.source)
            assertTrue(block.highlights.isEmpty())
        }
        val source = "```mermaid\ngraph TD; A-->B\n```"
        assertEquals(source, (parse(source).single() as MarkdownBlock.Mermaid).diagram.source)
        assertTrue(parse("앞\n\n```invalid\na\n```\n\n뒤").last() is MarkdownBlock.Paragraph)
    }

    @Test fun allSeventeenLanguageGroupsAndAliasesProduceBoundedHighlights() {
        val examples = mapOf(
            "c" to "return 1;", "cpp" to "return 1;", "dart" to "return 1;", "java" to "return 1;",
            "kotlin" to "val x = 1", "rust" to "let x = 1;", "csharp" to "return 1;", "coffeescript" to "return 1",
            "javascript" to "return 1;", "perl" to "return 1;", "python" to "return 1", "ruby" to "return 1",
            "shell" to "if true; then echo hi; fi", "swift" to "return 1", "typescript" to "return 1;",
            "go" to "return 1", "php" to "return 1;",
        )
        for ((language, body) in examples) {
            val block = code("```$language\n$body\n```")
            assertTrue("$language missing keyword", block.highlights.any { it.tone == CodeTone.Keyword })
            assertTrue(block.highlights.all { it.start >= 0 && it.end <= block.source.length })
        }
        for (alias in listOf("kt", "kts", "KT", "c++", "cxx", "c#", "cs", "coffee", "js", "pl", "py", "rb", "bash", "sh", "zsh", "ts", "golang")) {
            assertFalse(alias, code("```$alias ignored\nreturn if val let\n```").highlights.isEmpty())
        }
    }

    @Test fun unicodeStringsMalformedInputAndEngineFailureNeverChangeSource() {
        val body = "val wave = \"🌊\"\n/* unfinished\n"
        val block = code("```kotlin\n$body```")
        assertEquals(body, block.source)
        assertTrue(block.highlights.any { it.tone == CodeTone.String && block.source.substring(it.start, it.end).contains("🌊") })
        assertTrue(safelyHighlight(body) { error("engine failed") }.isEmpty())
        assertTrue(safelyHighlight(body) { listOf(CodeHighlight(-1, 3, CodeTone.Keyword)) }.isEmpty())
        try { safelyHighlight(body) { throw CancellationException() }; fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
    }
}
