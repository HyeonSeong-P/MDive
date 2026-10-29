package com.parka.mdive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toPixelMap
import com.parka.mdive.data.document.CommonMarkParser
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.domain.document.*
import com.parka.mdive.testing.ProductTestActivity
import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReaderMermaidTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()
    private fun awaitDisplayed(text: String) { compose.waitUntil(5_000) { try { compose.onNodeWithText(text).assertIsDisplayed(); true } catch (_: AssertionError) { false } } }
    private fun awaitReady() { compose.waitUntil(15_000) { compose.onAllNodesWithTag("mermaid-ready").fetchSemanticsNodes().isNotEmpty() } }

    @Test fun realOfflineEngineRendersAllFiveFamiliesAndFailuresStayLocal() {
        val examples = listOf(
            "flowchart TD\nA[시작]-->B[완료]",
            "sequenceDiagram\nAlice->>Bob: Hello",
            "classDiagram\nclass Animal {\n<<interface>>\n+int age\n}\nAnimal <|-- Duck",
            "stateDiagram-v2\nstate Choice <<choice>>\n[*] --> Choice\nChoice --> Ready: yes\nstate Ready {\n[*] --> Inner\n}",
            "erDiagram\nCUSTOMER ||--o{ ORDER : places",
        )
        val invalid = listOf("flowchart TD\nA[unfinished", "sequenceDiagram\nAlice->>:", "classDiagram\nA <|--", "stateDiagram-v2\n[*] -->", "erDiagram\nA ||--o{ : has")
        for (code in examples + invalid) {
            val latch = CountDownLatch(1)
            var result: MermaidRenderState? = null
            lateinit var engine: MermaidWebView
            compose.activityRule.scenario.onActivity { activity ->
                engine = MermaidWebView(activity, code, false) { result = it; latch.countDown() }
            }
            assertTrue("Engine did not finish: $code", latch.await(15, TimeUnit.SECONDS))
            assertEquals(code, if (code in invalid) MermaidRenderState.Failure else MermaidRenderState.Ready, result)
            compose.activityRule.scenario.onActivity { engine.close() }
        }
    }

    @Test fun parsedConnectionLimitAppliesToEveryFamily() {
        val examples = listOf(
            "flowchart TD\n" + (0..500).joinToString("\n") { "A$it --> B$it" },
            "sequenceDiagram\n" + (0..500).joinToString("\n") { "A->>B: message $it" },
            "classDiagram\n" + (0..500).joinToString("\n") { "A$it -- B$it" },
            "stateDiagram-v2\n" + (0..500).joinToString("\n") { "A$it --> B$it" },
            "erDiagram\n" + (0..500).joinToString("\n") { "A$it ||--o{ B$it : has" },
        )
        for (code in examples) {
            val latch = CountDownLatch(1)
            var result: MermaidRenderState? = null
            lateinit var engine: MermaidWebView
            compose.activityRule.scenario.onActivity { engine = MermaidWebView(it, code, false) { state -> result = state; latch.countDown() } }
            assertTrue(latch.await(15, TimeUnit.SECONDS))
            assertEquals(code.take(30), MermaidRenderState.Failure, result)
            compose.activityRule.scenario.onActivity { engine.close() }
        }
    }

    @Test fun normalAndFailedSourceSheetsAndExpandedBackKeepReaderPosition() {
        var home = 0
        val source = (1..20).joinToString("\n\n") { "앞 문단 $it" } + "\n\n```mermaid\nflowchart TD; A-->B\n```\n\n뒤 문단\n\n```mermaid\npie\n```"
        val vm = ReaderViewModel(object : DocumentRepository {
            override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("mermaid.md", source))
        }, CommonMarkParser())
        compose.setContent { MDiveDesignTheme { ReaderRoute({ home++ }, DocumentReference("fixture"), vm) } }
        compose.onNodeWithTag("markdown-body").performScrollToIndex(20)
        awaitReady()
        compose.onAllNodesWithText("원문 보기").onFirst().performScrollTo().assertIsDisplayed().performClick()
        awaitDisplayed("Mermaid 원문")
        compose.onNodeWithText("```mermaid\nflowchart TD; A-->B\n```").assertIsDisplayed()
        compose.onNodeWithContentDescription("패널 닫기").performClick()
        compose.onNodeWithTag("mermaid-picture").performScrollTo()
        val original = compose.onNodeWithTag("mermaid-ready").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("mermaid-picture").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("mermaid-viewport").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("확대").performClick()
        compose.onNodeWithTag("mermaid-viewport").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"2.0배"))
        compose.onNodeWithTag("mermaid-viewport").performTouchInput {
            down(0, center - androidx.compose.ui.geometry.Offset(40f, 0f))
            down(1, center + androidx.compose.ui.geometry.Offset(40f, 0f))
            moveTo(0, center - androidx.compose.ui.geometry.Offset(100f, 0f))
            moveTo(1, center + androidx.compose.ui.geometry.Offset(100f, 0f))
            up(0); up(1)
        }
        val zoomed = compose.onNodeWithTag("mermaid-viewport").fetchSemanticsNode().config[SemanticsProperties.StateDescription].removeSuffix("배").toFloat()
        assertTrue("Pinch did not increase scale", zoomed > 2f)
        compose.onNodeWithTag("mermaid-viewport").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("전체 맞춤").performClick()
        compose.onNodeWithTag("mermaid-viewport").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"1.0배"))
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        assertEquals(original, compose.onNodeWithTag("mermaid-ready").fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.onNodeWithTag("mermaid-picture").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("mermaid-viewport").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mermaid-viewport").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"1.0배"))
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("mermaid-expanded").assertDoesNotExist()
        compose.onNodeWithTag("markdown-body").performScrollToIndex(22)
        compose.onNodeWithText("다이어그램을 표시하지 못했어요").assertIsDisplayed()
        compose.onAllNodesWithText("원문 보기").onLast().performClick()
        compose.onNodeWithText("```mermaid\npie\n```").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Mermaid 원문").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, home) }
    }

    @Test fun longDiagramNarrowLargeTextBothThemesAndMalformedInputKeepBodyReadable() {
        val large = "flowchart LR\n" + (1..40).joinToString("\n") { "A$it --> A${it+1}" }
        val document = runBlocking { CommonMarkParser().parse("```mermaid\n$large\n```\n\n뒤 본문\n\n```mermaid\nflowchart TD\nA[unfinished\n```") }
        val dark = mutableStateOf(false)
        compose.setContent {
            MDiveDesignTheme(darkTheme = dark.value) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density,1.6f)) {
                    Box(Modifier.width(320.dp)) { ReaderMarkdownContent(document) }
                }
            }
        }
        awaitReady()
        compose.onNodeWithTag("mermaid-ready").assertIsDisplayed()
        compose.onNodeWithTag("mermaid-picture").assertWidthIsAtLeast(200.dp)
        compose.onNodeWithText("뒤 본문").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { dark.value = true }
        awaitReady()
        compose.onNodeWithText("다이어그램을 표시하지 못했어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("뒤 본문").performScrollTo().assertIsDisplayed()
    }
    @Test fun rendererDeathAfterReadyAndTimeoutReleaseTheView() {
        var state: MermaidRenderState? = null
        val ready = CountDownLatch(1)
        lateinit var engine: MermaidWebView
        compose.activityRule.scenario.onActivity { activity ->
            engine = MermaidWebView(activity, "flowchart TD; A-->B", false) { state = it; if (it == MermaidRenderState.Ready) ready.countDown() }
            activity.setContentView(android.widget.FrameLayout(activity).apply { addView(engine.view) })
        }
        assertTrue(ready.await(15, TimeUnit.SECONDS))
        compose.activityRule.scenario.onActivity {
            assertNotNull(engine.view.parent)
            engine.view.webViewClient.onRenderProcessGone(engine.view, object : android.webkit.RenderProcessGoneDetail() {
                override fun didCrash() = true
                override fun rendererPriorityAtExit() = android.webkit.WebView.RENDERER_PRIORITY_IMPORTANT
            })
            assertEquals(MermaidRenderState.Failure, state)
            assertNull(engine.view.parent)
            engine.close(); engine.close()
        }
        val timedOut = CountDownLatch(1)
        val start = android.os.SystemClock.uptimeMillis()
        compose.activityRule.scenario.onActivity { activity ->
            engine = MermaidWebView(activity, "flowchart TD; A-->B", false) { state = it; timedOut.countDown() }
            // Suppress only the trusted page completion in this test; production's 10s timer remains real.
            engine.view.webViewClient = object : android.webkit.WebViewClient() {
                override fun shouldInterceptRequest(view: android.webkit.WebView, request: android.webkit.WebResourceRequest) =
                    android.webkit.WebResourceResponse("text/html", "UTF-8", "<html>loading</html>".byteInputStream())
            }
            activity.setContentView(android.widget.FrameLayout(activity).apply { addView(engine.view) })
        }
        assertTrue(timedOut.await(15, TimeUnit.SECONDS))
        assertTrue(android.os.SystemClock.uptimeMillis() - start >= 9000)
        compose.activityRule.scenario.onActivity {
            assertEquals(MermaidRenderState.Failure, state)
            assertNull(engine.view.parent)
            engine.close()
        }
    }

    @Test fun readyDiagramHasActualVisiblePixels() {
        val source = "```mermaid\nflowchart TD\nA[문서 열기] --> B{다이어그램 지원?}\nB -->|예| C[다이어그램 표시]\nB -->|아니요| D[오류와 원문 보기]\nC --> E[확대해서 읽기]\nE --> F[본문으로 돌아오기]\nD --> F\n```"
        val document = runBlocking { CommonMarkParser().parse(source) }
        compose.setContent { MDiveDesignTheme { ReaderMarkdownContent(document) } }
        awaitReady()
        compose.activityRule.scenario.onActivity { activity ->
            fun web(v: android.view.View): android.webkit.WebView? = if (v is android.webkit.WebView) v else if (v is android.view.ViewGroup) (0 until v.childCount).firstNotNullOfOrNull { web(v.getChildAt(it)) } else null
            web(activity.window.decorView)?.evaluateJavascript("JSON.stringify({viewport:[innerWidth,innerHeight],body:[document.body.clientWidth,document.body.clientHeight],drawing:document.getElementById('drawing').getBoundingClientRect().toJSON(),svg:document.querySelector('svg').getBoundingClientRect().toJSON(),source:document.querySelector('svg').outerHTML.slice(0,600)})") { android.util.Log.i("MDiveMermaidMetrics",it) }
        }
        compose.waitUntil(5_000) {
            val image = compose.onNodeWithTag("mermaid-picture").captureToImage().toPixelMap()
            val background = image[0,0]
            var ink = 0
            for (y in 0 until image.height step 3) for (x in 0 until image.width step 3) if (image[x,y] != background) ink++
            ink > 50
        }
    }

    @Test fun completedSvgSurvivesScrollAndExpansionWhileThemeAndSourceInvalidate() {
        val code = "flowchart TD; A[첫째]-->B[둘째]"
        fun doc(value: String) = runBlocking { CommonMarkParser().parse("```mermaid\n$value\n```\n\n" + (1..40).joinToString("\n\n") { "문단 $it" }) }
        val document = mutableStateOf(doc(code))
        val cache = MermaidSessionCache()
        val dark = mutableStateOf(false)
        val expanded = mutableStateOf<MarkdownMermaid?>(null)
        compose.setContent { MDiveDesignTheme(darkTheme = dark.value) {
            ReaderMarkdownContent(document.value, onExpandMermaid = { expanded.value = it }, mermaidCache = cache)
            expanded.value?.let { MermaidExpanded(it, { expanded.value = null }, {}, cache) }
        } }
        fun webViews(view: android.view.View): List<android.webkit.WebView> = when (view) {
            is android.webkit.WebView -> listOf(view)
            is android.view.ViewGroup -> (0 until view.childCount).flatMap { webViews(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun ink(tag: String) { compose.waitUntil(5_000) {
            val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
            val background = pixels[0,0]
            var points = 0
            for (y in 0 until pixels.height step 3) for (x in 0 until pixels.width step 3) if (pixels[x,y] != background) points++
            points > 50
        } }
        fun capture(name: String) {
            ink("mermaid-viewport")
            val automation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            java.io.File(compose.activity.filesDir,name).outputStream().use { automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        }
        awaitReady(); ink("mermaid-picture")
        var firstView: android.webkit.WebView? = null
        compose.runOnIdle { assertEquals(1,cache.renderStarts); firstView = webViews(compose.activity.window.decorView).single(); assertTrue(firstView!!.settings.javaScriptEnabled) }
        compose.onNodeWithTag("markdown-body").performScrollToIndex(document.value.blocks.lastIndex)
        compose.onNodeWithTag("markdown-body").performScrollToIndex(0)
        awaitReady(); ink("mermaid-picture")
        compose.onAllNodesWithTag("mermaid-loading").assertCountEquals(0)
        compose.runOnIdle {
            val next = webViews(compose.activity.window.decorView).single()
            assertNotSame(firstView,next); assertFalse(next.settings.javaScriptEnabled); assertEquals(1,cache.renderStarts)
        }
        compose.onNodeWithTag("mermaid-picture").performClick()
        compose.onAllNodesWithTag("mermaid-expanded-loading").assertCountEquals(0)
        capture("s07-expanded-light.png")
        compose.runOnIdle { assertEquals(1,cache.renderStarts) }
        compose.onNodeWithContentDescription("축소").assertIsNotEnabled()
        compose.onNodeWithContentDescription("확대").performClick()
        compose.onNodeWithText("200%").assertIsDisplayed()
        compose.onNodeWithContentDescription("전체 맞춤").performClick()
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        compose.runOnIdle { dark.value = true }
        awaitReady()
        compose.runOnIdle { assertEquals(2,cache.renderStarts); assertTrue(webViews(compose.activity.window.decorView).single().settings.javaScriptEnabled) }
        compose.onNodeWithTag("mermaid-picture").performClick()
        capture("s07-expanded-dark.png")
        compose.runOnIdle { assertEquals(2,cache.renderStarts) }
        compose.onNodeWithContentDescription("확대 닫기").performClick()
        compose.runOnIdle { dark.value = false }
        awaitReady(); ink("mermaid-picture")
        compose.runOnIdle { assertEquals(2,cache.renderStarts); assertFalse(webViews(compose.activity.window.decorView).single().settings.javaScriptEnabled) }
        val other = doc("flowchart LR; C-->D")
        compose.runOnIdle { document.value = other }
        awaitReady()
        compose.runOnIdle { assertEquals(3,cache.renderStarts) }
        val malformed = doc("flowchart TD\nA[unfinished")
        compose.runOnIdle { document.value = malformed }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("mermaid-failure").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertEquals(4,cache.renderStarts) }
        compose.onNodeWithTag("markdown-body").performScrollToIndex(malformed.blocks.lastIndex)
        compose.onNodeWithTag("markdown-body").performScrollToIndex(0)
        compose.onNodeWithTag("mermaid-failure").assertIsDisplayed()
        compose.onAllNodesWithTag("mermaid-loading").assertCountEquals(0)
        compose.runOnIdle { assertEquals(4,cache.renderStarts) }
    }

    @Test fun bottomIconControlsFitNarrowLargeTextAndSkeletonHasOneLoadingMeaning() {
        val transform = mutableStateOf(DiagramTransform())
        compose.setContent { MDiveDesignTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.6f)) {
                androidx.compose.foundation.layout.Column(Modifier.width(320.dp)) {
                    MermaidSkeleton(Modifier.width(320.dp).then(Modifier.testTag("loading-preview")).height(240.dp))
                    MermaidZoomControls(transform.value, { transform.value = it })
                }
            }
        } }
        compose.onNodeWithContentDescription("Mermaid 다이어그램을 준비하고 있어요").assertHasNoClickAction()
        compose.onNodeWithText("콘텐츠 렌더링 중").assertDoesNotExist()
        compose.onNodeWithContentDescription("축소").assertIsNotEnabled().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("확대").assertIsDisplayed().performClick()
        compose.onNodeWithText("200%").assertIsDisplayed()
        compose.runOnIdle { transform.value = DiagramTransform(8f) }
        compose.onNodeWithContentDescription("확대").assertIsNotEnabled()
        compose.onNodeWithContentDescription("전체 맞춤").assertIsDisplayed().performClick()
        compose.onNodeWithText("100%").assertIsDisplayed()
        val bounds = compose.onNodeWithTag("mermaid-zoom-controls").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width <= with(compose.density) { 320.dp.toPx() })
    }

}
