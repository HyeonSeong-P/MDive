package com.parka.mdive.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.parka.mdive.MainActivity
import com.parka.mdive.data.document.ContentImageRepository
import dagger.hilt.android.EntryPointAccessors
import com.parka.mdive.testing.ImageDebugEntryPoint
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Uses the real user-visible Download/MDive-S10 fixture and unmodified SAF contracts. */
class LinkSystemPickerTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation
    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
    private fun labels() = nodes(automation.rootInActiveWindow).mapNotNull { it.text ?: it.contentDescription }.joinToString()
    private fun click(label: String, timeout: Long = 1500): Boolean {
        val end = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < end) {
            for (node in nodes(automation.rootInActiveWindow).filter { it.text?.toString().equals(label, ignoreCase = true) || it.contentDescription?.toString().equals(label, ignoreCase = true) }.sortedBy { if (it.viewIdResourceName == "android:id/title") 1 else 0 }.asReversed()) {
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                if (!bounds.isEmpty) {
                    shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
                    SystemClock.sleep(350)
                    return true
                }
            }
            SystemClock.sleep(100)
        }
        return false
    }
    private fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
    private fun awaitPlatformPicker() {
        val deadline = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < deadline && !automation.rootInActiveWindow?.packageName.toString().contains("documentsui")) SystemClock.sleep(100)
        assertTrue("Picker not active: " + labels(), automation.rootInActiveWindow?.packageName.toString().contains("documentsui"))
    }
    private fun cancelPlatformPicker() {
        awaitPlatformPicker()
        // DocumentsUI back first walks its restored folder path; only stop once it returns
        // to the product, rather than mistaking a parent-folder transition for cancellation.
        repeat(8) {
            if (automation.rootInActiveWindow?.packageName?.toString() == "com.parka.mdive") return
            shell("input keyevent 4")
            SystemClock.sleep(600)
        }
        assertEquals("Picker did not close", "com.parka.mdive", automation.rootInActiveWindow?.packageName?.toString())
    }

    private fun downloads() {
        click("Show roots", 600) || click("Open navigation drawer", 600)
        if (!click("Downloads", 1500) && !click("다운로드", 500)) {
            assertTrue("Storage root missing: " + labels(), click("sdk_gphone64_arm64", 1500))
            assertTrue("Download folder missing: " + labels(), click("Download", 2000))
        }
    }
    private fun openSample() {
        compose.onNodeWithText("파일 열기").performClick()
        SystemClock.sleep(500)
        // DocumentsUI restores its last file location. Prefer the visible sample before
        // opening roots, which may otherwise toggle a drawer still transitioning in.
        if (!click("links.md", 1500)) {
            downloads()
            assertTrue("Folder missing: " + labels(), click("MDive-S10", 2500))
            assertTrue("Sample missing: " + labels(), click("links.md", 2500))
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("markdown-body").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun chooseFolder(wrong: Boolean = false) {
        awaitPlatformPicker()
        SystemClock.sleep(500)
        click("Show roots", 600) || click("Open navigation drawer", 600)
        assertTrue("Storage root missing: " + labels(), click("sdk_gphone64_arm64", 1500))
        assertTrue("Download folder missing: " + labels(), click("Download", 2000))
        assertTrue("Folder missing: " + labels(), click("MDive-S10", 2500))
        if (wrong) assertTrue(click("하위", 2500))
        assertTrue("Cannot use folder: " + labels(), click("Use this folder", 2500) || click("이 폴더 사용", 1500))
        assertTrue("Permission dialog missing: " + labels(), click("Allow", 8000) || click("허용", 2000))
    }
    private fun awaitTitle(title: String) { compose.waitUntil(12_000) {
        try { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() } catch (_: IllegalStateException) { false }
    } }
    private fun follow(label: String) {
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText(label))
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        val node = compose.onNodeWithText(label)
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        node.performTouchInput { click(layouts.single().getBoundingBox(0).center) }
    }
    private fun assertPixels() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("image-ready").fetchSemanticsNodes().isNotEmpty() }
        val pixels = compose.onNodeWithTag("image-picture", useUnmergedTree = true).captureToImage().toPixelMap()
        var colorful = 0
        for (y in 0 until pixels.height step 3) for (x in 0 until pixels.width step 3) {
            val c = pixels[x,y]; if (maxOf(c.red,c.green,c.blue) - minOf(c.red,c.green,c.blue) > .15f) colorful++
        }
        assertTrue("Missing image pixels", colorful > 100)
    }
    private fun back() { compose.onNodeWithContentDescription("이전 화면으로 돌아가기").performClick() }
    private fun capture(name: String) { java.io.File(compose.activity.filesDir,name).outputStream().use { automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }

    @Test fun actualFileFolderRecoveryContinuousNavigationBrowserAndReturn() {
        assumeTrue("Explicit Download fixture harness", InstrumentationRegistry.getArguments().getString("linkProcessPhase") == "setup")
        // Remove only S10 fixture mappings, leaving the S08+S09 persisted image handoff intact.
        val prefs = compose.activity.getSharedPreferences("image-folders", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.filterValues { it is String && it.contains("MDive-S10") }.keys.forEach { key -> editor.remove(key).remove("directory:$key").remove("generation:$key") }
        editor.commit()
        openSample()
        follow("하위 문서")
        awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("links.md").assertIsDisplayed()
        compose.onNodeWithText("문서 폴더 연결").performClick()
        SystemClock.sleep(600); cancelPlatformPicker()
        awaitTitle("links.md")
        compose.onNodeWithText("links.md").assertIsDisplayed()
        compose.onNodeWithText("문서 폴더 연결").performClick(); chooseFolder(wrong = true)
        awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("links.md").assertIsDisplayed()
        compose.onNodeWithText("문서 폴더 연결").performClick(); chooseFolder()
        awaitTitle("하위 문서 정상")
        compose.onNodeWithText("문서 한글.MARKDOWN").assertIsDisplayed()
        assertPixels(); capture("s10-target-body.png")
        compose.onNodeWithTag("image-ready").performClick()
        compose.onNodeWithTag("image-expanded").assertIsDisplayed()
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        follow("상위 문서"); awaitTitle("links.md")
        compose.onNodeWithText("S10 실제 링크").assertIsDisplayed()
        back(); awaitTitle("하위 문서 정상")
        follow("다음 문서"); awaitTitle("연속 이동 정상")
        shell("input keyevent 4"); awaitTitle("하위 문서 정상")
        back(); awaitTitle("links.md")
        // The link was far below the first screen; its restored position is visible without scrolling.
        compose.onNodeWithText("하위 문서").assertIsDisplayed()
        capture("s10-returned-body.png")
        follow("없는 문서"); awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("links.md").assertIsDisplayed()
        // A failed replacement must retain the earlier successful document/tree connection.
        compose.onNodeWithText("문서 폴더 연결").performClick(); chooseFolder(wrong = true)
        awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("닫기").performClick()
        follow("하위 문서"); awaitTitle("하위 문서 정상")
        back(); awaitTitle("links.md")
        follow("현재 fragment"); awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("닫기").performClick()
        follow("대상 fragment"); awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("닫기").performClick()
        follow("범위 밖"); awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("닫기").performClick()
        follow("기타 URI"); awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("닫기").performClick()
        follow("웹 브라우저")
        val deadline = SystemClock.uptimeMillis() + 8000
        while (SystemClock.uptimeMillis() < deadline && automation.rootInActiveWindow?.packageName?.toString() == "com.parka.mdive") SystemClock.sleep(200)
        assertNotEquals("Expected browser UI", "com.parka.mdive", automation.rootInActiveWindow?.packageName?.toString())
        // Browser first-run UI is external; return to the original task without changing its state.
        repeat(5) {
            if (automation.rootInActiveWindow?.packageName?.toString() != "com.parka.mdive") { shell("input keyevent 4"); SystemClock.sleep(500) }
        }
        awaitTitle("links.md")
        compose.onNodeWithText("웹 브라우저").assertIsDisplayed()
        // Same URI may occur more than once; each visit must be a distinct Nav entry.
        follow("하위 문서"); awaitTitle("하위 문서 정상")
        follow("자기 자신"); awaitTitle("하위 문서 정상")
        back(); awaitTitle("하위 문서 정상")
        back(); awaitTitle("links.md")
        compose.onNodeWithText("하위 문서").assertIsDisplayed()
        compose.activityRule.scenario.recreate(); awaitTitle("links.md")
        back(); awaitTitle("반가워요.")
    }
    @Test fun persistedTreeSurvivesProcessRestartThenRevoke() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("linkProcessPhase") == "persisted")
        openSample(); follow("하위 문서"); awaitTitle("하위 문서 정상")
        assertPixels()
        val grants = compose.activity.contentResolver.persistedUriPermissions.filter { it.uri.toString().contains("MDive-S10") }
        assertTrue("No persisted S10 grant", grants.isNotEmpty())
        grants.forEach { compose.activity.contentResolver.releasePersistableUriPermission(it.uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        back(); awaitTitle("links.md"); back()
    }
    @Test fun revokedTreeMaintainsOriginalBodyAndReconnects() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("linkProcessPhase") == "revoked")
        openSample(); follow("하위 문서"); awaitTitle("링크를 열지 못했어요")
        compose.onNodeWithText("links.md").assertIsDisplayed()
        compose.onNodeWithText("문서 폴더 연결").performClick(); chooseFolder()
        awaitTitle("하위 문서 정상"); assertPixels()
        back(); awaitTitle("links.md"); compose.onNodeWithText("하위 문서").assertIsDisplayed()
        back()
    }

}
