package com.parka.mdive.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.test.platform.app.InstrumentationRegistry
import com.parka.mdive.testing.FixtureDocumentsProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercises Android's actual DocumentsUI, without replacing the activity-result contract. */
class SystemPickerTest {
    @get:Rule val compose = createAndroidComposeRule<com.parka.mdive.MainActivity>()
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = if (node == null) emptyList()
        else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
    private fun clickPlatform(label: String, timeout: Long = 8000): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < deadline) {
            val match = nodes(automation.rootInActiveWindow).firstOrNull {
                it.text?.toString() == label || it.contentDescription?.toString() == label
            }
            if (match != null) {
                var clickable: AccessibilityNodeInfo? = match
                while (clickable != null && !clickable.isClickable) clickable = clickable.parent
                if (clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
            }
            SystemClock.sleep(100)
        }
        return false
    }
    private fun selectFixture(name: String) {
        compose.onNodeWithText("파일 열기").performClick()
        // DocumentsUI transitions asynchronously; re-read its current surface until ready.
        val deadline = SystemClock.uptimeMillis() + 8000
        while (SystemClock.uptimeMillis() < deadline) {
            if (clickPlatform(name, 250)) return
            val labels = platformLabels()
            if (labels.contains("Show roots") || labels.contains("Open navigation drawer")) {
                clickPlatform("Show roots", 250) || clickPlatform("Open navigation drawer", 250)
            }
            if (platformLabels().contains("MDive S02 tests")) clickPlatform("MDive S02 tests", 250)
        }
        throw AssertionError("Fixture selection timed out: " + platformLabels())
    }
    private fun platformBack() {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
    }
    private fun awaitText(text: String) { compose.waitUntil(8000) {
        try { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        catch (_: IllegalStateException) { false }
    } }
    private fun platformLabels() = nodes(automation.rootInActiveWindow).mapNotNull { it.text ?: it.contentDescription }.joinToString()

    @Test fun actualSelectionReadsTitleAndCancelKeepsHome() {
        compose.onNodeWithText("파일 열기").performClick()
        SystemClock.sleep(500)
        platformBack()
        compose.waitUntil(8000) {
            try { compose.onNodeWithText("파일 열기").assertIsEnabled().assertIsDisplayed(); true }
            catch (_: AssertionError) { false }
            catch (_: IllegalStateException) { false }
        }
        compose.onNodeWithText("파일 열기").assertIsEnabled()
        selectFixture("source.md")
        awaitText("실제 선택")
        compose.onNodeWithText("source.md").assertIsDisplayed()
        compose.onNodeWithText("실제 선택").assertIsDisplayed()
        compose.onNodeWithText("선택한 인용").assertIsDisplayed()
        compose.onNodeWithText("선택한 목록").assertIsDisplayed()
        compose.onNodeWithText(FixtureDocumentsProvider.SOURCE).assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        awaitText("실제 선택")
        compose.onNodeWithText("source.md").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }

    @Test fun actualEmptyAndFailureRetryReturnHome() {
        selectFixture("empty.txt")
        awaitText("빈 문서예요")
        compose.onNodeWithText("빈 문서예요").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        try {
            FixtureDocumentsProvider.failReads = true
            selectFixture("source.md")
            awaitText("문서를 열지 못했어요")
            compose.onNodeWithText("문서를 열지 못했어요").assertIsDisplayed()
            FixtureDocumentsProvider.failReads = false
            compose.onNodeWithText("다시 시도").performClick()
            awaitText("실제 선택")
            compose.onNodeWithText("실제 선택").assertIsDisplayed()
        compose.onNodeWithText("선택한 인용").assertIsDisplayed()
        compose.onNodeWithText("선택한 목록").assertIsDisplayed()
        } finally { FixtureDocumentsProvider.failReads = false }
        platformBack()
        awaitText("반가워요.")
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }
    @Test fun actualGfmSelectionDisplaysMixedBodyAndReturnsHome() {
        selectFixture("gfm.md")
        awaitText("GFM 실제 선택")
        compose.onNodeWithText("gfm.md").assertIsDisplayed()
        compose.onNodeWithText("셀 본문").assertIsDisplayed()
        compose.onNodeWithText("할 일").assertIsDisplayed()
        compose.onNodeWithText("완료한 일").assertIsDisplayed()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("마지막 혼합 본문"))
        compose.onNodeWithText("[보존 링크](a.md)").assertHasNoClickAction()
        compose.onNodeWithText("마지막 혼합 본문").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }

}
