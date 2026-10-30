package com.parka.mdive.data.document

import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class ImagePolicyTest {
    @Test fun relativePathsDecodeOnceAndCannotEscapeSelectedTree() {
        assertEquals(listOf("그 림", "a+b.png"), imageSegments("%EA%B7%B8%20%EB%A6%BC/a+b.png"))
        assertEquals(listOf("images", "한글 #.png"), imageSegments("./tmp/../images/%ED%95%9C%EA%B8%80%20%23.png"))
        assertEquals(listOf("%2e%2e", "a.png"), imageSegments("%252e%252e/a.png"))
        listOf("../a", "a/../../x", "/a", "%2Fa", "file:///a", "content://a", "C:/a", "a\\b", "a%00b", "%zz").forEach { value ->
            try { imageSegments(value); fail(value) } catch (_: ImageProblem) { }
        }
        listOf("http://example.com/a", "https://u:p@example.com/a", "data:image/png,a", "https:/missing").forEach { value ->
            try { checkedImageUrl(value); fail(value) } catch (e: ImageProblem) { assertEquals(ImageFailure.Unsupported, e.reason) }
        }
        assertEquals("example.com", checkedImageUrl("https://example.com/a").host)
    }
    @Test fun actualBytesAndDimensionsEnforceLimitsBeforeAllocation() {
        assertEquals(IMAGE_BYTE_LIMIT, readImageBytes(ByteArrayInputStream(ByteArray(IMAGE_BYTE_LIMIT))).size)
        try { readImageBytes(ByteArrayInputStream(ByteArray(IMAGE_BYTE_LIMIT + 1))); fail() } catch (e: ImageProblem) { assertEquals(ImageFailure.TooLarge, e.reason) }
        try { readImageBytes(ByteArrayInputStream(byteArrayOf()), IMAGE_BYTE_LIMIT + 1L); fail() } catch (e: ImageProblem) { assertEquals(ImageFailure.TooLarge, e.reason) }
        checkImageDimensions(4000, 4000)
        try { checkImageDimensions(Int.MAX_VALUE, Int.MAX_VALUE); fail() } catch (e: ImageProblem) { assertEquals(ImageFailure.TooLarge, e.reason) }
    }
    @Test fun parserRecognizesImagesInBodyNestedFormattingAndTableWithoutExecutingLinks() = runBlocking {
        val doc = CommonMarkParser().parse("before **![**설명**](images/a.png \"제목\")** after\n\n| A |\n| - |\n| ![표](b.svg) |\n\n[링크](other.md)\n\n`![code](no.png)`")
        val runs = (doc.blocks[0] as MarkdownBlock.Paragraph).runs
        assertEquals(MarkdownImage("images/a.png", "설명", "제목"), runs.single { it.image != null }.image)
        assertTrue(runs.single { it.image != null }.strong)
        val table = doc.blocks[1] as MarkdownBlock.Table
        assertEquals("b.svg", table.rows.single().single().single().image?.destination)
        assertNull((doc.blocks[2] as MarkdownBlock.Paragraph).runs.single().image)
        assertNull((doc.blocks[3] as MarkdownBlock.Paragraph).runs.single().image)
    }
}
