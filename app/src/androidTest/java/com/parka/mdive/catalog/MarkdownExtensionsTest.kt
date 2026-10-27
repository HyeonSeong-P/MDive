package com.parka.mdive.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parka.mdive.designsystem.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownExtensionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun subscriptUsesRelativeSizeForCustomTextStyle() {
        compose.setContent { MDiveDesignTheme {
            MDiveMarkdownText(listOf(MDiveTextRun("2", baseline = MDiveTextBaseline.Subscript)), style = TextStyle(fontSize = 40.sp))
        } }
        val text = compose.onNodeWithText("2").fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals(.75f.em, text.spanStyles.single().item.fontSize)
    }

    @Test fun controlledDetailsRemovesHiddenContentAndDispatchesToggle() {
        val changes = mutableListOf<Boolean>()
        compose.setContent { MDiveDesignTheme {
            var expanded by remember { mutableStateOf(false) }
            MDiveMarkdownDetails("추가 설명", expanded, { expanded = it; changes.add(it) }) { Text("접힌 자식의 내용") }
        } }
        compose.onNodeWithText("접힌 자식의 내용").assertDoesNotExist()
        compose.onNodeWithText("추가 설명").performClick()
        compose.onNodeWithText("접힌 자식의 내용").assertExists()
        compose.onNodeWithText("추가 설명").performClick()
        compose.onNodeWithText("접힌 자식의 내용").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(true, false), changes) }
    }

    @Test fun referenceAndBacklinkPreserveOpaqueTargets() {
        val targets = mutableListOf<String>()
        compose.setContent { MDiveDesignTheme {
            Column {
                MDiveMarkdownReference(MDiveDocumentReference("각주 이동", "note:opaque"), { targets.add(it) })
                MDiveReferenceList(
                    listOf(MDiveReferenceEntry(MDiveDocumentReference("1.", "note:opaque"), listOf(MDiveTextRun("각주 본문")), listOf(MDiveDocumentReference("돌아가기", "return:opaque")))),
                    { targets.add(it) },
                )
            }
        } }
        compose.onNodeWithText("각주 이동").performTouchInput { click() }
        compose.onNodeWithText("돌아가기").performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf("note:opaque", "return:opaque"), targets) }
    }

    @Test fun failedEmbedKeepsSurroundingContentAndDoesNotComposePreparedSlot() {
        var sourceRequests = 0
        compose.setContent { MDiveDesignTheme {
            Column {
                Text("앞 문단")
                MDiveEmbeddedContent(MDiveResourceState.Failure, "미디어 설명", onViewSource = { sourceRequests++ }) { Text("준비된 외부 콘텐츠") }
                Text("뒤 문단")
            }
        } }
        compose.onNodeWithText("앞 문단").assertExists()
        compose.onNodeWithText("뒤 문단").assertExists()
        compose.onNodeWithText("준비된 외부 콘텐츠").assertDoesNotExist()
        compose.onNodeWithText("원문 보기").performClick()
        compose.runOnIdle { assertEquals(1, sourceRequests) }
    }
}
