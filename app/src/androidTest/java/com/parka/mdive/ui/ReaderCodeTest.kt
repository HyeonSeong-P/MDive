package com.parka.mdive.ui

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
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

class ReaderCodeTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()

    @Test fun realClipboardCopiesExactBodyThroughMviWithoutDuplicateModernFeedback() {
        val body = " \tval wave = \"🌊\"  \r\n\r\n"
        compose.setContent {
            MDiveDesignTheme {
                val vm = remember { ReaderViewModel(object : DocumentRepository {
                    override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("code.md", "```Kt\r\n$body```"))
                }, CommonMarkParser()) }
                ReaderRoute({}, DocumentReference("fixture"), vm)
            }
        }
        compose.onNodeWithContentDescription("코드 복사").performClick()
        compose.runOnIdle {
            val clipboard = compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(body, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
        if (Build.VERSION.SDK_INT >= 33) compose.onNodeWithText("코드를 복사했어요").assertDoesNotExist()
        else compose.onNodeWithText("코드를 복사했어요").assertIsDisplayed()
    }

    @Test fun clipboardFailureShowsFeedbackAndKeepsBodyAndRetryButton() {
        var attempts = 0
        var denied = true
        compose.setContent {
            MDiveDesignTheme {
                val platform = LocalClipboard.current
                val failing = remember { object : Clipboard {
                    override val nativeClipboard get() = platform.nativeClipboard
                    override suspend fun getClipEntry(): ClipEntry? = error("Must not read clipboard")
                    override suspend fun setClipEntry(clipEntry: ClipEntry?) { attempts++; if (denied) throw SecurityException("Denied") }
                } }
                val vm = remember { ReaderViewModel(object : DocumentRepository {
                    override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("code.md", "```unknown\nreadable\n```"))
                }, CommonMarkParser()) }
                CompositionLocalProvider(LocalClipboard provides failing) { ReaderRoute({}, DocumentReference("fixture"), vm) }
            }
        }
        compose.onNodeWithContentDescription("코드 복사").performClick()
        compose.onNodeWithText("복사하지 못했어요. 다시 시도해주세요").assertIsDisplayed()
        compose.onNodeWithText("readable\n").assertIsDisplayed()
        compose.onNodeWithContentDescription("코드 복사").performClick()
        compose.runOnIdle { assertEquals(2, attempts); denied = false }
        compose.onNodeWithContentDescription("코드 복사").performClick()
        compose.onNodeWithText("복사하지 못했어요. 다시 시도해주세요").assertDoesNotExist()
        compose.runOnIdle { assertEquals(3, attempts) }
    }

    @Test fun longCodeScrollsLocallyWithFixedHeaderAtNarrowWidthAndLargeFontInBothThemes() {
        val body = "val wave = \"" + "long".repeat(70) + "\"\n"
        val document = runBlocking { CommonMarkParser().parse("```kotlin\n$body```\n\n뒤 본문") }
        val dark = androidx.compose.runtime.mutableStateOf(false)
        var copied: String? = null
        compose.setContent {
            MDiveDesignTheme(darkTheme = dark.value) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                    Box(Modifier.width(320.dp)) { ReaderMarkdownContent(document, onCopyCode = { copied = it }) }
                }
            }
        }
        for (mode in listOf(false, true)) {
            compose.runOnUiThread { dark.value = mode }
            compose.onNodeWithText("kotlin").assertIsDisplayed()
            compose.onNodeWithContentDescription("코드 복사").assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(body, copied) }
            val code = compose.onNodeWithText(body)
            val layouts = mutableListOf<TextLayoutResult>()
            code.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(2, layouts.single().lineCount)
            assertTrue(layouts.single().layoutInput.text.spanStyles.isNotEmpty())
            val scroll = code.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
            assertTrue(scroll.maxValue() > 0)
            code.performSemanticsAction(SemanticsActions.ScrollBy) { it(1000f, 0f) }
            compose.onNodeWithContentDescription("코드 복사").assertIsDisplayed()
            compose.onNodeWithText("kotlin").assertIsDisplayed()
            compose.onNodeWithText("뒤 본문").assertIsDisplayed()
            assertFalse(compose.onNodeWithTag("markdown-body").fetchSemanticsNode().config.contains(SemanticsProperties.HorizontalScrollAxisRange))
        }
    }
}
