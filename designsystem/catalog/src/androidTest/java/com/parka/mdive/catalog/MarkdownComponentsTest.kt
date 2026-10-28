package com.parka.mdive.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parka.mdive.designsystem.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun copyPreservesWhitespaceAndInvalidHighlightFallsBack() {
        val source = "  val 한글 = 1\n\n\t // untouched\n"
        var copied: String? = null
        compose.setContent { MDiveDesignTheme {
            MDiveCodeBlock(source, highlights = listOf(MDiveCodeHighlight(-2, 900, MDiveCodeTone.Keyword)), onCopy = { copied = it })
        } }
        compose.onNodeWithText(source).assertExists()
        compose.onNodeWithContentDescription("코드 복사").performClick()
        compose.runOnIdle { assertEquals(source, copied) }
    }

    @Test fun linkOnlyDispatchesTargetToHost() {
        val target = "relative/notes.md#heading"
        var selected: String? = null
        compose.setContent { MDiveDesignTheme { MDiveMarkdownText(listOf(MDiveTextRun("문서 링크", strong = true, emphasis = true, link = target)), onLink = { selected = it }) } }
        compose.onNodeWithText("문서 링크").performTouchInput { click() }
        compose.runOnIdle { assertEquals(target, selected) }
    }

    @Test fun resourceRecoveryActionsRemainDistinctAndSurroundingTextSurvives() {
        val actions = mutableListOf<String>()
        compose.setContent { MDiveDesignTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("앞 본문")
                MDiveMarkdownImage(MDiveResourceState.AccessRequired, "로컬 이미지", onConnect = { actions.add("connect") }, onRetry = { actions.add("wrong") }) { Text("미표시") }
                MDiveMarkdownImage(MDiveResourceState.Failure, "웹 이미지", onConnect = { actions.add("wrong") }, onRetry = { actions.add("retry") }) { Text("미표시") }
                MDiveContentFallback("시연", "일부 실패", onViewSource = { actions.add("source") })
                Text("뒤 본문")
            }
        } }
        compose.onNodeWithText("앞 본문").assertExists()
        compose.onNodeWithText("폴더 연결").performScrollTo().performClick()
        compose.onNodeWithText("다시 시도").performScrollTo().performClick()
        compose.onNodeWithText("원문 보기").performScrollTo().performClick()
        compose.onNodeWithText("뒤 본문").assertExists()
        compose.onNodeWithText("미표시").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("connect", "retry", "source"), actions) }
    }

    @Test fun checkboxInteractiveDisabledAndReadonlyContracts() {
        var changed: Boolean? = null
        compose.setContent { MDiveDesignTheme {
            Column {
                MDiveCheckbox(false, { changed = it }, Modifier.testTag("checkbox"))
                MDiveCheckbox(false, { changed = it }, Modifier.testTag("disabled"), enabled = false)
                MDiveCheckbox(true, null, Modifier.testTag("readonly"))
            }
        } }
        compose.onNodeWithTag("checkbox").assertIsOff().performClick()
        compose.runOnIdle { assertEquals(true, changed) }
        compose.onNodeWithTag("disabled").assertIsNotEnabled()
        compose.onNodeWithTag("readonly").assertIsOn().assertHasNoClickAction()
    }

    @Test fun imageAltOwnsSemanticsAndDecorativeContentIsSilent() {
        compose.setContent { MDiveDesignTheme {
            Column {
                MDiveMarkdownImage(MDiveResourceState.Ready, "이미지 대체 설명") { Text("slot 내부 낭독 중복") }
                MDiveMarkdownImage(MDiveResourceState.Ready, "") { Text("장식 slot 내부 텍스트") }
            }
        } }
        compose.onNodeWithContentDescription("이미지 대체 설명").assertExists()
        compose.onNodeWithText("slot 내부 낭독 중복").assertDoesNotExist()
        compose.onNodeWithText("장식 slot 내부 텍스트").assertDoesNotExist()
    }

    @Test fun narrowLargeTextRtlKeepsCodeAndTableLocallyScrollable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                MDiveDesignTheme(darkTheme = true, paletteId = "green") {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        MDiveMarkdownHeading(6, listOf(MDiveTextRun("긴 제목 한글 English ".repeat(3))))
                        MDiveMarkdownListItem(MDiveListMarker.Task(true)) { MDiveMarkdownText(listOf(MDiveTextRun("읽기 전용 긴 항목 ".repeat(3)))) }
                        MDiveCodeBlock("a".repeat(150), Modifier.testTag("long-code"))
                        MDiveMarkdownTable(listOf(MDiveTableColumn(listOf(MDiveTextRun("헤더 A"))), MDiveTableColumn(listOf(MDiveTextRun("헤더 B"))), MDiveTableColumn(listOf(MDiveTextRun("헤더 C")))), emptyList(), Modifier.testTag("wide-table"))
                        Text("다음 문단")
                    }
                }
            }
        }
        compose.onNodeWithTag("long-code").performScrollTo()
        compose.onNodeWithText("a".repeat(150)).performTouchInput { swipeLeft() }
        compose.onNodeWithTag("wide-table").performScrollTo().performTouchInput { swipeRight() }
        compose.onNodeWithText("다음 문단").performScrollTo().assertIsDisplayed()
        compose.onNode(isToggleable()).assertExists().assertHasNoClickAction()
    }
    private fun assertNestedTaskMarkerMatchesFirstBodyLine(fontScale: Float, wrapped: Boolean = true) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MDiveDesignTheme {
                    Column(Modifier.width(280.dp)) {
                        MDiveMarkdownListItem(MDiveListMarker.Task(false)) {
                            MDiveMarkdownText(listOf(MDiveTextRun(if (wrapped) "부모 본문이 여러 줄로 이어져도 첫 줄에 상태를 맞추는 예시" else "부모")), Modifier.testTag("parent-body"))
                            MDiveMarkdownListItem(MDiveListMarker.Task(true)) {
                                MDiveMarkdownText(listOf(MDiveTextRun("완료한 자식 작업")))
                            }
                            MDiveMarkdownText(listOf(MDiveTextRun("부모의 두 번째 문단")))
                        }
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        val body = compose.onNodeWithTag("parent-body")
        body.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        if (wrapped) assertTrue(layout.lineCount > 1) else assertEquals(1, layout.lineCount)
        val marker = compose.onNode(isToggleable().and(isOff())).assertHasNoClickAction().fetchSemanticsNode().boundsInRoot
        val firstLineCenter = body.fetchSemanticsNode().boundsInRoot.top + (layout.getLineTop(0) + layout.getLineBottom(0)) / 2f
        assertEquals(firstLineCenter, marker.center.y, 2f)
        val child = compose.onNode(isToggleable().and(isOn())).assertHasNoClickAction().fetchSemanticsNode().boundsInRoot
        assertTrue(marker.bottom < child.top)
    }

    @Test fun nestedTaskMarkerStaysBesideFirstWrappedBodyLine() {
        assertNestedTaskMarkerMatchesFirstBodyLine(1f)
    }

    @Test fun nestedTaskMarkerFollowsLargeFontFirstLine() {
        assertNestedTaskMarkerMatchesFirstBodyLine(1.6f)
    }

    @Test fun singleLineTaskMarkerMatchesFirstLine() {
        assertNestedTaskMarkerMatchesFirstBodyLine(1f, wrapped = false)
    }

    @Test fun singleLineTaskMarkerMatchesLargeFontFirstLine() {
        assertNestedTaskMarkerMatchesFirstBodyLine(1.6f, wrapped = false)
    }

}
