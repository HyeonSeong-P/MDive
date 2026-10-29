package com.parka.mdive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.parka.mdive.data.document.CommonMarkParser
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.domain.document.*
import com.parka.mdive.testing.ProductTestActivity
import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReaderMathTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()

    @Test fun nativeEngineRendersBasicProfileAndKeepsFailuresLocal() {
        val source = "앞 \$x_1^2\$ 뒤\n\n\$\$\\frac{\\alpha}{\\sqrt{2}}+\\sum_{i=1}^n i+\\int_0^1 x dx\$\$\n\n```math\n\\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}\n```\n\n\$\\unsupported{x}\$\n\n마지막"
        val blocks = runBlocking { CommonMarkParser().parse(source).blocks }
        val inline = (blocks[0] as MarkdownBlock.Paragraph).runs.mapNotNull { it.math }.single()
        assertNotNull("inline native rendering", inline.raster)
        val display = (blocks[1] as MarkdownBlock.Math).math.raster
        assertNotNull("display native rendering", display)
        assertNotNull("matrix native rendering", (blocks[2] as MarkdownBlock.Math).math.raster)
        assertNull((blocks[3] as MarkdownBlock.Paragraph).runs.single().math?.raster)
        val pixels = inline.raster!!.copyPixels()
        assertTrue(pixels.any { (it ushr 24) != 0 })
        assertTrue(inline.raster.depth > 0)
        assertTrue(inline.raster.depth < inline.raster.height)
        compose.setContent { MDiveDesignTheme { ReaderMarkdownContent(MarkdownDocument(blocks)) } }
        compose.onNodeWithText("수식을 표시하지 못했어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("마지막").performScrollTo().assertIsDisplayed()
    }

    @Test fun inlinePlaceholderUsesTextBaselineAndTableKeepsAlignment() {
        val document = runBlocking { CommonMarkParser().parse("before \$x_1\$ after\n\n| left | center | right |\n| :--- | :---: | ---: |\n| \$x\$ | middle | end |") }
        compose.setContent { MDiveDesignTheme { ReaderMarkdownContent(document) } }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onAllNodesWithTag("math-inline").onFirst().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals(layout.firstBaseline, layout.placeholderRects.single()!!.bottom, 1f)
        for ((text, align) in listOf("left" to TextAlign.Left, "center" to TextAlign.Center, "right" to TextAlign.Right)) {
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertEquals(align, results.single().layoutInput.style.textAlign)
        }
    }

    @Test fun sourceSheetClosesWithButtonAndSystemBackBeforeLeavingDocument() {
        var home = 0
        val vm = ReaderViewModel(object : DocumentRepository {
            override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("math.md", "앞\n\n\$\\unknown{x}\$\n\n뒤"))
        }, CommonMarkParser())
        compose.setContent { MDiveDesignTheme { ReaderRoute({ home++ }, DocumentReference("fixture"), vm) } }
        compose.onNodeWithText("원문 보기").performClick()
        compose.onNodeWithText("수식 원문").assertIsDisplayed()
        compose.onNodeWithText("\$\\unknown{x}\$").assertIsDisplayed()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithText("수식 원문").assertDoesNotExist()
        compose.onNodeWithText("원문 보기").performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("수식 원문").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, home) }
        compose.onNodeWithText("뒤", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun longInlineMovesToLocalScrollAtNarrowWidthLargeFontAndBothThemes() {
        val tex = (1..60).joinToString("+") { "x_$it" }
        val document = runBlocking { CommonMarkParser().parse("앞 \$$tex\$ 뒤\n\n끝") }
        val dark = mutableStateOf(false)
        compose.setContent {
            MDiveDesignTheme(darkTheme = dark.value) {
                val original = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(original.density, 1.6f)) {
                    Box(Modifier.width(320.dp)) { ReaderMarkdownContent(document) }
                }
            }
        }
        compose.onNodeWithTag("math-block").assertIsDisplayed()
        compose.onNodeWithContentDescription("수식: $tex").assertIsDisplayed().performTouchInput { swipeLeft() }
        compose.onNodeWithText("뒤", substring = true).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { dark.value = true }
        compose.onNodeWithTag("math-block").assertIsDisplayed()
        compose.onNodeWithText("끝").performScrollTo().assertIsDisplayed()
    }
}
