package com.parka.mdive.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.MDiveButton
import com.parka.mdive.designsystem.MDiveButtonVariant
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.designsystem.MDiveMarkdownFlow
import com.parka.mdive.designsystem.MDiveMarkdownSection
import com.parka.mdive.designsystem.MDiveMarkdownHeading
import com.parka.mdive.designsystem.MDiveMarkdownText
import com.parka.mdive.designsystem.MDiveRecoveryActions
import com.parka.mdive.designsystem.MDiveTextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompositionContractsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun headingLeadsSectionWithMoreSpaceBeforeThanAfter() {
        compose.setContent {
            MDiveDesignTheme {
                MDiveMarkdownFlow {
                    MDiveMarkdownText(listOf(MDiveTextRun("앞 문단")), Modifier.testTag("before"))
                    MDiveMarkdownSection(heading = {
                        MDiveMarkdownHeading(2, listOf(MDiveTextRun("구역 제목")), Modifier.testTag("heading"))
                    }) {
                        MDiveMarkdownText(listOf(MDiveTextRun("뒤 문단")), Modifier.testTag("after"))
                        MDiveMarkdownText(listOf(MDiveTextRun("다음 문단")), Modifier.testTag("next"))
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("before").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithTag("heading").fetchSemanticsNode().boundsInRoot
        val after = compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithTag("next").fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        assertEquals(32 * density, title.top - before.bottom, 1f)
        assertEquals(8 * density, after.top - title.bottom, 1f)
        assertEquals(16 * density, next.top - after.bottom, 1f)
    }

    @Test
    fun flowPreservesColumnWeightAndChildAlignment() {
        compose.setContent {
            Box(Modifier.size(200.dp)) {
                MDiveMarkdownFlow(Modifier.fillMaxSize()) {
                    Box(Modifier.size(20.dp).align(Alignment.End).testTag("aligned"))
                    Box(Modifier.weight(1f).fillMaxWidth().testTag("weighted"))
                }
            }
        }
        val aligned = compose.onNodeWithTag("aligned").fetchSemanticsNode().boundsInRoot
        val weighted = compose.onNodeWithTag("weighted").fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        assertEquals(180 * density, aligned.left - weighted.left, 1f)
        assertEquals(164 * density, weighted.height, 1f)
    }

    @Test
    fun recoveryActionsWrapWithoutShrinkingTouchTargets() {
        compose.setContent {
            MDiveDesignTheme {
                Box(Modifier.width(160.dp)) {
                    MDiveRecoveryActions(
                        primary = { MDiveButton("다시 시도", {}) },
                        secondary = { MDiveButton("다시 선택", {}, variant = MDiveButtonVariant.Outlined) },
                        tertiary = { MDiveButton("홈으로", {}, variant = MDiveButtonVariant.Quiet) },
                    )
                }
            }
        }
        val primary = compose.onNodeWithText("다시 시도").fetchSemanticsNode().boundsInRoot
        val secondary = compose.onNodeWithText("다시 선택").fetchSemanticsNode().boundsInRoot
        val home = compose.onNodeWithText("홈으로").fetchSemanticsNode().boundsInRoot
        val minimum = 48 * compose.density.density
        assertTrue(primary.height >= minimum - 1)
        assertTrue(secondary.height >= minimum - 1)
        assertTrue(secondary.top >= primary.bottom)
        assertTrue(home.top >= secondary.bottom)
    }
}
