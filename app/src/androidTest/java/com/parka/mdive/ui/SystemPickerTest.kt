package com.parka.mdive.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        // DocumentsUI remembers its last root; open the drawer only if needed.
        if (!clickPlatform(name, 1000)) {
            assertTrue("System drawer: " + platformLabels(), clickPlatform("Show roots", 1000) || clickPlatform("Open navigation drawer", 1000))
            assertTrue("Fixture root: " + platformLabels(), clickPlatform("MDive S02 tests"))
            assertTrue("Fixture file: " + platformLabels(), clickPlatform(name))
        }
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
        awaitText("파일을 읽었어요")
        compose.onNodeWithText("source.md").assertIsDisplayed()
        compose.onNodeWithText("파일을 읽었어요").assertIsDisplayed()
        compose.onNodeWithText(FixtureDocumentsProvider.SOURCE).assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        awaitText("파일을 읽었어요")
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
            awaitText("파일을 읽었어요")
            compose.onNodeWithText("파일을 읽었어요").assertIsDisplayed()
        } finally { FixtureDocumentsProvider.failReads = false }
        platformBack()
        awaitText("반가워요.")
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }
}
