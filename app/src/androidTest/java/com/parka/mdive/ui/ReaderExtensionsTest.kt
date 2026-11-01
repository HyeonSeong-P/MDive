package com.parka.mdive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.domain.document.*
import com.parka.mdive.testing.ProductTestActivity
import com.parka.mdive.ui.reader.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReaderExtensionsTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()
    private fun text(value: String) = MarkdownBlock.Paragraph(listOf(MarkdownRun(value)))

    @Test fun detailsExcludeHiddenContentAndRequestsAndRetainNestedStateAcrossScroll() {
        val visibility = mutableListOf<Boolean>()
        val inner = MarkdownBlock.Details("inner", listOf(MarkdownRun("안쪽 제목")), true, listOf(
            text("숨겨진 본문"), MarkdownBlock.Paragraph(listOf(MarkdownRun("image", image = MarkdownImage("hidden.png", "숨긴 그림")))),
        ))
        val outer = MarkdownBlock.Details("outer", listOf(MarkdownRun("바깥 제목")), false, listOf(inner))
        val doc = MarkdownDocument(listOf(outer) + (0..45).map { text("긴 문단 $it") })
        compose.setContent { MDiveDesignTheme {
            var expanded by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
            Box(Modifier.width(320.dp)) { ReaderMarkdownContent(doc,
                images = ReaderImages(onVisible = { _, visible -> visibility += visible }),
                details = ReaderDetails(expanded) { id, open -> expanded = expanded + (id to open) }) }
        } }
        compose.onNodeWithText("바깥 제목").assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "접힘"))
        compose.onNodeWithText("안쪽 제목", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("숨겨진 본문", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { assertTrue(visibility.isEmpty()) }
        compose.onNodeWithText("바깥 제목").performClick()
        compose.onNodeWithText("바깥 제목").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "펼쳐짐"))
        compose.onNodeWithText("숨겨진 본문").assertIsDisplayed()
        compose.onNodeWithTag("image-loading").assertExists()
        compose.runOnIdle { assertEquals(listOf(true), visibility) }
        compose.onNodeWithText("안쪽 제목").performClick()
        compose.onNodeWithText("숨겨진 본문", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("image-loading", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(true, false), visibility) }
        compose.onNodeWithText("바깥 제목").performClick()
        compose.onNodeWithText("바깥 제목").performClick()
        compose.onNodeWithText("안쪽 제목").assertIsDisplayed()
        compose.onNodeWithText("숨겨진 본문", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("긴 문단 45"))
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("바깥 제목"))
        compose.onNodeWithText("안쪽 제목").assertIsDisplayed()
        compose.onNodeWithText("숨겨진 본문", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("안쪽 제목").performClick()
        compose.onNodeWithText("숨겨진 본문").assertIsDisplayed()
    }

    @Test fun inlineExtensionsKeepStylesAndLinksBesideMathInBothThemes() {
        val runs = listOf(
            MarkdownRun("mark", highlight = true, strong = true), MarkdownRun(" sub", subscript = true),
            MarkdownRun(" sup", superscript = true), MarkdownRun(" ins", inserted = true),
            MarkdownRun(" under", underline = true), MarkdownRun(" link", link = "target.md", strike = true, underline = true),
        )
        val math = MarkdownRun("math", math = MarkdownMath("x", "\$x\$", false, MathRaster(2, 2, 0, IntArray(4) { 0xff000000.toInt() })))
        val clicked = mutableListOf<String>()
        var dark by mutableStateOf(false)
        compose.setContent { MDiveDesignTheme(darkTheme = dark) {
            Box(Modifier.width(320.dp)) { ReaderMarkdownContent(MarkdownDocument(listOf(
                MarkdownBlock.Paragraph(runs), MarkdownBlock.Paragraph(listOf(math) + runs),
            )), onLink = { clicked += it }) }
        } }
        repeat(2) { pass ->
            for (mathFlow in listOf(false, true)) {
                val node = if (mathFlow) compose.onNodeWithTag("math-inline") else compose.onNodeWithText("mark sub sup ins under link")
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val value = layouts.single().layoutInput.text
                fun style(label: String) = value.spanStyles.first { it.start <= value.text.indexOf(label) && it.end > value.text.indexOf(label) }.item
                assertNotEquals(Color.Unspecified, style("mark").background)
                assertEquals(BaselineShift.Subscript, style("sub").baselineShift)
                assertEquals(BaselineShift.Superscript, style("sup").baselineShift)
                assertTrue(style("ins").textDecoration!!.contains(TextDecoration.Underline))
                assertTrue(style("under").textDecoration!!.contains(TextDecoration.Underline))
                assertTrue(style("link").textDecoration!!.contains(TextDecoration.Underline))
                assertTrue(style("link").textDecoration!!.contains(TextDecoration.LineThrough))
                val linkStart = value.text.indexOf("link")
                node.performTouchInput { click(layouts.single().getBoundingBox(linkStart).center) }
                compose.runOnIdle { assertEquals("target.md", clicked.last()) }
                assertFalse(layouts.single().didOverflowWidth)
            }
            if (pass == 0) compose.runOnIdle { dark = true }
        }
    }

    @Test fun calloutsDefinitionsAndDeepDetailsKeepWidthAtLargeFont() {
        val body = "좁은 폭에서도 끝까지 읽는 정의 설명입니다. ".repeat(4)
        var nested: MarkdownBlock = text(body)
        repeat(18) { depth -> nested = MarkdownBlock.Details("d$depth", listOf(MarkdownRun("설명 $depth")), true, listOf(nested)) }
        var callout: MarkdownBlock = text("깊은 callout 본문")
        repeat(8) { depth -> callout = MarkdownBlock.Callout(MarkdownCalloutKind.Caution, "사용자 제목 $depth", listOf(callout)) }
        val doc = MarkdownDocument(listOf(
            MarkdownBlock.Callout(MarkdownCalloutKind.Warning, "Warning", listOf(text("문서의 경고"))),
            MarkdownBlock.DefinitionList(listOf(MarkdownDefinition(listOf(listOf(MarkdownRun("첫 용어")), listOf(MarkdownRun("둘째 용어"))), listOf(listOf(text("첫 정의"), text("두번째 문단")), listOf(text("별도 정의")))))), nested, callout,
        ))
        compose.setContent { MDiveDesignTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                Box(Modifier.width(320.dp)) { ReaderMarkdownContent(doc) }
            }
        } }
        compose.onAllNodesWithTag("markdown-callout").onFirst().assertExists()
        for (label in listOf("문서의 경고", "첫 용어", "둘째 용어", "첫 정의", "두번째 문단", "별도 정의", body, "Caution · 사용자 제목 0", "깊은 callout 본문")) {
            compose.onNodeWithTag("markdown-body").performScrollToNode(hasText(label))
            compose.onNodeWithText(label).assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("overflow: $label", layouts.single().didOverflowWidth)
        }
        assertFalse(compose.onNodeWithTag("markdown-body").fetchSemanticsNode().config.contains(SemanticsProperties.HorizontalScrollAxisRange))
    }
    @Test fun summaryLinksAndImagesRemainAvailableWhileBodyIsCollapsed() {
        val visible = mutableListOf<String>()
        var destination: String? = null
        val summary = listOf(MarkdownRun("제목 링크", link = "summary.md"), MarkdownRun("summary image", image = MarkdownImage("summary.png", "제목 그림")))
        val hiddenImage = MarkdownBlock.Paragraph(listOf(MarkdownRun("hidden image", image = MarkdownImage("body.png", "본문 그림"))))
        val document = MarkdownDocument(listOf(MarkdownBlock.Details("summary", summary, false, listOf(text("숨긴 내용"), hiddenImage))))
        compose.setContent { MDiveDesignTheme {
            var expanded by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
            ReaderMarkdownContent(document, onLink = { destination = it },
                images = ReaderImages(onVisible = { image, show -> if (show) visible += image.destination }),
                details = ReaderDetails(expanded) { id, show -> expanded = expanded + (id to show) })
        } }
        compose.onNodeWithText("숨긴 내용", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("summary.png"), visible) }
        val node = compose.onNodeWithText("제목 링크", useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        node.performTouchInput { click(layouts.single().getBoundingBox(0).center) }
        compose.runOnIdle { assertEquals("summary.md", destination) }
        compose.onNodeWithText("숨긴 내용", useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "접힘")).performClick()
        compose.onNodeWithText("숨긴 내용").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("summary.png", "body.png"), visible) }
    }

}
