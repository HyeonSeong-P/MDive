package com.parka.mdive.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.onLast
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
            val matches = nodes(automation.rootInActiveWindow).filter {
                it.text?.toString() == label || it.contentDescription?.toString() == label
            }
            for (match in matches) {
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
        val deadline = SystemClock.uptimeMillis() + 12000
        var rootChosen = false
        while (SystemClock.uptimeMillis() < deadline) {
            if (clickPlatform(name, 250)) return
            if (rootChosen) {
                val root = automation.rootInActiveWindow
                if (root != null) {
                    // Some DocumentsUI releases omit scroll actions from their file RecyclerView.
                    // Derive a vertical gesture from the live window bounds, not screen constants.
                    val bounds = android.graphics.Rect()
                    root.getBoundsInScreen(bounds)
                    val x = bounds.centerX()
                    val from = bounds.top + bounds.height() * 3 / 4
                    val to = bounds.top + bounds.height() / 3
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input swipe $x $from $x $to 250")).use { it.readBytes() }
                }
                SystemClock.sleep(100)
                continue
            }
            val labels = platformLabels()
            if (labels.contains("Show roots") || labels.contains("Open navigation drawer")) {
                clickPlatform("Show roots", 250) || clickPlatform("Open navigation drawer", 250)
            }
            if (platformLabels().contains("MDive S02 tests")) rootChosen = clickPlatform("MDive S02 tests", 250)
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

    @Test fun actualCodeSelectionCopiesOriginalBodyAndReturnsHome() {
        selectFixture("code.md")
        awaitText("코드 실제 선택")
        compose.onNodeWithText("code.md").assertIsDisplayed()
        compose.onNodeWithContentDescription("코드 복사").performClick()
        compose.runOnIdle {
            val clipboard = compose.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            org.junit.Assert.assertEquals(FixtureDocumentsProvider.CODE_BODY, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
        compose.onNodeWithText("코드 뒤 본문").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }

    @Test fun actualMathSelectionKeepsPartialFailureSourceAndReturnsHome() {
        selectFixture("math.md")
        awaitText("수식 실제 선택")
        compose.onNodeWithText("math.md").assertIsDisplayed()
        compose.onNodeWithTag("math-block").assertIsDisplayed()
        compose.onNodeWithText("수식을 표시하지 못했어요").assertIsDisplayed()
        compose.onNodeWithText("원문 보기").performClick()
        compose.onNodeWithText("수식 원문").assertIsDisplayed()
        compose.onNodeWithText("\$\\unknown{x}\$").assertIsDisplayed()
        compose.onNodeWithContentDescription("패널 닫기").performClick()
        compose.onNodeWithText("수식 원문").assertDoesNotExist()
        compose.onNodeWithText("수식 뒤 본문").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }

    @Test fun actualMermaidSelectionMixedBodyExpandedReturnAndHome() {
        selectFixture("mermaid.md")
        awaitText("Mermaid 실제 선택")
        compose.onNodeWithText("mermaid.md").assertIsDisplayed()
        compose.onNodeWithTag("markdown-code").assertIsDisplayed()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("mermaid-picture").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mermaid-picture").performScrollTo().performClick()
        compose.waitUntil(15_000) { try { compose.onNodeWithTag("mermaid-viewport").assertExists(); true } catch (_: AssertionError) { false } }
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("다이어그램을 표시하지 못했어요"))
        compose.onAllNodesWithText("원문 보기").onLast().performClick()
        compose.onNodeWithText("```mermaid\npie\n```").assertIsDisplayed()
        compose.onNodeWithContentDescription("패널 닫기").performClick()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("Mermaid 뒤 본문"))
        compose.onNodeWithText("Mermaid 뒤 본문").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }

    @Test fun realDownloadedSampleDisplaysAllSixPicturesAndDismissesSourceBySwipe() {
        org.junit.Assume.assumeTrue("Self-contained Downloads setup requires MediaStore API29+", android.os.Build.VERSION.SDK_INT >= 29)
        val sample = InstrumentationRegistry.getInstrumentation().context.assets.open("mermaid.md").use { it.readBytes() }
        val sampleName = "mdive-s07-test-${java.util.UUID.randomUUID()}.md"
        val resolver = compose.activity.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, sampleName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val sampleUri = requireNotNull(resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            requireNotNull(resolver.openOutputStream(sampleUri)).use { it.write(sample) }
            resolver.update(sampleUri, android.content.ContentValues().apply { put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            compose.onNodeWithText("파일 열기").performClick()
            val deadline = SystemClock.uptimeMillis() + 12000
            var selected = false
            while (SystemClock.uptimeMillis() < deadline && !selected) {
                selected = clickPlatform(sampleName, 250)
                if (selected) break
                val labels = platformLabels()
                if (labels.contains("Show roots") || labels.contains("Open navigation drawer")) {
                    clickPlatform("Show roots", 250) || clickPlatform("Open navigation drawer", 250)
                }
                clickPlatform("Downloads", 250) || clickPlatform("다운로드", 250)
            }
            assertTrue("Downloaded sample was not selected: " + platformLabels(), selected)
            awaitText("Mermaid 직접 확인")
            val source = sample.toString(Charsets.UTF_8)
            val document = kotlinx.coroutines.runBlocking { com.parka.mdive.data.document.CommonMarkParser().parse(source) }
            val diagrams = document.blocks.withIndex().filter { it.value is com.parka.mdive.domain.document.MarkdownBlock.Mermaid }
            org.junit.Assert.assertEquals(8, diagrams.size)
            for ((number, entry) in diagrams.take(6).withIndex()) {
                val diagram = (entry.value as com.parka.mdive.domain.document.MarkdownBlock.Mermaid).diagram
                compose.onNodeWithTag("markdown-body").performScrollToIndex(entry.index)
                val description = "Mermaid 다이어그램: ${diagram.code}"
                compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty() }
                val picture = compose.onNodeWithContentDescription(description).performScrollTo()
                picture.assertIsDisplayed().assertHasClickAction()
                compose.waitUntil(5_000) {
                    val pixels = picture.captureToImage().toPixelMap()
                    val background = pixels[0,0]
                    var ink = 0
                    for (y in 0 until pixels.height step 3) for (x in 0 until pixels.width step 3) if (pixels[x,y] != background) ink++
                    ink > 50
                }
                val capture = picture.captureToImage().asAndroidBitmap()
                java.io.File(compose.activity.filesDir, "s07-real-diagram-${number+1}.png").outputStream().use { capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                if (number == 0) {
                    java.io.File(compose.activity.filesDir,"s07-real-sample-flow.png").outputStream().use { automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                    compose.onAllNodesWithText("콘텐츠 확대").assertCountEquals(0)
                    picture.performClick()
                    compose.waitUntil(15_000) { compose.onAllNodesWithTag("mermaid-viewport").fetchSemanticsNodes().isNotEmpty() }
                    compose.waitUntil(5_000) {
                        val image = compose.onNodeWithTag("mermaid-viewport").captureToImage().toPixelMap()
                        val background = image[0,0]
                        var ink = 0
                        for (y in 0 until image.height step 3) for (x in 0 until image.width step 3) if (image[x,y] != background) ink++
                        ink > 50
                    }
                    java.io.File(compose.activity.filesDir,"s07-real-expanded-light.png").outputStream().use { automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                    compose.onNodeWithContentDescription("확대 닫기").performClick()
                }
            }
            compose.onNodeWithTag("markdown-body").performScrollToIndex(diagrams[6].index)
            compose.waitUntil(15_000) { compose.onAllNodesWithText("다이어그램을 표시하지 못했어요").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("원문 보기").onFirst().performScrollTo().performClick()
            compose.onNodeWithText("닫기").assertDoesNotExist()
            compose.onNodeWithContentDescription("패널 닫기").performTouchInput {
                down(center); moveTo(center + androidx.compose.ui.geometry.Offset(0f,600f), delayMillis = 250); up()
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Mermaid 원문").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("마지막 본문입니다.", substring = true))
            compose.onNodeWithText("마지막 본문입니다.", substring = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
            compose.onNodeWithText("반가워요.").assertIsDisplayed()
        } finally {
            resolver.delete(sampleUri, null, null)
        }
    }

}
