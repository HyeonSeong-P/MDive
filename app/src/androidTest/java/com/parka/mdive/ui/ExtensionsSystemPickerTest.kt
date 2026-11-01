package com.parka.mdive.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.parka.mdive.MainActivity
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Explicit real Downloads fixture; ordinary suites do not depend on user storage contents. */
class ExtensionsSystemPickerTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation
    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
    private fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
    private fun labels() = nodes(automation.rootInActiveWindow).mapNotNull { it.text ?: it.contentDescription }.joinToString()
    private fun click(label: String, timeout: Long = 2000): Boolean {
        val until = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < until) {
            for (node in nodes(automation.rootInActiveWindow).filter { it.text?.toString().equals(label, true) || it.contentDescription?.toString().equals(label, true) }.sortedBy { if (it.viewIdResourceName == "android:id/title") 1 else 0 }.asReversed()) {
                val bounds = android.graphics.Rect(); node.getBoundsInScreen(bounds)
                if (!bounds.isEmpty) { shell("input tap ${bounds.centerX()} ${bounds.centerY()}"); SystemClock.sleep(350); return true }
            }
            SystemClock.sleep(100)
        }
        return false
    }
    private fun picker() {
        val until = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < until && !automation.rootInActiveWindow?.packageName.toString().contains("documentsui")) SystemClock.sleep(100)
        assertTrue("Picker missing: " + labels(), automation.rootInActiveWindow?.packageName.toString().contains("documentsui"))
    }
    private fun fixtureFolder() {
        click("Show roots", 600) || click("Open navigation drawer", 600)
        assertTrue("Device root missing: " + labels(), click("sdk_gphone64_arm64"))
        assertTrue("Download missing: " + labels(), click("Download"))
        assertTrue("Fixture folder missing: " + labels(), click("MDive-S11-S12"))
    }
    private fun open() {
        compose.onNodeWithText("파일 열기").performClick(); picker(); SystemClock.sleep(500)
        if (!click("extensions.md", 1000)) {
            fixtureFolder()
            assertTrue("Fixture missing: " + labels(), click("extensions.md"))
        }
        await("S11 S12 혼합 문서")
    }
    private fun await(text: String) { compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun scroll(text: String) = compose.onNodeWithTag("markdown-body").performScrollToNode(hasText(text))
    private fun follow(text: String) {
        scroll(text)
        val node = compose.onAllNodesWithText(text).onFirst()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        node.performTouchInput { click(layouts.single().getBoundingBox(0).center) }
    }
    private fun back() = compose.onNodeWithContentDescription("이전 화면으로 돌아가기").performClick()
    private fun capture(name: String) { java.io.File(compose.activity.filesDir, name).outputStream().use { automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }

    @Test fun actualSelectionDetailsResourcesRelativeNavigationAndVisitRestoration() {
        assumeTrue("Explicit Downloads fixture harness", InstrumentationRegistry.getArguments().getString("extensionsFixture") == "true")
        // This test owns only its fixture mappings; prior SAF/cache handoffs remain intact.
        val prefs = compose.activity.getSharedPreferences("image-folders", android.content.Context.MODE_PRIVATE)
        prefs.edit().also { editor ->
            prefs.all.filterValues { it is String && it.contains("MDive-S11-S12") }.keys.forEach { key -> editor.remove(key).remove("directory:$key").remove("generation:$key") }
        }.commit()
        open()
        scroll("펼칠 내용")
        compose.onNodeWithText("숨겨진 본문 표시", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("펼칠 내용").performClick()
        compose.onNodeWithText("숨겨진 본문 표시").assertExists()
        scroll("중첩 내용")
        compose.onNodeWithText("중첩 내용").performClick()
        compose.onNodeWithText("중첩 본문을 접었다 펼쳐도 방문 상태를 유지합니다.", useUnmergedTree = true).assertDoesNotExist()
        follow("상대 문서 열기")
        await("링크를 열지 못했어요")
        compose.onNodeWithText("문서 폴더 연결").performClick(); picker(); SystemClock.sleep(500)
        fixtureFolder()
        assertTrue(click("Use this folder") || click("이 폴더 사용"))
        assertTrue(click("Allow", 8000) || click("허용"))
        await("S11 S12 대상 문서")
        compose.onNodeWithText("상대 문서 이동 성공").assertIsDisplayed()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("image-ready").fetchSemanticsNodes().isNotEmpty() }
        val pixels = compose.onNodeWithTag("image-picture", useUnmergedTree = true).captureToImage().toPixelMap()
        var colored = 0
        for (y in 0 until pixels.height step 3) for (x in 0 until pixels.width step 3) {
            val p = pixels[x, y]; if (maxOf(p.red, p.green, p.blue) - minOf(p.red, p.green, p.blue) > .15f) colored++
        }
        assertTrue("Image pixels missing", colored > 100)
        capture("s11-target-body.png")
        compose.onNodeWithTag("image-ready").performClick()
        compose.onNodeWithTag("image-expanded").assertIsDisplayed()
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        follow("웹 열기")
        val until = SystemClock.uptimeMillis() + 8000
        while (SystemClock.uptimeMillis() < until && automation.rootInActiveWindow?.packageName?.toString() == "com.parka.mdive") SystemClock.sleep(100)
        assertNotEquals("External browser missing", "com.parka.mdive", automation.rootInActiveWindow?.packageName?.toString())
        repeat(5) { if (automation.rootInActiveWindow?.packageName?.toString() != "com.parka.mdive") { shell("input keyevent 4"); SystemClock.sleep(500) } }
        await("S11 S12 대상 문서")
        back(); await("extensions.md")
        // A restored position, not a fresh scroll to the details, must expose this link.
        compose.onAllNodesWithText("상대 문서 열기").onFirst().assertIsDisplayed()
        capture("s11-returned-body.png")
        scroll("중첩 내용")
        compose.onNodeWithText("중첩 본문을 접었다 펼쳐도 방문 상태를 유지합니다.", useUnmergedTree = true).assertDoesNotExist()
        compose.activityRule.scenario.recreate(); await("extensions.md")
        scroll("중첩 내용")
        compose.onNodeWithText("중첩 본문을 접었다 펼쳐도 방문 상태를 유지합니다.", useUnmergedTree = true).assertDoesNotExist()
        scroll("펼칠 내용"); compose.onNodeWithText("펼칠 내용").performClick()
        compose.onNodeWithText("숨겨진 본문 표시", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("펼칠 내용").performClick()
        compose.onNodeWithText("숨겨진 본문 표시").assertExists()
        scroll("중첩 내용")
        compose.onNodeWithText("중첩 본문을 접었다 펼쳐도 방문 상태를 유지합니다.", useUnmergedTree = true).assertDoesNotExist()
        back(); await("반가워요.")
    }
}
