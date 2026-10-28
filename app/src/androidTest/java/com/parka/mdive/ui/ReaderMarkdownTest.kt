package com.parka.mdive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
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
        compose.onNodeWithText("소스\n").assertIsDisplayed()
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

    @Test fun gfmTableScrollsLocallyAndLongCellWrapsAtLargeFont() {
        val longCell = "긴셀".repeat(32)
        val after = "본문".repeat(48)
        show(parse("| 왼쪽 | 중앙 | 오른쪽 |\n| :-- | :-: | --: |\n| $longCell | **굵게** | 끝셀 |\n\n$after"), large = true)
        val table = compose.onNodeWithTag("markdown-table")
        val range = table.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
        assertTrue(range.maxValue() > 0f)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(longCell).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.single().lineCount > 1)
        assertFalse(layouts.single().didOverflowWidth)
        table.performSemanticsAction(SemanticsActions.ScrollBy) { it(1000f, 0f) }
        compose.onNodeWithText("끝셀").assertIsDisplayed()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText(after))
        val bodyLayouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(after).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(bodyLayouts) }
        assertFalse(bodyLayouts.single().didOverflowWidth)
        assertFalse(compose.onNodeWithTag("markdown-body").fetchSemanticsNode().config.contains(SemanticsProperties.HorizontalScrollAxisRange))
    }

    @Test fun tasksExposeTwoInertStatesAndStrikethroughStyles() {
        show(parse("- [ ] 미완료\n  - [X] ~~**완료**~~\n\n~단일~"))
        for (state in listOf(ToggleableState.On, ToggleableState.Off)) {
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state))
                .assertHasNoClickAction()
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("완료").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.single().layoutInput.text.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough })
        compose.onNodeWithText("~단일~").assertIsDisplayed()
    }

    @Test fun deeplyNestedTasksKeepStatesAndBodyAtLargeFont() {
        val final = "깊은 task 본문을 좁은 화면에서도 읽어요"
        val source = (0 until 20).joinToString("\n") { "  ".repeat(it) + "- [x] " + if (it == 19) final else "완료 $it" }
        show(parse(source), large = true)
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText(final))
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(final).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().didOverflowWidth)
        assertTrue(layouts.single().size.width > 0)
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .fetchSemanticsNodes().forEach { assertFalse(it.config.contains(SemanticsActions.OnClick)) }
    }

    @Test fun tableWithinQuoteAndListMeasuresWithIntrinsicQuoteAtLargeFont() {
        show(parse("> - 표 앞 목록\n>\n>   | 인용 헤더 | 다음 헤더 |\n>   | :-- | --: |\n>   | 인용 셀 | "+ "긴 내용".repeat(20) + " |\n\n인용 뒤 본문"), large = true)
        compose.onNodeWithText("인용 셀").assertIsDisplayed()
        val table = compose.onNodeWithTag("markdown-table")
        val range = table.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
        assertTrue(range.maxValue() > 0f)
        table.performSemanticsAction(SemanticsActions.ScrollBy) { it(1000f, 0f) }
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("인용 뒤 본문"))
        compose.onNodeWithText("인용 뒤 본문").assertIsDisplayed()
    }

}
