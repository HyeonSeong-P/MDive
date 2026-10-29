package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MermaidParserTest {
    private fun parse(source: String) = runBlocking { CommonMarkParser().parse(source) }
    private fun diagram(code: String, close: Boolean = true): MarkdownMermaid =
        (parse("```mermaid\n$code" + if (close) "\n```" else "").blocks.single() as MarkdownBlock.Mermaid).diagram

    @Test fun supportedFamiliesKeepExactSourceAndCodeWhileOrdinaryCodeStaysCode() {
        for (code in listOf("flowchart TD\nA-->B", "graph LR; A-->B", "sequenceDiagram\nA->>B: hello", "classDiagram\nA -- B", "stateDiagram-v2\n[*] --> Ready", "erDiagram\nA ||--o{ B : has")) {
            val block = diagram(code)
            assertTrue(code, block.supported)
            assertEquals("$code\n", block.code)
            assertEquals("```mermaid\n$code\n```", block.source)
        }
        val raw = "```MeRmAiD extra\r\nflowchart TD\r\n A-->B\r\n```"
        assertEquals(raw, (parse(raw).blocks.single() as MarkdownBlock.Mermaid).diagram.source)
        assertTrue(parse("```kotlin\nval x = 1\n```").blocks.single() is MarkdownBlock.Code)
    }

    @Test fun unsupportedEmptyUnclosedAndOversizeAreLocalFailuresWithFullSource() {
        for (code in listOf("pie\n\"A\": 1", "", "flowchart TD\n" + "A".repeat(50_000))) {
            assertFalse(diagram(code).supported)
            assertTrue(diagram(code).source.startsWith("```mermaid"))
        }
        assertFalse(diagram("flowchart TD\nA-->B", false).supported)
        val blocks = parse("앞\n\n```mermaid\npie\n```\n\n뒤").blocks
        assertTrue(blocks.first() is MarkdownBlock.Paragraph)
        assertFalse((blocks[1] as MarkdownBlock.Mermaid).diagram.supported)
        assertTrue(blocks.last() is MarkdownBlock.Paragraph)
    }

    @Test fun documentSettingsHtmlLinksCallbacksAndExternalResourcesNeverReachEngine() {
        for (code in listOf(
            "%%{init: {securityLevel: 'loose'}}%%\nflowchart TD\nA-->B",
            "---\nconfig: {}\n---\nflowchart TD\nA-->B",
            "flowchart TD; A-->B; click A callback",
            "flowchart TD\nA[<b>HTML</b>]",
            "flowchart TD\nA[<</script>>]",
            "flowchart TD\nA[<<<script>>>]",
            "flowchart TD\nA[<<script>>]",
            "flowchart TD\nA[&lt;img&gt;]",
            "flowchart TD\nA@{ img: 'https://example.com/a.png' }",
            "flowchart TD\nclassDef danger fill:url(https://example.com)",
            "sequenceDiagram\nlink A: Docs @ https://example.com",
        )) assertFalse(code, diagram(code).supported)
        assertTrue(diagram("%% ordinary comment\nflowchart TD\nA-->B").supported)
        assertTrue(diagram("stateDiagram-v2\nstate Fork <<fork>>\n[*] --> Fork\nFork --> Ready").supported)
        assertTrue(diagram("classDiagram\nclass Animal {\n<<interface>>\n+int age\n}\nAnimal <|-- Duck").supported)
    }

    @Test fun diagramsInsideQuotesAndListsKeepStructuralSourceAndMixedMath() {
        val blocks = parse("> ```mermaid\n> flowchart TD\n> A-->B\n> ```\n\n- ```mermaid\n  sequenceDiagram\n  A->>B: hi\n  ```\n\n\$x\$").blocks
        val quote = (blocks[0] as MarkdownBlock.Quote).blocks.single() as MarkdownBlock.Mermaid
        assertTrue(quote.diagram.supported)
        assertTrue(quote.diagram.source.contains("> A-->B"))
        assertTrue(((blocks[1] as MarkdownBlock.ListBlock).items.single().single() as MarkdownBlock.Mermaid).diagram.supported)
        assertNotNull((blocks[2] as MarkdownBlock.Paragraph).runs.single().math)
    }
}
