package com.parka.mdive.ui

import com.parka.mdive.ui.reader.*
import com.parka.mdive.domain.document.*
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MermaidStateTest {
    @Test fun sourceAndExpansionAreExclusiveAndBackClosesBeforeLeaving() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val diagram = MarkdownMermaid("graph TD; A-->B", "```mermaid\ngraph TD; A-->B\n```", true)
            val vm = ReaderViewModel(object : DocumentRepository {
                override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("x.md", "body"))
            }, object : MarkdownParser { override suspend fun parse(source: String) = MarkdownDocument(listOf(MarkdownBlock.Mermaid(diagram))) })
            vm.onIntent(ReaderIntent.Open(DocumentReference("fixture")))
            advanceUntilIdle()
            vm.onIntent(ReaderIntent.ViewMermaidSource(diagram.source))
            assertEquals(diagram.source, vm.uiState.value.mermaidSource)
            vm.onIntent(ReaderIntent.ExpandMermaid(diagram))
            assertNull(vm.uiState.value.mermaidSource)
            assertEquals(diagram, vm.uiState.value.expandedMermaid)
            vm.onIntent(ReaderIntent.Back)
            assertNull(vm.uiState.value.expandedMermaid)
            assertEquals(ReaderContent.SourceReady, vm.uiState.value.content)
            vm.onIntent(ReaderIntent.ViewMermaidSource(diagram.source))
            vm.onIntent(ReaderIntent.Back)
            assertNull(vm.uiState.value.mermaidSource)
            vm.onIntent(ReaderIntent.ExpandMermaid(diagram))
            vm.onIntent(ReaderIntent.ViewMathSource("\$x\$"))
            assertNull(vm.uiState.value.expandedMermaid)
            assertEquals("\$x\$", vm.uiState.value.mathSource)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun transformStaysBetweenFitAndEightAndPanCannotLoseDrawing() {
        assertEquals(8f, DiagramTransform().zoomBy(100f).scale)
        assertEquals(DiagramTransform(), DiagramTransform().zoomBy(0.1f))
        val moved = DiagramTransform().change(2f, Offset(1000f, -1000f), Offset.Zero, 300f, 400f)
        assertEquals(150f, moved.x)
        assertEquals(-200f, moved.y)
        assertEquals(DiagramTransform(), moved.change(0.01f, Offset.Zero, Offset.Zero, 300f, 400f))
    }
}
