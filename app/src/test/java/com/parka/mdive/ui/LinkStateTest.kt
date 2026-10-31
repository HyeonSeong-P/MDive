package com.parka.mdive.ui

import com.parka.mdive.domain.document.*
import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LinkStateTest {
    private class Links : DocumentLinkRepository {
        var calls = 0
        var pending: CompletableDeferred<DocumentLinkResult>? = null
        var result: DocumentLinkResult = DocumentLinkResult.Failure(LinkFailure.Missing)
        override suspend fun resolve(document: DocumentReference, destination: String): DocumentLinkResult { calls++; return pending?.await() ?: result }
        override suspend fun connect(document: DocumentReference, destination: String, folder: String) = resolve(document, destination)
    }
    private fun vm(links: Links, failTarget: Boolean = false) = ReaderViewModel(object : DocumentRepository {
        override suspend fun read(reference: DocumentReference) = if (failTarget && reference.value == "target") DocumentReadResult.Failure(DocumentFailure.Access) else DocumentReadResult.Success(SourceDocument(reference.value, "body"))
    }, object : MarkdownParser { override suspend fun parse(source: String) = MarkdownDocument(emptyList()) }, links = links)

    @Test fun failedTargetAndUnsupportedLinksKeepOriginalBody() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val links = Links(); val vm = vm(links, true)
            vm.onIntent(ReaderIntent.Open(DocumentReference("original"))); runCurrent()
            links.result = DocumentLinkResult.Ready(DocumentReference("target"))
            vm.onIntent(ReaderIntent.OpenLink("target.md")); runCurrent()
            assertEquals(LinkFailure.Access, vm.uiState.value.linkFailure)
            assertEquals("original", vm.uiState.value.title); assertEquals("body", vm.uiState.value.document?.source)
            links.result = DocumentLinkResult.Failure(LinkFailure.Fragment)
            vm.onIntent(ReaderIntent.OpenLink("#title")); runCurrent()
            assertEquals(LinkFailure.Fragment, vm.uiState.value.linkFailure)
            assertEquals(ReaderContent.SourceReady, vm.uiState.value.content)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun duplicateAndCancelledLateResultsCannotNavigate() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val links = Links().apply { pending = CompletableDeferred() }; val vm = vm(links)
            vm.onIntent(ReaderIntent.Open(DocumentReference("original"))); runCurrent()
            vm.onIntent(ReaderIntent.OpenLink("target.md")); vm.onIntent(ReaderIntent.OpenLink("target.md")); runCurrent()
            assertEquals(1, links.calls); assertTrue(vm.uiState.value.linkLoading)
            vm.onIntent(ReaderIntent.Back); runCurrent()
            assertFalse(vm.uiState.value.linkLoading)
            links.pending!!.complete(DocumentLinkResult.Ready(DocumentReference("target"))); runCurrent()
            assertEquals("original", vm.uiState.value.title)
            vm.onIntent(ReaderIntent.Back); runCurrent()
            assertEquals(ReaderEffect.NavigateHome, vm.effects.first())
        } finally { Dispatchers.resetMain() }
    }
    @Test fun pickerCancelAndLateSelectionDoNotChangeDocument() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val links = Links(); val vm = vm(links)
            vm.onIntent(ReaderIntent.Open(DocumentReference("original"))); runCurrent()
            links.result = DocumentLinkResult.Failure(LinkFailure.FolderRequired)
            vm.onIntent(ReaderIntent.OpenLink("a.md")); runCurrent()
            vm.onIntent(ReaderIntent.ConnectDocumentFolder); runCurrent()
            assertEquals(ReaderEffect.SelectDocumentFolder, vm.effects.first())
            vm.onIntent(ReaderIntent.DocumentFolderSelected(null)); runCurrent()
            assertEquals(1, links.calls); assertEquals(LinkFailure.FolderRequired, vm.uiState.value.linkFailure)
            vm.onIntent(ReaderIntent.ConnectDocumentFolder); runCurrent(); vm.effects.first()
            vm.onIntent(ReaderIntent.Back); vm.onIntent(ReaderIntent.DocumentFolderSelected("tree")); runCurrent()
            assertEquals(1, links.calls); assertFalse(vm.uiState.value.linkFolderPending)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun browserAndReadyNavigationAreSingleEffectsUntilReturn() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val links = Links().apply { result = DocumentLinkResult.Web("https://example.org") }; val vm = vm(links)
            vm.onIntent(ReaderIntent.Open(DocumentReference("original"))); runCurrent()
            vm.onIntent(ReaderIntent.OpenLink("https://example.org")); runCurrent()
            assertEquals(ReaderEffect.OpenBrowser("https://example.org"), vm.effects.first())
            vm.onIntent(ReaderIntent.OpenLink("https://example.org")); runCurrent(); assertEquals(1, links.calls)
            vm.onIntent(ReaderIntent.BrowserFailed); assertNotNull(vm.uiState.value.linkFailure)
            links.result = DocumentLinkResult.Ready(DocumentReference("target"))
            vm.onIntent(ReaderIntent.OpenLink("target.md")); runCurrent()
            val effect = vm.effects.first() as ReaderEffect.NavigateDocument
            assertEquals("target", effect.prepared.source.title)
            vm.onIntent(ReaderIntent.LinkReturned); vm.onIntent(ReaderIntent.OpenLink("target.md")); runCurrent()
            assertEquals(3, links.calls)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun imageFolderConnectionBlocksLinkResolutionUntilItCompletes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val links = Links()
            val connection = CompletableDeferred<ImageFailure?>()
            val images = object : ImageRepository {
                override suspend fun connect(document: DocumentReference, folder: String) = connection.await()
                override suspend fun read(document: DocumentReference, destination: String, bypassCache: Boolean, documentVersion: String) = ImageReadResult.Failure(ImageFailure.Missing)
            }
            val vm = ReaderViewModel(object : DocumentRepository {
                override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("original", "body"))
            }, object : MarkdownParser { override suspend fun parse(source: String) = MarkdownDocument(emptyList()) }, images, links)
            vm.onIntent(ReaderIntent.Open(DocumentReference("original"))); runCurrent()
            vm.onIntent(ReaderIntent.ConnectImageFolder); runCurrent(); vm.effects.first()
            vm.onIntent(ReaderIntent.ImageFolderSelected("tree")); runCurrent()
            vm.onIntent(ReaderIntent.OpenLink("a.md")); runCurrent()
            assertEquals(0, links.calls)
            connection.complete(null); runCurrent()
            vm.onIntent(ReaderIntent.OpenLink("a.md")); runCurrent()
            assertEquals(1, links.calls)
        } finally { Dispatchers.resetMain() }
    }

}
