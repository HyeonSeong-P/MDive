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
import com.parka.mdive.domain.document.MarkdownDocument
import com.parka.mdive.testing.ProductTestActivity
import com.parka.mdive.ui.reader.ReaderMarkdownContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReaderMarkdownTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()
    private fun parse(source: String) = runBlocking { CommonMarkParser().parse(source) }
    private fun show(document: MarkdownDocument, large: Boolean = false) {
        compose.setContent {
            MDiveDesignTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.6f else 1f)) {
                    Box(Modifier.width(320.dp)) { ReaderMarkdownContent(document) }
                }
            }
        }
    }

    @Test fun basicStructuresAndInertUnsupportedSourceRemainReadable() {
        show(parse("# 제목\n\n문단 **굵게** *강조* `코드`\n\n> 인용\n\n3. 목록\n   - 중첩\n\n---\n\n[링크](https://example.com) ![이미지](image.png)\n\n```\n소스\n```\n\n마지막"))
        compose.onNodeWithText("제목").assertIsDisplayed()
        compose.onNodeWithText("문단 굵게 강조 코드").assertIsDisplayed()
        compose.onNodeWithText("인용").assertIsDisplayed()
        compose.onNodeWithText("3.").assertIsDisplayed()
        compose.onNodeWithText("중첩").assertIsDisplayed()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("[링크](https://example.com) ![이미지](image.png)"))
        compose.onNodeWithText("[링크](https://example.com) ![이미지](image.png)").assertHasNoClickAction()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("마지막"))
        compose.onNodeWithText("```\n소스\n```").assertIsDisplayed()
        compose.onNodeWithText("마지막").assertIsDisplayed()
    }

    @Test fun longHeadingAndUnbrokenBodyWrapAtNarrowWidthAndLargeFont() {
        val heading = "길고 긴 제목을 큰 글자로 좁은 화면에서도 끝까지 읽을 수 있어요"
        val paragraph = "x".repeat(96)
        show(parse("# $heading\n\n$paragraph"), large = true)
        for (text in listOf(heading, paragraph)) {
            compose.onNodeWithTag("markdown-body").performScrollToNode(hasText(text))
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.single().lineCount > 1)
            assertFalse(layouts.single().didOverflowWidth)
            assertFalse(layouts.single().didOverflowHeight)
        }
    }
    @Test fun deeplyNestedQuoteAndListKeepBodyWidthAtLargeFont() {
        val quote = "깊은 인용 본문"
        val list = "깊은 목록 본문"
        val source = "> > - 인용 안 목록\n\n" + "> ".repeat(20) + quote + "\n\n" +
            (0 until 20).joinToString("\n") { "  ".repeat(it) + "- " + if (it == 19) list else "항목 $it" }
        show(parse(source), large = true)
        for (text in listOf(quote, list)) {
            compose.onNodeWithTag("markdown-body").performScrollToNode(hasText(text))
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.single().size.width > 0)
            assertFalse(layouts.single().didOverflowWidth)
            assertFalse(layouts.single().didOverflowHeight)
        }
    }

}
