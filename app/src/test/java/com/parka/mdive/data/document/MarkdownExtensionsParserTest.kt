package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MarkdownExtensionsParserTest {
    private fun parse(source: String) = runBlocking { CommonMarkParser().parse(source).blocks }
    private fun runs(source: String) = (parse(source).single() as MarkdownBlock.Paragraph).runs
    private fun List<MarkdownRun>.text() = joinToString("") { it.text }

    @Test fun githubInlineAndAdditionalStylesNestWithoutStealingBoldOrStrike() {
        val text = runs("__bold__ ~one~ ~~two~~ <sub>H**2**</sub> <sup>up</sup> <ins>new</ins> <u>line</u> ==**mark**== ++insert++ ^high^")
        assertTrue(text.any { it.text == "bold" && it.strong && !it.underline })
        assertTrue(text.any { it.text == "one" && it.strike && !it.subscript })
        assertTrue(text.any { it.text == "two" && it.strike })
        assertTrue(text.any { it.text == "2" && it.subscript && it.strong })
        assertTrue(text.any { it.text == "up" && it.superscript })
        assertTrue(text.any { it.text == "new" && it.inserted && it.underline })
        assertTrue(text.any { it.text == "line" && it.underline })
        assertTrue(text.any { it.text == "mark" && it.highlight && it.strong })
        assertTrue(text.any { it.text == "insert" && it.inserted })
        assertTrue(text.any { it.text == "high" && it.superscript })
    }

    @Test fun emojiAliasesEscapeAndTypographyPreserveGithubMeaning() {
        val text = runs(":smile: :+1: :thumbsup: :unknown_name: \\:smile: ... -- --- (c) \"quotes\"")
        assertEquals("😄 👍 👍 :unknown_name: :smile: ... -- --- (c) \"quotes\"", text.text())
        assertEquals(":smile: ==mark==", runs("`:smile: ==mark==`").single().text)
        assertTrue(runs("`:smile: ==mark==`").single().code)
        val link = runs("[==label== :smile:](https://example.com/==raw==/:smile:) ")
        assertTrue(link.any { it.text == "label" && it.highlight })
        assertTrue(link.all { it.link == "https://example.com/==raw==/:smile:" })
        assertEquals(":smile: ++raw++", runs("\$ :smile: ++raw++ \$").single().math!!.tex.trim())
    }

    @Test fun imageAltKeepsEmojiAndStyledTextAccessible() {
        val run = runs("![==label== :smile: **bold**](a.png)").single()
        assertEquals("label 😄 bold", run.image!!.alt)
        val linked = runs("[![^^label^^ :thumbsup:](a.png)](target.md)").single()
        assertEquals("^^label^^ 👍", linked.image!!.alt)
        assertEquals("target.md", linked.link)
    }

    @Test fun malformedAndEscapedStylesRemainReadable() {
        val source = "\\==escape== ++open <sub>unclosed ^unfinished ===three=== +++three+++"
        val text = runs(source)
        assertEquals("==escape== ++open <sub>unclosed ^unfinished ===three=== +++three+++", text.text())
        assertTrue(text.none { it.highlight || it.inserted || it.subscript || it.superscript })
        assertEquals("<sub class='x'>text</sub>", runs("<sub class='x'>text</sub>").text())
    }

    @Test fun definitionTermsMultipleMeaningsAndNestedBlocks() {
        val blocks = parse("Term A\nTerm **B**\n: first :smile:\n\n    second paragraph\n\n    - nested\n: other definition\n\nAfter")
        val entry = (blocks[0] as MarkdownBlock.DefinitionList).entries.single()
        assertEquals(2, entry.terms.size)
        assertTrue(entry.terms[1].any { it.strong })
        assertEquals(2, entry.definitions.size)
        assertEquals("first 😄", (entry.definitions[0][0] as MarkdownBlock.Paragraph).runs.text())
        assertEquals("second paragraph", (entry.definitions[0][1] as MarkdownBlock.Paragraph).runs.text())
        assertTrue(entry.definitions[0][2] is MarkdownBlock.ListBlock)
        assertEquals("After", (blocks.last() as MarkdownBlock.Paragraph).runs.text())
        assertTrue(parse("# Heading\n: not definition")[0] is MarkdownBlock.Heading)
        assertTrue(parse("Term\n:missing space").single() is MarkdownBlock.Paragraph)
    }

    @Test fun calloutsAndGithubAlertsRemainSeparateAndDoNotAllowNestedGithubAlerts() {
        val normal = parse("::: caution Custom title\nbody\n\n::: note\nnested\n:::\n:::\n\nAfter")
        val callout = normal[0] as MarkdownBlock.Callout
        assertEquals(MarkdownCalloutKind.Caution, callout.kind)
        assertEquals("Custom title", callout.title)
        assertTrue(callout.blocks[1] is MarkdownBlock.Callout)
        MarkdownCalloutKind.entries.forEach { kind ->
            val alert = parse("> [!${kind.name.uppercase()}]\n> **body**").single() as MarkdownBlock.Callout
            assertEquals(kind, alert.kind)
            assertEquals("body", (alert.blocks.single() as MarkdownBlock.Paragraph).runs.text())
        }
        assertTrue(parse("> [!NOTE] Custom\n> body").single() is MarkdownBlock.Quote)
        assertTrue(parse("> \\[!NOTE]\n> body").single() is MarkdownBlock.Quote)
        assertTrue(parse("> &#91;!NOTE]\n> body").single() is MarkdownBlock.Quote)
        val nested = parse("> > [!NOTE]\n> > body").single() as MarkdownBlock.Quote
        assertTrue(nested.blocks.single() is MarkdownBlock.Quote)
        assertTrue(parse("::: unknown\nbody\n:::").first() is MarkdownBlock.Paragraph)
        assertTrue(parse("::: note\nnot closed").single() is MarkdownBlock.Source)
    }

    @Test fun detailsNestedStableIdsSummaryDefaultsAndMalformedPreservation() {
        val source = "<details open>\n<summary>**Title**</summary>\n\nbody\n\n<details>\n<summary></summary>\n\n![hidden](a.png)\n</details>\n</details>\n\nAfter"
        val first = parse(source)
        val outer = first[0] as MarkdownBlock.Details
        assertTrue(outer.initiallyOpen)
        assertTrue(outer.summary.single().strong)
        val inner = outer.blocks[1] as MarkdownBlock.Details
        assertFalse(inner.initiallyOpen)
        assertEquals("자세히 보기", inner.summary.text())
        assertNotEquals(outer.id, inner.id)
        assertEquals(outer.id, (parse(source)[0] as MarkdownBlock.Details).id)
        assertEquals("After", (first.last() as MarkdownBlock.Paragraph).runs.text())
        assertEquals("자세히 보기", (parse("<details>\n</details>").single() as MarkdownBlock.Details).summary.text())
        assertTrue(parse("<details>\n<summary>x</summary>\n\nunfinished").single() is MarkdownBlock.Source)
    }


    @Test fun compactDetailsAndReferencesInContainerBodiesUseDocumentDefinitions() {
        val source = "<details open=\"open\"><summary>Title</summary>[label][ref]</details>\n\n[ref]: https://example.com/:smile:"
        val details = parse(source).single() as MarkdownBlock.Details
        assertTrue(details.initiallyOpen)
        assertEquals("Title", details.summary.text())
        assertEquals("https://example.com/:smile:", (details.blocks.single() as MarkdownBlock.Paragraph).runs.single().link)
        val callout = parse("::: note\n[label][ref]\n:::\n\n[ref]: target.md").single() as MarkdownBlock.Callout
        assertEquals("target.md", (callout.blocks.single() as MarkdownBlock.Paragraph).runs.single().link)
        val definition = parse("Term\n: [label][ref]\n\n[ref]: target.md").single() as MarkdownBlock.DefinitionList
        assertEquals("target.md", (definition.entries.single().definitions.single().single() as MarkdownBlock.Paragraph).runs.single().link)
    }

    @Test fun nestedCodePreservesCrLfAndBlankLinesForCopy() {
        val details = parse("<details>\r\n<summary>Code</summary>\r\n\r\n```kotlin\r\n a\t \r\n\r\n```\r\n</details>").single() as MarkdownBlock.Details
        assertEquals(" a\t \r\n\r\n", (details.blocks.single() as MarkdownBlock.Code).source)
        val definitions = parse("Term\r\n: def\r\n\r\n    ```kotlin\r\n     a\t \r\n\r\n    ```").single() as MarkdownBlock.DefinitionList
        assertEquals(" a\t \r\n\r\n", (definitions.entries.single().definitions.single()[1] as MarkdownBlock.Code).source)
    }

    @Test fun spacedClosingTagsAndIndentedCodeNeverFailWholeDocument() {
        val spaced = parse("<details>\n<summary>x</summary>\n\nbody\n</details >\n\nAfter")
        assertTrue(spaced[0] is MarkdownBlock.Details)
        assertEquals("After", (spaced[1] as MarkdownBlock.Paragraph).runs.text())
        val indented = parse("<details>\n<summary>x</summary>\n\n    </details>\n\nbody\n</details>").single() as MarkdownBlock.Details
        assertTrue(indented.blocks[0] is MarkdownBlock.Code)
        assertEquals("body", (indented.blocks[1] as MarkdownBlock.Paragraph).runs.text())
        val callout = parse("::: note\n\n    :::\n\nbody\n:::").single() as MarkdownBlock.Callout
        assertTrue(callout.blocks[0] is MarkdownBlock.Code)
        assertEquals("body", (callout.blocks[1] as MarkdownBlock.Paragraph).runs.text())
        val listFence = parse("<details>\n<summary>x</summary>\n\n- ```html\n  </details>\n  ```\n\nbody\n</details>").single() as MarkdownBlock.Details
        assertTrue(listFence.blocks[0] is MarkdownBlock.ListBlock)
    }

    @Test fun summarylessDetailsAndDefinitionTrailingCodeWhitespaceAreNotTrimmed() {
        val details = parse("<details>\n\n    indented\n</details>").single() as MarkdownBlock.Details
        assertEquals("indented\n", (details.blocks.single() as MarkdownBlock.Code).source)
        val callout = parse("::: note\n\n    a\n:::").single() as MarkdownBlock.Callout
        assertEquals("a\n", (callout.blocks.single() as MarkdownBlock.Code).source)
        val definition = parse("Term\n: body\n\n    ~~~\n    exact  ").single() as MarkdownBlock.DefinitionList
        assertEquals("exact  ", (definition.entries.single().definitions.single()[1] as MarkdownBlock.Code).source)
        val refs = parse("Term\n: [a]\n\n    ~~~\n    exact  \n\n[a]: /target").first() as MarkdownBlock.DefinitionList
        val code = refs.entries.single().definitions.single()[1] as MarkdownBlock.Code
        assertTrue(code.source.startsWith("exact  "))
        assertFalse(code.source.contains("[a]: /target"))
    }

    @Test fun singleCrNestedCodeAndLeadingColonDefinitionsRemainExact() {
        val details = parse("<details>\r<summary>x</summary>\r\r~~~\ra\rb\r~~~\r</details>").single() as MarkdownBlock.Details
        assertEquals("a\rb\r", (details.blocks.single() as MarkdownBlock.Code).source)
        val definition = parse("Term\n: :smile:\n: :: literal").single() as MarkdownBlock.DefinitionList
        assertEquals("😄", (definition.entries.single().definitions[0].single() as MarkdownBlock.Paragraph).runs.text())
        assertEquals(":: literal", (definition.entries.single().definitions[1].single() as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun detailsSummaryAllowsCommonMarkHtmlIndentation() {
        for (indent in 1..3) {
            val details = parse("<details>\n" + " ".repeat(indent) + "<summary>Title</summary>\n\nbody\n</details>").single() as MarkdownBlock.Details
            assertEquals("Title", details.summary.text())
            assertEquals("body", (details.blocks.single() as MarkdownBlock.Paragraph).runs.text())
        }
    }

    @Test fun detailsInterruptsParagraphLikeCommonMarkHtmlBlock() {
        val blocks = parse("Before\n<details>\n<summary>X</summary>\n\nbody\n</details>\n\nAfter")
        assertEquals("Before", (blocks[0] as MarkdownBlock.Paragraph).runs.text())
        assertTrue(blocks[1] is MarkdownBlock.Details)
        assertEquals("After", (blocks[2] as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun invalidBacktickFenceInfoCannotConsumeContainerClosingOrFollowingBody() {
        val details = parse("<details>\n<summary>X</summary>\n\n```foo`bar\nordinary\n</details>\n\nAfter")
        assertTrue(details[0] is MarkdownBlock.Details)
        assertEquals("After", (details[1] as MarkdownBlock.Paragraph).runs.text())
        val callout = parse("::: note\n\n```foo`bar\nordinary\n:::\n\nAfter")
        assertTrue(callout[0] is MarkdownBlock.Callout)
        assertEquals("After", (callout[1] as MarkdownBlock.Paragraph).runs.text())
    }

    @Test fun containersKeepCodeAndClosingTagsInFencesInert() {
        val details = parse("<details>\n<summary>Code</summary>\n\n```html\n</details>\n:smile: ==raw==\n```\n\ntext\n</details>").single() as MarkdownBlock.Details
        assertEquals("</details>\n:smile: ==raw==\n", (details.blocks[0] as MarkdownBlock.Code).source)
        val code = parse("```markdown\n::: note\n<details>\n:smile:\n```").single() as MarkdownBlock.Code
        assertTrue(code.source.contains(":smile:"))
    }
}
