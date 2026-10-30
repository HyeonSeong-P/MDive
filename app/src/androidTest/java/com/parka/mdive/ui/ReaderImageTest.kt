package com.parka.mdive.ui

import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.parka.mdive.data.document.*
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.domain.document.*
import com.parka.mdive.testing.ProductTestActivity
import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.After

class ReaderImageTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()
    private val testContext get() = InstrumentationRegistry.getInstrumentation().context
    private val decoderDirectory by lazy { java.io.File(compose.activity.cacheDir, "coil-reader-test-${java.util.UUID.randomUUID()}") }
    private val decoderDelegate = lazy { ImageDecoder(compose.activity, diskDirectory = decoderDirectory) }
    private val imageDecoder by decoderDelegate
    @After fun closeDecoder() { if (decoderDelegate.isInitialized()) { imageDecoder.close(); decoderDirectory.deleteRecursively() } }
    private fun bytes(name: String) = testContext.assets.open("images/그림 폴더/$name").use { it.readBytes() }
    private fun awaitImage() { compose.waitUntil(10_000) { compose.onAllNodesWithTag("image-ready").fetchSemanticsNodes().isNotEmpty() } }
    private fun ink(tag: String = "image-picture") {
        val map = compose.onAllNodesWithTag(tag, useUnmergedTree = true).onFirst().captureToImage().toPixelMap()
        var colorful = 0
        for (y in 0 until map.height step 3) for (x in 0 until map.width step 3) {
            val c = map[x,y]
            if (maxOf(c.red,c.green,c.blue) - minOf(c.red,c.green,c.blue) > .15f) colorful++
        }
        assertTrue("actual image pixels missing", colorful > 100)
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        java.io.File(compose.activity.filesDir, name).outputStream().use { automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun realDecodersDisplayEveryApprovedFormatAndGifKeepsFirstFrame() = runBlocking<Unit> {
        val decoder = imageDecoder
        listOf("풍경.png", "풍경.jpg", "풍경.webp", "도형.svg").forEach { name ->
            val raster = decoder.decode(bytes(name))
            assertEquals(name, 640, raster.width); assertEquals(name, 360, raster.height)
            assertTrue(name, Color.blue(raster.pixels[300 * 640 + 20]) > Color.red(raster.pixels[300 * 640 + 20]))
            assertTrue(name, Color.red(raster.pixels[70 * 640 + 530]) > 200)
        }
        val gif = decoder.decode(bytes("첫 프레임.gif"))
        assertEquals(120, gif.width); assertTrue(Color.red(gif.pixels[0]) > 200)
        android.os.SystemClock.sleep(300)
        assertArrayEquals(gif.pixels, decoder.decode(bytes("첫 프레임.gif")).pixels)
    }
    @Test fun svgRejectsExecutionExternalEntitiesCompressionAndInternalReferenceCycles() = runBlocking<Unit> {
        fun svg(body: String) = "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'>$body</svg>"
        val rejected = listOf(
            svg("<script>alert(1)</script>"), svg("<image href='https://example.com/a'/>"),
            svg("<rect width='20' height='20' onclick='x()'/>"), svg("<style>@import 'https://x';</style>"),
            svg("<style>.a{fill:u/**/rl(#p)}</style>"),
            "<!DOCTYPE svg [<!ENTITY x 'danger'>]>" + svg("<text>&x;</text>"),
            "<?xml-stylesheet href='https://x'?>" + svg(""),
            svg("<use id='a' href='#a'/>"),
            svg("<defs><g id='a'><use href='#b'/></g><g id='b'><use href='#a'/></g></defs><use href='#a'/>"),
            svg("<defs><linearGradient id='a' href='#b'/><linearGradient id='b' href='#a'/></defs><rect fill='url(#a)'/>"),
            svg("<defs><pattern id='a' width='1' height='1'><rect fill='url(#a)' width='1' height='1'/></pattern></defs><rect fill='url(#a)'/>")
        )
        rejected.forEach { input ->
            listOf(input.toByteArray(), input.toByteArray(Charsets.UTF_16)).forEach { encoded ->
                try { imageDecoder.decode(encoded); fail(input) } catch (_: ImageProblem) { }
            }
        }
        val compressed = java.io.ByteArrayOutputStream().apply { java.util.zip.GZIPOutputStream(this).use { it.write(svg("").toByteArray()) } }.toByteArray()
        try { imageDecoder.decode(compressed); fail() } catch (_: ImageProblem) { }
        val exponential = (0..20).joinToString("") { level -> if (level == 0) "<g id='g0'><rect width='1' height='1'/></g>" else "<g id='g$level'><use href='#g${level-1}'/><use href='#g${level-1}'/></g>" }
        try { imageDecoder.decode(svg("<defs>$exponential</defs><use href='#g20'/>").toByteArray()); fail() } catch (_: ImageProblem) { }
        try { imageDecoder.decode(svg("<style>.a{fill:u<![CDATA[rl]]>(#p)}</style>").toByteArray()); fail() } catch (_: ImageProblem) { }
        val safe = svg("<defs><g id='safe'><rect width='20' height='20' fill='red'/></g></defs><use href='#safe'/><text>A &amp; B</text>")
        assertEquals(20, imageDecoder.decode(safe.toByteArray()).width)
        val viewBox = imageDecoder.decode("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 640 360'><rect width='640' height='360' fill='red'/></svg>".toByteArray())
        assertEquals(640, viewBox.width); assertEquals(360, viewBox.height)
        try { imageDecoder.decode("<svg xmlns='http://www.w3.org/2000/svg' width='4001' height='4000'/>".toByteArray()); fail() } catch (e: ImageProblem) { assertEquals(ImageFailure.TooLarge, e.reason) }
        assertEquals(16_000_000, imageDecoder.decode("<svg xmlns='http://www.w3.org/2000/svg' width='4000' height='4000'/>".toByteArray()).pixels.size)
    }
    @Test fun scrollExpansionAndThemeKeepActualPixelsWithoutReloadLoop() {
        val raster = runBlocking { imageDecoder.decode(bytes("풍경.png")) }
        var requests = 0
        val prepared = mutableSetOf<String>()
        val repository = object : ImageRepository {
            override suspend fun connect(document: DocumentReference, folder: String): ImageFailure? = null
            override suspend fun read(document: DocumentReference, destination: String, bypassCache: Boolean, documentVersion: String): ImageReadResult { if (prepared.add(destination) || bypassCache) requests++; return ImageReadResult.Ready(raster) }
        }
        val source = "![첫 그림](a.png)\n\n![둘째 그림](b.png)\n\n" + (1..35).joinToString("\n\n") { "스크롤 문단 $it" }
        val vm = ReaderViewModel(object : DocumentRepository { override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("image.md", source)) }, CommonMarkParser(), repository)
        val dark = mutableStateOf(false)
        compose.setContent { MDiveDesignTheme(darkTheme = dark.value) { ReaderRoute({}, DocumentReference("fixture"), vm) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("image-ready").fetchSemanticsNodes().size == 2 }
        compose.waitForIdle(); assertEquals(2, requests)
        ink(); compose.onAllNodesWithTag("image-loading").assertCountEquals(0)
        compose.onAllNodesWithTag("image-ready").onFirst().performClick()
        compose.onNodeWithTag("image-expanded").assertIsDisplayed(); ink("image-expanded-picture")
        compose.onNodeWithContentDescription("확대").performClick(); compose.onNodeWithText("200%").assertIsDisplayed()
        compose.onNodeWithTag("image-viewport").performTouchInput {
            down(0, center - androidx.compose.ui.geometry.Offset(40f, 0f)); down(1, center + androidx.compose.ui.geometry.Offset(40f, 0f))
            moveTo(0, center - androidx.compose.ui.geometry.Offset(100f, 0f)); moveTo(1, center + androidx.compose.ui.geometry.Offset(100f, 0f)); up(0); up(1)
        }
        compose.onNodeWithTag("image-viewport").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("전체 맞춤").performClick(); compose.onNodeWithText("100%").assertIsDisplayed()
        capture("s08-expanded-light.png")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("image-expanded").assertDoesNotExist()
        assertEquals(2, requests)
        compose.runOnIdle { dark.value = true }
        ink(); assertEquals(2, requests)
        compose.onAllNodesWithTag("image-ready").onFirst().performClick()
        compose.onNodeWithTag("image-expanded").assertIsDisplayed()
        ink("image-expanded-picture")
        capture("s08-expanded-dark.png")
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        compose.onNodeWithTag("markdown-body").performScrollToIndex(30)
        compose.onNodeWithTag("markdown-body").performScrollToIndex(0)
        awaitImage(); ink()
        val returned = requests
        repeat(2) { compose.onNodeWithTag("markdown-body").performScrollToIndex(30); compose.onNodeWithTag("markdown-body").performScrollToIndex(0); awaitImage(); ink() }
        assertEquals(returned, requests)
    }
    @Test fun coilNativeCachesSkipFetchAndDecodeWhileRetryAndKeysInvalidate() = runBlocking<Unit> {
        val directory = java.io.File(compose.activity.cacheDir, "coil-test-${java.util.UUID.randomUUID()}")
        val decoder = ImageDecoder(compose.activity, diskDirectory = directory)
        var sourceReads = 0
        val png = bytes("풍경.png"); val svg = bytes("도형.svg")
        suspend fun load(key: String, web: Boolean = true, retry: Boolean = false, data: ByteArray = png) = decoder.load(key, web, retry) { sourceReads++; data }
        load("web-png"); load("web-png")
        assertEquals(1, sourceReads); assertEquals(1, decoder.decodeStarts.get())
        decoder.clearMemory(); load("web-png")
        assertEquals(1, sourceReads); assertEquals(2, decoder.decodeStarts.get())
        load("web-png", retry = true)
        assertEquals(2, sourceReads); assertEquals(3, decoder.decodeStarts.get())
        load("svg-original", data = svg)
        val svgFetches = sourceReads; val svgDecodes = decoder.decodeStarts.get()
        load("svg-original", data = svg)
        assertEquals("SVG must reuse native memory cache", svgFetches, sourceReads)
        assertEquals("SVG must not decode on cache hit", svgDecodes, decoder.decodeStarts.get())
        load("local-v1", web = false); decoder.clearMemory(); load("local-v1", web = false)
        assertEquals(svgFetches + 2, sourceReads) // Local bytes never go to disk cache.
        listOf(SecurityException(), java.io.FileNotFoundException(), java.io.InterruptedIOException(), java.io.IOException()).forEachIndexed { index, expected ->
            try { decoder.load("error-$index", false, false) { throw expected }; fail() } catch (actual: Exception) { assertSame(expected, actual) }
        }
        load("document-version-2"); load("folder-generation-2")
        assertEquals(svgFetches + 4, sourceReads)
        decoder.close(); directory.deleteRecursively()
    }

    @Test fun fullSixteenMegapixelImageDisplaysAndExpandsWithoutAnotherDecode() {
        val raster = runBlocking { imageDecoder.decode("<svg xmlns='http://www.w3.org/2000/svg' width='4000' height='4000'><rect width='4000' height='4000' fill='#de3641'/></svg>".toByteArray()) }
        assertEquals(16_000_000, raster.pixels.size)
        val repository = object : ImageRepository {
            override suspend fun connect(document: DocumentReference, folder: String): ImageFailure? = null
            override suspend fun read(document: DocumentReference, destination: String, bypassCache: Boolean, documentVersion: String) = ImageReadResult.Ready(raster)
        }
        val vm = ReaderViewModel(object : DocumentRepository {
            override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("large-image.md", "![16MP 이미지](large.svg)"))
        }, CommonMarkParser(), repository)
        compose.setContent { MDiveDesignTheme { ReaderRoute({}, DocumentReference("16mp"), vm) } }
        awaitImage(); ink()
        compose.onNodeWithTag("image-ready").performClick()
        compose.onNodeWithTag("image-expanded").assertIsDisplayed(); ink("image-expanded-picture")
        compose.onNodeWithContentDescription("확대").performClick()
        compose.onNodeWithText("200%").assertIsDisplayed()
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        compose.onNodeWithTag("image-ready").assertIsDisplayed()
        assertEquals(1, imageDecoder.decodeStarts.get())
    }

    @Test fun narrowLargeTextFailuresKeepLocalConnectSeparateFromWebRetry() {
        val document = runBlocking { CommonMarkParser().parse("![로컬 대체 설명](a.png)\n\n![웹 대체 설명](https://example.com/a.png)\n\n뒤 본문") }
        var connects = 0; var retries = 0
        compose.setContent { MDiveDesignTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ReaderMarkdownContent(document, Modifier.width(320.dp), images = ReaderImages(mapOf("a.png" to ImageReadResult.Failure(ImageFailure.Access), "https://example.com/a.png" to ImageReadResult.Failure(ImageFailure.Network)), onLoad = { _, retry -> if (retry) retries++ }, onConnect = { connects++ }))
            }
        } }
        compose.onNodeWithText("이미지 폴더 연결").performScrollTo().performClick()
        compose.onNodeWithText("다시 시도").performScrollTo().performClick()
        assertEquals(1, connects); assertEquals(1, retries)
        compose.onNodeWithTag("markdown-body").performScrollToNode(hasText("뒤 본문")); compose.onNodeWithText("뒤 본문").assertIsDisplayed()
        capture("s08-narrow-failures.png")
    }
}
