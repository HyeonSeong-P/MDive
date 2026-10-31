package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DocumentLinkPolicyTest {
    @Test fun pathsDecodeOnceAndStayInsideTree() {
        assertEquals(listOf("하위", "문서 +.MARKDOWN"), linkSegments("하위/%EB%AC%B8%EC%84%9C%20+.MARKDOWN"))
        assertEquals(listOf("a.md"), linkSegments("../a.md", listOf("child")))
        assertEquals(listOf("literal%20.md"), linkSegments("literal%2520.md"))
        assertEquals(listOf("child", "x.md"), linkSegments("./x.md", listOf("child")))
    }
    @Test fun forbiddenAndUnsupportedDestinationsNeverBecomeFilePaths() {
        mapOf("../a.md" to LinkFailure.OutsideFolder, "/a.md" to LinkFailure.OutsideFolder,
            "%2Fa.md" to LinkFailure.OutsideFolder, "a%00.md" to LinkFailure.OutsideFolder,
            "file:a.md" to LinkFailure.OutsideFolder, "content:a.md" to LinkFailure.OutsideFolder,
            "a.md?q=x" to LinkFailure.OutsideFolder, "a.md#title" to LinkFailure.Fragment,
            "#title" to LinkFailure.Fragment, "%C0%AF.md" to LinkFailure.OutsideFolder,
            "%zz.md" to LinkFailure.OutsideFolder, "a.pdf" to LinkFailure.Unsupported,
            "a.txt" to LinkFailure.Unsupported, "a%5Cb.md" to LinkFailure.OutsideFolder).forEach { (input, reason) ->
            try { linkSegments(input); fail(input) } catch (e: LinkProblem) { assertEquals(input, reason, e.reason) }
        }
    }
    @Test fun webLinksOnlyUseHttpOrHttpsWithAHostAndNoCredentials() {
        listOf("https://example.org/a#heading", "http://example.org", "HTTPS://example.org/a%20b").forEach { assertTrue(it, checkedWebLink(it)) }
        listOf("mailto:a@b", "intent://a", "https://user@example.org", "https:///a", "https://example.org/a b", "javascript:alert(1)").forEach { assertFalse(it, checkedWebLink(it)) }
    }
    @Test fun nestedInlineReferenceAutolinkAndImageLinksKeepDestinations() = runBlocking {
        val blocks = CommonMarkParser().parse("[**굵게** *강조* `코드`](docs/a.md)\n\n[![그림](a.png)](b.md)\n\n<https://example.org>\n\n[참조][id]\n\n[id]: c.md\n\n[기타](mailto:a@b)").blocks
        val first = (blocks[0] as MarkdownBlock.Paragraph).runs
        assertTrue(first.all { it.link == "docs/a.md" })
        assertTrue(first.any { it.strong }); assertTrue(first.any { it.emphasis }); assertTrue(first.any { it.code })
        val linkedImage = (blocks[1] as MarkdownBlock.Paragraph).runs.single()
        assertEquals("b.md", linkedImage.link); assertEquals("a.png", linkedImage.image?.destination)
        assertEquals("https://example.org", (blocks[2] as MarkdownBlock.Paragraph).runs.single().link)
        assertEquals("c.md", (blocks[3] as MarkdownBlock.Paragraph).runs.single().link)
        assertEquals("mailto:a@b", (blocks.last() as MarkdownBlock.Paragraph).runs.single().link)
    }
}
