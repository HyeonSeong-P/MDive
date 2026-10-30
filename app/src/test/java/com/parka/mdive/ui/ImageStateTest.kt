package com.parka.mdive.ui

import com.parka.mdive.domain.document.*
import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageStateTest {
    private class Images : ImageRepository {
        val calls = mutableListOf<Pair<String, Boolean>>()
        var connection: CompletableDeferred<ImageFailure?>? = null
        var pendingRead: CompletableDeferred<ImageReadResult>? = null
        override suspend fun connect(document: DocumentReference, folder: String): ImageFailure? = connection?.await()
        override suspend fun read(document: DocumentReference, destination: String, bypassCache: Boolean, documentVersion: String): ImageReadResult {
            calls += destination to bypassCache
            return pendingRead?.await() ?: ImageReadResult.Ready(ImageRaster(2, 2, IntArray(4) { 0xffff0000.toInt() }))
        }
    }
    private fun vm(images: Images) = ReaderViewModel(object : DocumentRepository {
        override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("images.md", "body"))
    }, object : MarkdownParser { override suspend fun parse(source: String) = MarkdownDocument(emptyList()) }, images)

    @Test fun visibleResultsAreDeliveredTogetherAndRepositoryOwnsScrollReuse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val images = Images(); val vm = vm(images)
            vm.onIntent(ReaderIntent.Open(DocumentReference("doc"))); runCurrent()
            val a = MarkdownImage("a.png", "A"); val b = MarkdownImage("b.png", "B")
            vm.onIntent(ReaderIntent.ImageVisibility(a, true)); vm.onIntent(ReaderIntent.ImageVisibility(b, true)); runCurrent()
            assertEquals(2, images.calls.size)
            assertEquals(2, vm.uiState.value.images.size) // Both results delivered in the same scheduler frame.
            vm.onIntent(ReaderIntent.ExpandImage(a, (vm.uiState.value.images[a.destination] as ImageReadResult.Ready).raster))
            assertNotNull(vm.uiState.value.expandedImageRaster)
            vm.onIntent(ReaderIntent.Back)
            assertNull(vm.uiState.value.expandedImage)
            vm.onIntent(ReaderIntent.ImageVisibility(b, false)); vm.onIntent(ReaderIntent.ImageVisibility(b, true)); runCurrent()
            assertEquals(3, images.calls.size)
            vm.onIntent(ReaderIntent.ImageVisibility(a, false)); vm.onIntent(ReaderIntent.ImageVisibility(a, true)); runCurrent()
            assertEquals(4, images.calls.size) // Repository owns offscreen cache decisions.
            vm.onIntent(ReaderIntent.LoadImage(a, true)); runCurrent()
            assertEquals("a.png" to true, images.calls.last())
        } finally { Dispatchers.resetMain() }
    }
    @Test fun leavingWithMultiplePendingImagesCancelsAllWithoutMutatingAnActiveIterator() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val images = Images().apply { pendingRead = CompletableDeferred() }
            val vm = vm(images)
            vm.onIntent(ReaderIntent.Open(DocumentReference("doc"))); runCurrent()
            vm.onIntent(ReaderIntent.ImageVisibility(MarkdownImage("a.png", "A"), true))
            vm.onIntent(ReaderIntent.ImageVisibility(MarkdownImage("b.png", "B"), true)); runCurrent()
            assertEquals(2, images.calls.size)
            vm.onIntent(ReaderIntent.Back); runCurrent()
            assertEquals(ReaderEffect.NavigateHome, vm.effects.first())
            images.pendingRead!!.complete(ImageReadResult.Ready(ImageRaster(1, 1, intArrayOf(0))))
            runCurrent()
            assertTrue(vm.uiState.value.images.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun cancelledFolderPickerRetainsImagesAndConnectingPreventsSecondPicker() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val images = Images(); val vm = vm(images)
            vm.onIntent(ReaderIntent.Open(DocumentReference("doc"))); runCurrent()
            val a = MarkdownImage("a.png", "A")
            vm.onIntent(ReaderIntent.ImageVisibility(a, true)); runCurrent()
            vm.onIntent(ReaderIntent.ConnectImageFolder); vm.onIntent(ReaderIntent.ImageFolderSelected(null)); runCurrent()
            assertEquals(1, images.calls.size); assertFalse(vm.uiState.value.folderPickerPending)
            images.connection = CompletableDeferred()
            vm.onIntent(ReaderIntent.ConnectImageFolder); vm.onIntent(ReaderIntent.ImageFolderSelected("tree-a")); runCurrent()
            assertTrue(vm.uiState.value.folderPickerPending)
            vm.onIntent(ReaderIntent.ConnectImageFolder)
            images.connection!!.complete(null); runCurrent()
            assertFalse(vm.uiState.value.folderPickerPending)
            assertEquals(1, vm.uiState.value.imageGeneration)
            assertTrue(vm.uiState.value.images.isEmpty())
        } finally { Dispatchers.resetMain() }
    }
}
