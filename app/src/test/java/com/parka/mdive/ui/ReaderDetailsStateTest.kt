package com.parka.mdive.ui

import com.parka.mdive.domain.document.*
import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderDetailsStateTest {
    private val closed = MarkdownBlock.Details("closed", listOf(MarkdownRun("닫힌 제목")), false, emptyList())
    private val open = MarkdownBlock.Details("open", listOf(MarkdownRun("열린 제목")), true, listOf(closed))
    private val document = MarkdownDocument(listOf(MarkdownBlock.DefinitionList(listOf(
        MarkdownDefinition(listOf(listOf(MarkdownRun("용어"))), listOf(listOf(MarkdownBlock.Callout(MarkdownCalloutKind.Note, "Note", listOf(open))))),
    ))))
    private fun reader() = ReaderViewModel(object : DocumentRepository {
        override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("details.md", "details"))
    }, object : MarkdownParser { override suspend fun parse(source: String) = document })

    @Test fun expansionBelongsToVisitAndIgnoresUnknownOrLateEvents() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = reader()
            vm.onIntent(ReaderIntent.SetDetailsExpanded("closed", true))
            assertTrue(vm.uiState.value.detailsExpanded.isEmpty())
            val ref = DocumentReference("same-document")
            vm.onIntent(ReaderIntent.Open(ref))
            assertTrue(vm.uiState.value.detailsExpanded.isEmpty()) // The model owns initial defaults.
            vm.onIntent(ReaderIntent.SetDetailsExpanded("unknown", true))
            assertTrue(vm.uiState.value.detailsExpanded.isEmpty())
            vm.onIntent(ReaderIntent.SetDetailsExpanded("closed", true))
            vm.onIntent(ReaderIntent.SetDetailsExpanded("open", false))
            assertEquals(mapOf("closed" to true, "open" to false), vm.uiState.value.detailsExpanded)
            vm.onIntent(ReaderIntent.LinkReturned)
            vm.onIntent(ReaderIntent.Open(ref)) // Route recomposition/rotation must not reset this visit.
            assertEquals(true, vm.uiState.value.detailsExpanded["closed"])
            assertEquals(false, vm.uiState.value.detailsExpanded["open"])
            val otherVisit = reader()
            otherVisit.onIntent(ReaderIntent.Open(ref))
            assertTrue(otherVisit.uiState.value.detailsExpanded.isEmpty())
            vm.onIntent(ReaderIntent.Back)
            vm.onIntent(ReaderIntent.SetDetailsExpanded("closed", false))
            assertEquals(true, vm.uiState.value.detailsExpanded["closed"])
        } finally { Dispatchers.resetMain() }
    }
    @Test fun hidingLastImageCancelsPendingRequestWhileOtherVisibleCopiesKeepItAlive() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var calls = 0
            var cancelled = 0
            val images = object : ImageRepository {
                override suspend fun connect(document: DocumentReference, folder: String): ImageFailure? = null
                override suspend fun read(document: DocumentReference, destination: String, bypassCache: Boolean, documentVersion: String): ImageReadResult {
                    calls++
                    try { kotlinx.coroutines.awaitCancellation() }
                    finally { cancelled++ }
                }
            }
            val vm = ReaderViewModel(object : DocumentRepository {
                override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("a.md", "image"))
            }, object : MarkdownParser { override suspend fun parse(source: String) = document }, images)
            vm.onIntent(ReaderIntent.Open(DocumentReference("a"))); runCurrent()
            val image = MarkdownImage("a.png", "그림")
            vm.onIntent(ReaderIntent.ImageVisibility(image, true))
            vm.onIntent(ReaderIntent.ImageVisibility(image, true)); runCurrent()
            assertEquals(1, calls)
            vm.onIntent(ReaderIntent.ImageVisibility(image, false)); runCurrent()
            assertEquals(0, cancelled)
            vm.onIntent(ReaderIntent.ImageVisibility(image, false)); runCurrent()
            assertEquals(1, cancelled)
            assertTrue(vm.uiState.value.images.isEmpty())
            vm.onIntent(ReaderIntent.ImageVisibility(image, true)); runCurrent()
            assertEquals(2, calls)
            vm.onIntent(ReaderIntent.Back); runCurrent()
            assertEquals(2, cancelled)
        } finally { Dispatchers.resetMain() }
    }

}
