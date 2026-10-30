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

/** Uses the real user-visible Download/MDive-S08 fixture and unmodified SAF contracts. */
class ImageSystemPickerTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation
    private val repo get() = EntryPointAccessors.fromApplication(compose.activity.application, ImageDebugEntryPoint::class.java).images() as ContentImageRepository
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
    private fun cancelPlatformPicker() {
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
        if (!click("images.md", 1500)) {
            downloads()
            assertTrue("Folder missing: " + labels(), click("MDive-S08", 2500))
            assertTrue("Sample missing: " + labels(), click("images.md", 2500))
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("markdown-body").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun chooseFolder(wrong: Boolean = false) {
        SystemClock.sleep(500)
        click("Show roots", 600) || click("Open navigation drawer", 600)
        assertTrue("Storage root missing: " + labels(), click("sdk_gphone64_arm64", 1500))
        assertTrue("Download folder missing: " + labels(), click("Download", 2000))
        assertTrue("Folder missing: " + labels(), click("MDive-S08", 2500))
        if (wrong) assertTrue(click("images", 2500))
        assertTrue("Cannot use folder: " + labels(), click("Use this folder", 2500) || click("이 폴더 사용", 1500))
        click("Allow", 1500) || click("허용", 1000)
    }
    private fun ready() { compose.waitUntil(10_000) { compose.onAllNodesWithTag("image-ready").fetchSemanticsNodes().isNotEmpty() } }
    private fun picture(description: String): SemanticsNodeInteraction {
        val imageIndex = when (description) {
            "산과 바다 · PNG" -> 3
            "산과 바다 · JPEG" -> 6
            "산과 바다 · WebP" -> 8
            "빨간색 첫 프레임 · 애니메이션 없음" -> 10
            "산과 바다 · 정적 SVG" -> 12
            "위키미디어 PNG 투명도 예제" -> 14
            else -> error("Unknown fixture image: $description")
        }
        compose.onNodeWithTag("markdown-body").performScrollToIndex(imageIndex)
        // Hold the real image's viewport while asynchronous loading completes. Searching the
        // entire lazy list before readiness can scroll away and cancel the very request tested.
        compose.waitUntil(25_000) { compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty() }
        return compose.onNodeWithContentDescription(description).performScrollTo()
    }
    private fun pixels(description: String) {
        val map = picture(description).captureToImage().toPixelMap()
        var colorful = 0
        for (y in 0 until map.height step 3) for (x in 0 until map.width step 3) { val c = map[x,y]; if (maxOf(c.red,c.green,c.blue) - minOf(c.red,c.green,c.blue) > .15f) colorful++ }
        assertTrue("No picture pixels: $description", colorful > 100)
    }
    private fun capture(name: String) { java.io.File(compose.activity.filesDir,name).outputStream().use { automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }

    @Test fun actualDownloadedImagesConnectCancelWrongFolderAndPersist() {
        assumeTrue("Explicit Download fixture harness only", InstrumentationRegistry.getArguments().getString("imageProcessPhase") == "setup")
        compose.activity.getSharedPreferences("image-folders", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        openSample()
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("이미지 폴더 연결"))
        compose.onAllNodesWithText("이미지 폴더 연결").onFirst().performClick()
        SystemClock.sleep(500); cancelPlatformPicker()
        compose.waitUntil(5000) {
            try { compose.onAllNodesWithText("이미지 폴더 연결").fetchSemanticsNodes().isNotEmpty() }
            catch (_: IllegalStateException) { false }
        }
        compose.onNodeWithText("images.md").assertIsDisplayed()
        compose.onAllNodesWithText("이미지 폴더 연결").onFirst().performClick(); chooseFolder(wrong = true)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("연결한 폴더에 파일이 없어요. 문서 기준 폴더와 경로를 확인해주세요.", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("이미지 폴더 연결").onFirst().performClick(); chooseFolder()
        ready(); pixels("산과 바다 · PNG")
        capture("s08-real-sample.png")
        val starts = repo.readStarts.get()
        // Warm only successful local images. Unsupported/missing resources deliberately are not
        // cached as success and must not be counted as a cache-reuse failure.
        repeat(2) {
            compose.onNodeWithTag("markdown-body").performScrollToIndex(8)
            pixels("산과 바다 · WebP")
            compose.onNodeWithTag("markdown-body").performScrollToIndex(3)
            pixels("산과 바다 · PNG")
        }
        val afterWarm = repo.readStarts.get()
        val decodesAfterWarm = repo.decodeStarts.get()
        repeat(2) {
            compose.onNodeWithTag("markdown-body").performScrollToIndex(8)
            pixels("산과 바다 · WebP")
            compose.onNodeWithTag("markdown-body").performScrollToIndex(3)
            pixels("산과 바다 · PNG")
        }
        assertEquals(afterWarm, repo.readStarts.get())
        assertEquals(decodesAfterWarm, repo.decodeStarts.get())
        println("S08 actual local cache: fetch=$afterWarm decode=$decodesAfterWarm unchanged after repeated viewport disposal/re-entry")
        picture("산과 바다 · PNG").performClick()
        compose.onNodeWithTag("image-expanded").assertIsDisplayed(); capture("s08-real-expanded.png")
        compose.onNodeWithContentDescription("확대").performClick(); compose.onNodeWithText("200%").assertIsDisplayed()
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        assertEquals(afterWarm, repo.readStarts.get())
        assertEquals(decodesAfterWarm, repo.decodeStarts.get())
        listOf("산과 바다 · JPEG", "산과 바다 · WebP", "빨간색 첫 프레임 · 애니메이션 없음", "산과 바다 · 정적 SVG").forEach { pixels(it) }
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("스크롤 끝"))
        compose.onNodeWithText("스크롤 끝").assertIsDisplayed()
        compose.onNodeWithTag("markdown-body").performScrollToIndex(3)
        pixels("산과 바다 · PNG")
        capture("s08-real-returned.png")
        assertTrue(starts > 0)
        val prefs = compose.activity.getSharedPreferences("image-folders", android.content.Context.MODE_PRIVATE)
        assertEquals(1, prefs.all.values.filterIsInstance<String>().size)
        assertTrue(compose.activity.contentResolver.persistedUriPermissions.any { it.isReadPermission && it.uri.toString().contains("MDive-S08") })
        compose.activityRule.scenario.recreate(); ready(); pixels("산과 바다 · PNG")
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick(); compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }

    @Test fun actualHttpsImageDisplaysPixelsAndExpands() {
        assumeTrue("Explicit live HTTPS fixture harness only", InstrumentationRegistry.getArguments().getString("imageProcessPhase") == "https")
        openSample()
        compose.onNodeWithTag("markdown-body").performScrollToIndex(14)
        fun webReady() = compose.onAllNodesWithContentDescription("위키미디어 PNG 투명도 예제").fetchSemanticsNodes().isNotEmpty()
        fun retryVisible() = compose.onAllNodesWithText("다시 시도").fetchSemanticsNodes().isNotEmpty()
        compose.waitUntil(25_000) { webReady() || retryVisible() }
        if (!webReady()) {
            capture("s09-real-https-failure.png")
            val fetches = repo.readStarts.get()
            compose.onAllNodesWithText("다시 시도").onFirst().performClick()
            compose.waitUntil(5000) { repo.readStarts.get() > fetches }
            compose.waitUntil(25_000) { webReady() || retryVisible() }
            compose.onNodeWithText("부분 실패 · 본문 유지").assertIsDisplayed()
            capture("s09-real-https-after-retry.png")
            assertTrue("Live server still rejected the image after a fresh retry; alt, retry, and surrounding body remained visible. " + compose.onRoot().printToString(), webReady())
        }
        pixels("위키미디어 PNG 투명도 예제")
        capture("s09-real-https.png")
        picture("위키미디어 PNG 투명도 예제").performClick()
        compose.onNodeWithTag("image-expanded").assertIsDisplayed()
        capture("s09-real-https-expanded.png")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("image-expanded").assertDoesNotExist()
        pixels("위키미디어 PNG 투명도 예제")
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
    }

    /** Run in a separate instrumentation invocation after force-stop to verify persisted connection. */
    @Test fun persistedConnectionAfterProcessRestartAndRevokedGrantRecovers() {
        assumeTrue("Explicit external force-stop harness only", InstrumentationRegistry.getArguments().getString("imageProcessPhase") == "persisted")
        assertFalse("Run the setup test first", compose.activity.getSharedPreferences("image-folders", android.content.Context.MODE_PRIVATE).all.isEmpty())
        openSample(); compose.onNodeWithTag("markdown-body").performScrollToIndex(3); ready(); pixels("산과 바다 · PNG")
        capture("s08-after-process-restart.png")
        val grants = compose.activity.contentResolver.persistedUriPermissions.filter { it.uri.toString().contains("MDive-S08") }
        assertTrue(grants.isNotEmpty())
        grants.forEach { compose.activity.contentResolver.releasePersistableUriPermission(it.uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        // Force-stop is performed by the calling harness before the next test invocation;
        // transient grants cannot be revoked while the current activity task still owns them.
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
    }
    @Test fun revokedGrantShowsReconnectAndCanRecover() {
        assumeTrue("Explicit external revoke/force-stop harness only", InstrumentationRegistry.getArguments().getString("imageProcessPhase") == "revoked")
        openSample(); compose.onNodeWithTag("markdown-body").performScrollToIndex(3)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("이미지 폴더 연결").fetchSemanticsNodes().isNotEmpty() }
        capture("s08-revoked-grant.png")
        compose.onAllNodesWithText("이미지 폴더 연결").onFirst().performClick(); chooseFolder()
        ready(); pixels("산과 바다 · PNG")
        capture("s08-after-reconnect.png")
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
    }
}
