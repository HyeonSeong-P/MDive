package com.parka.mdive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.parka.mdive.data.document.CommonMarkParser
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.testing.ProductTestActivity
import com.parka.mdive.ui.reader.*
import com.parka.mdive.domain.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReaderLinkTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()
    private fun tapLink(label: String) {
        val node = compose.onNodeWithText(label)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        node.performTouchInput { click(layouts.single().getBoundingBox(0).center) }
    }
    @Test fun nestedLinksTableAndImageDestinationAreClickableAtLargeFont() {
        val doc = runBlocking { CommonMarkParser().parse("# [제목 링크](title.md)\n\n[**굵게** *강조* `코드`][id]\n\n| 링크 |\n| - |\n| [표 링크](table.md) |\n\n[![그림](a.png)](image.md)\n\n끝\n\n[id]: target.md") }
        val clicked = mutableListOf<String>()
        compose.setContent { MDiveDesignTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                Box(Modifier.width(320.dp)) { ReaderMarkdownContent(doc, onLink = { clicked += it }) }
            }
        } }
        tapLink("제목 링크")
        compose.runOnIdle { assertEquals("title.md", clicked.last()) }
        tapLink("굵게 강조 코드")
        compose.runOnIdle { assertEquals("target.md", clicked.last()) }
        compose.onNodeWithText("[id]: target.md").assertDoesNotExist()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("표 링크"))
        tapLink("표 링크")
        compose.runOnIdle { assertEquals("table.md", clicked.last()) }
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("image.md"))
        tapLink("image.md")
        compose.runOnIdle { assertEquals("image.md", clicked.last()) }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("image.md").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().didOverflowWidth)
        compose.onNodeWithTag("image-loading").assertExists()
    }
    @Test fun failureBannerKeepsBodyAndExposesDestinationAndRecovery() {
        val doc = runBlocking { CommonMarkParser().parse("원래 본문") }
        var connects = 0
        compose.setContent { MDiveDesignTheme {
            ReaderScreen(ReaderState(title = "original.md", content = ReaderContent.SourceReady,
                document = SourceDocument("original.md", "원래 본문"), markdown = doc,
                linkFailure = LinkFailure.Access, linkDestination = "하위/a.md"), {}, {}, onConnectDocumentFolder = { connects++ })
        } }
        compose.onNodeWithText("원래 본문").assertIsDisplayed()
        compose.onNodeWithText("하위/a.md", substring = true).assertIsDisplayed()
        compose.onNodeWithText("문서 폴더 연결").performClick()
        compose.runOnIdle { assertEquals(1, connects) }
    }
    @Test fun browserAdapterRejectsUnsupportedDestinationsAndExternalLaunchFailures() {
        assertFalse(openBrowser(compose.activity, "mailto:test@example.org"))
        assertFalse(openBrowser(compose.activity, "intent://bad"))
        assertFalse(openBrowser(compose.activity, "https:///bad"))
        assertFalse(openBrowser(compose.activity, "https://example.org", candidates = { emptyList() }))
        val candidates: (android.net.Uri) -> List<android.content.Intent> = { listOf(android.content.Intent(android.content.Intent.ACTION_VIEW, it).setPackage("fixture.browser")) }
        assertFalse(openBrowser(compose.activity, "https://example.org", candidates, launch = { throw android.content.ActivityNotFoundException() }))
        assertFalse(openBrowser(compose.activity, "https://example.org", candidates, launch = { throw SecurityException() }))
        var launched: android.content.Intent? = null
        assertTrue(openBrowser(compose.activity, "http://example.org/#anchor", candidates, launch = { launched = it }))
        assertEquals("http://example.org/#anchor", launched?.dataString)
        assertEquals("fixture.browser", launched?.`package`)
    }
    @Test fun mathNeighborsKeepLinkClickAndCombinedStrikeDecoration() {
        val doc = runBlocking { CommonMarkParser().parse("\$x\$ [~~취소 링크~~](strike.md)") }
        var clicked: String? = null
        compose.setContent { MDiveDesignTheme { ReaderMarkdownContent(doc, onLink = { clicked = it }) } }
        val node = compose.onNodeWithTag("math-inline")
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val text = layouts.single().layoutInput.text
        val start = text.text.indexOf("취소 링크")
        assertTrue(start >= 0)
        assertTrue(text.spanStyles.any { range -> range.start <= start && range.end > start &&
            range.item.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.LineThrough) == true &&
            range.item.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.Underline) == true })
        node.performTouchInput { click(layouts.single().getBoundingBox(start).center) }
        compose.runOnIdle { assertEquals("strike.md", clicked) }
    }

}
